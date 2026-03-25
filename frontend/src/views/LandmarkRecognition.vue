<template>
  <div class="landmark-page">
    <div class="header">
      <button class="back-btn" @click="goBack">
        <i class='bx bx-arrow-back'></i>
      </button>
      <h1>{{ $t('landmark.title') }}</h1>
      <div class="header-right"></div>
    </div>

    <div class="main-content">
      <div class="upload-section" v-if="!uploadedImage">
        <div class="upload-area" @click="triggerUpload">
          <i class='bx bx-camera'></i>
          <p>{{ $t('landmark.clickUpload') }}</p>
          <span>{{ $t('landmark.supportPhoto') }}</span>
        </div>
        <div class="location-filter">
          <label class="toggle">
            <input type="checkbox" v-model="useLocation" />
            <span>{{ $t('landmark.useLocation') }}</span>
          </label>
          <select v-if="useLocation" v-model="radiusKm" class="radius-select">
            <option :value="5">{{ $t('landmark.radiusKm', { km: 5 }) }}</option>
            <option :value="10">{{ $t('landmark.radiusKm', { km: 10 }) }}</option>
            <option :value="30">{{ $t('landmark.radiusKm', { km: 30 }) }}</option>
          </select>
        </div>
        
        <div class="upload-buttons">
          <button class="upload-btn" @click="takePhoto">
            <i class='bx bx-camera'></i>
            <span>{{ $t('landmark.takePhoto') }}</span>
          </button>
          <button class="upload-btn" @click="chooseFromGallery">
            <i class='bx bx-image'></i>
            <span>{{ $t('landmark.fromGallery') }}</span>
          </button>
        </div>

        <input 
          type="file" 
          ref="fileInput" 
          @change="handleFileUpload" 
          accept="image/*" 
          capture="environment"
          style="display: none;"
        />
      </div>

      <div class="result-section" v-else>
        <div class="image-preview">
          <img :src="uploadedImage" alt="上传的图片" />
          <button class="change-image-btn" @click="changeImage">
            <i class='bx bx-refresh'></i>
            <span>{{ $t('landmark.changeImage') }}</span>
          </button>
        </div>

        <div class="loading-state" v-if="isRecognizing">
          <div class="spinner"></div>
          <p>{{ $t('landmark.recognizing') }}</p>
        </div>

        <div class="recognition-result" v-else-if="recognitionResult">
          <div class="result-header">
            <i class='bx bx-map-pin'></i>
            <h2>{{ recognitionResult.name }}</h2>
            <div class="confidence">
              <span class="confidence-label">{{ $t('landmark.confidence') }}</span>
              <div class="confidence-bar">
                <div class="confidence-fill" :style="{ width: recognitionResult.confidence + '%' }"></div>
              </div>
              <span class="confidence-value">{{ recognitionResult.confidence }}%</span>
            </div>
          </div>

          <div class="result-content">
            <div class="info-card">
              <div class="info-item">
                <i class='bx bx-star'></i>
                <div class="info-text">
                  <span class="label">{{ $t('landmark.rating') }}</span>
                  <span class="value">{{ recognitionResult.rating }}</span>
                </div>
              </div>
              <div class="info-item">
                <i class='bx bx-time'></i>
                <div class="info-text">
                  <span class="label">{{ $t('landmark.openTime') }}</span>
                  <span class="value">{{ recognitionResult.duration }}</span>
                </div>
              </div>
              <div class="info-item">
                <i class='bx bx-calendar'></i>
                <div class="info-text">
                  <span class="label">{{ $t('landmark.bestSeason') }}</span>
                  <span class="value">{{ recognitionResult.bestSeason }}</span>
                </div>
              </div>
            </div>

            <div class="description-card">
              <h3><i class='bx bx-info-circle'></i> {{ $t('landmark.intro') }}</h3>
              <p>{{ recognitionResult.description }}</p>
            </div>

            <div class="description-card" v-if="recognitionResult.story">
              <h3><i class='bx bx-book'></i> {{ $t('landmark.story') }}</h3>
              <p>{{ recognitionResult.story }}</p>
            </div>

            <div class="gallery-card" v-if="recognitionResult.images && recognitionResult.images.length > 0">
              <h3><i class='bx bx-images'></i> {{ $t('landmark.images') }}</h3>
              <div 
                class="gallery-marquee" 
                @mouseenter="pauseMarquee" 
                @mouseleave="resumeMarquee"
              >
                <div 
                  class="marquee-track" 
                  :class="{ paused: isMarqueePaused }" 
                  :style="{ '--marquee-duration': marqueeDuration + 's' }"
                >
                  <img 
                    v-for="(img, index) in doubledImages" 
                    :key="'m1-' + index" 
                    :src="img" 
                    :alt="recognitionResult.name"
                    @click="openPreviewAt(index)"
                  />
                </div>
              </div>
            </div>

            <div class="recommend-card" v-if="recognitionResult.nearbyAttractions && recognitionResult.nearbyAttractions.length > 0">
              <h3><i class='bx bx-map-alt'></i> {{ $t('landmark.nearbyAttractions') }}</h3>
              <div class="recommend-list">
                <div 
                  v-for="attraction in recognitionResult.nearbyAttractions" 
                  :key="attraction.id"
                  class="recommend-item"
                  @click="openAttractionDetail(attraction)"
                >
                  <img :src="attraction.image" :alt="attraction.name" />
                  <div class="recommend-info">
                    <h4>{{ attraction.name }}</h4>
                    <p>{{ attraction.distance }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <teleport to="body">
    <div v-if="showCameraModal" class="camera-modal" @click.self="closeCamera">
      <div class="camera-modal-content">
        <div class="camera-header">
          <h3>{{ $t('landmark.takePhoto') }}</h3>
          <button class="camera-close-btn" @click="closeCamera">
            <i class='bx bx-x'></i>
          </button>
        </div>
        <div class="camera-body">
          <video ref="videoRef" class="camera-video" autoplay playsinline></video>
          <canvas ref="canvasRef" style="display: none;"></canvas>
        </div>
        <div class="camera-footer">
          <button class="camera-btn cancel" @click="closeCamera">
            <i class='bx bx-x'></i>
            <span>{{ $t('common.cancel') }}</span>
          </button>
          <button class="camera-btn capture" @click="capturePhoto">
            <i class='bx bx-camera'></i>
            <span>{{ $t('landmark.capture') }}</span>
          </button>
        </div>
      </div>
    </div>
    
    <div v-if="showImageModal" class="image-modal" @click.self="closeImageModal">
      <button class="image-modal-nav left" @click.stop="prevImage">❮</button>
      <img class="image-modal-content" :src="currentImage" alt="预览图片" />
      <button class="image-modal-nav right" @click.stop="nextImage">❯</button>
      <button class="image-modal-close" @click="closeImageModal">{{ $t('common.close') }}</button>
    </div>
    
    <div v-if="showAttractionModal && selectedAttraction" class="attraction-modal" @click.self="closeAttractionModal">
      <div class="attraction-modal-content">
        <button class="attraction-modal-close" @click="closeAttractionModal">{{ $t('common.close') }}</button>
        <div class="attraction-modal-header">
          <div class="attraction-info">
            <h3>{{ selectedAttraction.name }}</h3>
            <div class="attraction-details">
              <div class="detail-item">
                <span class="label">{{ $t('landmark.rating') }}</span>
                <span class="value">{{ selectedAttraction.rating || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">{{ $t('landmark.openTime') }}</span>
                <span class="value">{{ selectedAttraction.duration || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">{{ $t('landmark.bestSeason') }}</span>
                <span class="value">{{ selectedAttraction.bestSeason || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">{{ $t('landmark.distance') }}</span>
                <span class="value">{{ selectedAttraction.distance }}</span>
              </div>
            </div>
          </div>
          <div class="attraction-image">
            <img :src="selectedAttraction.image" :alt="selectedAttraction.name" />
          </div>
        </div>
        <div class="attraction-modal-body">
          <div class="section">
            <h4>{{ $t('landmark.intro') }}</h4>
            <p>{{ selectedAttraction.intro || $t('landmark.noIntro') }}</p>
          </div>
          <div class="section" v-if="selectedAttraction.story">
            <h4>{{ $t('landmark.story') }}</h4>
            <p>{{ selectedAttraction.story }}</p>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { recognizeLandmarkBase64 } from '../api/app'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const { t, locale } = useI18n()
const fileInput = ref(null)
const uploadedImage = ref(null)
const isRecognizing = ref(false)
const recognitionResult = ref(null)

const attractionNameToKey = {
  '武汉黄鹤楼': 'wuhanHuangHeLou',
  '北京故宫': 'beijingGuGong',
  '北京颐和园': 'beijingYiHeYuan',
  '北京天坛': 'beijingTianTan',
  '杭州西湖三潭印月': 'hangzhouXiHuSanTanYinYue',
  '西安大雁塔': 'xianDaYanTa',
  '开封铁塔': 'kaifengTieTa',
  '武汉东湖': 'wuhanDongHu',
  '杭州雷峰塔': 'hangzhouLeiFengTa',
  '西安钟楼': 'xianZhongLou',
  '开封开封府': 'kaifengKaiFengFu'
}

const getTranslatedAttraction = (name, data) => {
  const key = attractionNameToKey[name]
  if (!key) {
    return {
      name: name,
      rating: data.rating || '景区',
      duration: data.duration || '建议2-3小时',
      bestSeason: data.bestSeason || '四季皆宜',
      intro: data.intro || '暂无介绍',
      story: data.story || ''
    }
  }
  
  return {
    name: t(`attractions.${key}.name`),
    rating: t(`attractions.${key}.rating`),
    duration: t(`attractions.${key}.duration`),
    bestSeason: t(`attractions.${key}.bestSeason`),
    intro: t(`attractions.${key}.intro`),
    story: t(`attractions.${key}.story`)
  }
}

const getTranslatedNearbyAttraction = (attraction) => {
  const key = attractionNameToKey[attraction.name]
  if (!key) {
    return {
      id: attraction.id,
      name: attraction.name,
      rating: attraction.rating || '-',
      duration: attraction.duration || '-',
      bestSeason: attraction.bestSeason || '-',
      intro: attraction.intro || '',
      story: attraction.story || '',
      distance: attraction.distance || '附近景点'
    }
  }
  
  return {
    id: attraction.id,
    name: t(`attractions.${key}.name`),
    rating: t(`attractions.${key}.rating`),
    duration: t(`attractions.${key}.duration`),
    bestSeason: t(`attractions.${key}.bestSeason`),
    intro: t(`attractions.${key}.intro`),
    story: t(`attractions.${key}.story`),
    distance: attraction.distance || '附近景点'
  }
}

const showCameraModal = ref(false)
const videoRef = ref(null)
const canvasRef = ref(null)
const cameraStream = ref(null)
const cameraError = ref('')

const goBack = () => {
  router.back()
}

const triggerUpload = () => {
  fileInput.value.click()
}

const takePhoto = async () => {
  cameraError.value = ''
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
      audio: false
    })
    cameraStream.value = stream
    showCameraModal.value = true
    setTimeout(() => {
      if (videoRef.value) {
        videoRef.value.srcObject = stream
        videoRef.value.play()
      }
    }, 100)
  } catch (err) {
    console.error('无法访问摄像头:', err)
    cameraError.value = '无法访问摄像头，请检查权限设置'
    notify.error(t('landmark.cameraAccessFailed'))
  }
}

const capturePhoto = () => {
  if (!videoRef.value || !canvasRef.value) return
  
  const video = videoRef.value
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
  
  const imageData = canvas.toDataURL('image/jpeg', 0.9)
  uploadedImage.value = imageData
  
  closeCamera()
  recognizeLandmark()
}

const closeCamera = () => {
  if (cameraStream.value) {
    cameraStream.value.getTracks().forEach(track => track.stop())
    cameraStream.value = null
  }
  showCameraModal.value = false
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
}

const chooseFromGallery = () => {
  fileInput.value.removeAttribute('capture')
  fileInput.value.click()
}

const handleFileUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      uploadedImage.value = e.target.result
      recognizeLandmark()
    }
    reader.readAsDataURL(file)
  }
}

const changeImage = () => {
  uploadedImage.value = null
  recognitionResult.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const recognizeLandmark = async () => {
  isRecognizing.value = true
  
  try {
    let coords = null
    if (useLocation.value) {
      coords = await new Promise((resolve) => {
        if (!navigator.geolocation) { resolve(null); return }
        navigator.geolocation.getCurrentPosition(
          (pos) => resolve(pos.coords),
          () => resolve(null),
          { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
        )
      })
    }
    const payload = { image: uploadedImage.value }
    if (coords) {
      console.log('GPS 定位：', { lat: coords.latitude, lng: coords.longitude })
      console.log('识别半径：', radiusKm.value, 'km')
      payload.lat = coords.latitude
      payload.lng = coords.longitude
      payload.radius = radiusKm.value * 1000
    } else if (useLocation.value) {
      notify.warning(t('landmark.locationFailed'))
      console.warn('定位失败，未获取到坐标，按全局识别进行')
    }
    console.log('识别请求参数：', payload)
    const response = await recognizeLandmarkBase64(payload)
    
    if (response.code === 200 && response.data) {
      const data = response.data
      try {
        console.log('匹配策略:', data.strategy)
        console.log('调试信息:', data.debug)
        if (data.model) {
          console.log('模型原始返回（归一化）：', data.model)
          if (data.model.all_predictions) {
            console.log('模型全量预测（Top-N，0-1置信）：', data.model.all_predictions.slice(0, 10))
          }
        }
      } catch (e) {}
      
      const translated = getTranslatedAttraction(data.name, data)
      recognitionResult.value = {
        name: translated.name,
        confidence: Math.round(data.confidence * 100),
        location: '未知',
        rating: translated.rating,
        duration: translated.duration,
        bestSeason: translated.bestSeason,
        description: translated.intro,
        story: translated.story,
        images: (data.images || []).map(img => img.startsWith('http') ? img : `${img}`),
        nearbyAttractions: (data.nearbyAttractions || []).map(attraction => {
          const translatedNearby = getTranslatedNearbyAttraction(attraction)
          return {
            id: attraction.id,
            name: translatedNearby.name,
            image: attraction.cover ? (attraction.cover.startsWith('http') ? attraction.cover : `${attraction.cover}`) : '',
            rating: translatedNearby.rating,
            duration: translatedNearby.duration,
            bestSeason: translatedNearby.bestSeason,
            intro: translatedNearby.intro,
            story: translatedNearby.story,
            distance: translatedNearby.distance
          }
        })
      }
    } else {
      recognitionResult.value = {
        name: t('landmark.recognitionFailed'),
        confidence: 0,
        location: t('landmark.unknown'),
        rating: t('landmark.unknown'),
        duration: t('landmark.unknown'),
        bestSeason: t('landmark.unknown'),
        description: response.message || t('landmark.recognitionFailed'),
        images: [],
        nearbyAttractions: []
      }
    }
  } catch (error) {
    console.error('识别失败:', error)
    recognitionResult.value = {
      name: t('landmark.recognitionFailed'),
      confidence: 0,
      location: t('landmark.unknown'),
      rating: t('landmark.unknown'),
      duration: t('landmark.unknown'),
      bestSeason: t('landmark.unknown'),
      description: t('landmark.networkError'),
      images: [],
      nearbyAttractions: []
    }
  } finally {
    isRecognizing.value = false
  }
}

const previewImage = (img) => {
  openPreview(img)
}

const isMarqueePaused = ref(false)
const marqueeDuration = ref(30)
const useLocation = ref(false)
const radiusKm = ref(10)
const currentIndex = ref(0)
const baseImages = computed(() => {
  const imgs = (recognitionResult.value && Array.isArray(recognitionResult.value.images)) ? recognitionResult.value.images : []
  return imgs
})
const currentImage = computed(() => {
  const imgs = baseImages.value
  if (!imgs.length) return ''
  const idx = ((currentIndex.value % imgs.length) + imgs.length) % imgs.length
  return imgs[idx]
})
const openPreview = (img) => {
  const idx = baseImages.value.indexOf(img)
  currentIndex.value = idx >= 0 ? idx : 0
  showImageModal.value = true
  isMarqueePaused.value = true
  try { document.body.style.overflow = 'hidden' } catch (e) {}
}
const openPreviewAt = (i) => {
  const len = baseImages.value.length
  if (len === 0) return
  currentIndex.value = ((i % len) + len) % len
  openPreview(baseImages.value[currentIndex.value])
}
const pauseMarquee = () => { isMarqueePaused.value = true }
const resumeMarquee = () => { if (!showImageModal.value) isMarqueePaused.value = false }

const doubledImages = computed(() => {
  const imgs = baseImages.value
  return [...imgs, ...imgs]
})

const showImageModal = ref(false)
const selectedImage = ref('') // 保持兼容，已由 currentImage 代替显示
const showAttractionModal = ref(false)
const selectedAttraction = ref(null)

const closeImageModal = () => {
  showImageModal.value = false
  isMarqueePaused.value = false
  try { document.body.style.overflow = '' } catch (e) {}
}

const openAttractionDetail = (attraction) => {
  selectedAttraction.value = attraction
  showAttractionModal.value = true
  try { document.body.style.overflow = 'hidden' } catch (e) {}
}

const closeAttractionModal = () => {
  showAttractionModal.value = false
  selectedAttraction.value = null
  try { document.body.style.overflow = '' } catch (e) {}
}

const nextImage = () => {
  if (!baseImages.value.length) return
  currentIndex.value = (currentIndex.value + 1) % baseImages.value.length
}
const prevImage = () => {
  if (!baseImages.value.length) return
  currentIndex.value = (currentIndex.value - 1 + baseImages.value.length) % baseImages.value.length
}

const handleKeyDown = (e) => {
  if (!showImageModal.value) return
  if (e.key === 'ArrowRight') {
    e.preventDefault()
    nextImage()
  } else if (e.key === 'ArrowLeft') {
    e.preventDefault()
    prevImage()
  } else if (e.key === 'Escape') {
    e.preventDefault()
    closeImageModal()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.landmark-page {
  min-height: 100vh;
  background: linear-gradient(90deg, #e2e2e2, #c9d6ff);
  width: 100%;
}

/* 顶部导航栏 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header h1 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #333;
  cursor: pointer;
  padding: 5px;
  transition: all 0.3s;
}

.back-btn:hover {
  background: rgba(116, 148, 236, 0.2);
  transform: translateX(-5px);
}

.header-right {
  width: 34px;
}

/* 主要内容区 */
.main-content {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

/* 上传区域 */
.upload-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 30px;
}

.location-filter {
  display: flex;
  align-items: center;
  gap: 12px;
}
.location-filter .toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
}
.radius-select {
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.upload-area {
  background: rgba(255, 255, 255, 0.8);
  border: 3px dashed #7494ec;
  border-radius: 20px;
  padding: 60px 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.upload-area:hover {
  border-color: #5a7bd4;
  background: rgba(248, 249, 255, 0.9);
  transform: translateY(-5px);
  box-shadow: 0 4px 15px rgba(116, 148, 236, 0.2);
}

.upload-area i {
  font-size: 80px;
  color: #7494ec;
  margin-bottom: 20px;
}

.upload-area p {
  font-size: 18px;
  color: #333;
  margin: 10px 0;
  font-weight: 500;
}

.upload-area span {
  font-size: 14px;
  color: #999;
}

.upload-buttons {
  display: flex;
  gap: 20px;
  width: 100%;
  max-width: 400px;
}

.upload-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid rgba(116, 148, 236, 0.3);
  border-radius: 15px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.upload-btn:hover {
  background: rgba(116, 148, 236, 0.2);
  color: #333;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(116, 148, 236, 0.2);
}

.upload-btn:hover i,
.upload-btn:hover span {
  color: #7494ec;
}

.upload-btn i {
  font-size: 32px;
  color: #7494ec;
}

.upload-btn span {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

/* 图片预览 */
.image-preview {
  position: relative;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.image-preview img {
  width: 100%;
  max-height: 400px;
  object-fit: contain;
  display: block;
}

.change-image-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 15px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.change-image-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #7494ec;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-state p {
  font-size: 16px;
  color: #666;
}

/* 识别结果 */
.recognition-result {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.result-header {
  padding: 25px;
  background: linear-gradient(135deg, #7494ec 0%, #5a7bd4 100%);
  color: white;
}

.result-header i {
  font-size: 32px;
  margin-bottom: 10px;
}

.result-header h2 {
  font-size: 28px;
  margin: 10px 0;
}

.confidence {
  margin-top: 15px;
}

.confidence-label {
  font-size: 12px;
  opacity: 0.9;
  display: block;
  margin-bottom: 5px;
}

.confidence-bar {
  height: 8px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  overflow: hidden;
  margin: 5px 0;
}

.confidence-fill {
  height: 100%;
  background: white;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.confidence-value {
  font-size: 14px;
  font-weight: 600;
}

.result-content {
  padding: 20px;
}

/* 信息卡片 */
.info-card {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px;
  background: rgba(116, 148, 236, 0.1);
  border: 1px solid rgba(116, 148, 236, 0.3);
  border-radius: 12px;
  transition: all 0.3s;
}

.info-item:hover {
  background: rgba(116, 148, 236, 0.2);
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(116, 148, 236, 0.2);
}

.info-item i {
  font-size: 24px;
  color: #7494ec;
}

.info-text {
  display: flex;
  flex-direction: column;
}

.info-text .label {
  font-size: 12px;
  color: #999;
}

.info-text .value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

/* 描述卡片 */
.description-card,
.gallery-card,
.recommend-card {
  margin-bottom: 20px;
}

.description-card h3,
.gallery-card h3,
.recommend-card h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  color: #333;
  margin-bottom: 15px;
}

.description-card h3 i,
.gallery-card h3 i,
.recommend-card h3 i {
  color: #7494ec;
}

.description-card p {
  font-size: 15px;
  line-height: 1.8;
  color: #666;
  text-align: justify;
}

/* 图片画廊 - 无缝滚动 */
.gallery-marquee {
  overflow: hidden;
  width: 100%;
  border-radius: 12px;
}
.marquee-track {
  display: flex;
  align-items: center;
  gap: 12px;
  animation: marquee var(--marquee-duration, 30s) linear infinite;
}
.marquee-track.paused {
  animation-play-state: paused;
}
.marquee-track img {
  width: 180px;
  height: 180px;
  border-radius: 12px;
  object-fit: cover;
  flex: 0 0 auto;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.marquee-track img:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 16px rgba(0,0,0,0.15);
}
@keyframes marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

/* 图片预览弹窗 */
.image-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
}
.image-modal-content {
  max-width: 90vw;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
}
.image-modal-close {
  position: fixed;
  top: 24px;
  right: 24px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  border: none;
  border-radius: 20px;
  padding: 8px 14px;
  cursor: pointer;
  z-index: 2001;
}
.image-modal-close:hover {
  background: rgba(0,0,0,0.8);
}

.image-modal-nav {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: rgba(0,0,0,0.5);
  color: #fff;
  cursor: pointer;
  z-index: 2001;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
.image-modal-nav.left { left: 24px; }
.image-modal-nav.right { right: 24px; }
.image-modal-nav:hover { background: rgba(0,0,0,0.7); }

/* 周边景点详情弹窗 */
.attraction-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
}

.attraction-modal-content {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 800px;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
}

.attraction-modal-close {
  position: absolute;
  top: 15px;
  right: 15px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  border: none;
  border-radius: 20px;
  padding: 8px 14px;
  cursor: pointer;
  z-index: 10;
}

.attraction-modal-close:hover {
  background: rgba(0,0,0,0.8);
}

.attraction-modal-header {
  display: flex;
  padding: 20px;
  border-bottom: 1px solid #eee;
  gap: 20px;
}

.attraction-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.attraction-info h3 {
  font-size: 24px;
  color: #333;
  margin: 0 0 20px 0;
}

.attraction-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-item .label {
  font-size: 14px;
  color: #999;
  min-width: 80px;
}

.detail-item .value {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.attraction-image {
  flex-shrink: 0;
  width: 200px;
  height: 200px;
}

.attraction-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px;
}

.attraction-modal-body {
  padding: 20px;
}

.section {
  margin-bottom: 20px;
}

.section h4 {
  font-size: 18px;
  color: #333;
  margin: 0 0 10px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section h4::before {
  content: '•';
  color: #7494ec;
  font-size: 20px;
}

.section p {
  font-size: 16px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 600px) {
  .attraction-modal-header {
    flex-direction: column;
  }
  
  .attraction-image {
    width: 100%;
    height: 200px;
  }
}

/* 周边景点推荐 */
.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  background: rgba(116, 148, 236, 0.1);
  border: 1px solid rgba(116, 148, 236, 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.recommend-item:hover {
  background: rgba(116, 148, 236, 0.2);
  transform: translateX(5px);
  box-shadow: 0 4px 10px rgba(116, 148, 236, 0.2);
}

.recommend-item img {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  object-fit: cover;
}

.recommend-info h4 {
  font-size: 16px;
  color: #333;
  margin: 0 0 5px 0;
}

.recommend-info p {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.camera-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.camera-modal-content {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.camera-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  background: linear-gradient(135deg, #7494ec 0%, #5a7bd4 100%);
  color: white;
}

.camera-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.camera-close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  padding: 5px;
  line-height: 1;
  opacity: 0.9;
  transition: opacity 0.2s;
}

.camera-close-btn:hover {
  opacity: 1;
}

.camera-body {
  position: relative;
  background: #000;
}

.camera-video {
  width: 100%;
  display: block;
  max-height: 400px;
  object-fit: cover;
}

.camera-footer {
  display: flex;
  gap: 15px;
  padding: 20px;
  background: #f8f9fa;
}

.camera-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 15px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.camera-btn.cancel {
  background: #e9ecef;
  color: #666;
}

.camera-btn.cancel:hover {
  background: #dee2e6;
}

.camera-btn.capture {
  background: linear-gradient(135deg, #7494ec 0%, #5a7bd4 100%);
  color: white;
}

.camera-btn.capture:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(116, 148, 236, 0.4);
}

.camera-btn i {
  font-size: 20px;
}

/* 响应式设计 */
@media (max-width: 600px) {
  .info-card {
    grid-template-columns: 1fr;
  }
  
  .gallery-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .upload-buttons {
    flex-direction: column;
  }
}
</style>
