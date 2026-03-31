<template>
  <div class="merchant-info-page">
    <section class="hero-card">
      <div>
        <p class="hero-eyebrow">MERCHANT PROFILE</p>
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
        Reset To Saved Data
      </button>
    </section>

    <section v-if="hasApplication" class="status-card" :class="statusClass">
      <div class="status-head">
        <div>
          <p class="status-eyebrow">REVIEW STATUS</p>
          <h2>{{ statusTitle }}</h2>
          <p>{{ statusDescription }}</p>
        </div>
        <span class="status-badge" :class="statusClass">{{ statusBadge }}</span>
      </div>

      <div class="detail-grid">
        <div class="detail-item">
          <span>Real Name</span>
          <strong>{{ displayValue(storedInfo.realName) }}</strong>
        </div>
        <div class="detail-item">
          <span>Phone</span>
          <strong>{{ displayValue(storedInfo.phone) }}</strong>
        </div>
        <div class="detail-item">
          <span>ID Card</span>
          <strong>{{ displayValue(storedInfo.idCard) }}</strong>
        </div>
        <div class="detail-item">
          <span>Heritage Type</span>
          <strong>{{ displayValue(storedInfo.heritageType) }}</strong>
        </div>
        <div class="detail-item">
          <span>Shop Name</span>
          <strong>{{ displayValue(storedInfo.shopName) }}</strong>
        </div>
        <div class="detail-item">
          <span>Region</span>
          <strong>{{ displayValue(regionText(storedInfo)) }}</strong>
        </div>
        <div class="detail-item full-span">
          <span>Shop Address</span>
          <strong>{{ displayValue(storedInfo.shopAddress) }}</strong>
        </div>
        <div class="detail-item full-span">
          <span>Heritage Description</span>
          <strong>{{ displayValue(storedInfo.heritageDescription) }}</strong>
        </div>
        <div class="detail-item full-span">
          <span>Shop Intro</span>
          <strong>{{ displayValue(storedInfo.intro) }}</strong>
        </div>
      </div>

      <div class="proof-section">
        <span>Qualification Images</span>
        <div v-if="storedProofImages.length" class="proof-grid">
          <a
            v-for="(image, index) in storedProofImages"
            :key="`${image}-${index}`"
            class="proof-thumb"
            :href="image"
            target="_blank"
            rel="noreferrer"
          >
            <img :src="image" :alt="`Qualification image ${index + 1}`">
          </a>
        </div>
        <p v-else class="proof-empty">No saved qualification images yet.</p>
      </div>

      <div v-if="storedInfo.auditRemark" class="remark-box">
        <span>Review Remark</span>
        <p>{{ storedInfo.auditRemark }}</p>
      </div>

      <div class="status-meta">
        <span>Submitted At: {{ formatTime(storedInfo.createTime) }}</span>
        <span>Last Updated: {{ formatTime(storedInfo.updateTime || storedInfo.createTime) }}</span>
      </div>

      <div class="review-note">
        Every save will submit the merchant profile for qualification review again.
      </div>
    </section>

    <section class="form-card">
      <div class="section-head">
        <div>
          <p class="section-eyebrow">APPLICATION FORM</p>
          <h2>{{ formTitle }}</h2>
          <p>{{ formDescription }}</p>
        </div>
      </div>

      <div v-if="loading" class="loading-card">
        <span class="loading-spinner"></span>
        <p>Loading merchant profile...</p>
      </div>

      <form v-else class="apply-form" @submit.prevent="submitApplication">
        <div class="form-grid">
          <label class="form-field">
            <span>Real Name <em>*</em></span>
            <input v-model.trim="form.realName" type="text" maxlength="50" required>
          </label>

          <label class="form-field">
            <span>ID Card</span>
            <input v-model.trim="form.idCard" type="text" maxlength="18">
          </label>

          <label class="form-field">
            <span>Phone <em>*</em></span>
            <input v-model.trim="form.phone" type="text" maxlength="20" required>
          </label>

          <label class="form-field">
            <span>Heritage Type <em>*</em></span>
            <select v-model="form.heritageType" required>
              <option value="">Select</option>
              <option v-for="item in heritageTypes" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>

          <label class="form-field">
            <span>Shop Name <em>*</em></span>
            <input v-model.trim="form.shopName" type="text" maxlength="100" required>
          </label>

          <label class="form-field">
            <span>Province</span>
            <input v-model.trim="form.province" type="text" maxlength="32">
          </label>

          <label class="form-field">
            <span>City</span>
            <input v-model.trim="form.city" type="text" maxlength="32">
          </label>

          <label class="form-field full-span">
            <span>Shop Address</span>
            <input v-model.trim="form.shopAddress" type="text" maxlength="200">
          </label>

          <label class="form-field full-span">
            <span>Heritage Description <em>*</em></span>
            <textarea v-model.trim="form.heritageDescription" rows="5" maxlength="1000" required></textarea>
          </label>

          <label class="form-field full-span">
            <span>Shop Intro</span>
            <textarea v-model.trim="form.intro" rows="4" maxlength="255"></textarea>
          </label>

          <div class="form-field full-span">
            <span>Qualification Images</span>
            <div class="uploader-card">
              <div class="uploader-actions">
                <button
                  type="button"
                  class="secondary-btn"
                  :disabled="uploadingProofs || submitting"
                  @click="triggerProofInput"
                >
                  {{ uploadingProofs ? 'Uploading...' : 'Upload Images' }}
                </button>
                <button
                  type="button"
                  class="ghost-btn"
                  :disabled="!form.proofImages.length || submitting"
                  @click="clearProofImages"
                >
                  Clear All
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
                  <img :src="image" :alt="`Proof image ${index + 1}`">
                  <button
                    type="button"
                    class="remove-proof-btn"
                    :disabled="submitting"
                    @click="removeProofImage(index)"
                  >
                    Remove
                  </button>
                </div>
              </div>
              <p v-else class="proof-empty">
                Upload business certificates, inheritor proof, or any supporting qualification images.
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
              Reset
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
  'Embroidery',
  'Paper Cutting',
  'Ceramics',
  'Lacquerware',
  'Carving',
  'Opera',
  'Kunqu',
  'Guqin',
  'Tai Chi',
  'Traditional Medicine',
  'New Year Painting',
  'Other'
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

