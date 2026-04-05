<template>
  <div class="discover-page" @click="closeMobileMenu">
    <div class="top-nav">
      <div class="nav-content" @click.stop>
        <div class="logo">YayFolk</div>
        <div class="search-input">
          <i class='bx bx-search'></i>
          <input v-model="searchKeyword" type="text" placeholder="搜索" @keyup.enter="loadPosts" />
        </div>
        <div class="nav-buttons main-nav-buttons">
          <div 
            class="nav-button" 
            :class="{ active: currentPage === 'discover' }"
            @click="switchPage('discover')"
          >
            <i class='bx bx-compass'></i>
            <span>发现</span>
          </div>
          <div 
            class="nav-button" 
            :class="{ active: currentPage === 'post' }"
            @click="switchPage('post')"
          >
            <i class='bx bx-edit-alt'></i>
            <span>发布</span>
          </div>
          <div 
            class="nav-button" 
            :class="{ active: currentPage === 'notification' }"
            @click="switchPage('notification')"
          >
            <i class='bx bx-bell'></i>
            <span>通知</span>
            <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
        </div>
        <div class="hamburger-menu" @click="toggleMobileMenu">
          <i :class="showMobileMenu ? 'bx bx-x' : 'bx bx-menu'"></i>
        </div>
        <div v-if="showMobileMenu" class="mobile-menu">
          <div 
            class="mobile-menu-item" 
            :class="{ active: currentPage === 'discover' }"
            @click="switchPageMobile('discover')"
          >
            <i class='bx bx-compass'></i>
            <span>发现</span>
          </div>
          <div 
            class="mobile-menu-item" 
            :class="{ active: currentPage === 'post' }"
            @click="switchPageMobile('post')"
          >
            <i class='bx bx-edit-alt'></i>
            <span>发布</span>
          </div>
          <div 
            class="mobile-menu-item" 
            :class="{ active: currentPage === 'notification' }"
            @click="switchPageMobile('notification')"
          >
            <i class='bx bx-bell'></i>
            <span>通知</span>
            <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="main-container">
      <div v-if="currentPage === 'discover'" class="main-content">
        <div class="category-tabs-wrapper" :class="{ 'is-hidden': isCategoryTabsHidden }">
          <div class="category-tabs">
            <div 
              v-for="category in categories" 
              :key="category.id"
              class="tab"
              :class="{ active: activeCategory === category.id }"
              @click="handleCategoryChange(category.id)"
            >
              {{ category.name }}
            </div>
          </div>
          <div class="action-buttons">
            <div class="sort-dropdown" @click.stop>
              <button class="sort-btn" @click="toggleSortMenu">
                <i class='bx bx-sort-alt-2'></i>
                <span>{{ sortBy === 'latest' ? '最新' : '热门' }}</span>
                <i class='bx bx-chevron-down'></i>
              </button>
              <div v-if="showSortMenu" class="sort-menu">
                <div 
                  class="sort-option" 
                  :class="{ active: sortBy === 'latest' }"
                  @click="changeSort('latest')"
                >
                  <i class='bx bx-time'></i>
                  <span>最新</span>
                </div>
                <div 
                  class="sort-option" 
                  :class="{ active: sortBy === 'hot' }"
                  @click="changeSort('hot')"
                >
                  <i class='bx bx-hot'></i>
                  <span>热门</span>
                </div>
              </div>
            </div>
            <button class="refresh-btn" @click="handleRefresh" :disabled="isRefreshing">
              <i :class="isRefreshing ? 'bx bx-loader-alt bx-spin' : 'bx bx-refresh'"></i>
            </button>
          </div>
        </div>

        <div class="content-list">
          <div v-if="loadingPosts" class="loading-container">
            <div class="loading-spinner">
              <i class='bx bx-loader-alt bx-spin'></i>
            </div>
            <p class="loading-text">加载中...</p>
          </div>
          <template v-else>
            <div v-if="displayPosts.length === 0" class="empty-state">
              <i class='bx bx-file-blank'></i>
              <p>暂无帖子</p>
            </div>
            <div 
              v-else
              v-for="post in displayPosts" 
              :key="post.id"
              class="post-card"
              @click="openPostModal(post)"
            >
            <div class="image-grid" v-if="post.images && post.images.length > 0">
              <img 
                v-for="(image, index) in post.images" 
                :key="index"
                :src="image"
                alt="Post image"
                class="post-image"
                :class="{ 'single-image': post.images.length === 1 }"
              />
            </div>

            <div class="post-content">
              <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
              <p>{{ post.content }}</p>
            </div>

            <div class="post-footer">
              <div class="author-info" @click.stop="goToUserHomepage(post.author?.id)">
                <img :src="post.author.avatar" alt="Avatar" class="author-avatar" />
                <span class="author-name">{{ post.author.name }}</span>
              </div>
              <div class="post-stats">
                <span class="stat-item" @click.stop="toggleCollect(post)">
                  <i class='bx bx-star' :class="{ collected: post.bookmarked }"></i>
                  <span>{{ post.collects || 0 }}</span>
                </span>
                <span class="stat-item">
                  <i class='bx bx-comment'></i>
                  <span>{{ post.comments }}</span>
                </span>
                <span class="stat-item">
                  <i class='bx bx-show'></i>
                  <span>{{ post.viewCount || 0 }}</span>
                </span>
              </div>
            </div>
          </div>
          </template>
        </div>
      </div>

      <div v-else-if="currentPage === 'post'" class="main-content post-page">
        <div class="post-container">
          <h2>发布新帖子</h2>
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
        </div>
      </div>

      <div v-else-if="currentPage === 'notification'" class="main-content notification-page">
        <div class="notification-container">
          <h2>通知</h2>
          <div class="notification-list">
            <div class="notification-item">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=user1" alt="User" class="notification-avatar" />
              <div class="notification-content">
                <p><span class="notification-user">User A</span> 收藏了你的帖子</p>
                <span class="notification-time">10分钟前</span>
              </div>
            </div>
            <div class="notification-item">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=user2" alt="User" class="notification-avatar" />
              <div class="notification-content">
                <p><span class="notification-user">User B</span> 评论了你的帖子</p>
                <span class="notification-time">30分钟前</span>
              </div>
            </div>
            <div class="notification-item">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=user3" alt="User" class="notification-avatar" />
              <div class="notification-content">
                <p><span class="notification-user">User C</span> 关注了你</p>
                <span class="notification-time">1小时前</span>
              </div>
            </div>
            <div class="notification-item">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=system" alt="System" class="notification-avatar" />
              <div class="notification-content">
                <p><span class="notification-user">系统</span> 你的帖子被推荐</p>
                <span class="notification-time">2小时前</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <PostDetailModal 
    :visible="showPostModal" 
    :post="currentPost" 
    @close="closePostModal"
    @update="handlePostUpdate"
    @searchTag="handleSearchTag"
  />

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
import { computed, onMounted, onUnmounted, ref, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  createDiscoverPost,
  getDiscoverPostDetail,
  getDiscoverPosts,
  getUnreadCount,
  toggleDiscoverPostCollect,
  uploadPostImage,
  updateDiscoverPost,
  classifyDiscoverImage
} from '../api/app'
import PostDetailModal from '../components/PostDetailModal.vue'


