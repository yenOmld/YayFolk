import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import en from './locales/en.json'
import ja from './locales/ja.json'
import ko from './locales/ko.json'
import attractionsZh from './locales/attractions_zh.json'
import attractionsEn from './locales/attractions_en.json'
import attractionsJa from './locales/attractions_ja.json'
import attractionsKo from './locales/attractions_ko.json'

const messages = {
  zh: { ...zh, ...attractionsZh },
  en: { ...en, ...attractionsEn },
  ja: { ...ja, ...attractionsJa },
  ko: { ...ko, ...attractionsKo }
}

// 从 localStorage 获取语言设置，默认中文
const getLocale = () => {
  const savedLocale = localStorage.getItem('locale')
  if (savedLocale && ['zh', 'en', 'ja', 'ko'].includes(savedLocale)) {
    return savedLocale
  }
  
  // 根据浏览器语言自动检测
  const browserLang = navigator.language || navigator.userLanguage
  const langMap = {
    'zh': 'zh',
    'zh-CN': 'zh',
    'zh-TW': 'zh',
    'zh-HK': 'zh',
    'en': 'en',
    'en-US': 'en',
    'en-GB': 'en',
    'ja': 'ja',
    'ja-JP': 'ja',
    'ko': 'ko',
    'ko-KR': 'ko'
  }
  
  return langMap[browserLang] || 'zh'
}

const i18n = createI18n({
  legacy: false,
  locale: getLocale(),
  fallbackLocale: 'zh',
  messages
})

export default i18n

// 切换语言函数
export const setLocale = (locale) => {
  if (['zh', 'en', 'ja', 'ko'].includes(locale)) {
    i18n.global.locale.value = locale
    localStorage.setItem('locale', locale)
    document.querySelector('html').setAttribute('lang', locale)
  }
}

// 获取当前语言
export const getCurrentLocale = () => i18n.global.locale.value
