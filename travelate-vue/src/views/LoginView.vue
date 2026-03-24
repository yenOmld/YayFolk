<template>
  <div class="login-page">
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
              @input="validateUsername"
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
              @input="validatePassword"
              required
            >
            <i class='bx bxs-lock-alt'></i>
          </div>
          <div class="input-box">
            <input 
              type="password" 
              :placeholder="$t('login.confirmPassword')" 
              v-model="registerForm.confirmPassword" 
              @input="validateConfirmPassword"
              required
            >
            <i class='bx bxs-lock-alt'></i>
          </div>
          <div class="password-hint" v-if="passwordHint">
            {{ passwordHint }}
          </div>
          <button type="submit" class="btn" :disabled="!isFormValid || isRegistering">
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
              @input="validateUsername"
              required
            >
            <i :class="registerForm.isPhone ? 'bx bxs-phone' : 'bx bxs-envelope'"></i>
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
              @input="validatePassword"
              required
            >
            <i class='bx bxs-lock-alt'></i>
          </div>
          <div class="input-box">
            <input 
              type="password" 
              :placeholder="$t('login.confirmPassword')" 
              v-model="resetForm.confirmPassword" 
              @input="validateConfirmPassword"
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
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { login, register, sendCode, resetPassword } from '../api/app'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const { t } = useI18n()
const isActive = ref(false)
const showResetModal = ref(false)

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 注册表单
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

// 页面加载时自动检测国家/地区
onMounted(() => {
  detectCountry()
})

// 密码重置表单
const resetForm = reactive({
  username: '',
  verificationCode: '',
  password: '',
  confirmPassword: ''
})

// 状态
const isLoggingIn = ref(false)
const isRegistering = ref(false)
const isSendingCode = ref(false)
const countdown = ref(0)
const passwordHint = ref('')

// 验证用户名（判断是手机号还是邮箱）
const validateUsername = () => {
  const username = registerForm.username || resetForm.username
  if (!username) return
  
  // 简单判断是手机号还是邮箱
  registerForm.isPhone = username.includes('@') ? false : true
}

// 验证密码
const validatePassword = () => {
  const password = registerForm.password || resetForm.password
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
const validateConfirmPassword = () => {
  const password = registerForm.password || resetForm.password
  const confirmPassword = registerForm.confirmPassword || resetForm.confirmPassword
  
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
const isFormValid = computed(() => {
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

// 处理登录
const handleLogin = async () => {
  isLoggingIn.value = true
  
  try {
    const data = await login(loginForm)
    
    if (data.code === 200) {
        localStorage.setItem('token', data.data.token)
        localStorage.setItem('user', JSON.stringify(data.data.user))
        if (window.$axios) {
          window.$axios.defaults.headers.common['Authorization'] = `Bearer ${data.data.token}`
        }
        router.push('/home/translate')
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
    if (!isFormValid.value) return
    
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

<style>
@import url("https://fonts.googleapis.com/css2?family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&display=swap");

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: "Poppins", sans-serif;
  text-decoration: none;
  list-style: none;
}

.login-page {
  min-height: 100vh;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(90deg, #e2e2e2, #c9d6ff);
}
</style>

<style scoped>
.container {
  position: relative;
  width: 850px;
  height: 650px;
  background: #fff;
  margin: 20px;
  border-radius: 30px;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.container h1 {
  font-size: 36px;
  margin: -10px 0;
}

.container p {
  font-size: 14.5px;
  margin: 15px 0;
}

form {
  width: 100%;
}

.form-box {
  position: absolute;
  right: 0;
  width: 50%;
  height: 100%;
  background: #fff;
  display: flex;
  align-items: center;
  color: #333;
  text-align: center;
  padding: 40px;
  z-index: 1;
  transition: 0.6s ease-in-out 1.2s, visibility 0s 1s;
}

.container.active .form-box {
  right: 50%;
}

.form-box.register {
  visibility: hidden;
}

.container.active .form-box.register {
  visibility: visible;
}

.input-box {
  position: relative;
  margin: 30px 0;
}

.input-box input {
  width: 100%;
  padding: 13px 50px 13px 20px;
  background: #eee;
  border-radius: 8px;
  border: none;
  outline: none;
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.input-box input::placeholder {
  color: #888;
  font-weight: 400;
}

.input-box i {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
}

.country-select {
  width: 100%;
  padding: 13px 50px 13px 20px;
  background: #eee;
  border-radius: 8px;
  border: none;
  outline: none;
  font-size: 16px;
  color: #333;
  font-weight: 500;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
}

.country-select option {
  background: white;
  color: #333;
}

.verify-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: #7494ec;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}

.verify-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
  color: #333;
  cursor: pointer;
}

.btn {
  width: 100%;
  height: 48px;
  background: #7494ec;
  border-radius: 8px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #fff;
  font-weight: 600;
  transition: all 0.3s;
}

.btn:hover:not(:disabled) {
  background: #5a7bd4;
  transform: translateY(-2px);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.secondary {
  background: #95a5a6;
  margin-right: 10px;
}

.btn.secondary:hover {
  background: #7f8c8d;
}

.social-icons {
  display: flex;
  justify-content: center;
}

.social-icons a {
  display: inline-flex;
  padding: 10px;
  border: 2px solid #ccc;
  border-radius: 8px;
  font-size: 24px;
  color: #333;
  margin: 0 8px;
  transition: all 0.3s;
}

.social-icons a:hover {
  border-color: #7494ec;
  color: #7494ec;
}

.toggle-box {
  position: absolute;
  width: 100%;
  height: 100%;
}

.toggle-box::before {
  content: "";
  position: absolute;
  left: -250%;
  width: 300%;
  height: 100%;
  background: #7494ec;
  border-radius: 150px;
  z-index: 2;
  transition: 1.8s ease-in-out;
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

.toggle-panel p {
  margin-bottom: 20px;
}

.toggle-panel .btn {
  width: 160px;
  height: 46px;
  background: transparent;
  border: 2px solid #fff;
  box-shadow: none;
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
  background: white;
  border-radius: 20px;
  padding: 40px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.modal-content h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.modal-buttons {
  display: flex;
  gap: 10px;
  margin-top: 30px;
}

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
</style>
