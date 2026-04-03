<template>
  <div class="stats-panel">
    <div v-if="loading" class="stats-empty">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>加载商家分析数据中...</p>
    </div>

    <template v-else>
      <div class="stats-grid">
        <button
          v-for="item in summaryCards"
          :key="item.key"
          type="button"
          class="stats-card clickable"
          @click="emitNavigate(item.target)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.note }}</small>
        </button>
      </div>

      <section class="chart-card clickable" @click="emitNavigate(chartTarget)">
        <div class="chart-head">
          <div>
            <h4>{{ chartMeta.title }}</h4>
            <p>{{ chartMeta.description }}</p>
          </div>
          <span class="chart-link">{{ chartMeta.linkLabel }}</span>
        </div>
        <div ref="chartRef" class="chart-area"></div>
      </section>

      <div class="split-grid">
        <section class="panel-card">
          <div class="panel-head">
            <h4>预订状态</h4>
            <span>{{ bookingStatus.length }} 项</span>
          </div>
          <div class="status-stack">
            <button
              v-for="item in bookingStatus"
              :key="item.key"
              type="button"
              class="status-row clickable"
              @click="emitNavigate({ type: 'bookings', status: item.key })"
            >
              <span class="status-dot" :style="{ background: item.color }"></span>
              <strong>{{ item.label }}</strong>
              <em>{{ formatCount(item.count) }}</em>
            </button>
          </div>
        </section>

        <section class="panel-card">
          <div class="panel-head">
            <h4>热门活动</h4>
            <span>{{ topActivities.length }} 项</span>
          </div>
          <div v-if="topActivities.length" class="list-stack">
            <button
              v-for="item in topActivities"
              :key="item.activityId"
              type="button"
              class="rank-row clickable"
              @click="emitNavigate({ type: 'activity', activityId: item.activityId, title: item.title })"
            >
              <div>
                <strong>{{ item.title || '未命名活动' }}</strong>
                <small>{{ formatCount(item.bookingCount) }} 个预订 / {{ formatCount(item.participantCount) }} 位参与者</small>
              </div>
              <em>{{ formatCurrency(item.revenue) }}</em>
            </button>
          </div>
          <div v-else class="empty-note">暂无活动数据。</div>
        </section>
      </div>

      <section class="panel-card">
        <div class="panel-head">
          <h4>最近评价</h4>
          <span>{{ recentReviews.length }} 项</span>
        </div>
        <div v-if="recentReviews.length" class="review-stack">
          <div
            v-for="item in recentReviews"
            :key="item.id"
            class="review-row"
          >
            <img :src="item.userAvatar || '/default-avatar.svg'" alt="评价者" class="review-avatar">
            <div class="review-copy">
              <div class="review-top">
                <strong>{{ item.userName || '匿名用户' }}</strong>
                <span>{{ item.targetName || '活动评价' }}</span>
              </div>
              <p>{{ item.content || '未提供文本评价。' }}</p>
            </div>
            <b>{{ formatScore(item.score) }}</b>
          </div>
        </div>
        <div v-else class="empty-note">暂无评价数据。</div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const BRAND_RED = '#c04851'
const BRAND_BLUE = '#1661ab'
const BRAND_GOLD = '#c58b2a'

