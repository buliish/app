import { getDetail } from '../../api/api';

Page({
  data: {
    count: 1,
    pid: '',
    detailData: {},
    isLoading: true,
    errorMsg: '',
    isFavorite: false
  },

  async onLoad(options) {
    console.log('详情页接收参数：', options);
    // 1. 校验pid是否传递
    if (!options?.pid) {
      this.setData({
        isLoading: false,
        errorMsg: '未获取到商品ID'
      });
      return;
    }

    try {
      // 2. 请求商品详情
      const data = await getDetail(options.pid);
      console.log('接口返回完整数据：', data);

      // 3. 校验接口返回的核心数据
      const result = data.data?.result?.[0] || {};
      if (JSON.stringify(result) === '{}') {
        this.setData({
          isLoading: false,
          errorMsg: '暂无商品详情数据'
        });
        return;
      }

      // 4. 处理商品描述（兼容desc字段为空/不存在的情况）
      result.descData = result.desc ? result.desc.trim().split('\n') : [];

      // 5. 检查是否已收藏
      const app = getApp()
      const isFavorite = app.isFavorite(result.pid)

      // 6. 赋值渲染（确保字段名正确）
      this.setData({
        detailData: result,
        pid: result.pid,
        isLoading: false,
        errorMsg: '',
        isFavorite: isFavorite
      });
    } catch (err) {
      console.error('获取商品详情失败：', err);
      this.setData({
        isLoading: false,
        errorMsg: err.message || '获取商品详情失败，请重试'
      });
    }
  },

  // Stepper加减逻辑
  handleStepper(e) {
    const type = e.target.dataset.type;
    let { count } = this.data;
    if (type === 'reduce' && count > 1) count--;
    if (type === 'add') count++;
    this.setData({ count });
  },

  // 加入购物袋
  addToCart() {
    const app = getApp()
    const { detailData, count } = this.data
    
    if (!detailData || !detailData.pid) {
      wx.showToast({
        title: '商品信息不完整',
        icon: 'none'
      })
      return
    }

    // 添加商品到购物车
    app.addToCart(detailData, count)
    
    wx.showToast({
      title: '已加入购物袋',
      icon: 'success',
      duration: 2000
    })
  },

  // 跳转到购物袋
  goToShopbag() {
    wx.switchTab({
      url: '../shopbag/shopbag'
    })
  },

  // 收藏/取消收藏
  toggleFavorite() {
    const app = getApp()
    const { detailData } = this.data
    
    if (!detailData || !detailData.pid) {
      wx.showToast({
        title: '商品信息不完整',
        icon: 'none'
      })
      return
    }

    const isAdded = app.addToFavorites(detailData)
    this.setData({
      isFavorite: isAdded
    })
    
    wx.showToast({
      title: isAdded ? '已收藏' : '已取消收藏',
      icon: 'success',
      duration: 1500
    })
  }

});