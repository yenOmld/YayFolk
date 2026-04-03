<template>
  <Teleport to="body">
    <div v-if="visible" class="activity-modal-shell">
      <div class="activity-modal-overlay" @click="emit('close')"></div>

      <div class="activity-modal-card" role="dialog" aria-modal="true" aria-label="Activity detail">
        <button class="close-button" type="button" @click="emit('close')">
          <i class="bx bx-x"></i>
        </button>

        <div v-if="loading" class="state-block">
          <i class="bx bx-loader-alt bx-spin"></i>
          <p>加载活动详情中...</p>
        </div>

        <div v-else-if="!detail" class="state-block">
          <i class="bx bx-calendar-x"></i>
          <p>活动详情不可用。</p>
        </div>

        <div v-else class="modal-layout">
          <div class="media-column">
            <div class="hero-image">
              <img :src="activeImage" :alt="detail.title || '活动封面'">
              <span class="status-pill" :class="statusTone">{{ statusLabel(detail.status || detail.auditStatus) }}</span>
            </div>

            <div v-if="galleryImages.length > 1" class="thumb-row">
              <button
                v-for="(image, index) in galleryImages"
                :key="`${image}-${index}`"
                class="thumb-item"
                type="button"
                :class="{ active: currentImageIndex === index }"
                @click="currentImageIndex = index"
              >
                <img :src="image" :alt="`活动图片 ${index + 1}`">
              </button>
            </div>

            <div v-if="videoUrl" class="content-panel">
              <div class="panel-head">
                <h3>活动视频</h3>
              </div>
              <video controls preload="metadata" playsinline class="video-player" :poster="videoPoster">
                <source :src="videoUrl">
              </video>
            </div>
          </div>

          <div class="info-column">
            <div class="headline-card">
              <div class="eyebrow-row">
                <span class="type-pill">{{ detail.heritageType || '未分类' }}</span>
                <span class="type-pill subtle">{{ activityTypeLabel(detail.activityType) }}</span>
              </div>
              <h2>{{ detail.title || '未命名活动' }}</h2>
              <p class="subtitle">{{ detail.subtitle || detail.content || '暂无活动简介。' }}</p>

              <div class="meta-grid">
                <div class="meta-item">
                  <i class="bx bx-calendar"></i>
                  <div>
                    <span class="meta-label">时间</span>
                    <strong>{{ formatDateRange(detail) }}</strong>
                  </div>
                </div>
                <div class="meta-item">
                  <i class="bx bx-map"></i>
                  <div>
                    <span class="meta-label">地点</span>
                    <strong>{{ formatLocation(detail) }}</strong>
                  </div>
                </div>
                <div class="meta-item">
                  <i class="bx bx-group"></i>
                  <div>
                    <span class="meta-label">人数</span>
                    <strong>{{ participantSummary }}</strong>
                  </div>
                </div>
              </div>

              <div class="action-bar">
                <div class="price-block">
                  <span class="price-label">价格</span>
                  <strong>{{ priceLabel }}</strong>
                </div>

                <button
                  class="primary-action"
                  type="button"
                  :disabled="!canBook"
                  @click="goToBooking"
                >
                  {{ bookingButtonText }}
                </button>
              </div>

              <div v-if="isMerchantOwner" class="owner-actions">
                <button class="secondary-action" type="button" @click="goEditActivity">编辑活动</button>
                <button class="secondary-action" type="button" @click="goMerchantBookings">预订管理</button>
              </div>
            </div>

            <div class="content-panel merchant-panel">
              <div class="panel-head">
                <h3>商家</h3>
              </div>
              <div class="merchant-row">
                <img class="merchant-avatar" :src="detail.merchantAvatar || placeholderAvatar" :alt="detail.merchantName || '商家'">
                <div class="merchant-copy">
                  <strong>{{ detail.merchantName || '商家' }}</strong>
                  <p>{{ detail.merchantIntro || '暂无商家介绍。' }}</p>
                </div>
              </div>
              <div v-if="detail.merchantId" class="merchant-actions">
                <button class="secondary-action" type="button" @click="contactMerchant">联系商家</button>
                <button class="secondary-action" type="button" @click="goMerchantHomepage">查看主页</button>
              </div>
            </div>

            <div class="content-panel">
              <div class="panel-head">
                <h3>活动描述</h3>
              </div>
              <p class="rich-text">{{ detail.content || detail.subtitle || '暂无描述。' }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, getCurrentInstance, onBeforeUnmount, watch, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicActivityDetail } from '@/api/app.js'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  activityId: {
    type: [String, Number],
    default: ''
  },
  initialDetail: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'loaded'])

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const route = useRoute()
const router = useRouter()

const placeholderAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=merchant'
const loading = ref(false)
const detail = ref(null)
const currentImageIndex = ref(0)
const currentUser = ref(readCurrentUser())

function readCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || localStorage.getItem('userInfo') || 'null') || {}
  } catch {
    return {}
  }
}

const currentUserId = computed(() => Number(currentUser.value?.id || 0))
const isMerchantAccount = computed(() => currentUser.value?.role === 'merchant')
const isMerchantOwner = computed(() => isMerchantAccount.value && Number(detail.value?.merchantId || 0) === currentUserId.value)

const galleryImages = computed(() => {
  const value = detail.value?.images
  if (Array.isArray(value)) {
    return value.filter(Boolean)
  }
  if (typeof value === 'string' && value.trim()) {
    try {
      const parsed = JSON.parse(value)
      return Array.isArray(parsed) ? parsed.filter(Boolean) : []
    } catch {
      return detail.value?.coverImage ? [detail.value.coverImage] : []
    }
  }
  return detail.value?.coverImage ? [detail.value.coverImage] : []
})

const activeImage = computed(() => galleryImages.value[currentImageIndex.value] || detail.value?.coverImage || '')
const videoUrl = computed(() => detail.value?.videoUrl || '')
const videoPoster = computed(() => detail.value?.videoCoverUrl || detail.value?.coverImage || activeImage.value || '')
const participantSummary = computed(() => `${detail.value?.currentParticipants || 0}/${detail.value?.maxParticipants || '不限'}`)
const priceLabel = computed(() => Number(detail.value?.price || 0) > 0 ? `CNY ${(Number(detail.value.price) / 100).toFixed(2)}` : '免费')
const canBook = computed(() => {
  if (!detail.value) {
    return false
  }
  if (isMerchantAccount.value) {
    return false
  }
  const status = String(detail.value.status || '').toLowerCase()
  if (status === 'ended' || status === 'full') {
    return false
  }
  if (detail.value.maxParticipants && detail.value.currentParticipants >= detail.value.maxParticipants) {
    return false
  }
  return true
})
const bookingButtonText = computed(() => {
  if (!detail.value) {
    return '预订活动'
  }
  if (isMerchantOwner.value) {
    return '管理活动'
  }
  if (isMerchantAccount.value) {
    return '商家账户不可预订'
  }
  if (String(detail.value.status || '').toLowerCase() === 'ended') {
    return '活动已结束'
  }
  if (!canBook.value) {
    return '已满员'
  }
  return '立即预订'
})
const statusTone = computed(() => {
  const status = String(detail.value?.status || detail.value?.auditStatus || '').toLowerCase()
  if (status === 'signup' || status === 'approved' || status === 'ongoing') {
    return 'success'
  }
  if (status === 'ended' || status === 'full' || status === 'rejected') {
    return 'danger'
  }
  return 'pending'
})

function syncInitialDetail() {
  if (props.initialDetail && Number(props.initialDetail.id || 0) === Number(props.activityId || 0)) {
    detail.value = { ...props.initialDetail }
    currentImageIndex.value = 0
  }
}

async function loadDetail() {
  if (!props.activityId || !props.visible) {
    return
  }

  loading.value = true
  currentUser.value = readCurrentUser()

  try {
    const response = await getPublicActivityDetail(props.activityId)
    if (response.code !== 200) {
      throw new Error(response.message || 'Failed to load activity detail')
    }
    detail.value = response.data || null
    currentImageIndex.value = 0
    emit('loaded', detail.value)
  } catch (error) {
    detail.value = null
    notify.error(error.message || 'Failed to load activity detail')
  } finally {
    loading.value = false
  }
}

