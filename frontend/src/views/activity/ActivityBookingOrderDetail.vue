<template>
  <div class="detail-page">
    <div class="header-nav">
      <button class="back-btn" @click="goBack">
        <i class="bx bx-arrow-back"></i>
        <span>返回</span>
      </button>
      <div class="breadcrumb">
        <span @click="goBack">我的活动</span>
        <i class="bx bx-chevron-right"></i>
        <span>订单详情</span>
      </div>
    </div>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>加载订单详情中...</p>
    </div>

    <div v-else-if="!booking" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>订单详情不可用。</p>
    </div>

    <div v-else class="detail-layout">
      <section class="main-column">
        <article class="info-card activity-card">
          <img
            v-if="coverImage"
            :src="coverImage"
            :alt="booking.activityTitle || '活动封面'"
            class="cover-image"
          >
          <div class="activity-copy">
            <div class="title-row">
              <div>
                <h1>{{ booking.activityTitle || '活动报名' }}</h1>
                <p>{{ booking.activitySubtitle || booking.heritageType || '文化活动' }}</p>
              </div>
              <span :class="['status-tag', displayStatus.code]">{{ displayStatus.label }}</span>
            </div>
            <div class="meta-grid">
              <p>活动时间: {{ formatRange(booking.startTime, booking.endTime) }}</p>
              <p>地点: {{ fullLocation }}</p>
              <p>商家: {{ booking.shopName || booking.merchantName || '-' }}</p>
              <p>创建时间: {{ formatTime(booking.createTime) }}</p>
            </div>
            <p v-if="booking.activityContent" class="activity-content">{{ booking.activityContent }}</p>
          </div>
        </article>

        <article class="info-card">
          <h2>订单信息</h2>
          <div class="meta-grid">
            <p>订单号: {{ booking.reserveNo || '-' }}</p>
            <p>支付状态: {{ paymentStatusText }}</p>
            <p>参与人数: {{ booking.participantCount || 1 }}</p>
            <p>支付方式: {{ booking.paymentType || '-' }}</p>
            <p>总金额: {{ formatMoney(booking.totalAmount) }}</p>
            <p>已支付金额: {{ formatMoney(booking.payAmount) }}</p>
            <p v-if="booking.paymentTime">支付时间: {{ formatTime(booking.paymentTime) }}</p>
            <p v-if="booking.verificationTime">核销时间: {{ formatTime(booking.verificationTime) }}</p>
          </div>
          <p v-if="booking.remark" class="remark-line">备注: {{ booking.remark }}</p>
        </article>

        <article class="info-card">
          <h2>参与者信息</h2>
          <div v-if="participantsList.length > 0" class="participants-list">
            <div v-for="(participant, index) in participantsList" :key="index" class="participant-item">
              <div class="participant-header">
                <h4>参与者 {{ index + 1 }}</h4>
              </div>
              <div class="participant-info">
                <p><strong>姓名:</strong> {{ participant.name || '-' }}</p>
                <p><strong>电话:</strong> {{ participant.phone || '-' }}</p>
              </div>
            </div>
          </div>
          <div v-else class="participants-list">
            <div class="participant-item">
              <div class="participant-header">
                <h4>参与者 1</h4>
              </div>
              <div class="participant-info">
                <p><strong>姓名:</strong> {{ booking.participantName || '-' }}</p>
                <p><strong>电话:</strong> {{ booking.participantPhone || '-' }}</p>
              </div>
            </div>
          </div>
        </article>

        <article v-if="timeline.length" class="info-card">
          <h2>状态时间线</h2>
          <div class="timeline-list">
            <div v-for="item in timeline" :key="item.id" class="timeline-item">
              <strong>{{ formatTime(item.createTime) }}</strong>
              <span>{{ timelineStatus(item.newStatus) }}</span>
              <small>{{ item.operatorName || item.operatorType || '系统' }}{{ item.remark ? ` · ${item.remark}` : '' }}</small>
            </div>
          </div>
        </article>

        <article class="info-card">
          <h2>{{ hasReviewed ? '我的评价' : '活动评价' }}</h2>
          <div v-if="hasReviewed" class="review-box">
            <strong>{{ reviewScoreText }}</strong>
            <p>{{ booking.reviewContent || '未提供文字评价。' }}</p>
            <small>评价时间: {{ formatTime(booking.reviewTime) }}</small>
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
              placeholder="分享您的活动体验"
            ></textarea>
            <button class="submit-btn" :disabled="submittingReview" @click="submitReview">
              {{ submittingReview ? '提交中...' : '提交评价' }}
            </button>
          </div>
          <p v-else class="muted-text">
            活动核销成功或结束后即可评价。
          </p>
        </article>
      </section>

      <aside class="side-column">
        <article class="info-card sticky-card">
          <h2>下一步操作</h2>
          <p class="side-tip">{{ actionHint }}</p>

          <div class="action-list">
            <button v-if="booking.canPay" class="pay-btn" @click="payNow">
              继续支付
            </button>
            <button v-if="booking.canOpenQr" class="checkin-btn" @click="openCheckin">
              打开核销二维码
            </button>
            <button class="link-btn" @click="openActivity">打开活动页面</button>
            <button class="link-btn" @click="goToActivityList">打开活动列表</button>
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
const participantsList = computed(() => {
  if (Array.isArray(booking.value?.participants) && booking.value.participants.length > 0) {
    return booking.value.participants
  }
  return []
})
const paymentStatusText = computed(() => ({
  paid: '已支付',
  unpaid: '未支付',
  refunded: '已退款'
}[booking.value?.paymentStatus] || booking.value?.paymentStatus || '未支付'))
const reviewScoreText = computed(() => (
  booking.value?.reviewScore === null || booking.value?.reviewScore === undefined || booking.value?.reviewScore === ''
    ? '已评价'
    : `${Number(booking.value.reviewScore).toFixed(1)} / 5`
))
const actionHint = computed(() => {
  if (booking.value?.canPay) {
    return '支付尚未完成。请先完成支付以获取核销二维码。'
  }
  if (booking.value?.canOpenQr) {
    return '该订单已准备好现场核销。打开二维码并向商家出示。'
  }
  if (booking.value?.canReview) {
    return '该活动已完成。您可以现在留下评价。'
  }
  return '在此页面查看支付、核销和评价详情。'
})

