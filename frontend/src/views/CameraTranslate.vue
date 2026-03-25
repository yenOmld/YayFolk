<template>
  <div class="camera-translate-page">
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <i class='bx bx-arrow-back'></i>
        {{ $t('common.back') }}
      </button>
      <h1>{{ $t('camera.title') }}</h1>
      <div class="header-spacer"></div>
    </div>

    <div class="content">
      <div class="language-selector">
        <div class="lang-select-group">
          <label>{{ $t('translate.sourceLang') }}</label>
          <select v-model="sourceLang">
            <option value="auto">{{ $t('translate.autoDetect') }}</option>
            <option v-for="lang in languages" :key="lang.code" :value="lang.code">
              {{ lang.name }}
            </option>
          </select>
        </div>
        <div class="lang-select-group">
          <label>{{ $t('translate.targetLang') }}</label>
          <select v-model="targetLang">
            <option v-for="lang in languages" :key="lang.code" :value="lang.code">
              {{ lang.name }}
            </option>
          </select>
        </div>
      </div>

      <div class="camera-section">
        <div v-if="!capturedImage" class="camera-view">
          <video ref="videoElement" autoplay playsinline></video>
          <div class="camera-controls">
            <button class="capture-btn" @click="captureImage" :disabled="!isCameraReady">
              <i class='bx bx-camera'></i>
            </button>
            <button class="upload-btn" @click="triggerFileUpload">
              <i class='bx bx-upload'></i>
            </button>
            <input 
              ref="fileInput" 
              type="file" 
              accept="image/*" 
              style="display: none" 
              @change="handleFileUpload"
            >
          </div>
          <div v-if="!isCameraReady" class="camera-loading">
            <i class='bx bx-loader-alt bx-spin'></i>
            <span>{{ $t('camera.startCamera') }}</span>
          </div>
        </div>
        <div v-else class="image-preview">
          <img :src="capturedImage" alt="Captured image">
          <div class="image-controls">
            <button class="retake-btn" @click="retakeImage">
              <i class='bx bx-refresh'></i>
              {{ $t('camera.retake') }}
            </button>
            <button class="upload-new-btn" @click="triggerFileUpload">
              <i class='bx bx-upload'></i>
              {{ $t('camera.uploadImage') }}
            </button>
            <button class="translate-btn" @click="translateImage" :disabled="isTranslatingImage">
              <i class='bx bx-translate'></i>
              {{ isTranslatingImage ? $t('translate.translating') : $t('translate.translateBtn') }}
            </button>
            <input 
              ref="fileInput" 
              type="file" 
              accept="image/*" 
              style="display: none" 
              @change="handleFileUpload"
            >
          </div>
        </div>
      </div>

      <div v-if="ocrResult" class="result-section">
        <div class="result-card">
          <h3>{{ $t('camera.ocrResult') }}</h3>
          <div class="result-content">
            <h4>{{ $t('camera.originalText') }}</h4>
            <p>{{ ocrResult.ocrText }}</p>
          </div>
          <div class="result-content">
            <h4>{{ $t('camera.translatedText') }}</h4>
            <p>{{ ocrResult.translatedText }}</p>
          </div>
          <div class="result-actions">
            <button class="action-btn" @click="copyOcrText">
              <i class='bx bx-copy'></i>
              {{ $t('camera.copyOriginal') }}
            </button>
            <button class="action-btn" @click="copyTranslatedText">
              <i class='bx bx-copy'></i>
              {{ $t('camera.copyTranslation') }}
            </button>
            <button class="action-btn" @click="speakText('original')">
              <i class='bx bx-volume-full'></i>
              {{ $t('camera.speakOriginal') }}
            </button>
            <button class="action-btn" @click="speakText('translated')">
              <i class='bx bx-volume-full'></i>
              {{ $t('camera.speakTranslation') }}
            </button>
            <button class="action-btn" @click="pauseSpeech" :disabled="!isSpeaking">
              <i class='bx bx-pause'></i>
              {{ $t('camera.pausePlay') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useI18n } from 'vue-i18n'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const { t } = useI18n()
const router = useRouter()

const languages = computed(() => [
  { code: 'zh', name: '中文（简体）' },
  { code: 'zh-TW', name: '中文（繁體）' },
  { code: 'en', name: 'English' },
  { code: 'ja', name: '日本語' },
  { code: 'ko', name: '한국어' },
  { code: 'fr', name: 'Français' },
  { code: 'de', name: 'Deutsch' },
  { code: 'es', name: 'Español' },
  { code: 'it', name: 'Italiano' },
  { code: 'pt', name: 'Português' },
  { code: 'ru', name: 'Русский' },
  { code: 'ar', name: 'العربية' },
  { code: 'th', name: 'ไทย' },
  { code: 'vi', name: 'Tiếng Việt' },
  { code: 'id', name: 'Bahasa Indonesia' },
  { code: 'ms', name: 'Bahasa Melayu' },
  { code: 'tr', name: 'Türkçe' },
  { code: 'nl', name: 'Nederlands' },
  { code: 'pl', name: 'Polski' }
])

const sourceLang = ref('auto')
const targetLang = ref('en')
const videoElement = ref(null)
const fileInput = ref(null)
const capturedImage = ref(null)
const isTranslatingImage = ref(false)
const ocrResult = ref(null)
const isCameraReady = ref(false)
let mediaStream = null
let speechInstance = null
const isSpeaking = ref(false)

const startCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ 
      video: { facingMode: 'environment' } 
    })
    if (videoElement.value) {
      videoElement.value.srcObject = mediaStream
      isCameraReady.value = true
    }
  } catch (error) {
    console.error('无法访问摄像头:', error)
    notify.error(t('camera.cameraAccessFailed'))
  }
}

