// pages/coupons/coupons.js
Page({
  data: {
    currentTab: 'available',
    tabs: [
      { label: '可用', value: 'available' },
      { label: '已使用', value: 'used' },
      { label: '已过期', value: 'expired' }
    ],
    couponList: []
  },

  onLoad(options) {
    this.loadCoupons()
  },

  onShow() {
    this.loadCoupons()
  },

  // 加载优惠券列表
  loadCoupons() {
    let coupons = wx.getStorageSync('coupons') || []
    
    // 如果没数据，初始化一些示例数据
    if (coupons.length === 0) {
      coupons = this.getDefaultCoupons()
      wx.setStorageSync('coupons', coupons)
    }

    // 根据标签筛选
    let filteredCoupons = coupons
    if (this.data.currentTab === 'available') {
      filteredCoupons = coupons.filter(coupon => coupon.status === 'available')
    } else if (this.data.currentTab === 'used') {
      filteredCoupons = coupons.filter(coupon => coupon.status === 'used')
    } else if (this.data.currentTab === 'expired') {
      filteredCoupons = coupons.filter(coupon => coupon.status === 'expired')
    }

    // 格式化过期时间
    const formattedCoupons = filteredCoupons.map(coupon => {
      return {
        ...coupon,
        expireTime: this.formatTime(coupon.expireTime)
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
        status: 'used',
        expireTime: now - 10 * 24 * 60 * 60 * 1000 // 已过期
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

  // 切换标签
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({
      currentTab: tab
    })
    this.loadCoupons()
  },

  // 跳转到领取优惠券页面
  goToGetCoupon() {
    wx.navigateTo({
      url: '../getCoupon/getCoupon'
    })
  }
})

