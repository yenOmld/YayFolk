<template>
  <div class="user-homepage">
    <div class="shell">
      <div class="topbar">
        <button class="icon-btn" @click="goBack"><i class='bx bx-chevron-left'></i></button>
      </div>

      <section v-if="isAdminProfile" class="card blocked">
        <h1>{{ displayName }}</h1>
        <p>管理员账号不提供公开主页展示，请回到管理后台继续工作。</p>
        <div class="actions"><button class="ghost-btn" @click="goBack">返回</button><button v-if="isCurrentUser" class="primary-btn" @click="goAdmin">进入管理后台</button></div>
      </section>

      <template v-else>
        <section class="hero card">
          <div class="cover-box" :class="{ editable: isCurrentUser }" :style="coverStyle" @click="triggerCover">
            <div class="cover-mask"></div>
            <div v-if="isCurrentUser" class="media-hover cover-hover">
              <button class="camera-btn cover-camera" :disabled="coverUploading"><i class='bx bx-camera'></i></button>
              <span>{{ coverUploading ? '背景上传中...' : '更换背景图' }}</span>
            </div>
            <input ref="coverInput" class="hidden" type="file" accept="image/*" @change="handleCoverUpload">
            <div v-if="isCurrentUser" class="cover-settings-wrap">
              <button class="cover-settings-btn" @click.stop="toggleProfileMenu" aria-label="主页设置">
                <i class='bx bx-cog'></i>
              </button>
              <div v-if="showProfileMenu" class="cover-settings-menu" @click.stop>
                <!-- legacy homepage-settings entry removed
                  <span>主页设置</span>
                </button>
                -->
                <button @click="goEditProfile">
                  <i class='bx bx-user'></i>
                  <span>编辑资料</span>
                </button>
              </div>
            </div>
          </div>

          <div class="profile-panel">
            <div class="avatar-box" :class="{ editable: isCurrentUser }" @click="triggerAvatar">
              <img :src="user.avatar || defaultAvatar" class="avatar" :alt="displayName">
              <div v-if="isCurrentUser" class="media-hover avatar-hover">
                <button class="camera-btn avatar-camera" :disabled="avatarUploading"><i class='bx bx-camera'></i></button>
                <span>{{ avatarUploading ? '上传中...' : '更换头像' }}</span>
              </div>
              <input ref="avatarInput" class="hidden" type="file" accept="image/*" @change="handleAvatarUpload">
            </div>

            <div class="profile-copy">
              <div class="title-row">
                <div class="title-copy">
                  <div class="name-row">
                    <h1>{{ displayName }}</h1>
                    <span v-if="showRoleChip" class="role-chip"><i :class="roleIcon"></i>{{ roleLabel }}</span>
                  </div>
                </div>
              </div>
              <p v-if="displaySignature" class="signature">{{ displaySignature }}</p>
              <p class="bio">{{ displayBio }}</p>
              <div class="stats-row">
                <component
                  :is="item.action ? 'button' : 'div'"
                  v-for="item in heroStats"
                  :key="item.key"
                  class="stat"
                  :class="{ clickable: Boolean(item.action) }"
                  @click="item.action && item.action()"
                >
                  <strong>{{ item.value }}</strong>
                  <span>{{ item.label }}</span>
                </component>
              </div>
            </div>

            <div v-if="!isCurrentUser" class="hero-actions">
              <button class="primary-btn" :disabled="followSubmitting" @click="toggleFollow">{{ followSubmitting ? '处理中...' : followLabel }}</button>
              <button class="ghost-btn" @click="contactTarget">{{ contactLabel }}</button>
            </div>
          </div>
        </section>

        <section class="content card" v-if="!loading">
          <div class="tab-bar">
            <button v-for="tab in visibleTabs" :key="tab.key" :class="['tab-btn',{active:activeTab===tab.key}]" @click="switchTab(tab.key)">{{ tab.label }}<span>{{ tab.count }}</span></button>
          </div>

          <template v-if="activeTab==='posts'">
            <div v-if="posts.length" class="post-grid">
              <article v-for="post in posts" :key="post.id" class="post-card" @click="openPost(post)">
                <div class="media"><img v-if="post.images?.length" :src="post.images[0]" class="cover"><div v-else class="placeholder"><i class='bx bx-image-alt'></i></div></div>

                <button v-if="isCurrentUser" class="visibility-toggle-btn" @click.stop="togglePostVisibility(post)"><i :class="post.visibility === 'private' ? 'bx bx-lock-open-alt' : 'bx bx-lock-alt'"></i></button>

                <div class="card-body">
                  <h3>{{ post.title || '未命名帖子' }}</h3>
                  <div v-if="post.hashtags?.length" class="tags"><span v-for="tag in post.hashtags.slice(0,4)" :key="tag">#{{ tag }}</span></div>
                  <div class="meta"><span><i class='bx bx-show'></i>{{ post.viewCount || 0 }}</span><span><i class='bx bx-star'></i>{{ post.collects || 0 }}</span><span><i class='bx bx-comment'></i>{{ post.comments || 0 }}</span><span v-if="isCurrentUser" class="visibility-chip">{{ post.visibility === 'private' ? '私密' : '公开' }}</span></div>
                </div>
              </article>
            </div>
            <div v-else class="empty"><i class='bx bx-file-blank'></i><p>{{ isCurrentUser ? '你还没有发布内容' : 'TA 还没有发布内容' }}</p></div>
          </template>

          <template v-if="activeTab==='collections'">
            <div v-if="isCurrentUser" class="collection-banner">
              <div>
                <strong>收藏页当前{{ collectionVisibility === 'private' ? '私密' : '公开' }}</strong>
                <p>公开时别人可见你的收藏页；设为私密后，我的主页只展示动态和勋章。</p>
              </div>
              <button class="ghost-btn" @click="toggleHomepageCollectionVisibility">{{ collectionVisibility === 'private' ? '设为公开' : '设为私密' }}</button>
            </div>
            <div v-if="collections.length" class="post-grid">
              <article v-for="item in collections" :key="item.id" class="post-card" @click="openCollection(item)">
                <div class="media"><img v-if="item.coverImage" :src="item.coverImage" class="cover"><div v-else class="placeholder"><i class='bx bx-bookmark'></i></div></div>
                <button v-if="false && isCurrentUser" class="menu-btn" @click.stop="toggleCollectionVisibility(item)"><i class='bx bx-cog'></i></button>
                <div class="card-body">
                  <h3>{{ item.title || '未命名帖子' }}</h3>
                  <div class="meta"><span><i class='bx bx-bookmark'></i>{{ item.collectCount || 0 }}</span><span><i class='bx bx-comment'></i>{{ item.commentCount || 0 }}</span><span v-if="isCurrentUser">{{ item.visibility === 'private' ? '私密' : '公开' }}</span></div>
                </div>
              </article>
            </div>
            <div v-else class="empty"><i class='bx bx-bookmark'></i><p>{{ isCurrentUser ? '你还没有收藏内容' : 'TA 还没有公开收藏内容' }}</p></div>
          </template>

          <template v-if="activeTab==='achievements'">
            <div v-if="badges.length" class="badge-grid">
              <article
                v-for="badge in badges"
                :key="badge.code || badge.id"
                class="badge-card"
                :class="{ revealed: revealed[badge.code || badge.id], unlocked: badge.unlocked }"
                @click="toggleBadge(badge)"
              >
                <div class="badge-aura"></div>
                <div class="badge-head">
                  <div class="badge-medallion" :class="'badge-' + badge.type">
                    <div class="badge-icon" :class="'icon-' + badge.type">
                      <i :class="badgeIcon(badge.type)"></i>
                    </div>
                  </div>
                  <div class="badge-copy">
                    <div class="badge-topline">
                      <span class="badge-state">{{ badge.unlocked ? '已点亮' : '待点亮' }}</span>
                      <span class="badge-progress">{{ badgeProgress(badge) }}</span>
                    </div>
                    <h3>{{ badge.name || badge.badgeName }}</h3>
                    <p>{{ badge.description || badge.badgeDescription }}</p>
                  </div>
                </div>
                <div class="badge-detail">
                  <div class="badge-detail-row">
                    <span>当前进度</span>
                    <strong>{{ badgeProgress(badge) }}</strong>
                  </div>
                  <small>{{ revealed[badge.code || badge.id] ? '点击收起详情' : '点击展开详情' }}</small>
                </div>
              </article>
            </div>
            <div v-else class="empty"><i class='bx bx-medal'></i><p>暂时还没有成就展示</p></div>
          </template>

          <template v-if="activeTab==='activities'">
            <div class="filters">
              <select v-model="activityFilter.category"><option value="all">全部分类</option><option v-for="item in activityCategoryOptions" :key="item" :value="item">{{ item }}</option></select>
              <select v-model="activityFilter.time"><option value="all">全部时间</option><option value="upcoming">即将开始</option><option value="past">已结束</option></select>
              <select v-model="activityFilter.status"><option value="all">全部状态</option><option v-for="item in activityStatusOptions" :key="item" :value="item">{{ item }}</option></select>
            </div>
            <div v-if="filteredActivities.length" class="activity-grid">
              <article v-for="item in filteredActivities" :key="item.id" class="activity-card activity-preview-card" @click="openActivity(item)">
                <div class="activity-media">
                  <img v-if="item.coverImage" :src="item.coverImage" class="activity-cover">
                  <div v-else class="placeholder"><i class='bx bx-calendar-event'></i></div>
                  <span class="activity-preview-badge">{{ item.categoryName || item.heritageType || '非遗体验' }}</span>
                </div>
                <div class="activity-preview-body">
                  <h3 class="activity-preview-title">{{ item.title || '非遗活动' }}</h3>
                  <div class="activity-preview-head">
                    <span class="activity-preview-status">{{ activityStatusLabel(item) }}</span>
                  </div>
                  <p class="activity-preview-time">{{ activityPreviewTime(item) }}</p>
                  <p class="activity-preview-location">{{ item.location || '地点待定' }}</p>
                  <p class="activity-preview-summary">{{ item.subtitle || item.content || '线下体验活动' }}</p>
                  <div class="chips"><span>{{ item.categoryName || item.heritageType || '未分类' }}</span><span>{{ activityStatusLabel(item) }}</span></div>
                  <div class="meta"><span>{{ item.location || '地点待定' }}</span><span>{{ time(item.startTime) || '时间待定' }}</span></div>
                </div>
              </article>
            </div>
            <div v-else class="empty"><i class='bx bx-calendar-x'></i><p>当前筛选条件下还没有活动</p></div>
          </template>

          <template v-if="activeTab==='reviews'">
            <div class="review-summary">
              <div class="sum-box"><strong>{{ score(reviewSummary.averageScore) }}</strong><span>综合评分</span></div>
              <div class="sum-box"><strong>{{ reviewSummary.reviewCount || 0 }}</strong><span>评价数量</span></div>
              <div class="sum-box"><strong>{{ user.merchantKeyword || user.shopName || '非遗手作' }}</strong><span>关键词</span></div>
            </div>
            <div v-if="reviews.length" class="stack review-list">
              <article v-for="item in reviews" :key="item.id" class="review-card"><strong>{{ item.nickname || item.username || '匿名用户' }}</strong><span>{{ score(item.score) }} 分</span><p>{{ item.content || '该用户没有留下文字评价。' }}</p></article>
            </div>
            <div v-else class="empty"><i class='bx bx-message-rounded-dots'></i><p>目前还没有可展示的评价内容</p></div>
          </template>
        </section>

        <div v-else class="card empty loading"><i class='bx bx-loader-alt bx-spin'></i><p>正在加载主页内容...</p></div>
      </template>
    </div>

    <div v-if="showList" class="mask" @click.self="closeList">
      <div class="modal card">
        <div class="modal-head"><h3>{{ listTitle }}</h3><button class="icon-btn" @click="closeList"><i class='bx bx-x'></i></button></div>
        <div v-if="listLoading" class="empty"><i class='bx bx-loader-alt bx-spin'></i><p>正在加载...</p></div>
        <div v-else class="stack list-stack">
          <button v-for="item in listData" :key="item.id + '-' + listType" class="list-row" @click="openHomepage(item.id)">
            <img :src="item.avatar || defaultAvatar" class="mini-avatar">
            <div>
              <strong>{{ item.nickname || item.username || '匿名用户' }}</strong>
              <span>{{ listPrimary(item) }}</span>
              <small>{{ listMeta(item) }}</small>
            </div>
            <button v-if="item.id !== currentUserId" class="ghost-btn small-btn" :class="{following:item.isFollowing}" @click.stop="toggleListFollow(item)">{{ item.isFollowing ? '已关注' : '+ 关注' }}</button>
          </button>
          <div v-if="!listData.length" class="empty small">{{ listEmpty }}</div>
        </div>
      </div>
    </div>

    <PostDetailModal :visible="showDetail" :post="detailPost" @close="closeDetail" @update="syncPost" />
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteMyDiscoverPost, followUser, getCollectedBy, getDiscoverPostDetail, getFollowers, getFollowing, getUserHomepage, unfollowUser, updateDiscoverPostVisibility, updateHomepageSettings, updateUserProfile, uploadAvatar, uploadImage } from '../api/app'
import PostDetailModal from '../components/PostDetailModal.vue'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm
const route = useRoute()
const router = useRouter()
const defaultAvatar = '/default-avatar.svg'
const loading = ref(false)
const coverUploading = ref(false)
const avatarUploading = ref(false)
const followSubmitting = ref(false)
const showList = ref(false)
const listLoading = ref(false)
const listTitle = ref('')
const listEmpty = ref('')
const listType = ref('followers')
const listData = ref([])
const showDetail = ref(false)
const detailPost = ref(null)
const showProfileMenu = ref(false)
const coverInput = ref(null)
const avatarInput = ref(null)
const user = ref({})
const summary = ref({})
const posts = ref([])
const collections = ref([])
const badges = ref([])
const activities = ref([])
const reviews = ref([])
const reviewSummary = ref({})
const revealed = ref({})
const isCurrentUser = ref(false)
const isFollowingUser = ref(false)
const activeTab = ref('posts')
const activityFilter = ref({ category: 'all', time: 'all', status: 'all' })

