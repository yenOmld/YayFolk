<template>
  <div class="merchant-analysis-page">

    <section class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">商家工作台</p>
        <h1>数据分析</h1>
        <p>一站式跟踪活动、预订、收入和评价趋势。</p>
      </div>
      <div class="hero-actions">
        <button class="ai-btn" type="button" @click="openAISuggestions">
          <i class="bx bx-brain"></i>
          <span>AI 建议</span>
        </button>
        <button class="refresh-btn" :disabled="loading" @click="loadStats">
          {{ loading ? '刷新中...' : '刷新数据' }}
        </button>
      </div>
    </section>

    <section v-if="errorMessage" class="message-card">
      {{ errorMessage }}
    </section>

    <section class="panel">
      <MerchantStatsPanel
        :stats="stats"
        :loading="loading"
        :visible="true"
        :show-recent-reviews="false"
        @navigate="handleNavigate"
      />
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MerchantStatsPanel from '@/components/merchant/MerchantStatsPanel.vue'
import { getMerchantStats } from '@/api/app.js'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const errorMessage = ref('')
const stats = ref(readMerchantStatsCache() || createEmptyMerchantStats())

const MERCHANT_STATS_CACHE_KEY = 'merchant:stats:cache'

function currentMerchantCacheScope() {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return 'anonymous'
  }

  try {
    const user = JSON.parse(raw)
    const merchantId = user?.id !== undefined && user?.id !== null ? String(user.id) : ''
    const username = user?.username ? String(user.username) : ''
    return merchantId || username || 'anonymous'
  } catch (error) {
    return 'anonymous'
  }
}

function scopedCacheKey(baseKey) {
  return `${baseKey}:${currentMerchantCacheScope()}`
}

function readJsonCache(key) {
  const raw = localStorage.getItem(key)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}

function writeJsonCache(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
    // ignore cache write errors
  }
}

function readMerchantStatsCache() {
  const cached = readJsonCache(scopedCacheKey(MERCHANT_STATS_CACHE_KEY))
  if (!cached || typeof cached !== 'object') {
    return null
  }

  const payload = cached.data && typeof cached.data === 'object' ? cached.data : cached
  const normalized = normalizeStatsPayload(payload)
  const summary = normalized.summary || {}
  const hasAnyValue = [
    summary.activityCount,
    summary.bookingCount,
    summary.pendingCheckinCount,
    summary.checkedInCount,
    summary.rejectedCount,
    summary.cancelledCount,
    summary.reviewCount,
    summary.totalRevenue,
    summary.bookingRevenue,
    summary.averageScore,
    summary.followerCount,
    summary.uniqueCustomerCount
  ].some(value => Number(value || 0) > 0)
    || normalized.bookingStatus.some(item => Number(item?.count || 0) > 0)
    || normalized.salesTrend.some(item => Number(item?.bookingCount || 0) > 0 || Number(item?.bookingRevenue || 0) > 0)
    || normalized.topActivities.length > 0

  return hasAnyValue ? normalized : null
}

function saveMerchantStatsCache(value) {
  writeJsonCache(scopedCacheKey(MERCHANT_STATS_CACHE_KEY), {
    merchantScope: currentMerchantCacheScope(),
    savedAt: Date.now(),
    data: value
  })
}

function createRecentSalesTrend(days = 7) {
  const today = new Date()
  const salesTrend = []

  for (let offset = days - 1; offset >= 0; offset -= 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - offset)
    const dateKey = date.toISOString().slice(0, 10)
    salesTrend.push({
      date: dateKey,
      label: dateKey.slice(5),
      bookingCount: 0,
      participantCount: 0,
      bookingRevenue: 0
    })
  }

  return salesTrend
}

