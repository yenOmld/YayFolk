<template>
  <div class="scanner-page">
    <header class="scanner-header">
      <div class="header-main">
        <button class="icon-button" type="button" @click="goBack">
          <i class="bx bx-arrow-back"></i>
        </button>
        <div>
          <p class="eyebrow">Merchant Check-in</p>
          <h1>Booking Scanner</h1>
          <p class="header-copy">
            Use live camera detection when available. If the browser cannot detect QR codes in real time,
            capture a frame, upload a photo, or enter the booking code manually.
          </p>
        </div>
      </div>

      <div class="header-actions">
        <button class="ghost-button" type="button" @click="goRecords">Open Records</button>
      </div>
    </header>

    <section class="capability-strip">
      <div class="capability-item">
        <span>Camera</span>
        <strong>{{ cameraSupported ? 'Available' : 'Unavailable' }}</strong>
      </div>
      <div class="capability-item">
        <span>Live Detect</span>
        <strong>{{ detectorSupported ? 'Supported' : 'Photo Fallback' }}</strong>
      </div>
      <div class="capability-item">
        <span>Secure Context</span>
        <strong>{{ secureContextReady ? 'Ready' : 'Limited' }}</strong>
      </div>
    </section>

    <div class="scanner-layout">
      <section class="panel camera-panel">
        <div class="panel-head">
          <div>
            <h2>Camera</h2>
            <p>{{ cameraTip }}</p>
          </div>
          <span class="panel-status">{{ scannerStatus }}</span>
        </div>

        <div class="camera-stage">
          <video
            ref="videoRef"
            class="camera-video"
            autoplay
            muted
            playsinline
            webkit-playsinline="true"
          ></video>
          <canvas ref="captureCanvasRef" class="hidden-canvas"></canvas>

          <div class="scan-mask"></div>

          <div v-if="showOverlay" class="camera-overlay">
            <i :class="overlayIcon"></i>
            <strong>{{ overlayTitle }}</strong>
            <p>{{ cameraMessage }}</p>
          </div>
        </div>

        <div class="action-grid">
          <button class="ghost-button" type="button" :disabled="cameraStarting" @click="startCamera(true)">
            {{ cameraStarting ? 'Starting...' : (cameraActive ? 'Reconnect Camera' : 'Start Camera') }}
          </button>
          <button class="ghost-button" type="button" :disabled="!cameraActive" @click="stopCamera">
            Stop Camera
          </button>
          <button class="ghost-button" type="button" :disabled="captureDisabled" @click="captureCurrentFrameLookup">
            {{ photoProcessing ? 'Scanning Photo...' : 'Capture Current Frame' }}
          </button>
          <button class="ghost-button" type="button" :disabled="photoProcessing" @click="triggerFilePicker">
            {{ photoProcessing ? 'Scanning Photo...' : 'Take Photo / Choose Image' }}
          </button>
        </div>

        <input
          ref="fileInputRef"
          class="hidden-input"
          type="file"
          accept="image/*"
          capture="environment"
          @change="handleFileSelected"
        >

        <div class="manual-panel">
          <label class="field-label" for="manual-code">Booking Code</label>
          <div class="manual-row">
            <input
              id="manual-code"
              v-model.trim="manualCode"
              class="manual-input"
              type="text"
              placeholder="Enter booking no or booking id"
              @keyup.enter="lookupByManualCode"
            >
            <button class="primary-button" type="button" :disabled="lookupLoading" @click="lookupByManualCode">
              {{ lookupLoading ? 'Looking Up...' : 'Lookup Booking' }}
            </button>
          </div>
        </div>
      </section>

      <section class="panel result-panel">
        <div class="panel-head">
          <div>
            <h2>Lookup Result</h2>
            <p>Booking detail appears here after a successful scan or lookup.</p>
          </div>
          <span v-if="lookupResult" :class="['result-pill', lookupResult.status]">
            {{ statusText(lookupResult.status) }}
          </span>
        </div>

        <div v-if="busyState" class="state-card">
          <i class="bx bx-loader-alt bx-spin"></i>
          <strong>{{ busyTitle }}</strong>
          <p>{{ busyMessage }}</p>
        </div>

        <div v-else-if="lookupError" class="state-card error">
          <i class="bx bx-error-circle"></i>
          <strong>Lookup Failed</strong>
          <p>{{ lookupError }}</p>
        </div>

        <div v-else-if="lookupResult" class="result-body">
          <div class="result-title-row">
            <div>
              <strong>{{ lookupResult.activityTitle || 'Activity Booking' }}</strong>
              <p>{{ lookupResult.reserveNo || lookupResult.id }}</p>
            </div>
            <div class="result-actions-inline">
              <button class="ghost-button" type="button" @click="refreshCurrentResult">Refresh</button>
              <button class="ghost-button" type="button" @click="goRecords">Open Records</button>
            </div>
          </div>

          <div class="result-grid">
            <div class="result-item">
              <span>Participant</span>
              <strong>{{ lookupResult.customerName || lookupResult.participantName || '-' }}</strong>
            </div>
            <div class="result-item">
              <span>Phone</span>
              <strong>{{ lookupResult.participantPhone || '-' }}</strong>
            </div>
            <div class="result-item">
              <span>Participants</span>
              <strong>{{ lookupResult.participantCount || 1 }}</strong>
            </div>
            <div class="result-item">
              <span>Payment</span>
              <strong>{{ paymentText(lookupResult.paymentStatus || lookupResult.payStatus) }}</strong>
            </div>
            <div class="result-item wide">
              <span>Activity Time</span>
              <strong>{{ lookupResult.activityTime || formatRange(lookupResult) }}</strong>
            </div>
            <div class="result-item wide">
              <span>Location</span>
              <strong>{{ formatLocation(lookupResult) }}</strong>
            </div>
          </div>

          <p v-if="lookupResult.remark" class="remark-text">Remark: {{ lookupResult.remark }}</p>

          <div class="timeline-strip">
            <div class="timeline-item">
              <span>Status</span>
              <strong>{{ statusText(lookupResult.status) }}</strong>
            </div>
            <div class="timeline-item">
              <span>Booked At</span>
              <strong>{{ formatTime(lookupResult.createTime) }}</strong>
            </div>
            <div class="timeline-item">
              <span>Checked In</span>
              <strong>{{ formatTime(lookupResult.verificationTime || lookupResult.verifyTime) }}</strong>
            </div>
          </div>

          <div class="footer-actions">
            <button
              v-if="lookupResult.canCheckin"
              class="primary-button"
              type="button"
              :disabled="checkinLoading"
              @click="confirmCheckin"
            >
              {{ checkinLoading ? 'Checking In...' : 'Confirm Check-in' }}
            </button>
            <button v-else class="ghost-button" type="button" disabled>
              {{ resultActionHint }}
            </button>
          </div>
        </div>

        <div v-else class="state-card empty">
          <i class="bx bx-qr-scan"></i>
          <strong>No Booking Selected</strong>
          <p>Start the camera, upload a QR image, or enter a booking code to load booking detail.</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  checkinBooking,
  getMerchantBookings,
  lookupMerchantBooking,
  lookupMerchantBookingByImage
} from '@/api/app.js'
import { getRequestErrorMessage } from '@/utils/requestError.js'

