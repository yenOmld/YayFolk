<template>
  <Teleport to="body">
    <div v-if="visible" class="activity-modal-shell" :style="{ zIndex: zIndex }">
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
          <div class="top-row">
            <div class="media-column">
              <div class="hero-image">
                <template v-if="activeMedia?.type === 'video'">
                  <video
                    controls
                    preload="metadata"
                    playsinline
                    class="media-preview"
                    :poster="activeMedia.poster || detail.coverImage || ''"
                  >
                    <source :src="activeMedia.url">
                  </video>
                </template>
                <template v-else>
                  <img
                    class="media-preview"
                    :src="activeMedia?.url || detail.coverImage || ''"
                    :alt="detail.title || '活动封面'"
                  >
                </template>

                <span class="status-pill" :class="statusTone">{{ statusLabel(detail.status || detail.auditStatus) }}</span>
                <span v-if="mediaItems.length" class="media-type-pill">{{ activeMediaLabel }}</span>

                <button
                  v-if="mediaItems.length > 1"
                  class="media-nav media-prev"
                  type="button"
                  aria-label="上一张"
                  @click="prevMedia"
                >
                  <i class="bx bx-chevron-left"></i>
                </button>

                <button
                  v-if="mediaItems.length > 1"
                  class="media-nav media-next"
                  type="button"
                  aria-label="下一张"
                  @click="nextMedia"
                >
                  <i class="bx bx-chevron-right"></i>
                </button>

                <div v-if="mediaItems.length > 1" class="media-counter">
                  {{ currentMediaIndex + 1 }} / {{ mediaItems.length }}
                </div>
              </div>

              <div v-if="mediaItems.length > 1" class="media-dots" aria-label="切换媒体">
                <button
                  v-for="(media, index) in mediaItems"
                  :key="media.id"
                  type="button"
                  class="media-dot"
                  :class="{ active: currentMediaIndex === index }"
                  :aria-label="media.label"
                  @click="selectMedia(index)"
                ></button>
              </div>

              <button
                v-if="detail.vrModelUrl"
                class="viewer-cta"
                type="button"
                @click="showVRModal = true"
              >
                <i class="bx bx-cube-alt"></i>
                <span>360° 查看</span>
              </button>
            </div>
            <div class="info-column">
              <div class="headline-card">
                <div v-if="detail && typeof detail.avgScore === 'number' && detail.avgScore > 0" class="rating-floating-btn" :title="`综合评分 ${reviewScoreText}`" @click="scrollToReviews">
                  <div class="rating-floating-content">
                    <span class="rating-floating-score">{{ reviewScoreText }}</span>
                    <div class="rating-stars-row">
                      <i
                        v-for="star in 5"
                        :key="star"
                        class="bx bxs-star rating-star"
                        :style="{ clipPath: getStarClipPath(star) }"
                      ></i>
                      <div class="stars-bg">
                        <i v-for="star in 5" :key="'bg' + star" class="bx bx-star rating-star-bg"></i>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="merchant-header">
                  <img
                    class="merchant-avatar-link"
                    :src="detail.merchantAvatar || placeholderAvatar"
                    :alt="detail.merchantName || '商家'"
                    @error="handleAvatarError"
                    @click="goMerchantHomepage"
                  >
                  <span class="merchant-nickname" @click="goMerchantHomepage">{{ detail.merchantName || '商家' }}</span>
                </div>
                <div class="eyebrow-row">
                  <span class="type-pill">{{ detail.heritageType || '未分类' }}</span>
                  <span class="type-pill subtle">{{ activityTypeLabel(detail.activityType) }}</span>
                </div>
                <div class="headline-row">
                  <div class="headline-copy">
                    <h2>{{ detail.title || '未命名活动' }}</h2>
                    <p class="subtitle">{{ detail.subtitle || detail.content || '暂无活动简介。' }}</p>
                  </div>
                </div>

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
            </div>
          </div>

          <div class="content-panel">
            <div class="panel-head">
              <h3>活动描述</h3>
            </div>
            <p class="rich-text">{{ detail.content || detail.subtitle || '暂无描述。' }}</p>
          </div>

          <div class="content-panel reviews-panel" ref="reviewsSection">
            <div class="panel-head">
              <h3>评价</h3>
              <div class="review-controls">
                <button
                  class="sort-toggle-btn"
                  type="button"
                  :class="{ active: reviewSortType === 'score' }"
                  @click="toggleReviewSort"
                  :title="reviewSortType === 'score' ? (reviewSortOrder === 'desc' ? '按评分从高到低排序，点击切换' : '按评分从低到高排序，点击切换') : '按评分排序'"
                >
                  <i class="bx bx-sort-alt-2"></i>
                  <span>{{ reviewSortType === 'score' ? (reviewSortOrder === 'desc' ? '评分' : '评分 ↑') : '评分' }}</span>
                </button>
                <button
                  class="sort-toggle-btn"
                  type="button"
                  :class="{ active: reviewSortType === 'time' }"
                  @click="setReviewSortTime"
                  title="按时间排序，最新优先"
                >
                  <i class="bx bx-time-five"></i>
                  <span>最新</span>
                </button>
              </div>
            </div>
            <span class="review-summary" v-if="reviews.length > 0">{{ reviews.length }} 条评价</span>
              
              <div v-if="loadingReviews" class="loading-state">
                <i class='bx bx-loader-alt bx-spin'></i>
                <p>加载评价中...</p>
              </div>
              
              <div v-else-if="reviews.length === 0" class="empty-state">
                <i class='bx bx-message-square-detail'></i>
                <p>暂无评价。</p>
              </div>
              
              <div v-else class="reviews-list">
                <article
                  v-for="review in sortedReviews"
                  :key="review.id"
                  class="comment-item"
                  role="button"
                  tabindex="0"
                  @click="openReviewDetail(review)"
                  @keydown.enter.prevent="openReviewDetail(review)"
                >
                  <img :src="review.authorAvatar || placeholderAvatar" :alt="review.authorName" class="comment-avatar" @error="handleAvatarError">
                  <div class="comment-content">
                    <div class="comment-header">
                      <span class="comment-author">{{ review.isAnonymous ? '匿名用户' : (review.authorName || '匿名用户') }}</span>
                      <span class="comment-time">{{ formatReviewTime(review.createTime) }}</span>
                    </div>

                    <div class="review-rating-line">
                      <span class="rating-number">{{ Number(review.score || 0).toFixed(1) }}</span>
                      <div class="rating-stars">
                        <i
                          v-for="star in 5"
                          :key="star"
                          class="bx"
                          :class="star <= Math.round(Number(review.score || 0)) ? 'bxs-star' : 'bx-star'"
                        ></i>
                      </div>
                    </div>

                    <p class="comment-text">{{ review.contentVisible === false ? '仅作者可见' : getReviewExcerpt(review) }}</p>

                    <div v-if="review.images && review.images.length > 0" class="review-images">
                      <img v-for="(img, index) in review.images.slice(0, 3)" :key="index" :src="img" :alt="`评价图片 ${index + 1}`">
                      <span v-if="review.images.length > 3" class="more-images">+{{ review.images.length - 3 }}</span>
                    </div>
                  </div>
                </article>
              </div>


            </div>
        </div>
      </div>
    </div>
  </Teleport>

  <VRViewerModal
    :visible="showVRModal"
    :activity-id="detail?.id"
    :z-index="zIndex + 100"
    @close="showVRModal = false"
  />

  <PostDetailModal
    :visible="showReviewPostModal"
    :post="reviewPostDetail"
    :z-index="zIndex + 200"
    @close="closeReviewPostModal"
    @open-activity="handleReviewActivityClick"
  />
