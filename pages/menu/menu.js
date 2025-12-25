import {
  getType,
  getProductByType
} from '../../api/api'
// 引入AI工具函数
const { callCozeAPI } = require('../../utils/coze.js')

Page({

  /**
   * 页面初始数据
   */
  data: {
    //商品类型
    typeData: [],
    //选择商品类型下标
    selectedTypeIndex: 0,
    //商品数据
    productData:[],
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
  onLoad(options) {
    console.log('页面加载...');

    // 加载搜索历史记录
    this.loadSearchHistory();

    //获取商品类型
    getType().then(async result => {
      console.log('商品类型 result ==>', result);
      this.setData({
        typeData: result.data.result
      })

      //获取默认选中的商品类型id
      let typeId = this.data.typeData[this.data.selectedTypeIndex].typeId;
      console.log('typeId ==> ',typeId);

      let data = await getProductByType(typeId);
      console.log('根据商品类型获取商品数据 data ==> ',data);

      this.setData({
        productData: data.data.result,
        originalProductData: data.data.result // 保存原始数据
      })

    })
    .catch(err =>{
      console.log('获取商品类型失败，可能是本地服务器未启动:', err);
      // 如果本地服务器未启动，使用空数据，避免页面崩溃
      this.setData({
        typeData: []
      });
    })
  },
  
  //切换商品类型
  async toggleType(e){
    //e: 事件对象
    console.log('切换商品类型');
    let index = e.currentTarget.dataset.index;
    console.log('index ==> ',index);
    //判断当前点击的类型是否处于选中状态
    if (this.data.selectedTypeIndex == index){
      console.log('点击的类型处于选中状态');
      return;
    }
    this.setData({
      selectedTypeIndex: index
    })
    let typeId = this.data.typeData[index].typeId;
    console.log('切换商品类型 typeId ==>',typeId)
    let data = await getProductByType(typeId)
    console.log('根据商品类型获取商品数据 data ==>',data);
    this.setData({
      productData: data.data.result,
      originalProductData: data.data.result, // 保存原始数据
      searchValue: '', // 切换类型时清空搜索
      showHistory: false // 切换类型时隐藏历史记录
    })
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

  // 查看商品详情
  viewDetail(e) {
    let pid = e.currentTarget.dataset.pid;
    if (!pid) {
      wx.showToast({
        title: '商品ID不存在',
        icon: 'none'
      })
      return
    }
    wx.navigateTo({
      url: `../detail/detail?pid=${pid}`
    });
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