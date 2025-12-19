let baseUrl = 'http://127.0.0.1:7001';

//获取商品类型
export const getType = () => {
  return new Promise((resolve,reject) => {
    wx.request({
      url: `${baseUrl}/type`,
      method: 'GET',
      success: result =>{
        resolve(result)
      },
      fail: err =>{
        reject(err)
      }
    })
  })
}

//根据商品类型获取商品数据
export const getProductByType = typeId => {
  return new Promise((resolve,reject) => {
    wx.request({
      url: `${baseUrl}/typeProducts`,
      method: 'GET',
      data:{
        typeId
      },
      success: result =>{
        resolve(result)
      },
      fail: err =>{
        reject(err)
      }
    })
  })
}

//根据指定商品标志查询商品数据
export const getProductByFlag = flags => {
  return new Promise((resolve,reject) => {
    wx.request({
      url: `${baseUrl}/flagProducts`,
      method: 'GET',
      data:{
        typeId
      },
      success: result =>{
        resolve(result)
      },
      fail: err =>{
        reject(err)
      }
    })
  })
}