</template>

<script setup>
import { computed, getCurrentInstance, onBeforeUnmount, watch, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createConversation, getPublicActivityDetail, getActivityReviews, getDiscoverPostDetail, getMyOrderOverview } from '@/api/app.js'
import PostDetailModal from '@/components/PostDetailModal.vue'
import VRViewerModal from '@/components/VRViewerModal.vue'
import { isVideoUrl, normalizeMediaList } from '@/utils/media'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  zIndex: {
    type: Number,
    default: 1300
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
const currentMediaIndex = ref(0)
const currentUser = ref(readCurrentUser())
const reviews = ref([])
const loadingReviews = ref(false)
const reviewsSection = ref(null)
const userBookings = ref([])
const showReviewPostModal = ref(false)
const showVRModal = ref(false)
const reviewPostDetail = ref(null)
const reviewSortType = ref('score')
const reviewSortOrder = ref('desc')

function readCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || localStorage.getItem('userInfo') || 'null') || {}
  } catch {
    return {}
  }
}

function handleAvatarError(event) {
  const img = event?.target
  if (img && img.src !== placeholderAvatar) {
    img.src = placeholderAvatar
  }
}

const currentUserId = computed(() => Number(currentUser.value?.id || 0))
const isMerchantAccount = computed(() => currentUser.value?.role === 'merchant')
const isMerchantOwner = computed(() => isMerchantAccount.value && Number(detail.value?.merchantId || 0) === currentUserId.value)
const reviewScoreText = computed(() => Number(detail.value?.avgScore || 0).toFixed(1))
const reviewCountText = computed(() => Number(detail.value?.reviewCount || 0))
const reviewStarCount = computed(() => Math.round(Number(detail.value?.avgScore || 0)))

