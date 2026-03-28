<template>
  <div class="user-homepage-page">
    <div class="page-topbar">
      <button class="icon-btn" @click="goBack">
        <i class='bx bx-chevron-left'></i>
      </button>
      <span class="topbar-title">个人主页</span>
      <div v-if="isCurrentUser" class="topbar-actions">
        <button
          v-if="isEditing"
          class="topbar-secondary"
          :disabled="saving"
          @click="cancelEditing"
        >
          取消编辑
        </button>
        <button
          class="topbar-action"
          :disabled="saving"
          @click="handleTopAction"
        >
          {{ saving ? '保存中...' : (isEditing ? '保存' : '编辑主页') }}
        </button>
      </div>
      <div v-else class="topbar-placeholder"></div>
    </div>

    <div class="hero-card">
      <div class="hero-main" :style="coverStyle">
        <button
          v-if="isCurrentUser && isEditing"
          class="cover-upload-btn"
          @click="triggerCoverInput"
        >
          <i class='bx bx-image-add'></i>
          更换封面
        </button>
        <input
          ref="coverInput"
          type="file"
          accept="image/*"
          @change="handleCoverUpload"
          style="display: none"
        >
        <img :src="user.avatar || defaultAvatar" alt="avatar" class="hero-avatar" />

        <div class="hero-body">
          <div class="hero-title-row">
            <div>
              <h1>{{ user.nickname || user.username || 'YayFolk 用户' }}</h1>
            </div>

            <span v-if="roleLabel" class="role-chip">
              <i :class="roleIcon"></i>
              {{ roleLabel }}
            </span>
          </div>


          <div v-if="isCurrentUser && isEditing" class="editing-banner">
            <i class='bx bx-edit'></i>
            正在编辑主页，修改完成后点击右上角保存
          </div>

          <div v-if="!isCurrentUser" class="hero-actions">
            <button
              class="secondary-btn"
              :class="{ active: isFollowing }"
              :disabled="followSubmitting"
              @click="toggleFollow"
            >
              {{ followSubmitting ? '处理中...' : (isFollowing ? '取消关注' : '关注') }}
            </button>
            <button
              class="primary-btn"
              :disabled="!isFollowing || followSubmitting"
              @click="contactUser"
            >
              <i class='bx bx-message-square-detail'></i>
              联系
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <span class="stat-value">{{ summary.postCount || 0 }}</span>
        <span class="stat-label">帖子</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ displayFollowerCount }}</span>
        <span class="stat-label">粉丝</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ summary.totalViews || 0 }}</span>
        <span class="stat-label">浏览</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ summary.badgeCount || 0 }}</span>
        <span class="stat-label">成就</span>
      </div>
    </div>

    <div class="overview-layout">
      <div class="section-card summary-card">
        <div class="section-head">
          <h2>{{ isCurrentUser ? '主页简介' : '关于 TA' }}</h2>
          <span class="section-side">{{ displayLocation || '未填写地点' }}</span>
        </div>

        <template v-if="isCurrentUser && isEditing">
          <div class="edit-grid">
            <div class="inline-editor">
              <label>主页简介</label>
              <textarea
                v-model="draft.bio"
                rows="4"
                maxlength="200"
                placeholder="写一句主页简介，让别人知道你在分享什么。"
              ></textarea>
              <span class="editor-hint">{{ draft.bio.length }}/200</span>
            </div>
            <div class="inline-editor compact">
              <label>主页地点</label>
              <input
                v-model="draft.location"
                type="text"
                maxlength="100"
                placeholder="例如：杭州 / 西安 / 景德镇"
              >
            </div>
          </div>
        </template>
        <template v-else>
          <p class="summary-text">{{ displayBio }}</p>
          <div class="summary-facts">
            <span v-if="displayLocation"><i class='bx bx-map'></i>{{ displayLocation }}</span>
            <span v-if="user.createTime"><i class='bx bx-calendar'></i>{{ user.createTime }} 入驻</span>
            <span v-if="roleLabel"><i :class="roleIcon"></i>{{ roleLabel }}</span>
          </div>
        </template>
      </div>

      <div
        v-if="showProfileCard || canEditProfileCard"
        class="section-card"
        :class="{ 'profile-card': !canEditProfileCard }"
      >
        <div class="section-head">
          <h2>{{ canEditProfileCard ? '编辑主页卡片' : '商家名片' }}</h2>
          <span class="section-side">{{ canEditProfileCard ? '编辑完成后保存，预览时才展示卡片效果' : '展示服务与品牌' }}</span>
        </div>

        <template v-if="canEditProfileCard">
          <div class="edit-grid">
            <div class="inline-editor compact">
              <label>商家名称</label>
              <input
                v-model="draft.shopName"
                type="text"
                maxlength="100"
                placeholder="例如：西湖纸伞工坊"
              >
            </div>
            <div class="inline-editor">
              <label>商家介绍</label>
              <textarea
                v-model="draft.shopIntro"
                rows="4"
                maxlength="500"
                placeholder="介绍你的店铺、手艺和服务特色。"
              ></textarea>
              <span class="editor-hint">{{ draft.shopIntro.length }}/500</span>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="merchant-content">
            <div class="merchant-name-row">
              <i class='bx bxs-store'></i>
              <span>{{ displayShopName }}</span>
            </div>
            <p>{{ displayShopIntro }}</p>
          </div>
        </template>
      </div>
    </div>

    <div class="section-card">
      <div class="section-head">
        <h2>成就展示</h2>
        <span class="section-side">{{ unlockedBadges.length }} / {{ badges.length }}</span>
      </div>

      <div v-if="unlockedBadges.length > 0" class="badge-grid">
        <div v-for="badge in unlockedBadges.slice(0, 6)" :key="badge.code" class="badge-card">
          <div class="badge-icon">
            <i :class="badgeIcon(badge.type)"></i>
          </div>
          <div class="badge-text">
            <strong>{{ badge.name }}</strong>
            <span>{{ badge.description }}</span>
          </div>
        </div>
      </div>

      <div v-else class="empty-block">
        <i class='bx bx-medal'></i>
        <p>暂时还没有公开成就</p>
      </div>
    </div>

    <div class="section-card">
      <div class="section-head">
        <h2>{{ isCurrentUser ? '我发布的内容' : 'TA 的内容' }}</h2>
        <span class="section-side">{{ posts.length }} 篇 · {{ summary.totalViews || 0 }} 浏览</span>
      </div>

      <div v-if="posts.length > 0" class="posts-list">
        <article
          v-for="post in posts"
          :key="post.id"
          class="post-card"
          :class="{ disabled: isEditing }"
          @click="openPost(post)"
        >
          <div v-if="post.images && post.images.length > 0" class="post-cover-wrap">
            <img :src="post.images[0]" alt="post" class="post-cover" />
            <span v-if="post.images.length > 1" class="post-image-count">+{{ post.images.length - 1 }}</span>
          </div>

          <div class="post-body">
            <h3>{{ post.title || '未命名帖子' }}</h3>
            <p class="post-content">{{ post.content }}</p>

            <div v-if="post.hashtags && post.hashtags.length > 0" class="post-tags">
              <span v-for="tag in post.hashtags.slice(0, 4)" :key="tag">#{{ tag }}</span>
            </div>

            <div class="post-footer">
              <span>{{ post.time }}</span>
              <div class="post-stats">
                <span><i class='bx bx-show'></i>{{ post.viewCount || 0 }}</span>
                <span><i class='bx bx-star'></i>{{ post.collects || 0 }}</span>
                <span><i class='bx bx-comment'></i>{{ post.comments || 0 }}</span>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="empty-block">
        <i class='bx bx-file-blank'></i>
        <p>{{ isCurrentUser ? '你还没有发布内容' : 'TA 还没有发布内容' }}</p>
      </div>
    </div>

    <PostDetailModal
      :visible="showDetailModal"
      :post="currentPost"
      @close="closeDetailModal"
      @update="handlePostUpdate"
    />
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getDiscoverPostDetail,
  followUser,
  getHomepageSettings,
  getUserHomepage,
  unfollowUser,
  updateHomepageSettings,
  uploadImage
} from '../api/app'
import PostDetailModal from '../components/PostDetailModal.vue'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const route = useRoute()
const router = useRouter()

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=travelate-user'
const defaultBio = '这个人很低调，还没有留下主页简介。'

