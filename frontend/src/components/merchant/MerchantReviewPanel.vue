<template>
  <section class="merchant-review-panel">
    <div v-if="showSummary" class="review-stats" :class="{ lifted: liftSummary }">
      <button
        class="stat-card"
        :class="{ active: scorePreset === 'all' }"
        type="button"
        @click="setScorePreset('all')"
      >
        <strong>{{ stats.total }}</strong>
        <span>总评价数</span>
      </button>
      <button
        class="stat-card"
        :class="{ active: scorePreset === 'average' }"
        type="button"
        @click="setScorePreset('average')"
      >
        <strong>{{ stats.averageScore }}</strong>
        <span>平均评分</span>
      </button>
      <button
        class="stat-card"
        :class="{ active: scorePreset === 'high' }"
        type="button"
        @click="setScorePreset('high')"
      >
        <strong>{{ stats.highScoreCount }}</strong>
        <span>4.5分以上评价</span>
      </button>
      <div class="stat-card stat-card-static">
        <strong>{{ stats.activityCount }}</strong>
        <span>相关活动</span>
      </div>
    </div>

    <div class="toolbar">
      <select v-model="selectedActivityId" class="control select">
        <option v-for="item in activityOptions" :key="item.value" :value="item.value">
          {{ item.label }}
        </option>
      </select>
      <input
        v-model.trim="keyword"
        class="control input"
        type="text"
        placeholder="按评价内容、作者或活动筛选"
      >
      <button class="sort-btn" type="button" @click="toggleSort">
        <i class="bx bx-sort-alt-2"></i>
        <span>{{ sortLabel }}</span>
      </button>
    </div>

    <div v-if="loading" class="empty-state">加载评价中...</div>
    <div v-else-if="visibleReviews.length === 0" class="empty-state">
      {{ emptyText }}
    </div>
    <div v-else class="review-list">
      <button
        v-for="review in visibleReviews"
        :key="review.id"
        type="button"
        class="review-card"
        @click="openReview(review)"
      >
        <div class="review-media">
          <img
            v-if="previewMedia(review)"
            class="review-cover"
            :src="previewMedia(review)"
            :alt="reviewActivityName(review)"
          >
          <div v-else class="media-placeholder">
            <i class="bx bx-image-alt"></i>
            <span>无媒体</span>
          </div>
          <span class="media-badge">{{ reviewTypeLabel(review) }}</span>
          <span v-if="previewMediaCount(review) > 1" class="media-count">
            +{{ previewMediaCount(review) - 1 }}
          </span>
        </div>

        <div class="review-body">
          <div class="review-head">
            <div class="review-author-block">
              <strong>{{ reviewAuthorName(review) }}</strong>
              <p>{{ reviewActivityName(review) }}</p>
            </div>
            <span class="time">{{ formatTime(review.createTime) }}</span>
          </div>

          <div class="score-line">
            <span class="score">{{ Number(review.score || 0).toFixed(1) }}</span>
            <div class="stars">
              <i
                v-for="star in 5"
                :key="star"
                class="bx"
                :class="star <= Math.round(Number(review.score || 0)) ? 'bxs-star' : 'bx-star'"
              ></i>
            </div>
          </div>

          <h3 class="review-title">{{ review.title || reviewActivityName(review) }}</h3>
          <p class="content">{{ getExcerpt(review.content) }}</p>

          <div class="chips">
            <span class="chip">{{ review.reviewTypeLabel || '活动评价' }}</span>
            <span v-if="review.activityId" class="chip chip-soft">
              评价帖子已就绪
            </span>
          </div>
        </div>
      </button>
    </div>

    <PostDetailModal
      :visible="showReviewDetail"
      :post="reviewPostForModal"
      :z-index="1500"
      @close="closeReviewDetail"
    />
  </section>
</template>

<script setup>
import { computed, getCurrentInstance, ref, watch } from 'vue'
import { getDiscoverPostDetail } from '@/api/app'
import PostDetailModal from '@/components/PostDetailModal.vue'

