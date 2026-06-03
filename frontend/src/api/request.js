import axios from 'axios'
import { getToken, removeToken } from '@/utils/auth'
import { Message } from 'element-ui'
import router from '@/router'
import store from '@/store'
import { disconnectWebSocket } from '@/utils/websocket'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

function handle401() {
  removeToken()
  store.commit('user/CLEAR_ALL')
  disconnectWebSocket()
  router.push('/login')
}

service.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
})

service.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code !== 200 && res.code !== 0) {
      Message.error(res.message || '请求失败')
      if (res.code === 401) {
        handle401()
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      handle401()
      Message.error('登录已过期，请重新登录')
    } else {
      Message.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
