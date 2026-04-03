<template>
  <div class="scanner-page">
    <header class="scanner-header">
      <div class="header-main">
        <button class="icon-button" type="button" @click="goBack">
          <i class="bx bx-arrow-back"></i>
        </button>
        <div>
          <p class="eyebrow">商家签到</p>
          <h1>预订扫描器</h1>
          <p class="header-copy">
            可用时使用实时相机检测。如果浏览器无法实时检测二维码，
            可以捕获帧、上传照片或手动输入预订码。
          </p>
        </div>
      </div>

      <div class="header-actions">
        <button class="ghost-button" type="button" @click="goRecords">查看记录</button>
      </div>
    </header>

    <section class="capability-strip">
      <div class="capability-item">
        <span>相机</span>
        <strong>{{ cameraSupported ? '可用' : '不可用' }}</strong>
      </div>
      <div class="capability-item">
        <span>实时检测</span>
        <strong>支持</strong>
      </div>
      <div class="capability-item">
        <span>安全环境</span>
        <strong>{{ secureContextReady ? '就绪' : '受限' }}</strong>
      </div>
    </section>

    <div class="scanner-layout">
      <section class="panel camera-panel">
        <div class="panel-head">
          <div>
            <h2>相机</h2>
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
            {{ cameraStarting ? '启动中...' : (cameraActive ? '重新连接相机' : '启动相机') }}
          </button>
          <button class="ghost-button" type="button" :disabled="!cameraActive" @click="stopCamera">
            停止相机
          </button>
          <button class="ghost-button" type="button" :disabled="captureDisabled" @click="captureCurrentFrameLookup">
            {{ photoProcessing ? '扫描照片中...' : '捕获当前帧' }}
          </button>
          <button class="ghost-button" type="button" :disabled="photoProcessing" @click="triggerFilePicker">
            {{ photoProcessing ? '扫描照片中...' : '拍照 / 选择图片' }}
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
          <label class="field-label" for="manual-code">预订码</label>
          <div class="manual-row">
            <input
              id="manual-code"
              v-model.trim="manualCode"
              class="manual-input"
              type="text"
              placeholder="输入预订号或预订ID"
              @keyup.enter="lookupByManualCode"
            >
            <button class="primary-button" type="button" :disabled="lookupLoading" @click="lookupByManualCode">
              {{ lookupLoading ? '查找中...' : '查找预订' }}
            </button>
          </div>
        </div>
      </section>

      <section class="panel result-panel">
        <div class="panel-head">
          <div>
            <h2>查找结果</h2>
            <p>成功扫描或查找后，预订详情会显示在这里。</p>
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
          <strong>查找失败</strong>
          <p>{{ lookupError }}</p>
        </div>

        <div v-else-if="lookupResult" class="result-body">
          <div class="result-title-row">
            <div>
              <strong>{{ lookupResult.activityTitle || '活动预订' }}</strong>
              <p>{{ lookupResult.reserveNo || lookupResult.id }}</p>
            </div>
            <div class="result-actions-inline">
              <button class="ghost-button" type="button" @click="refreshCurrentResult">刷新</button>
              <button class="ghost-button" type="button" @click="goRecords">查看记录</button>
            </div>
          </div>

          <div class="result-grid">
            <div class="result-item">
              <span>参与者</span>
              <strong>{{ lookupResult.customerName || lookupResult.participantName || '-' }}</strong>
            </div>
            <div class="result-item">
              <span>电话</span>
              <strong>{{ lookupResult.participantPhone || '-' }}</strong>
            </div>
            <div class="result-item">
              <span>参与人数</span>
              <strong>{{ lookupResult.participantCount || 1 }}</strong>
            </div>
            <div class="result-item">
              <span>支付状态</span>
              <strong>{{ paymentText(lookupResult.paymentStatus || lookupResult.payStatus) }}</strong>
            </div>
            <div class="result-item wide">
              <span>活动时间</span>
              <strong>{{ lookupResult.activityTime || formatRange(lookupResult) }}</strong>
            </div>
            <div class="result-item wide">
              <span>地点</span>
              <strong>{{ formatLocation(lookupResult) }}</strong>
            </div>
          </div>

          <p v-if="lookupResult.remark" class="remark-text">备注：{{ lookupResult.remark }}</p>

          <div class="timeline-strip">
            <div class="timeline-item">
              <span>状态</span>
              <strong>{{ statusText(lookupResult.status) }}</strong>
            </div>
            <div class="timeline-item">
              <span>预订时间</span>
              <strong>{{ formatTime(lookupResult.createTime) }}</strong>
            </div>
            <div class="timeline-item">
              <span>签到时间</span>
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
              {{ checkinLoading ? '签到中...' : '确认签到' }}
            </button>
            <button v-else class="ghost-button" type="button" disabled>
              {{ resultActionHint }}
            </button>
          </div>
        </div>

        <div v-else class="state-card empty">
          <i class="bx bx-qr-scan"></i>
          <strong>未选择预订</strong>
          <p>启动相机、上传二维码图片或输入预订码来加载预订详情。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import jsQR from 'jsqr'
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
const cameraMessage = ref('相机空闲。页面将尝试自动启动它。')
const activeStream = ref(null)

