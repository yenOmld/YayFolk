<template>
  <div class="admin-shell">
    <div class="admin-shell__backdrop"></div>
    <div class="admin-shell__glow admin-shell__glow--left"></div>
    <div class="admin-shell__glow admin-shell__glow--right"></div>

    <div class="admin-shell__frame">
      <aside class="admin-shell__sidebar">
        <div class="admin-shell__brand">
          <div class="brand-mark">YF</div>
          <div class="brand-copy">
            <strong>管理后台</strong>
            <span>{{ userBadge }}</span>
          </div>
        </div>

        <div class="admin-shell__summary">
          <p class="summary-kicker">Control Panel</p>
          <h2>{{ currentUser.nickname || currentUser.username || '管理员' }}</h2>
          <p>统一采用登录页的玻璃拟态风格，保留后台信息密度与管理效率。</p>
        </div>

        <nav class="admin-shell__nav">
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

        <div class="admin-shell__footer">
          <div class="current-user">
            <span class="user-name">{{ currentUser.nickname || currentUser.username || '管理员' }}</span>
            <span class="user-account">@{{ currentUser.username || 'admin' }}</span>
          </div>
          <button class="back-btn" @click="goBack">
            <i class="bx bx-left-arrow-alt"></i>
            <span>返回应用</span>
          </button>
        </div>
      </aside>

      <main class="admin-shell__content">
        <div class="admin-shell__content-card">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
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
    console.error('读取管理员信息失败', error)
    return {}
  }
}

const currentUser = readStoredUser()
const isSuperAdmin = Number(currentUser?.isSuperAdmin || 0) === 1

const navItems = computed(() => {
  const items = [
    {
      to: '/admin/merchants',
      label: '商家审核',
      desc: '处理商家入驻申请',
      icon: 'bx-store-alt'
    },
    {
      to: '/admin/activities',
      label: '活动审核',
      desc: '审核商家发布的活动',
      icon: 'bx-calendar-check'
    },
    {
      to: '/admin/posts',
      label: '内容审核',
      desc: '审核用户发布内容',
      icon: 'bx-message-square-detail'
    },
    {
      to: '/admin/users',
      label: '用户管理',
      desc: '封禁或恢复账号状态',
      icon: 'bx-user-circle'
    },
    {
      to: '/admin/official',
      label: '官方内容',
      desc: '维护平台展示内容',
      icon: 'bx-news'
    }
  ]

  if (isSuperAdmin) {
    items.unshift({
      to: '/admin/admins',
      label: '管理员管理',
      desc: '维护后台管理员账号',
      icon: 'bx-shield-quarter'
    })
  }

  return items
})

const userBadge = computed(() => (isSuperAdmin ? '超级管理员' : '管理员'))
const goBack = () => router.push('/home/heritage')
</script>

<style scoped>
.admin-shell {
  --admin-ink: #f7f4ee;
  --admin-ink-soft: rgba(247, 244, 238, 0.74);
  --admin-panel: rgba(255, 255, 255, 0.14);
  --admin-panel-soft: rgba(255, 255, 255, 0.1);
  --admin-border: rgba(255, 255, 255, 0.18);
  --admin-accent: #6e1f1f;
  --admin-accent-soft: rgba(110, 31, 31, 0.14);
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(22, 10, 10, 0.94), rgba(48, 12, 18, 0.9) 48%, rgba(26, 22, 28, 0.94)),
    #120d10;
}

.admin-shell__backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top, rgba(110, 31, 31, 0.28), transparent 42%),
    radial-gradient(circle at bottom right, rgba(191, 143, 79, 0.14), transparent 30%),
    rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(12px);
}

.admin-shell__glow {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
}

.admin-shell__glow--left {
  top: -120px;
  left: -60px;
  background: rgba(110, 31, 31, 0.38);
}

.admin-shell__glow--right {
  right: -100px;
  bottom: -140px;
  background: rgba(186, 140, 83, 0.2);
}

.admin-shell__frame {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 24px;
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px;
  align-items: stretch;
}

.admin-shell__sidebar {
  width: 290px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
  border-radius: 32px;
  border: 1px solid var(--admin-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.08));
  box-shadow:
    0 28px 70px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(24px) saturate(120%);
}

