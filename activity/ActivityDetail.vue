<template>
  <div class="activity-detail-page">
    <div class="header-nav">
      <div class="back-button" @click="goBack">
        <i class='bx bx-arrow-back'></i>
        <span>返回</span>
      </div>
      <div class="breadcrumb">
        <span @click="router.push('/home/activity')">活动列表</span>
        <i class='bx bx-chevron-right'></i>
        <span>{{ activity.title || '活动详情' }}</span>
      </div>
    </div>

    <div class="detail-container">
      <!-- 顶部：封面与核心信息 -->
      <div class="top-section">
        <div class="cover-wrapper">
          <div class="main-cover">
            <img :src="activity.images.length > 0 ? activity.images[currentImageIndex] : activity.image" :alt="activity.title" />
            <span class="status-badge" :class="activity.status">{{ getStatusText(activity.status) }}</span>
          </div>
          <div v-if="activity.images.length > 1" class="thumbnails">
            <div 
              v-for="(img, index) in activity.images" 
              :key="index"
              class="thumbnail-item"
              :class="{ active: currentImageIndex === index }"
              @click="currentImageIndex = index"
            >
              <img :src="img" :alt="'图片 ' + (index + 1)" />
            </div>
          </div>
        </div>
        
        <div class="info-wrapper">
          <h1 class="title">{{ activity.title }}</h1>
          <div class="tags">
            <span class="tag"><i class='bx bx-category'></i> {{ activity.categoryName }}</span>
            <span class="tag"><i class='bx bx-time'></i> 时长: {{ activity.duration }}</span>
          </div>
          
          <div class="meta-list">
            <div class="meta-item">
              <i class='bx bx-calendar'></i>
              <div class="meta-content">
                <span class="label">活动时间</span>
                <span class="value">{{ activity.time }}</span>
              </div>
            </div>
            <div class="meta-item">
              <i class='bx bx-map'></i>
              <div class="meta-content">
                <span class="label">活动地点</span>
                <span class="value">{{ activity.location }}</span>
              </div>
            </div>
            <div class="meta-item">
              <i class='bx bx-group'></i>
              <div class="meta-content">
                <span class="label">活动人数</span>
                <span class="value">已报名 {{ activity.joined }} / 限额 {{ activity.capacity }} 人</span>
              </div>
            </div>
          </div>

          <div class="price-section">
            <div class="price">
              <span v-if="activity.price === 0" class="free">免费参与</span>
              <template v-else>
                <span class="currency">¥</span>
                <span class="amount">{{ activity.price?.toFixed(2) }}</span>
                <span class="unit">/人</span>
              </template>
            </div>
            
            <button 
              class="book-btn" 
              :disabled="activity.status === 'ended' || activity.joined >= activity.capacity"
              @click="goToBooking"
            >
              {{ getBookingBtnText() }}
            </button>
          </div>
        </div>
      </div>

      <!-- 底部：详细介绍与流程 -->
      <div class="bottom-section">
        <div class="tabs">
          <div class="tab active">活动详情</div>
        </div>
        
        <div class="content-area">
          <div class="section-block merchant-section">
            <h3><i class='bx bx-store'></i> 主办方</h3>
            <div class="merchant-card">
              <div class="merchant-avatar">
                <img :src="merchant.avatar || 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=100&h=100&fit=crop'" :alt="merchant.name" />
              </div>
              <div class="merchant-info">
                <h4 class="merchant-name">{{ merchant.name }}</h4>
                <p class="merchant-desc">{{ merchant.description }}</p>
                <div class="merchant-stats">
                  <span><i class='bx bx-calendar-check'></i> 已组织 {{ merchant.activityCount }} 场活动</span>
                  <span><i class='bx bx-star'></i> 评分 {{ merchant.rating }}</span>
                </div>
              </div>
              <button class="contact-merchant-btn" @click="contactMerchant">
                <i class='bx bx-chat'></i> 联系商家
              </button>
            </div>
          </div>
          
          <div class="section-block">
            <h3><i class='bx bx-info-circle'></i> 活动简介</h3>
            <p class="desc-text">{{ activity.description }}</p>
          </div>
          
          <div class="section-block">
            <h3><i class='bx bx-list-ol'></i> 活动流程</h3>
            <ul class="process-list">
              <li v-for="(step, index) in activity.process" :key="index">
                <div class="step-time">{{ step.time }}</div>
                <div class="step-desc">{{ step.content }}</div>
              </li>
            </ul>
          </div>
          
          <div class="section-block" v-if="activity.notes && activity.notes.length > 0">
            <h3><i class='bx bx-error-circle'></i> 注意事项</h3>
            <ul class="notes-list">
              <li v-for="(note, index) in activity.notes" :key="index">{{ note }}</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getActivityById, getActivitySchedules, getUserById, getActivityImages } from '../api/app'

