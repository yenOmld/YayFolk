<template>
  <div class="achievements-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Achievement Board</p>
        <h1>打卡成就</h1>
        <p>系统会根据你的商品订单、活动报名、线下打卡、发帖和浏览记录自动点亮成就徽章。</p>
      </div>
      <div class="headline-number">
        <strong>{{ summary.unlockedCount }}</strong>
        <span>/ {{ summary.totalCount }} 已解锁</span>
      </div>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <span>商品订单</span>
        <strong>{{ summary.productOrders }}</strong>
      </div>
      <div class="summary-card">
        <span>活动报名</span>
        <strong>{{ summary.activityBookings }}</strong>
      </div>
      <div class="summary-card">
        <span>已打卡</span>
        <strong>{{ summary.checkedInCount }}</strong>
      </div>
      <div class="summary-card">
        <span>搭子帖</span>
        <strong>{{ summary.partnerPosts }}</strong>
      </div>
    </section>

    <section class="badge-grid">
      <article
        v-for="badge in badges"
        :key="badge.code"
        :class="['badge-card', badge.unlocked ? 'unlocked' : 'locked']"
      >
        <div class="badge-top">
          <span class="badge-type">{{ typeLabel(badge.type) }}</span>
          <span class="badge-state">{{ badge.unlocked ? '已解锁' : '未解锁' }}</span>
        </div>
        <h2>{{ badge.name }}</h2>
        <p>{{ badge.description }}</p>
        <div class="progress-row">
          <div class="progress-track">
            <div
              class="progress-value"
              :style="{ width: `${Math.min((badge.progress / badge.target) * 100, 100)}%` }"
            />
          </div>
          <strong>{{ badge.current }} / {{ badge.target }}</strong>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { getMyAchievements } from '../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const badges = ref([])
const summary = ref({
  productOrders: 0,
  activityBookings: 0,
  checkedInCount: 0,
  partnerPosts: 0,
  unlockedCount: 0,
  totalCount: 0
})

const loadData = async () => {
  try {
    const res = await getMyAchievements()
    const data = res.data || {}
    badges.value = data.badges || []
    summary.value = {
      ...summary.value,
      ...(data.summary || {})
    }
  } catch (error) {
    notify?.error(error?.response?.data?.message || '加载成就失败')
  }
}

const typeLabel = (type) => {
  return {
    order: '订单',
    activity: '报名',
    checkin: '打卡',
    post: '发帖',
    partner: '搭子',
    history: '浏览'
  }[type] || '成就'
}

onMounted(loadData)
</script>

<style scoped>
.achievements-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 20px 80px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.summary-card,
.badge-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.hero-card {
  padding: 28px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  background:
    radial-gradient(circle at top right, rgba(251, 191, 36, 0.2), transparent 26%),
    linear-gradient(135deg, #ffffff, #fff7ed);
}

.eyebrow {
  margin: 0 0 8px;
  color: #b45309;
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
  max-width: 700px;
}

.headline-number {
  min-width: 160px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.headline-number strong {
  font-size: 42px;
  color: #111827;
}

.headline-number span {
  color: #6b7280;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-card span {
  color: #6b7280;
}

.summary-card strong {
  font-size: 28px;
  color: #111827;
}

.badge-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.badge-card {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.badge-card.unlocked {
  background:
    radial-gradient(circle at top right, rgba(251, 191, 36, 0.18), transparent 22%),
    linear-gradient(135deg, #ffffff, #fffbeb);
}

.badge-card.locked {
  background: linear-gradient(180deg, #ffffff, #f8fafc);
}

.badge-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.badge-type,
.badge-state {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.badge-type {
  background: #eff6ff;
  color: #2563eb;
}

.badge-state {
  background: #f3f4f6;
  color: #6b7280;
}

.badge-card.unlocked .badge-state {
  background: #ecfdf5;
  color: #059669;
}

.badge-card h2 {
  margin: 0;
  color: #111827;
  font-size: 22px;
}

.badge-card p {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.progress-track {
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background: #e5e7eb;
  overflow: hidden;
}

.progress-value {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #f59e0b, #ea580c);
}

.progress-row strong {
  color: #111827;
  font-size: 14px;
}

@media (max-width: 900px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .headline-number {
    align-items: flex-start;
  }

  .summary-grid,
  .badge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
