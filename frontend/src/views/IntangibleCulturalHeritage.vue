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
                <div class="stat-number">{{ events.length || '100+' }}</div>
                <div class="stat-label">非遗项目</div>
              </div>
              <div class="stat-item animate-item" style="animation-delay: 0.2s">
                <div class="stat-number">{{ knowledgeData.length || '50+' }}</div>
                <div class="stat-label">传承人</div>
              </div>
              <div class="stat-item animate-item" style="animation-delay: 0.3s">
                <div class="stat-number">{{ galleryWorks.length || '1000+' }}</div>
                <div class="stat-label">精品作品</div>
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
              <p>{{ item.subtitle }}</p>
            </div>
          </div>
          <div v-if="galleryWorks.length === 0" class="no-data">
            <p>暂无作品信息</p>
          </div>
        </div>
      </div>
    </div>

    <div class="footer-section animate-section">
      <div class="container">
        <div class="footer-content">
          <div class="footer-brand">
            <h3>非遗文化</h3>
            <p>传承千年匠心，守护中华文明</p>
          </div>
          <div class="footer-links">
            <div class="footer-column">
              <h4>快速链接</h4>
              <a href="#banner">首页</a>
              <a href="#events">活动宣传</a>
              <a href="#knowledge">非遗科普</a>
              <a href="#gallery">作品展示</a>
            </div>
            <div class="footer-column">
              <h4>联系方式</h4>
              <a href="#"><i class='bx bx-phone'></i> 010-12345678</a>
              <a href="#"><i class='bx bx-envelope'></i> contact@yayfolk.com</a>
              <a href="#"><i class='bx bx-map'></i> 北京市东城区景山前街4号</a>
            </div>
            <div class="footer-column">
              <h4>关注我们</h4>
              <div class="social-links">
                <a href="#"><i class='bx bxl-wechat'></i></a>
                <a href="#"><i class='bx bxl-weibo'></i></a>
                <a href="#"><i class='bx bxl-qq'></i></a>
              </div>
            </div>
          </div>
        </div>
        <div class="footer-bottom">
          <p>&copy; 2026 YayFolk 非遗文化 版权所有</p>
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
            <h3>代表性传承人</h3>
            <p>{{ selectedKnowledge.representativeInheritor || '暂无代表性传承人信息' }}</p>
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
    signup: 'Open',
    ongoing: 'Ongoing',
    ended: 'Ended',
    full: 'Full'
  }
  return statusMap[status] || 'Popular'
}

const formatDateTime = (value) => {
  return value ? new Date(value).toLocaleString('zh-CN') : 'Time TBD'
}

const formatLocation = (item) => {
  return [item.locationProvince, item.locationCity, item.locationDistrict, item.locationDetail].filter(Boolean).join(' / ') || 'Location TBD'
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
    
    events.value = (data.activities || []).slice(0, 3).map((item) => ({
      ...item,
      title: item.title || 'Untitled activity',
      description: item.content || item.subtitle || 'No activity description',
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
      title: item.title || 'Untitled work',
      subtitle: item.description || item.content || 'Tap to view work details',
      coverImage: item.coverImage || item.images || fallbackCover
    }))
  } catch (error) {
    console.error('Failed to load homepage official content', error)
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
  if (id) router.push(`/share?postId=${id}`)
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
  background: linear-gradient(180deg, transparent 0%, rgba(139, 0, 0, 0.9) 100%);
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
  font-size: 20px;
  font-weight: 600;
  color: #daa520;
  margin-bottom: 8px;
}

.gallery-overlay p {
  font-size: 14px;
  color: #fff;
}

.footer-section {
  background: #1a1a1a;
  color: #fff;
  padding: 80px 0 0;
}

.footer-content {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 80px;
  padding-bottom: 60px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.footer-brand h3 {
  font-size: 24px;
  font-weight: 700;
  color: #daa520;
  margin-bottom: 12px;
}

.footer-brand p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

.footer-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
}

.footer-column h4 {
  font-size: 16px;
  font-weight: 600;
  color: #daa520;
  margin-bottom: 20px;
}

.footer-column a {
  display: block;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  margin-bottom: 12px;
  text-decoration: none;
  transition: color 0.3s ease;
}

.footer-column a:hover {
  color: #daa520;
}

.footer-column a i {
  margin-right: 8px;
}

.social-links {
  display: flex;
  gap: 16px;
}

.social-links a {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  transition: all 0.3s ease;
}

.social-links a:hover {
  background: #daa520;
  color: #4f0915;
}

.footer-bottom {
  text-align: center;
  padding: 24px 0;
}

.footer-bottom p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
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
</style>
