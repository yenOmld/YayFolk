<template>
  <div class="my-posts-page">
    <div class="settings-header">
      <button class="back-btn" @click="goBack">
        <i class='bx bxs-chevron-left'></i>
      </button>
      <h1>{{ $t('myPosts.title') }}</h1>
    </div>

    <div class="posts-content">
      <div class="empty-state" v-if="posts.length === 0">
        <i class='bx bxs-edit-alt'></i>
        <h3>{{ $t('myPosts.emptyTitle') }}</h3>
        <p>{{ $t('myPosts.emptyHint') }}</p>
        <button class="btn primary" @click="goToTranslate">{{ $t('myPosts.goTranslate') }}</button>
      </div>

      <div class="posts-list" v-else>
        <div class="post-item" v-for="post in posts" :key="post.id" @click="viewPost(post)">
          <div class="post-images" v-if="post.images && post.images.length > 0">
            <img :src="post.images[0]" alt="Post image" class="post-cover" />
            <span v-if="post.images.length > 1" class="image-count">+{{ post.images.length - 1 }}</span>
          </div>
          <div class="post-content">
            <div class="post-status-row">
              <span :class="['audit-chip', post.auditStatus || 'pending']">{{ formatAuditStatus(post.auditStatus) }}</span>
            </div>
            <h4>{{ post.title }}</h4>
            <p class="post-text">{{ post.content }}</p>
            <div class="post-tags" v-if="post.hashtags && post.hashtags.length > 0">
              <span class="tag" v-for="(tag, index) in post.hashtags.slice(0, 3)" :key="index">#{{ tag }}</span>
            </div>
                        <p v-if="(post.auditStatus === 'rejected' || post.auditStatus === 'manual_review') && post.auditRemark" class="audit-remark">
              驳回原因：{{ post.auditRemark }}
            </p>
            <div class="post-meta">
              <div class="post-stats">
                <span class="stat-item">
                  <i class='bx bx-show'></i>
                  {{ post.viewCount || 0 }}
                </span>
                <span class="stat-item">
                  <i class='bx bx-star'></i>
                  {{ post.collects || 0 }}
                </span>
                <span class="stat-item">
                  <i class='bx bx-comment'></i>
                  {{ post.comments || 0 }}
                </span>
              </div>
              <span class="post-time">{{ post.time }}</span>
            </div>
          </div>
          <div class="post-actions" @click.stop>
            <button class="action-btn view-btn" @click="viewPost(post)" title="查看">
              <i class='bx bx-show'></i>
            </button>
            <button class="action-btn edit-btn" @click="openEditModal(post)" title="编辑">
              <i class='bx bx-edit'></i>
            </button>
            <button class="action-btn delete-btn" @click="deletePost(post.id)" title="删除">
              <i class='bx bxs-trash'></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <PostDetailModal 
      :visible="showDetailModal" 
      :post="currentPost" 
      @close="closeDetailModal"
      @update="handlePostUpdate"
    />

    <div v-if="showEditModal" class="edit-modal">
      <div class="modal-overlay" @click="closeEditModal"></div>
      <div class="modal-content">
        <div class="edit-header">
          <h2>{{ $t('myPosts.editPost') }}</h2>
          <button class="close-btn" @click="closeEditModal">
            <i class='bx bx-x'></i>
          </button>
        </div>
        <div class="post-form-horizontal">
          <div class="post-form-left">
            <div class="form-group">
              <label>{{ $t('discover.title') }}</label>
              <input v-model="editForm.title" type="text" :placeholder="$t('myPosts.inputTitle')" />
            </div>
            <div class="form-group">
              <label>{{ $t('discover.content') }}</label>
              <textarea v-model="editForm.content" :placeholder="$t('myPosts.shareStory')"></textarea>
            </div>
            <div class="form-group">
              <label>{{ $t('discover.category') }}</label>
              <select v-model="editForm.category">
                <option
                  v-for="category in categories"
                  :key="category.id"
                  :value="category.id"
                >
                  {{ category.name }}
                </option>
              </select>
            </div>
          </div>
          
          <div class="post-form-right">
            <div class="form-group">
              <label>{{ $t('discover.addImage') }}</label>
              <div class="image-upload-area" @click="$refs.editImageInput.click()">
                <i class='bx bx-plus-circle'></i>
                <span>点击上传图片</span>
                <input 
                  ref="editImageInput"
                  type="file" 
                  multiple 
                  accept="image/*" 
                  @change="handleImageUpload"
                  style="display: none;"
                />
              </div>
              <div v-if="editForm.images.length > 0" class="selected-images">
                <div 
                  v-for="(image, index) in editForm.images" 
                  :key="index"
                  class="image-preview-item"
                  draggable="true"
                  @dragstart="onDragStart($event, index)"
                  @dragover.prevent="onDragOver($event)"
                  @drop="onDrop($event, index)"
                  @dragend="onDragEnd($event)"
                  @click="previewImage(image)"
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
              <label>{{ $t('discover.tags') }} <span class="tag-count">({{ editForm.tags.length }}/10)</span></label>
              <div class="tags-container">
                <!-- 已选标签 -->
                <div v-if="editForm.tags.length > 0" class="selected-tags">
                  <span
                    v-for="tag in editForm.tags"
                    :key="tag"
                    class="selected-tag-item"
                  >
                    #{{ tag }}
                    <i class='bx bx-x' @click="removeTag(tag)"></i>
                  </span>
                </div>
                <!-- 预设标签 -->
                <div class="preset-tags">
                  <span
                    v-for="tag in presetTags"
                    :key="tag"
                    class="tag-item"
                    :class="{ active: editForm.tags.includes(tag), disabled: !editForm.tags.includes(tag) && editForm.tags.length >= 10 }"
                    @click="toggleTag(tag)"
                  >#{{ tag }}</span>
                </div>
                <!-- 自定义标签输入 -->
                <div class="tag-input-container">
                  <input 
                    v-model="customTagInput" 
                    type="text" 
                    :placeholder="$t('discover.addTagPlaceholder')" 
                    @keyup.enter="addCustomTags"
                    :disabled="editForm.tags.length >= 10"
                  />
                  <div class="tag-buttons">
                    <button class="tag-btn" @click.prevent="addCustomTags" :disabled="editForm.tags.length >= 10">{{ $t('discover.addTag') }}</button>
                  </div>
                </div>
              </div>
            </div>
            <button class="submit-post-btn" :disabled="saving" @click="saveEdit">
              {{ saving ? $t('editProfile.saving') : $t('common.save') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 图片预览模态框 -->
  <div v-if="showImagePreview" class="image-preview-modal">
    <div class="modal-overlay" @click="closeImagePreview"></div>
    <div class="modal-content">
      <button class="close-btn" @click="closeImagePreview">
        <i class='bx bx-x'></i>
      </button>
      <img :src="previewImageSrc" alt="Preview" class="preview-image-full" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { 
  deleteMyDiscoverPost, 
  getMyDiscoverPosts, 
  getDiscoverPostDetail,
  updateDiscoverPost,
  uploadPostImage
} from '../api/app'
import PostDetailModal from '../components/PostDetailModal.vue'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm

const { t } = useI18n()
const router = useRouter()
const posts = ref([])
const showDetailModal = ref(false)
const currentPost = ref(null)
const showEditModal = ref(false)
const saving = ref(false)
const customTagInput = ref('')
const categories = computed(() => [
  { id: '服饰妆造', name: t('discover.presetTags.costume') },
  { id: '美术造物', name: t('discover.presetTags.artistry') },
  { id: '民俗节气', name: t('discover.presetTags.folklore') },
  { id: '戏曲演绎', name: t('discover.presetTags.opera') },
  { id: '织物手工', name: t('discover.presetTags.textile') }
])
const presetTags = computed(() => [
  t('discover.presetTags.costume'),
  t('discover.presetTags.artistry'),
  t('discover.presetTags.folklore'),
  t('discover.presetTags.opera'),
  t('discover.presetTags.textile')
])

const editForm = ref({
  id: null,
  title: '',
  content: '',
  category: '服饰妆造',
  tags: [],
  images: []
})
const pendingFilesMap = ref(new Map())
const originalImages = ref([])
const draggedIndex = ref(null)
const showImagePreview = ref(false)
const previewImageSrc = ref('')

const goBack = () => {
  router.back()
}

const goToTranslate = () => {
  router.push('/home/discover')
}

const formatAuditStatus = (status) => {
  const labels = {
    pending: 'Pending review',
    manual_review: 'Under manual review',
    passed: 'Approved',
    rejected: 'Needs changes'
  }
  return labels[status] || 'Pending review'
}

const loadMyPosts = async () => {
  try {
    const response = await getMyDiscoverPosts()
    if (response.code === 200) {
      posts.value = Array.isArray(response.data) ? response.data : []
    } else {
      notify.error(response.message || t('myPosts.loadFailed'))
    }
  } catch (error) {
    notify.error(t('myPosts.loadFailedRetry'))
  }
}

const viewPost = async (post) => {
  try {
    const response = await getDiscoverPostDetail(post.id)
    if (response.code === 200) {
      currentPost.value = response.data
      showDetailModal.value = true
    } else {
      notify.error(response.message || t('myPosts.loadDetailFailed'))
    }
  } catch (error) {
    notify.error(t('myPosts.loadDetailFailedRetry'))
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  currentPost.value = null
}

const handlePostUpdate = (updatedPost) => {
  currentPost.value = updatedPost
  const index = posts.value.findIndex(p => p.id === updatedPost.id)
  if (index !== -1) {
    posts.value[index] = { ...posts.value[index], ...updatedPost }
  }
}

const openEditModal = async (post) => {
  try {
    const response = await getDiscoverPostDetail(post.id)
    if (response.code === 200) {
      const detail = response.data
      const images = detail.images || []
      editForm.value = {
        id: detail.id,
        title: detail.title || '',
        content: detail.content || '',
        category: detail.category || '服饰妆造',
        tags: detail.hashtags || [],
        images: images
      }
      originalImages.value = [...images]
      pendingFilesMap.value = new Map()
      showEditModal.value = true
    } else {
      notify.error(response.message || t('myPosts.loadDetailFailed'))
    }
  } catch (error) {
    notify.error(t('myPosts.loadDetailFailedRetry'))
  }
}

const closeEditModal = () => {
  showEditModal.value = false
  editForm.value.images.forEach(url => {
    if (url && url.startsWith('blob:')) {
      URL.revokeObjectURL(url)
    }
  })
  editForm.value = {
    id: null,
    title: '',
    content: '',
    category: '服饰妆造',
    tags: [],
    images: []
  }
  pendingFilesMap.value = new Map()
  originalImages.value = []
  customTagInput.value = ''
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
    const images = [...editForm.value.images]
    const [draggedImage] = images.splice(draggedIndex.value, 1)
    images.splice(dropIndex, 0, draggedImage)
    editForm.value.images = images
    
    // Update pendingFilesMap if needed
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

const previewImage = (imageUrl) => {
  previewImageSrc.value = imageUrl
  showImagePreview.value = true
}

const closeImagePreview = () => {
  showImagePreview.value = false
  previewImageSrc.value = ''
}

const handleImageUpload = async (event) => {
  const files = event.target.files ? Array.from(event.target.files) : []
  if (files.length === 0) return
  
  const remainingSlots = 9 - editForm.value.images.length
  if (remainingSlots <= 0) {
    notify.warning(t('myPosts.maxImages'))
    return
  }
  
  const filesToUpload = files.slice(0, remainingSlots)
  filesToUpload.forEach(file => {
    const previewUrl = URL.createObjectURL(file)
    editForm.value.images.push(previewUrl)
    pendingFilesMap.value.set(previewUrl, file)
  })
}

const removeImage = (index) => {
  const imageUrl = editForm.value.images[index]
  if (imageUrl && imageUrl.startsWith('blob:')) {
    URL.revokeObjectURL(imageUrl)
    pendingFilesMap.value.delete(imageUrl)
  }
  editForm.value.images = editForm.value.images.filter((_, i) => i !== index)
}

const toggleTag = (tag) => {
  const current = editForm.value.tags
  const exists = current.includes(tag)
  if (!exists && current.length >= 10) {
    notify.warning(t('discover.maxTags'))
    return
  }
  editForm.value.tags = exists ? current.filter(item => item !== tag) : [...current, tag]
}

const addCustomTags = () => {
  const tags = customTagInput.value.split(/[\s,，]+/).map(item => item.trim()).filter(Boolean)
  if (!tags.length) {
    return
  }
  const merged = [...editForm.value.tags]
  let addedCount = 0
  tags.forEach(tag => {
    if (merged.length >= 10) {
      return
    }
    if (!merged.includes(tag)) {
      merged.push(tag)
      addedCount++
    }
  })
  if (addedCount < tags.length) {
    notify.warning(t('discover.maxTags'))
  }
  editForm.value.tags = merged
  customTagInput.value = ''
}

const removeTag = (tag) => {
  editForm.value.tags = editForm.value.tags.filter(t => t !== tag)
}

const saveEdit = async () => {
  if (!editForm.value.title.trim()) {
    notify.warning(t('myPosts.enterTitle'))
    return
  }
  if (!editForm.value.content.trim()) {
    notify.warning(t('myPosts.enterContent'))
    return
  }
  if (!editForm.value.images || editForm.value.images.length === 0) {
    notify.warning(t('myPosts.uploadImage'))
    return
  }

  saving.value = true
  try {
    const postId = editForm.value.id
    const finalImages = []

    // 找出哪些是新图片（blob URL），并记录它们在最终数组中的位置
    const newImageInfo = []
    editForm.value.images.forEach((url, index) => {
      if (url.startsWith('blob:')) {
        newImageInfo.push({ index, url })
      }
    })

    // 上传新图片，按照它们在最终数组中的位置分配序号（从1开始）
    for (let i = 0; i < newImageInfo.length; i++) {
      const { index, url } = newImageInfo[i]
      const file = pendingFilesMap.value.get(url)
      if (file) {
        const formData = new FormData()
        formData.append('file', file)
        const response = await uploadPostImage(formData, postId, index + 1)
        if (response.code === 200 && response.data && response.data.url) {
          finalImages[index] = response.data.url
        } else {
          throw new Error(response.message || '上传失败')
        }
      }
    }

    // 填充旧图片
    editForm.value.images.forEach((url, index) => {
      if (!url.startsWith('blob:')) {
        finalImages[index] = url
      }
    })

    // 过滤掉undefined，得到最终的图片数组
    const cleanFinalImages = finalImages.filter(url => url !== undefined)

    const response = await updateDiscoverPost(postId, {
      title: editForm.value.title.trim(),
      content: editForm.value.content.trim(),
      category: editForm.value.category,
      tags: editForm.value.tags,
      images: cleanFinalImages
    })
    if (response.code === 200) {
      notify.success('Post updated and resubmitted for review')
      closeEditModal()
      await loadMyPosts()
    } else {
      notify.error(response.message || t('myPosts.saveFailed'))
    }
  } catch (error) {
    notify.error(t('myPosts.saveFailedRetry'))
  } finally {
    saving.value = false
  }
}

const deletePost = async (id) => {
  confirm({
    title: t('myPosts.confirmDeleteTitle'),
    message: t('myPosts.confirmDelete'),
    confirmText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onConfirm: async () => {
      try {
        const response = await deleteMyDiscoverPost(id)
        if (response.code === 200) {
          posts.value = posts.value.filter(post => post.id !== id)
          notify.success(t('myPosts.deleteSuccess'))
        } else {
          notify.error(response.message || t('myPosts.deleteFailed'))
        }
      } catch (error) {
        notify.error(t('myPosts.deleteFailedRetry'))
      }
    }
  })
}

onMounted(() => {
  loadMyPosts()
})
</script>

<style scoped>
.my-posts-page {
  min-height: 100vh;
  width: 100%;
  margin: 0 auto;
}

@media (min-width: 768px) {
  .my-posts-page {
    width: 85%;
    margin: 0 auto;
  }
}

@media (min-width: 1024px) {
  .my-posts-page {
    width: 70%;
    margin: 0 auto;
  }
}

.settings-header {
  background: white;
  padding: 15px 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #eee;
  border-radius: 0px 0px 12px 12px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #333;
  margin-right: 15px;
}

.settings-header h1 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.posts-content {
  padding: 20px;
}

.empty-state {
  background: white;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.empty-state i {
  font-size: 60px;
  color: #7494ec;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-state h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.empty-state p {
  margin: 0 0 30px 0;
  color: #666;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn.primary {
  background: #7494ec;
  color: white;
}

.btn.primary:hover {
  background: #6381d9;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.post-item {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
  display: flex;
  gap: 15px;
  position: relative;
}

.post-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.post-images {
  position: relative;
  flex-shrink: 0;
  width: 100px;
  height: 100px;
}

.post-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.image-count {
  position: absolute;
  bottom: 5px;
  right: 5px;
  background: rgba(0,0,0,0.6);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.post-content {
  flex: 1;
  min-width: 0;
}

.post-status-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.audit-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.audit-chip.pending {
  background: #fef3c7;
  color: #b45309;
}

.audit-chip.passed {
  background: #dcfce7;
  color: #15803d;
}

.audit-chip.manual_review {
  background: #ffedd5;
  color: #c2410c;
}

.audit-chip.rejected {
  background: #fee2e2;
  color: #b91c1c;
}

.post-content h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-text {
  margin: 0 0 10px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.audit-remark {
  margin: 0 0 10px 0;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff1f2;
  color: #be123c;
  font-size: 12px;
  line-height: 1.5;
}

.tag {
  background: #f0f2f5;
  color: #7494ec;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.post-stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-item i {
  font-size: 14px;
}

.post-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-left: 10px;
  border-left: 1px solid #eee;
}

.action-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #999;
  padding: 5px;
  transition: color 0.2s;
  border-radius: 4px;
}

.action-btn:hover {
  color: #7494ec;
  background: #f5f5f5;
}

.delete-btn:hover {
  color: #ff4757;
  background: #fff5f5;
}

.edit-modal {
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

.edit-modal .modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
}

.edit-modal .modal-content {
  position: relative;
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 1200px;
  max-height: 90vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30px 30px 20px 30px;
  border-bottom: 1px solid #f0f0f0;
}

.edit-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
}

.close-btn:hover {
  color: #333;
}

.post-form-horizontal {
  display: flex;
  gap: 30px;
  padding: 30px;
  flex: 1;
  overflow-y: auto;
}

.post-form-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-form-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-form-left .form-group textarea {
  min-height: 200px;
  flex: 1;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #666;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #ff2442;
}

.form-group textarea {
  resize: vertical;
  min-height: 120px;
}

.form-group select {
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  background: #fff;
}

.tags-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.tag-count {
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.selected-tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #ff2442;
  color: #fff;
  border-radius: 16px;
  font-size: 12px;
}

.selected-tag-item i {
  cursor: pointer;
  font-size: 14px;
  transition: opacity 0.3s;
}

.selected-tag-item i:hover {
  opacity: 0.8;
}

.preset-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.tag-item {
  padding: 6px 12px;
  background: #f5f5f5;
  border-radius: 16px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.tag-item:hover {
  background: #e8e8e8;
  color: #333;
}

.tag-item.active {
  background: #ff2442;
  color: #fff;
}

.tag-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tag-input-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tag-input-container input {
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.tag-input-container input:focus {
  border-color: #ff2442;
}

.tag-buttons {
  display: flex;
  gap: 10px;
}

.tag-btn {
  padding: 8px 16px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.tag-btn:hover {
  background: #e8e8e8;
  border-color: #d0d0d0;
  color: #333;
}

.tag-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tag-input-container input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.image-upload-area {
  border: 2px dashed #e0e0e0;
  border-radius: 8px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.image-upload-area:hover {
  border-color: #ff2442;
  background: rgba(255, 36, 66, 0.05);
}

.image-upload-area i {
  font-size: 32px;
  color: #999;
  margin-bottom: 10px;
}

.image-upload-area span {
  font-size: 14px;
  color: #666;
}

.image-upload-area input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.selected-images {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.image-preview-item {
  position: relative;
  width: 70px;
  height: 70px;
}

.preview-image {
  width: 70px;
  height: 70px;
  border-radius: 6px;
  object-fit: cover;
}

.remove-image-btn {
  position: absolute;
  top: -5px;
  right: -5px;
  width: 20px;
  height: 20px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.3s;
}

.remove-image-btn:hover {
  background: rgba(255, 36, 66, 0.9);
  transform: scale(1.1);
}

.submit-post-btn {
  padding: 12px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-post-btn:hover {
  background: #ff3a56;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(255, 36, 66, 0.3);
}

.submit-post-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

@media (max-width: 900px) {
  .post-form-horizontal {
    flex-direction: column;
  }

  .post-form-right {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .posts-content {
    padding: 10px;
  }
  
  .post-item {
    padding: 15px;
    flex-direction: column;
  }
  
  .post-images {
    width: 100%;
    height: 150px;
  }
  
  .post-actions {
    flex-direction: row;
    justify-content: flex-end;
    border-left: none;
    border-top: 1px solid #eee;
    padding-left: 0;
    padding-top: 10px;
    margin-top: 10px;
  }
  
  .edit-modal .modal-content {
    width: 95%;
    max-height: 95vh;
  }
  
  .edit-header {
    padding: 20px 20px 15px 20px;
  }
  
  .post-form-horizontal {
    padding: 20px;
    gap: 20px;
  }
}

/* 图片预览模态框 */
.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview-modal .modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.9);
}

.image-preview-modal .modal-content {
  position: relative;
  max-width: 90%;
  max-height: 90vh;
  z-index: 1;
}

.image-preview-modal .close-btn {
  position: absolute;
  top: -40px;
  right: 0;
  background: none;
  border: none;
  font-size: 24px;
  color: white;
  cursor: pointer;
  padding: 0;
}

.preview-image-full {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
}

/* 拖拽样式 */
.image-preview-item {
  cursor: move;
  transition: all 0.3s;
}

.image-preview-item:hover {
  transform: scale(1.05);
}

.drag-handle {
  position: absolute;
  bottom: -5px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-preview-item:hover .drag-handle {
  opacity: 1;
}
</style>