const cameraSupported = typeof navigator !== 'undefined' && Boolean(navigator.mediaDevices?.getUserMedia)
const secureContextReady = typeof window === 'undefined' || window.isSecureContext || isLocalhost()

let scanTimer = null
let lastDetectedCode = ''
let lastDetectedAt = 0
let scanCanvas = null
let scanContext = null

const cameraActive = computed(() => Boolean(activeStream.value))
const showOverlay = computed(() => !cameraActive.value || Boolean(cameraMessage.value))
const captureDisabled = computed(() => photoProcessing.value || (!cameraActive.value && !cameraSupported))
const busyState = computed(() => lookupLoading.value || photoProcessing.value || checkinLoading.value)

const scannerStatus = computed(() => {
  if (cameraStarting.value) return '启动中'
  if (photoProcessing.value) return '照片解码中'
  if (cameraActive.value && scanTimer) return '实时检测中'
  if (cameraActive.value) return '仅预览'
  if (cameraSupported) return '空闲'
  return '不可用'
})

const overlayIcon = computed(() => {
  if (cameraStarting.value) return 'bx bx-loader-alt bx-spin'
  if (cameraActive.value) return 'bx bx-camera'
  return 'bx bx-camera-off'
})

const overlayTitle = computed(() => {
  if (cameraStarting.value) return '正在打开相机'
  if (cameraActive.value) return '相机已就绪'
  return cameraSupported ? '相机未运行' : '相机不支持'
})

const cameraTip = computed(() => {
  if (!cameraSupported) {
    return '此浏览器无法访问相机。请使用图片上传或手动预订查找。'
  }
  if (!secureContextReady) {
    return '相机访问通常在HTTPS或本地主机之外被阻止。上传二维码图片仍然可以工作。'
  }
  if (cameraActive.value) {
    return '相机预览活跃时，二维码将被自动检测。'
  }
  return '如果相机无法启动，请检查权限、安全上下文以及是否有其他应用正在使用它。'
})

const busyTitle = computed(() => {
  if (checkinLoading.value) return '正在签到预订'
  if (photoProcessing.value) return '正在解码二维码图片'
  return '正在加载预订详情'
})

const busyMessage = computed(() => {
  if (checkinLoading.value) return '预订正在签到中。请保持此页面打开。'
  if (photoProcessing.value) return '图片正在准备、上传和解码中。'
  return '预订信息正在加载中。'
})

const resultActionHint = computed(() => {
  const status = lookupResult.value?.status
  if (status === 'checked_in') return '已签到'
  if (status === 'cancelled') return '预订已取消'
  if (status === 'rejected') return '预订已拒绝'
  return '签到不可用'
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
}

function stopDetectionLoop() {
  if (scanTimer) {
    window.clearInterval(scanTimer)
    scanTimer = null
  }
  lastDetectedCode = ''
  lastDetectedAt = 0
  scanCanvas = null
  scanContext = null
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
    cameraMessage.value = '相机已停止。您可以随时重新启动。'
  }
}

