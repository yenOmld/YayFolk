<template>
  <div class="unban-page">
    <div class="unban-card">
      <h1>账号解封申请</h1>
      <p class="subtitle">如果你的账号被封禁，请填写账号和申请原因，我们会尽快处理。</p>

      <label class="label" for="account">账号（用户名或邮箱）</label>
      <input
        id="account"
        v-model.trim="form.account"
        class="input"
        type="text"
        placeholder="请输入登录账号"
        @blur="loadLatest"
      />

      <label class="label" for="reason">申请原因</label>
      <textarea
        id="reason"
        v-model.trim="form.reason"
        class="textarea"
        maxlength="500"
        placeholder="请说明解封申请原因，最多500字"
      />
      <div class="count">{{ form.reason.length }}/500</div>

      <div class="actions">
        <button class="primary" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交申请' }}
        </button>
        <button class="ghost" @click="goLogin">返回登录</button>
      </div>

      <div v-if="latest" class="latest-box">
        <h3>最近一次申请</h3>
        <p><strong>状态：</strong><span :class="['status', latest.status]">{{ statusLabel(latest.status) }}</span></p>
        <p><strong>申请时间：</strong>{{ formatTime(latest.createTime) }}</p>
        <p><strong>申请原因：</strong>{{ latest.applyReason || '-' }}</p>
        <p><strong>管理员备注：</strong>{{ latest.adminRemark || '-' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { submitUnbanApplication, getLatestUnbanApplication } from '@/api/app.js'

const route = useRoute()
const router = useRouter()
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const form = reactive({
  account: '',
  reason: ''
})

const submitting = ref(false)
const latest = ref(null)

const statusLabel = (status) => ({
  pending: '待处理',
  approved: '已通过',
  rejected: '已驳回'
}[status] || status || '-')

const formatTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN')
}

const loadLatest = async () => {
  if (!form.account) {
    latest.value = null
    return
  }

  try {
    const res = await getLatestUnbanApplication(form.account)
    if (res.code === 200) {
      latest.value = res.data || null
    }
  } catch (error) {
    latest.value = null
  }
}

const handleSubmit = async () => {
  if (!form.account) {
    notify?.warning?.('请先填写账号')
    return
  }
  if (!form.reason) {
    notify?.warning?.('请填写申请原因')
    return
  }

  submitting.value = true
  try {
    const res = await submitUnbanApplication(form.account, form.reason)
    if (res.code !== 200) {
      throw new Error(res.message || '提交失败')
    }
    notify?.success?.('申请已提交，请等待管理员审核')
    form.reason = ''
    await loadLatest()
  } catch (error) {
    notify?.error?.(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const goLogin = () => {
  router.push('/login')
}

onMounted(async () => {
  const account = (route.query.account || '').toString().trim()
  if (account) {
    form.account = account
  }
  await loadLatest()
})
</script>

<style scoped>
.unban-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #eef2ff 100%);
}

.unban-card {
  width: 100%;
  max-width: 680px;
  background: #ffffff;
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
  padding: 28px;
}

.unban-card h1 {
  margin: 0;
  color: #111827;
  font-size: 30px;
}

.subtitle {
  margin-top: 8px;
  margin-bottom: 22px;
  color: #475569;
  line-height: 1.6;
}

.label {
  display: block;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.input,
.textarea {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 12px 14px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;
}

.input:focus,
.textarea:focus {
  border-color: #2563eb;
}

.textarea {
  min-height: 140px;
  resize: vertical;
}

.count {
  text-align: right;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.actions {
  margin-top: 18px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

button {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
}

.primary {
  background: #2563eb;
  color: #fff;
}

.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ghost {
  background: #e2e8f0;
  color: #334155;
}

.latest-box {
  margin-top: 26px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
}

.latest-box h3 {
  margin: 0 0 10px;
  color: #0f172a;
}

.latest-box p {
  margin: 8px 0;
  color: #334155;
  word-break: break-word;
}

.status {
  font-weight: 600;
}

.status.pending {
  color: #b45309;
}

.status.approved {
  color: #15803d;
}

.status.rejected {
  color: #b91c1c;
}

@media (max-width: 640px) {
  .unban-card {
    padding: 20px;
  }

  .unban-card h1 {
    font-size: 24px;
  }
}
</style>
