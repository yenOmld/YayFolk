<template>
  <div class="activity-editor-page">


    <div class="editor-shell">
      <div class="editor-header">
        <div>
          <h1>{{ isEdit ? '编辑活动' : '创建活动' }}</h1>
          <p>保存的活动内容将同步到公共活动列表并重新提交审核。</p>
        </div>
      </div>

      <div v-if="loading" class="state-card">
        <i class="bx bx-loader-alt bx-spin"></i>
        <p>加载中...</p>
      </div>

      <template v-else>
        <div class="editor-main">
          <section class="form-section form-section-basic">
            <h2>基本信息</h2>
            <div class="form-grid">
              <label class="field full">
                <span>活动标题</span>
                <input v-model.trim="form.title" type="text" maxlength="200" placeholder="例如：缫丝织品体验课" />
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
                <input v-model="form.startTime" type="datetime-local" :min="minStartTime" />
              </label>

              <label class="field">
                <span>结束时间</span>
                <input v-model="form.endTime" type="datetime-local" :min="minEndTime" />
              </label>

              <label class="field">
                <span>价格 (CNY)</span>
                <input v-model.number="form.priceYuan" type="number" min="0" step="0.01" placeholder="0 表示免费" />
              </label>

              <label class="field">
                <span>人数上限</span>
                <input v-model.number="form.maxParticipants" type="number" min="1" placeholder="留空表示不限" />
              </label>

              <label class="field">
                <span>省份</span>
                <input v-model.trim="form.locationProvince" type="text" placeholder="例如：广东省" />
              </label>

              <label class="field">
                <span>城市</span>
                <input v-model.trim="form.locationCity" type="text" placeholder="例如：深圳市" />
              </label>

              <label class="field full">
                <span>详细地址</span>
                <input v-model.trim="form.locationDetail" type="text" maxlength="200" placeholder="请输入活动的详细地址" />
              </label>
            </div>
          </section>

          <section class="form-section form-section-content">
            <h2>活动介绍</h2>
            <label class="field full">
              <span>详细内容</span>
              <textarea
                v-model.trim="form.content"
                rows="10"
                placeholder="请填写活动亮点、流程、适合人群、注意事项等"
              ></textarea>
            </label>
          </section>
        </div>

        <aside class="editor-rail">
          <section class="form-section form-section-media">
            <div class="section-head">
              <div>
                <h2>媒体素材</h2>
                <p class="hint">图片和视频在同一列表中展示，可以重新排序。封面默认为第一张图片，也可以手动设置。</p>
              </div>
              <div class="section-actions">
                <button class="ghost-btn" type="button" @click="triggerImageUpload" :disabled="uploadingImages || mediaItems.length >= MAX_MEDIA_ITEMS">
                  {{ uploadingImages ? '上传中...' : '上传图片' }}
                </button>
                <button class="ghost-btn" type="button" @click="triggerVideoUpload" :disabled="uploadingVideo || hasVideo">
                  {{ uploadingVideo ? '上传中...' : (hasVideo ? '已上传视频' : '上传视频') }}
                </button>
              </div>
              <input ref="imageInput" type="file" accept="image/*" multiple hidden @change="handleImageUpload" />
              <input ref="videoInput" type="file" accept="video/*" hidden @change="handleVideoUpload" />
            </div>

            <div v-if="mediaItems.length" class="media-grid">
              <article
                v-for="(item, index) in mediaItems"
                :key="item.id"
                class="media-card"
                :class="{ cover: isCoverMedia(item.id) }"
              >
                <div class="media-frame">
                  <template v-if="item.type === 'video'">
                    <video class="media-preview" :src="item.previewUrl" controls preload="metadata" playsinline :poster="coverPreviewUrl || ''"></video>
                  </template>
                  <template v-else>
                    <img class="media-preview" :src="item.previewUrl" :alt="`活动媒体${index + 1}`" />
                  </template>
                </div>

                <div class="media-badges">
                  <span class="type-badge" :class="item.type">{{ item.type === 'video' ? '视频' : '图片' }}</span>
                  <span v-if="isCoverMedia(item.id)" class="cover-badge">封面</span>
                </div>

                <div class="media-actions">
                  <button type="button" class="icon-btn" :disabled="index === 0" @click="moveMedia(index, -1)">
                    <i class="bx bx-chevron-up"></i>
                  </button>
                  <button type="button" class="icon-btn" :disabled="index === mediaItems.length - 1" @click="moveMedia(index, 1)">
                    <i class="bx bx-chevron-down"></i>
                  </button>
                  <button v-if="item.type === 'image'" type="button" class="text-btn" @click="setCoverByIndex(index)">设为封面</button>
                  <button type="button" class="danger-text-btn" @click="removeMedia(index)">删除</button>
                </div>
              </article>
            </div>

            <div v-else class="media-empty">
              <i class="bx bx-image-add"></i>
              <p>点击上方按钮上传图片或视频</p>
            </div>
          </section>

          <section class="form-section form-section-model">
            <div class="section-head">
              <div>
                <h2>VR模型 / 3D视图</h2>
                <p class="hint">仅支持 .glb 或 .gltf 格式。上传后可在活动详情页进行360°预览。</p>
              </div>
              <div class="section-actions">
                <button class="ghost-btn" type="button" @click="triggerModelUpload" :disabled="uploadingModel">
                  {{ uploadingModel ? '上传中...' : (hasModel ? '替换模型' : '上传模型') }}
                </button>
              </div>
              <input ref="modelInput" type="file" accept=".glb,.gltf,model/gltf-binary,model/gltf+json" hidden @change="handleModelUpload" />
            </div>

            <div class="model-card">
              <div class="model-art">
                <i class="bx bx-cube-alt"></i>
              </div>
              <div class="model-copy">
                <strong>{{ vrModelName || '暂未上传模型' }}</strong>
                <p>{{ vrModelUrl || '上传轻量化3D模型后将自动同步到活动详情页。' }}</p>
              </div>
            </div>
          </section>

          <div class="action-row">
            <button class="secondary-btn" type="button" @click="goBack">取消</button>
            <button class="primary-btn" type="button" :disabled="saving" @click="submitForm">
              {{ saving ? '保存中...' : (isEdit ? '保存并重新提交' : '创建并提交') }}
            </button>
          </div>
        </aside>
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
  uploadActivityImage,
  uploadActivityVideo,
  uploadModel
} from '../../api/app'
import { getRequestErrorMessage } from '../../utils/requestError'
import { isVideoUrl, normalizeMediaList } from '../../utils/media'

