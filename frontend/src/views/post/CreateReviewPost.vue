<template>
  <div class="create-review-page">
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <i class="bx bx-arrow-back"></i>
      </button>
      <h2>{{ editingPostId ? '编辑评价' : '发布评价' }}</h2>
      <div class="header-placeholder"></div>
    </div>

    <div class="post-form-container">
      <div class="form-section">
        <div class="form-group">
          <label>选择活动 <span class="required">*</span></label>
          <div class="activity-selector" @click="showActivityPicker = true">
            <div v-if="postForm.activityId" class="selected-activity">
              <i class="bx bx-calendar-event"></i>
              <span>{{ getSelectedActivityName() }}</span>
            </div>
            <div v-else class="placeholder">
              <i class="bx bx-calendar-plus"></i>
              <span>请选择已完成的活动</span>
            </div>
            <i class="bx bx-chevron-right"></i>
          </div>
        </div>

        <div v-if="postForm.activityInfo" class="activity-link-card" @click="openLinkedActivity">
          <img :src="postForm.activityInfo.coverImage || defaultCover" alt="活动封面" class="activity-link-cover" />
          <div class="activity-link-copy">
            <span class="activity-link-label">活动卡片</span>
            <strong>{{ postForm.activityInfo.title || '关联活动' }}</strong>
            <p>{{ postForm.activityInfo.location || '评价数据将计入该活动评分' }}</p>
          </div>
          <i class="bx bx-link-external"></i>
        </div>

        <div class="form-group">
          <label>评分 <span class="required">*</span></label>
          <div class="score-section">
            <div class="score-stars">
              <span
                v-for="star in 5"
                :key="star"
                class="star"
                :class="{ active: postForm.score >= star }"
                @click="postForm.score = star"
              >
                <i class="bx bxs-star"></i>
              </span>
            </div>
            <span class="score-text">{{ getScoreText() }}</span>
          </div>
        </div>

        <div class="form-group">
          <label>标题</label>
          <input v-model="postForm.title" type="text" placeholder="为你的评价起个标题" maxlength="50" />
          <span class="char-count">{{ postForm.title.length }}/50</span>
        </div>

        <div class="form-group">
          <label>评价内容</label>
          <textarea v-model="postForm.content" placeholder="分享你的真实体验" maxlength="1000"></textarea>
          <span class="char-count">{{ postForm.content.length }}/1000</span>
        </div>

        <div class="form-group">
          <label>添加图片 <span class="hint">最多9张</span></label>
          <div class="upload-area" @click="triggerImageUpload">
            <i class="bx bx-image-add"></i>
            <span>点击上传图片</span>
            <input ref="imageInput" type="file" multiple accept="image/*" hidden @change="handleImageUpload" />
          </div>
          <div v-if="postForm.images.length" class="media-grid">
            <div v-for="(image, index) in postForm.images" :key="`${image}-${index}`" class="media-card">
              <img :src="resolvePreview(image)" alt="预览" />
              <button class="remove-media-btn" type="button" @click="removeImage(index)">
                <i class="bx bx-x"></i>
              </button>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label>添加视频</label>
          <div class="upload-area upload-area-video" @click="triggerVideoUpload">
            <i class="bx bx-video-plus"></i>
            <span>{{ videoPreviewUrl ? '替换视频' : '点击上传视频' }}</span>
            <input ref="videoInput" type="file" accept="video/*" hidden @change="handleVideoUpload" />
          </div>
          <div v-if="videoPreviewUrl" class="video-preview-card">
            <video class="preview-video" controls preload="metadata" playsinline :poster="postForm.images[0] ? resolvePreview(postForm.images[0]) : ''">
              <source :src="videoPreviewUrl" />
            </video>
            <div class="video-preview-actions">
              <span class="video-url">{{ videoPreviewUrl }}</span>
              <button class="danger-text-btn" type="button" @click="removeVideo">移除视频</button>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label>标签 <span class="hint">最多10个</span></label>
          <div class="tags-container">
            <div v-if="postForm.tags.length" class="selected-tags">
              <span v-for="tag in postForm.tags" :key="tag" class="selected-tag-item">
                #{{ tag }}
                <i class="bx bx-x" @click="removeTag(tag)"></i>
              </span>
            </div>
            <div class="preset-tags">
              <span
                v-for="tag in presetTags"
                :key="tag"
                class="tag-item"
                :class="{ active: postForm.tags.includes(tag), disabled: !postForm.tags.includes(tag) && postForm.tags.length >= 10 }"
                @click="toggleTag(tag)"
              >
                #{{ tag }}
              </span>
            </div>
            <div class="tag-input-container">
              <input
                v-model="customTagsInput"
                type="text"
                placeholder="输入自定义标签，用逗号或空格分隔"
                :disabled="postForm.tags.length >= 10"
                @keyup.enter="addCustomTags"
              />
              <button class="add-tag-btn" type="button" :disabled="postForm.tags.length >= 10" @click="addCustomTags">添加</button>
            </div>
          </div>
        </div>

        <div class="toggle-row">
          <label class="checkbox-label">
            <input type="checkbox" v-model="postForm.isAnonymous" />
            <span class="checkbox-custom"></span>
            <span>匿名评价</span>
          </label>
        </div>
      </div>

      <aside class="review-rail">
        <div class="rail-card">
          <span class="rail-label">发布说明</span>
          <p>评价贴会保留评分与活动关联，匿名和私密只影响前台展示。</p>
        </div>

        <div v-if="postForm.activityInfo" class="rail-card rail-card--highlight">
          <span class="rail-label">当前关联</span>
          <strong>{{ postForm.activityInfo.title || '活动' }}</strong>
          <p>{{ postForm.activityInfo.location || '活动关联成功' }}</p>
        </div>

        <button class="submit-btn" :disabled="submitting" @click="submitPost">
          <i v-if="submitting" class="bx bx-loader-alt bx-spin"></i>
          {{ submitting ? '发布中...' : (editingPostId ? '保存修改' : '发布评价') }}
        </button>
      </aside>
    </div>

    <Transition name="modal">
      <div v-if="showActivityPicker" class="activity-picker-overlay" @click.self="showActivityPicker = false">
        <div class="activity-picker">
          <div class="picker-header">
            <h3>选择活动</h3>
            <button class="close-btn" type="button" @click="showActivityPicker = false">
              <i class="bx bx-x"></i>
            </button>
          </div>
          <div class="picker-content">
            <div v-if="loadingActivities" class="loading-state">
              <i class="bx bx-loader-alt bx-spin"></i>
              <span>加载中...</span>
            </div>
            <div v-else-if="availableActivities.length === 0" class="empty-state">
              <i class="bx bx-calendar-x"></i>
              <span>暂无符合条件的活动</span>
            </div>
            <div v-else class="activity-list">
              <div
                v-for="activity in availableActivities"
                :key="activity.id"
                class="activity-item"
                :class="{ selected: postForm.activityId === activity.id }"
                @click="selectActivity(activity)"
              >
                <img :src="activity.coverImage || defaultCover" alt="封面" class="activity-cover" />
                <div class="activity-info">
                  <h4>{{ activity.title }}</h4>
                  <p>{{ activity.location }}</p>
                </div>
                <i v-if="postForm.activityId === activity.id" class="bx bx-check-circle"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createDiscoverReviewPost,
  getDiscoverPostDetail,
  getMyOrderOverview,
  updateDiscoverReviewPost,
  uploadPostImage,
  uploadVideo
} from '../../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const router = useRouter()
const route = useRoute()

