<template>
  <div class="stats-panel">
    <div v-if="loading" class="stats-empty">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>Loading merchant analytics...</p>
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

      <section class="chart-card clickable" @click="emitNavigate({ type: 'bookings', status: 'all' })">
        <div class="chart-head">
          <div>
            <h4>Last 7 Days Booking Trend</h4>
            <p>Bars show booking revenue. The line shows booking volume.</p>
          </div>
          <span class="chart-link">Open booking management</span>
        </div>
        <div ref="chartRef" class="chart-area"></div>
      </section>

      <div class="split-grid">
        <section class="panel-card">
          <div class="panel-head">
            <h4>Booking Status</h4>
            <span>{{ bookingStatus.length }} items</span>
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
            <h4>Top Activities</h4>
            <span>{{ topActivities.length }} items</span>
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
                <strong>{{ item.title || 'Untitled Activity' }}</strong>
                <small>{{ formatCount(item.bookingCount) }} bookings / {{ formatCount(item.participantCount) }} participants</small>
              </div>
              <em>{{ formatCurrency(item.revenue) }}</em>
            </button>
          </div>
          <div v-else class="empty-note">No activity data yet.</div>
        </section>
      </div>

      <section class="panel-card">
        <div class="panel-head">
          <h4>Recent Reviews</h4>
          <span>{{ recentReviews.length }} items</span>
        </div>
        <div v-if="recentReviews.length" class="review-stack">
          <div
            v-for="item in recentReviews"
            :key="item.id"
            class="review-row"
          >
            <img :src="item.userAvatar || '/default-avatar.svg'" alt="reviewer" class="review-avatar">
            <div class="review-copy">
              <div class="review-top">
                <strong>{{ item.userName || 'Anonymous User' }}</strong>
                <span>{{ item.targetName || 'Activity Review' }}</span>
              </div>
              <p>{{ item.content || 'No text review was provided.' }}</p>
            </div>
            <b>{{ formatScore(item.score) }}</b>
          </div>
        </div>
        <div v-else class="empty-note">No review data yet.</div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const BRAND_RED = '#c04851'
const BRAND_BLUE = '#1661ab'

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
let renderTimer = null

const summary = computed(() => props.stats?.summary || {})
const bookingStatus = computed(() => Array.isArray(props.stats?.bookingStatus) ? props.stats.bookingStatus : [])
const salesTrend = computed(() => Array.isArray(props.stats?.salesTrend) ? props.stats.salesTrend : [])
const topActivities = computed(() => Array.isArray(props.stats?.topActivities) ? props.stats.topActivities : [])
const recentReviews = computed(() => Array.isArray(props.stats?.recentReviews) ? props.stats.recentReviews : [])

const formatCount = (value) => Number(value || 0).toLocaleString('en-US')
const formatCurrency = (value) => `CNY ${(Number(value || 0) / 100).toFixed(2)}`
const formatScore = (value) => (value === null || value === undefined || value === '' ? '--' : Number(value).toFixed(1))

const summaryCards = computed(() => [
  {
    key: 'activities',
    label: 'Activities',
    value: formatCount(summary.value.activityCount),
    note: 'Open activity management and review all published activities.',
    target: { type: 'activities' }
  },
  {
    key: 'bookings',
    label: 'Bookings',
    value: formatCount(summary.value.bookingCount),
    note: 'Open booking management and review all booking records.',
    target: { type: 'bookings', status: 'all' }
  },
  {
    key: 'pending',
    label: 'Pending Check-in',
    value: formatCount(summary.value.pendingCheckinCount),
    note: 'Check bookings that are waiting for on-site verification.',
    target: { type: 'bookings', status: 'registered' }
  },
  {
    key: 'checked',
    label: 'Checked In',
    value: formatCount(summary.value.checkedInCount),
    note: 'Review bookings that have already completed check-in.',
    target: { type: 'bookings', status: 'checked_in' }
  },
  {
    key: 'reviews',
    label: 'Reviews',
    value: formatCount(summary.value.reviewCount),
    note: 'Monitor feedback left by customers after completed bookings.',
    target: { type: 'bookings', status: 'checked_in' }
  },
  {
    key: 'revenue',
    label: 'Booking Revenue',
    value: formatCurrency(summary.value.totalRevenue ?? summary.value.bookingRevenue),
    note: 'Track the total revenue generated from valid activity bookings.',
    target: { type: 'bookings', status: 'all' }
  }
])

const emitNavigate = (target) => {
  if (!target) {
    return
  }
  emit('navigate', target)
}

const cleanupChart = () => {
  if (renderTimer) {
    clearTimeout(renderTimer)
    renderTimer = null
  }
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

const ensureChart = () => {
  if (!chartRef.value) {
    return null
  }
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  return chartInstance
}

const renderChart = async () => {
  if (props.loading || !props.visible) {
    return
  }

  await nextTick()
  if (!chartRef.value) {
    return
  }

  const instance = ensureChart()
  if (!instance) {
    return
  }

  const xData = salesTrend.value.map((item) => item.label)
  const revenueData = salesTrend.value.map((item) => Number(item.bookingRevenue || 0))
  const bookingCountData = salesTrend.value.map((item) => Number(item.bookingCount || 0))
  const hasData = revenueData.some((item) => item > 0) || bookingCountData.some((item) => item > 0)

  instance.setOption({
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
          formatter: (value) => `CNY ${(Number(value || 0) / 100).toFixed(0)}`
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
    graphic: hasData
      ? []
      : [
          {
            type: 'text',
            left: 'center',
            top: 'middle',
            style: {
              text: 'No trend data yet',
              fill: '#94a3b8',
              fontSize: 16
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
  }, true)

  instance.resize()
}

const scheduleRender = () => {
  if (renderTimer) {
    clearTimeout(renderTimer)
  }
  renderTimer = setTimeout(() => {
    renderChart()
  }, 16)
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  scheduleRender()
  window.addEventListener('resize', handleResize)

  if (window.ResizeObserver && chartRef.value) {
    resizeObserver = new window.ResizeObserver(() => {
      handleResize()
    })
    resizeObserver.observe(chartRef.value)
  }
})

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) {
      return
    }
    await nextTick()
    if (window.ResizeObserver && chartRef.value && !resizeObserver) {
      resizeObserver = new window.ResizeObserver(() => {
        handleResize()
      })
      resizeObserver.observe(chartRef.value)
    }
    scheduleRender()
  }
)

watch(
  () => props.loading,
  () => {
    scheduleRender()
  }
)

watch(
  () => props.stats,
  () => {
    scheduleRender()
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