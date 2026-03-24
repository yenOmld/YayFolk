<template>
  <div class="voice-translate-container">
    <header class="translate-header">
      <div class="header-content">
        <button class="back-btn" @click="goBack">
          <i class='bx bx-arrow-back'></i>
        </button>
        <h1>{{ $t('voice.title') }}</h1>
        <div class="header-right"></div>
      </div>
    </header>

    <main class="voice-translate-main">
      <div class="main-content-wrapper">
        <div class="left-section">
          <div class="language-selector">
            <select v-model="sourceLang" class="lang-select">
              <option value="zh">中文</option>
              <option value="en">English</option>
              <option value="jp">日本語</option>
              <option value="kor">한국어</option>
              <option value="fra">Français</option>
              <option value="de">Deutsch</option>
            </select>
            <button class="swap-btn" @click="swapLanguages" :disabled="isRecording">
              <i class='bx bx-transfer-alt'></i>
            </button>
            <select v-model="targetLang" class="lang-select">
              <option value="zh">中文</option>
              <option value="en">English</option>
              <option value="jp">日本語</option>
              <option value="kor">한국어</option>
              <option value="fra">Français</option>
              <option value="de">Deutsch</option>
            </select>
          </div>

          <div class="conversation-area" ref="conversationArea">
            <div v-if="messages.length === 0" class="empty-state">
              <p class="empty-title">{{ $t('voice.startConversation') }}</p>
              <span class="empty-hint">{{ $t('voice.supportVoiceText') }}</span>
            </div>
            <div class="messages-container">
              <div
                v-for="(msg, index) in messages"
                :key="index"
                class="message-item"
                :class="{ 'left': msg.direction === 'left', 'right': msg.direction === 'right' }"
              >
                <div class="message-bubble">
                  <div class="message-original">{{ msg.originalText }}</div>
                  <div class="message-translated">{{ msg.translatedText }}</div>
                  <div class="message-actions">
                    <button class="action-btn" @click="playMessageAudio(msg)">
                      <i class='bx bx-volume-full'></i>
                    </button>
                    <button class="action-btn" @click="copyText(msg.translatedText)">
                      <i class='bx bx-copy'></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="input-area">
            <textarea
              v-model="inputText"
              :placeholder="$t('voice.inputPlaceholder')"
              @keydown.enter.ctrl="translateText"
              :disabled="isRecording"
              rows="2"
            ></textarea>
            <button class="send-btn" @click="translateText" :disabled="!inputText.trim() || isRecording || isTextLoading">
              <span v-if="isTextLoading">{{ $t('translate.translating') }}</span>
              <span v-else>{{ $t('translate.translateBtn') }}</span>
            </button>
          </div>
        </div>

        <div class="right-section">
          <div class="record-area">
            <button class="record-btn" :class="{ recording: isRecording }" @click="toggleRecording">
              <i class='bx bx-microphone'></i>
            </button>
            <p class="record-hint">{{ isRecording ? $t('voice.clickToStop') : $t('voice.clickToStart') }}</p>
            <div v-if="isRecording" class="recording-indicator">
              <span class="recording-time">{{ recordingTime }}s / 60s</span>
            </div>
          </div>
          <button class="quick-conversation-btn" @click="goToConversation">
            <i class='bx bx-message-dots'></i>
            <span>{{ $t('voice.quickConversation') }}</span>
          </button>
        </div>
      </div>

      <div v-if="error" class="error-msg">{{ error }}</div>
    </main>
  </div>
</template>

