import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 请求封装
 * 后端统一返回 { code: 0成功/1失败, message, data }（见后端 Result.java）
 * 此处统一拦截：code!==0 时弹出后端提示并 reject。
 */
const request = axios.create({
  baseURL: '/',
  timeout: 10000
})

// 请求拦截器：自动携带 token
// 优先级：/admin 接口用 adminToken；其余接口用流通节点端 token，
// 未登录节点端但已登录管理端时（管理端页面查省市区 /region/**），用 adminToken 兜底，
// 否则 /region 请求不带令牌会被后端拦截器判为未登录并返回 401。
request.interceptors.request.use(
  (config) => {
    const url = config.url || ''
    const adminToken = localStorage.getItem('adminToken')
    const info = JSON.parse(localStorage.getItem('nodeInfo') || 'null')
    const nodeToken = info ? info.token : ''
    const token = url.indexOf('/admin') === 0 ? adminToken || '' : nodeToken || adminToken || ''
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 0) {
      return res
    }
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  (error) => {
    // 401：token 缺失或已失效（后端拦截器未放行）
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录状态已失效，请重新登录！')
      const url = (error.config && error.config.url) || ''
      // 按当前登录身份决定回退到哪个登录页：管理端接口，
      // 或未登录流通节点端但已登录管理端时（管理端页面的 /region/** 请求），都回管理端登录页
      const isAdmin = url.indexOf('/admin') === 0 ||
        (!localStorage.getItem('nodeInfo') && !!localStorage.getItem('adminToken'))
      if (isAdmin) {
        localStorage.removeItem('adminToken')
        window.location.href = '/sys/login'
      } else {
        localStorage.removeItem('nodeInfo')
        window.location.href = '/login'
      }
    } else {
      ElMessage.error('网络异常，请检查服务器是否启动！')
    }
    return Promise.reject(error)
  }
)

export default request
