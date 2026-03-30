<template>
  <div class="conversation-mode-container">
    <header class="conversation-header">
      <div class="header-content">
        <button class="back-btn" @click="goBack">
          <i class='bx bx-arrow-back'></i>
        </button>
        <h1>{{ $t('conversation.title') }}</h1>
        <div class="header-right">
          <div class="autoplay-toggle">
            <label class="toggle-label">
              <input type="checkbox" v-model="autoPlay" />
              <span class="toggle-slider"></span>
              <span class="toggle-text">{{ $t('conversation.autoPlay') }}</span>
            </label>
          </div>
          
          <div class="audio-controls" v-if="currentAudio">
            <button class="control-btn" @click="isPlaying ? pauseAudio() : resumeAudio()">
              <i :class="isPlaying ? 'bx bx-pause' : 'bx bx-play'"></i>
            </button>
            <span class="audio-status">{{ isPlaying ? $t('conversation.playing') : $t('conversation.paused') }}</span>
          </div>
        </div>
      </div>
    </header>

    <main class="conversation-main">
      <!-- 璇█閫夋嫨鍣?-->
      <div class="language-selector">
        <select v-model="sourceLang" class="lang-select">
          <option value="zh">涓枃</option>
          <option value="en">English</option>
          <option value="jp">Japanese</option>
          <option value="kor">Korean</option>
          <option value="fra">Fran莽ais</option>
          <option value="de">Deutsch</option>
        </select>
        <button class="swap-btn" @click="swapLanguages" :disabled="isRecording" :class="{ 'rotate-arrow': !isRightToLeft }">
          <i class='bx bx-arrow-back'></i>
        </button>
        <select v-model="targetLang" class="lang-select">
          <option value="zh">涓枃</option>
          <option value="en">English</option>
          <option value="jp">Japanese</option>
          <option value="kor">Korean</option>
          <option value="fra">Fran莽ais</option>
          <option value="de">Deutsch</option>
        </select>
      </div>

      <!-- 宸﹀彸鍒嗘爮瀵硅瘽鍖哄煙 -->
      <div class="conversation-area">
        <div class="left-panel">
          <div class="panel-content" ref="leftPanel">
            <div v-if="messages.length === 0" class="empty-state">
              <p>{{ $t('conversation.startConversation') }}</p>
            </div>
            <div class="messages-container">
              <div
                v-for="(item, index) in leftMessages"
                :key="index"
                class="message-item left"
              >
                <div class="message-bubble">
                  <div class="message-translated">{{ item.translatedText }}</div>
                  <div class="message-actions">
                    <button class="action-btn" @click="playMessageAudio(item)">
                      <i class='bx bx-volume-full'></i>
                    </button>
                    <button class="action-btn" @click="copyText(item.translatedText)">
                      <i class='bx bx-copy'></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="right-panel">
          <div class="panel-content" ref="rightPanel">
            <div v-if="messages.length === 0" class="empty-state">
              <p>{{ $t('conversation.startConversation') }}</p>
            </div>
            <div class="messages-container">
              <div
                v-for="(item, index) in rightMessages"
                :key="index"
                class="message-item right"
              >
                <div class="message-bubble">
                  <div class="message-translated">{{ item.translatedText }}</div>
                  <div class="message-actions">
                    <button class="action-btn" @click="playMessageAudio(item)">
                      <i class='bx bx-volume-full'></i>
                    </button>
                    <button class="action-btn" @click="copyText(item.translatedText)">
                      <i class='bx bx-copy'></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 褰曢煶鎸夐挳 -->
      <div class="record-area">
        <button class="record-btn" :class="{ recording: isRecording }" @click="toggleRecording">
          <i class='bx bx-microphone'></i>
        </button>
        <p class="record-hint">{{ isRecording ? $t('voice.clickToStop') : $t('voice.clickToStart') }}</p>
        <div v-if="isRecording" class="recording-indicator">
          <span class="recording-time">{{ recordingTime }}s / 60s</span>
        </div>
      </div>

      <div v-if="error" class="error-msg">{{ error }}</div>
    </main>
  </div>
</template>

