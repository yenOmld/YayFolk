<template>
  <div class="my-reviews-page">
    <button class="floating-back-btn" @click="goBack" aria-label="返回个人中心">
      <i class="bx bx-arrow-back"></i>
    </button>

    <section class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">个人中心</p>
        <h1>我的评价</h1>
        <p>这里会展示你发布的活动评价，包含活动关联、评分和匿名状态。</p>
      </div>
      <div class="hero-actions">
        <button class="refresh-btn" :disabled="reviewsLoading || bookingsLoading" @click="loadData">刷新</button>
      </div>
    </section>

    <section class="summary-grid">
      <button class="summary-card" :class="{ active: activeFilter === 'reviewed' }" @click="setFilter('reviewed')">
        <span class="summary-label">已发布</span>
        <strong>{{ summary.reviewed }}</strong>
      </button>
      <button class="summary-card" :class="{ active: activeFilter === 'pending' }" @click="setFilter('pending')">
        <span class="summary-label">待评价</span>
        <strong>{{ summary.pending }}</strong>
      </button>
      <button class="summary-card" :class="{ active: activeFilter === 'public' }" @click="setFilter('public')">
        <span class="summary-label">公开</span>
        <strong>{{ summary.publicCount }}</strong>
      </button>
      <button class="summary-card" :class="{ active: activeFilter === 'anonymous' }" @click="setFilter('anonymous')">
        <span class="summary-label">匿名</span>
        <strong>{{ summary.anonymousCount }}</strong>
      </button>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h2>{{ panelTitle }}</h2>
          <p class="panel-subtitle">{{ panelHint }}</p>
        </div>
        <span>{{ panelCount }} 条</span>
      </div>

      <div v-if="reviewsLoading || bookingsLoading" class="empty-state">正在加载中...</div>
      <div v-else-if="activeFilter === 'pending' && visiblePendingActivities.length === 0" class="empty-state">
        暂无待评价活动。完成并核销的活动会显示在这里。
      </div>
      <div v-else-if="activeFilter === 'pending'" class="pending-grid">
        <article v-for="booking in pendingActivities" :key="booking.id" class="pending-card">
          <img class="cover" :src="booking.activityCoverImage || booking.coverImage || defaultCover" alt="活动封面">
          <div class="pending-main">
            <h3>{{ booking.activityTitle || '活动' }}</h3>
            <p>{{ formatLocation(booking) }}</p>
            <p class="meta-line">{{ formatRange(booking.startTime, booking.endTime) }}</p>
          </div>
          <button class="write-btn" @click="goWriteReview(booking)">去评价</button>
        </article>
      </div>
      <div v-else-if="visibleReviewPosts.length === 0" class="empty-state">
        还没有评价。完成活动后就可以在这里发布。
      </div>
      <div v-else class="review-list">
        <article v-for="post in visibleReviewPosts" :key="post.id" class="review-card">
          <img class="cover" :src="post.images?.[0] || defaultCover" alt="评价封面">
          <div class="review-main" @click="openPreview(post)">
            <div class="review-top">
              <div class="title-block">
                <h3>{{ post.title || '活动评价' }}</h3>
                <p class="meta-line">{{ post.activityTitle || getActivityTitle(post.activityId) || '关联活动' }}</p>
              </div>
              <div class="badges">
                <span class="badge">{{ post.visibility === 'private' ? '私密' : '公开' }}</span>
                <span v-if="post.isAnonymous" class="badge subtle">匿名</span>
                <span
                  v-if="post.activityId"
                  class="badge activity-badge"
                  @click.stop="openLinkedActivity(post)"
                >
                  活动关联
                </span>
              </div>
            </div>

            <div class="score-row">
              <span class="score-text">{{ Number(post.score || 0).toFixed(1) }}</span>
              <div class="stars">
                <i
                  v-for="star in 5"
                  :key="star"
                  class="bx"
                  :class="star <= Math.round(Number(post.score || 0)) ? 'bxs-star' : 'bx-star'"
                ></i>
              </div>
            </div>

            <p class="excerpt">{{ post.visibility === 'private' ? '仅自己可见' : getExcerpt(post.content) }}</p>
            <p class="time-line">{{ post.time }}</p>
          </div>

          <div class="actions">
            <button class="primary-btn" @click="goEdit(post)">编辑</button>
            <button class="danger-btn" @click="deletePost(post)">删除</button>
          </div>
        </article>
      </div>
    </section>

    <PostDetailModal
      :visible="showPreview"
      :post="previewPost"
      :z-index="1900"
      @close="closePreview"
      @open-activity="openLinkedActivity"
    />

    <ActivityDetailModal
      :visible="showActivityDetail"
      :activity-id="selectedActivityId"
      :z-index="1950"
      @close="showActivityDetail = false"
    />
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onActivated, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  deleteMyDiscoverReviewPost,
  getDiscoverPostDetail,
  getMyDiscoverPosts,
  getMyOrderOverview
} from '../api/app'
import PostDetailModal from '../components/PostDetailModal.vue'
import ActivityDetailModal from '../components/ActivityDetailModal.vue'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm
const router = useRouter()

