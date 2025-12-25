// pages/favorites/favorites.js
const app = getApp()

Page({
  data: {
    favorites: [], // 收藏的商品列表
    isEmpty: false // 是否为空
  },

  onLoad(options) {
    // 页面加载时获取收藏列表
    this.loadFavorites()
  },

  onShow() {
    // 每次显示页面时刷新收藏列表（因为可能在详情页取消收藏）
    this.loadFavorites()
  },

  // 加载收藏列表
  loadFavorites() {
    // 从app中获取收藏列表
    app.initFavorites()
    const favorites = app.globalData.favorites || []
    
    this.setData({
      favorites: favorites,
      isEmpty: favorites.length === 0
    })
  },

  // 点击商品，跳转到详情页
  onProductTap(e) {
    const pid = e.currentTarget.dataset.pid
    if (!pid) {
      wx.showToast({
        title: '商品信息错误',
        icon: 'none'
      })
      return
    }
    
    wx.navigateTo({
      url: `../detail/detail?pid=${pid}`
    })
  },

  // 删除收藏
  onDeleteFavorite(e) {
    const pid = e.currentTarget.dataset.pid
    const product = this.data.favorites.find(item => item.pid === pid)
    
    if (!pid || !product) {
      wx.showToast({
        title: '商品信息错误',
        icon: 'none'
      })
      return
    }

    wx.showModal({
      title: '提示',
      content: `确定要取消收藏"${product.name}"吗？`,
      success: (res) => {
        if (res.confirm) {
          // 从收藏中移除
          app.removeFromFavorites(pid)
          
          // 刷新列表
          this.loadFavorites()
          
          wx.showToast({
            title: '已取消收藏',
            icon: 'success',
            duration: 1500
          })
        }
      }
    })
  },

  // 清空所有收藏
  onClearAll() {
    if (this.data.favorites.length === 0) {
      return
    }

    wx.showModal({
      title: '提示',
      content: '确定要清空所有收藏吗？',
      success: (res) => {
        if (res.confirm) {
          // 清空收藏列表（使用用户隔离的key）
          const storageKey = app.getUserStorageKey('favorites')
          wx.removeStorageSync(storageKey)
          app.globalData.favorites = []
          
          // 刷新列表
          this.loadFavorites()
          
          wx.showToast({
            title: '已清空收藏',
            icon: 'success',
            duration: 1500
          })
        }
      }
    })
  },

  // 去逛逛（跳转到首页）
  goShopping() {
    wx.switchTab({
      url: '../home/home'
    })
  }
})

