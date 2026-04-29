<template>
  <div class="ai-step-page">
    <div class="bg-orb orb-left"></div>
    <div class="bg-orb orb-right"></div>

    <div class="page-topbar">
      <button class="back-btn" type="button" @click="goBack">
        <i class="bx bx-arrow-back"></i>
      </button>
    </div>

    <section class="hero-panel">
      <div>
        <p class="hero-eyebrow">发布新帖子</p>
        <h1>分享你的精彩瞬间</h1>
      </div>
    </section>

    <section class="single-shell">
      <article class="glass-card post-card">
        <div class="post-form-horizontal">
          <div class="post-form-left">
            <div class="form-group">
              <label>标题</label>
              <input v-model="postForm.title" type="text" placeholder="标题" />
            </div>
            <div class="form-group">
              <label>内容</label>
              <textarea v-model="postForm.content" placeholder="内容"></textarea>
            </div>
            <div class="form-group">
              <label>分类</label>
              <select v-model="postForm.category">
                <option v-for="category in categories.filter(item => item.id !== 'all')" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
            </div>
          </div>
          
          <div class="post-form-right">
            <div class="form-group">
              <label>添加图片</label>
              <div class="image-upload-area" @click="$refs.imageInput.click()">
                <i class='bx bx-plus-circle'></i>
                <span>添加图片</span>
                <input 
                  ref="imageInput"
                  type="file" 
                  multiple 
                  accept="image/*" 
                  @change="handleImageUpload"
                  style="display: none;"
                />
              </div>
              <div v-if="selectedImages.length > 0" class="selected-images">
                <div 
                  v-for="(image, index) in selectedImages" 
                  :key="index"
                  class="image-preview-item"
                  draggable="true"
                  @dragstart="onDragStart($event, index)"
                  @dragover.prevent="onDragOver($event)"
                  @drop="onDrop($event, index)"
                  @dragend="onDragEnd($event)"
                  @click="previewImage(image)"
                  @touchstart="onTouchStart($event, index)"
                  @touchmove.prevent="onTouchMove($event)"
                  @touchend="onTouchEnd($event, index)"
                  style="touch-action: none; -webkit-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;"
                >
                  <img :src="image" alt="Preview" class="preview-image" />
                  <button class="remove-image-btn" @click.stop="removeImage(index)">
                    <i class='bx bx-x'></i>
                  </button>
                  <div class="drag-handle">
                    <i class='bx bx-move'></i>
                  </div>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label>标签 <span class="tag-count">({{ postForm.tags.length }}/10)</span></label>
              <div class="tags-container">
                <div v-if="postForm.tags.length > 0" class="selected-tags">
                  <span
                    v-for="tag in postForm.tags"
                    :key="tag"
                    class="selected-tag-item"
                  >
                    #{{ tag }}
                    <i class='bx bx-x' @click="removeTag(tag)"></i>
                  </span>
                </div>
                <div class="preset-tags">
                  <span
                    v-for="tag in presetTags"
                    :key="tag"
                    class="tag-item"
                    :class="{ active: postForm.tags.includes(tag), disabled: !postForm.tags.includes(tag) && postForm.tags.length >= 10 }"
                    @click="toggleTag(tag)"
                  >#{{ tag }}</span>
                </div>
                <div class="tag-input-container">
                  <input 
                    v-model="customTagsInput" 
                    type="text" 
                    placeholder="添加标签" 
                    @keyup.enter="addCustomTags"
                    :disabled="postForm.tags.length >= 10"
                  />
                  <div class="tag-buttons">
                    <button class="tag-btn" @click.prevent="addCustomTags" :disabled="postForm.tags.length >= 10">添加</button>
                  </div>
                </div>
              </div>
            </div>
            <button class="submit-post-btn" :disabled="submittingPost" @click="submitPost">
              {{ submittingPost ? '发布中...' : '发布' }}
            </button>
          </div>
        </div>
      </article>
    </section>

    <div v-if="showImagePreview" class="image-preview-modal">
      <div class="modal-overlay" @click="closeImagePreview"></div>
      <div class="modal-content">
        <button class="close-btn" @click="closeImagePreview">
          <i class='bx bx-x'></i>
        </button>
        <img :src="previewImageSrc" alt="Preview" class="preview-image-full" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createDiscoverPost,
  uploadPostImage,
  updateDiscoverPost,
  classifyDiscoverImage,
  proxyAiImage
} from '../../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const route = useRoute()