const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const route = useRoute()

const categories = computed(() => [
  { id: 'all', name: '全部' },
  { id: '服饰妆造', name: '服饰妆造' },
  { id: '美术造物', name: '美术造物' },
  { id: '民族节气', name: '民族节气' },
  { id: '戏曲演绎', name: '戏曲演绎' },
  { id: '织物手工', name: '织物手工' }
])

const posts = ref([])
const activeCategory = ref('all')
const currentPage = ref('discover')
const showPostModal = ref(false)
const currentPost = ref(null)
const searchKeyword = ref('')
const loadingPosts = ref(false)
const submittingPost = ref(false)
const unreadCount = ref(0)
const showMobileMenu = ref(false)
const sortBy = ref('latest')
const showSortMenu = ref(false)
const isRefreshing = ref(false)
const isCategoryTabsHidden = ref(false)
const lastScrollY = ref(0)
const postForm = ref({
  title: '',
  content: '',
  category: '服饰妆造',
  tags: [],
  images: []
})
const pendingFilesMap = ref(new Map())
const customTagsInput = ref('')
const presetTags = computed(() => [
  '服饰妆造',
  '美术造物',
  '民族节气',
  '戏曲演绎',
  '织物手工'
])
const classifierTagMap = computed(() => ({
  '服饰妆造': '服饰妆造',
  '美术造物': '美术造物',
  '民族节气': '民族节气',
  '戏曲演绎': '戏曲演绎',
  '织物手工': '织物手工'
}))
const bestClassification = ref({
  tag: '',
  confidence: 0
})
const selectedImages = computed(() => postForm.value.images)
const displayPosts = computed(() => posts.value)
const draggedIndex = ref(null)
const showImagePreview = ref(false)
const previewImageSrc = ref('')




