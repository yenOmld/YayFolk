<template>
  <div ref="sectionRef" class="heritage-3d-timeline">
    <!-- Sticky viewport containing everything visible -->
    <div class="sticky-viewport">
      <!-- 3D canvas (background) -->
      <canvas ref="canvasRef" class="three-canvas"></canvas>

      <!-- Decorative circle frame -->
      <div class="circle-decor">
        <svg viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="100" cy="100" r="94" stroke="rgba(218,165,32,0.12)" stroke-width="1" />
          <circle cx="100" cy="100" r="90" stroke="rgba(218,165,32,0.2)" stroke-width="0.5" stroke-dasharray="8 4" />
          <circle cx="100" cy="100" r="82" stroke="rgba(218,165,32,0.06)" stroke-width="2" />
          <circle cx="100" cy="100" r="74" stroke="rgba(218,165,32,0.1)" stroke-width="0.5" stroke-dasharray="3 6" />
        </svg>
      </div>

      <!-- Loading overlay -->
      <div v-if="loading" class="loading-overlay">
        <div class="loading-chip">
          <i class="bx bx-loader-alt bx-spin"></i>
          <span>加载3D模型中...</span>
        </div>
      </div>

      <!-- Horizontal track: slides left as user scrolls -->
      <div class="h-track">
        <div class="h-track-inner" ref="trackInnerRef">
          <!-- Hero panel -->
          <div class="track-hero">
            <h2 class="hero-title">
              <span class="title-line">中国世界级非遗</span>
              <span class="title-line accent">列入历程</span>
            </h2>
            <p class="hero-subtitle">
              The development history
              <br />of Chinese Intangible Cultural Heritage
            </p>
            <p class="hero-note">{{ subtitle }}</p>
          </div>

          <!-- Timeline cards -->
          <div
            v-for="(entry, i) in reversedTimelineData"
            :key="entry.year"
            class="track-card"
            :class="{ 'is-active': activeIndex === i, 'is-passed': activeIndex > i }"
            :ref="el => { if (el) cardRefs[i] = el }"
          >
            <h3 class="card-year">{{ entry.year }}</h3>
            <div class="card-divider"></div>
            <div class="card-items">
              <div
                v-for="item in entry.items"
                :key="item.name"
                class="card-item"
              >
                <span class="item-badge" :class="item.listType">{{ item.listLabel }}</span>
                <span class="item-name">{{ item.name }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Bottom progress bar -->
      <div class="progress-rail">
        <div class="progress-fill" :style="{ width: `${progressPct * 100}%` }"></div>
      </div>

      <!-- Scroll hint -->
      <div class="scroll-hint" :class="{ hidden: !showHint }">
        <span class="hint-text">scroll to explore</span>
        <span class="hint-mouse"><em class="mouse-wheel"></em></span>
      </div>

      <!-- Year indicator (floating) -->
      <div class="year-indicator" v-if="activeEntry">
        <span class="yi-year">{{ activeEntry.year }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

const props = defineProps({
  modelUrl: { type: String, default: 'https://yayfolk.bhyy.online/static/China-Dragon.glb' },
  timelineData: { type: Array, required: true },
  subtitle: {
    type: String,
    default: '截至2025年，中国共有45个项目列入UNESCO非遗名录、名册，总数居世界第一'
  }
})

const sectionRef = ref(null)
const canvasRef = ref(null)
const trackInnerRef = ref(null)
const loading = ref(true)
const activeIndex = ref(0)
const progressPct = ref(0)
const showHint = ref(true)
const cardRefs = ref({})

const reversedTimelineData = computed(() => [...props.timelineData].reverse())
const totalEntries = computed(() => reversedTimelineData.value.length)
const activeEntry = computed(() => reversedTimelineData.value[activeIndex.value] || null)

let renderer, scene, camera, model
let scrollTrigger
let animationId
let loadError = false

// ── Three.js ─────────────────────────────────────────────────

function initThree() {
  const canvas = canvasRef.value
  const viewport = canvas.parentElement
  const w = viewport.clientWidth
  const h = viewport.clientHeight

  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(w, h)
  renderer.outputColorSpace = THREE.SRGBColorSpace

  scene = new THREE.Scene()

  camera = new THREE.PerspectiveCamera(45, w / Math.max(h, 1), 0.1, 100)
  camera.position.set(1, 0.8, 7.5)
  camera.lookAt(0, 0, 0)

  const ambient = new THREE.AmbientLight(0xffffff, 0.7)
  scene.add(ambient)

  const key = new THREE.DirectionalLight(0xffffff, 2.2)
  key.position.set(5, 8, 6)
  scene.add(key)

  const rim = new THREE.DirectionalLight(0xd4a574, 1.2)
  rim.position.set(-3, 2, -4)
  scene.add(rim)

  const fill = new THREE.DirectionalLight(0x8b6914, 0.5)
  fill.position.set(0, -1, 3)
  scene.add(fill)

  const top = new THREE.DirectionalLight(0xffffff, 0.6)
  top.position.set(0, 6, 0)
  scene.add(top)
}

async function loadModel() {
  const loader = new GLTFLoader()
  try {
    const gltf = await loader.loadAsync(props.modelUrl)
    model = gltf.scene

    const box = new THREE.Box3().setFromObject(model)
    const size = box.getSize(new THREE.Vector3())
    const maxDim = Math.max(size.x, size.y, size.z)
    if (maxDim > 0) {
      const targetSize = 3.8
      model.scale.setScalar(targetSize / maxDim)
    }

    const center = box.getCenter(new THREE.Vector3())
    const s = model.scale.x || 1
    model.position.set(-center.x * s, -center.y * s, -center.z * s)

    scene.add(model)
  } catch (err) {
    console.error('Failed to load 3D model:', err)
    loadError = true
  } finally {
    loading.value = false
  }
}

// ── ScrollTrigger ────────────────────────────────────────────

function calcScrollHeight() {
  const track = trackInnerRef.value
  if (!track) return window.innerHeight * 3
  const totalW = track.scrollWidth
  const vw = window.innerWidth
  const style = window.getComputedStyle(track)
  const paddingRight = parseFloat(style.paddingRight) || 0
  const hTravel = totalW - vw + paddingRight
  const vhNeeded = Math.ceil((hTravel / vw) * 100) + 50
  return Math.max(250, vhNeeded) * (window.innerHeight / 100)
}

function setupScrollTrigger() {
  if (!window.ScrollTrigger || !sectionRef.value || !trackInnerRef.value) return

  const section = sectionRef.value
  const track = trackInnerRef.value

  // Set section height to create scroll room
  section.style.height = `${calcScrollHeight()}px`

  const totalWidth = track.scrollWidth
  const vw = window.innerWidth
  // Get the right padding to ensure last card is fully visible
  const style = window.getComputedStyle(track)
  const paddingRight = parseFloat(style.paddingRight) || 0
  const maxTranslate = Math.max(0, totalWidth - vw + paddingRight)

  scrollTrigger = window.ScrollTrigger.create({
    trigger: section,
    start: 'top top',
    end: 'bottom bottom',
    scrub: 1.5,
    onUpdate: (self) => {
      const p = self.progress
      progressPct.value = p

      // Horizontal translation
      track.style.transform = `translate3d(${-p * maxTranslate}px, 0, 0)`

      // 3D model animation — dragon flying
      if (model && !loadError) {
        // X: fly left → right across the viewport
        model.position.x = -4.5 + p * 5

        // Y: up/down wing-wave flight (2.5 full undulations), shifted downward
        model.position.y = Math.sin(p * Math.PI * 2) * 0.5 - 0.5

        // Z: gradually move away from camera (close → far) + depth sway
        const zDrift = p * 3.5
        const zSway = Math.sin(p * Math.PI * 3.2) * 0.35
        model.position.z = zDrift + zSway

        // Self rotation: slow oscillation within ±45°  →  [-π/4, π/4]
        model.rotation.y = Math.sin(p * Math.PI * 1.2) * -(Math.PI / 4)

        // Gentle pitch as dragon climbs/dives
        model.rotation.x = Math.sin(p * Math.PI * 3) * 0.15

        // Subtle banking roll
        model.rotation.z = Math.sin(p * Math.PI * 2.5) * 0.25
      }

      // Active entry: hero takes first ~10% of scroll, entries take the rest
      const heroFrac = 0.08
      const entryP = Math.max(0, Math.min(1, (p - heroFrac) / (1 - heroFrac)))
      const idx = Math.round(entryP * (totalEntries.value - 1))
      const clamped = Math.max(0, Math.min(totalEntries.value - 1, idx))
      if (activeIndex.value !== clamped) {
        activeIndex.value = clamped
      }

      showHint.value = p < 0.02
    }
  })

  window.addEventListener('resize', onResize)
}

// ── Render loop ──────────────────────────────────────────────

function animate() {
  animationId = requestAnimationFrame(animate)
  if (renderer && scene && camera) {
    renderer.render(scene, camera)
  }
}

function onResize() {
  if (!renderer || !canvasRef.value) return
  const vp = canvasRef.value.parentElement
  const w = vp.clientWidth
  const h = vp.clientHeight
  renderer.setSize(w, h)
  camera.aspect = w / Math.max(h, 1)
  camera.updateProjectionMatrix()

  if (scrollTrigger && trackInnerRef.value) {
    sectionRef.value.style.height = `${calcScrollHeight()}px`
    scrollTrigger.refresh()
  }
}

function disposeModel() {
  if (!model) return
  model.traverse((child) => {
    if (child.geometry) child.geometry.dispose()
    if (child.material) {
      const mats = Array.isArray(child.material) ? child.material : [child.material]
      mats.forEach((m) => {
        Object.values(m).forEach((v) => {
          if (v && typeof v === 'object' && v.isTexture) v.dispose()
        })
        m.dispose()
      })
    }
  })
}

onMounted(() => {
  initThree()
  loadModel()
  setTimeout(() => setupScrollTrigger(), 300)
  animate()
})

onUnmounted(() => {
  if (scrollTrigger) scrollTrigger.kill()
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', onResize)
  disposeModel()
  if (renderer) {
    renderer.dispose()
    renderer.forceContextLoss()
  }
})
</script>

<style scoped>
.heritage-3d-timeline {
  position: relative;
  width: 100%;
  background: linear-gradient(180deg, #fdf9f4 0%, #f8f4ed 100%);
}

/* ── Sticky viewport ──────────────────────────────────────── */

.sticky-viewport {
  position: sticky;
  top: 0;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  z-index: 0;
}

.three-canvas {
  display: block;
  width: 100%;
  height: 100%;
}

.circle-decor {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: min(64vw, 520px);
  height: min(64vw, 520px);
  pointer-events: none;
  z-index: 1;
}

.circle-decor svg {
  width: 100%;
  height: 100%;
}

/* ── Loading ──────────────────────────────────────────────── */

.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(8, 8, 8, 0.7);
  z-index: 5;
}

.loading-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 28px;
  border-radius: 999px;
  background: rgba(218, 165, 32, 0.1);
  border: 1px solid rgba(218, 165, 32, 0.2);
  color: #daa520;
  font-size: 14px;
  font-weight: 600;
}

.loading-chip i { font-size: 20px; }

/* ── Horizontal track ─────────────────────────────────────── */

.h-track {
  position: absolute;
  bottom: 20%;
  left: 0;
  width: 100%;
  height: 40%;
  z-index: 3;
  pointer-events: none;
}

.h-track-inner {
  display: flex;
  align-items: flex-start;
  gap: 60px;
  height: 100%;
  padding: 0 8vw;
  will-change: transform;
}

/* ── Hero panel (first item in track) ─────────────────────── */

.track-hero {
  flex-shrink: 0;
  width: min(440px, 60vw);
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
}

.hero-title {
  margin: 0 0 16px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.title-line {
  font-size: clamp(38px, 5vw, 64px);
  font-weight: 700;
  color: #2d1a1a;
  line-height: 1.15;
  letter-spacing: 0.02em;
  white-space: nowrap;
  font-family: 'ZCOOL KuaiLe', 'Ma Shan Zheng', 'STXingkai', 'KaiTi', '楷体', 'Noto Serif SC', cursive;
}

.title-line.accent { color: #daa520; }

.hero-subtitle {
  margin: 0 0 12px;
  font-size: clamp(13px, 1.8vw, 17px);
  font-weight: 400;
  color: rgba(45, 26, 26, 0.65);
  line-height: 1.6;
  font-family: 'Poppins', sans-serif;
}

.hero-note {
  margin: 0;
  font-size: 12px;
  color: rgba(45, 26, 26, 0.5);
  max-width: 380px;
  line-height: 1.8;
}

/* ── Timeline cards ───────────────────────────────────────── */

.track-card {
  flex-shrink: 0;
  width: min(340px, 50vw);
  padding: 24px 28px;
  border-radius: 20px;
  border: 1px solid rgba(45, 26, 26, 0.12);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  transition: all 0.5s ease;
  display: flex;
  flex-direction: column;
  gap: 14px;
  pointer-events: auto;
}

.track-card.is-passed {
  opacity: 0.4;
  border-color: rgba(45, 26, 26, 0.08);
}

.track-card.is-active {
  opacity: 1;
  border-color: rgba(218, 165, 32, 0.3);
  background: rgba(218, 165, 32, 0.06);
  box-shadow: 0 0 40px rgba(218, 165, 32, 0.08);
  transform: translateY(-8px);
}

.card-year {
  margin: 0;
  font-size: clamp(48px, 6vw, 72px);
  font-weight: 900;
  color: #daa520;
  line-height: 1;
  letter-spacing: -0.02em;
  font-family: 'Poppins', sans-serif;
}

.track-card.is-passed .card-year {
  color: rgba(218, 165, 32, 0.35);
}

.card-divider {
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, rgba(218, 165, 32, 0.4), transparent);
  border-radius: 1px;
}

.card-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 8px;
}

.card-items::-webkit-scrollbar {
  width: 4px;
}

.card-items::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 2px;
}

