<template>
  <div class="merchant-shell">
    <div class="merchant-shell__backdrop"></div>

    <div class="merchant-shell__inner">
      <aside class="merchant-shell__sidebar">
        <div class="merchant-shell__brand">
          <div class="brand-mark">YF</div>
          <div class="brand-copy">
            <strong>商家中心</strong>
            <span>延续活动页的暖色卡片风格</span>
          </div>
        </div>

        <div class="merchant-shell__shop">
          <p class="shop-kicker">{{ isMerchant ? '已入驻商家' : '入驻申请中' }}</p>
          <h2>{{ shopName }}</h2>
          <p>{{ isMerchant ? '管理活动发布、预约处理与到店核销。' : '先完成入驻申请，再开通完整商家能力。' }}</p>
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
          </router-link>
        </nav>

        <div class="merchant-shell__footer">
          <div v-if="isMerchant" class="merchant-shell__status">
            <span class="status-dot"></span>
            <span>商家身份已认证</span>
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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const readStoredUser = () => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return {}
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    console.error('读取商家信息失败', error)
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
        to: '/merchant/apply',
        label: '申请入驻',
        desc: '提交商家认证资料',
        icon: 'bx-file-find'
      }
    ]
  }

  return [
    {
      to: '/merchant/activities',
      label: '活动管理',
      desc: '发布与维护活动内容',
      icon: 'bx-calendar-event'
    },
    {
      to: '/merchant/bookings',
      label: '预约管理',
      desc: '查看报名和核销情况',
      icon: 'bx-clipboard'
    }
  ]
})

const goBack = () => router.push('/home/heritage')

onMounted(() => {
  userInfo.value = readStoredUser()
})
</script>