const readUser = () => { try { return JSON.parse(localStorage.getItem('user') || localStorage.getItem('userInfo') || 'null') } catch { return null } }
const saveUserPatch = (patch) => { const current = readUser(); if (!current) return; const next = { ...current, ...patch }; localStorage.setItem('user', JSON.stringify(next)); localStorage.setItem('userInfo', JSON.stringify(next)) }
const currentUserId = computed(() => Number(readUser()?.id || 0))
const profileUserId = computed(() => Number(route.params.userId || route.query.userId || currentUserId.value || 0))
const homepageBackTo = computed(() => typeof route.query.backTo === 'string' ? route.query.backTo : '')
const isAdminProfile = computed(() => user.value.role === 'admin')
const isMerchantProfile = computed(() => !isAdminProfile.value && (user.value.role === 'merchant' || user.value.isMerchant))
const collectionVisibility = computed(() => user.value.collectionVisibility === 'private' ? 'private' : 'public')
const collectionsTabVisible = computed(() => isCurrentUser.value || collectionVisibility.value === 'public')
const displayName = computed(() => user.value.nickname || user.value.username || 'YayFolk 用户')
const displaySignature = computed(() => {
  const signature = String(user.value.signature || '').trim()
  if (signature) {
    return signature
  }
  return isCurrentUser.value ? '还没写个性签名，去“我的主页”里补一句吧。' : ''
})
const displayBio = computed(() => isMerchantProfile.value ? (user.value.shopIntro || user.value.bio || '这位商家还没有留下店铺简介。') : (user.value.bio || '这个人很低调，还没有留下主页简介。'))
const roleLabel = computed(() => isAdminProfile.value ? '管理员' : (isMerchantProfile.value ? '商家' : '用户'))
const roleIcon = computed(() => isAdminProfile.value ? 'bx bxs-shield' : (isMerchantProfile.value ? 'bx bxs-store' : 'bx bx-user'))
const showRoleChip = computed(() => isMerchantProfile.value)
const coverStyle = computed(() => ({ backgroundImage: user.value.coverPhoto || user.value.shopCover ? `linear-gradient(180deg, rgba(24,29,39,.16), rgba(24,29,39,.56)), url(${user.value.coverPhoto || user.value.shopCover})` : 'linear-gradient(135deg,#c97335 0%,#f1a64d 45%,#6d9773 100%)' }))
const followLabel = computed(() => isMerchantProfile.value ? (isFollowingUser.value ? '已关注店铺' : '关注店铺') : (isFollowingUser.value ? '已关注' : '关注'))
const contactLabel = computed(() => isMerchantProfile.value ? '联系商家' : '私信')
const tabs = computed(() => isMerchantProfile.value ? [
  { key: 'posts', label: '动态', count: posts.value.length },
  { key: 'activities', label: '活动', count: activities.value.length },
  { key: 'reviews', label: '评价', count: Number(reviewSummary.value.reviewCount || reviews.value.length || 0) }
] : [
  { key: 'posts', label: '动态', count: posts.value.length },
  { key: 'collections', label: '收藏', count: collections.value.length },
  { key: 'achievements', label: '成就', count: badges.value.length }
])
const heroStats = computed(() => isMerchantProfile.value ? [
  { key: 'rating', label: '评分', value: score(reviewSummary.value.averageScore), action: () => switchTab('reviews') },
  { key: 'keyword', label: '关键词', value: user.value.merchantKeyword || user.value.shopName || '非遗手作', action: null },
  { key: 'followers', label: '粉丝', value: count(user.value.followerCount), action: null }
] : [
  { key: 'followers', label: '粉丝', value: count(user.value.followerCount), action: null },
  { key: 'collected', label: '被收藏', value: count(summary.value.collectedCount), action: () => openList('collected') },
  { key: 'badges', label: '勋章', value: count(summary.value.badgeCount), action: () => switchTab('achievements') }
])
const activityCategoryOptions = computed(() => [...new Set(activities.value.map(item => item.categoryName || item.heritageType).filter(Boolean))])
const activityStatusOptions = computed(() => [...new Set(activities.value.map(item => item.status || item.auditStatus).filter(Boolean))])
const visibleTabs = computed(() => tabs.value.filter(item => item.key !== 'collections' || collectionsTabVisible.value))
const filteredActivities = computed(() => activities.value.filter(item => {
  const category = activityFilter.value.category === 'all' || (item.categoryName || item.heritageType) === activityFilter.value.category
  const status = activityFilter.value.status === 'all' || (item.status || item.auditStatus) === activityFilter.value.status
  const start = item.startTime ? new Date(item.startTime) : null
  const now = new Date()
  const timePass = activityFilter.value.time === 'all' || (activityFilter.value.time === 'upcoming' ? (!start || start >= now) : (start && start < now))
  return category && status && timePass
}))
const count = (value) => Number(value || 0).toLocaleString('zh-CN')
const score = (value) => value === null || value === undefined || value === '' || Number.isNaN(Number(value)) ? '--' : Number(value).toFixed(1)
const time = (value) => { const d = value ? new Date(value) : null; return d && !Number.isNaN(d.getTime()) ? d.toLocaleString('zh-CN') : '时间待定' }
const badgeIcon = (type) => ({
  order: 'bx bxs-coin',      // 铜钱
  activity: 'bx bxs-hot',    // 灯笼火焰
  checkin: 'bx bxs-check-shield', // 印章
  post: 'bx bxs-pen',        // 毛笔
  partner: 'bx bxs-group',   // 人群
  history: 'bx bxs-compass'  // 罗盘
}[type] || 'bx bx-medal')
const badgeProgress = (badge) => badge?.unlocked ? '已解锁' : `进度 ${badge?.progress || 0}/${badge?.target || 1}`
const switchTab = (key) => { if (visibleTabs.value.some(item => item.key === key)) { activeTab.value = key; router.replace({ path: route.path, query: { ...route.query, tab: key } }) } }
const syncTab = () => { const tab = String(route.query.tab || ''); activeTab.value = visibleTabs.value.some(item => item.key === tab) ? tab : visibleTabs.value[0]?.key || 'posts' }
const closeProfileMenu = () => { showProfileMenu.value = false }
const toggleProfileMenu = () => { showProfileMenu.value = !showProfileMenu.value }
const goBack = () => {
  closeProfileMenu()
  if (homepageBackTo.value && homepageBackTo.value !== route.fullPath) {
    return router.push(homepageBackTo.value)
  }
  if (isCurrentUser.value) {
    return router.push('/home/personal')
  }
  router.back()
}
const goEditProfile = () => { closeProfileMenu(); router.push('/personal/edit-profile') }
const goAdmin = () => router.push(Number(user.value.isSuperAdmin || 0) === 1 ? '/admin/admins' : '/admin/merchants')
const goInbox = () => {
  closeProfileMenu()
  router.push({ path: '/notification', query: { returnTo: route.fullPath, scope: 'chat' } })
}
const openHomepage = (id) => id && router.push({ path: `/user-homepage/${id}`, query: { backTo: route.fullPath } })
const triggerCover = () => isCurrentUser.value && !coverUploading.value && coverInput.value?.click()
const triggerAvatar = () => isCurrentUser.value && !avatarUploading.value && avatarInput.value?.click()
const contactTarget = () => {
  if (isCurrentUser.value) {
    router.push({ path: '/notification', query: { scope: 'chat' } })
    return
  }
  if (!user.value.id) return
  router.push({ path: '/notification', query: { userId: user.value.id, returnTo: route.fullPath, scope: 'chat' } })
}
const toggleBadge = (badge) => { const key = badge.code || badge.id; revealed.value = { ...revealed.value, [key]: !revealed.value[key] } }
const handleDocumentClick = () => { showProfileMenu.value = false }
const listPrimary = (item) => item.bio || (listType.value === 'collected' ? '这个人收藏过主页内容。' : '@' + (item.username || 'yayfolk-user'))
const listMeta = (item) => listType.value === 'collected' ? `${item.collectCount || 0} 次收藏` : (item.latestCollectTime || item.visitTime || item.viewTime || '')
const applyFollowState = (payload, targetId) => {
  const following = Boolean(payload?.following)
  const followerCount = Number(payload?.followerCount || 0)
  const followingCount = Number(payload?.followingCount || 0)
  if (Number(targetId) === Number(profileUserId.value)) {
    isFollowingUser.value = following
    user.value = { ...user.value, followerCount }
  }
  listData.value = listData.value.map(item => Number(item.id) === Number(targetId) ? { ...item, isFollowing: following } : item)
  saveUserPatch({ followingCount })
}