const route = useRoute()
const router = useRouter()

const activity = ref({})
const merchant = ref({})
const currentImageIndex = ref(0)

const stripHtmlTags = (html) => {
  if (!html) return ''
  const div = document.createElement('div')
  div.innerHTML = html
  return div.textContent || div.innerText || ''
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

const fetchMerchantInfo = async (merchantId) => {
  try {
    const userRes = await getUserById(merchantId)
    if (userRes.code === 200 && userRes.data) {
      const userData = userRes.data
      merchant.value = {
        id: userData.id,
        name: userData.nickname || userData.username,
        description: '非遗活动举办方',
        avatar: userData.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + userData.username,
        activityCount: 0,
        rating: 4.8
      }
    }
  } catch (error) {
    console.error('获取商家信息失败:', error)
  }
}

// 获取活动详情
const fetchActivityDetail = async (id) => {
  try {
    const activityRes = await getActivityById(id)
    if (activityRes.code === 200 || activityRes.data) {
      const activityData = activityRes.data
      
      let images = []
      try {
        const imagesRes = await getActivityImages(id)
        if (imagesRes.code === 200 && imagesRes.data) {
          images = imagesRes.data.map(img => img.imageUrl)
        }
      } catch (e) {
        console.error('获取活动图片失败:', e)
      }
      
      if (images.length === 0 && activityData.images) {
        try {
          images = JSON.parse(activityData.images)
        } catch (e) {
          console.error('解析活动图片失败:', e)
        }
      }
      
      if (images.length === 0 && activityData.mainImage) {
        images = [activityData.mainImage]
      }
      
      activity.value = {
        id: activityData.id,
        title: activityData.title,
        categoryName: '',
        time: formatDateTime(activityData.startTime),
        duration: '',
        location: activityData.address,
        joined: activityData.bookedStock,
        capacity: activityData.maxStock,
        price: activityData.price / 100,
        status: mapStatus(activityData.status),
        image: activityData.mainImage,
        images: images,
        description: stripHtmlTags(activityData.detail),
        process: [],
        notes: []
      }
      
      try {
        const scheduleRes = await getActivitySchedules(id)
        if (scheduleRes.code === 200 || scheduleRes.data) {
          activity.value.process = (scheduleRes.data || []).map(s => ({
            time: s.stepTime,
            content: s.stepDesc || s.stepTitle
          }))
        }
      } catch (error) {
        console.error('获取活动流程失败:', error)
      }
      
      if (activityData.merchantId) {
        await fetchMerchantInfo(activityData.merchantId)
      }
    }
  } catch (error) {
    console.error('获取活动详情失败:', error)
  }
}

const contactMerchant = () => {
  alert('联系商家功能：这里可以跳转到商家详情页或发起会话')
}

onMounted(() => {
  const id = route.params.id
  fetchActivityDetail(id)
})

const getStatusText = (status) => {
  const map = { 'upcoming': '即将开始', 'ongoing': '进行中', 'ended': '已结束' }
  return map[status] || '未知'
}

const getBookingBtnText = () => {
  if (activity.value.status === 'ended') return '活动已结束'
  if (activity.value.joined >= activity.value.capacity) return '名额已满'
  return '立即预约'
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home/activity')
  }
}

const goToBooking = () => {
  router.push(`/home/activity/${activity.value.id}/booking`)
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&family=Noto+Sans+SC:wght@300;400;500&display=swap');

.activity-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  overflow-y: auto;
  position: relative;
  background-color: #F8F5F0;
}