const touchStartX = ref(0)
const touchStartY = ref(0)
const touchDragged = ref(false)
const touchDraggedIndex = ref(null)

const mergeTags = (incomingTags = []) => {
  if (!Array.isArray(incomingTags) || incomingTags.length === 0) {
    return
  }

  const merged = [...postForm.value.tags]
  incomingTags
    .map(tag => (typeof tag === 'string' ? tag.trim() : ''))
    .map(tag => classifierTagMap.value[tag] || tag)
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
  if (!normalizedTag || !(normalizedTag in classifierTagMap.value)) {
    return
  }

  if (confidence < bestClassification.value.confidence) {
    return
  }

  bestClassification.value = {
    tag: normalizedTag,
    confidence
  }
  postForm.value.category = normalizedTag
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

const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value
}

const closeMobileMenu = () => {
  showMobileMenu.value = false
  showSortMenu.value = false
}

const handleWindowScroll = () => {
  const currentY = window.scrollY || window.pageYOffset || 0
  const delta = currentY - lastScrollY.value

  if (currentPage.value !== 'discover') {
    isCategoryTabsHidden.value = false
    lastScrollY.value = currentY
    return
  }

  if (showMobileMenu.value || showSortMenu.value || currentY < 140) {
    isCategoryTabsHidden.value = false
    lastScrollY.value = currentY
    return
  }

  if (delta > 8) {
    isCategoryTabsHidden.value = true
  } else if (delta < -8) {
    isCategoryTabsHidden.value = false
  }

  lastScrollY.value = currentY
}

const switchPage = (page) => {
  if (page === 'notification') {
    router.push('/notification')
    return
  }
  currentPage.value = page
  isCategoryTabsHidden.value = false
  lastScrollY.value = window.scrollY || window.pageYOffset || 0
  if (page === 'discover') {
    loadPosts()
  }
}

const switchPageMobile = (page) => {
  showMobileMenu.value = false
  switchPage(page)
}

const loadPosts = async () => {
  loadingPosts.value = true
  try {
    const response = await getDiscoverPosts({
      category: activeCategory.value,
      keyword: searchKeyword.value.trim(),
      sortBy: sortBy.value
    })
    if (response.code === 200) {
      posts.value = Array.isArray(response.data) ? response.data : []
    } else {
      notify.error(response.message || '加载帖子失败')
    }
  } catch (error) {
    notify.error('加载帖子失败，请重试')
  } finally {
    loadingPosts.value = false
  }
}

const handleCategoryChange = (categoryId) => {
  activeCategory.value = categoryId
  loadPosts()
}

const toggleSortMenu = () => {
  showSortMenu.value = !showSortMenu.value
}

const changeSort = (sort) => {
  sortBy.value = sort
  showSortMenu.value = false
  loadPosts()
}

const handleRefresh = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true
  await loadPosts()
  setTimeout(() => {
    isRefreshing.value = false
  }, 500)
}

const openPostModal = async (post) => {
  try {
    const response = await getDiscoverPostDetail(post.id)
    if (response.code === 200) {
      currentPost.value = response.data
      showPostModal.value = true
      syncPostInList(response.data)
    } else {
      notify.error(response.message || '加载详情失败')
    }
  } catch (error) {
    notify.error('加载详情失败，请重试')
  }
}

const goToUserHomepage = (userId) => {
  if (!userId) return
  router.push(`/user-homepage/${userId}`)
}

const closePostModal = () => {
  showPostModal.value = false
  currentPost.value = null
}

const handleSearchTag = (tag) => {
  searchKeyword.value = tag
  currentPage.value = 'discover'
  loadPosts()
}

const handlePostUpdate = (updatedPost) => {
  currentPost.value = updatedPost
  syncPostInList(updatedPost)
}

const syncPostInList = (updatedPost) => {
  if (updatedPost?.auditStatus && updatedPost.auditStatus !== 'passed') {
    posts.value = posts.value.filter(item => item.id !== updatedPost.id)
    return
  }
  const index = posts.value.findIndex(item => item.id === updatedPost.id)
  if (index !== -1) {
    posts.value[index] = {
      ...posts.value[index],
      ...updatedPost
    }
  }
}

