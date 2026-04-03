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
        <button class="ghost-btn" @click="goScanPage">打开扫描器</button>
        <button class="ghost-btn" @click="goRecordsPage">打开记录</button>
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
          placeholder="搜索订单号、联系人、电话或活动"
          @keyup.enter="applyFilters"
        >
        <select v-model="selectedActivityId" class="activity-select" @change="applyFilters">
          <option value="">全部活动</option>
          <option v-for="item in activities" :key="item.id" :value="String(item.id)">{{ item.title }}</option>
        </select>
        <button class="primary-btn" @click="applyFilters">应用筛选</button>
      </div>
    </section>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>加载商家预订中...</p>
    </div>

    <div v-else-if="bookings.length === 0" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>没有符合当前筛选条件的预订。</p>
    </div>

    <template v-else>
      <div class="booking-list">
        <article v-for="booking in bookings" :key="booking.id" class="booking-card">
          <div class="booking-card__top">
            <div>
              <strong>{{ booking.activityTitle || '活动' }}</strong>
              <span>订单号：{{ booking.reserveNo || booking.id }}</span>
            </div>
            <em :class="['status-badge', booking.status]">{{ statusText(booking.status) }}</em>
          </div>

          <div class="booking-card__body">
            <div class="booking-user">
              <img :src="booking.customerAvatar || '/default-avatar.svg'" alt="用户头像" class="avatar">
              <div>
                <strong>{{ booking.customerName || booking.participantName || `用户 ${booking.userId}` }}</strong>
                <span>{{ booking.participantPhone || '-' }}</span>
              </div>
            </div>

            <div class="info-grid">
              <div><span>活动时间</span><strong>{{ booking.activityTime || formatRange(booking) }}</strong></div>
              <div><span>地点</span><strong>{{ formatLocation(booking) }}</strong></div>
              <div><span>参与人数</span><strong>{{ booking.participantCount || 1 }}</strong></div>
              <div><span>支付状态</span><strong>{{ paymentText(booking.paymentStatus) }}</strong></div>
              <div><span>金额</span><strong>{{ formatCurrency(booking.payAmount ?? booking.totalAmount) }}</strong></div>
              <div><span>创建时间</span><strong>{{ formatTime(booking.createTime) }}</strong></div>
            </div>

            <p v-if="booking.paymentStatus === 'refunded' && booking.status === 'registered'" class="hint-banner refund-hint">
              此订单已退款。用户可以再次付款并继续参与。
            </p>
            <p v-else-if="booking.status === 'rejected'" class="hint-banner reject-hint">
              此订单已被拒绝，用户将被阻止使用此记录参与活动。
            </p>
            <p v-if="booking.remark" class="remark">备注：{{ booking.remark }}</p>

            <div v-if="booking.timeline?.length" class="timeline">
              <div v-for="item in booking.timeline" :key="item.id" class="timeline-item">
                <span>{{ formatTime(item.createTime) }}</span>
                <strong>{{ statusText(item.newStatus) }}</strong>
                <small>{{ item.operatorName || item.operatorType || '系统' }}{{ item.remark ? ` | ${item.remark}` : '' }}</small>
              </div>
            </div>
          </div>

          <div class="booking-card__actions">
            <button class="ghost-btn" @click="goDetailPage(booking)">订单详情</button>
            <button class="ghost-btn" @click="goScanPage(booking.reserveNo || booking.id)">扫描 / 查找</button>
            <button
              v-if="booking.status === 'registered'"
              class="refund-btn"
              :disabled="isBusy(booking.id) || !booking.canRefund"
              @click="handleRefund(booking)"
            >
              {{ actionLabel(booking.id, 'refund', '仅退款') }}
            </button>
            <button
              v-if="booking.canReject"
              class="danger-btn"
              :disabled="isBusy(booking.id)"
              @click="handleReject(booking)"
            >
              {{ actionLabel(booking.id, 'reject', '拒绝预订') }}
            </button>
            <button
              v-if="booking.canCheckin"
              class="primary-btn"
              :disabled="isBusy(booking.id)"
              @click="handleCheckin(booking)"
            >
              {{ actionLabel(booking.id, 'checkin', '确认签到') }}
            </button>
          </div>
        </article>
      </div>

      <section class="pagination-card">
        <div class="pagination-summary">
          <strong>{{ total }}</strong>
          <span>{{ pageSummary }}</span>
        </div>

        <div class="pagination-controls">
          <label class="page-size-field">
            <span>每页</span>
            <select v-model="pageSize" @change="handlePageSizeChange">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
            </select>
          </label>

          <div class="pager-buttons">
            <button class="pager-btn" :disabled="currentPage <= 1 || loading" @click="changePage(currentPage - 1)">上一页</button>
            <span class="page-indicator">第 {{ currentPage }} 页 / 共 {{ totalPages || 1 }} 页</span>
            <button class="pager-btn" :disabled="currentPage >= totalPages || loading" @click="changePage(currentPage + 1)">下一页</button>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { checkinBooking, getMerchantActivities, getMerchantBookings, refundMerchantBooking, rejectBooking } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (message, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(message)
const confirm = appContext.config.globalProperties.$confirm
const route = useRoute()
const router = useRouter()

const DEFAULT_PAGE_SIZE = 3
const pageSizeOptions = [3, 6, 9, 12]

const loading = ref(false)
const submittingId = ref(null)
const submittingAction = ref('')
const activities = ref([])
const bookings = ref([])
const summary = ref({})
const keyword = ref('')
const activeTab = ref('registered')
const selectedActivityId = ref('')
const currentActivityTitle = ref('')
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const total = ref(0)
const totalPages = ref(0)

const isRecordPage = computed(() => route.name === 'merchant-bookings-records')
const pageTitle = computed(() => (isRecordPage.value ? '预订记录' : '预订管理'))
const pageDescription = computed(() => (
  isRecordPage.value
    ? '查看已完成、已拒绝、已取消和历史预订记录。'
    : '处理活跃预订，扫描二维码，退款或阻止参与。'
))
const pageSummary = computed(() => {
  if (!total.value) {
    return '暂无预订记录。'
  }
  const start = (currentPage.value - 1) * pageSize.value + 1
  const end = Math.min(currentPage.value * pageSize.value, total.value)
  return `显示 ${start}-${end} 共 ${total.value} 个预订。`
})

const emptySummary = () => ({
  bookingCount: 0,
  pendingCheckinCount: 0,
  checkedInCount: 0,
  rejectedCount: 0,
  cancelledCount: 0
})

const tabs = computed(() => [
  { key: 'registered', label: '活跃', count: Number(summary.value.pendingCheckinCount || 0) },
  { key: 'checked_in', label: '已签到', count: Number(summary.value.checkedInCount || 0) },
  { key: 'rejected', label: '已拒绝', count: Number(summary.value.rejectedCount || 0) },
  { key: 'cancelled', label: '已取消', count: Number(summary.value.cancelledCount || 0) },
  { key: 'all', label: '全部', count: Number(summary.value.bookingCount || 0) }
])

const normalizeRouteState = () => {
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  selectedActivityId.value = typeof route.query.activityId === 'string' ? route.query.activityId : ''
  currentActivityTitle.value = typeof route.query.title === 'string' ? route.query.title : ''

  const page = Number(route.query.page || 1)
  const size = Number(route.query.size || DEFAULT_PAGE_SIZE)
  currentPage.value = Number.isFinite(page) && page > 0 ? page : 1
  pageSize.value = pageSizeOptions.includes(size) ? size : DEFAULT_PAGE_SIZE

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
      status: activeTab.value === 'all' ? undefined : activeTab.value,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    if (response.code !== 200) {
      throw new Error(response.message || 'Failed to load bookings')
    }

    const payload = response.data || {}
    const items = Array.isArray(payload.items) ? payload.items : []
    bookings.value = items
    summary.value = payload.summary || buildSummary(items)
    total.value = Number(payload.total || 0)
    totalPages.value = Math.max(Number(payload.totalPages || 0), items.length ? 1 : 0)

    if (totalPages.value > 0 && currentPage.value > totalPages.value) {
      currentPage.value = totalPages.value
      await syncRoute()
      return
    }
  } catch (error) {
    bookings.value = []
    summary.value = emptySummary()
    total.value = 0
    totalPages.value = 0
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
  return router.replace({
    path: isRecordPage.value ? '/merchant/bookings/records' : '/merchant/bookings',
    query: {
      ...(selectedActivityId.value ? { activityId: selectedActivityId.value, title } : {}),
      ...(keyword.value ? { keyword: keyword.value } : {}),
      ...(activeTab.value && activeTab.value !== (isRecordPage.value ? 'all' : 'registered') ? { status: activeTab.value } : {}),
      ...(currentPage.value > 1 ? { page: String(currentPage.value) } : {}),
      ...(pageSize.value !== DEFAULT_PAGE_SIZE ? { size: String(pageSize.value) } : {})
    }
  })
}

const resetToFirstPage = () => {
  currentPage.value = 1
}

const applyFilters = async () => {
  resetToFirstPage()
  await syncRoute()
}

const switchTab = async (tab) => {
  activeTab.value = tab
  resetToFirstPage()
  await syncRoute()
}

const changePage = async (page) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) {
    return
  }
  currentPage.value = page
  await syncRoute()
}