.activity-detail-page::before {
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

.header-nav {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 22px;
  position: relative;
  z-index: 1;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #5A5045;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border: 1px solid #D9CFC1;
  box-shadow: 0 2px 8px rgba(44, 44, 44, 0.06);
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
}

.back-button:hover {
  color: #9D2929;
  box-shadow: 0 4px 16px rgba(157, 41, 41, 0.15);
  transform: translateX(-3px);
  border-color: rgba(157, 41, 41, 0.25);
}

.breadcrumb {
  font-size: 14px;
  color: #8B8074;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Noto Sans SC', sans-serif;
}

.breadcrumb span:first-child {
  cursor: pointer;
  transition: color 0.3s ease;
}

.breadcrumb span:first-child:hover {
  color: #9D2929;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 26px;
  position: relative;
  z-index: 1;
}

/* 顶部区域 */
.top-section {
  display: flex;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border: 1px solid #D9CFC1;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(44, 44, 44, 0.08);
}

.cover-wrapper {
  width: 50%;
  position: relative;
  display: flex;
  flex-direction: column;
}

.cover-wrapper::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to bottom, transparent 0%, transparent 65%, rgba(248, 245, 240, 0.25) 100%);
  z-index: 1;
  pointer-events: none;
}

.main-cover {
  flex: 1;
  position: relative;
  min-height: 360px;
}

.main-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  min-height: 360px;
  filter: saturate(0.88) contrast(0.96);
}

.thumbnails {
  display: grid;
  grid-template-columns: repeat(9, 1fr);
  gap: 8px;
  padding: 14px;
  background: rgba(248, 245, 240, 0.6);
  border-top: 1px solid rgba(217, 207, 193, 0.5);
  z-index: 2;
}

.thumbnail-item {
  aspect-ratio: 1;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.thumbnail-item:hover {
  transform: scale(1.05);
}

.thumbnail-item.active {
  border-color: #9D2929;
  box-shadow: 0 2px 8px rgba(157, 41, 41, 0.2);
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(0.85) contrast(0.95);
}

.status-badge {
  position: absolute;
  top: 22px;
  left: 22px;
  padding: 7px 18px;
  border-radius: 20px;
  color: white;
  font-weight: 500;
  font-size: 14px;
  backdrop-filter: blur(10px);
  z-index: 3;
  font-family: 'Noto Sans SC', sans-serif;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.status-badge.upcoming { background: rgba(157, 41, 41, 0.92); }
.status-badge.ongoing { background: rgba(88, 129, 87, 0.92); }
.status-badge.ended { background: rgba(139, 128, 116, 0.92); }

.info-wrapper {
  width: 50%;
  padding: 42px;
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 26px;
  color: #2C2C2C;
  margin: 0 0 18px 0;
  line-height: 1.45;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.tags {
  display: flex;
  gap: 14px;
  margin-bottom: 26px;
}

.tag {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #9D2929;
  background: rgba(157, 41, 41, 0.08);
  padding: 7px 14px;
  border-radius: 4px;
  font-family: 'Noto Sans SC', sans-serif;
  border: 1px solid rgba(157, 41, 41, 0.15);
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
  margin-bottom: 44px;
}

.meta-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.meta-item i {
  font-size: 20px;
  color: #9D8C7A;
  margin-top: 2px;
}

.meta-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-content .label {
  font-size: 13px;
  color: #8B8074;
  font-family: 'Noto Sans SC', sans-serif;
}

.meta-content .value {
  font-size: 15px;
  color: #2C2C2C;
  font-weight: 500;
  font-family: 'Noto Sans SC', sans-serif;
}

.price-section {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 26px;
  border-top: 1px solid rgba(217, 207, 193, 0.6);
}

.price {
  color: #9D2929;
}

.price .free {
  font-size: 25px;
  font-weight: bold;
  color: #588157;
  font-family: 'Noto Serif SC', serif;
}

.price .currency {
  font-size: 18px;
  margin-right: 4px;
}

.price .amount {
  font-size: 34px;
  font-weight: bold;
  font-family: 'Noto Serif SC', serif;
}

.price .unit {
  font-size: 14px;
  color: #8B8074;
  margin-left: 4px;
  font-family: 'Noto Sans SC', sans-serif;
}

.book-btn {
  background: linear-gradient(135deg, #9D2929, #B33030);
  color: white;
  border: none;
  padding: 15px 44px;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.3);
}

.book-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #8A2323, #9D2929);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(157, 41, 41, 0.4);
}

.book-btn:disabled {
  background: rgba(180, 171, 160, 0.8);
  cursor: not-allowed;
  box-shadow: none;
}

/* 底部区域 */
.bottom-section {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border: 1px solid #D9CFC1;
  border-radius: 12px;
  padding: 34px;
  box-shadow: 0 4px 20px rgba(44, 44, 44, 0.08);
}

.tabs {
  border-bottom: 1px solid rgba(217, 207, 193, 0.6);
  margin-bottom: 32px;
}

.tab {
  display: inline-block;
  font-size: 19px;
  color: #2C2C2C;
  font-weight: 600;
  padding-bottom: 14px;
  position: relative;
  font-family: 'Noto Serif SC', serif;
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #9D2929, #B33030);
  border-radius: 3px;
}

.content-area {
  display: flex;
  flex-direction: column;
  gap: 42px;
}

.section-block h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 19px;
  color: #2C2C2C;
  margin: 0 0 18px 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  position: relative;
  padding-bottom: 8px;
}

.section-block h3::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, #9D2929, transparent);
}

