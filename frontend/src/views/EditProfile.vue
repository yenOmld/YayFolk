<template>
  <div class="edit-profile-page">
    <div class="settings-header">
      <button class="back-btn" @click="goBack">
        <i class='bx bxs-chevron-left'></i>
      </button>
      <h1>{{ $t('editProfile.title') }}</h1>
      <button class="save-btn" @click="saveProfile" :disabled="isSaving">
        {{ isSaving ? $t('editProfile.saving') : $t('common.save') }}
      </button>
    </div>

    <div class="edit-content">
      <div class="avatar-section">
        <div class="avatar-container">
          <img :src="editForm.avatar" alt="Avatar" class="avatar">
          <div class="avatar-upload-btn" @click="triggerFileInput">
            <i class='bx bxs-camera'></i>
            <span>{{ $t('editProfile.changeAvatar') }}</span>
          </div>
          <input 
            type="file" 
            ref="fileInput" 
            accept="image/*" 
            @change="handleAvatarUpload"
            style="display: none;"
          >
        </div>
      </div>

      <div class="form-section">
        <div class="form-item">
          <label>{{ $t('editProfile.nickname') }}</label>
          <input 
            type="text" 
            v-model="editForm.nickname"
            :placeholder="$t('editProfile.nicknamePlaceholder')"
            maxlength="20"
          >
        </div>

        <div class="form-item">
          <label>{{ $t('editProfile.phone') }}</label>
          <input 
            type="text" 
            :value="editForm.phone || $t('editProfile.notSet')"
            disabled
          >
          <span class="hint">{{ $t('editProfile.notEditable') }}</span>
        </div>

        <div class="form-item">
          <label>{{ $t('editProfile.email') }}</label>
          <input 
            type="email" 
            :value="editForm.email || $t('editProfile.notSet')"
            disabled
          >
          <span class="hint">{{ $t('editProfile.notEditable') }}</span>
        </div>

        <div class="form-item">
          <label>{{ $t('editProfile.username') }}</label>
          <input 
            type="text" 
            :value="editForm.username"
            disabled
          >
          <span class="hint">{{ $t('editProfile.notEditable') }}</span>
        </div>

        <div class="form-item">
          <label>{{ $t('editProfile.country') }}</label>
          <input 
            type="text" 
            :value="editForm.country"
            disabled
          >
          <span class="hint">{{ $t('editProfile.autoDetected') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const { t } = useI18n()
const fileInput = ref(null)
const isSaving = ref(false)

// 编辑表单
const editForm = reactive({
  nickname: '',
  username: '',
  phone: '',
  email: '',
  country: '',
  avatar: ''
})

// 存储选择的文件（暂不上传）
const selectedFile = ref(null)

// 返回上一页
const goBack = () => {
  router.back()
}

// 触发文件输入
const triggerFileInput = () => {
  fileInput.value.click()
}

// 压缩图片
const compressImage = (base64String, maxWidth = 300, maxHeight = 300) => {
  return new Promise((resolve) => {
    const img = new Image()
    img.src = base64String
    img.onload = () => {
      const canvas = document.createElement('canvas')
      let width = img.width
      let height = img.height

      // 计算缩放比例
      if (width > height) {
        if (width > maxWidth) {
          height *= maxWidth / width
          width = maxWidth
        }
      } else {
        if (height > maxHeight) {
          width *= maxHeight / height
          height = maxHeight
        }
      }

      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, width, height)

      // 压缩为 JPEG，质量 0.8
      const compressedBase64 = canvas.toDataURL('image/jpeg', 0.8)
      resolve(compressedBase64)
    }
  })
}

// 处理头像上传
const handleAvatarUpload = (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    notify.warning(t('editProfile.selectImage'))
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    notify.warning(t('editProfile.imageSizeLimit'))
    return
  }

  // 存储文件但暂不上传
  selectedFile.value = file
  
  // 生成本地预览
  const reader = new FileReader()
  reader.onload = (e) => {
    editForm.avatar = e.target.result
  }
  reader.readAsDataURL(file)
  
  console.log('文件已选择，等待保存时上传')
}

// 保存个人资料
const saveProfile = async () => {
  if (!editForm.nickname.trim()) {
    notify.warning(t('editProfile.enterNickname'))
    return
  }

  isSaving.value = true

  try {
    let avatarUrl = editForm.avatar
    
    // 如果有新选择的文件，先上传到OSS
    if (selectedFile.value) {
      console.log('开始上传头像到 OSS...')
      const formData = new FormData()
      formData.append('file', selectedFile.value)
      
      // 添加上传进度监听和明确指定超时时间
      const uploadRes = await request.post('/upload/avatar', formData, {
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          console.log(`上传进度：${percentCompleted}%`)
        },
        timeout: 60000
      })
      
      if (uploadRes.code === 200) {
        avatarUrl = uploadRes.data.url
        console.log('头像上传成功:', avatarUrl)
      } else {
        notify.error('头像上传失败：' + uploadRes.message)
        isSaving.value = false
        return
      }
    }

    const updateData = {
      nickname: editForm.nickname,
      avatar: avatarUrl
    }

    console.log('更新用户资料:', updateData)
    const res = await request.put('/user/profile', updateData)

    if (res.code === 200) {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        user.nickname = editForm.nickname
        user.avatar = avatarUrl
        localStorage.setItem('user', JSON.stringify(user))
      }

      notify.success(t('editProfile.saveSuccess'))
      router.back()
    } else {
      notify.error(res.message || t('editProfile.saveFailed'))
    }
  } catch (error) {
    console.error('保存失败:', error)
    notify.error(t('editProfile.saveFailedRetry'))
  } finally {
    isSaving.value = false
  }
}