const props = defineProps({
  reviews: {
    type: Array,
    default: () => []
  },
  activities: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  showSummary: {
    type: Boolean,
    default: true
  },
  emptyText: {
    type: String,
    default: 'No reviews yet.'
  },
  liftSummary: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['open-activity'])

const { appContext } = getCurrentInstance() || {}
const notify = appContext?.config?.globalProperties?.$notify || {
  error: console.error,
  warning: console.warn
}

const selectedActivityId = ref('all')
const sortOrder = ref('desc')
const keyword = ref('')
const scorePreset = ref('all')
const showReviewDetail = ref(false)
const reviewDetail = ref(null)
const reviewDetailFallback = ref(null)
const detailMediaIndex = ref(0)

const defaultAvatar = 'https://yayfolk.bhyy.online/avatars/default.png'
const defaultMedia = 'https://api.dicebear.com/7.x/shapes/svg?seed=merchant-review'

const activityOptions = computed(() => {
  const seen = new Map()
  const source = Array.isArray(props.activities) ? props.activities : []

  source.forEach((activity) => {
    if (activity?.id === undefined || activity?.id === null) {
      return
    }
    const key = String(activity.id)
    if (!seen.has(key)) {
      seen.set(key, {
        value: key,
        label: activity.title || activity.name || 'Untitled Activity'
      })
    }
  })

  props.reviews.forEach((review) => {
    const activityId = review?.activityId
    if (activityId === undefined || activityId === null) {
      return
    }
    const key = String(activityId)
    if (!seen.has(key)) {
      seen.set(key, {
        value: key,
        label: review.targetName || review.activityTitle || 'Untitled Activity'
      })
    }
  })

  return [
    { value: 'all', label: '全部活动' },
    ...seen.values()
  ]
})

// 仅按活动筛选（不依赖 stats，供 stats 自身使用，避免循环依赖）
const activityFilteredReviews = computed(() => {
  const selected = String(selectedActivityId.value || 'all')
  if (selected === 'all') return props.reviews
  return props.reviews.filter(r => String(r.activityId || '') === selected)
})

const stats = computed(() => {
  const source = activityFilteredReviews.value
  const total = source.length
  const average = total
    ? source.reduce((sum, item) => sum + Number(item.score || 0), 0) / total
    : 0

  return {
    total,
    averageScore: total ? average.toFixed(1) : '--',
    averageValue: total ? average : 0,
    highScoreCount: source.filter(item => Number(item.score || 0) >= 4.5).length,
    activityCount: new Set(props.reviews.map(item => item.activityId).filter(Boolean)).size || props.activities.length || 0
  }
})

const visibleReviews = computed(() => {
  const keywordValue = keyword.value.toLowerCase()
  const avg = stats.value.averageValue

  const filtered = activityFilteredReviews.value.filter((review) => {
    const score = Number(review.score || 0)
    const matchesScore =
      scorePreset.value === 'all' ||
      (scorePreset.value === 'high' && score >= 4.5) ||
      (scorePreset.value === 'average' && avg > 0 && score >= Math.max(0, avg - 0.5) && score <= avg + 0.5)

    if (!matchesScore) {
      return false
    }
    if (!keywordValue) {
      return true
    }
    return [
      reviewAuthorName(review),
      reviewActivityName(review),
      review.content
    ].some(value => String(value || '').toLowerCase().includes(keywordValue))
  })

  return [...filtered].sort((left, right) => {
    const leftScore = Number(left.score || 0)
    const rightScore = Number(right.score || 0)
    if (rightScore !== leftScore) {
      return sortOrder.value === 'desc' ? rightScore - leftScore : leftScore - rightScore
    }
    const leftTime = new Date(left.createTime || left.updateTime || 0).getTime() || 0
    const rightTime = new Date(right.createTime || right.updateTime || 0).getTime() || 0
    return rightTime - leftTime
  })
})

const reviewDetailTitle = computed(() => reviewDetail.value?.title || reviewActivityName(reviewDetailFallback.value) || '评价详情')
const reviewDetailAuthorName = computed(() => reviewDetail.value?.author?.name || reviewDetail.value?.authorName || reviewDetailFallback.value?.authorName || '匿名')
const reviewDetailAuthorAvatar = computed(() => reviewDetail.value?.author?.avatar || reviewDetail.value?.authorAvatar || reviewDetailFallback.value?.authorAvatar || defaultAvatar)
const detailActivityId = computed(() => Number(reviewDetail.value?.activityId || reviewDetailFallback.value?.activityId || 0))
const detailActivityTitle = computed(() => reviewDetail.value?.activityInfo?.title || reviewDetailFallback.value?.targetName || reviewDetailFallback.value?.activityTitle || '')
const detailActivityTime = computed(() => reviewDetail.value?.activityInfo?.time || reviewDetailFallback.value?.activityTime || '')
const detailActivityLocation = computed(() => reviewDetail.value?.activityInfo?.location || reviewDetailFallback.value?.activityLocation || '')

const reviewPostForModal = computed(() => {
  const data = reviewDetail.value || reviewDetailFallback.value
  if (!data) return null
  
  const images = normalizeMediaList(data?.images)
  const authorId = data?.userId || data?.author?.id || 0
  const authorAvatar = data?.authorAvatar || data?.author?.avatar || defaultAvatar
  const authorName = data?.authorName || data?.author?.name || data?.nickname || data?.author?.nickname || '匿名用户'
  
  return {
    id: data?.id || data?.postId || data?.reviewPostId || 0,
    images: images.length > 0 ? images : normalizeMediaList(data?.activityImages),
    author: {
      id: authorId,
      avatar: authorAvatar,
      name: authorName,
      nickname: data?.nickname || data?.author?.nickname || authorName
    },
    title: data?.title || '',
    content: data?.content || '',
    score: data?.score || 0,
    createTime: data?.createTime || data?.time || '',
    activityId: data?.activityId || 0,
    viewCount: data?.viewCount || 0,
    commentCount: data?.commentCount || 0,
    collectCount: data?.collectCount || 0,
    type: data?.reviewType || data?.type || 'review'
  }
})

const detailMediaItems = computed(() => {
  const detailImages = normalizeMediaList(reviewDetail.value?.images)
  if (detailImages.length > 0) {
    return detailImages.map((url, index) => ({
      id: `detail-image-${index}-${url}`,
      url,
      type: 'image',
      poster: url
    }))
  }

  const fallbackImages = normalizeMediaList(reviewDetailFallback.value?.activityImages)
  if (fallbackImages.length > 0) {
    return fallbackImages.map((url, index) => ({
      id: `fallback-image-${index}-${url}`,
      url,
      type: 'image',
      poster: url
    }))
  }

  const fallbackCover = reviewDetailFallback.value?.activityCoverImage || reviewDetailFallback.value?.coverImage
  return fallbackCover
    ? [{
        id: `fallback-cover-${fallbackCover}`,
        url: fallbackCover,
        type: 'image',
        poster: fallbackCover
      }]
    : []
})

const detailActiveMedia = computed(() => detailMediaItems.value[detailMediaIndex.value] || detailMediaItems.value[0] || null)
const sortLabel = computed(() => (sortOrder.value === 'desc' ? '评分：从高到低' : '评分：从低到高'))

watch(detailMediaItems, (items) => {
  if (!items.length) {
    detailMediaIndex.value = 0
    return
  }
  if (detailMediaIndex.value > items.length - 1) {
    detailMediaIndex.value = 0
  }
})

function setScorePreset(preset) {
  scorePreset.value = preset
}

function toggleSort() {
  sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
}

function reviewAuthorName(review) {
  return review?.authorName || review?.nickname || review?.username || '匿名'
}

function reviewAvatar(review) {
  return review?.authorAvatar || review?.avatar || defaultAvatar
}

function reviewActivityName(review) {
  return review?.targetName || review?.activityTitle || '关联活动'
}

function reviewTypeLabel(review) {
  return review?.reviewTypeLabel || '活动评价'
}

function getExcerpt(text) {
  const value = String(text || '').trim()
  if (!value) {
    return '暂无文本内容。'
  }

  const sentences = value
    .split(/[。！？!?；;\n\r]+/)
    .map(item => String(item || '').trim())
    .filter(Boolean)

  const excerpt = (sentences.length > 0 ? sentences.slice(0, 2).join('. ') : value)
    .replace(/\.{2,}/g, '.')

  return excerpt.length > 120 ? `${excerpt.slice(0, 120).replace(/[.,!?;:\s]+$/, '')}...` : excerpt
}

function formatTime(value) {
  if (!value) {
    return '时间待定'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '时间待定'
  }
  return date.toLocaleString('zh-CN')
}

function normalizeMediaList(value) {
  if (Array.isArray(value)) {
    return value.filter(Boolean)
  }
  if (!value) {
    return []
  }
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) {
      return []
    }
    try {
      const parsed = JSON.parse(trimmed)
      if (Array.isArray(parsed)) {
        return parsed.filter(Boolean)
      }
    } catch {
      // fall through to raw string
    }
    return [trimmed]
  }
  return [value].filter(Boolean)
}

