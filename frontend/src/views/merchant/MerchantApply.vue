<template>
  <div class="merchant-info-page">
    <section class="hero-card">
      <div>
        <p class="hero-eyebrow">商家资料</p>
        <h1>{{ pageTitle }}</h1>
        <p class="hero-copy">{{ pageDescription }}</p>
      </div>
      <button
        v-if="hasApplication"
        type="button"
        class="ghost-btn"
        :disabled="loading || submitting"
        @click="resetFormToStored"
      >
        恢复已保存数据
      </button>
    </section>

    <section v-if="hasApplication" class="status-card" :class="statusClass">
      <div class="status-head">
        <div>
          <p class="status-eyebrow">审核状态</p>
          <h2>{{ statusTitle }}</h2>
          <p>{{ statusDescription }}</p>
        </div>
        <span class="status-badge" :class="statusClass">{{ statusBadge }}</span>
      </div>

      <div class="detail-grid">
        <div class="detail-item">
          <span>真实姓名</span>
          <strong>{{ displayValue(storedInfo.realName) }}</strong>
        </div>
        <div class="detail-item">
          <span>联系电话</span>
          <strong>{{ displayValue(storedInfo.phone) }}</strong>
        </div>
        <div class="detail-item">
          <span>身份证号</span>
          <strong>{{ displayValue(storedInfo.idCard) }}</strong>
        </div>
        <div class="detail-item">
          <span>非遗品类</span>
          <strong>{{ displayValue(storedInfo.heritageType) }}</strong>
        </div>
        <div class="detail-item">
          <span>店铺名称</span>
          <strong>{{ displayValue(storedInfo.shopName) }}</strong>
        </div>
        <div class="detail-item">
          <span>所在地区</span>
          <strong>{{ displayValue(regionText(storedInfo)) }}</strong>
        </div>
        <div class="detail-item full-span">
          <span>店铺地址</span>
          <strong>{{ displayValue(storedInfo.shopAddress) }}</strong>
        </div>
        <div class="detail-item full-span">
          <span>非遗描述</span>
          <strong>{{ displayValue(storedInfo.heritageDescription) }}</strong>
        </div>
        <div class="detail-item full-span">
          <span>店铺简介</span>
          <strong>{{ displayValue(storedInfo.intro) }}</strong>
        </div>
      </div>

      <div class="proof-section">
        <span>资质图片</span>
        <div v-if="storedProofImages.length" class="proof-grid">
          <a
            v-for="(image, index) in storedProofImages"
            :key="`${image}-${index}`"
            class="proof-thumb"
            :href="image"
            target="_blank"
            rel="noreferrer"
          >
            <img :src="image" :alt="`资质图片 ${index + 1}`">
          </a>
        </div>
        <p v-else class="proof-empty">暂无已保存的资质图片。</p>
      </div>

      <div v-if="storedInfo.auditRemark" class="remark-box">
        <span>审核备注</span>
        <p>{{ storedInfo.auditRemark }}</p>
      </div>

      <div class="status-meta">
        <span>提交时间：{{ formatTime(storedInfo.createTime) }}</span>
        <span>最后更新：{{ formatTime(storedInfo.updateTime || storedInfo.createTime) }}</span>
      </div>

      <div class="review-note">
        每次保存都会将商家资料重新提交资格审核。
      </div>
    </section>

    <section class="form-card">
      <div class="section-head">
        <div>
          <p class="section-eyebrow">申请表</p>
          <h2>{{ formTitle }}</h2>
          <p>{{ formDescription }}</p>
        </div>
      </div>

      <div v-if="loading" class="loading-card">
        <span class="loading-spinner"></span>
        <p>正在加载商家资料...</p>
      </div>

      <form v-else class="apply-form" @submit.prevent="submitApplication">
        <div class="form-grid">
          <label class="form-field">
            <span>真实姓名 <em>*</em></span>
            <input v-model.trim="form.realName" type="text" maxlength="50" required>
          </label>

          <label class="form-field">
            <span>身份证号</span>
            <input v-model.trim="form.idCard" type="text" maxlength="18">
          </label>

          <label class="form-field">
            <span>联系电话 <em>*</em></span>
            <input v-model.trim="form.phone" type="text" maxlength="20" required>
          </label>

          <label class="form-field">
            <span>非遗品类 <em>*</em></span>
            <select v-model="form.heritageType" required>
              <option value="">请选择</option>
              <option v-for="item in heritageTypes" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>

          <label class="form-field">
            <span>店铺名称 <em>*</em></span>
            <input v-model.trim="form.shopName" type="text" maxlength="100" required>
          </label>

          <label class="form-field">
            <span>省份</span>
            <input v-model.trim="form.province" type="text" maxlength="32">
          </label>

          <label class="form-field">
            <span>城市</span>
            <input v-model.trim="form.city" type="text" maxlength="32">
          </label>

          <label class="form-field full-span">
            <span>店铺地址</span>
            <input v-model.trim="form.shopAddress" type="text" maxlength="200">
          </label>

          <label class="form-field full-span">
            <span>非遗描述 <em>*</em></span>
            <textarea v-model.trim="form.heritageDescription" rows="5" maxlength="1000" required></textarea>
          </label>

          <label class="form-field full-span">
            <span>店铺简介</span>
            <textarea v-model.trim="form.intro" rows="4" maxlength="255"></textarea>
          </label>

          <div class="form-field full-span">
            <span>资质图片</span>
            <div class="uploader-card">
              <div class="uploader-actions">
                <button
                  type="button"
                  class="secondary-btn"
                  :disabled="uploadingProofs || submitting"
                  @click="triggerProofInput"
                >
                  {{ uploadingProofs ? '上传中...' : '上传图片' }}
                </button>
                <button
                  type="button"
                  class="ghost-btn"
                  :disabled="!form.proofImages.length || submitting"
                  @click="clearProofImages"
                >
                  清除全部
                </button>
              </div>
              <input
                ref="proofInput"
                class="hidden-input"
                type="file"
                accept="image/*"
                multiple
                @change="handleProofChange"
              >

              <div v-if="form.proofImages.length" class="proof-grid editable">
                <div
                  v-for="(image, index) in form.proofImages"
                  :key="`${image}-${index}`"
                  class="proof-thumb proof-thumb--editable"
                >
                  <img :src="image" :alt="`证明图片 ${index + 1}`">
                  <button
                    type="button"
                    class="remove-proof-btn"
                    :disabled="submitting"
                    @click="removeProofImage(index)"
                  >
                    移除
                  </button>
                </div>
              </div>
              <p v-else class="proof-empty">
                上传营业执照、传承人证明或其他资质图片。
              </p>
            </div>
          </div>
        </div>

        <div class="form-footer">
          <p class="form-hint">{{ submitHint }}</p>
          <div class="form-actions">
            <button
              v-if="hasApplication"
              type="button"
              class="secondary-btn"
              :disabled="submitting"
              @click="resetFormToStored"
            >
              重置
            </button>
            <button type="submit" class="primary-btn" :disabled="submitting || uploadingProofs">
              {{ submitButtonText }}
            </button>
          </div>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { applyMerchant, getMyApplication, uploadImage } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notifyApi = appContext.config.globalProperties.$notify

