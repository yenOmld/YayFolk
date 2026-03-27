<template>
  <div class="activity-page">
    <div class="activity-container">
      <!-- 左侧：分类筛选侧边栏 -->
      <aside class="sidebar">
        <div class="filter-section">
          <h3 class="filter-title"><i class='bx bx-category'></i> 活动分类</h3>
          <ul class="category-list">
            <li :class="{ active: selectedCategory === '' }" @click="selectCategory('')">
              全部活动
            </li>
            <li v-for="cat in categories" :key="cat.id" 
                :class="{ active: selectedCategory === cat.id }"
                @click="selectCategory(cat.id)">
              {{ cat.name }}
            </li>
          </ul>
        </div>
      </aside>

      <!-- 右侧：主内容区 -->
      <main class="main-content">
        <!-- 搜索与状态工具栏 -->
        <div class="toolbar">
          <div class="search-box">
            <i class='bx bx-search'></i>
            <input type="text" v-model="keyword" placeholder="搜索精彩活动..." @keyup.enter="applyFilters" />
            <button class="search-btn" @click="applyFilters">搜索</button>
          </div>
          
          <div class="toolbar-right">
            <div class="view-options" v-if="userInfo.role === 'merchant'">
              <span :class="{ active: viewFilter === 'all' }" @click="changeView('all')">全部活动</span>
              <span :class="{ active: viewFilter === 'my' }" @click="changeView('my')">我的活动</span>
            </div>
            <div class="status-options">
              <span :class="{ active: statusFilter === 'all' }" @click="changeStatus('all')">全部状态</span>
              <span :class="{ active: statusFilter === 'upcoming' }" @click="changeStatus('upcoming')">即将开始</span>
              <span :class="{ active: statusFilter === 'ongoing' }" @click="changeStatus('ongoing')">进行中</span>
              <span :class="{ active: statusFilter === 'ended' }" @click="changeStatus('ended')">已结束</span>
            </div>
            
            <button class="publish-btn" v-if="userInfo.role === 'merchant'" @click="goToPublish">
              <i class='bx bx-plus'></i> 发布活动
            </button>
          </div>
        </div>

        <!-- 活动网格展示 -->
        <div v-if="loading" class="loading-state">
          <i class='bx bx-loader-alt bx-spin'></i> 加载中...
        </div>
        
        <div v-else-if="paginatedActivities.length === 0" class="empty-state">
          <i class='bx bx-calendar-x'></i>
          <p>暂无符合条件的活动</p>
          <button class="reset-btn" @click="resetFilters">重置筛选</button>
        </div>
        
        <div v-else class="activity-grid">
          <div class="activity-card" v-for="item in paginatedActivities" :key="item.id" @click="goToDetail(item.id)">
            <div class="image-wrapper">
              <img :src="item.image" :alt="item.title" />
              <span class="status-badge" :class="item.status">{{ getStatusText(item.status) }}</span>
            </div>
            <div class="activity-info">
              <h4 class="activity-title" :title="item.title">{{ item.title }}</h4>
              
              <div class="activity-meta">
                <div class="meta-item">
                  <i class='bx bx-time'></i>
                  <span>{{ item.time }}</span>
                </div>
                <div class="meta-item">
                  <i class='bx bx-map'></i>
                  <span>{{ item.location }}</span>
                </div>
                <div class="meta-item">
                  <i class='bx bx-user'></i>
                  <span>已报名 {{ item.joined }}/{{ item.capacity }}</span>
                </div>
              </div>
              
              <div class="activity-bottom">
                <span class="activity-category">{{ getCategoryName(item.categoryId) }}</span>
                <span class="activity-price" :class="{ 'free': item.price === 0 }">
                  {{ item.price === 0 ? '免费' : `¥${item.price.toFixed(2)}` }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页组件 -->
        <div class="pagination" v-if="totalPages > 0">
          <button :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
            <i class='bx bx-chevron-left'></i> 上一页
          </button>
          <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
          <button :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
            下一页 <i class='bx bx-chevron-right'></i>
          </button>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getFirstLevelActivityCategories, getAllActivities, getCategoryActivities, getMerchantActivities } from '../api/app'

