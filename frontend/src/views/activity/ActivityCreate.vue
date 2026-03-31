<template>
  <div class="activity-editor-page">
    <div class="header-nav">
      <button class="back-button" @click="goBack">
        <i class="bx bx-arrow-back"></i>
        <span>返回活动管理</span>
      </button>
    </div>

    <div class="editor-shell">
      <div class="editor-header">
        <div>
          <h1>{{ isEdit ? '编辑活动' : '创建活动' }}</h1>
          <p>活动信息会同步到前台活动列表，保存后会重新进入审核流程。</p>
        </div>
        <div v-if="isEdit && activityMeta" class="audit-chip" :class="activityMeta.auditStatus">
          {{ auditLabel(activityMeta.auditStatus) }}
        </div>
      </div>

      <div v-if="loading" class="state-card">
        <i class="bx bx-loader-alt bx-spin"></i>
        <p>加载中...</p>
      </div>

      <template v-else>
        <section class="form-section">
          <h2>基础信息</h2>
          <div class="form-grid">
            <label class="field full">
              <span>活动标题</span>
              <input v-model.trim="form.title" type="text" maxlength="200" placeholder="例如：苏绣体验课" />
            </label>

            <label class="field full">
              <span>副标题</span>
              <input v-model.trim="form.subtitle" type="text" maxlength="100" placeholder="用一句话概括活动亮点" />
            </label>

            <label class="field">
              <span>非遗类别</span>
              <select v-model="form.heritageType">
                <option value="">请选择</option>
                <option v-for="item in heritageTypes" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>

            <label class="field">
              <span>活动形式</span>
              <select v-model="form.activityType">
                <option value="offline">线下体验</option>
                <option value="online">线上课程</option>
                <option value="exhibition">展览</option>
              </select>
            </label>

            <label class="field">
              <span>开始时间</span>
              <input v-model="form.startTime" type="datetime-local" />
            </label>

            <label class="field">
              <span>结束时间</span>
              <input v-model="form.endTime" type="datetime-local" />
            </label>

            <label class="field">
              <span>价格（元）</span>
              <input v-model.number="form.priceYuan" type="number" min="0" step="0.01" placeholder="0 表示免费" />
            </label>

            <label class="field">
              <span>人数上限</span>
              <input v-model.number="form.maxParticipants" type="number" min="1" placeholder="留空表示不限" />
            </label>

            <label class="field">
              <span>省份</span>
              <input v-model.trim="form.locationProvince" type="text" placeholder="例如：江苏省" />
            </label>

            <label class="field">
              <span>城市</span>
              <input v-model.trim="form.locationCity" type="text" placeholder="例如：苏州市" />
            </label>

            <label class="field full">
              <span>详细地址</span>
              <input v-model.trim="form.locationDetail" type="text" maxlength="200" placeholder="请输入详细活动地点" />
            </label>
          </div>
        </section>

        <section class="form-section">
          <div class="section-head">
            <div>
              <h2>活动图片</h2>
              <p class="hint">最多 9 张，第一张会作为封面图。</p>
            </div>
            <button class="ghost-btn" type="button" @click="triggerImageUpload" :disabled="uploadingImages || form.images.length >= 9">
              {{ uploadingImages ? '上传中...' : '上传图片' }}
            </button>
            <input ref="imageInput" type="file" accept="image/*" multiple hidden @change="handleImageUpload" />
          </div>

          <div class="image-grid">
            <div v-for="(image, index) in form.images" :key="`${image}-${index}`" class="image-card">
              <img :src="image" :alt="`活动图片${index + 1}`" />
              <div class="image-mask">
                <span v-if="index === 0" class="cover-tag">封面</span>
                <button type="button" class="icon-btn" @click="removeImage(index)">删除</button>
              </div>
            </div>
            <button v-if="form.images.length < 9" type="button" class="upload-card" @click="triggerImageUpload">
              <i class="bx bx-image-add"></i>
              <span>添加图片</span>
            </button>
          </div>
        </section>

        <section class="form-section">
          <div class="section-head">
            <div>
              <h2>活动视频</h2>
              <p class="hint">可选，上传后会保存视频 URL 到 `video_url`，详情页会直接播放。</p>
            </div>
            <button class="ghost-btn" type="button" @click="triggerVideoUpload" :disabled="uploadingVideo">
              {{ uploadingVideo ? '上传中...' : (form.videoUrl ? '更换视频' : '上传视频') }}
            </button>
            <input ref="videoInput" type="file" accept="video/*" hidden @change="handleVideoUpload" />
          </div>

          <div v-if="form.videoUrl" class="video-preview-card">
            <video
              class="video-preview"
              controls
              preload="metadata"
              playsinline
              :poster="form.videoCoverUrl || form.images[0] || ''"
            >
              <source :src="form.videoUrl" />
            </video>
            <div class="video-preview-actions">
              <span class="video-url">{{ form.videoUrl }}</span>
              <button type="button" class="danger-text-btn" @click="removeVideo">移除视频</button>
            </div>
          </div>

          <div v-else class="video-empty">
            <i class="bx bx-video-plus"></i>
            <span>还没有上传活动视频</span>
          </div>
        </section>

        <section class="form-section">
          <h2>活动介绍</h2>
          <label class="field full">
            <span>详情内容</span>
            <textarea
              v-model.trim="form.content"
              rows="10"
              placeholder="请填写活动亮点、流程、适合人群、注意事项等"
            ></textarea>
          </label>
        </section>

        <div class="action-row">
          <button class="secondary-btn" type="button" @click="goBack">取消</button>
          <button class="primary-btn" type="button" :disabled="saving" @click="submitForm">
            {{ saving ? '保存中...' : (isEdit ? '保存并重新提交审核' : '创建并提交审核') }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createMerchantActivity,
  getMerchantActivities,
  updateMerchantActivity,
  uploadImage,
  uploadVideo
} from '../../api/app'
import { getRequestErrorMessage } from '../../utils/requestError'

