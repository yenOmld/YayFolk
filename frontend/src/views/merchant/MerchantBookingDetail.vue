<template>
  <div class="merchant-booking-detail-page">
    <div class="header-nav">
      <button class="back-btn" @click="goBack">
        <i class="bx bx-arrow-back"></i>
        <span>Back To Bookings</span>
      </button>
      <div class="breadcrumb">
        <span @click="goBack">Merchant Bookings</span>
        <i class="bx bx-chevron-right"></i>
        <span>Booking Detail</span>
      </div>
    </div>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>Loading booking detail...</p>
    </div>

    <div v-else-if="!booking" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>This booking is not available.</p>
    </div>

    <div v-else class="detail-layout">
      <section class="main-column">
        <article class="info-card hero-card">
          <img v-if="coverImage" :src="coverImage" :alt="booking.activityTitle || 'Activity cover'" class="cover-image">
          <div class="hero-copy">
            <div class="title-row">
              <div>
                <p class="eyebrow">Merchant Booking Detail</p>
                <h1>{{ booking.activityTitle || 'Activity Booking' }}</h1>
                <p class="subtitle">{{ booking.activitySubtitle || booking.heritageType || 'Booking detail' }}</p>
              </div>
              <span :class="['status-tag', booking.status]">{{ statusText(booking.status) }}</span>
            </div>

            <div class="meta-grid">
              <p>Booking No: {{ booking.reserveNo || '-' }}</p>
              <p>Activity Time: {{ formatRange(booking.startTime, booking.endTime, booking.activityTime) }}</p>
              <p>Location: {{ fullLocation }}</p>
              <p>Created At: {{ formatTime(booking.createTime) }}</p>
            </div>

            <p v-if="booking.activityContent" class="activity-content">{{ booking.activityContent }}</p>
          </div>
        </article>

        <article class="info-card">
          <h2>Booking Info</h2>
          <div class="meta-grid">
            <p>Payment Status: {{ paymentText(booking.paymentStatus) }}</p>
            <p>Payment Method: {{ booking.paymentType || '-' }}</p>
            <p>Participants: {{ booking.participantCount || 1 }}</p>
            <p>Total Amount: {{ formatCurrency(booking.totalAmount) }}</p>
            <p>Paid Amount: {{ formatCurrency(booking.payAmount ?? booking.totalAmount) }}</p>
            <p>Paid At: {{ formatTime(booking.paymentTime) }}</p>
            <p>Checked In At: {{ formatTime(booking.verificationTime) }}</p>
            <p>Refund State: {{ refundStateText }}</p>
          </div>
          <p v-if="booking.remark" class="remark-line">Remark: {{ booking.remark }}</p>
        </article>

        <article class="info-card">
          <h2>Customer Info</h2>
          <div class="customer-card">
            <img :src="booking.customerAvatar || '/default-avatar.svg'" alt="Customer avatar" class="avatar">
            <div class="customer-copy">
              <strong>{{ booking.customerName || booking.participantName || `User ${booking.userId}` }}</strong>
              <span>Username: {{ booking.customerUsername || '-' }}</span>
              <span>Phone: {{ booking.customerPhone || booking.participantPhone || '-' }}</span>
              <span>Email: {{ booking.customerEmail || '-' }}</span>
              <span>Location: {{ booking.customerLocation || '-' }}</span>
            </div>
          </div>
        </article>

        <article v-if="timeline.length" class="info-card">
          <h2>Timeline</h2>
          <div class="timeline-list">
            <div v-for="item in timeline" :key="item.id" class="timeline-item">
              <strong>{{ formatTime(item.createTime) }}</strong>
              <span>{{ statusText(item.newStatus) }}</span>
              <small>{{ item.operatorName || item.operatorType || 'System' }}{{ item.remark ? ` | ${item.remark}` : '' }}</small>
            </div>
          </div>
        </article>

        <article v-if="hasReview" class="info-card">
          <h2>Customer Review</h2>
          <div class="review-box">
            <strong>{{ reviewTitle }}</strong>
            <p>{{ booking.reviewContent || 'No review content.' }}</p>
            <small>Reviewed At: {{ formatTime(booking.reviewTime) }}</small>
          </div>
        </article>
      </section>

      <aside class="side-column">
        <article class="info-card sticky-card">
          <h2>Actions</h2>
          <p class="side-tip">{{ actionHint }}</p>

          <div class="action-list">
            <button v-if="booking.status === 'registered'" class="refund-btn" :disabled="isSubmitting || !booking.canRefund" @click="handleRefund">
              {{ submittingAction === 'refund' ? 'Refunding...' : 'Refund Only' }}
            </button>
            <button v-if="booking.canReject" class="danger-btn" :disabled="isSubmitting" @click="handleReject">
              {{ submittingAction === 'reject' ? 'Rejecting...' : 'Reject Booking' }}
            </button>
          </div>
        </article>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMerchantBookingDetail, refundMerchantBooking, rejectBooking } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submittingAction = ref('')
const booking = ref(null)

