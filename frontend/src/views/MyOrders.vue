<template>
  <div class="orders-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">My Orders</p>
        <h1>我的订单与报名</h1>
        <p>
          统一查看商品订单和活动报名记录。待支付订单支持主动取消，已报名活动也支持用户主动取消，
          取消后会释放名额；已打卡记录仅保留展示，不允许再取消。
        </p>
      </div>
      <button class="ghost-btn" @click="loadData" :disabled="loading">
        {{ loading ? '加载中...' : '刷新列表' }}
      </button>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <span class="summary-label">商品订单</span>
        <strong>{{ summary.productOrderCount }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">活动报名</span>
        <strong>{{ summary.activityBookingCount }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">已打卡</span>
        <strong>{{ summary.checkedInCount }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>商品订单</h2>
        <span>{{ productOrders.length }} 条</span>
      </div>
      <div v-if="loading" class="empty-state">正在加载订单数据...</div>
      <div v-else-if="productOrders.length === 0" class="empty-state">
        还没有商品订单，先去看看非遗好物。
      </div>
      <div v-else class="card-list">
        <article v-for="order in productOrders" :key="`order-${order.id}`" class="order-card">
          <div class="card-main">
            <div class="title-row">
              <h3>{{ order.productName || '非遗商品订单' }}</h3>
              <span :class="['status-tag', order.status]">{{ orderStatusLabel(order.status) }}</span>
            </div>
            <p class="meta-line">订单号：{{ order.orderNo || '-' }}</p>
            <p class="meta-line">收货人：{{ order.receiverName || '-' }} / {{ order.receiverPhone || '-' }}</p>
            <p class="meta-line">地址：{{ fullAddress(order) }}</p>
            <p class="meta-line">数量：{{ order.quantity || 1 }}，实付：￥{{ formatMoney(order.payAmount) }}</p>
            <p class="meta-line">下单时间：{{ formatTime(order.createTime) }}</p>
            <p v-if="canCancelOrder(order)" class="action-hint">
              待支付订单支持主动取消，取消后可重新下单。
            </p>
          </div>
          <div class="card-actions">
            <button
              v-if="canCancelOrder(order)"
              class="danger-btn"
              :disabled="cancellingOrderId === order.id"
              @click="handleCancelOrder(order)"
            >
              {{ cancellingOrderId === order.id ? '取消中...' : '取消订单' }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>活动报名</h2>
        <span>{{ activityBookings.length }} 条</span>
      </div>
      <div v-if="loading" class="empty-state">正在加载报名数据...</div>
      <div v-else-if="activityBookings.length === 0" class="empty-state">
        还没有活动报名，去体验一场非遗活动吧。
      </div>
      <div v-else class="card-list">
        <article v-for="booking in activityBookings" :key="`booking-${booking.id}`" class="order-card">
          <img
            v-if="booking.activityCoverImage"
            :src="booking.activityCoverImage"
            alt="活动封面"
            class="cover-image"
          />
          <div class="card-main">
            <div class="title-row">
              <h3>{{ booking.activityTitle || '非遗活动报名' }}</h3>
              <span :class="['status-tag', bookingDisplayStatus(booking).code]">{{ bookingDisplayStatus(booking).label }}</span>
            </div>
            <p class="meta-line">报名人：{{ booking.participantName || '-' }} / {{ booking.participantPhone || '-' }}</p>
            <p class="meta-line">活动时间：{{ formatTime(booking.startTime) }} - {{ formatTime(booking.endTime) }}</p>
            <p class="meta-line">活动地点：{{ [booking.locationCity, booking.locationDetail].filter(Boolean).join(' ') || '-' }}</p>
            <p class="meta-line">商家：{{ booking.shopName || booking.merchantName || '-' }}</p>
            <p class="meta-line">报名时间：{{ formatTime(booking.createTime) }}</p>
            <p v-if="booking.remark" class="remark-line">备注：{{ booking.remark }}</p>
            <p class="status-note" :class="statusNoteClass(booking)">{{ bookingDisplayStatus(booking).description }}</p>
            <p v-if="canCancelBooking(booking)" class="action-hint">
              支持用户主动取消报名，取消后将自动释放活动名额。
            </p>
          </div>
          <div class="card-actions">
            <button
              v-if="canCancelBooking(booking)"
              class="danger-btn"
              :disabled="cancellingBookingId === booking.id"
              @click="handleCancelBooking(booking)"
            >
              {{ cancellingBookingId === booking.id ? '取消中...' : '取消报名' }}
            </button>
            <button
              v-else-if="canDeleteBooking(booking)"
              class="secondary-btn"
              :disabled="deletingBookingId === booking.id"
              @click="handleDeleteBooking(booking)"
            >
              {{ deletingBookingId === booking.id ? '删除中...' : '删除记录' }}
            </button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { cancelActivityBooking, cancelOrder, deleteActivityBooking, getMyOrderOverview } from '../api/app'
import {
  isCancelableActivityBooking,
  isDeletableActivityBooking,
  resolveActivityBookingDisplayStatus
} from '../utils/activityBooking'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const loading = ref(false)
const cancellingOrderId = ref(null)
const cancellingBookingId = ref(null)
const deletingBookingId = ref(null)
const summary = ref({
  productOrderCount: 0,
  activityBookingCount: 0,
  checkedInCount: 0
})
const productOrders = ref([])
const activityBookings = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyOrderOverview()
    const data = res.data || {}
    summary.value = {
      productOrderCount: data.summary?.productOrderCount || 0,
      activityBookingCount: data.summary?.activityBookingCount || 0,
      checkedInCount: data.summary?.checkedInCount || 0
    }
    productOrders.value = Array.isArray(data.productOrders) ? data.productOrders : []
    activityBookings.value = Array.isArray(data.activityBookings) ? data.activityBookings : []
  } catch (error) {
    notify?.error(error?.response?.data?.message || '加载订单失败')
  } finally {
    loading.value = false
  }
}

const canCancelOrder = (order) => order?.status === 'pending_payment'

const bookingDisplayStatus = (booking) => resolveActivityBookingDisplayStatus(booking)

const canCancelBooking = (booking) => isCancelableActivityBooking(booking)
const canDeleteBooking = (booking) => isDeletableActivityBooking(booking)

const handleCancelOrder = async (order) => {
  if (!canCancelOrder(order) || cancellingOrderId.value) {
    return
  }
  if (!window.confirm(`确认取消订单 ${order.orderNo || ''} 吗？`)) {
    return
  }

  cancellingOrderId.value = order.id
  try {
    const res = await cancelOrder(order.id)
    notify?.success(res.message || '订单已取消')
    await loadData()
  } catch (error) {
    notify?.error(error?.response?.data?.message || '取消订单失败')
  } finally {
    cancellingOrderId.value = null
  }
}

const handleCancelBooking = async (booking) => {
  if (!canCancelBooking(booking) || cancellingBookingId.value) {
    return
  }
  if (!window.confirm(`确认取消活动报名“${booking.activityTitle || '非遗活动'}”吗？`)) {
    return
  }

  cancellingBookingId.value = booking.id
  try {
    const res = await cancelActivityBooking(booking.id)
    notify?.success(res.message || '活动报名已取消')
    await loadData()
  } catch (error) {
    notify?.error(error?.response?.data?.message || '取消活动报名失败')
  } finally {
    cancellingBookingId.value = null
  }
}

const handleDeleteBooking = async (booking) => {
  if (!canDeleteBooking(booking) || deletingBookingId.value) {
    return
  }
  if (!window.confirm(`确认删除已取消的活动记录“${booking.activityTitle || '非遗活动'}”吗？`)) {
    return
  }

  deletingBookingId.value = booking.id
  try {
    const res = await deleteActivityBooking(booking.id)
    notify?.success(res.message || '活动记录已删除')
    await loadData()
  } catch (error) {
    notify?.error(error?.response?.data?.message || '删除活动记录失败')
  } finally {
    deletingBookingId.value = null
  }
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN')
}

const formatMoney = (value) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '0.00'
  }
  return Number(value).toFixed(2)
}

