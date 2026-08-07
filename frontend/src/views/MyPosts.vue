<template>
  <div class="my-posts-page">
    <div class="settings-header">
      <button class="back-btn" @click="goBack">
        <i class='bx bxs-chevron-left'></i>
      </button>
      <h1>动态管理</h1>
    </div>

    <div class="privacy-banner">
      <div>
        <strong>单条帖子可直接切换公开 / 私密</strong>
        <p>锁图标会同步更新帖子展示状态，不影响内容编辑。</p>
      </div>
    </div>

    <div class="posts-content">
      <div class="empty-state" v-if="posts.length === 0">
        <i class='bx bxs-edit-alt'></i>
        <h3>还没有发布任何帖子</h3>
        <p>去发布你的第一篇帖子吧</p>
        <button class="btn primary" @click="goToTranslate">去发布</button>
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
              <span :class="['type-chip', `type-${normalizePostType(post)}`]">{{ getPostTypeLabel(post) }}</span>
              <button
                class="visibility-toggle-btn"
                type="button"
                @click.stop="togglePostVisibility(post)"
                :title="post.visibility === 'private' ? '设为公开' : '设为私密'"
              >
                <i :class="post.visibility === 'private' ? 'bx bx-lock-open-alt' : 'bx bx-lock-alt'"></i>
                <span>{{ post.visibility === 'private' ? '私密' : '公开' }}</span>
              </button>
              <span
                v-if="isReviewPost(post)"
                class="activity-chip"
                @click.stop="openLinkedActivity(post)"
              >
                活动关联
              </span>
            </div>
            <h4>{{ post.title }}</h4>
            <p class="post-text">{{ post.content }}</p>
            <div class="post-tags" v-if="post.hashtags && post.hashtags.length > 0">
              <span v-for="tag in post.hashtags.slice(0, 3)" :key="tag" class="tag">#{{ tag }}</span>
              <span v-if="post.hashtags.length > 3" class="tag more">+{{ post.hashtags.length - 3 }}</span>
            </div>
            <div class="post-meta">
              <span><i class='bx bxs-heart'></i> {{ post.likes || 0 }}</span>
              <span><i class='bx bxs-message-rounded'></i> {{ post.comments || 0 }}</span>
              <span><i class='bx bxs-bookmark'></i> {{ post.collects || 0 }}</span>
              <span class="post-time">{{ post.time }}</span>
            </div>
          </div>
          <div class="post-actions">
            <button class="action-btn view-btn" @click="viewPost(post)" title="查看">
              <i class='bx bxs-show'></i>
            </button>
            <button class="action-btn edit-btn" @click.stop="openEditModal(post)" title="编辑">
              <i class='bx bxs-edit'></i>
            </button>
            <button class="action-btn delete-btn" @click.stop="deletePost(post.id)" title="删除">
              <i class='bx bxs-trash'></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑帖子弹窗 -->
    <div v-if="showEditModal" class="edit-modal" @click.self="closeEditModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>编辑帖子</h2>
          <button class="close-btn" @click="closeEditModal">
            <i class='bx bx-x'></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>标题</label>
            <input v-model="editForm.title" type="text" placeholder="请输入标题" />
          </div>
          <div class="form-group">
            <label>内容</label>
            <textarea v-model="editForm.content" placeholder="分享你的故事..."></textarea>
          </div>
          <div class="form-group">
            <label>分类</label>
            <select v-model="editForm.category">
              <option v-for="category in categories" :key="category.id" :value="category.id">
                {{ category.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>添加图片</label>
            <div class="image-upload-area" @click="$refs.editImageInput.click()">
              <i class='bx bx-plus-circle'></i>
              <span>添加图片</span>
              <input 
                ref="editImageInput"
                type="file" 
                multiple 
                accept="image/*" 
                @change="handleEditImageUpload"
                style="display: none;"
              />
            </div>
            <div v-if="editForm.images.length > 0" class="selected-images">
              <div 
                v-for="(image, index) in editForm.images" 
                :key="index"
                class="selected-image-item"
                draggable="true"
                @dragstart="onDragStart($event, index)"
                @dragover="onDragOver"
                @drop="onDrop($event, index)"
                @dragend="onDragEnd"
              >
                <img :src="image" alt="Selected" />
                <button class="remove-image-btn" @click="removeEditImage(index)">
                  <i class='bx bx-x'></i>
                </button>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label>标签 <span class="tag-count">({{ editForm.tags.length }}/10)</span></label>
            <div class="tags-container">
              <div v-if="editForm.tags.length > 0" class="selected-tags">
                <span
                  v-for="tag in editForm.tags"
                  :key="tag"
                  class="selected-tag-item"
                >
                  #{{ tag }}
                  <i class='bx bx-x' @click="removeEditTag(tag)"></i>
                </span>
              </div>
              <div class="preset-tags">
                <span
                  v-for="tag in presetTags"
                  :key="tag"
                  class="tag-item"
                  :class="{ active: editForm.tags.includes(tag), disabled: !editForm.tags.includes(tag) && editForm.tags.length >= 10 }"
                  @click="toggleEditTag(tag)"
                >#{{ tag }}</span>
              </div>
              <div class="tag-input-container">
                <input 
                  v-model="customTagInput" 
                  type="text" 
                  placeholder="添加标签" 
                  @keyup.enter="addCustomTags"
                  :disabled="editForm.tags.length >= 10"
                />
                <div class="tag-buttons">
                  <button class="tag-btn" @click.prevent="addCustomTags" :disabled="editForm.tags.length >= 10">添加</button>
                </div>
              </div>
            </div>
          </div>
          <button class="submit-post-btn" :disabled="saving" @click="saveEdit">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <PostDetailModal
      :visible="showPostDetail"
      :post="detailPost"
      @close="closePostDetail"
      @open-activity="openLinkedActivity"
    />

    <ActivityDetailModal
      :visible="showActivityDetail"
      :activity-id="selectedActivityId"
      :z-index="1900"
      @close="closeActivityDetail"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { getMyDiscoverPosts, deleteMyDiscoverPost, updateDiscoverPost, updateDiscoverPostVisibility, uploadPostImage, getDiscoverPostDetail } from '../api/app'
import PostDetailModal from '../components/PostDetailModal.vue'
import ActivityDetailModal from '@/components/ActivityDetailModal.vue'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm

const router = useRouter()
const posts = ref([])
const loading = ref(false)
const showEditModal = ref(false)
const saving = ref(false)
const uploadingImages = ref(false)
const editingPostId = ref(null)
const showPostDetail = ref(false)
const detailPost = ref(null)
const showActivityDetail = ref(false)
const selectedActivityId = ref('')

const categories = ref([
  { id: '服饰妆造', name: '服饰妆造' },
  { id: '美术造物', name: '美术造物' },
  { id: '民俗节气', name: '民俗节气' },
  { id: '戏曲演绎', name: '戏曲演绎' },
  { id: '织物手工', name: '织物手工' }
])

const presetTags = ref([
  '服饰妆造',
  '美术造物',
  '民俗节气',
  '戏曲演绎',
  '织物手工'
])

const editForm = ref({
  title: '',
  content: '',
  category: '服饰妆造',
  tags: [],
  images: []
})

const customTagInput = ref('')
const draggedIndex = ref(null)

const goBack = () => {
  router.back()
}

const goToTranslate = () => {
  router.push('/discover')
}

const formatAuditStatus = (status) => {
  const statusMap = {
    'pending': '审核中',
    'passed': '已通过',
    'rejected': '已拒绝'
  }
  return statusMap[status] || '审核中'
}

const loadPosts = async () => {
  loading.value = true
  try {
    const response = await getMyDiscoverPosts()
    if (response.code === 200) {
      posts.value = response.data || []
    } else {
      notify.error(response.message || '加载帖子失败')
    }
  } catch (error) {
    notify.error('加载帖子失败，请重试')
  } finally {
    loading.value = false
  }
}

const viewPost = async (post) => {
  try {
    const response = await getDiscoverPostDetail(post.id)
    if (response.code === 200) {
      detailPost.value = response.data
      showPostDetail.value = true
    } else {
      notify.error(response.message || '加载详情失败')
    }
  } catch (error) {
    notify.error('加载详情失败，请重试')
  }
}

const togglePostVisibility = async (post) => {
  const nextVisibility = post.visibility === 'private' ? 'public' : 'private'
  try {
    const response = await updateDiscoverPostVisibility(post.id, nextVisibility)
    if (response.code !== 200 || !response.data) {
      notify.error(response.message || '更新帖子公开状态失败')
      return
    }

    const nextPost = { ...post, visibility: response.data.visibility || nextVisibility }
    posts.value = posts.value.map(item => (item.id === post.id ? nextPost : item))
    if (detailPost.value && detailPost.value.id === post.id) {
      detailPost.value = { ...detailPost.value, visibility: nextPost.visibility }
    }
    notify.success(nextPost.visibility === 'private' ? '帖子已设为私密' : '帖子已设为公开')
  } catch (error) {
    notify.error('更新帖子公开状态失败，请重试')
  }
}

const closePostDetail = () => {
  showPostDetail.value = false
  detailPost.value = null
}

const openLinkedActivity = (payload) => {
  const activityId = Number(payload?.id || payload?.activityId || payload?.sourceId || 0)
  if (!activityId) return
  selectedActivityId.value = String(activityId)
  showActivityDetail.value = true
}

const closeActivityDetail = () => {
  showActivityDetail.value = false
  selectedActivityId.value = ''
}

const normalizePostType = (post) => String(post?.type || post?.postType || 'normal').toLowerCase()

const getPostTypeLabel = (post) => {
  const type = normalizePostType(post)
  if (type === 'review') return '评价贴'
  if (type === 'ai_video') return 'AI 内容'
  return '分享帖'
}

const isReviewPost = (post) => normalizePostType(post) === 'review'

const openEditModal = async (post) => {
  editingPostId.value = post.id
  try {
    editForm.value = {
      title: post.title || '',
      content: post.content || '',
      category: post.category || '服饰妆造',
      tags: post.hashtags || [],
      images: post.images || []
    }
    showEditModal.value = true
  } catch (error) {
    notify.error('加载详情失败，请重试')
  }
}

const closeEditModal = () => {
  showEditModal.value = false
  editingPostId.value = null
  editForm.value = {
    title: '',
    content: '',
    category: '服饰妆造',
    tags: [],
    images: []
  }
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
  }
  draggedIndex.value = null
  event.target.style.opacity = '1'
}

const onDragEnd = (event) => {
  event.target.style.opacity = '1'
  draggedIndex.value = null
}

const handleEditImageUpload = async (event) => {
  const input = event.target
  const files = input?.files ? Array.from(input.files) : []
  if (files.length === 0) return

  const remainingSlots = 9 - editForm.value.images.length
  if (remainingSlots <= 0) {
    notify.warning('最多上传9张图片')
    if (input) {
      input.value = ''
    }
    return
  }

  const filesToUpload = files.slice(0, remainingSlots)
  uploadingImages.value = true
  
  try {
    const uploadedUrls = []
    for (const file of filesToUpload) {
      const formData = new FormData()
      formData.append('file', file)
      const response = await uploadPostImage(formData, 'posts')
      if (response.code === 200 && response.data?.url) {
        uploadedUrls.push(response.data.url)
      } else {
        throw new Error(response.message || '图片上传失败')
      }
    }
    editForm.value.images.push(...uploadedUrls)
    notify.success(`成功上传 ${uploadedUrls.length} 张图片`)
  } catch (error) {
    notify.error(error.message || '图片上传失败，请重试')
  } finally {
    uploadingImages.value = false
    if (input) {
      input.value = ''
    }
  }
}

const removeEditImage = (index) => {
  editForm.value.images = editForm.value.images.filter((_, i) => i !== index)
}

const toggleEditTag = (tag) => {
  const current = editForm.value.tags
  const exists = current.includes(tag)
  if (!exists && current.length >= 10) {
    notify.warning('最多添加10个标签')
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
    notify.warning('最多添加10个标签')
  }
  editForm.value.tags = merged
  customTagInput.value = ''
}

const removeEditTag = (tag) => {
  editForm.value.tags = editForm.value.tags.filter(item => item !== tag)
}

const saveEdit = async () => {
  if (!editForm.value.title.trim()) {
    notify.warning('请输入标题')
    return
  }
  if (!editForm.value.content.trim()) {
    notify.warning('请输入内容')
    return
  }
  if (!editForm.value.images || editForm.value.images.length === 0) {
    notify.warning('请上传图片')
    return
  }

  saving.value = true
  try {
    const postId = editingPostId.value
    const finalTags = [...editForm.value.tags]
    
    const response = await updateDiscoverPost(postId, {
      title: editForm.value.title.trim(),
      content: editForm.value.content.trim(),
      category: editForm.value.category,
      tags: finalTags,
      images: editForm.value.images
    })
    if (response.code === 200) {
      notify.success('保存成功')
      closeEditModal()
      await loadPosts()
    } else {
      notify.error(response.message || '保存失败')
    }
  } catch (error) {
    notify.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

const deletePost = (id) => {
  confirm({
    title: '确认删除',
    message: '确定要删除这条帖子吗？删除后无法恢复。',
    confirmText: '确认',
    cancelText: '取消',
    onConfirm: async () => {
      try {
        const response = await deleteMyDiscoverPost(id)
        if (response.code === 200) {
          notify.success('删除成功')
          await loadPosts()
        } else {
          notify.error(response.message || '删除失败')
        }
      } catch (error) {
        notify.error('删除失败，请重试')
      }
    }
  })
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.my-posts-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f9f5f0 0%, #f0e6d8 100%);
  padding: 20px;
}

.settings-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(157, 41, 41, 0.1);
}

.back-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(157, 41, 41, 0.1);
  color: #9d2929;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(157, 41, 41, 0.2);
  transform: translateX(-2px);
}

.settings-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #2c1810;
  margin: 0;
}

