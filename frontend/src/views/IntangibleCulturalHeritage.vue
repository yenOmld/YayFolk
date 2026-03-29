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
              <div class="stat-number">100+</div>
              <div class="stat-label">非遗项目</div>
            </div>
            <div class="stat-item animate-item" style="animation-delay: 0.2s">
              <div class="stat-number">50+</div>
              <div class="stat-label">传承人</div>
            </div>
            <div class="stat-item animate-item" style="animation-delay: 0.3s">
              <div class="stat-number">1000+</div>
              <div class="stat-label">精品作品</div>
            </div>
          </div>
        </div>
          <div class="about-image animate-right">
            <img src="/heritage/202601-养心殿-冬季-.png" alt="关于我们" />
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
          <div class="event-item animate-up" style="animation-delay: 0.1s">
            <div class="event-image">
              <img src="https://picsum.photos/seed/event1/500/350" alt="活动1" />
              <div class="event-tag">热门</div>
            </div>
            <div class="event-content">
              <div class="event-meta">
                <span class="event-date"><i class='bx bx-calendar'></i> 2026年4月1日 - 6月30日</span>
                <span class="event-location"><i class='bx bx-map'></i> 非遗文化中心</span>
              </div>
              <h3 class="event-title">非遗文化大展</h3>
              <p class="event-desc">汇集百项非遗技艺，展现中华文明瑰宝，邀您共赏文化盛宴</p>
              <button class="event-link" @click="goToEventDetail('exhibition')">
                查看详情 <i class='bx bx-right-arrow-alt'></i>
              </button>
            </div>
          </div>
          
          <div class="event-item animate-up" style="animation-delay: 0.2s">
            <div class="event-image">
              <img src="https://picsum.photos/seed/event2/500/350" alt="活动2" />
            </div>
            <div class="event-content">
              <div class="event-meta">
                <span class="event-date"><i class='bx bx-calendar'></i> 每周六、周日</span>
                <span class="event-location"><i class='bx bx-map'></i> 非遗体验中心</span>
              </div>
              <h3 class="event-title">非遗体验工坊</h3>
              <p class="event-desc">亲手体验传统技艺，感受匠人精神，获得独特文化体验</p>
              <button class="event-link" @click="goToEventDetail('workshop')">
                查看详情 <i class='bx bx-right-arrow-alt'></i>
              </button>
            </div>
          </div>
          
          <div class="event-item animate-up" style="animation-delay: 0.3s">
            <div class="event-image">
              <img src="https://picsum.photos/seed/event3/500/350" alt="活动3" />
            </div>
            <div class="event-content">
              <div class="event-meta">
                <span class="event-date"><i class='bx bx-calendar'></i> 每月第一周周五</span>
                <span class="event-location"><i class='bx bx-map'></i> 非遗文化讲堂</span>
              </div>
              <h3 class="event-title">非遗文化讲座</h3>
              <p class="event-desc">专家学者深度解读，探索非遗奥秘，传承文化精髓</p>
              <button class="event-link" @click="goToEventDetail('lecture')">
                查看详情 <i class='bx bx-right-arrow-alt'></i>
              </button>
            </div>
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
          <div class="knowledge-card animate-item" style="animation-delay: 0.1s; cursor: pointer;" @click="openKnowledgeModal(0)" @mouseenter="handleCardMouseEnter(0)" @mouseleave="handleCardMouseLeave(0)">
            <div class="knowledge-media">
              <img :src="knowledgeData[0].image" :alt="knowledgeData[0].title" class="knowledge-image" />
              <video v-if="knowledgeData[0].video" :src="knowledgeData[0].video" class="knowledge-video" muted loop playsinline></video>
            </div>
            <div class="knowledge-icon">
              <i class='bx bx-cut'></i>
            </div>
            <h3>剪纸艺术</h3>
            <p>中国最古老的民间艺术之一，以纸为载体，通过剪刻技法创造精美图案</p>
          </div>
          <div class="knowledge-card animate-item" style="animation-delay: 0.2s; cursor: pointer;" @click="openKnowledgeModal(1)" @mouseenter="handleCardMouseEnter(1)" @mouseleave="handleCardMouseLeave(1)">
            <div class="knowledge-media">
              <img :src="knowledgeData[1].image" :alt="knowledgeData[1].title" class="knowledge-image" />
              <video v-if="knowledgeData[1].video" :src="knowledgeData[1].video" class="knowledge-video" muted loop playsinline></video>
            </div>
            <div class="knowledge-icon">
              <i class='bx bx-palette'></i>
            </div>
            <h3>景泰蓝</h3>
            <p>又称铜胎掐丝珐琅，是中国著名的特种金属工艺品之一</p>
          </div>
          <div class="knowledge-card animate-item" style="animation-delay: 0.3s; cursor: pointer;" @click="openKnowledgeModal(2)" @mouseenter="handleCardMouseEnter(2)" @mouseleave="handleCardMouseLeave(2)">
            <div class="knowledge-media">
              <img :src="knowledgeData[2].image" :alt="knowledgeData[2].title" class="knowledge-image" />
              <video v-if="knowledgeData[2].video" :src="knowledgeData[2].video" class="knowledge-video" muted loop playsinline></video>
            </div>
            <div class="knowledge-icon">
              <i class='bx bx-paint'></i>
            </div>
            <h3>苏绣</h3>
            <p>中国四大名绣之一，以精细的针法和秀丽的色彩著称</p>
          </div>
          <div class="knowledge-card animate-item" style="animation-delay: 0.4s; cursor: pointer;" @click="openKnowledgeModal(3)" @mouseenter="handleCardMouseEnter(3)" @mouseleave="handleCardMouseLeave(3)">
            <div class="knowledge-media">
              <img :src="knowledgeData[3].image" :alt="knowledgeData[3].title" class="knowledge-image" />
              <video v-if="knowledgeData[3].video" :src="knowledgeData[3].video" class="knowledge-video" muted loop playsinline></video>
            </div>
            <div class="knowledge-icon">
              <i class='bx bx-music'></i>
            </div>
            <h3>古琴艺术</h3>
            <p>中国最古老的弹拨乐器之一，被誉为"琴棋书画"四艺之首</p>
          </div>
          <div class="knowledge-card animate-item" style="animation-delay: 0.5s; cursor: pointer;" @click="openKnowledgeModal(4)" @mouseenter="handleCardMouseEnter(4)" @mouseleave="handleCardMouseLeave(4)">
            <div class="knowledge-media">
              <img :src="knowledgeData[4].image" :alt="knowledgeData[4].title" class="knowledge-image" />
              <video v-if="knowledgeData[4].video" :src="knowledgeData[4].video" class="knowledge-video" muted loop playsinline></video>
            </div>
            <div class="knowledge-icon">
              <i class='bx bx-coffee'></i>
            </div>
            <h3>茶道</h3>
            <p>通过品茶艺术来修身养性、品味人生的一种高雅文化</p>
          </div>
          <div class="knowledge-card animate-item" style="animation-delay: 0.6s; cursor: pointer;" @click="openKnowledgeModal(5)" @mouseenter="handleCardMouseEnter(5)" @mouseleave="handleCardMouseLeave(5)">
            <div class="knowledge-media">
              <img :src="knowledgeData[5].image" :alt="knowledgeData[5].title" class="knowledge-image" />
              <video v-if="knowledgeData[5].video" :src="knowledgeData[5].video" class="knowledge-video" muted loop playsinline></video>
            </div>
            <div class="knowledge-icon">
              <i class='bx bx-book'></i>
            </div>
            <h3>书法篆刻</h3>
            <p>汉字特有的传统艺术形式，融合了书写与雕刻技艺</p>
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
          <div class="gallery-item animate-item" style="animation-delay: 0.1s">
            <img src="https://picsum.photos/seed/gallery1/400/400" alt="作品1" />
            <div class="gallery-overlay">
              <h4>青花瓷瓶</h4>
              <p>景德镇传统工艺</p>
            </div>
          </div>
          <div class="gallery-item animate-item" style="animation-delay: 0.2s">
            <img src="https://picsum.photos/seed/gallery2/400/400" alt="作品2" />
            <div class="gallery-overlay">
              <h4>双面绣屏</h4>
              <p>苏绣精品</p>
            </div>
          </div>
          <div class="gallery-item animate-item" style="animation-delay: 0.3s">
            <img src="https://picsum.photos/seed/gallery3/400/400" alt="作品3" />
            <div class="gallery-overlay">
              <h4>雕漆茶具</h4>
              <p>北京雕漆工艺</p>
            </div>
          </div>
          <div class="gallery-item animate-item" style="animation-delay: 0.4s">
            <img src="https://picsum.photos/seed/gallery4/400/400" alt="作品4" />
            <div class="gallery-overlay">
              <h4>景泰蓝香炉</h4>
              <p>宫廷御制工艺</p>
            </div>
          </div>
          <div class="gallery-item animate-item" style="animation-delay: 0.5s">
            <img src="https://picsum.photos/seed/gallery5/400/400" alt="作品5" />
            <div class="gallery-overlay">
              <h4>玉雕摆件</h4>
              <p>苏州玉雕</p>
            </div>
          </div>
          <div class="gallery-item animate-item" style="animation-delay: 0.6s">
            <img src="https://picsum.photos/seed/gallery6/400/400" alt="作品6" />
            <div class="gallery-overlay">
              <h4>缂丝团扇</h4>
              <p>苏州缂丝</p>
            </div>
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
              <a href="#">首页</a>
              <a href="#events">活动宣传</a>
              <a href="#">非遗科普</a>
              <a href="#">作品展示</a>
            </div>
            <div class="footer-column">
              <h4>联系方式</h4>
              <a href="#"><i class='bx bx-phone'></i> 010-12345678</a>
              <a href="#"><i class='bx bx-envelope'></i> contact@gugong.com</a>
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
          <p>&copy; 2026 故宫·非遗文化 版权所有</p>
        </div>
      </div>
    </div>

    <!-- 非遗科普详情弹窗 -->
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
          <p>{{ selectedKnowledge.description }}</p>
          <div class="modal-details">
            <h3>历史渊源</h3>
            <p>{{ selectedKnowledge.history }}</p>
            <h3>工艺特点</h3>
            <p>{{ selectedKnowledge.features }}</p>
            <h3>文化价值</h3>
            <p>{{ selectedKnowledge.value }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { onMounted, onUnmounted, ref } from 'vue'

const router = useRouter()

const showKnowledgeModal = ref(false)
const selectedKnowledge = ref({})

const knowledgeData = [
  {
    id: 'paper-cut',
    icon: 'bx bx-cut',
    title: '剪纸艺术',
    description: '中国最古老的民间艺术之一，以纸为载体，通过剪刻技法创造精美图案',
    history: '剪纸艺术起源于汉代，距今已有两千多年历史。唐代时期剪纸艺术达到鼎盛，明清时期更是广泛流传于民间。',
    features: '剪纸主要使用剪刀或刻刀，通过镂空、阳刻、阴刻等技法，创造出丰富多样的图案。题材涵盖花鸟、人物、故事等。',
    value: '剪纸艺术是中华民族珍贵的文化遗产，体现了劳动人民的智慧和审美情趣，2009年被列入联合国教科文组织人类非物质文化遗产代表作名录。',
    image: 'https://picsum.photos/seed/papercut/400/300',
    video: '/heritage/剪纸.mp4'
  },
  {
    id: 'cloisonne',
    icon: 'bx bx-palette',
    title: '景泰蓝',
    description: '又称铜胎掐丝珐琅，是中国著名的特种金属工艺品之一',
    history: '景泰蓝起源于元代，盛行于明朝景泰年间，因釉色多以蓝色为主，故名"景泰蓝"。清代达到工艺巅峰。',
    features: '景泰蓝制作工艺复杂，包括制胎、掐丝、点蓝、烧蓝、打磨、镀金等多道工序。成品色彩艳丽、金碧辉煌。',
    value: '景泰蓝是中国宫廷艺术的代表，融合了中国传统工艺与外来技法，具有极高的艺术价值和收藏价值。',
    image: 'https://picsum.photos/seed/cloisonne/400/300',
    video: '/heritage/剪纸.mp4'
  },
  {
    id: 'su-embroidery',
    icon: 'bx bx-paint',
    title: '苏绣',
    description: '中国四大名绣之一，以精细的针法和秀丽的色彩著称',
    history: '苏绣起源于苏州，历史可追溯至春秋时期。明清时期苏绣技艺达到巅峰，成为皇家贡品。',
    features: '苏绣以针代笔，以线代墨，针法多达40余种。作品细腻逼真，色彩清雅，具有"平、齐、细、密、匀、顺、和、光"八大特点。',
    value: '苏绣是江南文化的瑰宝，体现了中国传统女红的最高成就，被誉为"东方艺术明珠"。',
    image: 'https://picsum.photos/seed/suembroidery/400/300',
    video: '/heritage/剪纸.mp4'
  },
  {
    id: 'guqin',
    icon: 'bx bx-music',
    title: '古琴艺术',
    description: '中国最古老的弹拨乐器之一，被誉为"琴棋书画"四艺之首',
    history: '古琴有三千多年历史，传说为伏羲所创。孔子、司马相如、诸葛亮等历史名人都是古琴大师。',
    features: '古琴有七弦十三徽，音色深沉悠远。演奏技法丰富，讲究"和、静、清、远、古、淡、恬、逸"的审美意境。',
    value: '古琴艺术是中国传统文化的象征，2003年被列入联合国教科文组织人类口头和非物质遗产代表作。',
    image: 'https://picsum.photos/seed/guqin/400/300',
    video: '/heritage/剪纸.mp4'
  },
  {
    id: 'tea-ceremony',
    icon: 'bx bx-coffee',
    title: '茶道',
    description: '通过品茶艺术来修身养性、品味人生的一种高雅文化',
    history: '中国茶道兴于唐，盛于宋，明清时期达到鼎盛。唐代陆羽著《茶经》，是世界上第一部茶叶专著。',
    features: '茶道讲究"和、敬、清、寂"，通过备器、择水、取火、候汤、习茶等程序，达到心灵的宁静与升华。',
    value: '茶道是中国传统文化的重要组成部分，体现了中国人的生活智慧和审美追求，影响遍及东亚乃至世界。',
    image: 'https://picsum.photos/seed/tea/400/300',
    video: '/heritage/剪纸.mp4'
  },
  {
    id: 'calligraphy',
    icon: 'bx bx-book',
    title: '书法篆刻',
    description: '汉字特有的传统艺术形式，融合了书写与雕刻技艺',
    history: '中国书法始于甲骨文，历经篆、隶、楷、行、草等书体演变。王羲之、颜真卿、苏轼等都是书法史上的巨匠。',
    features: '书法以毛笔为工具，通过笔法、字法、章法、墨法的运用，展现汉字的美感。篆刻则以刀代笔，在印章上刻写文字。',
    value: '书法篆刻是中华文化的核心艺术，被誉为"无言的诗，无行的舞，无图的画，无声的乐"。',
    image: 'https://picsum.photos/seed/calligraphy/400/300',
    video: '/heritage/剪纸.mp4'
  }
]

const openKnowledgeModal = (index) => {
  selectedKnowledge.value = knowledgeData[index]
  showKnowledgeModal.value = true
  document.body.style.overflow = 'hidden'
}

const closeKnowledgeModal = () => {
  showKnowledgeModal.value = false
  document.body.style.overflow = ''
}

const handleCardMouseEnter = (index) => {
  const video = document.querySelector(`.knowledge-card:nth-child(${index + 1}) .knowledge-video`)
  if (video) {
    video.play().catch(() => {})
  }
}

const handleCardMouseLeave = (index) => {
  const video = document.querySelector(`.knowledge-card:nth-child(${index + 1}) .knowledge-video`)
  if (video) {
    video.pause()
    video.currentTime = 0
  }
}

const goToEventDetail = (eventId) => {
  router.push(`/home/event/${eventId}`)
}

const scrollToSection = (sectionId) => {
  const element = document.getElementById(sectionId)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth' })
  }
}

