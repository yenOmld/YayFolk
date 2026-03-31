<template>
  <div class="ai-route-page tools-page">
    <section class="hero-panel">
      <div class="hero-copy">
        <p class="eyebrow">AI 文化遗产规划</p>
        <h1>AI 文化遗产路线规划</h1>
        <p class="hero-text">
          描述您的目的地、行程安排和兴趣爱好。规划助手将结合文化遗产主题、可预订的活动和编辑内容，
          为您生成一条更易理解、更易跟进的旅行路线。
        </p>
        <div class="hero-meta">
          <span>城市识别</span>
          <span>活动推荐</span>
          <span>商家导览</span>
          <span>路线收藏</span>
        </div>
      </div>

      <div class="hero-card">
        <h2>描述您的路线</h2>
        <label class="input-label" for="route-input">请包含目的地、天数和兴趣爱好</label>
        <textarea
          id="route-input"
          v-model.trim="userInput"
          rows="5"
          :disabled="loading"
          placeholder="例如：我想去北京3天旅行，重点是戏曲和传统手工艺，优先安排可预订的线下体验活动。"
        />

        <div class="prompt-grid">
          <button
            v-for="prompt in prompts"
            :key="prompt"
            type="button"
            class="prompt-chip"
            :disabled="loading"
            @click="fillPrompt(prompt)"
          >
            {{ prompt }}
          </button>
        </div>

        <p v-if="error" class="error-message">{{ error }}</p>
        <p v-if="favoriteMessage" class="success-message">{{ favoriteMessage }}</p>

        <div class="action-row">
          <button type="button" class="generate-btn" :disabled="loading" @click="generateRoute">
            {{ loading ? '生成中...' : '生成路线' }}
          </button>
          <button
            type="button"
            class="ghost-btn"
            :disabled="loadingFavorites"
            @click="loadFavorites"
          >
            {{ loadingFavorites ? '刷新中...' : '刷新收藏' }}
          </button>
        </div>
      </div>
    </section>

    <section class="favorites-panel">
      <div class="favorites-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">已保存路线</p>
            <h2>我的收藏路线</h2>
          </div>
          <span class="count-badge">{{ favorites.length }} 条收藏</span>
        </div>

        <div v-if="loadingFavorites" class="favorites-empty">
          <p>正在加载已保存的路线...</p>
        </div>
        <div v-else-if="!favorites.length" class="favorites-empty">
          <p>暂无已保存的路线。请先生成一条路线，然后保存以便快速访问。</p>
        </div>
        <div v-else class="favorites-list">
          <article
            v-for="item in favorites"
            :key="item.id"
            :class="['favorite-item', { active: activeFavoriteId === item.id }]"
          >
            <button type="button" class="favorite-main" @click="openFavorite(item.id)">
              <div class="favorite-meta">
                <strong>{{ item.destination || '全国' }} - {{ item.days || 1 }} 天<span v-if="(item.days || 1) > 1"></span></strong>
                <span>{{ formatDate(item.updateTime || item.createTime) }}</span>
              </div>
              <h3>{{ item.query }}</h3>
              <p>{{ item.summary || '已保存的AI路线，可随时重新打开。' }}</p>
            </button>
            <button type="button" class="delete-btn" @click="removeFavorite(item.id)">
              删除
            </button>
          </article>
        </div>
      </div>
    </section>

    <section v-if="loading" class="loading-panel">
      <div class="loading-card">
        <div class="pulse-line wide"></div>
        <div class="pulse-line"></div>
        <div class="pulse-line short"></div>
      </div>
      <div class="loading-card">
        <div class="pulse-line"></div>
        <div class="pulse-line wide"></div>
        <div class="pulse-line short"></div>
      </div>
    </section>

    <section v-else-if="normalizedRoute" class="result-section">
      <div class="overview-card">
        <div class="overview-head">
          <div>
            <p class="eyebrow">路线概览</p>
            <h2>{{ normalizedRoute.destination }} - {{ normalizedRoute.days }} 天<span v-if="normalizedRoute.days > 1"></span></h2>
          </div>
          <div class="overview-actions">
            <div class="overview-badges">
              <span>{{ normalizedRoute.itinerary.length }} 天行程</span>
              <span>{{ normalizedRoute.activities.length }} 个活动</span>
              <span>{{ normalizedRoute.officialHighlights.length }} 条文化解读</span>
            </div>
            <button
              type="button"
              class="save-btn"
              :disabled="savingFavorite"
              @click="saveRoute"
            >
              {{ savingFavorite ? '保存中...' : '保存路线' }}
            </button>
          </div>
        </div>

        <p class="overview-summary">{{ normalizedRoute.summary }}</p>

        <div class="tag-row">
          <span
            v-for="preference in normalizedRoute.preferences"
            :key="preference"
            class="tag"
          >
            {{ preference }}
          </span>
        </div>
      </div>

      <div class="content-grid">
        <div class="main-column">
          <article
            v-for="day in normalizedRoute.itinerary"
            :key="day.day"
            class="day-card"
          >
            <div class="day-header">
              <div>
                <p class="day-index">第 {{ day.day }} 天</p>
                <h3>{{ day.theme || ('第 ' + day.day + ' 天') }}</h3>
              </div>
            </div>

            <div class="timeline">
              <div
                v-for="activity in day.activities"
                :key="`${day.day}-${activity.time}-${activity.name}`"
                :class="['timeline-item', `timeline-item--${activity.type}`]"
              >
                <div class="time-pill">{{ activity.time || '全天' }}</div>
                <div :class="['timeline-body', `timeline-body--${activity.type}`]">
                  <div class="timeline-head">
                    <span :class="['entry-kind', `entry-kind--${activity.type}`]">
                      {{ activity.typeLabel }}
                    </span>
                    <span v-if="activity.typeHint" class="entry-hint">
                      {{ activity.typeHint }}
                    </span>
                  </div>
                  <h4>{{ activity.name }}</h4>
                  <p>{{ activity.description }}</p>

                  <div v-if="activity.details.length" class="detail-list">
                    <span
                      v-for="detail in activity.details"
                      :key="`${activity.name}-${detail.label}`"
                      class="detail-item"
                    >
                      {{ detail.label }}: {{ detail.value }}
                    </span>
                  </div>

                  <div v-if="activity.business" class="business-box">
                    <strong>{{ activity.business.name }}</strong>
                    <span>{{ activity.business.address }}</span>
                    <span>{{ activity.business.phone }}</span>
                  </div>

                  <div class="tag-row compact">
                    <span
                      v-for="tag in activity.tags"
                      :key="`${activity.name}-${tag}`"
                      class="tag"
                    >
                      {{ tag }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </div>

        <aside class="side-column">
          <section
            v-if="normalizedRoute.officialHighlights.length"
            class="side-card"
          >
            <div class="side-header">
              <p class="eyebrow">文化遗产背景</p>
              <h3>相关文化遗产介绍</h3>
            </div>

            <article
              v-for="item in normalizedRoute.officialHighlights"
              :key="`${item.title}-${item.category}`"
              class="side-item"
            >
              <h4>{{ item.title }}</h4>
              <p>{{ item.summary }}</p>
              <span>{{ item.category }}</span>
              <span>{{ item.relatedType }}</span>
              <span>{{ item.region }}</span>
            </article>
          </section>
        </aside>
      </div>
    </section>

    <section v-else class="empty-panel">
      <div class="empty-card">
        <p class="eyebrow">随时准备出发</p>
        <h2>暂无路线</h2>
        <p>在上方输入您的旅行计划，规划助手将为您生成可查看和收藏的路线。</p>
      </div>
    </section>
  </div>
</template><script setup>
import { computed, onMounted, ref } from 'vue'
import {
  deleteSavedHeritageRoute,
  generateHeritageRoute,
  getSavedHeritageRouteDetail,
  getSavedHeritageRoutes,
  saveHeritageRoute
} from '../api/app'

const userInput = ref('')
const routeResult = ref(null)
const loading = ref(false)
const loadingFavorites = ref(false)
const savingFavorite = ref(false)
const error = ref('')
const favoriteMessage = ref('')
const favorites = ref([])
const activeFavoriteId = ref(null)

const prompts = [
  '规划一条3天的北京旅行，重点是戏曲、传统手工艺，优先安排可预订的线下体验活动。',
  '设计一条轻松的2天杭州路线，围绕茶文化和手工体验工作坊。',
  '推荐适合家庭参与的周末非遗活动，简单易上手。'
]

const normalizedRoute = computed(() => {
  const raw = routeResult.value
  if (!raw || typeof raw !== 'object') {
    return null
  }

  return {
    id: raw.id ?? null,
    query: raw.query || userInput.value,
    destination: raw.destination || '全国',
    days: Number(raw.days) || 1,
    summary: raw.summary || '根据您的偏好生成。',
    preferences: Array.isArray(raw.preferences) && raw.preferences.length
      ? raw.preferences
      : ['文化遗产体验'],
    itinerary: normalizeItinerary(raw.itinerary),
    activities: normalizeActivityItems(raw.activities),
    officialHighlights: normalizeHighlights(raw.officialHighlights, raw.activities),
    matchedCount: Number(raw.matchedCount) || 0
  }
})

function normalizeItinerary(itinerary) {
  if (!Array.isArray(itinerary)) {
    return []
  }

  return itinerary.map((day, index) => ({
    day: Number(day?.day) || index + 1,
    theme: day?.theme || `第 ${index + 1} 天`,
    activities: Array.isArray(day?.activities) && day.activities.length
      ? day.activities.map(activity => {
        const type = resolveItineraryItemType(activity)
        return {
          type,
          typeLabel: itineraryTypeLabel(type),
          typeHint: itineraryTypeHint(type),
          time: activity?.time || '全天',
          name: activity?.name || '未命名站点',
          description: activity?.description || '暂无描述。',
          tags: Array.isArray(activity?.tags) ? activity.tags : [],
          details: Array.isArray(activity?.details) ? activity.details : [],
          business: activity?.business || null
        }
      })
      : [{
        type: 'fallback',
        typeLabel: itineraryTypeLabel('fallback'),
        typeHint: itineraryTypeHint('fallback'),
        time: '全天',
        name: '自由探索',
        description: '当天未找到更匹配的安排，此时间段开放自由探索。',
        tags: ['灵活安排'],
        details: [],
        business: null
      }]
  }))
}

function resolveItineraryItemType(activity) {
  const explicitType = typeof activity?.type === 'string' ? activity.type.trim().toLowerCase() : ''
  if (explicitType) {
    return explicitType
  }

  if (activity?.business) {
    return 'activity'
  }

  const tags = (Array.isArray(activity?.tags) ? activity.tags : [])
    .filter(tag => typeof tag === 'string')
    .map(tag => tag.toLowerCase())

  if (tags.some(tag => tag.includes('official'))) {
    return 'official'
  }
  if (tags.some(tag => tag.includes('free') || tag.includes('flex'))) {
    return 'fallback'
  }
  return 'heritage'
}

function itineraryTypeLabel(type) {
  if (type === 'official') {
    return '编辑解读'
  }
  if (type === 'heritage') {
    return '文化遗产主题'
  }
  if (type === 'fallback') {
    return '灵活时段'
  }
  return '活动'
}

function itineraryTypeHint(type) {
  if (type === 'official') {
    return '路线背景介绍，非可预订活动。'
  }
  if (type === 'heritage') {
    return '这是文化遗产主题或项目，非活动名称。'
  }
  if (type === 'fallback') {
    return '当日无更匹配的安排时，可作为灵活时段使用。'
  }
  return '这是路线推荐体验活动。'
}

function normalizeActivityItems(items) {
  if (!Array.isArray(items)) {
    return []
  }

  return items.map(item => ({
    id: item?.id ?? null,
    title: item?.title || '未命名活动',
    description: item?.description || '暂无详情。',
    date: item?.date || '时间待定',
    location: item?.location || '地点待定'
  }))
}

function normalizeHighlights(items, activityItems) {
  if (!Array.isArray(items)) {
    return []
  }

  const activityTitles = new Set(
    (Array.isArray(activityItems) ? activityItems : [])
      .map(item => typeof item?.title === 'string' ? item.title.trim() : '')
      .filter(Boolean)
  )

  return items
    .map(item => ({
      title: item?.title || '文化遗产介绍',
      summary: item?.summary || '暂无简介。',
      category: item?.category || '文化遗产概况',
      relatedType: item?.relatedType || '相关介绍',
      region: item?.region || '地区待定'
    }))
    .filter(item => !activityTitles.has(item.title))
}

function fillPrompt(prompt) {
  userInput.value = prompt
  error.value = ''
  favoriteMessage.value = ''
}

async function generateRoute() {
  if (!userInput.value) {
    error.value = '请先输入您的路线需求。'
    return
  }

  loading.value = true
  error.value = ''
  favoriteMessage.value = ''

  try {
    const response = await generateHeritageRoute({
      userInput: userInput.value
    })

    if (response.code !== 200) {
      throw new Error(response.message || '路线生成失败。')
    }

    routeResult.value = response.data
    activeFavoriteId.value = null
  } catch (err) {
    error.value = err?.response?.data?.message || err?.message || '路线生成失败，请稍后重试。'
    routeResult.value = null
  } finally {
    loading.value = false
  }
}

async function saveRoute() {
  if (!normalizedRoute.value) {
    error.value = '请先生成路线再保存。'
    return
  }

  savingFavorite.value = true
  error.value = ''
  favoriteMessage.value = ''

  try {
    const response = await saveHeritageRoute({
      query: userInput.value || normalizedRoute.value.query,
      route: normalizedRoute.value
    })

    if (response.code !== 200) {
      throw new Error(response.message || '保存路线失败。')
    }

    routeResult.value = response.data
    activeFavoriteId.value = response.data?.id ?? null
    favoriteMessage.value = '路线已保存。您可以随时从下方列表重新打开。'
    await loadFavorites()
  } catch (err) {
    error.value = err?.response?.data?.message || err?.message || '保存路线失败，请稍后重试。'
  } finally {
    savingFavorite.value = false
  }
}

async function loadFavorites() {
  loadingFavorites.value = true

  try {
    const response = await getSavedHeritageRoutes()
    if (response.code !== 200) {
      throw new Error(response.message || '加载已保存路线失败。')
    }
    favorites.value = Array.isArray(response.data) ? response.data : []
  } catch (err) {
    favorites.value = []
    error.value = err?.response?.data?.message || err?.message || '加载已保存路线失败，请稍后重试。'
  } finally {
    loadingFavorites.value = false
  }
}

async function openFavorite(id) {
  error.value = ''
  favoriteMessage.value = ''

  try {
    const response = await getSavedHeritageRouteDetail(id)
    if (response.code !== 200) {
      throw new Error(response.message || '加载已保存路线失败。')
    }

    routeResult.value = response.data
    userInput.value = response.data?.query || ''
    activeFavoriteId.value = id
  } catch (err) {
    error.value = err?.response?.data?.message || err?.message || '加载已保存路线失败，请稍后重试。'
  }
}

async function removeFavorite(id) {
  error.value = ''
  favoriteMessage.value = ''

  try {
    const response = await deleteSavedHeritageRoute(id)
    if (response.code !== 200) {
      throw new Error(response.message || '删除已保存路线失败。')
    }

    if (activeFavoriteId.value === id) {
      activeFavoriteId.value = null
      if (routeResult.value?.id === id) {
        routeResult.value = null
      }
    }

    favoriteMessage.value = '已保存的路线已删除。'
    await loadFavorites()
  } catch (err) {
    error.value = err?.response?.data?.message || err?.message || '删除已保存路线失败，请稍后重试。'
  }
}

function formatDate(value) {
  if (!value) {
    return '刚刚'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '刚刚'
  }

  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

onMounted(() => {
  loadFavorites()
})
</script><style scoped>
.ai-route-page {
  min-height: 100vh;
  padding: 24px 20px 96px;
  background:
    radial-gradient(circle at top left, rgba(234, 88, 12, 0.15), transparent 32%),
    radial-gradient(circle at bottom right, rgba(120, 53, 15, 0.12), transparent 28%),
    linear-gradient(180deg, #fff8ef 0%, #f6f1ea 100%);
}

.hero-panel,
.favorites-card,
.overview-card,
.day-card,
.side-card,
.empty-card,
.loading-card {
  border: 1px solid rgba(148, 88, 45, 0.12);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 48px rgba(110, 68, 28, 0.08);
  backdrop-filter: blur(12px);
}

.hero-panel {
  max-width: 1240px;
  margin: 0 auto 24px;
  padding: 28px;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 20px;
}

.hero-copy {
  padding: 8px 4px 8px 2px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #b45309;
}

.hero-copy h1 {
  margin: 0 0 12px;
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.08;
  color: #3f220f;
}

.hero-text {
  max-width: 680px;
  margin: 0;
  font-size: 16px;
  line-height: 1.85;
  color: #6b4a2c;
}

.hero-meta {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-meta span,
.count-badge {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(251, 191, 36, 0.16);
  color: #7c2d12;
  font-size: 13px;
  font-weight: 600;
}

.hero-card {
  padding: 24px;
  border-radius: 20px;
  background: linear-gradient(180deg, #fffdf8 0%, #fff8ef 100%);
}

.hero-card h2 {
  margin: 0 0 14px;
  font-size: 22px;
  color: #3f220f;
}

.input-label {
  display: block;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #7c2d12;
}

textarea {
  width: 100%;
  min-height: 132px;
  box-sizing: border-box;
  resize: vertical;
  padding: 16px 18px;
  border: 1px solid rgba(180, 83, 9, 0.2);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  color: #2f1b0f;
  font-size: 15px;
  line-height: 1.7;
  font-family: inherit;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

textarea:focus {
  outline: none;
  border-color: rgba(234, 88, 12, 0.5);
  box-shadow: 0 0 0 4px rgba(249, 115, 22, 0.12);
}

.prompt-grid {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.prompt-chip {
  padding: 10px 14px;
  border: 1px solid rgba(234, 88, 12, 0.18);
  border-radius: 999px;
  background: #fffaf2;
  color: #b45309;
  font-size: 13px;
  line-height: 1.4;
  cursor: pointer;
  transition: transform 0.18s ease, background-color 0.18s ease, border-color 0.18s ease;
}

.prompt-chip:hover:not(:disabled) {
  transform: translateY(-1px);
  background: #fff1dc;
  border-color: rgba(234, 88, 12, 0.32);
}

.action-row {
  margin-top: 18px;
  display: flex;
  gap: 12px;
}

.generate-btn,
.ghost-btn,
.save-btn,
.delete-btn {
  border: none;
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.generate-btn,
.save-btn {
  background: linear-gradient(135deg, #c2410c 0%, #ea580c 100%);
  color: #fffaf5;
  font-size: 15px;
  font-weight: 700;
}

.generate-btn {
  flex: 1;
  padding: 14px 20px;
}

.ghost-btn {
  padding: 14px 18px;
  background: #fff;
  color: #9a3412;
  border: 1px solid rgba(234, 88, 12, 0.16);
}

.save-btn {
  padding: 12px 18px;
  white-space: nowrap;
}

.delete-btn {
  padding: 10px 14px;
  background: #fff1f2;
  color: #be123c;
  font-weight: 600;
}

.generate-btn:hover:not(:disabled),
.ghost-btn:hover:not(:disabled),
.save-btn:hover:not(:disabled),
.delete-btn:hover:not(:disabled),
.prompt-chip:hover:not(:disabled) {
  transform: translateY(-1px);
}

.generate-btn:hover:not(:disabled),
.save-btn:hover:not(:disabled) {
  box-shadow: 0 14px 30px rgba(194, 65, 12, 0.24);
}

.generate-btn:disabled,
.ghost-btn:disabled,
.save-btn:disabled,
.delete-btn:disabled,
.prompt-chip:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.error-message,
.success-message {
  margin: 14px 0 0;
  font-size: 14px;
  line-height: 1.6;
}

.error-message {
  color: #b91c1c;
}

.success-message {
  color: #15803d;
}

.favorites-panel,
.loading-panel,
.result-section,
.empty-panel {
  max-width: 1240px;
  margin: 0 auto;
}

.favorites-panel {
  margin-bottom: 24px;
}

.favorites-card,
.overview-card {
  padding: 26px 28px;
}

.section-head,
.overview-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-head h2,
.overview-head h2 {
  margin: 0;
  color: #3f220f;
}

.favorites-empty {
  padding: 18px 0 4px;
  color: #6b4a2c;
}

.favorites-list {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.favorite-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(180, 83, 9, 0.12);
  background: #fffaf4;
}

.favorite-item.active {
  border-color: rgba(234, 88, 12, 0.28);
  box-shadow: 0 12px 28px rgba(194, 65, 12, 0.08);
}

.favorite-main {
  flex: 1;
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  cursor: pointer;
}

.favorite-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #8b6a4c;
  font-size: 12px;
}

.favorite-main h3 {
  margin: 10px 0 8px;
  color: #2f1b0f;
  font-size: 17px;
  line-height: 1.4;
}

.favorite-main p {
  margin: 0;
  color: #6b4a2c;
  line-height: 1.7;
}

.overview-actions {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.overview-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.overview-badges span,
.tag {
  padding: 6px 12px;
  border-radius: 999px;
  background: #fff1dc;
  color: #9a3412;
  font-size: 12px;
  font-weight: 700;
}

.overview-summary {
  margin: 14px 0 0;
  color: #6b4a2c;
  font-size: 15px;
  line-height: 1.9;
}

.tag-row {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-row.compact {
  margin-top: 10px;
}

.loading-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.loading-card {
  padding: 24px;
}

.pulse-line {
  height: 14px;
  margin-bottom: 14px;
  border-radius: 999px;
  background: linear-gradient(90deg, #f5e6d3, #fff6ea, #f5e6d3);
  background-size: 200% 100%;
  animation: shimmer 1.6s linear infinite;
}

.pulse-line.wide {
  width: 88%;
}

.pulse-line.short {
  width: 42%;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.8fr);
  gap: 20px;
}

.main-column,
.side-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.day-card,
.side-card {
  padding: 22px;
}

.day-header {
  margin-bottom: 16px;
}

.day-index {
  margin: 0 0 4px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #c2410c;
}

.day-header h3,
.side-header h3 {
  margin: 0;
  color: #3f220f;
  font-size: 21px;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 14px;
}

.time-pill {
  height: fit-content;
  padding: 10px 12px;
  border-radius: 14px;
  background: linear-gradient(180deg, #fff4e3 0%, #ffedd5 100%);
  color: #c2410c;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.timeline-body {
  padding: 18px;
  border-radius: 18px;
  background: #fffaf3;
  border: 1px solid rgba(234, 88, 12, 0.08);
}

.timeline-body--official {
  background: linear-gradient(180deg, #fffaf0 0%, #fff4dd 100%);
}

.timeline-body--heritage {
  background: linear-gradient(180deg, #fffaf5 0%, #fdf0e6 100%);
  border-color: rgba(194, 65, 12, 0.12);
}

.timeline-body--activity {
  background: linear-gradient(180deg, #fffdf8 0%, #fff7ea 100%);
}

.timeline-body--fallback {
  background: linear-gradient(180deg, #fffdfa 0%, #f8efe2 100%);
}

.timeline-head {
  margin-bottom: 10px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.entry-kind {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.entry-kind--official {
  background: rgba(217, 119, 6, 0.14);
  color: #9a3412;
}

.entry-kind--heritage {
  background: rgba(180, 83, 9, 0.12);
  color: #9a3412;
}

.entry-kind--activity {
  background: rgba(22, 163, 74, 0.12);
  color: #166534;
}

.entry-kind--fallback {
  background: rgba(120, 53, 15, 0.12);
  color: #7c2d12;
}

.entry-hint {
  color: #8b6a4c;
  font-size: 12px;
  line-height: 1.5;
}

.timeline-body h4,
.side-item h4 {
  margin: 0 0 8px;
  color: #2f1b0f;
  font-size: 18px;
}

.timeline-body p,
.side-item p,
.empty-card p {
  margin: 0;
  color: #6b4a2c;
  line-height: 1.8;
}

.detail-list {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
}

.detail-item {
  padding: 8px 10px;
  border-radius: 12px;
  background: #fff;
  color: #7c2d12;
  font-size: 13px;
}

.business-box {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #fff6ea 0%, #ffedd5 100%);
  color: #7c2d12;
  font-size: 13px;
}

.side-header {
  margin-bottom: 12px;
}

.side-item {
  padding: 14px 0;
  border-bottom: 1px solid rgba(148, 88, 45, 0.1);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.side-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.side-item span {
  color: #8b6a4c;
  font-size: 13px;
}

.empty-card {
  padding: 40px 28px;
  text-align: center;
}

.empty-card h2 {
  margin: 0 0 10px;
  color: #3f220f;
  font-size: 26px;
}

@keyframes shimmer {
  from {
    background-position: 200% 0;
  }
  to {
    background-position: -200% 0;
  }
}

@media (max-width: 1024px) {
  .hero-panel,
  .content-grid,
  .loading-panel,
  .favorites-list {
    grid-template-columns: 1fr;
  }

  .overview-head,
  .section-head,
  .overview-actions {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .ai-route-page {
    padding: 16px 14px 88px;
  }

  .hero-panel,
  .favorites-card,
  .overview-card,
  .day-card,
  .side-card,
  .empty-card,
  .loading-card {
    border-radius: 20px;
  }

  .hero-panel,
  .favorites-card,
  .overview-card,
  .day-card,
  .side-card,
  .hero-card,
  .empty-card,
  .loading-card {
    padding: 18px;
  }

  .timeline-item {
    grid-template-columns: 1fr;
  }

  .time-pill {
    width: fit-content;
  }

  .action-row {
    flex-direction: column;
  }

  .favorite-item {
    flex-direction: column;
  }
}
</style>