const toggleCollect = async (post) => {
  try {
    const response = await toggleDiscoverPostCollect(post.id)
    if (response.code !== 200) {
      notify.error(response.message || '操作失败')
      return
    }
    const { bookmarked, collects } = response.data
    const updated = { ...post, bookmarked, collects }
    syncPostInList(updated)
    if (currentPost.value && currentPost.value.id === post.id) {
      currentPost.value = { ...currentPost.value, bookmarked, collects }
    }
  } catch (error) {
    notify.error('收藏失败')
  }
}

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
  const tags = customTagsInput.value.split(/[\s,\uFF0C]+/).map(item => item.trim()).filter(Boolean)
  if (!tags.length) {
    return
  }
  const merged = [...postForm.value.tags]
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
  postForm.value.tags = merged
  customTagsInput.value = ''
}

const removeTag = (tag) => {
  postForm.value.tags = postForm.value.tags.filter(item => item !== tag)
}

const handleImageUpload = async (event) => {
  const input = event.target
  const files = input?.files ? Array.from(input.files) : []
  if (files.length === 0) return

  const remainingSlots = 9 - postForm.value.images.length
  if (remainingSlots <= 0) {
    notify.warning('最多上传9张图片')
    if (input) {
      input.value = ''
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

  if (input) {
    input.value = ''
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
    
    // Update pendingFilesMap if needed
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
      const file = pendingFilesMap.value.get(previewUrl)
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
      currentPage.value = 'discover'
      await loadPosts()
    } else {
      notify.error(updateResponse.message || '发布失败')
    }
  } catch (error) {
    notify.error('发布失败，请重试')
  } finally {
    submittingPost.value = false
  }
}

onMounted(async () => {
  lastScrollY.value = window.scrollY || window.pageYOffset || 0
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  await loadPosts()
  loadUnreadCount()
  const postId = route.query.postId
  const commentId = route.query.commentId
  if (postId) {
    try {
      const response = await getDiscoverPostDetail(parseInt(postId))
      if (response.code === 200) {
        currentPost.value = response.data
        showPostModal.value = true
        syncPostInList(response.data)
        
        if (commentId) {
          currentPost.value.highlightCommentId = commentId
        }
      }
    } catch (error) {
      console.error('加载帖子详情失败', error)
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleWindowScroll)
})

const loadUnreadCount = async () => {
  try {
    const response = await getUnreadCount()
    if (response.code === 200) {
      unreadCount.value = response.data
    }
  } catch (error) {
    console.error('加载未读数量失败', error)
  }
}
</script>

<style scoped>

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f5f5;
  color: #333;
}

.discover-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
}


.top-nav {
  color: white;
  padding: 2px 2px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.logo {
  font-size: 18px;
  font-weight: bold;
  color: white;
  line-height: 1;
}

.search-input {
  flex: 1;
  max-width: 480px;
  margin: 0 5px;
  display: flex;
  align-items: center;
  background: #f0f0f0;
  border-radius: 18px;
  padding: 6px 12px;
  border: 2px solid #7494ec;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.search-input:hover {
  box-shadow: 0 0 10px rgba(116, 148, 236, 0.5), 0 0 20px rgba(116, 148, 236, 0.3);
}

.search-input:hover::before {
  content: '';
  position: absolute;
  left: -50%;
  top: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    to right,
    transparent,
    rgba(116, 148, 236, 0.3),
    transparent
  );
  transform: translateX(-100%);
  animation: shimmer 3s infinite;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.search-input i {
  color: #7494ec;
  margin-right: 8px;
  font-size: 16px;
  z-index: 1;
}

.search-input input {
  border: none;
  background: transparent;
  flex: 1;
  outline: none;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  z-index: 1;
}

.search-input input::placeholder {
  color: #666;
  font-weight: 400;
}

.nav-buttons {
  display: flex;
  gap: 20px;
}

.nav-btn {
  font-size: 24px;
  cursor: pointer;
}


.main-container {
  flex: 1;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}


.main-nav-buttons {
  display: flex;
  flex-direction: row;
  gap: 12px;
  margin: 0 16px;
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: white;
  transition: all 0.3s ease;
  position: relative;
  padding: 6px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.1);
}

.nav-button:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.nav-button .badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #ff2442;
  color: white;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(255, 36, 66, 0.3);
}

.nav-button.active {
  color: white;
  font-weight: 600;
  background: rgba(255, 36, 66, 0.8);
  box-shadow: 0 4px 8px rgba(255, 36, 66, 0.3);
}

.nav-button i {
  font-size: 16px;
}

