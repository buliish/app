import { getProductByFlag } from '../../api/api'
// 引入AI工具函数
const { callCozeAPI } = require('../../utils/coze.js')

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
    originalProductData: [], // 保存原始商品数据，用于搜索过滤
    searchHistory: [], // 搜索历史记录
    showHistory: false, // 是否显示搜索历史
    aiSuggestions: [], // AI智能推荐关键词
    isLoadingAI: false // AI推荐加载状态
  },

  /**
   * 生命周期函数--监听页面加载
   */
  async onLoad(options) {
    // 显示加载中提示
    wx.showLoading({ title: '加载中...', mask: true });

    // 加载搜索历史记录
    this.loadSearchHistory();

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
      searchValue: value,
      showHistory: value === '' // 当搜索框为空时显示历史记录
    })
    // 如果输入框有内容，实时过滤商品
    if (value.trim() !== '') {
      this.filterProducts(value)
    } else {
      // 如果输入框为空，显示所有商品
      this.setData({
        productData: this.data.originalProductData
      })
    }
  },

  // 搜索确认
  onSearchConfirm(e) {
    const value = e.detail.value.trim()
    if (!value) {
      return
    }
    this.setData({
      searchValue: value,
      showHistory: false // 确认搜索后隐藏历史记录
    })
    // 保存搜索历史
    this.saveSearchHistory(value)
    // 执行搜索
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
    // 点击搜索框时显示历史记录
    this.setData({
      showHistory: this.data.searchValue === ''
    })
    // 如果搜索框为空，加载AI推荐
    if (this.data.searchValue === '') {
      this.loadAISuggestions()
    }
  },

  // 加载搜索历史记录
  loadSearchHistory() {
    try {
      // 从本地存储中读取搜索历史
      const history = wx.getStorageSync('searchHistory') || []
      this.setData({
        searchHistory: history
      })
    } catch (error) {
      console.error('加载搜索历史失败：', error)
      this.setData({
        searchHistory: []
      })
    }
  },

  // 保存搜索历史记录
  saveSearchHistory(keyword) {
    if (!keyword || keyword.trim() === '') {
      return
    }
    try {
      let history = wx.getStorageSync('searchHistory') || []
      // 移除重复的关键词
      history = history.filter(item => item !== keyword)
      // 将新关键词添加到最前面
      history.unshift(keyword)
      // 限制历史记录数量，最多保存10条
      if (history.length > 10) {
        history = history.slice(0, 10)
      }
      // 保存到本地存储
      wx.setStorageSync('searchHistory', history)
      // 更新页面数据
      this.setData({
        searchHistory: history
      })
    } catch (error) {
      console.error('保存搜索历史失败：', error)
    }
  },

  // 点击历史记录项
  onHistoryItemTap(e) {
    const keyword = e.currentTarget.dataset.keyword
    this.setData({
      searchValue: keyword,
      showHistory: false // 点击后隐藏历史记录
    })
    // 执行搜索
    this.filterProducts(keyword)
    // 将点击的历史记录移到最前面
    this.saveSearchHistory(keyword)
  },

  // 删除单条历史记录
  onDeleteHistoryItem(e) {
    const keyword = e.currentTarget.dataset.keyword
    try {
      let history = wx.getStorageSync('searchHistory') || []
      // 移除指定的历史记录
      history = history.filter(item => item !== keyword)
      // 保存到本地存储
      wx.setStorageSync('searchHistory', history)
      // 更新页面数据
      this.setData({
        searchHistory: history
      })
    } catch (error) {
      console.error('删除搜索历史失败：', error)
    }
  },

  // 清除所有搜索历史
  onClearAllHistory() {
    wx.showModal({
      title: '提示',
      content: '确定要清除所有搜索历史吗？',
      success: (res) => {
        if (res.confirm) {
          try {
            // 清空本地存储
            wx.removeStorageSync('searchHistory')
            // 更新页面数据
            this.setData({
              searchHistory: []
            })
            wx.showToast({
              title: '已清除',
              icon: 'success'
            })
          } catch (error) {
            console.error('清除搜索历史失败：', error)
          }
        }
      }
    })
  },

  // 隐藏搜索历史
  onHideHistory() {
    this.setData({
      showHistory: false
    })
  },

  // 加载AI智能推荐
  async loadAISuggestions() {
    // 如果正在加载或已有推荐，不重复加载
    if (this.data.isLoadingAI || this.data.aiSuggestions.length > 0) {
      return
    }

    this.setData({
      isLoadingAI: true
    })

    try {
      // 构建AI提示词，让AI推荐商品搜索关键词
      const prompt = `请根据咖啡、饮品、小食等商品类型，推荐5个热门搜索关键词，每个关键词用中文，简短（2-4个字），用逗号分隔，只返回关键词，不要其他说明。例如：拿铁,美式咖啡,卡布奇诺,摩卡,焦糖玛奇朵`
      
      // 调用AI API获取推荐
      const result = await callCozeAPI(prompt, (delta) => {
        // 流式更新可以在这里处理，但推荐关键词通常一次性返回
      })

      // 解析AI返回的关键词
      let suggestions = []
      if (result && result.content) {
        // 提取关键词，去除空格和换行
        const keywords = result.content
          .replace(/\n/g, ',')
          .split(',')
          .map(k => k.trim())
          .filter(k => k && k.length > 0 && k.length <= 6) // 过滤空值和过长的关键词
        
        // 限制最多5个推荐
        suggestions = keywords.slice(0, 5)
      }

      // 如果AI没有返回有效推荐，使用默认推荐
      if (suggestions.length === 0) {
        suggestions = ['拿铁', '美式咖啡', '卡布奇诺', '摩卡', '焦糖玛奇朵']
      }

      this.setData({
        aiSuggestions: suggestions,
        isLoadingAI: false
      })
    } catch (error) {
      console.error('加载AI推荐失败：', error)
      // AI失败时使用默认推荐
      this.setData({
        aiSuggestions: ['拿铁', '美式咖啡', '卡布奇诺', '摩卡', '焦糖玛奇朵'],
        isLoadingAI: false
      })
    }
  },

  // 点击AI推荐关键词
  onAISuggestionTap(e) {
    const keyword = e.currentTarget.dataset.keyword
    if (!keyword) return

    this.setData({
      searchValue: keyword,
      showHistory: false // 点击后隐藏历史记录
    })
    // 执行搜索
    this.filterProducts(keyword)
    // 保存到搜索历史
    this.saveSearchHistory(keyword)
  }
})