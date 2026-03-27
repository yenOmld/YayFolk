<template>
  <div class="ai-route-page">
    <section class="hero">
      <div>
        <h1>AI 非遗路线规划</h1>
        <p>输入你的城市、出行天数和兴趣偏好，系统会结合官方内容、活动与商家数据生成可执行路线。</p>
      </div>
    </section>

    <section class="planner-card">
      <label for="route-input">输入你的需求</label>
      <textarea
        id="route-input"
        v-model="userInput"
        rows="4"
        placeholder="例如：我想去杭州玩2天，喜欢传统工艺、茶文化，想安排可以报名的线下体验"
      />
      <div class="prompt-row">
        <button v-for="prompt in prompts" :key="prompt" class="prompt-chip" @click="userInput = prompt">{{ prompt }}</button>
      </div>
      <p v-if="error" class="error-message">{{ error }}</p>
      <button class="generate-btn" @click="generateRoute" :disabled="loading">
        {{ loading ? '生成中...' : '生成路线' }}
      </button>
    </section>

    <section v-if="routeResult" class="result-section">
      <div class="overview-card">
        <h2>{{ routeResult.destination }} · {{ routeResult.days }}天路线</h2>
        <p>{{ routeResult.summary }}</p>
        <div class="tag-row">
          <span v-for="preference in routeResult.preferences" :key="preference" class="tag">{{ preference }}</span>
        </div>
      </div>

      <div class="layout">
        <div class="main-column">
          <article v-for="day in routeResult.itinerary" :key="day.day" class="day-card">
            <div class="day-head">
              <div>
                <h3>第 {{ day.day }} 天</h3>
                <p>{{ day.theme }}</p>
              </div>
            </div>
            <div class="timeline">
              <div v-for="activity in day.activities" :key="`${day.day}-${activity.time}-${activity.name}`" class="timeline-item">
                <div class="time">{{ activity.time }}</div>
                <div class="content">
                  <h4>{{ activity.name }}</h4>
                  <p>{{ activity.description }}</p>
                  <div v-if="activity.details?.length" class="detail-list">
                    <span v-for="detail in activity.details" :key="`${activity.name}-${detail.label}`">
                      {{ detail.label }}：{{ detail.value }}
                    </span>
                  </div>
                  <div v-if="activity.business" class="business-box">
                    <strong>{{ activity.business.name }}</strong>
                    <span>{{ activity.business.address }}</span>
                    <span>{{ activity.business.phone }}</span>
                  </div>
                  <div class="tag-row">
                    <span v-for="tag in activity.tags || []" :key="`${activity.name}-${tag}`" class="tag">{{ tag }}</span>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </div>

        <aside class="side-column">
          <div class="side-card" v-if="routeResult.recommendedBusinesses?.length">
            <h3>推荐商家</h3>
            <article v-for="business in routeResult.recommendedBusinesses" :key="business.name" class="side-item">
              <h4>{{ business.name }}</h4>
              <p>{{ business.description }}</p>
              <span>{{ business.address }}</span>
              <div class="tag-row">
                <span v-for="tag in business.tags || []" :key="`${business.name}-${tag}`" class="tag">{{ tag }}</span>
              </div>
            </article>
          </div>

          <div class="side-card" v-if="routeResult.activities?.length">
            <h3>相关活动</h3>
            <article v-for="activity in routeResult.activities" :key="activity.id || activity.title" class="side-item">
              <h4>{{ activity.title }}</h4>
              <p>{{ activity.description }}</p>
              <span>{{ activity.date }}</span>
              <span>{{ activity.location }}</span>
            </article>
          </div>

          <div class="side-card" v-if="routeResult.officialHighlights?.length">
            <h3>官方导读</h3>
            <article v-for="item in routeResult.officialHighlights" :key="item.title" class="side-item">
              <h4>{{ item.title }}</h4>
              <p>{{ item.summary }}</p>
              <span>{{ item.category }}</span>
            </article>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { generateHeritageRoute } from '../api/app'

const userInput = ref('')
const routeResult = ref(null)
const loading = ref(false)
const error = ref('')