const notify = (type, message) => {
  notifyApi?.[type]?.(message)
}

const heritageTypes = [
  '刺绣',
  '剪纸',
  '陶瓷',
  '漆器',
  '雕刻',
  '戏曲',
  '昆曲',
  '古琴',
  '太极拳',
  '传统医药',
  '年画',
  '其他'
]

const createEmptyForm = () => ({
  realName: '',
  idCard: '',
  phone: '',
  heritageType: '',
  shopName: '',
  province: '',
  city: '',
  shopAddress: '',
  heritageDescription: '',
  intro: '',
  proofImages: []
})

const form = reactive(createEmptyForm())
const storedInfo = ref({})
const appStatus = ref('none')
const loading = ref(false)
const submitting = ref(false)
const uploadingProofs = ref(false)
const proofInput = ref(null)

const hasApplication = computed(() => {
  return Boolean(
    storedInfo.value?.applicationId ||
    storedInfo.value?.merchantProfileId ||
    appStatus.value !== 'none'
  )
})

const storedProofImages = computed(() => parseProofImages(storedInfo.value?.proofImages))

const pageTitle = computed(() => (hasApplication.value ? '商家信息' : '商家入驻申请'))
const pageDescription = computed(() => (
  hasApplication.value
    ? '查看和更新申请时提交的商家资料。保存后将触发新的审核流程。'
    : '填写商家申请表并提交商家认证申请。'
))

