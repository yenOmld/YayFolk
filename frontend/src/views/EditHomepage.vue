<template>
  <div class="shop-info-page">
    <div class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <i class='bx bxs-chevron-left'></i>
      </button>
      <div class="page-title">
        <p class="page-eyebrow">SHOP PROFILE</p>
        <h1>店铺信息</h1>
        <p>这里维护商家主页展示使用的店铺资料，不属于系统设置。</p>
      </div>
      <button class="save-btn" @click="saveShopInfo" :disabled="isSaving">
        {{ isSaving ? '保存中...' : '保存' }}
      </button>
    </div>

    <div v-if="!isMerchantRole" class="empty-card">
      <i class='bx bx-store-alt'></i>
      <h2>当前账号不是商家</h2>
      <p>店铺信息页仅对商家账号开放。</p>
    </div>

    <template v-else>
      <section class="panel">
        <div class="panel-head">
          <h2>基础信息</h2>
          <span>这些信息会展示在商家主页与店铺入口。</span>
        </div>

        <div class="form-grid">
          <div class="form-item full-span">
            <label for="shop-name">店铺名称</label>
            <input
              id="shop-name"
              v-model.trim="form.shopName"
              type="text"
              maxlength="100"
              placeholder="请输入店铺名称"
            >
          </div>

          <div class="form-item full-span">
            <label for="shop-intro">店铺简介</label>
            <textarea
              id="shop-intro"
              v-model.trim="form.shopIntro"
              rows="6"
              maxlength="500"
              placeholder="介绍你的店铺、非遗项目、服务特色和线下体验亮点"
            ></textarea>
            <span class="field-count">{{ form.shopIntro.length }}/500</span>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <h2>展示图片</h2>
          <span>店铺封面和主页背景图会用于商家主页视觉展示。</span>
        </div>

        <div class="media-grid">
          <article class="media-card">
            <div class="media-head">
              <strong>店铺封面</strong>
              <span>适合展示店铺空间或主打项目</span>
            </div>
            <div class="media-preview">
              <img v-if="form.shopCover" :src="form.shopCover" alt="店铺封面预览">
              <div v-else class="media-placeholder">
                <i class='bx bx-image-add'></i>
                <span>暂无店铺封面</span>
              </div>
            </div>
            <div class="media-actions">
              <button class="secondary-btn" @click="triggerShopCoverInput">上传图片</button>
              <button class="ghost-btn danger" @click="clearImage('shopCover')" :disabled="!form.shopCover">清空</button>
            </div>
            <input
              ref="shopCoverInput"
              type="file"
              accept="image/*"
              class="hidden-input"
              @change="handleImageChange($event, 'shopCover')"
            >
          </article>

          <article class="media-card">
            <div class="media-head">
              <strong>主页背景图</strong>
              <span>适合用作商家主页顶部大图</span>
            </div>
            <div class="media-preview">
              <img v-if="form.coverPhoto" :src="form.coverPhoto" alt="主页背景图预览">
              <div v-else class="media-placeholder">
                <i class='bx bx-landscape'></i>
                <span>暂无主页背景图</span>
              </div>
            </div>
            <div class="media-actions">
              <button class="secondary-btn" @click="triggerCoverPhotoInput">上传图片</button>
              <button class="ghost-btn danger" @click="clearImage('coverPhoto')" :disabled="!form.coverPhoto">清空</button>
            </div>
            <input
              ref="coverPhotoInput"
              type="file"
              accept="image/*"
              class="hidden-input"
              @change="handleImageChange($event, 'coverPhoto')"
            >
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getHomepageSettings, updateHomepageSettings, uploadImage } from '../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const shopCoverInput = ref(null)
const coverPhotoInput = ref(null)
const isSaving = ref(false)
const uploadFiles = reactive({
  shopCover: null,
  coverPhoto: null
})
const form = reactive({
  role: '',
  shopName: '',
  shopIntro: '',
  shopCover: '',
  coverPhoto: ''
})

const isMerchantRole = computed(() => ['merchant', 'admin'].includes(form.role))

const readStoredUser = () => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    console.error('读取本地用户信息失败:', error)
    return null
  }
}

const syncStoredUser = (payload = {}) => {
  const user = readStoredUser()
  if (!user) {
    return
  }

  const nextUser = {
    ...user,
    ...payload
  }

  localStorage.setItem('user', JSON.stringify(nextUser))
  localStorage.setItem('userInfo', JSON.stringify(nextUser))
}

const applySettings = (settings = {}) => {
  form.role = settings.role || form.role || readStoredUser()?.role || ''
  form.shopName = settings.shopName || ''
  form.shopIntro = settings.shopIntro || ''
  form.shopCover = settings.shopCover || ''
  form.coverPhoto = settings.coverPhoto || ''
}

const goBack = () => {
  router.back()
}

const triggerShopCoverInput = () => {
  shopCoverInput.value?.click()
}

const triggerCoverPhotoInput = () => {
  coverPhotoInput.value?.click()
}

const handleImageChange = (event, field) => {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  if (!file.type.startsWith('image/')) {
    notify.warning('请选择图片文件')
    event.target.value = ''
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    notify.warning('图片大小不能超过 10MB')
    event.target.value = ''
    return
  }

  uploadFiles[field] = file
  form[field] = URL.createObjectURL(file)
  event.target.value = ''
}

const clearImage = (field) => {
  uploadFiles[field] = null
  form[field] = ''
}

const uploadImageIfNeeded = async (field) => {
  const file = uploadFiles[field]
  if (!file) {
    return form[field] || ''
  }

  const formData = new FormData()
  formData.append('file', file)
  const response = await uploadImage(formData, 'homepage')
  if (response.code !== 200 || !response.data?.url) {
    throw new Error(response.message || '图片上传失败')
  }
  uploadFiles[field] = null
  return response.data.url
}

