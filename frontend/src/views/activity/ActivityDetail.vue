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
        </div>
      </div>

      <div class="bottom-section">
        <div class="section-block">
          <h3><i class='bx bx-store'></i> 主办商家</h3>
          <div class="merchant-card">
            <img class="merchant-avatar" :src="detail.merchantAvatar || placeholderAvatar" :alt="detail.merchantName || 'merchant'" />
            <div class="merchant-info">
              <h4>{{ detail.merchantName || '合作商家' }}</h4>
              <p>{{ detail.merchantIntro || '该商家正在发布非遗活动与线下体验内容。' }}</p>
            </div>
            <button v-if="detail.merchantId" class="contact-merchant-btn" @click="goMerchantHomepage">
              查看主页
            </button>
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
  if (detail.value.status === 'ended') return false
  if (detail.value.status === 'full') return false
  if (detail.value.maxParticipants && detail.value.currentParticipants >= detail.value.maxParticipants) return false
  return true
})

const bookingButtonText = computed(() => {
  if (!detail.value) return '立即报名'
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

const goMerchantHomepage = () => {
  if (!detail.value?.merchantId) return
  router.push(`/user-homepage/${detail.value.merchantId}`)
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
.contact-merchant-btn {
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
}

.activity-detail-page {
  position: relative;
  max-width: 1240px;
  padding: 28px 20px 104px;
  background:
    radial-gradient(circle at top left, rgba(157, 41, 41, 0.12), transparent 24%),
    radial-gradient(circle at top right, rgba(201, 145, 63, 0.12), transparent 18%),
    linear-gradient(180deg, rgba(255, 249, 241, 0.9), rgba(244, 235, 222, 0.94));
}

.activity-detail-page::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
  opacity: 0.45;
}

.header-nav,
.detail-container {
  position: relative;
  z-index: 1;
}

.header-nav {
  position: sticky;
  top: 106px;
  z-index: 10;
  padding: 16px 20px;
  border-radius: 22px;
  border: 1px solid rgba(190, 157, 124, 0.28);
  background:
    linear-gradient(135deg, rgba(157, 41, 41, 0.05), rgba(255, 255, 255, 0.86)),
    rgba(255, 251, 246, 0.86);
  box-shadow:
    0 18px 40px rgba(74, 46, 23, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.back-button {
  min-height: 42px;
  border-radius: 16px;
  font-weight: 700;
  color: #6b5949;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.back-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(74, 46, 23, 0.08);
}

.breadcrumb {
  flex-wrap: wrap;
  row-gap: 6px;
}

.breadcrumb span:first-child:hover {
  color: #9d2929;
}

.state-card,
.top-section,
.bottom-section,
.merchant-card,
.thumbnail-item {
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.state-card,
.top-section,
.bottom-section {
  border-radius: 26px;
  border: 1px solid rgba(190, 157, 124, 0.28);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 244, 238, 0.92));
  box-shadow:
    0 22px 46px rgba(74, 46, 23, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.detail-container {
  gap: 28px;
}

.top-section {
  align-items: stretch;
}

.main-cover {
  min-height: 420px;
}

.main-cover::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 46%;
  background: linear-gradient(180deg, transparent, rgba(19, 11, 6, 0.22));
  pointer-events: none;
}

.main-cover img {
  min-height: 420px;
  transition: transform 0.45s ease, filter 0.45s ease;
}

.top-section:hover .main-cover img {
  transform: scale(1.04);
  filter: saturate(1.04);
}

.status-badge {
  top: 20px;
  left: 20px;
  padding: 8px 15px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 12px 24px rgba(44, 44, 44, 0.12);
}

.thumbnails {
  gap: 10px;
  padding: 16px;
  background: rgba(248, 245, 240, 0.78);
}

.thumbnail-item {
  border-radius: 14px;
  border-width: 2px;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 10px 18px rgba(74, 46, 23, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.thumbnail-item:hover {
  transform: translateY(-1px);
}

.thumbnail-item.active {
  box-shadow: 0 14px 24px rgba(157, 41, 41, 0.16);
}

.info-wrapper {
  padding: 42px 38px;
  background:
    radial-gradient(circle at top right, rgba(201, 145, 63, 0.08), transparent 18%),
    transparent;
}

.title {
  font-size: clamp(30px, 3vw, 38px);
  line-height: 1.28;
}

.tags {
  gap: 10px;
}

.tag {
  border-radius: 999px;
  border: 1px solid rgba(157, 41, 41, 0.08);
  font-weight: 700;
}

.meta-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.meta-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 251, 246, 0.92);
  border: 1px solid rgba(217, 207, 193, 0.72);
  box-shadow: 0 12px 22px rgba(74, 46, 23, 0.04);
}

.meta-item i {
  color: #9d2929;
}

.meta-content .label {
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-size: 11px;
  font-weight: 700;
}

.price-section {
  padding-top: 26px;
}

.book-btn,
.contact-merchant-btn {
  min-height: 48px;
  border-radius: 16px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.book-btn {
  box-shadow: 0 16px 28px rgba(157, 41, 41, 0.18);
}

.book-btn:hover:not(:disabled),
.contact-merchant-btn:hover {
  transform: translateY(-1px);
}

.bottom-section {
  padding: 32px;
}

.section-block h3 {
  position: relative;
  padding-left: 16px;
  font-size: 22px;
}

.section-block h3::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 6px;
  height: 22px;
  border-radius: 999px;
  background: linear-gradient(180deg, #9d2929, #c9913f);
  transform: translateY(-50%);
}

.merchant-card {
  padding: 24px;
  border-radius: 20px;
  border: 1px solid rgba(217, 207, 193, 0.72);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(247, 240, 232, 0.94));
  box-shadow: 0 16px 28px rgba(74, 46, 23, 0.06);
}

.merchant-avatar {
  border: 3px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 26px rgba(74, 46, 23, 0.1);
}

.desc-text {
  font-size: 15px;
}

@media (max-width: 980px) {
  .meta-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .activity-detail-page {
    padding: 16px 12px 96px;
  }

  .header-nav {
    position: static;
    border-radius: 18px;
  }

  .state-card,
  .top-section,
  .bottom-section {
    border-radius: 22px;
  }

  .main-cover,
  .main-cover img {
    min-height: 280px;
  }
}
</style>