const SCAN_INTERVAL_MS = 700
const SCAN_DEBOUNCE_MS = 1600
const MAX_IMAGE_EDGE = 1440
const JPEG_QUALITY = 0.86

const { appContext } = getCurrentInstance()
const notify = (message, type = 'info') => {
  appContext.config.globalProperties.$notify?.[type]?.(message)
}

const route = useRoute()
const router = useRouter()

const videoRef = ref(null)
const captureCanvasRef = ref(null)
const fileInputRef = ref(null)

const manualCode = ref('')
const lookupResult = ref(null)
const lookupError = ref('')
const lookupLoading = ref(false)
const photoProcessing = ref(false)
const checkinLoading = ref(false)
const cameraStarting = ref(false)
const cameraMessage = ref('Camera is idle. The page will try to start it automatically.')
const activeStream = ref(null)

const detectorSupported = typeof window !== 'undefined' && 'BarcodeDetector' in window
const cameraSupported = typeof navigator !== 'undefined' && Boolean(navigator.mediaDevices?.getUserMedia)
const secureContextReady = typeof window === 'undefined' || window.isSecureContext || isLocalhost()

let detector = null
let scanTimer = null
let lastDetectedCode = ''
let lastDetectedAt = 0

const cameraActive = computed(() => Boolean(activeStream.value))
const showOverlay = computed(() => !cameraActive.value || Boolean(cameraMessage.value))
const captureDisabled = computed(() => photoProcessing.value || (!cameraActive.value && !cameraSupported))
const busyState = computed(() => lookupLoading.value || photoProcessing.value || checkinLoading.value)