const loadPage = async () => {
  if (!profileUserId.value) return
  closeProfileMenu()
  loading.value = true
  try {
    const res = await getUserHomepage(profileUserId.value)
    if (res.code !== 200 || !res.data) { notify.error(res.message || '加载主页失败'); return }
    isCurrentUser.value = Boolean(res.data.isCurrentUser)
    user.value = res.data.user || {}
    summary.value = res.data.summary || {}
    posts.value = Array.isArray(res.data.posts) ? res.data.posts : []
    collections.value = Array.isArray(res.data.collections)
      ? res.data.collections.map(item => ({ ...item, visibility: (res.data.user?.collectionVisibility === 'private' ? 'private' : 'public') }))
      : []
    badges.value = Array.isArray(res.data.badges) ? res.data.badges : []
    activities.value = Array.isArray(res.data.activities) ? res.data.activities : []
    reviews.value = Array.isArray(res.data.reviews) ? res.data.reviews : []
    reviewSummary.value = res.data.reviewSummary || {}
    isFollowingUser.value = Boolean(res.data.user?.isFollowing)
    syncTab()
  } catch (error) { console.error(error); notify.error('加载主页失败，请稍后重试') } finally { loading.value = false }
}
const handleCoverUpload = async (e) => {
  const file = e.target.files?.[0]; if (!file) return
  if (!file.type.startsWith('image/')) { notify.warning('请选择图片文件'); e.target.value=''; return }
  coverUploading.value = true
  try {
    const form = new FormData(); form.append('file', file)
    const upload = await uploadImage(form, 'homepage')
    if (upload.code !== 200 || !upload.data?.url) { notify.error(upload.message || '上传背景失败'); return }
    const save = await updateHomepageSettings({ coverPhoto: upload.data.url, ...(isMerchantProfile.value ? { shopCover: upload.data.url } : {}) })
    if (save.code !== 200) { notify.error(save.message || '保存背景失败'); return }
    user.value = { ...user.value, coverPhoto: upload.data.url, shopCover: upload.data.url }
    saveUserPatch({ coverPhoto: upload.data.url, shopCover: upload.data.url })
    notify.success('背景图片已更新')
  } catch { notify.error('上传背景失败，请稍后重试') } finally { coverUploading.value = false; e.target.value = '' }
}
const handleAvatarUpload = async (e) => {
  const file = e.target.files?.[0]; if (!file) return
  if (!file.type.startsWith('image/')) { notify.warning('请选择图片文件'); e.target.value=''; return }
  avatarUploading.value = true
  try {
    const form = new FormData(); form.append('file', file)
    const up = await uploadAvatar(form)
    if (up.code !== 200 || !up.data?.url) { notify.error(up.message || '上传头像失败'); return }
    const save = await updateUserProfile({ avatar: up.data.url })
    if (save.code !== 200) { notify.error(save.message || '保存头像失败'); return }
    user.value = { ...user.value, avatar: up.data.url }
    saveUserPatch({ avatar: up.data.url })
    notify.success('头像已更新')
  } catch { notify.error('上传头像失败，请稍后重试') } finally { avatarUploading.value = false; e.target.value = '' }
}
const toggleFollow = async () => {
  if (!profileUserId.value || !currentUserId.value || isCurrentUser.value || followSubmitting.value) return
  followSubmitting.value = true
  try {
    const res = isFollowingUser.value ? await unfollowUser(profileUserId.value) : await followUser(profileUserId.value)
    if (res.code !== 200 || !res.data) { notify.error(res.message || '更新关注关系失败'); return }
    applyFollowState(res.data, profileUserId.value)
    notify.success(res.data.following ? '已关注' : '已取消关注')
  } catch { notify.error('更新关注关系失败，请稍后重试') } finally { followSubmitting.value = false }
}
const openList = async (type) => {
  listType.value = type; listTitle.value = type === 'followers' ? '粉丝列表' : (type === 'following' ? '关注列表' : '谁收藏了 TA'); listEmpty.value = type === 'followers' ? '暂时还没有粉丝' : (type === 'following' ? '暂时还没有关注任何人' : '暂时还没有收藏记录'); listLoading.value = true; showList.value = true
  try {
    const res = type === 'followers' ? await getFollowers(profileUserId.value) : (type === 'following' ? await getFollowing(profileUserId.value) : await getCollectedBy(profileUserId.value))
    listData.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
  } catch { listData.value = [] } finally { listLoading.value = false }
}
const closeList = () => { showList.value = false; listData.value = [] }
const toggleListFollow = async (item) => {
  try {
    const res = item.isFollowing ? await unfollowUser(item.id) : await followUser(item.id)
    if (res.code !== 200 || !res.data) { notify.error(res.message || '更新关注关系失败'); return }
    applyFollowState(res.data, item.id)
  } catch { notify.error('更新关注关系失败，请稍后重试') }
}
const openPost = async (post) => { const id = Number(post?.id || post?.postId || 0); if (!id) return; try { const res = await getDiscoverPostDetail(id); if (res.code !== 200 || !res.data) { notify.error(res.message || '加载帖子详情失败'); return } detailPost.value = res.data; showDetail.value = true; syncPost(res.data) } catch { notify.error('加载帖子详情失败，请稍后重试') } }
const openCollection = (item) => openPost({ postId: item.postId })
const closeDetail = () => { showDetail.value = false; detailPost.value = null }
const syncPost = (post) => {
  posts.value = posts.value.map(item => Number(item.id) === Number(post.id) ? { ...item, ...post, hashtags: post.hashtags || item.hashtags, images: post.images || item.images } : item)
  if (detailPost.value && Number(detailPost.value.id) === Number(post.id)) {
    detailPost.value = { ...detailPost.value, ...post, hashtags: post.hashtags || detailPost.value.hashtags, images: post.images || detailPost.value.images }
  }
  const collectionItem = {
    id: `collection-${post.id}`,
    postId: post.id,
    title: post.title || '未命名帖子',
    description: post.content || '暂无描述',
    coverImage: Array.isArray(post.images) && post.images.length ? post.images[0] : '',
    images: Array.isArray(post.images) ? post.images : [],
    collectCount: post.collects ?? 0,
    commentCount: post.comments ?? 0,
    visibility: collectionVisibility.value,
    authorId: post.author?.id ?? user.value.id,
    authorName: post.author?.name || displayName.value,
    authorAvatar: post.author?.avatar || user.value.avatar || defaultAvatar
  }
  collections.value = collections.value
    .map(item => Number(item.postId) === Number(post.id) ? { ...item, ...collectionItem, id: item.id, visibility: collectionVisibility.value } : item)
  if (isCurrentUser.value && typeof post.bookmarked === 'boolean') {
    if (post.bookmarked) {
      if (!collections.value.some(item => Number(item.postId) === Number(post.id))) {
        collections.value = [collectionItem, ...collections.value]
      }
    } else {
      collections.value = collections.value.filter(item => Number(item.postId) !== Number(post.id))
    }
    summary.value = { ...summary.value, collectionCount: collections.value.length }
  }
}
const removePost = async (post) => { if (!window.confirm(`确认删除帖子“${post.title || '未命名帖子'}”吗？`)) return; try { const res = await deleteMyDiscoverPost(post.id); if (res.code !== 200) { notify.error(res.message || '删除失败'); return } posts.value = posts.value.filter(item => Number(item.id) !== Number(post.id)); summary.value = { ...summary.value, postCount: Math.max(0, Number(summary.value.postCount || 0) - 1) }; notify.success('帖子已删除') } catch { notify.error('删除失败，请稍后重试') } }
const toggleCollectionVisibility = async () => { await toggleHomepageCollectionVisibility() }
const toggleHomepageCollectionVisibility = async () => {
  const next = collectionVisibility.value === 'private' ? 'public' : 'private'
  try {
    const res = await updateHomepageSettings({ collectionVisibility: next })
    if (res.code !== 200) { notify.error(res.message || '更新收藏页可见性失败'); return }
    user.value = { ...user.value, collectionVisibility: next, collectionsVisible: true }
    collections.value = collections.value.map(item => ({ ...item, visibility: next }))
    notify.success(next === 'private' ? '收藏页已设为私密' : '收藏页已设为公开')
    syncTab()
  } catch {
    notify.error('更新收藏页可见性失败，请稍后重试')
  }
}
const togglePostVisibility = async (post) => {
  const next = post.visibility === 'private' ? 'public' : 'private'
  try {
    const res = await updateDiscoverPostVisibility(post.id, next)
    if (res.code !== 200 || !res.data) { notify.error(res.message || '更新动态可见性失败'); return }
    syncPost(res.data)
    notify.success(next === 'private' ? '动态已设为私密' : '动态已设为公开')
  } catch {
    notify.error('更新动态可见性失败，请稍后重试')
  }
}
const openActivity = (item) => item?.id && router.push(`/activity/${item.id}`)
const activityPreviewTime = (item) => {
  if (!item?.startTime && !item?.endTime) {
    return '时间待定'
  }
  const start = item?.startTime ? time(item.startTime) : '时间待定'
  const end = item?.endTime ? time(item.endTime) : ''
  return end ? `${start} - ${end}` : start
}
const activityStatusLabel = (item) => ({
  signup: '报名中',
  full: '已满员',
  ongoing: '进行中',
  ended: '已结束',
  pending: '待审核',
  approved: '已通过',
  rejected: '已驳回'
}[String(item?.status || item?.auditStatus || '').trim()] || item?.status || item?.auditStatus || '进行中')
watch(() => route.query.tab, syncTab)
watch(profileUserId, () => { closeDetail(); closeList(); closeProfileMenu(); loadPage() })
watch(() => route.query.backTo, closeProfileMenu)
onMounted(() => {
  loadPage()
  document.addEventListener('click', handleDocumentClick)
})
onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
})
</script>

