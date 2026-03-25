import axios from 'axios'

// 创建axios实例
const request = axios.create({
  baseURL: '/api', 
  timeout: 60000, 
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    } else {
      config.headers['Content-Type'] = 'application/json'
    }
    
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 挂载到window，方便其他地方使用
window.$axios = request

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 统一处理响应数据
    const res = response.data
    return res
  },
  error => {
    // 统一处理错误
    console.error('响应错误:', error)
    
    // 处理网络错误
    if (!error.response) {
      console.error('网络错误，请检查网络连接')
      return Promise.reject(error)
    }
    
    // 处理HTTP错误
    const status = error.response.status
    switch (status) {
      case 401:
        console.error('未授权，请重新登录')
        // 可以在这里跳转到登录页
        break
      case 403:
        console.error('拒绝访问')
        break
      case 404:
        console.error('请求的资源不存在')
        break
      case 500:
        console.error('服务器错误')
        break
      default:
        console.error(`请求错误: ${status}`)
    }
    
    return Promise.reject(error)
  }
)

// 封装请求方法
export default request