<script setup>
import { ref, nextTick, onUnmounted, computed, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import axios from 'axios'

// 鑾峰彇閫氱煡瀹炰緥
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const { t } = useI18n()

const sourceLang = ref('zh')
const targetLang = ref('en')
const messages = ref([])
const isRecording = ref(false)
const error = ref('')
const recordingTime = ref(0)
const leftPanel = ref(null)
const rightPanel = ref(null)
const isRightToLeft = ref(true)
const autoPlay = ref(true)
const currentAudio = ref(null)
const isPlaying = ref(false)

// 璁＄畻灞炴€ф潵杩囨护娑堟伅
const leftMessages = computed(() => {
  return messages.value.filter(msg => msg && msg.direction === 'left')
})

const rightMessages = computed(() => {
  return messages.value.filter(msg => msg && msg.direction === 'right')
})

let audioContext = null
let mediaStream = null
let audioWorkletNode = null
let recordedBuffers = []
let recordingTimer = null

const swapLanguages = () => {
  isRightToLeft.value = !isRightToLeft.value
}

const playAudio = (audioUrl) => {
  if (currentAudio.value) {
    currentAudio.value.pause()
    currentAudio.value = null
  }
  
  const audio = new Audio(audioUrl)
  currentAudio.value = audio
  isPlaying.value = true
  
  audio.onended = () => {
    isPlaying.value = false
    currentAudio.value = null
  }
  
  audio.onerror = () => {
    isPlaying.value = false
    currentAudio.value = null
    console.error('闊抽鎾斁澶辫触')
  }
  
  audio.play().catch(() => {
    isPlaying.value = false
    currentAudio.value = null
    console.error('闊抽鎾斁澶辫触')
  })
}

const pauseAudio = () => {
  if (currentAudio.value && isPlaying.value) {
    currentAudio.value.pause()
    isPlaying.value = false
  }
}

const resumeAudio = () => {
  if (currentAudio.value && !isPlaying.value) {
    currentAudio.value.play().catch(() => {
      isPlaying.value = false
      console.error('闊抽鎾斁澶辫触')
    })
    isPlaying.value = true
  }
}

const toggleAutoPlay = () => {
  autoPlay.value = !autoPlay.value
}

const scrollToBottom = () => {
  nextTick(() => {
    if (leftPanel.value) {
      leftPanel.value.scrollTop = leftPanel.value.scrollHeight
    }
    if (rightPanel.value) {
      rightPanel.value.scrollTop = rightPanel.value.scrollHeight
    }
  })
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
    
    // 鍔犺浇AudioWorklet澶勭悊鍣?
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
    
    // 鍒涘缓AudioWorkletNode
    audioWorkletNode = new AudioWorkletNode(audioContext, 'pcm-processor')
    
    // 澶勭悊浠嶢udioWorkletNode鎺ユ敹鐨凱CM鏁版嵁
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
    console.error('褰曢煶鍚姩澶辫触:', err)
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
    
    const fromLang = isRightToLeft.value ? targetLang.value : sourceLang.value
    const toLang = isRightToLeft.value ? sourceLang.value : targetLang.value
    
    formData.append('from', fromLang)
    formData.append('to', toLang)
    formData.append('format', 'wav')

    const response = await axios.post('/api/translate/speech/translate', formData, {
      headers: {
        Authorization: localStorage.getItem('token') ? `Bearer ${localStorage.getItem('token')}` : ''
      }
    })

    if (response.data.code === 200) {
      let originalMessage, translatedMessage
      
      if (isRightToLeft.value) {
        originalMessage = {
          originalText: response.data.data.originalText,
          translatedText: response.data.data.originalText,
          audioUrl: response.data.data.audioUrl,
          direction: 'right',
          sourceLang: fromLang,
          targetLang: fromLang
        }
        
        translatedMessage = {
          originalText: response.data.data.translatedText,
          translatedText: response.data.data.translatedText,
          audioUrl: response.data.data.audioUrl,
          direction: 'left',
          sourceLang: toLang,
          targetLang: toLang
        }
      } else {
        originalMessage = {
          originalText: response.data.data.originalText,
          translatedText: response.data.data.originalText,
          audioUrl: response.data.data.audioUrl,
          direction: 'left',
          sourceLang: fromLang,
          targetLang: fromLang
        }
        
        translatedMessage = {
          originalText: response.data.data.translatedText,
          translatedText: response.data.data.translatedText,
          audioUrl: response.data.data.audioUrl,
          direction: 'right',
          sourceLang: toLang,
          targetLang: toLang
        }
      }
      
      messages.value.push(originalMessage)
      messages.value.push(translatedMessage)
      scrollToBottom()
      
      swapLanguages()
      
      if (autoPlay.value && response.data.data.audioUrl) {
        playAudio(response.data.data.audioUrl)
      }
    } else {
      error.value = response.data.message || '璇煶缈昏瘧澶辫触'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '璇煶缈昏瘧璇锋眰澶辫触'
  }
}

const playMessageAudio = async (msg) => {
  if (!msg) {
    notify.error(t('conversation.messageNotFound'))
    return
  }
  if (msg.audioUrl) {
    const audio = new Audio(msg.audioUrl)
    audio.play().catch(() => {
      notify.error(t('conversation.audioPlayFailed'))
    })
    return
  }
  try {
    const response = await axios.post('/api/translate/speech/text-to-speech', {
      text: msg.translatedText,
      lang: msg.targetLang
    })
    if (response.data.code === 200 && response.data.data.audioUrl) {
      const audio = new Audio(response.data.data.audioUrl)
      audio.play()
    } else {
      notify.error(t('conversation.audioFetchFailed'))
    }
  } catch {
    notify.error(t('conversation.audioFetchFailed'))
  }
}

const copyText = async (text) => {
  if (!text) {
    notify.error(t('conversation.noContentToCopy'))
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    notify.success(t('conversation.copied'))
  } catch {
    notify.error(t('conversation.copyFailed'))
  }
}

const goBack = () => {
  router.push('/home/tools')
}

onUnmounted(() => {
  if (recordingTimer) clearInterval(recordingTimer)
  if (audioContext) audioContext.close()
  if (mediaStream) mediaStream.getTracks().forEach(track => track.stop())
})
</script>

<style scoped>
.conversation-mode-container {
  min-height: 100vh;
  background: linear-gradient(90deg, #e2e2e2, #c9d6ff);
  color: #333;
}

.conversation-header {
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

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-right .audio-controls {
  margin-top: 0;
  background: rgba(116, 148, 236, 0.1);
  padding: 5px 12px;
  border-radius: 15px;
}

.header-right .control-btn {
  width: 32px;
  height: 32px;
  font-size: 16px;
}

.header-right .audio-status {
  font-size: 12px;
}

.header-right .autoplay-toggle {
  margin-top: 0;
}

.header-right .toggle-label {
  font-size: 12px;
  gap: 8px;
}

.header-right .toggle-slider {
  width: 38px;
  height: 20px;
}

.header-right .toggle-slider::before {
  width: 16px;
  height: 16px;
}

.header-right .toggle-label input:checked + .toggle-slider::before {
  transform: translateX(18px);
}

.conversation-main {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
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

.swap-btn.rotate-arrow {
  transform: rotate(180deg);
}

.conversation-area {
  display: flex;
  gap: 20px;
  margin-bottom: 40px;
  position: relative;
}

.conversation-area::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 2px;
  background: rgba(116, 148, 236, 0.3);
  transform: translateX(-50%);
}

.left-panel,
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}



.panel-content {
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

.record-area {
  text-align: center;
  position: absolute;
  bottom: 0px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
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
}

.record-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(116, 148, 236, 0.4);
}

.record-btn.recording {
  background: linear-gradient(135deg, #e74c3c, #f36c5f, #ff9a9e);
  animation: pulse 1.5s infinite;
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
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
}

.audio-controls {
  margin-top: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(116, 148, 236, 0.1);
  padding: 8px 16px;
  border-radius: 20px;
}

.control-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: #7494ec;
  color: white;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.control-btn:hover {
  background: #5a7ae0;
  transform: scale(1.05);
}

.audio-status {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.autoplay-toggle {
  margin-top: 10px;
}

.toggle-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}

.toggle-label input {
  display: none;
}

.toggle-slider {
  width: 44px;
  height: 24px;
  background: #ccc;
  border-radius: 12px;
  position: relative;
  transition: background 0.3s ease;
}

.toggle-slider::before {
  content: '';
  position: absolute;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  top: 2px;
  left: 2px;
  transition: transform 0.3s ease;
}

.toggle-label input:checked + .toggle-slider {
  background: #7494ec;
}

.toggle-label input:checked + .toggle-slider::before {
  transform: translateX(20px);
}

.toggle-text {
  font-weight: 500;
}

.error-msg {
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 100px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}
</style>
