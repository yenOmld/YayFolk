<template>
  <div class="ai-step-page">
    <div class="bg-orb orb-left"></div>
    <div class="bg-orb orb-right"></div>

    <div class="page-topbar">
      <button class="back-btn" type="button" @click="goBack">
        <i class="bx bx-arrow-back"></i>
      </button>
    </div>

    <section class="hero-panel">
      <div>
        <p class="hero-eyebrow">AI 非遗大片 · 第一步</p>
        <h1>先上传一张原图</h1>
      </div>
      <div class="step-track">
        <span class="active">1 上传</span>
        <span>2 风格</span>
        <span>3 发布</span>
      </div>
    </section>

    <section class="single-shell">
      <article class="glass-card upload-card">
        <div class="card-head">
          <div>
            <span class="card-index">01</span>
            <h2>上传原图</h2>
          </div>
          <small>{{ currentStatus }}</small>
        </div>

        <div class="media-card">
          <div
            class="media-frame"
            :class="{ active: dragActive }"
            @click="triggerImageUpload"
            @dragover.prevent="dragActive = true"
            @dragleave.prevent="dragActive = false"
            @drop.prevent="handleDrop"
          >
            <input ref="imageInput" type="file" accept="image/*" hidden @change="handleImageUpload" />
            <div v-if="!previewUrl" class="media-empty">
              <i class="bx bx-image-add"></i>
              <strong>点击上传</strong>
              <p>支持单张图片</p>
            </div>
            <img v-else class="media-preview" :src="previewUrl" alt="原图预览" />
          </div>
        </div>

        <div class="action-row">
          <button class="primary-btn" type="button" :disabled="!canContinue" @click="goNext">下一步</button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getAiHeritageSourceFile,
  getAiHeritageSourcePreviewUrl,
  getAiHeritageReturnPath,
  clearAiHeritageReturnPath,
  readAiHeritageDraft,
  setAiHeritageSourceFile,
  writeAiHeritageDraft
} from '../../utils/aiHeritage'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const router = useRouter()
const route = useRoute()

const imageInput = ref(null)
const dragActive = ref(false)
const file = ref(null)
const localPreviewUrl = ref('')
const draft = ref(readAiHeritageDraft())

const previewUrl = computed(() => localPreviewUrl.value || draft.value.sourceImageUrl || '')
const canContinue = computed(() => Boolean(file.value || getAiHeritageSourceFile() || draft.value.sourceImageUrl))
const currentStatus = computed(() => {
  if (file.value || getAiHeritageSourceFile() || draft.value.sourceImageUrl) return '已选择'
  return '待上传'
})

function triggerImageUpload() {
  imageInput.value?.click?.()
}

function handleDrop(event) {
  dragActive.value = false
  handleFiles(event.dataTransfer?.files)
}

function handleImageUpload(event) {
  handleFiles(event.target?.files)
  if (event.target) event.target.value = ''
}

function handleFiles(files) {
  const nextFile = files?.[0]
  if (!nextFile) return
  if (!String(nextFile.type || '').startsWith('image/')) {
    notify.warning('请上传图片文件')
    return
  }

  if (localPreviewUrl.value) {
    URL.revokeObjectURL(localPreviewUrl.value)
  }

  file.value = nextFile
  const preview = setAiHeritageSourceFile(nextFile)
  localPreviewUrl.value = preview || ''
  draft.value = writeAiHeritageDraft({ sourceImageUrl: '', generatedImageUrl: '' })
}

async function goNext() {
  if (!canContinue.value) {
    notify.warning('请先上传原图')
    return
  }
  router.push({ name: 'create-ai-heritage-style' })
}

function goBack() {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }
  const returnPath = getAiHeritageReturnPath()
  if (returnPath) {
    clearAiHeritageReturnPath()
    router.push(returnPath)
    return
  }
  router.back()
}