.privacy-banner {
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(157, 41, 41, 0.08), rgba(201, 145, 63, 0.12));
  border: 1px solid rgba(157, 41, 41, 0.1);
  color: #5b2a22;
}

.privacy-banner strong {
  display: block;
  font-size: 15px;
}

.privacy-banner p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #7b5a4d;
}

.posts-content {
  max-width: 800px;
  margin: 0 auto;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(157, 41, 41, 0.1);
}

.empty-state i {
  font-size: 64px;
  color: #c9913f;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 20px;
  color: #2c1810;
  margin-bottom: 8px;
}

.empty-state p {
  color: #666;
  margin-bottom: 24px;
}

.btn.primary {
  background: linear-gradient(135deg, #9d2929, #c9913f);
  color: white;
  border: none;
  padding: 12px 32px;
  border-radius: 25px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(157, 41, 41, 0.3);
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-item {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(157, 41, 41, 0.08);
}

.post-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.post-images {
  position: relative;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
  border-radius: 12px;
  overflow: hidden;
}

.post-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-count {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.post-content {
  flex: 1;
  min-width: 0;
}

.post-status-row {
  margin-bottom: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.audit-chip {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.audit-chip.pending {
  background: rgba(201, 145, 63, 0.15);
  color: #c9913f;
}

.audit-chip.passed {
  background: rgba(82, 146, 82, 0.15);
  color: #529252;
}

.audit-chip.rejected {
  background: rgba(157, 41, 41, 0.15);
  color: #9d2929;
}

.type-chip,
.activity-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.type-chip {
  background: rgba(31, 111, 235, 0.1);
  color: #1f6feb;
}

.visibility-toggle-btn {
  border: 1px solid rgba(157, 41, 41, 0.12);
  background: #fff7f4;
  color: #9d2929;
  border-radius: 999px;
  padding: 4px 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.visibility-toggle-btn:hover {
  background: #fff0ea;
}

.type-chip.type-review {
  background: rgba(157, 41, 41, 0.12);
  color: #9d2929;
}

.type-chip.type-ai_video {
  background: rgba(15, 118, 110, 0.12);
  color: #0f766e;
}

.activity-chip {
  background: #ecfdf5;
  color: #047857;
  cursor: pointer;
}

.post-content h4 {
  font-size: 18px;
  font-weight: 600;
  color: #2c1810;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}


.post-text {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 12px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.post-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tag {
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.tag.more {
  background: rgba(201, 145, 63, 0.1);
  color: #c9913f;
}

.post-meta {
  display: flex;
  gap: 16px;
  color: #999;
  font-size: 13px;
}

.post-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.post-meta i {
  font-size: 14px;
}

.post-time {
  margin-left: auto;
}

.post-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.action-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.view-btn {
  background: rgba(82, 146, 82, 0.1);
  color: #529252;
}

.view-btn:hover {
  background: rgba(82, 146, 82, 0.2);
}

.edit-btn {
  background: rgba(31, 111, 235, 0.1);
  color: #1f6feb;
}

.edit-btn:hover {
  background: rgba(31, 111, 235, 0.2);
}

.delete-btn {
  background: rgba(157, 41, 41, 0.1);
  color: #9d2929;
}

.delete-btn:hover {
  background: rgba(157, 41, 41, 0.2);
}

/* 编辑弹窗样式 */
.edit-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.55) !important;
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  animation: fadeIn 0.25s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.modal-content {
  background: #ffffff;
  border-radius: 20px;
  width: 100%;
  max-width: 620px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(157, 41, 41, 0.08);
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  scrollbar-width: thin;
  scrollbar-color: rgba(157, 41, 41, 0.15) transparent;
}

.modal-content::-webkit-scrollbar {
  width: 5px;
}

.modal-content::-webkit-scrollbar-track {
  background: transparent;
}

.modal-content::-webkit-scrollbar-thumb {
  background: rgba(157, 41, 41, 0.15);
  border-radius: 20px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 16px;
  position: sticky;
  top: 0;
  background: #ffffff;
  z-index: 10;
  border-bottom: 1px solid rgba(157, 41, 41, 0.08);
  border-radius: 20px 20px 0 0;
}

.modal-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #2c1810;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-header h2::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 20px;
  background: linear-gradient(180deg, #9d2929, #c9913f);
  border-radius: 2px;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fafafa;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  font-size: 18px;
}

.close-btn:hover {
  background: #fee2e2;
  color: #9d2929;
  border-color: rgba(157, 41, 41, 0.2);
}

.modal-body {
  padding: 20px 24px 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #2c1810;
  margin-bottom: 8px;
  font-size: 14px;
}

.tag-count {
  font-weight: 400;
  color: #999;
  font-size: 12px;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 12px 16px;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.2s ease;
  background: #fafafa;
  color: #2c1810;
  box-sizing: border-box;
  font-family: inherit;
}

.form-group input:hover,
.form-group textarea:hover,
.form-group select:hover {
  border-color: rgba(157, 41, 41, 0.15);
  background: #fff;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #9d2929;
  box-shadow: 0 0 0 4px rgba(157, 41, 41, 0.08);
  background: #fff;
}

.form-group textarea {
  min-height: 140px;
  resize: vertical;
  line-height: 1.6;
}

.form-group select {
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 16px center;
  padding-right: 36px;
}

.image-upload-area {
  border: 2px dashed rgba(157, 41, 41, 0.2);
  border-radius: 14px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.image-upload-area:hover {
  border-color: #c9913f;
  background: rgba(201, 145, 63, 0.04);
  transform: translateY(-1px);
}

.image-upload-area i {
  font-size: 44px;
  color: #c9913f;
}

.image-upload-area span {
  color: #888;
  font-size: 14px;
  font-weight: 500;
}

.selected-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 16px;
}

.selected-image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
  cursor: grab;
  border: 2px solid transparent;
  transition: all 0.2s ease;
}

.selected-image-item:active {
  cursor: grabbing;
}

.selected-image-item:hover {
  border-color: #c9913f;
  transform: scale(1.02);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.selected-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.remove-image-btn:hover {
  background: rgba(157, 41, 41, 0.8);
}

.tags-container {
  background: #fafafa;
  border-radius: 14px;
  padding: 16px;
  border: 1.5px solid rgba(0, 0, 0, 0.06);
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.selected-tag-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(157, 41, 41, 0.1);
  color: #9d2929;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 13px;
}

.selected-tag-item i {
  cursor: pointer;
  font-size: 14px;
}

.preset-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tag-item {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fff;
  color: #666;
  border: 1px solid rgba(0, 0, 0, 0.06);
  font-weight: 500;
}

.tag-item:hover:not(.disabled) {
  border-color: rgba(157, 41, 41, 0.2);
  color: #9d2929;
}

.tag-item.active {
  background: linear-gradient(135deg, rgba(157, 41, 41, 0.08), rgba(201, 145, 63, 0.12));
  color: #9d2929;
  border-color: rgba(157, 41, 41, 0.2);
}

.tag-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tag-input-container {
  display: flex;
  gap: 8px;
}

.tag-input-container input {
  flex: 1;
  padding: 10px 14px;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  font-size: 14px;
  background: #fff;
  transition: all 0.2s ease;
}

.tag-input-container input:focus {
  outline: none;
  border-color: #9d2929;
  box-shadow: 0 0 0 3px rgba(157, 41, 41, 0.06);
}

.tag-btn {
  padding: 10px 18px;
  background: linear-gradient(135deg, rgba(157, 41, 41, 0.08), rgba(201, 145, 63, 0.1));
  color: #9d2929;
  border: 1px solid rgba(157, 41, 41, 0.1);
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.tag-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(157, 41, 41, 0.15), rgba(201, 145, 63, 0.18));
  border-color: rgba(157, 41, 41, 0.25);
}

.tag-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.edit-modal .submit-post-btn {
  width: 100%;
  padding: 14px 24px;
  background: linear-gradient(135deg, #9d2929 0%, #b33939 50%, #c9913f 100%) !important;
  color: white !important;
  border: none !important;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 16px rgba(157, 41, 41, 0.25);
  letter-spacing: 0.5px;
  position: relative;
  overflow: hidden;
  margin-top: 8px;
}

.edit-modal .submit-post-btn::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.15), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.edit-modal .submit-post-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(157, 41, 41, 0.4);
}

.edit-modal .submit-post-btn:hover:not(:disabled)::after {
  opacity: 1;
}

.edit-modal .submit-post-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(157, 41, 41, 0.2);
}

.edit-modal .submit-post-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

@media (max-width: 640px) {
  .my-posts-page {
    padding: 12px;
  }

  .post-item {
    flex-direction: column;
  }

  .post-images {
    width: 100%;
    height: 200px;
  }

  .post-actions {
    flex-direction: row;
    justify-content: flex-end;
  }

  .selected-images {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
