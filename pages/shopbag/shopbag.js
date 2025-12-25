// pages/shopbag/shopbag.js
const app = getApp()

Page({
  data: {
    cartList: [],
    totalPrice: '0.00',
    isEmpty: true,
    selectedItems: [] // 选中的商品pid列表
  },

  onLoad(options) {
    this.loadCart()
  },

  onShow() {
    // 每次显示页面时刷新购物车数据
    this.loadCart()
  },

  // 加载购物车数据
  loadCart() {
    const cart = app.globalData.cart || []
    
    // 获取当前购物车所有商品的pid（统一转为字符串）
    const cartPids = cart.map(item => String(item.pid))
    
    // 过滤selectedItems，只保留购物车中存在的商品（统一转为字符串）
    // 新添加的商品默认不选中
    let selectedItems = this.data.selectedItems
      .map(pid => String(pid))
      .filter(pid => cartPids.includes(pid))
    
    // 为每个商品添加checked属性，方便模板判断
    const cartWithChecked = cart.map(item => {
      const pidStr = String(item.pid)
      return {
        ...item,
        checked: selectedItems.includes(pidStr)
      }
    })
    
    // 判断是否全选
    const isAllSelected = cart.length > 0 && 
      cartPids.every(pid => selectedItems.includes(pid))
    
    // 计算选中商品的总价
    const totalPrice = this.calculateSelectedPrice(cart, selectedItems)
    
    console.log('loadCart - cart:', cart)
    console.log('loadCart - selectedItems:', selectedItems)
    console.log('loadCart - isAllSelected:', isAllSelected)
    
    this.setData({
      cartList: cartWithChecked,
      totalPrice: totalPrice,
      isEmpty: cart.length === 0,
      selectedItems: selectedItems,
      isAllSelected: isAllSelected
    })
  },

  // 计算选中商品的总价
  calculateSelectedPrice(cart, selectedItems) {
    const selectedItemsStr = selectedItems.map(id => String(id))
    return cart
      .filter(item => selectedItemsStr.includes(String(item.pid)))
      .reduce((total, item) => total + (item.price * item.count), 0)
      .toFixed(2)
  },

  // 增加商品数量
  addCount(e) {
    const pid = e.currentTarget.dataset.pid
    const item = this.data.cartList.find(item => item.pid === pid)
    if (item) {
      app.updateCartItem(pid, item.count + 1)
      // 重新加载购物车数据，保持勾选状态
      this.loadCart()
    }
  },

  // 减少商品数量
  reduceCount(e) {
    const pid = e.currentTarget.dataset.pid
    const item = this.data.cartList.find(item => item.pid === pid)
    if (item) {
      if (item.count > 1) {
        // 数量大于1，减少数量
        app.updateCartItem(pid, item.count - 1)
      } else {
        // 数量为1，减少后为0，从购物车移除
        app.removeFromCart(pid)
        // 从选中列表中移除
        const selectedItems = this.data.selectedItems.filter(id => String(id) !== String(pid))
        this.setData({
          selectedItems: selectedItems
        })
        wx.showToast({
          title: '已移除',
          icon: 'success',
          duration: 1500
        })
      }
      // 重新加载购物车数据，保持勾选状态
      this.loadCart()
    }
  },

  // 删除商品
  deleteItem(e) {
    const pid = e.currentTarget.dataset.pid
    wx.showModal({
      title: '提示',
      content: '确定要删除该商品吗？',
      success: (res) => {
        if (res.confirm) {
          app.removeFromCart(pid)
          // 从选中列表中移除
          const selectedItems = this.data.selectedItems.filter(id => id !== pid)
          this.setData({
            selectedItems: selectedItems
          })
          this.loadCart()
          wx.showToast({
            title: '已删除',
            icon: 'success'
          })
        }
      }
    })
  },

  // 跳转到商品详情
  goToDetail(e) {
    const pid = e.currentTarget.dataset.pid
    wx.navigateTo({
      url: `../detail/detail?pid=${pid}`
    })
  },

  // 切换商品选中状态
  toggleSelect(e) {
    const pid = String(e.currentTarget.dataset.pid)
    if (!pid) {
      console.error('toggleSelect: pid is empty')
      return
    }
    
    // 获取最新的购物车数据
    const cart = app.globalData.cart || []
    const selectedItems = this.data.selectedItems.map(id => String(id))
    const index = selectedItems.indexOf(pid)
    
    if (index > -1) {
      // 取消选中
      selectedItems.splice(index, 1)
    } else {
      // 选中
      selectedItems.push(pid)
    }
    
    // 更新商品列表的checked状态（使用最新的购物车数据）
    const cartWithChecked = cart.map(item => {
      const pidStr = String(item.pid)
      return {
        ...item,
        checked: selectedItems.includes(pidStr)
      }
    })
    
    // 判断是否全选
    const cartPids = cartWithChecked.map(item => String(item.pid))
    const isAllSelected = cartWithChecked.length > 0 && 
      cartPids.every(pid => selectedItems.includes(pid))
    
    // 计算选中商品的总价
    const totalPrice = this.calculateSelectedPrice(cart, selectedItems)
    
    this.setData({
      selectedItems: selectedItems,
      totalPrice: totalPrice,
      cartList: cartWithChecked,
      isAllSelected: isAllSelected
    })
    
    console.log('toggleSelect:', pid, 'selectedItems:', selectedItems, 'cartList length:', cartWithChecked.length)
  },


  // 全选/取消全选
  toggleSelectAll() {
    const cart = app.globalData.cart || []
    const selectedItems = this.data.selectedItems.map(id => String(id))
    
    // 判断是否全选（统一转为字符串比较）
    const cartPids = cart.map(item => String(item.pid))
    const isAllSelected = cart.length > 0 && 
      cartPids.every(pid => selectedItems.includes(pid))
    
    let newSelectedItems = []
    let newTotalPrice = '0.00'
    
    if (isAllSelected) {
      // 取消全选
      newSelectedItems = []
      newTotalPrice = '0.00'
    } else {
      // 全选
      newSelectedItems = cartPids
      newTotalPrice = this.calculateSelectedPrice(cart, cartPids)
    }
    
    // 更新商品列表的checked状态
    const cartWithChecked = cart.map(item => {
      const pidStr = String(item.pid)
      return {
        ...item,
        checked: newSelectedItems.includes(pidStr)
      }
    })
    
    // 更新全选状态
    const newIsAllSelected = cart.length > 0 && newSelectedItems.length === cart.length
    
    this.setData({
      selectedItems: newSelectedItems,
      totalPrice: newTotalPrice,
      cartList: cartWithChecked,
      isAllSelected: newIsAllSelected
    })
    
    console.log('toggleSelectAll:', isAllSelected ? '取消全选' : '全选', 'newSelectedItems:', newSelectedItems)
  },

  // 结算
  checkout() {
    // 检查是否登录
    const userInfo = wx.getStorageSync('userInfo') || app.globalData.userInfo
    if (!userInfo) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再进行结算操作',
        confirmText: '去登录',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({
              url: '../login/login'
            })
          }
        }
      })
      return
    }

    const selectedItems = this.data.selectedItems
    const selectedCart = this.data.cartList.filter(item => selectedItems.includes(String(item.pid)))
    
    if (selectedCart.length === 0) {
      wx.showToast({
        title: '请选择要结算的商品',
        icon: 'none'
      })
      return
    }

    // 保存选中的商品ID到本地存储，供付款页面使用
    const app = getApp()
    const selectedItemsKey = app.getUserStorageKey('selectedCartItems')
    wx.setStorageSync(selectedItemsKey, selectedItems.map(pid => String(pid)))
    
    // 跳转到付款页面
    wx.navigateTo({
      url: '../payment/payment'
    })
  },

  // 创建订单
  createOrder(products, address) {
    const orders = wx.getStorageSync('orders') || []
    const orderNo = `ORD${Date.now()}${Math.floor(Math.random() * 1000)}`
    
    const order = {
      id: `order_${Date.now()}`,
      orderNo: orderNo,
      products: products.map(item => ({
        pid: item.pid,
        name: item.name,
        enname: item.enname,
        price: item.price,
        count: item.count,
        smallImg: item.smallImg
      })),
      totalPrice: this.data.totalPrice,
      status: 'pending',
      address: {
        name: address.name,
        phone: address.phone,
        regionText: address.regionText,
        detailAddress: address.detailAddress,
        fullAddress: `${address.regionText} ${address.detailAddress}`
      },
      createTime: Date.now(),
      updateTime: Date.now()
    }
    
    orders.unshift(order)
    wx.setStorageSync('orders', orders)
  },

  // 去逛逛
  goShopping() {
    wx.switchTab({
      url: '../home/home'
    })
  },

  // 清空购物车
  clearCart() {
    if (this.data.cartList.length === 0) {
      wx.showToast({
        title: '购物袋已为空',
        icon: 'none'
      })
      return
    }

    wx.showModal({
      title: '提示',
      content: '确定要清空购物车吗？',
      success: (res) => {
        if (res.confirm) {
          app.clearCart()
          this.setData({
            selectedItems: []
          })
          this.loadCart()
          wx.showToast({
            title: '已清空',
            icon: 'success'
          })
        }
      }
    })
  }
})