<template>
  <div class="merchant-review-page">


    <header class="page-toolbar">
      <div class="hero-copy">
        <p class="eyebrow">商家后台</p>
        <h1>活动评价</h1>
      </div>
      <div class="hero-actions">
        <button class="refresh-btn" :disabled="pageLoading" @click="loadData">
          {{ pageLoading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </header>

    <section class="panel">
      <MerchantReviewPanel
        :reviews="merchantReviewPosts"
        :activities="merchantActivities"
        :loading="reviewsLoading && merchantReviewPosts.length === 0"
        :show-summary="true"
        :lift-summary="false"
        empty-text="暂无活动评价。"
        @open-activity="openLinkedActivity"
      />
    </section>

    <ActivityDetailModal
      :visible="showActivityDetail"
      :activity-id="selectedActivityIdForDetail"
      :z-index="1950"
      @close="showActivityDetail = false"
    />
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onActivated, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMerchantActivities, getMerchantReviewPosts } from '@/api/app'
import ActivityDetailModal from '@/components/ActivityDetailModal.vue'
import MerchantReviewPanel from '@/components/merchant/MerchantReviewPanel.vue'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const router = useRouter()

const defaultCover = 'https://api.dicebear.com/7.x/shapes/svg?seed=merchant-review'
const activitiesLoading = ref(false)
const reviewsLoading = ref(false)
const merchantActivities = ref([])
const merchantReviewPosts = ref([])
const showActivityDetail = ref(false)
const selectedActivityIdForDetail = ref('')
const skipNextActivationReload = ref(true)

const pageLoading = computed(() => merchantReviewPosts.value.length === 0 && (activitiesLoading.value || reviewsLoading.value))

function extractList(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.items)) return payload.items
  if (payload && Array.isArray(payload.records)) return payload.records
  if (payload && Array.isArray(payload.list)) return payload.list
  return []
}

function withTimeout(promise, ms = 3500) {
  let timerId
  const timeout = new Promise(resolve => {
    timerId = window.setTimeout(() => resolve(null), ms)
  })

  return Promise.race([
    promise.finally(() => {
      if (timerId) {
        window.clearTimeout(timerId)
      }
    }),
    timeout
  ])
}

function normalizeActivity(activity) {
  return {
    id: activity?.id,
    title: activity?.title || '未命名活动'
  }
}

function normalizeReviewPost(post) {
  return {
    ...post,
    createTime: post.createTime || post.time || '',
    targetName: post.targetName || post.activityTitle || '',
    authorName: post.authorName || post.nickname || post.username || '',
    authorAvatar: post.authorAvatar || post.avatar || defaultCover
  }
}



async function loadActivities() {
  activitiesLoading.value = true
  try {
    const response = await withTimeout(getMerchantActivities().catch(() => null), 3500)
    if (!response || response.code !== 200) {
      throw new Error(response?.message || '加载活动失败')
    }
    merchantActivities.value = extractList(response.data).map(normalizeActivity).filter(item => item.id !== undefined && item.id !== null)
  } catch (error) {
    merchantActivities.value = []
    notify.error(error.message || '加载活动失败')
  } finally {
    activitiesLoading.value = false
  }
}

async function loadReviews() {
  reviewsLoading.value = true
  try {
    const response = await withTimeout(getMerchantReviewPosts().catch(() => null), 3500)
    if (!response || response.code !== 200) {
      throw new Error(response?.message || '加载活动评价失败')
    }
    merchantReviewPosts.value = extractList(response.data).map(normalizeReviewPost)
      .sort((left, right) => {
        const leftTime = new Date(left.createTime || left.updateTime || 0).getTime() || 0
        const rightTime = new Date(right.createTime || right.updateTime || 0).getTime() || 0
        return rightTime - leftTime
      })
  } catch (error) {
    merchantReviewPosts.value = []
    notify.error(error.message || '加载活动评价失败')
  } finally {
    reviewsLoading.value = false
  }
}

async function loadData() {
  await Promise.allSettled([loadActivities(), loadReviews()])
}

function openLinkedActivity(payload) {
  const activityId = Number(payload?.id || payload || 0)
  if (!activityId) return
  selectedActivityIdForDetail.value = String(activityId)
  showActivityDetail.value = true
}

function handleAuthChanged() {
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
.merchant-review-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 20px 20px;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-toolbar,
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.page-toolbar {
  padding: 0;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  background: transparent;
  border: 0;
  box-shadow: none;
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

.hero-actions,
.panel-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.panel {
  padding: 18px;
}

.refresh-btn {
  border-radius: 14px;
  border: 1px solid #d8dce3;
  background: #fff;
  min-height: 42px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #1f2937;
  cursor: pointer;
}

.refresh-btn:disabled {
  cursor: wait;
  opacity: 0.72;
}

@media (max-width: 768px) {
  .merchant-review-page {
    padding: 72px 16px 64px;
  }

  .hero-card {
    padding: 22px;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>