const defaultCover = 'https://api.dicebear.com/7.x/shapes/svg?seed=activity'
const presetTags = computed(() => ['体验很棒', '强烈推荐', '环境优美', '服务专业', '值得一去'])

const postForm = ref({
  title: '',
  content: '',
  tags: [],
  images: [],
  activityId: null,
  activityInfo: null,
  score: 0,
  isAnonymous: false
})
const imageInput = ref(null)
const videoInput = ref(null)
const pendingFilesMap = ref(new Map())
const customTagsInput = ref('')
const submitting = ref(false)
const showActivityPicker = ref(false)
const availableActivities = ref([])
const loadingActivities = ref(false)
const videoFile = ref(null)
const videoPreviewUrl = ref('')
const editingPostId = ref('')
const selectedBookingId = ref('')

const goBack = () => {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }
  router.back()
}

const resolvePreview = (value) => {
  if (!value) return defaultCover
  if (String(value).startsWith('blob:')) return value
  return value
}

const getSelectedActivityName = () => postForm.value.activityInfo?.title || availableActivities.value.find(item => item.id === postForm.value.activityId)?.title || ''
const getScoreText = () => ['', '很差', '较差', '一般', '满意', '非常满意'][postForm.value.score] || '点击评分'

const openLinkedActivity = () => {
  if (!postForm.value.activityId) return
  router.push({
    path: `/activity/${postForm.value.activityId}`,
    query: { backTo: route.fullPath }
  })
}