const route = useRoute()
const router = useRouter()
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const heritageTypes = ['工艺', '刺绣', '雕塑', '器具', '编织', '纺织', '绘画', '剪纸', '陶瓷', '中医', '服饰', '其他']
const MAX_MEDIA_ITEMS = 10

const imageInput = ref(null)
const videoInput = ref(null)
const modelInput = ref(null)
const loading = ref(false)
const uploadingImages = ref(false)
const uploadingVideo = ref(false)
const uploadingModel = ref(false)
const saving = ref(false)
const mediaItems = ref([])
const selectedCoverId = ref('')
const vrModelUrl = ref('')
const vrModelName = ref('')

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
  endTime: ''
})

const isEdit = computed(() => Boolean(route.params.id))
const currentDateTimeInput = () => {
  const now = new Date()
  now.setSeconds(0, 0)
  const offset = now.getTimezoneOffset()
  return new Date(now.getTime() - offset * 60000).toISOString().slice(0, 16)
}
const minStartTime = computed(() => currentDateTimeInput())
const minEndTime = computed(() => form.value.startTime || minStartTime.value)
const hasVideo = computed(() => mediaItems.value.some(item => item.type === 'video'))
const coverPreviewUrl = computed(() => mediaItems.value.find(item => item.id === selectedCoverId.value)?.previewUrl || '')
const hasModel = computed(() => Boolean(vrModelUrl.value))

const showError = (message) => notify?.error?.(message)
const showSuccess = (message) => notify?.success?.(message)
const showWarning = (message) => notify?.warning?.(message)

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
  const list = normalizeMediaList(value)
  return list.length ? list : (coverImage ? [coverImage] : [])
}

const toInputDateTime = (value) => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  const offset = date.getTimezoneOffset()
  return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 16)
}

