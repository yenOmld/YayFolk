<template>
  <div class="activity-detail-page">
    <div class="header-nav">
      <button class="back-button" @click="goBack">
        <i class='bx bx-arrow-back'></i>
        <span>返回</span>
      </button>
      <div class="breadcrumb">
        <span @click="router.push('/home/activity')">活动列表</span>
        <i class='bx bx-chevron-right'></i>
        <span>{{ detail?.title || '活动详情' }}</span>
      </div>
    </div>

    <div v-if="loading" class="state-card">
      <i class='bx bx-loader-alt bx-spin'></i>
      <p>加载中...</p>
    </div>

    <div v-else-if="!detail" class="state-card">
      <i class='bx bx-calendar-x'></i>
      <p>活动不存在或暂不可查看</p>
    </div>

    <div v-else class="detail-container">
      <div class="top-section">
        <div class="cover-wrapper">
          <div class="main-cover">
            <img :src="galleryImages[currentImageIndex] || detail.coverImage || placeholderCover" :alt="detail.title" />
            <span class="status-badge" :class="detail.status">{{ statusLabel(detail.status) }}</span>
          </div>
          <div v-if="galleryImages.length > 1" class="thumbnails">
            <button
              v-for="(image, index) in galleryImages"
              :key="`${image}-${index}`"
              class="thumbnail-item"
              :class="{ active: currentImageIndex === index }"
              @click="currentImageIndex = index"
            >
              <img :src="image" :alt="`图片${index + 1}`" />
            </button>
          </div>
        </div>

        <div class="info-wrapper">
          <h1 class="title">{{ detail.title }}</h1>
          <div class="tags">
            <span class="tag"><i class='bx bx-category'></i> {{ detail.heritageType || '非遗活动' }}</span>
            <span class="tag"><i class='bx bx-shape-square'></i> {{ activityTypeLabel(detail.activityType) }}</span>
          </div>

          <div class="meta-list">
            <div class="meta-item">
              <i class='bx bx-calendar'></i>
              <div class="meta-content">
                <span class="label">活动时间</span>
                <span class="value">{{ formatDateTime(detail.startTime) }}</span>
              </div>
            </div>
            <div class="meta-item">
              <i class='bx bx-map'></i>
              <div class="meta-content">
                <span class="label">活动地点</span>
                <span class="value">{{ formatLocation(detail) }}</span>
              </div>
            </div>
            <div class="meta-item">
              <i class='bx bx-group'></i>
              <div class="meta-content">
                <span class="label">活动人数</span>
                <span class="value">已报名 {{ detail.currentParticipants || 0 }} / {{ detail.maxParticipants || '不限' }}</span>
              </div>
            </div>
          </div>

          <div class="price-section">
            <div class="price">
              <span v-if="!detail.price" class="free">免费参与</span>
              <template v-else>
                <span class="currency">¥</span>
                <span class="amount">{{ (detail.price / 100).toFixed(2) }}</span>
                <span class="unit">/ 人</span>
              </template>
            </div>

            <button
              class="book-btn"
              :disabled="!canBook"
              @click="goToBooking"
            >
              {{ bookingButtonText }}
            </button>
          </div>

          <div v-if="isMerchantAccount && detail?.merchantId === currentUserId" class="merchant-manage-row">
            <button class="merchant-manage-btn" @click="goEditActivity">编辑活动</button>
            <button class="merchant-manage-btn secondary" @click="goMerchantBookings">管理预约</button>
          </div>
        </div>
      </div>

      <div class="bottom-section">
        <div v-if="videoUrl" class="section-block">
          <h3><i class='bx bx-video'></i> 活动视频</h3>
          <div class="video-player-card">
            <video controls preload="metadata" playsinline :poster="videoCover">
              <source :src="videoUrl" />
              您的浏览器暂不支持视频播放。
            </video>
          </div>
        </div>

        <div class="section-block">
          <h3><i class='bx bx-store'></i> 主办商家</h3>
          <div class="merchant-card">
            <img class="merchant-avatar" :src="detail.merchantAvatar || placeholderAvatar" :alt="detail.merchantName || 'merchant'" />
            <div class="merchant-info">
              <h4>{{ detail.merchantName || '合作商家' }}</h4>
              <p>{{ detail.merchantIntro || '该商家正在发布非遗活动与线下体验内容。' }}</p>
            </div>
            <div v-if="detail.merchantId" class="merchant-actions">
              <button class="message-merchant-btn" @click="contactMerchant">
                私信商家
              </button>
              <button class="contact-merchant-btn" @click="goMerchantHomepage">
                查看主页
              </button>
            </div>
          </div>
        </div>

        <div class="section-block">
          <h3><i class='bx bx-info-circle'></i> 活动介绍</h3>
          <p class="desc-text">{{ detail.content || detail.subtitle || '暂无活动介绍' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicActivityDetail } from '../../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const route = useRoute()
const router = useRouter()

const placeholderCover = 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&q=80'
const placeholderAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=merchant'

const loading = ref(false)
const detail = ref(null)
const currentImageIndex = ref(0)
const currentUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || localStorage.getItem('userInfo') || 'null') || {}
  } catch (error) {
    return {}
  }
})
const currentUserId = computed(() => {
  return Number(currentUser.value?.id || 0)
})
const isMerchantAccount = computed(() => currentUser.value?.role === 'merchant')
const videoUrl = computed(() => detail.value?.videoUrl || '')
const videoCover = computed(() => detail.value?.videoCoverUrl || detail.value?.coverImage || '')

