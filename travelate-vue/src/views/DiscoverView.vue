<template>
  <div class="discover-page" @click="closeMobileMenu">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <div class="nav-content" @click.stop>
        <div class="logo">Travelate</div>
        <div class="search-input">
          <i class='bx bx-search'></i>
          <input v-model="searchKeyword" type="text" :placeholder="$t('discover.searchPlaceholder')" @keyup.enter="loadPosts" />
        </div>
        <!-- 桌面端导航按钮 -->
        <div class="nav-buttons main-nav-buttons">
          <div 
            class="nav-button" 
            :class="{ active: currentPage === 'discover' }"
            @click="switchPage('discover')"
          >
            <i class='bx bx-compass'></i>
            <span>{{ $t('nav.discover') }}</span>
          </div>
          <div 
            class="nav-button" 
            :class="{ active: currentPage === 'post' }"
            @click="switchPage('post')"
          >
            <i class='bx bx-edit-alt'></i>
            <span>{{ $t('nav.post') }}</span>
          </div>
          <div 
            class="nav-button" 
            :class="{ active: currentPage === 'notification' }"
            @click="switchPage('notification')"
          >
            <i class='bx bx-bell'></i>
            <span>{{ $t('nav.notification') }}</span>
            <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
        </div>
        <!-- 汉堡菜单按钮 -->
        <div class="hamburger-menu" @click="toggleMobileMenu">
          <i :class="showMobileMenu ? 'bx bx-x' : 'bx bx-menu'"></i>
        </div>
        <!-- 移动端下拉菜单 -->
        <div v-if="showMobileMenu" class="mobile-menu">
          <div 
            class="mobile-menu-item" 
            :class="{ active: currentPage === 'discover' }"
            @click="switchPageMobile('discover')"
          >
            <i class='bx bx-compass'></i>
            <span>{{ $t('nav.discover') }}</span>
          </div>
          <div 
            class="mobile-menu-item" 
            :class="{ active: currentPage === 'post' }"
            @click="switchPageMobile('post')"
          >
            <i class='bx bx-edit-alt'></i>
            <span>{{ $t('nav.post') }}</span>
          </div>
          <div 
            class="mobile-menu-item" 
            :class="{ active: currentPage === 'notification' }"
            @click="switchPageMobile('notification')"
          >
            <i class='bx bx-bell'></i>
            <span>{{ $t('nav.notification') }}</span>
            <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 主容器 -->
    <div class="main-container">
      <!-- 发现页面 -->
      <div v-if="currentPage === 'discover'" class="main-content">
        <!-- 分类标签 -->
        <div class="category-tabs-wrapper">
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
                <span>{{ sortBy === 'latest' ? $t('discover.latest') : $t('discover.hot') }}</span>
                <i class='bx bx-chevron-down'></i>
              </button>
              <div v-if="showSortMenu" class="sort-menu">
                <div 
                  class="sort-option" 
                  :class="{ active: sortBy === 'latest' }"
                  @click="changeSort('latest')"
                >
                  <i class='bx bx-time'></i>
                  <span>{{ $t('discover.latest') }}</span>
                </div>
                <div 
                  class="sort-option" 
                  :class="{ active: sortBy === 'hot' }"
                  @click="changeSort('hot')"
                >
                  <i class='bx bx-hot'></i>
                  <span>{{ $t('discover.hot') }}</span>
                </div>
              </div>
            </div>
            <button class="refresh-btn" @click="handleRefresh" :disabled="isRefreshing">
              <i :class="isRefreshing ? 'bx bx-loader-alt bx-spin' : 'bx bx-refresh'"></i>
            </button>
          </div>
        </div>

        <!-- 内容列表 -->
        <div class="content-list">
          <!-- 加载中状态 -->
          <div v-if="loadingPosts" class="loading-container">
            <div class="loading-spinner">
              <i class='bx bx-loader-alt bx-spin'></i>
            </div>
            <p class="loading-text">{{ $t('discover.loading') }}</p>
          </div>
          <!-- 帖子列表 -->
          <template v-else>
            <div v-if="displayPosts.length === 0" class="empty-state">
              <i class='bx bx-file-blank'></i>
              <p>{{ $t('discover.noPosts') }}</p>
            </div>
            <div 
              v-else
              v-for="post in displayPosts" 
              :key="post.id"
              class="post-card"
              @click="openPostModal(post)"
            >
            <!-- 图片网格 -->
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

            <!-- 帖子内容 -->
            <div class="post-content">
              <p>{{ post.content }}</p>
            </div>

            <!-- 作者信息和互动栏 -->
            <div class="post-footer">
              <div class="author-info">
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

      <!-- 发布页面 -->
      <div v-else-if="currentPage === 'post'" class="main-content post-page">
        <div class="post-container">
          <h2>{{ $t('discover.newPost') }}</h2>
          <div class="post-form-horizontal">
            <!-- 左侧：标题、内容、分类 -->
            <div class="post-form-left">
              <div class="form-group">
                <label>{{ $t('discover.title') }}</label>
                <input v-model="postForm.title" type="text" :placeholder="$t('discover.title')" />
              </div>
              <div class="form-group">
                <label>{{ $t('discover.content') }}</label>
                <textarea v-model="postForm.content" :placeholder="$t('discover.content')"></textarea>
              </div>
              <div class="form-group">
                <label>{{ $t('discover.category') }}</label>
                <select v-model="postForm.category">
                  <option v-for="category in categories.filter(item => item.id !== 'all')" :key="category.id" :value="category.id">
                    {{ category.name }}
                  </option>
                </select>
              </div>
            </div>
            
            <!-- 右侧：图片、标签、发布按钮 -->
            <div class="post-form-right">
              <div class="form-group">
                <label>{{ $t('discover.addImage') }}</label>
                <div class="image-upload-area" @click="$refs.imageInput.click()">
                  <i class='bx bx-plus-circle'></i>
                  <span>{{ $t('discover.addImage') }}</span>
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
                <label>{{ $t('discover.tags') }} <span class="tag-count">({{ postForm.tags.length }}/10)</span></label>
                <div class="tags-container">
                  <!-- 已选标签 -->
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
                  <!-- 预设标签 -->
                  <div class="preset-tags">
                    <span
                      v-for="tag in presetTags"
                      :key="tag"
                      class="tag-item"
                      :class="{ active: postForm.tags.includes(tag), disabled: !postForm.tags.includes(tag) && postForm.tags.length >= 10 }"
                      @click="toggleTag(tag)"
                    >#{{ tag }}</span>
                  </div>
                  <!-- 自定义标签输入 -->
                  <div class="tag-input-container">
                    <input 
                      v-model="customTagsInput" 
                      type="text" 
                      :placeholder="$t('discover.addTagPlaceholder')" 
                      @keyup.enter="addCustomTags"
                      :disabled="postForm.tags.length >= 10"
                    />
                    <div class="tag-buttons">
                      <button class="tag-btn" @click.prevent="addCustomTags" :disabled="postForm.tags.length >= 10">{{ $t('discover.addTag') }}</button>
                    </div>
                  </div>
                </div>
              </div>
              <button class="submit-post-btn" :disabled="submittingPost" @click="submitPost">
                {{ submittingPost ? $t('common.loading') : $t('discover.publish') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 通知页面 -->
      <div v-else-if="currentPage === 'notification'" class="main-content notification-page">
        <div class="notification-container">
          <h2>{{ $t('nav.notification') }}</h2>
          <div class="notification-list">
            <div class="notification-item">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=user1" alt="User" class="notification-avatar" />
              <div class="notification-content">
                <p><span class="notification-user">小明</span> {{ $t('discover.collectedYourPost') }}</p>
                <span class="notification-time">10{{ $t('discover.minutesAgo') }}</span>
              </div>
            </div>
            <div class="notification-item">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=user2" alt="User" class="notification-avatar" />
              <div class="notification-content">
                <p><span class="notification-user">小红</span> {{ $t('discover.commentedYourPost') }}</p>
                <span class="notification-time">30{{ $t('discover.minutesAgo') }}</span>
              </div>
            </div>
            <div class="notification-item">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=user3" alt="User" class="notification-avatar" />
              <div class="notification-content">
                <p><span class="notification-user">小李</span> {{ $t('discover.followedYou') }}</p>
                <span class="notification-time">1{{ $t('discover.hoursAgo') }}</span>
              </div>
            </div>
            <div class="notification-item">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=system" alt="System" class="notification-avatar" />
              <div class="notification-content">
                <p><span class="notification-user">{{ $t('discover.system') }}</span> {{ $t('discover.postFeatured') }}</p>
                <span class="notification-time">2{{ $t('discover.hoursAgo') }}</span>
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
import { computed, onMounted, ref, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  createDiscoverPost,
  getDiscoverPostDetail,
  getDiscoverPosts,
  getUnreadCount,
  toggleDiscoverPostCollect,
  uploadPostImage,
  updateDiscoverPost
} from '../api/app'
import PostDetailModal from '../components/PostDetailModal.vue'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const categories = computed(() => [
  { id: 'all', name: t('discover.categoryAll') },
  { id: 'food', name: t('discover.presetTags.food') },
  { id: 'travel', name: t('discover.presetTags.travel') },
  { id: 'hotel', name: t('discover.presetTags.hotel') },
  { id: 'transport', name: t('discover.presetTags.transport') },
  { id: 'tips', name: t('discover.presetTags.guide') }
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
const postForm = ref({
  title: '',
  content: '',
  category: 'travel',
  tags: [],
  images: []
})
const pendingFilesMap = ref(new Map())
const customTagsInput = ref('')
const presetTags = computed(() => [
  t('discover.presetTags.travel'),
  t('discover.presetTags.food'),
  t('discover.presetTags.scenery'),
  t('discover.presetTags.guide'),
  t('discover.presetTags.hotel'),
  t('discover.presetTags.transport')
])
const selectedImages = computed(() => postForm.value.images)
const displayPosts = computed(() => posts.value)
const draggedIndex = ref(null)
const showImagePreview = ref(false)
const previewImageSrc = ref('')

// 触摸事件相关变量
const touchStartX = ref(0)
const touchStartY = ref(0)
const touchDragged = ref(false)
const touchDraggedIndex = ref(null)

const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value
}

const closeMobileMenu = () => {
  showMobileMenu.value = false
  showSortMenu.value = false
}

const switchPage = (page) => {
  if (page === 'notification') {
    router.push('/notification')
    return
  }
  currentPage.value = page
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
      notify.error(response.message || t('discover.loadPostsFailed'))
    }
  } catch (error) {
    notify.error(t('discover.loadPostsFailedRetry'))
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
      notify.error(response.message || t('discover.loadDetailFailed'))
    }
  } catch (error) {
    notify.error(t('discover.loadDetailFailedRetry'))
  }
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
      notify.error(response.message || t('discover.operationFailed'))
      return
    }
    const { bookmarked, collects } = response.data
    const updated = { ...post, bookmarked, collects }
    syncPostInList(updated)
    if (currentPost.value && currentPost.value.id === post.id) {
      currentPost.value = { ...currentPost.value, bookmarked, collects }
    }
  } catch (error) {
    notify.error(t('discover.collectFailed'))
  }
}