<style scoped>
.merchant-shell {
  --merchant-bg: #f8f5f0;
  --merchant-paper: rgba(255, 255, 255, 0.9);
  --merchant-paper-strong: rgba(255, 255, 255, 0.96);
  --merchant-border: #d9cfc1;
  --merchant-ink: #2c2c2c;
  --merchant-soft: #6f6255;
  --merchant-muted: #8b8074;
  --merchant-accent: #9d2929;
  --merchant-accent-deep: #7f1d1d;
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

.merchant-shell__brand,
.merchant-shell__shop,
.merchant-shell__footer {
  border-radius: 22px;
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
  gap: 8px;
  padding: 12px 16px;
  border: 1px solid rgba(157, 41, 41, 0.18);
  border-radius: 16px;
  background: #fff;
  color: var(--merchant-accent);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, background 0.22s ease;
}

.back-btn:hover {
  transform: translateY(-1px);
  background: rgba(157, 41, 41, 0.06);
  box-shadow: 0 12px 24px rgba(157, 41, 41, 0.1);
}

.merchant-shell__content {
  flex: 1;
  min-width: 0;
}

.merchant-shell__content-card {
  min-height: calc(100vh - 56px);
  padding: 26px;
  border-radius: 30px;
  border: 1px solid var(--merchant-border);
  background: var(--merchant-paper);
  box-shadow: 0 18px 50px rgba(44, 44, 44, 0.08);
  backdrop-filter: blur(10px);
}

:deep(.merchant-page),
:deep(.merchant-reservations) {
  max-width: none;
  color: var(--merchant-ink);
  background: transparent !important;
  min-height: auto !important;
}

:deep(.merchant-page .page-header),
:deep(.merchant-reservations .page-header) {
  margin-bottom: 24px;
  padding: 24px 26px;
  border-radius: 24px;
  border: 1px solid rgba(217, 207, 193, 0.82);
  background:
    linear-gradient(135deg, rgba(157, 41, 41, 0.05), rgba(255, 255, 255, 0.74)),
    rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 34px rgba(44, 44, 44, 0.06);
}

:deep(.merchant-page .page-header h1),
:deep(.merchant-page .page-header h2),
:deep(.merchant-reservations .page-header h1),
:deep(.merchant-reservations .page-header h2) {
  margin: 0;
  color: var(--merchant-ink);
  font-size: 28px;
  line-height: 1.2;
}

:deep(.merchant-page .page-header p),
:deep(.merchant-page .subtitle),
:deep(.merchant-page .stat),
:deep(.merchant-reservations .activity-filter) {
  color: var(--merchant-soft);
}

:deep(.merchant-page .loading),
:deep(.merchant-page .empty),
:deep(.merchant-page .empty-state),
:deep(.merchant-page .state-card),
:deep(.merchant-page .status-card),
:deep(.merchant-page .apply-form-wrap),
:deep(.merchant-page .activity-card),
:deep(.merchant-page .product-card),
:deep(.merchant-page .order-card),
:deep(.merchant-page .modal),
:deep(.merchant-reservations .tabs),
:deep(.merchant-reservations .reservation-card),
:deep(.merchant-reservations .empty-state),
:deep(.merchant-reservations .modal-content) {
  border: 1px solid rgba(217, 207, 193, 0.82) !important;
  background: var(--merchant-paper-strong) !important;
  box-shadow: 0 16px 38px rgba(44, 44, 44, 0.06) !important;
}

:deep(.merchant-page .loading),
:deep(.merchant-page .empty),
:deep(.merchant-page .empty-state),
:deep(.merchant-page .state-card),
:deep(.merchant-reservations .empty-state) {
  color: var(--merchant-muted) !important;
  border-radius: 24px !important;
}

:deep(.merchant-page .product-card),
:deep(.merchant-page .order-card),
:deep(.merchant-page .activity-card),
:deep(.merchant-reservations .reservation-card) {
  border-radius: 22px !important;
  overflow: hidden;
}

:deep(.merchant-page .product-card:hover),
:deep(.merchant-page .activity-card:hover),
:deep(.merchant-reservations .reservation-card:hover) {
  transform: translateY(-3px);
  box-shadow: 0 22px 46px rgba(44, 44, 44, 0.1) !important;
}

:deep(.merchant-reservations .tabs) {
  margin-bottom: 22px;
  padding: 10px;
  border-radius: 22px;
}

:deep(.merchant-page .create-btn),
:deep(.merchant-page .submit-btn),
:deep(.merchant-page .go-btn),
:deep(.merchant-page .retry-btn),
:deep(.merchant-page .act-btn.bookings) {
  background: linear-gradient(135deg, #9d2929, #b33030) !important;
  color: #fff !important;
  border-color: transparent !important;
}

:deep(.merchant-page .action-btn),
:deep(.merchant-page .act-btn),
:deep(.merchant-page .btn.cancel),
:deep(.merchant-page .create-btn),
:deep(.merchant-page .submit-btn),
:deep(.merchant-page .go-btn),
:deep(.merchant-page .retry-btn) {
  border-radius: 14px !important;
}

:deep(.merchant-page input),
:deep(.merchant-page select),
:deep(.merchant-page textarea) {
  border: 1px solid rgba(217, 207, 193, 0.92) !important;
  border-radius: 16px !important;
  background: rgba(248, 245, 240, 0.7) !important;
  color: var(--merchant-ink);
  box-shadow: none !important;
}

:deep(.merchant-page input:focus),
:deep(.merchant-page select:focus),
:deep(.merchant-page textarea:focus) {
  border-color: rgba(157, 41, 41, 0.42) !important;
  box-shadow: 0 0 0 4px rgba(157, 41, 41, 0.1) !important;
  outline: none;
}

@media (max-width: 1100px) {
  .merchant-shell__inner {
    flex-direction: column;
    padding: 18px 16px;
  }

  .merchant-shell__sidebar {
    width: 100%;
    position: static;
  }

  .merchant-shell__content-card {
    min-height: auto;
  }
}

@media (max-width: 768px) {
  .merchant-shell__content-card {
    padding: 16px;
    border-radius: 24px;
  }

  :deep(.merchant-page .page-header),
  :deep(.merchant-reservations .page-header) {
    padding: 18px;
  }

  :deep(.merchant-page .page-header h1),
  :deep(.merchant-page .page-header h2),
  :deep(.merchant-reservations .page-header h1),
  :deep(.merchant-reservations .page-header h2) {
    font-size: 24px;
  }
}
</style>
