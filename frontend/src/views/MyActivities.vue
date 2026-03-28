<template>
  <div class="activities-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">My Activities</p>
        <h1>我的活动报名</h1>
        <p>
          这里会展示你报名过的全部活动，并根据活动时间和核销情况自动显示当前状态，
          包括待开始、待签到/核销、已签到/核销、已完成和已取消。
        </p>
      </div>
      <button class="ghost-btn" @click="loadData" :disabled="loading">
        {{ loading ? '加载中...' : '刷新记录' }}
      </button>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <span class="summary-label">报名总数</span>
        <strong>{{ summary.total }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">待参与</span>
        <strong>{{ summary.pending }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">已完成</span>
        <strong>{{ summary.completed }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">已取消/结束</span>
        <strong>{{ summary.closed }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>活动记录</h2>
        <span>{{ bookings.length }} 条</span>
      </div>

      <div v-if="loading" class="empty-state">正在加载活动报名记录...</div>
      <div v-else-if="bookings.length === 0" class="empty-state">
        暂无活动报名记录，去首页挑一场喜欢的非遗活动吧。
      </div>

      <div v-else class="card-list">
        <article v-for="booking in bookings" :key="booking.id" class="booking-card">
          <img
            v-if="booking.activityCoverImage"
            :src="booking.activityCoverImage"
            alt="活动封面"
            class="cover-image"
          />

          <div class="card-main">
            <div class="title-row">
              <div>
                <h3>{{ booking.activityTitle || '非遗活动报名' }}</h3>
                <p class="subtitle">{{ booking.activitySubtitle || booking.heritageType || '线下文化体验' }}</p>
              </div>
              <span :class="['status-tag', bookingDisplayStatus(booking).code]">
                {{ bookingDisplayStatus(booking).label }}
              </span>
            </div>

            <div class="meta-grid">
              <p class="meta-line">报名人：{{ booking.participantName || '-' }} / {{ booking.participantPhone || '-' }}</p>
              <p class="meta-line">活动时间：{{ formatTime(booking.startTime) }} - {{ formatTime(booking.endTime) }}</p>
              <p class="meta-line">活动地点：{{ [booking.locationCity, booking.locationDetail].filter(Boolean).join(' ') || '-' }}</p>
              <p class="meta-line">商家：{{ booking.shopName || booking.merchantName || '-' }}</p>
              <p class="meta-line">报名时间：{{ formatTime(booking.createTime) }}</p>
              <p class="meta-line" v-if="booking.verificationTime">核销时间：{{ formatTime(booking.verificationTime) }}</p>
            </div>

            <p v-if="booking.remark" class="remark-line">备注：{{ booking.remark }}</p>
            <p class="status-note" :class="statusNoteClass(booking)">
              {{ bookingDisplayStatus(booking).description }}
            </p>
          </div>

          <div class="card-actions">
            <button
              v-if="canCancelBooking(booking)"
              class="danger-btn"
              :disabled="cancellingBookingId === booking.id"
              @click="handleCancelBooking(booking)"
            >
              {{ cancellingBookingId === booking.id ? '取消中...' : '取消报名' }}
            </button>
            <button
              v-else-if="canDeleteBooking(booking)"
              class="secondary-btn"
              :disabled="deletingBookingId === booking.id"
              @click="handleDeleteBooking(booking)"
            >
              {{ deletingBookingId === booking.id ? '删除中...' : '删除记录' }}
            </button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { cancelActivityBooking, deleteActivityBooking, getMyOrderOverview } from '../api/app'
import {
  isCancelableActivityBooking,
  isDeletableActivityBooking,
  resolveActivityBookingDisplayStatus
} from '../utils/activityBooking'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const loading = ref(false)
const cancellingBookingId = ref(null)
const deletingBookingId = ref(null)
const bookings = ref([])
const summary = ref({
  total: 0,
  pending: 0,
  completed: 0,
  closed: 0
})

const bookingDisplayStatus = (booking) => resolveActivityBookingDisplayStatus(booking)
const canCancelBooking = (booking) => isCancelableActivityBooking(booking)
const canDeleteBooking = (booking) => isDeletableActivityBooking(booking)

const statusNoteClass = (booking) => {
  const code = bookingDisplayStatus(booking).code
  if (code === 'completed' || code === 'checked_in') {
    return 'success-note'
  }
  if (code === 'rejected') {
    return 'warning-note'
  }
  if (code === 'cancelled' || code === 'missed') {
    return 'muted-note'
  }
  return 'info-note'
}

const buildSummary = (list) => {
  const next = {
    total: list.length,
    pending: 0,
    completed: 0,
    closed: 0
  }

  list.forEach((booking) => {
    const code = bookingDisplayStatus(booking).code
    if (code === 'pending_start' || code === 'pending_checkin' || code === 'checked_in') {
      next.pending += 1
      return
    }
    if (code === 'completed') {
      next.completed += 1
      return
    }
    next.closed += 1
  })

  summary.value = next
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyOrderOverview()
    const list = Array.isArray(res.data?.activityBookings) ? res.data.activityBookings : []
    bookings.value = list
    buildSummary(list)
  } catch (error) {
    notify?.error(error?.response?.data?.message || '加载活动记录失败')
  } finally {
    loading.value = false
  }
}

const handleCancelBooking = async (booking) => {
  if (!canCancelBooking(booking) || cancellingBookingId.value) {
    return
  }
  if (!window.confirm(`确认取消活动报名“${booking.activityTitle || '非遗活动'}”吗？`)) {
    return
  }

  cancellingBookingId.value = booking.id
  try {
    const res = await cancelActivityBooking(booking.id)
    notify?.success(res.message || '活动报名已取消')
    await loadData()
  } catch (error) {
    notify?.error(error?.response?.data?.message || '取消活动报名失败')
  } finally {
    cancellingBookingId.value = null
  }
}

const handleDeleteBooking = async (booking) => {
  if (!canDeleteBooking(booking) || deletingBookingId.value) {
    return
  }
  if (!window.confirm(`确认删除已取消的活动记录“${booking.activityTitle || '非遗活动'}”吗？`)) {
    return
  }

  deletingBookingId.value = booking.id
  try {
    const res = await deleteActivityBooking(booking.id)
    notify?.success(res.message || '活动记录已删除')
    await loadData()
  } catch (error) {
    notify?.error(error?.response?.data?.message || '删除活动记录失败')
  } finally {
    deletingBookingId.value = null
  }
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(loadData)
</script>

<style scoped>
.activities-page {
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
  align-items: center;
  background:
    radial-gradient(circle at top right, rgba(13, 148, 136, 0.16), transparent 30%),
    linear-gradient(135deg, #ffffff, #f0fdfa);
}

.eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 12px;
  font-weight: 700;
}

.hero-card h1 {
  margin: 0 0 8px;
  font-size: 30px;
  color: #111827;
}

.hero-card p {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
  max-width: 720px;
}

.ghost-btn,
.danger-btn,
.secondary-btn {
  border: none;
  cursor: pointer;
  transition: 0.2s ease;
}

.ghost-btn {
  padding: 12px 18px;
  border-radius: 14px;
  background: #111827;
  color: #fff;
}

.ghost-btn:disabled,
.danger-btn:disabled,
.secondary-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
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
  font-size: 20px;
  color: #111827;
}

.panel-header span,
.meta-line,
.remark-line,
.subtitle {
  color: #6b7280;
}

.subtitle {
  margin: 6px 0 0;
  font-size: 13px;
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
  align-items: flex-start;
}

.cover-image {
  width: 144px;
  height: 144px;
  border-radius: 16px;
  object-fit: cover;
  flex-shrink: 0;
}

.card-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.title-row h3 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
}

.meta-line {
  margin: 0;
  line-height: 1.6;
}

.status-tag {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.status-tag.pending_start {
  background: #eff6ff;
  color: #2563eb;
}

.status-tag.pending_checkin {
  background: #fff7ed;
  color: #c2410c;
}

.status-tag.checked_in,
.status-tag.completed {
  background: #ecfdf5;
  color: #059669;
}

.status-tag.cancelled {
  background: #fef2f2;
  color: #dc2626;
}

.status-tag.rejected {
  background: #fef3c7;
  color: #b45309;
}

.status-tag.missed {
  background: #f8fafc;
  color: #64748b;
}

.danger-btn {
  padding: 10px 14px;
  border-radius: 12px;
  background: #fef2f2;
  color: #dc2626;
}

.danger-btn:hover:not(:disabled) {
  background: #fee2e2;
}

.secondary-btn {
  padding: 10px 14px;
  border-radius: 12px;
  background: #f3f4f6;
  color: #374151;
}

.secondary-btn:hover:not(:disabled) {
  background: #e5e7eb;
}

.status-note {
  margin: 0;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.6;
}

.success-note {
  background: #ecfdf5;
  color: #047857;
}

.info-note {
  background: #eff6ff;
  color: #1d4ed8;
}

.warning-note {
  background: #fffbeb;
  color: #b45309;
}

.muted-note {
  background: #f8fafc;
  color: #64748b;
}

.empty-state {
  padding: 32px 18px;
  text-align: center;
  color: #94a3b8;
}

@media (max-width: 900px) {
  .summary-grid,
  .meta-grid {
    grid-template-columns: 1fr;
  }

  .hero-card,
  .booking-card {
    flex-direction: column;
  }

  .cover-image {
    width: 100%;
    height: 220px;
  }

  .title-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