const pageTitle = computed(() => (hasApplication.value ? 'Merchant Info' : 'Merchant Application'))
const pageDescription = computed(() => (
  hasApplication.value
    ? 'Review and update the merchant details submitted during your application. Saving will trigger a new review.'
    : 'Complete the merchant application form to submit your merchant verification request.'
))

const formTitle = computed(() => (hasApplication.value ? 'Edit Merchant Details' : 'Application Details'))
const formDescription = computed(() => (
  hasApplication.value
    ? 'This page manages the same merchant qualification details used during merchant onboarding.'
    : 'Provide complete merchant details to help the admin team review your application.'
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
    return 'Approved'
  }
  if (appStatus.value === 'rejected') {
    return 'Rejected'
  }
  if (appStatus.value === 'pending') {
    return 'In Review'
  }
  return 'Not Submitted'
})

const statusDescription = computed(() => {
  if (appStatus.value === 'approved') {
    return 'Your current merchant profile is active, but every new save will send it back for review.'
  }
  if (appStatus.value === 'rejected') {
    return 'Update the required fields based on the review remark and submit again.'
  }
  if (appStatus.value === 'pending') {
    return 'The admin team is currently reviewing the latest merchant details you submitted.'
  }
  return 'No merchant application has been submitted yet.'
})

const statusBadge = computed(() => {
  if (appStatus.value === 'approved') {
    return 'Approved'
  }
  if (appStatus.value === 'rejected') {
    return 'Rejected'
  }
  if (appStatus.value === 'pending') {
    return 'Pending'
  }
  return 'Draft'
})

const submitButtonText = computed(() => {
  if (submitting.value) {
    return hasApplication.value ? 'Saving...' : 'Submitting...'
  }
  return hasApplication.value ? 'Save And Resubmit' : 'Submit Application'
})

const submitHint = computed(() => (
  hasApplication.value
    ? 'Saving the profile will reset its status to pending review.'
    : 'After submission, please wait for the review result.'
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
  return value ? value : 'Not provided'
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
    throw new Error(response.message || 'Failed to upload qualification image')
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
      notify('warning', 'Only image files are supported.')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      notify('warning', 'Each image must be smaller than 10MB.')
      return
    }
  }

  uploadingProofs.value = true
  try {
    const uploadedUrls = await Promise.all(files.map(uploadProofImage))
    form.proofImages = [...form.proofImages, ...uploadedUrls]
    notify('success', 'Qualification images uploaded successfully.')
  } catch (error) {
    notify('error', error.message || 'Failed to upload qualification images.')
  } finally {
    uploadingProofs.value = false
  }
}

async function loadApplication() {
  loading.value = true
  try {
    const response = await getMyApplication()
    if (response.code !== 200) {
      throw new Error(response.message || 'Failed to load merchant profile')
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
    notify('error', error.message || 'Failed to load merchant profile')
  } finally {
    loading.value = false
  }
}

function validateForm() {
  if (!form.realName || !form.phone || !form.heritageType || !form.shopName || !form.heritageDescription) {
    notify('warning', 'Please complete all required fields first.')
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
      throw new Error(response.message || 'Failed to submit merchant profile')
    }

    notify(
      'success',
      isResubmission
        ? 'Merchant profile saved and resubmitted for review.'
        : 'Merchant application submitted successfully.'
    )
    syncStoredUser({
      shopName: form.shopName,
      shopStatus: 'pending'
    })
    await loadApplication()
  } catch (error) {
    notify('error', error.message || 'Failed to submit merchant profile')
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