const scannerStatus = computed(() => {
  if (cameraStarting.value) return 'Starting'
  if (photoProcessing.value) return 'Photo Decode'
  if (cameraActive.value && detectorSupported) return 'Live Detecting'
  if (cameraActive.value && !detectorSupported) return 'Preview Only'
  if (cameraSupported) return 'Idle'
  return 'Unavailable'
})

const overlayIcon = computed(() => {
  if (cameraStarting.value) return 'bx bx-loader-alt bx-spin'
  if (cameraActive.value) return 'bx bx-camera'
  return 'bx bx-camera-off'
})

const overlayTitle = computed(() => {
  if (cameraStarting.value) return 'Opening Camera'
  if (cameraActive.value) return detectorSupported ? 'Camera Ready' : 'Preview Mode'
  return cameraSupported ? 'Camera Not Running' : 'Camera Unsupported'
})

const cameraTip = computed(() => {
  if (!cameraSupported) {
    return 'This browser cannot access the camera. Use image upload or manual booking lookup instead.'
  }
  if (!secureContextReady) {
    return 'Camera access is usually blocked outside HTTPS or localhost. Uploading a QR image will still work.'
  }
  if (cameraActive.value && detectorSupported) {
    return 'QR codes will be detected automatically while the camera preview is active.'
  }
  if (cameraActive.value && !detectorSupported) {
    return 'Live QR detection is unavailable here. Capture a frame or choose an image for server-side decoding.'
  }
  return 'If the camera cannot start, check permissions, secure context, and whether another app is using it.'
})

const busyTitle = computed(() => {
  if (checkinLoading.value) return 'Checking In Booking'
  if (photoProcessing.value) return 'Decoding QR Image'
  return 'Loading Booking Detail'
})

const busyMessage = computed(() => {
  if (checkinLoading.value) return 'The booking is being checked in. Please keep this page open.'
  if (photoProcessing.value) return 'The image is being prepared, uploaded, and decoded.'
  return 'Booking information is being loaded.'
})

const resultActionHint = computed(() => {
  const status = lookupResult.value?.status
  if (status === 'checked_in') return 'Already Checked In'
  if (status === 'cancelled') return 'Booking Cancelled'
  if (status === 'rejected') return 'Booking Rejected'
  return 'Check-in Unavailable'
})

function normalizeCode(value) {
  return String(value || '').trim()
}

function isLocalhost() {
  if (typeof window === 'undefined') {
    return true
  }
  const host = window.location.hostname
  return host === 'localhost' || host === '127.0.0.1' || host === '::1'
}

function goBack() {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }
  router.push('/merchant/bookings')
}

function goRecords() {
  router.push({
    name: 'merchant-bookings-records',
    query: { backTo: route.fullPath }
  })
}function stopDetectionLoop() {
  if (scanTimer) {
    window.clearInterval(scanTimer)
    scanTimer = null
  }
  detector = null
  lastDetectedCode = ''
  lastDetectedAt = 0
}

function stopCamera() {
  stopDetectionLoop()

  if (activeStream.value) {
    activeStream.value.getTracks().forEach(track => track.stop())
    activeStream.value = null
  }

  if (videoRef.value) {
    videoRef.value.pause?.()
    videoRef.value.srcObject = null
  }

  if (cameraSupported) {
    cameraMessage.value = 'Camera stopped. You can start it again anytime.'
  }
}

async function attachStream(stream) {
  if (!videoRef.value) return

  videoRef.value.srcObject = stream

  await new Promise(resolve => {
    const element = videoRef.value
    const onReady = () => {
      element.removeEventListener('loadedmetadata', onReady)
      resolve()
    }

    if (element.readyState >= 1) {
      resolve()
      return
    }

    element.addEventListener('loadedmetadata', onReady)
  })

  await videoRef.value.play().catch(() => {})
}