const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
  isCameraReady.value = false
}

const captureImage = () => {
  if (!videoElement.value) return
  
  const canvas = document.createElement('canvas')
  canvas.width = videoElement.value.videoWidth
  canvas.height = videoElement.value.videoHeight
  
  const ctx = canvas.getContext('2d')
  ctx.drawImage(videoElement.value, 0, 0)
  
  capturedImage.value = canvas.toDataURL('image/jpeg', 0.8)
  stopCamera()
}

const retakeImage = async () => {
  capturedImage.value = null
  ocrResult.value = null
  await startCamera()
}

const triggerFileUpload = () => {
  if (fileInput.value) {
    fileInput.value.click()
  }
}

const handleFileUpload = (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  const reader = new FileReader()
  reader.onload = (e) => {
    capturedImage.value = e.target.result
    stopCamera()
  }
  reader.readAsDataURL(file)
}

const translateImage = async () => {
  if (!capturedImage.value) return
  
  isTranslatingImage.value = true
  
  try {
    const imageBase64 = capturedImage.value.split(',')[1]
    
    const token = localStorage.getItem('token')
    const response = await axios.post('/api/ocr/translate', {
      imageBase64: imageBase64,
      sourceLang: sourceLang.value,
      targetLang: targetLang.value
    }, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    })
    
    if (response.data.code === 200) {
      ocrResult.value = {
        ocrText: response.data.ocrText,
        translatedText: response.data.translatedText
      }
    } else {
      notify.error(t('camera.translateFailed') + ': ' + response.data.message)
    }
  } catch (error) {
    console.error('翻译错误:', error)
    if (error.response && error.response.status === 401) {
      notify.error(t('camera.loginRequired'))
      router.push('/')
    } else {
      notify.error(t('camera.translateFailedRetry'))
    }
  } finally {
    isTranslatingImage.value = false
  }
}

const copyOcrText = () => {
  if (!ocrResult.value) return
  navigator.clipboard.writeText(ocrResult.value.ocrText)
  notify.success(t('camera.copiedOriginal'))
}

const copyTranslatedText = () => {
  if (!ocrResult.value) return
  navigator.clipboard.writeText(ocrResult.value.translatedText)
  notify.success(t('camera.copiedTranslated'))
}

const speakText = (type) => {
  if (!ocrResult.value) return
  
  // 先停止之前的播放
  speechSynthesis.cancel()
  
  const text = type === 'original' ? ocrResult.value.ocrText : ocrResult.value.translatedText
  const lang = type === 'original' ? sourceLang.value === 'auto' ? 'en' : sourceLang.value : targetLang.value
  
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = lang === 'zh' ? 'zh-CN' : lang
  
  utterance.onstart = () => {
    isSpeaking.value = true
  }
  
  utterance.onend = () => {
    isSpeaking.value = false
  }
  
  speechSynthesis.speak(utterance)
}

