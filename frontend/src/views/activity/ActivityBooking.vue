<template>
  <div class="activity-booking-page">
    <div class="header-nav">
      <button class="back-button" @click="goBack">
        <i class="bx bx-arrow-back"></i>
        <span>返回</span>
      </button>
      <div class="breadcrumb">
        <span @click="router.push('/home/activity')">活动</span>
        <i class="bx bx-chevron-right"></i>
        <span @click="goBack">详情</span>
        <i class="bx bx-chevron-right"></i>
        <span>报名</span>
      </div>
    </div>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>加载活动中...</p>
    </div>

    <div v-else-if="!activity" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>该活动当前无法报名。</p>
    </div>

    <div v-else class="booking-layout">
      <section class="booking-main">
        <div class="section-card">
          <h1>报名信息</h1>
          <p class="section-desc">
            请先提交报名信息。如需支付，将进入支付页面。
          </p>

          <div class="activity-summary">
            <img :src="activity.coverImage || placeholderCover" :alt="activity.title || '活动封面'">
            <div class="summary-info">
              <h2>{{ activity.title }}</h2>
              <div class="summary-tags">
                <span>{{ activity.heritageType || '文化活动' }}</span>
                <span>{{ activityTypeLabel(activity.activityType) }}</span>
              </div>
              <p>{{ formatDateTime(activity.startTime) }}</p>
              <p>{{ formatLocation(activity) }}</p>
            </div>
          </div>
        </div>

        <div class="section-card">
          <div class="form-grid">
            <label class="field">
              <span>姓名</span>
              <input v-model.trim="form.participantName" type="text" placeholder="请输入您的姓名">
            </label>

            <label class="field">
              <span>电话</span>
              <input v-model.trim="form.participantPhone" type="tel" placeholder="请输入您的手机号码">
            </label>

            <label class="field">
              <span>参与人数</span>
              <div class="quantity-box">
                <button type="button" @click="changeCount(-1)">-</button>
                <input v-model.number="form.participantCount" type="number" min="1" :max="remainingSlots">
                <button type="button" @click="changeCount(1)">+</button>
              </div>
              <small>剩余名额: {{ remainingSlots }}</small>
            </label>

            <label class="field full">
              <span>备注</span>
              <textarea
                v-model.trim="form.remark"
                rows="4"
                maxlength="200"
                placeholder="给商家的留言（选填）"
              ></textarea>
            </label>
          </div>
        </div>
      </section>

      <aside class="booking-side">
        <div class="section-card sticky-card">
          <h3>报名汇总</h3>

          <div class="summary-line">
            <span>单价</span>
            <strong>{{ priceText }}</strong>
          </div>
          <div class="summary-line">
            <span>人数</span>
            <strong>{{ form.participantCount }}</strong>
          </div>
          <div class="summary-line">
            <span>活动时间</span>
            <strong>{{ shortDateTime(activity.startTime) }}</strong>
          </div>

          <div class="total-box">
            <span>总计</span>
            <strong>{{ totalPriceText }}</strong>
          </div>

          <button class="submit-btn" :disabled="submitting || !canSubmit" @click="submitBooking">
            {{ submitting ? '提交中...' : submitText }}
          </button>
          <p class="pay-tip">
            免费活动提交后立即完成。付费活动提交后将进入支付步骤。
          </p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { bookActivity, getPublicActivityDetail } from '../../api/app'

const route = useRoute()
const router = useRouter()
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const placeholderCover = 'https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=1200&q=80'

const loading = ref(false)
const submitting = ref(false)
const activity = ref(null)
const form = ref({
  participantName: '',
  participantPhone: '',
  participantCount: 1,
  remark: ''
})

const readStoredUser = () => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return {}
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    return {}
  }
}

const remainingSlots = computed(() => {
  if (!activity.value?.maxParticipants) {
    return 99
  }
  const current = Number(activity.value.currentParticipants || 0)
  const max = Number(activity.value.maxParticipants || 0)
  return Math.max(0, max - current)
})