const route = useRoute()
const router = useRouter()
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const heritageTypes = ['刺绣', '剪纸', '陶瓷', '漆器', '雕刻', '京剧', '昆曲', '古琴', '太极', '中医', '年画', '其他']

const imageInput = ref(null)
const videoInput = ref(null)
const loading = ref(false)
const uploadingImages = ref(false)
const uploadingVideo = ref(false)
const saving = ref(false)
const activityMeta = ref(null)
const IMAGE_UPLOAD_BATCH_SIZE = 3

const form = ref({
  title: '',
  subtitle: '',
  content: '',
  heritageType: '',
  activityType: 'offline',
  locationProvince: '',
  locationCity: '',
  locationDetail: '',
  priceYuan: 0,
  maxParticipants: null,
  startTime: '',
  endTime: '',
  images: [],
  videoUrl: '',
  videoCoverUrl: ''
})

const isEdit = computed(() => Boolean(route.params.id))

const showError = (message) => notify?.error?.(message) ?? window.alert(message)
const showSuccess = (message) => notify?.success?.(message) ?? window.alert(message)
const showWarning = (message) => notify?.warning?.(message) ?? window.alert(message)

const readStoredUser = () => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return {}
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    return {}
  }
}

const parseImages = (value, coverImage) => {
  if (Array.isArray(value)) {
    return value.filter(Boolean)
  }

  if (typeof value === 'string' && value) {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) {
        return parsed.filter(Boolean)
      }
    } catch (error) {
      return coverImage ? [coverImage] : []
    }
  }

  return coverImage ? [coverImage] : []
}

const toInputDateTime = (value) => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  const offset = date.getTimezoneOffset()
  return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 16)
}

const auditLabel = (status) => ({
  pending: '待审核',
  approved: '已通过',
  rejected: '已驳回'
}[status] || '待提交')

const goBack = () => {
  router.push('/merchant/activities')
}

const triggerImageUpload = () => {
  imageInput.value?.click()
}

const triggerVideoUpload = () => {
  videoInput.value?.click()
}

const loadActivity = async () => {
  if (!isEdit.value) {
    return
  }

  loading.value = true
  try {
    const response = await getMerchantActivities()
    if (response.code !== 200) {
      throw new Error(response.message || '加载活动失败')
    }

    const current = (response.data || []).find(item => String(item.id) === String(route.params.id))
    if (!current) {
      throw new Error('未找到该活动')
    }

    activityMeta.value = current
    form.value = {
      title: current.title || '',
      subtitle: current.subtitle || '',
      content: current.content || '',
      heritageType: current.heritageType || '',
      activityType: current.activityType || 'offline',
      locationProvince: current.locationProvince || '',
      locationCity: current.locationCity || '',
      locationDetail: current.locationDetail || '',
      priceYuan: current.price ? Number(current.price) / 100 : 0,
      maxParticipants: current.maxParticipants ?? null,
      startTime: toInputDateTime(current.startTime),
      endTime: toInputDateTime(current.endTime),
      images: parseImages(current.images, current.coverImage),
      videoUrl: current.videoUrl || '',
      videoCoverUrl: current.videoCoverUrl || ''
    }
  } catch (error) {
    showError(error.message || '加载活动失败')
    goBack()
  } finally {
    loading.value = false
  }
}

