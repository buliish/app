// pages/newAddress/newAddress.js
Page({
  data: {
    name: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    regionArray: [],
    regionText: '',
    detailAddress: '',
    isDefault: false,
    addressId: null // 编辑时使用
  },

  onLoad(options) {
    // 如果是编辑地址，从options获取地址ID并加载数据
    if (options.id) {
      this.loadAddressData(options.id)
    }
  },

  // 加载地址数据（编辑时使用）
  loadAddressData(id) {
    // 从本地存储或全局数据中获取地址信息
    const addresses = wx.getStorageSync('addresses') || []
    const address = addresses.find(addr => addr.id === id)
    
    if (address) {
      // 解析regionText为数组
      let regionArray = []
      if (address.regionText) {
        regionArray = address.regionText.split(' ')
      }
      
      this.setData({
        name: address.name || '',
        phone: address.phone || '',
        province: address.province || '',
        city: address.city || '',
        district: address.district || '',
        regionArray: regionArray,
        regionText: address.regionText || '',
        detailAddress: address.detailAddress || '',
        isDefault: address.isDefault || false,
        addressId: id
      })
    }
  },

  // 收货人姓名输入
  onNameInput(e) {
    this.setData({
      name: e.detail.value
    })
  },

  // 手机号输入
  onPhoneInput(e) {
    this.setData({
      phone: e.detail.value
    })
  },

  // 选择所在地区
  onRegionChange(e) {
    const regionArray = e.detail.value
    const regionText = regionArray.join(' ')
    
    this.setData({
      regionArray: regionArray,
      regionText: regionText,
      province: regionArray[0] || '',
      city: regionArray[1] || '',
      district: regionArray[2] || ''
    })
  },

  // 详细地址输入
  onDetailAddressInput(e) {
    this.setData({
      detailAddress: e.detail.value
    })
  },

  // 默认地址开关变化
  onSwitchChange(e) {
    this.setData({
      isDefault: e.detail.value
    })
  },

  // 保存地址
  saveAddress() {
    const { name, phone, regionText, detailAddress, isDefault, addressId } = this.data

    // 验证表单
    if (!name || !name.trim()) {
      wx.showToast({
        title: '请输入收货人姓名',
        icon: 'none'
      })
      return
    }

    if (!phone || !phone.trim()) {
      wx.showToast({
        title: '请输入手机号',
        icon: 'none'
      })
      return
    }

    // 验证手机号格式
    const phoneReg = /^1[3-9]\d{9}$/
    if (!phoneReg.test(phone)) {
      wx.showToast({
        title: '手机号格式不正确',
        icon: 'none'
      })
      return
    }

    if (!regionText || !regionText.trim()) {
      wx.showToast({
        title: '请选择所在地区',
        icon: 'none'
      })
      return
    }

    if (!detailAddress || !detailAddress.trim()) {
      wx.showToast({
        title: '请输入详细地址',
        icon: 'none'
      })
      return
    }

    // 获取地址列表
    let addresses = wx.getStorageSync('addresses') || []

    // 如果设为默认，取消其他地址的默认状态
    if (isDefault) {
      addresses = addresses.map(addr => ({
        ...addr,
        isDefault: false
      }))
    }

    const addressData = {
      id: addressId || `addr_${Date.now()}`,
      name: name.trim(),
      phone: phone.trim(),
      regionText: regionText.trim(),
      detailAddress: detailAddress.trim(),
      isDefault: isDefault,
      createTime: addressId ? addresses.find(a => a.id === addressId)?.createTime : Date.now(),
      updateTime: Date.now()
    }

    if (addressId) {
      // 编辑地址
      const index = addresses.findIndex(addr => addr.id === addressId)
      if (index > -1) {
        addresses[index] = addressData
      }
      wx.showToast({
        title: '修改成功',
        icon: 'success'
      })
    } else {
      // 新增地址
      addresses.push(addressData)
      wx.showToast({
        title: '保存成功',
        icon: 'success'
      })
    }

    // 保存到本地存储
    wx.setStorageSync('addresses', addresses)

    // 延迟返回上一页
    setTimeout(() => {
      wx.navigateBack()
    }, 1500)
  }
})

