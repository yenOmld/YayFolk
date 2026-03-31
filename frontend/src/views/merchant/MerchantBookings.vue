<template>
  <div class="merchant-bookings-page">
    <div class="page-header">
      <div class="page-header__main">
        <button class="back-btn" @click="goBack">
          <i class="bx bx-arrow-back"></i>
        </button>
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageDescription }}</p>
          <p v-if="currentActivityTitle" class="activity-note">当前活动：{{ currentActivityTitle }}</p>
        </div>
      </div>
      <div class="header-actions">
        <button class="ghost-btn" @click="goScanPage">打开扫码</button>
        <button class="ghost-btn" @click="goRecordsPage">查看记录</button>
      </div>
    </div>

    <section class="toolbar-card">
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tab-item', { active: activeTab === tab.key }]"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
          <span>{{ tab.count }}</span>
        </button>
      </div>

      <div class="filters">
        <input
          v-model.trim="keyword"
          type="text"
          class="keyword-input"
          placeholder="搜索报名编号、联系人、电话或活动"
          @keyup.enter="applyFilters"
        >
        <select v-model="selectedActivityId" class="activity-select" @change="applyFilters">
          <option value="">全部活动</option>
          <option v-for="item in activities" :key="item.id" :value="String(item.id)">{{ item.title }}</option>
        </select>
        <button class="primary-btn" @click="applyFilters">筛选</button>
      </div>
    </section>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>正在加载商家报名...</p>
    </div>

    <div v-else-if="bookings.length === 0" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>没有符合当前筛选条件的报名。</p>
    </div>

    <div v-else class="booking-list">
      <article v-for="booking in bookings" :key="booking.id" class="booking-card">
        <div class="booking-card__top">
          <div>
            <strong>{{ booking.activityTitle || '活动' }}</strong>
            <span>报名编号：{{ booking.reserveNo || booking.id }}</span>
          </div>
          <em :class="['status-badge', booking.status]">{{ statusText(booking.status) }}</em>
        </div>

        <div class="booking-card__body">
          <div class="booking-user">
            <img :src="booking.customerAvatar || '/default-avatar.svg'" alt="Customer avatar" class="avatar">
            <div>
              <strong>{{ booking.customerName || booking.participantName || `用户 ${booking.userId}` }}</strong>
              <span>{{ booking.participantPhone || '-' }}</span>
            </div>
          </div>

          <div class="info-grid">
            <div><span>活动时间</span><strong>{{ booking.activityTime || formatRange(booking) }}</strong></div>
            <div><span>地点</span><strong>{{ formatLocation(booking) }}</strong></div>
            <div><span>人数</span><strong>{{ booking.participantCount || 1 }}</strong></div>
            <div><span>支付状态</span><strong>{{ paymentText(booking.paymentStatus) }}</strong></div>
            <div><span>金额</span><strong>{{ formatCurrency(booking.payAmount ?? booking.totalAmount) }}</strong></div>
            <div><span>创建时间</span><strong>{{ formatTime(booking.createTime) }}</strong></div>
          </div>

          <p v-if="booking.remark" class="remark">备注：{{ booking.remark }}</p>

          <div v-if="booking.timeline?.length" class="timeline">
            <div v-for="item in booking.timeline" :key="item.id" class="timeline-item">
              <span>{{ formatTime(item.createTime) }}</span>
              <strong>{{ statusText(item.newStatus) }}</strong>
              <small>{{ item.operatorName || item.operatorType || '系统' }}{{ item.remark ? ` · ${item.remark}` : '' }}</small>
            </div>
          </div>
        </div>

        <div class="booking-card__actions">
          <button class="ghost-btn" @click="goDetailPage(booking)">订单详情</button>
          <button class="ghost-btn" @click="goScanPage(booking.reserveNo || booking.id)">扫码/查询</button>
          <button
            v-if="booking.canReject"
            class="danger-btn"
            :disabled="submittingId === booking.id"
            @click="handleReject(booking)"
          >
            {{ submittingId === booking.id ? '处理中...' : '拒绝报名' }}
          </button>
          <button
            v-if="booking.canCheckin"
            class="primary-btn"
            :disabled="submittingId === booking.id"
            @click="handleCheckin(booking)"
          >
            {{ submittingId === booking.id ? '处理中...' : '确认核销' }}
          </button>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { checkinBooking, getMerchantActivities, getMerchantBookings, rejectBooking } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (message, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(message)
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submittingId = ref(null)
const activities = ref([])
const bookings = ref([])
const summary = ref({})
const keyword = ref('')
const activeTab = ref('registered')
const selectedActivityId = ref('')
const currentActivityTitle = ref('')

const isRecordPage = computed(() => route.name === 'merchant-bookings-records')
const pageTitle = computed(() => (isRecordPage.value ? '报名记录' : '报名管理'))
const pageDescription = computed(() => (
  isRecordPage.value
    ? '查看已完成、已拒绝、已取消及历史的报名记录。'
    : '处理实时报名、按活动筛选，并快速进入扫码核销。'
))

const emptySummary = () => ({
  bookingCount: 0,
  pendingCheckinCount: 0,
  checkedInCount: 0,
  rejectedCount: 0,
  cancelledCount: 0
})

const tabs = computed(() => [
  { key: 'registered', label: '待核销', count: Number(summary.value.pendingCheckinCount || 0) },
  { key: 'checked_in', label: '已核销', count: Number(summary.value.checkedInCount || 0) },
  { key: 'rejected', label: '已拒绝', count: Number(summary.value.rejectedCount || 0) },
  { key: 'cancelled', label: '已取消', count: Number(summary.value.cancelledCount || 0) },
  { key: 'all', label: '全部', count: Number(summary.value.bookingCount || 0) }
])

const normalizeRouteState = () => {
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  selectedActivityId.value = typeof route.query.activityId === 'string' ? route.query.activityId : ''
  currentActivityTitle.value = typeof route.query.title === 'string' ? route.query.title : ''

  if (typeof route.query.status === 'string') {
    activeTab.value = route.query.status
    return
  }

  activeTab.value = isRecordPage.value ? 'all' : 'registered'
}

const buildSummary = (items = []) => {
  return items.reduce((acc, item) => {
    acc.bookingCount += 1
    if (item.status === 'registered') acc.pendingCheckinCount += 1
    if (item.status === 'checked_in') acc.checkedInCount += 1
    if (item.status === 'rejected') acc.rejectedCount += 1
    if (item.status === 'cancelled') acc.cancelledCount += 1
    return acc
  }, emptySummary())
}

const loadActivities = async () => {
  const response = await getMerchantActivities()
  activities.value = response.code === 200 && Array.isArray(response.data) ? response.data : []
  if (!currentActivityTitle.value && selectedActivityId.value) {
    currentActivityTitle.value = activities.value.find(item => String(item.id) === selectedActivityId.value)?.title || ''
  }
}

const loadBookings = async () => {
  loading.value = true
  try {
    const response = await getMerchantBookings({
      activityId: selectedActivityId.value || undefined,
      keyword: keyword.value || undefined,
      status: activeTab.value === 'all' ? undefined : activeTab.value
    })
    if (response.code !== 200) {
      throw new Error(response.message || 'Failed to load bookings')
    }
    const items = Array.isArray(response.data?.items) ? response.data.items : []
    bookings.value = items
    summary.value = response.data?.summary || buildSummary(items)
  } catch (error) {
    bookings.value = []
    summary.value = emptySummary()
    notify(error.message || 'Failed to load bookings', 'error')
  } finally {
    loading.value = false
  }
}

const syncRoute = () => {
  const title = selectedActivityId.value
    ? activities.value.find(item => String(item.id) === selectedActivityId.value)?.title || currentActivityTitle.value
    : ''

  currentActivityTitle.value = title
  router.replace({
    path: isRecordPage.value ? '/merchant/bookings/records' : '/merchant/bookings',
    query: {
      ...(selectedActivityId.value ? { activityId: selectedActivityId.value, title } : {}),
      ...(keyword.value ? { keyword: keyword.value } : {}),
      ...(activeTab.value && activeTab.value !== (isRecordPage.value ? 'all' : 'registered') ? { status: activeTab.value } : {})
    }
  })
}

const applyFilters = async () => {
  syncRoute()
  await loadBookings()
}

const switchTab = async (tab) => {
  activeTab.value = tab
  await applyFilters()
}

const goBack = () => {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }
  router.push('/merchant/activities')
}