function previewMedia(review) {
  const reviewImages = normalizeMediaList(review?.images)
  if (reviewImages.length > 0) {
    return reviewImages[0]
  }

  const activityImages = normalizeMediaList(review?.activityImages)
  if (activityImages.length > 0) {
    return activityImages[0]
  }

  return review?.activityCoverImage || review?.coverImage || defaultMedia
}

function previewMediaCount(review) {
  const reviewImages = normalizeMediaList(review?.images)
  if (reviewImages.length > 0) {
    return reviewImages.length
  }
  const activityImages = normalizeMediaList(review?.activityImages)
  if (activityImages.length > 0) {
    return activityImages.length
  }
  return review?.activityCoverImage || review?.coverImage ? 1 : 0
}

function openReview(review) {
  const postId = Number(review?.postId || review?.reviewPostId || review?.id || 0)
  if (!postId) {
    notify.warning('评价帖子尚未可用。')
    return
  }
  reviewDetailFallback.value = review
  showReviewDetail.value = true
}

async function loadReviewDetail(review, postId) {
  reviewDetailFallback.value = review
  try {
    const response = await getDiscoverPostDetail(postId)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '加载评价帖子失败')
    }
    reviewDetail.value = response.data
    showReviewDetail.value = true
    detailMediaIndex.value = 0
  } catch (error) {
    reviewDetail.value = null
    notify.error(error.message || '加载评价帖子失败')
  }
}

