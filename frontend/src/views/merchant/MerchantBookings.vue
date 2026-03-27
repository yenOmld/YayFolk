<template>
  <div class="merchant-reservations">
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <i class='bx bx-arrow-back'></i>
      </button>
      <div class="header-text">
        <h1>预约订单管理</h1>
        <p v-if="activityTitle" class="activity-filter">活动：{{ activityTitle }}</p>
      </div>
    </div>

    <div class="tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.key"
        :class="['tab-item', { 'active': activeTab === tab.key }]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
        <span class="count" v-if="tab.count > 0">{{ tab.count }}</span>
      </button>
    </div>

    <div class="reservations-list">
      <div 
        v-for="reservation in filteredReservations" 
        :key="reservation.id"
        class="reservation-card"
      >
        <div class="reservation-header">
          <div class="order-info">
            <span class="order-id">订单号: {{ reservation.id }}</span>
            <span class="order-time">{{ formatTime(reservation.createTime) }}</span>
          </div>
          <span :class="['status-badge', reservation.status]">
            {{ getStatusText(reservation.status) }}
          </span>
        </div>

        <div class="reservation-content">
          <div class="user-section">
            <div class="user-info">
              <i class='bx bx-user'></i>
              <div class="user-details">
                <span class="user-name">{{ reservation.participantName }}</span>
                <span class="user-phone">{{ reservation.participantPhone }}</span>
              </div>
            </div>
            <div class="product-info">
              <span class="product-name">{{ reservation.productName }}</span>
              <div class="reservation-details">
                <span><i class='bx bx-calendar'></i> {{ reservation.reservationTime }}</span>
                <span><i class='bx bx-user'></i> {{ reservation.participantCount }} 人</span>
              </div>
            </div>
          </div>

          <div class="price-section">
            <span class="price-label">订单金额</span>
            <span class="price-value">¥{{ formatPrice(reservation.totalPrice) }}</span>
            <span :class="['pay-badge', reservation.paymentStatus]">{{ getPayStatusText(reservation.paymentStatus) }}</span>
          </div>
        </div>

        <div class="qr-section" v-if="reservation.status === 'checked_in'">
          <div class="qr-info">
            <div class="qr-code">
              <i class='bx bx-check-shield'></i>
            </div>
            <span class="qr-text">核销时间：{{ formatTime(reservation.verificationTime) }}</span>
          </div>
        </div>

        <div class="reservation-actions">
          <template v-if="reservation.status === 'registered'">
            <button class="btn reject" @click="rejectReservation(reservation)">
              <i class='bx bx-x-circle'></i> 拒绝
            </button>
            <button class="btn confirm" @click="verifyReservation(reservation)">
              <i class='bx bx-check-circle'></i> 核销
            </button>
          </template>
          <template v-else-if="reservation.status === 'checked_in'">
            <button class="btn secondary" disabled>
              <i class='bx bx-check-double'></i> 已核销
            </button>
          </template>
          <template v-else-if="reservation.status === 'rejected'">
            <button class="btn secondary" disabled>
              <i class='bx bx-block'></i> 已拒绝
            </button>
          </template>
          <template v-else-if="reservation.status === 'cancelled'">
            <button class="btn secondary" disabled>
              <i class='bx bx-x-circle'></i> 用户已取消
            </button>
          </template>
        </div>
      </div>

      <div v-if="filteredReservations.length === 0" class="empty-state">
        <i class='bx bx-calendar-x'></i>
        <p>暂无预约订单</p>
      </div>
    </div>

    <div class="modal" v-if="showVerifyModal" @click="showVerifyModal = false">
      <div class="modal-content" @click.stop>
        <h3>核销确认</h3>
        <p>确认核销此预约订单吗？</p>
        <div class="verify-info" v-if="selectedReservation">
          <p><strong>用户：</strong>{{ selectedReservation.participantName }}</p>
          <p><strong>活动：</strong>{{ selectedReservation.productName }}</p>
          <p><strong>人数：</strong>{{ selectedReservation.participantCount }}</p>
        </div>
        <div class="modal-buttons">
          <button class="btn secondary" @click="showVerifyModal = false">取消</button>
          <button class="btn primary" @click="doVerify">确认核销</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  checkinBooking,
  getMerchantActivities,
  getMerchantActivityBookings,
  rejectBooking
} from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (message, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(message)
const router = useRouter()
const route = useRoute()
const activeTab = ref('registered')
const reservations = ref([])
const showVerifyModal = ref(false)
const selectedReservation = ref(null)
const activityId = ref(route.query.activityId || null)
const activityTitle = ref(route.query.title || '')

const tabs = computed(() => [
  { key: 'registered', label: '待核销', count: reservations.value.filter(r => r.status === 'registered').length },
  { key: 'checked_in', label: '已核销', count: reservations.value.filter(r => r.status === 'checked_in').length },
  { key: 'rejected', label: '已拒绝', count: reservations.value.filter(r => r.status === 'rejected').length },
  { key: 'cancelled', label: '已取消', count: reservations.value.filter(r => r.status === 'cancelled').length }
])

const filteredReservations = computed(() => {
  return reservations.value.filter(r => r.status === activeTab.value)
})

const getStatusText = (status) => {
  const statusMap = {
    registered: '待核销',
    checked_in: '已核销',
    rejected: '已拒绝',
    cancelled: '已取消'
  }
  return statusMap[status] || status
}

const getPayStatusText = (status) => {
  const statusMap = {
    unpaid: '未付款',
    paid: '已付款',
    refunded: '已退款'
  }
  return statusMap[status] || status || '未付款'
}

const switchTab = (key) => {
  activeTab.value = key
}

const goBack = () => {
  router.back()
}

const loadReservations = async () => {
  try {
    const activitiesRes = await getMerchantActivities()
    const activities = Array.isArray(activitiesRes.data) ? activitiesRes.data : []
    const activityMap = new Map(activities.map(item => [String(item.id), item]))

    if (activityId.value) {
      const response = await getMerchantActivityBookings(activityId.value)
      const activity = activityMap.get(String(activityId.value))
      reservations.value = (response.data || []).map(item => mapReservation(item, activity))
      if (!activityTitle.value) {
        activityTitle.value = activity?.title || ''
      }
    } else {
      const merged = []
      for (const activity of activities) {
        const response = await getMerchantActivityBookings(activity.id)
        const list = (response.data || []).map(item => mapReservation(item, activity))
        merged.push(...list)
      }
      reservations.value = merged.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
    }

    if (!tabs.value.some(tab => tab.key === activeTab.value && tab.count > 0)) {
      activeTab.value = tabs.value.find(tab => tab.count > 0)?.key || 'registered'
    }
  } catch (error) {
    console.error('获取预约列表失败:', error)
    notify(error?.response?.data?.message || '获取预约列表失败', 'error')
  }
}

const rejectReservation = async (reservation) => {
  if (window.confirm('确认拒绝该用户参加活动？拒绝后该用户不可再次报名该活动。')) {
    try {
      await rejectBooking(reservation.id)
      reservation.status = 'rejected'
      notify('已拒绝该报名', 'success')
    } catch (error) {
      console.error('拒绝失败:', error)
      notify(error?.response?.data?.message || '拒绝报名失败', 'error')
    }
  }
}

const verifyReservation = (reservation) => {
  selectedReservation.value = reservation
  showVerifyModal.value = true
}

const doVerify = async () => {
  if (selectedReservation.value) {
    try {
      await checkinBooking(selectedReservation.value.id)
      selectedReservation.value.status = 'checked_in'
      selectedReservation.value.verificationTime = new Date().toISOString()
      showVerifyModal.value = false
      notify('核销成功', 'success')
    } catch (error) {
      console.error('核销失败:', error)
      notify(error?.response?.data?.message || '核销失败', 'error')
      showVerifyModal.value = false
    }
  }
}

const mapReservation = (reservation, activity) => ({
  ...reservation,
  participantName: reservation.participantName || `用户${reservation.userId}`,
  participantPhone: reservation.participantPhone || '-',
  productName: activity?.title || activityTitle.value || '活动预约',
  reservationTime: buildReservationTime(activity),
  participantCount: reservation.participantCount || 1,
  totalPrice: Number(activity?.price || 0),
  paymentStatus: reservation.paymentStatus || 'unpaid'
})

const buildReservationTime = (activity) => {
  if (!activity?.startTime && !activity?.endTime) {
    return '-'
  }
  const start = formatTime(activity?.startTime)
  const end = formatTime(activity?.endTime)
  return activity?.endTime ? `${start} - ${end}` : start
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN')
}

const formatPrice = (value) => {
  const amount = Number(value || 0)
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
}

onMounted(() => {
  loadReservations()
})
</script>

<style scoped>
.merchant-reservations {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 80px;
}

.page-header {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  background: white;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #333;
  cursor: pointer;
  padding: 5px;
  margin-right: 15px;
}

.page-header h1 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.activity-filter {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.tabs {
  display: flex;
  background: white;
  padding: 10px;
  gap: 10px;
  overflow-x: auto;
}

.tab-item {
  flex: 1;
  min-width: 70px;
  padding: 10px 15px;
  border: none;
  background: #f5f5f5;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.tab-item.active {
  background: #7494ec;
  color: white;
}

.tab-item .count {
  background: rgba(255,255,255,0.3);
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
}

.reservations-list {
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.reservation-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.reservation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background: #fafafa;
  border-bottom: 1px solid #f5f5f5;
}

.order-info {
  display: flex;
  flex-direction: column;
}

.order-id {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.order-time {
  font-size: 12px;
  color: #999;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.registered {
  background: #fff7e6;
  color: #fa8c16;
}

.status-badge.checked_in {
  background: #f6ffed;
  color: #52c41a;
}

.status-badge.rejected {
  background: #fff1f0;
  color: #ff4d4f;
}

.status-badge.cancelled {
  background: #f5f5f5;
  color: #999;
}

.reservation-content {
  padding: 15px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.user-section {
  flex: 1;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.user-info i {
  font-size: 20px;
  color: #7494ec;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.user-phone {
  font-size: 12px;
  color: #999;
}

.product-info {
  margin-left: 30px;
}

.product-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  display: block;
}

.reservation-details {
  display: flex;
  gap: 15px;
}

.reservation-details span {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
}

.price-section {
  text-align: right;
}

.price-label {
  font-size: 12px;
  color: #999;
  display: block;
  margin-bottom: 4px;
}

.price-value {
  font-size: 18px;
  font-weight: 600;
  color: #ff4757;
}

.pay-badge {
  margin-top: 8px;
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.pay-badge.unpaid {
  background: #fff7e6;
  color: #fa8c16;
}

.pay-badge.paid {
  background: #f6ffed;
  color: #52c41a;
}

.pay-badge.refunded {
  background: #f5f5f5;
  color: #999;
}

.qr-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background: #e6f7ff;
  border-top: 1px solid #f5f5f5;
}

.qr-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.qr-code {
  width: 40px;
  height: 40px;
  background: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #1890ff;
}

.qr-text {
  font-size: 13px;
  color: #1890ff;
  font-weight: 500;
}

.reservation-actions {
  display: flex;
  gap: 10px;
  padding: 15px;
  border-top: 1px solid #f5f5f5;
}

.btn {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.3s;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.btn.primary {
  background: #7494ec;
  color: white;
}

.btn.secondary {
  background: #f0f0f0;
  color: #333;
}

.btn.confirm {
  background: #52c41a;
  color: white;
}

.btn.reject {
  background: white;
  color: #ff4757;
  border: 1px solid #ff4757;
}

.btn.verify {
  background: #1890ff;
  color: white;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state i {
  font-size: 60px;
  margin-bottom: 15px;
  opacity: 0.5;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 25px;
  width: 90%;
  max-width: 400px;
}

.modal-content h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #333;
}

.modal-content p {
  color: #666;
  margin-bottom: 15px;
}

.verify-info {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.verify-info p {
  margin: 8px 0;
  font-size: 14px;
  color: #333;
}

.modal-buttons {
  display: flex;
  gap: 10px;
}
</style>