const saveShopInfo = async () => {
  if (!isMerchantRole.value) {
    return
  }

  if (!form.shopName.trim()) {
    notify.warning('请输入店铺名称')
    return
  }

  isSaving.value = true
  try {
    const [shopCover, coverPhoto] = await Promise.all([
      uploadImageIfNeeded('shopCover'),
      uploadImageIfNeeded('coverPhoto')
    ])

    const response = await updateHomepageSettings({
      shopName: form.shopName.trim(),
      shopIntro: form.shopIntro.trim(),
      shopCover,
      coverPhoto
    })

    if (response.code !== 200 || !response.data) {
      notify.error(response.message || '保存店铺信息失败')
      return
    }

    applySettings(response.data)
    syncStoredUser({
      shopName: response.data.shopName || '',
      shopIntro: response.data.shopIntro || '',
      shopCover: response.data.shopCover || '',
      coverPhoto: response.data.coverPhoto || ''
    })
    notify.success('店铺信息已保存')
  } catch (error) {
    console.error('保存店铺信息失败:', error)
    notify.error(error.message || '保存店铺信息失败，请稍后重试')
  } finally {
    isSaving.value = false
  }
}

onMounted(async () => {
  const user = readStoredUser()
  if (user) {
    form.role = user.role || ''
  }

  try {
    const response = await getHomepageSettings()
    if (response.code === 200 && response.data) {
      applySettings(response.data)
    }
  } catch (error) {
    console.error('获取店铺信息失败:', error)
  }
})
</script>

<style scoped>
.shop-info-page {
  min-height: 100vh;
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px 20px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background:
    radial-gradient(circle at top left, rgba(201, 122, 40, 0.1), transparent 24%),
    linear-gradient(180deg, #f8f4ec 0%, #fbfaf7 100%);
}

.page-header,
.panel-head,
.media-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header {
  gap: 16px;
  padding: 20px 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #eadfce;
  box-shadow: 0 18px 36px rgba(58, 69, 88, 0.08);
}

.back-btn,
.save-btn,
.secondary-btn,
.ghost-btn {
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.back-btn {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  background: #f4ede4;
  color: #6b4f2b;
  font-size: 20px;
}

.page-title {
  flex: 1;
}

.page-eyebrow {
  margin: 0 0 8px;
  color: #a06a2b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.page-title h1 {
  margin: 0;
  color: #243447;
  font-size: 30px;
}

.page-title p {
  margin: 8px 0 0;
  color: #7a8699;
  line-height: 1.7;
}

.save-btn {
  padding: 11px 18px;
  background: linear-gradient(135deg, #c97a28, #e5a548);
  color: white;
  font-size: 14px;
  font-weight: 700;
}

.save-btn:disabled,
.secondary-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.panel,
.empty-card {
  padding: 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #eadfce;
  box-shadow: 0 18px 36px rgba(58, 69, 88, 0.08);
}

.panel-head {
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.panel-head h2 {
  margin: 0;
  color: #243447;
  font-size: 20px;
}

.panel-head span {
  color: #8b98ab;
  font-size: 13px;
  line-height: 1.6;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.full-span {
  grid-column: 1 / -1;
}

.form-item label {
  color: #4a5568;
  font-size: 14px;
  font-weight: 600;
}

.form-item input,
.form-item textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #d9dee7;
  border-radius: 12px;
  background: #fffdf9;
  color: #243447;
  font-size: 14px;
  box-sizing: border-box;
  resize: vertical;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-item input:focus,
.form-item textarea:focus {
  outline: none;
  border-color: #c97a28;
  box-shadow: 0 0 0 3px rgba(201, 122, 40, 0.14);
}

.field-count {
  display: block;
  text-align: right;
  color: #98a2b3;
  font-size: 12px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.media-card {
  padding: 16px;
  border: 1px solid #edf0f5;
  border-radius: 18px;
  background: #fafbfd;
}

.media-head strong,
.media-head span {
  display: block;
}

.media-head strong {
  color: #243447;
  font-size: 15px;
}

.media-head span {
  margin-top: 4px;
  color: #98a2b3;
  font-size: 12px;
}

.media-preview {
  margin-top: 14px;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 14px;
  overflow: hidden;
  background: #eef2f7;
}

.media-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #98a2b3;
}

.media-placeholder i {
  font-size: 30px;
}

.media-actions {
  gap: 10px;
  margin-top: 14px;
}

.secondary-btn,
.ghost-btn {
  flex: 1;
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 700;
}

.secondary-btn {
  background: #edf2ff;
  color: #4b67d1;
}

.ghost-btn {
  background: #f3f4f6;
  color: #5b6472;
}

.ghost-btn.danger {
  background: #fff1f1;
  color: #d9485f;
}

.save-btn:hover:not(:disabled),
.secondary-btn:hover:not(:disabled),
.ghost-btn:hover:not(:disabled),
.back-btn:hover {
  transform: translateY(-1px);
}

.empty-card {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #7a8699;
}

.empty-card i {
  font-size: 42px;
  margin-bottom: 12px;
  color: #c97a28;
}

.empty-card h2 {
  margin: 0;
  color: #243447;
  font-size: 22px;
}

.empty-card p {
  margin: 10px 0 0;
}

.hidden-input {
  display: none;
}

@media (max-width: 900px) {
  .form-grid,
  .media-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .shop-info-page {
    padding: 14px 12px 32px;
  }

  .page-header,
  .panel {
    padding: 18px;
    border-radius: 20px;
  }

  .page-header,
  .panel-head,
  .media-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .save-btn {
    width: 100%;
  }
}
</style>