.nav-button span {
  font-size: 12px;
  font-weight: 500;
}


.hamburger-menu {
  display: none;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #8f5151ff;
  font-size: 28px;
  border-radius: 8px;
  transition: background 0.3s;
}

.hamburger-menu:hover {
  background: rgba(255, 255, 255, 0.1);
}


.mobile-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 150px;
  overflow: hidden;
  z-index: 1000;
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mobile-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  color: #333;
  cursor: pointer;
  transition: background 0.3s;
}

.mobile-menu-item:hover {
  background: #f5f5f5;
}

.mobile-menu-item.active {
  color: #ff2442;
  background: #fff5f6;
}

.mobile-menu-item i {
  font-size: 18px;
}

.mobile-menu-item span {
  font-size: 14px;
}

.mobile-menu-item .badge {
  background: #ff2442;
  color: white;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
  font-weight: 500;
  margin-left: auto;
}


.contact-btn {
  padding: 6px 14px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(255, 36, 66, 0.3);
}

.contact-btn:hover {
  background: #ff3a56;
  box-shadow: 0 4px 8px rgba(255, 36, 66, 0.4);
  transform: translateY(-1px);
}


.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}


.category-tabs-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  padding: 5px 20px;
  border-bottom: 1px solid #f0f0f0;
  border-radius: 12px;
  position: sticky;
  top: 60px;
  z-index: 10;
  transition:
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.24s ease,
    box-shadow 0.24s ease;
  will-change: transform, opacity;
}

.category-tabs-wrapper.is-hidden {
  transform: translateY(calc(-100% - 18px));
  opacity: 0;
  pointer-events: none;
  box-shadow: none !important;
}

.category-tabs {
  display: flex;
  overflow-x: auto;
  gap: 20px;
  scrollbar-width: none;
  -ms-overflow-style: none;
  flex: 1;
}

.category-tabs::-webkit-scrollbar {
  display: none;
}

.tab {
  white-space: nowrap;
  font-size: 15px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 8px 16px;
  border-radius: 20px;
  background: #f5f5f5;
  margin-right: 8px;
}

.tab:hover {
  color: #333;
  background: #e8e8e8;
  transform: translateY(-2px);
}

.tab.active {
  color: white;
  font-weight: 600;
  background: #ff2442;
  box-shadow: 0 4px 8px rgba(255, 36, 66, 0.3);
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 20px;
}



.sort-dropdown {
  position: relative;
}

.sort-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.sort-btn:hover {
  border-color: #a71d2fff;
  color: #a12031ff;
  box-shadow: 0 4px 8px rgba(255, 36, 66, 0.2);
  transform: translateY(-1px);
}

.sort-btn i:first-child {
  font-size: 16px;
}

.sort-btn i:last-child {
  font-size: 12px;
}

.sort-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  min-width: 120px;
  z-index: 100;
}

.sort-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: all 0.2s;
}

.sort-option:hover {
  background: #f5f5f5;
}

.sort-option.active {
  color: #ff2442;
  background: #fff5f5;
}

.sort-option i {
  font-size: 18px;
}

.refresh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid #e8e8e8;
  border-radius: 50%;
  background: white;
  cursor: pointer;
  font-size: 20px;
  color: #666;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.refresh-btn:hover:not(:disabled) {
  border-color: #ff2442;
  color: #ff2442;
  box-shadow: 0 4px 8px rgba(255, 36, 66, 0.2);
  transform: rotate(180deg) scale(1.05);
}

.refresh-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.refresh-btn .bx-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}


.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  width: 100%;
  grid-column: 1 / -1;
}

.loading-spinner {
  font-size: 48px;
  color: #7494ec;
  margin-bottom: 16px;
}

.loading-text {
  color: #666;
  font-size: 14px;
}


.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  width: 100%;
  grid-column: 1 / -1;
}

.empty-state i {
  font-size: 64px;
  color: #ccc;
  margin-bottom: 16px;
}

.empty-state p {
  color: #999;
  font-size: 14px;
}


.content-list {
  padding: 20px;
  gap: 15px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  justify-content: center;
  flex: 1;
}


.post-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}




.image-grid {
  position: relative;
  width: 100%;
  padding-top: 100%; /* 1:1 比例容器 */
  overflow: hidden;
}

.post-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.post-card:hover .post-image {
  transform: scale(1.02);
}


