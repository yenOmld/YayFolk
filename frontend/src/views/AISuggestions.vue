<template>
  <div class="ai-suggestions-page">
    <button class="floating-back-btn" @click="goBack" aria-label="Back to Data Analysis">
      <i class="bx bx-arrow-back"></i>
    </button>

    <section class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">智能分析</p>
        <h1>AI 建议</h1>
        <p>基于您店铺的真实数据，AI 提供个性化的运营建议和优化方案。</p>
      </div>
      <div class="hero-actions">
        <button class="refresh-btn" :disabled="loading" @click="generateSuggestions">
          {{ loading ? '生成中...' : '重新生成' }}
        </button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>智能分析</h2>
      </div>
      <div class="analysis-summary">
        <div class="summary-item">
          <div class="summary-icon" style="background: #e6f7ff; color: #1890ff;">
            <i class='bx bxs-bar-chart-alt-2'></i>
          </div>
          <div class="summary-content">
            <h3>运营状态</h3>
            <p>{{ analysis.operationStatus || '分析中...' }}</p>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon" style="background: #fff0f6; color: #ff6b81;">
            <i class='bx bxs-star'></i>
          </div>
          <div class="summary-content">
            <h3>客户满意度</h3>
            <p>{{ analysis.customerSatisfaction || '分析中...' }}</p>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon" style="background: #f0fdf4; color: #22c55e;">
            <i class='bx bxs-dollar-circle'></i>
          </div>
          <div class="summary-content">
            <h3>收入趋势</h3>
            <p>{{ analysis.revenueTrend || '分析中...' }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>AI 建议</h2>
      </div>
      <div class="suggestions-list">
        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>AI 正在分析您的真实数据...</p>
        </div>
        <div v-else-if="error" class="error-state">
          <i class="bx bx-error-circle"></i>
          <p>{{ error }}</p>
          <button class="retry-btn" @click="generateSuggestions">重试</button>
        </div>
        <div v-else-if="suggestions.length === 0" class="empty-state">
          <i class="bx bx-info-circle"></i>
          <p>暂无建议，请点击重新生成</p>
        </div>
        <div v-else class="suggestion-card" v-for="(suggestion, index) in suggestions" :key="index">
          <div class="suggestion-header">
            <div class="suggestion-icon" :style="{ background: suggestion.color?.bg || '#e6f7ff', color: suggestion.color?.text || '#1890ff' }">
              <i :class="suggestion.icon || 'bx bxs-lightbulb'" :title="suggestion.title"></i>
            </div>
            <h3>{{ suggestion.title }}</h3>
            <span class="suggestion-badge" :class="suggestion.priority || 'medium'">
              {{ (suggestion.priority === 'high' ? '高' : suggestion.priority === 'medium' ? '中' : '低') }}优先级
            </span>
          </div>
          <div class="suggestion-content">
            <p>{{ suggestion.description }}</p>
            <div class="suggestion-tips" v-if="suggestion.tips && suggestion.tips.length > 0">
              <h4>具体建议：</h4>
              <ul>
                <li v-for="(tip, tipIndex) in suggestion.tips" :key="tipIndex">{{ tip }}</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>数据概览</h2>
      </div>
      <div class="data-overview">
        <div class="data-item">
          <span class="data-label">活动数量</span>
          <span class="data-value">{{ dataOverview.activityCount || 0 }}</span>
        </div>
        <div class="data-item">
          <span class="data-label">订单数量</span>
          <span class="data-value">{{ dataOverview.orderCount || 0 }}</span>
        </div>
        <div class="data-item">
          <span class="data-label">评价数量</span>
          <span class="data-value">{{ dataOverview.reviewCount || 0 }}</span>
        </div>
        <div class="data-item">
          <span class="data-label">平均评分</span>
          <span class="data-value">{{ dataOverview.averageRating || 0 }}</span>
        </div>
        <div class="data-item">
          <span class="data-label">总收入</span>
          <span class="data-value">¥{{ dataOverview.totalRevenue || 0 }}</span>
        </div>
      </div>
    </section>

    <section class="panel" v-if="topActivities.length > 0">
      <div class="panel-header">
        <h2>热门活动 Top 5</h2>
      </div>
      <div class="activity-list">
        <div class="activity-card" v-for="activity in topActivities" :key="activity.id">
          <div class="activity-info">
            <h3>{{ activity.title || '未命名活动' }}</h3>
            <p class="activity-meta">
              <span><i class='bx bxs-user'></i> {{ activity.participantCount || 0 }} 参与人数</span>
              <span><i class='bx bxs-star'></i> {{ activity.averageRating || 0 }} 评分</span>
            </p>
            <p class="activity-revenue">收入：¥{{ activity.revenue || 0 }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="panel" v-if="recentReviews.length > 0">
      <div class="panel-header">
        <h2>最近评价</h2>
      </div>
      <div class="review-list">
        <div class="review-item" v-for="(review, index) in recentReviews" :key="index">
          <div class="review-score">
            <i class='bx bxs-star'></i>
            <span>{{ review.score }}</span>
          </div>
          <p class="review-content">{{ review.content || '无评价内容' }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMerchantActivities, getMerchantBookings, getMerchantStats, getMerchantReviewPosts } from '@/api/app'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const analysis = ref({
  operationStatus: '',
  customerSatisfaction: '',
  revenueTrend: ''
})
const suggestions = ref([])
const dataOverview = ref({
  activityCount: 0,
  orderCount: 0,
  reviewCount: 0,
  averageRating: 0,
  totalRevenue: 0
})
const topActivities = ref([])
const recentReviews = ref([])

const goBack = () => {
  router.push('/merchant/analysis')
}

const extractList = (data) => {
  if (Array.isArray(data)) return data
  if (data && typeof data === 'object') {
    return data.items || data.records || data.list || []
  }
  return []
}

const fetchMerchantData = async () => {
  try {
    const [activitiesRes, bookingsRes, statsRes, reviewsRes] = await Promise.allSettled([
      getMerchantActivities(),
      getMerchantBookings({ status: 'confirmed' }),
      getMerchantStats(),
      getMerchantReviewPosts()
    ])

    let activities = []
    if (activitiesRes.status === 'fulfilled' && activitiesRes.value?.code === 200) {
      activities = extractList(activitiesRes.value.data)
    }

    let bookings = []
    let totalRevenue = 0
    if (bookingsRes.status === 'fulfilled' && bookingsRes.value?.code === 200) {
      bookings = extractList(bookingsRes.value.data)
      if (Array.isArray(bookings)) {
        totalRevenue = bookings.reduce((sum, booking) => sum + (booking.totalAmount || 0), 0)
      }
    }

    let stats = {}
    if (statsRes.status === 'fulfilled' && statsRes.value?.code === 200) {
      stats = statsRes.value.data || {}
    }

    let reviews = []
    let reviewCount = 0
    let averageRating = 0
    if (reviewsRes.status === 'fulfilled' && reviewsRes.value?.code === 200) {
      reviews = extractList(reviewsRes.value.data)
      reviewCount = reviews.length
      if (reviewCount > 0) {
        const totalScore = reviews.reduce((sum, review) => sum + (review.score || 0), 0)
        averageRating = (totalScore / reviewCount).toFixed(1)
      }
    }

    const activityParticipantMap = {}
    if (Array.isArray(bookings)) {
      bookings.forEach(booking => {
        if (booking.activityId) {
          activityParticipantMap[booking.activityId] = (activityParticipantMap[booking.activityId] || 0) + 1
        }
      })
    }

    const activitiesWithStats = activities.map(activity => ({
      id: activity.id,
      title: activity.title,
      participantCount: activityParticipantMap[activity.id] || activity.participantCount || 0,
      averageRating: activity.averageRating || 0,
      revenue: activity.revenue || 0,
      status: activity.status,
      createTime: activity.createTime
    }))

    recentReviews.value = reviews.slice(0, 10).map(review => ({
      score: review.score || 0,
      content: review.content || review.title || ''
    }))

    topActivities.value = [...activitiesWithStats]
      .sort((a, b) => b.participantCount - a.participantCount)
      .slice(0, 5)

    dataOverview.value = {
      activityCount: activities.length,
      orderCount: bookings.length,
      reviewCount: reviewCount,
      averageRating: averageRating,
      totalRevenue: totalRevenue
    }

    return {
      activityCount: activities.length,
      orderCount: bookings.length,
      reviewCount: reviewCount,
      averageRating: averageRating,
      totalRevenue: totalRevenue,
      activities: activitiesWithStats,
      reviews: recentReviews.value,
      stats: stats
    }
  } catch (err) {
    console.error('Failed to fetch merchant data:', err)
    throw err
  }
}

const generateSuggestions = async () => {
  loading.value = true
  error.value = ''

  try {
    const merchantData = await fetchMerchantData()

    const systemPrompt = `你是一名专业的商业顾问，擅长分析商家运营数据并提供优化建议。
请基于用户提供的真实数据，返回JSON格式的分析和建议。
JSON必须包含analysis和suggestions字段，不要返回其他内容。
请使用中文返回所有内容。`

    const userPrompt = `请分析以下商家的真实数据并返回JSON格式的建议：

商家数据：
- 活动总数：${merchantData.activityCount}
- 订单总数：${merchantData.orderCount}
- 评价总数：${merchantData.reviewCount}
- 平均评分：${merchantData.averageRating}
- 总收入：${merchantData.totalRevenue}

热门活动：
${merchantData.activities.length > 0 ? merchantData.activities.slice(0, 5).map((a, i) => `${i + 1}. ${a.title} (${a.participantCount} 参与人数, 评分 ${a.averageRating}, 收入 ¥${a.revenue})`).join('\n') : '无活动数据'}

最近评价：
${merchantData.reviews.length > 0 ? merchantData.reviews.map(r => `- ${r.score} 星：${r.content}`).join('\n') : '无评价数据'}

请返回以下JSON格式（不要包含markdown代码块，直接返回纯JSON）：
{
  "analysis": {
    "operationStatus": "运营状态评估",
    "customerSatisfaction": "客户满意度评估",
    "revenueTrend": "收入趋势评估"
  },
  "suggestions": [
    {
      "title": "建议标题",
      "description": "详细描述",
      "tips": ["步骤1", "步骤2"],
      "priority": "high或medium或low",
      "icon": "bx图标类名",
      "color": {"bg": "#背景颜色", "text": "#文本颜色"}
    }
  ]
}`

    const response = await fetch('https://api.deepseek.com/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer sk-cddd84f4be02447ca29735575dbe6aad'
      },
      body: JSON.stringify({
        model: 'deepseek-chat',
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userPrompt }
        ],
        temperature: 0.5,
        max_tokens: 2500
      })
    })

    if (!response.ok) {
      const errText = await response.text()
      console.error('DeepSeek API Error:', response.status, errText)
      throw new Error(`API request failed: ${response.status}`)
    }

    const data = await response.json()
    const aiContent = data.choices?.[0]?.message?.content || ''

    let result = null
    try {
      let jsonStr = aiContent.trim()
      const jsonMatch = jsonStr.match(/\{[\s\S]*\}/)
      if (jsonMatch) {
        jsonStr = jsonMatch[0]
      }
      result = JSON.parse(jsonStr)
    } catch (parseErr) {
      console.error('JSON parse failed:', parseErr, 'Raw content:', aiContent)
      result = null
    }

    if (result && result.analysis && result.suggestions) {
      analysis.value = result.analysis
      suggestions.value = result.suggestions
    } else {
      analysis.value = generateDefaultAnalysis(merchantData)
      suggestions.value = generateDefaultSuggestions(merchantData)
    }

  } catch (err) {
    console.error('Failed to generate AI suggestions:', err)
    error.value = `Failed to generate suggestions: ${err.message}`

    try {
      const merchantData = await fetchMerchantData()
      analysis.value = generateDefaultAnalysis(merchantData)
      suggestions.value = generateDefaultSuggestions(merchantData)
      error.value = ''
    } catch (fetchErr) {
      console.error('Failed to fetch default data:', fetchErr)
    }
  } finally {
    loading.value = false
  }
}