const defaultCover = 'https://api.dicebear.com/7.x/shapes/svg?seed=review'

const activeFilter = ref('reviewed')
const reviewsLoading = ref(false)
const bookingsLoading = ref(false)
const reviewedPosts = ref([])
const bookings = ref([])
const showPreview = ref(false)
const previewPost = ref(null)
const showActivityDetail = ref(false)
const selectedActivityId = ref('')
const skipNextActivationReload = ref(true)

const reviewedActivityIds = computed(() => new Set(reviewedPosts.value.map(item => Number(item.activityId || 0)).filter(Boolean)))
const pendingActivities = computed(() => bookings.value.filter(item => item.canReview && !reviewedActivityIds.value.has(Number(item.activityId || 0))))

const summary = computed(() => ({
  reviewed: reviewedPosts.value.length,
  pending: pendingActivities.value.length,
  publicCount: reviewedPosts.value.filter(item => item.visibility !== 'private').length,
  anonymousCount: reviewedPosts.value.filter(item => item.isAnonymous).length
}))

const visibleReviewPosts = computed(() => {
  if (activeFilter.value === 'public') {
    return reviewedPosts.value.filter(item => item.visibility !== 'private')
  }
  if (activeFilter.value === 'anonymous') {
    return reviewedPosts.value.filter(item => item.isAnonymous)
  }
  return reviewedPosts.value
})

const visiblePendingActivities = computed(() => pendingActivities.value)

const panelTitle = computed(() => {
  const titleMap = {
    reviewed: '已发布的评价',
    pending: '待评价活动',
    public: '公开评价',
    anonymous: '匿名评价'
  }
  return titleMap[activeFilter.value] || '我的评价'
})

const panelHint = computed(() => {
  const hintMap = {
    reviewed: '展示你发布过的所有评价贴。',
    pending: '这里是已核销完成、还未发评价的活动。',
    public: '仅展示公开发布的评价。',
    anonymous: '仅展示设置了匿名发布的评价。'
  }
  return hintMap[activeFilter.value] || ''
})

const panelCount = computed(() => {
  return activeFilter.value === 'pending' ? pendingActivities.value.length : visibleReviewPosts.value.length
})

const goBack = () => router.push('/home/personal')

const setFilter = (filter) => {
  activeFilter.value = filter
}

const normalizePost = (post) => ({
  ...post,
  activityLinkLabel: post.activityLinkLabel || '活动关联',
  time: post.time || post.createTime || '',
  postType: String(post.postType || post.type || '').toUpperCase(),
  sourceId: post.sourceId || post.activityId
})

const isReviewPost = (post) => {
  const postType = String(post?.postType || post?.type || '').toUpperCase()
  return postType === 'REVIEW'
}

const sortByTimeDesc = (list) => {
  return [...list].sort((a, b) => {
    const at = new Date(a.time || a.createTime || 0).getTime()
    const bt = new Date(b.time || b.createTime || 0).getTime()
    return bt - at
  })
}