const formTitle = computed(() => (hasApplication.value ? '编辑商家资料' : '申请详情'))
const formDescription = computed(() => (
  hasApplication.value
    ? '此页面管理商家入驻时使用的相同资质信息。'
    : '提供完整的商家资料，以便管理员审核您的申请。'
))

const statusClass = computed(() => {
  if (appStatus.value === 'approved') {
    return 'approved'
  }
  if (appStatus.value === 'rejected') {
    return 'rejected'
  }
  if (appStatus.value === 'pending') {
    return 'pending'
  }
  return 'draft'
})

const statusTitle = computed(() => {
  if (appStatus.value === 'approved') {
    return '已通过'
  }
  if (appStatus.value === 'rejected') {
    return '已驳回'
  }
  if (appStatus.value === 'pending') {
    return '审核中'
  }
  return '未提交'
})

const statusDescription = computed(() => {
  if (appStatus.value === 'approved') {
    return '当前商家资料已生效，但每次保存都会将状态重置为待审核。'
  }
  if (appStatus.value === 'rejected') {
    return '请根据审核备注更新相应字段后重新提交。'
  }
  if (appStatus.value === 'pending') {
    return '管理员正在审核您最新提交的商家资料。'
  }
  return '尚未提交任何商家申请。'
})

const statusBadge = computed(() => {
  if (appStatus.value === 'approved') {
    return '已通过'
  }
  if (appStatus.value === 'rejected') {
    return '已驳回'
  }
  if (appStatus.value === 'pending') {
    return '待审核'
  }
  return '草稿'
})

const submitButtonText = computed(() => {
  if (submitting.value) {
    return hasApplication.value ? '保存中...' : '提交中...'
  }
  return hasApplication.value ? '保存并重新提交' : '提交申请'
})

const submitHint = computed(() => (
  hasApplication.value
    ? '保存后将把资料状态重置为待审核。'
    : '提交后请等待审核结果。'
))

function normalizeStatus(value) {
  return ['approved', 'rejected', 'pending'].includes(value) ? value : 'none'
}

function stringValue(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

function parseProofImages(value) {
  if (Array.isArray(value)) {
    return value.filter(Boolean).map(item => String(item))
  }

  if (!value) {
    return []
  }

  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) {
        return parsed.filter(Boolean).map(item => String(item))
      }
    } catch (error) {
      return value ? [value] : []
    }
  }

  return []
}

function applyForm(source = {}) {
  form.realName = stringValue(source.realName)
  form.idCard = stringValue(source.idCard)
  form.phone = stringValue(source.phone)
  form.heritageType = stringValue(source.heritageType)
  form.shopName = stringValue(source.shopName)
  form.province = stringValue(source.province)
  form.city = stringValue(source.city)
  form.shopAddress = stringValue(source.shopAddress)
  form.heritageDescription = stringValue(source.heritageDescription)
  form.intro = stringValue(source.intro)
  form.proofImages = parseProofImages(source.proofImages)
}

function readStoredUser() {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}

function syncStoredUser(patch = {}) {
  const currentUser = readStoredUser()
  if (!currentUser) {
    return
  }

  const nextUser = {
    ...currentUser,
    ...patch
  }

  localStorage.setItem('user', JSON.stringify(nextUser))
  localStorage.setItem('userInfo', JSON.stringify(nextUser))
}

