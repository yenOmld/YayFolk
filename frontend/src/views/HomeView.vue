<template>
  <div class="home-container">
    <!-- 主内容区 -->
    <router-view v-slot="{ Component, route }">
      <transition name="fade" mode="out-in">
        <div :key="route.path">
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
        <span>{{ $t('nav.home') }}</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: currentRoute === '/home/activity' }"
        @click="navigateTo('/home/activity')"
      >
        <i class='bx bx-calendar-event'></i>
        <span>{{ $t('nav.activity') }}</span>
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
        <span>{{ $t('nav.discover') }}</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: currentRoute === '/home/tools' }"
        @click="navigateTo('/home/tools')"
      >
        <i class='bx bx-wrench'></i>
        <span>{{ $t('nav.tools') }}</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: currentRoute === '/home/personal' }"
        @click="navigateTo('/home/personal')"
      >
        <i class='bx bx-user'></i>
        <span>{{ $t('nav.personal') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUnreadCount } from '../api/app'

const route = useRoute()
const router = useRouter()
const currentRoute = ref('/home/translate')
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
    console.error('获取未读消息数量失败', error)
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
  display: block;
}

/* 底部导航栏 */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 10px 0px 0px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.3s;
  color: #666;
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
  right: -12px;
  background: #ff2442;
  color: white;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
  font-weight: 500;
}

.nav-item i {
  font-size: 24px;
}

.nav-item span {
  font-size: 12px;
}

.nav-item.active {
  color: #7494ec;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 适配底部导航栏的内容区域 */
:deep(.personal-center),
:deep(.translate-page),
:deep(.discover-page) {
  padding-bottom: 75px;
}
</style>