const props = defineProps({
  stats: {
    type: Object,
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  },
  visible: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['navigate'])

const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null
let parentResizeObserver = null
let renderTimer = null
let pendingFrame = 0
let retryCount = 0

const summary = computed(() => props.stats?.summary || {})
const bookingStatus = computed(() => Array.isArray(props.stats?.bookingStatus) ? props.stats.bookingStatus : [])
const salesTrend = computed(() => Array.isArray(props.stats?.salesTrend) ? props.stats.salesTrend : [])
const topActivities = computed(() => Array.isArray(props.stats?.topActivities) ? props.stats.topActivities : [])
const recentReviews = computed(() => Array.isArray(props.stats?.recentReviews) ? props.stats.recentReviews : [])

const formatCount = (value) => Number(value || 0).toLocaleString('en-US')
const formatCurrency = (value) => `CNY ${(Number(value || 0) / 100).toFixed(2)}`
const formatCurrencyAxis = (value) => {
  const amount = Number(value || 0) / 100
  const precision = Math.abs(amount) >= 100 ? 0 : Math.abs(amount) >= 10 ? 1 : 2
  return `CNY ${amount.toFixed(precision)}`
}
const formatScore = (value) => (value === null || value === undefined || value === '' ? '--' : Number(value).toFixed(1))

const summaryCards = computed(() => [
  {
    key: 'activities',
    label: '活动',
    value: formatCount(summary.value.activityCount),
    note: '打开活动管理并查看所有已发布的活动。',
    target: { type: 'activities' }
  },
  {
    key: 'bookings',
    label: '预订',
    value: formatCount(summary.value.bookingCount),
    note: '打开预订管理并查看所有预订记录。',
    target: { type: 'bookings', status: 'all' }
  },
  {
    key: 'pending',
    label: '待签到',
    value: formatCount(summary.value.pendingCheckinCount),
    note: '查看等待现场验证的预订。',
    target: { type: 'bookings', status: 'registered' }
  },
  {
    key: 'checked',
    label: '已签到',
    value: formatCount(summary.value.checkedInCount),
    note: '查看已完成签到的预订。',
    target: { type: 'bookings', status: 'checked_in' }
  },
  {
    key: 'reviews',
    label: '评价',
    value: formatCount(summary.value.reviewCount),
    note: '监控客户在完成预订后留下的反馈。',
    target: { type: 'bookings', status: 'checked_in' }
  },
  {
    key: 'revenue',
    label: '预订收入',
    value: formatCurrency(summary.value.totalRevenue ?? summary.value.bookingRevenue),
    note: '跟踪有效活动预订产生的总收入。',
    target: { type: 'bookings', status: 'all' }
  }
])

const hasTrendData = computed(() => salesTrend.value.some((item) => Number(item?.bookingRevenue || 0) > 0 || Number(item?.bookingCount || 0) > 0))
const hasActivityChartData = computed(() => topActivities.value.length > 0)
const chartMode = computed(() => {
  if (hasTrendData.value) {
    return 'trend'
  }
  if (hasActivityChartData.value) {
    return 'activity'
  }
  return 'empty'
})

const chartMeta = computed(() => {
  if (chartMode.value === 'trend') {
    return {
      title: '最近7天预订趋势',
      description: '柱状图显示预订收入。折线图显示预订量。',
      linkLabel: '打开预订管理'
    }
  }
  if (chartMode.value === 'activity') {
    return {
      title: '活动表现快照',
      description: '通过预订量、参与人数和预订收入比较您的热门活动。',
      linkLabel: '打开活动管理'
    }
  }
  return {
    title: '商家分析概览',
    description: '创建活动或接收预订以解锁此处的可视化趋势。',
    linkLabel: '打开商家中心'
  }
})

const chartTarget = computed(() => {
  if (chartMode.value === 'activity') {
    return { type: 'activities' }
  }
  return { type: 'bookings', status: 'all' }
})

const activityChartData = computed(() => topActivities.value.slice(0, 5).map((item) => ({
  label: shortenLabel(item.title || 'Untitled Activity'),
  fullLabel: item.title || 'Untitled Activity',
  bookingCount: Number(item.bookingCount || 0),
  participantCount: Number(item.participantCount || 0),
  revenue: Number(item.revenue || 0)
})))

function shortenLabel(value) {
  const text = String(value || '').trim()
  if (!text) {
    return '未命名活动'
  }
  return text.length > 14 ? `${text.slice(0, 14)}...` : text
}

const emitNavigate = (target) => {
  if (!target) {
    return
  }
  emit('navigate', target)
}

const disposeChart = () => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

const cleanupChart = () => {
  if (renderTimer) {
    clearTimeout(renderTimer)
    renderTimer = null
  }
  if (pendingFrame) {
    cancelAnimationFrame(pendingFrame)
    pendingFrame = 0
  }
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (parentResizeObserver) {
    parentResizeObserver.disconnect()
    parentResizeObserver = null
  }
  disposeChart()
}

const ensureChart = () => {
  if (!chartRef.value) {
    return null
  }
  if (chartInstance?.getDom?.() !== chartRef.value) {
    disposeChart()
  }
  if (!chartInstance || chartInstance.isDisposed?.()) {
    chartInstance = echarts.init(chartRef.value, null, { renderer: 'canvas' })
  }
  return chartInstance
}

function buildTrendOption() {
  const xData = salesTrend.value.map((item) => item.label)
  const revenueData = salesTrend.value.map((item) => Number(item.bookingRevenue || 0))
  const bookingCountData = salesTrend.value.map((item) => Number(item.bookingCount || 0))

  return {
    backgroundColor: 'transparent',
    color: [BRAND_RED, BRAND_BLUE],
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params) => {
        const rows = [`${params?.[0]?.axisValueLabel || ''}`]
        params.forEach((item) => {
          if (item.seriesName === 'Revenue') {
            rows.push(`${item.marker}${item.seriesName}: ${formatCurrency(item.value)}`)
            return
          }
          rows.push(`${item.marker}${item.seriesName}: ${formatCount(item.value)}`)
        })
        return rows.join('<br>')
      }
    },
    grid: {
      left: 18,
      right: 18,
      top: 40,
      bottom: 18,
      containLabel: true
    },
    legend: {
      data: ['Revenue', 'Bookings'],
      top: 0,
      textStyle: {
        color: '#4b5563'
      }
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLine: {
        lineStyle: {
          color: '#d1d5db'
        }
      },
      axisLabel: {
        color: '#6b7280'
      }
    },
    yAxis: [
      {
        type: 'value',
        axisLabel: {
          color: '#6b7280',
          formatter: (value) => formatCurrencyAxis(value)
        },
        splitLine: {
          lineStyle: {
            color: 'rgba(209, 213, 219, 0.6)'
          }
        }
      },
      {
        type: 'value',
        axisLabel: {
          color: '#6b7280',
          formatter: (value) => formatCount(value)
        },
        splitLine: {
          show: false
        }
      }
    ],
    series: [
      {
        name: 'Revenue',
        type: 'bar',
        barWidth: 18,
        itemStyle: {
          color: BRAND_RED,
          borderRadius: [8, 8, 0, 0]
        },
        data: revenueData
      },
      {
        name: 'Bookings',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 8,
        itemStyle: {
          color: BRAND_BLUE
        },
        lineStyle: {
          width: 3,
          color: BRAND_BLUE
        },
        areaStyle: {
          color: 'rgba(22, 97, 171, 0.12)'
        },
        data: bookingCountData
      }
    ]
  }
}