const loading = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const isCurrentUser = ref(false)
const isFollowing = ref(false)
const followSubmitting = ref(false)
const user = ref({})
const summary = ref({})
const badges = ref([])
const posts = ref([])
const showDetailModal = ref(false)
const currentPost = ref(null)
const coverInput = ref(null)

const draft = reactive({
  bio: '',
  location: '',
  shopName: '',
  shopIntro: '',
  shopCover: ''
})

const currentUser = (() => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
})()

const profileUserId = computed(() => {
  const routeId = Number(route.params.userId || route.query.userId || 0)
  if (routeId > 0) {
    return routeId
  }
  const selfId = Number(currentUser?.id || 0)
  return selfId > 0 ? selfId : 0
})

const displayBio = computed(() => {
  if (isCurrentUser.value && isEditing.value) {
    return draft.bio || defaultBio
  }
  return user.value.bio || defaultBio
})

const displayLocation = computed(() => {
  if (isCurrentUser.value && isEditing.value) {
    return draft.location
  }
  return user.value.location || ''
})

const canEditProfileCard = computed(() => Boolean(isCurrentUser.value && isEditing.value && user.value.isMerchant))

const displayShopName = computed(() => {
  if (!user.value.isMerchant) {
    return ''
  }
  if (canEditProfileCard.value) {
    return draft.shopName || user.value.nickname || '未命名商家名片'
  }
  return user.value.shopName || user.value.nickname || '未命名商家名片'
})