const loadReviews = async () => {
  reviewsLoading.value = true
  try {
    const reviewResp = await getMyDiscoverPosts()
    if (reviewResp.code !== 200) {
      throw new Error(reviewResp.message || '加载评价失败')
    }
    const reviewList = Array.isArray(reviewResp.data) ? reviewResp.data : []
    reviewedPosts.value = sortByTimeDesc(reviewList.filter(isReviewPost).map(normalizePost))
  } catch (error) {
    reviewedPosts.value = []
    notify.error(error.message || '加载评价失败')
  } finally {
    reviewsLoading.value = false
  }
}

const loadBookings = async () => {
  bookingsLoading.value = true
  try {
    const overviewResp = await getMyOrderOverview()
    if (overviewResp.code !== 200) {
      throw new Error(overviewResp.message || '加载活动失败')
    }
    bookings.value = Array.isArray(overviewResp.data?.activityBookings) ? overviewResp.data.activityBookings : []
  } catch (error) {
    bookings.value = []
    notify.error(error.message || '加载活动失败')
  } finally {
    bookingsLoading.value = false
  }
}

const loadData = async () => {
  await Promise.all([loadReviews(), loadBookings()])
}

const goWriteReview = (booking) => {
  router.push({
    name: 'create-review-post',
    query: {
      activityId: String(booking.activityId || ''),
      activityTitle: booking.activityTitle || '',
      bookingId: String(booking.id || ''),
      backTo: '/personal/my-reviews'
    }
  })
}

const goEdit = (post) => {
  router.push({
    name: 'create-review-post',
    query: {
      postId: String(post.id || ''),
      activityId: String(post.activityId || ''),
      backTo: '/personal/my-reviews'
    }
  })
}

const deletePost = (post) => {
  confirm({
    title: '删除评价',
    message: '确定删除这条评价吗？删除后无法恢复。',
    confirmText: '删除',
    cancelText: '取消',
    onConfirm: async () => {
      try {
        const response = await deleteMyDiscoverReviewPost(post.id)
        if (response.code !== 200) {
          throw new Error(response.message || '删除失败')
        }
        notify.success('删除成功')
        await loadData()
      } catch (error) {
        notify.error(error.message || '删除失败')
      }
    }
  })
}

const openPreview = async (post) => {
  try {
    const response = await getDiscoverPostDetail(post.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '加载失败')
    }
    previewPost.value = response.data
    showPreview.value = true
  } catch (error) {
    notify.error(error.message || '加载失败')
  }
}

const closePreview = () => {
  showPreview.value = false
  previewPost.value = null
}

const openLinkedActivity = (payload) => {
  const activityId = Number(payload?.id || payload?.activityId || payload?.sourceId || 0)
  if (!activityId) return
  selectedActivityId.value = String(activityId)
  showActivityDetail.value = true
}

const getActivityTitle = (activityId) => {
  const booking = bookings.value.find(item => Number(item.activityId || 0) === Number(activityId || 0))
  return booking?.activityTitle || ''
}

const getExcerpt = (text) => {
  const value = String(text || '').trim()
  if (!value) return '暂无文字内容'
  return value.length > 80 ? `${value.slice(0, 80)}...` : value
}

const formatLocation = (booking) => {
  return [booking.locationProvince, booking.locationCity, booking.locationDistrict, booking.locationDetail].filter(Boolean).join(' / ') || '地点待补充'
}

const formatRange = (start, end) => {
  const format = value => value ? new Date(value).toLocaleString('zh-CN') : '待定'
  const startText = format(start)
  const endText = end ? format(end) : ''
  return endText ? `${startText} - ${endText}` : startText
}

const handleAuthChanged = () => {
  loadData()
}

onMounted(() => {
  loadData()
  window.addEventListener('auth-changed', handleAuthChanged)
})

onActivated(() => {
  if (skipNextActivationReload.value) {
    skipNextActivationReload.value = false
    return
  }
  loadData()
})

onBeforeUnmount(() => {
  window.removeEventListener('auth-changed', handleAuthChanged)
})
</script>

