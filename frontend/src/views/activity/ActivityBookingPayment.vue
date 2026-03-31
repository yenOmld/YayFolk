<template>
  <div class="payment-page">
    <div class="header-nav">
      <button class="back-btn" @click="goBack">
        <i class="bx bx-arrow-back"></i>
        <span>返回</span>
      </button>
      <div class="breadcrumb">
        <span @click="goBack">我的活动</span>
        <i class="bx bx-chevron-right"></i>
        <span>支付</span>
      </div>
    </div>

    <div v-if="loading" class="state-card">加载支付信息中...</div>
    <div v-else-if="!booking" class="state-card">该订单当前无法支付。</div>

    <div v-else class="payment-layout">
      <section class="main-column">
        <article class="panel-card hero-panel">
          <p class="eyebrow">活动结算</p>
          <h1>{{ success ? '支付完成' : '确认活动支付' }}</h1>
          <p v-if="success">
            支付已完成。您可以查看订单详情或打开核销二维码。
          </p>
          <p v-else>
            本项目目前使用模拟支付流程，以便完整测试报名、支付和核销全流程。
          </p>
        </article>

        <article class="panel-card order-panel">
          <div class="order-head">
            <div>
              <h2>{{ booking.activityTitle || '活动报名' }}</h2>
              <p>订单号: {{ booking.reserveNo || '-' }}</p>
            </div>
            <strong>{{ formatMoney(booking.payAmount ?? booking.totalAmount) }}</strong>
          </div>

          <div class="summary-grid">
            <div><span>参与人数</span><strong>{{ booking.participantCount || 1 }}</strong></div>
            <div><span>参与者</span><strong>{{ booking.participantName || '-' }}</strong></div>
            <div><span>电话</span><strong>{{ booking.participantPhone || '-' }}</strong></div>
            <div><span>活动时间</span><strong>{{ formatRange(booking.startTime, booking.endTime) }}</strong></div>
            <div class="full-span"><span>地点</span><strong>{{ fullLocation }}</strong></div>
          </div>
        </article>

        <article v-if="!success && booking.canPay" class="panel-card">
          <h2>选择支付方式</h2>
          <div class="method-grid">
            <button
              v-for="method in paymentMethods"
              :key="method.value"
              type="button"
              :class="['method-card', { active: selectedMethod === method.value }]"
              @click="selectedMethod = method.value"
            >
              <div class="method-icon" :class="method.theme"><i :class="method.icon"></i></div>
              <div class="method-copy">
                <strong>{{ method.label }}</strong>
                <p>{{ method.desc }}</p>
              </div>
            </button>
          </div>
          <div class="hint-box">
            <strong>当前行为</strong>
            <p>选择的支付方式会保存在订单中，支付会在应用内立即完成。</p>
          </div>
        </article>

        <article v-else class="panel-card success-panel">
          <div class="success-icon"><i class="bx bx-check-circle"></i></div>
          <h2>{{ success ? '支付成功，二维码已生成' : '该订单无需支付' }}</h2>
          <p>{{ successMessage }}</p>
        </article>
      </section>

      <aside class="side-column">
        <article class="panel-card sticky-card">
          <h2>支付汇总</h2>
          <div class="amount-box">
            <span>应付金额</span>
            <strong>{{ formatMoney(booking.payAmount ?? booking.totalAmount) }}</strong>
          </div>
          <div class="summary-list">
            <p>报名状态: {{ displayStatus.label }}</p>
            <p>支付状态: {{ paymentStatusText }}</p>
            <p>支付方式: {{ paymentLabel }}</p>
            <p v-if="booking.paymentTime">支付时间: {{ formatTime(booking.paymentTime) }}</p>
          </div>
          <div class="action-list">
            <button v-if="!success && booking.canPay" class="primary-btn" :disabled="processing" @click="confirmPayment">
              {{ processing ? '处理中...' : '确认支付' }}
            </button>
            <button class="secondary-btn" @click="openDetail">查看订单详情</button>
            <button v-if="success && booking.canOpenQr" class="ghost-btn" @click="openCheckin">
              打开核销二维码
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
import { getActivityBookingDetail, payActivityBooking } from '../../api/app'
import { resolveActivityBookingDisplayStatus } from '../../utils/activityBooking'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const processing = ref(false)
const success = ref(false)
const booking = ref(null)
const selectedMethod = ref('simulated')