// 本地默认头像（用于网络不可用时的回退）
const localDefaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI0MCIgZmlsbD0iIzc0OTRlYyIvPgogIDxjaXJjbGUgY3g9IjUwIiBjeT0iMzAiIHI9IjIwIiBmaWxsPSIjNzQ5NGVjIi8+CiAgPGNpcmNsZSBjeD0iNTUiIGN5PSI0NSIgcj0iNSIgZmlsbD0iI2ZmZiIvPgogIDxjaXJjbGUgY3g9IjQ1IiBjeT0iNDUiIHI9IjUiIGZpbGw9IiNmZmYiLz4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjYwIiByPSIyIiBmaWxsPSIjNzQ5NGVjIi8+CiAgPHRleHQgeD0iNTAiIHk9Ijc1IiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTIiIGZpbGw9IiMzMzMiIG5hbWU9ImNvbnRlbnQiPldyb3lhbC4uLjwvdGV4dD4KPC9zdmc+'

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
    // 先从后端获取最新用户信息
    const res = await request.get('/user/profile')
    if (res.code === 200 && res.data) {
      const user = res.data
      editForm.nickname = user.nickname || ''
      editForm.username = user.username || ''
      editForm.phone = user.phone || ''
      editForm.email = user.email || ''
      editForm.country = user.country || ''
      editForm.avatar = user.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=user'
      
      // 更新 localStorage 中的用户信息
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const localUser = JSON.parse(userStr)
        localUser.nickname = user.nickname
        localUser.avatar = user.avatar
        localUser.country = user.country
        localUser.regionCode = user.regionCode
        localStorage.setItem('user', JSON.stringify(localUser))
      }
    } else {
      // 如果后端获取失败，使用 localStorage 的数据
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        editForm.nickname = user.nickname || ''
        editForm.username = user.username || ''
        editForm.phone = user.phone || ''
        editForm.email = user.email || ''
        editForm.country = user.country || ''
        editForm.avatar = user.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=user'
      }
    }
    
    // 检查头像是否可访问，如果不可访问则使用本地默认头像
    const isAccessible = await checkAvatarAccessibility(editForm.avatar)
    if (!isAccessible) {
      editForm.avatar = localDefaultAvatar
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    // 使用 localStorage 的数据作为回退
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      editForm.nickname = user.nickname || ''
      editForm.username = user.username || ''
      editForm.phone = user.phone || ''
      editForm.email = user.email || ''
      editForm.country = user.country || ''
      editForm.avatar = user.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=user'
    } else {
      editForm.avatar = localDefaultAvatar
    }
  }
})
</script>

<style scoped>
.edit-profile-page {
  min-height: 100vh;
  width: 100%;
  margin: 0 auto;
}

@media (min-width: 768px) {
  .edit-profile-page {
    width: 85%;
    margin: 0 auto;
  }
}

@media (min-width: 1024px) {
  .edit-profile-page {
    width: 70%;
    margin: 0 auto;
  }
}

.settings-header {
  background: white;
  padding: 15px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
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
}

.settings-header h1 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.save-btn {
  background: #7494ec;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.save-btn:hover:not(:disabled) {
  background: #6381d9;
}

.save-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.edit-content {
  padding: 20px;
}

.avatar-section {
  background: white;
  border-radius: 12px;
  padding: 30px;
  text-align: center;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.avatar-container {
  width: 180px;
  margin: 0 auto;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  object-position: center;
  border: 3px solid #7494ec;
  min-width: 120px;
  min-height: 120px;
}

.avatar-upload-btn {
  background: rgba(0,0,0,0.7);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 35px;
  transition: background-color 0.2s;
}

.avatar-upload-btn:hover {
  background: rgba(0,0,0,0.8);
}

.form-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.form-item {
  margin-bottom: 20px;
  position: relative;
}

.form-item:last-child {
  margin-bottom: 0;
}

.form-item label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  margin-bottom: 8px;
}

.form-item input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  transition: border-color 0.2s;
}

.form-item input:focus {
  outline: none;
  border-color: #7494ec;
}

.form-item input:disabled {
  background: #f5f5f5;
  color: #999;
}

.form-item .hint {
  position: absolute;
  right: 12px;
  top: 42px;
  font-size: 12px;
  color: #999;
}
</style>
