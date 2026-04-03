<template>
  <div class="result-page">
    <section class="hero-card">
      <div :class="['hero-icon', stateTone]">
        <i :class="stateIcon"></i>
      </div>

      <p class="eyebrow">活动支付结果</p>
      <h1>{{ stateTitle }}</h1>
      <p class="hero-copy">{{ stateDescription }}</p>

      <div v-if="showPollingHint" class="polling-box">
        <strong>等待确认中</strong>
        <p>
          浏览器已从支付宝返回，但异步回调可能仍在更新订单。
          我们正在自动刷新报名状态。
        </p>
        <span>尝试 {{ Math.max(attemptCount, 1) }} / {{ maxAttempts }}</span>
      </div>

      <div v-if="route.query.tradeNo" class="trade-box">
        <span>交易号</span>
        <strong>{{ route.query.tradeNo }}</strong>
      </div>
    </section>

    <section v-if="booking" class="content-grid">
      <article class="panel-card detail-card">
        <div class="panel-head">
          <div>
            <p class="panel-label">报名</p>
            <h2>{{ booking.activityTitle || '活动报名' }}</h2>
          </div>
          <span :class="['status-pill', displayStatus.code]">{{ displayStatus.label }}</span>
        </div>

        <div class="detail-grid">
          <div class="detail-item">
            <span>订单号</span>
            <strong>{{ booking.reserveNo || '-' }}</strong>
          </div>
          <div class="detail-item">
            <span>支付状态</span>
            <strong>{{ paymentStatusText }}</strong>
          </div>
          <div class="detail-item">
            <span>金额</span>
            <strong>{{ formatMoney(booking.payAmount ?? booking.totalAmount) }}</strong>
          </div>
          <div class="detail-item">
            <span>参与人数</span>
            <strong>{{ booking.participantCount || 1 }}</strong>
          </div>
          <div class="detail-item">
            <span>参与者</span>
            <strong>{{ booking.participantName || '-' }}</strong>
          </div>
          <div class="detail-item">
            <span>电话</span>
            <strong>{{ booking.participantPhone || '-' }}</strong>
          </div>
          <div class="detail-item wide">
            <span>日程</span>
            <strong>{{ formatRange(booking.startTime, booking.endTime) }}</strong>
          </div>
          <div class="detail-item wide">
            <span>地点</span>
            <strong>{{ fullLocation }}</strong>
          </div>
        </div>

        <p :class="['status-note', stateTone]">
          {{ displayStatus.description }}
        </p>
      </article>

      <aside class="panel-card side-card">
        <p class="panel-label">下一步</p>
        <h2>{{ actionTitle }}</h2>
        <p class="side-copy">{{ actionDescription }}</p>

        <div class="action-list">
          <button class="primary-btn" type="button" @click="openDetail">
            查看报名详情
          </button>
          <button
            v-if="booking.canOpenQr"
            class="secondary-btn"
            type="button"
            @click="openCheckin"
          >
            打开核销二维码
          </button>
          <button
            v-if="booking.activityId"
            class="ghost-btn"
            type="button"
            @click="openActivity"
          >
            打开活动页面
          </button>
          <button
            class="ghost-btn"
            type="button"
            :disabled="loading || refreshing"
            @click="refreshNow"
          >
            {{ refreshing ? '刷新中...' : '刷新状态' }}
          </button>
          <button class="ghost-btn" type="button" @click="goBack">
            返回我的活动
          </button>
          <button class="ghost-btn" type="button" @click="goToActivityList">
            回到活动列表
          </button>
        </div>
      </aside>
    </section>

    <section v-else-if="!loading" class="panel-card empty-card">
      <i class="bx bx-error-circle"></i>
      <h2>报名不可用</h2>
      <p>{{ errorMessage || '无法加载报名详情。' }}</p>
      <button class="primary-btn" type="button" @click="goBack">返回我的活动</button>
      <button class="ghost-btn" type="button" @click="goToActivityList">回到活动列表</button>
    </section>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getActivityBookingDetail, reconcileAlipayTrade } from '../../api/app'
import { resolveActivityBookingDisplayStatus } from '../../utils/activityBooking'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const route = useRoute()
const router = useRouter()

const maxAttempts = 8
const pollDelayMs = 1500

const loading = ref(false)
const refreshing = ref(false)
const attemptCount = ref(0)
const booking = ref(null)
const errorMessage = ref('')

const resolvedBackTo = computed(() => (
  typeof route.query.backTo === 'string' && route.query.backTo ? route.query.backTo : '/personal/activities'
))