const handleImageUpload = async (event) => {
  const files = Array.from(event.target.files || [])
  if (!files.length) {
    return
  }

  const remaining = Math.max(0, 9 - form.value.images.length)
  const selected = files.slice(0, remaining)
  if (!selected.length) {
    event.target.value = ''
    return
  }

  uploadingImages.value = true
  try {
    const uploadedUrls = []
    const failures = []
    for (let index = 0; index < selected.length; index += IMAGE_UPLOAD_BATCH_SIZE) {
      const batch = selected.slice(index, index + IMAGE_UPLOAD_BATCH_SIZE)
      const results = await Promise.allSettled(batch.map(async (file) => {
        const formData = new FormData()
        formData.append('file', file)
        const response = await uploadImage(formData, 'activities')
        if (response.code !== 200 || !response.data?.url) {
          throw new Error(response.message || '图片上传失败')
        }
        return response.data.url
      }))

      results.forEach((result) => {
        if (result.status === 'fulfilled') {
          uploadedUrls.push(result.value)
          return
        }

        failures.push(getRequestErrorMessage(result.reason, {
          timeoutMessage: '图片上传较慢，请稍等后再试',
          fallbackMessage: '图片上传失败'
        }))
      })
    }

    if (uploadedUrls.length) {
      form.value.images.push(...uploadedUrls)
    }

    if (!failures.length) {
      showSuccess(uploadedUrls.length > 1 ? `已上传 ${uploadedUrls.length} 张图片` : '图片上传成功')
      return
    }

    if (uploadedUrls.length) {
      showWarning(`已上传 ${uploadedUrls.length} 张图片，另有 ${failures.length} 张失败：${failures[0]}`)
      return
    }

    showError(failures[0] || '图片上传失败')
  } catch (error) {
    showError(getRequestErrorMessage(error, {
      timeoutMessage: '图片上传较慢，请稍等后再试',
      fallbackMessage: '图片上传失败'
    }))
  } finally {
    uploadingImages.value = false
    event.target.value = ''
  }
}

const handleVideoUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  if (file.size > 100 * 1024 * 1024) {
    showWarning('视频大小不能超过 100MB')
    event.target.value = ''
    return
  }

  uploadingVideo.value = true
  try {
    const uploadFormData = new FormData()
    uploadFormData.append('file', file)

    let uploadResponse
    try {
      uploadResponse = await uploadVideo(uploadFormData, 'activities/videos')
    } catch (error) {
      showError(getRequestErrorMessage(error, {
        timeoutMessage: '视频上传较慢，请稍等后再试',
        fallbackMessage: '视频上传失败'
      }))
      return
    }

    if (uploadResponse.code !== 200 || !uploadResponse.data?.url) {
      throw new Error(uploadResponse.message || '视频上传失败')
    }

    form.value.videoUrl = uploadResponse.data.url
    showSuccess('视频上传成功')
  } catch (error) {
    showError(getRequestErrorMessage(error, {
      timeoutMessage: '视频上传较慢，请稍等后再试',
      fallbackMessage: '视频上传失败'
    }))
  } finally {
    uploadingVideo.value = false
    event.target.value = ''
  }
}

const removeImage = (index) => {
  form.value.images.splice(index, 1)
}

const removeVideo = () => {
  form.value.videoUrl = ''
  form.value.videoCoverUrl = ''
}