.post-image {
  background: linear-gradient(120deg, #f0f0f0 30%, #e0e0e0 50%, #f0f0f0 70%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.post-image[src] {
  background: none;
  animation: none;
}


.post-content {
  padding: 12px;
  flex: 1;
}

.post-title {
  margin: 0 0 8px;
  font-size: 16px;
  line-height: 1.5;
  color: #1f2937;
}



.post-content p {
  margin: 0;
  font-size: 14px;
  line-height: 1.4;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-line;
}


.post-footer {
  padding: 0 12px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.author-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.author-name {
  font-size: 12px;
  color: #666;
}

.post-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: color 0.3s;
}

.stat-item:hover {
  color: #ff2442;
}

.stat-item i {
  font-size: 14px;
}

.stat-item .bx-star.collected {
  color: #ff2442;
}


@media (max-width: 1024px) {
  .content-list {
    grid-template-columns: repeat(3, 1fr);
    gap: 15px;
    padding: 15px;
  }
  
  .left-nav {
    width: 70px;
  }
  
  .nav-content {
    padding: 0 15px;
  }
  
  .search-input {
    margin: 0 20px;
  }
}

@media (max-width: 768px) {
  .content-list {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    padding: 10px;
  }

  .category-tabs-wrapper {
    padding: 12px 15px;
    top: 60px;
  }

  .category-tabs {
    gap: 16px;
  }

  .action-buttons {
    margin-left: 10px;
    gap: 8px;
  }

  .sort-btn {
    padding: 4px 8px;
    font-size: 12px;
  }

  .refresh-btn {
    width: 28px;
    height: 28px;
    font-size: 16px;
  }

  .nav-content {
    padding: 0 10px;
    flex-wrap: wrap;
    position: relative;
  }

  .main-nav-buttons {
    display: none;
  }

  .hamburger-menu {
    display: flex;
  }

  .search-input {
    margin: 10px 0;
    max-width: 100%;
    order: 3;
    width: 100%;
  }

  .nav-buttons {
    display: none;
  }
}

@media (max-width: 480px) {
  .content-list {
    grid-template-columns: 1fr;
  }
  
  .category-tabs {
    gap: 12px;
  }
  
  .tab {
    font-size: 13px;
  }
  
  .left-nav {
    gap: 15px;
  }
  
  .nav-item i {
    font-size: 18px;
  }
  
  .nav-item span {
    font-size: 10px;
  }
}


.post-page {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 40px 20px;
}

.post-container {
  width: 100%;
  max-width: 1200px;
  background: white;
  border-radius: 12px;
  margin-top: 25px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.post-container h2 {
  margin: 0 0 20px 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.post-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}


.post-form-horizontal {
  display: flex;
  gap: 30px;
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
.form-group textarea {
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
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

.more-tags {
  font-weight: 500;
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
  touch-action: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
  transition: all 0.3s;
  cursor: pointer;
}

.image-preview-item:active {
  opacity: 0.7;
  transform: scale(0.95);
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


.notification-page {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 40px 20px;
}

.notification-container {
  width: 100%;
  max-width: 600px;
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.notification-container h2 {
  margin: 0 0 20px 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  padding: 15px;
  border-radius: 8px;
  transition: background-color 0.3s;
}

.notification-item:hover {
  background: #f9f9f9;
}

.notification-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
}

.notification-content p {
  margin: 0 0 5px 0;
  font-size: 14px;
  line-height: 1.4;
  color: #333;
}

.notification-user {
  font-weight: 600;
  color: #333;
}

.notification-time {
  font-size: 12px;
  color: #999;
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
  .modal-content {
    flex-direction: column;
    width: 95vw;
    height: 95vh;
  }

  .modal-left {
    height: 50%;
  }

  .modal-right {
    width: 100%;
    height: 50%;
  }

  .post-page,
  .notification-page {
    padding: 20px 15px;
  }

  .post-container,
  .notification-container {
    padding: 20px;
    width: 100%;
  }
}

.discover-page {
  bottom: 50px;
  position: relative;
  min-height: 100vh;
  padding-bottom: 28px;
  color: var(--pc-ink);
}

.discover-page::before,
.discover-page::after {
  content: '';
  position: fixed;
  pointer-events: none;
  z-index: 0;
  filter: blur(8px);
}

.discover-page::before {
  top: 112px;
  left: -72px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(157, 41, 41, 0.16), transparent 68%);
}

.discover-page::after {
  right: -80px;
  bottom: 72px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(201, 145, 63, 0.14), transparent 70%);
}

.top-nav,
.main-container,
.image-preview-modal {
  position: relative;
  z-index: 1;
}

.nav-content {
  align-items: center !important;
  gap: 18px !important;
  background:
    linear-gradient(135deg, rgba(157, 41, 41, 0.05), rgba(255, 255, 255, 0.8)),
    rgba(255, 251, 246, 0.92) !important;
}

.logo {
  position: relative;
  padding-left: 16px;
  font-size: 26px !important;
  font-weight: 800 !important;
  letter-spacing: 0.06em;
  color: #2f241d !important;
}

.logo::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #9d2929, #c9913f);
  transform: translateY(-50%);
  box-shadow: 0 0 0 6px rgba(157, 41, 41, 0.08);
}

.search-input {
  position: relative;
  flex: 1;
  min-height: 56px;
  padding: 0 18px !important;
  border-radius: 20px !important;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(249, 243, 235, 0.9)) !important;
}

.search-input::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  pointer-events: none;
}

