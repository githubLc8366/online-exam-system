import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  // 生产环境读取 .env.production 里的后端地址；本地开发回退到 '/api' 走 vite 代理
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 可以在这里添加 token 等认证信息
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else {
      // 使用后端返回的 msg 字段显示错误信息（silent 请求不弹出）
      if (!response.config?.silent) ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
  },
  error => {
    const silent = error.config && error.config.silent
    const status = error.response && error.response.status
    // 401 未授权：Token 过期/无效，优先跳转登录（即使带 msg）
    if (status === 401) {
      localStorage.clear()
      sessionStorage.clear()
      ElMessage.error('登录状态已过期，请重新登录')
      window.location.href = '/login'
    } else if (error.response && error.response.data && error.response.data.msg) {
      if (!silent) ElMessage.error(error.response.data.msg)
    } else if (status === 403) {
      if (!silent) ElMessage.error('权限不足，无法访问')
    } else {
      if (!silent) ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