const timeline = computed(() => Array.isArray(booking.value?.timeline) ? booking.value.timeline : [])
const hasReview = computed(() => booking.value?.reviewScore !== undefined || booking.value?.reviewContent || booking.value?.reviewTime)
const reviewTitle = computed(() => (
  booking.value?.reviewScore === undefined || booking.value?.reviewScore === null || booking.value?.reviewScore === ''
    ? 'Review Submitted'
    : `Score ${Number(booking.value.reviewScore).toFixed(1)} / 5`
))
const coverImage = computed(() => booking.value?.coverImage || booking.value?.activityImages?.[0] || booking.value?.detailImages?.[0] || '')
const fullLocation = computed(() => (
  [booking.value?.locationProvince, booking.value?.locationCity, booking.value?.locationDistrict, booking.value?.locationDetail]
    .filter(Boolean)
    .join(' / ') || '-'
))
const isSubmitting = computed(() => Boolean(submittingAction.value))
const refundStateText = computed(() => {
  if (booking.value?.paymentStatus === 'refunded' && booking.value?.status === 'registered') {
    return 'Refunded, user can pay again'
  }
  if (booking.value?.paymentStatus === 'refunded' && booking.value?.status === 'rejected') {
    return 'Refunded and blocked'
  }
  return 'Not refunded'
})
const actionHint = computed(() => {
  if (booking.value?.status === 'registered' && booking.value?.canRefund) {
    return 'Refund Only returns the money but keeps this booking active so the user can pay again later.'
  }
  if (booking.value?.status === 'registered' && !booking.value?.canRefund) {
    return 'Refund Only is visible here, but it only works after the booking has been paid.'
  }
  if (booking.value?.canReject) return 'Reject Booking will block this booking. Paid bookings will be refunded first.'
  if (booking.value?.status === 'rejected') return 'This booking has been rejected and can no longer participate.'
  if (booking.value?.status === 'checked_in') return 'This booking has already been checked in.'
  if (booking.value?.status === 'cancelled') return 'This booking has been cancelled.'
  return 'Review the booking details before taking action.'
})

const loadBooking = async () => {
  loading.value = true
  try {
    const response = await getMerchantBookingDetail(route.params.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || 'Failed to load booking detail')
    }
    booking.value = response.data
  } catch (error) {
    booking.value = null
    notify?.error?.(error.message || 'Failed to load booking detail')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }
  router.push('/merchant/bookings')
}

const openScanPage = () => {
  if (!booking.value) return
  router.push({
    name: 'merchant-bookings-scan',
    query: {
      code: booking.value.reserveNo || '',
      backTo: route.fullPath
    }
  })
}

const openActivity = () => {
  if (!booking.value?.activityId) return
  router.push(`/activity/${booking.value.activityId}`)
}

const handleCheckin = async () => {
  if (!booking.value?.id || isSubmitting.value) return
  confirm({
    title: '确认核销',
    message: `确定核销报名 ${booking.value.reserveNo || booking.value.id}？`,
    onConfirm: async () => {
      submittingAction.value = 'checkin'
      try {
        const response = await checkinBooking(booking.value.id)
        if (response.code !== 200) throw new Error(response.message || '核销失败')
        notify?.success?.('报名核销成功')
        await loadBooking()
      } catch (error) {
        notify?.error?.(error.message || '核销失败')
      } finally {
        submittingAction.value = ''
      }
    }
  })
}

const handleRefund = async () => {
  if (!booking.value?.id || isSubmitting.value) return
  if (!booking.value?.canRefund) {
    notify?.warning?.('只有已支付的订单才能退款')
    return
  }
  confirm({
    title: '确认退款',
    message: `确定退款报名 ${booking.value.reserveNo || booking.value.id}？用户之后可以重新支付。`,
    onConfirm: async () => {
      submittingAction.value = 'refund'
      try {
        const response = await refundMerchantBooking(booking.value.id, {
          reason: '商家退款并保持报名有效'
        })
        if (response.code !== 200) throw new Error(response.message || '退款失败')
        notify?.success?.('退款成功。用户之后可以重新支付。')
        await loadBooking()
      } catch (error) {
        notify?.error?.(error.message || '退款失败')
      } finally {
        submittingAction.value = ''
      }
    }
  })
}

const handleReject = async () => {
  if (!booking.value?.id || isSubmitting.value) return
  const message = booking.value?.canRefund
    ? `拒绝报名 ${booking.value.reserveNo || booking.value.id}？这将退款并阻止参与。`
    : `拒绝报名 ${booking.value.reserveNo || booking.value.id}？这将阻止参与。`
  confirm({
    title: '拒绝报名',
    message,
    onConfirm: async () => {
      submittingAction.value = 'reject'
      try {
        const response = await rejectBooking(booking.value.id, {
          reason: '商家拒绝了报名'
        })
        if (response.code !== 200) throw new Error(response.message || '拒绝失败')
        notify?.success?.('报名已拒绝')
        await loadBooking()
      } catch (error) {
        notify?.error?.(error.message || '拒绝失败')
      } finally {
        submittingAction.value = ''
      }
    }
  })
}