const canSubmit = computed(() => {
  const phone = /^1[3-9]\d{9}$/
  return Boolean(
    activity.value
    && form.value.participantName
    && phone.test(form.value.participantPhone)
    && form.value.participantCount >= 1
    && form.value.participantCount <= Math.max(remainingSlots.value, 1)
  )
})

const priceValue = computed(() => Number(activity.value?.price || 0) / 100)
const priceText = computed(() => (priceValue.value > 0 ? `¥${priceValue.value.toFixed(2)}` : '免费'))
const totalPriceText = computed(() => {
  const total = priceValue.value * Number(form.value.participantCount || 0)
  return total > 0 ? `¥${total.toFixed(2)}` : '免费'
})
const submitText = computed(() => (priceValue.value > 0 ? '去支付' : '确认报名'))

const showError = (message) => notify?.error?.(message)
const showSuccess = (message) => notify?.success?.(message)
const showWarning = (message) => notify?.warning?.(message)

const loadActivity = async () => {
  loading.value = true
  try {
    const response = await getPublicActivityDetail(route.params.id)
    if (response.code !== 200) {
      throw new Error(response.message || 'Failed to load activity')
    }

    const detail = response.data || null
    const current = Number(detail?.currentParticipants || 0)
    const max = Number(detail?.maxParticipants || 0)
    if (detail?.status === 'ended' || detail?.status === 'full' || (detail?.maxParticipants && current >= max)) {
      throw new Error('This activity is currently unavailable')
    }

    activity.value = detail
    form.value.participantCount = Math.min(Math.max(form.value.participantCount, 1), Math.max(remainingSlots.value, 1))
  } catch (error) {
    activity.value = null
    showError(error.message || 'Failed to load activity')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push(`/activity/${route.params.id}`)
}

const changeCount = (delta) => {
  const next = Number(form.value.participantCount || 1) + delta
  form.value.participantCount = Math.min(Math.max(next, 1), Math.max(remainingSlots.value, 1))
}

const openBookingDetail = (bookingId) => {
  router.push({
    name: 'activity-booking-detail',
    params: { id: bookingId },
    query: {
      backTo: `/activity/${route.params.id}`
    }
  })
}

const openPayment = (bookingId) => {
  router.push({
    name: 'activity-booking-payment',
    params: { id: bookingId },
    query: {
      backTo: `/activity/${route.params.id}`
    }
  })
}

const submitBooking = async () => {
  if (!canSubmit.value || !activity.value) {
    showError('Please complete the booking form first')
    return
  }

  submitting.value = true
  try {
    const response = await bookActivity(activity.value.id, {
      participantName: form.value.participantName,
      participantPhone: form.value.participantPhone,
      participantCount: form.value.participantCount,
      remark: form.value.remark || undefined
    })
    if (response.code !== 200 || !response.data?.id) {
      throw new Error(response.message || 'Failed to create booking')
    }

    if (response.data.paymentStatus === 'paid' || Number(response.data.payAmount || 0) <= 0) {
      showSuccess('Booking created successfully')
      openBookingDetail(response.data.id)
      return
    }

    showSuccess('Booking created, continue payment next')
    openPayment(response.data.id)
  } catch (error) {
    showError(error.message || 'Failed to create booking')
  } finally {
    submitting.value = false
  }
}

const formatDateTime = (value) => (value ? new Date(value).toLocaleString() : '待定')
const shortDateTime = (value) => (value ? new Date(value).toLocaleString() : '待定')
const formatLocation = (item) => (
  [item.locationProvince, item.locationCity, item.locationDistrict, item.locationDetail]
    .filter(Boolean)
    .join(' / ') || '待定'
)

const activityTypeLabel = (type) => ({
  offline: '线下',
  online: '线上',
  exhibition: '展览'
}[type] || '活动')

onMounted(() => {
  const user = readStoredUser()
  const role = String(user.role || '').toLowerCase()
  if (role && role !== 'user') {
    showWarning('Merchant and admin accounts cannot create activity bookings')
    router.replace(`/activity/${route.params.id}`)
    return
  }
  form.value.participantName = user.nickname || user.username || ''
  form.value.participantPhone = user.phone || ''
  loadActivity()
})
</script>

<style scoped>
.activity-booking-page {
  position: relative;
  max-width: 1240px;
  margin: 0 auto;
  padding: 28px 20px 104px;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(157, 41, 41, 0.12), transparent 24%),
    radial-gradient(circle at top right, rgba(201, 145, 63, 0.12), transparent 20%),
    linear-gradient(180deg, rgba(255, 249, 241, 0.9), rgba(244, 235, 222, 0.94));
}

