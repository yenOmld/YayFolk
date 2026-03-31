<template>
  <div class="checkin-page">
    <div class="header-nav">
      <button class="back-btn" @click="goBack">
        <i class="bx bx-arrow-back"></i>
        <span>Back</span>
      </button>
      <div class="breadcrumb">
        <span @click="goBack">Check-in Center</span>
        <i class="bx bx-chevron-right"></i>
        <span>QR Code</span>
      </div>
    </div>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>Loading check-in information...</p>
    </div>

    <div v-else-if="!booking" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>This booking is unavailable.</p>
    </div>

    <div v-else class="page-layout">
      <section class="qr-panel">
        <div class="qr-card">
          <p class="eyebrow">Check-in Code</p>
          <h1>{{ booking.activityTitle || 'Activity Booking' }}</h1>
          <p class="sub-copy">{{ booking.shopName || booking.merchantName || 'Merchant' }}</p>

          <div v-if="booking.canOpenQr && qrCodeUrl" class="qr-box">
            <img :src="qrCodeUrl" alt="Booking QR code">
            <strong>{{ booking.reserveNo }}</strong>
            <span>Show this QR code to the merchant on site for check-in.</span>
          </div>

          <div v-else-if="booking.canOpenQr && !qrError" class="qr-loading">
            <i class="bx bx-loader-alt bx-spin"></i>
            <span>Generating QR code...</span>
          </div>

          <div v-else class="qr-fallback">
            <i class="bx bx-qr"></i>
            <strong>{{ booking.reserveNo || '-' }}</strong>
            <p>{{ qrFallbackText }}</p>
          </div>

          <div class="action-row">
            <button v-if="booking.canPay" class="pay-btn" @click="payNow">
              Continue Payment
            </button>
            <button class="detail-btn" @click="openDetail">Order Detail</button>
          </div>
        </div>
      </section>

      <aside class="info-panel">
        <article class="info-card">
          <h2>Check-in Tips</h2>
          <ul class="tip-list">
            <li>Confirm the activity time, location, and participant count before arrival.</li>
            <li>After merchant verification, the booking status will update to checked in.</li>
            <li>If the QR code cannot load, you can still show the booking number to the merchant.</li>
          </ul>
        </article>

        <article class="info-card">
          <h2>Booking Summary</h2>
          <div class="meta-list">
            <p>Booking No: {{ booking.reserveNo || '-' }}</p>
            <p>Payment Status: {{ paymentStatusText }}</p>
            <p>Activity Time: {{ formatRange(booking.startTime, booking.endTime) }}</p>
            <p>Location: {{ fullLocation }}</p>
            <p>Participant: {{ booking.participantName || '-' }} / {{ booking.participantPhone || '-' }}</p>
            <p>Participants: {{ booking.participantCount || 1 }}</p>
            <p v-if="booking.verificationTime">Checked In At: {{ formatTime(booking.verificationTime) }}</p>
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
  paid: 'Paid',
  unpaid: 'Unpaid',
  refunded: 'Refunded'
}[booking.value?.paymentStatus] || booking.value?.paymentStatus || 'Unpaid'))

const qrFallbackText = computed(() => (
  booking.value?.canPay
    ? 'Payment is still pending. The QR code will be available after payment is completed.'
    : 'The QR code could not be loaded right now. You can still show the booking number to the merchant.'
))

const loadBooking = async () => {
  loading.value = true
  qrCodeUrl.value = ''
  qrError.value = false
  try {
    const response = await getActivityBookingDetail(route.params.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || 'Failed to load check-in information')
    }
    booking.value = response.data
    if (response.data.canOpenQr) {
      await loadQrCode()
    }
  } catch (error) {
    booking.value = null
    notify?.error?.(error.message || 'Failed to load check-in information')
  } finally {
    loading.value = false
  }
}

const loadQrCode = async () => {
  try {
    const response = await getActivityBookingQrCode(route.params.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || 'Failed to load QR code')
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
  const startText = start ? formatTime(start) : 'TBD'
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