async function attachStream(stream) {
  if (!videoRef.value) return

  videoRef.value.srcObject = stream

  await new Promise(resolve => {
    const element = videoRef.value
    const onReady = () => {
      element.removeEventListener('loadeddata', onReady)
      element.removeEventListener('canplay', onReady)
      resolve()
    }

    if (element.readyState >= 2) {
      resolve()
      return
    }

    element.addEventListener('loadeddata', onReady)
    element.addEventListener('canplay', onReady)
  })

  await videoRef.value.play().catch(() => {})
  
  // 额外等待一小段时间确保视频帧可用
  await new Promise(resolve => setTimeout(resolve, 100))
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
    return '相机访问通常需要HTTPS或本地主机。切换到安全来源或使用图片上传。'
  }

  if (['NotAllowedError', 'SecurityError', 'PermissionDeniedError'].includes(error?.name)) {
    return '相机权限被拒绝。请在浏览器中允许访问并重试。'
  }

  if (['NotFoundError', 'DevicesNotFoundError'].includes(error?.name)) {
    return '未找到可用的相机设备。'
  }

  if (['NotReadableError', 'TrackStartError'].includes(error?.name)) {
    return '相机已被其他应用程序使用。'
  }

  if (['OverconstrainedError', 'ConstraintNotSatisfiedError'].includes(error?.name)) {
    return '无法打开首选相机。请尝试重新连接相机。'
  }

  return '相机启动失败。请检查浏览器权限和设备可用性。'
}

async function startCamera(manual = false) {
  if (!cameraSupported || cameraStarting.value) {
    return
  }

  cameraStarting.value = true
  cameraMessage.value = manual ? '正在重新连接相机...' : '正在启动相机...'

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
      throw lastError || new Error('相机启动失败')
    }

    activeStream.value = stream
    console.log('相机流已获取，正在附加到视频元素...')
    await attachStream(stream)
    console.log('视频元素已就绪，videoWidth:', videoRef.value?.videoWidth, 'videoHeight:', videoRef.value?.videoHeight)

    // 启动实时检测（使用jsQR）
    if (scanTimer) {
      clearInterval(scanTimer)
    }
    scanTimer = window.setInterval(scanFrame, SCAN_INTERVAL_MS)
    console.log('实时检测已启动（使用jsQR）')
    cameraMessage.value = ''
  } catch (error) {
    stopCamera()
    cameraMessage.value = describeCameraError(error)
    console.error('启动相机失败:', error)
  } finally {
    cameraStarting.value = false
  }
}

async function scanFrame() {
  if (!videoRef.value) {
    return
  }
  
  if (!cameraActive.value) {
    return
  }
  
  if (lookupLoading.value || photoProcessing.value || checkinLoading.value) {
    return
  }

  const now = Date.now()
  if (now - lastDetectedAt < SCAN_DEBOUNCE_MS) {
    return
  }

  lastDetectedAt = now

  try {
    const videoWidth = videoRef.value.videoWidth || 0
    const videoHeight = videoRef.value.videoHeight || 0
    
    if (videoWidth === 0 || videoHeight === 0) {
      return
    }

    // 初始化canvas（只初始化一次）
    if (!scanCanvas) {
      scanCanvas = document.createElement('canvas')
      scanContext = scanCanvas.getContext('2d', { willReadFrequently: true })
    }

    // 确保canvas尺寸与视频匹配
    if (scanCanvas.width !== videoWidth || scanCanvas.height !== videoHeight) {
      scanCanvas.width = videoWidth
      scanCanvas.height = videoHeight
    }

    // 从视频帧绘制到canvas
    scanContext.drawImage(videoRef.value, 0, 0, videoWidth, videoHeight)
    
    // 获取图像数据
    const imageData = scanContext.getImageData(0, 0, videoWidth, videoHeight)
    
    // 使用jsQR解析二维码
    const code = jsQR(imageData.data, imageData.width, imageData.height, {
      inversionAttempts: 'dontInvert'
    })

    if (!code || !code.data) {
      return
    }

    const qrCode = normalizeCode(code.data)

    if (!qrCode) {
      return
    }
    
    if (qrCode === lastDetectedCode) {
      return
    }

    console.log('检测到二维码:', qrCode)
    lastDetectedCode = qrCode
    manualCode.value = qrCode
    await lookupBookingByCode(qrCode, { announceSuccess: true })
  } catch (error) {
    console.error('扫描帧失败:', error)
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
    reader.onerror = () => reject(new Error('读取图片文件失败'))
    reader.readAsDataURL(file)
  })
}

