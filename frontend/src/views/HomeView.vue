<template>
  <div class="home-container">
    <!-- 主内容区 -->
    <router-view v-slot="{ Component, route }">
      <transition name="fade" mode="out-in">
        <div :key="route.path" class="page-shell">
          <component :is="Component" />
        </div>
      </transition>
    </router-view>

    <!-- 底部导航栏 -->
    <div class="bottom-nav">
      <div 
        class="nav-item" 
        :class="{ active: currentRoute === '/home/heritage' }"
        @click="navigateTo('/home/heritage')"
      >
        <i class='bx bx-transfer'></i>
        <span>首页</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: currentRoute === '/home/activity' }"
        @click="navigateTo('/home/activity')"
      >
        <i class='bx bx-calendar-event'></i>
        <span>活动</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: currentRoute === '/home/discover' }"
        @click="navigateTo('/home/discover')"
      >
        <div class="nav-icon-wrapper">
          <i class='bx bx-compass'></i>
          <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </div>
        <span>发现</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: currentRoute === '/home/tools' }"
        @click="navigateTo('/home/tools')"
      >
        <i class='bx bx-wrench'></i>
        <span>工具</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: currentRoute === '/home/personal' }"
        @click="navigateTo('/home/personal')"
      >
        <i class='bx bx-user'></i>
        <span>个人中心</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUnreadCount } from '../api/app'

const route = useRoute()
const router = useRouter()
const currentRoute = ref('/home/heritage')
const unreadCount = ref(0)

const updateCurrentRoute = () => {
  currentRoute.value = route.path
}

const navigateTo = (path) => {
  router.push(path)
}

const loadUnreadCount = async () => {
  try {
    const response = await getUnreadCount()
    if (response.code === 200) {
      unreadCount.value = response.data
    }
  } catch (error) {
    console.error('获取未读消息数量错误', error)
  }
}

watch(() => route.path, () => {
  updateCurrentRoute()
  loadUnreadCount()
})

onMounted(() => {
  updateCurrentRoute()
  loadUnreadCount()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  width: 100%;
  position: relative;
  background:
    radial-gradient(circle at top center, rgba(255, 255, 255, 0.6), transparent 22%),
    linear-gradient(180deg, rgba(248, 244, 238, 0.22), rgba(248, 244, 238, 0));
}

.home-container::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.06) 50%, transparent 100%);
  opacity: 0.5;
  z-index: 0;
}

.page-shell {
  min-height: 100vh;
  padding-bottom: 0;
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}

.bottom-nav {
  position: fixed;
  left: 50%;
  bottom: 5px;
  transform: translateX(-50%);
  width: min(760px, calc(100% - 24px));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px calc(10px + env(safe-area-inset-bottom, 0px));
  border: 1px solid rgba(204, 175, 145, 0.85);
  border-radius: 28px;
  background:
    linear-gradient(135deg, rgba(157, 41, 41, 0.08), rgba(255, 255, 255, 0.9)),
    rgba(255, 250, 244, 0.88);
  box-shadow:
    0 18px 40px rgba(77, 51, 30, 0.14),
    0 6px 20px rgba(157, 41, 41, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(22px) saturate(140%);
  -webkit-backdrop-filter: blur(22px) saturate(140%);
  z-index: 120;
}

.nav-item {
  flex: 1;
  min-width: 0;
  min-height: 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 10px 8px;
  border-radius: 20px;
  color: #7b6a59;
  cursor: pointer;
  user-select: none;
  transition:
    transform 0.2s ease,
    color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.nav-item:hover {
  color: #8f2f2f;
  background: rgba(157, 41, 41, 0.06);
}

.nav-item:active {
  transform: scale(0.97);
}

.nav-item i {
  font-size: 23px;
  line-height: 1;
}

.nav-item span {
  font-size: 12px;
  font-weight: 600;
  line-height: 1.1;
  white-space: nowrap;
}

.nav-item.active {
  color: #fff9f3;
  background:
    linear-gradient(135deg, #7a1d1d 0%, #9d2929 52%, #b33d2d 100%);
  box-shadow:
    0 12px 24px rgba(157, 41, 41, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.nav-icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-icon-wrapper .badge {
  position: absolute;
  top: -8px;
  right: -14px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 2px solid rgba(255, 248, 239, 0.96);
  background: linear-gradient(135deg, #e85d3f 0%, #c2410c 100%);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  box-sizing: border-box;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .page-shell {
    padding-bottom: 0;
  }

  .bottom-nav {
    bottom: 5px;
    width: calc(100% - 16px);
    gap: 4px;
    padding: 8px 8px calc(8px + env(safe-area-inset-bottom, 0px));
    border-radius: 24px;
  }

  .nav-item {
    min-height: 58px;
    padding: 8px 4px;
    border-radius: 18px;
  }

  .nav-item i {
    font-size: 21px;
  }

  .nav-item span {
    font-size: 11px;
  }

  .nav-icon-wrapper .badge {
    right: -10px;
  }
}
</style>