const generateDefaultAnalysis = (data) => {
  const { activityCount, orderCount, averageRating, totalRevenue } = data

  let operationStatus = '一般'
  if (activityCount > 10 && orderCount > 100) {
    operationStatus = '优秀'
  } else if (activityCount > 5 && orderCount > 50) {
    operationStatus = '良好'
  }

  let customerSatisfaction = '一般'
  if (averageRating >= 4.5) {
    customerSatisfaction = '非常高'
  } else if (averageRating >= 4.0) {
    customerSatisfaction = '高'
  } else if (averageRating >= 3.0) {
    customerSatisfaction = '一般'
  } else if (averageRating > 0) {
    customerSatisfaction = '需要改进'
  }

  let revenueTrend = '一般'
  if (totalRevenue > 10000) {
    revenueTrend = '良好上升趋势'
  } else if (totalRevenue > 5000) {
    revenueTrend = '稳定增长'
  } else if (totalRevenue > 0) {
    revenueTrend = '有提升空间'
  } else {
    revenueTrend = '无收入数据'
  }

  return { operationStatus, customerSatisfaction, revenueTrend }
}

const generateDefaultSuggestions = (data) => {
  const suggestions = []

  if (data.activityCount === 0) {
    suggestions.push({
      title: '创建更多活动',
      description: '您还未创建任何活动。建议根据您的业务特点创建有吸引力的活动，以吸引用户参与。',
      tips: ['分析目标用户群体的兴趣和需求', '根据节假日、季节等时间节点策划活动', '设计互动性强的活动内容'],
      priority: 'high',
      icon: 'bx bxs-calendar-plus',
      color: { bg: '#e6f7ff', text: '#1890ff' }
    })
  }

  if (data.orderCount < 50 && data.activityCount > 0) {
    suggestions.push({
      title: '提高活动参与率',
      description: `您有 ${data.activityCount} 个活动，但只有 ${data.orderCount} 个订单，参与率较低。建议优化活动内容或加强推广。`,
      tips: ['优化活动描述和封面图片', '在社交媒体上加强活动推广', '设置早鸟价格或团体折扣', '增加用户互动环节'],
      priority: 'high',
      icon: 'bx bxs-trending-up',
      color: { bg: '#fff0f6', text: '#ff6b81' }
    })
  }

  if (data.averageRating > 0 && data.averageRating < 4.0) {
    suggestions.push({
      title: '提升服务质量',
      description: `您的平均评分为 ${data.averageRating}，有提升空间。建议关注用户反馈，不断改善服务体验。`,
      tips: ['认真对待每一条用户评价', '及时回应用户反馈', '总结常见问题并改进', '加强员工服务培训'],
      priority: 'high',
      icon: 'bx bxs-star',
      color: { bg: '#fef3c7', text: '#f59e0b' }
    })
  }

  if (data.totalRevenue === 0 && data.orderCount > 0) {
    suggestions.push({
      title: '优化定价策略',
      description: '您有订单但暂无收入，建议检查活动定价或支付流程。',
      tips: ['确认活动价格设置是否正确', '检查支付接口是否正常工作', '考虑是否设置免费活动引流'],
      priority: 'medium',
      icon: 'bx bxs-dollar-circle',
      color: { bg: '#f0fdf4', text: '#22c55e' }
    })
  }

  if (data.reviewCount === 0 && data.orderCount > 10) {
    suggestions.push({
      title: '鼓励用户评价',
      description: `您有 ${data.orderCount} 个订单但暂无评价，建议积极邀请用户提供反馈。`,
      tips: ['活动结束后发送评价邀请', '设置评价奖励机制', '简化评价流程', '对高质量评价给予积分奖励'],
      priority: 'medium',
      icon: 'bx bxs-message-dots',
      color: { bg: '#eef2ff', text: '#4f46e5' }
    })
  }

  if (suggestions.length === 0) {
    suggestions.push({
      title: '持续优化运营',
      description: '您的数据表现良好，建议保持并持续优化。',
      tips: ['定期分析活动数据', '关注用户反馈趋势', '尝试新的活动形式', '建立用户社区增强粘性'],
      priority: 'low',
      icon: 'bx bxs-rocket',
      color: { bg: '#e6f7ff', text: '#1890ff' }
    })
  }

  return suggestions
}

