import request from '../utils/request'

/**
 * 行政区域与上游企业联动接口
 * 新建批号时逐级选择：省 -> 市 -> 上游企业 -> 上游批号 -> 品种
 */

// 全部省
export const provincesApi = () => request.get('/region/provinces')

// 指定省下的市
export const citiesApi = (provId) => request.get('/region/cities', { params: { provId } })

// 指定类型与区域下的企业（上游企业下拉）
export const nodesApi = (type, provId, cityId) =>
  request.get('/region/nodes', { params: { type, provId, cityId } })

// 指定企业可流通的上游批号（含品种、产品类型）
export const upBatchesApi = (nodeId) => request.get('/region/batches', { params: { nodeId } })