const toggleTag = (tag) => {
  const current = postForm.value.tags
  const exists = current.includes(tag)
  if (!exists && current.length >= 10) {
    notify.warning(t('discover.maxTags'))
    return
  }
  postForm.value.tags = exists ? current.filter(item => item !== tag) : [...current, tag]
}

const addCustomTags = () => {
  const tags = customTagsInput.value.split(/[\s,，]+/).map(item => item.trim()).filter(Boolean)
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
    notify.warning(t('discover.maxTags'))
  }
  postForm.value.tags = merged
  customTagsInput.value = ''
}

const removeTag = (tag) => {
  postForm.value.tags = postForm.value.tags.filter(item => item !== tag)
}

const handleImageUpload = async (event) => {
  const files = event.target.files ? Array.from(event.target.files) : []
  if (files.length === 0) return
  
  const remainingSlots = 9 - postForm.value.images.length
  if (remainingSlots <= 0) {
    notify.warning(t('discover.maxImages'))
    return
  }
  
  const filesToUpload = files.slice(0, remainingSlots)
  filesToUpload.forEach(file => {
    const previewUrl = URL.createObjectURL(file)
    postForm.value.images.push(previewUrl)
    pendingFilesMap.value.set(previewUrl, file)
  })
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
    category: 'travel',
    tags: [],
    images: []
  }
  pendingFilesMap.value = new Map()
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