function closeReviewDetail() {
  showReviewDetail.value = false
  reviewDetail.value = null
  reviewDetailFallback.value = null
}

function prevDetailMedia() {
  if (!detailMediaItems.value.length) return
  detailMediaIndex.value = (detailMediaIndex.value - 1 + detailMediaItems.value.length) % detailMediaItems.value.length
}

function nextDetailMedia() {
  if (!detailMediaItems.value.length) return
  detailMediaIndex.value = (detailMediaIndex.value + 1) % detailMediaItems.value.length
}

function openActivityFromReview() {
  const activityId = Number(reviewDetail.value?.activityId || reviewDetailFallback.value?.activityId || 0)
  if (!activityId) {
    return
  }
  closeReviewDetail()
  emit('open-activity', activityId)
}
</script>

<style scoped>
.merchant-review-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.review-stats.lifted {
  margin-top: 36px;
}

.stat-card {
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, #fff, #fff8f2);
  border: 1px solid #f1ddd1;
  text-align: left;
  cursor: pointer;
}

.stat-card-static {
  cursor: default;
}

.stat-card.active {
  border-color: #e76f51;
  box-shadow: 0 12px 24px rgba(231, 111, 81, 0.12);
}

.stat-card strong {
  display: block;
  font-size: 26px;
  color: #1f2937;
}

.stat-card span {
  color: #6b7280;
  font-size: 13px;
}

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.control,
.sort-btn {
  min-height: 42px;
  border-radius: 14px;
  border: 1px solid #d8dce3;
  background: #fff;
}

.select,
.input {
  padding: 0 14px;
  color: #1f2937;
}

.input {
  flex: 1;
  min-width: 240px;
}

.sort-btn {
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #1f2937;
}

