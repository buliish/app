import request from '../utils/request'
import { prefixOf } from '../utils/nodeType'

/**
 * 产品批号接口（3.2.3 ~ 3.2.6）
 * 四个角色的接口形式一致，仅路径前缀不同：farm / froz / whol / reta
 */

// 批号列表：status 传状态值，不传返回全部可浏览批号
export const batchListApi = (nodeType, status) =>
  request.get(`/${prefixOf(nodeType)}/batch/list`, { params: { status } })

// 批号详情
export const batchDetailApi = (nodeType, id) => request.get(`/${prefixOf(nodeType)}/batch/${id}`)

// 批号唯一性校验（blur 事件）：返回 true 表示已存在
export const batchCheckApi = (nodeType, batchNo) =>
  request.get(`/${prefixOf(nodeType)}/batch/check`, { params: { batchNo } })

// 新建批号
export const batchSaveApi = (nodeType, data) => request.post(`/${prefixOf(nodeType)}/batch`, data)

// 更新批号：flag=true 表示同时发布 / 向上游发送确认请求
export const batchUpdateApi = (nodeType, data, flag) =>
  request.put(`/${prefixOf(nodeType)}/batch`, data, { params: { publish: flag, sendConfirm: flag } })

// 删除批号
export const batchDeleteApi = (nodeType, id) => request.delete(`/${prefixOf(nodeType)}/batch/${id}`)

// 下架批号
export const batchOfflineApi = (nodeType, id) => request.put(`/${prefixOf(nodeType)}/batch/offline/${id}`)

// 下游企业进场确认列表（可按下游企业名称模糊查询）
export const confirmListApi = (nodeType, downName) =>
  request.get(`/${prefixOf(nodeType)}/confirm/list`, { params: { downName } })

// 确认下游企业进场
export const confirmApi = (nodeType, id) => request.put(`/${prefixOf(nodeType)}/confirm/${id}`)

// 冷冻加工工序记录（清洗 / 分级 / 冷冻 / 包装）
export const processListApi = (batchId) => request.get(`/froz/process/${batchId}`)
export const processAddApi = (data) => request.post('/froz/process', data)
export const processDeleteApi = (id) => request.delete(`/froz/process/${id}`)
