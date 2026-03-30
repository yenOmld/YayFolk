<template>
  <div class="promotion-page">
    <div class="banner-section" id="banner">
      <div class="banner-bg"></div>
      <div class="banner-content">
        <div class="banner-subtitle">传承千年文化</div>
        <h1 class="banner-title">非遗文化</h1>
        <p class="banner-desc">守护中华文明瑰宝，弘扬传统匠心精神</p>
        <div class="banner-btns">
          <button class="btn-primary" @click="scrollToSection('about')">了解更多</button>
          <button class="btn-secondary" @click="scrollToSection('events')">查看活动</button>
        </div>
      </div>
      <div class="scroll-hint">
        <span>向下滚动</span>
        <i class='bx bx-chevron-down'></i>
      </div>
    </div>

    <div class="about-section animate-section" id="about">
      <div class="parallax-bg parallax-bg-1"></div>
      <div class="container">
        <div class="section-header">
          <span class="section-label">关于我们</span>
          <h2 class="section-title">传承非遗，守护文明</h2>
        </div>
        <div class="about-content">
          <div class="about-text animate-left">
            <p>我们致力于保护和传承非物质文化遗产，通过展览、教育、研究等多种方式，让更多人了解和热爱非遗文化。</p>
            <p>我们汇集了来自全国各地的非遗技艺，包括传统工艺、戏曲、音乐、舞蹈等多个门类，为您呈现一场文化盛宴。</p>
            <div class="about-stats">
              <div class="stat-item animate-item" style="animation-delay: 0.1s">
                <div class="stat-number">{{ homepageStats.activities }}</div>
                <div class="stat-label">活动数量</div>
              </div>
              <div class="stat-item animate-item" style="animation-delay: 0.2s">
                <div class="stat-number">{{ homepageStats.heritages }}</div>
                <div class="stat-label">非遗数量</div>
              </div>
              <div class="stat-item animate-item" style="animation-delay: 0.3s">
                <div class="stat-number">{{ homepageStats.works }}</div>
                <div class="stat-label">作品数量</div>
              </div>
            </div>
          </div>
          <div class="about-image animate-right">
            <img :src="'/heritage/about-img.png'" alt="关于我们" />
          </div>
        </div>
      </div>
    </div>

    <div class="features-section animate-section">
      <div class="parallax-bg parallax-bg-2"></div>
      <div class="container">
        <div class="section-header centered animate-up">
          <span class="section-label">核心板块</span>
          <h2 class="section-title">探索非遗文化</h2>
        </div>
        <div class="features-grid">
          <div class="feature-item animate-item" style="animation-delay: 0.1s; cursor: pointer;" @click="scrollToSection('events')">
            <div class="feature-icon">
              <i class='bx bx-calendar-event'></i>
            </div>
            <h3>活动宣传</h3>
            <p>精彩活动预告，邀您共赏非遗文化盛宴</p>
          </div>
          <div class="feature-item animate-item" style="animation-delay: 0.2s; cursor: pointer;" @click="scrollToSection('knowledge')">
            <div class="feature-icon">
              <i class='bx bx-book-reader'></i>
            </div>
            <h3>非遗科普</h3>
            <p>深入了解非遗知识，传承传统文化精髓</p>
          </div>
          <div class="feature-item animate-item" style="animation-delay: 0.3s; cursor: pointer;" @click="scrollToSection('gallery')">
            <div class="feature-icon">
              <i class='bx bx-gift'></i>
            </div>
            <h3>作品展示</h3>
            <p>欣赏非遗精品，感受匠心独运的魅力</p>
          </div>
        </div>
      </div>
    </div>

    <div class="events-section animate-section" id="events">
      <div class="parallax-bg parallax-bg-3"></div>
      <div class="container">
        <div class="section-header centered animate-up">
          <span class="section-label">精彩活动</span>
          <h2 class="section-title">活动宣传</h2>
        </div>
        <div class="events-list">
          <div v-for="(item, index) in events" :key="item.id || index" class="event-item animate-up" :style="{ animationDelay: `${(index + 1) * 0.1}s` }">
            <div class="event-image">
              <img :src="item.coverImage || fallbackCover" :alt="item.title || `活动${index + 1}`" />
              <div class="event-tag">{{ item.statusLabel }}</div>
            </div>
            <div class="event-content">
              <div class="event-meta">
                <span class="event-date"><i class='bx bx-calendar'></i> {{ item.dateText }}</span>
                <span class="event-location"><i class='bx bx-map'></i> {{ item.locationText }}</span>
              </div>
              <h3 class="event-title">{{ item.title }}</h3>
              <p class="event-desc">{{ item.description }}</p>
              <button class="event-link" @click="goToActivity(item.id)">
                查看详情 <i class='bx bx-right-arrow-alt'></i>
              </button>
            </div>
          </div>
          <div v-if="events.length === 0" class="no-data">
            <p>暂无活动信息</p>
          </div>
        </div>
      </div>
    </div>

    <div class="knowledge-section animate-section" id="knowledge">
      <div class="parallax-bg parallax-bg-4"></div>
      <div class="container">
        <div class="section-header centered animate-up">
          <span class="section-label">非遗知识</span>
          <h2 class="section-title">非遗科普</h2>
        </div>
        <div class="knowledge-list">
          <div v-for="(item, index) in knowledgeData" :key="item.id || index" class="knowledge-card animate-item" :style="{ animationDelay: `${(index + 1) * 0.1}s`, cursor: 'pointer' }" @click="openKnowledgeModal(index)" @mouseenter="handleCardMouseEnter(index)" @mouseleave="handleCardMouseLeave(index)">
            <div class="knowledge-media">
              <img :src="item.image" :alt="item.title" class="knowledge-image" />
              <video v-if="item.video" :src="item.video" class="knowledge-video" muted loop playsinline></video>
            </div>
            <div class="knowledge-icon">
              <i :class="item.icon"></i>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </div>
          <div v-if="knowledgeData.length === 0" class="no-data">
            <p>暂无非遗科普信息</p>
          </div>
        </div>
      </div>
    </div>

    <div class="gallery-section animate-section" id="gallery">
      <div class="parallax-bg parallax-bg-5"></div>
      <div class="container">
        <div class="section-header centered animate-up">
          <span class="section-label">精品展示</span>
          <h2 class="section-title">非遗作品展示</h2>
        </div>
        <div class="gallery-grid">
          <div v-for="(item, index) in galleryWorks" :key="item.id || index" class="gallery-item animate-item" :style="{ animationDelay: `${(index + 1) * 0.1}s` }" @click="goToWork(item.id)">
            <img :src="item.coverImage || fallbackCover" :alt="item.title || `作品${index + 1}`" />
            <div class="gallery-overlay">
              <h4>{{ item.title }}</h4>
            </div>
          </div>
          <div v-if="galleryWorks.length === 0" class="no-data">
            <p>暂无作品信息</p>
          </div>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="showKnowledgeModal" @click="closeKnowledgeModal">
      <div class="modal-content" @click.stop>
        <button class="modal-close" @click="closeKnowledgeModal">
          <i class='bx bx-x'></i>
        </button>
        <div class="modal-header">
          <div class="modal-icon">
            <i :class="selectedKnowledge.icon"></i>
          </div>
          <h2>{{ selectedKnowledge.title }}</h2>
        </div>
        <div class="modal-body">
          <p>{{ selectedKnowledge.introduction || selectedKnowledge.description || '暂无简介信息' }}</p>
          <div class="modal-details">
            <h3>基础信息</h3>
            <p>{{ heritageBaseInfo }}</p>
            <h3>历史渊源</h3>
            <p>{{ selectedKnowledge.history || '暂无历史信息' }}</p>
            <h3>传承价值</h3>
            <p>{{ selectedKnowledge.inheritanceValue || '暂无传承价值信息' }}</p>
            <h3>代表性非遗数量</h3>
            <p>{{ selectedKnowledge.representativeInheritor || '暂无代表性非遗数量信息' }}</p>
            <h3>相关诗词</h3>
            <p>{{ formatKnowledgeList(selectedKnowledge.relatedPoems, '暂无相关诗词信息') }}</p>
            <h3>相关节气</h3>
            <p>{{ formatKnowledgeList(selectedKnowledge.relatedSolarTerms, '暂无相关节气信息') }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getHomepageOfficialContents } from '@/api/app.js'

