import request from '../utils/request'

/** 系统管理端接口（3.2.8） */

// 管理员登录
export const adminLoginApi = (data) => request.post('/admin/login', data)

// 节点企业分页 + 模糊查询
export const adminNodePageApi = (params) => request.get('/admin/node/page', { params })

// 节点企业详情
export const adminNodeDetailApi = (id) => request.get(`/admin/node/${id}`)

// 新建节点企业
export const adminNodeSaveApi = (data) => request.post('/admin/node', data)

// 编辑节点企业
export const adminNodeUpdateApi = (data) => request.put('/admin/node', data)

// 删除节点企业
export const adminNodeDeleteApi = (id) => request.delete(`/admin/node/${id}`)

// 注册信息统计数据（可视化大屏）
export const adminStatsApi = () => request.get('/admin/stats')

// 批次追溯查询（管理端）
export const adminTraceSearchApi = (keyword) =>
  request.get('/admin/trace/search', { params: { keyword } })
export const adminTraceChainApi = (keyword) =>
  request.get('/admin/trace/chain', { params: { keyword } })
export const adminTraceChainByIdApi = (id) => request.get(`/admin/trace/chain/${id}`)
export const adminTraceStatsApi = () => request.get('/admin/trace/stats')

// 完整产业链树：一批虾派生出的虾滑/虾丸等各条分支都在里面（管理端视角）
export const adminTraceTreeApi = (retaBatchId) =>
  request.get(`/admin/trace/tree/${retaBatchId}`)
