<template>
  <div class="checkins-page">
    <section class="hero-card">
      <div class="hero-copy">
        <button class="back-btn" @click="goBack">
          <i class="bx bx-arrow-back"></i>
          <span>返回个人中心</span>
        </button>
        <p class="eyebrow">签到中心</p>
        <h1>我的签到二维码</h1>
        <p>
          仅显示已支付且有效的报名。打开报名后可显示商家核销用的二维码。
        </p>
      </div>
      <button class="ghost-btn" :disabled="loading" @click="loadData">
        {{ loading ? '加载中...' : '刷新' }}
      </button>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <span class="summary-label">总计</span>
        <strong>{{ summary.total }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">待核销</span>
        <strong>{{ summary.ready }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">已核销</span>
        <strong>{{ summary.checkedIn }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">已完成</span>
        <strong>{{ summary.completed }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>可核销报名</h2>
        <span>{{ checkinBookings.length }} 条</span>
      </div>

      <div v-if="loading" class="empty-state">正在加载签到报名...</div>
      <div v-else-if="checkinBookings.length === 0" class="empty-state">
        暂无已支付可核销的报名。
      </div>

      <div v-else class="card-list">
        <article v-for="booking in checkinBookings" :key="booking.id" class="booking-card">
          <img
            v-if="booking.activityCoverImage"
            :src="booking.activityCoverImage"
            :alt="booking.activityTitle || '活动封面'"
            class="cover-image"
          >

          <div class="card-main">
            <div class="title-row">
              <div>
                <h3>{{ booking.activityTitle || '活动报名' }}</h3>
                <p class="subtitle">{{ booking.shopName || booking.merchantName || '商家' }}</p>
              </div>
              <span :class="['status-tag', displayStatus(booking).code]">
                {{ displayStatus(booking).label }}
              </span>
            </div>

            <div class="meta-grid">
              <p class="meta-line">报名编号：{{ booking.reserveNo || '-' }}</p>
              <p class="meta-line">时间：{{ formatRange(booking.startTime, booking.endTime) }}</p>
              <p class="meta-line">地点：{{ formatLocation(booking) }}</p>
              <p class="meta-line">支付时间：{{ formatTime(booking.paymentTime) }}</p>
              <p v-if="booking.verificationTime" class="meta-line">核销时间：{{ formatTime(booking.verificationTime) }}</p>
            </div>

            <p class="status-note" :class="noteClass(booking)">
              {{ statusDescription(booking) }}
            </p>
          </div>

          <div class="card-actions">
            <button class="teal-btn" @click="openCheckin(booking)">
              打开二维码
            </button>
            <button class="detail-btn" @click="openDetail(booking)">
              订单详情
            </button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyOrderOverview } from '../api/app'
import { canOpenActivityCheckin, resolveActivityBookingDisplayStatus } from '../utils/activityBooking'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const bookings = ref([])

const checkinBookings = computed(() => bookings.value.filter(item => canOpenActivityCheckin(item)))

const summary = computed(() => {
  const next = {
    total: checkinBookings.value.length,
    ready: 0,
    checkedIn: 0,
    completed: 0
  }

  checkinBookings.value.forEach((booking) => {
    const code = displayStatus(booking).code
    if (code === 'checked_in') {
      next.checkedIn += 1
      return
    }
    if (code === 'completed') {
      next.completed += 1
      return
    }
    next.ready += 1
  })

  return next
})

const displayStatus = (booking) => resolveActivityBookingDisplayStatus(booking)

const statusDescription = (booking) => {
  const code = displayStatus(booking).code
  if (code === 'checked_in') {
    return '该报名已核销，但仍可在此重新打开记录。'
  }
  if (code === 'completed') {
    return '活动已结束。报名记录保留供查看。'
  }
  return '打开二维码后，向现场商家出示进行核销。'
}

const noteClass = (booking) => {
  const code = displayStatus(booking).code
  return code === 'checked_in' || code === 'completed' ? 'success-note' : 'info-note'
}

const loadData = async () => {
  loading.value = true
  try {
    const response = await getMyOrderOverview()
    if (response.code !== 200) {
      throw new Error(response.message || '加载签到报名失败')
    }
    bookings.value = Array.isArray(response.data?.activityBookings) ? response.data.activityBookings : []
  } catch (error) {
    notify?.error?.(error.message || '加载签到报名失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/home/personal')
}

const openCheckin = (booking) => {
  router.push({
    name: 'activity-booking-checkin',
    params: { id: booking.id },
    query: { backTo: route.fullPath }
  })
}

const openDetail = (booking) => {
  router.push({
    name: 'activity-booking-detail',
    params: { id: booking.id },
    query: { backTo: route.fullPath }
  })
}

const formatTime = (value) => (value ? new Date(value).toLocaleString() : '-')
const formatRange = (start, end) => {
  const startText = start ? formatTime(start) : '待定'
  const endText = end ? formatTime(end) : ''
  return endText ? `${startText} - ${endText}` : startText
}
const formatLocation = (booking) => (
  [booking.locationProvince, booking.locationCity, booking.locationDistrict, booking.locationDetail]
    .filter(Boolean)
    .join(' / ') || '待定'
)

onMounted(loadData)
</script>

<style scoped>
.checkins-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 28px 20px 80px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.panel,
.summary-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.hero-card {
  padding: 28px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.18), transparent 30%),
    linear-gradient(135deg, #ffffff, #ecfeff);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.back-btn,
.ghost-btn,
.teal-btn,
.detail-btn {
  border: none;
  cursor: pointer;
}

.back-btn {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #1e293b;
}

.ghost-btn {
  padding: 12px 18px;
  border-radius: 14px;
  background: #111827;
  color: #fff;
}

.eyebrow {
  margin: 0;
  color: #0f766e;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 12px;
  font-weight: 700;
}

.hero-card h1 {
  margin: 0;
  font-size: 30px;
  color: #111827;
}

.hero-card p {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
  max-width: 760px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-label {
  color: #6b7280;
  font-size: 14px;
}

.summary-card strong {
  font-size: 30px;
  color: #111827;
}

.panel {
  padding: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.panel-header h2 {
  margin: 0;
  font-size: 22px;
  color: #111827;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.booking-card {
  border: 1px solid #eef2f7;
  border-radius: 18px;
  padding: 18px;
  display: flex;
  gap: 18px;
  background: #fbfdff;
}

.cover-image {
  width: 220px;
  height: 160px;
  object-fit: cover;
  border-radius: 18px;
  flex-shrink: 0;
}

.card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.title-row h3 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #111827;
}

.subtitle,
.meta-line {
  margin: 0;
  color: #64748b;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-tag.pending_start,
.status-tag.pending_checkin {
  background: #cffafe;
  color: #0f766e;
}

.status-tag.checked_in,
.status-tag.completed {
  background: #dcfce7;
  color: #15803d;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 18px;
}

.status-note {
  margin: 0;
  line-height: 1.7;
  padding: 10px 12px;
  border-radius: 12px;
}

.info-note {
  background: #ecfeff;
  color: #0f766e;
}

.success-note {
  background: #ecfdf5;
  color: #15803d;
}

.card-actions {
  width: 154px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.teal-btn,
.detail-btn {
  width: 100%;
  border-radius: 12px;
  padding: 11px 14px;
  font-weight: 600;
}

.teal-btn {
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  color: #fff;
}

.detail-btn {
  background: #e2e8f0;
  color: #1e293b;
}

.empty-state {
  padding: 32px 18px;
  text-align: center;
  color: #94a3b8;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

@media (max-width: 960px) {
  .summary-grid,
  .meta-grid {
    grid-template-columns: 1fr 1fr;
  }

  .hero-card,
  .booking-card {
    flex-direction: column;
  }

  .card-actions,
  .cover-image {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .checkins-page {
    padding: 16px 12px 60px;
  }

  .summary-grid,
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
