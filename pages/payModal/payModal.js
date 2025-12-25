// pages/payModal/payModal.js
const app = getApp()

Page({
  data: {
    // 支付来源：'cart' 从购物车结算，'order' 从订单支付
    source: 'cart',
    // 订单ID（从订单支付时使用）
    orderId: null,
    // 商品列表
    products: [],
    // 收货地址
    address: null,
    // 总价
    totalPrice: '0.00',
    // 订单号（从订单支付时显示）
    orderNo: '',
    // 支付密码（模拟）
    payPassword: '',
    // 显示密码点
    showPassword: ['', '', '', '', '', ''],
    // 当前输入位置
    currentIndex: 0
  },

  onLoad(options) {
    // 从全局数据获取支付信息
    const paymentData = app.globalData.paymentData || {}
    
    this.setData({
      source: paymentData.source || 'cart',
      orderId: paymentData.orderId || null,
      products: paymentData.products || [],
      address: paymentData.address || null,
      totalPrice: paymentData.totalPrice || '0.00',
      orderNo: paymentData.orderNo || ''
    })
  },

  // 输入密码
  onPasswordInput(e) {
    const value = e.detail.value
    const length = value.length
    
    if (length > 6) return
    
    const showPassword = []
    for (let i = 0; i < 6; i++) {
      showPassword.push(i < length ? '●' : '')
    }
    
    this.setData({
      payPassword: value,
      showPassword: showPassword,
      currentIndex: length
    })
    
    // 如果输入了6位密码，自动确认支付
    if (length === 6) {
      setTimeout(() => {
        this.confirmPay()
      }, 300)
    }
  },

  // 删除密码
  deletePassword() {
    const password = this.data.payPassword
    if (password.length > 0) {
      const newPassword = password.slice(0, -1)
      const length = newPassword.length
      const showPassword = []
      for (let i = 0; i < 6; i++) {
        showPassword.push(i < length ? '●' : '')
      }
      
      this.setData({
        payPassword: newPassword,
        showPassword: showPassword,
        currentIndex: length
      })
    }
  },

  // 确认支付
  confirmPay() {
    if (this.data.payPassword.length < 6) {
      wx.showToast({
        title: '请输入6位支付密码',
        icon: 'none'
      })
      return
    }
    
    if (this.data.source === 'cart') {
      // 从购物车结算，创建订单并支付
      this.createOrderAndPay()
    } else {
      // 从订单支付，直接更新订单状态
      this.payOrder()
    }
  },

  // 创建订单并支付
  createOrderAndPay() {
    // 确保app可用
    const appInstance = app || getApp()
    if (!appInstance) {
      wx.showToast({
        title: '系统错误，请重试',
        icon: 'none'
      })
      return
    }

    const products = this.data.products
    const address = this.data.address

    const orderNo = `ORD${Date.now()}${Math.floor(Math.random() * 1000)}`
    
    const paymentData = appInstance.globalData.paymentData || {}
    const order = {
      id: `order_${Date.now()}`,
      orderNo: orderNo,
      userId: appInstance.getCurrentUserId(), // 添加用户ID
      products: products.map(item => ({
        pid: item.pid,
        name: item.name,
        enname: item.enname,
        price: item.price,
        count: item.count,
        smallImg: item.smallImg
      })),
      totalPrice: this.data.totalPrice,
      originalPrice: paymentData.originalPrice || this.data.totalPrice,
      status: 'delivering', // 支付成功，状态为配送中
      address: {
        name: address.name,
        phone: address.phone,
        regionText: address.regionText,
        detailAddress: address.detailAddress,
        fullAddress: `${address.regionText} ${address.detailAddress}`
      },
      coupon: paymentData.coupon || null,
      discountAmount: paymentData.discountAmount || '0.00',
      createTime: Date.now(),
      updateTime: Date.now()
    }
    
    appInstance.saveOrder(order)

    // 如果使用了优惠券，标记为已使用
    if (paymentData.coupon) {
      const couponsKey = appInstance.getUserStorageKey('coupons')
      let coupons = wx.getStorageSync(couponsKey) || []
      const couponIndex = coupons.findIndex(c => c.id === paymentData.coupon.id)
      if (couponIndex > -1) {
        coupons[couponIndex].status = 'used'
        wx.setStorageSync(couponsKey, coupons)
      }
    }

    // 从购物车中移除已结算的商品
    const selectedItemsKey = appInstance.getUserStorageKey('selectedCartItems')
    const selectedPids = wx.getStorageSync(selectedItemsKey) || []
    selectedPids.forEach(pid => {
      appInstance.removeFromCart(pid)
    })
    wx.removeStorageSync(selectedItemsKey)

    // 清除全局支付数据
    appInstance.globalData.paymentData = null

    wx.showToast({
      title: '支付成功',
      icon: 'success'
    })

    setTimeout(() => {
      // 使用 reLaunch 清除所有页面栈，避免返回时回到支付页面
      wx.reLaunch({
        url: '../orders/orders?status=delivering'
      })
    }, 1500)
  },

  // 支付订单
  payOrder() {
    // 确保app可用
    const appInstance = app || getApp()
    if (!appInstance) {
      wx.showToast({
        title: '系统错误，请重试',
        icon: 'none'
      })
      return
    }

    const orderId = this.data.orderId
    
    // 获取订单信息
    const orders = appInstance.getUserOrders()
    const order = orders.find(o => o.id === orderId)
    
    if (order) {
      const updates = {
        status: 'delivering'
      }
      
      // 如果地址有更新，同步更新订单地址
      if (this.data.address) {
        updates.address = {
          name: this.data.address.name,
          phone: this.data.address.phone,
          regionText: this.data.address.regionText,
          detailAddress: this.data.address.detailAddress,
          fullAddress: `${this.data.address.regionText} ${this.data.address.detailAddress}`
        }
      }
      
      appInstance.updateOrder(orderId, updates)
      
      // 清除全局支付数据
      appInstance.globalData.paymentData = null
      
      wx.showToast({
        title: '支付成功',
        icon: 'success'
      })

      setTimeout(() => {
        // 使用 reLaunch 清除所有页面栈，避免返回时回到支付页面
        wx.reLaunch({
          url: '../orders/orders?status=delivering'
        })
      }, 1500)
    }
  },

  // 取消支付（退出支付页面）
  cancelPay() {
    // 确保app可用
    const appInstance = app || getApp()
    
    if (this.data.source === 'cart') {
      // 从购物车结算，创建待付款订单
      this.createPendingOrder()
    }
    
    // 清除全局支付数据
    if (appInstance) {
      appInstance.globalData.paymentData = null
    }
    
    // 返回上一页
    wx.navigateBack()
  },

  // 创建待付款订单
  createPendingOrder() {
    // 确保app可用
    const appInstance = app || getApp()
    if (!appInstance) {
      wx.showToast({
        title: '系统错误，请重试',
        icon: 'none'
      })
      return
    }

    const products = this.data.products
    const address = this.data.address

    if (!address) {
      wx.showToast({
        title: '请先选择收货地址',
        icon: 'none'
      })
      return
    }

    const orders = wx.getStorageSync('orders') || []
    const orderNo = `ORD${Date.now()}${Math.floor(Math.random() * 1000)}`
    
    const paymentData = appInstance.globalData.paymentData || {}
    const order = {
      id: `order_${Date.now()}`,
      orderNo: orderNo,
      userId: appInstance.getCurrentUserId(), // 添加用户ID
      products: products.map(item => ({
        pid: item.pid,
        name: item.name,
        enname: item.enname,
        price: item.price,
        count: item.count,
        smallImg: item.smallImg
      })),
      totalPrice: this.data.totalPrice,
      originalPrice: paymentData.originalPrice || this.data.totalPrice,
      status: 'pending', // 待付款
      address: {
        name: address.name,
        phone: address.phone,
        regionText: address.regionText,
        detailAddress: address.detailAddress,
        fullAddress: `${address.regionText} ${address.detailAddress}`
      },
      coupon: paymentData.coupon || null,
      discountAmount: paymentData.discountAmount || '0.00',
      createTime: Date.now(),
      updateTime: Date.now()
    }
    
    appInstance.saveOrder(order)

    // 从购物车中移除已结算的商品
    const selectedItemsKey = appInstance.getUserStorageKey('selectedCartItems')
    const selectedPids = wx.getStorageSync(selectedItemsKey) || []
    selectedPids.forEach(pid => {
      appInstance.removeFromCart(pid)
    })
    wx.removeStorageSync(selectedItemsKey)

    // 清除全局支付数据
    appInstance.globalData.paymentData = null

    wx.showToast({
      title: '订单已创建',
      icon: 'success'
    })
  },

  // 阻止事件冒泡
  stopPropagation() {
    // 空函数，用于阻止点击事件冒泡
  }
})