function createEmptyMerchantStats() {
  return {
    summary: {
      activityCount: 0,
      bookingCount: 0,
      pendingCheckinCount: 0,
      checkedInCount: 0,
      rejectedCount: 0,
      cancelledCount: 0,
      reviewCount: 0,
      totalRevenue: 0,
      bookingRevenue: 0,
      averageScore: 0,
      followerCount: 0,
      uniqueCustomerCount: 0
    },
    bookingStatus: [
      { key: 'registered', label: 'Pending Check-ins', color: '#1661ab', count: 0 },
      { key: 'checked_in', label: 'Checked In', color: '#1f8a70', count: 0 },
      { key: 'rejected', label: 'Rejected', color: '#c04851', count: 0 },
      { key: 'cancelled', label: 'Cancelled', color: '#6b7280', count: 0 }
    ],
    salesTrend: createRecentSalesTrend(),
    topActivities: [],

  }
}

function normalizeStatsPayload(payload) {
  const base = createEmptyMerchantStats()
  const statsPayload = payload && typeof payload === 'object' ? payload : {}
  return {
    summary: {
      ...base.summary,
      ...(statsPayload.summary || {})
    },
    bookingStatus: Array.isArray(statsPayload.bookingStatus) && statsPayload.bookingStatus.length
      ? statsPayload.bookingStatus
      : base.bookingStatus,
    salesTrend: Array.isArray(statsPayload.salesTrend) && statsPayload.salesTrend.length
      ? statsPayload.salesTrend
      : base.salesTrend,
    topActivities: Array.isArray(statsPayload.topActivities) ? statsPayload.topActivities : base.topActivities,

  }
}

function withTimeout(promise, ms = 5000) {
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



const openAISuggestions = () => {
  router.push('/ai-suggestions')
}

const handleNavigate = (target) => {
  if (!target?.type) {
    return
  }

  if (target.type === 'activities') {
    router.push('/merchant/activities')
    return
  }

  if (target.type === 'bookings') {
    router.push({
      path: '/merchant/bookings',
      query: {
        ...(target.status ? { status: target.status } : {}),
        backTo: route.fullPath
      }
    })
    return
  }

  if (target.type === 'activity' && target.activityId) {
    router.push({
      path: '/merchant/bookings',
      query: {
        activityId: String(target.activityId),
        ...(target.title ? { title: target.title } : {}),
        status: 'all',
        backTo: route.fullPath
      }
    })
  }
}

async function loadStats() {
  loading.value = true
  errorMessage.value = ''
  const cachedStats = readMerchantStatsCache()

  if (cachedStats) {
    stats.value = cachedStats
  }

  try {
    const response = await withTimeout(getMerchantStats().catch(() => null), 3500)
    if (!response || response.code !== 200) {
      throw new Error(response?.message || '加载商家分析数据失败')
    }

    stats.value = normalizeStatsPayload(response.data || {})
    saveMerchantStatsCache(stats.value)
  } catch (error) {
    if (cachedStats) {
      errorMessage.value = '实时分析暂时不可用，已显示缓存数据。'
      return
    }

    stats.value = createEmptyMerchantStats()
    errorMessage.value = error.message || '加载商家分析数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.merchant-analysis-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 20px 20px;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.panel,
.message-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.hero-card {
  padding: 4px 2px 0;
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

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-btn {
  padding: 12px 16px;
  border-radius: 999px;
  border: 1px solid #d8dce3;
  background: #fff;
  color: #1f2937;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  cursor: pointer;
}

.ai-btn i {
  font-size: 18px;
  color: #9d2929;
}

.refresh-btn {
  padding: 12px 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, #c04851, #e18b32);
  color: #fff;
  font-weight: 700;
  border: none;
  cursor: pointer;
  min-height: 42px;
}

.refresh-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.message-card {
  padding: 14px 18px;
  color: #9d2929;
}

.panel {
  padding: 24px;
}

@media (max-width: 640px) {
  .merchant-analysis-page {
    padding: 66px 14px 70px;
  }

  .hero-card {
    padding: 22px;
    flex-direction: column;
  }
}
</style>