<template>
  <div class="merchant-shell">
    <div class="merchant-shell__backdrop"></div>

    <div class="merchant-shell__inner">
      <aside class="merchant-shell__sidebar">
        <div class="merchant-shell__brand">
          <div class="brand-mark">YF</div>
          <div class="brand-copy">
            <strong>商家中心</strong>
            <span>活动、预约与审核动态集中处理</span>
          </div>
        </div>

        <div class="merchant-shell__shop">
          <p class="shop-kicker">{{ isMerchant ? '商家工作台' : '商家申请' }}</p>
          <h2>{{ shopName }}</h2>
          <p>{{ isMerchant ? '新的活动审核结果会在这里同步红标提醒。' : '提交入驻资料后，新的审核结果会在这里显示红标提醒。' }}</p>
        </div>

        <nav class="merchant-shell__nav">
          <router-link
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="nav-item"
          >
            <i :class="['bx', item.icon]"></i>
            <div class="nav-copy">
              <span>{{ item.label }}</span>
              <small>{{ item.desc }}</small>
            </div>
            <span v-if="getNavBadgeCount(item.key) > 0" class="nav-badge">
              {{ formatBadgeCount(getNavBadgeCount(item.key)) }}
            </span>
          </router-link>
        </nav>

        <div class="merchant-shell__footer">
          <div v-if="isMerchant" class="merchant-shell__status">
            <span class="status-dot"></span>
            <span>商家身份已开通</span>
          </div>
          <button class="back-btn" @click="goBack">
            <i class="bx bx-left-arrow-alt"></i>
            <span>返回应用</span>
          </button>
        </div>
      </aside>

      <main class="merchant-shell__content">
        <div class="merchant-shell__content-card">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  markMerchantActivityUpdatesSeen,
  markMerchantApplicationUpdateSeen,
  refreshWorkbenchBadges,
  workbenchBadgeState
} from '@/utils/workbenchBadge.js'

const router = useRouter()
const route = useRoute()
let badgeTimer = null

const readStoredUser = () => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return {}
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    console.error('Failed to read merchant user info', error)
    return {}
  }
}

const userInfo = ref(readStoredUser())
const isMerchant = computed(() => userInfo.value.role === 'merchant' || userInfo.value.role === 'admin')
const shopName = computed(() => userInfo.value.shopName || userInfo.value.nickname || '我的店铺')

const navItems = computed(() => {
  if (!isMerchant.value) {
    return [
      {
        key: 'apply',
        to: '/merchant/apply',
        label: '申请入驻',
        desc: '提交商家认证资料',
        icon: 'bx-file-find'
      }
    ]
  }

  return [
    {
      key: 'activities',
      to: '/merchant/activities',
      label: '活动管理',
      desc: '查看活动审核结果与活动状态',
      icon: 'bx-calendar-event'
    },
    {
      key: 'bookings',
      to: '/merchant/bookings',
      label: '预约管理',
      desc: '查看报名和核销情况',
      icon: 'bx-clipboard'
    }
  ]
})

const navBadgeCountMap = computed(() => ({
  apply: workbenchBadgeState.merchant.applicationCount,
  activities: workbenchBadgeState.merchant.activitiesCount,
  bookings: 0
}))

const getNavBadgeCount = (key) => Number(navBadgeCountMap.value[key] || 0)
const formatBadgeCount = (count) => (count > 99 ? '99+' : String(count))
const goBack = () => router.push('/home/heritage')

const syncSeenStateWithRoute = () => {
  if (!route.path.startsWith('/merchant')) {
    return
  }

  if (workbenchBadgeState.merchant.applicationCount > 0) {
    markMerchantApplicationUpdateSeen()
  }

  if (route.path === '/merchant/activities' && workbenchBadgeState.merchant.activitiesCount > 0) {
    markMerchantActivityUpdatesSeen()
  }
}

const refreshBadges = async () => {
  await refreshWorkbenchBadges()
  syncSeenStateWithRoute()
}

onMounted(() => {
  userInfo.value = readStoredUser()
  refreshBadges()
  badgeTimer = window.setInterval(refreshBadges, 60000)
})

watch(() => route.path, () => {
  userInfo.value = readStoredUser()
  syncSeenStateWithRoute()
})

onBeforeUnmount(() => {
  if (badgeTimer) {
    window.clearInterval(badgeTimer)
    badgeTimer = null
  }
})
</script>

