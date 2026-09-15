import request from '../utils/request'

/** 消费者端溯源接口（3.2.7，免登录） */
export const traceApi = (traceCode) => request.get(`/trace/${encodeURIComponent(traceCode)}`)

/** 在售商品列表（消费者端首页"售卖的产品"） */
export const productListApi = (params) => request.get('/trace/products', { params })

/** 商品详情：产品信息 + 四级企业链 + 检测记录 + 加工工序 */
export const productDetailApi = (code) => request.get(`/trace/product/${encodeURIComponent(code)}`)