const handlePageSizeChange = async () => {
  resetToFirstPage()
  await syncRoute()
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
      ...(activeTab.value ? { status: activeTab.value } : {}),
      ...(currentPage.value > 1 ? { page: String(currentPage.value) } : {}),
      ...(pageSize.value !== DEFAULT_PAGE_SIZE ? { size: String(pageSize.value) } : {}),
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
      ...(pageSize.value !== DEFAULT_PAGE_SIZE ? { size: String(pageSize.value) } : {}),
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
      ...(activeTab.value ? { status: activeTab.value } : {}),
      ...(currentPage.value > 1 ? { page: String(currentPage.value) } : {}),
      ...(pageSize.value !== DEFAULT_PAGE_SIZE ? { size: String(pageSize.value) } : {}),
      backTo: route.fullPath
    }
  })
}

const isBusy = (bookingId) => submittingId.value === bookingId

const actionLabel = (bookingId, action, idleLabel) => {
  if (submittingId.value !== bookingId || submittingAction.value !== action) {
    return idleLabel
  }
  return {
    refund: '退款中...',
    reject: '拒绝中...',
    checkin: '签到中...'
  }[action] || idleLabel
}

const handleCheckin = async (booking) => {
  confirm({
    title: '确认签到',
    message: `确认签到订单 ${booking.reserveNo || booking.id}？`,
    onConfirm: async () => {
      submittingId.value = booking.id
      submittingAction.value = 'checkin'
      try {
        const response = await checkinBooking(booking.id)
        if (response.code !== 200) {
          throw new Error(response.message || '签到失败')
        }
        notify('签到成功。', 'success')
        await loadBookings()
      } catch (error) {
        notify(error.message || '签到失败', 'error')
      } finally {
        submittingId.value = null
        submittingAction.value = ''
      }
    }
  })
}