const router = useRouter()
const fallbackCover = 'https://picsum.photos/seed/heritage-home/800/600'
const heritageIcons = ['bx bx-cut', 'bx bx-palette', 'bx bx-paint', 'bx bx-music', 'bx bx-coffee', 'bx bx-book']

const events = ref([])
const knowledgeData = ref([])
const galleryWorks = ref([])
const homepageStats = ref({ activities: 0, heritages: 0, works: 0 })
const showKnowledgeModal = ref(false)
const selectedKnowledge = ref({})

const aboutImage = computed(() => knowledgeData.value[0]?.image || '/videos/202601-鍏诲績娈?鍐-.png')
const heritageBaseInfo = computed(() => {
  const item = selectedKnowledge.value || {}
  const parts = [
    item.category ? `分类：${item.category}` : '',
    item.subcategory ? `子类：${item.subcategory}` : '',
    item.region ? `地区：${item.region}` : '',
    item.level ? `级别：${item.level}` : '',
    item.dynasty ? `朝代：${item.dynasty}` : '',
    typeof item.viewCount === 'number' ? `浏览量：${item.viewCount}` : ''
  ].filter(Boolean)
  return parts.join(' / ') || '暂无基础信息'
})

const mapEventStatus = (status) => {
  const statusMap = {
    signup: '开放报名',
    ongoing: '进行中',
    ended: '已结束',
    full: '已满'
  }
  return statusMap[status] || '热门'
}