const fullAddress = (order) => {
  return [
    order?.receiverProvince,
    order?.receiverCity,
    order?.receiverDistrict,
    order?.receiverAddress
  ].filter(Boolean).join(' ') || '-'
}

const orderStatusLabel = (status) => {
  return {
    pending_payment: '待支付',
    paid: '已支付',
    shipped: '已发货',
    completed: '已完成',
    cancelled: '已取消'
  }[status] || status || '未知状态'
}

const statusNoteClass = (booking) => {
  const code = bookingDisplayStatus(booking).code
  if (code === 'completed' || code === 'checked_in') {
    return 'success-note'
  }
  if (code === 'rejected') {
    return 'warning-note'
  }
  if (code === 'cancelled' || code === 'missed') {
    return 'muted-note'
  }
  return 'info-note'
}

onMounted(loadData)
</script>

<style scoped>
.orders-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 20px 80px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.panel,
.summary-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.hero-card {
  padding: 28px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  background:
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.16), transparent 28%),
    linear-gradient(135deg, #ffffff, #f0fdfa);
}

.eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 12px;
  font-weight: 700;
}

.hero-card h1 {
  margin: 0 0 8px;
  font-size: 30px;
  color: #111827;
}

.hero-card p {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
  max-width: 720px;
}

.ghost-btn,
.danger-btn,
.secondary-btn {
  border: none;
  cursor: pointer;
  transition: 0.2s ease;
}