const galleryImages = computed(() => {
  const value = detail.value?.images
  if (Array.isArray(value)) {
    return value.filter(Boolean)
  }
  if (typeof value === 'string' && value) {
    try {
      const parsed = JSON.parse(value)
      return Array.isArray(parsed) ? parsed.filter(Boolean) : []
    } catch (error) {
      return []
    }
  }
  return detail.value?.coverImage ? [detail.value.coverImage] : []
})

const canBook = computed(() => {
  if (!detail.value) return false
  if (isMerchantAccount.value) return false
  if (detail.value.status === 'ended') return false
  if (detail.value.status === 'full') return false
  if (detail.value.maxParticipants && detail.value.currentParticipants >= detail.value.maxParticipants) return false
  return true
})

const bookingButtonText = computed(() => {
  if (!detail.value) return '立即报名'
  if (isMerchantAccount.value && detail.value.merchantId === currentUserId.value) return '请前往商家管理'
  if (isMerchantAccount.value) return '商家账号不可预约'
  if (detail.value.status === 'ended') return '活动已结束'
  if (!canBook.value) return '名额已满'
  return '立即报名'
})

const loadDetail = async () => {
  loading.value = true
  try {
    const response = await getPublicActivityDetail(route.params.id)
    if (response.code !== 200) {
      throw new Error(response.message || '加载活动详情失败')
    }
    detail.value = response.data || null
    currentImageIndex.value = 0
  } catch (error) {
    detail.value = null
    notify.error(error.message || '加载活动详情失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/home/activity')
}

const goToBooking = () => {
  if (!detail.value || !canBook.value) {
    return
  }
  router.push(`/activity/${detail.value.id}/booking`)
}

const goEditActivity = () => {
  if (!detail.value?.id) return
  router.push({
    name: 'merchant-activity-edit',
    params: { id: detail.value.id }
  })
}

const goMerchantBookings = () => {
  if (!detail.value?.id) return
  router.push({
    name: 'merchant-bookings',
    query: {
      activityId: String(detail.value.id),
      title: detail.value.title || '',
      backTo: route.fullPath
    }
  })
}

const goMerchantHomepage = () => {
  if (!detail.value?.merchantId) return
  router.push(`/user-homepage/${detail.value.merchantId}`)
}

const contactMerchant = () => {
  if (!detail.value?.merchantId) return
  if (Number(detail.value.merchantId) === currentUserId.value) {
    notify.warning('不能给自己发私信')
    return
  }
  router.push({
    path: '/notification',
    query: {
      userId: String(detail.value.merchantId),
      returnTo: route.fullPath
    }
  })
}

const formatDateTime = (value) => value ? new Date(value).toLocaleString('zh-CN') : '待定'

const formatLocation = (item) => [item.locationProvince, item.locationCity, item.locationDistrict, item.locationDetail]
  .filter(Boolean)
  .join(' · ') || '地点待定'

const statusLabel = (status) => ({
  signup: '报名中',
  ongoing: '进行中',
  ended: '已结束',
  full: '已满员'
}[status] || '待开始')

const activityTypeLabel = (type) => ({
  offline: '线下体验',
  online: '线上课程',
  exhibition: '展览'
}[type] || '活动')

onMounted(loadDetail)
</script>

<style scoped>
.activity-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  background-color: #f8f5f0;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 22px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 24px;
  border: 1px solid #d9cfc1;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8b8074;
}

