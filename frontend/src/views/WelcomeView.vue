<template>
  <div class="welcome-container">
    <!-- 视频背景 -->
    <div class="video-background">
      <video 
        autoplay 
        muted 
        loop 
        playsinline
        preload="auto"
        poster="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1920 1080'%3E%3Crect fill='%238B0000' width='1920' height='1080'/%3E%3C/svg%3E"
        ref="videoRef"
      >
        <source src="/backgroung.mp4" type="video/mp4">
        您的浏览器不支持视频标签
      </video>
      <div class="video-overlay"></div>
    </div>

    <!-- 声音控制按钮 -->
    <button class="sound-toggle" @click="toggleSound" :class="{ 'sound-on': !isMuted }">
      <i :class="isMuted ? 'bx bx-volume-mute' : 'bx bx-volume-full'"></i>
    </button>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- Logo 和标题 -->
      <div class="logo-section">
        <h1 class="app-title">yayfolk</h1>
        <p class="app-subtitle" ref="subtitleRef">——专注非遗文化传播</p>
        <button class="login-arrow" :class="{ pulling: isCloudLifting }" @click="slideToLogin">
          <img :src="cloudImg" alt="祥云" class="cloud-arrow" />
        </button>
      </div>

      <!-- 描述文字 -->
      <div class="description-section">
        <p class="description-text">
          <span class="desc-line">
            <span v-for="(ch, i) in line1Chars" :key="'l1-'+i" class="desc-char">{{ ch }}</span>
          </span>
          <br>
          <span class="desc-line">
            <span v-for="(ch, i) in line2Chars" :key="'l2-'+i" class="desc-char">{{ ch }}</span>
          </span>
        </p>
      </div>
    </div>

    <!-- 触发登录箭头已移动到标题下方 -->

    <!-- 登录面板 -->
    <transition name="pull-up">
      <div class="login-panel" v-if="showLoginPanel" @click.self="closeLoginPanel">
        <div class="panel-content">
        <button class="close-btn" @click="closeLoginPanel">
          <i class='bx bx-x'></i>
        </button>
        
        <div class="container" :class="{ active: isActive }">
          <div class="form-box login">
            <form action="#" @submit.prevent="handleLogin">
              <h1>{{ $t('login.title') }}</h1>
              <div class="input-box">
                <input type="text" :placeholder="$t('login.username')" v-model="loginForm.username" required>
                <i class='bx bxs-user'></i>
              </div>
              <div class="input-box">
                <input type="password" :placeholder="$t('login.password')" v-model="loginForm.password" required>
                <i class='bx bxs-lock-alt'></i>
              </div>
              <div class="forgot-link">
                <a href="#" @click.prevent="showResetPassword">{{ $t('login.forgotPassword') }}</a>
              </div>
              <button type="submit" class="btn" :disabled="isLoggingIn">
                {{ isLoggingIn ? $t('login.loggingIn') : $t('login.loginBtn') }}
              </button>
              <p>{{ $t('login.socialLogin') }}</p>
              <div class="social-icons">
                <a href="#"><i class='bx bxl-google'></i></a>
                <a href="#"><i class='bx bxl-facebook'></i></a>
                <a href="/api/oauth/github/login"><i class='bx bxl-github'></i></a>
                <a href="#"><i class='bx bxl-linkedin'></i></a>
              </div>
            </form>
          </div>

          <div class="form-box register">
            <form action="#" @submit.prevent="handleRegister">
              <h1>{{ $t('login.register') }}</h1>
              <div class="input-box">
                <input 
                  type="text" 
                  :placeholder="$t('login.username')" 
                  v-model="registerForm.username"
                  @input="validateUsername('register')"
                  required
                >
                <i :class="registerForm.isPhone ? 'bx bxs-phone' : 'bx bxs-envelope'"></i>
              </div>
              <div class="input-box">
                <input 
                  type="text" 
                  :placeholder="$t('login.verificationCode')" 
                  v-model="registerForm.verificationCode" 
                  required
                >
                <button 
                  type="button" 
                  class="verify-btn" 
                  @click="sendVerificationCode"
                  :disabled="!registerForm.username || isSendingCode || countdown > 0"
                >
                  {{ countdown > 0 ? `${countdown}s` : $t('login.getCode') }}
                </button>
              </div>
              <div class="input-box">
                <input 
                  type="password" 
                  :placeholder="$t('login.password')" 
                  v-model="registerForm.password" 
                  @input="validatePassword('register')"
                  required
                >
                <i class='bx bxs-lock-alt'></i>
              </div>
              <div class="input-box">
                <input 
                  type="password" 
                  :placeholder="$t('login.confirmPassword')" 
                  v-model="registerForm.confirmPassword" 
                  @input="validateConfirmPassword('register')"
                  required
                >
                <i class='bx bxs-lock-alt'></i>
              </div>
              <div class="password-hint" v-if="passwordHint">
                {{ passwordHint }}
              </div>
              <button type="submit" class="btn" :disabled="!isRegisterFormValid || isRegistering">
                {{ isRegistering ? $t('login.registering') : $t('login.registerBtn') }}
              </button>
              <p>{{ $t('login.socialLogin') }}</p>
              <div class="social-icons">
                <a href="#"><i class='bx bxl-google'></i></a>
                <a href="#"><i class='bx bxl-facebook'></i></a>
                <a href="/api/oauth/github/login"><i class='bx bxl-github'></i></a>
                <a href="#"><i class='bx bxl-linkedin'></i></a>
              </div>
            </form>
          </div>

          <!-- 密码重置弹窗 -->
          <div class="modal" v-if="showResetModal">
            <div class="modal-content">
              <h2>{{ $t('login.resetPassword') }}</h2>
              <div class="input-box">
                <input 
                  type="text" 
                  :placeholder="$t('login.username')" 
                  v-model="resetForm.username"
                  @input="validateUsername('reset')"
                  required
                >
                <i :class="resetForm.isPhone ? 'bx bxs-phone' : 'bx bxs-envelope'"></i>
              </div>
              <div class="input-box">
                <input 
                  type="text" 
                  :placeholder="$t('login.verificationCode')" 
                  v-model="resetForm.verificationCode" 
                  required
                >
                <button 
                  type="button" 
                  class="verify-btn" 
                  @click="sendResetVerificationCode"
                  :disabled="!resetForm.username || isSendingCode || countdown > 0"
                >
                  {{ countdown > 0 ? `${countdown}s` : $t('login.getCode') }}
                </button>
              </div>
              <div class="input-box">
                <input 
                  type="password" 
                  :placeholder="$t('login.newPassword')" 
                  v-model="resetForm.password" 
                  @input="validatePassword('reset')"
                  required
                >
                <i class='bx bxs-lock-alt'></i>
              </div>
              <div class="input-box">
                <input 
                  type="password" 
                  :placeholder="$t('login.confirmPassword')" 
                  v-model="resetForm.confirmPassword" 
                  @input="validateConfirmPassword('reset')"
                  required
                >
                <i class='bx bxs-lock-alt'></i>
              </div>
              <div class="password-hint" v-if="passwordHint">
                {{ passwordHint }}
              </div>
              <div class="modal-buttons">
                <button class="btn secondary" @click="showResetModal = false">{{ $t('common.cancel') }}</button>
                <button class="btn primary" @click="handleResetPassword" :disabled="!isResetFormValid">{{ $t('login.resetPassword') }}</button>
              </div>
            </div>
          </div>

          <div class="toggle-box">
            <div class="toggle-panel toggle-left">
              <h1>{{ $t('login.welcomeBack') }}</h1>
              <p>{{ $t('login.noAccount') }}</p>
              <button class="btn register-btn" @click="showRegister">{{ $t('login.registerBtn') }}</button>
            </div>

            <div class="toggle-panel toggle-right">
              <h1>{{ $t('login.welcomeBack') }}</h1>
              <p>{{ $t('login.hasAccount') }}</p>
              <button class="btn login-btn" @click="showLogin">{{ $t('login.loginBtn') }}</button>
            </div>
          </div>
        </div>
        </div>
      </div>
    </transition>

    <!-- 漂浮装饰元素 -->
    <div class="floating-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { login, register, sendCode, resetPassword } from '../api/app'