.ghost-btn {
  padding: 12px 18px;
  border-radius: 14px;
  background: #111827;
  color: #fff;
}

.ghost-btn:disabled,
.danger-btn:disabled,
.secondary-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-label {
  color: #6b7280;
  font-size: 14px;
}

.summary-card strong {
  font-size: 30px;
  color: #111827;
}

.panel {
  padding: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.panel-header h2 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.panel-header span,
.meta-line,
.remark-line {
  color: #6b7280;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order-card {
  border: 1px solid #eef2f7;
  border-radius: 18px;
  padding: 18px;
  display: flex;
  gap: 18px;
  align-items: flex-start;
}

.cover-image {
  width: 120px;
  height: 120px;
  border-radius: 16px;
  object-fit: cover;
  flex-shrink: 0;
}

.card-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.title-row h3 {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.status-tag {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-tag.pending_payment,
.status-tag.pending_start {
  background: #eff6ff;
  color: #2563eb;
}

.status-tag.pending_checkin {
  background: #fff7ed;
  color: #c2410c;
}

.status-tag.checked_in,
.status-tag.completed {
  background: #ecfdf5;
  color: #059669;
}

.status-tag.cancelled {
  background: #fef2f2;
  color: #dc2626;
}

.status-tag.rejected {
  background: #fef3c7;
  color: #b45309;
}

.status-tag.missed {
  background: #f8fafc;
  color: #64748b;
}

.danger-btn {
  padding: 10px 14px;
  border-radius: 12px;
  background: #fef2f2;
  color: #dc2626;
}

.danger-btn:hover:not(:disabled) {
  background: #fee2e2;
}

.secondary-btn {
  padding: 10px 14px;
  border-radius: 12px;
  background: #f3f4f6;
  color: #374151;
}

.secondary-btn:hover:not(:disabled) {
  background: #e5e7eb;
}

.action-hint,
.status-note {
  margin: 6px 0 0;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.6;
}

.action-hint {
  background: #fff7ed;
  color: #c2410c;
}

.success-note {
  background: #ecfdf5;
  color: #047857;
}

.info-note {
  background: #eff6ff;
  color: #1d4ed8;
}

.warning-note {
  background: #fffbeb;
  color: #b45309;
}

.muted-note {
  background: #f8fafc;
  color: #64748b;
}

.empty-state {
  padding: 32px 18px;
  text-align: center;
  color: #94a3b8;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .hero-card,
  .order-card {
    flex-direction: column;
  }

  .cover-image {
    width: 100%;
    height: 220px;
  }

  .title-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