const formatTime = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '-')
const formatCurrency = (value) => `CNY ${(Number(value || 0) / 100).toFixed(2)}`
const formatRange = (start, end, fallback) => {
  if (start || end) {
    const startText = start ? formatTime(start) : 'TBD'
    const endText = end ? formatTime(end) : ''
    return endText ? `${startText} - ${endText}` : startText
  }
  return fallback || 'TBD'
}
const paymentText = (status) => ({ paid: 'Paid', unpaid: 'Unpaid', refunded: 'Refunded' }[status] || status || 'Unpaid')
const statusText = (status) => ({ registered: 'Active', checked_in: 'Checked In', rejected: 'Rejected', cancelled: 'Cancelled' }[status] || status || '-')

onMounted(loadBooking)
</script>

<style scoped>
.merchant-booking-detail-page { max-width: 1180px; margin: 0 auto; }
.header-nav, .breadcrumb, .title-row, .customer-card, .action-list { display: flex; }
.header-nav { align-items: center; gap: 14px; margin-bottom: 22px; flex-wrap: wrap; }
.back-btn { display: inline-flex; align-items: center; gap: 8px; min-height: 44px; padding: 0 16px; border: none; border-radius: 999px; background: #fff; box-shadow: 0 10px 24px rgba(15,23,42,0.08); cursor: pointer; }
.breadcrumb { align-items: center; gap: 8px; color: #64748b; }
.breadcrumb span:first-child { cursor: pointer; }
.state-card, .info-card { border-radius: 22px; border: 1px solid #e5e7eb; background: rgba(255,255,255,0.96); box-shadow: 0 16px 36px rgba(15,23,42,0.06); }
.state-card { padding: 76px 24px; text-align: center; color: #64748b; }
.detail-layout { display: grid; grid-template-columns: minmax(0,1fr) 320px; gap: 20px; }
.main-column, .side-column { display: flex; flex-direction: column; gap: 18px; }
.info-card { padding: 20px; }
.hero-card { display: grid; grid-template-columns: 220px minmax(0,1fr); gap: 20px; }
.cover-image { width: 100%; height: 220px; object-fit: cover; border-radius: 18px; }
.hero-copy, .customer-copy { display: flex; flex-direction: column; }
.title-row { justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; font-size: 12px; font-weight: 700; letter-spacing: 0.08em; color: #1661ab; text-transform: uppercase; }
.hero-copy h1, .info-card h2 { margin: 0; color: #0f172a; }
.subtitle, .activity-content, .side-tip, .remark-line { color: #475569; line-height: 1.7; }
.subtitle { margin: 8px 0 0; }
.activity-content { margin: 14px 0 0; }
.status-tag { display: inline-flex; align-items: center; justify-content: center; min-width: 88px; min-height: 34px; padding: 0 14px; border-radius: 999px; background: #f1f5f9; color: #334155; font-weight: 700; }
.status-tag.registered { background: #dbeafe; color: #1d4ed8; }
.status-tag.checked_in { background: #dcfce7; color: #15803d; }
.status-tag.rejected { background: #fee2e2; color: #b42318; }
.status-tag.cancelled { background: #f3f4f6; color: #6b7280; }
.meta-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 12px 18px; margin-top: 14px; }
.meta-grid p { margin: 0; color: #334155; }
.customer-card { gap: 16px; align-items: center; }
.avatar { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; background: #f8fafc; }
.customer-copy { gap: 6px; color: #475569; }
.timeline-list { display: grid; gap: 12px; margin-top: 14px; }
.timeline-item { padding: 14px 16px; border-radius: 16px; background: #f8fafc; display: flex; flex-direction: column; gap: 10px; }
.timeline-item strong { color: #0f172a; }
.timeline-item span, .timeline-item small { color: #475569; }
.review-box { margin-top: 14px; padding: 16px; border-radius: 16px; background: #fff7ed; }
.review-box strong { color: #9a3412; }
.review-box p, .review-box small { color: #7c2d12; }
.sticky-card { position: sticky; top: 16px; }
.action-list { flex-direction: column; gap: 12px; margin-top: 16px; }
.ghost-btn, .primary-btn, .danger-btn, .refund-btn { min-height: 44px; border: none; border-radius: 12px; font-weight: 700; cursor: pointer; }
.ghost-btn { background: #f4f6fb; color: #334155; }
.primary-btn { background: #1661ab; color: #fff; }
.refund-btn { background: #fff7ed; color: #c2410c; }
.danger-btn { background: #fdecec; color: #b42318; }
button:disabled { opacity: 0.7; cursor: not-allowed; }
@media (max-width: 960px) { .detail-layout, .hero-card { grid-template-columns: 1fr; } .sticky-card { position: static; } }
@media (max-width: 640px) { .meta-grid { grid-template-columns: 1fr; } .title-row { flex-direction: column; } .back-btn { width: 100%; justify-content: center; } }
</style>