const selectActivity = (activity) => {
  postForm.value.activityId = activity.id
  postForm.value.activityInfo = {
    id: activity.id,
    title: activity.title,
    coverImage: activity.coverImage,
    location: activity.location
  }
  selectedBookingId.value = activity.bookingId ? String(activity.bookingId) : ''
  showActivityPicker.value = false
}

const loadAvailableActivities = async () => {
  loadingActivities.value = true
  try {
    const response = await getMyOrderOverview()
    if (response.code !== 200) {
      throw new Error(response.message || '加载活动失败')
    }
    const bookings = Array.isArray(response.data?.activityBookings) ? response.data.activityBookings : []
    const routeBookingId = Number(route.query.bookingId || 0)
    const routeActivityId = Number(route.query.activityId || 0)
    const seen = new Set()
    availableActivities.value = bookings
      .filter(item => item.canReview)
      .filter(item => !seen.has(Number(item.activityId || 0)) && seen.add(Number(item.activityId || 0)))
      .map(item => ({
        bookingId: item.id,
        id: item.activityId,
        title: item.activityTitle || '活动',
        coverImage: item.activityCoverImage || item.coverImage || defaultCover,
        location: [item.locationProvince, item.locationCity, item.locationDistrict, item.locationDetail].filter(Boolean).join(' / ') || '地点待定'
      }))

    if (routeBookingId) {
      const matchedBooking = bookings.find(item => Number(item.id || 0) === routeBookingId && item.canReview)
      if (matchedBooking) {
        selectActivity({
          bookingId: matchedBooking.id,
          id: matchedBooking.activityId,
          title: matchedBooking.activityTitle || '活动',
          coverImage: matchedBooking.activityCoverImage || matchedBooking.coverImage || defaultCover,
          location: [matchedBooking.locationProvince, matchedBooking.locationCity, matchedBooking.locationDistrict, matchedBooking.locationDetail].filter(Boolean).join(' / ') || '地点待定'
        })
      }
    } else if (routeActivityId) {
      const matched = availableActivities.value.find(item => Number(item.id) === routeActivityId)
      if (matched) {
        selectActivity(matched)
      }
    }
  } catch (error) {
    notify.error(error.message || '加载活动失败')
  } finally {
    loadingActivities.value = false
  }
}

const loadEditingPost = async (postId) => {
  if (!postId) return
  const response = await getDiscoverPostDetail(postId)
  if (response.code !== 200 || !response.data) {
    throw new Error(response.message || '加载评价失败')
  }
  const post = response.data
  editingPostId.value = String(post.id || '')
  postForm.value.title = post.title || ''
  postForm.value.content = post.content || ''
  postForm.value.tags = Array.isArray(post.hashtags) ? [...post.hashtags] : []
  postForm.value.images = Array.isArray(post.images) ? [...post.images] : []
  postForm.value.activityId = post.activityId || null
  postForm.value.activityInfo = post.activityInfo || (post.activityId ? {
    id: post.activityId,
    title: post.activityInfo?.title || route.query.activityTitle || '',
    coverImage: post.activityInfo?.coverImage || '',
    location: post.activityInfo?.location || ''
  } : null)
  postForm.value.score = Number(post.score || 0)
  postForm.value.isAnonymous = Boolean(post.isAnonymous)
  selectedBookingId.value = String(route.query.bookingId || '')
}