const displayShopIntro = computed(() => {
  if (!user.value.isMerchant) {
    return ''
  }
  const defaultIntro = '这位商家还没有填写店铺介绍。'
  if (canEditProfileCard.value) {
    return draft.shopIntro || defaultIntro
  }
  return user.value.shopIntro || defaultIntro
})

const displayFollowerCount = computed(() => Number(user.value.followerCount || 0))
const displayFollowingCount = computed(() => Number(user.value.followingCount || 0))

const heroTagline = computed(() => {
  if (user.value.isMerchant && canEditProfileCard.value) {
    return draft.shopName?.trim() || draft.bio?.trim() || defaultBio
  }
  if (user.value.isMerchant) {
    return user.value.shopName || user.value.bio || defaultBio
  }
  return user.value.bio || defaultBio
})

const coverStyle = computed(() => {
  const cover = isCurrentUser.value && isEditing.value ? draft.shopCover : user.value.shopCover
  if (cover) {
    return { backgroundImage: `url(${cover})` }
  }
  return {
    backgroundImage: 'linear-gradient(135deg, #1f6feb 0%, #22c55e 100%)'
  }
})

const unlockedBadges = computed(() => badges.value.filter(item => item.unlocked))
const showProfileCard = computed(() => Boolean(user.value.isMerchant && !isEditing.value && (user.value.shopName || user.value.shopIntro)))

const roleLabel = computed(() => {
  if (user.value.role === 'admin') return '管理员'
  if (user.value.isMerchant) return '商家'
  return ''
})

const roleIcon = computed(() => {
  if (user.value.role === 'admin') return 'bx bxs-shield'
  if (user.value.isMerchant) return 'bx bxs-store'
  return 'bx bx-user'
})

const badgeIcon = (type) => {
  switch (type) {
    case 'order':
      return 'bx bx-package'
    case 'activity':
      return 'bx bx-calendar-event'
    case 'checkin':
      return 'bx bx-check-circle'
    case 'post':
      return 'bx bx-edit-alt'
    case 'partner':
      return 'bx bx-group'
    case 'history':
      return 'bx bx-compass'
    default:
      return 'bx bx-medal'
  }
}

const syncFollowState = () => {
  if (isCurrentUser.value) {
    isFollowing.value = false
    return
  }
  isFollowing.value = Boolean(user.value.isFollowing)
}

const syncDraftFromUser = (source) => {
  draft.bio = source?.bio || ''
  draft.location = source?.location || ''
  draft.shopName = source?.shopName || ''
  draft.shopIntro = source?.shopIntro || ''
  draft.shopCover = source?.shopCover || ''
}

