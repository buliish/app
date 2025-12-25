// app.js
App({
  onLaunch() {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 登录
    wx.login({
      success: res => {
        // 发送 res.code 到后台换取 openId, sessionKey, unionId
      }
    })

    // 初始化购物车数据
    this.initCart()
    // 初始化收藏列表
    this.initFavorites()
  },
  globalData: {
    userInfo: null,
    cart: [], // 购物车数据
    favorites: [], // 收藏列表
    paymentData: null // 支付数据（临时存储）
  },

  // 初始化购物车（从本地存储读取）
  initCart() {
    const cart = wx.getStorageSync('cart') || []
    this.globalData.cart = cart
  },

  // 添加商品到购物车
  addToCart(product, count = 1) {
    const cart = this.globalData.cart
    const existingIndex = cart.findIndex(item => item.pid === product.pid)
    
    if (existingIndex > -1) {
      // 如果商品已存在，增加数量
      cart[existingIndex].count += count
    } else {
      // 如果商品不存在，添加新商品
      cart.push({
        pid: product.pid,
        name: product.name,
        enname: product.enname,
        price: parseFloat(product.price) || 0,
        smallImg: product.smallImg || product.largeImg,
        flag: product.flag,
        count: count
      })
    }
    
    this.saveCart()
    return cart
  },

  // 更新购物车商品数量
  updateCartItem(pid, count) {
    const cart = this.globalData.cart
    const item = cart.find(item => item.pid === pid)
    if (item) {
      item.count = count
      if (item.count <= 0) {
        // 如果数量为0，移除商品
        this.removeFromCart(pid)
        return
      }
      this.saveCart()
    }
  },

  // 从购物车移除商品
  removeFromCart(pid) {
    const cart = this.globalData.cart
    const index = cart.findIndex(item => item.pid === pid)
    if (index > -1) {
      cart.splice(index, 1)
      this.saveCart()
    }
  },

  // 清空购物车
  clearCart() {
    this.globalData.cart = []
    this.saveCart()
  },

  // 保存购物车到本地存储
  saveCart() {
    wx.setStorageSync('cart', this.globalData.cart)
  },

  // 获取购物车总数量
  getCartTotalCount() {
    return this.globalData.cart.reduce((total, item) => total + item.count, 0)
  },

  // 获取购物车总价
  getCartTotalPrice() {
    return this.globalData.cart.reduce((total, item) => total + (item.price * item.count), 0).toFixed(2)
  },

  // 初始化收藏列表（从本地存储读取）
  initFavorites() {
    const favorites = wx.getStorageSync('favorites') || []
    this.globalData.favorites = favorites
  },

  // 添加商品到收藏
  addToFavorites(product) {
    const favorites = this.globalData.favorites
    const existingIndex = favorites.findIndex(item => item.pid === product.pid)
    
    if (existingIndex > -1) {
      // 如果商品已收藏，取消收藏
      favorites.splice(existingIndex, 1)
      this.saveFavorites()
      return false
    } else {
      // 如果商品未收藏，添加收藏
      favorites.push({
        pid: product.pid,
        name: product.name,
        enname: product.enname,
        price: parseFloat(product.price) || 0,
        smallImg: product.smallImg || product.largeImg,
        flag: product.flag,
        addTime: Date.now()
      })
      this.saveFavorites()
      return true
    }
  },

  // 从收藏移除商品
  removeFromFavorites(pid) {
    const favorites = this.globalData.favorites
    const index = favorites.findIndex(item => item.pid === pid)
    if (index > -1) {
      favorites.splice(index, 1)
      this.saveFavorites()
    }
  },

  // 检查商品是否已收藏
  isFavorite(pid) {
    return this.globalData.favorites.some(item => item.pid === pid)
  },

  // 保存收藏列表到本地存储
  saveFavorites() {
    wx.setStorageSync('favorites', this.globalData.favorites)
  },

  // 获取当前用户ID
  getCurrentUserId() {
    const userInfo = this.globalData.userInfo || wx.getStorageSync('userInfo')
    return userInfo ? (userInfo.phone || userInfo.userId || '') : ''
  },

  // 获取用户订单（根据用户ID筛选）
  getUserOrders() {
    const userId = this.getCurrentUserId()
    if (!userId) return []
    
    const allOrders = wx.getStorageSync('orders') || []
    return allOrders.filter(order => order.userId === userId)
  },

  // 保存订单（自动添加用户ID）
  saveOrder(order) {
    const userId = this.getCurrentUserId()
    if (!userId) {
      console.error('用户未登录，无法保存订单')
      return false
    }

    // 确保订单有用户ID
    if (!order.userId) {
      order.userId = userId
    }

    const allOrders = wx.getStorageSync('orders') || []
    const index = allOrders.findIndex(o => o.id === order.id)
    
    if (index > -1) {
      // 更新订单
      allOrders[index] = order
    } else {
      // 新增订单
      allOrders.unshift(order)
    }
    
    wx.setStorageSync('orders', allOrders)
    return true
  },

  // 更新订单（只更新当前用户的订单）
  updateOrder(orderId, updates) {
    const userId = this.getCurrentUserId()
    if (!userId) return false

    const allOrders = wx.getStorageSync('orders') || []
    const index = allOrders.findIndex(order => order.id === orderId && order.userId === userId)
    
    if (index > -1) {
      allOrders[index] = {
        ...allOrders[index],
        ...updates,
        updateTime: Date.now()
      }
      wx.setStorageSync('orders', allOrders)
      return true
    }
    return false
  }
})
