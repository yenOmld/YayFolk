<template>
  <div ref="sectionRef" class="scroll-3d-section">
    <div class="sticky-viewport">
      <canvas ref="canvasRef" class="three-canvas"></canvas>
      <div v-if="loading" class="loading-overlay">
        <div class="loading-chip">
          <i class="bx bx-loader-alt bx-spin"></i>
          <span>加载3D模型中...</span>
        </div>
      </div>
      <div class="viewport-hint">
        <i class="bx bx-mouse"></i>
        <span>滚动探索</span>
      </div>
    </div>
    <div class="scroll-spacer" :style="{ height: `${scrollVh}vh` }"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

const props = defineProps({
  modelUrl: {
    type: String,
    default: '/heritage-model.glb'
  },
  scrollVh: {
    type: Number,
    default: 300
  },
  totalRotation: {
    type: Number,
    default: Math.PI * 4
  }
})

const sectionRef = ref(null)
const canvasRef = ref(null)
const loading = ref(true)

let renderer, scene, camera, model
let scrollTrigger
let animationId
let loadError = false

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
  camera.position.set(0, 0.5, 7)
  camera.lookAt(0, 0, 0)

  const ambient = new THREE.AmbientLight(0xffffff, 0.7)
  scene.add(ambient)

  const key = new THREE.DirectionalLight(0xffffff, 2)
  key.position.set(5, 8, 5)
  scene.add(key)

  const rim = new THREE.DirectionalLight(0xd4a574, 1)
  rim.position.set(-3, 2, -3)
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
      const targetSize = 4.5
      const scale = targetSize / maxDim
      model.scale.setScalar(scale)
    }

    const center = box.getCenter(new THREE.Vector3())
    model.position.set(
      -center.x * (model.scale.x || 1),
      -center.y * (model.scale.y || 1),
      -center.z * (model.scale.z || 1)
    )

    model.traverse((child) => {
      if (child.isMesh && child.material && !child.material.name) {
        child.material = child.material.clone()
      }
    })

    scene.add(model)
  } catch (err) {
    console.error('Failed to load 3D model:', err)
    loadError = true
  } finally {
    loading.value = false
  }
}

function setupScrollTrigger() {
  const gsap = window.gsap
  if (!gsap) {
    console.warn('GSAP not found')
    return
  }

  scrollTrigger = window.ScrollTrigger.create({
    trigger: sectionRef.value,
    start: 'top top',
    end: 'bottom bottom',
    scrub: 1.8,
    onUpdate: (self) => {
      if (!model || loadError) return
      const p = self.progress

      model.rotation.y = p * props.totalRotation

      const zStart = 4
      const zEnd = -0.5
      model.position.z = zStart + (zEnd - zStart) * p

      const floatAmplitude = 0.4
      model.position.y += (Math.sin(p * Math.PI * 2) * floatAmplitude - model.position.y) * 0.1

      model.position.x = Math.sin(p * Math.PI) * 0.5
    }
  })
}

function animate() {
  animationId = requestAnimationFrame(animate)
  if (renderer && scene && camera) {
    renderer.render(scene, camera)
  }
}

function onResize() {
  if (!renderer || !canvasRef.value) return
  const viewport = canvasRef.value.parentElement
  const w = viewport.clientWidth
  const h = viewport.clientHeight
  renderer.setSize(w, h)
  camera.aspect = w / Math.max(h, 1)
  camera.updateProjectionMatrix()
}

function disposeModel() {
  if (!model) return
  model.traverse((child) => {
    if (child.geometry) child.geometry.dispose()
    if (child.material) {
      if (Array.isArray(child.material)) {
        child.material.forEach((m) => {
          Object.values(m).forEach((v) => {
            if (v && typeof v === 'object' && v.isTexture) v.dispose()
          })
          m.dispose()
        })
      } else {
        Object.values(child.material).forEach((v) => {
          if (v && typeof v === 'object' && v.isTexture) v.dispose()
        })
        child.material.dispose()
      }
    }
  })
}

onMounted(() => {
  initThree()
  loadModel()
  setupScrollTrigger()
  animate()
  window.addEventListener('resize', onResize)
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
.scroll-3d-section {
  position: relative;
  width: 100%;
  background: linear-gradient(180deg, #0d0d0d 0%, #1a1410 30%, #1a1410 70%, #0d0d0d 100%);
}

.sticky-viewport {
  position: sticky;
  top: 0;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.three-canvas {
  display: block;
  width: 100%;
  height: 100%;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(13, 13, 13, 0.6);
  z-index: 2;
}

.loading-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 24px;
  border-radius: 999px;
  background: rgba(218, 165, 32, 0.12);
  border: 1px solid rgba(218, 165, 32, 0.25);
  color: #daa520;
  font-size: 14px;
  font-weight: 600;
}

.loading-chip i {
  font-size: 20px;
}

.viewport-hint {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: rgba(218, 165, 32, 0.5);
  font-size: 13px;
  z-index: 2;
  animation: hintFloat 2.5s ease-in-out infinite;
  pointer-events: none;
}

.viewport-hint i {
  font-size: 24px;
}

@keyframes hintFloat {
  0%, 100% { transform: translateX(-50%) translateY(0); opacity: 0.5; }
  50% { transform: translateX(-50%) translateY(8px); opacity: 0.9; }
}

.scroll-spacer {
  width: 100%;
  pointer-events: none;
}

@media (max-width: 768px) {
  .viewport-hint {
    bottom: 30px;
    font-size: 12px;
  }
}
</style>