function loadImageElement(source) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('加载图片失败'))
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
    throw new Error('准备二维码查找图片失败')
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
    throw new Error('请先输入预订码。')
  }

  const announceSuccess = options.announceSuccess === true
  const silent = options.silent === true

  lookupLoading.value = true
  lookupError.value = ''

  try {
    const response = await lookupMerchantBooking(normalizedCode)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '未找到预订')
    }

    applyLookupResult(response.data, normalizedCode)
    if (announceSuccess && !silent) {
      notify('预订二维码检测成功。', 'success')
    }
    return response.data
  } catch (primaryError) {
    try {
      const fallback = await getMerchantBookings({ keyword: normalizedCode })
      const items = Array.isArray(fallback.data?.items) ? fallback.data.items : []
      if (items.length === 1) {
        applyLookupResult(items[0], normalizedCode)
        if (announceSuccess && !silent) {
          notify('预订匹配成功。', 'success')
        }
        return items[0]
      }
    } catch (fallbackError) {
      // Ignore fallback network errors and surface the primary message.
    }

    const message = getRequestErrorMessage(primaryError, {
      fallbackMessage: '未找到预订'
    })
    lookupResult.value = null
    lookupError.value = message
    throw new Error(message)
  } finally {
    lookupLoading.value = false
  }
}

async function lookupBookingByImage(imageData, successMessage = '二维码图片解码成功。') {
  if (!imageData) {
    throw new Error('图片数据为空')
  }

  photoProcessing.value = true
  lookupError.value = ''

  try {
    const response = await lookupMerchantBookingByImage(imageData)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '无法从图片解码预订二维码')
    }

    applyLookupResult(response.data, response.data?.reserveNo || response.data?.id || '')
    notify(successMessage, 'success')
    return response.data
  } catch (error) {
    lookupResult.value = null

    const genericMessage = getRequestErrorMessage(error, {
      timeoutMessage: '图片上传时间过长。请尝试使用更小的二维码图片。',
      fallbackMessage: '解码二维码图片失败'
    })

    lookupError.value = error?.response?.status === 500
      ? '图片太大或服务器无法处理它。请靠近二维码并重试。'
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
    lookupError.value = '当前相机帧尚未准备好。请稍等片刻并重试。'
    return
  }

  await lookupBookingByImage(imageData, '捕获帧解码成功。')
}

async function handleFileSelected(event) {
  const file = event.target?.files?.[0]
  if (!file) {
    return
  }

  try {
    const imageData = await readFileAsDataUrl(file)
    const normalized = await normalizeImageForLookup(imageData)
    await lookupBookingByImage(normalized, '选择的图片解码成功。')
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
    lookupError.value = '请先输入预订码。'
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
      throw new Error(response.message || '签到失败')
    }

    notify('预订签到成功。', 'success')
    applyLookupResult(response.data || lookupResult.value, lookupResult.value.reserveNo || lookupResult.value.id)
    await refreshCurrentResult()
  } catch (error) {
    notify(getRequestErrorMessage(error, { fallbackMessage: '签到失败' }), 'error')
  } finally {
    checkinLoading.value = false
  }
}

function statusText(status) {
  return {
    registered: '待签到',
    checked_in: '已签到',
    rejected: '已拒绝',
    cancelled: '已取消'
  }[status] || String(status || '-').replace(/_/g, ' ')
}

function paymentText(status) {
  const normalized = String(status ?? '').toLowerCase()
  if (normalized === 'paid' || normalized === '1') return '已支付'
  if (normalized === 'refunded' || normalized === '2') return '已退款'
  return '未支付'
}

function formatTime(value) {
  if (!value) return '未记录'
  return new Date(value).toLocaleString()
}function formatRange(booking) {
  if (!booking?.startTime && !booking?.endTime) {
    return '时间待定'
  }

  const start = booking?.startTime ? formatTime(booking.startTime) : '时间待定'
  const end = booking?.endTime ? formatTime(booking.endTime) : ''
  return end ? `${start} - ${end}` : start
}

function formatLocation(booking) {
  return [
    booking?.locationProvince,
    booking?.locationCity,
    booking?.locationDistrict,
    booking?.locationDetail
  ].filter(Boolean).join(' / ') || '地点待定'
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