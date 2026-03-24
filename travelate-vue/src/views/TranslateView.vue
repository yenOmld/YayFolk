<template>
  <div class="translate-page">
    <!-- 主要内容区 -->
    <main class="main-content">
      <!-- 欢迎语 -->
      <div class="welcome-section">
        <div class="welcome-content">
          <img src="https://travelate.oss-cn-wuhan-lr.aliyuncs.com/icons/welcome-icon.png" alt="Travelate" class="welcome-icon" />
          <div class="welcome-text">
            <h1>{{ $t('translate.title') }}</h1>
            <p>{{ $t('translate.subtitle') }}</p>
          </div>
        </div>
      </div>

      <!-- 翻译区域 -->
      <div class="translate-container">
        <!-- 语言选择栏 -->
        <div class="language-bar">
          <div class="lang-select">
            <select v-model="sourceLang">
              <option v-for="lang in languages" :key="lang.code" :value="lang.code">
                {{ lang.name }}
              </option>
            </select>
          </div>
          <div class="lang-select">
            <select v-model="targetLang" disabled>
              <option value="zh">{{ $t('translate.chinese') }}</option>
            </select>
          </div>
        </div>

        <!-- 翻译输入输出区 -->
        <div class="translate-boxes">
          <!-- 输入区 -->
          <div class="input-box">
            <div class="box-header">
              <span class="lang-label">{{ getLangName(sourceLang) }}</span>
              <div class="tools">
                <button @click="clearInput" :title="$t('translate.clear')">
                  <i class='bx bx-trash'></i>
                </button>
                <button @click="speak(sourceText, sourceLang)" :title="$t('translate.speak')">
                  <i class='bx bx-volume-full'></i>
                </button>
              </div>
            </div>
            <textarea 
              v-model="sourceText" 
              :placeholder="$t('translate.inputPlaceholder')"
              @input="handleInput"
            ></textarea>
            <div class="box-footer">
              <span class="char-count">{{ sourceText.length }} / 5000</span>
              <button class="translate-btn" @click="translate" :disabled="!sourceText || isTranslating">
                <i class='bx bx-search'></i>
                {{ isTranslating ? $t('translate.translating') : $t('translate.translateBtn') }}
              </button>
            </div>
          </div>

          <!-- 输出区 -->
          <div class="output-box">
            <div class="box-header">
              <span class="lang-label">{{ getLangName(targetLang) }}</span>
              <div class="tools">
                <button @click="copyResult" :title="$t('translate.copy')">
                  <i class='bx bx-copy'></i>
                </button>
                <button @click="speak(translatedText, targetLang)" :title="$t('translate.speak')">
                  <i class='bx bx-volume-full'></i>
                </button>
                <button @click="saveToFavorites" :title="$t('translate.favorite')">
                  <i class='bx bx-star'></i>
                </button>
              </div>
            </div>
            <div class="result-area">
              <p v-if="translatedText" class="translated-text">{{ translatedText }}</p>
              <p v-else class="placeholder">{{ $t('translate.outputPlaceholder') }}</p>
            </div>
            <div class="box-footer">
              <span v-if="translatedText" class="translation-info">
                <i class='bx bx-check-circle'></i> {{ $t('translate.translationComplete') }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷功能 -->
      <div class="quick-features">
        <div class="feature-card" @click="openCamera">
          <i class='bx bx-camera'></i>
          <span>{{ $t('translate.camera') }}</span>
        </div>
        <div class="feature-card" @click="openVoice">
          <i class='bx bx-microphone'></i>
          <span>{{ $t('translate.voice') }}</span>
        </div>
        <div class="feature-card" @click="openLandmark">
          <i class='bx bx-map'></i>
          <span>{{ $t('translate.landmark') }}</span>
        </div>
      </div>

      <!-- 常用短语 -->
      <div class="common-phrases">
        <h3><i class='bx bx-book'></i> {{ $t('translate.commonPhrases') }}</h3>
        <div class="phrases-grid">
          <div 
            v-for="phrase in commonPhrases" 
            :key="phrase.id"
            class="phrase-card"
          >
            <div class="phrase-content" @click="usePhrase(phrase.text)">
              <span class="phrase-text">{{ phrase.originalText || phrase.text }}</span>
              <span class="phrase-category">{{ phrase.category }}</span>
            </div>
            <button class="delete-btn" @click.stop="deletePhrase(phrase.id)" :title="$t('common.delete')">
              <i class='bx bx-trash'></i>
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useI18n } from 'vue-i18n'
import { getPhrases, addPhrase, deletePhrase as deletePhraseAPI } from '../api/app'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm

const { t } = useI18n()
const router = useRouter()

// 本地默认头像（用于网络不可用时的回退）
const localDefaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI0MCIgZmlsbD0iIzc0OTRlYyIvPgogIDxjaXJjbGUgY3g9IjUwIiBjeT0iMzAiIHI9IjIwIiBmaWxsPSIjNzQ5NGVjIi8+CiAgPGNpcmNsZSBjeD0iNTUiIGN5PSI0NSIgcj0iNSIgZmlsbD0iI2ZmZiIvPgogIDxjaXJjbGUgY3g9IjQ1IiBjeT0iNDUiIHI9IjUiIGZpbGw9IiNmZmYiLz4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjYwIiByPSIyIiBmaWxsPSIjNzQ5NGVjIi8+CiAgPHRleHQgeD0iNTAiIHk9Ijc1IiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTIiIGZpbGw9IiMzMzMiIG5hbWU9ImNvbnRlbnQiPldyb3lhbC4uLjwvdGV4dD4KPC9zdmc+'

// 用户头像
const userAvatar = ref('https://api.dicebear.com/7.x/avataaars/svg?seed=Felix')

// 检查头像URL是否可访问
const checkAvatarAccessibility = (url) => {
  return new Promise((resolve) => {
    if (!url || url === localDefaultAvatar) {
      resolve(true)
      return
    }
    
    const img = new Image()
    img.onload = () => resolve(true)
    img.onerror = () => resolve(false)
    img.src = url
  })
}

// 页面加载时获取用户信息和头像
onMounted(async () => {
  // 从本地存储获取用户信息
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    if (user.avatar) {
      userAvatar.value = user.avatar
      // 检查头像是否可访问
      const isAccessible = await checkAvatarAccessibility(user.avatar)
      if (!isAccessible) {
        userAvatar.value = localDefaultAvatar
      }
    }
  } else {
    // 本地存储中没有用户信息，检查默认头像是否可访问
    const isAccessible = await checkAvatarAccessibility(userAvatar.value)
    if (!isAccessible) {
      userAvatar.value = localDefaultAvatar
    }
  }
  
  // 加载旅行常用语
  await loadPhrases()
})