.admin-shell__brand {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.brand-mark {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border-radius: 20px;
  background: linear-gradient(180deg, #7a2323, #5c1515);
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.08em;
  box-shadow: 0 16px 28px rgba(110, 31, 31, 0.3);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-copy strong {
  color: var(--admin-ink);
  font-size: 18px;
}

.brand-copy span {
  color: var(--admin-ink-soft);
  font-size: 12px;
}

.admin-shell__summary {
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.04)),
    rgba(255, 255, 255, 0.04);
}

.summary-kicker {
  margin: 0 0 8px;
  color: rgba(251, 216, 181, 0.82);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.admin-shell__summary h2 {
  margin: 0;
  color: var(--admin-ink);
  font-size: 24px;
  line-height: 1.15;
}

.admin-shell__summary p {
  margin: 10px 0 0;
  color: var(--admin-ink-soft);
  line-height: 1.7;
  font-size: 14px;
}

.admin-shell__nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 15px 16px;
  border-radius: 20px;
  border: 1px solid transparent;
  color: var(--admin-ink-soft);
  text-decoration: none;
  transition: transform 0.24s ease, background 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease;
}

.nav-item i {
  font-size: 20px;
  margin-top: 1px;
  color: rgba(251, 216, 181, 0.88);
}

.nav-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-copy span {
  color: var(--admin-ink);
  font-size: 14px;
  font-weight: 700;
}

.nav-copy small {
  color: var(--admin-ink-soft);
  font-size: 12px;
  line-height: 1.5;
}

.nav-item:hover,
.nav-item.router-link-active {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.14);
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.14);
}

.admin-shell__footer {
  margin-top: auto;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.current-user {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 14px;
}

.user-name {
  color: var(--admin-ink);
  font-size: 14px;
  font-weight: 700;
}

.user-account {
  color: var(--admin-ink-soft);
  font-size: 12px;
}

.back-btn {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--admin-ink);
  cursor: pointer;
  transition: transform 0.24s ease, background 0.24s ease, box-shadow 0.24s ease;
}

.back-btn:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.14);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.16);
}

.admin-shell__content {
  flex: 1;
  min-width: 0;
}

.admin-shell__content-card {
  min-height: calc(100vh - 48px);
  padding: 24px;
  border-radius: 34px;
  border: 1px solid var(--admin-border);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(244, 244, 241, 0.1));
  box-shadow:
    0 28px 80px rgba(0, 0, 0, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(24px) saturate(120%);
}

:deep(.admin-page) {
  max-width: none;
  color: var(--admin-ink);
}

:deep(.admin-page .page-header) {
  margin-bottom: 24px;
  padding: 22px 24px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.06));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

:deep(.admin-page .page-header h2),
:deep(.admin-page .hero-card h2) {
  color: var(--admin-ink) !important;
}

:deep(.admin-page .page-header h2) {
  margin: 0;
  font-size: 28px !important;
}

:deep(.admin-page .page-header p),
:deep(.admin-page .hero-desc),
:deep(.admin-page .tip-text),
:deep(.admin-page .table-header),
:deep(.admin-page .small-text),
:deep(.admin-page .post-time),
:deep(.admin-page .sub),
:deep(.admin-page .time),
:deep(.admin-page .loading),
:deep(.admin-page .empty) {
  color: rgba(247, 244, 238, 0.74) !important;
}

:deep(.admin-page .filter-tabs),
:deep(.admin-page .search-box),
:deep(.admin-page .table-wrap),
:deep(.admin-page .merchant-card),
:deep(.admin-page .activity-card),
:deep(.admin-page .post-card),
:deep(.admin-page .content-card),
:deep(.admin-page .table-card),
:deep(.admin-page .hero-card),
:deep(.admin-page .loading-card),
:deep(.admin-page .modal),
:deep(.admin-page .dialog-card) {
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.08)) !important;
  box-shadow:
    0 22px 50px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.08) !important;
  backdrop-filter: blur(18px);
}

:deep(.admin-page .merchant-card),
:deep(.admin-page .activity-card),
:deep(.admin-page .post-card),
:deep(.admin-page .content-card),
:deep(.admin-page .table-card),
:deep(.admin-page .hero-card) {
  border-radius: 24px !important;
}

:deep(.admin-page .hero-card) {
  padding: 28px !important;
}

:deep(.admin-page .hero-card),
:deep(.admin-page .table-card),
:deep(.admin-page .loading-card) {
  color: var(--admin-ink) !important;
}

:deep(.admin-page .hero-card .eyebrow),
:deep(.admin-page .summary-kicker),
:deep(.admin-page .count-badge),
:deep(.admin-page .status-badge.pending),
:deep(.admin-page .audit-tag.pending),
:deep(.admin-page .audit-badge.pending),
:deep(.admin-page .role-tag.super) {
  background: rgba(251, 216, 181, 0.14) !important;
  color: #ffe2bf !important;
}