function buildConstraintAttempts() {
  return [
    { video: { facingMode: { exact: 'environment' } }, audio: false },
    { video: { facingMode: { ideal: 'environment' } }, audio: false },
    { video: { facingMode: { ideal: 'user' } }, audio: false },
    { video: true, audio: false }
  ]
}

function describeCameraError(error) {
  if (!secureContextReady) {
    return 'Camera access usually requires HTTPS or localhost. Switch to a secure origin or use image upload.'
  }

  if (['NotAllowedError', 'SecurityError', 'PermissionDeniedError'].includes(error?.name)) {
    return 'Camera permission was denied. Allow access in the browser and try again.'
  }

  if (['NotFoundError', 'DevicesNotFoundError'].includes(error?.name)) {
    return 'No available camera device was found.'
  }

  if (['NotReadableError', 'TrackStartError'].includes(error?.name)) {
    return 'The camera is already being used by another app.'
  }

  if (['OverconstrainedError', 'ConstraintNotSatisfiedError'].includes(error?.name)) {
    return 'The preferred camera could not be opened. Try reconnecting the camera.'
  }

  return 'Camera start failed. Please check browser permission and device availability.'
}

function canRunLiveDetect() {
  return detectorSupported && typeof window.BarcodeDetector === 'function'
}

function startDetectionLoop() {
  if (!canRunLiveDetect() || !videoRef.value) {
    return
  }

  try {
    detector = new window.BarcodeDetector({ formats: ['qr_code'] })
  } catch (error) {
    detector = null
    cameraMessage.value = 'Live QR detection is unavailable. Use photo capture or image upload instead.'
    return
  }

  scanTimer = window.setInterval(scanFrame, SCAN_INTERVAL_MS)
}

async function startCamera(manual = false) {
  if (!cameraSupported || cameraStarting.value) {
    return
  }

  cameraStarting.value = true
  cameraMessage.value = manual ? 'Reconnecting camera...' : 'Starting camera...'

  try {
    stopCamera()

    let stream = null
    let lastError = null

    for (const constraints of buildConstraintAttempts()) {
      try {
        stream = await navigator.mediaDevices.getUserMedia(constraints)
        break
      } catch (error) {
        lastError = error
      }
    }

    if (!stream) {
      throw lastError || new Error('Camera start failed')
    }

    activeStream.value = stream
    await attachStream(stream)

    if (canRunLiveDetect()) {
      cameraMessage.value = ''
      startDetectionLoop()
    } else {
      cameraMessage.value = 'Camera preview is active. Use frame capture or image upload for QR decoding.'
    }
  } catch (error) {
    stopCamera()
    cameraMessage.value = describeCameraError(error)
  } finally {
    cameraStarting.value = false
  }
}

async function scanFrame() {
  if (!detector || !videoRef.value || !cameraActive.value || lookupLoading.value || photoProcessing.value || checkinLoading.value) {
    return
  }

  const now = Date.now()
  if (now - lastDetectedAt < SCAN_DEBOUNCE_MS) {
    return
  }

  lastDetectedAt = now

  try {
    const barcodes = await detector.detect(videoRef.value)
    const code = normalizeCode(barcodes?.[0]?.rawValue)

    if (!code || code === lastDetectedCode) {
      return
    }

    lastDetectedCode = code
    manualCode.value = code
    await lookupBookingByCode(code, { announceSuccess: true })
  } catch (error) {
    // Keep the scan loop running even if one frame fails.
  }
}

function triggerFilePicker() {
  fileInputRef.value?.click()
}

function getCanvas() {
  return captureCanvasRef.value || document.createElement('canvas')
}

function buildScaledSize(width, height) {
  if (!width || !height) {
    return { width: 0, height: 0 }
  }

  const longestEdge = Math.max(width, height)
  if (longestEdge <= MAX_IMAGE_EDGE) {
    return { width, height }
  }

  const ratio = MAX_IMAGE_EDGE / longestEdge
  return {
    width: Math.max(1, Math.round(width * ratio)),
    height: Math.max(1, Math.round(height * ratio))
  }
}