const paid = computed(() => booking.value?.paymentStatus === 'paid')
const displayStatus = computed(() => resolveActivityBookingDisplayStatus(booking.value))
const fullLocation = computed(() => (
  [
    booking.value?.locationProvince,
    booking.value?.locationCity,
    booking.value?.locationDistrict,
    booking.value?.locationDetail
  ].filter(Boolean).join(' / ') || '-'
))
const paymentStatusText = computed(() => ({
  paid: '已支付',
  unpaid: '未支付',
  refunded: '已退款'
}[booking.value?.paymentStatus] || booking.value?.paymentStatus || '未支付'))

const stateTone = computed(() => {
  if (loading.value && !booking.value) {
    return 'loading'
  }
  if (errorMessage.value && !booking.value) {
    return 'error'
  }
  if (paid.value) {
    return 'success'
  }
  return 'pending'
})

const stateIcon = computed(() => {
  if (stateTone.value === 'loading') return 'bx bx-loader-alt bx-spin'
  if (stateTone.value === 'success') return 'bx bx-check-circle'
  if (stateTone.value === 'error') return 'bx bx-error-circle'
  return 'bx bx-time-five'
})

const stateTitle = computed(() => {
  if (stateTone.value === 'loading') return '加载支付结果'
  if (stateTone.value === 'success') return '支付已确认'
  if (stateTone.value === 'error') return '结果不可用'
  return '等待支付确认'
})

const stateDescription = computed(() => {
  if (stateTone.value === 'loading') {
    return '我们正在加载最新的报名状态并检查支付结果。'
  }
  if (stateTone.value === 'success') {
    return '报名已标记为已支付。您可以打开二维码或查看报名详情页面。'
  }
  if (stateTone.value === 'error') {
    return errorMessage.value || '无法加载报名结果。'
  }
  return '已从支付宝返回网站，但异步支付确认可能仍在处理中。'
})

const showPollingHint = computed(() => !paid.value && Boolean(booking.value) && attemptCount.value < maxAttempts)

const actionTitle = computed(() => {
  if (paid.value && booking.value?.canOpenQr) {
    return '二维码已就绪'
  }
  if (paid.value) {
    return '查看报名详情'
  }
  return '保持此页面打开'
})

const actionDescription = computed(() => {
  if (paid.value && booking.value?.canOpenQr) {
    return '您现在可以打开二维码，在核销时向商家出示。'
  }
  if (paid.value) {
    return '支付已完成。查看报名详情以获取最新状态。'
  }
  return '如果支付确认略有延迟，请稍后刷新或打开详情页面查看最新状态。'
})

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
const currentTradeNo = () => (typeof route.query.tradeNo === 'string' ? route.query.tradeNo : '')

const fetchBooking = async () => {
  const response = await getActivityBookingDetail(route.params.id)
  if (response.code !== 200 || !response.data) {
    throw new Error(response.message || '加载报名结果失败')
  }
  booking.value = response.data
  errorMessage.value = ''
}

const reconcileTrade = async () => {
  const tradeNo = currentTradeNo()
  if (!tradeNo) {
    return
  }
  await reconcileAlipayTrade({ outTradeNo: tradeNo })
}