const paymentMethods = [
  { value: 'simulated', label: '模拟支付', desc: '应用内立即完成支付，用于完整测试报名流程。', icon: 'bx bx-credit-card', theme: 'default' },
  { value: 'alipay', label: '支付宝', desc: '选择此方式后，支付仍会在应用内完成。', icon: 'bx bxl-alipay', theme: 'alipay' }
]

const resolvedBackTo = computed(() => (
  typeof route.query.backTo === 'string' && route.query.backTo ? route.query.backTo : '/personal/activities'
))

const displayStatus = computed(() => resolveActivityBookingDisplayStatus(booking.value))
const fullLocation = computed(() => (
  [booking.value?.locationProvince, booking.value?.locationCity, booking.value?.locationDistrict, booking.value?.locationDetail]
    .filter(Boolean)
    .join(' / ') || '-'
))
const paymentStatusText = computed(() => ({ paid: '已支付', unpaid: '未支付', refunded: '已退款' }[booking.value?.paymentStatus] || booking.value?.paymentStatus || '未支付'))
const paymentLabel = computed(() => ({ pending: '待选择', simulated: '模拟支付', free: '免费', alipay: '支付宝' }[booking.value?.paymentType || selectedMethod.value] || '待选择'))
const successMessage = computed(() => booking.value?.paymentStatus === 'paid'
  ? '订单已标记为支付成功。您可以打开二维码进行现场核销。'
  : '该订单无需再次支付。')

const showError = (message) => notify?.error?.(message)
const showSuccess = (message) => notify?.success?.(message)

const loadBooking = async () => {
  loading.value = true
  try {
    const response = await getActivityBookingDetail(route.params.id)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || 'Failed to load booking payment information')
    }
    booking.value = response.data
    success.value = booking.value.paymentStatus === 'paid'
    if (booking.value.paymentType && booking.value.paymentType !== 'pending') {
      selectedMethod.value = booking.value.paymentType
    }
  } catch (error) {
    booking.value = null
    showError(error.message || 'Failed to load booking payment information')
  } finally {
    loading.value = false
  }
}

const confirmPayment = async () => {
  if (!booking.value?.canPay || processing.value) {
    return
  }

  processing.value = true
  try {
    const response = await payActivityBooking(booking.value.id, { paymentType: selectedMethod.value })
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || 'Payment failed')
    }
    booking.value = response.data
    success.value = booking.value.paymentStatus === 'paid'
    showSuccess(success.value ? 'Payment successful' : 'Payment status updated')
    if (success.value) {
      router.replace({
        name: 'activity-booking-pay-result',
        params: { id: booking.value.id },
        query: { backTo: resolvedBackTo.value }
      })
    }
  } catch (error) {
    showError(error.message || 'Payment failed')
  } finally {
    processing.value = false
  }
}

const openDetail = () => {
  if (!booking.value?.id) return
  router.push({ name: 'activity-booking-detail', params: { id: booking.value.id }, query: { backTo: resolvedBackTo.value } })
}

const openCheckin = () => {
  if (!booking.value?.id) return
  router.push({ name: 'activity-booking-checkin', params: { id: booking.value.id }, query: { backTo: route.query.backTo || '/personal/checkins' } })
}