<style scoped>
.user-homepage {
  min-height: 100vh;
  padding: 20px 24px 40px;
  background:
    radial-gradient(circle at top left, rgba(201, 124, 55, 0.14), transparent 30%),
    linear-gradient(180deg, #f8f4ec 0%, #fbfaf7 100%);
  color: #2d3748;
}

.shell {
  max-width: 1360px;
  margin: 0 auto;
}

.card {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #eadfce;
  box-shadow: 0 18px 40px rgba(58, 69, 88, 0.08);
  border-radius: 28px;
}

.topbar,
.title-row,
.name-row,
.tab-bar,
.meta,
.chips,
.modal-head,
.actions,
.review-summary,
.sub-head,
.hero-actions,
.list-row,
.inbox-entry {
  display: flex;
  align-items: center;
}

.topbar,
.title-row,
.modal-head,
.sub-head,
.inbox-entry {
  justify-content: space-between;
}

.topbar {
  margin-bottom: 14px;
}

.icon-btn,
.ghost-btn,
.primary-btn,
.tab-btn,
.menu-btn,
.camera-btn,
.small-btn {
  border: none;
  cursor: pointer;
}

.icon-btn,
.ghost-btn,
.primary-btn,
.tab-btn,
.menu-btn,
.camera-btn,
.small-btn {
  border-radius: 999px;
}

.icon-btn,
.menu-btn {
  width: 42px;
  height: 42px;
  background: #fff;
  color: #415365;
  box-shadow: 0 10px 20px rgba(53, 66, 84, 0.12);
}

.ghost-btn,
.primary-btn,
.small-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 18px;
  font-weight: 700;
}

