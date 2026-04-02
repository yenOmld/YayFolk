<template>
  <div class="merchant-dashboard-page">
    <section class="hero-card">
      <div class="hero-copy">
        <p class="hero-eyebrow">Merchant Workspace</p>
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageDescription }}</p>
        <div class="hero-meta">
          <span :class="['status-pill', statusMeta.className]">{{ statusMeta.badge }}</span>
          <span v-if="storedInfo.shopName">{{ storedInfo.shopName }}</span>
          <span v-if="storedInfo.updateTime">Updated {{ formatTime(storedInfo.updateTime) }}</span>
        </div>
      </div>

      <div class="hero-actions">
        <button
          v-if="canShowStats"
          type="button"
          class="secondary-btn"
          @click="router.push('/merchant/activities')"
        >
          Open Activities
        </button>
        <button
          v-if="hasApplication && !isApproved"
          type="button"
          class="ghost-btn"
          :disabled="loading || submitting"
          @click="resetFormToStored"
        >
          Reset Form
        </button>
      </div>
    </section>

    <section v-if="canShowStats" class="panel-card stats-card-shell">
      <div class="section-head">
        <div>
          <p class="section-eyebrow">Business Overview</p>
          <h2>Merchant Analytics</h2>
          <p>Overview of bookings, booking revenue, activity performance, and recent reviews.</p>
        </div>
        <button
          type="button"
          class="ghost-btn"
          :disabled="merchantStatsLoading"
          @click="loadMerchantStats"
        >
          {{ merchantStatsLoading ? 'Refreshing...' : 'Refresh Stats' }}
        </button>
      </div>

      <MerchantStatsPanel
        :stats="merchantStats"
        :loading="merchantStatsLoading"
        :visible="canShowStats"
        @navigate="handleStatsNavigate"
      />
    </section>

    <section class="panel-card status-card" :class="statusMeta.className">
      <div class="section-head status-head">
        <div>
          <p class="section-eyebrow">Application Status</p>
          <h2>{{ statusMeta.title }}</h2>
          <p>{{ statusMeta.description }}</p>
        </div>
        <div class="status-head-actions">
          <button
            v-if="isApproved"
            type="button"
            class="secondary-btn"
            :disabled="loading || submitting"
            @click="openFormModal"
          >
            Update Merchant Information
          </button>
          <span :class="['status-pill', statusMeta.className]">{{ statusMeta.badge }}</span>
        </div>
      </div>

      <div v-if="hasApplication" class="detail-grid">
        <div class="detail-item">
          <span>Legal Name</span>
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
          <span>Shop Introduction</span>
          <strong>{{ displayValue(storedInfo.intro) }}</strong>
        </div>
      </div>

      <div v-else class="empty-state compact-empty">
        <i class="bx bx-file-find"></i>
        <p>No merchant application has been submitted yet.</p>
      </div>

      <div v-if="storedProofImages.length" class="proof-section">
        <div class="proof-head">
          <span>Submitted Proof Files</span>
          <small>{{ storedProofImages.length }} file(s)</small>
        </div>
        <div class="proof-grid">
          <a
            v-for="(image, index) in storedProofImages"
            :key="`${image}-${index}`"
            class="proof-thumb"
            :href="image"
            target="_blank"
            rel="noreferrer"
          >
            <img :src="image" :alt="`Proof ${index + 1}`">
          </a>
        </div>
      </div>

      <div v-if="storedInfo.auditRemark" class="remark-box">
        <span>Audit Remark</span>
        <p>{{ storedInfo.auditRemark }}</p>
      </div>

      <div v-if="hasApplication" class="status-meta">
        <span>Submitted {{ formatTime(storedInfo.createTime) }}</span>
        <span>Updated {{ formatTime(storedInfo.updateTime || storedInfo.createTime) }}</span>
        <span v-if="storedInfo.auditTime">Reviewed {{ formatTime(storedInfo.auditTime) }}</span>
      </div>
    </section>

    <section v-if="showInlineForm" class="panel-card form-card">
      <div class="section-head">
        <div>
          <p class="section-eyebrow">Merchant Profile</p>
          <h2>{{ formTitle }}</h2>
          <p>{{ formDescription }}</p>
        </div>
      </div>

      <MerchantProfileForm
        :form="form"
        :loading="loading"
        :submitting="submitting"
        :uploading-proofs="uploadingProofs"
        :has-application="hasApplication"
        :heritage-types="heritageTypes"
        :submit-hint="submitHint"
        :submit-button-text="submitButtonText"
        @submit="submitApplication"
        @reset="resetFormToStored"
        @proof-change="handleProofChange"
        @clear-proof-images="clearProofImages"
        @remove-proof-image="removeProofImage"
      />
    </section>

    <div v-if="showFormModal" class="modal-overlay" @click.self="closeFormModal">
      <section class="modal-card">
        <div class="modal-head">
          <div>
            <p class="section-eyebrow">Merchant Profile</p>
            <h2>{{ formTitle }}</h2>
            <p>{{ formDescription }}</p>
          </div>
          <button
            type="button"
            class="modal-close-btn"
            :disabled="submitting"
            @click="closeFormModal"
          >
            <i class="bx bx-x"></i>
          </button>
        </div>

        <MerchantProfileForm
          :form="form"
          :loading="loading"
          :submitting="submitting"
          :uploading-proofs="uploadingProofs"
          :has-application="hasApplication"
          :heritage-types="heritageTypes"
          :submit-hint="submitHint"
          :submit-button-text="submitButtonText"
          @submit="submitApplication"
          @reset="resetFormToStored"
          @proof-change="handleProofChange"
          @clear-proof-images="clearProofImages"
          @remove-proof-image="removeProofImage"
        />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MerchantProfileForm from '@/components/merchant/MerchantProfileForm.vue'
