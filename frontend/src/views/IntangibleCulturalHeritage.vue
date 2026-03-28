<template>
  <div class="heritage-page">
    <section class="hero">
      <div>
        <h1>官方非遗库</h1>
        <p>浏览平台发布的非遗介绍、文化资讯与运营公告，建立完整的在地文化认知。</p>
      </div>
    </section>

    <section class="category-grid">
      <article v-for="item in categories" :key="item.title" class="category-card">
        <div class="icon">{{ item.icon }}</div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.description }}</p>
      </article>
    </section>

    <section class="toolbar">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-btn', { active: currentTab === tab.value }]"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </section>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="contents.length === 0" class="empty">官方内容库暂时为空，请先在管理后台发布内容。</div>
    <section v-else class="content-grid">
      <article v-for="item in contents" :key="item.id" class="content-card">
        <img v-if="item.coverImage" :src="item.coverImage" class="cover" alt="官方内容封面" />
        <div v-else class="cover placeholder">📚</div>
        <div class="card-body">
          <div class="top-row">
            <span :class="['category-tag', item.category]">{{ categoryLabel(item.category) }}</span>
            <span class="time">{{ formatDate(item.createTime) }}</span>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ excerpt(item.content) }}</p>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { getOfficialContents } from '../api/app'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const tabs = [
  { label: '全部内容', value: '' },
  { label: '非遗介绍', value: 'introduction' },
  { label: '文化资讯', value: 'news' },
  { label: '平台公告', value: 'announcement' }
]

const categories = [
  { icon: '🎭', title: '传统表演', description: '戏曲、曲艺、舞蹈等表演类非遗内容' },
  { icon: '🧵', title: '传统工艺', description: '刺绣、剪纸、陶瓷、漆器等手作技艺' },
  { icon: '🍵', title: '生活美学', description: '茶艺、香事、节气与民俗礼仪体验' },
  { icon: '🌏', title: '国际传播', description: '面向海内外用户的非遗数字化传播内容' }
]

const currentTab = ref('')
const contents = ref([])
const loading = ref(false)

const loadContents = async () => {
  loading.value = true
  try {
    const response = await getOfficialContents(currentTab.value || undefined)
    contents.value = response.data || []
  } catch (error) {
    notify('获取官方内容失败', 'error')
  } finally {
    loading.value = false
  }
}

const switchTab = async (tab) => {
  currentTab.value = tab
  await loadContents()
}

const categoryLabel = (category) => ({
  introduction: '非遗介绍',
  news: '文化资讯',
  announcement: '平台公告'
}[category] || '官方内容')

const excerpt = (content) => !content ? '暂无内容摘要' : content.length > 120 ? `${content.slice(0, 120)}...` : content
const formatDate = (date) => date ? new Date(date).toLocaleDateString('zh-CN') : '-'

onMounted(loadContents)
</script>

<style scoped>
.heritage-page { min-height: 100vh; background: #f5f7fa; padding: 20px 20px 90px; }
.hero { max-width: 1200px; margin: 0 auto 20px; padding: 28px; border-radius: 18px; background: linear-gradient(135deg, #1d4ed8, #0f766e); color: #fff; }
.hero h1 { margin: 0 0 8px; font-size: 30px; }
.hero p { margin: 0; max-width: 720px; line-height: 1.7; color: rgba(255,255,255,0.88); }
.category-grid { max-width: 1200px; margin: 0 auto 20px; display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.category-card { background: #fff; border-radius: 16px; padding: 20px; border: 1px solid #e5e7eb; }
.icon { font-size: 32px; margin-bottom: 12px; }
.category-card h3 { margin: 0 0 8px; font-size: 17px; color: #111827; }
.category-card p { margin: 0; color: #6b7280; line-height: 1.6; font-size: 14px; }
.toolbar { max-width: 1200px; margin: 0 auto 20px; display: flex; flex-wrap: wrap; gap: 10px; }
.tab-btn { padding: 9px 16px; border-radius: 20px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-size: 14px; color: #4b5563; }
.tab-btn.active { background: #1d4ed8; border-color: #1d4ed8; color: #fff; }
.loading, .empty { max-width: 1200px; margin: 0 auto; text-align: center; padding: 80px 20px; color: #9ca3af; }
.content-grid { max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.content-card { background: #fff; border-radius: 16px; overflow: hidden; border: 1px solid #e5e7eb; }
.cover { width: 100%; height: 220px; object-fit: cover; background: #f3f4f6; }
.cover.placeholder { display: flex; align-items: center; justify-content: center; font-size: 56px; }
.card-body { padding: 18px; }
.top-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.category-tag { padding: 4px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.category-tag.introduction { background: #dbeafe; color: #1d4ed8; }
.category-tag.news { background: #dcfce7; color: #15803d; }
.category-tag.announcement { background: #fef3c7; color: #b45309; }
.time { font-size: 12px; color: #9ca3af; }
.content-card h3 { margin: 0 0 10px; font-size: 18px; color: #111827; }
.content-card p { margin: 0; color: #6b7280; line-height: 1.7; font-size: 14px; }
@media (max-width: 900px) {
  .category-grid { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 640px) {
  .category-grid { grid-template-columns: 1fr; }
}
</style>
