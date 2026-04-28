<template>
  <Teleport to="body">
    <div v-if="visible" class="scroll-overlay" :style="{ zIndex }" @click="handleOverlayClick">
      <div class="scroll-modal">
        <button class="scroll-close" type="button" @click="handleClose">×</button>
        
        <div class="scroll-container">
          <div class="scroll-top"></div>
          <div class="scroll-content">
            <div class="scroll-bg"></div>
            <img :src="arCoverUrl" alt="AR封面" class="scroll-cover">
            <div class="scroll-bottom-content">
              <div class="scroll-text-section">
                <h2 class="scroll-title">恭喜获得「{{ badgeTitle }}」徽章！</h2>
                <p class="scroll-description">扫描二维码，体验AR互动，解锁更多惊喜！</p>
              </div>
              <div class="scroll-qr-section">
                <img :src="arQrUrl" alt="AR二维码" class="scroll-qr-image">
                <div class="scroll-qr-label">AR二维码</div>
              </div>
            </div>
          </div>
          <div class="scroll-bottom"></div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  badge: {
    type: Object,
    default: null
  },
  zIndex: {
    type: Number,
    default: 2100
  }
})

const emit = defineEmits(['close'])

const scrollContentRef = ref(null)

const badgeTitle = computed(() => props.badge?.name || props.badge?.badgeName || '成就')

const getBadgeIndex = () => {
  const code = String(props.badge?.code || '')
  if (!code) return '1'
  const codeToIndex = {
    'first-order': '1',
    'first-booking': '1',
    'check-in': '2',
    'deep-explorer': '3',
    'storyteller': '4',
    'partner-host': '4',
    'collector': '5',
    'wanderer': '6'
  }
  if (codeToIndex[code]) return codeToIndex[code]
  const num = code.match(/\d+/)
  return num ? num[0] : '1'
}

const arCoverUrl = computed(() => `/uploads/demo/AR/ar-cover-${getBadgeIndex()}.png`)
const arQrUrl = computed(() => `/uploads/demo/AR/ar-qr-${getBadgeIndex()}.png`)

watch(() => props.visible, (newVal) => {
  if (newVal && scrollContentRef.value) {
    scrollContentRef.value.style.animation = 'none'
    scrollContentRef.value.offsetHeight
    scrollContentRef.value.style.animation = 'scrollOpen 1.2s ease-out forwards'
  }
})

const handleClose = () => {
  emit('close')
}

const handleOverlayClick = (e) => {
  if (e.target === e.currentTarget) {
    handleClose()
  }
}
</script>

<style scoped>
.scroll-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2100;
  padding: 20px;
}

.scroll-modal {
  background: transparent;
  max-width: 400px;
  width: 100%;
  padding: 0;
  position: relative;
  overflow: visible;
}

.scroll-close {
  position: absolute;
  top: -50px;
  right: -10px;
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(139, 115, 85, 0.9);
  color: white;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  z-index: 100;
}

.scroll-close:hover {
  background: #8B7355;
  transform: rotate(90deg);
}

.scroll-container {
  position: relative;
  width: 100%;
}

.scroll-top {
  position: relative;
  height: 36px;
  background: linear-gradient(180deg, #D4B896 0%, #B89B76 50%, #D4B896 100%);
  border-radius: 3px;
  margin: 0 8px;
  box-shadow: 0 3px 10px rgba(0,0,0,0.4), inset 0 1px 0 rgba(255,255,255,0.3);
  z-index: 2;
}

.scroll-top::before,
.scroll-top::after {
  content: '';
  position: absolute;
  width: 28px;
  height: 36px;
  background: linear-gradient(180deg, #A88868 0%, #8B7355 50%, #A88868 100%);
  border-radius: 3px;
  top: 0;
}

.scroll-top::before {
  left: -23px;
  box-shadow: -3px 3px 8px rgba(0,0,0,0.4), inset 1px 0 0 rgba(255,255,255,0.2);
}

.scroll-top::after {
  right: -23px;
  box-shadow: 3px 3px 8px rgba(0,0,0,0.4), inset -1px 0 0 rgba(255,255,255,0.2);
}

.scroll-bottom {
  position: relative;
  height: 36px;
  background: linear-gradient(180deg, #D4B896 0%, #B89B76 50%, #D4B896 100%);
  border-radius: 3px;
  margin: 0 8px;
  box-shadow: 0 -3px 10px rgba(0,0,0,0.4), inset 0 -1px 0 rgba(255,255,255,0.3);
  z-index: 2;
}

.scroll-bottom::before,
.scroll-bottom::after {
  content: '';
  position: absolute;
  width: 28px;
  height: 36px;
  background: linear-gradient(180deg, #A88868 0%, #8B7355 50%, #A88868 100%);
  border-radius: 3px;
  top: 0;
}

.scroll-bottom::before {
  left: -23px;
  box-shadow: -3px -3px 8px rgba(0,0,0,0.4), inset 1px 0 0 rgba(255,255,255,0.2);
}

.scroll-bottom::after {
  right: -23px;
  box-shadow: 3px -3px 8px rgba(0,0,0,0.4), inset -1px 0 0 rgba(255,255,255,0.2);
}

.scroll-content {
  background: linear-gradient(90deg, #E8D4BC 0%, #F5E6D3 10%, #F5E6D3 90%, #E8D4BC 100%);
  margin: 0 12px;
  padding: 25px 20px;
  position: relative;
  overflow: hidden;
  max-height: 0;
  opacity: 0;
  animation: scrollOpen 1.2s ease-out forwards;
  box-shadow: inset 0 0 30px rgba(0,0,0,0.1), 0 0 20px rgba(0,0,0,0.2);
  border-left: 3px solid #D4B896;
  border-right: 3px solid #D4B896;
}

@keyframes scrollOpen {
  0% {
    max-height: 0;
    opacity: 0;
  }
  100% {
    max-height: 600px;
    opacity: 1;
  }
}

.scroll-bg {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 200%;
  background: url("/uploads/demo/texture.webp") 50% 0/auto 50%;
  mix-blend-mode: multiply;
  opacity: 0.3;
  animation: scrollBg 8s linear infinite;
  pointer-events: none;
}

@keyframes scrollBg {
  0% {
    transform: translateY(-50%);
  }
  100% {
    transform: translateY(0%);
  }
}

.scroll-cover {
  width: 100%;
  height: auto;
  display: block;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
  margin-bottom: 15px;
}

.scroll-bottom-content {
  display: flex;
  gap: 20px;
  align-items: center;
}

.scroll-text-section {
  flex: 1;
}

.scroll-title {
  font-size: 18px;
  font-weight: 700;
  color: #4A3728;
  margin-bottom: 10px;
  font-family: 'Noto Sans SC', sans-serif;
}

.scroll-description {
  font-size: 13px;
  color: #6B5344;
  line-height: 1.6;
  margin-bottom: 14px;
}

.scroll-qr-section {
  text-align: center;
}

.scroll-qr-image {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  border: 3px solid #8B7355;
  margin-bottom: 8px;
  background: white;
}

.scroll-qr-label {
  font-size: 12px;
  color: #6B5344;
  font-weight: 500;
}

@media (max-width: 640px) {
  .scroll-modal {
    max-width: 100%;
  }
  
  .scroll-bottom-content {
    flex-direction: column;
    text-align: center;
  }
  
  .scroll-qr-section {
    order: -1;
  }
  
  .scroll-close {
    right: 0;
  }
}
</style>