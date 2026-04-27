<template>
  <Teleport to="body">
    <div v-if="visible" class="vr-modal-shell" :style="{ zIndex: zIndex }">
      <div class="vr-modal-overlay" @click="emit('close')"></div>

      <div class="vr-modal-card" role="dialog" aria-modal="true" aria-label="VR Viewer">
        <button class="close-button" type="button" @click="emit('close')">
          <i class="bx bx-x"></i>
        </button>

        <div class="hero-copy">
          <h1>VR成品展示</h1>
        </div>

        <div v-if="loading" class="state-card">
          <i class="bx bx-loader-alt bx-spin"></i>
          <p>加载模型中...</p>
        </div>

        <div v-else-if="!vrModelUrl" class="state-card empty">
          <i class="bx bx-cube"></i>
          <p>当前活动暂未上传VR模型</p>
        </div>

        <div v-else class="viewer-stage">
          <model-viewer
            class="model-viewer"
            :src="vrModelUrl"
            alt="Activity VR model"
            camera-controls
            interaction-prompt="auto"
            auto-rotate
            rotation-per-second="20deg"
            shadow-intensity="1"
            exposure="1.05"
            loading="lazy"
          ></model-viewer>
        </div>

        <div class="footer-row">
          <div class="hint-chip">
            <i class="bx bx-mouse"></i>
            <span>拖拽查看</span>
          </div>
          <div class="hint-chip">
            <i class="bx bx-zoom-in"></i>
            <span>滚轮缩放</span>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getPublicActivityDetail } from '@/api/app'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  zIndex: {
    type: Number,
    default: 1300
  },
  activityId: {
    type: [String, Number],
    default: ''
  }
})

const emit = defineEmits(['close'])

const loading = ref(false)
const detail = ref(null)

const vrModelUrl = computed(() => {
  return detail.value?.vrModelUrl || ''
})

async function loadDetail() {
  if (!props.activityId) {
    return
  }

  loading.value = true
  try {
    const response = await getPublicActivityDetail(props.activityId)
    if (response.code !== 200) {
      throw new Error(response.message || '获取活动信息失败')
    }
    detail.value = response.data || null
  } catch (error) {
    console.error('Failed to load activity detail:', error)
  } finally {
    loading.value = false
  }
}

watch(() => props.activityId, loadDetail, { immediate: true })
watch(() => props.visible, (val) => {
  if (val) {
    loadDetail()
  }
})
</script>

<style scoped>
.vr-modal-shell {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1300;
  display: flex;
  align-items: center;
  justify-content: center;
}

.vr-modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(44, 22, 11, 0.45);
  backdrop-filter: blur(6px);
  animation: fadeIn 0.3s ease-out;
}

.vr-modal-card {
  position: relative;
  width: 95vw;
  max-width: 1000px;
  max-height: 90vh;
  overflow-y: auto;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(255, 251, 246, 0.96));
  border: 1px solid rgba(230, 212, 194, 0.95);
  border-radius: 28px;
  box-shadow: 0 24px 56px rgba(74, 46, 23, 0.22);
  padding: 24px;
  animation: slideUp 0.3s ease-out;
}

.close-button {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: #5a3928;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(60, 42, 28, 0.12);
  transition: transform 0.2s, box-shadow 0.2s;
  z-index: 10;
}

.close-button:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 16px rgba(60, 42, 28, 0.18);
}

.hero-copy {
  text-align: center;
  margin-bottom: 16px;
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(24px, 4vw, 36px);
  color: #2f241d;
}

.state-card {
  min-height: 400px;
  margin-top: 16px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(182, 92, 56, 0.08), rgba(233, 205, 170, 0.16));
  display: grid;
  place-items: center;
  gap: 10px;
  color: #6f5745;
}

.state-card i {
  font-size: 32px;
  color: #b65c38;
}

.viewer-stage {
  margin-top: 16px;
  border-radius: 24px;
  overflow: hidden;
  background: radial-gradient(circle at top, rgba(255, 255, 255, 0.95), rgba(250, 240, 230, 0.8));
  min-height: 480px;
}

.model-viewer {
  width: 100%;
  height: 480px;
  background: transparent;
}

.footer-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(230, 212, 194, 0.6);
}

.hint-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  background: rgba(182, 92, 56, 0.08);
  color: #b65c38;
  font-size: 13px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
