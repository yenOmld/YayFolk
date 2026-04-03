﻿<template>
  <div class="activity-page">
    <!-- 移动端下拉菜单 -->
    <header class="mobile-header" :class="{ active: showMobileMenu }">
      <transition name="slide-down">
        <div v-if="showMobileMenu" class="mobile-menu">
            <!-- 分类筛选 -->
            <div class="mobile-section">
              <h4 class="mobile-section-title">活动分类</h4>
              <div class="mobile-tags">
                <span
                  :class="{ active: selectedCategory === '' }"
                  @click="selectCategory(''); closeMobileMenu()"
                >全部</span>
                <span
                  v-for="category in categories"
                  :key="category"
                  :class="{ active: selectedCategory === category }"
                  @click="selectCategory(category); closeMobileMenu()"
                >{{ category }}</span>
              </div>
            </div>

            <!-- 状态筛选 -->
            <div class="mobile-section">
              <h4 class="mobile-section-title">活动状态</h4>
              <div class="mobile-tags">
                <span :class="{ active: statusFilter === 'all' }" @click="statusFilter = 'all'; closeMobileMenu()">全部</span>
                <span :class="{ active: statusFilter === 'signup' }" @click="statusFilter = 'signup'; closeMobileMenu()">报名中</span>
                <span :class="{ active: statusFilter === 'ongoing' }" @click="statusFilter = 'ongoing'; closeMobileMenu()">进行中</span>
                <span :class="{ active: statusFilter === 'ended' }" @click="statusFilter = 'ended'; closeMobileMenu()">已结束</span>
              </div>
            </div>

            <!-- 城市筛选 -->
            <div class="mobile-section">
              <h4 class="mobile-section-title">城市筛选</h4>
              <div class="mobile-city-input">
                <i class='bx bx-map-pin'></i>
                <input v-model.trim="city" type="text" placeholder="输入城市名称" @keyup.enter="loadActivities(); closeMobileMenu()" />
              </div>
            </div>
          </div>
        </transition>
      </header>

    <div class="activity-container">
      <!-- 桌面端侧边栏 -->
      <aside class="sidebar">
        <h3 class="filter-title"><i class='bx bx-category'></i> 活动分类</h3>
          <ul class="category-list">
            <li :class="{ active: selectedCategory === '' }" @click="selectCategory('')">全部活动</li>
            <li
              v-for="category in categories"
              :key="category"
              :class="{ active: selectedCategory === category }"
              @click="selectCategory(category)"
            >
              {{ category }}
            </li>
          </ul>
      </aside>

      <main class="main-content">
        <!-- 桌面端工具栏 -->
        <div class="toolbar">
          <div class="search-box">
            <i class='bx bx-search'></i>
            <input v-model.trim="keyword" type="text" placeholder="搜索精彩活动..." @keyup.enter="loadActivities" />
            <button class="search-btn" @click="loadActivities">搜索</button>
          </div>

          <div class="toolbar-right">
            <div class="city-box">
              <i class='bx bx-map-pin'></i>
              <input v-model.trim="city" type="text" placeholder="按城市筛选" @keyup.enter="loadActivities" />
            </div>
            <div class="status-options">
              <span :class="{ active: statusFilter === 'all' }" @click="statusFilter = 'all'">全部状态</span>
              <span :class="{ active: statusFilter === 'signup' }" @click="statusFilter = 'signup'">报名中</span>
              <span :class="{ active: statusFilter === 'ongoing' }" @click="statusFilter = 'ongoing'">进行中</span>
              <span :class="{ active: statusFilter === 'ended' }" @click="statusFilter = 'ended'">已结束</span>
            </div>
          </div>
        </div>

        <!-- 移动端搜索栏和汉堡菜单 -->
        <div class="mobile-search-bar">
          <div class="mobile-search-input">
            <i class='bx bx-search'></i>
            <input v-model.trim="keyword" type="text" placeholder="搜索活动..." @keyup.enter="loadActivities" />
          </div>
          <button class="mobile-search-btn" @click="loadActivities">
            <i class='bx bx-search'></i>
          </button>
          <button class="hamburger-btn-mobile" @click="toggleMobileMenu" :class="{ active: showMobileMenu }">
            <span></span>
            <span></span>
            <span></span>
          </button>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <i class='bx bx-loader-alt bx-spin'></i>
          <p>加载中...</p>
        </div>

        <!-- 空状态 -->
        <div v-else-if="paginatedActivities.length === 0" class="empty-state">
          <i class='bx bx-calendar-x'></i>
          <p>暂无符合条件的活动</p>
          <button class="reset-btn" @click="resetFilters">重置筛选</button>
        </div>

        <!-- 活动列表 -->
        <div v-else class="activity-grid">
          <article v-for="item in paginatedActivities" :key="item.id" class="activity-card" @click="openDetail(item)">
            <div class="image-wrapper">
              <img :src="item.coverImage || placeholderCover" :alt="item.title" loading="lazy" />
              <span v-if="Number(item.merchantId || 0) === currentUserId" class="owner-badge">我的活动</span>
              <span class="status-badge" :class="item.status">{{ statusLabel(item.status) }}</span>
            </div>

            <div class="activity-info">
              <h4 class="activity-title">{{ item.title }}</h4>

              <div class="activity-meta">
                <div class="meta-item">
                  <i class='bx bx-time'></i>
                  <span>{{ formatDateTime(item.startTime) }}</span>
                </div>
                <div class="meta-item">
                  <i class='bx bx-map'></i>
                  <span>{{ formatLocation(item) }}</span>
                </div>
                <div class="meta-item">
                  <i class='bx bx-user'></i>
                  <span>已报名 {{ item.currentParticipants || 0 }}/{{ item.maxParticipants || '不限' }}</span>
                </div>
              </div>

              <div class="activity-bottom">
                <span class="activity-category">{{ item.heritageType || '其他' }}</span>
                <span class="activity-price" :class="{ free: !item.price }">
                  {{ item.price ? `¥${(item.price / 100).toFixed(2)}` : '免费' }}
                </span>
              </div>
            </div>
          </article>
        </div>

        <!-- 分页 -->
        <div v-if="totalPages > 1" class="pagination">
          <button :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
            <i class='bx bx-chevron-left'></i>
          </button>
          <span class="page-numbers">
            <span
              v-for="page in displayedPages"
              :key="page"
              :class="{ active: currentPage === page, ellipsis: page === '...' }"
              @click="page !== '...' && changePage(page)"
            >{{ page }}</span>
          </span>
          <button :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
            <i class='bx bx-chevron-right'></i>
          </button>
        </div>
      </main>
    </div>

    <ActivityDetailModal
      :visible="showDetailModal"
      :activity-id="selectedActivityId"
      :initial-detail="selectedActivity"
      @close="closeDetail"
    />
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPublicActivities } from '../../api/app'
import ActivityDetailModal from '@/components/ActivityDetailModal.vue'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const router = useRouter()