function buildActivityOption() {
  const xData = activityChartData.value.map((item) => item.label)
  const bookingData = activityChartData.value.map((item) => item.bookingCount)
  const participantData = activityChartData.value.map((item) => item.participantCount)
  const revenueData = activityChartData.value.map((item) => item.revenue)

  return {
    backgroundColor: 'transparent',
    color: [BRAND_RED, BRAND_GOLD, BRAND_BLUE],
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params) => {
        const rows = [`${activityChartData.value[params?.[0]?.dataIndex || 0]?.fullLabel || params?.[0]?.axisValueLabel || ''}`]
        params.forEach((item) => {
          if (item.seriesName === 'Revenue') {
            rows.push(`${item.marker}${item.seriesName}: ${formatCurrency(item.value)}`)
            return
          }
          rows.push(`${item.marker}${item.seriesName}: ${formatCount(item.value)}`)
        })
        return rows.join('<br>')
      }
    },
    grid: {
      left: 18,
      right: 18,
      top: 46,
      bottom: 28,
      containLabel: true
    },
    legend: {
      data: ['Bookings', 'Participants', 'Revenue'],
      top: 0,
      textStyle: {
        color: '#4b5563'
      }
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLine: {
        lineStyle: {
          color: '#d1d5db'
        }
      },
      axisLabel: {
        color: '#6b7280',
        interval: 0,
        rotate: xData.length > 3 ? 18 : 0
      }
    },
    yAxis: [
      {
        type: 'value',
        axisLabel: {
          color: '#6b7280',
          formatter: (value) => formatCount(value)
        },
        splitLine: {
          lineStyle: {
            color: 'rgba(209, 213, 219, 0.6)'
          }
        }
      },
      {
        type: 'value',
        axisLabel: {
          color: '#6b7280',
          formatter: (value) => formatCurrencyAxis(value)
        },
        splitLine: {
          show: false
        }
      }
    ],
    series: [
      {
        name: 'Bookings',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: {
          color: BRAND_RED,
          borderRadius: [8, 8, 0, 0]
        },
        data: bookingData
      },
      {
        name: 'Participants',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: {
          color: BRAND_GOLD,
          borderRadius: [8, 8, 0, 0]
        },
        data: participantData
      },
      {
        name: 'Revenue',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 8,
        itemStyle: {
          color: BRAND_BLUE
        },
        lineStyle: {
          width: 3,
          color: BRAND_BLUE
        },
        areaStyle: {
          color: 'rgba(22, 97, 171, 0.10)'
        },
        data: revenueData
      }
    ]
  }
}

function buildEmptyOption() {
  return {
    backgroundColor: 'transparent',
    graphic: [
      {
        type: 'group',
        left: 'center',
        top: 'middle',
        children: [
          {
            type: 'text',
            top: -10,
            style: {
              text: '暂无图表数据',
              fill: '#64748b',
              fontSize: 18,
              fontWeight: 600,
              textAlign: 'center'
            }
          },
          {
            type: 'text',
            top: 22,
            style: {
              text: '发布活动或接收预订以解锁分析功能。',
              fill: '#94a3b8',
              fontSize: 14,
              textAlign: 'center'
            }
          }
        ]
      }
    ]
  }
}

