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
              <span>参与人数</span>
              <div class="quantity-box">
                <button 
                  type="button" 
                  @click="changeCount(-1)"
                  :disabled="participantCount <= 1"
                  class="quantity-btn minus"
                >
                  <i class="bx bx-minus"></i>
                </button>
                <input 
                  :value="participantCount" 
                  type="number" 
                  min="1" 
                  :max="remainingSlots"
                  class="quantity-input"
                  readonly
                >
                <button 
                  type="button" 
                  @click="changeCount(1)"
                  :disabled="participantCount >= remainingSlots"
                  class="quantity-btn plus"
                >
                  <i class="bx bx-plus"></i>
                </button>
              </div>
              <small>剩余名额: {{ remainingSlots }}</small>
            </label>

            <div class="field full">
              <span>参与者信息</span>
              <div class="participants-list">
                <div 
                  v-for="(participant, index) in form.participants" 
                  :key="index"
                  class="participant-item"
                >
                  <div class="participant-header">
                    <h4>参与者 {{ index + 1 }}</h4>
                  </div>
                  <div class="participant-fields">
                    <label class="field">
                      <span>姓名</span>
                      <input 
                        v-model.trim="participant.name" 
                        type="text" 
                        placeholder="请输入姓名"
                      >
                    </label>
                    <label class="field">
                      <span>电话</span>
                      <input 
                        v-model.trim="participant.phone" 
                        type="tel" 
                        placeholder="请输入手机号码"
                      >
                    </label>
                  </div>
                </div>
              </div>
            </div>

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
  participants: [
    {
      name: '',
      phone: ''
    }
  ],
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

const participantCount = computed(() => form.value.participants.length)

const canSubmit = computed(() => {
  const phoneRegex = /^1[3-9]\d{9}$/
  
  if (!activity.value || participantCount.value < 1) {
    return false
  }
  
  for (const participant of form.value.participants) {
    if (!participant.name || !phoneRegex.test(participant.phone)) {
      return false
    }
  }
  
  return participantCount.value <= Math.max(remainingSlots.value, 1)
})

const priceValue = computed(() => Number(activity.value?.price || 0) / 100)
const priceText = computed(() => (priceValue.value > 0 ? `¥${priceValue.value.toFixed(2)}` : '免费'))
const totalPriceText = computed(() => {
  const total = priceValue.value * participantCount.value
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
      throw new Error(response.message || '加载活动失败')
    }

    const detail = response.data || null
    const current = Number(detail?.currentParticipants || 0)
    const max = Number(detail?.maxParticipants || 0)
    if (detail?.status === 'ended' || detail?.status === 'full' || (detail?.maxParticipants && current >= max)) {
      throw new Error('该活动当前无法报名')
    }

    activity.value = detail
    
    // 调整参与者数量，确保不超过剩余名额
    const maxAllowed = Math.max(remainingSlots.value, 1)
    if (participantCount.value > maxAllowed) {
      form.value.participants = form.value.participants.slice(0, maxAllowed)
    }
  } catch (error) {
    activity.value = null
    showError(error.message || '加载活动失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push(`/activity/${route.params.id}`)
}

const changeCount = (delta) => {
  const currentCount = participantCount.value
  const nextCount = currentCount + delta
  const maxCount = Math.max(remainingSlots.value, 1)
  
  if (nextCount < 1 || nextCount > maxCount) {
    return
  }
  
  if (delta > 0) {
    // 增加人数，添加新的参与者对象
    form.value.participants.push({
      name: '',
      phone: ''
    })
  } else {
    // 减少人数，移除最后一个参与者
    form.value.participants.pop()
  }
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
    showError('请先完整填写报名信息')
    return
  }

  submitting.value = true
  try {
    const response = await bookActivity(activity.value.id, {
      participantName: form.value.participants[0].name,
      participantPhone: form.value.participants[0].phone,
      participantCount: participantCount.value,
      remark: form.value.remark || undefined,
      participants: form.value.participants
    })
    if (response.code !== 200 || !response.data?.id) {
      throw new Error(response.message || '报名失败')
    }

    if (response.data.paymentStatus === 'paid' || Number(response.data.payAmount || 0) <= 0) {
      showSuccess('报名成功')
      openBookingDetail(response.data.id)
      return
    }

    showSuccess('报名成功，即将进入支付')
    openPayment(response.data.id)
  } catch (error) {
    showError(error.message || '报名失败')
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
    showWarning('商家和管理员账户不能创建活动报名')
    router.replace(`/activity/${route.params.id}`)
    return
  }
  // 设置第一个参与者的信息
  if (form.value.participants[0]) {
    form.value.participants[0].name = user.nickname || user.username || ''
    form.value.participants[0].phone = user.phone || ''
  }
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
  border: none;
  background: #fff;
  font-size: 14px;
  outline: none;
  box-shadow: 0 2px 8px rgba(74, 46, 23, 0.06);
  transition: all 0.3s ease;
}

.field input:focus,
.field textarea:focus {
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.15);
  transform: translateY(-1px);
}

.field textarea {
  min-height: 132px;
  resize: vertical;
}

.field small {
  color: #8b8074;
}

.quantity-box {
  display: inline-flex;
  align-items: center;
  max-width: 200px;
  border: 2px solid #d9cfc1;
  border-radius: 20px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 4px 12px rgba(74, 46, 23, 0.08);
  transition: all 0.3s ease;
}

.quantity-box:hover {
  border-color: #9d2929;
  box-shadow: 0 6px 16px rgba(157, 41, 41, 0.12);
}

.quantity-btn {
  flex: 0 0 50px;
  height: 52px;
  border: none;
  background: #f6efe5;
  color: #9d2929;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quantity-btn:hover:not(:disabled) {
  background: #e8ded2;
  transform: translateY(-1px);
}

.quantity-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: #f9f5f0;
  color: #cbbfb1;
}

.quantity-input {
  flex: 1;
  min-width: 60px;
  height: 50px;
  border: none;
  border-left: 1px solid #d9cfc1;
  border-right: 1px solid #d9cfc1;
  background: #fff;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #2c2c2c;
  outline: none;
  transition: all 0.2s ease;
}

.quantity-input:focus {
  background: #f9f5f0;
}

.quantity-input::-webkit-inner-spin-button,
.quantity-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.quantity-input {
  -moz-appearance: textfield;
}

.participants-list {
  margin-top: 16px;
}

.participant-item {
  margin-bottom: 20px;
  padding: 20px;
  border-radius: 16px;
  background: #f9f5f0;
  border: 1px solid rgba(217, 207, 193, 0.7);
  transition: all 0.3s ease;
}

.participant-item:hover {
  border-color: #9d2929;
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.08);
}

.participant-header {
  margin-bottom: 16px;
}

.participant-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #9d2929;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.participant-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 768px) {
  .participant-fields {
    grid-template-columns: 1fr;
  }
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
