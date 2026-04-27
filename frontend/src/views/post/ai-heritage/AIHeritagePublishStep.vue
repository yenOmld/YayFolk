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
        <p class="hero-eyebrow">AI 非遗大片 · 第 3 步</p>
        <h1>生成结果</h1>
        <p class="hero-desc">确认成品后，可先保存到本地，再去发现页发布分享。</p>
      </div>
      <div class="step-track">
        <span>1 上传</span>
        <span>2 风格</span>
        <span class="active">3 发布</span>
      </div>
    </section>

    <section class="single-shell">
      <article class="glass-card publish-card">
        <div class="media-card">
          <div class="media-frame media-frame-preview result-frame" :class="{ ready: Boolean(generatedImageUrl) }">
            <img v-if="generatedImageUrl" class="media-preview" :src="generatedImageUrl" alt="AI 生成结果" />
            <div v-else class="media-empty">
              <i class="bx bx-images"></i>
              <strong>{{ selectedStyle.name }}</strong>
              <p>生成后会在这里展示成品画面</p>
            </div>
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
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getAiHeritageSourceFile,
  getPosterStyleById,
  readAiHeritageDraft,
  writeAiHeritageDraft
} from '../../../utils/aiHeritage'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const router = useRouter()

const draft = ref(readAiHeritageDraft())
const savingLocal = ref(false)
const sharing = ref(false)

const generatedImageUrl = computed(() => draft.value.generatedImageUrl || draft.value.sourceImageUrl || '')
const selectedStyle = computed(() => getPosterStyleById(draft.value.posterStyle))

function goBack() {
  router.push({ name: 'create-ai-heritage-style' })
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
    const filename = `AI非遗大片-${selectedStyle.value.name}.png`
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

onMounted(() => {
  draft.value = readAiHeritageDraft()
  if (!draft.value.generatedImageUrl && !getAiHeritageSourceFile() && !draft.value.sourceImageUrl) {
    router.replace({ name: 'create-ai-heritage-post' })
    return
  }

  if (!draft.value.theme) {
    draft.value = writeAiHeritageDraft({ theme: selectedStyle.value.heritageThemeId })
  }
  if (!draft.value.generatedImageUrl && draft.value.sourceImageUrl) {
    draft.value = writeAiHeritageDraft({ generatedImageUrl: draft.value.sourceImageUrl })
  }
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