.empty-state {
  padding: 28px 16px;
  text-align: center;
  color: #6b7280;
  border-radius: 18px;
  background: #fff;
  border: 1px dashed #d8dce3;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-card {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: 0;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  background: #fff;
  padding: 0;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
}

.review-card:hover {
  border-color: #f1bfa4;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.review-media {
  position: relative;
  min-height: 220px;
  background: linear-gradient(135deg, #fff7f2, #fff);
}

.review-cover,
.detail-media-main {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.media-placeholder,
.detail-empty {
  width: 100%;
  height: 100%;
  min-height: 220px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.media-badge,
.media-count {
  position: absolute;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.media-badge {
  top: 14px;
  left: 14px;
  padding: 6px 10px;
  background: rgba(17, 24, 39, 0.72);
  color: #fff;
}

.media-count {
  right: 14px;
  bottom: 14px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.94);
  color: #111827;
}

.review-body {
  flex: 1;
  min-width: 0;
  padding: 18px 20px;
}

.review-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.review-author-block strong,
.review-author-block p,
.content,
.review-title,
.detail-text,
.detail-author strong,
.detail-author p,
.detail-head h3,
.detail-head p,
.detail-empty span,
.activity-card h4,
.activity-card p {
  margin: 0;
}

.review-author-block p {
  color: #6b7280;
  font-size: 13px;
  margin-top: 4px;
}

.time {
  color: #94a3b8;
  font-size: 12px;
  white-space: nowrap;
}

.score-line,
.detail-score {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.score {
  font-size: 20px;
  font-weight: 700;
  color: #ea580c;
}

.score.large {
  font-size: 30px;
}

.stars {
  display: inline-flex;
  gap: 2px;
  color: #f59e0b;
}

.review-title {
  margin-top: 10px;
  font-size: 18px;
  color: #111827;
}

.content {
  margin-top: 10px;
  color: #374151;
  line-height: 1.6;
}

.chips {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 12px;
}

.chip-soft {
  background: #fff3ea;
  color: #c2410c;
}

.detail-mask {
  position: fixed;
  inset: 0;
  z-index: 2600;
  background: rgba(15, 23, 42, 0.46);
  display: grid;
  place-items: center;
  padding: 18px;
}

.detail-modal {
  width: min(1100px, 100%);
  max-height: min(92vh, 920px);
  overflow: auto;
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 34px 70px rgba(15, 23, 42, 0.26);
  padding: 22px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #c2410c;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.detail-head h3 {
  font-size: 24px;
  color: #111827;
}

.close-btn {
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 50%;
  background: #f8fafc;
  cursor: pointer;
}

.detail-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.detail-media {
  position: relative;
  min-height: 420px;
  border-radius: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, #fff7f2, #fff);
}

.detail-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
  cursor: pointer;
}

.detail-nav.prev {
  left: 14px;
}

.detail-nav.next {
  right: 14px;
}

.detail-dots {
  position: absolute;
  left: 50%;
  bottom: 16px;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
}

.detail-dot {
  width: 9px;
  height: 9px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.52);
}

.detail-dot.active {
  width: 24px;
  background: #fff;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 10px 4px;
}

.detail-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  flex: none;
}

.detail-author p {
  color: #6b7280;
  font-size: 13px;
  margin-top: 4px;
}

.detail-text {
  color: #1f2937;
  line-height: 1.8;
  font-size: 15px;
}

.activity-card {
  border: 1px solid #ead8ca;
  border-radius: 22px;
  padding: 16px;
  background: linear-gradient(180deg, #fff, #fff8f2);
  cursor: pointer;
}

.activity-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #c2410c;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 10px;
}

.activity-card h4 {
  font-size: 18px;
  color: #111827;
}

.activity-card p {
  color: #6b7280;
  margin-top: 6px;
}

.activity-link-hint {
  display: inline-flex;
  margin-top: 10px;
  color: #c2410c;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .review-card {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .review-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .review-stats {
    grid-template-columns: 1fr;
  }

  .review-card {
    grid-template-columns: 1fr;
  }

  .review-media,
  .media-placeholder,
  .detail-media {
    min-height: 220px;
  }

  .review-head,
  .detail-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-modal {
    padding: 18px;
  }
}
</style>