.section-block h3 i {
  color: #9D2929;
  font-size: 22px;
}

.merchant-card {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 26px;
  background: rgba(248, 245, 240, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(217, 207, 193, 0.6);
}

.merchant-avatar {
  width: 82px;
  height: 82px;
  flex-shrink: 0;
  border: 2px solid #D9CFC1;
  border-radius: 50%;
  overflow: hidden;
}

.merchant-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  filter: saturate(0.9) contrast(0.95);
}

.merchant-info {
  flex: 1;
}

.merchant-name {
  font-size: 18px;
  color: #2C2C2C;
  margin: 0 0 8px 0;
  font-weight: 600;
  font-family: 'Noto Serif SC', serif;
}

.merchant-desc {
  font-size: 14px;
  color: #5A5045;
  margin: 0 0 14px 0;
  line-height: 1.7;
  font-family: 'Noto Sans SC', sans-serif;
}

.merchant-stats {
  display: flex;
  gap: 24px;
}

.merchant-stats span {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #8B8074;
  font-family: 'Noto Sans SC', sans-serif;
}

.merchant-stats span i {
  color: #9D8C7A;
}

.contact-merchant-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  background: white;
  color: #9D2929;
  border: 1px solid #9D2929;
  padding: 11px 22px;
  border-radius: 22px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
}

.contact-merchant-btn:hover {
  background: linear-gradient(135deg, #9D2929, #B33030);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(157, 41, 41, 0.25);
}

.desc-text {
  font-size: 15px;
  color: #5A5045;
  line-height: 1.9;
  margin: 0;
  font-family: 'Noto Sans SC', sans-serif;
}

.process-list {
  list-style: none;
  padding: 0;
  margin: 0;
  position: relative;
}

.process-list::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 10px;
  bottom: 10px;
  width: 2px;
  background: rgba(217, 207, 193, 0.8);
}

.process-list li {
  display: flex;
  gap: 22px;
  margin-bottom: 22px;
  position: relative;
}

.process-list li:last-child {
  margin-bottom: 0;
}

.process-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #F8F5F0;
  border: 2px solid #9D2929;
  z-index: 1;
}

.step-time {
  width: 120px;
  font-weight: 500;
  color: #2C2C2C;
  padding-left: 24px;
  font-family: 'Noto Sans SC', sans-serif;
}

.step-desc {
  flex: 1;
  color: #5A5045;
  line-height: 1.6;
  font-family: 'Noto Sans SC', sans-serif;
}

.notes-list {
  padding-left: 20px;
  color: #5A5045;
  line-height: 1.9;
  margin: 0;
  font-family: 'Noto Sans SC', sans-serif;
}

.notes-list li {
  margin-bottom: 8px;
}

.notes-list li:last-child {
  margin-bottom: 0;
}

@media (max-width: 900px) {
  .top-section {
    flex-direction: column;
  }
  
  .cover-wrapper, .info-wrapper {
    width: 100%;
  }
  
  .cover-wrapper img {
    min-height: 260px;
  }
  
  .info-wrapper {
    padding: 28px;
  }
  
  .process-list li {
    flex-direction: column;
    gap: 5px;
  }
}

@media (max-width: 768px) {
  .activity-detail-page {
    padding: 12px;
  }
  
  .content-area {
    gap: 28px;
  }
  
  .merchant-card {
    flex-wrap: wrap;
    padding: 20px;
  }
  
  .merchant-avatar {
    width: 64px;
    height: 64px;
  }
  
  .contact-merchant-btn {
    width: 100%;
    margin-top: 14px;
  }
  
  .step-time {
    width: 100%;
    padding-left: 24px;
  }
  
  .process-list li {
    flex-direction: column;
    gap: 10px;
    margin-bottom: 18px;
  }
  
  .thumbnails {
    grid-template-columns: repeat(6, 1fr);
  }
}
</style>