<template>
  <div class="scan-page">
    <div class="page-header">
      <div class="page-header__main">
        <button class="back-btn" @click="goBack">
          <i class="bx bx-arrow-back"></i>
        </button>
        <div>
          <h1>报名扫码</h1>
          <p>
            获授权限后，相机预览将自动开启。如二维码自动识别不可用，
            您可以拍摄照片并让服务器解码二维码图片。
          </p>
        </div>
      </div>
      <button class="ghost-btn" @click="goRecords">查看记录</button>
    </div>

    <div class="scan-layout">
      <section class="camera-card">
        <div class="card-head">
          <h2>相机扫描</h2>
          <span>{{ scannerStatus }}</span>
        </div>

        <div class="camera-box">
          <video
            ref="videoRef"
            class="camera-video"
            autoplay
            muted
            playsinline
            webkit-playsinline="true"
          ></video>
          <canvas ref="captureCanvasRef" class="capture-canvas"></canvas>
          <div v-if="showCameraOverlay" class="camera-placeholder">
            <i class="bx bx-camera-off"></i>
            <p>{{ cameraMessage }}</p>
          </div>
          <div class="scan-frame"></div>
        </div>

        <p class="camera-tip">{{ cameraTip }}</p>

        <div class="camera-actions">
          <button class="ghost-btn" :disabled="cameraStarting" @click="startCamera(true)">
            {{ cameraStarting ? '启动中...' : (cameraActive ? '重新连接相机' : '启动相机') }}
          </button>
          <button class="ghost-btn" :disabled="!cameraActive" @click="stopCamera">停止相机</button>
        </div>

        <div class="photo-actions">
          <button class="ghost-btn" :disabled="photoProcessing || (!cameraActive && !cameraSupported)" @click="capturePhotoForLookup">
            {{ photoProcessing ? '正在扫描...' : '截取当前画面' }}
          </button>
          <button class="ghost-btn" :disabled="photoProcessing" @click="triggerFilePicker">
            {{ photoProcessing ? '正在扫描...' : '拍照/选择图片' }}
          </button>
          <input
            ref="fileInputRef"
            class="hidden-input"
            type="file"
            accept="image/*"
            capture="environment"
            @change="handleFileSelected"
          >
        </div>

        <div class="manual-row">
          <input
            v-model.trim="manualCode"
            type="text"
            class="manual-input"
            placeholder="输入报名编号或报名ID"
            @keyup.enter="lookupBooking()"
          >
          <button class="primary-btn" :disabled="lookupLoading" @click="lookupBooking()">
            {{ lookupLoading ? '查询中...' : '查询报名' }}
          </button>
        </div>
      </section>

      <section class="result-card">
        <div class="card-head">
          <h2>查询结果</h2>
          <span v-if="lookupResult">{{ statusText(lookupResult.status) }}</span>
        </div>

        <div v-if="lookupLoading || photoProcessing" class="state-card">
          <i class="bx bx-loader-alt bx-spin"></i>
          <p>{{ photoProcessing ? '正在上传并解码图片...' : '正在加载报名信息...' }}</p>
        </div>

        <div v-else-if="lookupError" class="state-card error-state">
          <i class="bx bx-error-circle"></i>
          <p>{{ lookupError }}</p>
        </div>

        <div v-else-if="lookupResult" class="result-body">
          <div class="result-top">
            <strong>{{ lookupResult.activityTitle || '活动' }}</strong>
            <em :class="['status-badge', lookupResult.status]">{{ statusText(lookupResult.status) }}</em>
          </div>

          <div class="info-grid">
            <div><span>报名编号</span><strong>{{ lookupResult.reserveNo || lookupResult.id }}</strong></div>
            <div><span>参与者</span><strong>{{ lookupResult.customerName || lookupResult.participantName || '-' }}</strong></div>
            <div><span>电话</span><strong>{{ lookupResult.participantPhone || '-' }}</strong></div>
            <div><span>人数</span><strong>{{ lookupResult.participantCount || 1 }}</strong></div>
            <div><span>活动时间</span><strong>{{ lookupResult.activityTime || formatRange(lookupResult) }}</strong></div>
            <div><span>地点</span><strong>{{ formatLocation(lookupResult) }}</strong></div>
          </div>

          <p v-if="lookupResult.remark" class="remark">备注：{{ lookupResult.remark }}</p>

          <div class="result-actions">
            <button class="ghost-btn" @click="lookupBooking(lookupResult.reserveNo || lookupResult.id)">刷新</button>
            <button class="ghost-btn" @click="goRecords">查看记录</button>
            <button v-if="lookupResult.canCheckin" class="primary-btn" :disabled="submitting" @click="confirmCheckin">
              {{ submitting ? '核销中...' : '确认核销' }}
            </button>
          </div>
        </div>

        <div v-else class="state-card">
          <i class="bx bx-qr-scan"></i>
          <p>扫描、拍摄照片或输入报名码查看报名信息。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { checkinBooking, getMerchantBookings, lookupMerchantBooking, lookupMerchantBookingByImage } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (message, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(message)
const route = useRoute()
const router = useRouter()

const videoRef = ref(null)
const captureCanvasRef = ref(null)
const fileInputRef = ref(null)
const manualCode = ref('')
const lookupResult = ref(null)
const lookupLoading = ref(false)
const lookupError = ref('')
const detectorSupported = typeof window !== 'undefined' && 'BarcodeDetector' in window
const cameraSupported = typeof navigator !== 'undefined' && Boolean(navigator.mediaDevices?.getUserMedia)
const cameraStarting = ref(false)
const cameraMessage = ref(cameraSupported ? '相机空闲。页面加载时将自动启动。' : '此浏览器不支持相机访问。')
const submitting = ref(false)
const photoProcessing = ref(false)

let mediaStream = null
let detector = null
let scanTimer = null
let lastCode = ''
let lastScanAt = 0

const cameraActive = computed(() => Boolean(mediaStream))
const showCameraOverlay = computed(() => !cameraActive.value || Boolean(cameraMessage.value))
const scannerStatus = computed(() => {
  if (cameraStarting.value) return '启动中'
  if (photoProcessing.value) return '图片解码'
  if (cameraActive.value && detectorSupported) return '实时检测'
  if (cameraActive.value && !detectorSupported) return '仅预览'
  return cameraSupported ? '空闲' : '不可用'
})

const cameraTip = computed(() => {
  if (!cameraSupported) {
    return '当前浏览器不支持相机访问。请使用图片上传或手动输入报名编号查询。'
  }
  if (cameraActive.value && detectorSupported) {
    return '相机预览激活时将自动检测二维码。'
  }
  if (cameraActive.value && !detectorSupported) {
    return '当前浏览器不支持自动二维码检测。请使用"截取当前画面"或"拍照/选择图片"。'
  }
  return '如果相机启动失败，请检查浏览器权限、HTTPS 或 localhost 访问，以及是否有其他应用正在使用相机。'
})

const normalizeCode = (value) => String(value || '').trim()

const goBack = () => {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }
  router.push('/merchant/bookings')
}

