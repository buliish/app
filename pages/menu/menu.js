import {
  getType,
  getProductByType
} from '../../api/api'

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
    originalProductData: [] // 保存原始商品数据，用于搜索过滤
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    console.log('页面加载...');

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
      searchValue: '' // 切换类型时清空搜索
    })
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
  }
})