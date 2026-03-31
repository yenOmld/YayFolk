<template>
  <div class="merchant-booking-detail-page">
    <div class="header-nav">
      <button class="back-btn" @click="goBack">
        <i class="bx bx-arrow-back"></i>
        <span>返回报名列表</span>
      </button>
      <div class="breadcrumb">
        <span @click="goBack">商家报名</span>
        <i class="bx bx-chevron-right"></i>
        <span>订单详情</span>
      </div>
    </div>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>正在加载报名详情...</p>
    </div>

    <div v-else-if="!booking" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>该报名不可用。</p>
    </div>

    <div v-else class="detail-layout">
      <section class="main-column">
        <article class="info-card hero-card">
          <img v-if="coverImage" :src="coverImage" :alt="booking.activityTitle || '活动封面'" class="cover-image">
          <div class="hero-copy">
            <div class="title-row">
              <div>
                <p class="eyebrow">商家报名详情</p>
                <h1>{{ booking.activityTitle || '活动报名' }}</h1>
                <p class="subtitle">{{ booking.activitySubtitle || booking.heritageType || '报名详情' }}</p>
              </div>
              <span :class="['status-tag', booking.status]">{{ statusText(booking.status) }}</span>
            </div>

            <div class="meta-grid">
              <p>报名编号：{{ booking.reserveNo || '-' }}</p>
              <p>活动时间：{{ formatRange(booking.startTime, booking.endTime, booking.activityTime) }}</p>
              <p>地点：{{ fullLocation }}</p>
              <p>创建时间：{{ formatTime(booking.createTime) }}</p>
            </div>

            <p v-if="booking.activityContent" class="activity-content">{{ booking.activityContent }}</p>
          </div>
        </article>

        <article class="info-card">
          <h2>订单信息</h2>
          <div class="meta-grid">
            <p>支付状态：{{ paymentText(booking.paymentStatus) }}</p>
            <p>支付方式：{{ booking.paymentType || '-' }}</p>
            <p>参加人数：{{ booking.participantCount || 1 }}</p>
            <p>总金额：{{ formatCurrency(booking.totalAmount) }}</p>
            <p>已付金额：{{ formatCurrency(booking.payAmount ?? booking.totalAmount) }}</p>
            <p>支付时间：{{ formatTime(booking.paymentTime) }}</p>
            <p>核销时间：{{ formatTime(booking.verificationTime) }}</p>
          </div>
          <p v-if="booking.remark" class="remark-line">备注：{{ booking.remark }}</p>
        </article>

        <article class="info-card">
          <h2>客户信息</h2>
          <div class="customer-card">
            <img :src="booking.customerAvatar || '/default-avatar.svg'" alt="Customer avatar" class="avatar">
            <div class="customer-copy">
              <strong>{{ booking.customerName || booking.participantName || `用户 ${booking.userId}` }}</strong>
              <span>用户名：{{ booking.customerUsername || '-' }}</span>
              <span>电话：{{ booking.customerPhone || booking.participantPhone || '-' }}</span>
              <span>邮箱：{{ booking.customerEmail || '-' }}</span>
              <span>地址：{{ booking.customerLocation || '-' }}</span>
            </div>
          </div>
        </article>

        <article v-if="timeline.length" class="info-card">
          <h2>状态记录</h2>
          <div class="timeline-list">
            <div v-for="item in timeline" :key="item.id" class="timeline-item">
              <strong>{{ formatTime(item.createTime) }}</strong>
              <span>{{ statusText(item.newStatus) }}</span>
              <small>{{ item.operatorName || item.operatorType || '系统' }}{{ item.remark ? ` · ${item.remark}` : '' }}</small>
            </div>
          </div>
        </article>

        <article v-if="hasReview" class="info-card">
          <h2>客户评价</h2>
          <div class="review-box">
            <strong>{{ reviewTitle }}</strong>
            <p>{{ booking.reviewContent || '暂无评价内容。' }}</p>
            <small>评价时间：{{ formatTime(booking.reviewTime) }}</small>
          </div>
        </article>
      </section>

      <aside class="side-column">
        <article class="info-card sticky-card">
          <h2>操作</h2>
          <p class="side-tip">{{ actionHint }}</p>

          <div class="action-list">
            <button class="ghost-btn" @click="openScanPage">打开扫码</button>
            <button class="ghost-btn" @click="openActivity">查看活动页面</button>
            <button v-if="booking.canReject" class="danger-btn" :disabled="submitting" @click="handleReject">
              {{ submitting ? '处理中...' : '拒绝报名' }}
            </button>
            <button v-if="booking.canCheckin" class="primary-btn" :disabled="submitting" @click="handleCheckin">
              {{ submitting ? '处理中...' : '确认核销' }}
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
import { checkinBooking, getMerchantBookingDetail, rejectBooking } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const booking = ref(null)

