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
        <p class="hero-eyebrow">AI 文创定制 · 第 2 步</p>
        <h1>选择文创商品</h1>
      </div>
      <div class="step-track">
        <span>1 上传</span>
        <span class="active">2 设计</span>
        <span>3 发布</span>
      </div>
    </section>

    <section class="single-shell">
      <article class="glass-card merch-card">
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
              <span>{{ selectedMerchType?.name || '选择商品' }}</span>
              <span>{{ selectedMerchColor?.name || '选择颜色' }}</span>
            </div>
            <p class="preview-note">选择你想要定制的文创商品类型和颜色，AI 将为你生成专属定制效果图。</p>
          </aside>

          <div class="control-panel">
            <div class="section-block">
              <div class="section-head">
                <h3>商品类型</h3>
              </div>
              <p class="section-hint">选一个你想要定制的文创商品。</p>
              <div class="merch-type-grid">
                <button
                  v-for="type in merchTypePresets"
                  :key="type.id"
                  type="button"
                  class="merch-type-card"
                  :class="{ active: draft.merchType === type.id }"
                  @click="selectMerchType(type)"
                >
                  <span class="merch-icon" style="background: rgba(157, 41, 41, 0.1)">
                    <i v-if="type.icon.startsWith('bx')" :class="type.icon"></i>
                    <span v-else v-html="type.icon" class="custom-icon"></span>
                  </span>
                  <strong>{{ type.name }}</strong>
                  <span>{{ type.brief }}</span>
                </button>
              </div>
            </div>

            <div class="section-block">
              <div class="section-head">
                <h3>商品颜色</h3>
              </div>
              <p class="section-hint">选择预设颜色或使用调色盘自定义。</p>
              <div class="color-palette-grid">
                <button
                  v-for="color in merchColorPresets"
                  :key="color.id"
                  type="button"
                  class="color-palette-item"
                  :class="{ active: draft.merchColor === color.id }"
                  :title="color.name"
                  @click="selectPresetColor(color)"
                >
                  <span class="color-dot" :style="{ background: color.hex }"></span>
                </button>
              </div>
              <div class="color-picker-container">
                <div class="color-picker-header">
                  <div class="color-preview-box" :style="{ background: currentColorPreview }"></div>
                  <input
                    v-model="colorHexInput"
                    type="text"
                    class="color-code-input"
                    placeholder="#RRGGBBAA"
                    maxlength="9"
                    @change="parseColorInput"
                  />
                </div>
                <div class="color-picker-body">
                  <div 
                    class="hue-bar"
                    @click="handleHueBarClick"
                    @mousedown="startHueDrag"
                  >
                    <div class="hue-pointer" :style="{ left: `${hue}%` }"></div>
                  </div>
                  <div 
                    class="color-grid"
                    :style="{ background: `linear-gradient(to bottom, hsl(${hue}, 100%, 50%), hsl(${hue}, 100%, 0%))` }"
                    @click="handleColorGridClick"
                    @mousedown="startColorDrag"
                  >
                    <div 
                      class="color-grid-mask"
                      :style="{ background: `linear-gradient(to right, rgba(0,0,0,0), rgba(0,0,0,1))` }"
                    ></div>
                    <div 
                      class="color-pointer"
                      :style="{ left: `${saturation}%`, top: `${100 - lightness}%` }"
                    ></div>
                  </div>
                </div>
                <div class="opacity-control">
                  <span class="opacity-label">透明度</span>
                  <input
                    type="range"
                    class="opacity-slider"
                    min="0"
                    max="100"
                    :value="opacity"
                    @input="handleOpacityChange"
                  />
                  <span class="opacity-value">{{ opacity }}%</span>
                </div>
                <button class="apply-color-btn-large" type="button" @click="applyCustomColor">
                  应用颜色
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
  buildMerchPrompt,
  getAiHeritageSourceFile,
  getAiHeritageSourcePreviewUrl,
  getMerchTypeById,
  getMerchColorById,
  merchColorPresets,
  merchTypePresets,
  normalizePosterSizeRequestValue,
  posterSizePresets,
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

const hue = ref(0)
const saturation = ref(100)
const lightness = ref(50)
const opacity = ref(100)
const colorHexInput = ref('#FFFFFF')
const generating = ref(false)
const progressValue = ref(0)
const progressLabel = ref('准备中')
const progressDetail = ref('正在读取原图')
const progressTimers = []