import MerchantStatsPanel from '@/components/merchant/MerchantStatsPanel.vue'
import { applyMerchant, getMerchantActivities, getMerchantBookings, getMerchantStats, getMyApplication, uploadImage } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notifyApi = appContext.config.globalProperties.$notify
const router = useRouter()
const route = useRoute()

const notify = (type, message) => {
  notifyApi?.[type]?.(message)
}

const heritageTypes = [
  'Embroidery',
  'Paper Cutting',
  'Ceramics',
  'Lacquerware',
  'Wood Carving',
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
const currentUser = ref(readStoredUser())
const storedInfo = ref({})
const loading = ref(false)
const submitting = ref(false)
const uploadingProofs = ref(false)
const merchantStatsLoading = ref(false)
const merchantStats = ref({})
const showFormModal = ref(false)

const hasMerchantAccess = computed(() => {
  const storedRole = currentUser.value?.role
  if (storedRole === 'merchant' || storedRole === 'admin') {
    return true
  }

  if (Number(storedInfo.value?.isMerchant || 0) === 1) {
    return true
  }

  const status = normalizeStatus(storedInfo.value?.applicationStatus || storedInfo.value?.businessStatus || storedInfo.value?.shopStatus)
  return status === 'approved'
})

const canShowStats = computed(() => hasMerchantAccess.value)
const isApproved = computed(() => normalizeStatus(storedInfo.value?.applicationStatus || storedInfo.value?.businessStatus || storedInfo.value?.shopStatus) === 'approved')
const showInlineForm = computed(() => !isApproved.value)
const storedProofImages = computed(() => parseProofImages(storedInfo.value?.proofImages))
const hasApplication = computed(() => Boolean(
  storedInfo.value?.applicationId ||
  storedInfo.value?.merchantProfileId ||
  normalizeStatus(storedInfo.value?.applicationStatus || storedInfo.value?.businessStatus || storedInfo.value?.shopStatus) !== 'none'
))

const statusMeta = computed(() => {
  const status = normalizeStatus(storedInfo.value?.applicationStatus || storedInfo.value?.businessStatus || storedInfo.value?.shopStatus)
  if (status === 'approved') {
    return {
      className: 'approved',
      title: 'Approved Merchant Profile',
      badge: 'Approved',
      description: 'Your merchant profile is active. Saving the form again will submit a fresh review request.'
    }
  }
  if (status === 'rejected') {
    return {
      className: 'rejected',
      title: 'Changes Required',
      badge: 'Rejected',
      description: 'Please update the profile based on the audit remark and submit it again.'
    }
  }
  if (status === 'pending') {
    return {
      className: 'pending',
      title: 'Review In Progress',
      badge: 'Pending',
      description: 'Your latest merchant profile is waiting for review.'
    }
  }
  return {
    className: 'draft',
    title: 'Draft Merchant Application',
    badge: 'Draft',
    description: 'Submit your merchant information to start the approval process.'
  }
})

const pageTitle = computed(() => (
  canShowStats.value
    ? 'Merchant Center'
    : hasApplication.value
      ? 'Merchant Application Workspace'
      : 'Apply to Become a Merchant'
))

const pageDescription = computed(() => (
  canShowStats.value
    ? 'Manage merchant qualifications, check the health of recent bookings, and jump directly into activities or booking management.'
    : 'Complete the merchant profile, upload proof materials, and track the application review result here.'
))

const formTitle = computed(() => (hasApplication.value ? 'Update Merchant Information' : 'Merchant Application Form'))
const formDescription = computed(() => (
  hasApplication.value
    ? 'The form below is prefilled with your latest submitted merchant profile. Saving will start a new review round.'
    : 'Provide complete merchant information so the platform team can review your qualification request.'
))

const submitButtonText = computed(() => {
  if (submitting.value) {
    return hasApplication.value ? 'Saving...' : 'Submitting...'
  }
  return hasApplication.value ? 'Save and Resubmit' : 'Submit Application'
})

const submitHint = computed(() => (
  hasApplication.value
    ? 'Any update to this form will reset the review status to pending.'
    : 'After submission, please wait for the merchant review result.'
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
      return value ? [String(value)] : []
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
    return {}
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    return {}
  }
}

function syncStoredUser(patch = {}) {
  const nextUser = {
    ...readStoredUser(),
    ...patch
  }

  currentUser.value = nextUser
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
    return 'Not recorded'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return 'Not recorded'
  }

  return date.toLocaleString('en-US')
}

