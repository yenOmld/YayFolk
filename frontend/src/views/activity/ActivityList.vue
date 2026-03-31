<template>
  <div class="activity-page">
    <div class="activity-container">
      <aside class="sidebar">
        <div class="filter-section">
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
        </div>
      </aside>

      <main class="main-content">
        <div v-if="isMerchant" class="role-banner">
          <div>
            <strong>当前是商家视角</strong>
            <p>商家账号在活动广场不能预约活动，但可以直接发布、管理和编辑自己的活动。</p>
          </div>
          <div class="role-actions">
            <button class="manage-btn" @click="goCreate">发布活动</button>
            <button class="manage-btn secondary" @click="goManage">管理我的活动</button>
          </div>
        </div>

        <div class="toolbar">
          <div class="search-box">
            <i class='bx bx-search'></i>
            <input v-model.trim="keyword" type="text" placeholder="搜索精彩活动..." @keyup.enter="loadActivities" />
            <button class="search-btn" @click="loadActivities">搜索</button>
          </div>

          <div class="toolbar-right">
            <button class="activity-notice-btn" @click="goToActivityNotifications">
              <div class="activity-notice-icon">
                <i class='bx bxs-bell-ring'></i>
                <span v-if="activityNotificationUnread > 0" class="activity-notice-badge">
                  {{ activityNotificationUnread > 99 ? '99+' : activityNotificationUnread }}
                </span>
              </div>
              <div class="activity-notice-copy">
                <strong>活动通知</strong>
                <span>{{ activityNotificationHint }}</span>
              </div>
            </button>
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

        <div v-if="loading" class="loading-state">
          <i class='bx bx-loader-alt bx-spin'></i>
          <p>加载中...</p>
        </div>

        <div v-else-if="paginatedActivities.length === 0" class="empty-state">
          <i class='bx bx-calendar-x'></i>
          <p>暂无符合条件的活动</p>
          <button class="reset-btn" @click="resetFilters">重置筛选</button>
        </div>

        <div v-else class="activity-grid">
          <article v-for="item in paginatedActivities" :key="item.id" class="activity-card" @click="goToDetail(item.id)">
            <div class="image-wrapper">
              <img :src="item.coverImage || placeholderCover" :alt="item.title" />
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

        <div v-if="totalPages > 1" class="pagination">
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
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getActivityNotificationUnreadCount, getPublicActivities } from '../../api/app'

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
const pageSize = ref(6)
const allActivities = ref([])
const activityNotificationUnread = ref(0)
const currentUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || localStorage.getItem('userInfo') || 'null') || {}
  } catch (error) {
    return {}
  }
})
const currentUserId = computed(() => Number(currentUser.value?.id || 0))
const isMerchant = computed(() => currentUser.value?.role === 'merchant')
const activityNotificationHint = computed(() => (isMerchant.value ? '预约 / 订单 / 到账' : '支付 / 核销提醒'))

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

const loadActivityNotificationUnread = async () => {
  try {
    const response = await getActivityNotificationUnreadCount()
    activityNotificationUnread.value = response?.code === 200 ? Number(response.data || 0) : 0
  } catch (error) {
    activityNotificationUnread.value = 0
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
  if (page < 1 || page > totalPages.value) {
    return
  }
  currentPage.value = page
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goToDetail = (id) => {
  router.push(`/activity/${id}`)
}

const goToActivityNotifications = () => {
  router.push({ name: 'activity-notifications', query: { returnTo: '/home/activity' } })
}

const goCreate = () => {
  router.push('/merchant/activities/create')
}

const goManage = () => {
  router.push('/merchant/activities')
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
  loadActivityNotificationUnread()
})
</script>

<style scoped>
.activity-page {
  padding: 20px;
  background-color: #f8f5f0;
  min-height: 100vh;
}

.activity-container {
  display: flex;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
  align-items: flex-start;
}

.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #d9cfc1;
  border-radius: 12px;
  padding: 24px 20px;
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
  padding: 11px 14px;
  border-radius: 8px;
  cursor: pointer;
  color: #2c2c2c;
  font-size: 14px;
  transition: all 0.25s ease;
  margin-bottom: 6px;
}

.category-list li:hover,
.category-list li.active {
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.role-banner {
  margin-bottom: 18px;
  padding: 18px 22px;
  border-radius: 18px;
  border: 1px solid #d9cfc1;
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.12), rgba(251, 191, 36, 0.12));
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.role-banner strong {
  display: block;
  color: #7c2d12;
  margin-bottom: 6px;
}

.role-banner p {
  margin: 0;
  color: #7c2d12;
  line-height: 1.6;
}

.role-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.manage-btn {
  border: none;
  border-radius: 14px;
  padding: 10px 16px;
  cursor: pointer;
  background: #9d2929;
  color: #fff;
}

.manage-btn.secondary {
  background: #fff;
  color: #9d2929;
  border: 1px solid rgba(157, 41, 41, 0.28);
}

.toolbar {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #d9cfc1;
  border-radius: 12px;
  padding: 18px 24px;
  box-shadow: 0 4px 20px rgba(44, 44, 44, 0.06);
  margin-bottom: 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
}