import { useI18n } from 'vue-i18n'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const { t } = useI18n()
const isSidebarHover = ref(false)
const showLoginPanel = ref(false)
const isActive = ref(false) // 用于切换登录/注册
const isLoggingIn = ref(false)
const isRegistering = ref(false)
const isMuted = ref(true)
const videoRef = ref(null)
const showResetModal = ref(false)
const isSendingCode = ref(false)
const countdown = ref(0)
const passwordHint = ref('')
const isCloudLifting = ref(false)

const subtitleRef = ref(null)
const subtitleWidth = ref(0)
const cloudImg = '/cloud.png'
const line1Chars = computed(() => Array.from('以非遗为媒，连接世界'))
const line2Chars = computed(() => Array.from('yayfolk—体验无界'))

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  verificationCode: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  country: 'United States',
  langCode: 'en',
  regionCode: 'US',
  isPhone: false
})

const resetForm = reactive({
  username: '',
  verificationCode: '',
  password: '',
  confirmPassword: '',
  isPhone: false
})

// 语言代码到地区信息的映射
const languageToRegion = {
  'zh-CN': { country: 'China', langCode: 'zh', regionCode: 'CN' },
  'zh-TW': { country: 'China', langCode: 'zh', regionCode: 'TW' },
  'zh-HK': { country: 'China', langCode: 'zh', regionCode: 'HK' },
  'ja': { country: 'Japan', langCode: 'ja', regionCode: 'JP' },
  'ja-JP': { country: 'Japan', langCode: 'ja', regionCode: 'JP' },
  'ko': { country: 'South Korea', langCode: 'ko', regionCode: 'KR' },
  'ko-KR': { country: 'South Korea', langCode: 'ko', regionCode: 'KR' },
  'en': { country: 'United States', langCode: 'en', regionCode: 'US' },
  'en-US': { country: 'United States', langCode: 'en', regionCode: 'US' },
  'en-GB': { country: 'United Kingdom', langCode: 'en', regionCode: 'GB' },
  'de': { country: 'Germany', langCode: 'de', regionCode: 'DE' },
  'de-DE': { country: 'Germany', langCode: 'de', regionCode: 'DE' },
  'fr': { country: 'France', langCode: 'fr', regionCode: 'FR' },
  'fr-FR': { country: 'France', langCode: 'fr', regionCode: 'FR' },
  'es': { country: 'Spain', langCode: 'es', regionCode: 'ES' },
  'es-ES': { country: 'Spain', langCode: 'es', regionCode: 'ES' },
  'it': { country: 'Italy', langCode: 'it', regionCode: 'IT' },
  'it-IT': { country: 'Italy', langCode: 'it', regionCode: 'IT' },
  'pt': { country: 'Brazil', langCode: 'pt', regionCode: 'BR' },
  'pt-BR': { country: 'Brazil', langCode: 'pt', regionCode: 'BR' },
  'ru': { country: 'Russia', langCode: 'ru', regionCode: 'RU' },
  'ru-RU': { country: 'Russia', langCode: 'ru', regionCode: 'RU' },
  'th': { country: 'Thailand', langCode: 'th', regionCode: 'TH' },
  'th-TH': { country: 'Thailand', langCode: 'th', regionCode: 'TH' },
  'vi': { country: 'Vietnam', langCode: 'vi', regionCode: 'VN' },
  'vi-VN': { country: 'Vietnam', langCode: 'vi', regionCode: 'VN' },
  'ms': { country: 'Malaysia', langCode: 'ms', regionCode: 'MY' },
  'ms-MY': { country: 'Malaysia', langCode: 'ms', regionCode: 'MY' },
  'id': { country: 'Indonesia', langCode: 'id', regionCode: 'ID' },
  'id-ID': { country: 'Indonesia', langCode: 'id', regionCode: 'ID' },
  'hi': { country: 'India', langCode: 'hi', regionCode: 'IN' },
  'hi-IN': { country: 'India', langCode: 'hi', regionCode: 'IN' }
}