function regionText(source = {}) {
  return [source.province, source.city].filter(Boolean).join(' / ')
}

function displayValue(value) {
  return value ? value : '未填写'
}

function formatTime(value) {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '-'
  }

  return date.toLocaleString()
}

function resetFormToStored() {
  applyForm(storedInfo.value)
}

function triggerProofInput() {
  proofInput.value?.click()
}

function clearProofImages() {
  form.proofImages = []
}

function removeProofImage(index) {
  form.proofImages.splice(index, 1)
}

async function uploadProofImage(file) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await uploadImage(formData, 'merchant-proof')
  if (response.code !== 200 || !response.data?.url) {
    throw new Error(response.message || '上传资质图片失败')
  }

  return response.data.url
}

async function handleProofChange(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''

  if (!files.length) {
    return
  }

  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      notify('warning', '仅支持图片文件。')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      notify('warning', '每张图片大小不能超过 10MB。')
      return
    }
  }

  uploadingProofs.value = true
  try {
    const uploadedUrls = await Promise.all(files.map(uploadProofImage))
    form.proofImages = [...form.proofImages, ...uploadedUrls]
    notify('success', '资质图片上传成功。')
  } catch (error) {
    notify('error', error.message || '上传资质图片失败。')
  } finally {
    uploadingProofs.value = false
  }
}

async function loadApplication() {
  loading.value = true
  try {
    const response = await getMyApplication()
    if (response.code !== 200) {
      throw new Error(response.message || '加载商家资料失败')
    }

    const data = response.data || {}
    storedInfo.value = data
    appStatus.value = normalizeStatus(data.applicationStatus || data.businessStatus || data.shopStatus)
    applyForm(data)
    syncStoredUser({
      shopName: stringValue(data.shopName),
      shopStatus: stringValue(data.shopStatus || data.applicationStatus || data.businessStatus)
    })
  } catch (error) {
    storedInfo.value = {}
    appStatus.value = 'none'
    applyForm(createEmptyForm())
    notify('error', error.message || '加载商家资料失败')
  } finally {
    loading.value = false
  }
}

function validateForm() {
  if (!form.realName || !form.phone || !form.heritageType || !form.shopName || !form.heritageDescription) {
    notify('warning', '请先填写所有必填项。')
    return false
  }
  return true
}

