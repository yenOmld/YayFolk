<template>
  <div class="orders-page">
    <section v-if="paymentBanner" :class="['payment-banner', paymentBanner.type]">
      <div>
        <strong>{{ paymentBanner.title }}</strong>
        <p>{{ paymentBanner.message }}</p>
      </div>
      <button class="banner-close" @click="paymentBanner = null">Close</button>
    </section>

    <section class="hero-card">
      <div>
        <p class="eyebrow">My Orders</p>
        <h1>Orders And Bookings</h1>
        <p>
          Review product orders and activity bookings in one place. Pending product orders can continue to Alipay,
          and eligible activity bookings can be cancelled with the payment state refreshed afterward.
        </p>
      </div>
      <button class="ghost-btn" @click="loadData" :disabled="loading">
        {{ loading ? 'Loading...' : 'Refresh' }}
      </button>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <span class="summary-label">Product Orders</span>
        <strong>{{ summary.productOrderCount }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">Activity Bookings</span>
        <strong>{{ summary.activityBookingCount }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">Checked In</span>
        <strong>{{ summary.checkedInCount }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>Product Orders</h2>
        <span>{{ productOrders.length }} items</span>
      </div>
      <div v-if="loading" class="empty-state">Loading product orders...</div>
      <div v-else-if="productOrders.length === 0" class="empty-state">No product orders yet.</div>
      <div v-else class="card-list">
        <article
          v-for="order in productOrders"
          :key="`order-${order.id}`"
          :ref="(element) => registerOrderCard(order.id, element)"
          :class="['order-card', { highlighted: highlightedOrderId === Number(order.id) }]"
        >
          <div class="card-main">
            <div class="title-row">
              <h3>{{ order.productName || 'Product Order' }}</h3>
              <span :class="['status-tag', order.status]">{{ orderStatusLabel(order.status) }}</span>
            </div>
            <p class="meta-line">Order No: {{ order.orderNo || '-' }}</p>
            <p class="meta-line">Receiver: {{ order.receiverName || '-' }} / {{ order.receiverPhone || '-' }}</p>
            <p class="meta-line">Address: {{ fullAddress(order) }}</p>
            <p class="meta-line">Quantity: {{ order.quantity || 1 }}, Amount: CNY {{ formatMoney(order.payAmount) }}</p>
            <p class="meta-line">Created: {{ formatTime(order.createTime) }}</p>
            <p v-if="highlightedOrderId === Number(order.id)" class="status-note success-note">
              This is the latest payment result returned from Alipay.
            </p>
            <p v-else-if="canPayOrder(order)" class="status-note info-note">
              This order is still pending and can continue to Alipay.
            </p>
            <p v-else-if="canCancelOrder(order)" class="action-hint">
              Pending orders can still be cancelled before payment.
            </p>
          </div>
          <div class="card-actions">
            <button
              v-if="canPayOrder(order)"
              class="ghost-btn pay-btn"
              :disabled="payingOrderId === order.id"
              @click="handlePayOrder(order)"
            >
              {{ payingOrderId === order.id ? 'Redirecting...' : 'Pay Now' }}
            </button>
            <button
              v-if="canCancelOrder(order)"
              class="danger-btn"
              :disabled="cancellingOrderId === order.id"
              @click="handleCancelOrder(order)"
            >
              {{ cancellingOrderId === order.id ? 'Cancelling...' : 'Cancel Order' }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>Activity Bookings</h2>
        <span>{{ activityBookings.length }} items</span>
      </div>
      <div v-if="loading" class="empty-state">Loading activity bookings...</div>
      <div v-else-if="activityBookings.length === 0" class="empty-state">No activity bookings yet.</div>
      <div v-else class="card-list">
        <article v-for="booking in activityBookings" :key="`booking-${booking.id}`" class="order-card">
          <img
            v-if="booking.activityCoverImage"
            :src="booking.activityCoverImage"
            alt="activity cover"
            class="cover-image"
          />
          <div class="card-main">
            <div class="title-row">
              <h3>{{ booking.activityTitle || 'Activity Booking' }}</h3>
              <span :class="['status-tag', bookingDisplayStatus(booking).code]">{{ bookingDisplayStatus(booking).label }}</span>
            </div>
            <p class="meta-line">Participant: {{ booking.participantName || '-' }} / {{ booking.participantPhone || '-' }}</p>
            <p class="meta-line">Schedule: {{ formatTime(booking.startTime) }} - {{ formatTime(booking.endTime) }}</p>
            <p class="meta-line">Location: {{ [booking.locationCity, booking.locationDetail].filter(Boolean).join(' ') || '-' }}</p>
            <p class="meta-line">Merchant: {{ booking.shopName || booking.merchantName || '-' }}</p>
            <p class="meta-line">Created: {{ formatTime(booking.createTime) }}</p>
            <p v-if="booking.remark" class="remark-line">Remark: {{ booking.remark }}</p>
            <p class="status-note" :class="statusNoteClass(booking)">{{ bookingDisplayStatus(booking).description }}</p>
            <p v-if="canCancelBooking(booking) && booking.paymentStatus === 'paid'" class="action-hint">
              Cancelling this paid booking will trigger an Alipay refund.
            </p>
            <p v-else-if="canCancelBooking(booking)" class="action-hint">
              This booking can still be cancelled before it is used.
            </p>
          </div>
          <div class="card-actions">
            <button
              v-if="canCancelBooking(booking)"
              class="danger-btn"
              :disabled="cancellingBookingId === booking.id"
              @click="handleCancelBooking(booking)"
            >
              {{ cancellingBookingId === booking.id ? 'Cancelling...' : 'Cancel Booking' }}
            </button>
            <button
              v-else-if="canDeleteBooking(booking)"
              class="secondary-btn"
              :disabled="deletingBookingId === booking.id"
              @click="handleDeleteBooking(booking)"
            >
              {{ deletingBookingId === booking.id ? 'Deleting...' : 'Delete Record' }}
            </button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cancelActivityBooking, cancelOrder, deleteActivityBooking, getMyOrderOverview, payOrder, reconcileAlipayTrade } from '../api/app'
import { isAlipayPagePayment, submitAlipayForm } from '../utils/alipay'
import {
  isCancelableActivityBooking,
  isDeletableActivityBooking,
  resolveActivityBookingDisplayStatus
} from '../utils/activityBooking'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm
const route = useRoute()
const router = useRouter()

const returnPollAttempts = 8
const returnPollDelayMs = 1500

const loading = ref(false)
const payingOrderId = ref(null)
const cancellingOrderId = ref(null)
const cancellingBookingId = ref(null)
const deletingBookingId = ref(null)
const highlightedOrderId = ref(null)
const paymentBanner = ref(null)
const summary = ref({
  productOrderCount: 0,
  activityBookingCount: 0,
  checkedInCount: 0
})
const productOrders = ref([])
const activityBookings = ref([])
const orderCardRefs = new Map()

const registerOrderCard = (orderId, element) => {
  const normalizedId = Number(orderId)
  if (!element) {
    orderCardRefs.delete(normalizedId)
    return
  }
  orderCardRefs.set(normalizedId, element)
}

const canPayOrder = (order) => Boolean(order?.canPay)
const canCancelOrder = (order) => order?.status === 'pending_payment'
const bookingDisplayStatus = (booking) => resolveActivityBookingDisplayStatus(booking)
const canCancelBooking = (booking) => isCancelableActivityBooking(booking)
const canDeleteBooking = (booking) => isDeletableActivityBooking(booking)
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const applyOverview = (data = {}) => {
  summary.value = {
    productOrderCount: data.summary?.productOrderCount || 0,
    activityBookingCount: data.summary?.activityBookingCount || 0,
    checkedInCount: data.summary?.checkedInCount || 0
  }
  productOrders.value = Array.isArray(data.productOrders) ? data.productOrders : []
  activityBookings.value = Array.isArray(data.activityBookings) ? data.activityBookings : []
}

const fetchOverview = async () => {
  const res = await getMyOrderOverview()
  applyOverview(res.data || {})
  await nextTick()
}

const findOrderById = (orderId) => productOrders.value.find((item) => Number(item.id) === Number(orderId))

const pollReturnedOrderStatus = async (orderId) => {
  if (!Number.isFinite(orderId) || orderId <= 0) {
    return null
  }

  let matchedOrder = findOrderById(orderId)
  if (matchedOrder && !canPayOrder(matchedOrder)) {
    return matchedOrder
  }

  for (let index = 0; index < returnPollAttempts; index += 1) {
    if (index > 0) {
      await sleep(returnPollDelayMs)
      await fetchOverview()
    }
    matchedOrder = findOrderById(orderId)
    if (matchedOrder && !canPayOrder(matchedOrder)) {
      return matchedOrder
    }
  }

  return matchedOrder || null
}

const loadData = async () => {
  loading.value = true
  try {
    await fetchOverview()
    await handleReturnState()
  } catch (error) {
    notify?.error(error?.response?.data?.message || error?.message || 'Failed to load orders')
  } finally {
    loading.value = false
  }
}

const handleReturnState = async () => {
  const paymentStatus = typeof route.query.payment === 'string' ? route.query.payment : ''
  const orderId = Number(route.query.orderId || 0)
  const hasOrderTarget = Number.isFinite(orderId) && orderId > 0

  if (!paymentStatus) {
    return
  }

  let matchedOrder = null
  if (hasOrderTarget) {
    matchedOrder = await pollReturnedOrderStatus(orderId)
  }

  const resolvedSuccess = Boolean(matchedOrder && !canPayOrder(matchedOrder) && matchedOrder.status === 'paid')
  const finalStatus = resolvedSuccess ? 'success' : paymentStatus

  if (finalStatus === 'success') {
    paymentBanner.value = {
      type: 'success',
      title: 'Payment Success',
      message: hasOrderTarget
        ? `Order #${orderId} has been confirmed. We highlighted it below.`
        : 'The payment has been confirmed.'
    }
    notify?.success?.('Payment successful')
  } else {
    paymentBanner.value = {
      type: 'pending',
      title: 'Payment Pending',
      message: hasOrderTarget
        ? `Order #${orderId} is still syncing with Alipay. Refresh again in a moment if needed.`
        : 'The payment callback has not been confirmed yet. You can refresh the list in a moment.'
    }
    notify?.warning?.('Payment result is still pending')
  }

  if (hasOrderTarget) {
    highlightedOrderId.value = orderId
    await nextTick()
    const target = orderCardRefs.get(orderId)
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  const nextQuery = { ...route.query }
  delete nextQuery.payment
  delete nextQuery.orderId
  await router.replace({ path: route.path, query: nextQuery })
}

const handlePayOrder = async (order) => {
  if (!canPayOrder(order) || payingOrderId.value) {
    return
  }

  payingOrderId.value = order.id
  try {
    const res = await payOrder(order.id, { paymentType: 'alipay' })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || 'Failed to create Alipay payment')
    }
    if (isAlipayPagePayment(res.data)) {
      submitAlipayForm(res.data.formHtml)
      return
    }
    notify?.success('Order is already paid')
    await loadData()
  } catch (error) {
    notify?.error(error?.message || error?.response?.data?.message || 'Failed to create Alipay payment')
  } finally {
    payingOrderId.value = null
  }
}

const handleCancelOrder = async (order) => {
  if (!canCancelOrder(order) || cancellingOrderId.value) {
    return
  }
  confirm({
    title: '取消订单',
    message: `确定取消订单 ${order.orderNo || ''}？`,
    onConfirm: async () => {
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
  })
}

const handleCancelBooking = async (booking) => {
  if (!canCancelBooking(booking) || cancellingBookingId.value) {
    return
  }
  confirm({
    title: '取消报名',
    message: `确定取消报名 ${booking.activityTitle || '活动'}？`,
    onConfirm: async () => {
      cancellingBookingId.value = booking.id
      try {
        const res = await cancelActivityBooking(booking.id)
        notify?.success(res.message || '报名已取消')
        await loadData()
      } catch (error) {
        notify?.error(error?.response?.data?.message || '取消报名失败')
      } finally {
        cancellingBookingId.value = null
      }
    }
  })
}

const handleDeleteBooking = async (booking) => {
  if (!canDeleteBooking(booking) || deletingBookingId.value) {
    return
  }
  confirm({
    title: '删除记录',
    message: `确定删除已取消的报名 ${booking.activityTitle || '活动'}？`,
    onConfirm: async () => {
      deletingBookingId.value = booking.id
      try {
        const res = await deleteActivityBooking(booking.id)
        notify?.success(res.message || '报名记录已删除')
        await loadData()
      } catch (error) {
        notify?.error(error?.response?.data?.message || '删除报名记录失败')
      } finally {
        deletingBookingId.value = null
      }
    }
  })
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
  return (Number(value) / 100).toFixed(2)
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
    pending_payment: 'Pending Payment',
    paid: 'Paid',
    shipped: 'Shipped',
    completed: 'Completed',
    cancelled: 'Cancelled',
    refunded: 'Refunded'
  }[status] || status || 'Unknown'
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

.payment-banner,
.hero-card,
.panel,
.summary-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.payment-banner {
  padding: 18px 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.payment-banner.success {
  border-color: #bbf7d0;
  background: linear-gradient(135deg, #f0fdf4, #ecfeff);
}

.payment-banner.pending {
  border-color: #fde68a;
  background: linear-gradient(135deg, #fffbeb, #fff7ed);
}

.payment-banner strong {
  display: block;
  color: #111827;
  margin-bottom: 4px;
}

.payment-banner p {
  margin: 0;
  color: #6b7280;
}

.banner-close {
  border: none;
  background: rgba(255, 255, 255, 0.8);
  color: #334155;
  border-radius: 12px;
  padding: 10px 14px;
  cursor: pointer;
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

.pay-btn {
  background: linear-gradient(135deg, #1677ff, #3b82f6);
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
  transition: border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
}

.order-card.highlighted {
  border-color: #60a5fa;
  box-shadow: 0 18px 36px rgba(59, 130, 246, 0.16);
  transform: translateY(-2px);
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

.status-tag.cancelled,
.status-tag.refunded {
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

  .payment-banner,
  .hero-card,
  .order-card {
    flex-direction: column;
    align-items: flex-start;
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
