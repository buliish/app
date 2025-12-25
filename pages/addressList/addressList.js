// pages/addressList/addressList.js
Page({
  data: {
    addressList: [],
    selectMode: false, // 是否为选择模式
    orderId: null // 订单ID（从订单详情页跳转时使用）
  },

  onLoad(options) {
    // 如果是从订单详情页跳转过来
    if (options.orderId) {
      this.setData({
        selectMode: true,
        orderId: options.orderId
      })
    } else if (options.select === 'true') {
      // 兼容旧的选择模式
      this.setData({
        selectMode: true
      })
    }
    this.loadAddressList()
  },

  onShow() {
    // 每次显示时刷新地址列表
    this.loadAddressList()
  },

  // 加载地址列表
  loadAddressList() {
    const addresses = wx.getStorageSync('addresses') || []
    // 默认地址排在前面
    const sortedAddresses = addresses.sort((a, b) => {
      if (a.isDefault && !b.isDefault) return -1
      if (!a.isDefault && b.isDefault) return 1
      return b.updateTime - a.updateTime
    })
    
    this.setData({
      addressList: sortedAddresses
    })
  },

  // 选择地址（选择模式下）
  selectAddress(e) {
    if (!this.data.selectMode) return
    
    const id = e.currentTarget.dataset.id
    const address = this.data.addressList.find(addr => addr.id === id)
    
    if (address) {
      // 如果是从订单详情页或付款页跳转过来的，更新订单的收货地址
      if (this.data.orderId) {
        this.updateOrderAddress(this.data.orderId, address)
      } else {
        // 将选中的地址传递回上一页（付款页或订单详情页）
        const pages = getCurrentPages()
        const prevPage = pages[pages.length - 2]
        if (prevPage && prevPage.setSelectedAddress) {
          prevPage.setSelectedAddress(address)
        }
      }
      wx.navigateBack()
    }
  },

  // 更新订单的收货地址
  updateOrderAddress(orderId, address) {
    const app = getApp()
    const updateSuccess = app.updateOrder(orderId, {
      address: {
        name: address.name,
        phone: address.phone,
        regionText: address.regionText,
        detailAddress: address.detailAddress,
        fullAddress: `${address.regionText} ${address.detailAddress}`
      }
    })
    
    if (updateSuccess) {
      
      wx.showToast({
        title: '地址已更新',
        icon: 'success'
      })
    } else {
      wx.showToast({
        title: '订单不存在',
        icon: 'none'
      })
    }
  },

  // 添加地址
  addAddress() {
    wx.navigateTo({
      url: '../newAddress/newAddress'
    })
  },

  // 编辑地址
  editAddress(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `../newAddress/newAddress?id=${id}`
    })
  },

  // 删除地址
  deleteAddress(e) {
    const id = e.currentTarget.dataset.id
    const address = this.data.addressList.find(addr => addr.id === id)
    
    if (!address) return

    wx.showModal({
      title: '提示',
      content: '确定要删除该地址吗？',
      success: (res) => {
        if (res.confirm) {
          let addresses = wx.getStorageSync('addresses') || []
          addresses = addresses.filter(addr => addr.id !== id)
          wx.setStorageSync('addresses', addresses)
          
          this.loadAddressList()
          wx.showToast({
            title: '已删除',
            icon: 'success'
          })
        }
      }
    })
  }
})