const goScanPage = (code = '') => {
  router.push({
    path: '/merchant/bookings/scan',
    query: {
      ...(selectedActivityId.value ? { activityId: selectedActivityId.value } : {}),
      ...(currentActivityTitle.value ? { title: currentActivityTitle.value } : {}),
      ...(keyword.value ? { keyword: keyword.value } : {}),
      ...(code ? { code } : {}),
      backTo: route.fullPath
    }
  })
}

const goRecordsPage = () => {
  router.push({
    path: '/merchant/bookings/records',
    query: {
      ...(selectedActivityId.value ? { activityId: selectedActivityId.value } : {}),
      ...(currentActivityTitle.value ? { title: currentActivityTitle.value } : {}),
      ...(keyword.value ? { keyword: keyword.value } : {}),
      backTo: route.fullPath
    }
  })
}

const goDetailPage = (booking) => {
  if (!booking?.id) {
    return
  }
  router.push({
    name: 'merchant-booking-detail',
    params: { id: booking.id },
    query: {
      ...(selectedActivityId.value ? { activityId: selectedActivityId.value } : {}),
      ...(currentActivityTitle.value ? { title: currentActivityTitle.value } : {}),
      ...(keyword.value ? { keyword: keyword.value } : {}),
      backTo: route.fullPath
    }
  })
}

