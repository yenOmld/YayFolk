<template>
  <div class="checkin-page">
    <div class="header-nav">
      <button class="back-btn" @click="goBack">
        <i class="bx bx-arrow-back"></i>
        <span>返回</span>
      </button>
      <div class="breadcrumb">
        <span @click="goBack">核销中心</span>
        <i class="bx bx-chevron-right"></i>
        <span>二维码</span>
      </div>
    </div>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>加载核销信息中...</p>
    </div>

    <div v-else-if="!booking" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>该订单不可用。</p>
    </div>

    <div v-else class="page-layout">
      <section class="qr-panel">
        <div class="qr-card">
          <p class="eyebrow">核销码</p>
          <h1>{{ booking.activityTitle || '活动报名' }}</h1>
          <p class="sub-copy">{{ booking.shopName || booking.merchantName || '商家' }}</p>

          <div v-if="booking.canOpenQr && qrCodeUrl" class="qr-box">
            <img :src="qrCodeUrl" alt="报名二维码">
            <strong>{{ booking.reserveNo }}</strong>
            <span>现场向商家出示此二维码进行核销。</span>
          </div>

          <div v-else-if="booking.canOpenQr && !qrError" class="qr-loading">
            <i class="bx bx-loader-alt bx-spin"></i>
            <span>生成二维码中...</span>
          </div>

          <div v-else class="qr-fallback">
            <i class="bx bx-qr"></i>
            <strong>{{ booking.reserveNo || '-' }}</strong>
            <p>{{ qrFallbackText }}</p>
          </div>

          <div class="action-row">
            <button v-if="booking.canPay" class="pay-btn" @click="payNow">
              继续支付
            </button>
            <button class="detail-btn" @click="openDetail">订单详情</button>
          </div>
        </div>
      </section>

      <aside class="info-panel">
        <article class="info-card">
          <h2>核销提示</h2>
          <ul class="tip-list">
            <li>到达前请确认活动时间、地点和参与人数。</li>
            <li>商家核销后，订单状态将更新为已核销。</li>
            <li>如果二维码无法加载，您仍可以向商家出示订单号。</li>
          </ul>
        </article>

        <article class="info-card">
          <h2>订单汇总</h2>
          <div class="meta-list">
            <p>订单号: {{ booking.reserveNo || '-' }}</p>
            <p>支付状态: {{ paymentStatusText }}</p>
            <p>活动时间: {{ formatRange(booking.startTime, booking.endTime) }}</p>
            <p>地点: {{ fullLocation }}</p>
            <p>参与者: {{ booking.participantName || '-' }} / {{ booking.participantPhone || '-' }}</p>
            <p>参与人数: {{ booking.participantCount || 1 }}</p>
            <p v-if="booking.verificationTime">核销时间: {{ formatTime(booking.verificationTime) }}</p>
          </div>
        </article>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getActivityBookingDetail, getActivityBookingQrCode } from '../../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const booking = ref(null)
const qrCodeUrl = ref('')
const qrError = ref(false)

const fullLocation = computed(() => (
  [booking.value?.locationProvince, booking.value?.locationCity, booking.value?.locationDistrict, booking.value?.locationDetail]
    .filter(Boolean)
    .join(' / ') || '-'
))

const paymentStatusText = computed(() => ({
  paid: '已支付',
  unpaid: '未支付',
  refunded: '已退款'
}[booking.value?.paymentStatus] || booking.value?.paymentStatus || '未支付'))

const qrFallbackText = computed(() => (
  booking.value?.canPay
    ? '支付尚未完成。支付完成后二维码将可用。'
    : '二维码暂时无法加载。您仍可以向商家出示订单号。'
))

const loadBooking = async () => {
  loading.value = true
  qrCodeUrl.value = ''
  qrError.value = false
  try {
    const response = await getActivityBookingDetail(route.params.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '加载核销信息失败')
    }
    booking.value = response.data
    if (response.data.canOpenQr) {
      await loadQrCode()
    }
  } catch (error) {
    booking.value = null
    notify?.error?.(error.message || '加载核销信息失败')
  } finally {
    loading.value = false
  }
}

