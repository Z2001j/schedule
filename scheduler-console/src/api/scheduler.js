import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

// 管理 API 令牌鉴权：与 admin 的 management-token（为空则 access-token）一致。
// 生产通过 VITE_SCHED_TOKEN 注入，切勿使用默认值。
http.defaults.headers.common['X-Access-Token'] = import.meta.env.VITE_SCHED_TOKEN || 'changeme'

http.interceptors.response.use(
  res => {
    const r = res.data
    if (r.code !== 0) return Promise.reject(new Error(r.message || '请求失败'))
    return r.data
  },
  err => {
    const msg = err.response?.data?.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export const listTasks = () => http.get('/tasks')
export const getTask = id => http.get(`/tasks/${id}`)
// 写操作携带 version 实现乐观锁；version 来自任务视图，冲突时后端返回 400
export const updateSchedule = (id, d, version) => http.put(`/tasks/${id}/schedule?version=${version}`, d)
export const updateRoute = (id, d, version) => http.put(`/tasks/${id}/route?version=${version}`, d)
export const startTask = (id, version) => http.post(`/tasks/${id}/start?version=${version}`)
export const stopTask = (id, version) => http.post(`/tasks/${id}/stop?version=${version}`)
export const triggerTask = id => http.post(`/tasks/${id}/trigger`)
export const listExecutors = () => http.get('/executors')
export const getLogs = (id, params) => http.get(`/tasks/${id}/logs`, { params })