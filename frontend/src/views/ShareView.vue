<template>
  <div class="share-page">
    <div class="share-container" v-if="post">
      <div class="share-header">
        <div class="logo">YayFolk</div>
      </div>

      <div class="share-content">
        <div class="post-images" v-if="post.images && post.images.length > 0">
          <div class="image-slider">
            <img 
              :src="post.images[currentImageIndex]"
              alt="Post image"
              class="post-image"
              @click="openImagePreview"
              style="cursor: zoom-in;"
            />
            
            <button 
              v-if="post.images.length > 1" 
              class="slider-btn prev-btn"
              @click="prevImage"
            >
              <i class='bx bx-chevron-left'></i>
            </button>
            <button 
              v-if="post.images.length > 1" 
              class="slider-btn next-btn"
              @click="nextImage"
            >
              <i class='bx bx-chevron-right'></i>
            </button>
            
            <div v-if="post.images.length > 1" class="image-indicators">
              <span 
                v-for="(image, index) in post.images" 
                :key="index"
                class="indicator-dot"
                :class="{ active: currentImageIndex === index }"
                @click="currentImageIndex = index"
              ></span>
            </div>
            
            <div v-if="post.images.length > 1" class="image-counter">
              {{ currentImageIndex + 1 }} / {{ post.images.length }}
            </div>
          </div>
        </div>

        <div class="post-info">
          <div class="author-info" @click="showLoginPrompt">
            <img :src="post.author?.avatar || defaultAvatar" alt="Avatar" class="author-avatar" />
            <div class="author-details">
              <h4>{{ post.author?.name || '未知用户' }}</h4>
              <p>{{ post.time }} · {{ post.author?.location || '未知' }}</p>
            </div>
          </div>

          <div class="post-content">
            <h3>{{ post.title || '帖子标题' }}</h3>
            <p>{{ post.content }}</p>
            <div class="hashtags" v-if="post.hashtags && post.hashtags.length > 0">
              <span v-for="(tag, index) in post.hashtags" :key="index" class="hashtag">#{{ tag }}</span>
            </div>
          </div>

          <div class="post-stats" @click="showLoginPrompt">
            <div class="stat-item">
              <i class='bx bx-star'></i>
              <span>{{ post.collects || 0 }}</span>
            </div>
            <div class="stat-item">
              <i class='bx bx-comment'></i>
              <span>{{ post.comments || 0 }}</span>
            </div>
            <div class="stat-item">
              <i class='bx bx-show'></i>
              <span>{{ post.viewCount || 0 }}</span>
            </div>
          </div>

          <div class="login-prompt">
            <p>登录后查看更多精彩内容</p>
            <button class="login-btn" @click="goToLogin">立即登录</button>
          </div>
        </div>
      </div>
    </div>

    <div class="loading-state" v-else-if="loading">
      <i class='bx bx-loader-alt bx-spin'></i>
      <p>加载中...</p>
    </div>

    <div class="error-state" v-else>
      <i class='bx bx-error-circle'></i>
      <p>{{ errorMessage || '帖子不存在或已被删除' }}</p>
      <button class="back-btn" @click="goHome">返回首页</button>
    </div>

    <div v-if="showImagePreview" class="image-preview-modal">
      <div class="preview-overlay" @click="closeImagePreview"></div>
      <div class="preview-content">
        <img 
          :src="post?.images?.[currentImageIndex]"
          alt="Preview"
          class="preview-image-full"
          @click="closeImagePreview"
        />
        
        <button class="preview-close-btn" @click="closeImagePreview">
          <i class='bx bx-x'></i>
        </button>
        
        <button 
          v-if="post?.images?.length > 1" 
          class="preview-nav-btn preview-prev-btn"
          @click="prevPreviewImage"
        >
          <i class='bx bx-chevron-left'></i>
        </button>
        <button 
          v-if="post?.images?.length > 1" 
          class="preview-nav-btn preview-next-btn"
          @click="nextPreviewImage"
        >
          <i class='bx bx-chevron-right'></i>
        </button>
        
        <div v-if="post?.images?.length > 1" class="preview-indicators">
          <span 
            v-for="(image, index) in post?.images" 
            :key="index"
            class="preview-indicator-dot"
            :class="{ active: currentImageIndex === index }"
            @click="currentImageIndex = index"
          ></span>
        </div>
        
        <div v-if="post?.images?.length > 1" class="preview-counter">
          {{ currentImageIndex + 1 }} / {{ post?.images?.length }}
        </div>
      </div>
    </div>

    <div v-if="showLoginModal" class="login-modal">
      <div class="login-overlay" @click="closeLoginModal"></div>
      <div class="login-content">
        <div class="login-icon">
          <i class='bx bx-lock-alt'></i>
        </div>
        <h3>需要登录</h3>
        <p>登录后查看更多精彩内容</p>
        <div class="login-actions">
          <button class="cancel-btn" @click="closeLoginModal">取消</button>
          <button class="confirm-btn" @click="goToLogin">确认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSharePostDetail } from '../api/app'