.ghost-btn,
.small-btn {
  border: 1px solid #e5d8c7;
  background: #fff;
  color: #415365;
}

.primary-btn {
  background: linear-gradient(135deg, #c97a28, #edab4a);
  color: #fff;
  box-shadow: 0 14px 24px rgba(201, 122, 40, 0.2);
}

.hero {
  overflow: hidden;
  padding-bottom: 18px;
}

.cover-box {
  position: relative;
  height: 300px;
  background: #d9a15e;
  background-size: cover;
  background-position: center;
  cursor: default;
}

.cover-box.editable {
  cursor: pointer;
}

.cover-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(20, 25, 33, 0.08), rgba(20, 25, 33, 0.54));
}

.media-hover {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.media-hover span {
  font-size: 13px;
  font-weight: 700;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.22);
}

.cover-box.editable:hover .media-hover,
.avatar-box.editable:hover .media-hover {
  opacity: 1;
}

.camera-btn {
  width: 60px;
  height: 60px;
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  backdrop-filter: blur(12px);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.24);
}

.camera-btn i {
  font-size: 28px;
}

.hidden {
  display: none;
}

.profile-panel {
  position: relative;
  z-index: 3;
  margin: -72px 24px 0;
  padding: 28px;
  display: grid;
  grid-template-columns: 156px minmax(0, 1fr) auto;
  gap: 24px;
  align-items: end;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 20px 36px rgba(47, 56, 70, 0.12);
}

.cover-settings-wrap {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
}

.cover-settings-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: background 0.2s;
}

.cover-settings-btn:hover {
  background: rgba(255, 255, 255, 0.7);
}

.cover-settings-btn i {
  font-size: 22px;
}

.cover-settings-menu {
  position: absolute;
  top: 54px;
  right: 0;
  min-width: 164px;
  padding: 8px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  box-shadow: 0 18px 36px rgba(47, 56, 70, 0.16);
}

.cover-settings-menu button {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: none;
  border-radius: 14px;
  background: transparent;
  color: #415365;
  cursor: pointer;
  text-align: left;
}

.cover-settings-menu button:hover {
  background: #f6efe4;
  color: #8b6134;
}

.avatar-box {
  position: relative;
  width: 156px;
  height: 156px;
  border-radius: 34px;
  overflow: hidden;
  background: #f3eadb;
  box-shadow: 0 18px 36px rgba(27, 37, 49, 0.18);
  cursor: default;
}

.avatar-box.editable {
  cursor: pointer;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-hover {
  border-radius: 34px;
}

.avatar-camera {
  width: 54px;
  height: 54px;
}

.profile-copy {
  min-width: 0;
}

.title-copy {
  min-width: 0;
}

.name-row {
  gap: 12px;
  flex-wrap: wrap;
}

.profile-copy h1,
.card-body h3,
.badge-card h3 {
  margin: 0;
}

.profile-copy h1 {
  font-size: clamp(32px, 4vw, 44px);
  line-height: 1.04;
}

.profile-copy p,
.card-body p,
.review-card p {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #64748b;
}

.role-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 14px;
  background: #f7ede0;
  color: #98591d;
  font-weight: 700;
  border-radius: 999px;
}