// 语言列表
const languages = computed(() => [
  { code: 'auto', name: t('translate.autoDetect') },
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

// 常用短语
const commonPhrases = ref([])

// 响应式数据
const sourceLang = ref('en')
const targetLang = ref('zh')
const sourceText = ref('')
const translatedText = ref('')
const isTranslating = ref(false)

// 加载旅行常用语
const loadPhrases = async () => {
  try {
    const response = await getPhrases()
    if (response.code === 200) {
      // 将后端数据映射到前端需要的格式
      const phrases = response.data || []
      if (phrases.length > 0) {
        commonPhrases.value = phrases.map(p => ({
          id: p.id,
          text: p.text,
          originalText: p.originalText,
          category: p.category
        }))
      } else {
        // 如果用户没有常用语，显示空列表（新用户注册时会自动初始化）
        commonPhrases.value = []
      }
    }
  } catch (error) {
    console.error('加载旅行常用语失败:', error)
    // 如果加载失败，显示空列表
    commonPhrases.value = []
  }
}

// 获取语言名称
const getLangName = (code) => {
  const lang = languages.value.find(l => l.code === code)
  return lang ? lang.name : code
}

// 输入处理（防抖翻译）
let translateTimeout
const handleInput = () => {
  clearTimeout(translateTimeout)
  if (sourceText.value.length > 0) {
    translateTimeout = setTimeout(() => {
      translate()
    }, 1000)
  }
}

// 翻译功能
const translate = async () => {
  if (!sourceText.value) return
  
  isTranslating.value = true
  
  try {
    const token = localStorage.getItem('token')
    const response = await axios.post('/api/translate', {
      text: sourceText.value,
      sourceLang: sourceLang.value,
      targetLang: targetLang.value
    }, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    })
    
    if (response.data.code === 200) {
      translatedText.value = response.data.translatedText
    } else {
      notify.error(t('translate.translateFailed') + '：' + response.data.message)
    }
  } catch (error) {
    console.error('翻译请求失败:', error)
    if (error.response && error.response.status === 401) {
      notify.error(t('translate.loginRequired'))
      router.push('/')
    } else {
      notify.error(t('translate.translateFailedRetry'))
    }
  } finally {
    isTranslating.value = false
  }
}

// 清空输入
const clearInput = () => {
  sourceText.value = ''
  translatedText.value = ''
}

// 朗读功能
const speak = (text, lang) => {
  if (!text) return
  
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = lang === 'zh' ? 'zh-CN' : lang
  speechSynthesis.speak(utterance)
}

// 复制结果
const copyResult = () => {
  if (!translatedText.value) return
  navigator.clipboard.writeText(translatedText.value)
  notify.success(t('translate.copied'))
}

// 收藏
const saveToFavorites = async () => {
  if (!translatedText.value) {
    notify.warning(t('translate.noContentToSave'))
    return
  }
  
  try {
    const response = await addPhrase({
      text: translatedText.value,
      originalText: sourceText.value,
      category: '收藏'
    })
    
    if (response.code === 200) {
      notify.success(t('translate.addedToPhrases'))
      await loadPhrases()
    } else {
      notify.error(t('translate.saveFailed') + '：' + response.message)
    }
  } catch (error) {
    console.error('收藏失败:', error)
    notify.error(t('translate.saveFailedRetry'))
  }
}

// 使用常用短语
const usePhrase = (text) => {
  if (!text) return
  
  // 先停止之前的播放，避免冲突
  speechSynthesis.cancel()
  
  // 使用浏览器 TTS 发音
  const utterance = new SpeechSynthesisUtterance(text)
  
  // 固定使用中文发音，因为text字段总是中文
  utterance.lang = 'zh-CN'
  
  // 添加错误处理
  utterance.onerror = (event) => {
    console.error('语音播放失败:', event)
  }
  
  speechSynthesis.speak(utterance)
}

// 删除短语
const deletePhrase = async (phraseId) => {
  confirm({
    title: t('translate.confirmDeletePhraseTitle'),
    message: t('translate.confirmDeletePhrase'),
    confirmText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onConfirm: async () => {
      try {
        const response = await deletePhraseAPI(phraseId)
        if (response.code === 200) {
          notify.success(t('translate.deleteSuccess'))
          await loadPhrases()
        } else {
          notify.error(t('translate.deleteFailed') + '：' + response.message)
        }
      } catch (error) {
        console.error('删除失败:', error)
        notify.error(t('translate.deleteFailedRetry'))
      }
    }
  })
}

// 快捷功能
const openCamera = () => {
  router.push('/camera-translate')
}
const openVoice = () => {
  router.push('/voice-translate')
}
const openLandmark = () => {
  router.push('/landmark-recognition')
}

// 退出登录
const logout = () => {
  // 清除localStorage中的token和用户信息
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  // 跳转到登录页面
  router.push('/')
}
</script>

<style scoped>
.translate-page {
  min-height: 100vh;
  //background: linear-gradient(90deg, #83b3f7ff, #c9d6ff);
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

/* 主内容区 */
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

/* 欢迎语 */
.welcome-section {
  text-align: center;
  color: white;
  margin-bottom: 20px;
}

.welcome-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.welcome-icon {
  width: 150px;
  height: 150px;
  object-fit: contain;
}

.welcome-text {
  text-align: left;
}

.welcome-text h1 {
  font-size: 32px;
  margin-bottom: 8px;
}

.welcome-text p {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

/* 翻译容器 */
.translate-container {
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  margin-bottom: 30px;
}

/* 语言选择栏 */
.language-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.lang-select select {
  padding: 12px 40px 12px 20px;
  border: 2px solid #e9ecef;
  border-radius: 10px;
  font-size: 16px;
  background: white;
  cursor: pointer;
  min-width: 180px;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 15px center;
}

.lang-select select:focus {
  outline: none;
  border-color: #7494ec;
}

/* 翻译输入输出区 */
.translate-boxes {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 300px;
}

.input-box,
.output-box {
  display: flex;
  flex-direction: column;
}

.input-box {
  border-right: 1px solid #e9ecef;
}

.box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.lang-label {
  font-weight: 600;
  color: #7494ec;
}

.tools {
  display: flex;
  gap: 10px;
}

.tools button {
  background: none;
  border: none;
  font-size: 20px;
  color: #666;
  cursor: pointer;
  padding: 5px;
  transition: color 0.3s;
}

.tools button:hover {
  color: #7494ec;
}

textarea {
  flex: 1;
  padding: 20px;
  border: none;
  resize: none;
  font-size: 18px;
  line-height: 1.6;
  font-family: inherit;
}

textarea:focus {
  outline: none;
}

.result-area {
  flex: 1;
  padding: 20px;
  font-size: 18px;
  line-height: 1.6;
}

.translated-text {
  color: #333;
}

.placeholder {
  color: #999;
  font-style: italic;
}

.box-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f8f9fa;
  border-top: 1px solid #e9ecef;
}

.char-count {
  color: #999;
  font-size: 14px;
}

.translate-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 25px;
  background: linear-gradient(135deg, #7494ec, #6381d9, #526fd6, #415ed3);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.translate-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #6381d9, #526fd6, #415ed3, #304dcc);
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(116, 148, 236, 0.4);
}

.translate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.translation-info {
  color: #28a745;
  font-size: 14px;
}

/* 快捷功能 */
.quick-features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.feature-card {
  background: white;
  border-radius: 15px;
  padding: 25px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.feature-card i {
  font-size: 36px;
  color: #7494ec;
  margin-bottom: 10px;
}

.feature-card span {
  display: block;
  color: #333;
  font-weight: 500;
}

/* 常用短语 */
.common-phrases {
  background: white;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.common-phrases h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  color: #333;
}

.common-phrases h3 i {
  color: #7494ec;
}

.phrases-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
}

.phrase-card {
  padding: 15px 20px;
  background: #f8f9fa;
  border-radius: 10px;
  transition: all 0.3s;
  border: 2px solid transparent;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.phrase-card:hover {
  border-color: #7494ec;
  background: #f0f4ff;
}

.phrase-content {
  flex: 1;
  cursor: pointer;
}

.phrase-text {
  display: block;
  color: #333;
  font-weight: 500;
  margin-bottom: 5px;
}

.phrase-category {
  font-size: 12px;
  color: #999;
}

.delete-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 5px;
  transition: color 0.3s;
  margin-left: 10px;
}

.delete-btn:hover {
  color: #e74c3c;
}

/* 响应式设计 */
@media (max-width: 900px) {
  .translate-boxes {
    grid-template-columns: 1fr;
  }
  
  .input-box {
    border-right: none;
    border-bottom: 1px solid #e9ecef;
  }
  
  .navbar {
    padding: 15px 20px;
  }
  
  .nav-links {
    display: none;
  }
}

@media (max-width: 768px) {
  .quick-features {
    gap: 15px;
  }
  
  .feature-card {
    padding: 20px;
  }
  
  .feature-card i {
    font-size: 30px;
  }
  
  .feature-card span {
    font-size: 14px;
  }
}

@media (max-width: 600px) {
  .quick-features {
    gap: 10px;
  }
  
  .feature-card {
    padding: 15px;
  }
  
  .feature-card i {
    font-size: 28px;
  }
  
  .feature-card span {
    font-size: 12px;
  }
  
  .language-bar {
    flex-direction: column;
    gap: 10px;
  }

  .welcome-content {
    flex-direction: column;
    gap: 10px;
  }

  .welcome-icon {
    width: 100px;
    height: 100px;
  }

  .welcome-text {
    text-align: center;
  }

  .welcome-text h1 {
    font-size: 24px;
  }

  .welcome-text p {
    font-size: 14px;
  }
}

/* 中等屏幕 - 只显示图标 */
@media (max-width: 550px) {
  .quick-features {
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }
  
  .feature-card {
    padding: 15px;
  }
  
  .feature-card i {
    font-size: 32px;
    margin-bottom: 0;
  }
  
  .feature-card span {
    display: none;
  }
}

/* 小屏幕 - 单列布局 */
@media (max-width: 480px) {
  .quick-features {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  
  .feature-card {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 15px;
    padding: 15px 20px;
  }
  
  .feature-card i {
    font-size: 24px;
    margin-bottom: 0;
  }
  
  .feature-card span {
    display: block;
    font-size: 16px;
  }
}
</style>
