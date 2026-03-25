<template>
  <div class="text-translate-page">
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <i class='bx bx-arrow-back'></i>
        {{ $t('common.back') }}
      </button>
      <h1>{{ $t('translate.title') }}</h1>
      <div class="header-spacer"></div>
    </div>

    <div class="content">
      <!-- 语言选择栏 -->
      <div class="language-selector">
        <div class="lang-select-group">
          <label>{{ $t('translate.sourceLang') }}</label>
          <select v-model="sourceLang">
            <option v-for="lang in languages" :key="lang.code" :value="lang.code">
              {{ lang.name }}
            </option>
          </select>
        </div>
        <div class="lang-select-group">
          <label>{{ $t('translate.targetLang') }}</label>
          <select v-model="targetLang" disabled>
            <option value="zh">{{ $t('translate.chinese') }}</option>
          </select>
        </div>
      </div>

      <!-- 翻译输入输出区 -->
      <div class="translate-section">
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
    </div>
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

// 退出登录
const logout = () => {
  // 清除localStorage中的token和用户信息
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  // 跳转到登录页面
  router.push('/')
}

// 返回按钮功能
const goBack = () => {
  router.back()
}
</script>

<style scoped>
.text-translate-page {
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

.translate-section {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
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
  background: rgba(255, 255, 255, 0.8);
}

textarea:focus {
  outline: none;
}

.result-area {
  flex: 1;
  padding: 20px;
  font-size: 18px;
  line-height: 1.6;
  background: rgba(255, 255, 255, 0.8);
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

/* 常用短语 */
.common-phrases {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
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
}

@media (max-width: 768px) {
  .language-selector {
    flex-direction: column;
  }
  
  .page-header {
    padding: 15px;
  }
  
  .page-header h1 {
    font-size: 20px;
  }
  
  .content {
    padding: 15px;
  }
}

@media (max-width: 600px) {
  .back-btn {
    padding: 8px 15px;
    font-size: 14px;
  }
  
  .page-header h1 {
    font-size: 18px;
  }
  
  .header-spacer {
    width: 80px;
  }
}
</style>