const loadHomepage = async () => {
  if (!profileUserId.value) {
    notify.error('未找到用户信息')
    return
  }

  loading.value = true
  try {
    const response = await getUserHomepage(profileUserId.value)
    if (response.code !== 200 || !response.data) {
      notify.error(response.message || '加载个人主页失败')
      return
    }

    isCurrentUser.value = Boolean(response.data.isCurrentUser)
    user.value = response.data.user || {}
    summary.value = response.data.summary || {}
    badges.value = Array.isArray(response.data.badges) ? response.data.badges : []
    posts.value = Array.isArray(response.data.posts) ? response.data.posts : []
    syncDraftFromUser(user.value)
    syncFollowState()
  } catch (error) {
    notify.error('加载个人主页失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadHomepageSettings = async () => {
  const response = await getHomepageSettings()
  if (response.code !== 200 || !response.data) {
    throw new Error(response.message || '加载主页编辑数据失败')
  }
  syncDraftFromUser(response.data)
}

const handleTopAction = async () => {
  if (!isCurrentUser.value) return
  if (isEditing.value) {
    await saveHomepage()
    return
  }
  try {
    await loadHomepageSettings()
    isEditing.value = true
  } catch (error) {
    notify.error(error.message || '进入编辑模式失败')
  }
}

const cancelEditing = () => {
  isEditing.value = false
  syncDraftFromUser(user.value)
}

const triggerCoverInput = () => {
  coverInput.value?.click()
}

const handleCoverUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    notify.warning('请选择图片文件')
    event.target.value = ''
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    notify.warning('封面图片不能超过 10MB')
    event.target.value = ''
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', file)
    const response = await uploadImage(formData, 'homepage')
    if (response.code === 200 && response.data?.url) {
      draft.shopCover = response.data.url
      notify.success('主页封面已更新')
    } else {
      notify.error(response.message || '上传封面失败')
    }
  } catch (error) {
    notify.error('上传封面失败，请稍后重试')
  } finally {
    event.target.value = ''
  }
}