function resetFormToStored() {
  applyForm(storedInfo.value)
}

function openFormModal() {
  resetFormToStored()
  showFormModal.value = true
}

function closeFormModal() {
  if (submitting.value) {
    return
  }
  showFormModal.value = false
}

function createRecentSalesTrend(days = 7) {
  const today = new Date()
  const salesTrend = []
  for (let offset = days - 1; offset >= 0; offset -= 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - offset)
    const dateKey = date.toISOString().slice(0, 10)
    salesTrend.push({
      date: dateKey,
      label: dateKey.slice(5),
      bookingCount: 0,
      participantCount: 0,
      bookingRevenue: 0
    })
  }
  return salesTrend
}

function createEmptyMerchantStats() {
  return {
    summary: {
      activityCount: 0,
      bookingCount: 0,
      pendingCheckinCount: 0,
      checkedInCount: 0,
      rejectedCount: 0,
      cancelledCount: 0,
      reviewCount: 0,
      totalRevenue: 0,
      bookingRevenue: 0,
      averageScore: 0,
      followerCount: 0,
      uniqueCustomerCount: 0
    },
    bookingStatus: [
      { key: 'registered', label: 'Active', color: '#1661ab', count: 0 },
      { key: 'checked_in', label: 'Checked In', color: '#1f8a70', count: 0 },
      { key: 'rejected', label: 'Rejected', color: '#c04851', count: 0 },
      { key: 'cancelled', label: 'Cancelled', color: '#6b7280', count: 0 }
    ],
    salesTrend: createRecentSalesTrend(),
    topActivities: [],
    recentReviews: []
  }
}

function normalizeStatsPayload(payload) {
  const base = createEmptyMerchantStats()
  const stats = payload && typeof payload === 'object' ? payload : {}
  return {
    summary: {
      ...base.summary,
      ...(stats.summary || {})
    },
    bookingStatus: Array.isArray(stats.bookingStatus) && stats.bookingStatus.length ? stats.bookingStatus : base.bookingStatus,
    salesTrend: Array.isArray(stats.salesTrend) && stats.salesTrend.length ? stats.salesTrend : base.salesTrend,
    topActivities: Array.isArray(stats.topActivities) ? stats.topActivities : base.topActivities,
    recentReviews: Array.isArray(stats.recentReviews) ? stats.recentReviews : base.recentReviews
  }
}

function normalizeBookingStatus(status) {
  if (!status) {
    return 'registered'
  }
  if (['registered', 'pending'].includes(status)) {
    return 'registered'
  }
  if (['checked_in', 'completed'].includes(status)) {
    return 'checked_in'
  }
  if (status === 'rejected') {
    return 'rejected'
  }
  if (status === 'cancelled') {
    return 'cancelled'
  }
  return status
}