<script setup>
import { ref, nextTick, onUnmounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import axios from 'axios'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const { t } = useI18n()

const sourceLang = ref('zh')
const targetLang = ref('en')
const inputText = ref('')
const messages = ref([])
const isRecording = ref(false)
const isTextLoading = ref(false)
const error = ref('')
const recordingTime = ref(0)
const conversationArea = ref(null)

let audioContext = null
let mediaStream = null
let audioWorkletNode = null
let recordedBuffers = []
let recordingTimer = null

const swapLanguages = () => {
  const temp = sourceLang.value
  sourceLang.value = targetLang.value
  targetLang.value = temp
  
  // Toggle rotation animation
  const swapBtn = document.querySelector('.swap-btn')
  if (swapBtn) {
    if (swapBtn.classList.contains('rotate')) {
      swapBtn.classList.remove('rotate')
    } else {
      swapBtn.classList.add('rotate')
    }
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (conversationArea.value) {
      conversationArea.value.scrollTop = conversationArea.value.scrollHeight
    }
  })
}

const translateText = async () => {
  if (!inputText.value.trim()) return
  isTextLoading.value = true
  error.value = ''

  try {
    const response = await axios.post('/api/translate/speech/translate-text', {
      text: inputText.value,
      from: sourceLang.value,
      to: targetLang.value
    })

    if (response.data.code === 200) {
      messages.value.push({
        originalText: inputText.value,
        translatedText: response.data.data.translatedText,
        audioUrl: response.data.data.audioUrl,
        direction: sourceLang.value === 'zh' ? 'right' : 'left',
        sourceLang: sourceLang.value,
        targetLang: targetLang.value
      })
      inputText.value = ''
      scrollToBottom()
    } else {
      error.value = response.data.message || '翻译失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '翻译请求失败'
  } finally {
    isTextLoading.value = false
  }
}

const toggleRecording = async () => {
  if (isRecording.value) {
    stopRecording()
  } else {
    await startRecording()
  }
}

const startRecording = async () => {
  try {
    recordedBuffers = []
    audioContext = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 16000 })
    
    // 加载AudioWorklet处理器
    await audioContext.audioWorklet.addModule('/audio-processor.js')
    
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        sampleRate: 16000,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true
      }
    })

    const source = audioContext.createMediaStreamSource(mediaStream)
    
    // 创建AudioWorkletNode
    audioWorkletNode = new AudioWorkletNode(audioContext, 'pcm-processor')
    
    // 处理从AudioWorkletNode接收的PCM数据
    audioWorkletNode.port.onmessage = (event) => {
      const { type, data } = event.data
      if (type === 'pcmData') {
        recordedBuffers.push(data)
      }
    }

    source.connect(audioWorkletNode)
    audioWorkletNode.connect(audioContext.destination)

    isRecording.value = true
    recordingTime.value = 0
    recordingTimer = setInterval(() => {
      recordingTime.value++
      if (recordingTime.value >= 60) {
        stopRecording()
      }
    }, 1000)
  } catch (err) {
    console.error('录音启动失败:', err)
    notify.error(t('voice.micAccessFailed'))
  }
}

const stopRecording = async () => {
  if (!isRecording.value) return
  isRecording.value = false

  if (recordingTimer) {
    clearInterval(recordingTimer)
    recordingTimer = null
  }
  if (audioWorkletNode) {
    audioWorkletNode.disconnect()
    audioWorkletNode = null
  }
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }

  if (recordedBuffers.length > 0) {
    const totalLength = recordedBuffers.reduce((acc, buf) => acc + buf.length, 0)
    const pcmData = new Int16Array(totalLength)
    let offset = 0
    for (const buf of recordedBuffers) {
      pcmData.set(buf, offset)
      offset += buf.length
    }
    const wavBlob = createWavBlob(pcmData)
    await processAudio(wavBlob)
  }
}