const emit = defineEmits(['post-created', 'cancel'])

const props = defineProps({
  initialForm: {
    type: Object,
    default: () => ({
      title: '',
      content: '',
      category: '服饰妆造',
      tags: [],
      images: []
    })
  }
})

const categories = computed(() => [
  { id: 'all', name: '全部' },
  { id: '服饰妆造', name: '服饰妆造' },
  { id: '美术造物', name: '美术造物' },
  { id: '民俗节气', name: '民俗节气' },
  { id: '戏曲演绎', name: '戏曲演绎' },
  { id: '织物手工', name: '织物手工' },
  { id: '其他', name: '其他' }
])

const postForm = ref({
  title: props.initialForm.title,
  content: props.initialForm.content,
  category: props.initialForm.category,
  tags: [...props.initialForm.tags],
  images: [...props.initialForm.images]
})

const pendingFilesMap = ref(new Map())
const customTagsInput = ref('')
const submittingPost = ref(false)
const draggedIndex = ref(null)
const showImagePreview = ref(false)
const previewImageSrc = ref('')

const touchStartX = ref(0)
const touchStartY = ref(0)
const touchDragged = ref(false)
const touchDraggedIndex = ref(null)

const presetTags = computed(() => [
  '服饰妆造',
  '美术造物',
  '民俗节气',
  '戏曲演绎',
  '织物手工'
])

const classifierTagMap = computed(() => ({
  '服饰妆造': '服饰妆造',
  '美术造物': '美术造物',
  '民俗节气': '民俗节气',
  '戏曲演绎': '戏曲演绎',
  '织物手工': '织物手工'
}))

const bestClassification = ref({
  tag: '',
  confidence: 0
})

const selectedImages = computed(() => postForm.value.images)

const mergeTags = (incomingTags = []) => {
  if (!Array.isArray(incomingTags) || incomingTags.length === 0) {
    return
  }

  const merged = [...postForm.value.tags]
  incomingTags
    .map(tag => (typeof tag === 'string' ? tag.trim() : ''))
    .map(tag => classifierTagMap.value[tag])
    .filter(Boolean)
    .forEach(tag => {
      if (merged.length < 10 && !merged.includes(tag)) {
        merged.push(tag)
      }
    })

  postForm.value.tags = merged
}

const syncCategoryFromPrediction = (primaryTag, confidence = 0) => {
  const normalizedTag = typeof primaryTag === 'string' ? primaryTag.trim() : ''
  if (!normalizedTag) {
    return
  }

  if (confidence < bestClassification.value.confidence) {
    return
  }

  bestClassification.value = {
    tag: normalizedTag,
    confidence
  }
  
  postForm.value.category = normalizedTag in classifierTagMap.value ? normalizedTag : '其他'
}

const classifySelectedImage = async (file) => {
  if (!file) {
    return
  }

  const formData = new FormData()
  formData.append('image', file)

  try {
    const response = await classifyDiscoverImage(formData)
    if (response.code === 200) {
      const primaryTag = typeof response.data?.primaryTag === 'string' ? response.data.primaryTag.trim() : ''
      const confidence = Number(response.data?.confidence || 0)
      const autoTags = Array.isArray(response.data?.autoTags) ? response.data.autoTags : []
      syncCategoryFromPrediction(primaryTag, confidence)
      mergeTags(autoTags)
      return
    }

    if (response.code === 503) {
      console.warn('本地图片分类器不可用。', response.message)
    }
  } catch (error) {
    console.error('发现页图片分类失败。', error)
  }
}

const handleImageUpload = (event) => {
  const files = Array.from(event.target.files || [])
  if (files.length === 0) return

  const remainingSlots = 9 - postForm.value.images.length
  if (remainingSlots <= 0) {
    notify.warning('最多上传9张图片')
    if (event.target) {
      event.target.value = ''
    }
    return
  }

  const filesToUpload = files.slice(0, remainingSlots)
  const classifyTasks = []

  filesToUpload.forEach(file => {
    const previewUrl = URL.createObjectURL(file)
    postForm.value.images.push(previewUrl)
    pendingFilesMap.value.set(previewUrl, file)
    classifyTasks.push(classifySelectedImage(file))
  })

  if (event.target) {
    event.target.value = ''
  }

  Promise.allSettled(classifyTasks)
}

const removeImage = (index) => {
  const previewUrl = postForm.value.images[index]
  if (previewUrl && previewUrl.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl)
    pendingFilesMap.value.delete(previewUrl)
  }
  postForm.value.images = postForm.value.images.filter((_, i) => i !== index)
}