// 自动检测国家/地区
const detectCountry = async () => {
  let detectedInfo = null
  
  // 1. 尝试从浏览器语言获取
  const browserLang = navigator.language || navigator.userLanguage
  const langCode = browserLang.toLowerCase()
  
  if (languageToRegion[langCode]) {
    detectedInfo = languageToRegion[langCode]
  } else {
    const shortLang = langCode.split('-')[0]
    if (languageToRegion[shortLang]) {
      detectedInfo = languageToRegion[shortLang]
    }
  }
  
  // 2. 如果语言检测失败，尝试IP定位
  if (!detectedInfo) {
    try {
      const response = await fetch('https://ipapi.co/json/')
      const data = await response.json()
      if (data.country_name) {
        // 根据国家设置对应的语言
        let countryLangCode = 'en' // 默认英文
        if (data.country_name === 'Japan') {
          countryLangCode = 'ja'
        } else if (data.country_name === 'Korea, Republic of') {
          countryLangCode = 'ko'
        } else if (data.country_name === 'China' || data.country_name === 'Taiwan' || data.country_name === 'Hong Kong' || data.country_name === 'Macao') {
          countryLangCode = 'zh'
        }
        
        detectedInfo = {
          country: data.country_name,
          langCode: countryLangCode,
          regionCode: data.country_code || 'US'
        }
      }
    } catch (error) {
      console.log('IP定位失败:', error)
    }
  }
  
  // 3. 如果都失败，使用默认值
  if (!detectedInfo) {
    detectedInfo = { country: 'United States', langCode: 'en', regionCode: 'US' }
  }
  
  registerForm.country = detectedInfo.country
  registerForm.langCode = detectedInfo.langCode
  registerForm.regionCode = detectedInfo.regionCode
}

// 验证用户名（判断是手机号还是邮箱）
const validateUsername = (type = 'register') => {
  const username = type === 'reset' ? resetForm.username : registerForm.username
  if (!username) return
  
  // 简单判断是手机号还是邮箱
  if (type === 'reset') {
    resetForm.isPhone = username.includes('@') ? false : true
  } else {
    registerForm.isPhone = username.includes('@') ? false : true
  }
}

