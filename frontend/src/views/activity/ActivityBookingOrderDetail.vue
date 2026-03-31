<template>
  <div class="detail-page">
    <div class="header-nav">
      <button class="back-btn" @click="goBack">
        <i class="bx bx-arrow-back"></i>
        <span>Back</span>
      </button>
      <div class="breadcrumb">
        <span @click="goBack">My Activities</span>
        <i class="bx bx-chevron-right"></i>
        <span>Order Detail</span>
      </div>
    </div>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>Loading booking detail...</p>
    </div>

    <div v-else-if="!booking" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>Booking detail is unavailable.</p>
    </div>

    <div v-else class="detail-layout">
      <section class="main-column">
        <article class="info-card activity-card">
          <img
            v-if="coverImage"
            :src="coverImage"
            :alt="booking.activityTitle || 'Activity cover'"
            class="cover-image"
          >
          <div class="activity-copy">
            <div class="title-row">
              <div>
                <h1>{{ booking.activityTitle || 'Activity Booking' }}</h1>
                <p>{{ booking.activitySubtitle || booking.heritageType || 'Cultural activity' }}</p>
              </div>
              <span :class="['status-tag', displayStatus.code]">{{ displayStatus.label }}</span>
            </div>
            <div class="meta-grid">
              <p>Activity Time: {{ formatRange(booking.startTime, booking.endTime) }}</p>
              <p>Location: {{ fullLocation }}</p>
              <p>Merchant: {{ booking.shopName || booking.merchantName || '-' }}</p>
              <p>Created: {{ formatTime(booking.createTime) }}</p>
            </div>
            <p v-if="booking.activityContent" class="activity-content">{{ booking.activityContent }}</p>
          </div>
        </article>

        <article class="info-card">
          <h2>Order Information</h2>
          <div class="meta-grid">
            <p>Booking No: {{ booking.reserveNo || '-' }}</p>
            <p>Payment Status: {{ paymentStatusText }}</p>
            <p>Participant: {{ booking.participantName || '-' }}</p>
            <p>Phone: {{ booking.participantPhone || '-' }}</p>
            <p>Participants: {{ booking.participantCount || 1 }}</p>
            <p>Payment Type: {{ booking.paymentType || '-' }}</p>
            <p>Total Amount: {{ formatMoney(booking.totalAmount) }}</p>
            <p>Paid Amount: {{ formatMoney(booking.payAmount) }}</p>
            <p v-if="booking.paymentTime">Paid At: {{ formatTime(booking.paymentTime) }}</p>
            <p v-if="booking.verificationTime">Checked In At: {{ formatTime(booking.verificationTime) }}</p>
          </div>
          <p v-if="booking.remark" class="remark-line">Remark: {{ booking.remark }}</p>
        </article>

        <article v-if="timeline.length" class="info-card">
          <h2>Status Timeline</h2>
          <div class="timeline-list">
            <div v-for="item in timeline" :key="item.id" class="timeline-item">
              <strong>{{ formatTime(item.createTime) }}</strong>
              <span>{{ timelineStatus(item.newStatus) }}</span>
              <small>{{ item.operatorName || item.operatorType || 'System' }}{{ item.remark ? ` · ${item.remark}` : '' }}</small>
            </div>
          </div>
        </article>

        <article class="info-card">
          <h2>{{ hasReviewed ? 'Your Review' : 'Activity Review' }}</h2>
          <div v-if="hasReviewed" class="review-box">
            <strong>{{ reviewScoreText }}</strong>
            <p>{{ booking.reviewContent || 'No written review provided.' }}</p>
            <small>Reviewed At: {{ formatTime(booking.reviewTime) }}</small>
          </div>
          <div v-else-if="booking.canReview" class="review-form">
            <div class="star-row">
              <button
                v-for="star in 5"
                :key="star"
                type="button"
                class="star-btn"
                :class="{ active: reviewForm.score >= star }"
                @click="reviewForm.score = star"
              >
                <i class="bx bxs-star"></i>
              </button>
            </div>
            <textarea
              ref="reviewTextarea"
              v-model.trim="reviewForm.content"
              rows="4"
              maxlength="500"
              placeholder="Share your experience with this activity"
            ></textarea>
            <button class="submit-btn" :disabled="submittingReview" @click="submitReview">
              {{ submittingReview ? 'Submitting...' : 'Submit Review' }}
            </button>
          </div>
          <p v-else class="muted-text">
            Reviews become available after a successful check-in or after the activity is completed.
          </p>
        </article>
      </section>

      <aside class="side-column">
        <article class="info-card sticky-card">
          <h2>Next Step</h2>
          <p class="side-tip">{{ actionHint }}</p>

          <div class="action-list">
            <button v-if="booking.canPay" class="pay-btn" @click="payNow">
              Continue Payment
            </button>
            <button v-if="booking.canOpenQr" class="checkin-btn" @click="openCheckin">
              Open Check-in QR
            </button>
            <button class="link-btn" @click="openActivity">Open Activity Page</button>
          </div>
        </article>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getActivityBookingDetail, submitActivityBookingReview } from '../../api/app'
