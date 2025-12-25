// pages/my/my.js
const app = getApp()

Page({
  data: {
    userInfo: null,
    orderStats: {
      all: 0,
      pending: 0,
      delivering: 0,
      completed: 0
    }
  },

  onLoad(options) {
    this.loadUserInfo()
    this.loadOrderStats()
  },

  onShow() {
    // 每次显示时刷新用户信息和订单统计
    this.loadUserInfo()
    this.loadOrderStats()
  },

  // 加载用户信息
  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo') || app.globalData.userInfo
    this.setData({
      userInfo: userInfo
    })
  },

  // 加载订单统计（模拟数据）
  loadOrderStats() {
    // 获取当前用户的订单
    const orders = app.getUserOrders()
    const stats = {
      all: orders.length,
      pending: orders.filter(o => o.status === 'pending').length,
      delivering: orders.filter(o => o.status === 'delivering').length,
      completed: orders.filter(o => o.status === 'completed').length
    }
    this.setData({
      orderStats: stats
    })
  },

  // 跳转到登录页
  goToLogin() {
    wx.navigateTo({
      url: '../login/login'
    })
  },

  // 跳转到购物袋
  goToShopbag() {
    wx.switchTab({
      url: '../shopbag/shopbag'
    })
  },

  // 跳转到订单列表
  goToOrders(e) {
    const status = e.currentTarget.dataset.status || 'all'
    wx.navigateTo({
      url: `../orders/orders?status=${status}`
    })
  },

  // 跳转到收货地址
  goToAddress() {
    wx.navigateTo({
      url: '../addressList/addressList'
    })
  },

  // 跳转到优惠券
  goToCoupons() {
    wx.navigateTo({
      url: '../coupons/coupons'
    })
  },

  // 跳转到设置
  goToSettings() {
    wx.showToast({
      title: '设置功能开发中',
      icon: 'none'
    })
  },

  // 退出登录
  logout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('userInfo')
          app.globalData.userInfo = null
          this.setData({
            userInfo: null
          })
          wx.showToast({
            title: '已退出登录',
            icon: 'success'
          })
        }
      }
    })
  }
})