function safeNumber(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function toDateKey(value) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toISOString().slice(0, 10)
}

function sortByCreateTimeDesc(items = []) {
  return [...items].sort((left, right) => {
    const leftTime = new Date(left?.createTime || 0).getTime() || 0
    const rightTime = new Date(right?.createTime || 0).getTime() || 0
    return rightTime - leftTime
  })
}

function buildFallbackSalesTrend(bookings = []) {
  const recentBuckets = createRecentSalesTrend()
  const grouped = new Map(recentBuckets.map(item => [item.date, { ...item }]))

  bookings.forEach((booking) => {
    const bookingDate = toDateKey(booking.paymentTime || booking.createTime)
    if (!bookingDate || !grouped.has(bookingDate)) {
      return
    }

    const bucket = grouped.get(bookingDate)
    bucket.bookingCount += 1
    bucket.participantCount += safeNumber(booking.participantCount ?? booking.participantNum ?? 1)
    if (booking.paymentStatus === 'paid' || safeNumber(booking.payStatus) === 1) {
      bucket.bookingRevenue += safeNumber(booking.payAmount ?? booking.totalAmount)
    }
  })

  return [...grouped.values()].sort((left, right) => left.date.localeCompare(right.date))
}

function buildFallbackTopActivities(activities = [], bookings = []) {
  const grouped = new Map()

  activities.forEach((activity) => {
    grouped.set(String(activity.id), {
      activityId: activity.id,
      title: activity.title || 'Untitled Activity',
      bookingCount: safeNumber(activity.bookingCount),
      participantCount: safeNumber(activity.currentParticipants),
      revenue: 0,
      viewCount: safeNumber(activity.viewCount),
      collectCount: safeNumber(activity.collectCount),
      signupCount: safeNumber(activity.signupCount)
    })
  })

  bookings.forEach((booking) => {
    const key = String(booking.activityId || '')
    if (!key) {
      return
    }

    if (!grouped.has(key)) {
      grouped.set(key, {
        activityId: booking.activityId,
        title: booking.activityTitle || 'Untitled Activity',
        bookingCount: 0,
        participantCount: 0,
        revenue: 0,
        viewCount: 0,
        collectCount: 0,
        signupCount: 0
      })
    }

    const item = grouped.get(key)
    item.bookingCount += 1
    item.participantCount += safeNumber(booking.participantCount ?? booking.participantNum ?? 1)
    if (booking.paymentStatus === 'paid' || safeNumber(booking.payStatus) === 1) {
      item.revenue += safeNumber(booking.payAmount ?? booking.totalAmount)
    }
  })

  return [...grouped.values()]
    .sort((left, right) => {
      if (right.revenue !== left.revenue) return right.revenue - left.revenue
      if (right.bookingCount !== left.bookingCount) return right.bookingCount - left.bookingCount
      if (right.signupCount !== left.signupCount) return right.signupCount - left.signupCount
      if (right.viewCount !== left.viewCount) return right.viewCount - left.viewCount
      if (right.collectCount !== left.collectCount) return right.collectCount - left.collectCount
      return right.participantCount - left.participantCount
    })
    .slice(0, 5)
}

function buildFallbackRecentReviews(bookings = []) {
  return sortByCreateTimeDesc(
    bookings
      .filter(item => item.reviewScore !== undefined && item.reviewScore !== null)
      .map(item => ({
        id: item.id,
        score: item.reviewScore,
        content: item.reviewContent,
        createTime: item.reviewTime || item.updateTime || item.createTime,
        userName: item.customerName || item.participantName || `User ${item.userId}`,
        userAvatar: item.customerAvatar || '/default-avatar.svg',
        targetName: item.activityTitle || 'Activity Review'
      }))
  ).slice(0, 5)
}