function renderCompressedImage(source, width, height) {
  const { width: nextWidth, height: nextHeight } = buildScaledSize(width, height)
  if (!nextWidth || !nextHeight) {
    return ''
  }

  const canvas = getCanvas()
  canvas.width = nextWidth
  canvas.height = nextHeight

  const context = canvas.getContext('2d', { alpha: false })
  if (!context) {
    return ''
  }

  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, nextWidth, nextHeight)
  context.drawImage(source, 0, 0, nextWidth, nextHeight)

  return canvas.toDataURL('image/jpeg', JPEG_QUALITY)
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('Failed to read image file'))
    reader.readAsDataURL(file)
  })
}

function loadImageElement(source) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('Failed to load image'))
    image.src = source
  })
}async function normalizeImageForLookup(imageData) {
  const image = await loadImageElement(imageData)
  const normalized = renderCompressedImage(
    image,
    image.naturalWidth || image.width,
    image.naturalHeight || image.height
  )

  if (!normalized) {
    throw new Error('Failed to prepare image for QR lookup')
  }

  return normalized
}

function captureCurrentFrame() {
  if (!videoRef.value || !cameraActive.value) {
    return ''
  }

  const width = videoRef.value.videoWidth
  const height = videoRef.value.videoHeight
  if (!width || !height) {
    return ''
  }

  return renderCompressedImage(videoRef.value, width, height)
}

function applyLookupResult(result, code = '') {
  lookupResult.value = result
  lookupError.value = ''
  if (code) {
    manualCode.value = code
  }
}

async function lookupBookingByCode(code, options = {}) {
  const normalizedCode = normalizeCode(code)
  if (!normalizedCode) {
    throw new Error('Please enter a booking code first.')
  }

  const announceSuccess = options.announceSuccess === true
  const silent = options.silent === true

  lookupLoading.value = true
  lookupError.value = ''

  try {
    const response = await lookupMerchantBooking(normalizedCode)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || 'Booking not found')
    }

    applyLookupResult(response.data, normalizedCode)
    if (announceSuccess && !silent) {
      notify('Booking QR detected successfully.', 'success')
    }
    return response.data
  } catch (primaryError) {
    try {
      const fallback = await getMerchantBookings({ keyword: normalizedCode })
      const items = Array.isArray(fallback.data?.items) ? fallback.data.items : []
      if (items.length === 1) {
        applyLookupResult(items[0], normalizedCode)
        if (announceSuccess && !silent) {
          notify('Booking matched successfully.', 'success')
        }
        return items[0]
      }
    } catch (fallbackError) {
      // Ignore fallback network errors and surface the primary message.
    }

    const message = getRequestErrorMessage(primaryError, {
      fallbackMessage: 'Booking not found'
    })
    lookupResult.value = null
    lookupError.value = message
    throw new Error(message)
  } finally {
    lookupLoading.value = false
  }
}

async function lookupBookingByImage(imageData, successMessage = 'QR image decoded successfully.') {
  if (!imageData) {
    throw new Error('Image data is empty')
  }

  photoProcessing.value = true
  lookupError.value = ''

  try {
    const response = await lookupMerchantBookingByImage(imageData)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || 'Unable to decode booking QR from image')
    }

    applyLookupResult(response.data, response.data?.reserveNo || response.data?.id || '')
    notify(successMessage, 'success')
    return response.data
  } catch (error) {
    lookupResult.value = null

    const genericMessage = getRequestErrorMessage(error, {
      timeoutMessage: 'The image is taking too long to upload. Please try again with a smaller QR image.',
      fallbackMessage: 'Failed to decode QR image'
    })

    lookupError.value = error?.response?.status === 500
      ? 'The image was too large or the server could not process it. Move closer to the QR code and try again.'
      : genericMessage

    throw error
  } finally {
    photoProcessing.value = false
  }
}

async function captureCurrentFrameLookup() {
  if (photoProcessing.value) {
    return
  }

  const imageData = captureCurrentFrame()
  if (!imageData) {
    lookupError.value = 'The current camera frame is not ready yet. Wait a moment and try again.'
    return
  }

  await lookupBookingByImage(imageData, 'Captured frame decoded successfully.')
}

async function handleFileSelected(event) {
  const file = event.target?.files?.[0]
  if (!file) {
    return
  }

  try {
    const imageData = await readFileAsDataUrl(file)
    const normalized = await normalizeImageForLookup(imageData)
    await lookupBookingByImage(normalized, 'Selected image decoded successfully.')
  } catch (error) {
    // Error state is already handled in lookupBookingByImage.
  } finally {
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
  }
}