import { hasReviewedActivityBooking, resolveActivityBookingDisplayStatus } from '../../utils/activityBooking'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submittingReview = ref(false)
const booking = ref(null)
const reviewTextarea = ref(null)
const reviewForm = ref({
  score: 5,
  content: ''
})

const displayStatus = computed(() => resolveActivityBookingDisplayStatus(booking.value))
const hasReviewed = computed(() => hasReviewedActivityBooking(booking.value))
const coverImage = computed(() => (
  booking.value?.activityCoverImage
  || booking.value?.detailImages?.[0]
  || booking.value?.activityImages?.[0]
  || ''
))
const fullLocation = computed(() => (
  [booking.value?.locationProvince, booking.value?.locationCity, booking.value?.locationDistrict, booking.value?.locationDetail]
    .filter(Boolean)
    .join(' / ') || '-'
))
const timeline = computed(() => Array.isArray(booking.value?.timeline) ? booking.value.timeline : [])
const paymentStatusText = computed(() => ({
  paid: 'Paid',
  unpaid: 'Unpaid',
  refunded: 'Refunded'
}[booking.value?.paymentStatus] || booking.value?.paymentStatus || 'Unpaid'))
const reviewScoreText = computed(() => (
  booking.value?.reviewScore === null || booking.value?.reviewScore === undefined || booking.value?.reviewScore === ''
    ? 'Reviewed'
    : `${Number(booking.value.reviewScore).toFixed(1)} / 5`
))
const actionHint = computed(() => {
  if (booking.value?.canPay) {
    return 'Payment is still pending. Complete payment first to unlock your check-in QR code.'
  }
  if (booking.value?.canOpenQr) {
    return 'This booking is ready for on-site check-in. Open the QR code and show it to the merchant.'
  }
  if (booking.value?.canReview) {
    return 'The activity is finished for this booking. You can leave a review now.'
  }
  return 'Use this page to review payment, check-in, and review details for the booking.'
})

const showError = (message) => notify?.error?.(message) ?? window.alert(message)
const showSuccess = (message) => notify?.success?.(message) ?? window.alert(message)

const loadBooking = async () => {
  loading.value = true
  try {
    const response = await getActivityBookingDetail(route.params.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || 'Failed to load booking detail')
    }
    booking.value = response.data
    if (!hasReviewed.value && route.query.focus === 'review') {
      await nextTick()
      reviewTextarea.value?.focus()
    }
  } catch (error) {
    booking.value = null
    showError(error.message || 'Failed to load booking detail')
  } finally {
    loading.value = false
  }
}

const payNow = () => {
  if (!booking.value?.canPay) {
    return
  }
  router.push({
    name: 'activity-booking-payment',
    params: { id: booking.value.id },
    query: { backTo: route.fullPath }
  })
}

const submitReview = async () => {
  if (!booking.value?.canReview || submittingReview.value) {
    return
  }

  submittingReview.value = true
  try {
    const response = await submitActivityBookingReview(booking.value.id, {
      score: reviewForm.value.score,
      content: reviewForm.value.content || ''
    })
    if (response.code !== 200) {
      throw new Error(response.message || 'Failed to submit review')
    }
    showSuccess('Review submitted')
    await loadBooking()
  } catch (error) {
    showError(error.message || 'Failed to submit review')
  } finally {
    submittingReview.value = false
  }
}

const openCheckin = () => {
  if (!booking.value?.id) {
    return
  }
  router.push({
    name: 'activity-booking-checkin',
    params: { id: booking.value.id },
    query: { backTo: route.fullPath }
  })
}

const goBack = () => {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }
  router.push('/personal/activities')
}

const openActivity = () => {
  if (!booking.value?.activityId) {
    return
  }
  router.push(`/activity/${booking.value.activityId}`)
}

const timelineStatus = (status) => ({
  registered: 'Registered',
  checked_in: 'Checked In',
  rejected: 'Rejected',
  cancelled: 'Cancelled'
}[status] || status)

