// pages/payment/payment.js
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
    // 原价（未使用优惠券前的价格）
    originalPrice: '0.00',
    // 订单号（从订单支付时显示）
    orderNo: '',
    // 选中的优惠券
    selectedCoupon: null,
    // 优惠金额
    discountAmount: '0.00'
  },

  onLoad(options) {
    if (options.orderId) {
      // 从订单支付进入
      this.setData({
        source: 'order',
        orderId: options.orderId
      })
      this.loadOrderData(options.orderId)
    } else {
      // 从购物车结算进入
      this.setData({
        source: 'cart'
      })
      this.loadCartData()
    }
  },

  onShow() {
    // 从地址列表返回时，刷新地址信息
    if (this.data.source === 'cart') {
      this.loadCartData()
    } else if (this.data.orderId) {
      this.loadOrderData(this.data.orderId)
    }
  },

  // 加载购物车数据
  loadCartData() {
    const cart = app.globalData.cart || []
    const selectedPids = wx.getStorageSync('selectedCartItems') || []
    
    // 获取选中的商品
    const selectedProducts = cart.filter(item => 
      selectedPids.includes(String(item.pid))
    )
    
    if (selectedProducts.length === 0) {
      wx.showToast({
        title: '请选择要结算的商品',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
      return
    }

    // 计算原价
    const originalPrice = selectedProducts
      .reduce((total, item) => total + (item.price * item.count), 0)
      .toFixed(2)

    // 获取收货地址
    const addresses = wx.getStorageSync('addresses') || []
    const defaultAddress = addresses.find(addr => addr.isDefault) || addresses[0]

    // 计算优惠后的价格
    const { totalPrice, discountAmount } = this.calculatePriceWithCoupon(parseFloat(originalPrice))

    this.setData({
      products: selectedProducts,
      originalPrice: originalPrice,
      totalPrice: totalPrice,
      discountAmount: discountAmount,
      address: defaultAddress
    })
  },

  // 加载订单数据
  loadOrderData(orderId) {
    const orders = app.getUserOrders()
    const order = orders.find(o => o.id === orderId)
    
    if (!order) {
      wx.showToast({
        title: '订单不存在',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
      return
    }

    // 如果订单有原价，使用原价；否则使用总价作为原价
    const originalPrice = order.originalPrice || order.totalPrice || '0.00'
    // 如果订单已有优惠券，使用订单的优惠券；否则使用当前选中的优惠券
    const coupon = order.coupon || this.data.selectedCoupon
    const { totalPrice, discountAmount } = this.calculatePriceWithCoupon(parseFloat(originalPrice), coupon)

    this.setData({
      products: order.products || [],
      originalPrice: originalPrice,
      totalPrice: totalPrice,
      discountAmount: discountAmount,
      address: order.address,
      orderNo: order.orderNo || '',
      selectedCoupon: coupon
    })
  },

  // 修改收货地址
  changeAddress() {
    if (this.data.source === 'cart') {
      // 从购物车结算，直接跳转到地址列表
      wx.navigateTo({
        url: '../addressList/addressList?select=true'
      })
    } else {
      // 从订单支付，跳转到地址列表并传递订单ID
      wx.navigateTo({
        url: `../addressList/addressList?orderId=${this.data.orderId}`
      })
    }
  },

  // 确认支付
  confirmPay() {
    // 检查是否有收货地址
    if (!this.data.address) {
      wx.showModal({
        title: '提示',
        content: '请先选择收货地址',
        confirmText: '去选择',
        success: (res) => {
          if (res.confirm) {
            this.changeAddress()
          }
        }
      })
      return
    }

    // 将支付数据保存到全局，供支付弹窗使用
    app.globalData.paymentData = {
      source: this.data.source,
      orderId: this.data.orderId,
      products: this.data.products,
      address: this.data.address,
      totalPrice: this.data.totalPrice,
      originalPrice: this.data.originalPrice,
      orderNo: this.data.orderNo,
      coupon: this.data.selectedCoupon,
      discountAmount: this.data.discountAmount
    }

    // 跳转到支付弹窗页面
    wx.navigateTo({
      url: '../payModal/payModal'
    })
  },


  // 设置选中的地址（从地址列表返回时调用）
  setSelectedAddress(address) {
    this.setData({
      address: address
    })
  },

  // 选择优惠券
  selectCoupon() {
    wx.navigateTo({
      url: `../selectCoupon/selectCoupon?totalPrice=${this.data.originalPrice}&selectedCouponId=${this.data.selectedCoupon ? this.data.selectedCoupon.id : ''}`
    })
  },

  // 设置选中的优惠券（从优惠券选择页返回时调用）
  setSelectedCoupon(coupon) {
    const originalPrice = parseFloat(this.data.originalPrice)
    const { totalPrice, discountAmount } = this.calculatePriceWithCoupon(originalPrice, coupon)
    
    this.setData({
      selectedCoupon: coupon,
      totalPrice: totalPrice,
      discountAmount: discountAmount
    })
  },

  // 计算优惠后的价格
  calculatePriceWithCoupon(originalPrice, coupon = null) {
    const selectedCoupon = coupon || this.data.selectedCoupon
    let discountAmount = 0
    
    if (selectedCoupon) {
      // 检查优惠券是否可用
      if (selectedCoupon.type === 'free' || originalPrice >= selectedCoupon.fullAmount) {
        discountAmount = selectedCoupon.amount
      }
    }
    
    const totalPrice = Math.max(0, originalPrice - discountAmount).toFixed(2)
    discountAmount = discountAmount.toFixed(2)
    
    return { totalPrice, discountAmount }
  }
})