const showError = (message) => notify?.error?.(message)
const showSuccess = (message) => notify?.success?.(message)

const loadBooking = async () => {
  loading.value = true
  try {
    const response = await getActivityBookingDetail(route.params.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '加载订单详情失败')
    }
    booking.value = response.data
    if (!hasReviewed.value && route.query.focus === 'review') {
      await nextTick()
      reviewTextarea.value?.focus()
    }
  } catch (error) {
    booking.value = null
    showError(error.message || '加载订单详情失败')
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
      throw new Error(response.message || '提交评价失败')
    }
    showSuccess('评价已提交')
    await loadBooking()
  } catch (error) {
    showError(error.message || '提交评价失败')
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

const goToActivityList = () => {
  router.push('/home/activity')
}

const timelineStatus = (status) => ({
  registered: '已报名',
  checked_in: '已核销',
  rejected: '已拒绝',
  cancelled: '已取消'
}[status] || status)

const formatMoney = (value) => `¥${(Number(value || 0) / 100).toFixed(2)}`
const formatTime = (value) => (value ? new Date(value).toLocaleString() : '-')
const formatRange = (start, end) => {
  const startText = start ? formatTime(start) : '待定'
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
.participants-list { margin-top: 14px; display: flex; flex-direction: column; gap: 14px; }
.participant-item { padding: 16px; border-radius: 14px; background: #f8fafc; border: 1px solid #e2e8f0; }
.participant-header { margin-bottom: 12px; }
.participant-header h4 { margin: 0; font-size: 14px; font-weight: 700; color: #9d2929; text-transform: uppercase; letter-spacing: 0.04em; }
.participant-info { display: flex; flex-direction: column; gap: 8px; }
.participant-info p { margin: 0; color: #5a5045; }
.participant-info strong { color: #2c2c2c; font-weight: 600; }
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