const router = useRouter()

// 获取用户信息判断角色
const userInfo = ref(JSON.parse(localStorage.getItem('user') || '{"role":"user"}'))

// 分类数据
const categories = ref([])

// 加载分类数据
const loadCategories = async () => {
  try {
    const res = await getFirstLevelActivityCategories()
    if (res.code === 200 || res.data) {
      categories.value = (res.data || []).filter(cat => cat.id !== 1)
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 活动数据
const allActivities = ref([])

// 加载活动数据
const loadActivities = async () => {
  try {
    loading.value = true
    let res
    if (viewFilter.value === 'my' && userInfo.value.role === 'merchant') {
      res = await getMerchantActivities(userInfo.value.id)
    } else if (selectedCategory.value !== '') {
      res = await getCategoryActivities(selectedCategory.value)
    } else {
      res = await getAllActivities()
    }
    if (res.code === 200 || res.data) {
      allActivities.value = (res.data || []).map(activity => ({
        id: activity.id,
        title: activity.title,
        categoryId: activity.categoryId,
        time: formatDateTime(activity.startTime),
        location: activity.address,
        joined: activity.bookedStock,
        capacity: activity.maxStock,
        price: activity.price / 100,
        status: mapStatus(activity.status),
        image: activity.mainImage
      }))
    }
  } catch (error) {
    console.error('加载活动失败:', error)
  } finally {
    loading.value = false
  }
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 映射状态
const mapStatus = (status) => {
  const statusMap = {
    '即将开始': 'upcoming',
    '进行中': 'ongoing',
    '已结束': 'ended',
    '下架': 'ended'
  }
  return statusMap[status] || 'upcoming'
}

// 状态
const loading = ref(false)
const keyword = ref('')
const selectedCategory = ref('')
const statusFilter = ref('all')
const viewFilter = ref('all') // 'all' 或 'my'
const currentPage = ref(1)
const pageSize = ref(6)

const filteredActivities = ref([])

// 辅助方法
const getCategoryName = (id) => {
  const cat = categories.value.find(c => c.id === id)
  return cat ? cat.name : '其他'
}

const getStatusText = (status) => {
  const map = {
    'upcoming': '即将开始',
    'ongoing': '进行中',
    'ended': '已结束'
  }
  return map[status] || '未知'
}

// 初始化
onMounted(async () => {
  await loadCategories()
  await loadActivities()
  applyFilters()
})

// 交互方法
const selectCategory = async (id) => {
  selectedCategory.value = id
  await loadActivities()
  applyFilters()
}

const changeStatus = (status) => {
  statusFilter.value = status
  applyFilters()
}

const changeView = async (view) => {
  viewFilter.value = view
  await loadActivities()
  applyFilters()
}

const applyFilters = () => {
  loading.value = true
  currentPage.value = 1
  
  let result = [...allActivities.value]

  if (keyword.value.trim()) {
    const lowerKeyword = keyword.value.toLowerCase()
    result = result.filter(p => p.title.toLowerCase().includes(lowerKeyword) || p.location.toLowerCase().includes(lowerKeyword))
  }

  if (statusFilter.value !== 'all') {
    result = result.filter(p => p.status === statusFilter.value)
  }

  filteredActivities.value = result
  loading.value = false
}

const resetFilters = async () => {
  keyword.value = ''
  selectedCategory.value = ''
  statusFilter.value = 'all'
  await loadActivities()
  applyFilters()
}

// 分页计算
const totalPages = computed(() => Math.ceil(filteredActivities.value.length / pageSize.value))

const paginatedActivities = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredActivities.value.slice(start, end)
})

const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 跳转到详情页
const goToDetail = (id) => {
  router.push(`/home/activity/${id}`)
}

// 去发布活动
const goToPublish = () => {
  router.push('/home/activity/create')
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&family=Noto+Sans+SC:wght@300;400;500&display=swap');

.activity-page {
  padding: 20px;
  background-color: #F8F5F0;
  min-height: 100vh;
  position: relative;
  background-attachment: fixed;
}

.activity-page::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url('https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1920&q=80');
  background-size: cover;
  background-position: center;
  opacity: 0.15;
  z-index: 0;
  pointer-events: none;
}

.activity-container {
  display: flex;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
  align-items: flex-start;
  position: relative;
  z-index: 1;
}

/* 侧边栏样式 */
.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border: 1px solid #D9CFC1;
  border-radius: 10px;
  padding: 24px 20px;
  box-shadow: 0 4px 20px rgba(44, 44, 44, 0.06);
  position: sticky;
  top: 80px;
}

.filter-section {
  margin-bottom: 24px;
}

.filter-title {
  font-size: 17px;
  color: #9D2929;
  margin: 0 0 18px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  position: relative;
  padding-bottom: 8px;
}

.filter-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 36px;
  height: 2px;
  background: linear-gradient(90deg, #9D2929, transparent);
}

.filter-title i {
  color: #9D2929;
}

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-list li {
  padding: 11px 14px;
  border-radius: 6px;
  cursor: pointer;
  color: #2C2C2C;
  font-size: 14px;
  font-family: 'Noto Sans SC', sans-serif;
  transition: all 0.3s ease;
  margin-bottom: 5px;
  border: 1px solid transparent;
}

.category-list li:hover {
  background: rgba(157, 41, 41, 0.06);
  color: #9D2929;
  border-color: rgba(157, 41, 41, 0.15);
}

.category-list li.active {
  background: rgba(157, 41, 41, 0.1);
  color: #9D2929;
  font-weight: 500;
  border-color: rgba(157, 41, 41, 0.25);
}

/* 主内容区样式 */
.main-content {
  flex: 1;
  min-width: 0;
}

.toolbar {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
  border: 1px solid #D9CFC1;
  border-radius: 10px;
  padding: 18px 26px;
  box-shadow: 0 4px 20px rgba(44, 44, 44, 0.06);
  margin-bottom: 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.search-box {
  display: flex;
  align-items: center;
  background: #F8F5F0;
  border: 1px solid #D9CFC1;
  border-radius: 24px;
  padding: 6px 6px 6px 18px;
  flex: 1;
  max-width: 420px;
}

.search-box i {
  color: #A09283;
  font-size: 18px;
}

.search-box input {
  border: none;
  background: transparent;
  padding: 8px 12px;
  outline: none;
  width: 100%;
  font-size: 14px;
  font-family: 'Noto Sans SC', sans-serif;
  color: #2C2C2C;
}

.search-box input::placeholder {
  color: #A09283;
}

.search-btn {
  background: linear-gradient(135deg, #9D2929, #B33030);
  color: white;
  border: none;
  padding: 9px 22px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-family: 'Noto Sans SC', sans-serif;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(157, 41, 41, 0.25);
}

.search-btn:hover {
  background: linear-gradient(135deg, #8A2323, #9D2929);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.35);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 22px;
}

.publish-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #9D2929, #B33030);
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-family: 'Noto Sans SC', sans-serif;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(157, 41, 41, 0.25);
}

.publish-btn:hover {
  background: linear-gradient(135deg, #8A2323, #9D2929);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.35);
}

.status-options {
  display: flex;
  gap: 12px;
}

.status-options span {
  font-size: 14px;
  color: #5A5045;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 6px 12px;
  border-radius: 18px;
  font-family: 'Noto Sans SC', sans-serif;
  border: 1px solid transparent;
}

.status-options span:hover {
  color: #9D2929;
  background: rgba(157, 41, 41, 0.06);
  border-color: rgba(157, 41, 41, 0.15);
}

.status-options span.active {
  color: white;
  background: linear-gradient(135deg, #9D2929, #B33030);
  font-weight: 500;
  box-shadow: 0 2px 6px rgba(157, 41, 41, 0.25);
}

.view-options {
  display: flex;
  gap: 12px;
}

.view-options span {
  font-size: 14px;
  color: #5A5045;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 6px 12px;
  border-radius: 18px;
  font-family: 'Noto Sans SC', sans-serif;
  border: 1px solid transparent;
}

.view-options span:hover {
  color: #9D2929;
  background: rgba(157, 41, 41, 0.06);
  border-color: rgba(157, 41, 41, 0.15);
}

.view-options span.active {
  color: white;
  background: linear-gradient(135deg, #9D2929, #B33030);
  font-weight: 500;
  box-shadow: 0 2px 6px rgba(157, 41, 41, 0.25);
}

/* 网格样式 */
.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.activity-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border: 1px solid #D9CFC1;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(44, 44, 44, 0.08);
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.activity-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 32px rgba(44, 44, 44, 0.15);
  border-color: rgba(157, 41, 41, 0.25);
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 190px;
  overflow: hidden;
}

.image-wrapper::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to bottom, transparent 0%, transparent 70%, rgba(248, 245, 240, 0.3) 100%);
  z-index: 1;
  pointer-events: none;
}

.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  filter: saturate(0.85) contrast(0.95);
}

.activity-card:hover .image-wrapper img {
  transform: scale(1.08);
  filter: saturate(0.95) contrast(1);
}

.status-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  padding: 5px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 500;
  color: white;
  backdrop-filter: blur(8px);
  font-family: 'Noto Sans SC', sans-serif;
  z-index: 2;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.status-badge.upcoming {
  background: rgba(157, 41, 41, 0.9);
}

.status-badge.ongoing {
  background: rgba(88, 129, 87, 0.9);
}

.status-badge.ended {
  background: rgba(139, 128, 116, 0.9);
}

.activity-info {
  padding: 18px 18px 16px 18px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.activity-title {
  margin: 0 0 14px 0;
  font-size: 17px;
  color: #2C2C2C;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.activity-meta {
  display: flex;
  flex-direction: column;
  gap: 9px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  color: #5A5045;
  font-family: 'Noto Sans SC', sans-serif;
}

.meta-item i {
  color: #9D8C7A;
  font-size: 15px;
}

.activity-bottom {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid rgba(217, 207, 193, 0.6);
}

.activity-category {
  font-size: 12px;
  color: #9D2929;
  background: rgba(157, 41, 41, 0.08);
  padding: 5px 12px;
  border-radius: 4px;
  font-family: 'Noto Sans SC', sans-serif;
  border: 1px solid rgba(157, 41, 41, 0.15);
}

.activity-price {
  color: #9D2929;
  font-size: 19px;
  font-weight: bold;
  font-family: 'Noto Serif SC', serif;
}

.activity-price.free {
  color: #588157;
  font-size: 17px;
}

/* 状态样式 */
.loading-state,
.empty-state {
  text-align: center;
  padding: 70px 0;
  color: #8B8074;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #D9CFC1;
  border-radius: 12px;
  backdrop-filter: blur(8px);
  font-family: 'Noto Sans SC', sans-serif;
}

.loading-state i {
  font-size: 36px;
  color: #9D2929;
  margin-bottom: 18px;
}

.empty-state i {
  font-size: 52px;
  margin-bottom: 18px;
  color: #C4B8AA;
}

.reset-btn {
  margin-top: 18px;
  padding: 10px 28px;
  background: white;
  border: 1px solid #9D2929;
  color: #9D2929;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
}

.reset-btn:hover {
  background: linear-gradient(135deg, #9D2929, #B33030);
  color: white;
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.3);
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 18px;
  margin-top: 32px;
}

.pagination button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: 1px solid #D9CFC1;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 22px;
  color: #5A5045;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
}

.pagination button:hover:not(:disabled) {
  border-color: #9D2929;
  color: #9D2929;
  background: rgba(157, 41, 41, 0.05);
  transform: translateY(-1px);
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: rgba(217, 207, 193, 0.3);
  color: #A09283;
}

.page-info {
  font-size: 14px;
  color: #8B8074;
  font-family: 'Noto Sans SC', sans-serif;
}

/* 响应式 */
@media (max-width: 768px) {
  .activity-container {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    position: static;
  }
  
  .status-options {
    width: 100%;
    justify-content: space-between;
    overflow-x: auto;
    padding-bottom: 8px;
  }
  
  .activity-grid {
    grid-template-columns: 1fr;
  }
}
</style>