const prompts = [
  '我想去北京玩3天，重点看戏曲和传统工艺',
  '我想在杭州安排2天茶文化和手作体验',
  '我想找适合亲子体验的非遗活动'
]

const generateRoute = async () => {
  if (!userInput.value.trim()) {
    error.value = '请输入你的需求'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const response = await generateHeritageRoute({
      userInput: userInput.value
    })

    routeResult.value = response.data
  } catch (err) {
    error.value = err.response?.data?.message || '生成路线失败，请稍后重试'
    console.error('路线生成失败:', err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-route-page { min-height: 100vh; background: #f5f7fa; padding: 20px 20px 90px; }
.hero { max-width: 1200px; margin: 0 auto 20px; padding: 28px; border-radius: 18px; background: linear-gradient(135deg, #7c2d12, #ea580c); color: #fff; }
.hero h1 { margin: 0 0 8px; font-size: 30px; }
.hero p { margin: 0; max-width: 720px; line-height: 1.7; color: rgba(255,255,255,0.88); }
.planner-card, .overview-card, .day-card, .side-card { background: #fff; border-radius: 16px; border: 1px solid #e5e7eb; }
.planner-card { max-width: 1200px; margin: 0 auto 20px; padding: 24px; }
.planner-card label { display: block; font-size: 15px; font-weight: 600; margin-bottom: 10px; color: #111827; }
.planner-card textarea { width: 100%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 12px; padding: 14px; font-size: 15px; resize: vertical; font-family: inherit; }
.prompt-row, .tag-row { display: flex; flex-wrap: wrap; gap: 8px; }
.prompt-row { margin-top: 14px; }
.prompt-chip { border: 1px solid #fed7aa; background: #fff7ed; color: #c2410c; padding: 6px 12px; border-radius: 999px; cursor: pointer; font-size: 13px; }
.generate-btn { margin-top: 16px; width: 100%; padding: 12px 18px; border: none; border-radius: 12px; background: #ea580c; color: #fff; cursor: pointer; font-size: 15px; font-weight: 600; }
.error-message { margin-top: 12px; color: #dc2626; }
.result-section { max-width: 1200px; margin: 0 auto; }
.overview-card { padding: 24px; margin-bottom: 20px; }
.overview-card h2 { margin: 0 0 10px; font-size: 24px; color: #111827; }
.overview-card p { margin: 0 0 14px; color: #4b5563; line-height: 1.7; }
.tag { padding: 4px 10px; border-radius: 999px; background: #fff7ed; color: #c2410c; font-size: 12px; font-weight: 500; }
.layout { display: grid; grid-template-columns: 1.4fr 0.9fr; gap: 20px; }
.main-column, .side-column { display: flex; flex-direction: column; gap: 16px; }
.day-card, .side-card { padding: 20px; }
.day-head h3, .side-card h3 { margin: 0 0 6px; color: #111827; }
.day-head p { margin: 0 0 14px; color: #6b7280; }
.timeline { display: flex; flex-direction: column; gap: 16px; }
.timeline-item { display: grid; grid-template-columns: 90px 1fr; gap: 14px; }
.time { font-size: 13px; font-weight: 600; color: #c2410c; padding-top: 2px; }
.content h4, .side-item h4 { margin: 0 0 8px; color: #111827; }
.content p, .side-item p { margin: 0 0 10px; color: #4b5563; line-height: 1.7; }
.detail-list { display: flex; flex-wrap: wrap; gap: 8px 12px; margin-bottom: 10px; font-size: 13px; color: #6b7280; }
.business-box { display: flex; flex-direction: column; gap: 4px; padding: 12px; border-radius: 12px; background: #fff7ed; margin-bottom: 10px; font-size: 13px; color: #7c2d12; }
.side-item { padding: 14px 0; border-bottom: 1px solid #f3f4f6; display: flex; flex-direction: column; gap: 6px; }
.side-item:last-child { border-bottom: none; padding-bottom: 0; }
.side-item span { font-size: 13px; color: #9ca3af; }
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .timeline-item { grid-template-columns: 1fr; }
}
</style>