const renderChart = async () => {
  if (props.loading || !props.visible) {
    return
  }

  await nextTick()
  if (!chartRef.value) {
    return
  }

  const width = chartRef.value.clientWidth
  const height = chartRef.value.clientHeight
  if (!width || !height) {
    if (retryCount < 8) {
      retryCount += 1
      scheduleRender(120)
    }
    return
  }

  retryCount = 0
  const instance = ensureChart()
  if (!instance) {
    return
  }

  let option = buildEmptyOption()
  if (chartMode.value === 'trend') {
    option = buildTrendOption()
  } else if (chartMode.value === 'activity') {
    option = buildActivityOption()
  }

  instance.setOption(option, true)
  instance.resize()
}

const scheduleRender = (delay = 16) => {
  if (renderTimer) {
    clearTimeout(renderTimer)
  }
  if (pendingFrame) {
    cancelAnimationFrame(pendingFrame)
    pendingFrame = 0
  }
  renderTimer = setTimeout(() => {
    pendingFrame = requestAnimationFrame(() => {
      pendingFrame = requestAnimationFrame(() => {
        renderChart()
      })
    })
  }, delay)
}

const handleResize = () => {
  scheduleRender(24)
}

const requestRender = async () => {
  await nextTick()
  scheduleRender(24)
  scheduleRender(120)
}

onMounted(async () => {
  await requestRender()
  window.addEventListener('resize', handleResize)

  if (window.ResizeObserver && chartRef.value) {
    resizeObserver = new window.ResizeObserver(() => {
      handleResize()
    })
    resizeObserver.observe(chartRef.value)

    const parentElement = chartRef.value.parentElement
    if (parentElement) {
      parentResizeObserver = new window.ResizeObserver(() => {
        handleResize()
      })
      parentResizeObserver.observe(parentElement)
    }
  }
})

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) {
      disposeChart()
      return
    }
    await requestRender()
  }
)

watch(
  () => props.loading,
  async (loading) => {
    if (loading) {
      disposeChart()
      return
    }
    await requestRender()
  }
)

watch(
  () => props.stats,
  () => {
    scheduleRender(24)
  },
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  cleanupChart()
})
</script>

<style scoped>
.stats-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stats-empty,
.empty-note {
  padding: 28px 18px;
  border-radius: 18px;
  background: #f8fafc;
  color: #64748b;
  text-align: center;
}

.stats-empty i {
  font-size: 32px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.stats-card,
.chart-card,
.panel-card {
  border-radius: 20px;
  border: 1px solid rgba(192, 72, 81, 0.12);
  background: rgba(255, 255, 255, 0.95);
}

.stats-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}

.stats-card span,
.panel-head span,
.chart-link {
  color: #7c8694;
  font-size: 13px;
}

.stats-card strong {
  color: #1f2937;
  font-size: 28px;
}

.stats-card small {
  color: #6b7280;
  line-height: 1.6;
}

.chart-card,
.panel-card {
  padding: 18px;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.05);
}

.chart-head,
.panel-head,
.review-top,
.rank-row,
.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-head h4,
.panel-head h4 {
  margin: 0;
  color: #1f2937;
  font-size: 16px;
}

.chart-head p {
  margin: 6px 0 0;
  color: #6b7280;
  line-height: 1.6;
}

.chart-link {
  font-weight: 600;
  color: #1661ab;
}

.chart-area {
  height: 320px;
  margin-top: 12px;
}

.split-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.status-stack,
.list-stack,
.review-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.status-row,
.rank-row,
.review-row {
  width: 100%;
  padding: 14px 16px;
  border-radius: 16px;
  background: #faf8f5;
  text-align: left;
}

.status-row strong,
.rank-row strong,
.review-row strong {
  color: #1f2937;
}

.status-row em,
.rank-row em,
.review-row b {
  color: #c04851;
  font-style: normal;
  font-weight: 700;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 10px;
  flex-shrink: 0;
}

.status-row strong {
  flex: 1;
}

.rank-row small,
.review-copy span,
.review-copy p {
  color: #6b7280;
}

.review-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: flex-start;
}

.review-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.review-copy {
  min-width: 0;
}

.review-top {
  gap: 12px;
}

.review-copy p {
  margin: 8px 0 0;
  line-height: 1.7;
}

.clickable {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

button.clickable {
  border: none;
}

.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.08);
  border-color: rgba(22, 97, 171, 0.2);
}

@media (max-width: 960px) {
  .stats-grid,
  .split-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .chart-area {
    height: 260px;
  }

  .review-row {
    grid-template-columns: 1fr;
  }
}
</style>