.breadcrumb span:first-child {
  cursor: pointer;
}

.state-card {
  text-align: center;
  padding: 80px 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #d9cfc1;
  border-radius: 14px;
  color: #8b8074;
}

.state-card i {
  font-size: 40px;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 26px;
}

.top-section,
.bottom-section {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #d9cfc1;
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(44, 44, 44, 0.08);
}

.top-section {
  display: flex;
  overflow: hidden;
}

.cover-wrapper,
.info-wrapper {
  width: 50%;
}

.main-cover {
  position: relative;
  min-height: 360px;
}

.main-cover img {
  width: 100%;
  height: 100%;
  min-height: 360px;
  object-fit: cover;
}

.thumbnails {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
  gap: 8px;
  padding: 14px;
  background: rgba(248, 245, 240, 0.7);
}

.thumbnail-item {
  aspect-ratio: 1;
  border: 2px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  padding: 0;
  background: transparent;
  cursor: pointer;
}

.thumbnail-item.active {
  border-color: #9d2929;
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  position: absolute;
  top: 18px;
  left: 18px;
  padding: 7px 16px;
  border-radius: 18px;
  color: #fff;
  font-size: 13px;
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

.info-wrapper {
  padding: 38px;
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 28px;
  color: #2c2c2c;
  margin: 0 0 18px;
  line-height: 1.45;
}

.tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  color: #9d2929;
  background: rgba(157, 41, 41, 0.08);
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 32px;
}

.meta-item {
  display: flex;
  gap: 14px;
}

.meta-item i {
  font-size: 20px;
  color: #9d8c7a;
}

.meta-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-content .label {
  color: #8b8074;
  font-size: 13px;
}

.meta-content .value {
  color: #2c2c2c;
  font-size: 15px;
  font-weight: 500;
}

.price-section {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding-top: 24px;
  border-top: 1px solid rgba(217, 207, 193, 0.6);
}

.price .free {
  font-size: 26px;
  font-weight: 700;
  color: #588157;
}

.price .currency,
.price .unit {
  color: #8b8074;
}

.price .amount {
  font-size: 34px;
  font-weight: 700;
  color: #9d2929;
}

.book-btn,
.contact-merchant-btn,
.message-merchant-btn {
  border: none;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.book-btn {
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: #fff;
  padding: 14px 32px;
  font-size: 15px;
  font-weight: 600;
}

.book-btn:disabled {
  background: #c7beb4;
  cursor: not-allowed;
}

.merchant-manage-row {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.merchant-manage-btn {
  border: none;
  border-radius: 18px;
  padding: 11px 18px;
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}

.merchant-manage-btn.secondary {
  background: #f6efe5;
  color: #7a2d2d;
  border: 1px solid rgba(157, 41, 41, 0.18);
}

.bottom-section {
  padding: 30px;
}

.section-block + .section-block {
  margin-top: 28px;
}

.section-block h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  color: #2c2c2c;
}

.merchant-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 22px;
  background: rgba(248, 245, 240, 0.72);
  border-radius: 12px;
}

.merchant-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.merchant-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
}

.merchant-info {
  flex: 1;
}

.merchant-info h4 {
  margin: 0 0 8px;
}

.merchant-info p,
.desc-text {
  margin: 0;
  color: #5a5045;
  line-height: 1.8;
  white-space: pre-line;
}

.contact-merchant-btn {
  background: white;
  color: #9d2929;
  border: 1px solid #9d2929;
  padding: 10px 18px;
}

.message-merchant-btn {
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: #fff;
  padding: 10px 18px;
}

.video-player-card {
  overflow: hidden;
  border-radius: 12px;
  background: #120f0d;
}

.video-player-card video {
  display: block;
  width: 100%;
  max-height: 520px;
  background: #120f0d;
}

@media (max-width: 900px) {
  .top-section {
    flex-direction: column;
  }

  .cover-wrapper,
  .info-wrapper {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .activity-detail-page {
    padding: 12px;
  }

  .header-nav {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-wrapper,
  .bottom-section {
    padding: 20px;
  }

  .merchant-card {
    flex-wrap: wrap;
  }

  .merchant-actions {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  .merchant-manage-row {
    width: 100%;
  }

  .merchant-manage-btn {
    flex: 1 1 180px;
  }
}
</style>