:deep(.admin-page .status-badge.approved),
:deep(.admin-page .audit-tag.approved),
:deep(.admin-page .status-tag.active),
:deep(.admin-page .status-dot.active),
:deep(.admin-page .role-tag.normal) {
  background: rgba(190, 242, 211, 0.12) !important;
  color: #b8f1cf !important;
}

:deep(.admin-page .status-badge.rejected),
:deep(.admin-page .audit-tag.rejected),
:deep(.admin-page .audit-badge.rejected),
:deep(.admin-page .status-dot.banned),
:deep(.admin-page .status-tag.disabled) {
  background: rgba(255, 181, 181, 0.12) !important;
  color: #ffc4c4 !important;
}

:deep(.admin-page .tab-btn),
:deep(.admin-page .search-btn),
:deep(.admin-page .create-btn),
:deep(.admin-page .primary-btn),
:deep(.admin-page .btn.approve),
:deep(.admin-page .btn.submit),
:deep(.admin-page .action-btn.unban),
:deep(.admin-page .page-btn),
:deep(.admin-page .submit-btn) {
  border-radius: 14px !important;
}

:deep(.admin-page .tab-btn.active),
:deep(.admin-page .search-btn),
:deep(.admin-page .create-btn),
:deep(.admin-page .primary-btn),
:deep(.admin-page .btn.approve),
:deep(.admin-page .btn.submit),
:deep(.admin-page .action-btn.unban),
:deep(.admin-page .page-btn:not(:disabled):hover),
:deep(.admin-page .submit-btn) {
  background: linear-gradient(180deg, #7a2323, #5c1515) !important;
  color: #fff !important;
  border-color: transparent !important;
}

:deep(.admin-page .btn.reject),
:deep(.admin-page .action-btn.ban),
:deep(.admin-page .ghost-btn.danger),
:deep(.admin-page .del-btn) {
  background: rgba(110, 31, 31, 0.16) !important;
  color: #ffd4d4 !important;
  border: 1px solid rgba(255, 167, 167, 0.18) !important;
}

:deep(.admin-page .ghost-btn),
:deep(.admin-page .btn.cancel),
:deep(.admin-page .subtle-btn),
:deep(.admin-page .page-btn:disabled) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: var(--admin-ink) !important;
}

:deep(.admin-page .search-box input),
:deep(.admin-page input),
:deep(.admin-page select),
:deep(.admin-page textarea) {
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  border-radius: 16px !important;
  background: rgba(255, 255, 255, 0.08) !important;
  color: var(--admin-ink) !important;
  box-shadow: none !important;
}

:deep(.admin-page input::placeholder),
:deep(.admin-page textarea::placeholder) {
  color: rgba(247, 244, 238, 0.42);
}

:deep(.admin-page .search-box input:focus),
:deep(.admin-page input:focus),
:deep(.admin-page select:focus),
:deep(.admin-page textarea:focus) {
  border-color: rgba(255, 255, 255, 0.28) !important;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.08) !important;
  outline: none;
}

:deep(.admin-page .admin-table th),
:deep(.admin-page .user-table th) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: rgba(247, 244, 238, 0.82) !important;
}

:deep(.admin-page .admin-table th),
:deep(.admin-page .admin-table td),
:deep(.admin-page .user-table th),
:deep(.admin-page .user-table td) {
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
  color: var(--admin-ink) !important;
}

:deep(.admin-page .thumb),
:deep(.admin-page .cover),
:deep(.admin-page .avatar) {
  border-color: rgba(255, 255, 255, 0.12) !important;
}

:deep(.admin-page .modal-mask),
:deep(.admin-page .dialog-mask) {
  background: rgba(0, 0, 0, 0.48) !important;
  backdrop-filter: blur(8px);
}

:deep(.admin-page .dialog-note),
:deep(.admin-page .remark),
:deep(.admin-page .info-row.remark) {
  color: #ffd5d5 !important;
}

:deep(.admin-page .label),
:deep(.admin-page .card-body),
:deep(.admin-page .meta-line),
:deep(.admin-page .post-content),
:deep(.admin-page .card-body),
:deep(.admin-page .content-card .card-body) {
  color: rgba(247, 244, 238, 0.78) !important;
}

@media (max-width: 1100px) {
  .admin-shell__frame {
    flex-direction: column;
  }

  .admin-shell__sidebar {
    width: 100%;
  }

  .admin-shell__content-card {
    min-height: auto;
  }
}

@media (max-width: 768px) {
  .admin-shell__frame {
    padding: 16px;
  }

  .admin-shell__content-card {
    padding: 16px;
    border-radius: 26px;
  }

  :deep(.admin-page .page-header) {
    padding: 18px;
  }

  :deep(.admin-page .page-header h2) {
    font-size: 24px !important;
  }
}
</style>