const handleCheckin = async (booking) => {
  if (!window.confirm(`确认核销报名 ${booking.reserveNo || booking.id}？`)) {
    return
  }
  submittingId.value = booking.id
  try {
    const response = await checkinBooking(booking.id)
    if (response.code !== 200) {
      throw new Error(response.message || '核销失败')
    }
    notify('报名已核销', 'success')
    await loadBookings()
  } catch (error) {
    notify(error.message || '核销失败', 'error')
  } finally {
    submittingId.value = null
  }
}

const handleReject = async (booking) => {
  if (!window.confirm(`拒绝报名 ${booking.reserveNo || booking.id}？`)) {
    return
  }
  submittingId.value = booking.id
  try {
    const response = await rejectBooking(booking.id)
    if (response.code !== 200) {
      throw new Error(response.message || '拒绝失败')
    }
    notify('已拒绝该报名', 'success')
    await loadBookings()
  } catch (error) {
    notify(error.message || '拒绝失败', 'error')
  } finally {
    submittingId.value = null
  }
}

const formatTime = (value) => (value ? new Date(value).toLocaleString() : '未记录')
const formatCurrency = (value) => `¥${(Number(value || 0) / 100).toFixed(2)}`
const formatRange = (booking) => {
  if (!booking?.startTime && !booking?.endTime) return '待定'
  const start = booking.startTime ? formatTime(booking.startTime) : '待定'
  const end = booking.endTime ? formatTime(booking.endTime) : ''
  return end ? `${start} - ${end}` : start
}
const formatLocation = (booking) => [booking.locationProvince, booking.locationCity, booking.locationDistrict, booking.locationDetail].filter(Boolean).join(' / ') || '待定'
const paymentText = (status) => ({ paid: '已支付', unpaid: '未支付', refunded: '已退款' }[status] || status || '未支付')
const statusText = (status) => ({ registered: '待核销', checked_in: '已核销', rejected: '已拒绝', cancelled: '已取消' }[status] || status)

watch(() => route.fullPath, async () => {
  normalizeRouteState()
  await loadBookings()
})

onMounted(async () => {
  normalizeRouteState()
  await loadActivities()
  await loadBookings()
})
</script>

