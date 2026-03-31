<template>
  <div class="pay-result-page">
    <div class="result-card">
      <div v-if="loading" class="state-box">
        <i class="bx bx-loader-circle bx-spin"></i>
        <p>同步支付结果中...</p>
      </div>

      <div v-else-if="paid" class="state-box success">
        <i class="bx bx-check-circle"></i>
        <h1>支付成功</h1>
        <p>{{ booking?.activityTitle || '活动报名' }}</p>
        <p class="meta">订单号: {{ booking?.reserveNo || '-' }}</p>
        <p class="amount">支付金额: {{ formatMoney(booking?.payAmount) }}</p>
        <p class="hint">您可以查看订单详情或直接打开二维码进行商家核销。</p>
      </div>

      <div v-else class="state-box pending">
        <i class="bx bx-time-five"></i>
        <h1>支付待处理</h1>
        <p>订单尚未标记为支付成功。您可以重新打开订单详情稍后再试。</p>
      </div>

      <div class="action-row">
        <button class="primary-btn" @click="openDetail">查看订单详情</button>
        <button v-if="paid && booking?.canOpenQr" class="checkin-btn" @click="openCheckin">
          打开核销二维码
        </button>
        <button class="ghost-btn" @click="goBack">返回</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getActivityBookingDetail } from '../../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const booking = ref(null)

const paid = computed(() => booking.value?.paymentStatus === 'paid')
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const loadDetail = async () => {
  const response = await getActivityBookingDetail(route.params.id)
  if (response.code !== 200 || !response.data) {
    throw new Error(response.message || '加载支付结果失败')
  }
  booking.value = response.data
}

const loadData = async () => {
  loading.value = true
  try {
    for (let index = 0; index < 3; index += 1) {
      await loadDetail()
      if (paid.value) {
        break
      }
      if (index < 2) {
        await sleep(1200)
      }
    }
  } catch (error) {
    notify?.error?.(error.message || '加载支付结果失败')
  } finally {
    loading.value = false
  }
}

const openDetail = () => {
  router.replace({
    name: 'activity-booking-detail',
    params: { id: route.params.id },
    query: { backTo: route.query.backTo || '/personal/activities' }
  })
}

const openCheckin = () => {
  router.replace({
    name: 'activity-booking-checkin',
    params: { id: route.params.id },
    query: { backTo: route.query.backTo || '/personal/checkins' }
  })
}

const goBack = () => {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }
  router.push('/personal/activities')
}

const formatMoney = (value) => `¥${(Number(value || 0) / 100).toFixed(2)}`

onMounted(loadData)
</script>

<style scoped>
.pay-result-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; background: linear-gradient(135deg, #f8f5f0 0%, #ede8df 100%); }
.result-card { width: 100%; max-width: 560px; padding: 36px; border-radius: 22px; background: rgba(255,255,255,0.95); border: 1px solid #e5e7eb; box-shadow: 0 18px 40px rgba(15,23,42,0.08); }
.state-box { display: flex; flex-direction: column; align-items: center; gap: 12px; text-align: center; }
.state-box i { font-size: 72px; }
.state-box.success i { color: #059669; }
.state-box.pending i, .state-box i.bx-loader-circle { color: #b45309; }
.state-box h1 { margin: 0; font-size: 28px; color: #111827; }
.state-box p { margin: 0; color: #64748b; line-height: 1.7; }
.meta, .amount { font-weight: 600; }
.hint { max-width: 420px; }
.action-row { display: flex; gap: 12px; margin-top: 24px; }
.primary-btn, .checkin-btn, .ghost-btn { flex: 1; border: none; border-radius: 14px; padding: 12px 16px; font-weight: 600; cursor: pointer; }
.primary-btn { background: linear-gradient(135deg, #9d2929, #b33030); color: #fff; }
.checkin-btn { background: linear-gradient(135deg, #0f766e, #14b8a6); color: #fff; }
.ghost-btn { background: #e2e8f0; color: #1e293b; }
@media (max-width: 720px) { .action-row { flex-direction: column; } }
</style>