.signature {
  max-width: 760px;
  color: #8b6134 !important;
  font-size: 16px;
  font-weight: 600;
}

.bio {
  max-width: 760px;
}

.stats-row,
.post-grid,
.activity-grid,
.badge-grid,
.edit-grid {
  display: grid;
  gap: 16px;
}

.stats-row {
  margin-top: 18px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stat {
  border: none;
  border-radius: 999px;
  padding: 16px;
  background: #fff8ef;
  color: #2d3748;
  text-align: left;
  cursor: default;
}

.stat.clickable {
  cursor: pointer;
}

.stat strong {
  display: block;
  font-size: 28px;
}

.stat span {
  display: block;
  margin-top: 6px;
  color: #8b6a44;
}

.hero-actions {
  gap: 12px;
  align-self: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.inbox-entry {
  width: 100%;
  margin-top: 18px;
  padding: 18px 22px;
  border: 1px solid #eadfce;
  background: rgba(255, 255, 255, 0.96);
  cursor: pointer;
  text-align: left;
}

.inbox-icon {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #8b6134, #dca45f);
  color: #fff;
  font-size: 28px;
}

.inbox-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 14px;
}

.inbox-copy strong {
  color: #263445;
  font-size: 18px;
}

.inbox-copy span {
  color: #64748b;
  line-height: 1.6;
}

.inbox-arrow {
  font-size: 24px;
  color: #b06e26;
}

.content {
  margin-top: 22px;
  padding: 22px;
  position: relative;
}

.tab-bar {
  gap: 12px;
  flex-wrap: wrap;
}

.collection-banner {
  margin-top: 18px;
  padding: 18px 20px;
  border: 1px solid #eadfce;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(255, 248, 239, 0.96), rgba(249, 242, 229, 0.96));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.collection-banner strong {
  display: block;
  color: #8a5b1f;
  font-size: 16px;
}

.collection-banner p {
  margin: 6px 0 0;
  color: #6b7280;
  line-height: 1.6;
}

.tab-btn {
  padding: 12px 18px;
  background: #fff;
  border: 1px solid #e5d8c7;
  color: #526172;
  display: inline-flex;
  gap: 10px;
}

.tab-btn.active {
  background: linear-gradient(135deg, #8b6134, #dca45f);
  color: #fff;
  box-shadow: 0 14px 24px rgba(139, 97, 52, 0.18);
}

.tab-btn span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
}

.post-grid,
.activity-grid,
.badge-grid {
  margin-top: 18px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.activity-grid {
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.post-card,
.activity-card,
.badge-card,
.review-card,
.sum-box,
.list-row,
.info-box {
  position: relative;
  border: 1px solid #eee2d3;
  background: #fbf7f1;
  border-radius: 22px;
  overflow: hidden;
}

.media,
.activity-cover,
.placeholder {
  height: 210px;
  background: #eadbc7;
}

.activity-media {
  position: relative;
  height: 170px;
  background: #eadbc7;
}

.cover,
.activity-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: #a67f55;
}

.activity-preview-card {
  cursor: pointer;
}

.activity-preview-card .chips,
.activity-preview-card .meta {
  display: none;
}

.activity-preview-badge {
  position: absolute;
  left: 12px;
  bottom: 12px;
  max-width: calc(100% - 24px);
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 250, 243, 0.92);
  color: #8a5b1f;
  font-size: 12px;
  font-weight: 700;
}

.activity-preview-body {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.activity-preview-title {
  margin: 0;
  color: #2d3748;
  font-size: 16px;
  line-height: 1.45;
}

.activity-preview-head {
  display: flex;
  justify-content: flex-end;
}

.activity-preview-status {
  padding: 5px 10px;
  border-radius: 999px;
  background: #f3e6d4;
  color: #8a5b1f;
  font-size: 12px;
  font-weight: 700;
}

.activity-preview-body > h3:not(.activity-preview-title) {
  display: none;
}

.activity-preview-body h3,
.activity-preview-time,
.activity-preview-location,
.activity-preview-body > p {
  margin: 0;
}

.activity-preview-time {
  color: #8a5b1f;
  font-size: 13px;
  font-weight: 700;
}

.activity-preview-location {
  color: #7f684a;
  font-size: 12px;
}

.activity-preview-summary {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.card-body {
  padding: 16px;
}

.tags,
.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.tags span,
.chips span {
  padding: 5px 10px;
  border-radius: 999px;
  background: #f3e6d4;
  color: #8a5b1f;
  font-size: 12px;
}

.meta {
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 14px;
  color: #7f684a;
  font-size: 13px;
}

.menu-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
}

.visibility-toggle-btn {
  position: absolute;
  top: 12px;
  right: 20px;
  z-index: 3;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  color: #8a5b1f;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 18px rgba(58, 69, 88, 0.12);
}

.menu-pop {
  position: absolute;
  top: 58px;
  right: 12px;
  z-index: 4;
  background: #fff;
  border: 1px solid #eadfce;
  border-radius: 16px;
  box-shadow: 0 16px 28px rgba(58, 69, 88, 0.14);
  overflow: hidden;
}

.menu-pop button {
  display: block;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.menu-pop .danger {
  color: #b2453f;
}

.visibility-chip {
  padding: 4px 10px;
  border-radius: 999px;
  background: #f3e6d4;
  color: #8a5b1f;
}

/* ===== 勋章基础样式 ===== */
.badge-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  cursor: pointer;
  border-radius: 20px;
  border: none;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.badge-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: 20px 20px 0 0;
}

.badge-card:hover {
  transform: translateY(-6px) scale(1.02);
}

/* ===== 订单类 - 景泰蓝青花瓷风 ===== */
.badge-order {
  background: linear-gradient(135deg, #f8fbff 0%, #e8f4fc 50%, #d6eaf8 100%);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-order::before {
  background: linear-gradient(90deg, #3b82f6, #60a5fa, #3b82f6);
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}

.badge-order:hover {
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-order .badge-icon {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: linear-gradient(145deg, #93c5fd 0%, #3b82f6 40%, #1d4ed8 100%);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.4), inset 0 2px 4px rgba(255, 255, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  animation: floatBadge 4s ease-in-out infinite;
}

.badge-order .badge-icon::before {
  content: '';
  position: absolute;
  inset: 8px;
  border-radius: 50%;
  border: 2px dashed rgba(255, 255, 255, 0.6);
  animation: rotateSlow 20s linear infinite;
}

/* ===== 活动类 - 剪纸中国红风 ===== */
.badge-activity {
  background: linear-gradient(135deg, #fffaf0 0%, #fef3f0 50%, #fee8e4 100%);
  box-shadow: 0 4px 20px rgba(239, 68, 68, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-activity::before {
  background: linear-gradient(90deg, #ef4444, #f87171, #ef4444);
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}

.badge-activity:hover {
  box-shadow: 0 12px 32px rgba(239, 68, 68, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-activity .badge-icon {
  width: 72px;
  height: 80px;
  border-radius: 36px 36px 16px 16px;
  background: linear-gradient(180deg, #fca5a5 0%, #ef4444 35%, #dc2626 70%, #b91c1c 100%);
  box-shadow: 0 8px 24px rgba(220, 38, 38, 0.35), inset 0 2px 4px rgba(255, 200, 200, 0.4);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: swingBadge 3s ease-in-out infinite;
}

.badge-activity .badge-icon::before {
  content: '';
  position: absolute;
  inset: 6px;
  border-radius: 30px 30px 12px 12px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-style: dashed;
}

/* ===== 打卡类 - 篆刻印章风 ===== */
.badge-checkin {
  background: linear-gradient(135deg, #fefefe 0%, #fef2f2 50%, #fee8e8 100%);
  box-shadow: 0 4px 20px rgba(185, 28, 28, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-checkin::before {
  background: linear-gradient(90deg, #b91c1c, #dc2626, #b91c1c);
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}

.badge-checkin:hover {
  box-shadow: 0 12px 32px rgba(185, 28, 28, 0.18), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-checkin .badge-icon {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  background: linear-gradient(145deg, #dc2626 0%, #b91c1c 50%, #7f1d1d 100%);
  box-shadow: 0 8px 24px rgba(153, 27, 27, 0.35), inset 0 2px 4px rgba(255, 150, 150, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  animation: stampBadge 2s ease-in-out infinite;
}

.badge-checkin .badge-icon::before {
  content: '';
  position: absolute;
  inset: 8px;
  border-radius: 4px;
  border: 3px solid rgba(255, 255, 255, 0.5);
}

/* ===== 发帖类 - 水墨山水风 ===== */
.badge-post {
  background: linear-gradient(135deg, #f8fcf8 0%, #f0f9f0 50%, #e8f5e8 100%);
  box-shadow: 0 4px 20px rgba(34, 197, 94, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-post::before {
  background: linear-gradient(90deg, #16a34a, #22c55e, #16a34a);
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}

.badge-post:hover {
  box-shadow: 0 12px 32px rgba(34, 197, 94, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-post .badge-icon {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: linear-gradient(145deg, #86efac 0%, #22c55e 40%, #15803d 100%);
  box-shadow: 0 8px 24px rgba(34, 197, 94, 0.35), inset 0 2px 4px rgba(255, 255, 255, 0.4);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: floatBadge 4s ease-in-out infinite 0.5s;
}

.badge-post .badge-icon::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: repeating-linear-gradient(45deg, transparent, transparent 6px, rgba(255, 255, 255, 0.12) 6px, rgba(255, 255, 255, 0.12) 8px);
}

/* ===== 搭子类 - 苏绣丝绸风 ===== */
.badge-partner {
  background: linear-gradient(135deg, #fdfaff 0%, #faf5ff 50%, #f3e8ff 100%);
  box-shadow: 0 4px 20px rgba(168, 85, 247, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-partner::before {
  background: linear-gradient(90deg, #9333ea, #a855f7, #9333ea);
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}

.badge-partner:hover {
  box-shadow: 0 12px 32px rgba(168, 85, 247, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-partner .badge-icon {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: linear-gradient(145deg, #d8b4fe 0%, #a855f7 40%, #7c3aed 100%);
  box-shadow: 0 8px 24px rgba(168, 85, 247, 0.35), inset 0 2px 4px rgba(255, 255, 255, 0.4);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulseBadge 3s ease-in-out infinite;
}

.badge-partner .badge-icon::before {
  content: '';
  position: absolute;
  inset: 6px;
  border-radius: 50%;
  background: repeating-conic-gradient(from 0deg, rgba(255, 255, 255, 0.15) 0deg 10deg, transparent 10deg 20deg);
  animation: rotateSlow 15s linear infinite;
}

/* ===== 浏览类 - 琥珀琉璃风 ===== */
.badge-history {
  background: linear-gradient(135deg, #fffdf5 0%, #fef9e7 50%, #fef3c7 100%);
  box-shadow: 0 4px 20px rgba(217, 119, 6, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-history::before {
  background: linear-gradient(90deg, #d97706, #f59e0b, #d97706);
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}

.badge-history:hover {
  box-shadow: 0 12px 32px rgba(217, 119, 6, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.badge-history .badge-icon {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: linear-gradient(145deg, #fcd34d 0%, #f59e0b 40%, #b45309 100%);
  box-shadow: 0 8px 24px rgba(217, 119, 6, 0.35), inset 0 2px 4px rgba(255, 255, 255, 0.4);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: floatBadge 4s ease-in-out infinite 1s;
}

.badge-history .badge-icon::before {
  content: '';
  position: absolute;
  inset: 8px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.4);
}

/* ===== 动画关键帧 ===== */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes floatBadge {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@keyframes swingBadge {
  0%, 100% { transform: rotate(-3deg); }
  50% { transform: rotate(3deg); }
}

@keyframes stampBadge {
  0%, 100% { transform: scale(1) rotate(0deg); }
  25% { transform: scale(1.05) rotate(-2deg); }
  75% { transform: scale(1.05) rotate(2deg); }
}

@keyframes pulseBadge {
  0%, 100% { transform: scale(1); box-shadow: 0 8px 24px rgba(168, 85, 247, 0.35); }
  50% { transform: scale(1.05); box-shadow: 0 12px 32px rgba(168, 85, 247, 0.5); }
}

@keyframes rotateSlow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== 通用图标样式 ===== */
.badge-medallion {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-icon {
  font-size: 28px;
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 流光动画效果 */
.badge-aura {
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent 0%, rgba(255, 255, 255, 0.4) 32%, transparent 62%);
  transform: translateX(-100%);
  transition: transform 0.8s ease;
  pointer-events: none;
}

.badge-card:hover .badge-aura,
.badge-card.revealed .badge-aura {
  transform: translateX(100%);
}

/* 解锁状态光效 */
.badge-card.unlocked {
  animation: unlockGlow 2s ease-out;
}

.badge-card.unlocked .badge-icon {
  animation: unlockBadge 0.6s ease-out;
}

@keyframes unlockGlow {
  0% { box-shadow: 0 0 0 0 rgba(255, 215, 0, 0.5); }
  50% { box-shadow: 0 0 30px 10px rgba(255, 215, 0, 0.3); }
  100% { box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1); }
}

@keyframes unlockBadge {
  0% { transform: scale(1); }
  50% { transform: scale(1.2) rotate(10deg); }
  100% { transform: scale(1) rotate(0deg); }
}

/* 未解锁状态 */
.badge-card:not(.unlocked) {
  opacity: 0.75;
}

.badge-card:not(.unlocked) .badge-icon {
  filter: grayscale(0.4) brightness(0.9);
}

.badge-card:not(.unlocked):hover .badge-icon {
  filter: grayscale(0.2) brightness(0.95);
}

/* 文本样式 */
.badge-head {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.badge-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.badge-state,
.badge-progress {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.badge-order .badge-state { background: linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(96, 165, 250, 0.1)); color: #1d4ed8; }
.badge-activity .badge-state { background: linear-gradient(135deg, rgba(239, 68, 68, 0.12), rgba(248, 113, 113, 0.1)); color: #dc2626; }
.badge-checkin .badge-state { background: linear-gradient(135deg, rgba(185, 28, 28, 0.12), rgba(220, 38, 38, 0.1)); color: #b91c1c; }
.badge-post .badge-state { background: linear-gradient(135deg, rgba(34, 197, 94, 0.15), rgba(74, 222, 128, 0.1)); color: #15803d; }
.badge-partner .badge-state { background: linear-gradient(135deg, rgba(168, 85, 247, 0.15), rgba(192, 132, 252, 0.1)); color: #7c3aed; }
.badge-history .badge-state { background: linear-gradient(135deg, rgba(217, 119, 6, 0.15), rgba(251, 191, 36, 0.1)); color: #b45309; }

.badge-progress {
  background: linear-gradient(135deg, rgba(139, 97, 52, 0.08), rgba(180, 140, 80, 0.06));
  color: #8b6134;
}

.badge-medallion {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-copy,
.badge-detail {
  position: relative;
  z-index: 2;
}

.badge-topline,
.badge-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.badge-topline {
  margin-bottom: 8px;
}

.badge-state,
.badge-progress {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(139, 97, 52, 0.09);
  color: #8b6134;
  font-size: 12px;
  font-weight: 700;
}

.badge-card.unlocked .badge-state {
  background: rgba(185, 61, 41, 0.12);
  color: #a83e26;
}

.badge-detail {
  padding-top: 16px;
  border-top: 1px solid rgba(139, 97, 52, 0.12);
  display: grid;
  gap: 10px;
  max-height: 0;
  opacity: 0;
  overflow: hidden;
  transition: max-height 0.35s ease, opacity 0.25s ease, padding-top 0.25s ease;
}

.badge-card.revealed .badge-detail {
  max-height: 180px;
  opacity: 1;
}

.badge-detail-row span,
.badge-detail small {
  color: #7f684a;
}

.badge-detail-row strong {
  color: #263445;
}

.filters {
  display: flex;
  gap: 6px;
  position: absolute;
  top: 22px;
  right: 22px;
  z-index: 10;
}

.filters select {
  flex: none;
  width: auto;
  padding: 6px 12px;
  border: 1px solid #e1d6c7;
  border-radius: 12px;
  background: rgba(255, 253, 249, 0.95);
  outline: none;
  font-size: 12px;
  color: #293241;
  cursor: pointer;
}

select,
input,
textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e1d6c7;
  border-radius: 16px;
  background: #fffdf9;
  outline: none;
  font-size: 14px;
  color: #293241;
}

.review-summary {
  margin-top: 18px;
  gap: 14px;
  flex-wrap: wrap;
}

.sum-box,
.review-card {
  padding: 18px;
}

.sum-box {
  min-width: 160px;
}

.sum-box strong {
  display: block;
  font-size: 28px;
}

.review-list {
  margin-top: 18px;
}

.blocked,
.loading,
.empty,
.modal {
  padding: 22px;
}

.empty {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #8b795d;
}

.mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(16, 23, 36, 0.46);
}

.modal {
  width: min(760px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
}

.wide {
  width: min(980px, 100%);
}

.list-stack,
.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-row {
  gap: 14px;
  padding: 14px 16px;
  text-align: left;
  border-radius: 18px;
}

.mini-avatar {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  object-fit: cover;
  background: #fff;
}

.list-row > div {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.list-row span,
.list-row small {
  color: #64748b;
  line-height: 1.5;
}

.small-btn.following {
  background: #f3ede4;
  color: #8a5b1f;
}

.edit-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 14px;
}

.full-span {
  grid-column: 1 / -1;
}

@media (max-width: 1120px) {
  .post-grid,
  .activity-grid,
  .badge-grid,
  .edit-grid,
  .profile-panel {
    grid-template-columns: 1fr;
  }

  .activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .profile-settings-wrap {
    top: 18px;
    right: 18px;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 760px) {
  .user-homepage {
    padding: 14px;
  }

  .collection-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .cover-box {
    height: 220px;
  }

  .profile-panel {
    margin: -44px 16px 0;
    padding: 16px;
    padding-top: 60px;
    gap: 12px;
    display: flex;
    flex-direction: column;
    position: relative;
  }

  .cover-settings-wrap {
    top: 12px;
    right: 12px;
  }

  .profile-panel .avatar-box {
    position: absolute;
    top: -30px;
    left: 16px;
    width: 80px;
    height: 80px;
    border-radius: 20px;
  }

  .avatar-hover {
    border-radius: 20px;
  }

  .hero-actions {
    position: absolute;
    top: -22px;
    right: 16px;
    flex-direction: row;
    align-items: center;
    gap: 8px;
  }

  .hero-actions .ghost-btn {
    padding: 6px 14px;
    font-size: 13px;
    border-radius: 20px;
  }

  .profile-copy {
    margin-top: 0;
    margin-left: 96px;
    text-align: left;
  }

  .title-row,
  .modal-head,
  .sub-head,
  .topbar,
  .actions,
  .inbox-entry {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-row {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin-top: 16px;
    margin-left: -96px;
    width: calc(100% + 96px);
    padding: 0 16px 0 0;
  }

  .stat {
    padding: 10px 8px;
    text-align: center;
  }

  .stat strong {
    font-size: 20px;
  }

  .stat span {
    font-size: 12px;
  }

  .ghost-btn,
  .primary-btn,
  .tab-btn,
  .small-btn {
    width: auto;
    min-width: 70px;
  }

  .visibility-toggle-btn {
    right: 52px;
  }

  /* 选项卡按钮撑满一排 */
  .tab-bar {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 6px;
  }

  .tab-btn {
    justify-content: center;
    padding: 10px 8px;
    font-size: 13px;
  }

  .tab-btn span {
    min-width: 20px;
    height: 20px;
    font-size: 11px;
    padding: 0 4px;
  }

  /* 帖子网格改为两列错落排列 */
  .post-grid,
  .activity-grid,
  .badge-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .post-card,
  .activity-card,
  .badge-card {
    border-radius: 16px;
  }

  .media,
  .activity-cover,
  .placeholder {
    height: 140px;
  }

  .activity-media {
    height: 140px;
  }

  .card-body {
    padding: 12px;
  }

  .activity-preview-body {
    padding: 12px;
    gap: 6px;
  }

  .activity-preview-body h3 {
    font-size: 14px;
  }

  .activity-preview-time,
  .activity-preview-location,
  .activity-preview-summary {
    font-size: 12px;
  }

  .card-body h3 {
    font-size: 14px;
    margin-bottom: 6px;
  }

  .card-body p {
    font-size: 12px;
    margin-top: 6px;
  }

  .meta {
    font-size: 11px;
    margin-top: 10px;
  }

  .menu-btn,
  .visibility-toggle-btn {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }

  /* 筛选框缩短水平一排 */
  .content {
    position: static;
  }

  .filters {
    position: static;
    display: flex;
    gap: 6px;
    margin-top: 10px;
  }

  .filters select {
    flex: 1;
    min-width: 0;
    padding: 6px 8px;
    font-size: 11px;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.95);
  }

  .badge-head {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .badge-icon {
    width: 56px;
    height: 56px;
    border-radius: 20px;
    font-size: 22px;
  }

  .badge-card {
    padding: 16px;
    gap: 12px;
  }

  .badge-card h3 {
    font-size: 14px;
  }

  .badge-card p {
    font-size: 12px;
    margin-top: 6px;
  }

  .inbox-copy {
    padding: 14px 0;
  }

  /* 收藏和勋章卡片缩小 */
  .collection-banner {
    padding: 14px 16px;
  }

  .collection-banner strong {
    font-size: 14px;
  }

  .collection-banner p {
    font-size: 12px;
  }

  .list-row {
    padding: 12px 14px;
    gap: 10px;
  }

  .mini-avatar {
    width: 44px;
    height: 44px;
    border-radius: 14px;
  }

  .list-row > div {
    font-size: 13px;
  }

  .list-row strong {
    font-size: 14px;
  }

  .list-row span,
  .list-row small {
    font-size: 12px;
  }
}
</style>