const handleImageUpload = async (event) => {
  const input = event.target
  const files = input?.files ? Array.from(input.files) : []
  if (!files.length) return

  const remainingSlots = 9 - postForm.value.images.length
  if (remainingSlots <= 0) {
    notify.warning('最多上传9张图片')
    if (input) input.value = ''
    return
  }

  files.slice(0, remainingSlots).forEach(file => {
    const previewUrl = URL.createObjectURL(file)
    postForm.value.images.push(previewUrl)
    pendingFilesMap.value.set(previewUrl, file)
  })

  if (input) input.value = ''
}

const removeImage = (index) => {
  const previewUrl = postForm.value.images[index]
  if (previewUrl?.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl)
    pendingFilesMap.value.delete(previewUrl)
  }
  postForm.value.images.splice(index, 1)
}

const handleVideoUpload = (event) => {
  const input = event.target
  const file = input?.files?.[0]
  if (!file) return
  if (!file.type.startsWith('video/')) {
    notify.warning('请选择视频文件')
    if (input) input.value = ''
    return
  }
  if (file.size > 100 * 1024 * 1024) {
    notify.warning('视频大小不能超过100MB')
    if (input) input.value = ''
    return
  }

  if (videoPreviewUrl.value?.startsWith('blob:')) {
    URL.revokeObjectURL(videoPreviewUrl.value)
  }
  videoFile.value = file
  videoPreviewUrl.value = URL.createObjectURL(file)

  if (input) input.value = ''
}

const removeVideo = () => {
  if (videoPreviewUrl.value?.startsWith('blob:')) {
    URL.revokeObjectURL(videoPreviewUrl.value)
  }
  videoFile.value = null
  videoPreviewUrl.value = ''
}

const triggerImageUpload = () => imageInput.value?.click()
const triggerVideoUpload = () => videoInput.value?.click()

const toggleTag = (tag) => {
  const current = postForm.value.tags
  const exists = current.includes(tag)
  if (!exists && current.length >= 10) {
    notify.warning('最多添加10个标签')
    return
  }
  postForm.value.tags = exists ? current.filter(item => item !== tag) : [...current, tag]
}

const addCustomTags = () => {
  const tags = customTagsInput.value.split(/[\s,，]+/).map(item => item.trim()).filter(Boolean)
  if (!tags.length) return

  const merged = [...postForm.value.tags]
  tags.forEach(tag => {
    if (merged.length >= 10) return
    if (!merged.includes(tag)) merged.push(tag)
  })
  postForm.value.tags = merged
  customTagsInput.value = ''
}

const removeTag = (tag) => {
  postForm.value.tags = postForm.value.tags.filter(item => item !== tag)
}

const buildFinalImages = async () => {
  const urls = []
  let index = 1
  for (const image of postForm.value.images) {
    if (String(image).startsWith('blob:')) {
      const file = pendingFilesMap.value.get(image)
      if (!file) continue
      const formData = new FormData()
      formData.append('file', file)
      const response = await uploadPostImage(formData, editingPostId.value || undefined, index)
      if (response.code === 200 && response.data?.url) {
        urls.push(response.data.url)
        index += 1
      } else {
        throw new Error(response.message || '图片上传失败')
      }
    } else {
      urls.push(image)
    }
  }
  return urls
}

