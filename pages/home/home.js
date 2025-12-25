import { getProductByFlag } from '../../api/api'

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 轮播图展示热卖的商品，首页推荐上新、特价数据
    flags: ['热卖', '上新', '特价'],
    // 轮播图数据
    bannerData: [],
    productData: [],
    // 搜索相关
    searchValue: '',
    originalProductData: [] // 保存原始商品数据，用于搜索过滤
  },

  /**
   * 生命周期函数--监听页面加载
   */
  async onLoad(options) {
    // 显示加载中提示
    wx.showLoading({ title: '加载中...', mask: true });

    try {
      console.log('开始请求商品数据');
      // 接口入参优化：避免不必要的 JSON.stringify（根据实际接口调整，若接口需要字符串则保留）
      let result = await getProductByFlag(this.data.flags); 
      console.log('商品数据请求结果 ==> ', result);

      // 严格的空值校验
      if (!result || !result.data || !Array.isArray(result.data.result)) {
        throw new Error('商品数据格式异常');
      }

      const bannerFlag = this.data.flags[0];
      const bannerData = [];
      const productData = [];

      // 遍历数据分类（增加空值防护）
      result.data.result.forEach(item => {
        if (item && item.flag === bannerFlag) {
          bannerData.push(item);
        } else if (item) {
          productData.push(item);
        }
      });

      // 数据兜底：若轮播无数据，使用默认图
      this.setData({
        bannerData: bannerData.length > 0 ? bannerData : [{
          largeImg: '/images/home.png'
        }],
        productData: productData,
        originalProductData: productData // 保存原始数据
      });

    } catch (error) {
      console.error('数据加载失败：', error.message);
      wx.showToast({ title: '数据加载失败', icon: 'none', duration: 2000 });

      // 兜底数据：保证页面不空白
      this.setData({
        bannerData: [{
          largeImg: '/images/home.png'
        }],
        productData: [],
        originalProductData: []
      });
    } finally {
      // 关闭加载提示（无论成功/失败都执行）
      wx.hideLoading();
    }
  },

  //查看商品详情
  viewDetail(e){
    let pid = e.currentTarget.dataset.pid;
    wx.navigateTo({
      url: `../detail/detail?pid=${pid}`
    });
  },

  // 搜索输入
  onSearchInput(e) {
    const value = e.detail.value
    this.setData({
      searchValue: value
    })
    this.filterProducts(value)
  },

  // 搜索确认
  onSearchConfirm(e) {
    const value = e.detail.value
    this.setData({
      searchValue: value
    })
    this.filterProducts(value)
  },

  // 过滤商品
  filterProducts(keyword) {
    const { originalProductData } = this.data
    if (!keyword || keyword.trim() === '') {
      // 如果搜索关键词为空，显示所有商品
      this.setData({
        productData: originalProductData
      })
      return
    }

    // 过滤商品（根据商品名称和英文名称）
    const filtered = originalProductData.filter(item => {
      const name = (item.name || '').toLowerCase()
      const enname = (item.enname || '').toLowerCase()
      const searchKey = keyword.toLowerCase()
      return name.includes(searchKey) || enname.includes(searchKey)
    })

    this.setData({
      productData: filtered
    })
  },

  // 点击搜索框
  onSearchTap() {
    // 可以在这里添加搜索页面的跳转，或者直接在当前页面搜索
  }
})