const goBack = () => {
  router.push(resolvedBackTo.value)
}

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
.payment-page { max-width: 1180px; margin: 0 auto; padding: 20px; }
.header-nav { display: flex; align-items: center; gap: 16px; margin-bottom: 22px; flex-wrap: wrap; }
.back-btn { display: inline-flex; align-items: center; gap: 6px; padding: 10px 16px; border-radius: 24px; border: 1px solid #d9cfc1; background: rgba(255,255,255,0.9); cursor: pointer; }
.breadcrumb { display: flex; align-items: center; gap: 8px; color: #8b8074; }
.breadcrumb span:first-child { cursor: pointer; }
.state-card, .panel-card { background: rgba(255,255,255,0.98); border: 1px solid #e5e7eb; border-radius: 24px; box-shadow: 0 18px 40px rgba(15,23,42,0.06); }
.state-card { padding: 80px 20px; text-align: center; color: #94a3b8; }
.payment-layout { display: grid; grid-template-columns: minmax(0,1.5fr) 320px; gap: 20px; }
.main-column { display: flex; flex-direction: column; gap: 18px; }
.panel-card { padding: 24px; }
.hero-panel { background: radial-gradient(circle at top right, rgba(56,189,248,0.18), transparent 28%), linear-gradient(135deg, #ffffff, #eff6ff); }
.eyebrow { margin: 0 0 8px; font-size: 12px; font-weight: 700; letter-spacing: 0.14em; text-transform: uppercase; color: #0369a1; }
.hero-panel h1, .panel-card h2 { margin: 0; color: #0f172a; }
.hero-panel p, .method-copy p, .hint-box p, .success-panel p { color: #64748b; line-height: 1.7; }
.order-head { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 20px; }
.order-head p { margin: 6px 0 0; color: #64748b; }
.order-head strong { font-size: 28px; color: #0f172a; }
.summary-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 16px; }
.summary-grid div { padding: 16px; border-radius: 18px; background: #f8fafc; border: 1px solid #e2e8f0; }
.summary-grid span, .amount-box span { display: block; font-size: 13px; color: #64748b; margin-bottom: 8px; }
.summary-grid strong, .amount-box strong { color: #0f172a; }
.full-span { grid-column: 1 / -1; }
.method-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 14px; margin-top: 18px; }
.method-card { display: flex; gap: 14px; align-items: flex-start; padding: 18px; border-radius: 18px; border: 1px solid #dbe4f0; background: #fff; cursor: pointer; text-align: left; }
.method-card.active { border-color: #38bdf8; box-shadow: 0 12px 30px rgba(56,189,248,0.12); }
.method-icon { width: 48px; height: 48px; border-radius: 16px; display: grid; place-items: center; color: #fff; font-size: 26px; flex-shrink: 0; }
.method-icon.default { background: linear-gradient(135deg, #334155, #475569); }
.method-icon.alipay { background: linear-gradient(135deg, #1677ff, #3b82f6); }
.method-copy strong { display: block; margin-bottom: 6px; color: #0f172a; }
.method-copy p { margin: 0; }
.hint-box, .amount-box { margin-top: 18px; padding: 16px 18px; border-radius: 18px; background: #f8fafc; border: 1px solid #dbe4f0; }
.hint-box strong { display: block; margin-bottom: 6px; color: #0f172a; }
.side-column { position: relative; }
.sticky-card { position: sticky; top: 20px; }
.summary-list { display: grid; gap: 8px; margin-top: 18px; color: #475569; }
.summary-list p { margin: 0; }
.action-list { display: grid; gap: 12px; margin-top: 22px; }
.primary-btn, .secondary-btn, .ghost-btn { width: 100%; border: none; border-radius: 16px; padding: 14px 18px; font-weight: 700; cursor: pointer; }
.primary-btn { background: linear-gradient(135deg, #0284c7, #0ea5e9); color: #fff; }
.secondary-btn { background: #e2e8f0; color: #0f172a; }
.ghost-btn { background: #fff; border: 1px solid #dbe4f0; color: #475569; }
.success-panel { text-align: center; }
.success-icon { width: 64px; height: 64px; margin: 0 auto 16px; display: grid; place-items: center; border-radius: 20px; background: #ecfeff; color: #0891b2; font-size: 34px; }
@media (max-width: 960px) { .payment-layout { grid-template-columns: 1fr; } .sticky-card { position: static; } }
@media (max-width: 720px) { .summary-grid, .method-grid { grid-template-columns: 1fr; } .order-head { flex-direction: column; } }
</style>
