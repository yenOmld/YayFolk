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
        <p class="hero-eyebrow">AI 非遗大片 · 第 2 步</p>
        <h1>选择风格</h1>
      </div>
      <div class="step-track">
        <span>1 上传</span>
        <span class="active">2 风格</span>
        <span>3 发布</span>
      </div>
    </section>

    <section class="single-shell">
      <article class="glass-card style-card">
        <div class="workspace-grid">
          <aside class="preview-panel">
            <div class="media-card preview-card">
              <div class="media-frame media-frame-preview">
                <img v-if="sourcePreviewUrl" class="media-preview" :src="sourcePreviewUrl" alt="原图预览" />
                <div v-else class="media-empty">
                  <i class="bx bx-image"></i>
                  <p>请先上传原图</p>
                </div>
              </div>

              <transition name="fade">
                <div v-if="generating" class="progress-overlay">
                  <div class="progress-chip">
                    <span class="progress-dot"></span>
                    <strong>{{ progressLabel }}</strong>
                  </div>
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: `${progressValue}%` }"></div>
                    <div class="progress-sheen"></div>
                  </div>
                  <p class="progress-text">{{ progressDetail }}</p>
                </div>
              </transition>
            </div>

            <div class="preview-meta">
              <span>{{ selectedStyle.name }}</span>
              <span>{{ selectedSize.aspect }}</span>
            </div>
            <p class="preview-note">背景负责质感和气氛，场景负责主体摆位和空间层次；生成时会先保留主体再重绘背景。</p>
          </aside>

          <div class="control-panel">
            <div class="section-block">
              <div class="section-head">
                <h3>背景风格</h3>
              </div>
              <p class="section-hint">选一个你喜欢的背景气质，决定画面是活泼、清爽还是高级。</p>
              <div class="style-grid">
                <button
                  v-for="style in posterStylePresets"
                  :key="style.id"
                  type="button"
                  class="option-card"
                  :class="{ active: draft.posterStyle === style.id }"
                  @click="selectStyle(style)"
                >
                  <span class="option-icon" v-html="style.icon"></span>
                  <strong>{{ style.name }}</strong>
                  <span>{{ style.shortDesc }}</span>
                </button>
              </div>
            </div>

            <div class="section-block">
              <div class="section-head">
                <h3>构图场景</h3>
              </div>
              <p class="section-hint">决定主体怎么放、空间怎么排和画面怎么稳住。</p>
              <div class="scene-grid">
                <button
                  v-for="scene in scenePresets"
                  :key="scene.id"
                  type="button"
                  class="scene-card"
                  :class="{ active: draft.scene === scene.id }"
                  @click="draft.scene = scene.id"
                >
                  <strong>{{ scene.name }}</strong><br>
                  <span>{{ scene.brief }}</span>
                </button>
              </div>
            </div>

            <div class="section-block">
              <div class="section-head">
                <h3>画幅比例</h3>
              </div>
              <div class="size-grid">
                <button
                  v-for="size in posterSizePresets"
                  :key="size.id"
                  type="button"
                  class="size-card"
                  :class="{ active: draft.imageSize === size.id }"
                  @click="draft.imageSize = size.id"
                >
                  <strong>{{ size.aspect }}</strong><br>
                  <span>{{ size.shortDesc }}</span>
                </button>
              </div>
            </div>

            <div class="action-row">
              <button class="primary-btn" type="button" @click="goBack">
                上一步
              </button>
              <button class="primary-btn" type="button" :disabled="!hasSource || generating" @click="goNext">
                <i v-if="generating" class="bx bx-loader-alt bx-spin"></i>
                {{ generating ? '生成中' : '生成并下一步' }}
              </button>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { generateAiHeritagePoster } from '../../../api/app'
import {
  buildHeritagePrompt,
  getAiHeritageSourceFile,
  getAiHeritageSourcePreviewUrl,
  getPosterSizeById,
  getPosterStyleById,
  normalizePosterSizeRequestValue,
  posterSizePresets,
  posterStylePresets,
  readAiHeritageDraft,
  scenePresets,
  writeAiHeritageDraft,
  getAiHeritageReturnPath,
  clearAiHeritageReturnPath
} from '../../../utils/aiHeritage'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const router = useRouter()
const route = useRoute()
const draft = ref(readAiHeritageDraft())

