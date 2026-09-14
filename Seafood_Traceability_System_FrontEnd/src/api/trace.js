import request from '../utils/request'

/** 消费者端溯源接口（3.2.7，免登录） */
export const traceApi = (traceCode) => request.get(`/trace/${encodeURIComponent(traceCode)}`)