function buildStatsFromCollections(activities = [], bookings = [], apiStats = {}) {
  const base = normalizeStatsPayload(apiStats)
  const stats = createEmptyMerchantStats()

  const uniqueCustomers = new Set()
  let bookingRevenue = 0
  let reviewTotal = 0
  let reviewCount = 0

  bookings.forEach((booking) => {
    const status = normalizeBookingStatus(booking.status || booking.reserveStatus)
    stats.summary.bookingCount += 1
    if (status === 'registered') stats.summary.pendingCheckinCount += 1
    if (status === 'checked_in') stats.summary.checkedInCount += 1
    if (status === 'rejected') stats.summary.rejectedCount += 1
    if (status === 'cancelled') stats.summary.cancelledCount += 1
    if (booking.userId) uniqueCustomers.add(String(booking.userId))

    if (booking.paymentStatus === 'paid' || safeNumber(booking.payStatus) === 1) {
      bookingRevenue += safeNumber(booking.payAmount ?? booking.totalAmount)
    }

    if (booking.reviewScore !== undefined && booking.reviewScore !== null) {
      reviewTotal += safeNumber(booking.reviewScore)
      reviewCount += 1
    }
  })

  stats.summary.activityCount = activities.length
  stats.summary.reviewCount = reviewCount
  stats.summary.totalRevenue = bookingRevenue
  stats.summary.bookingRevenue = bookingRevenue
  stats.summary.uniqueCustomerCount = uniqueCustomers.size
  stats.summary.averageScore = reviewCount ? Number((reviewTotal / reviewCount).toFixed(1)) : 0

  stats.bookingStatus = [
    { key: 'registered', label: 'Active', color: '#1661ab', count: stats.summary.pendingCheckinCount },
    { key: 'checked_in', label: 'Checked In', color: '#1f8a70', count: stats.summary.checkedInCount },
    { key: 'rejected', label: 'Rejected', color: '#c04851', count: stats.summary.rejectedCount },
    { key: 'cancelled', label: 'Cancelled', color: '#6b7280', count: stats.summary.cancelledCount }
  ]
  stats.salesTrend = buildFallbackSalesTrend(bookings)
  stats.topActivities = buildFallbackTopActivities(activities, bookings)
  stats.recentReviews = buildFallbackRecentReviews(bookings)

  return {
    summary: {
      ...base.summary,
      ...stats.summary,
      activityCount: stats.summary.activityCount || base.summary.activityCount,
      bookingCount: stats.summary.bookingCount || base.summary.bookingCount,
      pendingCheckinCount: stats.summary.pendingCheckinCount || base.summary.pendingCheckinCount,
      checkedInCount: stats.summary.checkedInCount || base.summary.checkedInCount,
      rejectedCount: stats.summary.rejectedCount || base.summary.rejectedCount,
      cancelledCount: stats.summary.cancelledCount || base.summary.cancelledCount,
      reviewCount: stats.summary.reviewCount || base.summary.reviewCount,
      totalRevenue: stats.summary.totalRevenue || base.summary.totalRevenue,
      bookingRevenue: stats.summary.bookingRevenue || base.summary.bookingRevenue,
      averageScore: stats.summary.averageScore || base.summary.averageScore,
      uniqueCustomerCount: stats.summary.uniqueCustomerCount || base.summary.uniqueCustomerCount
    },
    bookingStatus: stats.bookingStatus.some(item => item.count > 0) ? stats.bookingStatus : base.bookingStatus,
    salesTrend: stats.salesTrend.length ? stats.salesTrend : base.salesTrend,
    topActivities: stats.topActivities.length ? stats.topActivities : base.topActivities,
    recentReviews: stats.recentReviews.length ? stats.recentReviews : base.recentReviews
  }
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
    throw new Error(response.message || 'Failed to upload proof image')
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
      notify('warning', 'Each proof image must be smaller than 10MB.')
      return
    }
  }

  uploadingProofs.value = true
  try {
    const uploadedUrls = await Promise.all(files.map(uploadProofImage))
    form.proofImages = [...form.proofImages, ...uploadedUrls]
    notify('success', 'Proof images uploaded successfully.')
  } catch (error) {
    notify('error', error.message || 'Failed to upload proof images.')
  } finally {
    uploadingProofs.value = false
  }
}

async function loadApplication() {
  loading.value = true
  try {
    const response = await getMyApplication()
    if (response.code !== 200) {
      throw new Error(response.message || 'Failed to load merchant information')
    }

    const data = response.data || {}
    storedInfo.value = data
    applyForm(data)
    const resolvedStatus = normalizeStatus(data.applicationStatus || data.businessStatus || data.shopStatus)
    const nextRole = currentUser.value?.role === 'admin'
      ? 'admin'
      : (Number(data.isMerchant || 0) === 1 || resolvedStatus === 'approved' ? 'merchant' : currentUser.value?.role)

    syncStoredUser({
      role: nextRole,
      isMerchant: Number(data.isMerchant || 0),
      shopName: stringValue(data.shopName) || currentUser.value.shopName,
      shopStatus: stringValue(data.shopStatus || data.applicationStatus || data.businessStatus)
    })
  } catch (error) {
    storedInfo.value = {}
    applyForm(createEmptyForm())
    notify('error', error.message || 'Failed to load merchant information')
  } finally {
    loading.value = false
  }
}