const formatDateTime = (value) => {
  return value ? new Date(value).toLocaleString('zh-CN') : '时间待定'
}

const formatLocation = (item) => {
  return [item.locationProvince, item.locationCity, item.locationDistrict, item.locationDetail].filter(Boolean).join(' / ') || '位置待定'
}

const formatKnowledgeList = (value, fallback = '暂无相关信息') => {
  if (Array.isArray(value)) {
    const text = value.filter(Boolean).join('、')
    return text || fallback
  }
  if (typeof value === 'string') {
    return value.trim() || fallback
  }
  return fallback
}

const loadHomepageData = async () => {
  try {
    const res = await getHomepageOfficialContents()
    const data = res.data || {}
    homepageStats.value = {
      activities: Number(data.stats?.activities || 0),
      heritages: Number(data.stats?.heritages || 0),
      works: Number(data.stats?.works || 0)
    }
    
    events.value = (data.activities || []).slice(0, 3).map((item) => ({
      ...item,
      title: item.title || '未命名活动',
      description: item.content || item.subtitle || '暂无活动描述',
      dateText: `${formatDateTime(item.startTime)} - ${formatDateTime(item.endTime)}`,
      locationText: formatLocation(item),
      statusLabel: mapEventStatus(item.status),
      coverImage: item.coverImage || fallbackCover
    }))
    
    knowledgeData.value = (data.heritages || []).slice(0, 6).map((item, index) => ({
      ...item,
      icon: heritageIcons[index % heritageIcons.length],
      title: item.title || item.name || '未命名非遗',
      description: item.description || item.introduction || '暂无非遗简介',
      image: item.coverImage || item.imageList?.[0] || item.images || fallbackCover,
      video: item.videoUrl || ''
    }))
    
    galleryWorks.value = (data.works || []).slice(0, 6).map((item) => ({
      ...item,
      title: item.title || '未命名作品',
      subtitle: item.description || item.content || '点击查看作品详情',
      coverImage: item.coverImage || item.images || fallbackCover
    }))
  } catch (error) {
    console.error('加载首页官方内容失败', error)
  }
}

const openKnowledgeModal = (index) => {
  selectedKnowledge.value = knowledgeData.value[index] || {}
  showKnowledgeModal.value = true
  document.body.style.overflow = 'hidden'
}

const closeKnowledgeModal = () => {
  showKnowledgeModal.value = false
  document.body.style.overflow = ''
}

const handleCardMouseEnter = (index) => {
  const video = document.querySelector(`.knowledge-card:nth-child(${index + 1}) .knowledge-video`)
  if (video) video.play().catch(() => {})
}

const handleCardMouseLeave = (index) => {
  const video = document.querySelector(`.knowledge-card:nth-child(${index + 1}) .knowledge-video`)
  if (video) {
    video.pause()
    video.currentTime = 0
  }
}