const resetPostForm = () => {
  postForm.value.images.forEach(url => {
    if (url && url.startsWith('blob:')) {
      URL.revokeObjectURL(url)
    }
  })
  postForm.value = {
    title: '',
    content: '',
    category: '服饰妆造',
    tags: [],
    images: []
  }
  pendingFilesMap.value = new Map()
  bestClassification.value = {
    tag: '',
    confidence: 0
  }
  customTagsInput.value = ''
}

const onDragStart = (event, index) => {
  draggedIndex.value = index
  event.target.style.opacity = '0.5'
}

const onDragOver = (event) => {
  event.preventDefault()
}

const onDrop = (event, dropIndex) => {
  event.preventDefault()
  if (draggedIndex.value !== null && draggedIndex.value !== dropIndex) {
    const images = [...postForm.value.images]
    const [draggedImage] = images.splice(draggedIndex.value, 1)
    images.splice(dropIndex, 0, draggedImage)
    postForm.value.images = images
    
    if (draggedImage.startsWith('blob:')) {
      const files = [...pendingFilesMap.value.entries()]
      const [key, value] = files.splice(draggedIndex.value, 1)[0]
      files.splice(dropIndex, 0, [key, value])
      pendingFilesMap.value = new Map(files)
    }
  }
  draggedIndex.value = null
  event.target.style.opacity = '1'
}

const onDragEnd = (event) => {
  event.target.style.opacity = '1'
  draggedIndex.value = null
}

const onTouchStart = (event, index) => {
  const touch = event.touches[0]
  touchStartX.value = touch.clientX
  touchStartY.value = touch.clientY
  touchDragged.value = false
  touchDraggedIndex.value = index
}

const onTouchMove = (event) => {
  if (!touchDraggedIndex.value) return
  
  const touch = event.touches[0]
  const deltaX = Math.abs(touch.clientX - touchStartX.value)
  const deltaY = Math.abs(touch.clientY - touchStartY.value)
  
  if (deltaX > 10 || deltaY > 10) {
    touchDragged.value = true
  }
}

const onTouchEnd = (event, index) => {
  if (touchDragged.value && touchDraggedIndex.value !== null && touchDraggedIndex.value !== index) {
    const images = [...postForm.value.images]
    const [draggedImage] = images.splice(touchDraggedIndex.value, 1)
    images.splice(index, 0, draggedImage)
    postForm.value.images = images
    
    if (draggedImage.startsWith('blob:')) {
      const files = [...pendingFilesMap.value.entries()]
      const [key, value] = files.splice(touchDraggedIndex.value, 1)[0]
      files.splice(index, 0, [key, value])
      pendingFilesMap.value = new Map(files)
    }
  }
  
  touchDragged.value = false
  touchDraggedIndex.value = null
}

const previewImage = (imageUrl) => {
  previewImageSrc.value = imageUrl
  showImagePreview.value = true
}

const closeImagePreview = () => {
  showImagePreview.value = false
  previewImageSrc.value = ''
}

const toggleTag = (tag) => {
  const index = postForm.value.tags.indexOf(tag)
  if (index === -1) {
    if (postForm.value.tags.length < 10) {
      postForm.value.tags.push(tag)
    }
  } else {
    postForm.value.tags.splice(index, 1)
  }
}

const removeTag = (tag) => {
  const index = postForm.value.tags.indexOf(tag)
  if (index !== -1) {
    postForm.value.tags.splice(index, 1)
  }
}

const addCustomTags = () => {
  const input = customTagsInput.value.trim()
  if (!input) return
  
  const tags = input.split(/[,，]/).map(tag => tag.trim()).filter(Boolean)
  
  tags.forEach(tag => {
    if (postForm.value.tags.length < 10 && !postForm.value.tags.includes(tag)) {
      postForm.value.tags.push(tag)
    }
  })
  
  customTagsInput.value = ''
}

// 将远程图片URL转换为File对象
const urlToFile = async (url, filename) => {
  try {
    // 对于豆包API的图片，使用后端代理接口避免跨域
    if (url.includes('volces.com') || url.includes('doubao')) {
      const response = await proxyAiImage(url)
      if (response) {
        // 后端返回的是二进制图片数据(ArrayBuffer)
        const blob = new Blob([response], { type: 'image/png' })
        return new File([blob], filename, { type: 'image/png' })
      }
      throw new Error('代理下载图片失败')
    }
    
    // 其他图片直接下载
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error('图片下载失败')
    }
    const blob = await response.blob()
    return new File([blob], filename, { type: blob.type || 'image/png' })
  } catch (error) {
    console.error('转换图片URL为File失败:', error)
    throw error
  }
}