.search-input i {
  color: var(--pc-accent) !important;
}

.search-input input {
  height: 100%;
  padding: 0 5px;
  font-size: 15px !important;
  color: var(--pc-ink) !important;
}

.search-input input::placeholder {
  color: var(--pc-soft) !important;
}

.main-nav-buttons {
  gap: 10px !important;
  margin: 0 !important;
}

.nav-button {
  min-height: 48px;
  padding: 0 16px !important;
  font-weight: 600;
  box-shadow: none !important;
}

.nav-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(157, 41, 41, 0.08) !important;
}

.nav-button .badge,
.mobile-menu-item .badge {
  box-shadow: 0 8px 16px rgba(157, 41, 41, 0.22) !important;
}

.hamburger-menu {
  color: var(--pc-accent) !important;
  background: rgba(157, 41, 41, 0.06);
}

.mobile-menu {
  padding: 6px;
  border-radius: 18px !important;
  background: rgba(255, 251, 246, 0.98) !important;
}

.mobile-menu-item {
  border-radius: 14px !important;
}

.main-container {
  max-width: 1240px !important;
}

.category-tabs-wrapper {
  top: 95px;
  gap: 18px;
  padding: 18px 20px !important;
  border-radius: 24px !important;
  background:
    radial-gradient(circle at top right, rgba(201, 145, 63, 0.08), transparent 22%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(249, 244, 237, 0.92)) !important;
}

.category-tabs {
  gap: 12px !important;
}

.tab {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px !important;
  font-size: 13px !important;
  font-weight: 700 !important;
  letter-spacing: 0.02em;
}

.tab:hover {
  box-shadow: 0 10px 18px rgba(157, 41, 41, 0.08);
}

.action-buttons {
  gap: 12px !important;
}

.sort-btn,
.refresh-btn {
  min-height: 42px;
  border-radius: 16px !important;
}

.sort-btn {
  padding: 0 14px !important;
  font-weight: 600;
}

.refresh-btn {
  width: 42px !important;
  height: 42px !important;
}

.sort-menu {
  padding: 6px;
  border-radius: 18px !important;
}

.sort-option {
  border-radius: 14px !important;
  font-weight: 600;
}

.content-list {
  padding: 0 !important;
  margin-top: 20px;
  gap: 18px !important;
  align-items: stretch;
}

.post-card {
  border-radius: 24px !important;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 244, 238, 0.92)) !important;
  overflow: hidden;
}

.post-card:hover {
  transform: translateY(-4px) !important;
  box-shadow: 0 22px 38px rgba(74, 46, 23, 0.1) !important;
}

.image-grid {
  position: relative;
  overflow: hidden;
}

.image-grid::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 44%;
  background: linear-gradient(180deg, transparent, rgba(22, 15, 10, 0.12));
  pointer-events: none;
}

.post-image,
.preview-image {
  transition: transform 0.4s ease, filter 0.4s ease !important;
}

.post-card:hover .post-image {
  transform: scale(1.04) !important;
  filter: saturate(1.04);
}

.post-content {
  padding: 16px 16px 12px !important;
}

.post-title {
  margin-bottom: 10px !important;
  font-size: 17px !important;
  line-height: 1.45;
}

.post-content p {
  line-height: 1.75 !important;
}

.post-footer {
  padding: 0 16px 16px !important;
  gap: 12px;
}

.author-info {
  min-width: 0;
}

.author-avatar {
  width: 32px !important;
  height: 32px !important;
}