const goRecords = () => {
  router.push({
    path: '/merchant/bookings/records',
    query: { backTo: route.fullPath }
  })
}

const isLocalhost = () => {
  const host = window.location.hostname
  return host === 'localhost' || host === '127.0.0.1' || host === '::1'
}

const describeCameraError = (error) => {
  if (!window.isSecureContext && !isLocalhost()) {
    return '相机访问需要 HTTPS 或 localhost。请在安全环境下打开此页面。'
  }
  if (['NotAllowedError', 'SecurityError', 'PermissionDeniedError'].includes(error?.name)) {
    return '相机权限被拒绝。请在浏览器中允许相机访问后重试。'
  }
  if (['NotFoundError', 'DevicesNotFoundError'].includes(error?.name)) {
    return '未找到可用的相机设备。'
  }
  if (['NotReadableError', 'TrackStartError'].includes(error?.name)) {
    return '相机被其他应用占用。请关闭其他应用后重试。'
  }
  if (['OverconstrainedError', 'ConstraintNotSatisfiedError'].includes(error?.name)) {
    return '无法打开首选相机。请尝试重新连接相机。'
  }
  return '相机启动失败。请检查浏览器权限和设备可用性。'
}

const buildVideoConstraintAttempts = () => ([
  { video: { facingMode: { exact: 'environment' } }, audio: false },
  { video: { facingMode: { ideal: 'environment' } }, audio: false },
  { video: { facingMode: { ideal: 'user' } }, audio: false },
  { video: true, audio: false }
])