function getScorePercent() {
  return Math.min(100, Math.max(0, (Number(detail.value?.avgScore || 0) / 5) * 100))
}

function getStarClipPath(star) {
  const score = Number(detail.value?.avgScore || 0)
  if (score >= star) return 'inset(0 0 0 0)'
  if (score <= star - 1) return 'inset(0 100% 0 0)'
  const percent = (score - (star - 1)) * 100
  return `inset(0 ${100 - percent}% 0 0)`
}

function getStarFillPercent(star) {
  const score = Number(detail.value?.avgScore || 0)
  if (score >= star) return 100
  if (score <= star - 1) return 0
  return Math.max(0, Math.min(100, (score - (star - 1)) * 100))
}

const galleryImages = computed(() => {
  const value = normalizeMediaList(detail.value?.images)
  const media = value.length ? value : (detail.value?.coverImage ? [detail.value.coverImage] : [])
  if (detail.value?.videoUrl && !media.some(item => String(item) === String(detail.value.videoUrl))) {
    media.push(detail.value.videoUrl)
  }
  return media.filter(Boolean)
})

const mediaItems = computed(() => {
  const poster = detail.value?.videoCoverUrl || detail.value?.coverImage || galleryImages.value.find(url => !isVideoUrl(url)) || ''
  return galleryImages.value.map((url, index) => ({
    id: `${isVideoUrl(url) ? 'video' : 'image'}-${index}`,
    type: isVideoUrl(url) ? 'video' : 'image',
    url,
    poster: isVideoUrl(url) ? poster : url,
    label: isVideoUrl(url) ? '视频' : `图片 ${index + 1}`
  }))
})

const activeMedia = computed(() => mediaItems.value[currentMediaIndex.value] || mediaItems.value[0] || null)
const activeMediaLabel = computed(() => activeMedia.value?.label || '预览')
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
    currentMediaIndex.value = 0
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
      throw new Error(response.message || '加载活动详情失败')
    }
    detail.value = response.data || null
    currentMediaIndex.value = 0
    emit('loaded', detail.value)
    
    await loadReviews()
    await loadUserBookings()
  } catch (error) {
    detail.value = null
    notify.error(error.message || '加载活动详情失败')
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  if (!props.activityId) return
  
  loadingReviews.value = true
  try {
    const response = await getActivityReviews(props.activityId)
    if (response.code === 200) {
      reviews.value = response.data || []
    }
  } catch (error) {
    console.error('Failed to load reviews:', error)
  } finally {
    loadingReviews.value = false
  }
}