const goBack = () => {
  router.push('/merchant/activities')
}

const triggerImageUpload = () => imageInput.value?.click()
const triggerVideoUpload = () => videoInput.value?.click()
const triggerModelUpload = () => modelInput.value?.click()

const createMediaItem = ({ type, previewUrl, file = null, uploadedUrl = '' }) => ({
  id: `${type}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  type,
  previewUrl,
  file,
  uploadedUrl
})

const isCoverMedia = (mediaId) => selectedCoverId.value === mediaId

const pickDefaultCover = () => {
  selectedCoverId.value = mediaItems.value.find(item => item.type === 'image')?.id || ''
}

const setCoverByIndex = (index) => {
  const item = mediaItems.value[index]
  if (item?.type === 'image') {
    selectedCoverId.value = item.id
  }
}

const moveMedia = (index, direction) => {
  const target = index + direction
  if (target < 0 || target >= mediaItems.value.length) return
  const items = [...mediaItems.value]
  const [moved] = items.splice(index, 1)
  items.splice(target, 0, moved)
  mediaItems.value = items
}

const removeMedia = (index) => {
  const item = mediaItems.value[index]
  if (!item) return
  if (item.previewUrl?.startsWith('blob:')) {
    URL.revokeObjectURL(item.previewUrl)
  }
  mediaItems.value.splice(index, 1)
  if (item.id === selectedCoverId.value) {
    pickDefaultCover()
  }
}

const rebuildMediaFromCurrent = (current) => {
  const combined = parseImages(current.images, current.coverImage)
  if (current.videoUrl && !combined.some(url => String(url) === String(current.videoUrl))) {
    combined.push(current.videoUrl)
  }
  mediaItems.value = combined.map(url => createMediaItem({
    type: isVideoUrl(url) ? 'video' : 'image',
    previewUrl: url,
    uploadedUrl: url
  }))
  selectedCoverId.value = mediaItems.value.find(item => item.type === 'image' && item.previewUrl === current.coverImage)?.id
    || mediaItems.value.find(item => item.type === 'image')?.id
    || ''
  vrModelUrl.value = current.vrModelUrl || ''
  vrModelName.value = current.vrModelUrl ? String(current.vrModelUrl).split('/').pop() : ''
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

    rebuildMediaFromCurrent(current)
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
      endTime: toInputDateTime(current.endTime)
    }
  } catch (error) {
    showError(error.message || '加载活动失败')
    goBack()
  } finally {
    loading.value = false
  }
}

const handleModelUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  const name = String(file.name || '').toLowerCase()
  if (!name.endsWith('.glb') && !name.endsWith('.gltf')) {
    showWarning('请上传 .glb 或 .gltf 格式的3D模型')
    event.target.value = ''
    return
  }
  if (file.size > 100 * 1024 * 1024) {
    showWarning('3D模型大小不能超过 100 MB')
    event.target.value = ''
    return
  }

  uploadingModel.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const rawId = route.params.id
    const activityId = (isEdit.value && rawId) ? rawId : Date.now()
    console.log('VR上传 - isEdit:', isEdit.value, ', route.params.id:', rawId, ', activityId:', activityId)
    const response = await uploadModel(formData, activityId)
    if (response.code !== 200 || !response.data?.url) {
      throw new Error(response.message || '3D模型上传失败')
    }
    vrModelUrl.value = response.data.url
    vrModelName.value = file.name
    showSuccess('3D模型上传成功')
  } catch (error) {
    showError(error.message || '3D模型上传失败')
  } finally {
    uploadingModel.value = false
    event.target.value = ''
  }
}

const handleImageUpload = (event) => {
  const files = Array.from(event.target.files || [])
  if (!files.length) return

  const remaining = Math.max(0, MAX_MEDIA_ITEMS - mediaItems.value.length)
  const selected = files.slice(0, remaining)
  if (!selected.length) {
    event.target.value = ''
    return
  }

  uploadingImages.value = true
  try {
    selected.forEach(file => {
      const previewUrl = URL.createObjectURL(file)
      mediaItems.value.push(createMediaItem({
        type: 'image',
        previewUrl,
        file
      }))
    })
    if (!selectedCoverId.value) {
      pickDefaultCover()
    }
    showSuccess(selected.length > 1 ? `成功添加 ${selected.length} 张图片。` : '图片添加成功。')
  } finally {
    uploadingImages.value = false
    event.target.value = ''
  }
}

const handleVideoUpload = (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('video/')) {
    showWarning('请选择视频文件')
    event.target.value = ''
    return
  }
  if (file.size > 100 * 1024 * 1024) {
    showWarning('视频大小不能超过 100 MB')
    event.target.value = ''
    return
  }

  uploadingVideo.value = true
  try {
    const previewUrl = URL.createObjectURL(file)
    const existingIndex = mediaItems.value.findIndex(item => item.type === 'video')
    if (existingIndex >= 0) {
      const oldItem = mediaItems.value[existingIndex]
      if (oldItem.previewUrl?.startsWith('blob:')) {
        URL.revokeObjectURL(oldItem.previewUrl)
      }
      mediaItems.value[existingIndex] = createMediaItem({
        type: 'video',
        previewUrl,
        file
      })
    } else {
      mediaItems.value.push(createMediaItem({
        type: 'video',
        previewUrl,
        file
      }))
    }
    showSuccess('视频添加成功')
  } finally {
    uploadingVideo.value = false
    event.target.value = ''
  }
}

const uploadMediaFiles = async (activityId) => {
  const uploadedImages = []
  let uploadedVideo = null

  for (const item of mediaItems.value) {
    if (!item.file) {
      if (item.type === 'image') {
        uploadedImages.push(item.previewUrl)
      } else if (item.type === 'video') {
        uploadedVideo = item.previewUrl
      }
      continue
    }

    const formData = new FormData()
    formData.append('file', item.file)
    const uploadFn = item.type === 'video' ? uploadActivityVideo : uploadActivityImage

    const response = await uploadFn(formData, activityId, uploadedImages.length + 1)
    if (response.code !== 200 || !response.data?.url) {
      throw new Error(response.message || `${item.type === 'video' ? '视频' : '图片'}上传失败`)
    }

    if (item.type === 'image') {
      uploadedImages.push(response.data.url)
    } else {
      uploadedVideo = response.data.url
    }

    URL.revokeObjectURL(item.previewUrl)
  }

  return { uploadedImages, uploadedVideo }
}

const submitForm = async () => {
  if (!form.value.title || !form.value.startTime || !form.value.endTime) {
    showWarning('请填写活动标题、开始时间和结束时间')
    return
  }
  if (!form.value.content) {
    showWarning('请填写活动描述')
    return
  }
  const imageItems = mediaItems.value.filter(item => item.type === 'image')
  if (!imageItems.length) {
    showWarning('请至少上传一张活动图片')
    return
  }

  const startTime = new Date(form.value.startTime)
  const endTime = new Date(form.value.endTime)
  const now = new Date(currentDateTimeInput())

  if (startTime.getTime() < now.getTime()) {
    showWarning('开始时间不能早于当前时间')
    return
  }
  if (endTime.getTime() <= startTime.getTime()) {
    showWarning('结束时间必须晚于开始时间')
    return
  }

  saving.value = true
  try {
    const activityId = isEdit.value ? route.params.id : Date.now()
    const { uploadedImages, uploadedVideo } = await uploadMediaFiles(activityId)
    
    const coverImage = uploadedImages.find((url, index) => {
      const mediaIndex = mediaItems.value.findIndex(item => item.type === 'image' && item.previewUrl === url)
      return mediaItems.value[mediaIndex]?.id === selectedCoverId.value
    }) || uploadedImages[0]

    const payload = {
      title: form.value.title,
      subtitle: form.value.subtitle || '',
      content: form.value.content,
      coverImage,
      images: JSON.stringify(uploadedImages),
      videoUrl: uploadedVideo || null,
      videoCoverUrl: coverImage || null,
      vrModelUrl: vrModelUrl.value || null,
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
  if (user.role !== 'merchant') {
    showWarning('只有商家可以管理活动')
    router.push('/merchant/apply')
    return
  }
  loadActivity()
})
</script>

<style scoped>
.activity-editor-page {
  min-height: 100vh;
  background: linear-gradient(180deg, rgba(248, 244, 238, 0.3), rgba(255, 255, 255, 0.95));
  padding: 18px 0 100px;
}



.editor-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 20px;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 16px;
  align-items: start;
}

.editor-header,
.form-section,
.state-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #eadfd4;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
  padding: 22px;
}

.editor-header,
.state-card {
  grid-column: 1 / -1;
}

.editor-main,
.editor-rail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.editor-rail {
  position: sticky;
  top: 92px;
}

.editor-header h1,
.form-section h2 {
  margin: 0;
  color: #2f241d;
}

.editor-header p,
.hint {
  margin: 8px 0 0;
  color: #7b6a59;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
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

label span {
  font-weight: 600;
  color: #2f241d;
}

input,
select,
textarea {
  width: 100%;
  border: 1px solid #e7ddd3;
  border-radius: 14px;
  padding: 13px 15px;
  font-size: 15px;
  background: #fff;
  color: #2f241d;
  box-sizing: border-box;
}

textarea {
  min-height: 150px;
  resize: vertical;
}

.section-head {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.section-actions {
  display: flex;
  gap: 10px;
}

.model-card {
  margin-top: 16px;
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(182, 92, 56, 0.08), rgba(233, 205, 170, 0.16));
}

.model-art {
  width: 64px;
  height: 64px;
  flex: 0 0 64px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.85);
  color: #b65c38;
  font-size: 28px;
}

.model-copy {
  min-width: 0;
}

.model-copy strong,
.model-copy p {
  display: block;
  margin: 0;
}

.model-copy strong {
  color: #2f241d;
}

.model-copy p {
  margin-top: 6px;
  color: #7b6a59;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.ghost-btn,
.secondary-btn,
.primary-btn,
.danger-text-btn,
.text-btn,
.icon-btn {
  border: none;
  cursor: pointer;
}

.ghost-btn {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
}

.media-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 14px;
}

.media-card {
  position: relative;
  border: 1px solid #eadfd4;
  border-radius: 18px;
  padding: 10px;
  background: #fff;
}

.media-card.cover {
  border-color: #9d2929;
  box-shadow: 0 0 0 2px rgba(157, 41, 41, 0.1);
}

.media-frame {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
}

.media-preview {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
  display: block;
}

.media-badges {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
}

.type-badge,
.cover-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.type-badge.image {
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
}

.type-badge.video {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
}

.cover-badge {
  background: rgba(157, 41, 41, 0.12);
  color: #9d2929;
}

.media-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #f7f2e8;
  color: #2f241d;
}

.icon-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.text-btn,
.danger-text-btn {
  border-radius: 999px;
  padding: 8px 12px;
}

.text-btn {
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
}

.danger-text-btn {
  background: rgba(239, 68, 68, 0.1);
  color: #b91c1c;
}

.media-empty,
.state-card {
  text-align: center;
}

.media-empty {
  margin-top: 18px;
  padding: 28px 20px;
  border: 1px dashed #d7c9bb;
  border-radius: 18px;
  color: #7b6a59;
  background: #fff;
}

.media-empty i,
.state-card i {
  font-size: 28px;
  color: #b65c38;
}

.action-row {
  display: flex;
  gap: 12px;
}

.secondary-btn,
.primary-btn {
  flex: 1;
  padding: 14px 18px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
}

.secondary-btn {
  background: #fff;
  color: #2f241d;
  border: 1px solid #e7ddd3;
}

.primary-btn {
  background: linear-gradient(135deg, #b65c38 0%, #d28d44 100%);
  color: #fff;
  box-shadow: 0 8px 20px rgba(182, 92, 56, 0.25);
}

.primary-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .page-header,
  .editor-shell {
    padding-left: 14px;
    padding-right: 14px;
  }

  .editor-shell {
    grid-template-columns: 1fr;
  }

  .editor-rail {
    position: static;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .section-head {
    flex-direction: column;
  }

  .section-actions,
  .action-row {
    width: 100%;
  }

  .secondary-btn,
  .primary-btn {
    width: 100%;
  }
}
</style>