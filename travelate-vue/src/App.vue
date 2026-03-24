<template>
  <div>
    <router-view />
    <Notification ref="notificationRef" />
    <ConfirmModal ref="confirmModalRef" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Notification from './components/Notification.vue'
import ConfirmModal from './components/ConfirmModal.vue'

const router = useRouter()
const notificationRef = ref(null)
const confirmModalRef = ref(null)

// 通知方法
const notify = {
  success: (message) => notificationRef.value?.success(message),
  error: (message) => notificationRef.value?.error(message),
  warning: (message) => notificationRef.value?.warning(message),
  info: (message) => notificationRef.value?.info(message)
}

// 确认框方法
const confirm = (options) => {
  if (confirmModalRef.value) {
    confirmModalRef.value.show(options)
  }
}

// 将notify和confirm方法暴露给全局
import { getCurrentInstance } from 'vue'
const app = getCurrentInstance()
if (app) {
  app.appContext.config.globalProperties.$notify = notify
  app.appContext.config.globalProperties.$confirm = confirm
}

onMounted(() => {
  // 处理GitHub登录回调
  const urlParams = new URLSearchParams(window.location.search)
  const token = urlParams.get('token')
  const userStr = urlParams.get('user')
  const error = urlParams.get('error')
  const errorDesc = urlParams.get('desc')

  if (token && userStr) {
    try {
      const user = JSON.parse(decodeURIComponent(userStr))
      // 保存token和用户信息到localStorage
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
      // 清除URL参数
      window.history.replaceState({}, document.title, window.location.pathname)
      // 跳转到主页面
      router.push('/home/translate')
    } catch (e) {
      console.error('处理GitHub登录回调失败:', e)
      notify.error('GitHub登录处理失败，请重试')
      window.history.replaceState({}, document.title, window.location.pathname)
    }
  } else if (error) {
    console.error('GitHub登录失败:', error, errorDesc)
    
    // 显示友好的错误提示
    let errorMessage = 'GitHub登录失败'
    if (error === 'github_token_failed') {
      errorMessage = 'GitHub授权失败：' + (errorDesc || '无法获取访问令牌')
    } else if (error === 'github_auth_failed') {
      errorMessage = 'GitHub认证失败，请检查应用配置'
    } else if (error === 'github_rate_limit') {
      errorMessage = 'GitHub API请求频率限制，请稍后再试'
    } else if (error === 'github_network_error') {
      errorMessage = '网络连接失败，请检查网络或代理设置'
    } else if (error === 'github_login_failed') {
      errorMessage = 'GitHub登录失败，请重试'
    }
    
    notify.error(errorMessage)
    // 清除URL参数
    window.history.replaceState({}, document.title, window.location.pathname)
  }
})
</script>

<style>
#app {
  background: linear-gradient(90deg, #83b3f7ff, #c9d6ff);
  min-height: 100vh;
  width: 100%;
}
</style>
