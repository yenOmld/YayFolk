<template>
  <div class="official-page">
    <div class="page-header">
      <h2 class="page-title">非遗文化 · 官方宣传</h2>
      <p class="page-subtitle">传承与创新，让文化之光跨越山海</p>
    </div>
    <div class="hero-carousel" @mouseenter="pauseCarousel" @mouseleave="resumeCarousel">
      <div class="slides" :style="{ transform: 'translateX(' + (-currentSlide*100) + '%)'}">
        <div class="slide" v-for="(img,i) in carouselImages" :key="i">
          <img :src="img" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event, i)" />
        </div>
      </div>
      <div class="hot hot-left"></div>
      <div class="hot hot-right"></div>
      <button class="nav prev" @click.stop="prevSlide"><i class='bx bx-chevron-left'></i></button>
      <button class="nav next" @click.stop="nextSlide"><i class='bx bx-chevron-right'></i></button>
      <div class="dots">
        <span v-for="(img,i) in carouselImages" :key="'d'+i" :class="{active: i===currentSlide}" @click.stop="goSlide(i)"></span>
      </div>
    </div>
    <h3 class="section-title" style="margin: 30px auto 10px;">非遗介绍</h3>
    <div class="stars-container">
      <div class="star star-1 flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('papercut')">
        <div class="flower-mask-border"></div> <!-- 扇形窗户边框 -->
        <div class="flower-mask-cover"></div> <!-- 扇形遮罩覆盖层 -->
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="intro">
          <h4>剪纸</h4>
          <p>以纸为载体的民间装饰艺术，寓意吉祥与祝福。</p>
        </div>
        <div class="ich-sheet" v-if="modalSlug==='papercut'" @click.stop>
          <button class="sheet-close" @click.stop="closeModal"><i class='bx bx-x'></i></button>
          <h3 class="ich-title">{{ modalTitle }}</h3>
          <p class="ich-desc">{{ modalDesc }}</p>
        </div>
      </div>
      <div class="star star-2 flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('shadowplay')">
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="flower-mask-border"></div>
        <div class="intro">
          <h4>皮影</h4>
          <p>光与影的戏剧表达，纸与皮的巧妙联动。</p>
        </div>
        <div class="ich-sheet" v-if="modalSlug==='shadowplay'" @click.stop>
          <button class="sheet-close" @click.stop="closeModal"><i class='bx bx-x'></i></button>
          <h3 class="ich-title">{{ modalTitle }}</h3>
          <p class="ich-desc">{{ modalDesc }}</p>
        </div>
      </div>
      <div class="star star-3 flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('nianhua')">
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="flower-mask-border"></div>
        <div class="intro">
          <h4>年画</h4>
          <p>木版年画记录节俗记忆，守护万家团圆的色彩。</p>
        </div>
        <div class="ich-sheet" v-if="modalSlug==='nianhua'" @click.stop>
          <button class="sheet-close" @click.stop="closeModal"><i class='bx bx-x'></i></button>
          <h3 class="ich-title">{{ modalTitle }}</h3>
          <p class="ich-desc">{{ modalDesc }}</p>
        </div>
      </div>
      <div class="star star-4 flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('embroidery')">
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="flower-mask-border"></div>
        <div class="intro">
          <h4>刺绣</h4>
          <p>丝线绘纹样，针法织匠心，凝结工艺之美。</p>
        </div>
        <div class="ich-sheet" v-if="modalSlug==='embroidery'" @click.stop>
          <button class="sheet-close" @click.stop="closeModal"><i class='bx bx-x'></i></button>
          <h3 class="ich-title">{{ modalTitle }}</h3>
          <p class="ich-desc">{{ modalDesc }}</p>
        </div>
      </div>
      <div class="star star-5 flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('dragon')">
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="flower-mask-border"></div>
        <div class="intro">
          <h4>龙舞</h4>
          <p>节庆祈愿的舞乐传统，激荡人心的生活礼赞。</p>
        </div>
        <div class="ich-sheet" v-if="modalSlug==='dragon'" @click.stop>
          <button class="sheet-close" @click.stop="closeModal"><i class='bx bx-x'></i></button>
          <h3 class="ich-title">{{ modalTitle }}</h3>
          <p class="ich-desc">{{ modalDesc }}</p>
        </div>
      </div>
      <div class="star star-6 flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('kite')">
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="flower-mask-border"></div>
        <div class="intro">
          <h4>风筝</h4>
          <p>工艺与风的艺术，从指尖飞向天空的童趣。</p>
        </div>
        <div class="ich-sheet" v-if="modalSlug==='kite'" @click.stop>
          <button class="sheet-close" @click.stop="closeModal"><i class='bx bx-x'></i></button>
          <h3 class="ich-title">{{ modalTitle }}</h3>
          <p class="ich-desc">{{ modalDesc }}</p>
        </div>
      </div>
      <div class="star star-7 flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('opera')">
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="flower-mask-border"></div>
        <div class="intro">
          <h4>戏曲</h4>
          <p>唱念做打凝练百戏之美，水袖翻飞传古今。</p>
        </div>
        <div class="ich-sheet" v-if="modalSlug==='opera'" @click.stop>
          <button class="sheet-close" @click.stop="closeModal"><i class='bx bx-x'></i></button>
          <h3 class="ich-title">{{ modalTitle }}</h3>
          <p class="ich-desc">{{ modalDesc }}</p>
        </div>
      </div>
      <div class="star star-10b flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('batik')">
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="flower-mask-border"></div>
        <div class="intro">
          <h4>蜡染</h4>
          <p>以蜡为墨，绘染成画，古老技艺的现代传承。</p>
        </div>
      </div>
      <div class="star star-8 flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('ceramic')">
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="flower-mask-border"></div>
        <div class="intro">
          <h4>瓷艺</h4>
          <p>泥与火的艺术，釉色与纹样凝练东方美学。</p>
        </div>
      </div>
      <div class="star star-9 flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('woodcarving')">
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="flower-mask-border"></div>
        <div class="intro">
          <h4>木雕</h4>
          <p>刀与木的对话，纹理间诉说匠人的故事。</p>
        </div>
      </div>
      <div class="star star-10 flower-mask" @mouseenter="playVideo" @mouseleave="pauseVideo" @click="openDetail('tie-dye')">
        <img :src="chinaImg" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event)" />
        <video muted loop playsinline preload="metadata" src="/videos/中国风背景.mp4"></video>
        <div class="flower-mask-border"></div>
        <div class="intro">
          <h4>扎染</h4>
          <p>绳结与染液交织的蓝白之美，流淌生活智慧。</p>
        </div>
      </div>
      
      <div class="star star-11 flower-mask">
        <div class="red-placeholder">
          <span class="placeholder-text">敬请期待</span>
        </div>
        <div class="flower-mask-border"></div>
      </div>
    </div>
    <div v-if="modalVisible" class="ich-modal" @click.self="closeModal">
      <div class="ich-modal-content">
        <button class="modal-close" @click="closeModal">
          <i class='bx bx-x'></i>
        </button>
        <h3 class="ich-title">{{ modalTitle }}</h3>
        <p class="ich-desc">{{ modalDesc }}</p>
      </div>
    </div>
    <h3 class="section-title works-title">作品展示</h3>
    <div class="ich-marquee" @mouseenter="pauseMarquee" @mouseleave="resumeMarquee">
      <div class="marquee-track" :style="{ animationPlayState: marqueePaused ? 'paused' : 'running' }">
        <div
          v-for="(img, i) in marqueeLoop"
          :key="'m'+i"
          class="marquee-item flower-mask"
          @click="openBottomSheet(i % marqueeImages.length)"
        >
          <img :src="img" alt="" loading="lazy" referrerpolicy="no-referrer" @error="onImgError($event, i)" />
          <div class="flower-mask-border"></div>
          <div class="item-sheet" v-if="bottomSheetIndex=== (i % marqueeImages.length)">
            <button class="sheet-close" @click.stop="closeBottomSheet"><i class='bx bx-x'></i></button>
            <h4 class="sheet-title">{{ bottomTitle }}</h4>
            <p class="sheet-desc">{{ bottomDesc }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const fallbackImage = '/1.png'
const chinaImg = fallbackImage
const carouselImages = [
  '/1.png',
  '/1.png?slide=2',
  '/1.png?slide=3',
  '/1.png?slide=4'
]
const marqueeImages = [
  '/1.png?work=1',
  '/1.png?work=2',
  '/1.png?work=3',
  '/1.png?work=4',
  '/1.png?work=5',
  '/1.png?work=6'
]
const heritageMap = {
  papercut: {
    title: '剪纸',
    desc: '以纸为载体的民间装饰艺术，通过镂空与剪刻表达吉祥寓意，常见于节庆与礼俗场景。'
  },
  shadowplay: {
    title: '皮影',
    desc: '借助灯光、唱腔与操偶形成独特叙事，是口头传统、表演艺术与工艺制作的综合表达。'
  },
  nianhua: {
    title: '年画',
    desc: '木版年画以鲜明色彩记录节俗记忆，承载祈福、迎新与地方审美传统。'
  },
  embroidery: {
    title: '刺绣',
    desc: '不同针法在织物上塑造纹样与层次，体现细腻手工与东方工艺美学。'
  },
  dragon: {
    title: '龙舞',
    desc: '节庆礼俗中的集体表演形式，以腾跃翻转的动态节奏传递祈愿与欢庆。'
  },
  kite: {
    title: '风筝',
    desc: '将结构、绘制与放飞技巧结合起来，是工艺与童趣并存的传统民俗项目。'
  },
  opera: {
    title: '戏曲',
    desc: '唱念做打凝练百戏之美，以程式化表演、服饰与唱腔保留传统舞台艺术。'
  },
  ceramic: {
    title: '瓷艺',
    desc: '泥与火反复淬炼出的器物工艺，在釉色、造型与纹饰中沉淀生活智慧。'
  },
  woodcarving: {
    title: '木雕',
    desc: '以刀代笔，在木材纹理中塑造人物、花鸟与故事，展现匠人的造型能力。'
  },
  'tie-dye': {
    title: '扎染',
    desc: '通过扎结、缝缀与浸染形成层层晕染纹样，保留天然质感与生活温度。'
  },
  batik: {
    title: '蜡染',
    desc: '以蜡为媒介控制染色区域，形成独特防染纹理，是少数民族传统工艺的重要代表。'
  },
  default: {
    title: '非遗文化',
    desc: '非遗是可被感知、可被实践、也可继续生长的文化记忆，连接地方生活与代际传承。'
  }
}

const currentSlide = ref(0)
let carouselTimer = null
const modalVisible = ref(false)
const modalSlug = ref('')
const modalTitle = ref('')
const modalDesc = ref('')
const marqueePaused = ref(false)
const bottomSheetIndex = ref(-1)
const bottomTitle = ref('非遗作品')
const bottomDesc = ref('以匠心与传承为谱，书写在生活中的艺术。')
const marqueeLoop = [...marqueeImages, ...marqueeImages, ...marqueeImages, ...marqueeImages]

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % carouselImages.length
}

