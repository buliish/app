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
    allProducts: [], // 所有分类的商品数据（用于跨分类搜索）
    searchHistory: [], // 搜索历史记录
    showHistory: false, // 是否显示搜索历史
    aiSuggestions: [], // AI智能推荐关键词
    isLoadingAI: false, // AI推荐加载状态
    isSearching: false // 是否正在搜索（跨分类搜索状态）
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
      const typeData = result.data.result
      this.setData({
        typeData: typeData
      })

      // 加载所有分类的商品数据（用于跨分类搜索）
      await this.loadAllProducts(typeData)

      //获取默认选中的商品类型id
      let typeId = typeData[this.data.selectedTypeIndex].typeId;
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

  // 加载所有分类的商品数据（用于跨分类搜索）
  async loadAllProducts(typeData) {
    try {
      // 使用传入的typeData，如果没有则使用this.data.typeData
      const types = typeData || this.data.typeData || []
      if (types.length === 0) {
        console.warn('没有分类数据，无法加载所有商品')
        return
      }

      const allProducts = []
      
      // 遍历所有分类，加载每个分类的商品
      for (let i = 0; i < types.length; i++) {
        const type = types[i]
        try {
          const data = await getProductByType(type.typeId)
          if (data && data.data && data.data.result) {
            // 为每个商品添加所属分类信息
            const productsWithType = data.data.result.map(product => ({
              ...product,
              typeId: type.typeId,
              typeName: type.type,
              typeIndex: i // 保存分类索引，方便跳转
            }))
            allProducts.push(...productsWithType)
          }
        } catch (error) {
          console.error(`加载分类 ${type.type} 的商品失败:`, error)
        }
      }
      
      this.setData({
        allProducts: allProducts
      })
      console.log('所有商品加载完成，数量:', allProducts.length)
    } catch (error) {
      console.error('加载所有商品失败:', error)
    }
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
      showHistory: false, // 切换类型时隐藏历史记录
      isSearching: false // 切换类型时退出搜索状态
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

  // 过滤商品（支持跨分类搜索）
  filterProducts(keyword) {
    if (!keyword || keyword.trim() === '') {
      // 如果搜索关键词为空，显示当前分类的所有商品
      this.setData({
        productData: this.data.originalProductData,
        isSearching: false
      })
      return
    }

    // 跨分类搜索：在所有商品中搜索
    const { allProducts } = this.data
    const searchKey = keyword.toLowerCase()
    
    const filtered = allProducts.filter(item => {
      const name = (item.name || '').toLowerCase()
      const enname = (item.enname || '').toLowerCase()
      const desc = (item.desc || '').toLowerCase()
      return name.includes(searchKey) || enname.includes(searchKey) || desc.includes(searchKey)
    })

    this.setData({
      productData: filtered,
      isSearching: true // 标记为搜索状态
    })
  },

  // 查看商品详情（支持从搜索结果跳转到对应分类）
  async viewDetail(e) {
    let pid = e.currentTarget.dataset.pid;
    if (!pid) {
      wx.showToast({
        title: '商品ID不存在',
        icon: 'none'
      })
      return
    }

    // 如果是在搜索状态下，需要先切换到对应分类
    if (this.data.isSearching) {
      // 从搜索结果中找到该商品
      const product = this.data.productData.find(item => item.pid === pid)
      if (product && product.typeIndex !== undefined) {
        // 切换到商品所属的分类
        await this.switchToType(product.typeIndex)
        // 清空搜索状态，显示该分类的所有商品
        this.setData({
          searchValue: '',
          isSearching: false,
          showHistory: false
        })
        // 滚动到该商品位置（可选）
        // 由于商品列表是动态加载的，这里先跳转到详情页
      }
    }

    // 跳转到商品详情页
    wx.navigateTo({
      url: `../detail/detail?pid=${pid}`
    });
  },

  // 切换到指定分类
  async switchToType(typeIndex) {
    if (typeIndex === this.data.selectedTypeIndex) {
      return // 已经是当前分类，无需切换
    }

    const { typeData } = this.data
    if (typeIndex < 0 || typeIndex >= typeData.length) {
      console.error('分类索引无效:', typeIndex)
      return
    }

    // 更新选中的分类索引
    this.setData({
      selectedTypeIndex: typeIndex
    })

    // 加载该分类的商品
    const typeId = typeData[typeIndex].typeId
    try {
      const data = await getProductByType(typeId)
      if (data && data.data && data.data.result) {
        this.setData({
          productData: data.data.result,
          originalProductData: data.data.result
        })
      }
    } catch (error) {
      console.error('加载分类商品失败:', error)
    }
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
      const app = getApp()
      const storageKey = app.getUserStorageKey('searchHistory')
      // 从本地存储中读取搜索历史（使用用户隔离的key）
      const history = wx.getStorageSync(storageKey) || []
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
      const app = getApp()
      const storageKey = app.getUserStorageKey('searchHistory')
      let history = wx.getStorageSync(storageKey) || []
      // 移除重复的关键词
      history = history.filter(item => item !== keyword)
      // 将新关键词添加到最前面
      history.unshift(keyword)
      // 限制历史记录数量，最多保存10条
      if (history.length > 10) {
        history = history.slice(0, 10)
      }
      // 保存到本地存储（使用用户隔离的key）
      wx.setStorageSync(storageKey, history)
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
      const app = getApp()
      const storageKey = app.getUserStorageKey('searchHistory')
      let history = wx.getStorageSync(storageKey) || []
      // 移除指定的历史记录
      history = history.filter(item => item !== keyword)
      // 保存到本地存储
      wx.setStorageSync(storageKey, history)
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
            const app = getApp()
            const storageKey = app.getUserStorageKey('searchHistory')
            // 清空本地存储
            wx.removeStorageSync(storageKey)
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