async function loadMerchantStats() {
  if (!canShowStats.value) {
    merchantStats.value = createEmptyMerchantStats()
    return
  }

  merchantStatsLoading.value = true
  try {
    const [statsResponse, activitiesResponse, bookingsResponse] = await Promise.all([
      getMerchantStats().catch(() => null),
      getMerchantActivities().catch(() => null),
      getMerchantBookings({ status: 'all' }).catch(() => null)
    ])

    const apiStats = statsResponse?.code === 200 ? statsResponse.data || {} : {}
    const activities = activitiesResponse?.code === 200 && Array.isArray(activitiesResponse.data) ? activitiesResponse.data : []
    const bookings = bookingsResponse?.code === 200 && Array.isArray(bookingsResponse.data?.items) ? bookingsResponse.data.items : []

    merchantStats.value = buildStatsFromCollections(activities, bookings, apiStats)
  } catch (error) {
    merchantStats.value = createEmptyMerchantStats()
    notify('error', error.message || 'Failed to load merchant analytics')
  } finally {
    merchantStatsLoading.value = false
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
      throw new Error(response.message || 'Failed to submit merchant information')
    }

    notify('success', isResubmission ? 'Merchant profile updated and resubmitted for review.' : 'Merchant application submitted successfully.')
    showFormModal.value = false
    syncStoredUser({
      role: currentUser.value?.role,
      isMerchant: Number(currentUser.value?.isMerchant || 0),
      shopName: form.shopName,
      shopStatus: 'pending'
    })
    await loadApplication()
    await loadMerchantStats()
  } catch (error) {
    notify('error', error.message || 'Failed to submit merchant information')
  } finally {
    submitting.value = false
  }
}

function handleStatsNavigate(target) {
  if (!target?.type) {
    return
  }

  if (target.type === 'activities') {
    router.push('/merchant/activities')
    return
  }

  if (target.type === 'bookings') {
    router.push({
      path: '/merchant/bookings',
      query: {
        ...(target.status ? { status: target.status } : {}),
        backTo: route.fullPath
      }
    })
    return
  }

  if (target.type === 'activity' && target.activityId) {
    router.push({
      path: '/merchant/bookings',
      query: {
        activityId: String(target.activityId),
        ...(target.title ? { title: target.title } : {}),
        status: 'all',
        backTo: route.fullPath
      }
    })
  }
}

onMounted(async () => {
  currentUser.value = readStoredUser()
  await loadApplication()
  await loadMerchantStats()
})
</script>

