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
        <p class="hero-eyebrow">{{ isMerch ? 'AI 非遗文创 · 第 3 步' : 'AI 非遗大片 · 第 3 步' }}</p>
        <h1>生成结果</h1>
        <p class="hero-desc">确认成品后，可先保存到本地，再去发现页发布分享。</p>
      </div>
      <div class="step-track">
        <span>1 上传</span>
        <span>{{ isMerch ? '2 文创' : '2 风格' }}</span>
        <span class="active">3 发布</span>
      </div>
    </section>

    <section class="single-shell">
      <article class="glass-card publish-card">
        <div class="media-card">
          <!-- 3D模型显示 -->
          <div v-if="preview3DMode && model3DUrl" class="media-frame model-viewer-wrapper">
            <model-viewer
              :src="model3DUrl"
              camera-controls
              auto-rotate
              rotation-per-second="30deg"
              exposure="1"
              shadow-intensity="0.6"
              environment-image="neutral"
              class="model-viewer"
              alt="3D 模型"
            ></model-viewer>
          </div>
          <!-- 生成结果图 -->
          <div v-else class="media-frame media-frame-preview result-frame" :class="{ ready: Boolean(generatedImageUrl) }">
            <img v-if="generatedImageUrl" class="media-preview" :src="generatedImageUrl" alt="AI 生成结果" />
            <div v-else class="media-empty">
              <i class="bx bx-images"></i>
              <strong>{{ isMerch ? selectedMerchType?.name : selectedStyle?.name }}</strong>
              <p>生成后会在这里展示成品画面</p>
            </div>
          </div>

          <!-- 3D生成进度 -->
          <transition name="fade">
            <div v-if="gen3DLoading" class="progress-overlay">
              <div class="progress-chip gen3d-chip">
                <span class="progress-dot"></span>
                <strong>{{ gen3DProgressLabel }}</strong>
              </div>
              <div class="progress-track">
                <div class="progress-fill gen3d-fill" :style="{ width: `${gen3DProgressValue}%` }"></div>
                <div class="progress-sheen"></div>
              </div>
              <p class="progress-text">{{ gen3DProgressDetail }}</p>
            </div>
          </transition>
        </div>

        <!-- 预览模式切换 + 3D操作 -->
        <div v-if="generatedImageUrl" class="three-d-section">
          <div v-if="model3DUrl" class="preview-mode-tabs">
            <button class="mode-tab" :class="{ active: !preview3DMode }" @click="preview3DMode = false">效果图</button>
            <button class="mode-tab" :class="{ active: preview3DMode }" @click="preview3DMode = true">3D预览</button>
          </div>
          <div class="three-d-actions">
            <button
              v-if="!model3DUrl"
              class="gen3d-btn"
              type="button"
              :disabled="gen3DLoading"
              @click="generate3D"
            >
              <i v-if="gen3DLoading" class="bx bx-loader-alt bx-spin"></i>
              <i v-else class="bx bx-cube"></i>
              {{ gen3DLoading ? '生成中' : '图生3D' }}
            </button>
            <button v-if="model3DUrl" class="download-3d-btn" type="button" @click="download3DModel">
              <i class="bx bx-download"></i>
              下载3D模型
            </button>
          </div>
        </div>

        <div class="action-row">
          <button class="ghost-btn" type="button" :disabled="savingLocal || !generatedImageUrl" @click="saveLocal">
            <i v-if="savingLocal" class="bx bx-loader-alt bx-spin"></i>
            {{ savingLocal ? '保存中' : '保存本地' }}
          </button>
          <button class="submit-btn" type="button" :disabled="sharing || !generatedImageUrl" @click="goShare">
            <i v-if="sharing" class="bx bx-loader-alt bx-spin"></i>
            {{ sharing ? '跳转中' : '去分享' }}
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import '@google/model-viewer'
import { generate3DFromImage } from '../../../api/app'
import {
  getAiHeritageSourceFile,
  getPosterStyleById,
  getMerchTypeById,
  readAiHeritageDraft,
  writeAiHeritageDraft,
  getAiHeritageReturnPath,
  clearAiHeritageReturnPath
} from '../../../utils/aiHeritage'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const router = useRouter()
const route = useRoute()

const draft = ref(readAiHeritageDraft())
const savingLocal = ref(false)
const sharing = ref(false)