const generating = ref(false)
const progressValue = ref(0)
const progressLabel = ref('准备中')
const progressDetail = ref('正在读取原图')
const progressTimers = []

const sourcePreviewUrl = computed(() => getAiHeritageSourcePreviewUrl() || draft.value.sourceImageUrl || '')
const hasSource = computed(() => Boolean(getAiHeritageSourceFile() || sourcePreviewUrl.value))
const selectedStyle = computed(() => getPosterStyleById(draft.value.posterStyle))
const selectedSize = computed(() => getPosterSizeById(draft.value.imageSize))

const progressStages = [
  { label: '分析原图', detail: '先识别主体，再准备背景' },
  { label: '匹配风格', detail: '套用你选中的风格语言' },
  { label: '连接模型', detail: '正在请求生成接口' },
  { label: '细化画面', detail: '修正光影和层次感' },
  { label: '即将完成', detail: '结果马上返回' }
]

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

function selectStyle(style) {
  draft.value = writeAiHeritageDraft({
    posterStyle: style.id,
    theme: style.heritageThemeId || draft.value.theme
  })
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('原图读取失败'))
    reader.readAsDataURL(file)
  })
}

function startProgress() {
  stopProgress()
  progressValue.value = 8
  progressLabel.value = progressStages[0].label
  progressDetail.value = progressStages[0].detail

  let stageIndex = 0

  progressTimers.push(setInterval(() => {
    const ceiling = generating.value ? 92 : 100
    if (progressValue.value < ceiling) {
      const delta = Math.max(1, Math.round((ceiling - progressValue.value) / 8))
      progressValue.value = Math.min(ceiling, progressValue.value + delta)
    }
  }, 240))

  progressTimers.push(setInterval(() => {
    stageIndex = (stageIndex + 1) % progressStages.length
    progressLabel.value = progressStages[stageIndex].label
    progressDetail.value = progressStages[stageIndex].detail
  }, 900))
}

function stopProgress() {
  while (progressTimers.length) {
    clearInterval(progressTimers.pop())
  }
}

async function goNext() {
  if (!hasSource.value) {
    notify.warning('请先上传原图')
    return
  }

  generating.value = true
  startProgress()

  try {
    await nextTick()

    const sourceFile = getAiHeritageSourceFile()
    const sourceImageUrl = draft.value.sourceImageUrl || (sourceFile ? await fileToDataUrl(sourceFile) : '')
    if (!sourceImageUrl) {
      throw new Error('请先上传原图')
    }

    const response = await generateAiHeritagePoster({
      imageUrl: sourceImageUrl,
      prompt: buildHeritagePrompt(draft.value.theme, draft.value.scene, draft.value.posterStyle, draft.value.imageSize),
      size: normalizePosterSizeRequestValue(selectedSize.value.size)
    })

    if (response.code !== 200 || !response.data?.url) {
      throw new Error(response.message || 'AI 生成失败')
    }

    progressValue.value = 100
    progressLabel.value = '生成完成'
    progressDetail.value = '正在进入发布页'

    writeAiHeritageDraft({
      theme: draft.value.theme || selectedStyle.value.heritageThemeId,
      scene: draft.value.scene,
      posterStyle: draft.value.posterStyle,
      imageSize: draft.value.imageSize,
      title: draft.value.title || `${selectedStyle.value.name} 成品展示`,
      content: draft.value.content || `${selectedStyle.value.name} · ${scenePresets.find(item => item.id === draft.value.scene)?.name || '构图场景'}`,
      generatedImageUrl: response.data.url,
      merchType: '',
      merchColor: '',
      merchCustomColor: '',
      merchGeneratedImageUrl: ''
    })

    await new Promise(resolve => setTimeout(resolve, 350))
    router.push({ name: 'create-ai-heritage-publish' })
  } catch (error) {
    notify.error(error?.message || 'AI 生成失败，请重试')
    console.error(error)
  } finally {
    generating.value = false
    stopProgress()
    progressValue.value = 0
  }
}

onMounted(() => {
  draft.value = readAiHeritageDraft()
  if (!getAiHeritageSourceFile() && !draft.value.sourceImageUrl) {
    router.replace({ name: 'create-ai-heritage-post' })
    return
  }
  if (!draft.value.theme) {
    draft.value = writeAiHeritageDraft({ theme: selectedStyle.value.heritageThemeId })
  }
})