const goToActivity = (id) => {
  if (id) router.push(`/activity/${id}`)
}

const goToWork = (id) => {
  if (id) router.push(`/home/discover?postId=${id}`)
}

const scrollToSection = (sectionId) => {
  const element = document.getElementById(sectionId)
  if (element) element.scrollIntoView({ behavior: 'smooth' })
}

let observer = null

const handleParallax = () => {
  const scrollY = window.scrollY
  const bannerBg = document.querySelector('.banner-bg')
  if (bannerBg) bannerBg.style.transform = `translateY(${scrollY * 0.4}px)`
  
  const parallaxBgs = document.querySelectorAll('.parallax-bg')
  parallaxBgs.forEach((bg, index) => {
    const speed = 0.2 + (index * 0.1)
    bg.style.transform = `translateY(${scrollY * speed}px)`
  })
}

const initObserver = () => {
  observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('animate-active')
      } else {
        entry.target.classList.remove('animate-active')
      }
    })
  }, {
    root: null,
    rootMargin: '0px 0px -100px 0px',
    threshold: 0.1
  })
  
  document.querySelectorAll('.animate-up, .animate-left, .animate-right, .animate-item').forEach((el) => {
    observer.observe(el)
  })
}

onMounted(async () => {
  await loadHomepageData()
  initObserver()
  window.addEventListener('scroll', handleParallax, { passive: true })
  handleParallax()
})

onUnmounted(() => {
  if (observer) observer.disconnect()
  window.removeEventListener('scroll', handleParallax)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.promotion-page {
  min-height: 100vh;
  background: #fff;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
  z-index: 1;
}

.banner-section {
  position: relative;
  height: 100vh;
  min-height: 600px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow: hidden;
}

.banner-bg {
  position: absolute;
  top: -20%;
  left: 0;
  right: 0;
  bottom: -20%;
  background: url('/heritage/爱上紫禁城.jpg') center/cover no-repeat;
  will-change: transform;
}

.banner-bg::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(79, 9, 21, 0.6);
}

.banner-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
  max-width: 1000px;
  padding: 0 24px;
  top: 100px;
}

.banner-subtitle {
  font-size: 18px;
  letter-spacing: 8px;
  color: #daa520;
  margin-bottom: 16px;
}

.banner-title {
  font-size: 180px;
  font-weight: 900;
  margin-bottom: 24px;
  color: #ffffff;
  text-shadow: 2px 2px 8px rgba(0, 0, 0, 0.3);
  letter-spacing: 24px;
  white-space: nowrap;
  font-family: 'FangSong', '仿宋', 'STFangsong', '华文仿宋', serif;
  opacity: 0.65;
}

.banner-desc {
  font-size: 20px;
  color: #daa520;
  margin-bottom: 40px;
  line-height: 1.8;
}

.banner-btns {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.btn-primary {
  padding: 14px 40px;
  background: #daa520;
  color: #4f0915;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(218, 165, 32, 0.3);
}

.btn-secondary {
  padding: 14px 40px;
  background: transparent;
  color: #daa520;
  border: 2px solid #daa520;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background: #daa520;
  color: #4f0915;
}

.scroll-hint {
  position: absolute;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  animation: bounce 2s infinite;
  z-index: 2;
}

.scroll-hint i {
  font-size: 24px;
}

@keyframes bounce {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(10px); }
}

.section-header {
  margin-bottom: 60px;
}

.section-header.centered {
  text-align: center;
}

.section-label {
  display: inline-block;
  font-size: 14px;
  letter-spacing: 4px;
  color: #4f0915;
  margin-bottom: 12px;
  font-weight: 600;
}

.section-title {
  font-size: 42px;
  font-weight: 700;
  color: #333;
  margin-bottom: 16px;
}

.about-section {
  position: relative;
  padding: 120px 0;
  background: #fdf5e6;
  overflow: hidden;
}

.features-section {
  position: relative;
  overflow: hidden;
}

.events-section {
  position: relative;
  overflow: hidden;
}

.knowledge-section {
  position: relative;
  overflow: hidden;
}

.gallery-section {
  position: relative;
  overflow: hidden;
}