// 3D 生成相关
const gen3DLoading = ref(false)
const model3DUrl = ref('')
const preview3DMode = ref(false)
const gen3DProgressValue = ref(0)
const gen3DProgressLabel = ref('准备中')
const gen3DProgressDetail = ref('正在生成3D模型')
const gen3DProgressTimers = []

const isMerch = computed(() => Boolean(draft.value.merchType))
const generatedImageUrl = computed(() => draft.value.generatedImageUrl || draft.value.merchGeneratedImageUrl || '')
const selectedStyle = computed(() => getPosterStyleById(draft.value.posterStyle))
const selectedMerchType = computed(() => getMerchTypeById(draft.value.merchType))

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

function downloadImage(url, filename) {
  try {
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = filename
    anchor.target = '_blank'
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
  } catch (error) {
    throw new Error('下载失败')
  }
}

function saveLocal() {
  if (!generatedImageUrl.value) {
    notify.warning('请先生成成品')
    return
  }

  savingLocal.value = true
  try {
    writeAiHeritageDraft({
      ...draft.value,
      savedAt: Date.now()
    })
    let filename
    if (isMerch.value) {
      filename = `AI非遗文创-${selectedMerchType.value?.name || '定制'}.png`
    } else {
      filename = `AI非遗大片-${selectedStyle.value?.name || '定制'}.png`
    }
    downloadImage(generatedImageUrl.value, filename)
    notify.success('已保存到本地')
  } catch (error) {
    notify.error(error?.message || '保存失败，请稍后重试')
  } finally {
    savingLocal.value = false
  }
}

async function goShare() {
  if (!generatedImageUrl.value) {
    notify.warning('请先生成成品')
    return
  }

  sharing.value = true
  try {
    writeAiHeritageDraft({
      ...draft.value,
      generatedImageUrl: generatedImageUrl.value,
      savedAt: Date.now()
    })
    await router.push({
      name: 'discover',
      query: {
        page: 'post',
        source: 'ai-heritage'
      }
    })
  } finally {
    sharing.value = false
  }
}

async function generate3D() {
  if (!generatedImageUrl.value) {
    notify.warning('请先生成效果图')
    return
  }

  gen3DLoading.value = true
  startGen3DProgress()

  try {
    const response = await generate3DFromImage({
      imageUrl: generatedImageUrl.value
    })

    if (response.code !== 200 || !response.data?.modelUrl) {
      throw new Error(response.message || '3D 生成失败')
    }

    model3DUrl.value = `/api/ai/proxy-3d?url=${encodeURIComponent(response.data.modelUrl)}`
    preview3DMode.value = true
    notify.success('3D 模型生成成功')
  } catch (error) {
    notify.error(error?.message || '3D 生成失败，请重试')
    console.error(error)
  } finally {
    gen3DLoading.value = false
    stopGen3DProgress()
    gen3DProgressValue.value = 0
  }
}

function startGen3DProgress() {
  stopGen3DProgress()
  gen3DProgressValue.value = 5
  gen3DProgressLabel.value = '图生3D'
  gen3DProgressDetail.value = '正在上传图片'
  gen3DProgressTimers.push(setInterval(() => {
    const ceiling = gen3DLoading.value ? 92 : 100
    if (gen3DProgressValue.value < ceiling) {
      gen3DProgressValue.value = Math.min(ceiling, gen3DProgressValue.value + Math.max(1, Math.round((ceiling - gen3DProgressValue.value) / 6)))
    }
  }, 300))
  const stages = [
    { label: '图生3D', detail: '识别主体特征' },
    { label: '图生3D', detail: '构建3D几何体' },
    { label: '图生3D', detail: '渲染纹理材质' },
    { label: '图生3D', detail: '模型即将生成' }
  ]
  let stageIndex = 0
  gen3DProgressTimers.push(setInterval(() => {
    stageIndex = (stageIndex + 1) % stages.length
    gen3DProgressLabel.value = stages[stageIndex].label
    gen3DProgressDetail.value = stages[stageIndex].detail
  }, 1400))
}

function stopGen3DProgress() {
  while (gen3DProgressTimers.length) {
    clearInterval(gen3DProgressTimers.pop())
  }
}

async function download3DModel() {
  if (!model3DUrl.value) return
  try {
    const resp = await fetch(model3DUrl.value)
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `yayfolk-3d-${Date.now()}.glb`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    notify.error('下载失败')
  }
}