function float32ToPCM16(float32Array) {
  const int16Array = new Int16Array(float32Array.length)
  for (let i = 0; i < float32Array.length; i++) {
    const s = Math.max(-1, Math.min(1, float32Array[i]))
    int16Array[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
  }
  return int16Array
}

function createWavBlob(pcmData) {
  const buffer = new ArrayBuffer(44 + pcmData.length * 2)
  const view = new DataView(buffer)
  writeString(view, 0, 'RIFF')
  view.setUint32(4, 36 + pcmData.length * 2, true)
  writeString(view, 8, 'WAVE')
  writeString(view, 12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, 16000, true)
  view.setUint32(28, 32000, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeString(view, 36, 'data')
  view.setUint32(40, pcmData.length * 2, true)
  let offset = 44
  for (let i = 0; i < pcmData.length; i++) {
    view.setInt16(offset, pcmData[i], true)
    offset += 2
  }
  return new Blob([buffer], { type: 'audio/wav' })
}

function writeString(view, offset, string) {
  for (let i = 0; i < string.length; i++) {
    view.setUint8(offset + i, string.charCodeAt(i))
  }
}

const processAudio = async (audioBlob) => {
  try {
    const formData = new FormData()
    formData.append('audio', new File([audioBlob], 'recording.wav', { type: 'audio/wav' }))
    formData.append('from', sourceLang.value)
    formData.append('to', targetLang.value)
    formData.append('format', 'wav')

    const response = await axios.post('/api/translate/speech/translate', formData, {
      headers: {
        Authorization: localStorage.getItem('token') ? `Bearer ${localStorage.getItem('token')}` : ''
      }
    })

    if (response.data.code === 200) {
      messages.value.push({
        originalText: response.data.data.originalText,
        translatedText: response.data.data.translatedText,
        audioUrl: response.data.data.audioUrl,
        direction: sourceLang.value === 'zh' ? 'right' : 'left',
        sourceLang: sourceLang.value,
        targetLang: targetLang.value
      })
      scrollToBottom()
    } else {
      error.value = response.data.message || '语音翻译失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '语音翻译请求失败'
  }
}

const playMessageAudio = (msg) => {
  if (!msg.translatedText) return
  
  // 先停止之前的播放，避免冲突
  speechSynthesis.cancel()
  
  // 使用浏览器 TTS 发音
  const utterance = new SpeechSynthesisUtterance(msg.translatedText)
  
  // 根据语言设置发音
  const langMap = {
    'zh': 'zh-CN',
    'en': 'en-US',
    'jp': 'ja-JP',
    'kor': 'ko-KR',
    'fra': 'fr-FR',
    'de': 'de-DE'
  }
  
  utterance.lang = langMap[msg.targetLang] || 'zh-CN'
  
  // 添加错误处理
  utterance.onerror = (event) => {
    console.error('语音播放失败:', event)
    notify.error(t('voice.playbackFailed'))
  }
  
  speechSynthesis.speak(utterance)
}

const copyText = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    notify.success(t('voice.copied'))
  } catch {
    notify.error(t('voice.copyFailed'))
  }
}

const goBack = () => {
  router.push('/home/translate')
}

const goToConversation = () => {
  router.push('/conversation')
}

onUnmounted(() => {
  if (recordingTimer) clearInterval(recordingTimer)
  if (audioContext) audioContext.close()
  if (mediaStream) mediaStream.getTracks().forEach(track => track.stop())
})
</script>

<style scoped>
.voice-translate-container {
  min-height: 100vh;
  background: linear-gradient(90deg, #e2e2e2, #c9d6ff);
  color: #333;
}

.translate-header {
  padding: 20px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1000px;
  margin: 0 auto;
}

.back-btn {
  background: none;
  border: none;
  color: #333;
  font-size: 24px;
  cursor: pointer;
}

.header-content h1 {
  color: #333;
}

.voice-translate-main {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.main-content-wrapper {
  display: flex;
  gap: 30px;
  align-items: center;
}

.left-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.right-section {
  width: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 20px;
}

.language-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 30px;
  margin-bottom: 20px;
}

.lang-select {
  padding: 10px 16px;
  border: 2px solid rgba(116, 148, 236, 0.3);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
  color: #333;
  width: 30%;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.lang-select option {
  background: white;
  color: #333;
}

.swap-btn {
  background: rgba(116, 148, 236, 0.2);
  border: none;
  color: #333;
  font-size: 20px;
  border-radius: 50%;
  width: 42px;
  height: 42px;
  cursor: pointer;
  transition: transform 0.3s ease;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.swap-btn.rotate {
  transform: rotate(180deg);
}

.conversation-area {
  height: 60vh;
  overflow-y: auto;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.empty-state {
  text-align: center;
  opacity: 0.8;
  padding-top: 100px;
  color: #666;
}

.messages-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-item.left {
  text-align: left;
}

.message-item.right {
  text-align: right;
}

.message-bubble {
  display: inline-block;
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 14px;
  text-align: left;
}

.message-item.left .message-bubble {
  background: rgba(116, 148, 236, 0.1);
  border: 1px solid rgba(116, 148, 236, 0.3);
}

.message-item.right .message-bubble {
  background: rgba(116, 148, 236, 0.2);
  border: 1px solid rgba(116, 148, 236, 0.4);
}

.message-original {
  font-size: 12px;
  opacity: 0.7;
  color: #666;
}

.message-translated {
  font-size: 16px;
  margin-top: 4px;
  color: #333;
}

.message-actions {
  margin-top: 8px;
  display: flex;
  gap: 6px;
}

.action-btn {
  background: rgba(116, 148, 236, 0.2);
  border: none;
  color: #333;
  border-radius: 6px;
  cursor: pointer;
  padding: 4px 8px;
  transition: background 0.3s ease;
}

.action-btn:hover {
  background: rgba(116, 148, 236, 0.3);
}

.input-area {
  margin-top: 16px;
}

.input-area textarea {
  width: 100%;
  border-radius: 12px;
  border: 2px solid rgba(116, 148, 236, 0.3);
  padding: 12px;
  resize: none;
  background: rgba(255, 255, 255, 0.8);
  color: #333;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.send-btn {
  width: 100%;
  margin-top: 10px;
  border: none;
  border-radius: 10px;
  padding: 12px;
  background: linear-gradient(135deg, #7494ec, #94a8f0);
  color: white;
  cursor: pointer;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.send-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(116, 148, 236, 0.4);
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.record-area {
  text-align: center;
  position: relative;
  margin-top: 18px;
  z-index: 10;
}

.quick-conversation-btn {
  width: 100%;
  margin-top: 20px;
  border: none;
  border-radius: 10px;
  padding: 12px;
  background: linear-gradient(135deg, #7494ec, #94a8f0);
  color: white;
  cursor: pointer;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
}

.quick-conversation-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(116, 148, 236, 0.4);
}

.quick-conversation-btn i {
  font-size: 18px;
}

.record-btn {
  width: 120px;
  height: 120px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9a9e, #fad0c4, #e0c3fc, #8fd3f4);
  color: white;
  font-size: 32px;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  margin: 0 auto;
}

.record-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(116, 148, 236, 0.4);
}

.record-btn.recording {
  background: linear-gradient(135deg, #e74c3c, #f36c5f, #ff9a9e);
  animation: pulse 1.5s infinite;
}

.record-hint {
  margin-top: 12px;
  font-size: 16px;
  background: rgba(116, 148, 236, 0.2);
  color: #333;
  padding: 8px 16px;
  border-radius: 20px;
  display: inline-block;
  font-weight: 500;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.recording-indicator {
  margin-top: 8px;
  color: #e74c3c;
  font-size: 14px;
  font-weight: 500;
}

.error-msg {
  margin-top: 12px;
  color: #e74c3c;
  text-align: center;
  background: rgba(231, 76, 60, 0.1);
  border: 1px solid rgba(231, 76, 60, 0.3);
  padding: 10px;
  border-radius: 8px;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 100px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

@media (max-width: 768px) {
  .main-content-wrapper {
    flex-direction: column;
  }

  .left-section {
    width: 100%;
  }

  .right-section {
    width: 100%;
    padding-top: 0;
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(231, 76, 60, 0.7);
  }
  70% {
    transform: scale(1.05);
    box-shadow: 0 0 0 10px rgba(231, 76, 60, 0);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(231, 76, 60, 0);
  }
}
</style>