// 验证密码
const validatePassword = (type = 'register') => {
  const password = type === 'reset' ? resetForm.password : registerForm.password
  if (!password) {
    passwordHint.value = ''
    return
  }
  
  // 密码规则：长度8-20位，包含字母和数字还有部分特殊字符
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/
  
  if (password.length < 8) {
    passwordHint.value = '密码长度至少8位'
  } else if (password.length > 20) {
    passwordHint.value = '密码长度不能超过20位'
  } else if (!/(?=.*[A-Za-z])/.test(password)) {
    passwordHint.value = '密码必须包含字母'
  } else if (!/(?=.*\d)/.test(password)) {
    passwordHint.value = '密码必须包含数字'
  } else if (!/(?=.*[@$!%*#?&._])/.test(password)) {
    passwordHint.value = '密码必须包含特殊字符(@$!%*#?&)'
  } else {
    passwordHint.value = ''
  }
}

// 密码格式验证函数
const isPasswordValid = (password) => {
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&._])[A-Za-z\d@$!%*#?&._]{8,20}$/
  return passwordRegex.test(password)
}

// 验证确认密码
const validateConfirmPassword = (type = 'register') => {
  const password = type === 'reset' ? resetForm.password : registerForm.password
  const confirmPassword = type === 'reset' ? resetForm.confirmPassword : registerForm.confirmPassword
  
  // 如果第一个密码框格式不正确，不显示"两次输入的密码不一致"
  if (!isPasswordValid(password)) {
    return
  }
  
  if (confirmPassword && password !== confirmPassword) {
    passwordHint.value = '两次输入的密码不一致'
  } else if (passwordHint.value === '两次输入的密码不一致') {
    passwordHint.value = ''
  }
}

// 计算表单是否有效
const isRegisterFormValid = computed(() => {
  return !passwordHint.value && 
    registerForm.username && 
    registerForm.verificationCode && 
    registerForm.password && 
    registerForm.confirmPassword && 
    registerForm.password === registerForm.confirmPassword &&
    isPasswordValid(registerForm.password)
})

const isResetFormValid = computed(() => {
  return !passwordHint.value && 
    resetForm.username && 
    resetForm.verificationCode && 
    resetForm.password && 
    resetForm.confirmPassword && 
    resetForm.password === resetForm.confirmPassword &&
    isPasswordValid(resetForm.password)
})

// 发送验证码
const sendVerificationCode = async () => {
  if (!registerForm.username) {
    notify.warning(t('login.enterPhoneOrEmail'))
    return
  }
  
  isSendingCode.value = true
  
  try {
    const data = await sendCode({ username: registerForm.username })
    
    if (data.code === 200) {
      startCountdown()
      notify.success(t('login.codeSent'))
    } else {
      notify.error(data.message || t('login.sendCodeFailed'))
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    notify.error(t('login.sendCodeFailedRetry'))
  } finally {
    isSendingCode.value = false
  }
}

// 发送重置密码验证码
const sendResetVerificationCode = async () => {
  if (!resetForm.username) {
    notify.warning(t('login.enterPhoneOrEmail'))
    return
  }
  
  isSendingCode.value = true
  
  try {
    const data = await sendCode({ username: resetForm.username })
    
    if (data.code === 200) {
      startCountdown()
      notify.success(t('login.codeSent'))
    } else {
      notify.error(data.message || t('login.sendCodeFailed'))
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    notify.error(t('login.sendCodeFailedRetry'))
  } finally {
    isSendingCode.value = false
  }
}

// 开始倒计时
const startCountdown = () => {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

// 显示注册
const showRegister = () => {
  isActive.value = true
}

// 显示登录
const showLogin = () => {
  isActive.value = false
}

// 显示密码重置
const showResetPassword = () => {
  showResetModal.value = true
}

// 优化视频加载
let syncWidth = null
let panelOpenTimer = null
let cloudLiftTimer = null

onMounted(() => {
  // 自动检测国家/地区
  detectCountry()
  
  if (videoRef.value) {
    // 强制视频重新加载
    videoRef.value.load()
    
    // 监听视频加载事件
    videoRef.value.addEventListener('loadeddata', () => {
      videoRef.value.play().catch(err => {
        console.log('视频自动播放被阻止:', err)
      })
    })
  }
  syncWidth = () => {
    if (subtitleRef.value) {
      subtitleWidth.value = subtitleRef.value.offsetWidth
    }
  }
  syncWidth()
  window.addEventListener('resize', syncWidth)
})

onBeforeUnmount(() => {
  if (syncWidth) {
    window.removeEventListener('resize', syncWidth)
  }
  if (panelOpenTimer) {
    window.clearTimeout(panelOpenTimer)
  }
  if (cloudLiftTimer) {
    window.clearTimeout(cloudLiftTimer)
  }
})

// 切换声音
const toggleSound = () => {
  if (videoRef.value) {
    isMuted.value = !isMuted.value
    videoRef.value.muted = isMuted.value
    
    // 如果取消静音，尝试播放
    if (!isMuted.value) {
      videoRef.value.play().catch(err => {
        console.log('播放失败:', err)
        isMuted.value = true
        videoRef.value.muted = true
      })
    }
  }
}

const slideToLogin = () => {
  if (showLoginPanel.value || isCloudLifting.value) {
    return
  }

  isCloudLifting.value = true
  panelOpenTimer = window.setTimeout(() => {
    showLoginPanel.value = true
  }, 180)
  cloudLiftTimer = window.setTimeout(() => {
    isCloudLifting.value = false
  }, 1320)
}

const closeLoginPanel = () => {
  if (panelOpenTimer) {
    window.clearTimeout(panelOpenTimer)
    panelOpenTimer = null
  }
  if (cloudLiftTimer) {
    window.clearTimeout(cloudLiftTimer)
    cloudLiftTimer = null
  }

  isCloudLifting.value = false
  showLoginPanel.value = false
  isActive.value = false
  loginForm.username = ''
  loginForm.password = ''
  registerForm.username = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  registerForm.verificationCode = ''
  showResetModal.value = false
  resetForm.username = ''
  resetForm.password = ''
  resetForm.confirmPassword = ''
  resetForm.verificationCode = ''
}

const handleLogin = async () => {
  isLoggingIn.value = true
  
  try {
    const data = await login(loginForm)
    
    if (data.code === 200) {
        localStorage.setItem('token', data.data.token)
        localStorage.setItem('user', JSON.stringify(data.data.user))
        localStorage.setItem('userInfo', JSON.stringify(data.data.user))
        if (window.$axios) {
          window.$axios.defaults.headers.common['Authorization'] = `Bearer ${data.data.token}`
        }
        if (data.data.user.role === 'admin') {
          router.push(Number(data.data.user.isSuperAdmin || 0) === 1 ? '/admin/admins' : '/admin/merchants')
        } else {
          router.push('/home/heritage')
        }
      } else {
        notify.error(data.message || t('login.loginFailed'))
      }
  } catch (error) {
    console.error('登录失败:', error)
    notify.error(t('login.loginFailedRetry'))
  } finally {
    isLoggingIn.value = false
  }
}

// 处理注册
const handleRegister = async () => {
  if (!isRegisterFormValid.value) return
  
  // 确保国家/语言信息已检测
  if (registerForm.langCode === 'en' && registerForm.country === 'United States') {
    await detectCountry()
  }
  
  isRegistering.value = true
  
  try {
    const data = await register(registerForm)
    
    if (data.code === 200) {
      notify.success(t('login.registerSuccess'))
      loginForm.username = registerForm.username
      loginForm.password = registerForm.password
      handleLogin()
    } else {
      notify.error(data.message || t('login.registerFailed'))
    }
  } catch (error) {
    console.error('注册失败:', error)
    notify.error(t('login.registerFailedRetry'))
  } finally {
    isRegistering.value = false
  }
}

// 处理密码重置
const handleResetPassword = async () => {
  if (!isResetFormValid.value) return
  
  try {
    const data = await resetPassword({
      username: resetForm.username,
      verificationCode: resetForm.verificationCode,
      newPassword: resetForm.password,
      confirmPassword: resetForm.confirmPassword
    })
    
    if (data.code === 200) {
      showResetModal.value = false
      notify.success(t('login.resetSuccess'))
      resetForm.username = ''
      resetForm.verificationCode = ''
      resetForm.password = ''
      resetForm.confirmPassword = ''
    } else {
      notify.error(data.message || t('login.resetFailed'))
    }
  } catch (error) {
    console.error('重置密码失败:', error)
    notify.error(t('login.resetFailedRetry'))
  }
}

 
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.welcome-container {
  --ink-strong: #121212;
  --ink-main: #242424;
  --ink-soft: #6a6a6a;
  --line-soft: rgba(18, 18, 18, 0.08);
  --accent-red: #6e1f1f;
  --accent-red-deep: #4f1313;
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* 视频背景 */
.video-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.video-background video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  will-change: transform;
  transform: translateZ(0);
  backface-visibility: hidden;
}

.video-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(30, 5, 10, 0.5) 0%, rgba(60, 10, 20, 0.35) 50%, rgba(80, 15, 30, 0.25) 100%);
  will-change: transform;
  transform: translateZ(0);
}

/* 主内容区域 */
.main-content {
  position: relative;
  z-index: 1;
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 2rem;
  animation: fadeInUp 1s ease-out;
}

/* Logo 部分 */
.logo-section {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  text-align: center;
}

 

.app-title {
  width: 100%;
  display: block;
  font-size: clamp(10rem, 18vw, 22rem);
  font-weight: 800;
  line-height: 0.95;
  text-align: center;
  margin: 0.2rem 0 0.6rem 0;
  letter-spacing: 0.02em;
  font-style: italic;
  color: #c6b2b2;
  opacity: 0.3;
  text-shadow: none;
  transition: transform 1s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.6s ease;
  will-change: transform, opacity;
}

.app-title::after {
  display: none;
}

.app-title:hover {
  transform: none;
}

.app-title:hover::after {
  width: 60%;
}

.app-subtitle {
  font-size: clamp(1rem, 2vw, 1.5rem);
  color: rgba(255, 255, 255, 0.95);
  font-weight: 400;
  letter-spacing: 0.15em;
  font-style: italic;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: "KaiTi", "楷体", "STKaiti", "SimSun", serif;
  transform: translateX(380px);
}

.app-subtitle:hover {
  transform: translateX(380px) scale(1.05);
}

/* 描述部分 */
.description-section {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 2rem;
  margin: 0;
  width: 100%;
  text-align: center;
}

.description-text {
  font-size: clamp(0.95rem, 1.6vw, 1.2rem);
  color: rgba(255, 255, 255, 0.96);
  line-height: 1.9;
  letter-spacing: 0.02em;
  max-width: 720px;
  margin: 0 auto;
  padding: 0 1rem;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  display: inline-block;
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  will-change: transform;
}

.description-text:hover {
  transform: scale(1.025);
}

.desc-line {
  display: inline-block;
}

.desc-char {
  display: inline-block;
  margin: 0 0.12em;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1), color 0.3s ease;
  will-change: transform;
}

.desc-char:hover {
  transform: translateY(-3px) scale(1.25);
  animation: charPop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes charPop {
  0%   { transform: translateY(0) scale(1); }
  60%  { transform: translateY(-4px) scale(1.28); }
  100% { transform: translateY(-3px) scale(1.25); }
}
/* 右侧侧边栏 */
.sidebar-container {
  position: absolute;
  top: 50%;
  right: 0;
  transform: translateY(-50%);
  z-index: 10;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-trigger {
  width: 40px;
  height: 120px;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  border-radius: 20px 0 0 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: -5px 0 20px rgba(59, 130, 246, 0.4);
  transition: all 0.3s ease;
}

.sidebar-trigger i {
  font-size: 1.5rem;
  color: white;
  animation: bounceLeft 2s ease-in-out infinite;
}

@keyframes bounceLeft {
  0%, 100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(-5px);
  }
}

.sidebar-content {
  position: absolute;
  top: 0;
  right: -280px;
  width: 280px;
  height: 400px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px 0 0 20px;
  padding: 2rem;
  box-shadow: -10px 0 40px rgba(0, 0, 0, 0.2);
  opacity: 0;
  visibility: hidden;
  transform: translateX(20px);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-container.sidebar-hover .sidebar-content {
  opacity: 1;
  visibility: visible;
  transform: translateX(0);
  right: 40px;
}

.sidebar-header {
  margin-bottom: 2rem;
}

.sidebar-header h3 {
  font-size: 1.5rem;
  color: #1e3a8a;
  margin-bottom: 0.5rem;
}

.sidebar-header p {
  color: #64748b;
  font-size: 0.875rem;
}

.sidebar-buttons {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sidebar-btn {
  width: 100%;
  padding: 1rem;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  transition: all 0.3s ease;
}

.sidebar-btn i {
  font-size: 1.25rem;
}

.login-btn {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.5);
}

.register-btn {
  background: white;
  color: #3b82f6;
  border: 2px solid #3b82f6;
}

.register-btn:hover {
  background: #3b82f6;
  color: white;
  transform: translateY(-2px);
}

.sidebar-footer {
  margin-top: 2rem;
  text-align: center;
}

.sidebar-footer p {
  color: #94a3b8;
  font-size: 0.75rem;
}

/* 滑动登录面板 */
.slide-login-panel {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  z-index: 130;
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel-content {
  width: 100%;
  height: 100vh;
  background: transparent;
  padding: 0;
  position: relative;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  transform-origin: center bottom;
}

.pull-up-enter-active,
.pull-up-leave-active {
  transition: opacity 0.72s ease;
}

.pull-up-enter-active .panel-content,
.pull-up-leave-active .panel-content {
  transition:
    transform 1.22s cubic-bezier(0.16, 1, 0.3, 1),
    opacity 0.72s ease;
}

.pull-up-enter-from {
  opacity: 0;
}

.pull-up-enter-from .panel-content {
  transform: translateY(42vh) scale(0.94);
  opacity: 0;
}

.pull-up-leave-to {
  opacity: 0;
}

.pull-up-leave-to .panel-content {
  transform: translateY(22vh) scale(0.97);
  opacity: 0;
}

.panel-wrapper {
  width: 420px;
  max-width: 90%;
  padding: 3rem 2rem;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.close-btn {
  position: absolute;
  top: 1.5rem;
  right: 1.5rem;
  width: 40px;
  height: 40px;
  border: none;
  background: #f1f5f9;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  z-index: 10;
}

.close-btn:hover {
  background: #e2e8f0;
  transform: rotate(90deg);
}

.close-btn i {
  font-size: 1.5rem;
  color: #64748b;
}

/* 完整登录表单样式 */
.form-header {
  text-align: center;
  margin-bottom: 2rem;
}

.form-header h2 {
  font-size: 2rem;
  color: #ffffff;
  margin-bottom: 0.5rem;
}

.input-box {
  position: relative;
  margin: 1.5rem 0;
}

.input-box input {
  width: 100%;
  padding: 13px 50px 13px 20px;
  background: #f1f5f9;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  outline: none;
  font-size: 16px;
  color: #333;
  font-weight: 500;
  transition: all 0.3s ease;
}

.input-box input:focus {
  border-color: #3b82f6;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.input-box input::placeholder {
  color: #94a3b8;
  font-weight: 400;
}

.input-box i {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 1.25rem;
}

.input-box .verify-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  padding: 8px 16px;
  background: rgba(110, 31, 31, 0.08);
  color: var(--accent-red);
  border: 1px solid rgba(110, 31, 31, 0.15);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.28s ease;
  white-space: nowrap;
}

.input-box .verify-btn:hover:not(:disabled) {
  background: rgba(110, 31, 31, 0.12);
  transform: translateY(-50%) translateY(-1px);
  box-shadow: 0 6px 12px rgba(110, 31, 31, 0.12);
}

.input-box .verify-btn:disabled {
  opacity: 0.38;
  cursor: not-allowed;
  color: rgba(110, 31, 31, 0.38);
  border-color: rgba(110, 31, 31, 0.08);
}

.forgot-link {
  text-align: right;
  margin: 0.5rem 0 1.5rem;
}

.forgot-link a {
  color: #3b82f6;
  text-decoration: none;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.forgot-link a:hover {
  color: #2563eb;
  text-decoration: underline;
}

.btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}

.btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.5);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn.secondary {
  background: #f1f5f9;
  color: #64748b;
  box-shadow: none;
}

.btn.secondary:hover {
  background: #e2e8f0;
  transform: none;
}

.btn.primary {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
}

.social-login-text {
  text-align: center;
  margin: 1.5rem 0 1rem;
  color: #94a3b8;
  font-size: 0.9rem;
  position: relative;
}

.social-login-text::before,
.social-login-text::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background: #e2e8f0;
}

.social-login-text::before {
  left: 0;
}

.social-login-text::after {
  right: 0;
}

.social-icons {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin: 1rem 0;
}

.social-icons a {
  width: 45px;
  height: 45px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  border-radius: 50%;
  transition: all 0.3s ease;
  color: #64748b;
  font-size: 1.25rem;
}

.social-icons a:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.15);
}