onMounted(() => {
  draft.value = readAiHeritageDraft()
  localPreviewUrl.value = getAiHeritageSourcePreviewUrl() || draft.value.sourceImageUrl || ''
})
</script>

<style scoped>
.ai-step-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 24px 24px 110px;
  background:
    radial-gradient(circle at top left, rgba(145, 56, 31, 0.14), transparent 30%),
    radial-gradient(circle at top right, rgba(15, 118, 110, 0.1), transparent 24%),
    linear-gradient(180deg, #f8f1e8 0%, #fffaf3 50%, #fbf3e8 100%);
}

.bg-orb {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.48;
  pointer-events: none;
}

.orb-left { left: -140px; top: -120px; background: rgba(157, 41, 41, 0.22); }
.orb-right { right: -140px; bottom: 80px; background: rgba(15, 118, 110, 0.16); }

.page-topbar,
.hero-panel,
.glass-card {
  max-width: 1120px;
  margin: 0 auto;
}

.page-topbar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.hero-panel,
.glass-card {
  border: 1px solid rgba(182, 143, 106, 0.24);
  border-radius: 28px;
  box-shadow: 0 18px 48px rgba(92, 61, 39, 0.09);
  backdrop-filter: blur(18px);
}

.hero-panel {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(249, 241, 229, 0.92));
  margin-bottom: 18px;
}

.back-btn {
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 14px;
  background: rgba(141, 35, 35, 0.1);
  color: #8d2323;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 22px;
}

.hero-eyebrow {
  margin: 0 0 10px;
  color: #8d2323;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero-panel h1 {
  margin: 0;
  font-size: clamp(28px, 4vw, 48px);
  color: #2d2118;
  line-height: 1.08;
}

.step-track {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.step-track span {
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #8b7662;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(182, 143, 106, 0.2);
}

.step-track .active {
  color: #fff;
  background: linear-gradient(135deg, #8d2323, #c2410c);
  border-color: transparent;
}

.single-shell {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
}

.upload-card {
  width: min(1120px, 100%);
  padding: 22px;
  background: rgba(255, 252, 246, 0.92);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.card-index {
  display: inline-block;
  color: #8d2323;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.card-head h2 {
  margin: 4px 0 0;
  color: #2d2118;
  font-size: 20px;
}

.card-head small {
  color: #8b7662;
  font-size: 12px;
}

.media-card {
  border: 1px solid rgba(182, 143, 106, 0.24);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  overflow: hidden;
}

.media-frame {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  cursor: pointer;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 240, 229, 0.94));
}

.media-frame.active {
  box-shadow: 0 0 0 4px rgba(141, 35, 35, 0.08);
  outline: 1px dashed rgba(141, 35, 35, 0.4);
  outline-offset: -1px;
}

.media-empty {
  text-align: center;
  color: #7f6b58;
}

.media-empty i {
  display: block;
  font-size: 60px;
  margin-bottom: 10px;
  color: #8d2323;
}

.media-empty strong {
  display: block;
  color: #2d2118;
  font-size: 18px;
}

.media-empty p {
  margin: 8px 0 0;
  font-size: 12px;
}

.media-preview {
  max-width: 100%;
  max-height: 540px;
  width: auto;
  height: auto;
  object-fit: contain;
  display: block;
  border-radius: 18px;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.primary-btn {
  min-width: 180px;
  border: none;
  border-radius: 18px;
  padding: 14px 18px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(135deg, #8d2323 0%, #c2410c 100%);
  box-shadow: 0 14px 28px rgba(141, 35, 35, 0.24);
}

.primary-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

@media (max-width: 900px) {
  .hero-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-row {
    flex-direction: column;
  }

  .primary-btn {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .ai-step-page {
    padding: 14px 14px 96px;
  }

  .hero-panel,
  .upload-card {
    border-radius: 22px;
    padding: 18px;
  }

  .media-frame {
    min-height: 280px;
  }
}
</style>