const submitPost = async () => {
  if (!postForm.value.activityId) {
    notify.warning('请选择活动')
    return
  }
  if (!postForm.value.score) {
    notify.warning('请选择评分')
    return
  }

  submitting.value = true
  try {
    const finalImages = await buildFinalImages()
    let videoUrl = ''
    if (videoFile.value) {
      const formData = new FormData()
      formData.append('file', videoFile.value)
      const response = await uploadVideo(formData, 'posts/videos')
      if (response.code === 200 && response.data?.url) {
        videoUrl = response.data.url
      } else {
        throw new Error(response.message || '视频上传失败')
      }
    }

    const payload = {
      title: postForm.value.title.trim() || `${getSelectedActivityName() || '活动'} - 评价`,
      content: postForm.value.content.trim(),
      tags: postForm.value.tags,
      images: [...finalImages, ...(videoUrl ? [videoUrl] : [])],
      videos: videoUrl ? [videoUrl] : [],
      type: 'review',
      activityId: postForm.value.activityId,
      bookingId: selectedBookingId.value || undefined,
      score: postForm.value.score,
      isAnonymous: postForm.value.isAnonymous,
      visibility: 'public'
    }

    let response
    if (editingPostId.value) {
      response = await updateDiscoverReviewPost(editingPostId.value, payload)
    } else {
      response = await createDiscoverReviewPost(payload)
    }

    if (response.code !== 200) {
      throw new Error(response.message || '发布失败')
    }

    notify.success(editingPostId.value ? '保存成功' : '评价发布成功')
    if (typeof route.query.backTo === 'string' && route.query.backTo) {
      router.push(route.query.backTo)
    } else {
      router.push('/personal/my-reviews')
    }
  } catch (error) {
    notify.error(error.message || '发布失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadAvailableActivities()
  if (route.query.postId) {
    try {
      await loadEditingPost(route.query.postId)
    } catch (error) {
      notify.error(error.message || '加载评价失败')
    }
  }
})
</script>

<style scoped>
.create-review-page {
  min-height: 100vh;
  background: linear-gradient(180deg, rgba(248, 244, 238, 0.3), rgba(255, 255, 255, 0.95));
  padding-bottom: 100px;
}

.page-header {
  position: sticky;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(204, 175, 145, 0.2);
  z-index: 10;
}

.back-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: none;
  background: rgba(157, 41, 41, 0.08);
  color: #7b6a59;
  font-size: 22px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-header h2 {
  font-size: 18px;
  font-weight: 700;
  color: #2f241d;
  margin: 0;
}

.header-placeholder {
  width: 40px;
}

.post-form-container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 20px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 250px;
  gap: 20px;
  align-items: start;
}

.form-section {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 1fr);
  gap: 18px 20px;
  align-items: start;
}

.review-rail {
  position: sticky;
  top: 92px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rail-card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  border: 1px solid rgba(204, 175, 145, 0.24);
  padding: 16px;
  box-shadow: 0 10px 24px rgba(77, 51, 30, 0.08);
}

.rail-card--highlight {
  border-color: rgba(157, 41, 41, 0.22);
  background: linear-gradient(180deg, rgba(255, 247, 242, 0.98), rgba(255, 255, 255, 0.95));
}

.rail-label {
  display: inline-block;
  margin-bottom: 8px;
  color: #9d2929;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.rail-card strong {
  display: block;
  color: #2f241d;
  font-size: 15px;
  margin-bottom: 6px;
}

.rail-card p {
  margin: 0;
  color: #7b6a59;
  font-size: 13px;
  line-height: 1.6;
}

.activity-link-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 0;
  padding: 12px;
  border-radius: 18px;
  border: 1px solid rgba(157, 41, 41, 0.14);
  background: linear-gradient(135deg, rgba(157, 41, 41, 0.05), rgba(255, 255, 255, 0.94));
  cursor: pointer;
  transition: all 0.2s ease;
  grid-column: 1 / -1;
}

.form-section > .form-group:first-of-type {
  grid-column: 1 / -1;
}

.activity-link-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(77, 51, 30, 0.08);
}

.activity-link-cover {
  width: 76px;
  height: 58px;
  object-fit: cover;
  border-radius: 12px;
  flex-shrink: 0;
}

.activity-link-copy {
  flex: 1;
  min-width: 0;
}

.activity-link-label {
  display: inline-block;
  margin-bottom: 4px;
  color: #9d2929;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.activity-link-copy strong {
  display: block;
  color: #2f241d;
  font-size: 15px;
  margin-bottom: 4px;
}

.activity-link-copy p {
  margin: 0;
  color: #7b6a59;
  font-size: 12px;
  line-height: 1.5;
}

.activity-link-card > i {
  color: #c9913f;
  font-size: 18px;
}

.form-group {
  margin-bottom: 0;
}

label {
  display: block;
  font-weight: 600;
  color: #2f241d;
  margin-bottom: 10px;
}

.required,
.hint {
  color: #9b7b64;
  font-weight: 400;
  font-size: 13px;
}