.social-icons a:nth-child(1):hover {
  background: #DB4437;
  color: white;
}

.social-icons a:nth-child(2):hover {
  background: #4267B2;
  color: white;
}

.social-icons a:nth-child(3):hover {
  background: #333;
  color: white;
}

.social-icons a:nth-child(4):hover {
  background: #0077B5;
  color: white;
}

.switch-form {
  text-align: center;
  margin-top: 1.5rem;
  color: #64748b;
  font-size: 0.95rem;
}

.switch-form a {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 600;
}

.switch-form a:hover {
  text-decoration: underline;
}

.password-hint {
  margin: 1rem 0;
  padding: 0.75rem;
  background: #fef3c7;
  border-left: 3px solid #f59e0b;
  border-radius: 4px;
  color: #92400e;
  font-size: 0.875rem;
}

/* 密码重置弹窗 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(5px);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease;
}

.modal-content {
  background: white;
  border-radius: 20px;
  padding: 2.5rem;
  width: 450px;
  max-width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-content h2 {
  font-size: 1.75rem;
  color: #1e293b;
  margin-bottom: 1.5rem;
  text-align: center;
}

.modal-buttons {
  display: flex;
  gap: 1rem;
  margin-top: 2rem;
}

.modal-buttons .btn {
  flex: 1;
}

/* 漂浮装饰 */
.floating-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.3) 0%, rgba(59, 130, 246, 0.3) 100%);
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 100px;
  height: 100px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 60px;
  height: 60px;
  top: 60%;
  right: 15%;
  animation-delay: 2s;
}