.search-box,
.city-box {
  display: flex;
  align-items: center;
  background: #f8f5f0;
  border: 1px solid #d9cfc1;
  border-radius: 24px;
}

.search-box {
  padding: 6px 6px 6px 18px;
  flex: 1;
  min-width: 280px;
}

.city-box {
  padding: 0 14px;
  min-width: 180px;
  height: 46px;
}

.search-box i,
.city-box i {
  color: #a09283;
  font-size: 18px;
}

.search-box input,
.city-box input {
  border: none;
  background: transparent;
  padding: 8px 12px;
  outline: none;
  width: 100%;
  font-size: 14px;
  color: #2c2c2c;
}

.search-btn {
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: white;
  border: none;
  padding: 9px 22px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}

.toolbar-right,
.status-options {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.activity-notice-btn {
  border: 1px solid rgba(157, 41, 41, 0.18);
  background:
    radial-gradient(circle at top right, rgba(255, 204, 102, 0.28), transparent 52%),
    linear-gradient(135deg, rgba(157, 41, 41, 0.1), rgba(195, 113, 54, 0.12));
  color: #7f1d1d;
  border-radius: 24px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 56px;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(157, 41, 41, 0.08);
}

.activity-notice-icon {
  width: 42px;
  height: 42px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-size: 22px;
}

.activity-notice-badge {
  position: absolute;
  top: -5px;
  right: -6px;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border-radius: 999px;
  background: #dc2626;
  color: #fff;
  font-size: 11px;
  line-height: 20px;
  text-align: center;
}

.activity-notice-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
}

.activity-notice-copy strong {
  font-size: 14px;
}

.activity-notice-copy span {
  font-size: 12px;
  color: #7c2d12;
}

.status-options span {
  font-size: 14px;
  color: #5a5045;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 18px;
  transition: all 0.25s ease;
}

.status-options span.active {
  color: white;
  background: linear-gradient(135deg, #9d2929, #b33030);
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.activity-card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #d9cfc1;
  border-radius: 14px;
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
  height: 190px;
}

.owner-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.92);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  z-index: 1;
}

.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
}

.status-badge.signup {
  background: rgba(157, 41, 41, 0.92);
}

.status-badge.ongoing {
  background: rgba(88, 129, 87, 0.92);
}

.status-badge.ended,
.status-badge.full {
  background: rgba(139, 128, 116, 0.92);
}

.activity-info {
  padding: 18px;
}

.activity-title {
  margin: 0 0 14px;
  font-size: 18px;
  color: #2c2c2c;
  line-height: 1.5;
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
  padding: 5px 12px;
  border-radius: 8px;
}

.activity-price {
  color: #9d2929;
  font-size: 18px;
  font-weight: 700;
}

.activity-price.free {
  color: #588157;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 70px 0;
  color: #8b8074;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid #d9cfc1;
  border-radius: 12px;
}

.loading-state i,
.empty-state i {
  font-size: 40px;
}

.reset-btn {
  margin-top: 18px;
  padding: 10px 28px;
  background: white;
  border: 1px solid #9d2929;
  color: #9d2929;
  border-radius: 24px;
  cursor: pointer;
}

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
  border: 1px solid #d9cfc1;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 22px;
  color: #5a5045;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .activity-container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    position: static;
  }
}

@media (max-width: 768px) {
  .activity-page {
    padding: 12px;
  }

  .role-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar {
    padding: 16px;
    padding-top: 0;
    position: relative;
  }

  /* 状态筛选框缩短放在右上角 */
  .toolbar-right {
    position: absolute;
    top: 0;
    right: 0;
    z-index: 10;
  }

  .city-box {
    display: none; /* 手机端隐藏城市筛选框 */
  }

  .status-options {
    display: flex;
    gap: 4px;
    flex-wrap: nowrap;
  }

  .status-options span {
    font-size: 11px;
    padding: 4px 8px;
    border-radius: 12px;
    white-space: nowrap;
  }

  .search-box {
    padding: 10px 12px 10px 18px;
    min-width: 100%;
    margin-bottom: 12px;
  }

  .search-box input {
    font-size: 13px;
  }

  .search-btn {
    padding: 8px 16px;
    font-size: 13px;
  }

  .sidebar {
    display: none; /* 手机端隐藏侧边栏 */
  }

  .activity-container {
    display: block;
  }

  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .activity-card {
    border-radius: 12px;
  }

  .image-wrapper {
    height: 120px;
  }

  .status-badge {
    top: 10px;
    right: 10px;
    padding: 4px 10px;
    font-size: 11px;
    border-radius: 10px;
  }

  .activity-info {
    padding: 12px;
  }

  .activity-title {
    font-size: 14px;
    margin-bottom: 10px;
  }

  .activity-meta {
    gap: 6px;
    margin-bottom: 10px;
  }

  .meta-item {
    font-size: 11px;
    gap: 6px;
  }

  .activity-bottom {
    padding-top: 10px;
  }

  .activity-category {
    font-size: 11px;
    padding: 4px 10px;
    border-radius: 6px;
  }

  .activity-price {
    font-size: 14px;
  }

  .pagination button {
    padding: 8px 14px;
    font-size: 13px;
  }

  .page-info {
    font-size: 13px;
  }
}
</style>
