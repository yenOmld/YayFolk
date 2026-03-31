<template>
  <div class="settings-page">
    <div class="settings-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <i class='bx bxs-chevron-left'></i>
      </button>
      <h1>设置</h1>
    </div>

    <div class="settings-content">
      <section class="setting-section">
        <h3>语言</h3>
        <div class="setting-item">
          <span>应用语言</span>
          <select class="language-select" v-model="currentLanguage" @change="changeLanguage(currentLanguage)">
            <option v-for="lang in languages" :key="lang.code" :value="lang.code">
              {{ lang.name }}
            </option>
          </select>
        </div>
      </section>

      <section class="setting-section">
        <h3>通知</h3>
        <div class="setting-item">
          <span>推送通知</span>
          <label class="switch">
            <input type="checkbox" v-model="notificationSettings.push">
            <span class="slider round"></span>
          </label>
        </div>
        <div class="setting-item">
          <span>邮件通知</span>
          <label class="switch">
            <input type="checkbox" v-model="notificationSettings.email">
            <span class="slider round"></span>
          </label>
        </div>
      </section>

      <section class="setting-section">
        <h3>其他</h3>
        <div class="setting-item" @click="clearCache">
          <span>清除缓存</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
        <div class="setting-item">
          <span>夜间模式</span>
          <label class="switch">
            <input type="checkbox" v-model="nightMode">
            <span class="slider round"></span>
          </label>
        </div>
      </section>

      <section class="setting-section">
        <h3>关于</h3>
        <div class="setting-item">
          <span>版本</span>
          <span class="version">1.0.0</span>
        </div>
        <div class="setting-item" @click="showUserAgreement">
          <span>用户协议</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
        <div class="setting-item" @click="showPrivacyPolicy">
          <span>隐私政策</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
        <div class="setting-item" @click="checkUpdate">
          <span>检查更新</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()

const languages = [
  { code: 'zh', name: '中文' },
  { code: 'en', name: 'English' },
  { code: 'ja', name: '日本語' },
  { code: 'ko', name: '한국어' }
]

const currentLanguage = ref('zh')
const notificationSettings = ref({
  push: true,
  email: true
})
const nightMode = ref(false)

const syncStoredUser = (payload = {}) => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return
  }

  try {
    const nextUser = {
      ...JSON.parse(raw),
      ...payload
    }
    localStorage.setItem('user', JSON.stringify(nextUser))
    localStorage.setItem('userInfo', JSON.stringify(nextUser))
  } catch (error) {
    console.error('同步本地用户信息失败:', error)
  }
}

const goBack = () => {
  router.back()
}

const changeLanguage = async (langCode) => {
  currentLanguage.value = langCode

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
      syncStoredUser({ langCode })
      notify.success('语言已更改为：' + languages.find(lang => lang.code === langCode)?.name || langCode)
    }
  } catch (error) {
    console.error('更新语言偏好失败:', error)
  }
}

const clearCache = () => {
  localStorage.removeItem('cache')
  notify.success('缓存已清除')
}

const showUserAgreement = () => {
  notify.info('用户协议即将推出')
}

const showPrivacyPolicy = () => {
  notify.info('隐私政策即将推出')
}

const checkUpdate = () => {
  notify.info('已是最新版本')
}

onMounted(() => {
  currentLanguage.value = 'zh'

  const savedNotifications = localStorage.getItem('notifications')
  if (savedNotifications) {
    notificationSettings.value = JSON.parse(savedNotifications)
  }

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
  }
}

@media (min-width: 1024px) {
  .settings-page {
    width: 70%;
  }
}

.settings-header {
  display: flex;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #e0e0e0;
  margin-bottom: 20px;
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  margin-right: 15px;
  color: #333;
}

.settings-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.settings-content {
  padding: 0 20px;
}

.setting-section {
  margin-bottom: 30px;
}

.setting-section h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 15px;
  color: #333;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.setting-item span {
  font-size: 16px;
  color: #333;
}

.setting-item .version {
  color: #999;
  font-size: 14px;
}

.language-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
}

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
  border-radius: 24px;
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
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #9d2929;
}

input:checked + .slider:before {
  transform: translateX(26px);
}

.round {
  border-radius: 24px;
}
</style>
