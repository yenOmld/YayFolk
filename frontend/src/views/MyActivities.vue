<template>
  <div class="activities-page">
    <section class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">活动中心</p>
        <h1>我的活动报名</h1>
        <p>
          在这里管理您的所有活动报名。您可以继续支付、打开核销二维码、
          在活动结束后留下评价，或管理已取消的报名记录。
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
        <span class="summary-label">待支付</span>
        <strong>{{ summary.payable }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">即将开始</span>
        <strong>{{ summary.upcoming }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">已完成</span>
        <strong>{{ summary.completed }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>报名列表</h2>
        <span>{{ bookings.length }} 项</span>
      </div>

      <div v-if="loading" class="empty-state">加载活动报名中...</div>
      <div v-else-if="bookings.length === 0" class="empty-state">
        暂无活动报名。去发现您喜欢的活动吧。
      </div>

      <div v-else class="card-list">
        <article v-for="booking in bookings" :key="booking.id" class="booking-card">
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
                <p class="subtitle">{{ booking.activitySubtitle || booking.heritageType || '文化活动' }}</p>
              </div>
              <span :class="['status-tag', displayStatus(booking).code]">
                {{ displayStatus(booking).label }}
              </span>
            </div>

            <div class="meta-grid">
              <p class="meta-line">订单号: {{ booking.reserveNo || '-' }}</p>
              <p class="meta-line">支付状态: {{ paymentText(booking.paymentStatus) }}</p>
              <p class="meta-line">联系人: {{ booking.participantName || '-' }}</p>
              <p class="meta-line">电话: {{ booking.participantPhone || '-' }}</p>
              <p class="meta-line">日程: {{ formatRange(booking.startTime, booking.endTime) }}</p>
              <p class="meta-line">地点: {{ formatLocation(booking) }}</p>
              <p class="meta-line">金额: {{ formatMoney(booking.payAmount ?? booking.totalAmount) }}</p>
              <p class="meta-line">创建时间: {{ formatTime(booking.createTime) }}</p>
              <p v-if="booking.paymentTime" class="meta-line">支付时间: {{ formatTime(booking.paymentTime) }}</p>
              <p v-if="booking.verificationTime" class="meta-line">核销时间: {{ formatTime(booking.verificationTime) }}</p>
            </div>

            <p v-if="booking.remark" class="remark-line">备注: {{ booking.remark }}</p>
            <p class="status-note" :class="noteClass(booking)">
              {{ statusDescription(booking) }}
            </p>
          </div>

          <div class="card-actions">
            <button v-if="canPay(booking)" class="primary-btn" @click="openPayment(booking)">
              立即支付
            </button>
            <button v-if="canOpenCheckin(booking)" class="teal-btn" @click="openCheckin(booking)">
              打开二维码
            </button>
            <button class="detail-btn" @click="openDetail(booking)">
              报名详情
            </button>
            <button v-if="canReview(booking)" class="review-btn" @click="openReview(booking)">
              评价
            </button>
            <button
              v-if="canCancel(booking)"
              class="danger-btn"
              :disabled="busyId === booking.id"
              @click="handleCancel(booking)"
            >
              {{ busyId === booking.id ? '取消中...' : '取消报名' }}
            </button>
            <button
              v-else-if="canDelete(booking)"
              class="secondary-btn"
              :disabled="busyId === booking.id"
              @click="handleDelete(booking)"
            >
              {{ busyId === booking.id ? '删除中...' : '删除记录' }}
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
import { cancelActivityBooking, deleteActivityBooking, getMyOrderOverview } from '../api/app'
import {
  canOpenActivityCheckin,
  isCancelableActivityBooking,
  isDeletableActivityBooking,
  isPayableActivityBooking,
  isReviewableActivityBooking,
  resolveActivityBookingDisplayStatus
} from '../utils/activityBooking'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const busyId = ref(null)
const bookings = ref([])

const summary = computed(() => {
  const next = {
    total: bookings.value.length,
    payable: 0,
    upcoming: 0,
    completed: 0
  }

  bookings.value.forEach((booking) => {
    const status = displayStatus(booking).code
    if (canPay(booking)) {
      next.payable += 1
    }
    if (status === 'pending_start' || status === 'pending_checkin' || status === 'checked_in') {
      next.upcoming += 1
    }
    if (status === 'completed') {
      next.completed += 1
    }
  })

  return next
})

const displayStatus = (booking) => resolveActivityBookingDisplayStatus(booking)
const canCancel = (booking) => isCancelableActivityBooking(booking)
const canDelete = (booking) => isDeletableActivityBooking(booking)
const canPay = (booking) => isPayableActivityBooking(booking)
const canOpenCheckin = (booking) => canOpenActivityCheckin(booking)
const canReview = (booking) => isReviewableActivityBooking(booking)

const paymentText = (status) => ({
  paid: '已支付',
  unpaid: '未支付',
  refunded: '已退款'
}[status] || status || '未支付')

const statusDescription = (booking) => {
  if (canPay(booking)) {
    return '支付仍在等待中。完成支付后可解锁二维码和完整的报名流程。'
  }
  if (canReview(booking)) {
    return '该订单现在可以评价。活动结束后分享您的体验。'
  }
  return displayStatus(booking).description
}

const noteClass = (booking) => {
  const code = displayStatus(booking).code
  if (canPay(booking)) {
    return 'warning-note'
  }
  if (code === 'completed' || code === 'checked_in') {
    return 'success-note'
  }
  if (code === 'cancelled' || code === 'missed') {
    return 'muted-note'
  }
  if (code === 'rejected') {
    return 'warning-note'
  }
  return 'info-note'
}

const showError = (message) => notify?.error?.(message)
const showSuccess = (message) => notify?.success?.(message)

const loadData = async () => {
  loading.value = true
  try {
    const response = await getMyOrderOverview()
    if (response.code !== 200) {
      throw new Error(response.message || '加载活动报名失败')
    }
    bookings.value = Array.isArray(response.data?.activityBookings) ? response.data.activityBookings : []
  } catch (error) {
    showError(error.message || '加载活动报名失败')
  } finally {
    loading.value = false
  }
}

const openDetail = (booking, extraQuery = {}) => {
  router.push({
    name: 'activity-booking-detail',
    params: { id: booking.id },
    query: {
      backTo: route.fullPath,
      ...extraQuery
    }
  })
}

const openReview = (booking) => openDetail(booking, { focus: 'review' })

const openPayment = (booking) => {
  router.push({
    name: 'activity-booking-payment',
    params: { id: booking.id },
    query: { backTo: route.fullPath }
  })
}

const openCheckin = (booking) => {
  router.push({
    name: 'activity-booking-checkin',
    params: { id: booking.id },
    query: { backTo: route.fullPath }
  })
}

const handleCancel = async (booking) => {
  if (!canCancel(booking) || busyId.value) {
    return
  }
  confirm({
    title: '取消报名',
    message: `确定取消报名 ${booking.reserveNo || booking.id}？`,
    onConfirm: async () => {
      busyId.value = booking.id
      try {
        const response = await cancelActivityBooking(booking.id)
        if (response.code !== 200) {
          throw new Error(response.message || '取消报名失败')
        }
        showSuccess(response.message || '报名已取消')
        await loadData()
      } catch (error) {
        showError(error.message || error?.response?.data?.message || '取消报名失败')
      } finally {
        busyId.value = null
      }
    }
  })
}

const handleDelete = async (booking) => {
  if (!canDelete(booking) || busyId.value) {
    return
  }
  confirm({
    title: '删除记录',
    message: `确定删除报名记录 ${booking.reserveNo || booking.id}？`,
    onConfirm: async () => {
      busyId.value = booking.id
      try {
        const response = await deleteActivityBooking(booking.id)
        if (response.code !== 200) {
          throw new Error(response.message || '删除报名记录失败')
        }
        showSuccess(response.message || '报名记录已删除')
        await loadData()
      } catch (error) {
        showError(error.message || error?.response?.data?.message || '删除报名记录失败')
      } finally {
        busyId.value = null
      }
    }
  })
}

const formatTime = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '-')
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
const formatMoney = (value) => `¥${(Number(value || 0) / 100).toFixed(2)}`

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
  align-items: flex-start;
  background:
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.16), transparent 28%),
    linear-gradient(135deg, #ffffff, #f0fdfa);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.ghost-btn,
.primary-btn,
.teal-btn,
.detail-btn,
.review-btn,
.danger-btn,
.secondary-btn {
  border: none;
  cursor: pointer;
}

.ghost-btn {
  padding: 12px 18px;
  border-radius: 14px;
  background: #111827;
  color: #fff;
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

.panel-header span,
.meta-line,
.remark-line {
  color: #64748b;
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

.subtitle {
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
  background: #dbeafe;
  color: #1d4ed8;
}

.status-tag.checked_in,
.status-tag.completed {
  background: #dcfce7;
  color: #15803d;
}

.status-tag.cancelled,
.status-tag.missed {
  background: #e5e7eb;
  color: #6b7280;
}

.status-tag.rejected {
  background: #fee2e2;
  color: #b91c1c;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 18px;
}

.meta-line,
.remark-line,
.status-note {
  margin: 0;
  line-height: 1.7;
}

.status-note {
  padding: 10px 12px;
  border-radius: 12px;
}

.info-note {
  background: #eff6ff;
  color: #1d4ed8;
}

.warning-note {
  background: #fff7ed;
  color: #c2410c;
}

.success-note {
  background: #ecfdf5;
  color: #15803d;
}

.muted-note {
  background: #f8fafc;
  color: #64748b;
}

.card-actions {
  width: 154px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.primary-btn,
.teal-btn,
.detail-btn,
.review-btn,
.danger-btn,
.secondary-btn {
  width: 100%;
  border-radius: 12px;
  padding: 11px 14px;
  font-weight: 600;
}

.primary-btn {
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: #fff;
}

.teal-btn {
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  color: #fff;
}

.detail-btn {
  background: #e2e8f0;
  color: #1e293b;
}

.review-btn {
  background: #fef3c7;
  color: #92400e;
}

.danger-btn {
  background: #fee2e2;
  color: #b91c1c;
}

.secondary-btn {
  background: #e5e7eb;
  color: #374151;
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
  .activities-page {
    padding: 16px 12px 60px;
  }

  .summary-grid,
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>