const sourcePreviewUrl = computed(() => getAiHeritageSourcePreviewUrl() || draft.value.sourceImageUrl || '')
const hasSource = computed(() => Boolean(getAiHeritageSourceFile() || sourcePreviewUrl.value))
const selectedMerchType = computed(() => getMerchTypeById(draft.value.merchType))
const selectedMerchColor = computed(() => {
  const preset = getMerchColorById(draft.value.merchColor)
  if (preset) return preset
  return {
    id: 'custom',
    name: '自定义',
    hex: draft.value.merchCustomColor || '#FFFFFF'
  }
})

const currentColorPreview = computed(() => {
  return `hsla(${hue.value}, ${saturation.value}%, ${lightness.value}%, ${opacity.value / 100})`
})

const progressStages = [
  { label: '分析原图', detail: '识别主体特征' },
  { label: '匹配商品', detail: '套用商品模板' },
  { label: '连接模型', detail: '正在请求生成接口' },
  { label: '渲染效果', detail: '生成定制效果图' },
  { label: '即将完成', detail: '结果马上返回' }
]

function goBack() {
  const returnPath = getAiHeritageReturnPath()
  if (returnPath) {
    clearAiHeritageReturnPath()
    router.push(returnPath)
    return
  }
  router.back()
}

function selectMerchType(type) {
  draft.value = writeAiHeritageDraft({
    merchType: type.id,
    merchColor: draft.value.merchColor || merchColorPresets[0].id
  })
}

function selectPresetColor(color) {
  draft.value = writeAiHeritageDraft({
    merchColor: color.id,
    merchCustomColor: null
  })
  const rgb = hexToRgb(color.hex)
  if (rgb) {
    updateHslFromRgb(rgb.r, rgb.g, rgb.b)
    opacity.value = 100
    updateHexInput()
  }
}

function hexToRgb(hex) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : null
}

function rgbToHsl(r, g, b) {
  r /= 255, g /= 255, b /= 255
  const max = Math.max(r, g, b), min = Math.min(r, g, b)
  let h, s, l = (max + min) / 2
  if (max === min) {
    h = s = 0
  } else {
    const d = max - min
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
    switch (max) {
      case r: h = ((g - b) / d + (g < b ? 6 : 0)) / 6; break
      case g: h = ((b - r) / d + 2) / 6; break
      case b: h = ((r - g) / d + 4) / 6; break
      default: h = 0
    }
  }
  return { h: h * 360, s: s * 100, l: l * 100 }
}

function hslToHex(h, s, l, a = 100) {
  s /= 100
  l /= 100
  const aHex = Math.round((a / 100) * 255).toString(16).padStart(2, '0').toUpperCase()
  const c = (1 - Math.abs(2 * l - 1)) * s
  const x = c * (1 - Math.abs((h / 60) % 2 - 1))
  const m = l - c / 2
  let r, g, b
  if (h >= 0 && h < 60) { r = c; g = x; b = 0 }
  else if (h >= 60 && h < 120) { r = x; g = c; b = 0 }
  else if (h >= 120 && h < 180) { r = 0; g = c; b = x }
  else if (h >= 180 && h < 240) { r = 0; g = x; b = c }
  else if (h >= 240 && h < 300) { r = x; g = 0; b = c }
  else { r = c; g = 0; b = x }
  r = Math.round((r + m) * 255).toString(16).padStart(2, '0').toUpperCase()
  g = Math.round((g + m) * 255).toString(16).padStart(2, '0').toUpperCase()
  b = Math.round((b + m) * 255).toString(16).padStart(2, '0').toUpperCase()
  return `#${r}${g}${b}${aHex}`
}

function updateHslFromRgb(r, g, b) {
  const hsl = rgbToHsl(r, g, b)
  hue.value = hsl.h
  saturation.value = hsl.s
  lightness.value = hsl.l
}

function updateHexInput() {
  colorHexInput.value = hslToHex(hue.value, saturation.value, lightness.value, opacity.value)
}

function parseColorInput() {
  const value = colorHexInput.value.trim().toUpperCase()
  const hexRegex = /^#?([0-9A-F]{2})([0-9A-F]{2})([0-9A-F]{2})([0-9A-F]{2})?$/
  const match = value.match(hexRegex)
  if (match) {
    const r = parseInt(match[1], 16)
    const g = parseInt(match[2], 16)
    const b = parseInt(match[3], 16)
    const a = match[4] ? parseInt(match[4], 16) : 255
    updateHslFromRgb(r, g, b)
    opacity.value = Math.round((a / 255) * 100)
    updateHexInput()
  }
}