input,
textarea {
  width: 100%;
  border: 1px solid #e7ddd3;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
  color: #2f241d;
  box-sizing: border-box;
}

textarea {
  min-height: 140px;
  resize: vertical;
}

.char-count {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #a18a76;
  text-align: right;
}

.activity-selector,
.upload-area {
  min-height: 130px;
  border: 1px dashed #d7c9bb;
  border-radius: 18px;
  background: #fff;
  color: #7b6a59;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
}

.activity-selector {
  flex-direction: row;
  justify-content: space-between;
  padding: 16px 18px;
}

.selected-activity,
.placeholder {
  display: flex;
  align-items: center;
  gap: 10px;
}

.upload-area i,
.activity-selector i {
  font-size: 28px;
  color: #b65c38;
}

.score-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-stars {
  display: flex;
  gap: 8px;
}

.star {
  font-size: 24px;
  color: #d8d8d8;
  cursor: pointer;
}

.star.active {
  color: #f5a623;
}

.score-text {
  color: #7b6a59;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.media-card,
.video-preview-card {
  position: relative;
  aspect-ratio: 3 / 4;
  background: #fff;
  border: 1px solid #eadfd4;
  border-radius: 16px;
  overflow: hidden;
}

.media-card img,
.preview-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
}

.remove-media-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  cursor: pointer;
}

.video-preview-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.video-url {
  font-size: 12px;
  color: #7b6a59;
  word-break: break-all;
}

.danger-text-btn,
.add-tag-btn {
  border: none;
  border-radius: 999px;
  padding: 10px 14px;
  cursor: pointer;
}

.danger-text-btn {
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
}

.tags-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selected-tags,
.preset-tags,
.activity-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.selected-tag-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(157, 41, 41, 0.1);
  color: #9d2929;
}

.tag-item {
  border: 1px solid #eadfd4;
  border-radius: 12px;
  padding: 10px 12px;
  text-align: center;
  cursor: pointer;
  background: #fff;
}

.tag-item.active {
  background: #9d2929;
  color: #fff;
  border-color: #9d2929;
}

.tag-item.disabled {
  opacity: 0.45;
  pointer-events: none;
}

.tag-input-container {
  display: flex;
  gap: 10px;
}

.add-tag-btn {
  background: #9d2929;
  color: #fff;
  min-width: 70px;
}

.toggle-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  grid-column: 1 / -1;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  white-space: nowrap;
}

.checkbox-label input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.checkbox-label span {
  flex-shrink: 0;
}

.checkbox-custom {
  width: 16px;
  height: 16px;
  border: 1px solid #e7ddd3;
  border-radius: 4px;
  background: #fff;
  flex-shrink: 0;
}

.checkbox-label input:checked + .checkbox-custom {
  background: #9d2929;
  border-color: #9d2929;
  position: relative;
}

.checkbox-label input:checked + .checkbox-custom::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 5px;
  width: 4px;
  height: 8px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.submit-btn {
  width: 100%;
  margin-top: 0;
  padding: 14px 20px;
  background: linear-gradient(135deg, #b65c38 0%, #d28d44 100%);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}

.activity-picker-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  padding: 20px;
}

.activity-picker {
  width: min(720px, 100%);
  max-height: 80vh;
  overflow: auto;
  background: #fff;
  border-radius: 20px;
  padding: 20px;
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.close-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
}

.picker-content,
.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #eadfd4;
  border-radius: 16px;
  padding: 10px;
  cursor: pointer;
}

.activity-item.selected {
  border-color: #9d2929;
  background: rgba(157, 41, 41, 0.06);
}

.activity-cover {
  width: 72px;
  height: 54px;
  object-fit: cover;
  border-radius: 12px;
}

.activity-info h4,
.activity-info p {
  margin: 0;
}

@media (max-width: 640px) {
  .post-form-container {
    grid-template-columns: 1fr;
  }

  .form-section {
    grid-template-columns: 1fr;
  }

  .review-rail {
    position: static;
  }

  .post-form-container {
    padding: 14px;
  }

  .form-section {
    padding: 18px;
  }

  .tag-input-container {
    flex-direction: column;
  }

  .category-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