.shape-3 {
  width: 80px;
  height: 80px;
  bottom: 20%;
  left: 20%;
  animation-delay: 4s;
}

/* 声音控制按钮 */
.sound-toggle {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.3);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.sound-toggle:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.sound-toggle i {
  font-size: 1.5rem;
  color: white;
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 10px 30px rgba(59, 130, 246, 0.5);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 15px 40px rgba(59, 130, 246, 0.7);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

/* Slide 过渡动画 */
.slide-enter-active,
.slide-leave-active {
  transition:
    transform 1.0s cubic-bezier(0.16, 1, 0.3, 1),
    opacity 0.6s ease;
}

.slide-enter-from {
  transform: translateY(100%);
  opacity: 1;
}

.slide-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

 

@keyframes subtlePop {
  0%   { transform: scale(0.985); }
  80%  { transform: scale(1.012); }
  100% { transform: scale(1); }
}

 

.login-arrow {
  padding: 0;
  border: none;
  background: transparent;
  display: block;
  cursor: pointer;
  transition: transform 0.82s cubic-bezier(0.16,1,0.3,1), opacity 0.42s ease;
  opacity: 0.9;
  margin: 2rem auto;
  transform-origin: center bottom;
}

.login-arrow .cloud-arrow {
  width: clamp(6rem, 12vw, 16rem);
  height: auto;
  object-fit: contain;
  filter: drop-shadow(0 8px 24px rgba(255,255,255,0.32));
  transition:
    transform 0.92s cubic-bezier(0.16,1,0.3,1),
    filter 0.42s ease;
}

.login-arrow:hover {
  transform: translateY(-6px) scale(1.12);
  opacity: 1;
}

.login-arrow:hover .cloud-arrow {
  transform: translateY(-2px);
}

.login-arrow.pulling {
  transform: translateY(-30px) scale(1.18);
  opacity: 1;
}

.login-arrow.pulling .cloud-arrow {
  transform: translateY(-22px) scale(1.06);
  filter: drop-shadow(0 20px 40px rgba(255,255,255,0.46));
}

/* 登录面板 */
.login-panel {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  background:
    radial-gradient(circle at top, rgba(110, 31, 31, 0.16), transparent 42%),
    rgba(10, 10, 10, 0.5);
  backdrop-filter: blur(14px);
  z-index: 130;
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel-content {
  width: 100%;
  height: 100vh;
  background: transparent;
  padding: 0;
  position: relative;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 登录面板内的跳过按钮 */
.skip-login-btn {
  width: 100%;
  padding: 12px 24px;
  border: 2px solid rgba(255,255,255,0.4);
  background: rgba(255,255,255,0.05);
  backdrop-filter: blur(10px);
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
  margin-top: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.skip-login-btn:hover {
  background: rgba(255,255,255,0.15);
  border-color: rgba(255,255,255,0.7);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.2);
}

.skip-login-btn i {
  font-size: 20px;
  transition: transform 0.3s ease;
}

.skip-login-btn:hover i {
  transform: translateX(4px);
}

/* 登录容器样式 */
.container {
  position: relative;
  width: 850px;
  height: 650px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.24), rgba(244, 244, 241, 0.14));
  margin: 20px;
  border-radius: 34px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow:
    0 28px 80px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.32);
  backdrop-filter: blur(18px) saturate(120%);
  overflow: hidden;
  isolation: isolate;
}