onBeforeUnmount(() => {
  stopProgress()
})
</script>

<style scoped>
.ai-step-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 24px 24px 110px;
  background:
    radial-gradient(circle at top left, rgba(157, 41, 41, 0.08), transparent 30%),
    radial-gradient(circle at top right, rgba(218, 170, 124, 0.12), transparent 24%),
    linear-gradient(180deg, #faf6f1 0%, #fdf8f3 50%, #faf4ed 100%);
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

.orb-left { left: -140px; top: -120px; background: rgba(157, 41, 41, 0.15); }
.orb-right { right: -140px; bottom: 80px; background: rgba(218, 170, 124, 0.12); }

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
  border: 1px solid rgba(218, 170, 124, 0.3);
  border-radius: 28px;
  box-shadow: 0 18px 48px rgba(92, 61, 39, 0.08);
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
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(253, 248, 242, 0.95));
  margin-bottom: 18px;
}

.back-btn {
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 14px;
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 22px;
}

.hero-eyebrow {
  margin: 0 0 10px;
  color: #9d2929;
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
  border: 1px solid rgba(218, 170, 124, 0.2);
}

.step-track .active {
  color: #fff;
  background: linear-gradient(135deg, #9d2929, #c2410c);
  border-color: transparent;
}

.single-shell {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
}

.style-card {
  width: min(1120px, 100%);
  padding: 22px;
  background: rgba(255, 252, 246, 0.92);
}

.workspace-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

@media (min-width: 1200px) {
  .workspace-grid {
    grid-template-columns: 1fr 1fr;
  }
}

.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.media-card {
  border: 1px solid rgba(218, 170, 124, 0.24);
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
  color: #9d2929;
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

.progress-overlay {
  padding: 0 16px 16px;
}

.progress-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(157, 41, 41, 0.1);
  color: #9d2929;
  margin-top: 10px;
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
  background: rgba(157, 41, 41, 0.1);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #9d2929, #f59e0b);
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

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.preview-meta span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
  font-size: 12px;
  font-weight: 700;
}

.preview-note {
  margin: 0;
  color: #7f6b58;
  font-size: 13px;
  line-height: 1.7;
}

.control-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-block {
  display: grid;
  gap: 12px;
}

.section-head h3 {
  margin: 0;
  font-size: 18px;
  color: #2d2118;
}

.section-hint {
  margin: 0;
  color: #7f6b58;
  font-size: 13px;
}

.style-grid,
.scene-grid,
.size-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.option-card,
.scene-card,
.size-card {
  border: 1px solid rgba(218, 170, 124, 0.24);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  padding: 16px;
  text-align: left;
  cursor: pointer;
  transition: all 0.22s ease;
  color: #2d2118;
}

.option-card:hover,
.scene-card:hover,
.size-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(92, 61, 39, 0.08);
}

.option-card.active,
.scene-card.active,
.size-card.active {
  border-color: #9d2929;
  box-shadow: 0 0 0 3px rgba(157, 41, 41, 0.08);
}

.option-card {
  display: grid;
  gap: 10px;
}

.option-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
  font-size: 22px;
}

.option-card strong,
.scene-card strong,
.size-card strong {
  font-size: 15px;
}

.option-card span,
.scene-card span,
.size-card span {
  font-size: 12px;
  color: #7f6b58;
  line-height: 1.6;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.primary-btn {
  min-width: 200px;
  border: none;
  border-radius: 18px;
  padding: 14px 18px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #9d2929 0%, #c2410c 100%);
  box-shadow: 0 14px 28px rgba(157, 41, 41, 0.24);
  cursor: pointer;
}

.primary-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@keyframes sheen {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@media (max-width: 960px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .ai-step-page {
    padding: 14px 14px 96px;
  }

  .hero-panel,
  .style-card {
    border-radius: 22px;
    padding: 18px;
  }

  .media-frame {
    min-height: 280px;
  }

  .style-grid,
  .scene-grid,
  .size-grid {
    grid-template-columns: 1fr;
  }

  .primary-btn {
    width: 100%;
  }
}
</style>