onMounted(() => {
  generateSuggestions()
})
</script>

<style scoped>
.ai-suggestions-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 76px 20px 80px;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.hero-card {
  padding: 28px;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  background: linear-gradient(135deg, #fff, #f8fafc);
}

.floating-back-btn {
  position: absolute;
  top: 28px;
  left: 20px;
  width: 46px;
  height: 46px;
  border: none;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: #9d2929;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.14);
  border: 1px solid #f3d7c7;
  cursor: pointer;
  z-index: 2;
}

.floating-back-btn:hover {
  transform: translateY(-1px);
  background: #fff7f2;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.eyebrow {
  margin: 0 0 8px;
  color: #9d2929;
  letter-spacing: 0.12em;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0 0 10px;
  font-size: 30px;
  color: #111827;
}

.hero-copy p {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.refresh-btn {
  padding: 12px 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, #c04851, #e18b32);
  color: #fff;
  font-weight: 700;
  border: none;
  cursor: pointer;
  min-height: 42px;
}

.refresh-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.panel {
  padding: 24px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.panel-header h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
}

.analysis-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.summary-content h3 {
  margin: 0 0 4px;
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.summary-content p {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #6b7280;
  gap: 12px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #c04851;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-state i {
  font-size: 48px;
  color: #ef4444;
}

.error-state p {
  margin: 0;
  text-align: center;
}

.retry-btn {
  padding: 8px 16px;
  border-radius: 999px;
  background: #c04851;
  color: #fff;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.empty-state i {
  font-size: 48px;
  color: #9d2929;
}

.empty-state p {
  margin: 0;
}

.suggestion-card {
  padding: 20px;
  border-radius: 18px;
  background: #fbfbfb;
  border: 1px solid #ece7e2;
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.suggestion-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.suggestion-header h3 {
  flex: 1;
  margin: 0;
  font-size: 16px;
  color: #1f2937;
}

.suggestion-badge {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.suggestion-badge.high {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.suggestion-badge.medium {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.suggestion-badge.low {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.suggestion-content p {
  margin: 0 0 12px;
  color: #6b7280;
  line-height: 1.6;
}

.suggestion-tips h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #1f2937;
  font-weight: 600;
}

.suggestion-tips ul {
  margin: 0;
  padding-left: 20px;
}

.suggestion-tips li {
  margin-bottom: 4px;
  color: #6b7280;
  line-height: 1.4;
}

.data-overview {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.data-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.data-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.data-value {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-card {
  display: flex;
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.activity-info {
  flex: 1;
  min-width: 0;
}

.activity-info h3 {
  margin: 0 0 8px;
  color: #1f2937;
  font-size: 16px;
}

.activity-meta {
  margin: 0 0 8px;
  color: #6b7280;
  font-size: 14px;
}

.activity-meta span {
  margin-right: 16px;
}

.activity-meta i {
  margin-right: 4px;
}

.activity-revenue {
  margin: 0;
  color: #9d2929;
  font-weight: 600;
  font-size: 14px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.review-score {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #f59e0b;
  font-weight: 600;
  min-width: 50px;
}

.review-score i {
  font-size: 16px;
}

.review-content {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
}

@media (max-width: 900px) {
  .analysis-summary {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }

  .data-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .ai-suggestions-page {
    padding: 66px 14px 70px;
  }

  .hero-card {
    padding: 22px;
    flex-direction: column;
  }

  .data-overview {
    grid-template-columns: 1fr;
  }
}
</style>