// pages/selectCoupon/selectCoupon.js
Page({
  data: {
    couponList: [],
    selectedCouponId: null,
    totalPrice: 0
  },

  onLoad(options) {
    const totalPrice = parseFloat(options.totalPrice) || 0
    this.setData({
      totalPrice: totalPrice,
      selectedCouponId: options.selectedCouponId || null
    })
    this.loadAvailableCoupons()
  },

  // 加载可用优惠券
  loadAvailableCoupons() {
    const app = getApp()
    const storageKey = app.getUserStorageKey('coupons')
    let coupons = wx.getStorageSync(storageKey) || []
    
    // 如果没有数据，初始化一些示例数据
    if (coupons.length === 0) {
      coupons = this.getDefaultCoupons()
      wx.setStorageSync(storageKey, coupons)
    }

    // 筛选可用优惠券（不过滤不满足满减条件的，只是标记为不可用）
    const availableCoupons = coupons.filter(coupon => {
      if (coupon.status !== 'available') return false
      if (coupon.expireTime && coupon.expireTime < Date.now()) return false
      return true
    })

    // 格式化过期时间
    const formattedCoupons = availableCoupons.map(coupon => {
      // 计算还差多少金额可用（仅用于满减券）
      let needMoreAmount = '0.00'
      if (coupon.type === 'full' && this.data.totalPrice < coupon.fullAmount) {
        needMoreAmount = (coupon.fullAmount - this.data.totalPrice).toFixed(2)
      }
      
      return {
        ...coupon,
        expireTime: this.formatTime(coupon.expireTime),
        canUse: coupon.type === 'free' || this.data.totalPrice >= coupon.fullAmount,
        needMoreAmount: needMoreAmount
      }
    })

    this.setData({
      couponList: formattedCoupons
    })
  },

  // 获取默认优惠券（示例数据）
  getDefaultCoupons() {
    const now = Date.now()
    return [
      {
        id: 'coupon_1',
        name: '新用户专享',
        desc: '新用户首次下单可用',
        amount: 10,
        type: 'full',
        fullAmount: 50,
        status: 'available',
        expireTime: now + 30 * 24 * 60 * 60 * 1000 // 30天后
      },
      {
        id: 'coupon_2',
        name: '满减优惠',
        desc: '满100减20',
        amount: 20,
        type: 'full',
        fullAmount: 100,
        status: 'available',
        expireTime: now + 15 * 24 * 60 * 60 * 1000 // 15天后
      },
      {
        id: 'coupon_3',
        name: '无门槛优惠',
        desc: '任意金额可用',
        amount: 5,
        type: 'free',
        fullAmount: 0,
        status: 'available',
        expireTime: now + 20 * 24 * 60 * 60 * 1000 // 20天后
      },
      {
        id: 'coupon_4',
        name: '大额优惠',
        desc: '满200减50',
        amount: 50,
        type: 'full',
        fullAmount: 200,
        status: 'available',
        expireTime: now + 10 * 24 * 60 * 60 * 1000 // 10天后
      }
    ]
  },

  // 格式化时间
  formatTime(timestamp) {
    if (!timestamp) return ''
    const date = new Date(timestamp)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  // 选择优惠券
  selectCoupon(e) {
    const couponId = e.currentTarget.dataset.id
    const coupon = this.data.couponList.find(c => c.id === couponId)
    
    if (!coupon || !coupon.canUse) {
      wx.showToast({
        title: '该优惠券不可用',
        icon: 'none'
      })
      return
    }

    this.setData({
      selectedCouponId: couponId === this.data.selectedCouponId ? null : couponId
    })
  },

  // 确认选择
  confirmSelect() {
    const selectedCoupon = this.data.selectedCouponId 
      ? this.data.couponList.find(c => c.id === this.data.selectedCouponId)
      : null

    // 将选中的优惠券传递回上一页
    const pages = getCurrentPages()
    const prevPage = pages[pages.length - 2]
    if (prevPage && prevPage.setSelectedCoupon) {
      prevPage.setSelectedCoupon(selectedCoupon)
    }

    wx.navigateBack()
  },

  // 不使用优惠券
  noCoupon() {
    this.setData({
      selectedCouponId: null
    })
    this.confirmSelect()
  }
})

