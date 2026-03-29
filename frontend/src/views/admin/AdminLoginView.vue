<template>
  <div class="admin-login-page">
    <div class="admin-login-page__backdrop"></div>
    <div class="admin-login-page__glow admin-login-page__glow--left"></div>
    <div class="admin-login-page__glow admin-login-page__glow--right"></div>

    <div class="login-shell">
      <section class="login-hero">
        <span class="hero-badge">Admin Access</span>
        <h1>管理员登录</h1>
        <p>
          管理后台，用于商家审核、活动审核、用户管理和官方内容维护。
        </p>

        <div class="hero-points">
          <div class="point-card">
            <i class="bx bx-shield-quarter"></i>
            <div>
              <strong>安全登录</strong>
            </div>
          </div>
          <div class="point-card">
            <i class="bx bx-check-circle"></i>
            <div>
              <strong>统一体验</strong>
            </div>
          </div>
        </div>
      </section>

      <section class="login-panel">
        <div class="panel-copy">
          <span class="panel-tag">YayFolk Console</span>
          <h2>进入管理后台</h2>
          <p>输入管理员账号与密码后继续。</p>
        </div>

        <form class="login-form" @submit.prevent="handleAdminLogin">
          <label>
            <span>管理员账号</span>
            <div class="input-box">
              <input v-model.trim="form.username" type="text" placeholder="请输入管理员用户名" required />
              <i class="bx bxs-user"></i>
            </div>
          </label>

          <label>
            <span>登录密码</span>
            <div class="input-box">
              <input v-model="form.password" type="password" placeholder="请输入管理员密码" required />
              <i class="bx bxs-lock-alt"></i>
            </div>
          </label>

          <button type="submit" class="submit-btn" :disabled="submitting">
            {{ submitting ? '登录中...' : '进入管理后台' }}
          </button>
        </form>

        <div class="footer-links">
          <router-link to="/login">返回普通登录</router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { getCurrentInstance, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const router = useRouter()

const submitting = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const handleAdminLogin = async () => {
  submitting.value = true

  try {
    const result = await login({
      username: form.username,
      password: form.password
    })

    if (result.code !== 200) {
      notify.error(result.message || '管理员登录失败')
      return
    }

    if (result.data.user.role !== 'admin') {
      notify.error('当前账号不是管理员，无法进入后台')
      return
    }

    localStorage.setItem('token', result.data.token)
    localStorage.setItem('user', JSON.stringify(result.data.user))
    localStorage.setItem('userInfo', JSON.stringify(result.data.user))
    if (window.$axios) {
      window.$axios.defaults.headers.common.Authorization = `Bearer ${result.data.token}`
    }

    notify.success('登录成功')
    router.push(Number(result.data.user.isSuperAdmin || 0) === 1 ? '/admin/admins' : '/admin/merchants')
  } catch (error) {
    console.error('管理员登录失败:', error)
    notify.error(error.response?.data?.message || '管理员登录失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    linear-gradient(135deg, rgba(22, 10, 10, 0.96), rgba(48, 12, 18, 0.92) 48%, rgba(26, 22, 28, 0.96)),
    #120d10;
}

.admin-login-page__backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top, rgba(110, 31, 31, 0.28), transparent 42%),
    radial-gradient(circle at bottom right, rgba(191, 143, 79, 0.16), transparent 28%),
    rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(14px);
}

.admin-login-page__glow {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  filter: blur(96px);
  pointer-events: none;
}

.admin-login-page__glow--left {
  top: -120px;
  left: -80px;
  background: rgba(110, 31, 31, 0.36);
}

.admin-login-page__glow--right {
  right: -120px;
  bottom: -140px;
  background: rgba(186, 140, 83, 0.22);
}

.login-shell {
  position: relative;
  z-index: 1;
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: 1.06fr 0.94fr;
  gap: 22px;
  padding: 22px;
  border-radius: 36px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.14), rgba(244, 244, 241, 0.08));
  box-shadow:
    0 32px 90px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(24px) saturate(120%);
}

.login-hero,
.login-panel {
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.06));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.login-hero {
  padding: 38px;
  color: #f7f4ee;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 30px;
}

.hero-badge,
.panel-tag {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(251, 216, 181, 0.14);
  color: #ffe2bf;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.login-hero h1 {
  margin: 18px 0 12px;
  font-size: 46px;
  line-height: 1.05;
  color: #f7f4ee;
}

.login-hero p {
  margin: 0;
  max-width: 520px;
  color: rgba(247, 244, 238, 0.76);
  font-size: 15px;
  line-height: 1.85;
}

.hero-points {
  display: grid;
  gap: 14px;
}

.point-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.point-card i {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffe2bf;
  font-size: 22px;
}

.point-card strong {
  display: block;
  color: #f7f4ee;
  font-size: 15px;
}

.point-card span {
  color: rgba(247, 244, 238, 0.68);
  font-size: 13px;
}

.login-panel {
  padding: 36px;
  color: #f7f4ee;
}

.panel-copy {
  margin-bottom: 24px;
}

.panel-copy h2 {
  margin: 16px 0 10px;
  color: #f7f4ee;
  font-size: 30px;
}

.panel-copy p {
  margin: 0;
  color: rgba(247, 244, 238, 0.68);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-form label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: rgba(247, 244, 238, 0.82);
  font-size: 14px;
  font-weight: 600;
}

.input-box {
  position: relative;
}

.input-box input {
  width: 100%;
  box-sizing: border-box;
  padding: 15px 52px 15px 18px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  color: #f7f4ee;
  outline: none;
  font-size: 15px;
  transition: border-color 0.24s ease, box-shadow 0.24s ease, background 0.24s ease;
}

.input-box input::placeholder {
  color: rgba(247, 244, 238, 0.42);
}

.input-box input:focus {
  border-color: rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.08);
}

.input-box i {
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(251, 216, 181, 0.82);
  font-size: 20px;
}

.submit-btn {
  margin-top: 8px;
  padding: 14px 16px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(180deg, #7a2323, #5c1515);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 18px 28px rgba(110, 31, 31, 0.24);
  transition: transform 0.24s ease, box-shadow 0.24s ease, opacity 0.24s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 22px 34px rgba(110, 31, 31, 0.28);
}

.submit-btn:disabled {
  opacity: 0.72;
  cursor: not-allowed;
}

.footer-links {
  margin-top: 18px;
}

.footer-links a {
  color: #ffe2bf;
  text-decoration: none;
  font-size: 14px;
}

.footer-links a:hover {
  color: #fff2df;
}

@media (max-width: 960px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-hero {
    padding: 28px;
  }

  .login-hero h1 {
    font-size: 38px;
  }

  .login-panel {
    padding: 28px;
  }
}

@media (max-width: 640px) {
  .admin-login-page {
    padding: 16px;
  }

  .login-shell {
    padding: 14px;
    border-radius: 28px;
  }

  .login-hero,
  .login-panel {
    padding: 22px;
  }

  .login-hero h1 {
    font-size: 32px;
  }

  .panel-copy h2 {
    font-size: 26px;
  }
}
</style>