const placeholderCover = 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&q=80'

const loading = ref(false)
const keyword = ref('')
const city = ref('')
const selectedCategory = ref('')
const statusFilter = ref('all')
const currentPage = ref(1)
const pageSize = ref(9)
const allActivities = ref([])
const showDetailModal = ref(false)
const selectedActivityId = ref('')
const selectedActivity = ref(null)
const showMobileMenu = ref(false)

const currentUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || localStorage.getItem('userInfo') || 'null') || {}
  } catch (error) {
    return {}
  }
})
const currentUserId = computed(() => Number(currentUser.value?.id || 0))

const categories = computed(() => {
  const values = new Set(
    allActivities.value
      .map(item => item.heritageType)
      .filter(Boolean)
  )
  return Array.from(values)
})

const filteredActivities = computed(() => {
  let result = [...allActivities.value]

  if (selectedCategory.value) {
    result = result.filter(item => item.heritageType === selectedCategory.value)
  }

  if (statusFilter.value !== 'all') {
    result = result.filter(item => item.status === statusFilter.value)
  }

  return result
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredActivities.value.length / pageSize.value)))

const paginatedActivities = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredActivities.value.slice(start, start + pageSize.value)
})

const displayedPages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const pages = []

  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    if (current <= 3) {
      pages.push(1, 2, 3, 4, '...', total)
    } else if (current >= total - 2) {
      pages.push(1, '...', total - 3, total - 2, total - 1, total)
    } else {
      pages.push(1, '...', current - 1, current, current + 1, '...', total)
    }
  }
  return pages
})