function handleHueBarClick(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  const x = e.clientX - rect.left
  hue.value = Math.min(360, Math.max(0, (x / rect.width) * 360))
  updateHexInput()
}

function handleColorGridClick(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  saturation.value = Math.min(100, Math.max(0, (x / rect.width) * 100))
  lightness.value = Math.min(100, Math.max(0, 100 - (y / rect.height) * 100))
  updateHexInput()
}

function handleOpacityChange(e) {
  opacity.value = parseInt(e.target.value)
  updateHexInput()
}

function startHueDrag(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  const moveHandler = (e) => {
    const x = e.clientX - rect.left
    hue.value = Math.min(360, Math.max(0, (x / rect.width) * 360))
    updateHexInput()
  }
  const upHandler = () => {
    document.removeEventListener('mousemove', moveHandler)
    document.removeEventListener('mouseup', upHandler)
  }
  document.addEventListener('mousemove', moveHandler)
  document.addEventListener('mouseup', upHandler)
}

function startColorDrag(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  const moveHandler = (e) => {
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    saturation.value = Math.min(100, Math.max(0, (x / rect.width) * 100))
    lightness.value = Math.min(100, Math.max(0, 100 - (y / rect.height) * 100))
    updateHexInput()
  }
  const upHandler = () => {
    document.removeEventListener('mousemove', moveHandler)
    document.removeEventListener('mouseup', upHandler)
  }
  document.addEventListener('mousemove', moveHandler)
  document.addEventListener('mouseup', upHandler)
}

