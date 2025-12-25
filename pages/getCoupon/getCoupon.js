// pages/getCoupon/getCoupon.js
const app = getApp()

Page({
  data: {
    // 用户当天消费金额
    todaySpent: 0,
    // 可领取的优惠券列表
    availableCoupons: [],
    // 今天已领取的优惠券ID列表
    todayReceivedIds: [],
    // 当前日期（用于判断是否是新的一天）
    currentDate: ''
  },

  onLoad(options) {
    // 检查并初始化日期数据
    this.checkAndResetDailyData()
    // 计算用户当天消费金额
    this.calculateTodaySpent()
    // 加载可领取的优惠券
    this.loadAvailableCoupons()
  },

  onShow() {
    // 每次显示时检查日期并刷新数据
    this.checkAndResetDailyData()
    this.calculateTodaySpent()
    this.loadAvailableCoupons()
  },

  // 获取今天的日期字符串（YYYY-MM-DD格式）
  getTodayDateString() {
    const today = new Date()
    const year = today.getFullYear()
    const month = String(today.getMonth() + 1).padStart(2, '0')
    const day = String(today.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  // 检查并重置每日数据
  checkAndResetDailyData() {
    const today = this.getTodayDateString()
    const lastDate = wx.getStorageSync('couponLastDate') || ''
    
    // 如果是新的一天，重置领取记录
    if (lastDate !== today) {
      // 清除昨天的领取记录
      wx.removeStorageSync('dailyCouponReceived')
      // 更新日期
      wx.setStorageSync('couponLastDate', today)
    }
    
    this.setData({
      currentDate: today
    })
  },

  // 计算用户当天消费金额
  calculateTodaySpent() {
    const userId = app.getCurrentUserId()
    if (!userId) {
      this.setData({
        todaySpent: 0
      })
      return
    }

    const today = this.getTodayDateString()
    const todayStart = new Date(today + ' 00:00:00').getTime()
    const todayEnd = new Date(today + ' 23:59:59').getTime()

    // 获取用户的所有已完成订单
    const orders = app.getUserOrders()
    const completedOrders = orders.filter(order => {
      // 只统计今天完成的订单
      const orderTime = order.createTime || order.updateTime || 0
      return (order.status === 'completed' || order.status === 'delivering') &&
             orderTime >= todayStart && orderTime <= todayEnd
    })

    // 计算当天消费金额（使用原价，因为优惠券是额外奖励）
    let todaySpent = 0
    completedOrders.forEach(order => {
      // 使用原价，如果没有原价则使用总价
      const orderAmount = parseFloat(order.originalPrice || order.totalPrice || 0)
      todaySpent += orderAmount
    })

    this.setData({
      todaySpent: todaySpent.toFixed(2)
    })
  },

  // 加载可领取的优惠券
  loadAvailableCoupons() {
    // 获取今天已领取的优惠券ID（从每日记录中获取）
    const dailyReceived = wx.getStorageSync('dailyCouponReceived') || []
    const todayReceivedIds = dailyReceived.map(item => item.couponId)
    
    this.setData({
      todayReceivedIds: todayReceivedIds
    })

    // 定义可领取的优惠券列表（通过消费解锁）
    const couponTemplates = [
      {
        id: 'coupon_unlock_1',
        name: '消费满50元解锁',
        desc: '消费满50元即可领取',
        amount: 5,
        type: 'free',
        fullAmount: 0,
        unlockAmount: 50, // 解锁所需消费金额
        expireDays: 30 // 领取后有效期天数
      },
      {
        id: 'coupon_unlock_2',
        name: '消费满100元解锁',
        desc: '消费满100元即可领取',
        amount: 10,
        type: 'free',
        fullAmount: 0,
        unlockAmount: 100,
        expireDays: 30
      },
      {
        id: 'coupon_unlock_3',
        name: '消费满200元解锁',
        desc: '消费满200元即可领取',
        amount: 20,
        type: 'full',
        fullAmount: 100,
        unlockAmount: 200,
        expireDays: 30
      },
      {
        id: 'coupon_unlock_4',
        name: '消费满500元解锁',
        desc: '消费满500元即可领取',
        amount: 50,
        type: 'full',
        fullAmount: 200,
        unlockAmount: 500,
        expireDays: 30
      }
    ]

    // 根据当天消费金额和已领取状态，筛选可领取的优惠券
    const todaySpent = parseFloat(this.data.todaySpent)
    // 使用已更新的 todayReceivedIds（从 data 中获取）
    const receivedIds = this.data.todayReceivedIds
    
    const availableCoupons = couponTemplates.map(template => {
      const isUnlocked = todaySpent >= template.unlockAmount // 是否已解锁（基于当天消费）
      const isReceived = receivedIds.includes(template.id) // 今天是否已领取
      
      // 计算还需消费的金额（如果未解锁）
      const remainingAmount = isUnlocked ? 0 : Math.max(0, template.unlockAmount - todaySpent)
      const remainingAmountText = remainingAmount.toFixed(2) // 格式化为两位小数
      
      // 计算解锁进度
      const progress = Math.min((todaySpent / template.unlockAmount) * 100, 100)
      const progressPercent = Math.round(progress) // 进度百分比（整数）
      
      return {
        ...template,
        isUnlocked: isUnlocked,
        isReceived: isReceived,
        canReceive: isUnlocked && !isReceived, // 是否可以领取
        progress: progress, // 解锁进度（0-100）
        progressPercent: progressPercent, // 进度百分比（整数，用于显示）
        remainingAmount: remainingAmountText // 还需消费的金额（已格式化）
      }
    })

    this.setData({
      availableCoupons: availableCoupons
    })
  },

  // 领取优惠券
  receiveCoupon(e) {
    const couponId = e.currentTarget.dataset.id
    const coupon = this.data.availableCoupons.find(c => c.id === couponId)

    if (!coupon) {
      wx.showToast({
        title: '优惠券不存在',
        icon: 'none'
      })
      return
    }

    if (!coupon.canReceive) {
      if (!coupon.isUnlocked) {
        wx.showToast({
          title: `消费满${coupon.unlockAmount}元即可解锁`,
          icon: 'none',
          duration: 2000
        })
      } else if (coupon.isReceived) {
        wx.showToast({
          title: '已领取过该优惠券',
          icon: 'none'
        })
      }
      return
    }

    // 创建新的优惠券实例
    const now = Date.now()
    const newCoupon = {
      id: coupon.id,
      name: coupon.name,
      desc: coupon.desc,
      amount: coupon.amount,
      type: coupon.type,
      fullAmount: coupon.fullAmount,
      status: 'available',
      expireTime: now + coupon.expireDays * 24 * 60 * 60 * 1000,
      receiveTime: now // 领取时间
    }

    // 保存到优惠券列表（用于在优惠券页面显示）
    let coupons = wx.getStorageSync('coupons') || []
    coupons.push(newCoupon)
    wx.setStorageSync('coupons', coupons)

    // 记录今天已领取的优惠券（用于每日重置）
    const dailyReceived = wx.getStorageSync('dailyCouponReceived') || []
    dailyReceived.push({
      couponId: coupon.id,
      receiveTime: now,
      date: this.getTodayDateString()
    })
    wx.setStorageSync('dailyCouponReceived', dailyReceived)

    wx.showToast({
      title: '领取成功',
      icon: 'success'
    })

    // 刷新列表
    setTimeout(() => {
      this.loadAvailableCoupons()
    }, 1000)
  }
})