async function lookupByManualCode() {
  const code = normalizeCode(manualCode.value)
  if (!code) {
    lookupResult.value = null
    lookupError.value = 'Please enter a booking code first.'
    return
  }

  try {
    await lookupBookingByCode(code)
  } catch (error) {
    // Error state is already handled in lookupBookingByCode.
  }
}

async function refreshCurrentResult() {
  const currentCode = normalizeCode(lookupResult.value?.reserveNo || lookupResult.value?.id || manualCode.value)
  if (!currentCode) {
    return
  }

  try {
    await lookupBookingByCode(currentCode, { silent: true })
  } catch (error) {
    // Error state is already handled in lookupBookingByCode.
  }
}

async function confirmCheckin() {
  if (!lookupResult.value?.id || checkinLoading.value) {
    return
  }

  checkinLoading.value = true

  try {
    const response = await checkinBooking(lookupResult.value.id)
    if (response.code !== 200) {
      throw new Error(response.message || 'Check-in failed')
    }

    notify('Booking checked in successfully.', 'success')
    applyLookupResult(response.data || lookupResult.value, lookupResult.value.reserveNo || lookupResult.value.id)
    await refreshCurrentResult()
  } catch (error) {
    notify(getRequestErrorMessage(error, { fallbackMessage: 'Check-in failed' }), 'error')
  } finally {
    checkinLoading.value = false
  }
}

function statusText(status) {
  return {
    registered: 'Pending Check-in',
    checked_in: 'Checked In',
    rejected: 'Rejected',
    cancelled: 'Cancelled'
  }[status] || String(status || '-').replace(/_/g, ' ')
}

function paymentText(status) {
  const normalized = String(status ?? '').toLowerCase()
  if (normalized === 'paid' || normalized === '1') return 'Paid'
  if (normalized === 'refunded' || normalized === '2') return 'Refunded'
  return 'Unpaid'
}

function formatTime(value) {
  if (!value) return 'Not Recorded'
  return new Date(value).toLocaleString()
}function formatRange(booking) {
  if (!booking?.startTime && !booking?.endTime) {
    return 'Time TBD'
  }

  const start = booking?.startTime ? formatTime(booking.startTime) : 'Time TBD'
  const end = booking?.endTime ? formatTime(booking.endTime) : ''
  return end ? `${start} - ${end}` : start
}

function formatLocation(booking) {
  return [
    booking?.locationProvince,
    booking?.locationCity,
    booking?.locationDistrict,
    booking?.locationDetail
  ].filter(Boolean).join(' / ') || 'Location TBD'
}

watch(
  () => route.query.code,
  async value => {
    if (typeof value !== 'string' || !value.trim()) {
      return
    }

    manualCode.value = value.trim()
    try {
      await lookupBookingByCode(value.trim(), { silent: true })
    } catch (error) {
      // Error state is already handled in lookupBookingByCode.
    }
  },
  { immediate: true }
)

onMounted(async () => {
  await startCamera()
})

onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped>
.scanner-page {
  max-width: 1220px;
  margin: 0 auto;
  padding: 8px 0 28px;
}

.scanner-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.header-main {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.icon-button,
.primary-button,
.ghost-button {
  border: none;
  cursor: pointer;
}

.icon-button {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #ffffff;
  color: #1f2937;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  font-size: 22px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #9f1239;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
  font-weight: 700;
}

.scanner-header h1,
.panel-head h2 {
  margin: 0;
  color: #0f172a;
}

.header-copy,
.panel-head p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.capability-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.capability-item,
.panel,
.state-card {
  border-radius: 24px;
  border: 1px solid #e5e7eb;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
}

.capability-item {
  padding: 14px 16px;
}

.capability-item span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.capability-item strong {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 16px;
}

.scanner-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(0, 0.98fr);
  gap: 18px;
}