.container h1 {
  font-size: 34px;
  margin: -10px 0 6px;
  color: var(--ink-strong);
  font-weight: 700;
  letter-spacing: -0.03em;
}

.container p {
  font-size: 14.5px;
  margin: 15px 0;
  color: var(--ink-soft);
}

form {
  width: 100%;
}

.form-box {
  position: absolute;
  right: 0;
  width: 50%;
  height: 100%;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.24), rgba(247, 245, 242, 0.12)),
    rgba(255, 255, 255, 0.08);
  border-left: 1px solid rgba(255, 255, 255, 0.14);
  display: flex;
  align-items: center;
  color: var(--ink-main);
  text-align: center;
  padding: 48px;
  z-index: 1;
  opacity: 1;
  transition:
    right 0.72s cubic-bezier(0.16, 1, 0.3, 1),
    opacity 0.42s ease,
    visibility 0s linear 0.42s;
  backdrop-filter: blur(34px) saturate(155%);
}


.container.active .form-box {
  right: 50%;
}

.form-box.login {
  background: transparent;
  border-left: none;
  backdrop-filter: none;
  z-index: 1;
}

.form-box.register {
  visibility: hidden;
  opacity: 0;
  background: transparent;
  border-left: none;
  backdrop-filter: none;
  z-index: 1;
}

.container.active .form-box.register {
  visibility: visible;
  opacity: 1;
  z-index: 1;
}

.container.active .form-box.login {
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
}

.input-box {
  position: relative;
  margin: 22px 0;
}

.input-box input {
  width: 100%;
  padding: 15px 52px 15px 18px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  outline: none;
  font-size: 16px;
  color: #111111;
  font-weight: 600;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.45),
    0 8px 24px rgba(15, 15, 15, 0.05);
  transition:
    border-color 0.28s ease,
    box-shadow 0.28s ease,
    background 0.28s ease,
    transform 0.28s ease;
}

.input-box input:focus {
  border-color: rgba(110, 31, 31, 0.4);
  background: rgba(255, 255, 255, 0.68);
  box-shadow:
    0 0 0 4px rgba(110, 31, 31, 0.1),
    0 16px 34px rgba(15, 15, 15, 0.08);
  transform: translateY(-1px);
}

.input-box input::placeholder {
  color: rgba(40, 40, 40, 0.5);
  font-weight: 400;
}

.input-box i {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  color: rgba(24, 24, 24, 0.52);
}

.verify-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(110, 31, 31, 0.08);
  color: var(--accent-red);
  border: 1px solid rgba(110, 31, 31, 0.15);
  border-radius: 10px;
  padding: 9px 14px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition:
    background 0.28s ease,
    transform 0.28s ease,
    box-shadow 0.28s ease;
}

.verify-btn:hover:not(:disabled) {
  background: rgba(110, 31, 31, 0.12);
  transform: translateY(-50%) translateY(-1px);
  box-shadow: 0 6px 12px rgba(110, 31, 31, 0.12);
}

.verify-btn:disabled {
  opacity: 0.38;
  cursor: not-allowed;
  color: rgba(110, 31, 31, 0.38);
  border-color: rgba(110, 31, 31, 0.08);
}

.password-hint {
  font-size: 12px;
  color: #e74c3c;
  margin: -15px 0 15px;
  text-align: left;
}

.forgot-link {
  margin: -15px 0 15px;
}

.forgot-link a {
  font-size: 14.5px;
  color: var(--ink-soft);
  cursor: pointer;
  text-decoration: none;
  transition: color 0.28s ease;
}

.forgot-link a:hover {
  color: var(--accent-red);
}

.admin-entry {
  display: inline-block;
  margin-top: 12px;
  color: var(--accent-red);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  letter-spacing: 0.02em;
}

.admin-entry:hover {
  color: var(--accent-red-deep);
}