.header-nav {
  position: sticky;
  top: 20px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding: 16px 20px;
  border-radius: 22px;
  border: 1px solid rgba(190, 157, 124, 0.28);
  background:
    linear-gradient(135deg, rgba(157, 41, 41, 0.05), rgba(255, 255, 255, 0.86)),
    rgba(255, 251, 246, 0.86);
  box-shadow: 0 18px 40px rgba(74, 46, 23, 0.08);
  backdrop-filter: blur(18px);
  flex-wrap: wrap;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: 1px solid #d9cfc1;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8b8074;
  flex-wrap: wrap;
}

.breadcrumb span {
  cursor: default;
}

.breadcrumb span:first-child,
.breadcrumb span:nth-child(3) {
  cursor: pointer;
}

.state-card,
.section-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(190, 157, 124, 0.28);
  border-radius: 24px;
  box-shadow: 0 22px 44px rgba(74, 46, 23, 0.08);
}

.state-card {
  text-align: center;
  padding: 80px 20px;
  color: #8b8074;
}

.state-card i {
  font-size: 40px;
}

.booking-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) 340px;
  gap: 24px;
}

.booking-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  padding: 28px;
}

.section-card h1,
.section-card h3 {
  margin: 0 0 10px;
  color: #2c2c2c;
}

.section-desc,
.pay-tip {
  margin: 0;
  color: #8b8074;
  line-height: 1.7;
}

.activity-summary {
  margin-top: 20px;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(247, 240, 232, 0.96));
  border: 1px solid rgba(217, 207, 193, 0.74);
}

.activity-summary img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 18px;
}

.summary-info h2 {
  margin: 0 0 10px;
  font-size: 26px;
  color: #2c2c2c;
}

.summary-info p {
  margin: 8px 0 0;
  color: #5a5045;
}

.summary-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-tags span {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
  font-size: 13px;
  font-weight: 700;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field.full {
  grid-column: 1 / -1;
}

.field span {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #8a2b2b;
}

.field input,
.field textarea {
  width: 100%;
  min-height: 52px;
  padding: 14px 16px;
  border: 1px solid #d9cfc1;
  border-radius: 16px;
  background: #fff;
  font-size: 14px;
  outline: none;
}

.field textarea {
  min-height: 132px;
}

.field small {
  color: #8b8074;
}

.quantity-box {
  display: inline-grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: stretch;
  max-width: 196px;
  border: 1px solid #d9cfc1;
  border-radius: 16px;
  overflow: hidden;
}

.quantity-box button,
.quantity-box input {
  border: none;
  background: transparent;
  height: 46px;
  text-align: center;
}

.quantity-box button {
  cursor: pointer;
  background: #f6efe5;
  color: #9d2929;
  font-weight: 700;
}

.sticky-card {
  position: sticky;
  top: 106px;
}

.summary-line,
.total-box {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.summary-line {
  padding: 12px 0;
  color: #5a5045;
  border-bottom: 1px solid rgba(217, 207, 193, 0.7);
}

.total-box {
  padding: 20px 0;
  color: #2c2c2c;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid rgba(217, 207, 193, 0.7);
}

.submit-btn {
  width: 100%;
  margin-top: 20px;
  padding: 14px 18px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.submit-btn:disabled {
  background: #cbbfb1;
  cursor: not-allowed;
}

.pay-tip {
  margin-top: 12px;
  font-size: 13px;
}

@media (max-width: 960px) {
  .header-nav {
    position: static;
  }

  .booking-layout {
    grid-template-columns: 1fr;
  }

  .sticky-card {
    position: static;
  }
}

@media (max-width: 768px) {
  .activity-booking-page {
    padding: 16px 12px 96px;
  }

  .activity-summary,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
