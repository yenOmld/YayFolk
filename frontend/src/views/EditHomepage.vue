<template>
  <div class="edit-homepage-page">
    <div class="settings-header">
      <button class="back-btn" @click="goBack">
        <i class='bx bxs-chevron-left'></i>
      </button>
      <h1>编辑个人主页</h1>
      <button class="save-btn" :disabled="saving" @click="saveHomepage">
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </div>

    <div class="page-content">
      <section class="preview-card">
        <div class="preview-cover" :style="coverStyle">
          <button class="cover-upload-btn" @click="triggerCoverInput">
            <i class='bx bx-image-add'></i>
            更换封面
          </button>
          <input
            ref="coverInput"
            type="file"
            accept="image/*"
            @change="handleCoverUpload"
            style="display: none"
          >
        </div>

        <div class="preview-main">
          <img :src="form.avatar || defaultAvatar" alt="avatar" class="preview-avatar" />
          <div class="preview-info">
            <h2>{{ form.nickname || 'YayFolk 用户' }}</h2>
            <p class="preview-handle">@{{ form.username || 'user' }}</p>
            <p class="preview-bio">{{ form.bio || '这里会展示你的主页简介。' }}</p>
            <div class="preview-meta">
              <span v-if="form.location"><i class='bx bx-map'></i>{{ form.location }}</span>
              <span v-if="form.isMerchant && form.shopName"><i class='bx bxs-store'></i>{{ form.shopName }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="form-card">
        <div class="section-title">
          <h3>主页展示信息</h3>
          <button class="secondary-btn" @click="previewHomepage">查看主页</button>
        </div>

        <div class="form-item">
          <label>主页简介</label>
          <textarea
            v-model="form.bio"
            maxlength="200"
            rows="4"
            placeholder="一句话介绍你自己，或者告诉别人你在分享什么。"
          ></textarea>
          <span class="hint">{{ form.bio.length }}/200</span>
        </div>

        <div class="form-item">
          <label>主页地点</label>
          <input
            v-model="form.location"
            maxlength="100"
            type="text"
            placeholder="例如：杭州 / 西安 / 景德镇"
          >
          <span class="hint">显示在主页头部，用于告诉别人你所在或活跃的地区。</span>
        </div>
      </section>

      <section class="form-card">
        <div class="section-title">
          <h3>{{ form.isMerchant ? '商家主页卡片' : '主页标题卡片' }}</h3>
        </div>

        <div class="form-item">
          <label>{{ form.isMerchant ? '商家名称' : '主页标题' }}</label>
          <input
            v-model="form.shopName"
            maxlength="100"
            type="text"
            :placeholder="form.isMerchant ? '例如：西湖纸伞工坊' : '例如：阿青的非遗手作日记'"
          >
        </div>

        <div class="form-item">
          <label>{{ form.isMerchant ? '商家介绍' : '卡片介绍' }}</label>
          <textarea
            v-model="form.shopIntro"
            maxlength="500"
            rows="5"
            :placeholder="form.isMerchant ? '介绍你的店铺、手艺、服务和特色内容。' : '补充你的主页介绍，比如擅长分享的内容方向。'"
          ></textarea>
          <span class="hint">{{ form.shopIntro.length }}/500</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getHomepageSettings, updateHomepageSettings, uploadImage } from '../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=travelate-user'
const saving = ref(false)
const coverInput = ref(null)

const form = reactive({
  id: null,
  username: '',
  nickname: '',
  avatar: '',
  role: 'user',
  bio: '',
  location: '',
  shopName: '',
  shopIntro: '',
  shopCover: '',
  isMerchant: false
})

const coverStyle = computed(() => {
  if (form.shopCover) {
    return { backgroundImage: `url(${form.shopCover})` }
  }
  return {
    backgroundImage: 'linear-gradient(135deg, #1f6feb 0%, #22c55e 100%)'
  }
})

const goBack = () => {
  router.back()
}

const triggerCoverInput = () => {
  coverInput.value?.click()
}

const handleCoverUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    notify.warning('请选择图片文件')
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    notify.warning('封面图片不能超过 10MB')
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', file)
    const response = await uploadImage(formData, 'homepage')
    if (response.code === 200 && response.data?.url) {
      form.shopCover = response.data.url
      notify.success('主页封面已更新')
    } else {
      notify.error(response.message || '上传封面失败')
    }
  } catch (error) {
    notify.error('上传封面失败，请稍后重试')
  } finally {
    event.target.value = ''
  }
}

const loadHomepageSettings = async () => {
  try {
    const response = await getHomepageSettings()
    if (response.code !== 200 || !response.data) {
      notify.error(response.message || '加载主页设置失败')
      return
    }

    Object.assign(form, {
      id: response.data.id || null,
      username: response.data.username || '',
      nickname: response.data.nickname || '',
      avatar: response.data.avatar || defaultAvatar,
      role: response.data.role || 'user',
      bio: response.data.bio || '',
      location: response.data.location || '',
      shopName: response.data.shopName || '',
      shopIntro: response.data.shopIntro || '',
      shopCover: response.data.shopCover || '',
      isMerchant: Boolean(response.data.isMerchant)
    })
  } catch (error) {
    notify.error('加载主页设置失败，请稍后重试')
  }
}