const pauseSpeech = () => {
  speechSynthesis.pause()
  isSpeaking.value = false
}

const goBack = () => {
  stopCamera()
  router.back()
}

onMounted(() => {
  startCamera()
})

onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped>
.camera-translate-page {
  min-height: 100vh;
  background: linear-gradient(90deg, #e2e2e2, #c9d6ff);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: none;
  border: none;
  color: #333;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.back-btn:hover {
  background: rgba(116, 148, 236, 0.2);
  transform: translateX(-5px);
}

.page-header h1 {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.header-spacer {
  width: 100px;
}

.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.language-selector {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  background: rgba(255, 255, 255, 0.8);
  padding: 20px;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.lang-select-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.lang-select-group label {
  font-weight: 600;
  color: #333;
}

.lang-select-group select {
  padding: 12px 15px;
  border: 2px solid rgba(116, 148, 236, 0.3);
  border-radius: 10px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.8);
  color: #333;
  cursor: pointer;
  transition: border-color 0.3s;
}

.lang-select-group select:focus {
  outline: none;
  border-color: #7494ec;
}

.camera-section {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.camera-view {
  position: relative;
  width: 100%;
  height: 500px;
  background: #000;
}

.camera-view video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-controls {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 20px;
  align-items: center;
}

.capture-btn,
.upload-btn {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #7494ec;
  border: 5px solid rgba(116, 148, 236, 0.3);
  color: white;
  font-size: 32px;
  cursor: pointer;
  transition: all 0.3s;
}

.capture-btn:hover:not(:disabled),
.upload-btn:hover {
  background: #5a7ae0;
  transform: scale(1.1);
  box-shadow: 0 4px 15px rgba(116, 148, 236, 0.4);
}

.capture-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.camera-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  color: white;
  font-size: 18px;
}

.camera-loading i {
  font-size: 48px;
}

.image-preview {
  padding: 30px;
  text-align: center;
}

.image-preview img {
  max-width: 100%;
  max-height: 500px;
  border-radius: 10px;
  margin-bottom: 20px;
}

.image-controls {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.retake-btn,
.upload-new-btn,
.image-controls .translate-btn {
  padding: 15px 35px;
  border: 1px solid rgba(116, 148, 236, 0.3);
  border-radius: 10px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 10px;
}

.retake-btn {
  background: rgba(108, 117, 125, 0.2);
  color: #333;
  border: 1px solid rgba(108, 117, 125, 0.3);
}

.retake-btn:hover {
  background: rgba(108, 117, 125, 0.3);
  transform: translateY(-2px);
}

.upload-new-btn {
  background: rgba(40, 167, 69, 0.2);
  color: #333;
  border: 1px solid rgba(40, 167, 69, 0.3);
}

.upload-new-btn:hover {
  background: rgba(40, 167, 69, 0.3);
  transform: translateY(-2px);
}

.image-controls .translate-btn {
  background: linear-gradient(135deg, #7494ec, #6381d9, #526fd6, #415ed3);
  color: white;
}

.image-controls .translate-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(116, 148, 236, 0.4);
}

.image-controls .translate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.result-section {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.result-card h3 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 24px;
}

.result-content {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 10px;
}

.result-content h4 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 16px;
}

.result-content p {
  margin: 0;
  color: #555;
  line-height: 1.8;
  white-space: pre-wrap;
  font-size: 16px;
}

.result-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.action-btn {
  flex: 1;
  min-width: 150px;
  padding: 12px 20px;
  background: rgba(116, 148, 236, 0.2);
  color: #333;
  border: 1px solid rgba(116, 148, 236, 0.3);
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-btn:hover {
  background: rgba(116, 148, 236, 0.3);
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .language-selector {
    flex-direction: column;
  }
  
  .camera-view {
    height: 400px;
  }
  
  .image-controls {
    flex-direction: column;
  }
  
  .result-actions {
    flex-direction: column;
  }
  
  .action-btn {
    width: 100%;
  }
}
</style>