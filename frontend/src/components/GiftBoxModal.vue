<template>
  <Teleport to="body">
    <div v-if="visible" class="scroll-shell" :style="{ zIndex }">
      <div class="scroll-overlay" @click="handleClose"></div>

      <div class="scroll-modal">
        <button class="close-btn" type="button" @click="handleClose">
          <i class="bx bx-x"></i>
        </button>

        <div class="scroll-scene" :class="{ opening: isOpening, opened: isOpened }">
          <div class="scroll-wrapper" @click="handleScrollClick">
            <div class="scroll-rod left-rod">
              <div class="rod-cap top"></div>
              <div class="rod-body"></div>
              <div class="rod-cap bottom"></div>
            </div>

            <div class="scroll-paper">
              <div class="paper-content">
                <div v-if="!isOpening && !isOpened" class="scroll-hint">
                  <span class="hint-icon">卷</span>
                  <span class="hint-text">点击展开</span>
                </div>
              </div>
            </div>

            <div class="scroll-rod right-rod">
              <div class="rod-cap top"></div>
              <div class="rod-body"></div>
              <div class="rod-cap bottom"></div>
            </div>
          </div>
        </div>

        <transition name="ar-fade">
          <div v-if="isOpened" class="ar-content">
            <div class="ar-image-frame">
              <div class="frame-corner top-left"></div>
              <div class="frame-corner top-right"></div>
              <div class="frame-corner bottom-left"></div>
              <div class="frame-corner bottom-right"></div>
              <img class="ar-hero-image" :src="arCoverUrl" :alt="badgeTitle" />
              <div class="qr-frame-container">
                <div class="qr-frame">
                  <img class="qr-image" :src="arQrUrl" alt="QR code" />
                </div>
                <span class="qr-label">扫码查看AR</span>
              </div>
            </div>
            <div class="ar-info">
              <p class="ar-eyebrow">成就</p>
              <h3>{{ badgeTitle }}</h3>
            </div>
          </div>
        </transition>
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

const isOpening = ref(false)
const isOpened = ref(false)

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
  if (newVal) {
    isOpening.value = false
    isOpened.value = false
  }
})

const handleScrollClick = () => {
  if (isOpening.value || isOpened.value) return
  isOpening.value = true
  setTimeout(() => {
    isOpened.value = true
  }, 800)
}

const handleClose = () => {
  if (isOpening.value && !isOpened.value) return
  emit('close')
}
</script>

<style scoped>
.scroll-shell {
  position: fixed;
  inset: 0;
}

.scroll-overlay {
  position: absolute;
  inset: 0;
  background: rgba(20, 18, 15, 0.75);
  backdrop-filter: blur(8px);
}

.scroll-modal {
  position: relative;
  z-index: 1;
  width: min(880px, calc(100vw - 32px));
  max-height: calc(100vh - 32px);
  margin: 16px auto;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.99), rgba(248, 240, 230, 0.98));
  box-shadow: 0 32px 64px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 24px;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(58, 45, 34, 0.15);
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #5a4a3a;
  transition: all 0.3s ease;
}

.close-btn:hover {
  transform: scale(1.1) rotate(90deg);
  background: #fff;
}

.scroll-scene {
  perspective: 1200px;
  position: relative;
  transition: all 0.5s ease;
}

.opened .scroll-scene {
  opacity: 0;
  transform: scale(0.9);
  pointer-events: none;
  position: absolute;
}

.scroll-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.scroll-wrapper:hover {
  transform: scale(1.02);
}

.scroll-rod {
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 2;
}

.rod-body {
  width: 16px;
  height: 180px;
  background: linear-gradient(90deg, #8b4513 0%, #d2691e 30%, #cd853f 50%, #d2691e 70%, #8b4513 100%);
  border-radius: 4px;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.3);
}