.btn {
  width: 100%;
  height: 52px;
  background: linear-gradient(180deg, #1f1f1f, #111111);
  border-radius: 16px;
  box-shadow:
    0 18px 32px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  font-size: 16px;
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.01em;
  transition:
    transform 0.32s ease,
    box-shadow 0.32s ease,
    background 0.32s ease;
}

.btn:hover:not(:disabled) {
  background: linear-gradient(180deg, var(--accent-red), var(--accent-red-deep));
  transform: translateY(-2px);
  box-shadow:
    0 22px 36px rgba(79, 19, 19, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn.secondary {
  background: rgba(24, 24, 24, 0.08);
  color: var(--ink-main);
  border: 1px solid var(--line-soft);
  box-shadow: none;
  margin-right: 10px;
}

.btn.secondary:hover {
  background: rgba(110, 31, 31, 0.08);
}

.btn.primary {
  background: linear-gradient(180deg, var(--accent-red), var(--accent-red-deep));
}

.social-icons {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin: 1rem 0;
}

.social-icons a {
  display: inline-flex;
  width: 48px;
  height: 48px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  font-size: 22px;
  color: var(--ink-main);
  margin: 0;
  background: rgba(255, 255, 255, 0.74);
  transition:
    transform 0.28s ease,
    border-color 0.28s ease,
    color 0.28s ease,
    background 0.28s ease,
    box-shadow 0.28s ease;
  text-decoration: none;
}

.social-icons a:hover {
  transform: translateY(-2px);
  border-color: rgba(110, 31, 31, 0.22);
  background: rgba(110, 31, 31, 0.08);
  color: var(--accent-red);
  box-shadow: 0 16px 26px rgba(15, 15, 15, 0.08);
}

.social-icons a:nth-child(1):hover,
.social-icons a:nth-child(2):hover,
.social-icons a:nth-child(3):hover,
.social-icons a:nth-child(4):hover {
  transform: translateY(-2px);
  border-color: rgba(110, 31, 31, 0.22);
  background: rgba(110, 31, 31, 0.08);
  color: var(--accent-red);
  box-shadow: 0 16px 26px rgba(15, 15, 15, 0.08);
}

.toggle-box {
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 3;
  pointer-events: none;
}

.toggle-box::before {
  content: "";
  position: absolute;
  left: -250%;
  width: 300%;
  height: 100%;
  background:
    radial-gradient(circle at 18% 24%, rgba(255, 255, 255, 0.1), transparent 18%),
    linear-gradient(135deg, #1a1a1a 0%, #3a1414 58%, #561818 100%);
  border-radius: 150px;
  z-index: 1;
  transition: 1.8s ease-in-out;
  pointer-events: none;
}

.container.active .toggle-box::before {
  left: 50%;
}

.toggle-panel {
  position: absolute;
  width: 50%;
  height: 100%;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 2;
  transition: 0.6s ease-in-out;
  padding: 0 36px;
  text-align: center;
  pointer-events: auto;
}

.toggle-panel.toggle-left {
  left: 0;
  transition-delay: 1.2s;
}

.container.active .toggle-panel.toggle-left {
  left: -50%;
  transition-delay: 0.6s;
}

.toggle-panel.toggle-right {
  right: -50%;
  transition-delay: 0.6s;
}

.container.active .toggle-panel.toggle-right {
  right: 0;
  transition-delay: 1.2s;
}

.toggle-panel h1 {
  color: #fff;
}

.toggle-panel p {
  margin-bottom: 20px;
  color: rgba(255, 255, 255, 0.72);
}

.toggle-panel .btn {
  width: 160px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: none;
  backdrop-filter: blur(10px);
}

.toggle-panel .btn:hover {
  background: rgba(255, 255, 255, 0.16);
}

/* 弹窗样式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: rgba(249, 249, 247, 0.96);
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.48);
  padding: 40px;
  width: 400px;
  max-width: 90%;
  box-shadow:
    0 24px 60px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(24px);
}

.modal-content h2 {
  text-align: center;
  margin-bottom: 30px;
  color: var(--ink-strong);
}

.modal-buttons {
  display: flex;
  gap: 10px;
  margin-top: 30px;
}

/* 响应式设计 */
@media screen and (max-width: 650px) {
  .container {
    height: calc(100vh - 40px);
  }

  .form-box {
    bottom: 0;
    width: 100%;
    height: 80%;
  }

  .container.active .form-box {
    right: 0;
    bottom: 20%;
  }

  .toggle-box::before {
    left: 0;
    top: -270%;
    width: 100%;
    height: 300%;
    border-radius: 20vw;
  }

  .container.active .toggle-box::before {
    left: 0;
    top: 80%;
  }

  .container.active .toggle-panel.toggle-left {
    left: 0;
    top: -20%;
  }

  .toggle-panel {
    width: 100%;
    height: 20%;
  }

  .toggle-panel.toggle-left {
    top: 0;
  }

  .toggle-panel.toggle-right {
    right: 0;
    bottom: -20%;
  }

  .container.active .toggle-panel.toggle-right {
    bottom: 0;
  }
}

@media screen and (max-width: 400px) {
  .form-box {
    padding: 20px;
  }

  .toggle-panel h1 {
    font-size: 30px;
  }
}

@media (max-width: 768px) {
  .app-title {
    font-size: 2.5rem;
  }

  .app-subtitle {
    font-size: 1rem;
  }

  .description-text {
    font-size: 1.25rem;
  }

  .logo-icon {
    width: 80px;
    height: 80px;
  }

  .logo-icon i {
    font-size: 2.5rem;
  }

  .sidebar-content {
    width: 250px;
    right: -250px;
  }

  .sidebar-container.sidebar-hover .sidebar-content {
    right: 40px;
  }

  .panel-content {
    width: 90%;
    padding: 2rem;
  }

  .description-section {
    bottom: 1.25rem;
  }

  .sound-toggle {
    bottom: 1rem;
    right: 1rem;
    width: 45px;
    height: 45px;
  }

  .sound-toggle i {
    font-size: 1.25rem;
  }
}
</style>