const saveHomepage = async () => {
  saving.value = true
  try {
    const payload = {
      bio: draft.bio.trim(),
      location: draft.location.trim()
    }

    if (user.value.isMerchant) {
      payload.shopName = draft.shopName.trim()
      payload.shopIntro = draft.shopIntro.trim()
      payload.shopCover = draft.shopCover
    }

    const response = await updateHomepageSettings(payload)
    if (response.code !== 200) {
      notify.error(response.message || '保存主页失败')
      return
    }

    user.value = {
      ...user.value,
      ...payload
    }

    const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
    if (raw) {
      try {
        const storedUser = JSON.parse(raw)
        const nextUser = {
          ...storedUser,
          ...payload
        }
        localStorage.setItem('user', JSON.stringify(nextUser))
        localStorage.setItem('userInfo', JSON.stringify(nextUser))
      } catch (error) {
        console.error('更新本地用户信息失败', error)
      }
    }

    isEditing.value = false
    notify.success('主页已保存，当前为预览模式')
  } catch (error) {
    notify.error('保存主页失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const persistCurrentUserFollowingCount = (followingCount) => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) return
  try {
    const storedUser = JSON.parse(raw)
    const nextUser = {
      ...storedUser,
      followingCount: Number(followingCount || 0)
    }
    localStorage.setItem('user', JSON.stringify(nextUser))
    localStorage.setItem('userInfo', JSON.stringify(nextUser))
  } catch (error) {
    console.error('更新本地关注统计失败', error)
  }
}

const toggleFollow = () => {
  if (!currentUser?.id || !profileUserId.value || isCurrentUser.value || followSubmitting.value) {
    return
  }
  handleFollowToggle()
}

const handleFollowToggle = async () => {
  const wasFollowing = isFollowing.value
  followSubmitting.value = true
  try {
    const response = wasFollowing
      ? await unfollowUser(profileUserId.value)
      : await followUser(profileUserId.value)

    if (response.code !== 200 || !response.data) {
      notify.error(response.message || (wasFollowing ? '取消关注失败' : '关注失败'))
      return
    }

    isFollowing.value = Boolean(response.data.following)
    user.value = {
      ...user.value,
      isFollowing: Boolean(response.data.following),
      followerCount: Number(response.data.followerCount || 0),
      followingCount: Number(user.value.followingCount || 0)
    }
    persistCurrentUserFollowingCount(response.data.followingCount)
    notify.success(wasFollowing ? '已取消关注' : '已关注')
  } catch (error) {
    notify.error(wasFollowing ? '取消关注失败，请稍后重试' : '关注失败，请稍后重试')
  } finally {
    followSubmitting.value = false
  }
}

const openPost = async (post) => {
  if (isEditing.value) return
  try {
    const response = await getDiscoverPostDetail(post.id)
    if (response.code === 200) {
      currentPost.value = response.data
      showDetailModal.value = true
      syncPost(response.data)
      return
    }
    notify.error(response.message || '加载帖子详情失败')
  } catch (error) {
    notify.error('加载帖子详情失败，请稍后重试')
  }
}

const syncPost = (updatedPost) => {
  const index = posts.value.findIndex(item => item.id === updatedPost.id)
  if (index === -1) return
  posts.value[index] = {
    ...posts.value[index],
    ...updatedPost
  }
}

const handlePostUpdate = (updatedPost) => {
  currentPost.value = updatedPost
  syncPost(updatedPost)
}

const closeDetailModal = () => {
  showDetailModal.value = false
  currentPost.value = null
}

const goBack = () => {
  if (isEditing.value) {
    cancelEditing()
    return
  }
  router.back()
}

const contactUser = () => {
  if (!isFollowing.value) {
    notify.warning('请先关注，再联系对方')
    return
  }
  if (!user.value.id) return
  router.push({
    path: '/notification',
    query: { userId: user.value.id }
  })
}

onMounted(() => {
  loadHomepage()
})

watch(profileUserId, () => {
  isEditing.value = false
  showDetailModal.value = false
  currentPost.value = null
  loadHomepage()
})
</script>

<style scoped>
.user-homepage-page {
  min-height: 100vh;
  padding: 20px;
  background:
    radial-gradient(circle at top left, rgba(34, 197, 94, 0.14), transparent 28%),
    radial-gradient(circle at top right, rgba(31, 111, 235, 0.16), transparent 34%),
    #f4f7fb;
}

.page-topbar {
  max-width: 1080px;
  margin: 0 auto 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-btn,
.topbar-action,
.topbar-secondary,
.primary-btn,
.secondary-btn,
.cover-upload-btn {
  border: none;
  cursor: pointer;
}

.icon-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  color: #16324f;
  font-size: 22px;
}

.topbar-title {
  flex: 1;
  font-size: 18px;
  font-weight: 700;
  color: #16324f;
}

.topbar-placeholder {
  min-width: 96px;
}

.topbar-action,
.topbar-secondary {
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
}

.topbar-action {
  background: #16324f;
  color: #fff;
}

.topbar-secondary {
  background: #edf3fb;
  color: #16324f;
}

.topbar-action:disabled,
.topbar-secondary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.hero-card,
.section-card {
  max-width: 1080px;
  margin: 0 auto 18px;
  border-radius: 28px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.75);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.hero-main {
  position: relative;
  padding: 200px 28px 30px;
  min-height: 300px;
  display: flex;
  gap: 22px;
  align-items: flex-end;
  background-size: cover;
  background-position: center;
  overflow: hidden;
}

.hero-main::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(10, 25, 47, 0.12) 0%, rgba(10, 25, 47, 0.58) 100%);
}

.cover-upload-btn {
  position: absolute;
  right: 28px;
  top: 28px;
  z-index: 10;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #fff;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.hero-avatar {
  position: relative;
  z-index: 2;
  width: 112px;
  height: 112px;
  border-radius: 28px;
  object-fit: cover;
  border: 4px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.15);
  background: #fff;
}

.hero-body {
  position: relative;
  z-index: 2;
  flex: 1;
}

.hero-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.hero-title-row h1 {
  margin: 0;
  font-size: 32px;
  line-height: 1.1;
  color: #fff;
}

.role-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.hero-bio {
  margin: 16px 0 0;
  color: rgba(255, 255, 255, 0.94);
  font-size: 15px;
  line-height: 1.7;
}

.hero-meta {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: rgba(255, 255, 255, 0.84);
  font-size: 13px;
}

.hero-meta span,
.summary-facts span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.editing-banner {
  margin-top: 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.hero-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
}

.primary-btn,
.secondary-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 18px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  min-width: 120px;
}

.primary-btn {
  background: linear-gradient(135deg, #16324f 0%, #1f6feb 100%);
  color: #fff;
  box-shadow: 0 12px 26px rgba(31, 111, 235, 0.2);
}

.primary-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  box-shadow: none;
}

.secondary-btn {
  background: rgba(255, 255, 255, 0.9);
  color: #16324f;
}

.secondary-btn.active {
  background: #dce9fb;
  color: #1f6feb;
}

