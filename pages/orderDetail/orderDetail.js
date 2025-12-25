// pages/orderDetail/orderDetail.js
Page({
  data: {
    order: null,
    orderId: null
  },

  onLoad(options) {
    if (options.id) {
      this.setData({
        orderId: options.id
      })
      this.loadOrderDetail(options.id)
    }
  },

  onShow() {
    // 从地址列表返回时，如果选择了新地址，需要刷新订单详情
    if (this.data.orderId) {
      this.loadOrderDetail(this.data.orderId)
    }
  },

  // 加载订单详情
  loadOrderDetail(id) {
    const app = getApp()
    const orders = app.getUserOrders()
    const order = orders.find(o => o.id === id)
    
    if (order) {
      const statusMap = {
        'pending': '待付款',
        'delivering': '配送中',
        'completed': '已完成',
        'cancelled': '已取消'
      }
      
      this.setData({
        order: {
          ...order,
          statusText: statusMap[order.status] || '未知',
          createTime: this.formatTime(order.createTime),
          updateTime: order.updateTime ? this.formatTime(order.updateTime) : ''
        }
      })
    } else {
      wx.showToast({
        title: '订单不存在',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }
  },

  // 格式化时间
  formatTime(timestamp) {
    if (!timestamp) return ''
    const date = new Date(timestamp)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hour = String(date.getHours()).padStart(2, '0')
    const minute = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}`
  },

  // 支付订单
  payOrder() {
    // 跳转到付款页面
    wx.navigateTo({
      url: `../payment/payment?orderId=${this.data.orderId}`
    })
  },

  // 取消订单
  cancelOrder() {
    const app = getApp()
    wx.showModal({
      title: '提示',
      content: '确定要取消该订单吗？',
      success: (res) => {
        if (res.confirm) {
          if (app.updateOrder(this.data.order.id, { status: 'cancelled' })) {
            this.loadOrderDetail(this.data.order.id)
            wx.showToast({
              title: '已取消',
              icon: 'success'
            })
          } else {
            wx.showToast({
              title: '订单不存在',
              icon: 'none'
            })
          }
        }
      }
    })
  },

  // 确认收货
  confirmReceive() {
    const app = getApp()
    wx.showModal({
      title: '确认收货',
      content: '确定已收到商品吗？',
      success: (res) => {
        if (res.confirm) {
          if (app.updateOrder(this.data.order.id, { status: 'completed' })) {
            this.loadOrderDetail(this.data.order.id)
            wx.showToast({
              title: '确认收货成功',
              icon: 'success'
            })
          } else {
            wx.showToast({
              title: '订单不存在',
              icon: 'none'
            })
          }
        }
      }
    })
  },

  // 修改收货地址
  changeAddress() {
    wx.navigateTo({
      url: `../addressList/addressList?orderId=${this.data.orderId}&from=orderDetail`
    })
  }
})