const handleRefund = async (booking) => {
  if (!booking?.canRefund) {
    notify('只有已支付的订单才能退款。', 'warning')
    return
  }
  confirm({
    title: '确认退款',
    message: `退款订单 ${booking.reserveNo || booking.id}？用户之后可以再次付款。`,
    onConfirm: async () => {
      submittingId.value = booking.id
      submittingAction.value = 'refund'
      try {
        const response = await refundMerchantBooking(booking.id, {
          reason: '商家退款，订单保持活跃'
        })
        if (response.code !== 200) {
          throw new Error(response.message || '退款失败')
        }
        notify('退款完成。用户稍后可以再次付款。', 'success')
        await loadBookings()
      } catch (error) {
        notify(error.message || '退款失败', 'error')
      } finally {
        submittingId.value = null
        submittingAction.value = ''
      }
    }
  })
}

const handleReject = async (booking) => {
  const message = booking.canRefund
    ? `拒绝订单 ${booking.reserveNo || booking.id}？已支付的订单将被退款，用户将被阻止使用此记录参与活动。`
    : `拒绝订单 ${booking.reserveNo || booking.id}？用户将被阻止使用此记录参与活动。`

  confirm({
    title: '拒绝预订',
    message,
    onConfirm: async () => {
      submittingId.value = booking.id
      submittingAction.value = 'reject'
      try {
        const response = await rejectBooking(booking.id, {
          reason: '商家拒绝预订'
        })
        if (response.code !== 200) {
          throw new Error(response.message || '拒绝失败')
        }
        notify('拒绝成功。', 'success')
        await loadBookings()
      } catch (error) {
        notify(error.message || '拒绝失败', 'error')
      } finally {
        submittingId.value = null
        submittingAction.value = ''
      }
    }
  })
}