const loadQrCode = async () => {
  try {
    const response = await getActivityBookingQrCode(route.params.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '加载二维码失败')
    }
    qrCodeUrl.value = typeof response.data === 'string' && !response.data.startsWith('data:image/')
      ? `data:image/png;base64,${response.data}`
      : response.data
  } catch (error) {
    qrError.value = true
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

const openDetail = () => {
  if (!booking.value?.id) {
    return
  }
  router.push({
    name: 'activity-booking-detail',
    params: { id: booking.value.id },
    query: {
      backTo: route.query.backTo || '/personal/checkins'
    }
  })
}

const goBack = () => {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }
  router.push('/personal/checkins')
}

const formatTime = (value) => (value ? new Date(value).toLocaleString() : '-')
const formatRange = (start, end) => {
  const startText = start ? formatTime(start) : '待定'
  const endText = end ? formatTime(end) : ''
  return endText ? `${startText} - ${endText}` : startText
}

onMounted(loadBooking)
</script>

<style scoped>
.checkin-page { max-width: 1120px; margin: 0 auto; padding: 20px; }
.header-nav { display: flex; align-items: center; gap: 16px; margin-bottom: 22px; flex-wrap: wrap; }
.back-btn { display: inline-flex; align-items: center; gap: 6px; padding: 10px 16px; border-radius: 24px; border: 1px solid #d9cfc1; background: rgba(255,255,255,0.9); cursor: pointer; }
.breadcrumb { display: flex; align-items: center; gap: 8px; color: #8b8074; }
.breadcrumb span:first-child { cursor: pointer; }
.state-card, .qr-card, .info-card { background: rgba(255,255,255,0.96); border: 1px solid #d9cfc1; border-radius: 20px; box-shadow: 0 12px 32px rgba(44,44,44,0.08); }
.state-card { padding: 80px 20px; text-align: center; color: #8b8074; }
.state-card i { font-size: 42px; }
.page-layout { display: grid; grid-template-columns: minmax(0,1.2fr) 320px; gap: 20px; }
.qr-card { padding: 28px; text-align: center; background: radial-gradient(circle at top center, rgba(20,184,166,0.12), transparent 34%), rgba(255,255,255,0.98); }
.eyebrow { margin: 0 0 8px; color: #0f766e; text-transform: uppercase; letter-spacing: 0.12em; font-size: 12px; font-weight: 700; }
.qr-card h1 { margin: 0 0 6px; color: #111827; font-size: 30px; }
.sub-copy { margin: 0; color: #64748b; }
.qr-box, .qr-loading, .qr-fallback { margin-top: 24px; padding: 22px; border-radius: 20px; background: #f8fafc; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.qr-box img { width: 280px; height: 280px; object-fit: contain; border-radius: 18px; background: #fff; padding: 12px; }
.qr-loading i, .qr-fallback i { font-size: 56px; color: #0f766e; }
.qr-box strong, .qr-fallback strong { color: #111827; font-size: 18px; }
.qr-box span, .qr-loading span, .qr-fallback p { margin: 0; color: #64748b; line-height: 1.7; }
.action-row { display: flex; gap: 12px; margin-top: 20px; }
.pay-btn, .detail-btn { flex: 1; border: none; border-radius: 14px; padding: 12px 16px; font-weight: 600; cursor: pointer; }
.pay-btn { background: linear-gradient(135deg, #9d2929, #b33030); color: #fff; }
.detail-btn { background: #e2e8f0; color: #1e293b; }
.info-panel { display: flex; flex-direction: column; gap: 18px; }
.info-card { padding: 20px; }
.info-card h2 { margin: 0 0 14px; color: #111827; }
.tip-list, .meta-list { margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 10px; }
.tip-list li, .meta-list p { color: #5a5045; line-height: 1.7; margin: 0; }
@media (max-width: 980px) { .page-layout { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .checkin-page { padding: 12px; } .action-row { flex-direction: column; } .qr-box img { width: 100%; height: auto; } }
</style>