const submitForm = async () => {
  if (!form.value.title || !form.value.startTime || !form.value.endTime) {
    showWarning('请填写活动标题、开始时间和结束时间')
    return
  }
  if (!form.value.content) {
    showWarning('请填写活动介绍')
    return
  }
  if (!form.value.images.length) {
    showWarning('请至少上传一张活动图片')
    return
  }
  if (new Date(form.value.endTime).getTime() <= new Date(form.value.startTime).getTime()) {
    showWarning('结束时间必须晚于开始时间')
    return
  }

  saving.value = true
  try {
    const payload = {
      title: form.value.title,
      subtitle: form.value.subtitle || '',
      content: form.value.content,
      coverImage: form.value.images[0],
      images: JSON.stringify(form.value.images),
      videoUrl: form.value.videoUrl || null,
      videoCoverUrl: form.value.videoCoverUrl || null,
      heritageType: form.value.heritageType || '',
      activityType: form.value.activityType,
      locationProvince: form.value.locationProvince || '',
      locationCity: form.value.locationCity || '',
      locationDetail: form.value.locationDetail || '',
      price: Math.round(Number(form.value.priceYuan || 0) * 100),
      maxParticipants: form.value.maxParticipants ? Number(form.value.maxParticipants) : null,
      startTime: form.value.startTime,
      endTime: form.value.endTime
    }

    const response = isEdit.value
      ? await updateMerchantActivity(route.params.id, payload)
      : await createMerchantActivity(payload)

    if (response.code !== 200) {
      throw new Error(response.message || '保存失败')
    }

    showSuccess(isEdit.value ? '活动已更新并重新提交审核' : '活动已创建，等待审核')
    goBack()
  } catch (error) {
    showError(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  const user = readStoredUser()
  if (user.role !== 'merchant' && user.role !== 'admin') {
    showWarning('只有商家可以管理活动')
    router.push('/merchant/apply')
    return
  }
  loadActivity()
})
</script>

<style scoped>
.activity-editor-page {
  max-width: 1080px;
}

.header-nav {
  margin-bottom: 18px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: 1px solid #d8dee9;
  border-radius: 12px;
  background: #fff;
  color: #334155;
  cursor: pointer;
}

.editor-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.editor-header,
.form-section,
.state-card,
.action-row {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  box-shadow: 0 10px 32px rgba(15, 23, 42, 0.05);
}

.editor-header,
.form-section,
.state-card {
  padding: 24px;
}

.editor-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.editor-header h1,
.form-section h2 {
  margin: 0;
  color: #0f172a;
}

.editor-header p,
.hint {
  margin: 8px 0 0;
  color: #64748b;
}

.audit-chip {
  padding: 8px 14px;
  border-radius: 999px;
  font-weight: 600;
  white-space: nowrap;
}

.audit-chip.pending {
  background: #fff7ed;
  color: #c2410c;
}

.audit-chip.approved {
  background: #ecfdf5;
  color: #047857;
}

.audit-chip.rejected {
  background: #fef2f2;
  color: #b91c1c;
}

.state-card {
  text-align: center;
  color: #64748b;
}

.state-card i {
  font-size: 36px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field.full {
  grid-column: 1 / -1;
}

.field span {
  color: #334155;
  font-weight: 600;
}

.field input,
.field select,
.field textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  background: #fff;
  font-size: 14px;
  outline: none;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  border-color: #2563eb;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.image-card,
.upload-card {
  position: relative;
  aspect-ratio: 1;
  border-radius: 16px;
  overflow: hidden;
}

.image-card {
  border: 1px solid #e5e7eb;
}

.image-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-mask {
  position: absolute;
  inset: auto 0 0 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px;
  background: linear-gradient(180deg, transparent, rgba(15, 23, 42, 0.76));
}

.cover-tag {
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  font-size: 12px;
}

.icon-btn,
.ghost-btn,
.secondary-btn,
.primary-btn,
.danger-text-btn {
  border: none;
  cursor: pointer;
  font-weight: 600;
}

.icon-btn {
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.ghost-btn {
  padding: 10px 14px;
  border-radius: 12px;
  background: #eff6ff;
  color: #1d4ed8;
}

.ghost-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.upload-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 2px dashed #bfdbfe;
  background: #f8fbff;
  color: #2563eb;
}

.upload-card i,
.video-empty i {
  font-size: 32px;
}

.video-preview-card,
.video-empty {
  margin-top: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  overflow: hidden;
  background: #f8fafc;
}

.video-preview {
  display: block;
  width: 100%;
  max-height: 420px;
  background: #020617;
}

.video-preview-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
}

.video-url {
  min-width: 0;
  color: #475569;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.danger-text-btn {
  flex-shrink: 0;
  background: transparent;
  color: #dc2626;
}

.video-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 160px;
  color: #64748b;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 18px 24px;
}

.secondary-btn,
.primary-btn {
  padding: 12px 18px;
  border-radius: 12px;
  min-width: 180px;
}

.secondary-btn {
  background: #f1f5f9;
  color: #334155;
}

.primary-btn {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
}

.primary-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .editor-header,
  .section-head,
  .action-row,
  .video-preview-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .secondary-btn,
  .primary-btn {
    width: 100%;
  }

  .video-url {
    white-space: normal;
    word-break: break-all;
  }
}
</style>