const timeline = computed(() => Array.isArray(booking.value?.timeline) ? booking.value.timeline : [])
const hasReview = computed(() => booking.value?.reviewScore !== undefined || booking.value?.reviewContent || booking.value?.reviewTime)
const reviewTitle = computed(() => (
  booking.value?.reviewScore === undefined || booking.value?.reviewScore === null || booking.value?.reviewScore === ''
    ? '已提交评价'
    : `评分 ${Number(booking.value.reviewScore).toFixed(1)} / 5`
))
const coverImage = computed(() => booking.value?.coverImage || booking.value?.activityImages?.[0] || booking.value?.detailImages?.[0] || '')
const fullLocation = computed(() => (
  [booking.value?.locationProvince, booking.value?.locationCity, booking.value?.locationDistrict, booking.value?.locationDetail]
    .filter(Boolean)
    .join(' / ') || '-'
))
const actionHint = computed(() => {
  if (booking.value?.canCheckin) return '该报名已支付，可以进行核销。'
  if (booking.value?.status === 'checked_in') return '该报名已核销成功。'
  if (booking.value?.status === 'rejected') return '该报名已被拒绝，已作为记录保留。'
  if (booking.value?.status === 'cancelled') return '该报名已取消，在此作为历史记录显示。'
  return '请在确认或拒绝前审核参与者和报名详情。'
})

const loadBooking = async () => {
  loading.value = true
  try {
    const response = await getMerchantBookingDetail(route.params.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '加载报名详情失败')
    }
    booking.value = response.data
  } catch (error) {
    booking.value = null
    notify?.error?.(error.message || '加载报名详情失败')
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
  if (!booking.value?.id || submitting.value) return
  if (!window.confirm(`确认核销报名 ${booking.value.reserveNo || booking.value.id}？`)) return
  submitting.value = true
  try {
    const response = await checkinBooking(booking.value.id)
    if (response.code !== 200) throw new Error(response.message || '核销失败')
    notify?.success?.('报名已核销')
    await loadBooking()
  } catch (error) {
    notify?.error?.(error.message || '核销失败')
  } finally {
    submitting.value = false
  }
}

const handleReject = async () => {
  if (!booking.value?.id || submitting.value) return
  if (!window.confirm(`拒绝报名 ${booking.value.reserveNo || booking.value.id}？`)) return
  submitting.value = true
  try {
    const response = await rejectBooking(booking.value.id)
    if (response.code !== 200) throw new Error(response.message || '拒绝失败')
    notify?.success?.('已拒绝该报名')
    await loadBooking()
  } catch (error) {
    notify?.error?.(error.message || '拒绝失败')
  } finally {
    submitting.value = false
  }
}

const formatTime = (value) => (value ? new Date(value).toLocaleString() : '-')
const formatCurrency = (value) => `¥${(Number(value || 0) / 100).toFixed(2)}`
const formatRange = (start, end, fallback) => {
  if (start || end) {
    const startText = start ? formatTime(start) : '待定'
    const endText = end ? formatTime(end) : ''
    return endText ? `${startText} - ${endText}` : startText
  }
  return fallback || '待定'
}
const paymentText = (status) => ({ paid: '已支付', unpaid: '未支付', refunded: '已退款' }[status] || status || '未支付')
const statusText = (status) => ({ registered: '待核销', checked_in: '已核销', rejected: '已拒绝', cancelled: '已取消' }[status] || status || '-')

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
.detail-layout { display: grid; grid-template-columns: minmax(0,1fr) 300px; gap: 20px; }
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
.status-tag.registered { background: #fdecec; color: #b42318; }
.status-tag.checked_in { background: #e7f3ff; color: #1661ab; }
.status-tag.rejected { background: #fff4e5; color: #b45309; }
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
.ghost-btn, .primary-btn, .danger-btn { min-height: 44px; border: none; border-radius: 12px; font-weight: 700; cursor: pointer; }
.ghost-btn { background: #f4f6fb; color: #334155; }
.primary-btn { background: #1661ab; color: #fff; }
.danger-btn { background: #fdecec; color: #b42318; }
@media (max-width: 960px) { .detail-layout, .hero-card { grid-template-columns: 1fr; } .sticky-card { position: static; } }
@media (max-width: 640px) { .meta-grid { grid-template-columns: 1fr; } .title-row { flex-direction: column; } .back-btn { width: 100%; justify-content: center; } }
</style>