.card-items::-webkit-scrollbar-thumb {
  background: rgba(218, 165, 32, 0.3);
  border-radius: 2px;
}

.card-items::-webkit-scrollbar-thumb:hover {
  background: rgba(218, 165, 32, 0.5);
}

.card-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  line-height: 1.5;
}

.item-badge {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.03em;
  white-space: nowrap;
}

.item-badge.representative { background: rgba(218, 165, 32, 0.15); color: #daa520; }
.item-badge.urgent { background: rgba(220, 53, 69, 0.15); color: #e06060; }
.item-badge.safeguarding { background: rgba(40, 167, 69, 0.15); color: #57c070; }

.item-name {
  color: rgba(45, 26, 26, 0.6);
  font-weight: 400;
}

.track-card.is-active .item-name {
  color: rgba(45, 26, 26, 0.9);
}

/* ── Progress bar ─────────────────────────────────────────── */

.progress-rail {
  position: absolute;
  bottom: 12%;
  left: 8vw;
  right: 8vw;
  height: 1px;
  background: rgba(45, 26, 26, 0.1);
  z-index: 4;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, rgba(218, 165, 32, 0.6), #daa520);
  border-radius: 1px;
  transition: width 0.1s linear;
}

/* ── Scroll hint ──────────────────────────────────────────── */

.scroll-hint {
  position: absolute;
  bottom: 40px;
  right: 8vw;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 5;
  transition: opacity 0.5s ease;
  pointer-events: none;
}

.scroll-hint.hidden { opacity: 0; }

.hint-text {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(218, 165, 32, 0.45);
}

.hint-mouse {
  display: block;
  width: 18px;
  height: 28px;
  border: 2px solid rgba(218, 165, 32, 0.25);
  border-radius: 10px;
  position: relative;
}

.mouse-wheel {
  display: block;
  width: 2px;
  height: 5px;
  background: rgba(218, 165, 32, 0.4);
  border-radius: 1px;
  position: absolute;
  top: 5px;
  left: 50%;
  transform: translateX(-50%);
  animation: wheelHScroll 1.8s ease-in-out infinite;
}

@keyframes wheelHScroll {
  0% { top: 5px; opacity: 1; }
  100% { top: 14px; opacity: 0.15; }
}

/* ── Year indicator ───────────────────────────────────────── */

.year-indicator {
  position: absolute;
  top: 10%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3;
  pointer-events: none;
  transition: opacity 0.5s ease;
}

.yi-year {
  font-size: clamp(96px, 14vw, 180px);
  font-weight: 900;
  color: rgba(218, 165, 32, 0.1);
  line-height: 1;
  letter-spacing: -0.03em;
  font-family: 'Poppins', sans-serif;
  user-select: none;
}

/* ── Responsive ───────────────────────────────────────────── */

@media (max-width: 768px) {
  .h-track {
    bottom: 12%;
    height: 44%;
  }

  .h-track-inner {
    gap: 32px;
    padding: 0 6vw;
  }

  .track-hero {
    width: min(280px, 55vw);
  }

  .track-card {
    width: min(260px, 55vw);
    padding: 18px 20px;
    gap: 10px;
  }

  .circle-decor {
    width: min(80vw, 380px);
    height: min(80vw, 380px);
  }

  .scroll-hint {
    bottom: 28px;
    right: 6vw;
  }

  .progress-rail {
    bottom: 8%;
    left: 6vw;
    right: 6vw;
  }
}
</style>