// 触摸事件处理
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
  
  // 检测是否有明显的拖动
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
    notify.warning(t('discover.enterTitle'))
    return
  }
  if (!postForm.value.content.trim()) {
    notify.warning(t('discover.enterContent'))
    return
  }
  if (!postForm.value.images || postForm.value.images.length === 0) {
    notify.warning(t('discover.uploadImage'))
    return
  }
  submittingPost.value = true
  try {
    // 第一步：创建帖子（不提交图片）
    const createResponse = await createDiscoverPost({
      title: postForm.value.title.trim(),
      content: postForm.value.content.trim(),
      category: postForm.value.category,
      tags: postForm.value.tags,
      images: []
    })
    if (createResponse.code !== 200) {
      notify.error(createResponse.message || t('discover.publishFailed'))
      return
    }
    const postId = createResponse.data.id

    // 第二步：上传图片到OSS（使用postId作为文件夹）
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
          throw new Error(response.message || '上传失败')
        }
      }
    }

    // 第三步：更新帖子，添加图片URL
    const updateResponse = await updateDiscoverPost(postId, {
      title: postForm.value.title.trim(),
      content: postForm.value.content.trim(),
      category: postForm.value.category,
      tags: postForm.value.tags,
      images: ossUrls
    })
    if (updateResponse.code === 200) {
      notify.success(t('discover.publishSuccess'))
      resetPostForm()
      currentPage.value = 'discover'
      await loadPosts()
    } else {
      notify.error(updateResponse.message || t('discover.publishFailed'))
    }
  } catch (error) {
    notify.error(t('discover.publishFailedRetry'))
  } finally {
    submittingPost.value = false
  }
}