const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value
}

const closeMobileMenu = () => {
  showMobileMenu.value = false
}

const loadActivities = async () => {
  loading.value = true
  try {
    const response = await getPublicActivities({
      keyword: keyword.value || undefined,
      city: city.value || undefined
    })
    if (response.code !== 200) {
      throw new Error(response.message || '加载活动失败')
    }
    allActivities.value = Array.isArray(response.data) ? response.data : []
    currentPage.value = 1
  } catch (error) {
    allActivities.value = []
    notify.error(error.message || '加载活动失败')
  } finally {
    loading.value = false
  }
}

const selectCategory = (value) => {
  selectedCategory.value = value
  currentPage.value = 1
}

const resetFilters = async () => {
  keyword.value = ''
  city.value = ''
  selectedCategory.value = ''
  statusFilter.value = 'all'
  await loadActivities()
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const openDetail = (item) => {
  selectedActivityId.value = item?.id ? String(item.id) : ''
  selectedActivity.value = item ? { ...item } : null
  showDetailModal.value = Boolean(selectedActivityId.value)
}

const closeDetail = () => {
  showDetailModal.value = false
  selectedActivityId.value = ''
  selectedActivity.value = null
}

const formatDateTime = (value) => {
  if (!value) return '时间待定'
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatLocation = (item) => {
  return [item.locationCity, item.locationDetail].filter(Boolean).join(' · ') || '地点待定'
}

const statusLabel = (status) => ({
  signup: '报名中',
  ongoing: '进行中',
  ended: '已结束',
  full: '已满员'
}[status] || '待开始')

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
/* ========== 页面基础 ========== */
.activity-page {
  padding: 20px 20px 70px;
  background-color: #f8f5f0;
  min-height: 100vh;
}

.activity-container {
  display: flex;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
  align-items: flex-start;
}

/* ========== 移动端头部（下拉菜单容器） ========== */
.mobile-header {
  display: none;
  position: fixed;
  top: 64px;
  right: 12px;
  z-index: 100;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  border: 1px solid #d9cfc1;
  border-radius: 12px;
  width: 280px;
  max-height: 70vh;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.mobile-header.active {
  display: block;
}

/* ========== 移动端下拉菜单 ========== */
.mobile-menu {
  background: rgba(255, 255, 255, 0.98);
  border-top: 1px solid #d9cfc1;
  padding: 16px 0;
  max-height: 70vh;
  overflow-y: auto;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.mobile-section {
  padding: 12px 0;
  border-bottom: 1px solid rgba(217, 207, 193, 0.5);
}

.mobile-section:last-child {
  border-bottom: none;
}

.mobile-section-title {
  font-size: 13px;
  color: #8b8074;
  margin: 0 0 10px;
  font-weight: 500;
}

.mobile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mobile-tags span {
  padding: 6px 14px;
  background: #f8f5f0;
  border: 1px solid #d9cfc1;
  border-radius: 16px;
  font-size: 13px;
  color: #5a5045;
  cursor: pointer;
  transition: all 0.25s ease;
}

.mobile-tags span.active {
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: white;
  border-color: #9d2929;
}

.mobile-city-input {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8f5f0;
  border: 1px solid #d9cfc1;
  border-radius: 24px;
  padding: 10px 16px;
}

.mobile-city-input i {
  color: #a09283;
  font-size: 18px;
}

.mobile-city-input input {
  border: none;
  background: transparent;
  outline: none;
  flex: 1;
  font-size: 14px;
  color: #2c2c2c;
}

/* ========== 桌面端侧边栏 ========== */
.sidebar {
  width: 260px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #d9cfc1;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(44, 44, 44, 0.06);
  position: sticky;
  top: 84px;
}

.filter-title {
  font-size: 17px;
  color: #9d2929;
  margin: 0 0 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-list li {
  padding: 12px 16px;
  border-radius: 10px;
  cursor: pointer;
  color: #2c2c2c;
  font-size: 14px;
  transition: all 0.25s ease;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.category-list li::after {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: transparent;
  transition: all 0.25s ease;
}

.category-list li:hover,
.category-list li.active {
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
}

.category-list li.active::after {
  background: #9d2929;
}

/* ========== 主内容区 ========== */
.main-content {
  flex: 1;
  min-width: 0;
  margin-bottom: 40px;
}

/* ========== 桌面端工具栏 ========== */
.toolbar {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #d9cfc1;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 4px 20px rgba(44, 44, 44, 0.06);
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.search-box {
  display: flex;
  align-items: center;
  background: #f8f5f0;
  border: 1px solid #d9cfc1;
  border-radius: 28px;
  padding: 6px 6px 6px 20px;
  flex: 1;
  min-width: 300px;
  max-width: 480px;
  transition: all 0.25s ease;
}

.search-box:focus-within {
  border-color: #9d2929;
  box-shadow: 0 0 0 3px rgba(157, 41, 41, 0.1);
}

.search-box i {
  color: #a09283;
  font-size: 18px;
}

.search-box input {
  border: none;
  background: transparent;
  padding: 10px 14px;
  outline: none;
  flex: 1;
  font-size: 14px;
  color: #2c2c2c;
}

.search-box input::placeholder {
  color: #a09283;
}

.search-btn {
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 22px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.25s ease;
}

.search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.3);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.city-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8f5f0;
  border: 1px solid #d9cfc1;
  border-radius: 24px;
  padding: 0 16px;
  height: 48px;
  min-width: 180px;
  transition: all 0.25s ease;
}

.city-box:focus-within {
  border-color: #9d2929;
  box-shadow: 0 0 0 3px rgba(157, 41, 41, 0.1);
}

.city-box i {
  color: #a09283;
  font-size: 18px;
}

.city-box input {
  border: none;
  background: transparent;
  outline: none;
  width: 100%;
  font-size: 14px;
  color: #2c2c2c;
}

.city-box input::placeholder {
  color: #a09283;
}

.status-options {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.status-options span {
  font-size: 13px;
  color: #5a5045;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  transition: all 0.25s ease;
  border: 1px solid transparent;
}

.status-options span:hover {
  background: rgba(157, 41, 41, 0.06);
  color: #9d2929;
}

.status-options span.active {
  color: white;
  background: linear-gradient(135deg, #9d2929, #b33030);
  border-color: #9d2929;
}

/* ========== 移动端搜索栏 ========== */
.mobile-search-bar {
  display: none;
  gap: 10px;
  margin-bottom: 16px;
  align-items: center;
}

.mobile-search-input {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  background: white;
  border: 1px solid #d9cfc1;
  border-radius: 24px;
  padding: 0 16px;
  height: 44px;
}

.mobile-search-input i {
  color: #a09283;
  font-size: 18px;
}

.mobile-search-input input {
  border: none;
  background: transparent;
  outline: none;
  flex: 1;
  font-size: 14px;
  color: #2c2c2c;
}

.mobile-search-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.25s ease;
  flex-shrink: 0;
}

.mobile-search-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.3);
}

.hamburger-btn-mobile {
  width: 44px;
  height: 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #d9cfc1;
  cursor: pointer;
  padding: 8px;
  border-radius: 10px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.hamburger-btn-mobile:hover {
  background: rgba(157, 41, 41, 0.08);
  border-color: #9d2929;
}

.hamburger-btn-mobile span {
  display: block;
  width: 18px;
  height: 2px;
  background: #5a5045;
  border-radius: 2px;
  transition: all 0.3s ease;
  transform-origin: center;
}

.hamburger-btn-mobile.active span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}

.hamburger-btn-mobile.active span:nth-child(2) {
  opacity: 0;
  transform: scaleX(0);
}

.hamburger-btn-mobile.active span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

/* ========== 加载和空状态 ========== */
.loading-state,
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #8b8074;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid #d9cfc1;
  border-radius: 16px;
}

.loading-state i,
.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  display: block;
}

.loading-state p,
.empty-state p {
  font-size: 15px;
  margin: 0;
}

.reset-btn {
  margin-top: 20px;
  padding: 12px 32px;
  background: white;
  border: 1px solid #9d2929;
  color: #9d2929;
  border-radius: 24px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.25s ease;
}

.reset-btn:hover {
  background: #9d2929;
  color: white;
}

/* ========== 活动卡片网格 ========== */
.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.activity-card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #d9cfc1;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(44, 44, 44, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
}

.activity-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(44, 44, 44, 0.14);
}