let observer = null
let lastScrollY = 0

const handleParallax = () => {
  const scrollY = window.scrollY
  lastScrollY = scrollY
  
  const bannerBg = document.querySelector('.banner-bg')
  if (bannerBg) {
    bannerBg.style.transform = `translateY(${scrollY * 0.4}px)`
  }
  
  const parallaxBgs = document.querySelectorAll('.parallax-bg')
  parallaxBgs.forEach((bg, index) => {
    const speed = 0.2 + (index * 0.1)
    bg.style.transform = `translateY(${scrollY * speed}px)`
  })
}

const initObserver = () => {
  const options = {
    root: null,
    rootMargin: '0px 0px -100px 0px',
    threshold: 0.1
  }

  observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('animate-active')
      } else {
        entry.target.classList.remove('animate-active')
      }
    })
  }, options)

  const animateElements = document.querySelectorAll('.animate-up, .animate-left, .animate-right, .animate-item')
  animateElements.forEach((el) => observer.observe(el))
}

onMounted(() => {
  initObserver()
  window.addEventListener('scroll', handleParallax, { passive: true })
  handleParallax()
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
  window.removeEventListener('scroll', handleParallax)
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
  top: 100px;
  padding: 0 24px;
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
  color:#daa520;
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
}

.knowledge-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 40px rgba(139, 0, 0, 0.1);
  border-color: #4f0915;
}

.knowledge-icon {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #4f0915 0%, #6b101d 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
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

/* 弹窗样式 */
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

/* 知识卡片悬停效果 */
.knowledge-card {
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
  position: relative;
}

.knowledge-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 16px 40px rgba(139, 0, 0, 0.15);
}

.knowledge-card:active {
  transform: translateY(-4px) scale(0.98);
}

/* 知识卡片媒体容器 */
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

/* 调整知识卡片icon位置 */
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
</style>