const stopCamera = () => {
  if (scanTimer) {
    window.clearInterval(scanTimer)
    scanTimer = null
  }
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
  if (videoRef.value) {
    videoRef.value.pause?.()
    videoRef.value.srcObject = null
  }
  detector = null
  lastCode = ''
  lastScanAt = 0
  if (cameraSupported) {
    cameraMessage.value = '相机已停止。您可以随时重新启动。'
  }
}

const attachStream = async (stream) => {
  if (!videoRef.value) {
    return
  }
  videoRef.value.srcObject = stream
  await videoRef.value.play().catch(() => {})
}

const startDetectorLoop = () => {
  if (!detectorSupported || !videoRef.value) {
    return
  }
  detector = new window.BarcodeDetector({ formats: ['qr_code'] })
  scanTimer = window.setInterval(scanFrame, 700)
}

const startCamera = async (manual = false) => {
  if (!cameraSupported || cameraStarting.value) {
    return
  }

  cameraStarting.value = true
  cameraMessage.value = manual ? '正在重新连接相机...' : '正在启动相机...'
  try {
    stopCamera()

    let stream = null
    let lastError = null
    for (const constraints of buildVideoConstraintAttempts()) {
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

    mediaStream = stream
    await attachStream(stream)

    if (detectorSupported) {
      cameraMessage.value = ''
      startDetectorLoop()
    } else {
      cameraMessage.value = '相机预览已激活。当前浏览器不支持自动二维码检测，请拍摄照片或手动输入报名编号。'
    }
  } catch (error) {
    stopCamera()
    cameraMessage.value = describeCameraError(error)
  } finally {
    cameraStarting.value = false
  }
}

const scanFrame = async () => {
  if (!detector || !videoRef.value || lookupLoading.value || !cameraActive.value) {
    return
  }
  const now = Date.now()
  if (now - lastScanAt < 500) {
    return
  }
  lastScanAt = now
  try {
    const barcodes = await detector.detect(videoRef.value)
    const code = normalizeCode(barcodes?.[0]?.rawValue)
    if (!code || code === lastCode) {
      return
    }
    lastCode = code
    manualCode.value = code
    await lookupBooking(code)
  } catch (error) {
    // Ignore frame-level detector errors and keep scanning.
  }
}

const triggerFilePicker = () => {
  fileInputRef.value?.click()
}

const loadImageElement = (source) => new Promise((resolve, reject) => {
  const image = new Image()
  image.onload = () => resolve(image)
  image.onerror = () => reject(new Error('图片加载失败'))
  image.src = source
})

const buildScaledSize = (width, height, maxEdge = MAX_IMAGE_EDGE) => {
  if (!width || !height) {
    return { width: 0, height: 0 }
  }
  const longestEdge = Math.max(width, height)
  if (longestEdge <= maxEdge) {
    return { width, height }
  }
  const ratio = maxEdge / longestEdge
  return {
    width: Math.max(1, Math.round(width * ratio)),
    height: Math.max(1, Math.round(height * ratio))
  }
}

const renderCompressedImage = (source, width, height) => {
  const { width: nextWidth, height: nextHeight } = buildScaledSize(width, height)
  if (!nextWidth || !nextHeight) {
    return ''
  }
  const canvas = captureCanvasRef.value || document.createElement('canvas')
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

const readFileAsDataUrl = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(String(reader.result || ''))
  reader.onerror = () => reject(new Error('读取图片文件失败'))
  reader.readAsDataURL(file)
})

const normalizeImageForLookup = async (imageData) => {
  const image = await loadImageElement(imageData)
  const normalized = renderCompressedImage(image, image.naturalWidth || image.width, image.naturalHeight || image.height)
  if (!normalized) {
    throw new Error('无法准备图片用于二维码查询')
  }
  return normalized
}

const lookupBookingByImage = async (imageData) => {
  if (!imageData) {
    throw new Error('图片数据为空')
  }
  const response = await lookupMerchantBookingByImage(imageData)
  if (response.code !== 200 || !response.data) {
    throw new Error(response.message || '无法从图片中解析报名二维码')
  }
  lookupResult.value = response.data
  lookupError.value = ''
}

