import request from '../utils/request'

/**
 * 流通节点端共通功能接口（3.2.2）
 */

// 登录
export const loginApi = (data) => request.post('/user/login', data)

// 退出登录
export const logoutApi = () => request.post('/user/logout')

// 查询当前登录企业信息（后端按登录编码 code 查询）
export const getNodeInfoApi = (code) => request.get(`/user/getInfo/${code}`)

// 更新密码（后端为 PATCH 请求）
export const updatePwdApi = (data) => request.patch('/user/updatePwd', data)