onMounted(() => {
  draft.value = readAiHeritageDraft()
  if (!draft.value.generatedImageUrl && !draft.value.merchGeneratedImageUrl && !getAiHeritageSourceFile() && !draft.value.sourceImageUrl) {
    router.replace({ name: 'create-ai-heritage-post' })
    return
  }

  if (!isMerch.value && !draft.value.theme && selectedStyle.value) {
    draft.value = writeAiHeritageDraft({ theme: selectedStyle.value.heritageThemeId })
  }
  if (!draft.value.generatedImageUrl && draft.value.merchGeneratedImageUrl) {
    draft.value = writeAiHeritageDraft({ generatedImageUrl: draft.value.merchGeneratedImageUrl })
  }
})

onBeforeUnmount(() => {
  stopGen3DProgress()
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

.orb-left {
  left: -140px;
  top: -120px;
  background: rgba(157, 41, 41, 0.22);
}

.orb-right {
  right: -140px;
  bottom: 80px;
  background: rgba(15, 118, 110, 0.16);
}

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

.hero-desc {
  margin: 10px 0 0;
  color: #7f6b58;
  font-size: 14px;
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

.publish-card {
  width: min(1120px, 100%);
  padding: 22px;
  background: rgba(255, 252, 246, 0.92);
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
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 240, 229, 0.94));
}

.result-frame {
  cursor: default;
}

.media-preview {
  width: 100%;
  height: auto;
  max-width: 100%;
  max-height: 540px;
  display: block;
  object-fit: contain;
  border-radius: 18px;
}

.media-empty {
  text-align: center;
  color: #7f6b58;
}

.media-empty i {
  display: block;
  font-size: 60px;
  margin-bottom: 6px;
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

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
}

.ghost-btn,
.submit-btn {
  min-width: 180px;
  border: none;
  border-radius: 18px;
  padding: 14px 18px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.ghost-btn {
  background: rgba(255, 255, 255, 0.85);
  color: #6f5b4a;
  border: 1px solid rgba(182, 143, 106, 0.24);
}

.submit-btn {
  color: #fff;
  background: linear-gradient(135deg, #8d2323 0%, #c2410c 100%);
  box-shadow: 0 14px 28px rgba(141, 35, 35, 0.24);
}

/* 3D模型视窗 */
.model-viewer-wrapper {
  padding: 0;
  min-height: 420px;
  background: #1a1a1a;
}

.model-viewer {
  width: 100%;
  height: 100%;
  min-height: 420px;
}

/* 3D区域 */
.three-d-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.preview-mode-tabs {
  display: flex;
  gap: 6px;
}

.mode-tab {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid rgba(182, 143, 106, 0.3);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.7);
  color: #7f6b58;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.22s ease;
}

.mode-tab.active {
  border-color: #8d2323;
  background: rgba(141, 35, 35, 0.08);
  color: #8d2323;
}

.three-d-actions {
  display: flex;
  gap: 10px;
}

.gen3d-btn {
  flex: 1;
  padding: 12px 16px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.gen3d-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.35);
}

.gen3d-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.download-3d-btn {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid rgba(182, 143, 106, 0.4);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
  color: #2d2118;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.download-3d-btn:hover {
  border-color: #8d2323;
  background: rgba(141, 35, 35, 0.06);
  color: #8d2323;
}

/* 3D进度覆盖 */
.progress-overlay {
  padding: 0 16px 16px;
}

.progress-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(141, 35, 35, 0.1);
  color: #8d2323;
  margin-top: 10px;
}

.gen3d-chip {
  background: rgba(99, 102, 241, 0.12);
  color: #6366f1;
}

.progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.progress-track {
  position: relative;
  height: 10px;
  margin-top: 12px;
  border-radius: 999px;
  background: rgba(141, 35, 35, 0.1);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #8d2323, #f59e0b);
}

.gen3d-fill {
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
}

.progress-sheen {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.32), transparent);
  transform: translateX(-100%);
  animation: sheen 1.6s infinite linear;
}

.progress-text {
  margin: 10px 0 0;
  color: #7f6b58;
  font-size: 12px;
}

@keyframes sheen {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@media (max-width: 900px) {
  .hero-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-row {
    flex-direction: column;
  }

  .ghost-btn,
  .submit-btn {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .ai-step-page {
    padding: 14px 14px 96px;
  }

  .hero-panel,
  .publish-card {
    border-radius: 22px;
    padding: 18px;
  }

  .media-frame {
    min-height: 280px;
  }
}
</style>