const captureCurrentFrame = () => {
  if (!videoRef.value || !captureCanvasRef.value || !cameraActive.value) {
    return ''
  }
  const width = videoRef.value.videoWidth
  const height = videoRef.value.videoHeight
  if (!width || !height) {
    return ''
  }
  return renderCompressedImage(videoRef.value, width, height)
}

const capturePhotoForLookup = async () => {
  if (photoProcessing.value) {
    return
  }
  const imageData = captureCurrentFrame()
  if (!imageData) {
    lookupError.value = '当前相机画面尚未就绪，请稍后重试。'
    return
  }
  photoProcessing.value = true
  lookupError.value = ''
  try {
    await lookupBookingByImage(imageData)
    notify('二维码图片解码成功', 'success')
  } catch (error) {
    lookupResult.value = null
    lookupError.value = error?.response?.status === 500
      ? '图片上传过大或服务器无法处理。请靠近二维码后重试。'
      : (error.message || '二维码图片解码失败')
  } finally {
    photoProcessing.value = false
  }
}

const handleFileSelected = async (event) => {
  const file = event.target?.files?.[0]
  if (!file) {
    return
  }
  photoProcessing.value = true
  lookupError.value = ''
  try {
    const imageData = await readFileAsDataUrl(file)
    const normalized = await normalizeImageForLookup(imageData)
    await lookupBookingByImage(normalized)
    notify('二维码图片解码成功', 'success')
  } catch (error) {
    lookupResult.value = null
    lookupError.value = error?.response?.status === 500
      ? '图片上传过大或服务器无法处理。请靠近二维码后重试。'
      : (error.message || '二维码图片解码失败')
  } finally {
    photoProcessing.value = false
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
  }
}

const lookupBooking = async (value = manualCode.value) => {
  const code = normalizeCode(value)
  if (!code) {
    lookupResult.value = null
    lookupError.value = '请先输入报名编号。'
    return
  }

  manualCode.value = code
  lookupLoading.value = true
  lookupError.value = ''
  try {
    const response = await lookupMerchantBooking(code)
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '未找到该报名')
    }
    lookupResult.value = response.data
  } catch (error) {
    const fallback = await getMerchantBookings({ keyword: code })
    const items = Array.isArray(fallback.data?.items) ? fallback.data.items : []
    if (items.length === 1) {
      lookupResult.value = items[0]
      lookupError.value = ''
    } else {
      lookupResult.value = null
      lookupError.value = error.message || '未找到该报名'
    }
  } finally {
    lookupLoading.value = false
  }
}

const confirmCheckin = async () => {
  if (!lookupResult.value?.id || submitting.value) {
    return
  }
  submitting.value = true
  try {
    const response = await checkinBooking(lookupResult.value.id)
    if (response.code !== 200) {
      throw new Error(response.message || '核销失败')
    }
    notify('报名已核销', 'success')
    await lookupBooking(lookupResult.value.reserveNo || lookupResult.value.id)
  } catch (error) {
    notify(error.message || '核销失败', 'error')
  } finally {
    submitting.value = false
  }
}

const statusText = (status) => ({ registered: '待核销', checked_in: '已核销', rejected: '已拒绝', cancelled: '已取消' }[status] || status)
const formatTime = (value) => (value ? new Date(value).toLocaleString() : '未记录')
const formatRange = (booking) => {
  if (!booking?.startTime && !booking?.endTime) return '待定'
  const start = booking.startTime ? formatTime(booking.startTime) : '待定'
  const end = booking.endTime ? formatTime(booking.endTime) : ''
  return end ? `${start} - ${end}` : start
}
const formatLocation = (booking) => [booking.locationProvince, booking.locationCity, booking.locationDistrict, booking.locationDetail].filter(Boolean).join(' / ') || '待定'

watch(() => route.query.code, async (value) => {
  if (typeof value === 'string' && value.trim()) {
    manualCode.value = value.trim()
    await lookupBooking(value.trim())
  }
})

onMounted(async () => {
  if (typeof route.query.code === 'string' && route.query.code.trim()) {
    manualCode.value = route.query.code.trim()
    await lookupBooking(route.query.code.trim())
  }
  await startCamera()
})

onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped>
.scan-page { max-width: 1180px; margin: 0 auto; }
.page-header, .page-header__main, .scan-layout, .camera-actions, .photo-actions, .manual-row, .result-actions { display: flex; }
.page-header, .result-actions { justify-content: space-between; }
.page-header { align-items: flex-start; gap: 18px; margin-bottom: 20px; }
.page-header__main { gap: 14px; align-items: flex-start; }
.page-header h1, .card-head h2 { margin: 0; color: #0f172a; }
.page-header h1 { font-size: 28px; }
.page-header p { margin: 8px 0 0; color: #64748b; line-height: 1.7; }
.back-btn, .primary-btn, .ghost-btn { border: none; cursor: pointer; }
.back-btn { width: 44px; height: 44px; border-radius: 50%; background: #fff; box-shadow: 0 8px 20px rgba(15,23,42,0.08); font-size: 22px; }
.primary-btn, .ghost-btn { min-height: 44px; padding: 0 18px; border-radius: 12px; font-weight: 700; }
.primary-btn { background: #c04851; color: #fff; }
.ghost-btn { background: #f4f6fb; color: #334155; }
.scan-layout { gap: 18px; align-items: stretch; }
.camera-card, .result-card, .state-card { border-radius: 22px; border: 1px solid #e5e7eb; background: rgba(255,255,255,0.95); box-shadow: 0 16px 36px rgba(15,23,42,0.05); }
.camera-card, .result-card { flex: 1; padding: 18px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.card-head span { color: #6b7280; }
.camera-box { position: relative; margin-top: 16px; border-radius: 20px; overflow: hidden; background: linear-gradient(135deg, #111827, #1f2937); min-height: 340px; }
.camera-video { width: 100%; height: 340px; object-fit: cover; display: block; }
.capture-canvas { display: none; }
.camera-placeholder { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: rgba(255,255,255,0.9); background: radial-gradient(circle at center, rgba(30,41,59,0.28), rgba(15,23,42,0.78)); z-index: 1; }
.camera-placeholder i { font-size: 40px; }
.camera-placeholder p { margin: 0; font-weight: 600; text-align: center; padding: 0 16px; }
.scan-frame { position: absolute; inset: 50% auto auto 50%; width: 220px; height: 220px; transform: translate(-50%, -50%); border: 2px solid rgba(255,255,255,0.9); border-radius: 20px; box-shadow: 0 0 0 9999px rgba(15,23,42,0.18); z-index: 2; pointer-events: none; }
.camera-tip { margin: 14px 0 0; color: #64748b; line-height: 1.7; }
.camera-actions, .photo-actions, .manual-row { gap: 12px; margin-top: 14px; }
.manual-input { flex: 1; min-height: 44px; padding: 0 14px; border-radius: 12px; border: 1px solid #d4d9e0; }
.hidden-input { display: none; }
.state-card { padding: 54px 18px; text-align: center; color: #94a3b8; }
.state-card i { font-size: 40px; }
.error-state { color: #b42318; }
.result-body { margin-top: 16px; }
.result-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.result-top strong { color: #111827; font-size: 20px; }
.status-badge { padding: 6px 12px; border-radius: 999px; font-style: normal; font-weight: 700; }
.status-badge.registered { color: #c04851; background: rgba(192,72,81,0.12); }
.status-badge.checked_in { color: #1661ab; background: rgba(22,97,171,0.12); }
.status-badge.rejected { color: #b45309; background: #fef3c7; }
.status-badge.cancelled { color: #6b7280; background: #e5e7eb; }
.info-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 14px; margin-top: 16px; }
.info-grid span { display: block; color: #7c8694; font-size: 13px; }
.info-grid strong { display: block; margin-top: 6px; color: #1f2937; line-height: 1.6; }
.remark { margin: 16px 0 0; color: #475569; line-height: 1.7; }
.result-actions { gap: 12px; margin-top: 18px; flex-wrap: wrap; }
@media (max-width: 960px) { .scan-layout { flex-direction: column; } }
@media (max-width: 760px) { .page-header, .page-header__main, .photo-actions, .manual-row, .result-top { flex-direction: column; } .manual-input, .primary-btn, .ghost-btn { width: 100%; } .info-grid { grid-template-columns: 1fr; } }
</style>