const route = useRoute()
const router = useRouter()

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=travelate-user'
const post = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const currentImageIndex = ref(0)
const showImagePreview = ref(false)
const showLoginModal = ref(false)

const loadPost = async () => {
  const postId = route.query.postId
  if (!postId) {
    loading.value = false
    errorMessage.value = '帖子ID不存在'
    return
  }

  try {
    const response = await getSharePostDetail(postId)
    if (response.code === 200) {
      post.value = response.data
    } else {
      errorMessage.value = response.message || '加载失败'
    }
  } catch (error) {
    errorMessage.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const prevImage = () => {
  if (!post.value?.images?.length) return
  currentImageIndex.value = (currentImageIndex.value - 1 + post.value.images.length) % post.value.images.length
}

const nextImage = () => {
  if (!post.value?.images?.length) return
  currentImageIndex.value = (currentImageIndex.value + 1) % post.value.images.length
}

const openImagePreview = () => {
  showImagePreview.value = true
}

const closeImagePreview = () => {
  showImagePreview.value = false
}

const prevPreviewImage = () => {
  prevImage()
}

const nextPreviewImage = () => {
  nextImage()
}

const showLoginPrompt = () => {
  showLoginModal.value = true
}

const closeLoginModal = () => {
  showLoginModal.value = false
}

const goToLogin = () => {
  showLoginModal.value = false
  router.push('/login')
}

const goHome = () => {
  router.push('/')
}

onMounted(() => {
  loadPost()
})
</script>

<style scoped>
.share-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.share-container {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  min-height: 100vh;
}

.share-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #ff2442;
}

.share-content {
  padding: 20px;
}

.post-images {
  margin-bottom: 20px;
}

.image-slider {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  background: #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.post-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.slider-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.3s;
  z-index: 10;
}

.slider-btn:hover {
  background: rgba(0, 0, 0, 0.7);
  transform: translateY(-50%) scale(1.1);
}

.prev-btn {
  left: 20px;
}

.next-btn {
  right: 20px;
}

.image-indicators {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 10;
}

.indicator-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s;
}

.indicator-dot.active {
  background: white;
  width: 20px;
  border-radius: 4px;
}

.indicator-dot:hover {
  background: rgba(255, 255, 255, 0.8);
}

.image-counter {
  position: absolute;
  top: 20px;
  right: 20px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 12px;
  z-index: 10;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.author-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.author-details h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.author-details p {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #999;
}

.post-content {
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
}

.post-content h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
}

.post-content p {
  margin: 0 0 12px 0;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.hashtags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.hashtag {
  font-size: 12px;
  color: #666;
  cursor: pointer;
}

.post-stats {
  display: flex;
  gap: 24px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
}

.stat-item i {
  font-size: 18px;
}

.login-prompt {
  padding: 24px 0;
  text-align: center;
}

.login-prompt p {
  margin: 0 0 16px 0;
  color: #999;
  font-size: 14px;
}

.login-btn {
  padding: 12px 32px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.login-btn:hover {
  background: #ff3a56;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.3);
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  color: #999;
}

.loading-state i,
.error-state i {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-state p {
  margin: 0 0 20px 0;
  font-size: 16px;
}

.back-btn {
  padding: 10px 24px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.back-btn:hover {
  background: #ff3a56;
}

.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.85);
}

.preview-content {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image-full {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  cursor: zoom-out;
}

.preview-close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.3s;
  z-index: 10;
}

.preview-close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.1);
}

.preview-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 50px;
  height: 50px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  transition: all 0.3s;
  z-index: 10;
}

.preview-nav-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-50%) scale(1.1);
}

.preview-prev-btn {
  left: 30px;
}

.preview-next-btn {
  right: 30px;
}

.preview-indicators {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 10;
}

.preview-indicator-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all 0.3s;
}

.preview-indicator-dot.active {
  background: white;
  width: 24px;
  border-radius: 5px;
}

.preview-indicator-dot:hover {
  background: rgba(255, 255, 255, 0.7);
}

.preview-counter {
  position: absolute;
  top: 20px;
  left: 20px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 8px 16px;
  border-radius: 16px;
  font-size: 14px;
  z-index: 10;
}

.login-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
}

.login-content {
  position: relative;
  background: white;
  border-radius: 16px;
  padding: 32px;
  width: 320px;
  text-align: center;
  z-index: 1;
}

.login-icon {
  width: 64px;
  height: 64px;
  background: #fff0f3;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.login-icon i {
  font-size: 32px;
  color: #ff2442;
}

.login-content h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
}

.login-content p {
  margin: 0 0 24px 0;
  color: #666;
  font-size: 14px;
}

.login-actions {
  display: flex;
  gap: 12px;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  padding: 12px;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background: #f5f5f5;
  border: none;
  color: #666;
}

.cancel-btn:hover {
  background: #eee;
}

.confirm-btn {
  background: #ff2442;
  border: none;
  color: white;
}

.confirm-btn:hover {
  background: #ff3a56;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.3);
}
</style>