<style scoped>
.merchant-shell {
  --merchant-bg: #f8f5f0;
  --merchant-paper: rgba(255, 255, 255, 0.9);
  --merchant-border: #d9cfc1;
  --merchant-ink: #2c2c2c;
  --merchant-soft: #6f6255;
  --merchant-muted: #8b8074;
  --merchant-accent: #9d2929;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(157, 41, 41, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(180, 144, 103, 0.18), transparent 30%),
    var(--merchant-bg);
}

.merchant-shell__backdrop {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.28), transparent 45%),
    linear-gradient(180deg, rgba(217, 207, 193, 0.24), transparent 60%);
  pointer-events: none;
}

.merchant-shell__inner {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 24px;
  max-width: 1380px;
  margin: 0 auto;
  padding: 28px 24px;
  align-items: flex-start;
}

.merchant-shell__sidebar {
  width: 280px;
  flex-shrink: 0;
  position: sticky;
  top: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
  border-radius: 28px;
  border: 1px solid var(--merchant-border);
  background: var(--merchant-paper);
  box-shadow: 0 18px 48px rgba(44, 44, 44, 0.08);
  backdrop-filter: blur(10px);
}

.merchant-shell__brand {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(217, 207, 193, 0.72);
}

.brand-mark {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: linear-gradient(135deg, #b33030, #9d2929);
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.08em;
  box-shadow: 0 14px 30px rgba(157, 41, 41, 0.22);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-copy strong {
  color: var(--merchant-ink);
  font-size: 18px;
}

.brand-copy span {
  color: var(--merchant-muted);
  font-size: 12px;
  line-height: 1.5;
}

.merchant-shell__shop {
  padding: 18px;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(157, 41, 41, 0.05), rgba(157, 41, 41, 0.01)),
    #fffdf9;
  border: 1px solid rgba(157, 41, 41, 0.12);
}

.shop-kicker {
  margin: 0 0 8px;
  color: var(--merchant-accent);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.merchant-shell__shop h2 {
  margin: 0;
  color: var(--merchant-ink);
  font-size: 24px;
  line-height: 1.2;
}

.merchant-shell__shop p {
  margin: 10px 0 0;
  color: var(--merchant-soft);
  font-size: 14px;
  line-height: 1.7;
}

.merchant-shell__nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid transparent;
  color: var(--merchant-soft);
  text-decoration: none;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease, background 0.22s ease, color 0.22s ease;
}

.nav-item i {
  font-size: 20px;
  color: var(--merchant-accent);
  margin-top: 2px;
}

.nav-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-copy span {
  color: var(--merchant-ink);
  font-size: 14px;
  font-weight: 700;
}

.nav-copy small {
  color: var(--merchant-muted);
  font-size: 12px;
  line-height: 1.5;
}

.nav-badge {
  min-width: 28px;
  height: 22px;
  margin-left: auto;
  padding: 0 8px;
  margin-top: 1px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 999px;
  border: 1px solid rgba(201, 145, 63, 0.28);
  background: linear-gradient(180deg, #fff8ea, #f7edd7);
  color: #8a5a16;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.nav-item:hover,
.nav-item.router-link-active {
  transform: translateY(-1px);
  background: rgba(157, 41, 41, 0.08);
  border-color: rgba(157, 41, 41, 0.14);
  box-shadow: 0 12px 28px rgba(157, 41, 41, 0.08);
}

.merchant-shell__footer {
  margin-top: auto;
  padding-top: 18px;
  border-top: 1px solid rgba(217, 207, 193, 0.72);
}

.merchant-shell__status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  color: #588157;
  font-size: 13px;
  font-weight: 600;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #588157;
  box-shadow: 0 0 0 5px rgba(88, 129, 87, 0.14);
}

.back-btn {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 13px 16px;
  border: 1px solid rgba(157, 41, 41, 0.16);
  border-radius: 16px;
  background: #fffdfa;
  color: var(--merchant-accent);
  font-weight: 700;
  cursor: pointer;
}

.merchant-shell__content {
  flex: 1;
  min-width: 0;
}

.merchant-shell__content-card {
  min-height: calc(100vh - 56px);
  padding: 24px;
  border-radius: 30px;
  border: 1px solid rgba(217, 207, 193, 0.72);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 24px 56px rgba(44, 44, 44, 0.08);
  backdrop-filter: blur(14px);
}

@media (max-width: 1024px) {
  .merchant-shell__inner {
    flex-direction: column;
  }

  .merchant-shell__sidebar {
    width: 100%;
    position: static;
  }
}

@media (max-width: 640px) {
  .merchant-shell__inner {
    padding: 16px;
  }

  .merchant-shell__content-card {
    padding: 16px;
    border-radius: 22px;
  }
}
</style>
