<template>
  <div class="settings-page">
    <div class="settings-header">
      <button class="back-btn" @click="goBack">
        <i class='bx bxs-chevron-left'></i>
      </button>
      <h1>{{ $t('settings.title') }}</h1>
    </div>

    <div class="settings-content">
      <!-- 语言设置 -->
      <div class="setting-section">
        <h3>{{ $t('settings.language') }}</h3>
        <div class="setting-item">
          <span>{{ $t('settings.appLanguage') }}</span>
          <select class="language-select" v-model="currentLanguage" @change="changeLanguage(currentLanguage)">
            <option v-for="lang in languages" :key="lang.code" :value="lang.code">
              {{ lang.name }}
            </option>
          </select>
        </div>
      </div>

      <!-- 通知设置 -->
      <div class="setting-section">
        <h3>{{ $t('settings.notification') }}</h3>
        <div class="setting-item">
          <span>{{ $t('settings.pushNotification') }}</span>
          <label class="switch">
            <input type="checkbox" v-model="notificationSettings.push">
            <span class="slider round"></span>
          </label>
        </div>
        <div class="setting-item">
          <span>{{ $t('settings.emailNotification') }}</span>
          <label class="switch">
            <input type="checkbox" v-model="notificationSettings.email">
            <span class="slider round"></span>
          </label>
        </div>
      </div>

      <!-- 其他设置 -->
      <div class="setting-section">
          <h3>{{ $t('settings.other') }}</h3>
          <div class="setting-item" @click="clearCache">
            <span>{{ $t('settings.clearCache') }}</span>
            <i class='bx bxs-chevron-right'></i>
          </div>
          <div class="setting-item">
            <span>{{ $t('settings.nightMode') }}</span>
            <label class="switch">
              <input type="checkbox" v-model="nightMode">
              <span class="slider round"></span>
            </label>
          </div>
        </div>

      <!-- 关于 -->
      <div class="setting-section">
        <h3>{{ $t('settings.about') }}</h3>
        <div class="setting-item">
          <span>{{ $t('settings.version') }}</span>
          <span class="version">1.0.0</span>
        </div>
        <div class="setting-item" @click="showUserAgreement">
          <span>{{ $t('settings.userAgreement') }}</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
        <div class="setting-item" @click="showPrivacyPolicy">
          <span>{{ $t('settings.privacyPolicy') }}</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
        <div class="setting-item" @click="checkUpdate">
          <span>{{ $t('settings.checkUpdate') }}</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { setLocale } from '../i18n'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const { t, locale } = useI18n()

// 语言选项 - 仅支持中英日韩
const languages = [
  { code: 'zh', name: '中文' },
  { code: 'en', name: 'English' },
  { code: 'ja', name: '日本語' },
  { code: 'ko', name: '한국어' }
]

// 当前语言
const currentLanguage = ref(locale.value)

// 通知设置
const notificationSettings = ref({
  push: true,
  email: true
})

// 夜间模式
const nightMode = ref(false)

// 返回上一页
const goBack = () => {
  router.back()
}

// 切换语言
const changeLanguage = async (langCode) => {
  setLocale(langCode)
  currentLanguage.value = langCode
  
  // 调用后端API更新用户语言偏好
  try {
    const response = await fetch('/api/user/language', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({ langCode })
    })
    
    const data = await response.json()
    if (data.code === 200) {
      // 更新本地存储中的用户信息
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        user.langCode = langCode
        localStorage.setItem('user', JSON.stringify(user))
      }
      notify.success(t('settings.languageChanged', languages.find(lang => lang.code === langCode).name))
    }
  } catch (error) {
    console.error('更新语言偏好失败:', error)
  }
}

// 清除缓存
const clearCache = () => {
  localStorage.removeItem('cache')
  notify.success(t('settings.cacheCleared'))
}

// 显示用户协议
const showUserAgreement = () => {
  notify.info(t('settings.userAgreement') + t('common.comingSoon'))
}

// 显示隐私政策
const showPrivacyPolicy = () => {
  notify.info(t('settings.privacyPolicy') + t('common.comingSoon'))
}

// 检查更新
const checkUpdate = () => {
  notify.info(t('settings.latestVersion'))
}

// 页面加载时获取设置
onMounted(() => {
  // 从 i18n 获取当前语言
  currentLanguage.value = locale.value
  
  // 从本地存储获取通知设置
  const savedNotifications = localStorage.getItem('notifications')
  if (savedNotifications) {
    notificationSettings.value = JSON.parse(savedNotifications)
  }
  
  // 从本地存储获取夜间模式设置
  const savedNightMode = localStorage.getItem('nightMode')
  if (savedNightMode) {
    nightMode.value = JSON.parse(savedNightMode)
  }
})
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  width: 100%;
  margin: 0 auto;
}

@media (min-width: 768px) {
  .settings-page {
    width: 85%;
    margin: 0 auto;
  }
}

@media (min-width: 1024px) {
  .settings-page {
    width: 70%;
    margin: 0 auto;
  }
}

.settings-header {
  background: white;
  padding: 15px 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #eee;
  border-radius: 0px 0px 12px 12px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #333;
  margin-right: 15px;
}

.settings-header h1 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.settings-content {
  padding: 20px;
}

.setting-section {
  background: white;
  border-radius: 12px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.setting-section h3 {
  padding: 15px 20px;
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #666;
  background: #f9f9f9;
  border-bottom: 1px solid #eee;
}

.setting-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background-color 0.2s;
}

.setting-item:hover {
  background-color: #f5f5f5;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item span {
  flex: 1;
  color: #333;
  font-size: 15px;
}

.setting-item .version {
  color: #999;
  font-size: 14px;
}

.setting-item .bx-chevron-right {
  color: #999;
  font-size: 16px;
}

/* 语言选择器 */
.language-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  outline: none;
  min-width: 120px;
}

.language-select:focus {
  border-color: #7494ec;
}

/* 开关样式 */
.switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .4s;
}

input:checked + .slider {
  background-color: #7494ec;
}

input:focus + .slider {
  box-shadow: 0 0 1px #7494ec;
}

input:checked + .slider:before {
  transform: translateX(26px);
}

.slider.round {
  border-radius: 24px;
}

.slider.round:before {
  border-radius: 50%;
}

@media (max-width: 768px) {
  .settings-content {
    padding: 10px;
  }
  
  .setting-item {
    padding: 12px 15px;
  }
}
</style>