.stats-grid {
  max-width: 1080px;
  margin: 0 auto 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  border-radius: 22px;
  padding: 22px 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #16324f;
}

.stat-label {
  font-size: 13px;
  color: #6b7a90;
}

.overview-layout {
  max-width: 1080px;
  margin: 0 auto 18px;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.95fr);
  gap: 18px;
}

.overview-layout .section-card {
  max-width: none;
  margin: 0;
}

.section-card {
  padding: 24px 26px 26px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.section-head h2 {
  margin: 0;
  font-size: 20px;
  color: #16324f;
}

.section-side {
  color: #6b7a90;
  font-size: 13px;
}

.summary-text {
  margin: 0;
  color: #2b3b52;
  font-size: 15px;
  line-height: 1.8;
}

.summary-facts {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #6b7a90;
  font-size: 13px;
}

.profile-card {
  background: linear-gradient(135deg, rgba(22, 50, 79, 0.95) 0%, rgba(31, 111, 235, 0.92) 100%);
  color: #fff;
}

.profile-card .section-head h2,
.profile-card .section-side,
.profile-card p,
.profile-card span,
.profile-card i,
.profile-card label {
  color: #fff;
}

.merchant-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.merchant-name-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
}

.edit-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.inline-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.inline-editor.compact {
  max-width: 360px;
}

.inline-editor label {
  font-size: 13px;
  font-weight: 600;
  color: #16324f;
}

.inline-editor input,
.inline-editor textarea {
  width: 100%;
  border: 1px solid #d7e1ec;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  padding: 12px 14px;
  font-size: 14px;
  color: #22344d;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  box-sizing: border-box;
}

.inline-editor input:focus,
.inline-editor textarea:focus {
  border-color: #1f6feb;
  box-shadow: 0 0 0 3px rgba(31, 111, 235, 0.12);
}

.inline-editor textarea {
  resize: vertical;
}

.editor-hint {
  color: #7b8aa0;
  font-size: 12px;
}

.posts-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.post-card {
  border-radius: 22px;
  overflow: hidden;
  background: #f8fafc;
  cursor: pointer;
  border: 1px solid #e8eef6;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.post-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
}

.post-card.disabled {
  cursor: default;
  opacity: 0.7;
}

.post-card.disabled:hover {
  transform: none;
  box-shadow: none;
}

.post-cover-wrap {
  position: relative;
  height: 220px;
  background: #dbe6f3;
}

.post-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-image-count {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.7);
  color: #fff;
  font-size: 12px;
}

.post-body {
  padding: 18px;
}

.post-body h3 {
  margin: 0;
  color: #16324f;
  font-size: 18px;
}

.post-content {
  margin: 12px 0 0;
  color: #42536b;
  font-size: 14px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.post-tags span {
  padding: 5px 10px;
  border-radius: 999px;
  background: #eaf1f9;
  color: #1f6feb;
  font-size: 12px;
}

.post-footer {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #6b7a90;
  font-size: 12px;
}

.post-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.post-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.badge-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.badge-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.08) 0%, rgba(34, 197, 94, 0.08) 100%);
}

.badge-icon {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #16324f;
  color: #fff;
  font-size: 22px;
  flex-shrink: 0;
}

.badge-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.badge-text strong {
  color: #16324f;
  font-size: 15px;
}

.badge-text span {
  color: #6b7a90;
  font-size: 13px;
  line-height: 1.6;
}

.empty-block {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8a97ab;
  gap: 10px;
}

.empty-block i {
  font-size: 42px;
}

@media (max-width: 980px) {
  .overview-layout,
  .stats-grid,
  .posts-list,
  .badge-grid {
    grid-template-columns: 1fr;
  }

  .hero-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-title-row,
  .post-footer,
  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .user-homepage-page {
    padding: 14px;
  }

  .hero-main,
  .section-card {
    padding-left: 18px;
    padding-right: 18px;
  }

  .hero-avatar {
    width: 88px;
    height: 88px;
    border-radius: 22px;
  }

  .hero-title-row h1 {
    font-size: 26px;
  }

  .hero-actions,
  .topbar-actions {
    flex-direction: column;
    width: 100%;
  }

  .topbar-placeholder {
    min-width: 0;
  }

  .topbar-action,
  .topbar-secondary,
  .secondary-btn,
  .primary-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
