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
          <p>活动信息会同步到前台活动列表，保存后重新进入审核。</p>
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
              <input v-model.trim="form.subtitle" type="text" maxlength="100" placeholder="一句话概括活动亮点" />
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
              <input v-model.trim="form.locationProvince" type="text" placeholder="例如：浙江省" />
            </label>

            <label class="field">
              <span>城市</span>
              <input v-model.trim="form.locationCity" type="text" placeholder="例如：杭州市" />
            </label>

            <label class="field full">
              <span>详细地址</span>
              <input v-model.trim="form.locationDetail" type="text" maxlength="200" placeholder="请输入详细活动地点" />
            </label>
          </div>
        </section>

        <section class="form-section">
          <div class="section-head">
            <h2>活动图片</h2>
            <button class="ghost-btn" type="button" @click="triggerUpload" :disabled="uploading || form.images.length >= 9">
              {{ uploading ? '上传中...' : '上传图片' }}
            </button>
            <input ref="uploadInput" type="file" accept="image/*" multiple hidden @change="handleUpload" />
          </div>
          <p class="hint">最多 9 张，第一张会作为封面图。</p>

          <div class="image-grid">
            <div v-for="(image, index) in form.images" :key="`${image}-${index}`" class="image-card">
              <img :src="image" :alt="`活动图${index + 1}`" />
              <div class="image-mask">
                <span v-if="index === 0" class="cover-tag">封面</span>
                <button type="button" class="icon-btn" @click="removeImage(index)">删除</button>
              </div>
            </div>
            <button
              v-if="form.images.length < 9"
              type="button"
              class="upload-card"
              @click="triggerUpload"
            >
              <i class="bx bx-image-add"></i>
              <span>添加图片</span>
            </button>
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
  uploadImage
} from '../../api/app'

const route = useRoute()
const router = useRouter()
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const heritageTypes = ['刺绣', '剪纸', '陶瓷', '漆器', '雕刻', '京剧', '昆曲', '古琴', '太极', '中医', '年画', '其他']

const uploadInput = ref(null)
const loading = ref(false)
const uploading = ref(false)
const saving = ref(false)
const activityMeta = ref(null)

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
  images: []
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

const triggerUpload = () => {
  uploadInput.value?.click()
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
      images: parseImages(current.images, current.coverImage)
    }
  } catch (error) {
    showError(error.message || '加载活动失败')
    goBack()
  } finally {
    loading.value = false
  }
}

const handleUpload = async (event) => {
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

  uploading.value = true
  try {
    for (const file of selected) {
      const formData = new FormData()
      formData.append('file', file)
      const response = await uploadImage(formData, 'activities')
      if (response.code !== 200 || !response.data?.url) {
        throw new Error(response.message || '图片上传失败')
      }
      form.value.images.push(response.data.url)
    }
    showSuccess('图片上传成功')
  } catch (error) {
    showError(error.message || '图片上传失败')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

const removeImage = (index) => {
  form.value.images.splice(index, 1)
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
  align-items: center;
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
.primary-btn {
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

.upload-card i {
  font-size: 32px;
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
  .action-row {
    flex-direction: column;
    align-items: stretch;
  }

  .secondary-btn,
  .primary-btn {
    width: 100%;
  }
}
</style>
