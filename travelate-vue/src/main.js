import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import Notification from './components/Notification.vue'

const app = createApp(App)

app.use(router)
app.use(i18n)

// 注册Notification组件为全局组件
app.component('Notification', Notification)

// 添加全局通知方法（将在App.vue中实现具体逻辑）
app.config.globalProperties.$notify = {
  success: (message) => {},
  error: (message) => {},
  warning: (message) => {},
  info: (message) => {}
}

// 添加全局错误处理器，捕获未检查的运行时错误
app.config.errorHandler = (err, vm, info) => {
  // 忽略 Chrome 扩展相关的消息端口错误
  if (err.message && err.message.includes('message port closed')) {
    // 这些错误通常是浏览器扩展引起的，可以安全忽略
    return
  }
  console.error('应用错误:', err, info)
}

app.mount('#app')