const loadResult = async () => {
  loading.value = true
  try {
    await fetchBooking()
    if (!paid.value) {
      try {
        await reconcileTrade()
        await fetchBooking()
      } catch (error) {
      }
    }

    for (let index = 0; index < maxAttempts; index += 1) {
      attemptCount.value = index + 1
      if (paid.value) {
        if (index > 0) {
          notify?.success?.('支付已确认')
        }
        return
      }
      if (index < maxAttempts - 1) {
        await sleep(pollDelayMs)
        await fetchBooking()
      }
    }
  } catch (error) {
    booking.value = null
    errorMessage.value = error.message || '加载报名结果失败'
    notify?.error?.(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const refreshNow = async () => {
  if (refreshing.value) {
    return
  }

  refreshing.value = true
  try {
    await reconcileTrade()
    await fetchBooking()
    if (paid.value) {
      notify?.success?.('支付已确认')
    } else {
      notify?.warning?.('支付回调仍在处理中')
    }
  } catch (error) {
    errorMessage.value = error.message || '刷新报名结果失败'
    notify?.error?.(errorMessage.value)
  } finally {
    refreshing.value = false
  }
}

const openDetail = () => {
  router.push({
    name: 'activity-booking-detail',
    params: { id: route.params.id },
    query: { backTo: resolvedBackTo.value }
  })
}

const openCheckin = () => {
  router.push({
    name: 'activity-booking-checkin',
    params: { id: route.params.id },
    query: { backTo: '/personal/checkins' }
  })
}

const openActivity = () => {
  if (!booking.value?.activityId) {
    return
  }
  router.push(`/activity/${booking.value.activityId}`)
}

const goBack = () => {
  router.push(resolvedBackTo.value)
}

const goToActivityList = () => {
  router.push('/home/activity')
}

const formatMoney = (value) => `¥${(Number(value || 0) / 100).toFixed(2)}`
const formatTime = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '-')
const formatRange = (start, end) => {
  const startText = start ? formatTime(start) : 'TBD'
  const endText = end ? formatTime(end) : ''
  return endText ? `${startText} - ${endText}` : startText
}

onMounted(loadResult)
</script>

<style scoped>
.result-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 28px 20px 80px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.panel-card {
  border-radius: 28px;
  border: 1px solid #e5e7eb;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.07);
}

.hero-card {
  padding: 30px;
  background:
    radial-gradient(circle at top right, rgba(34, 197, 94, 0.12), transparent 30%),
    radial-gradient(circle at bottom left, rgba(59, 130, 246, 0.1), transparent 26%),
    linear-gradient(135deg, #ffffff, #f8fafc);
}

.hero-icon {
  width: 72px;
  height: 72px;
  border-radius: 24px;
  display: grid;
  place-items: center;
  font-size: 38px;
  margin-bottom: 18px;
}

.hero-icon.loading {
  background: #eff6ff;
  color: #2563eb;
}

.hero-icon.success {
  background: #ecfdf5;
  color: #059669;
}

.hero-icon.pending {
  background: #fff7ed;
  color: #c2410c;
}

.hero-icon.error {
  background: #fef2f2;
  color: #dc2626;
}

.eyebrow,
.panel-label {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-card h1,
.panel-card h2 {
  margin: 0;
  color: #111827;
}

.hero-copy,
.side-copy,
.polling-box p,
.trade-box span,
.status-note {
  color: #64748b;
  line-height: 1.7;
}

.hero-copy {
  margin: 10px 0 0;
  max-width: 760px;
}

.polling-box,
.trade-box {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 18px;
}

.polling-box {
  background: #fff7ed;
  border: 1px solid #fed7aa;
}

.polling-box strong,
.trade-box strong {
  display: block;
  color: #111827;
}

.polling-box span {
  display: inline-block;
  margin-top: 8px;
  color: #9a3412;
  font-weight: 700;
}

.trade-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) 320px;
  gap: 20px;
}

.panel-card {
  padding: 24px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.detail-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-item {
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.detail-item.wide {
  grid-column: 1 / -1;
}

.detail-item span {
  display: block;
  color: #64748b;
  font-size: 13px;
  margin-bottom: 8px;
}

.detail-item strong {
  color: #111827;
  line-height: 1.7;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.status-pill.pending_start,
.status-pill.pending_checkin {
  background: #eff6ff;
  color: #1d4ed8;
}

.status-pill.checked_in,
.status-pill.completed {
  background: #ecfdf5;
  color: #047857;
}

.status-pill.cancelled,
.status-pill.missed {
  background: #f8fafc;
  color: #64748b;
}

.status-pill.rejected {
  background: #fef2f2;
  color: #dc2626;
}

.status-note {
  margin: 18px 0 0;
  padding: 14px 16px;
  border-radius: 16px;
}

.status-note.success {
  background: #ecfdf5;
  color: #047857;
}

.status-note.pending {
  background: #fff7ed;
  color: #c2410c;
}

.status-note.loading,
.status-note.error {
  background: #f8fafc;
}

.action-list {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}

.primary-btn,
.secondary-btn,
.ghost-btn {
  width: 100%;
  min-height: 48px;
  border: none;
  border-radius: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s ease;
}

.primary-btn {
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  color: #ffffff;
}

.secondary-btn {
  background: linear-gradient(135deg, #1677ff, #3b82f6);
  color: #ffffff;
}

.ghost-btn {
  background: #f8fafc;
  color: #334155;
  border: 1px solid #e2e8f0;
}

.primary-btn:disabled,
.secondary-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.empty-card {
  text-align: center;
  padding: 54px 24px;
}

.empty-card i {
  font-size: 44px;
  color: #dc2626;
}

.empty-card p {
  margin: 12px auto 20px;
  max-width: 560px;
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .result-page {
    padding: 18px 14px 60px;
  }

  .panel-head,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .panel-head {
    flex-direction: column;
  }

  .detail-item.wide {
    grid-column: auto;
  }
}
</style>