function handleEscape(event) {
  if (event.key === 'Escape' && props.visible) {
    emit('close')
  }
}

function lockBody(locked) {
  document.body.style.overflow = locked ? 'hidden' : ''
}

function statusLabel(status) {
  return {
    signup: '开放预订',
    ongoing: '进行中',
    ended: '已结束',
    full: '已满员',
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }[String(status || '').toLowerCase()] || '即将开始'
}

function activityTypeLabel(type) {
  return {
    offline: '线下体验',
    online: '线上活动',
    exhibition: '展览'
  }[String(type || '').toLowerCase()] || '活动'
}

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '时间待定'
}

function formatDateRange(item) {
  if (!item?.startTime && !item?.endTime) {
    return '时间待定'
  }
  const start = item?.startTime ? formatDateTime(item.startTime) : '时间待定'
  const end = item?.endTime ? formatDateTime(item.endTime) : ''
  return end ? `${start} - ${end}` : start
}

function formatLocation(item) {
  return [item?.locationProvince, item?.locationCity, item?.locationDistrict, item?.locationDetail]
    .filter(Boolean)
    .join(' / ') || '地点待定'
}

function goToBooking() {
  if (!detail.value?.id) {
    return
  }

  if (isMerchantOwner.value) {
    goMerchantBookings()
    return
  }

  if (!canBook.value) {
    return
  }

  router.push({
    name: 'activity-booking',
    params: { id: detail.value.id },
    query: { backTo: route.fullPath }
  })
}

function goEditActivity() {
  if (!detail.value?.id) {
    return
  }
  router.push({
    name: 'merchant-activity-edit',
    params: { id: detail.value.id }
  })
}

function goMerchantBookings() {
  if (!detail.value?.id) {
    return
  }
  router.push({
    name: 'merchant-bookings',
    query: {
      activityId: String(detail.value.id),
      title: detail.value.title || '',
      backTo: route.fullPath
    }
  })
}

function goMerchantHomepage() {
  if (!detail.value?.merchantId) {
    return
  }
  router.push({
    path: `/user-homepage/${detail.value.merchantId}`,
    query: { backTo: route.fullPath }
  })
}

function contactMerchant() {
  if (!detail.value?.merchantId) {
    return
  }
  if (Number(detail.value.merchantId) === currentUserId.value) {
    notify.warning('您不能给自己发消息。')
    return
  }
  router.push({
    path: '/notification',
    query: {
      userId: String(detail.value.merchantId),
      returnTo: route.fullPath,
      scope: 'chat'
    }
  })
}

watch(
  () => [props.visible, props.activityId],
  async ([visible]) => {
    if (visible) {
      syncInitialDetail()
      lockBody(true)
      await loadDetail()
    } else {
      lockBody(false)
    }
  },
  { immediate: true }
)

watch(
  () => props.initialDetail,
  () => {
    syncInitialDetail()
  }
)

watch(galleryImages, (images) => {
  if (!images.length) {
    currentImageIndex.value = 0
    return
  }
  if (currentImageIndex.value > images.length - 1) {
    currentImageIndex.value = 0
  }
})

document.addEventListener('keydown', handleEscape)

onBeforeUnmount(() => {
  lockBody(false)
  document.removeEventListener('keydown', handleEscape)
})
</script>

<style scoped>
.activity-modal-shell {
  position: fixed;
  inset: 0;
  z-index: 1300;
}

.activity-modal-overlay {
  position: absolute;
  inset: 0;
  background: rgba(10, 16, 26, 0.56);
  backdrop-filter: blur(5px);
}

.activity-modal-card {
  position: absolute;
  inset: 24px;
  max-width: 1380px;
  margin: 0 auto;
  padding: 28px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(182, 92, 56, 0.08), transparent 22%),
    linear-gradient(180deg, #fcfaf6 0%, #f7f2e8 100%);
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.24);
  overflow: auto;
}
.activity-modal-card::-webkit-scrollbar {
  display: none;
}
.activity-modal-card {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.close-button {
  position: sticky;
  top: 0;
  margin-left: auto;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #29384a;
  cursor: pointer;
  z-index: 2;
}

.modal-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 22px;
}

