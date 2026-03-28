<template>
  <div class="tools-page">
    <!-- 主要内容区 -->
    <main class="main-content">
      <!-- 欢迎语 -->
      <div class="welcome-section">
        <div class="welcome-content">
          <img src="https://travelate.oss-cn-wuhan-lr.aliyuncs.com/icons/welcome-icon.png" alt="YayFolk" class="welcome-icon" />
          <div class="welcome-text">
            <h1>{{ $t('translate.title') }}</h1>
            <p>{{ $t('translate.subtitle') }}</p>
          </div>
        </div>
      </div>

      <!-- 快捷功能 -->
      <div class="quick-features">
        <div class="feature-card" @click="openText">
          <i class='bx bx-text'></i>
          <span>{{ $t('translate.text') }}</span>
        </div>
        <div class="feature-card" @click="openCamera">
          <i class='bx bx-camera'></i>
          <span>{{ $t('translate.camera') }}</span>
        </div>
        <div class="feature-card" @click="openVoice">
          <i class='bx bx-microphone'></i>
          <span>{{ $t('translate.voice') }}</span>
        </div>
        <div class="feature-card" @click="openAIHeritage">
          <i class='bx bx-route'></i>
          <span>非遗路线</span>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()

// 本地默认头像（用于网络不可用时的回退）
const localDefaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI0MCIgZmlsbD0iIzc0OTRlYyIvPgogIDxjaXJjbGUgY3g9IjUwIiBjeT0iMzAiIHI9IjIwIiBmaWxsPSIjNzQ5NGVjIi8+CiAgPGNpcmNsZSBjeD0iNTUiIGN5PSI0NSIgcj0iNSIgZmlsbD0iI2ZmZiIvPgogIDxjaXJjbGUgY3g9IjQ1IiBjeT0iNDUiIHI9IjUiIGZpbGw9IiNmZmYiLz4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjYwIiByPSIyIiBmaWxsPSIjNzQ5NGVjIi8+CiAgPHRleHQgeD0iNTAiIHk9Ijc1IiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTIiIGZpbGw9IiMzMzMiIG5hbWU9ImNvbnRlbnQiPldyb3lhbC4uLjwvdGV4dD4KPC9zdmc+'

// 用户头像
const userAvatar = ref('https://api.dicebear.com/7.x/avataaars/svg?seed=Felix')

// 检查头像URL是否可访问
const checkAvatarAccessibility = (url) => {
  return new Promise((resolve) => {
    if (!url || url === localDefaultAvatar) {
      resolve(true)
      return
    }
    
    const img = new Image()
    img.onload = () => resolve(true)
    img.onerror = () => resolve(false)
    img.src = url
  })
}

// 页面加载时获取用户信息和头像
onMounted(async () => {
  // 从本地存储获取用户信息
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    if (user.avatar) {
      userAvatar.value = user.avatar
      // 检查头像是否可访问
      const isAccessible = await checkAvatarAccessibility(user.avatar)
      if (!isAccessible) {
        userAvatar.value = localDefaultAvatar
      }
    }
  } else {
    // 本地存储中没有用户信息，检查默认头像是否可访问
    const isAccessible = await checkAvatarAccessibility(userAvatar.value)
    if (!isAccessible) {
      userAvatar.value = localDefaultAvatar
    }
  }
})

// 快捷功能
const openText = () => {
  router.push('/text-translate')
}
const openCamera = () => {
  router.push('/camera-translate')
}
const openVoice = () => {
  router.push('/voice-translate')
}
const openAIHeritage = () => {
  router.push('/ai-heritage-route')
}


// 退出登录
const logout = () => {
  // 清除localStorage中的token和用户信息
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  // 跳转到登录页面
  router.push('/')
}
</script>

<style scoped>
.tools-page {
  min-height: 100vh;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

/* 主内容区 */
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

/* 欢迎语 */
.welcome-section {
  text-align: center;
  color: white;
  margin-bottom: 20px;
}

.welcome-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.welcome-icon {
  width: 150px;
  height: 150px;
  object-fit: contain;
}

.welcome-text {
  text-align: left;
}

.welcome-text h1 {
  font-size: 32px;
  margin-bottom: 8px;
}

.welcome-text p {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

/* 快捷功能 */
.quick-features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.feature-card {
  background: white;
  border-radius: 15px;
  padding: 25px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.feature-card i {
  font-size: 36px;
  color: #7494ec;
  margin-bottom: 10px;
}

.feature-card span {
  display: block;
  color: #333;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 900px) {
  .navbar {
    padding: 15px 20px;
  }
  
  .nav-links {
    display: none;
  }
}

@media (max-width: 768px) {
  .quick-features {
    gap: 15px;
  }
  
  .feature-card {
    padding: 20px;
  }
  
  .feature-card i {
    font-size: 30px;
  }
  
  .feature-card span {
    font-size: 14px;
  }
  
  .welcome-content {
    flex-direction: column;
    gap: 10px;
  }

  .welcome-icon {
    width: 100px;
    height: 100px;
  }

  .welcome-text {
    text-align: center;
  }

  .welcome-text h1 {
    font-size: 24px;
  }

  .welcome-text p {
    font-size: 14px;
  }
}

@media (max-width: 600px) {
  .quick-features {
    gap: 10px;
  }
  
  .feature-card {
    padding: 15px;
  }
  
  .feature-card i {
    font-size: 28px;
  }
  
  .feature-card span {
    font-size: 12px;
  }
}

/* 中等屏幕 - 只显示图标 */
@media (max-width: 550px) {
  .quick-features {
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
  }
  
  .feature-card {
    padding: 15px;
  }
  
  .feature-card i {
    font-size: 32px;
    margin-bottom: 0;
  }
  
  .feature-card span {
    display: none;
  }
}

/* 小屏幕 - 单列布局 */
@media (max-width: 480px) {
  .quick-features {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  
  .feature-card {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 15px;
    padding: 15px 20px;
  }
  
  .feature-card i {
    font-size: 24px;
    margin-bottom: 0;
  }
  
  .feature-card span {
    display: block;
    font-size: 16px;
  }
}
</style>
