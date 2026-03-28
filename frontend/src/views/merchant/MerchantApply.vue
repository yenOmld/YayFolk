<template>
  <div class="apply-page">
    <div v-if="appStatus === 'applied'" class="status-card pending">
      <div class="status-icon">...</div>
      <h3>申请审核中</h3>
      <p>您的商家入驻申请正在审核，请耐心等待管理员处理。</p>
      <div class="info-grid" v-if="appInfo">
        <div class="info-item">
          <span>店铺名称</span>
          <strong>{{ appInfo.shopName || '-' }}</strong>
        </div>
        <div class="info-item">
          <span>非遗品类</span>
          <strong>{{ appInfo.heritageType || '-' }}</strong>
        </div>
        <div class="info-item">
          <span>申请时间</span>
          <strong>{{ formatTime(appInfo.createTime) }}</strong>
        </div>
        <div class="info-item">
          <span>审核状态</span>
          <strong>{{ appInfo.applicationStatus || 'pending' }}</strong>
        </div>
      </div>
    </div>

    <div v-else-if="appStatus === 'approved'" class="status-card approved">
      <div class="status-icon">OK</div>
      <h3>申请已通过</h3>
      <p>您已成为商家，可以开始发布活动和商品。</p>
      <router-link to="/merchant/activities" class="go-btn">前往活动管理</router-link>
    </div>

    <div v-else-if="appStatus === 'rejected'" class="status-card rejected">
      <div class="status-icon">!</div>
      <h3>申请被驳回</h3>
      <p v-if="appInfo?.auditRemark">驳回原因：{{ appInfo.auditRemark }}</p>
      <button class="retry-btn" @click="appStatus = 'none'">重新申请</button>
    </div>

    <div v-else class="apply-form-wrap">
      <div class="form-header">
        <h2>申请成为非遗传承商家</h2>
        <p>填写以下信息完成商家认证申请，审核通过后即可发布活动和商品。</p>
      </div>

      <form class="apply-form" @submit.prevent="submitApply">
        <div class="section-title">基本信息</div>
        <div class="form-row">
          <div class="form-group">
            <label>真实姓名 <span class="req">*</span></label>
            <input v-model="form.realName" placeholder="请输入真实姓名" required />
          </div>
          <div class="form-group">
            <label>身份证号</label>
            <input v-model="form.idCard" placeholder="请输入身份证号" maxlength="18" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>联系电话 <span class="req">*</span></label>
            <input v-model="form.phone" placeholder="请输入联系电话" required />
          </div>
          <div class="form-group">
            <label>非遗品类 <span class="req">*</span></label>
            <select v-model="form.heritageType" required>
              <option value="">请选择</option>
              <option v-for="item in heritageTypes" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
        </div>

        <div class="section-title">店铺信息</div>
        <div class="form-row">
          <div class="form-group">
            <label>店铺名称 <span class="req">*</span></label>
            <input v-model="form.shopName" placeholder="请输入店铺名称" required />
          </div>
          <div class="form-group">
            <label>店铺地址</label>
            <input v-model="form.shopAddress" placeholder="请输入店铺地址" />
          </div>
        </div>

        <div class="form-group full">
          <label>非遗项目描述 <span class="req">*</span></label>
          <textarea
            v-model="form.heritageDescription"
            placeholder="请介绍您的非遗项目、传承经历和相关资质"
            rows="4"
            required
          />
        </div>

        <div class="form-actions">
          <button type="submit" class="submit-btn" :disabled="submitting">
            {{ submitting ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { applyMerchant, getMyApplication } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const appStatus = ref('none')
const appInfo = ref(null)
const submitting = ref(false)

const heritageTypes = ['刺绣', '剪纸', '陶瓷', '漆器', '雕刻', '京剧', '昆曲', '古琴', '太极', '中医', '年画', '其他']

const form = ref({
  realName: '',
  idCard: '',
  phone: '',
  heritageType: '',
  shopName: '',
  shopAddress: '',
  heritageDescription: ''
})

const loadStatus = async () => {
  try {
    const res = await getMyApplication()
    if (res.code !== 200) {
      throw new Error(res.message || '加载申请状态失败')
    }
    const data = res.data || {}
    appStatus.value = data.shopStatus || 'none'
    appInfo.value = data
  } catch (error) {
    appStatus.value = 'none'
    appInfo.value = null
  }
}

const submitApply = async () => {
  submitting.value = true
  try {
    const res = await applyMerchant(form.value)
    if (res.code !== 200) {
      throw new Error(res.message || '提交失败，请重试')
    }
    notify('申请提交成功，请等待管理员审核', 'success')
    await loadStatus()
  } catch (error) {
    notify(error.message || error.response?.data?.message || '提交失败，请重试', 'error')
  } finally {
    submitting.value = false
  }
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(loadStatus)
</script>

<style scoped>
.apply-page {
  max-width: 680px;
}

.status-card {
  text-align: center;
  padding: 48px 32px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e5e7eb;
}

.status-icon {
  font-size: 36px;
  margin-bottom: 16px;
  font-weight: 700;
}

.status-card h3 {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 10px;
  color: #111827;
}

.status-card p {
  color: #6b7280;
  font-size: 15px;
  margin-bottom: 24px;
}

.status-card.pending {
  border-color: #fcd34d;
}

.status-card.approved {
  border-color: #34d399;
}

.status-card.rejected {
  border-color: #f87171;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  text-align: left;
  max-width: 460px;
  margin: 0 auto;
}

.info-item {
  background: #f9fafb;
  padding: 12px 16px;
  border-radius: 8px;
}

.info-item span {
  font-size: 12px;
  color: #9ca3af;
  display: block;
  margin-bottom: 4px;
}

.info-item strong {
  font-size: 14px;
  color: #111827;
}

.go-btn {
  display: inline-block;
  padding: 12px 28px;
  background: #059669;
  color: #fff;
  border-radius: 10px;
  text-decoration: none;
  font-weight: 500;
}

.retry-btn {
  padding: 10px 24px;
  background: #7c3aed;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.apply-form-wrap {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  border: 1px solid #e5e7eb;
}

.form-header {
  margin-bottom: 28px;
}

.form-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px;
}

.form-header p {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #7c3aed;
  margin: 20px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ede9fe;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group.full {
  margin-top: 16px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.req {
  color: #ef4444;
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 10px 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #7c3aed;
}

.form-group textarea {
  resize: vertical;
  font-family: inherit;
}

.form-actions {
  margin-top: 28px;
  text-align: center;
}

.submit-btn {
  padding: 12px 48px;
  background: #7c3aed;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background: #6d28d9;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .form-row,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .apply-form-wrap,
  .status-card {
    padding: 24px 20px;
  }
}
</style>
