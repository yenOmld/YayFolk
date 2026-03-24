<template>
  <div class="personal-center">
    <!-- 个人信息概览 -->
    <div class="profile-section">
      <div class="avatar-container">
        <img :src="userInfo.avatar" alt="Avatar" class="avatar">
        <div class="edit-avatar-btn" @click="navigateToEditProfile">
          <i class='bx bxs-camera'></i>
        </div>
      </div>
      <h2>{{ userInfo.nickname }}</h2>
      <p class="username">{{ userInfo.username }}</p>
      <div class="profile-stats">
        <div class="stat-item">
          <span class="stat-value">{{ stats.posts }}</span>
          <span class="stat-label">{{ $t('personal.posts') }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.collections }}</span>
          <span class="stat-label">{{ $t('personal.collections') }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.history }}</span>
          <span class="stat-label">{{ $t('personal.history') }}</span>
        </div>
      </div>
    </div>

    <!-- 功能菜单 -->
    <div class="menu-section">
      <!-- 个人信息 -->
      <div class="menu-group">
        <h3>{{ $t('personal.personalInfo') }}</h3>
        <div class="menu-item" @click="navigateToEditProfile">
          <i class='bx bxs-user'></i>
          <span>{{ $t('personal.editProfile') }}</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
        <div class="menu-item" @click="showChangePassword">
          <i class='bx bxs-lock-alt'></i>
          <span>{{ $t('personal.changePassword') }}</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
      </div>

      <!-- 我的内容 -->
      <div class="menu-group">
        <h3>{{ $t('personal.myContent') }}</h3>
        <div class="menu-item" @click="navigateToMyPosts">
          <i class='bx bxs-edit-alt'></i>
          <span>{{ $t('personal.myPosts') }}</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
        <div class="menu-item" @click="navigateToMyCollections">
          <i class='bx bxs-star'></i>
          <span>{{ $t('personal.myCollections') }}</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
        <div class="menu-item" @click="navigateToHistory">
          <i class='bx bxs-history'>
            <svg  xmlns="http://www.w3.org/2000/svg" width="18" height="18"  
              fill="#7494ec" viewBox="2 2 20 20" >
              <path d="M21.21 8.11c-.25-.59-.56-1.16-.92-1.7-.36-.53-.77-1.03-1.22-1.48s-.95-.86-1.48-1.22c-.54-.36-1.11-.67-1.7-.92-.6-.26-1.24-.45-1.88-.58-1.31-.27-2.72-.27-4.03 0-.64.13-1.27.33-1.88.58-.59.25-1.16.56-1.7.92-.53.36-1.03.77-1.48 1.22-.17.17-.32.35-.48.52L1.99 3v6h6L5.86 6.87c.15-.18.31-.36.48-.52.36-.36.76-.69 1.18-.98.43-.29.89-.54 1.36-.74.48-.2.99-.36 1.5-.47 1.05-.21 2.18-.21 3.23 0 .51.11 1.02.26 1.5.47.47.2.93.45 1.36.74.42.29.82.62 1.18.98s.69.76.98 1.18c.29.43.54.89.74 1.36.2.48.36.99.47 1.5.11.53.16 1.07.16 1.61a7.85 7.85 0 0 1-.63 3.11c-.2.47-.45.93-.74 1.36-.29.42-.62.82-.98 1.18s-.76.69-1.18.98c-.43.29-.89.54-1.36.74-.48.2-.99.36-1.5.47-1.05.21-2.18.21-3.23 0a8 8 0 0 1-1.5-.47c-.47-.2-.93-.45-1.36-.74-.42-.29-.82-.62-1.18-.98s-.69-.76-.98-1.18c-.29-.43-.54-.89-.74-1.36-.2-.48-.36-.99-.47-1.5A8 8 0 0 1 3.99 12h-2c0 .68.07 1.35.2 2.01.13.64.33 1.27.58 1.88.25.59.56 1.16.92 1.7.36.53.77 1.03 1.22 1.48s.95.86 1.48 1.22c.54.36 1.11.67 1.7.92.6.26 1.24.45 1.88.58.66.13 1.33.2 2.01.2s1.36-.07 2.01-.2c.64-.13 1.27-.33 1.88-.58.59-.25 1.16-.56 1.7-.92.53-.36 1.03-.77 1.48-1.22s.86-.95 1.22-1.48c.36-.54.67-1.11.92-1.7.26-.6.45-1.24.58-1.88.13-.66.2-1.34.2-2.01s-.07-1.35-.2-2.01c-.13-.64-.33-1.27-.58-1.88Z"></path><path d="M11 7v6h6v-2h-4V7z"></path>
            </svg>
          </i>
          <span>{{ $t('personal.browseHistory') }}</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
      </div>

      <!-- 应用设置 -->
      <div class="menu-group">
        <h3>{{ $t('personal.appSettings') }}</h3>
        <div class="menu-item" @click="navigateToSettings">
          <i class='bx bxs-cog'></i>
          <span>{{ $t('personal.settings') }}</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
      </div>

      <!-- 关于我们 -->
      <div class="menu-group">
        <h3>{{ $t('personal.aboutUs') }}</h3>
        <div class="menu-item" @click="showAbout">
          <i class='bx bxs-info-circle'></i>
          <span>{{ $t('personal.about') }}</span>
          <i class='bx bxs-chevron-right'></i>
        </div>
      </div>

      <!-- 退出登录 -->
      <div class="logout-section">
        <button class="logout-btn" @click="showLogoutConfirm">
          <i class='bx bxs-log-out'></i>
          {{ $t('personal.logout') }}
        </button>
        <button class="delete-account-btn" @click="showDeleteConfirm">
          <i class='bx bxs-user-x'></i>
          {{ $t('personal.deleteAccount') }}
        </button>
      </div>
    </div>

    <!-- 退出登录确认弹窗 -->
    <div class="modal" v-if="showLogoutModal">
      <div class="modal-content">
        <h3>{{ $t('personal.logout') }}</h3>
        <p>{{ $t('personal.logoutConfirm') }}</p>
        <div class="modal-buttons">
          <button class="btn secondary" @click="showLogoutModal = false">{{ $t('common.cancel') }}</button>
          <button class="btn primary" @click="handleLogout">{{ $t('common.confirm') }}</button>
        </div>
      </div>
    </div>
    <div class="modal" v-if="showDeleteModal">
      <div class="modal-content">
        <h3>{{ $t('personal.deleteAccount') }}</h3>
        <p>{{ $t('personal.deleteConfirm') }}</p>
        <div class="modal-buttons">
          <button class="btn secondary" @click="showDeleteModal = false">{{ $t('common.cancel') }}</button>
          <button class="btn danger" @click="handleDeleteAccount">{{ $t('common.confirm') }}</button>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div class="modal" v-if="showPasswordModal">
      <div class="modal-content">
        <h3>{{ $t('personal.changePassword') }}</h3>
        <div class="form-group">
          <label>{{ $t('personal.oldPassword') }}</label>
          <input 
            type="password" 
            v-model="passwordForm.oldPassword" 
            :placeholder="$t('personal.oldPassword')"
          />
        </div>
        <div class="form-group">
          <label>{{ $t('personal.newPassword') }}</label>
          <input 
            type="password" 
            v-model="passwordForm.newPassword" 
            placeholder="8-20"
          />
          <span v-if="passwordErrors.newPassword" class="error-text">{{ passwordErrors.newPassword }}</span>
        </div>
        <div class="form-group">
          <label>{{ $t('personal.confirmNewPassword') }}</label>
          <input 
            type="password" 
            v-model="passwordForm.confirmPassword" 
            :placeholder="$t('personal.confirmNewPassword')"
          />
          <span v-if="passwordErrors.confirmPassword" class="error-text">{{ passwordErrors.confirmPassword }}</span>
        </div>
        <div class="modal-buttons">
          <button class="btn secondary" @click="closePasswordModal">{{ $t('common.cancel') }}</button>
          <button class="btn primary" @click="submitPasswordChange">{{ $t('common.confirm') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getMyDiscoverStats } from '../api/app'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const { t } = useI18n()
const showLogoutModal = ref(false)

// 修改密码弹窗
const showPasswordModal = ref(false)

// 密码表单
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码错误信息
const passwordErrors = ref({
  newPassword: '',
  confirmPassword: ''
})

// 用户信息
const userInfo = ref({
  nickname: '用户12345',
  username: 'user@example.com',
  avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=user'
})
const stats = ref({
  posts: 0,
  collections: 0,
  history: 0
})

// 本地默认头像（用于网络不可用时的回退）
const localDefaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI0MCIgZmlsbD0iIzc0OTRlYyIvPgogIDxjaXJjbGUgY3g9IjUwIiBjeT0iMzAiIHI9IjIwIiBmaWxsPSIjNzQ5NGVjIi8+CiAgPGNpcmNsZSBjeD0iNTUiIGN5PSI0NSIgcj0iNSIgZmlsbD0iI2ZmZiIvPgogIDxjaXJjbGUgY3g9IjQ1IiBjeT0iNDUiIHI9IjUiIGZpbGw9IiNmZmYiLz4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjYwIiByPSIyIiBmaWxsPSIjNzQ5NGVjIi8+CiAgPHRleHQgeD0iNTAiIHk9Ijc1IiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTIiIGZpbGw9IiMzMzMiIG5hbWU9ImNvbnRlbnQiPldyb3lhbC4uLjwvdGV4dD4KPC9zdmc+'

// 导航到编辑个人资料
const navigateToEditProfile = () => {
  router.push('/personal/edit-profile')
}

// 导航到我的发布
const navigateToMyPosts = () => {
  router.push('/personal/my-posts')
}

// 导航到我的收藏
const navigateToMyCollections = () => {
  router.push('/personal/my-collections')
}

// 导航到浏览历史
const navigateToHistory = () => {
  router.push('/personal/history')
}

// 导航到设置
const navigateToSettings = () => {
  router.push('/personal/settings')
}

// 显示修改密码
const showChangePassword = () => {
  showPasswordModal.value = true
  resetPasswordForm()
}

// 关闭修改密码弹窗
const closePasswordModal = () => {
  showPasswordModal.value = false
  resetPasswordForm()
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  passwordErrors.value = {
    newPassword: '',
    confirmPassword: ''
  }
}

// 验证新密码格式
const validatePassword = (password) => {
  if (!password) {
    return '请输入新密码'
  }
  if (password.length < 8 || password.length > 20) {
    return '密码长度应为8-20位'
  }
  if (!/[a-zA-Z]/.test(password)) {
    return '密码必须包含字母'
  }
  if (!/[0-9]/.test(password)) {
    return '密码必须包含数字'
  }
  return ''
}

// 验证确认密码
const validateConfirmPassword = (password, confirmPassword) => {
  if (!confirmPassword) {
    return '请再次输入新密码'
  }
  if (password !== confirmPassword) {
    return '两次输入的密码不一致'
  }
  return ''
}

// 提交修改密码
const submitPasswordChange = async () => {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value

  if (!oldPassword) {
    notify.warning(t('personal.enterOldPassword'))
    return
  }

  const newPasswordError = validatePassword(newPassword)
  if (newPasswordError) {
    passwordErrors.value.newPassword = newPasswordError
    return
  }
  passwordErrors.value.newPassword = ''

  const confirmPasswordError = validateConfirmPassword(newPassword, confirmPassword)
  if (confirmPasswordError) {
    passwordErrors.value.confirmPassword = confirmPasswordError
    return
  }
  passwordErrors.value.confirmPassword = ''

  try {
    const response = await fetch('/api/user/change-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        oldPassword,
        newPassword,
        confirmPassword
      })
    })

    const data = await response.json()

    if (data.code === 200) {
      notify.success(t('personal.passwordChangeSuccess'))
      closePasswordModal()
    } else {
      notify.error(data.message || t('personal.passwordChangeFailed'))
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    notify.error(t('personal.networkError'))
  }
}