const saveHomepage = async () => {
  saving.value = true
  try {
    const payload = {
      bio: form.bio.trim(),
      location: form.location.trim(),
      shopName: form.shopName.trim(),
      shopIntro: form.shopIntro.trim(),
      shopCover: form.shopCover
    }

    const response = await updateHomepageSettings(payload)
    if (response.code !== 200) {
      notify.error(response.message || '保存主页失败')
      return
    }

    const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
    if (raw) {
      try {
        const storedUser = JSON.parse(raw)
        const nextUser = {
          ...storedUser,
          bio: payload.bio,
          location: payload.location,
          shopName: payload.shopName,
          shopIntro: payload.shopIntro,
          shopCover: payload.shopCover
        }
        localStorage.setItem('user', JSON.stringify(nextUser))
        localStorage.setItem('userInfo', JSON.stringify(nextUser))
      } catch (error) {
        console.error('更新本地主页数据失败', error)
      }
    }

    notify.success('主页装修已保存')
    previewHomepage()
  } catch (error) {
    notify.error('保存主页失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const previewHomepage = () => {
  if (!form.id) return
  router.push(`/user-homepage/${form.id}`)
}

onMounted(() => {
  loadHomepageSettings()
})
</script>

<style scoped>
.edit-homepage-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(34, 197, 94, 0.12), transparent 25%),
    radial-gradient(circle at top right, rgba(31, 111, 235, 0.14), transparent 30%),
    #f4f7fb;
}

.settings-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.back-btn,
.save-btn,
.secondary-btn,
.cover-upload-btn {
  border: none;
  cursor: pointer;
}

.back-btn {
  background: none;
  color: #16324f;
  font-size: 22px;
}

.settings-header h1 {
  margin: 0;
  flex: 1;
  text-align: center;
  font-size: 18px;
  color: #16324f;
}

.save-btn {
  padding: 10px 16px;
  border-radius: 999px;
  background: #16324f;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.page-content {
  max-width: 980px;
  margin: 0 auto;
  padding: 20px;
}

.preview-card,
.form-card {
  margin-bottom: 18px;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.preview-cover {
  height: 220px;
  position: relative;
  background-size: cover;
  background-position: center;
}

.cover-upload-btn {
  position: absolute;
  right: 18px;
  bottom: 18px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #fff;
  font-size: 14px;
}

.preview-main {
  margin-top: -52px;
  position: relative;
  z-index: 1;
  padding: 0 24px 24px;
  display: flex;
  gap: 18px;
  align-items: flex-end;
}

.preview-avatar {
  width: 104px;
  height: 104px;
  object-fit: cover;
  border-radius: 26px;
  border: 4px solid #fff;
  background: #fff;
}

.preview-info {
  flex: 1;
}

.preview-info h2 {
  margin: 0;
  font-size: 30px;
  color: #16324f;
}

.preview-handle {
  margin: 6px 0 0;
  color: #6b7a90;
  font-size: 14px;
}

.preview-bio {
  margin: 12px 0 0;
  color: #31445d;
  font-size: 15px;
  line-height: 1.7;
}

.preview-meta {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #6b7a90;
  font-size: 13px;
}

.preview-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.form-card {
  padding: 22px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.section-title h3 {
  margin: 0;
  color: #16324f;
  font-size: 18px;
}

.secondary-btn {
  padding: 10px 14px;
  border-radius: 999px;
  background: #eaf1f9;
  color: #1f6feb;
  font-size: 14px;
  font-weight: 600;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
}

.form-item:last-child {
  margin-bottom: 0;
}

.form-item label {
  font-size: 14px;
  font-weight: 600;
  color: #16324f;
}

.form-item input,
.form-item textarea {
  width: 100%;
  border: 1px solid #d7e1ec;
  border-radius: 16px;
  background: #f9fbfd;
  padding: 14px 16px;
  font-size: 14px;
  color: #22344d;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-item input:focus,
.form-item textarea:focus {
  border-color: #1f6feb;
  box-shadow: 0 0 0 3px rgba(31, 111, 235, 0.12);
}

.form-item textarea {
  resize: vertical;
  min-height: 110px;
}

.hint {
  color: #7b8aa0;
  font-size: 12px;
}

@media (max-width: 768px) {
  .settings-header h1 {
    font-size: 16px;
  }

  .page-content {
    padding: 14px;
  }

  .preview-cover {
    height: 160px;
  }

  .preview-main {
    flex-direction: column;
    align-items: flex-start;
    margin-top: -40px;
  }

  .preview-info h2 {
    font-size: 24px;
  }
}
</style>