.parallax-bg {
  position: absolute;
  top: -30%;
  left: 0;
  right: 0;
  bottom: -30%;
  z-index: 0;
  will-change: transform;
  pointer-events: none;
}

.parallax-bg-1 {
  background: radial-gradient(circle at 20% 50%, rgba(79, 9, 21, 0.05) 0%, transparent 50%);
}

.parallax-bg-2 {
  background: radial-gradient(circle at 80% 30%, rgba(218, 165, 32, 0.08) 0%, transparent 50%);
}

.parallax-bg-3 {
  background: radial-gradient(circle at 30% 70%, rgba(79, 9, 21, 0.06) 0%, transparent 50%);
}

.parallax-bg-4 {
  background: radial-gradient(circle at 70% 60%, rgba(218, 165, 32, 0.07) 0%, transparent 50%);
}

.parallax-bg-5 {
  background: radial-gradient(circle at 50% 20%, rgba(79, 9, 21, 0.05) 0%, transparent 50%);
}

.about-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 80px;
  align-items: center;
}

.about-text p {
  font-size: 16px;
  color: #666;
  line-height: 1.8;
  margin-bottom: 20px;
}

.about-stats {
  display: flex;
  gap: 60px;
  margin-top: 40px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 48px;
  font-weight: 700;
  color: #4f0915;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.about-image {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(139, 0, 0, 0.15);
}

.about-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
}

.features-section {
  padding: 120px 0;
  background: #fff;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
}

.feature-item {
  text-align: center;
  padding: 48px 32px;
  background: #fdf5e6;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.feature-item:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 20px 40px rgba(139, 0, 0, 0.15);
  background: #fff5e6;
}

.feature-item:active {
  transform: translateY(-4px) scale(0.98);
}

.feature-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #4f0915 0%, #6b101d 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
}

.feature-icon i {
  font-size: 36px;
  color: #daa520;
}

.feature-item h3 {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.feature-item p {
  font-size: 15px;
  color: #666;
  line-height: 1.6;
}

.events-section {
  padding: 120px 0;
  background: #fdf5e6;
}

.events-list {
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.event-item {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 40px;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(139, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.event-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 50px rgba(139, 0, 0, 0.12);
}

.event-image {
  position: relative;
  height: 100%;
  min-height: 300px;
}

.event-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.event-tag {
  position: absolute;
  top: 20px;
  left: 20px;
  background: #4f0915;
  color: #daa520;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.event-content {
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.event-meta {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.event-date,
.event-location {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #4f0915;
}

.event-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.event-desc {
  font-size: 16px;
  color: #666;
  line-height: 1.8;
  margin-bottom: 24px;
}

.event-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #4f0915;
  font-size: 16px;
  font-weight: 600;
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.event-link:hover {
  gap: 16px;
}

.knowledge-section {
  padding: 120px 0;
  background: #fff;
}

.knowledge-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.knowledge-card {
  padding: 40px 32px;
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.02) 0%, rgba(255, 215, 0, 0.02) 100%);
  border-radius: 16px;
  border: 1px solid rgba(139, 0, 0, 0.1);
  transition: all 0.3s ease;
  text-align: center;
  cursor: pointer;
  overflow: hidden;
  position: relative;
}

.knowledge-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 16px 40px rgba(139, 0, 0, 0.15);
  border-color: #4f0915;
}

.knowledge-card:active {
  transform: translateY(-4px) scale(0.98);
}

.knowledge-media {
  position: relative;
  width: 100%;
  height: 200px;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
}

.knowledge-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.5s ease;
}

.knowledge-video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.5s ease;
}

.knowledge-card:hover .knowledge-image {
  opacity: 0;
}

.knowledge-card:hover .knowledge-video {
  opacity: 1;
}

.knowledge-icon {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #4f0915 0%, #6b101d 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  position: relative;
  z-index: 2;
}

.knowledge-icon i {
  font-size: 32px;
  color: #daa520;
}

.knowledge-card h3 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.knowledge-card p {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.gallery-section {
  padding: 120px 0;
  background: #fdf5e6;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.gallery-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
}

.gallery-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.gallery-item:hover img {
  transform: scale(1.1);
}

.gallery-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.6) 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 24px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.gallery-item:hover .gallery-overlay {
  opacity: 1;
}