const formatTime = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '未记录')
const formatCurrency = (value) => `¥ ${(Number(value || 0) / 100).toFixed(2)}`
const formatRange = (booking) => {
  if (!booking?.startTime && !booking?.endTime) return '待定'
  const start = booking.startTime ? formatTime(booking.startTime) : '待定'
  const end = booking.endTime ? formatTime(booking.endTime) : ''
  return end ? `${start} - ${end}` : start
}
const formatLocation = (booking) => [booking.locationProvince, booking.locationCity, booking.locationDistrict, booking.locationDetail].filter(Boolean).join(' / ') || '待定'
const paymentText = (status) => ({ paid: '已支付', unpaid: '未支付', refunded: '已退款' }[status] || status || '未支付')
const statusText = (status) => ({ registered: '活跃', checked_in: '已签到', rejected: '已拒绝', cancelled: '已取消' }[status] || status)

watch(
  () => route.fullPath,
  async () => {
    normalizeRouteState()
    await loadBookings()
  }
)

onMounted(async () => {
  normalizeRouteState()
  await loadActivities()
  await loadBookings()
})
</script>

<style scoped>
.merchant-bookings-page { max-width: 1100px; margin: 0 auto; }
.page-header, .page-header__main, .header-actions, .tabs, .filters, .booking-card__top, .booking-card__actions, .booking-user, .timeline-item, .pagination-controls, .pager-buttons, .page-size-field, .pagination-summary { display: flex; }
.page-header, .booking-card__top, .booking-card__actions { justify-content: space-between; }
.page-header { align-items: flex-start; gap: 18px; margin-bottom: 20px; }
.page-header__main { gap: 14px; align-items: flex-start; }
.page-header h1 { margin: 0; font-size: 28px; color: #0f172a; }
.page-header p { margin: 8px 0 0; color: #64748b; line-height: 1.7; }
.activity-note { color: #1661ab; font-weight: 600; }
.back-btn, .tab-item, .primary-btn, .ghost-btn, .danger-btn, .refund-btn, .pager-btn { border: none; cursor: pointer; }
.back-btn { width: 44px; height: 44px; border-radius: 50%; background: #fff; box-shadow: 0 8px 20px rgba(15,23,42,0.08); font-size: 22px; }
.header-actions, .filters { gap: 12px; }
.toolbar-card, .booking-card, .state-card, .pagination-card { border-radius: 20px; border: 1px solid #e5e7eb; background: rgba(255,255,255,0.94); box-shadow: 0 16px 36px rgba(15,23,42,0.05); }
.toolbar-card { padding: 18px; margin-bottom: 18px; }
.tabs { gap: 12px; flex-wrap: wrap; }
.tab-item { padding: 10px 16px; border-radius: 999px; background: #f8fafc; color: #475569; font-weight: 700; }
.tab-item.active { background: #c04851; color: #fff; }
.tab-item span { margin-left: 8px; }
.filters { margin-top: 16px; align-items: center; flex-wrap: wrap; }
.keyword-input, .activity-select { min-height: 44px; padding: 0 14px; border-radius: 12px; border: 1px solid #d4d9e0; background: #fff; color: #1f2937; }
.keyword-input { flex: 1 1 280px; }
.activity-select { min-width: 220px; }
.primary-btn, .ghost-btn, .danger-btn, .refund-btn { min-height: 44px; padding: 0 18px; border-radius: 12px; font-weight: 700; }
.primary-btn { background: #1661ab; color: #fff; }
.ghost-btn { background: #f4f6fb; color: #334155; }
.refund-btn { background: #fff7ed; color: #c2410c; }
.danger-btn { background: #fdecec; color: #b42318; }
button:disabled { opacity: 0.7; cursor: not-allowed; }
.state-card { padding: 72px 20px; text-align: center; color: #94a3b8; }
.state-card i { font-size: 40px; }
.booking-list { display: flex; flex-direction: column; gap: 16px; }
.booking-card { padding: 18px; }
.booking-card__top { align-items: flex-start; gap: 16px; }
.booking-card__top strong { display: block; color: #111827; font-size: 18px; }
.booking-card__top span { display: block; margin-top: 6px; color: #6b7280; }
.status-badge { padding: 6px 12px; border-radius: 999px; font-style: normal; font-weight: 700; background: #f3f4f6; }
.status-badge.registered { color: #1d4ed8; background: #dbeafe; }
.status-badge.checked_in { color: #15803d; background: #dcfce7; }
.status-badge.rejected { color: #b42318; background: #fee2e2; }
.status-badge.cancelled { color: #6b7280; background: #e5e7eb; }
.booking-card__body { margin-top: 16px; }
.booking-user { gap: 12px; align-items: center; }
.avatar { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; }
.booking-user span { display: block; margin-top: 4px; color: #6b7280; }
.info-grid { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap: 14px; margin-top: 16px; }
.info-grid span { display: block; color: #7c8694; font-size: 13px; }
.info-grid strong { display: block; margin-top: 6px; color: #1f2937; line-height: 1.6; }
.hint-banner { margin: 16px 0 0; padding: 12px 14px; border-radius: 14px; line-height: 1.7; }
.refund-hint { background: #fff7ed; color: #c2410c; }
.reject-hint { background: #fee2e2; color: #b42318; }
.remark { margin: 16px 0 0; color: #475569; line-height: 1.7; }
.timeline { margin-top: 16px; display: flex; flex-direction: column; gap: 10px; }
.timeline-item { gap: 14px; align-items: flex-start; padding: 12px 14px; border-radius: 14px; background: #faf8f5; }
.timeline-item span, .timeline-item small { color: #6b7280; }
.timeline-item strong { color: #1f2937; }
.timeline-item small { flex: 1; }
.booking-card__actions { gap: 12px; margin-top: 18px; flex-wrap: wrap; }
.pagination-card { margin-top: 18px; padding: 18px; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.pagination-summary { align-items: baseline; gap: 10px; color: #64748b; }
.pagination-summary strong { color: #111827; font-size: 24px; }
.pagination-controls, .pager-buttons, .page-size-field { align-items: center; gap: 12px; }
.page-size-field span, .page-indicator { color: #64748b; font-size: 13px; }
.page-size-field select, .pager-btn { min-height: 40px; border-radius: 10px; border: 1px solid #d4d9e0; background: #fff; }
.page-size-field select { padding: 0 12px; }
.pager-btn { padding: 0 14px; color: #334155; }
@media (max-width: 900px) { .info-grid { grid-template-columns: repeat(2, minmax(0,1fr)); } }
@media (max-width: 760px) {
  .page-header, .page-header__main, .booking-card__top, .pagination-card { flex-direction: column; }
  .header-actions, .filters, .booking-card__actions, .pagination-controls, .pager-buttons, .page-size-field { width: 100%; }
  .keyword-input, .activity-select, .primary-btn, .ghost-btn, .danger-btn, .refund-btn, .pager-btn, .page-size-field select { width: 100%; }
  .info-grid { grid-template-columns: 1fr; }
  .timeline-item { flex-direction: column; gap: 6px; }
}
</style>