async function loadUserBookings() {
  if (!currentUser.value || !currentUser.value.id) return
  
  try {
    const response = await getMyOrderOverview()
    if (response.code === 200) {
      userBookings.value = Array.isArray(response.data?.activityBookings) ? response.data.activityBookings : []
    }
  } catch (error) {
    console.error('Failed to load user bookings:', error)
  }
}

function handleEscape(event) {
  if (event.key !== 'Escape' || !props.visible) {
    return
  }
  if (showReviewPostModal.value) {
    closeReviewPostModal()
    return
  }
  if (props.visible) {
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
  createConversation({ otherUserId: detail.value.merchantId })
    .then((res) => {
      if (res.code !== 200 || !res.data?.id) {
        notify.error(res.message || '打开聊天失败')
        return
      }
      router.push({
        path: '/notification',
        query: {
          conversationId: String(res.data.id),
          returnTo: route.fullPath,
          scope: 'chat'
        }
      })
    })
    .catch(() => {
      notify.error('打开聊天失败，请稍后重试')
    })
}

function scrollToReviews() {
  if (reviewsSection.value) {
    reviewsSection.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

function toggleReviewSort() {
  if (reviewSortType.value === 'score') {
    reviewSortOrder.value = reviewSortOrder.value === 'desc' ? 'asc' : 'desc'
  } else {
    reviewSortType.value = 'score'
    reviewSortOrder.value = 'desc'
  }
}

function setReviewSortTime() {
  reviewSortType.value = 'time'
}

const sortedReviews = computed(() => {
  return [...reviews.value].sort((left, right) => {
    if (reviewSortType.value === 'score') {
      const leftScore = Number(left.score || 0)
      const rightScore = Number(right.score || 0)
      return reviewSortOrder.value === 'desc' ? rightScore - leftScore : leftScore - rightScore
    } else {
      const leftTime = new Date(left.createTime || 0).getTime() || 0
      const rightTime = new Date(right.createTime || 0).getTime() || 0
      return rightTime - leftTime
    }
  })
})

function formatReviewTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

function getTruncatedText(text, maxLength = 100) {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

function normalizeReviewText(text) {
  return String(text || '')
    .replace(/\s+/g, ' ')
    .replace(/^[\s\d.、\-*#✅📍⭐️✨]+/, '')
    .trim()
}

function splitReviewHighlights(text) {
  return normalizeReviewText(text)
    .split(/[。！？!?；;，,、|/\\\n\r：:]+/)
    .map(item => normalizeReviewText(item))
    .filter(item => item.length >= 2)
}

function getReviewSummary(review) {
  return getReviewExcerpt(review)
}

function getReviewExcerpt(review, maxSentences = 2, maxLength = 72) {
  const text = normalizeReviewText(review?.content || review?.title)
  if (!text) {
    return '暂无评价内容'
  }

  const sentences = text
    .split(/[。！？!?；;\n\r]+/)
    .map(item => normalizeReviewText(item))
    .filter(Boolean)

  const excerpt = (sentences.length > 0 ? sentences.slice(0, maxSentences).join('。') : text)
    .replace(/。{2,}/g, '。')

  if (excerpt.length <= maxLength) {
    return excerpt
  }

  return `${excerpt.slice(0, maxLength).replace(/[，,。！？!?；;\s]+$/, '')}…`
}

function prevMedia() {
  if (!mediaItems.value.length) return
  currentMediaIndex.value = (currentMediaIndex.value - 1 + mediaItems.value.length) % mediaItems.value.length
}

function nextMedia() {
  if (!mediaItems.value.length) return
  currentMediaIndex.value = (currentMediaIndex.value + 1) % mediaItems.value.length
}

function selectMedia(index) {
  if (index < 0 || index >= mediaItems.value.length) return
  currentMediaIndex.value = index
}

function openImagePreview(imageUrl) {
  window.open(imageUrl, '_blank')
}

async function openReviewDetail(review) {
  if (!review.id) return
  if (review.visibility === 'private' && Number(review.userId || 0) !== currentUserId.value) return

  try {
    const response = await getDiscoverPostDetail(review.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '加载评价失败')
    }

    reviewPostDetail.value = response.data
    showReviewPostModal.value = true
  } catch (error) {
    notify.error(error.message || '加载评价失败，请稍后重试')
  }
}

function closeReviewPostModal() {
  showReviewPostModal.value = false
  reviewPostDetail.value = null
}

function handleReviewActivityClick() {
  closeReviewPostModal()
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
      showReviewPostModal.value = false
      reviewPostDetail.value = null
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

watch(mediaItems, (items) => {
  if (!items.length) {
    currentMediaIndex.value = 0
    return
  }
  if (currentMediaIndex.value > items.length - 1) {
    currentMediaIndex.value = 0
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
  max-width: 1180px;
  margin: 0 auto;
  padding: 22px;
  border-radius: 24px;
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
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.top-row {
  display: grid;
  grid-template-columns: minmax(0, 420px) minmax(0, 1fr);
  gap: 18px;
  align-items: stretch;
}

.media-column,
.info-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.info-column {
  align-items: stretch;
}

.headline-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
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
  width: 100%;
  max-width: 100%;
  aspect-ratio: 3 / 4;
  background: #fff;
  min-height: unset;
  max-height: 78vh;
  margin: 0 auto;
}

.media-preview {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
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

.media-type-pill {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.66);
  color: #fff;
  font-size: 0.82rem;
  font-weight: 700;
}

.media-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.92);
  color: #233142;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.18);
}

.media-nav:hover {
  background: #fff;
}

.media-prev {
  left: 14px;
}

.media-next {
  right: 14px;
}

.media-counter {
  position: absolute;
  right: 16px;
  bottom: 16px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.62);
  color: #fff;
  font-size: 0.8rem;
  font-weight: 600;
}

.media-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
}

.viewer-cta {
  margin-top: 12px;
  width: 100%;
  border: 1px solid rgba(182, 92, 56, 0.18);
  border-radius: 16px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, rgba(182, 92, 56, 0.12), rgba(232, 191, 145, 0.18));
  color: #7f3e22;
  font-weight: 700;
  cursor: pointer;
}

.viewer-cta:hover {
  background: linear-gradient(135deg, rgba(182, 92, 56, 0.16), rgba(232, 191, 145, 0.24));
}

.media-dot {
  width: 10px;
  height: 10px;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: rgba(182, 92, 56, 0.22);
  cursor: pointer;
  transition: all 0.2s ease;
}

.media-dot.active {
  width: 28px;
  background: #b65c38;
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
  grid-template-columns: 1fr;
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

.merchant-header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 14px;
}

.merchant-avatar-link {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid rgba(182, 92, 56, 0.2);
}

.merchant-avatar-link:hover {
  transform: scale(1.05);
  border-color: #b65c38;
  box-shadow: 0 4px 12px rgba(182, 92, 56, 0.2);
}

.merchant-nickname {
  font-size: 0.85rem;
  color: #7a8796;
  cursor: pointer;
  transition: color 0.2s ease;
}

.merchant-nickname:hover {
  color: #b65c38;
}

.review-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid rgba(182, 92, 56, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #7a8796;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sort-toggle-btn:hover {
  background: #faf6ef;
  border-color: #b65c38;
  color: #b65c38;
}

.sort-toggle-btn.active {
  background: linear-gradient(135deg, #b65c38 0%, #d28d44 100%);
  border-color: #b65c38;
  color: #fff;
}

.sort-toggle-btn i {
  font-size: 1rem;
}

.merchant-copy strong {
  display: block;
  margin-bottom: 6px;
  color: #243342;
}

.rich-text {
  white-space: pre-line;
}

.rating-floating-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(250, 245, 238, 0.95) 100%);
  border: 3px solid rgba(182, 92, 56, 0.4);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.15), 0 2px 8px rgba(182, 92, 56, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.rating-floating-btn:hover {
  transform: scale(1.08);
  border-color: #b65c38;
  box-shadow: 0 12px 32px rgba(182, 92, 56, 0.25), 0 4px 12px rgba(182, 92, 56, 0.15);
}

.rating-floating-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.rating-floating-score {
  font-size: 2rem;
  font-weight: 800;
  color: #b65c38;
  line-height: 1;
  text-shadow: 0 1px 2px rgba(182, 92, 56, 0.15);
}

.rating-stars-row {
  position: relative;
  display: flex;
  gap: 1px;
}

.rating-star {
  font-size: 14px;
  color: #ffc107;
  position: relative;
  z-index: 2;
}

.rating-star-bg {
  font-size: 14px;
  color: #ddd;
  position: absolute;
  top: 0;
}

.stars-bg {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  gap: 1px;
}

.score-stars {
  display: flex;
  gap: 1px;
}

.score-stars i {
  font-size: 14px;
  color: #ffc107;
}

.score-stars i.bx-star {
  color: #ddd;
}

.review-count {
  font-size: 0.9rem;
  color: #7a8796;
}

.vr-panel {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.vr-viewer {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.6);
  border: 2px dashed #cbd5e0;
  overflow: hidden;
}

.model-viewer {
  width: 100%;
  height: 100%;
  min-height: 320px;
  background: linear-gradient(180deg, #fcfbf7 0%, #eef2f6 100%);
}

.vr-hint {
  color: #7a8796;
  font-size: 0.82rem;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  background: #faf6ef;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.comment-item:hover {
  background: #f5efe4;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(182, 92, 56, 0.1);
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: space-between;
}

.comment-author {
  font-weight: 600;
  color: #243342;
  font-size: 0.95rem;
}

.comment-time {
  font-size: 0.85rem;
  color: #7a8796;
  flex-shrink: 0;
}

.review-rating-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-number {
  font-size: 1.15rem;
  font-weight: 700;
  color: #b65c38;
  min-width: 44px;
}

.comment-title {
  margin: 0;
  color: #243342;
  font-weight: 600;
}

.review-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-keyword {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(182, 92, 56, 0.12);
  color: #9d4d30;
  font-size: 0.8rem;
  font-weight: 600;
}

.comment-text {
  margin: 0;
  color: #5f6f7f;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: normal;
}

.reviews-panel {
  max-height: 600px;
  overflow-y: auto;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.review-summary {
  font-size: 0.85rem;
  color: #7a8796;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #7a8796;
}

.loading-state i,
.empty-state i {
  font-size: 2.5rem;
  margin-bottom: 12px;
  color: #b65c38;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 16px;
}

.rating-stars {
  display: flex;
  gap: 1px;
}

.rating-stars i {
  font-size: 12px;
  color: #ffc107;
}

.rating-stars i.bx-star {
  color: #ddd;
}

.review-time {
  font-size: 0.85rem;
  color: #7a8796;
  margin-left: auto;
}

.review-title {
  margin: 0;
  font-size: 1rem;
  color: #243342;
  font-weight: 600;
}

.review-text {
  margin: 0;
  color: #5f6f7f;
  line-height: 1.6;
  font-size: 0.9rem;
}

.review-images {
  display: flex;
  gap: 8px;
  align-items: center;
}

.review-images img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.more-images {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(182, 92, 56, 0.1);
  border-radius: 8px;
  color: #b65c38;
  font-weight: 600;
  font-size: 0.9rem;
}

.write-review-btn {
  width: 100%;
  margin-top: 20px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #b65c38 0%, #d28d44 100%);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 8px 20px rgba(182, 92, 56, 0.25);
}

.write-review-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(182, 92, 56, 0.35);
}

.write-review-btn i {
  font-size: 1.2rem;
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

.headline-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-top: 6px;
}

.headline-copy {
  min-width: 0;
  flex: 1;
}

@media (max-width: 1100px) {
  .activity-modal-card {
    inset: 16px;
    padding: 20px;
  }

  .top-row {
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