.gallery-overlay h4 {
  font-size: 18px;
  font-weight: 600;
  color: #e2bc5aff;
  margin-bottom: 8px;
}

.gallery-overlay p {
  font-size: 14px;
  color: #fff;
}



.no-data {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}

.animate-up {
  opacity: 0;
  transform: translateY(80px);
  transition: opacity 1s cubic-bezier(0.4, 0, 0.2, 1), transform 1s cubic-bezier(0.4, 0, 0.2, 1);
}

.animate-up.animate-active {
  opacity: 1;
  transform: translateY(0);
}

.animate-left {
  opacity: 0;
  transform: translateX(-80px);
  transition: opacity 1s cubic-bezier(0.4, 0, 0.2, 1), transform 1s cubic-bezier(0.4, 0, 0.2, 1);
}

.animate-left.animate-active {
  opacity: 1;
  transform: translateX(0);
}

.animate-right {
  opacity: 0;
  transform: translateX(80px);
  transition: opacity 1s cubic-bezier(0.4, 0, 0.2, 1), transform 1s cubic-bezier(0.4, 0, 0.2, 1);
}

.animate-right.animate-active {
  opacity: 1;
  transform: translateX(0);
}

.animate-item {
  opacity: 0;
  transform: translateY(50px);
  transition: opacity 0.8s cubic-bezier(0.4, 0, 0.2, 1), transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.animate-item.animate-active {
  opacity: 1;
  transform: translateY(0);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: linear-gradient(135deg, #fff 0%, #fdf5e6 100%);
  border-radius: 20px;
  max-width: 700px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
  animation: slideUp 0.4s ease;
  box-shadow: 0 25px 80px rgba(79, 9, 21, 0.4);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-close {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #4f0915;
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.3s ease;
  z-index: 10;
}

.modal-close:hover {
  background: #6b101d;
  transform: rotate(90deg);
}

.modal-header {
  text-align: center;
  padding: 48px 32px 32px;
  background: linear-gradient(135deg, #4f0915 0%, #6b101d 100%);
  border-radius: 20px 20px 0 0;
}

.modal-icon {
  width: 100px;
  height: 100px;
  background: rgba(218, 165, 32, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
}

.modal-icon i {
  font-size: 48px;
  color: #daa520;
}

.modal-header h2 {
  font-size: 32px;
  color: #daa520;
  margin: 0;
  font-weight: 700;
}

.modal-body {
  padding: 32px;
}

.modal-body > p {
  font-size: 16px;
  color: #333;
  line-height: 1.8;
  margin-bottom: 32px;
  text-align: center;
}

.modal-details h3 {
  font-size: 20px;
  color: #4f0915;
  margin: 24px 0 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-details h3::before {
  content: '';
  width: 4px;
  height: 20px;
  background: #daa520;
  border-radius: 2px;
}

.modal-details p {
  font-size: 15px;
  color: #555;
  line-height: 1.8;
  margin: 0 0 16px 12px;
}

.promotion-page {
  background:
    radial-gradient(circle at top left, rgba(157, 41, 41, 0.1), transparent 20%),
    radial-gradient(circle at top right, rgba(201, 145, 63, 0.12), transparent 18%),
    linear-gradient(180deg, rgba(255, 249, 241, 0.96), rgba(244, 235, 222, 0.92));
}

.container {
  max-width: 1240px;
}

.banner-section {
  min-height: 720px;
}

.banner-bg::before {
  background:
    linear-gradient(180deg, rgba(38, 17, 14, 0.24), rgba(52, 16, 23, 0.62)),
    radial-gradient(circle at top, rgba(218, 165, 32, 0.16), transparent 30%);
}

.banner-content {
  max-width: 1080px;
}

.banner-subtitle {
  font-weight: 700;
  text-transform: uppercase;
}

.banner-title {
  text-shadow: 0 18px 34px rgba(0, 0, 0, 0.3);
}

.banner-desc {
  max-width: 720px;
  margin-left: auto;
  margin-right: auto;
}

.btn-primary,
.btn-secondary {
  min-height: 52px;
  padding: 0 34px;
  border-radius: 999px;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.btn-primary {
  box-shadow: 0 18px 30px rgba(218, 165, 32, 0.28);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.section-header {
  margin-bottom: 54px;
}

.section-label {
  position: relative;
  padding-left: 18px;
}

.section-label::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #9d2929, #c9913f);
  transform: translateY(-50%);
  box-shadow: 0 0 0 6px rgba(157, 41, 41, 0.08);
}

.section-title {
  font-size: clamp(32px, 3vw, 46px);
  letter-spacing: -0.03em;
  color: #2f241d;
}

.about-section,
.events-section,
.gallery-section {
  background:
    linear-gradient(180deg, rgba(255, 250, 244, 0.78), rgba(247, 238, 226, 0.92));
}

.features-section,
.knowledge-section {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(250, 246, 239, 0.9));
}

.about-image,
.feature-item,
.event-item,
.knowledge-card,
.gallery-item,
.modal-content {
  border: 1px solid rgba(190, 157, 124, 0.24);
  box-shadow:
    0 22px 46px rgba(74, 46, 23, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.68);
}

.about-image {
  border-radius: 26px;
}

.about-image img {
  border-radius: 26px;
}

.about-text p {
  color: #695647;
}

.stat-item {
  min-width: 124px;
  padding: 14px 18px;
  border-radius: 18px;
  background: rgba(255, 251, 246, 0.76);
  border: 1px solid rgba(190, 157, 124, 0.18);
}

.feature-item {
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 243, 236, 0.96));
}

.feature-item:hover {
  box-shadow: 0 28px 50px rgba(74, 46, 23, 0.14);
}

.feature-icon {
  box-shadow:
    0 18px 30px rgba(79, 9, 21, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

.event-item {
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 244, 238, 0.94));
}

.event-image::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 40%;
  background: linear-gradient(180deg, transparent, rgba(17, 10, 7, 0.18));
}

.event-tag {
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 12px 20px rgba(44, 44, 44, 0.14);
}

.event-content {
  padding: 42px;
}

.event-link {
  width: fit-content;
  min-height: 44px;
  padding: 0 16px;
  border-radius: 999px;
  background: rgba(157, 41, 41, 0.06);
}

.event-link:hover {
  gap: 12px;
  background: rgba(157, 41, 41, 0.1);
}

.knowledge-card {
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 243, 236, 0.94));
}

.knowledge-card:hover {
  box-shadow: 0 28px 50px rgba(74, 46, 23, 0.14);
}

.knowledge-media,
.gallery-item {
  border-radius: 20px;
}

.knowledge-icon {
  box-shadow:
    0 16px 28px rgba(79, 9, 21, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

.gallery-item {
  box-shadow: 0 20px 40px rgba(74, 46, 23, 0.08);
}

.gallery-overlay {
  padding: 28px;
}

.modal-overlay {
  background:
    radial-gradient(circle at top, rgba(157, 41, 41, 0.18), transparent 30%),
    rgba(19, 10, 6, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.modal-content {
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 243, 236, 0.96));
}

.modal-close {
  box-shadow: 0 14px 24px rgba(44, 44, 44, 0.16);
}

@media (max-width: 1024px) {
  .banner-title {
    font-size: 120px;
    letter-spacing: 12px;
  }

  .about-content,
  .event-item {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .features-grid,
  .knowledge-list,
  .gallery-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .banner-section {
    min-height: 620px;
  }

  .banner-content {
    top: 88px;
  }

  .banner-title {
    font-size: 72px;
    letter-spacing: 8px;
    white-space: normal;
  }

  .banner-desc {
    font-size: 17px;
  }

  .banner-btns {
    flex-direction: column;
    align-items: center;
  }

  .about-section,
  .features-section,
  .events-section,
  .knowledge-section,
  .gallery-section {
    padding: 88px 0;
  }

  .about-stats,
  .event-meta {
    flex-direction: column;
    gap: 12px;
  }

  .features-grid,
  .knowledge-list,
  .gallery-grid {
    grid-template-columns: 1fr;
  }

  .feature-item,
  .knowledge-card,
  .event-content {
    padding: 26px 22px;
  }

  .modal-content {
    border-radius: 24px;
  }

  .modal-header,
  .modal-body {
    padding-left: 22px;
    padding-right: 22px;
  }
}
</style>



