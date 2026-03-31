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
          <img :src="editForm.avatar || defaultAvatar" alt="Avatar" class="avatar">
          <div class="avatar-upload-btn" @click="triggerFileInput">
            <i class='bx bxs-camera'></i>
            <span>{{ $t('editProfile.changeAvatar') }}</span>
          </div>
          <input
            ref="fileInput"
            type="file"
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
            v-model="editForm.nickname"
            type="text"
            :placeholder="$t('editProfile.nicknamePlaceholder')"
            maxlength="20"
          >
        </div>

        <div class="form-item">
          <label>{{ $t('editProfile.signature') }}</label>
          <textarea
            v-model="editForm.signature"
            rows="3"
            maxlength="60"
            :placeholder="$t('editProfile.signaturePlaceholder')"
          ></textarea>
        </div>

        <div class="form-item">
          <label>{{ $t('editProfile.bio') }}</label>
          <textarea
            v-model="editForm.bio"
            rows="5"
            maxlength="200"
            :placeholder="$t('editProfile.bioPlaceholder')"
          ></textarea>
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
            :value="editForm.country || $t('editProfile.notSet')"
            disabled
          >
          <span class="hint">{{ $t('editProfile.autoDetected') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getHomepageSettings, getUserProfile, updateHomepageSettings, updateUserProfile } from '../api/app'
import request from '../utils/request'

const defaultAvatar = '/default-avatar.svg'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const { t } = useI18n()
const fileInput = ref(null)
const isSaving = ref(false)
const selectedFile = ref(null)

const editForm = reactive({
  nickname: '',
  username: '',
  phone: '',
  email: '',
  country: '',
  avatar: defaultAvatar,
  signature: '',
  bio: ''
})

const applyProfile = (profile = {}) => {
  editForm.nickname = profile.nickname || ''
  editForm.username = profile.username || ''
  editForm.phone = profile.phone || ''
  editForm.email = profile.email || ''
  editForm.country = profile.country || ''
  editForm.avatar = profile.avatar || defaultAvatar
}

const applyHomepageSettings = (settings = {}) => {
  editForm.signature = settings.signature || ''
  editForm.bio = settings.bio || ''
}

const syncLocalUser = (avatarUrl) => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return

  const user = JSON.parse(userStr)
  user.nickname = editForm.nickname.trim()
  user.avatar = avatarUrl
  user.signature = editForm.signature.trim()
  user.bio = editForm.bio.trim()
  localStorage.setItem('user', JSON.stringify(user))
}

const goBack = () => {
  router.back()
}

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleAvatarUpload = (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    notify.warning(t('editProfile.selectImage'))
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    notify.warning(t('editProfile.imageSizeLimit'))
    return
  }

  selectedFile.value = file
  const reader = new FileReader()
  reader.onload = ({ target }) => {
    editForm.avatar = target?.result || defaultAvatar
  }
  reader.readAsDataURL(file)
}

const uploadAvatarIfNeeded = async () => {
  if (!selectedFile.value) {
    return editForm.avatar || defaultAvatar
  }

  const formData = new FormData()
  formData.append('file', selectedFile.value)
  const uploadRes = await request.post('/upload/avatar', formData, { timeout: 60000 })
  if (uploadRes.code !== 200 || !uploadRes.data?.url) {
    throw new Error(uploadRes.message || 'upload avatar failed')
  }
  return uploadRes.data.url
}

const saveProfile = async () => {
  if (!editForm.nickname.trim()) {
    notify.warning(t('editProfile.enterNickname'))
    return
  }

  isSaving.value = true

  try {
    const avatarUrl = await uploadAvatarIfNeeded()
    const nickname = editForm.nickname.trim()
    const signature = editForm.signature.trim()
    const bio = editForm.bio.trim()

    const [profileRes, homepageRes] = await Promise.all([
      updateUserProfile({ nickname, avatar: avatarUrl }),
      updateHomepageSettings({ signature, bio })
    ])

    if (profileRes.code !== 200) {
      notify.error(profileRes.message || t('editProfile.saveFailed'))
      return
    }

    if (homepageRes.code !== 200) {
      notify.error(homepageRes.message || t('editProfile.saveFailed'))
      return
    }

    editForm.avatar = avatarUrl
    syncLocalUser(avatarUrl)
    notify.success(t('editProfile.saveSuccess'))
    router.back()
  } catch (error) {
    console.error('save profile failed:', error)
    notify.error(t('editProfile.saveFailedRetry'))
  } finally {
    isSaving.value = false
  }
}

onMounted(async () => {
  try {
    const [profileRes, homepageRes] = await Promise.all([
      getUserProfile(),
      getHomepageSettings()
    ])

    if (profileRes.code === 200 && profileRes.data) {
      applyProfile(profileRes.data)
    }

    if (homepageRes.code === 200 && homepageRes.data) {
      applyHomepageSettings(homepageRes.data)
    }
  } catch (error) {
    console.error('load profile failed:', error)
  }

  const userStr = localStorage.getItem('user')
  if (!userStr) return

  const user = JSON.parse(userStr)
  if (!editForm.nickname) editForm.nickname = user.nickname || ''
  if (!editForm.username) editForm.username = user.username || ''
  if (!editForm.phone) editForm.phone = user.phone || ''
  if (!editForm.email) editForm.email = user.email || ''
  if (!editForm.country) editForm.country = user.country || ''
  if (!editForm.avatar || editForm.avatar === defaultAvatar) editForm.avatar = user.avatar || defaultAvatar
  if (!editForm.signature) editForm.signature = user.signature || ''
  if (!editForm.bio) editForm.bio = user.bio || ''
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
  border-radius: 0 0 12px 12px;
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: background-color 0.2s;
}

.avatar-upload-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.form-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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

.form-item input,
.form-item textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  transition: border-color 0.2s;
  resize: vertical;
}

.form-item input:focus,
.form-item textarea:focus {
  outline: none;
  border-color: #ff2442;
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