const submitPost = async () => {
  if (!postForm.value.title.trim()) {
    notify.warning('请输入标题')
    return
  }
  if (!postForm.value.content.trim()) {
    notify.warning('请输入内容')
    return
  }
  if (!postForm.value.images || postForm.value.images.length === 0) {
    notify.warning('请上传图片')
    return
  }
  submittingPost.value = true
  try {
    const finalTags = [...postForm.value.tags]
    
    const createResponse = await createDiscoverPost({
      title: postForm.value.title.trim(),
      content: postForm.value.content.trim(),
      category: postForm.value.category,
      tags: finalTags,
      images: []
    })
    if (createResponse.code !== 200) {
      notify.error(createResponse.message || '发布失败')
      return
    }
    const postId = createResponse.data.id

    const ossUrls = []
    let index = 1
    for (const previewUrl of postForm.value.images) {
      let file = pendingFilesMap.value.get(previewUrl)
      
      // 如果不是本地文件，可能是远程URL（如AI生成的图片），需要下载转换
      if (!file && previewUrl.startsWith('http')) {
        try {
          file = await urlToFile(previewUrl, `image_${index}.png`)
        } catch (error) {
          console.error('下载远程图片失败:', previewUrl, error)
          notify.warning('部分图片下载失败，请重试')
          continue
        }
      }
      
      if (file) {
        const formData = new FormData()
        formData.append('file', file)
        const response = await uploadPostImage(formData, postId, index)
        if (response.code === 200 && response.data && response.data.url) {
          ossUrls.push(response.data.url)
          index++
        } else {
          throw new Error(response.message || '图片上传失败')
        }
      }
    }

    const updateResponse = await updateDiscoverPost(postId, {
      title: postForm.value.title.trim(),
      content: postForm.value.content.trim(),
      category: postForm.value.category,
      tags: finalTags,
      images: ossUrls
    })
    if (updateResponse.code === 200) {
      notify.success('发布成功')
      resetPostForm()
      emit('post-created', updateResponse.data)
    } else {
      notify.error(updateResponse.message || '发布失败')
    }
  } catch (error) {
    notify.error('发布失败，请重试')
  } finally {
    submittingPost.value = false
  }
}

function goBack() {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }
  router.back()
}
</script>

<style scoped>
.ai-step-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 24px 24px 110px;
  background:
    radial-gradient(circle at top left, rgba(145, 56, 31, 0.14), transparent 30%),
    radial-gradient(circle at top right, rgba(15, 118, 110, 0.1), transparent 24%),
    linear-gradient(180deg, #f8f1e8 0%, #fffaf3 50%, #fbf3e8 100%);
}

.bg-orb {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.48;
  pointer-events: none;
}

.orb-left { left: -140px; top: -120px; background: rgba(157, 41, 41, 0.22); }
.orb-right { right: -140px; bottom: 80px; background: rgba(15, 118, 110, 0.16); }

.page-topbar,
.hero-panel,
.glass-card {
  max-width: 1120px;
  margin: 0 auto;
}

.page-topbar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.hero-panel,
.glass-card {
  border: 1px solid rgba(182, 143, 106, 0.24);
  border-radius: 28px;
  box-shadow: 0 18px 48px rgba(92, 61, 39, 0.09);
  backdrop-filter: blur(18px);
}

.hero-panel {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(249, 241, 229, 0.92));
  margin-bottom: 18px;
}

.back-btn {
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 14px;
  background: rgba(141, 35, 35, 0.1);
  color: #8d2323;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 22px;
}

.hero-eyebrow {
  margin: 0 0 10px;
  color: #8d2323;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero-panel h1 {
  margin: 0;
  font-size: clamp(28px, 4vw, 48px);
  color: #2d2118;
  line-height: 1.08;
}

.single-shell {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
}

.post-card {
  width: min(1120px, 100%);
  padding: 22px;
  background: rgba(255, 252, 246, 0.92);
}

.post-form-horizontal {
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
}

.post-form-left {
  flex: 1;
  min-width: 300px;
}

.post-form-right {
  flex: 1;
  min-width: 300px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(182, 143, 106, 0.24);
  border-radius: 12px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(92, 61, 39, 0.06);
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #9d2929;
  box-shadow: 0 0 0 3px rgba(157, 41, 41, 0.1);
  background: #fff;
}