<style scoped>
.merchant-dashboard-page {
  --merchant-accent: #9d2929;
  --merchant-accent-strong: #7f1d1d;
  --merchant-blue: #1661ab;
  --merchant-ink: #1f2937;
  --merchant-soft: #64748b;
  --merchant-border: #e7ddd1;
  --merchant-panel: rgba(255, 255, 255, 0.92);
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.panel-card {
  border-radius: 28px;
  border: 1px solid var(--merchant-border);
  background: var(--merchant-panel);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
}

.hero-card {
  padding: 28px 30px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.hero-eyebrow,
.section-eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #9a3412;
}

.hero-copy h1,
.section-head h2 {
  margin: 0;
  color: var(--merchant-ink);
}

.hero-copy p,
.section-head p,
.form-hint,
.status-meta,
.detail-item span,
.proof-empty,
.compact-empty p {
  color: var(--merchant-soft);
}

.hero-copy > p:last-of-type,
.section-head p {
  margin: 10px 0 0;
  line-height: 1.7;
}

.hero-meta,
.hero-actions,
.status-meta,
.proof-head,
.status-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-meta {
  margin-top: 16px;
  flex-wrap: wrap;
}

.hero-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.status-head-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.status-pill,
.primary-btn,
.secondary-btn,
.ghost-btn,
.modal-close-btn {
  border: none;
  border-radius: 14px;
  font-weight: 700;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  font-size: 13px;
}

.status-pill.approved {
  background: #dcfce7;
  color: #166534;
}

.status-pill.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.status-pill.pending {
  background: #fef3c7;
  color: #92400e;
}

.status-pill.draft {
  background: #e5e7eb;
  color: #374151;
}

.primary-btn,
.secondary-btn,
.ghost-btn,
.modal-close-btn {
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease, box-shadow 0.2s ease;
}

.primary-btn,
.secondary-btn,
.ghost-btn {
  min-height: 46px;
  padding: 0 18px;
}

.primary-btn {
  background: linear-gradient(135deg, var(--merchant-accent), #d97706);
  color: #ffffff;
}

.secondary-btn {
  background: rgba(22, 97, 171, 0.1);
  color: var(--merchant-blue);
}

.ghost-btn {
  background: #f3f4f6;
  color: #374151;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.panel-card {
  padding: 28px 30px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
}

.stats-card-shell {
  overflow: hidden;
}

.status-card.approved {
  border-color: rgba(34, 197, 94, 0.28);
}

.status-card.rejected {
  border-color: rgba(239, 68, 68, 0.28);
}

.status-card.pending {
  border-color: rgba(245, 158, 11, 0.28);
}

.status-card.draft {
  border-color: rgba(148, 163, 184, 0.28);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-item {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid #e5e7eb;
  background: #faf8f5;
}

.detail-item span,
.detail-item strong {
  display: block;
}

.detail-item span,
.proof-head span,
.remark-box span {
  font-size: 12px;
  margin-bottom: 8px;
}

.detail-item strong {
  color: var(--merchant-ink);
  line-height: 1.7;
  word-break: break-word;
}

.full-span {
  grid-column: 1 / -1;
}

.proof-section {
  margin-top: 18px;
}

.proof-head {
  justify-content: space-between;
  margin-bottom: 12px;
}

.proof-head small {
  color: #94a3b8;
}

.proof-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.proof-thumb {
  position: relative;
  display: block;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.proof-thumb img {
  display: block;
  width: 100%;
  height: 148px;
  object-fit: cover;
}

.editable-proof-grid {
  margin-top: 14px;
}

.remove-proof-btn {
  position: absolute;
  right: 10px;
  bottom: 10px;
  min-height: 32px;
  padding: 0 12px;
  background: rgba(15, 23, 42, 0.82);
  color: #ffffff;
}

.remark-box {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid #fdba74;
  background: #fff7ed;
}

.remark-box p {
  margin: 0;
  color: #9a3412;
  line-height: 1.7;
}

.status-meta {
  margin-top: 18px;
  flex-wrap: wrap;
  font-size: 13px;
}

.form-card {
  overflow: hidden;
}

.proof-empty,
.empty-state {
  margin: 0;
  text-align: center;
}

.empty-state {
  padding: 42px 20px;
  border-radius: 20px;
  background: #f8fafc;
}

.empty-state i {
  display: block;
  font-size: 32px;
  color: #94a3b8;
  margin-bottom: 12px;
}

.compact-empty {
  padding: 28px 20px;
}

.loading-state p {
  color: #64748b;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.48);
}

.modal-card {
  width: min(960px, 100%);
  max-height: calc(100vh - 48px);
  overflow: auto;
  border-radius: 28px;
  border: 1px solid rgba(231, 221, 209, 0.9);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.2);
  padding: 28px 30px;
}

.modal-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
}

.modal-close-btn {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #f3f4f6;
  color: #374151;
  flex-shrink: 0;
}

.modal-close-btn i {
  font-size: 22px;
}

.modal-head p {
  margin: 0;
  line-height: 1.7;
}

button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

@media (max-width: 920px) {
  .hero-card,
  .section-head,
  .modal-head,
  .status-head {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-actions {
    width: 100%;
    flex-wrap: wrap;
    justify-content: stretch;
  }

  .hero-actions > * {
    flex: 1 1 180px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .hero-card,
  .panel-card {
    padding: 22px 18px;
    border-radius: 22px;
  }

  .modal-overlay {
    padding: 12px;
  }

  .modal-card {
    max-height: calc(100vh - 24px);
    padding: 22px 18px;
    border-radius: 22px;
  }

  .proof-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-actions > * {
    width: 100%;
  }
}
</style>