function applyCustomColor() {
  const colorHex = hslToHex(hue.value, saturation.value, lightness.value, opacity.value)
  draft.value = writeAiHeritageDraft({
    merchColor: 'custom',
    merchCustomColor: colorHex
  })
  notify.success('颜色已应用')
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

  if (!draft.value.merchType) {
    notify.warning('请先选择商品类型')
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

    const selectedSize = posterSizePresets.find(s => s.id === 'landscape-classic') || posterSizePresets[1]

    const response = await generateAiHeritagePoster({
      imageUrl: sourceImageUrl,
      prompt: buildMerchPrompt(
        draft.value.merchType,
        draft.value.merchColor,
        selectedMerchType.value,
        selectedMerchColor.value
      ),
      size: normalizePosterSizeRequestValue(selectedSize.size)
    })

    if (response.code !== 200 || !response.data?.url) {
      throw new Error(response.message || 'AI 生成失败')
    }

    progressValue.value = 100
    progressLabel.value = '生成完成'
    progressDetail.value = '正在进入发布页'

    writeAiHeritageDraft({
      merchType: draft.value.merchType,
      merchColor: draft.value.merchColor,
      merchCustomColor: draft.value.merchCustomColor,
      merchGeneratedImageUrl: response.data.url,
      generatedImageUrl: response.data.url,
      title: draft.value.title || `${selectedMerchType.value.name} 定制`,
      content: draft.value.content || `${selectedMerchType.value.name} 定制 - AI 非遗文创`,
      posterStyle: '',
      scene: '',
      imageSize: ''
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
  if (!draft.value.merchType && merchTypePresets.length > 0) {
    draft.value = writeAiHeritageDraft({
      merchType: merchTypePresets[0].id,
      merchColor: merchColorPresets[0].id
    })
  }
  if (draft.value.merchColor === 'custom' && draft.value.merchCustomColor) {
    const rgb = hexToRgb(draft.value.merchCustomColor)
    if (rgb) {
      updateHslFromRgb(rgb.r, rgb.g, rgb.b)
      const hexMatch = draft.value.merchCustomColor.match(/^#?[0-9A-Fa-f]{8}$/)
      if (hexMatch) {
        const a = parseInt(draft.value.merchCustomColor.slice(-2), 16)
        opacity.value = Math.round((a / 255) * 100)
      } else {
        opacity.value = 100
      }
    }
  } else if (draft.value.merchColor) {
    const preset = getMerchColorById(draft.value.merchColor)
    if (preset) {
      const rgb = hexToRgb(preset.hex)
      if (rgb) {
        updateHslFromRgb(rgb.r, rgb.g, rgb.b)
        opacity.value = 100
      }
    }
  }
  updateHexInput()
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

.merch-card {
  width: min(1120px, 100%);
  padding: 22px;
  background: rgba(255, 252, 246, 0.92);
}

.workspace-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 18px;
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

.merch-type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.color-palette-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.merch-type-card {
  border: 1px solid rgba(218, 170, 124, 0.24);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  padding: 14px;
  text-align: left;
  cursor: pointer;
  transition: all 0.22s ease;
  color: #2d2118;
}

.merch-type-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(92, 61, 39, 0.08);
}

.merch-type-card.active {
  border-color: #9d2929;
  box-shadow: 0 0 0 3px rgba(157, 41, 41, 0.08);
}

.merch-type-card {
  display: grid;
  gap: 8px;
}

.merch-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.merch-type-card strong {
  font-size: 14px;
}

.merch-type-card span {
  font-size: 11px;
  color: #7f6b58;
  line-height: 1.5;
}

.color-palette-item {
  width: 100%;
  aspect-ratio: 1;
  border: 2px solid transparent;
  border-radius: 12px;
  background: transparent;
  padding: 4px;
  cursor: pointer;
  transition: all 0.22s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.color-palette-item:hover {
  transform: scale(1.1);
}

.color-palette-item.active {
  border-color: #9d2929;
  box-shadow: 0 0 0 3px rgba(157, 41, 41, 0.15);
}

.color-dot {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.color-picker-container {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(218, 170, 124, 0.3);
  border-radius: 16px;
  padding: 16px;
}

.color-picker-header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.color-preview-box {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  border: 2px solid rgba(218, 170, 124, 0.4);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.color-code-input {
  flex: 1;
  padding: 12px 14px;
  border: 1px solid rgba(218, 170, 124, 0.4);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.22s ease;
}

.color-code-input:focus {
  outline: none;
  border-color: #9d2929;
  box-shadow: 0 0 0 3px rgba(157, 41, 41, 0.1);
}

.color-picker-body {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.hue-bar {
  width: 32px;
  height: 200px;
  border-radius: 8px;
  background: linear-gradient(to bottom, 
    hsl(0, 100%, 50%), 
    hsl(60, 100%, 50%), 
    hsl(120, 100%, 50%), 
    hsl(180, 100%, 50%), 
    hsl(240, 100%, 50%), 
    hsl(300, 100%, 50%), 
    hsl(360, 100%, 50%)
  );
  cursor: pointer;
  position: relative;
}

.hue-pointer {
  position: absolute;
  left: -4px;
  top: 50%;
  width: 40px;
  height: 4px;
  background: #fff;
  border-radius: 2px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
  transform: translateY(-50%);
}

.color-grid {
  flex: 1;
  height: 200px;
  border-radius: 8px;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.color-grid-mask {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.color-pointer {
  position: absolute;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid rgba(0, 0, 0, 0.3);
  transform: translate(-50%, -50%);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.opacity-control {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.opacity-label {
  font-size: 13px;
  font-weight: 600;
  color: #7f6b58;
  width: 50px;
}

.opacity-slider {
  flex: 1;
  height: 8px;
  -webkit-appearance: none;
  appearance: none;
  background: linear-gradient(to right, 
    rgba(0, 0, 0, 0), 
    rgba(0, 0, 0, 1)
  );
  border-radius: 4px;
  cursor: pointer;
}

.opacity-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  border: 2px solid #9d2929;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  cursor: pointer;
}

.opacity-slider::-moz-range-thumb {
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  border: 2px solid #9d2929;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  cursor: pointer;
}

.opacity-value {
  font-size: 13px;
  font-weight: 600;
  color: #7f6b58;
  width: 45px;
  text-align: right;
}

.apply-color-btn-large {
  width: 100%;
  padding: 14px 18px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #9d2929, #c2410c);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s ease;
}

.apply-color-btn-large:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(157, 41, 41, 0.3);
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
  .merch-card {
    border-radius: 22px;
    padding: 18px;
  }

  .media-frame {
    min-height: 280px;
  }

  .merch-type-grid {
    grid-template-columns: 1fr;
  }

  .color-palette-grid {
    grid-template-columns: repeat(6, 1fr);
  }

  .color-picker-body {
    flex-direction: column;
  }

  .hue-bar {
    width: 100%;
    height: 32px;
  }

  .hue-pointer {
    left: 50%;
    top: -4px;
    width: 4px;
    height: 40px;
    transform: translateX(-50%);
  }

  .color-grid {
    height: 150px;
  }

  .primary-btn {
    width: 100%;
  }
}
</style>