.form-group textarea {
  resize: vertical;
  min-height: 150px;
  line-height: 1.5;
}

.image-upload-area {
  border: 2px dashed rgba(182, 143, 106, 0.24);
  border-radius: 16px;
  padding: 50px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: inset 0 2px 8px rgba(92, 61, 39, 0.06);
}

.image-upload-area:hover {
  border-color: #9d2929;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: inset 0 2px 12px rgba(92, 61, 39, 0.1);
}

.image-upload-area i {
  font-size: 56px;
  color: #9d2929;
  margin-bottom: 15px;
}

.image-upload-area span {
  display: block;
  color: #666;
  font-size: 16px;
  font-weight: 500;
}

.selected-images {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-preview-item {
  position: relative;
  width: 110px;
  height: 110px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(182, 143, 106, 0.24);
  cursor: move;
  box-shadow: 0 4px 12px rgba(92, 61, 39, 0.08);
  transition: all 0.3s ease;
}

.image-preview-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(92, 61, 39, 0.12);
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.remove-image-btn:hover {
  background: rgba(0, 0, 0, 0.9);
  transform: scale(1.1);
}

.drag-handle {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border: none;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: move;
  transition: all 0.3s ease;
}

.drag-handle:hover {
  background: rgba(0, 0, 0, 0.9);
}

.tag-count {
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

.tags-container {
  border: 1px solid rgba(182, 143, 106, 0.24);
  border-radius: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(92, 61, 39, 0.06);
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 15px;
}

.selected-tag-item {
  background: linear-gradient(135deg, #9d2929, #c2410c);
  color: white;
  padding: 6px 12px;
  border-radius: 18px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 2px 8px rgba(157, 41, 41, 0.2);
  transition: all 0.3s ease;
}

.selected-tag-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.3);
}

.selected-tag-item i {
  cursor: pointer;
  font-size: 12px;
  transition: transform 0.2s ease;
}

.selected-tag-item i:hover {
  transform: scale(1.2);
}

.preset-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.tag-item {
  padding: 6px 12px;
  border: 1px solid rgba(182, 143, 106, 0.24);
  border-radius: 18px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 4px rgba(92, 61, 39, 0.06);
}

.tag-item:hover:not(.disabled) {
  border-color: #9d2929;
  color: #9d2929;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(92, 61, 39, 0.1);
}

.tag-item.active {
  background: linear-gradient(135deg, #9d2929, #c2410c);
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.3);
}

.tag-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tag-input-container {
  display: flex;
  gap: 12px;
  align-items: center;
}

.tag-input-container input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid rgba(182, 143, 106, 0.24);
  border-radius: 10px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 4px rgba(92, 61, 39, 0.06);
}

.tag-btn {
  background: linear-gradient(135deg, #9d2929, #c2410c);
  color: white;
  border: none;
  border-radius: 10px;
  padding: 10px 20px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.2);
  min-width: 80px;
}

.tag-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(157, 41, 41, 0.3);
}

.tag-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

.submit-post-btn {
  width: 100%;
  background: linear-gradient(135deg, #9d2929, #c2410c);
  color: white;
  border: none;
  border-radius: 16px;
  padding: 16px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 8px 24px rgba(157, 41, 41, 0.3);
  margin-top: 10px;
}

.submit-post-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(157, 41, 41, 0.4);
}

.submit-post-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.image-preview-modal {
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
  backdrop-filter: blur(8px);
}

.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.modal-content {
  position: relative;
  max-width: 90%;
  max-height: 90%;
  z-index: 1001;
}

.close-btn {
  position: absolute;
  top: -40px;
  right: 0;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  color: white;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.1);
}

.preview-image-full {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

@media (max-width: 900px) {
  .hero-panel {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .post-form-horizontal {
    flex-direction: column;
  }
  
  .post-form-left,
  .post-form-right {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .ai-step-page {
    padding: 14px 14px 96px;
  }
  
  .hero-panel,
  .post-card {
    border-radius: 22px;
    padding: 18px;
  }
  
  .image-upload-area {
    padding: 40px 15px;
  }
  
  .image-upload-area i {
    font-size: 48px;
  }
  
  .image-preview-item {
    width: 90px;
    height: 90px;
  }
  
  .tags-container {
    padding: 16px;
  }
  
  .submit-post-btn {
    padding: 14px;
  }
}
</style>