// 显示关于我们
const showAbout = () => {
  notify.info(t('personal.aboutUsContent'))
}

// 显示退出登录确认
const showLogoutConfirm = () => {
  showLogoutModal.value = true
}

const showDeleteModal = ref(false)
const showDeleteConfirm = () => {
  showDeleteModal.value = true
}

// 处理退出登录
const handleLogout = () => {
  // 清除本地存储
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  // 跳转到登录页
  router.push('/login')
}

const handleDeleteAccount = async () => {
  try {
    showDeleteModal.value = false
    const response = await fetch('/api/user/account', {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const data = await response.json()
    if (data.code === 200) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      notify.success(t('personal.accountDeleted'))
      router.push('/login')
    } else {
      notify.error(data.message || t('personal.deleteAccountFailed'))
    }
  } catch (error) {
    notify.error(t('personal.networkError'))
  }
}

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

// 页面加载时获取用户信息
onMounted(async () => {
  try {
    // 从本地存储获取用户信息
    const userStr = localStorage.getItem('user')
    if (userStr) {
      try {
        const user = JSON.parse(userStr)
        // 确保avatar有默认值
        user.avatar = user.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=user'
        userInfo.value = user
        
        // 检查头像是否可访问，如果不可访问则使用本地默认头像
        try {
          const isAccessible = await checkAvatarAccessibility(user.avatar)
          if (!isAccessible) {
            userInfo.value.avatar = localDefaultAvatar
          }
        } catch (error) {
          console.error('检查头像可访问性失败:', error)
          userInfo.value.avatar = localDefaultAvatar
        }
      } catch (error) {
        console.error('解析用户信息失败:', error)
        // 解析失败，使用默认值
        userInfo.value.avatar = localDefaultAvatar
      }
    } else {
      // 本地存储中没有用户信息，使用默认值
      userInfo.value.avatar = localDefaultAvatar
    }

    try {
      const response = await getMyDiscoverStats()
      if (response.code === 200 && response.data) {
        stats.value = {
          posts: response.data.posts || 0,
          collections: response.data.collections || 0,
          history: response.data.history || 0
        }
      }
    } catch (error) {
      console.error('获取统计数据失败:', error)
      stats.value = {
        posts: 0,
        collections: 0,
        history: 0
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    // 使用默认值
    userInfo.value.avatar = localDefaultAvatar
    stats.value = {
      posts: 0,
      collections: 0,
      history: 0
    }
  }
})
</script>

<style scoped>
.personal-center {
  min-height: 100vh;
  //background-color: #f5f5f5;
  padding: 20px;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

@media (min-width: 768px) {
  .personal-center {
    width: 85%;
  }
}

@media (min-width: 1024px) {
  .personal-center {
    width: 70%;
  }
}

.profile-section {
  background: white;
  border-radius: 16px;
  padding: 30px;
  text-align: center;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.avatar-container {
  position: relative;
  display: inline-block;
  margin-bottom: 20px;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #7494ec;
}

.edit-avatar-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #7494ec;
  color: white;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px solid white;
}

.profile-section h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  font-weight: 600;
}

.username {
  color: #666;
  margin: 0 0 20px 0;
}

.profile-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  display: block;
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}

.menu-section {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  margin-bottom: 80px;
}

.menu-group {
  border-bottom: 1px solid #eee;
}

.menu-group:last-child {
  border-bottom: none;
}

.menu-group h3 {
  padding: 15px 20px;
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #666;
  background: #f9f9f9;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.menu-item:hover {
  background-color: #f5f5f5;
}

.menu-item i {
  font-size: 20px;
  color: #7494ec;
  margin-right: 15px;
  width: 24px;
  text-align: center;
}

.menu-item span {
  flex: 1;
  color: #333;
}

.menu-item .bx-chevron-right {
  color: #999;
  margin-right: 0;
}

.logout-section {
  padding: 20px 0;
}

.logout-btn {
  width: 60%;
  padding: 15px;
  background: #ff4757;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.logout-btn:hover {
  background: #ff3742;
}

.logout-btn i {
  margin-right: 10px;
  font-size: 18px;
}

.delete-account-btn {
  width: 60%;
  padding: 12px;
  background: #c0392b;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 12px auto 0;
}

.delete-account-btn:hover {
  background: #a93226;
}

.btn.danger {
  background: #c0392b;
  color: #fff;
}
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 30px;
  width: 90%;
  max-width: 400px;
  text-align: center;
}

.modal-content h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  font-weight: 600;
}

.modal-content p {
  margin: 0 0 20px 0;
  color: #666;
}

.modal-buttons {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn.secondary {
  background: #f0f0f0;
  color: #333;
}

.btn.primary {
  background: #7494ec;
  color: white;
}

.btn:hover {
  opacity: 0.9;
}

/* 表单样式 */
.form-group {
  margin-bottom: 20px;
  text-align: left;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-group input:focus {
  border-color: #7494ec;
}

.form-group input::placeholder {
  color: #999;
}

.error-text {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #ff4d4f;
}
</style>