const prevSlide = () => {
  currentSlide.value = (currentSlide.value - 1 + carouselImages.length) % carouselImages.length
}

const goSlide = (index) => {
  currentSlide.value = index
}

const stopCarousel = () => {
  if (!carouselTimer) {
    return
  }
  clearInterval(carouselTimer)
  carouselTimer = null
}

const startCarousel = () => {
  stopCarousel()
  carouselTimer = setInterval(nextSlide, 4000)
}

const pauseCarousel = () => stopCarousel()
const resumeCarousel = () => startCarousel()

const onImgError = (event) => {
  if (event?.target) {
    event.target.src = fallbackImage
  }
}

const playVideo = (event) => {
  const video = event.currentTarget.querySelector('video')
  if (video) {
    video.play().catch(() => {})
  }
}

const pauseVideo = (event) => {
  const video = event.currentTarget.querySelector('video')
  if (video) {
    video.pause()
  }
}

const openDetail = (slug) => {
  const current = heritageMap[slug] || heritageMap.default
  modalTitle.value = current.title
  modalDesc.value = current.desc
  modalSlug.value = ''
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
  modalSlug.value = ''
}

const pauseMarquee = () => {
  marqueePaused.value = true
}

const resumeMarquee = () => {
  marqueePaused.value = false
}