<style scoped>
.my-reviews-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 76px 20px 80px;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.summary-card,
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.hero-card {
  padding: 28px;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  background: linear-gradient(135deg, #fff, #fff7f2);
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.eyebrow {
  margin: 0 0 8px;
  color: #9d2929;
  letter-spacing: 0.12em;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0 0 10px;
  font-size: 30px;
  color: #111827;
}

.hero-copy p {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.refresh-btn,
.primary-btn,
.danger-btn,
.write-btn {
  border: none;
  cursor: pointer;
}

.floating-back-btn {
  position: absolute;
  top: 28px;
  left: 20px;
  width: 46px;
  height: 46px;
  border: none;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: #9d2929;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.14);
  border: 1px solid #f3d7c7;
  cursor: pointer;
  z-index: 2;
}

.floating-back-btn:hover {
  transform: translateY(-1px);
  background: #fff7f2;
}

.refresh-btn {
  padding: 12px 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, #c04851, #e18b32);
  color: #fff;
  font-weight: 700;
}

.refresh-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  width: 100%;
  text-align: left;
  appearance: none;
  border: 1px solid #e5e7eb;
  padding: 18px;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  cursor: pointer;
}

.summary-card:hover,
.summary-card.active {
  transform: translateY(-2px);
  border-color: #d87c41;
  box-shadow: 0 20px 40px rgba(192, 72, 81, 0.12);
}

.summary-label {
  display: block;
  color: #6b7280;
  margin-bottom: 8px;
}

.summary-card strong {
  font-size: 28px;
  color: #111827;
}

.panel-header > div {
  min-width: 0;
}

.panel-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.panel {
  padding: 22px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-header h2 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.panel-header span {
  color: #6b7280;
}

.empty-state {
  padding: 28px 16px;
  text-align: center;
  color: #6b7280;
  background: #fafafa;
  border-radius: 16px;
}

.review-list,
.pending-grid {
  display: grid;
  gap: 14px;
}

.review-card,
.pending-card {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: stretch;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid #f1f5f9;
  background: #fff;
}

.cover {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 16px;
  background: #f3f4f6;
}

.review-main,
.pending-main {
  min-width: 0;
}

.review-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.title-block {
  min-width: 0;
}

.title-block h3,
.pending-main h3 {
  margin: 0 0 6px;
  color: #111827;
  font-size: 18px;
}

.meta-line {
  margin: 0;
  color: #6b7280;
}

.badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.badge {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #9d2929;
  background: #fff4ec;
}

.badge.subtle {
  color: #6b7280;
  background: #f1f5f9;
}

.activity-badge {
  cursor: pointer;
}

.score-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 14px 0 10px;
}

.score-text {
  font-size: 24px;
  font-weight: 700;
  color: #c04851;
}

.stars {
  display: inline-flex;
  gap: 4px;
  color: #f59e0b;
}

.excerpt {
  margin: 0 0 10px;
  color: #374151;
  line-height: 1.7;
}

.time-line {
  margin: 0;
  color: #9ca3af;
  font-size: 13px;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  justify-content: center;
}

.primary-btn,
.danger-btn,
.write-btn {
  padding: 10px 16px;
  border-radius: 999px;
  font-weight: 600;
}

.primary-btn {
  background: #fff4ec;
  color: #9d2929;
}

.danger-btn {
  background: #fee2e2;
  color: #b91c1c;
}

.write-btn {
  align-self: center;
  background: linear-gradient(135deg, #c04851, #e18b32);
  color: #fff;
  height: fit-content;
}

@media (max-width: 960px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .review-card,
  .pending-card {
    grid-template-columns: 100px minmax(0, 1fr);
  }

  .actions,
  .write-btn {
    grid-column: 1 / -1;
    align-self: start;
  }

  .cover {
    width: 100px;
    height: 100px;
  }
}

@media (max-width: 640px) {
  .my-reviews-page {
    padding: 16px 14px 60px;
  }

  .hero-card {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }

  .review-top {
    flex-direction: column;
  }

  .badges {
    justify-content: flex-start;
  }

  .review-card,
  .pending-card {
    grid-template-columns: 1fr;
  }

  .cover {
    width: 100%;
    height: 180px;
  }

  .actions {
    flex-direction: row;
  }
}
</style>