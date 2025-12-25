// pages/orders/orders.js
Page({
  data: {
    currentStatus: 'all',
    statusTabs: [
      { label: '全部', value: 'all' },
      { label: '待付款', value: 'pending' },
      { label: '配送中', value: 'delivering' },
      { label: '已完成', value: 'completed' }
    ],
    orderList: []
  },

  onLoad(options) {
    const status = options.status || 'all'
    this.setData({
      currentStatus: status
    })
    this.loadOrders()
  },

  onShow() {
    // 每次显示时刷新订单列表
    this.loadOrders()
  },

  // 加载订单列表
  loadOrders() {
    const app = getApp()
    // 获取当前用户的订单
    let orders = app.getUserOrders()
    
    // 根据状态筛选
    if (this.data.currentStatus !== 'all') {
      orders = orders.filter(order => order.status === this.data.currentStatus)
    }

    // 格式化订单数据
    const formattedOrders = orders.map(order => {
      const statusMap = {
        'pending': '待付款',
        'delivering': '配送中',
        'completed': '已完成',
        'cancelled': '已取消'
      }
      
      return {
        ...order,
        statusText: statusMap[order.status] || '未知',
        createTime: this.formatTime(order.createTime)
      }
    })

    // 按时间倒序排列
    formattedOrders.sort((a, b) => b.createTime - a.createTime)

    this.setData({
      orderList: formattedOrders
    })
  },

  // 切换订单状态
  switchStatus(e) {
    const status = e.currentTarget.dataset.status
    this.setData({
      currentStatus: status
    })
    this.loadOrders()
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
  payOrder(e) {
    const id = e.currentTarget.dataset.id
    // 跳转到付款页面
    wx.navigateTo({
      url: `../payment/payment?orderId=${id}`
    })
  },

  // 取消订单
  cancelOrder(e) {
    const id = e.currentTarget.dataset.id
    const app = getApp()
    wx.showModal({
      title: '提示',
      content: '确定要取消该订单吗？',
      success: (res) => {
        if (res.confirm) {
          if (app.updateOrder(id, { status: 'cancelled' })) {
            this.loadOrders()
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

  // 查看订单详情
  viewOrderDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `../orderDetail/orderDetail?id=${id}`
    })
  },

  // 确认收货
  confirmReceive(e) {
    const id = e.currentTarget.dataset.id
    const app = getApp()
    wx.showModal({
      title: '确认收货',
      content: '确定已收到商品吗？',
      success: (res) => {
        if (res.confirm) {
          if (app.updateOrder(id, { status: 'completed' })) {
            this.loadOrders()
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

  // 修改订单地址
  changeOrderAddress(e) {
    const orderId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `../addressList/addressList?orderId=${orderId}`
    })
  }
})