const formatMoney = (value) => `$${(Number(value || 0) / 100).toFixed(2)}`
const formatTime = (value) => (value ? new Date(value).toLocaleString() : '-')
const formatRange = (start, end) => {
  const startText = start ? formatTime(start) : 'TBD'
  const endText = end ? formatTime(end) : ''
  return endText ? `${startText} - ${endText}` : startText
}

onMounted(loadBooking)
</script>

<style scoped>
.detail-page { max-width: 1180px; margin: 0 auto; padding: 20px; }
.header-nav { display: flex; align-items: center; gap: 16px; margin-bottom: 22px; flex-wrap: wrap; }
.back-btn { display: inline-flex; align-items: center; gap: 6px; padding: 10px 16px; border-radius: 24px; border: 1px solid #d9cfc1; background: rgba(255,255,255,0.9); cursor: pointer; }
.breadcrumb { display: flex; align-items: center; gap: 8px; color: #8b8074; }
.breadcrumb span:first-child { cursor: pointer; }
.state-card, .info-card { background: rgba(255,255,255,0.94); border: 1px solid #d9cfc1; border-radius: 18px; box-shadow: 0 12px 32px rgba(44,44,44,0.08); }
.state-card { padding: 80px 20px; text-align: center; color: #8b8074; }
.state-card i { font-size: 42px; }
.detail-layout { display: grid; grid-template-columns: minmax(0,1.6fr) 320px; gap: 20px; }
.main-column { display: flex; flex-direction: column; gap: 18px; }
.info-card { padding: 22px; }
.activity-card { display: grid; grid-template-columns: 240px minmax(0,1fr); gap: 18px; }
.cover-image { width: 100%; height: 200px; object-fit: cover; border-radius: 16px; }
.title-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.title-row h1 { margin: 0 0 8px; font-size: 28px; color: #2c2c2c; }
.activity-copy p, .meta-grid p, .remark-line, .muted-text, .side-tip { margin: 0; color: #5a5045; line-height: 1.7; }
.status-tag { display: inline-flex; align-items: center; padding: 6px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.status-tag.pending_start, .status-tag.pending_checkin { background: #dbeafe; color: #1d4ed8; }
.status-tag.checked_in, .status-tag.completed { background: #dcfce7; color: #15803d; }
.status-tag.cancelled, .status-tag.missed { background: #e5e7eb; color: #6b7280; }
.status-tag.rejected { background: #fee2e2; color: #b91c1c; }
.meta-grid { margin-top: 12px; display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 8px 18px; }
.activity-content { margin-top: 14px; white-space: pre-line; }
.timeline-list { display: flex; flex-direction: column; gap: 12px; margin-top: 14px; }
.timeline-item { padding: 12px 14px; border-radius: 14px; background: #f8fafc; display: flex; flex-direction: column; gap: 4px; }
.timeline-item strong { color: #9d2929; }
.timeline-item span, .timeline-item small { color: #5a5045; }
.review-box { display: flex; flex-direction: column; gap: 8px; padding: 14px 16px; border-radius: 14px; background: #fff7ed; color: #92400e; }
.review-form { display: flex; flex-direction: column; gap: 14px; margin-top: 14px; }
.star-row { display: flex; gap: 10px; }
.star-btn { width: 40px; height: 40px; border: none; border-radius: 50%; background: #f1f5f9; color: #94a3b8; cursor: pointer; }
.star-btn.active { background: #fef3c7; color: #d97706; }
.review-form textarea { width: 100%; border: 1px solid #d9cfc1; border-radius: 14px; padding: 14px; resize: vertical; outline: none; }
.side-column { min-width: 0; }
.sticky-card { position: sticky; top: 24px; }
.action-list { display: flex; flex-direction: column; gap: 12px; margin-top: 18px; }
.pay-btn, .checkin-btn, .link-btn, .submit-btn { border: none; border-radius: 14px; padding: 12px 16px; font-weight: 600; cursor: pointer; }
.pay-btn, .submit-btn { background: linear-gradient(135deg, #9d2929, #b33030); color: #fff; }
.checkin-btn { background: linear-gradient(135deg, #0f766e, #14b8a6); color: #fff; }
.link-btn { background: #e2e8f0; color: #1e293b; }
button:disabled { opacity: 0.7; cursor: not-allowed; }
@media (max-width: 980px) { .detail-layout { grid-template-columns: 1fr; } .sticky-card { position: static; } }
@media (max-width: 760px) { .detail-page { padding: 12px; } .activity-card, .meta-grid { grid-template-columns: 1fr; } .header-nav { flex-direction: column; align-items: flex-start; } }
</style>