onMounted(async () => {
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
        // 传递 commentId 给模态框
        if (commentId) {
          currentPost.value.highlightCommentId = commentId
        }
      }
    } catch (error) {
      console.error('加载帖子详情失败', error)
    }
  }
})

const loadUnreadCount = async () => {
  try {
    const response = await getUnreadCount()
    if (response.code === 200) {
      unreadCount.value = response.data
    }
  } catch (error) {
    console.error('获取未读消息数量失败', error)
  }
}
</script>

<style scoped>
/* 全局样式重置 */
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

/* 顶部红色导航栏 */
.top-nav {
  color: white;
  padding: 10px 0;
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
  padding: 0 20px;
  width: 100%;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: white;
}

.search-input {
  flex: 1;
  max-width: 600px;
  margin: 0 40px;
  display: flex;
  align-items: center;
  background: #f0f0f0;
  border-radius: 20px;
  padding: 8px 15px;
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
  margin-right: 10px;
  font-size: 18px;
  z-index: 1;
}

.search-input input {
  border: none;
  background: transparent;
  flex: 1;
  outline: none;
  font-size: 16px;
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

/* 主容器 */
.main-container {
  flex: 1;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

/* 主导航按钮 */
.main-nav-buttons {
  display: flex;
  flex-direction: row;
  gap: 20px;
  margin: 0 20px;
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  color: white;
  transition: all 0.3s;
  position: relative;
}

.nav-button .badge {
  position: absolute;
  top: -5px;
  right: -10px;
  background: #ff2442;
  color: white;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
  font-weight: 500;
}

.nav-button.active {
  color: white;
  font-weight: 600;
}

.nav-button i {
  font-size: 18px;
}

.nav-button span {
  font-size: 12px;
}

/* 汉堡菜单按钮 */
.hamburger-menu {
  display: none;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: white;
  font-size: 24px;
  border-radius: 8px;
  transition: background 0.3s;
}

.hamburger-menu:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 移动端下拉菜单 */
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

/* 联系按钮 */
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

/* 主内容区 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 分类标签 */
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
  color: #333;
  cursor: pointer;
  transition: all 0.3s;
  padding: 5px 0;
}

.tab.active {
  color: #ff2442;
  font-weight: 600;
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
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.3s;
}

.sort-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
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
  width: 32px;
  height: 32px;
  border: 1px solid #e8e8e8;
  border-radius: 50%;
  background: white;
  cursor: pointer;
  font-size: 18px;
  color: #666;
  transition: all 0.3s;
}

.refresh-btn:hover:not(:disabled) {
  border-color: #ff2442;
  color: #ff2442;
  transform: rotate(180deg);
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

/* 加载中状态 */
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

/* 空状态 */
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

/* 内容列表 */
.content-list {
  padding: 20px;
  gap: 15px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  justify-content: center;
  flex: 1;
}

/* 帖子卡片 */
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

/* 图片网格 */
.image-grid {
  position: relative;
  width: 100%;
  padding-top: 100%; /* 1:1 比例 */
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

/* 图片加载效果 */
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

/* 帖子内容 */
.post-content {
  padding: 12px;
  flex: 1;
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

/* 帖子底部 */
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

/* 响应式设计 */
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

  .sort-btn span {
    display: none;
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

/* 发布页面样式 */
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
  padding: 30px;
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

/* 左右布局的发布表单 */
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

/* 标签容器 */
.tags-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

/* 标签数量提示 */
.tag-count {
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

/* 已选标签 */
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

/* 预设标签 */
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

/* 标签输入容器 */
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

/* 标签按钮 */
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

/* 通知页面样式 */
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

/* 响应式设计 */
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
</style>