.image-wrapper {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.activity-card:hover .image-wrapper img {
  transform: scale(1.05);
}

.owner-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  z-index: 1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  z-index: 1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.status-badge.signup {
  background: linear-gradient(135deg, #9d2929, #b33030);
}

.status-badge.ongoing {
  background: linear-gradient(135deg, #588157, #7fb069);
}

.status-badge.ended,
.status-badge.full {
  background: linear-gradient(135deg, #6b7280, #9ca3af);
}

.activity-info {
  padding: 20px;
}

.activity-title {
  margin: 0 0 14px;
  font-size: 17px;
  color: #2c2c2c;
  line-height: 1.5;
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.activity-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #5a5045;
}

.meta-item i {
  color: #a09283;
  font-size: 15px;
}

.activity-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid rgba(217, 207, 193, 0.6);
}

.activity-category {
  font-size: 12px;
  color: #9d2929;
  background: rgba(157, 41, 41, 0.08);
  padding: 6px 14px;
  border-radius: 10px;
  font-weight: 500;
}

.activity-price {
  color: #9d2929;
  font-size: 20px;
  font-weight: 700;
}

.activity-price.free {
  color: #588157;
  font-size: 16px;
}

/* ========== 分页 ========== */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 40px;
}

.pagination button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid #d9cfc1;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 10px;
  color: #5a5045;
  cursor: pointer;
  font-size: 20px;
  transition: all 0.25s ease;
}

.pagination button:hover:not(:disabled) {
  border-color: #9d2929;
  color: #9d2929;
  background: rgba(157, 41, 41, 0.05);
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 8px;
}

.page-numbers span {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d9cfc1;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 10px;
  color: #5a5045;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.page-numbers span:hover:not(.ellipsis) {
  border-color: #9d2929;
  color: #9d2929;
}

.page-numbers span.active {
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: white;
  border-color: #9d2929;
}

.page-numbers span.ellipsis {
  cursor: default;
  border: none;
  background: transparent;
}

/* ========== 响应式设计 ========== */

/* 平板端 (1024px 以下) */
@media (max-width: 1024px) {
  .activity-container {
    gap: 20px;
  }

  .sidebar {
    width: 200px;
    padding: 20px;
  }

  .toolbar {
    padding: 16px 20px;
  }

  .search-box {
    min-width: 200px;
  }

  .activity-grid {
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 16px;
  }
}

/* 小平板端 (768px - 1024px) */
@media (max-width: 1024px) and (min-width: 769px) {
  .activity-container {
    flex-direction: row;
  }

  .sidebar {
    display: block;
    width: 180px;
    flex-shrink: 0;
  }

  .category-list li {
    padding: 10px 12px;
    font-size: 13px;
  }
}

/* 小平板端 (768px 以下) */
@media (max-width: 768px) {
  .activity-container {
    flex-direction: column;
  }

  .sidebar {
    display: none;
  }

  .toolbar {
    flex-direction: column;
    gap: 14px;
  }

  .toolbar-right {
    width: 100%;
    justify-content: flex-start;
  }

  .search-box {
    width: 100%;
    max-width: none;
    min-width: unset;
  }

  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

/* 移动端 (768px 以下) */
@media (max-width: 768px) {
  .activity-page {
    padding: 0 0 60px;
    background: #f8f5f0;
  }

  .hamburger-btn-mobile {
    display: flex;
  }

  .toolbar {
    display: none;
  }

  .mobile-search-bar {
    display: flex;
    padding: 16px 12px 0;
    margin-top: 12px;
  }

  .main-content {
    padding: 0 12px;
    width: 100%;
  }

  .activity-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .activity-card {
    border-radius: 12px;
    display: flex;
    flex-direction: row;
    min-height: 140px;
  }

  .image-wrapper {
    width: 140px;
    min-height: 140px;
    flex-shrink: 0;
  }

  .activity-info {
    flex: 1;
    padding: 12px;
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  .activity-title {
    font-size: 15px;
    margin: 0 0 6px;
    color: #2c2c2c;
    font-weight: 600;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    flex-shrink: 0;
  }

  .activity-meta {
    gap: 4px;
    margin-bottom: 8px;
    flex: 1;
  }

  .meta-item {
    font-size: 12px;
  }

  .meta-item i {
    font-size: 13px;
  }

  .activity-bottom {
    padding-top: 8px;
    flex-shrink: 0;
  }

  .activity-category {
    font-size: 11px;
    padding: 4px 10px;
  }

  .activity-price {
    font-size: 16px;
  }

  .owner-badge {
    padding: 3px 8px;
    font-size: 10px;
    top: 8px;
    left: 8px;
  }

  .status-badge {
    padding: 4px 10px;
    font-size: 10px;
    top: 8px;
    right: 8px;
  }

  .loading-state,
  .empty-state {
    margin: 0;
    padding: 60px 20px;
    border-radius: 12px;
  }

  .pagination {
    gap: 8px;
    margin-top: 24px;
    padding: 0;
  }

  .pagination button {
    width: 36px;
    height: 36px;
    font-size: 18px;
  }

  .page-numbers span {
    min-width: 36px;
    height: 36px;
    padding: 0 10px;
    font-size: 13px;
  }
}

/* 小屏移动端 (480px 以下) */
@media (max-width: 480px) {
  .mobile-search-bar {
    gap: 8px;
  }

  .mobile-search-input {
    height: 40px;
    padding: 0 12px;
  }

  .hamburger-btn-mobile {
    width: 40px;
    height: 40px;
  }

  .mobile-search-btn {
    width: 40px;
    height: 40px;
  }

  .activity-card {
    height: 120px;
  }

  .image-wrapper {
    width: 120px;
  }

  .activity-info {
    padding: 12px;
  }

  .activity-title {
    font-size: 14px;
  }

  .meta-item {
    font-size: 11px;
  }

  .activity-price {
    font-size: 14px;
  }

  .pagination {
    gap: 6px;
  }

  .page-numbers span:not(.active):not(.ellipsis) {
    display: none;
  }

  .page-numbers span.active {
    display: flex;
  }
}

/* 超小屏移动端 (360px 以下) */
@media (max-width: 360px) {
  .activity-card {
    height: 110px;
  }

  .image-wrapper {
    width: 110px;
  }

  .activity-info {
    padding: 10px;
  }

  .activity-title {
    font-size: 13px;
  }

  .activity-meta {
    gap: 4px;
  }

  .meta-item {
    font-size: 10px;
  }
}
</style>