.author-name {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.post-stats {
  gap: 10px !important;
}

.stat-item {
  min-width: 0;
  padding: 6px 8px;
  border-radius: 999px;
  background: rgba(157, 41, 41, 0.05);
}

.stat-item:hover {
  background: rgba(157, 41, 41, 0.1);
}

.loading-container,
.empty-state {
  min-height: 280px;
}

.loading-spinner {
  color: var(--pc-accent) !important;
}

.post-page,
.notification-page {
  padding: 0 !important;
}

.post-container,
.notification-container {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(201, 145, 63, 0.08), transparent 18%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 244, 238, 0.94)) !important;
}

.post-container h2,
.notification-container h2 {
  margin-bottom: 24px !important;
  font-size: 28px !important;
}

.post-form-horizontal {
  gap: 24px !important;
}

.form-group {
  gap: 10px !important;
}

.form-group label {
  font-size: 13px !important;
  font-weight: 700 !important;
  letter-spacing: 0.04em;
  color: var(--pc-accent) !important;
  text-transform: uppercase;
}

.form-group input,
.form-group textarea,
.form-group select,
.tag-input-container input {
  min-height: 52px;
  padding: 14px 16px !important;
}

.form-group textarea {
  min-height: 180px !important;
}

.selected-tags {
  padding-bottom: 14px !important;
  border-bottom: 1px solid rgba(217, 207, 193, 0.8) !important;
}

.selected-tag-item,
.tag-item.active {
  border: 1px solid rgba(157, 41, 41, 0.1);
}

.tag-item {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px !important;
  border-radius: 999px !important;
  font-weight: 600;
}

.tag-item:hover {
  transform: translateY(-1px);
}

.image-upload-area {
  min-height: 186px;
  border-style: solid !important;
  border-color: rgba(157, 41, 41, 0.14) !important;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(248, 244, 238, 0.92)) !important;
}

.image-upload-area:hover {
  box-shadow: 0 16px 30px rgba(157, 41, 41, 0.08) !important;
}

.image-upload-area i {
  color: var(--pc-accent) !important;
}

.selected-images {
  gap: 12px !important;
}

.image-preview-item,
.preview-image {
  width: 80px !important;
  height: 80px !important;
}

.image-preview-item {
  border-radius: 14px;
  overflow: visible;
}

.preview-image {
  border-radius: 14px !important;
  border: 1px solid rgba(217, 207, 193, 0.8);
  box-shadow: 0 10px 18px rgba(44, 44, 44, 0.08);
}

.remove-image-btn {
  top: -8px !important;
  right: -8px !important;
  width: 24px !important;
  height: 24px !important;
  border-radius: 999px !important;
}

.drag-handle {
  bottom: -8px !important;
  padding: 4px 8px !important;
  border-radius: 999px !important;
}

.submit-post-btn {
  min-height: 54px;
  font-size: 15px !important;
  font-weight: 700 !important;
  letter-spacing: 0.03em;
  box-shadow: 0 16px 28px rgba(157, 41, 41, 0.18);
}

.submit-post-btn:hover {
  box-shadow: 0 22px 34px rgba(157, 41, 41, 0.24);
}

.notification-list {
  gap: 12px !important;
}

.notification-item {
  padding: 18px !important;
}

.notification-avatar {
  width: 46px !important;
  height: 46px !important;
}

.notification-user {
  color: var(--pc-ink) !important;
}

.image-preview-modal .modal-content {
  max-width: min(980px, 92vw);
  padding: 0 !important;
  border-radius: 26px !important;
  overflow: hidden;
}

.image-preview-modal .close-btn {
  top: 16px !important;
  right: 16px !important;
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(24, 18, 12, 0.54);
}

.preview-image-full {
  border-radius: 0 !important;
}

@media (max-width: 1024px) {
  .nav-content {
    gap: 14px !important;
  }

  .main-nav-buttons {
    gap: 8px !important;
  }

  .content-list {
    grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
  }
}

@media (max-width: 768px) {
  .discover-page::before,
  .discover-page::after {
    display: none;
  }

  .nav-content {
    gap: 10px !important;
  }

  .logo {
    font-size: 22px !important;
  }

  .search-input {
    min-height: 35px;
  }

  .category-tabs-wrapper {
    top: 90px;
    gap: 14px;
  }

  .content-list {
    grid-template-columns: 1fr !important;
    margin-top: 16px;
  }

  .post-form-horizontal {
    gap: 18px !important;
  }
}
</style>