<style scoped>
.merchant-bookings-page { max-width: 1100px; margin: 0 auto; }
.page-header, .page-header__main, .header-actions, .tabs, .filters, .booking-card__top, .booking-card__actions, .booking-user, .timeline-item { display: flex; }
.page-header, .booking-card__top, .booking-card__actions { justify-content: space-between; }
.page-header { align-items: flex-start; gap: 18px; margin-bottom: 20px; }
.page-header__main { gap: 14px; align-items: flex-start; }
.page-header h1 { margin: 0; font-size: 28px; color: #0f172a; }
.page-header p { margin: 8px 0 0; color: #64748b; line-height: 1.7; }
.activity-note { color: #1661ab; font-weight: 600; }
.back-btn, .tab-item, .primary-btn, .ghost-btn, .danger-btn { border: none; cursor: pointer; }
.back-btn { width: 44px; height: 44px; border-radius: 50%; background: #fff; box-shadow: 0 8px 20px rgba(15,23,42,0.08); font-size: 22px; }
.header-actions, .filters { gap: 12px; }
.toolbar-card, .booking-card, .state-card { border-radius: 20px; border: 1px solid #e5e7eb; background: rgba(255,255,255,0.94); box-shadow: 0 16px 36px rgba(15,23,42,0.05); }
.toolbar-card { padding: 18px; margin-bottom: 18px; }
.tabs { gap: 12px; flex-wrap: wrap; }
.tab-item { padding: 10px 16px; border-radius: 999px; background: #f8fafc; color: #475569; font-weight: 700; }
.tab-item.active { background: #c04851; color: #fff; }
.tab-item span { margin-left: 8px; }
.filters { margin-top: 16px; align-items: center; flex-wrap: wrap; }
.keyword-input, .activity-select { min-height: 44px; padding: 0 14px; border-radius: 12px; border: 1px solid #d4d9e0; background: #fff; color: #1f2937; }
.keyword-input { flex: 1 1 280px; }
.activity-select { min-width: 220px; }
.primary-btn, .ghost-btn, .danger-btn { min-height: 44px; padding: 0 18px; border-radius: 12px; font-weight: 700; }
.primary-btn { background: #1661ab; color: #fff; }
.ghost-btn { background: #f4f6fb; color: #334155; }
.danger-btn { background: #fdecec; color: #b42318; }
.state-card { padding: 72px 20px; text-align: center; color: #94a3b8; }
.state-card i { font-size: 40px; }
.booking-list { display: flex; flex-direction: column; gap: 16px; }
.booking-card { padding: 18px; }
.booking-card__top { align-items: flex-start; gap: 16px; }
.booking-card__top strong { display: block; color: #111827; font-size: 18px; }
.booking-card__top span { display: block; margin-top: 6px; color: #6b7280; }
.status-badge { padding: 6px 12px; border-radius: 999px; font-style: normal; font-weight: 700; background: #f3f4f6; }
.status-badge.registered { color: #c04851; background: rgba(192,72,81,0.12); }
.status-badge.checked_in { color: #1661ab; background: rgba(22,97,171,0.12); }
.status-badge.rejected { color: #b45309; background: #fef3c7; }
.status-badge.cancelled { color: #6b7280; background: #e5e7eb; }
.booking-card__body { margin-top: 16px; }
.booking-user { gap: 12px; align-items: center; }
.avatar { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; }
.booking-user span { display: block; margin-top: 4px; color: #6b7280; }
.info-grid { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap: 14px; margin-top: 16px; }
.info-grid span { display: block; color: #7c8694; font-size: 13px; }
.info-grid strong { display: block; margin-top: 6px; color: #1f2937; line-height: 1.6; }
.remark { margin: 16px 0 0; color: #475569; line-height: 1.7; }
.timeline { margin-top: 16px; display: flex; flex-direction: column; gap: 10px; }
.timeline-item { gap: 14px; align-items: flex-start; padding: 12px 14px; border-radius: 14px; background: #faf8f5; }
.timeline-item span, .timeline-item small { color: #6b7280; }
.timeline-item strong { color: #1f2937; }
.timeline-item small { flex: 1; }
.booking-card__actions { gap: 12px; margin-top: 18px; flex-wrap: wrap; }
@media (max-width: 900px) { .info-grid { grid-template-columns: repeat(2, minmax(0,1fr)); } }
@media (max-width: 760px) {
  .page-header, .page-header__main, .booking-card__top { flex-direction: column; }
  .header-actions, .filters, .booking-card__actions { width: 100%; }
  .keyword-input, .activity-select, .primary-btn, .ghost-btn, .danger-btn { width: 100%; }
  .info-grid { grid-template-columns: 1fr; }
  .timeline-item { flex-direction: column; gap: 6px; }
}
</style>