const closeBottomSheet = () => {
  bottomSheetIndex.value = -1
  marqueePaused.value = false
}

const openBottomSheet = (index) => {
  if (bottomSheetIndex.value === index) {
    closeBottomSheet()
    return
  }

  bottomSheetIndex.value = index
  marqueePaused.value = true
}

onMounted(startCarousel)
onUnmounted(stopCarousel)
</script>
  
  <style scoped>
  .official-page {
    min-height: 100vh !important;
    width: 100% !important;
    /* 最底层：暗红色背景 */
    background-color: #5b0e18;
    
    /* 背景叠加：顶部装饰图 + 底部栏杆图 */
    background-image: 
      url('/videos/屋檐.png'),    /* 顶部透明装饰图 */
      url('/videos/栏杆.png');    /* 底部栏杆透明图 */
    
    background-position: 
      top center,    /* 顶部图位置 */
      bottom center; /* 栏杆图：固定在页面最底部 */
    
    background-repeat: no-repeat;
    background-size: 
      120% 1500px,    /* 屋檐图：拉宽到 120% */
      100% 1900px;    /* 栏杆图：宽度铺满 */
    
    background-attachment: scroll; /* 随页面滚动，只有滑到底才看到完整栏杆 */
    margin: 0 !important;
    padding: 50px 0 120px 0; /* 顶部向下移 100px，底部留出足够空间，避免被导航栏遮挡 */
    
    color: #333;
    display: flex;
    flex-direction: column;
    overflow-x: hidden; /* 防止横向滚动 */
  }
  
  .hero-carousel {
    width: min(1100px, 92vw);
    margin: 100px auto 2vh; /* 顶部增加 50px 间距，使轮播框向下移动 */
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 16px 40px rgba(0,0,0,0.18);
    position: relative;
    aspect-ratio: 21/9;
    background: rgba(255,255,255,0.6);
    backdrop-filter: blur(6px);
  }
  .slides {
    display: flex;
    width: 100%;
    height: 100%;
    transition: transform .6s cubic-bezier(0.16,1,0.3,1);
  }
  .slide {
    min-width: 100%;
    height: 100%;
  }
  .slide img {
    width: 100%;
    height: 100%;
    margin: 0;
    object-fit: cover;
    display: block;
    border-radius: 0;
    box-shadow: none;
  }
  .hot {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 50%;
    z-index: 1;
  }
  .hot-left { left: 0; }
  .hot-right { right: 0; }
  .nav {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    width: 44px;
    height: 44px;
    border: none;
    border-radius: 50%;
    background: rgba(255,255,255,0.8);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: 0 6px 16px rgba(0,0,0,0.15);
    opacity: 0;
    pointer-events: none;
    transition: opacity .2s ease;
    z-index: 3;
  }
  .nav.prev { left: 12px; }
  .nav.next { right: 12px; }
  .hot-left:hover ~ .nav.prev { opacity: 1; pointer-events: auto; }
  .hot-right:hover ~ .nav.next { opacity: 1; pointer-events: auto; }
  .nav.prev:hover,
  .nav.next:hover { opacity: 1; pointer-events: auto; }
  .dots {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 10px;
    display: flex;
    justify-content: center;
    gap: 8px;
  }
  .dots span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: rgba(255,255,255,0.6);
    cursor: pointer;
    transition: transform .2s ease, background .2s ease;
  }
  .dots span.active { background: #ffffff; transform: scale(1.15); }
  
  .hero {
    display: none;
  }
  .page-header {
    width: min(1100px, 92vw);
    margin: 3vh auto 1vh;
    text-align: center;
  }
  .page-title {
    font-size: clamp(48px, 7vw, 72px); /* 加大字体 */
    font-weight: 800;
    font-style: italic;
    color: #ffffff;
    margin: 0;
  }
  .page-subtitle {
    margin: 6px 0 0;
    color: #ffffff;
    opacity: 0.9;
    font-size: clamp(14px, 1.6vw, 16px);
  }
  .section-title {
    text-align: center;
    width: min(1100px, 92vw);
    margin: 270px auto 20px; /* 从上边距 220px 改为 270px，整体往下移 50px */
    font-size: clamp(24px, 3.8vw, 36px);
    font-weight: 800;
    color: #ffffff;
  }
  
  /* 作品展示标题专属样式：只移动标题，不影响其他元素 */
  .works-title {
    margin: 0px auto 30px; /* 从上边距 00px 下边距 200px 改为上边距 0 下边距 30px，大幅减小间距 */
  }
  
  .stars-container {
    width: min(1100px, 92vw);
    margin: 6vh auto 8vh;
    display: grid;
    grid-template-columns: repeat(12, 1fr);
    gap: 18px;
    padding: 0 8px;
  }
  .star {
    position: relative;
    width: 100%;
    border-radius: 14px;
    overflow: hidden;
    /* 移除长方形阴影，只保留花形阴影 */
    transition: transform .35s ease, box-shadow .35s ease;
  }
  
  /* 花形窗户遮罩效果 */
  .star.flower-mask {
    border-radius: 0; /* 移除圆角，使用花形轮廓 */
    overflow: visible; /* 允许内容溢出显示 */
    position: relative;
    background-color: transparent; /* 去掉黑色背景 */
    /* 使用 drop-shadow 创建花形阴影 */
    filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.3));
  }
  
  /* 花形窗户边框：透明底，显示边框 */
  .star.flower-mask .flower-mask-border {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 100%; /* 放大到 100%，占满容器 */
    height: 100%; /* 放大到 100%，占满容器 */
    background-image: url('/videos/花形窗户边框.png');
    background-size: contain;
    background-repeat: no-repeat;
    background-position: center;
    transform: translate(-50%, -50%); /* 居中对齐 */
    z-index: 110; /* 在最上层，高于所有内容 */
    pointer-events: none; /* 不影响点击 */
  }
  
  /* 剪纸窗户专属：使用扇形窗户边框 */
  .star.star-1.flower-mask .flower-mask-border {
    background-image: url('/videos/扇形窗户边框.PNG');
    transform: translate(-50%, -50%) scale(2.0); /* 放大 80% */
  }
  
  /* 剪纸窗户专属：使用扇形窗户遮罩 */
  .star.star-1.flower-mask > img,
  .star.star-1.flower-mask > video {
    -webkit-mask-image: url('/videos/扇形窗户.PNG');
    mask-image: url('/videos/扇形窗户.PNG');
    -webkit-mask-size: contain;
    mask-size: contain;
    -webkit-mask-repeat: no-repeat;
    mask-repeat: no-repeat;
    -webkit-mask-position: center;
    mask-position: center;
    -webkit-mask-mode: alpha;
    mask-mode: alpha;
    transform: translate(-50%, -50%) scale(2.0); /* 放大 80% */
  }
  
  /* 花形窗户内的图片/视频样式 */
  .star.flower-mask > img,
  .star.flower-mask > video {
    -webkit-mask-image: url('/videos/花形窗户.png');
    mask-image: url('/videos/花形窗户.png');
    -webkit-mask-size: contain;
    mask-size: contain;
    -webkit-mask-repeat: no-repeat;
    mask-repeat: no-repeat;
    -webkit-mask-position: center;
    mask-position: center;
    -webkit-mask-mode: alpha;
    mask-mode: alpha;
    /* 关键修改：放大尺寸，让窗户更大 */
    width: 100%; /* 放大到 100%，占满容器 */
    height: 100%; /* 放大到 100%，占满容器 */
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%) scale(1); /* 不额外缩放，使用完整尺寸 */
    object-fit: cover; /* 改为 contain，避免裁剪，优先完整显示 */
    object-position: center; /* 确保内容居中 */
  }
  
  .star.flower-mask .flower-mask-cover {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 100; /* 在最上层 */
    pointer-events: none;
  }
  
  /* 壁画窗户专属：纯红背景占位符 */
  .star.star-11.flower-mask .red-placeholder {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 100%;
    height: 100%;
    background-color: #4f0915; /* 深红背景 */
    transform: translate(-50%, -50%);
    z-index: 50;
    /* 使用花形遮罩限制显示区域 */
    -webkit-mask-image: url('/videos/花形窗户.png');
    mask-image: url('/videos/花形窗户.png');
    -webkit-mask-size: contain;
    mask-size: contain;
    -webkit-mask-repeat: no-repeat;
    mask-repeat: no-repeat;
    -webkit-mask-position: center;
    mask-position: center;
    -webkit-mask-mode: alpha;
    mask-mode: alpha;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .star.star-11.flower-mask .placeholder-text {
    font-size: 24px;
    font-weight: 800;
    color: #ffffff;
    text-align: center;
    white-space: nowrap;
  }
  
  /* 文字提示改为透明框形式 - 紧贴边框正下方 */
  .star.flower-mask .intro {
    position: absolute;
    top: 50%; /* 与边框同一起点 */
    left: 50%; /* 与边框同一起点 */
    width: 100%; /* 从 80% 改为 100%，与边框同宽 */
    transform: translate(-50%, calc(-50% + 20%)) !important; /* 居中后，向下移动边框高度 10%，紧贴边框底部 */
    background: rgba(255, 255, 255, 0); /* 完全透明背景 */
    color: #fff;
    padding: 0; /* 移除内边距，让边框紧贴文字 */
    border-radius: 0; /* 移除圆角 */
    z-index: 50;
    pointer-events: none;
    text-align: center;
    white-space: normal; /* 允许换行 */
    opacity: 0; /* 默认隐藏 */
    transition: opacity 0.3s ease; /* 只保留透明度过渡 */
  }
  
  /* 剪纸窗户专属：文字紧贴放大后的扇形边框 */
  .star.star-1.flower-mask .intro {
    transform: translate(-50%, calc(-50% - 28%)) !important; /* 向上移动，紧贴放大后的扇形边框底部 */
  }
  
  /* 鼠标悬停时显示介绍 */
  .star.flower-mask:hover .intro {
    opacity: 1; /* 显示 */
    /* transform 保持不变，由默认样式定义 */
  }
  
  .star.flower-mask .intro h4 {
    margin: 0 0 2px; /* 减小标题下边距 */
    font-size: 14px;
    font-weight: 600;
    text-shadow: 0 2px 4px rgba(0,0,0,0.5);
  }
  
  .star.flower-mask .intro p {
    margin: 0;
    font-size: 11px;
    opacity: 0.9;
    text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  }
  
  .star img,
  .star video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  .star img {
    opacity: 1;
    transition: opacity .35s ease, transform .35s ease;
  }
  .star video {
    position: absolute;
    inset: 0;
    opacity: 0;
    transition: opacity .35s ease;
  }
  .star:hover { 
    transform: translateY(-4px) scale(1.02); /* 向上移动并轻微放大 */
  }
  /* 花形窗户：悬停时放大，保持放大状态 */
  .star.flower-mask:hover {
    transform: scale(1.25) translateY(-20px); /* 从 1.2 放大到 1.25，保持向上移动 */
  }
  
  /* 剪纸窗户专属：悬停时弹性扩张，保持向下位移 */
  .star.star-1.flower-mask:hover {
    transform: scale(1.25) translateY(80px); /* 放大到 1.25 倍，保持向下 80px 位移 */
  }
  .star:hover img { opacity: 0; } /* 移除图片的 scale，避免冲突 */
  .star:hover video { opacity: 1; }
  
  /* 文字介绍不受悬停影响，保持在边框底部 */
  .star.flower-mask:hover .intro {
    opacity: 1; /* 只显示，不移动位置 */
  }
  .intro {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    padding: 14px;
    background: linear-gradient(180deg, rgba(0,0,0,0) 40%, rgba(0,0,0,0.55) 100%);
    color: #fff;
    opacity: 0;
    transform: translateY(10px);
    transition: opacity .3s ease, transform .3s ease;
    pointer-events: none;
  }
  .intro h4 {
    font-size: 16px;
    font-weight: 700;
    line-height: 1.2;
    margin: 0 0 6px 0;
  }
  .intro p {
    font-size: 13px;
    line-height: 1.6;
    margin: 0;
    opacity: 0.95;
  }
  .star:hover .intro { opacity: 1; transform: translateY(0); }
  
  /* 俄罗斯方块式错落布局 */
  /* 第一行：长条方块 */
  .star-1 { 
    grid-column: 1 / span 6; /* 从 span 4 改为 span 6，增加容器宽度 */
    aspect-ratio: 1/1; /* 正方形 */
    margin-left: 300px; /* 从 250px 改为 300px，向右移动 50px */
    transform: scale(1.2) translateY(80px); /* 放大到 120%，向下移动 100px（从 -20px 改为 +80px） */
  }
  .star-2 { 
    grid-column: 1 / span 6; /* 改为 span 6，与 star-1 相同 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  .star-3 { 
    grid-column: 7 / span 6; /* 改为 span 6，放在第二列 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  /* 第二行 */
  .star-4 { 
    grid-column: 1 / span 6; /* 改为 span 6 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  .star-5 { 
    grid-column: 7 / span 6; /* 改为 span 6 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  /* 第三行 */
  .star-6 { 
    grid-column: 1 / span 6; /* 改为 span 6 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  .star-7 { 
    grid-column: 7 / span 6; /* 改为 span 6 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  /* 第四行 */
  .star-10b { 
    grid-column: 1 / span 6; /* 改为 span 6 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  .star-8 { 
    grid-column: 7 / span 6; /* 改为 span 6 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  /* 第五行 */
  .star-9 { 
    grid-column: 1 / span 6; /* 改为 span 6 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  .star-10 { 
    grid-column: 7 / span 6; /* 改为 span 6 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  /* 第六行 */
  .star-11 { 
    grid-column: 4 / span 6; /* 从第 4 列开始，span 6 列，居中显示 */
    aspect-ratio: 1/1; /* 正方形 */
    transform: scale(1.2) translateY(-20px); /* 放大到 120% */
  }
  
  @media (max-width: 900px) {
    .stars-container { grid-template-columns: repeat(8, 1fr); gap: 16px; }
    .star-1 { grid-column: 1 / span 8; }
    .star-2 { grid-column: 1 / span 4; }
    .star-3 { grid-column: 5 / span 4; }
    .star-4 { grid-column: 1 / span 4; }
    .star-5 { grid-column: 5 / span 4; }
    .star-6 { grid-column: 1 / span 5; }
    .star-7 { grid-column: 6 / span 3; }
    .star-10b { grid-column: 1 / span 5; }
    .star-8 { grid-column: 6 / span 3; }
    .star-9 { grid-column: 1 / span 4; }
    .star-10 { grid-column: 5 / span 4; }
    .star-11 { grid-column: 1 / span 8; }
  }
  @media (max-width: 600px) {
    .stars-container { grid-template-columns: repeat(4, 1fr); gap: 12px; margin: 8vh auto 12vh; }
    .star { width: 100%; margin: 0; }
    .star-1, .star-2, .star-3, .star-4, .star-5, .star-6, .star-7,
    .star-8, .star-9, .star-10,
    .star-10b, .star-11 { grid-column: 1 / span 4; aspect-ratio: 16/10; }
    .star-1 { margin-top: 50px; }
  }
  .duotone-red {
    filter: grayscale(1) sepia(1) saturate(8) hue-rotate(-10deg) contrast(1.2) brightness(0.95);
    mix-blend-mode: multiply;
  }
  .ich-sheet {
    position: absolute;
    top: 50%; /* 与边框同一起点 */
    left: 50%; /* 与边框同一起点 */
    width: 60%; /* 使用 60% 宽度，匹配花形实际可视区域 */
    height: 60%; /* 使用 60% 高度，匹配花形实际可视区域 */
    transform: translate(-50%, -50%); /* 居中显示 */
    background: #4f0915;
    border-radius: 14px;
    box-shadow: 0 18px 40px rgba(0,0,0,0.22);
    padding: 16px 16px 14px;
    opacity: 1; /* 默认显示，由动画控制 */
    animation: sheetUp .28s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
  @keyframes sheetUp {
    0% { transform: translate(-50%, -50%) scale(0.95); opacity: 0; }
    100% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  }
  .sheet-close {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 10px;
    background: #f1f5f9;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background .2s ease, transform .2s ease;
  }
  .sheet-close:hover { background: #e2e8f0; transform: scale(1.04); }
  .ich-title {
    font-size: 20px;
    font-weight: 800;
    color: #ffffff;
    margin: 0 0 8px;
  }
  .ich-desc {
    font-size: 15px;
    line-height: 1.9;
    color: #ffffff;
    margin: 0;
  }
  .ich-modal {
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,0.4);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
    padding: 16px;
    animation: modalBackdrop .25s ease both;
  }
  .ich-modal-content {
    width: min(640px, 92vw);
    background: #4f0915;
    border-radius: 16px;
    box-shadow: 0 20px 60px rgba(0,0,0,0.28);
    padding: 24px 24px 20px;
    position: relative;
    transform: scale(0.97) translateY(4px);
    opacity: 0;
    animation: modalIn .22s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
  .modal-close {
    position: absolute;
    top: 12px;
    right: 12px;
    width: 36px;
    height: 36px;
    border: none;
    border-radius: 10px;
    background: #f9f1f1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background .2s ease, transform .2s ease;
  }
  .modal-close:hover { background: #e2e8f0; transform: scale(1.05); }
  @keyframes modalIn {
    0% { transform: scale(0.97) translateY(4px); opacity: 0; }
    100% { transform: scale(1) translateY(0); opacity: 1; }
  }
  @keyframes modalBackdrop {
    0% { opacity: 0; }
    100% { opacity: 1; }
  }
  @media (max-width: 600px) {
    .ich-modal { align-items: flex-end; padding: 0; }
    .ich-modal-content {
      width: 100vw;
      height: 100vh;
      border-radius: 0;
      box-shadow: none;
      padding: 20px 16px;
      overflow-y: auto;
      animation-duration: .28s;
    }
    .modal-close { top: 10px; right: 10px; width: 40px; height: 40px; }
  }
  .ich-marquee {
    width: 100vw; /* 使用 100vw 铺满整个视口宽度 */
    /* 固定跑马灯的上边距，不随标题变化 */
    margin: 20px 0 -100px !important; /* 从上边距 20px 下边距 0px 改为上边距 20px 下边距 -100px，让走马灯向上移动 */
    padding: 0; /* 移除 padding，避免额外间隔 */
    border-radius: 16px;
  }
  .marquee-track {
    display: flex;
    gap: 20px; /* 从 50px 改为 20px，适配更小的窗户 */
    width: max-content;
    animation: marqueeScroll 60s linear infinite; /* 从 30s 改为 60s，调慢速度 */
    will-change: transform;
  }
  @keyframes marqueeScroll {
    0% { transform: translateX(0); }
    100% { transform: translateX(-50%); }
  }
  .marquee-item {
    position: relative;
    width: 600px; /* 从 500px 改为 600px，再放大一点 */
    height: 600px; /* 从 500px 改为 600px，再放大一点 */
    border-radius: 14px;
    overflow: visible; /* 改为 visible，允许花形边框溢出 */
    box-shadow: none; /* 移除长方形阴影 */
    transition: transform .25s cubic-bezier(0.16,1,0.3,1), box-shadow .25s ease;
    cursor: pointer;
  }
  
  /* 走马灯的花形窗户格式 */
  .marquee-item.flower-mask {
    border-radius: 0; /* 移除圆角，使用花形轮廓 */
    overflow: visible; /* 允许内容溢出显示 */
    background-color: transparent; /* 去掉黑色背景 */
    box-shadow: none; /* 移除长方形阴影 */
    filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.3)); /* 使用 drop-shadow 创建花形阴影 */
  }
  
  /* 走马灯的花形窗户边框 */
  .marquee-item.flower-mask .flower-mask-border {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 100%;
    height: 100%;
    background-image: url('/videos/五边形窗户边框.PNG');
    background-size: contain; /* 使用 contain 保持比例 */
    background-repeat: no-repeat;
    background-position: center;
    transform: translate(-50%, -50%);
    z-index: 110;
    pointer-events: none;
  }
  
  /* 走马灯的花形窗户内的图片样式 */
  .marquee-item.flower-mask > img {
    -webkit-mask-image: url('/videos/五边形窗户.PNG');
    mask-image: url('/videos/五边形窗户.PNG');
    -webkit-mask-size: contain; /* 使用 contain 保持比例 */
    mask-size: contain;
    -webkit-mask-repeat: no-repeat;
    mask-repeat: no-repeat;
    -webkit-mask-position: center;
    mask-position: center;
    -webkit-mask-mode: alpha;
    mask-mode: alpha;
    width: 100%;
    height: 100%;
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%) scale(1);
    object-fit: cover;
  }
  
  /* 走马灯中的 item-sheet 使用花形遮罩限制显示区域 */
  .marquee-item.flower-mask .item-sheet {
    top: 50%; /* 与花形窗户同一起点 */
    left: 50%; /* 与花形窗户同一起点 */
    width: 100%; /* 填满容器 */
    height: 100%; /* 填满容器 */
    transform: translate(-50%, -50%); /* 居中显示，与花形窗户对齐 */
    background: rgba(79, 9, 21, 0.95); /* 半透明背景 */
    border-radius: 0; /* 移除圆角 */
    padding: 0; /* 移除内边距 */
    /* 使用五边形遮罩限制显示区域 */
    -webkit-mask-image: url('/videos/五边形窗户.PNG');
    mask-image: url('/videos/五边形窗户.PNG');
    -webkit-mask-size: contain;
    mask-size: contain;
    -webkit-mask-repeat: no-repeat;
    mask-repeat: no-repeat;
    -webkit-mask-position: center;
    mask-position: center;
    /* 内容居中 */
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .marquee-item img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  .ich-marquee:hover .marquee-track { animation-play-state: paused; }
  /* 走马灯的花形窗户悬停效果 */
  .marquee-item.flower-mask:hover {
    transform: scale(1.08); /* 轻微放大 8% */
  }
  
  /* 作品展示详情框通用样式 */
  .item-sheet {
    position: absolute;
    top: 50%; /* 与边框同一起点 */
    left: 50%; /* 与边框同一起点 */
    width: 60%; /* 使用 60% 宽度，匹配花形实际可视区域 */
    height: 60%; /* 使用 60% 高度，匹配花形实际可视区域 */
    transform: translate(-50%, -50%) scale(0.98); /* 居中显示 */
    background: #4f0915;
    color: #ffffff;
    padding: 16px;
    border-radius: 14px;
    opacity: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    animation: itemSheetOverlay .25s cubic-bezier(0.16,1,0.3,1) forwards;
    overflow: hidden; /* 确保内容不超出边界 */
  }
  @keyframes itemSheetOverlay {
    0% { transform: translate(-50%, -50%) scale(0.98); opacity: 0; }
    100% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  }
  .sheet-title { margin: 0 0 6px; font-size: 16px; font-weight: 800; color: #ffffff; }
  .sheet-desc { margin: 0; font-size: 13px; line-height: 1.7; color: #ffffff; }
  @media (max-width: 600px) {
    .ich-marquee { margin-bottom: 0px; /* 从 64px 改为 0px，减小与底部的间距 */ }
  }
  
 
  
  @media (max-width: 960px) {
    .grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }
  
  @media (max-width: 600px) {
    .grid {
      grid-template-columns: 1fr;
    }
  }
  </style>