.rod-cap {
  width: 24px;
  height: 12px;
  background: linear-gradient(180deg, #daa520 0%, #b8860b 50%, #8b6914 100%);
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.rod-cap.top {
  border-radius: 6px 6px 4px 4px;
}

.rod-cap.bottom {
  border-radius: 4px 4px 6px 6px;
}

.scroll-paper {
  position: relative;
  width: 50px;
  height: 180px;
  background: linear-gradient(180deg, #f5f0e6 0%, #faf8f3 20%, #f5f0e6 40%, #faf8f3 60%, #f5f0e6 80%, #faf8f3 100%);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.opening .scroll-paper {
  width: 260px;
}

.paper-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.scroll-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  opacity: 1;
  transition: opacity 0.3s ease;
}

.opening .scroll-hint {
  opacity: 0;
}

.hint-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #c41e3a 0%, #8b0000 100%);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(196, 30, 58, 0.4);
}

.hint-text {
  font-size: 14px;
  color: #8b4513;
  font-weight: 600;
}

.ar-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ar-fade-enter-active {
  animation: arFadeIn 0.5s ease forwards;
}

@keyframes arFadeIn {
  0% {
    opacity: 0;
    transform: translateY(20px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.ar-image-frame {
  position: relative;
  padding: 20px;
  background: linear-gradient(135deg, #f8f4ec 0%, #fff 50%, #f8f4ec 100%);
  border-radius: 20px;
}

.frame-corner {
  position: absolute;
  width: 20px;
  height: 20px;
  border: 2px solid #c97a28;
  z-index: 2;
}

.frame-corner.top-left {
  top: 8px;
  left: 8px;
  border-right: none;
  border-bottom: none;
  border-radius: 8px 0 0 0;
}

.frame-corner.top-right {
  top: 8px;
  right: 8px;
  border-left: none;
  border-bottom: none;
  border-radius: 0 8px 0 0;
}

.frame-corner.bottom-left {
  bottom: 8px;
  left: 8px;
  border-right: none;
  border-top: none;
  border-radius: 0 0 0 8px;
}

.frame-corner.bottom-right {
  bottom: 8px;
  right: 8px;
  border-left: none;
  border-top: none;
  border-radius: 0 0 8px 0;
}

.ar-hero-image {
  display: block;
  width: 100%;
  max-height: 420px;
  object-fit: contain;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 16px 32px rgba(60, 42, 26, 0.12);
}

.qr-frame-container {
  position: absolute;
  right: 32px;
  bottom: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  z-index: 5;
}

.qr-frame {
  width: 110px;
  height: 110px;
  padding: 8px;
  background: linear-gradient(145deg, #fff 0%, #fffdf7 100%);
  border-radius: 16px;
  box-shadow:
    0 12px 24px rgba(34, 24, 18, 0.18),
    0 0 0 2px rgba(201, 122, 40, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 8px;
}

.qr-label {
  padding: 5px 12px;
  background: linear-gradient(135deg, rgba(201, 122, 40, 0.95), rgba(237, 171, 74, 0.95));
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  border-radius: 999px;
  box-shadow: 0 4px 10px rgba(201, 122, 40, 0.25);
  white-space: nowrap;
}

.ar-info {
  text-align: center;
}

.ar-eyebrow {
  margin: 0 0 6px;
  color: #b65c38;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.ar-info h3 {
  margin: 0;
  color: #2f241d;
  font-size: 22px;
}

@media (max-width: 640px) {
  .scroll-modal {
    width: calc(100vw - 16px);
    margin: 8px auto;
    padding: 24px 16px;
    border-radius: 20px;
  }

  .rod-body {
    height: 140px;
  }

  .scroll-paper {
    height: 140px;
  }

  .opening .scroll-paper {
    width: 200px;
  }

  .ar-hero-image {
    max-height: 300px;
  }

  .qr-frame-container {
    right: 20px;
    bottom: 20px;
  }

  .qr-frame {
    width: 90px;
    height: 90px;
  }

  .ar-info h3 {
    font-size: 18px;
  }
}
</style>