async function submitApplication() {
  if (!validateForm()) {
    return
  }

  submitting.value = true
  const isResubmission = hasApplication.value

  try {
    const response = await applyMerchant({
      realName: form.realName,
      idCard: form.idCard,
      phone: form.phone,
      heritageType: form.heritageType,
      shopName: form.shopName,
      province: form.province,
      city: form.city,
      shopAddress: form.shopAddress,
      heritageDescription: form.heritageDescription,
      intro: form.intro,
      proofImages: JSON.stringify(form.proofImages)
    })

    if (response.code !== 200) {
      throw new Error(response.message || '提交商家资料失败')
    }

    notify(
      'success',
      isResubmission
        ? '商家资料已保存并重新提交审核。'
        : '商家申请提交成功。'
    )
    syncStoredUser({
      shopName: form.shopName,
      shopStatus: 'pending'
    })
    await loadApplication()
  } catch (error) {
    notify('error', error.message || '提交商家资料失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadApplication)
</script>

<style scoped>
.merchant-info-page {
  max-width: 1040px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.status-card,
.form-card,
.loading-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.hero-card,
.status-head,
.form-footer,
.form-actions,
.uploader-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.hero-card {
  padding: 28px 30px;
}

.hero-eyebrow,
.status-eyebrow,
.section-eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  font-weight: 700;
  color: #9a3412;
}

.hero-card h1,
.status-card h2,
.form-card h2 {
  margin: 0;
  color: #111827;
}

.hero-copy,
.status-card p,
.form-card p,
.form-hint,
.status-meta,
.detail-item span,
.remark-box span,
.proof-section span {
  color: #6b7280;
}

.hero-copy {
  margin: 10px 0 0;
  line-height: 1.7;
}

.ghost-btn,
.secondary-btn,
.primary-btn {
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.ghost-btn,
.secondary-btn {
  background: #f3f4f6;
  color: #374151;
  padding: 12px 18px;
}

.primary-btn {
  background: linear-gradient(135deg, #ea580c, #f59e0b);
  color: #ffffff;
  padding: 12px 24px;
}

.ghost-btn:disabled,
.secondary-btn:disabled,
.primary-btn:disabled,
.remove-proof-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.status-card,
.form-card {
  padding: 28px 30px;
}

.status-card.pending {
  border-color: #fbbf24;
}

.status-card.approved {
  border-color: #34d399;
}

.status-card.rejected {
  border-color: #f87171;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  padding: 10px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.status-badge.pending {
  background: #fef3c7;
  color: #92400e;
}

.status-badge.approved {
  background: #dcfce7;
  color: #166534;
}

.status-badge.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.status-badge.draft {
  background: #e5e7eb;
  color: #374151;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.detail-item {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.detail-item span,
.detail-item strong {
  display: block;
}

.detail-item span,
.remark-box span,
.proof-section span {
  font-size: 12px;
  margin-bottom: 8px;
}

.detail-item strong {
  color: #111827;
  line-height: 1.6;
  word-break: break-word;
}

.full-span {
  grid-column: 1 / -1;
}

.proof-section {
  margin-top: 18px;
}

.proof-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.proof-grid.editable {
  margin-top: 14px;
}

.proof-thumb {
  display: block;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.proof-thumb img {
  display: block;
  width: 100%;
  height: 140px;
  object-fit: cover;
}

.proof-thumb--editable {
  position: relative;
}

.remove-proof-btn {
  position: absolute;
  right: 10px;
  bottom: 10px;
  border: none;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.8);
  color: #ffffff;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
}

.proof-empty {
  margin: 0;
  color: #94a3b8;
  line-height: 1.7;
}

.remark-box {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #fff7ed;
  border: 1px solid #fdba74;
}

.remark-box p,
.review-note {
  margin: 0;
  line-height: 1.7;
}

.status-meta {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 13px;
}

.review-note {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 14px;
}

.section-head {
  margin-bottom: 20px;
}

.section-head p {
  margin: 10px 0 0;
  line-height: 1.7;
}

.apply-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-field span {
  color: #374151;
  font-size: 14px;
  font-weight: 600;
}

.form-field em {
  color: #ef4444;
  font-style: normal;
}

.form-field input,
.form-field select,
.form-field textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #111827;
  font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-field textarea {
  resize: vertical;
  font-family: inherit;
}

.form-field input:focus,
.form-field select:focus,
.form-field textarea:focus {
  outline: none;
  border-color: #f97316;
  box-shadow: 0 0 0 4px rgba(249, 115, 22, 0.12);
}

.uploader-card {
  padding: 16px;
  border: 1px dashed #d1d5db;
  border-radius: 16px;
  background: #fafaf9;
}

.form-footer {
  padding-top: 2px;
}

.form-hint {
  margin: 0;
  font-size: 13px;
}

.loading-card {
  padding: 40px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: 3px solid #fed7aa;
  border-top-color: #ea580c;
  animation: spin 0.9s linear infinite;
}

.hidden-input {
  display: none;
}

.ghost-btn:hover:not(:disabled),
.secondary-btn:hover:not(:disabled),
.primary-btn:hover:not(:disabled),
.remove-proof-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .merchant-info-page {
    padding: 16px;
  }

  .hero-card,
  .status-card,
  .form-card {
    padding: 22px 18px;
  }

  .hero-card,
  .status-head,
  .form-footer,
  .form-actions,
  .uploader-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .primary-btn,
  .secondary-btn,
  .ghost-btn {
    width: 100%;
  }
}
</style>