.media-column,
.info-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-image,
.headline-card,
.content-panel,
.state-block {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(227, 214, 196, 0.9);
  box-shadow: 0 14px 34px rgba(50, 60, 75, 0.08);
}

.hero-image {
  position: relative;
  overflow: hidden;
  min-height: 360px;
}

.hero-image img {
  width: 100%;
  height: 100%;
  min-height: 360px;
  object-fit: cover;
  display: block;
}

.status-pill {
  position: absolute;
  top: 16px;
  left: 16px;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 700;
}

.status-pill.success {
  background: rgba(37, 99, 235, 0.92);
  color: #fff;
}

.status-pill.danger {
  background: rgba(185, 28, 28, 0.92);
  color: #fff;
}

.status-pill.pending {
  background: rgba(217, 119, 6, 0.92);
  color: #fff;
}

.thumb-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(84px, 1fr));
  gap: 10px;
}

.thumb-item {
  border: 2px solid transparent;
  border-radius: 18px;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
}

.thumb-item.active {
  border-color: #b65c38;
}

.thumb-item img {
  width: 100%;
  height: 84px;
  object-fit: cover;
  display: block;
}

.headline-card,
.content-panel {
  padding: 22px;
}

.eyebrow-row,
.meta-grid,
.action-bar,
.owner-actions,
.merchant-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.eyebrow-row {
  margin-bottom: 14px;
}

.type-pill {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(182, 92, 56, 0.12);
  color: #9d4d30;
  font-size: 0.84rem;
  font-weight: 700;
}

.type-pill.subtle {
  background: rgba(36, 48, 63, 0.08);
  color: #415162;
}

.headline-card h2,
.panel-head h3 {
  margin: 0;
  color: #233142;
}

.subtitle,
.merchant-copy p,
.rich-text,
.state-block p {
  margin: 0;
  color: #5f6f7f;
  line-height: 1.7;
}

.subtitle {
  margin-top: 10px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.meta-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  background: #faf6ef;
  color: #314154;
}

.meta-item i {
  font-size: 1.2rem;
  color: #b65c38;
}

.meta-item strong {
  display: block;
  margin-top: 4px;
  line-height: 1.5;
}

.meta-label,
.price-label {
  font-size: 0.82rem;
  color: #7a8796;
}

.action-bar {
  margin-top: 20px;
  align-items: center;
  justify-content: space-between;
}

.price-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.price-block strong {
  font-size: 1.8rem;
  color: #b23f31;
}

.primary-action,
.secondary-action {
  min-height: 44px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  font: inherit;
  cursor: pointer;
}

.primary-action {
  background: linear-gradient(135deg, #b65c38 0%, #d28d44 100%);
  color: #fff;
  box-shadow: 0 14px 28px rgba(182, 92, 56, 0.22);
}

.secondary-action {
  background: #efe6d9;
  color: #334255;
}

.primary-action:disabled {
  opacity: 0.58;
  cursor: not-allowed;
  box-shadow: none;
}

.panel-head {
  margin-bottom: 14px;
}

.merchant-row {
  display: flex;
  gap: 14px;
  align-items: center;
}

.merchant-avatar {
  width: 60px;
  height: 60px;
  border-radius: 18px;
  object-fit: cover;
  flex: 0 0 auto;
}

.merchant-copy strong {
  display: block;
  margin-bottom: 6px;
  color: #243342;
}

.rich-text {
  white-space: pre-line;
}

.video-player {
  width: 100%;
  border-radius: 18px;
  background: #111827;
}

.state-block {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
}

.state-block i {
  font-size: 2rem;
  color: #b65c38;
}

@media (max-width: 1100px) {
  .activity-modal-card {
    inset: 16px;
    padding: 20px;
  }

  .modal-layout {
    grid-template-columns: 1fr;
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .activity-modal-card {
    inset: 0;
    border-radius: 0;
    padding: 16px;
  }

  .hero-image,
  .hero-image img {
    min-height: 240px;
  }

  .action-bar {
    align-items: stretch;
  }
}
</style>