.panel {
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-status {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.camera-stage {
  position: relative;
  margin-top: 16px;
  min-height: 360px;
  overflow: hidden;
  border-radius: 22px;
  background: linear-gradient(135deg, #111827, #1f2937);
}

.camera-video {
  display: block;
  width: 100%;
  height: 360px;
  object-fit: cover;
}

.hidden-canvas,
.hidden-input {
  display: none;
}

.scan-mask {
  position: absolute;
  inset: 50% auto auto 50%;
  width: 220px;
  height: 220px;
  transform: translate(-50%, -50%);
  border: 2px solid rgba(255, 255, 255, 0.92);
  border-radius: 22px;
  box-shadow: 0 0 0 9999px rgba(15, 23, 42, 0.2);
  pointer-events: none;
  z-index: 2;
}

.camera-overlay {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.96);
  text-align: center;
  background: radial-gradient(circle at center, rgba(30, 41, 59, 0.28), rgba(15, 23, 42, 0.8));
  padding: 20px;
}

.camera-overlay i {
  font-size: 42px;
}

.camera-overlay strong {
  font-size: 18px;
}

.camera-overlay p {
  margin: 0;
  max-width: 460px;
  line-height: 1.7;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.manual-panel {
  margin-top: 16px;
}.field-label {
  display: inline-block;
  margin-bottom: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.manual-row,
.footer-actions,
.result-actions-inline,
.timeline-strip {
  display: flex;
  gap: 12px;
}

.manual-input {
  flex: 1;
  min-height: 46px;
  padding: 0 14px;
  border: 1px solid #d4d9e0;
  border-radius: 14px;
  background: #ffffff;
  color: #0f172a;
}

.primary-button,
.ghost-button {
  min-height: 46px;
  padding: 0 18px;
  border-radius: 14px;
  font-weight: 700;
}

.primary-button {
  background: linear-gradient(135deg, #be123c, #e11d48);
  color: #ffffff;
}

.ghost-button {
  background: #f8fafc;
  color: #334155;
}

.result-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.result-pill.registered {
  background: rgba(225, 29, 72, 0.12);
  color: #be123c;
}

.result-pill.checked_in {
  background: rgba(22, 97, 171, 0.12);
  color: #1661ab;
}

.result-pill.rejected {
  background: #fef3c7;
  color: #b45309;
}

.result-pill.cancelled {
  background: #e5e7eb;
  color: #6b7280;
}

.state-card {
  margin-top: 16px;
  padding: 56px 18px;
  text-align: center;
  color: #94a3b8;
}

.state-card i {
  font-size: 42px;
}

.state-card strong {
  display: block;
  margin-top: 14px;
  color: #0f172a;
}

.state-card p {
  margin: 10px 0 0;
  line-height: 1.7;
}

.state-card.error {
  color: #b42318;
}

.result-body {
  margin-top: 16px;
}

.result-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.result-title-row strong {
  display: block;
  color: #111827;
  font-size: 22px;
}

.result-title-row p {
  margin: 6px 0 0;
  color: #64748b;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.result-item {
  padding: 14px;
  border-radius: 18px;
  background: #fafafa;
  border: 1px solid #eef2f7;
}

.result-item.wide {
  grid-column: span 2;
}

.result-item span,
.timeline-item span {
  display: block;
  color: #7c8694;
  font-size: 13px;
}

.result-item strong,
.timeline-item strong {
  display: block;
  margin-top: 8px;
  color: #1f2937;
  line-height: 1.7;
}

.remark-text {
  margin: 16px 0 0;
  color: #475569;
  line-height: 1.7;
}

.timeline-strip {
  margin-top: 18px;
  padding: 14px;
  border-radius: 18px;
  background: #f8fafc;
  justify-content: space-between;
  flex-wrap: wrap;
}

.timeline-item {
  min-width: 160px;
}

.footer-actions {
  margin-top: 18px;
  justify-content: flex-start;
  flex-wrap: wrap;
}

@media (max-width: 980px) {
  .scanner-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .scanner-header,
  .header-main,
  .panel-head,
  .manual-row,
  .result-title-row,
  .result-actions-inline,
  .footer-actions {
    flex-direction: column;
  }

  .capability-strip,
  .action-grid,
  .result-grid {
    grid-template-columns: 1fr;
  }

  .result-item.wide {
    grid-column: span 1;
  }

  .primary-button,
  .ghost-button,
  .manual-input {
    width: 100%;
  }

  .camera-video,
  .camera-stage {
    min-height: 300px;
    height: 300px;
  }

  .scan-mask {
    width: 180px;
    height: 180px;
  }
}
</style>