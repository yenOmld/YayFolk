<template>
  <div>
    <div class="post-modal" v-if="visible">
      <div class="modal-overlay" @click="$emit('close')"></div>
      <div class="modal-content">
        <div class="modal-left">
          <div class="image-slider">
            <img 
              :src="post?.images?.[currentImageIndex]"
              alt="Post image"
              class="modal-image"
              @click="openImagePreview"
              style="cursor: zoom-in;"
            />
            
            <button 
              v-if="post?.images?.length > 1" 
              class="slider-btn prev-btn"
              @click="prevImage"
            >
              <i class='bx bx-chevron-left'></i>
            </button>
            <button 
              v-if="post?.images?.length > 1" 
              class="slider-btn next-btn"
              @click="nextImage"
            >
              <i class='bx bx-chevron-right'></i>
            </button>
            
            <div v-if="post?.images?.length > 1" class="image-indicators">
              <span 
                v-for="(image, index) in post?.images" 
                :key="index"
                class="indicator-dot"
                :class="{ active: currentImageIndex === index }"
                @click="currentImageIndex = index"
              ></span>
            </div>
            
            <div v-if="post?.images?.length > 1" class="image-counter">
              {{ currentImageIndex + 1 }} / {{ post?.images?.length }}
            </div>
          </div>
        </div>
        
        <div class="modal-right">
          <div class="modal-header">
            <img :src="post?.author.avatar" alt="Avatar" class="modal-author-avatar clickable-user" @click="goToUserHomepage(post?.author?.id)" />
            <div class="modal-author-info clickable-user" @click="goToUserHomepage(post?.author?.id)">
              <h4>{{ post?.author.name }}</h4>
              <p>{{ post?.time }} · {{ post?.author.location || $t('postDetail.unknown') }}</p>
            </div>
            <button v-if="currentUser && post?.author?.id !== currentUser.id" class="contact-btn" :class="{ active: isFollowing }" :disabled="followSubmitting" @click="toggleFollow">{{ followSubmitting ? '处理中...' : (isFollowing ? '已关注' : '关注') }}</button>
          </div>
            <div class="modal-post-view">
              <div class="modal-post-content">
                <h3>{{ post?.title || $t('postDetail.postTitle') }}</h3>
                <p>{{ post?.content }}</p>
                <p v-if="showPostTranslation && translatedPostText" class="translated-text">{{ translatedPostText }}</p>
                <button
                  v-if="canTranslatePost()"
                  class="translate-toggle-btn"
                  :class="{ active: showPostTranslation }"
                  :disabled="translatingPost"
                  @click="togglePostTranslate"
                >
                  <i v-if="translatingPost" class='bx bx-loader-alt bx-spin'></i>
                  <span>{{ translatingPost ? $t('postDetail.translating') : (showPostTranslation ? $t('postDetail.viewOriginal') : $t('postDetail.translatePost')) }}</span>
                </button>
                <div class="hashtags" v-if="post?.hashtags">
                  <span v-for="(tag, index) in post.hashtags" :key="index" class="hashtag" @click="handleTagClick(tag)">#{{ tag }}</span>
                </div>
              </div>
              
              <div class="modal-comments">
                <h4>{{ $t('postDetail.commentsCount', { count: post?.comments || 0 }) }}</h4>
                <div 
                  class="comment-item" 
                  v-for="(comment, index) in post?.commentList || []" 
                  :key="index" 
                  :ref="el => { if (el) commentRefs[comment.id] = el }"
                  :class="{ 'reply-comment': comment.parentId, 'comment-highlight': highlightCommentId === String(comment.id) }"
                >
                  <img :src="comment.avatar" alt="Comment avatar" class="comment-avatar clickable-user" @click="goToUserHomepage(comment.userId)" />
                  <div class="comment-content">
                    <div class="comment-header">
                      <span class="comment-author clickable-user" @click="goToUserHomepage(comment.userId)">{{ comment.author }}</span>
                      <span class="comment-time">{{ comment.time }}</span>
                    </div>
                    <p class="comment-text">
                      <span v-if="comment.replyTo" class="reply-to">{{ $t('postDetail.replyTo', { author: comment.replyTo }) }}</span>
                      {{ comment.content }}
                    </p>
                    <p v-if="isCommentTranslationVisible(comment)" class="comment-translation-text">{{ getCommentTranslatedText(comment) }}</p>
                    <div class="comment-actions">
                      <span class="comment-like" @click="toggleCommentLike(comment)">
                        <i class='bx bx-heart' :class="{ liked: comment.liked }"></i>
                        <span>{{ comment.likes }}</span>
                      </span>
                      <span class="comment-reply" @click="replyToComment(comment)">{{ $t('postDetail.reply') }}</span>
                      <span
                        v-if="canTranslateComment(comment)"
                        class="comment-translate"
                        @click="toggleCommentTranslate(comment)"
                      >
                        <i v-if="isCommentTranslating(comment)" class='bx bx-loader-alt bx-spin'></i>
                        <span>{{ isCommentTranslating(comment) ? $t('postDetail.translating') : (isCommentTranslationVisible(comment) ? $t('postDetail.viewOriginal') : $t('postDetail.translate')) }}</span>
                      </span>
                      <span 
                        v-if="canDeleteComment(comment)" 
                        class="comment-delete" 
                        @click="handleDeleteComment(comment, index)"
                      >{{ $t('postDetail.delete') }}</span>
                    </div>
                  </div>
                </div>
            </div>
          </div>
          
          <div class="modal-footer">
            <div class="interaction-buttons">
              <button class="interaction-btn" @click="toggleCollect">
                <i class='bx bx-star' :class="{ collected: post?.bookmarked }"></i>
                <span>{{ post?.collects || 0 }}</span>
              </button>
              <button class="interaction-btn">
                <i class='bx bx-comment'></i>
                <span>{{ post?.comments }}</span>
              </button>
              <button class="interaction-btn">
                <i class='bx bx-show'></i>
                <span>{{ post?.viewCount || 0 }}</span>
              </button>
              <button class="interaction-btn share-btn" @click="openShareModal">
                <i class='bx bx-share-alt'></i>
                <span>{{ $t('postDetail.forward') }}</span>
              </button>
              <button class="interaction-btn report-btn" @click="reportPost">
                <i class='bx bx-error-circle'></i>
                <span>举报</span>
              </button>
            </div>
            <div class="comment-input">
              <div v-if="replyToCommentId" class="reply-info">
                {{ $t('postDetail.reply') }} {{ replyToAuthor }} <i class='bx bx-x' @click="cancelReply"></i>
              </div>
              <input v-model="newComment" type="text" :placeholder="replyToCommentId ? $t('postDetail.replyComment') : $t('postDetail.saySomething')" @keyup.enter="submitComment" />
              <button class="send-btn" :disabled="submittingComment" @click="submitComment">{{ $t('postDetail.send') }}</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showImagePreview" class="image-preview-modal">
      <div class="preview-overlay"></div>
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

    <div v-if="showShareModal" class="share-modal">
      <div class="share-overlay" @click="closeShareModal"></div>
      <div class="share-content">
        <div class="share-header">
          <h3>{{ $t('postDetail.shareTo') }}</h3>
          <button class="share-close" @click="closeShareModal">
            <i class='bx bx-x'></i>
          </button>
        </div>
        <div class="share-options">
          <div class="share-option" @click="shareToWechat">
            <div class="share-icon wechat">
              <i class='bx bxl-wechat'></i>
            </div>
            <span>{{ $t('postDetail.wechat') }}</span>
          </div>
          <div class="share-option" @click="shareToQQ">
            <div class="share-icon qq">
              <i class='bx bxl-qq'></i>
            </div>
            <span>{{ $t('postDetail.qq') }}</span>
          </div>
          <div class="share-option" @click="shareToWeibo">
            <div class="share-icon weibo">
              <i class='bx bxl-weibo'></i>
            </div>
            <span>{{ $t('postDetail.weibo') }}</span>
          </div>
          <div class="share-option" @click="copyLink">
            <div class="share-icon link">
              <i class='bx bx-link'></i>
            </div>
            <span>{{ $t('postDetail.copyLink') }}</span>
          </div>
        </div>
        <div class="share-link-preview">
          <p>{{ shareUrl }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  createDiscoverPostComment,
  deleteDiscoverComment,
  reportDiscoverPost,
  translateDiscoverComment,
  translateDiscoverPost,
  toggleDiscoverCommentLike,
  toggleDiscoverPostCollect,
  followUser,
  unfollowUser,
  getFollowStatus
} from '../api/app'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm

const { t } = useI18n()

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  post: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'update', 'searchTag'])

const router = useRouter()
const currentImageIndex = ref(0)
const newComment = ref('')
const submittingComment = ref(false)
const replyToCommentId = ref(null)
const replyToAuthor = ref('')
const showImagePreview = ref(false)
const showShareModal = ref(false)
const translatingPost = ref(false)
const showPostTranslation = ref(false)
const translatedPostText = ref('')
const commentTranslationState = ref({})
const preferredLanguage = ref('zh-CN')
const highlightCommentId = ref(null)
const commentRefs = ref({})

const shareUrl = ref('')
const isFollowing = ref(false)
const followSubmitting = ref(false)

const getCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      return JSON.parse(userStr)
    } catch (e) {
      return null
    }
  }
  return null
}

const currentUser = getCurrentUser()

watch(() => props.visible, (val) => {
  if (!val) {
    currentImageIndex.value = 0
    newComment.value = ''
    replyToCommentId.value = null
    replyToAuthor.value = ''
    showImagePreview.value = false
    commentRefs.value = {}
  }
})

watch(() => props.post, async (newPost) => {
  if (!newPost) return
  
  translatedPostText.value = ''
  showPostTranslation.value = false
  commentTranslationState.value = {}
  preferredLanguage.value = getPreferredLanguage()
  
  // 获取关注状态
  if (newPost.author?.id && currentUser) {
    try {
      const response = await getFollowStatus(newPost.author.id)
      if (response.code === 200) {
        isFollowing.value = response.data
      }
    } catch (error) {
      console.error('获取关注状态失败:', error)
    }
  }
  
  // 检查是否需要高亮评论
  if (newPost.highlightCommentId) {
    // 等待DOM更新
    await nextTick()
    // 确保类型匹配
    highlightCommentId.value = String(newPost.highlightCommentId)
    
    // 自动滚动到高亮评论
    setTimeout(() => {
      const targetComment = commentRefs.value[highlightCommentId.value]
      if (targetComment) {
        targetComment.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    }, 100)
    
    // 延迟执行以确保动画完成
    setTimeout(() => {
      highlightCommentId.value = null
    }, 3000)
  }
}, { deep: true })

const handleTagClick = (tag) => {
  emit('searchTag', tag)
  emit('close')
}

const prevImage = () => {
  if (!props.post?.images?.length) return
  currentImageIndex.value = (currentImageIndex.value - 1 + props.post.images.length) % props.post.images.length
}

const nextImage = () => {
  if (!props.post?.images?.length) return
  currentImageIndex.value = (currentImageIndex.value + 1) % props.post.images.length
}

const openImagePreview = () => {
  showImagePreview.value = true
}

const closeImagePreview = () => {
  showImagePreview.value = false
}

const prevPreviewImage = () => {
  if (!props.post?.images?.length) return
  currentImageIndex.value = (currentImageIndex.value - 1 + props.post.images.length) % props.post.images.length
}

const nextPreviewImage = () => {
  if (!props.post?.images?.length) return
  currentImageIndex.value = (currentImageIndex.value + 1) % props.post.images.length
}

const toggleCollect = async () => {
  if (!props.post) return
  try {
    const response = await toggleDiscoverPostCollect(props.post.id)
    if (response.code !== 200) {
      notify.error(response.message || t('postDetail.operationFailed'))
      return
    }
    const { bookmarked, collects } = response.data
    emit('update', { ...props.post, bookmarked, collects })
  } catch (error) {
    notify.error(t('postDetail.collectFailed'))
  }
}

const reportPost = async () => {
  if (!props.post?.id) return
  const confirmed = window.confirm('确认举报该内容吗？')
  if (!confirmed) return

  const reasonInput = window.prompt('请输入举报原因（选填）', '')
  if (reasonInput === null) return
  const reason = reasonInput.trim()

  try {
    const response = await reportDiscoverPost(props.post.id, reason)
    if (response.code !== 200) {
      notify.error(response.message || '举报提交失败')
      return
    }
    emit('update', {
      ...props.post,
      auditStatus: response.data?.auditStatus || props.post.auditStatus,
      auditRemark: response.data?.auditRemark || props.post.auditRemark
    })
    notify.success('举报已提交，管理员将优先复核')
    emit('close')
  } catch (error) {
    notify.error('举报提交失败，请稍后重试')
  }
}

const getPreferredLanguage = () => {
  // i18n 使用的是 'locale' 键
  const locale = localStorage.getItem('locale')
  if (locale) {
    return locale
  }
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      if (user.langCode) {
        return user.langCode
      }
    } catch (error) {
      return 'zh'
    }
  }
  return 'zh'
}

const normalizeLang = (lang) => {
  if (!lang) return ''
  const lower = String(lang).replace('_', '-').toLowerCase()
  if (lower === 'zh-cn' || lower === 'zh-hans') return 'zh'
  if (lower === 'zh-tw' || lower === 'zh-hant') return 'zh-tw'
  if (lower.includes('-')) return lower.split('-')[0]
  return lower
}

const canTranslateText = (sourceLang) => {
  const source = normalizeLang(sourceLang)
  const target = normalizeLang(preferredLanguage.value)
  if (!source || !target) return false
  return source !== target
}

const canTranslatePost = () => {
  if (!props.post) return false
  if (!props.post.content) return false
  return canTranslateText(props.post.sourceLang)
}

const togglePostTranslate = async () => {
  if (!props.post || !canTranslatePost()) return
  if (showPostTranslation.value) {
    showPostTranslation.value = false
    return
  }
  if (translatedPostText.value) {
    showPostTranslation.value = true
    return
  }
  translatingPost.value = true
  try {
    const response = await translateDiscoverPost(props.post.id, preferredLanguage.value)
    if (response.code === 200) {
      translatedPostText.value = response.data?.translatedText || ''
      showPostTranslation.value = true
    } else {
      notify.error(response.message || t('postDetail.translateFailed'))
    }
  } catch (error) {
    notify.error(t('postDetail.translateFailed'))
  } finally {
    translatingPost.value = false
  }
}

const getCommentTranslateState = (commentId) => {
  if (!commentTranslationState.value[commentId]) {
    commentTranslationState.value[commentId] = {
      translatedText: '',
      showTranslation: false,
      loading: false
    }
  }
  return commentTranslationState.value[commentId]
}

const canTranslateComment = (comment) => {
  if (!comment || !comment.content) return false
  return canTranslateText(comment.sourceLang)
}

const isCommentTranslating = (comment) => {
  return getCommentTranslateState(comment.id).loading
}

const isCommentTranslationVisible = (comment) => {
  return getCommentTranslateState(comment.id).showTranslation
}

const getCommentTranslatedText = (comment) => {
  return getCommentTranslateState(comment.id).translatedText
}

const toggleCommentTranslate = async (comment) => {
  if (!canTranslateComment(comment)) return
  const state = getCommentTranslateState(comment.id)
  if (state.showTranslation) {
    state.showTranslation = false
    return
  }
  if (state.translatedText) {
    state.showTranslation = true
    return
  }
  state.loading = true
  try {
    const response = await translateDiscoverComment(comment.id, preferredLanguage.value)
    if (response.code === 200) {
      state.translatedText = response.data?.translatedText || ''
      state.showTranslation = true
    } else {
      notify.error(response.message || t('postDetail.translateFailed'))
    }
  } catch (error) {
    notify.error(t('postDetail.translateFailed'))
  } finally {
    state.loading = false
  }
}

const submitComment = async () => {
  if (!props.post || !newComment.value.trim()) {
    return
  }
  submittingComment.value = true
  try {
    const payload = {
      content: newComment.value.trim()
    }
    if (replyToCommentId.value) {
      payload.parentId = replyToCommentId.value.toString()
    }
    const response = await createDiscoverPostComment(props.post.id, payload)
    if (response.code === 200) {
      const comment = response.data
      const list = Array.isArray(props.post.commentList) ? props.post.commentList : []
      let updatedList
      
      if (replyToCommentId.value) {
        const parentIndex = list.findIndex(c => String(c.id) === String(replyToCommentId.value))
        if (parentIndex !== -1) {
          updatedList = [...list.slice(0, parentIndex + 1), comment, ...list.slice(parentIndex + 1)]
        } else {
          updatedList = [...list, comment]
        }
      } else {
        updatedList = [...list, comment]
      }
      
      const updatedPost = {
        ...props.post,
        comments: (props.post.comments || 0) + 1,
        commentList: updatedList
      }
      emit('update', updatedPost)
      newComment.value = ''
      replyToCommentId.value = null
      replyToAuthor.value = ''
    } else {
      notify.error(response.message || t('postDetail.commentFailed'))
    }
  } catch (error) {
    notify.error(t('postDetail.commentFailedRetry'))
  } finally {
    submittingComment.value = false
  }
}

const replyToComment = (comment) => {
  replyToCommentId.value = comment.id
  replyToAuthor.value = comment.author
}

const cancelReply = () => {
  replyToCommentId.value = null
  replyToAuthor.value = ''
}

const toggleCommentLike = async (comment) => {
  try {
    const response = await toggleDiscoverCommentLike(comment.id)
    if (response.code !== 200) {
      notify.error(response.message || t('postDetail.operationFailed'))
      return
    }
    const { liked, likes } = response.data
    comment.liked = liked
    comment.likes = likes
  } catch (error) {
    notify.error(t('postDetail.likeFailed'))
  }
}

const canDeleteComment = (comment) => {
  if (!currentUser || !props.post) return false
  return currentUser.id === comment.userId || currentUser.id === props.post.author?.id
}

const handleDeleteComment = async (comment, index) => {
  confirm({
    title: t('postDetail.confirmDeleteCommentTitle'),
    message: t('postDetail.confirmDeleteComment'),
    confirmText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onConfirm: async () => {
      try {
        const response = await deleteDiscoverComment(comment.id)
        if (response.code === 200) {
          const list = props.post.commentList || []
          const idsToRemove = getAllDescendantIds(comment.id, list)
          idsToRemove.add(comment.id)
          const updatedPost = {
            ...props.post,
            commentList: list.filter(c => !idsToRemove.has(c.id)),
            comments: Math.max(0, (props.post.comments || 0) - idsToRemove.size)
          }
          emit('update', updatedPost)
        } else {
          notify.error(response.message || t('postDetail.deleteFailed'))
        }
      } catch (error) {
        notify.error(t('postDetail.deleteFailedRetry'))
      }
    }
  })
}

const getAllDescendantIds = (parentId, comments) => {
  const ids = new Set()
  comments.forEach(c => {
    if (c.parentId === parentId) {
      ids.add(c.id)
      const childIds = getAllDescendantIds(c.id, comments)
      childIds.forEach(id => ids.add(id))
    }
  })
  return ids
}

const goToUserHomepage = (userId) => {
  if (!userId) return
  emit('close')
  router.push(`/user-homepage/${userId}`)
}

const toggleFollow = async () => {
  if (!props.post || !props.post.author) return
  const authorId = props.post.author.id
  if (currentUser && authorId === currentUser.id) {
    notify.warning(t('postDetail.cannotChatWithSelf'))
    return
  }
  
  followSubmitting.value = true
  try {
    const response = isFollowing.value ? await unfollowUser(authorId) : await followUser(authorId)
    if (response.code === 200) {
      isFollowing.value = !isFollowing.value
      notify.success(isFollowing.value ? t('postDetail.followSuccess') : t('postDetail.unfollowSuccess'))
    } else {
      notify.error(response.message || t('postDetail.operationFailed'))
    }
  } catch (error) {
    notify.error(t('postDetail.operationFailed'))
  } finally {
    followSubmitting.value = false
  }
}

const openShareModal = () => {
  if (!props.post) return
  const baseUrl = window.location.origin
  shareUrl.value = `${baseUrl}/share?postId=${props.post.id}`
  showShareModal.value = true
}

const closeShareModal = () => {
  showShareModal.value = false
}

const shareToWechat = () => {
  const title = props.post?.title || t('postDetail.sharePost')
  const desc = props.post?.content?.substring(0, 50) || ''
  const url = `https://cli.im/api/qrcode/code?text=${encodeURIComponent(shareUrl.value)}`
  window.open(url, '_blank', 'width=600,height=600')
}

const shareToQQ = () => {
  const title = props.post?.title || t('postDetail.sharePost')
  const desc = props.post?.content?.substring(0, 100) || ''
  const url = `https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(shareUrl.value)}&title=${encodeURIComponent(title)}&desc=${encodeURIComponent(desc)}`
  window.open(url, '_blank', 'width=600,height=500')
}

const shareToWeibo = () => {
  const title = props.post?.title || t('postDetail.sharePost')
  const url = `https://service.weibo.com/share/share.php?url=${encodeURIComponent(shareUrl.value)}&title=${encodeURIComponent(title)}`
  window.open(url, '_blank', 'width=600,height=500')
}

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    notify.success(t('postDetail.linkCopied'))
  } catch (err) {
    const textArea = document.createElement('textarea')
    textArea.value = shareUrl.value
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    notify.success(t('postDetail.linkCopied'))
  }
}
</script>

<style scoped>
.post-modal {
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

.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
}

.modal-content {
  position: relative;
  display: flex;
  width: 90vw;
  height: 90vh;
  max-width: 1200px;
  background: white;
  border-radius: 12px;
  z-index: 1001;
}

.modal-left {
  flex: 1;
  position: relative;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-slider {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-image {
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

.modal-right {
  width: 400px;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
  gap: 10px;
}

.modal-author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.modal-author-info {
  flex: 1;
}

.modal-author-info h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.modal-author-info p {
  margin: 3px 0 0 0;
  font-size: 12px;
  color: #999;
}

.clickable-user {
  cursor: pointer;
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

.contact-btn.active {
  background: #2f6bff;
  box-shadow: 0 2px 4px rgba(47, 107, 255, 0.3);
}

.contact-btn.active:hover {
  background: #2454ce;
  box-shadow: 0 4px 8px rgba(47, 107, 255, 0.4);
}

.modal-post-view{
  overflow-y: auto;
}

.modal-post-content {
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-post-content h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: 600;
}

.modal-post-content p {
  margin: 0 0 10px 0;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  white-space: pre-line;
}

.translated-text {
  margin: 0 0 10px 0;
  font-size: 13px;
  line-height: 1.5;
  color: #888;
  white-space: pre-line;
}

.translate-toggle-btn {
  border: none;
  background: #f1f5ff;
  color: #2f6bff;
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.translate-toggle-btn:hover {
  background: #dce8ff;
  color: #2454ce;
}

.translate-toggle-btn.active {
  background: #f0f0f0;
  color: #666;
}

.translate-toggle-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.hashtags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.hashtag {
  font-size: 12px;
  color: #ff2442;
  cursor: pointer;
  padding: 4px 10px;
  background: #fff5f5;
  border-radius: 12px;
  transition: all 0.3s;
}

.hashtag:hover {
  background: #ffe0e0;
}

.modal-comments {
  flex: 1;
  padding: 15px;
}

.modal-comments h4 {
  margin: 0 0 15px 0;
  font-size: 14px;
  font-weight: 600;
}

.comment-item {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.comment-author {
  font-size: 12px;
  font-weight: 600;
}

.comment-time {
  font-size: 11px;
  color: #999;
}

.comment-text {
  margin: 0 0 5px 0;
  font-size: 13px;
  line-height: 1.4;
  color: #333;
  white-space: pre-line;
}

.comment-actions {
  display: flex;
  gap: 15px;
  font-size: 11px;
}

.comment-translation-text {
  margin: 0 0 6px 0;
  font-size: 12px;
  line-height: 1.4;
  color: #888;
  white-space: pre-line;
}

.comment-like {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.comment-like .bx-heart.liked {
  color: #ff2442;
}

.comment-reply {
  cursor: pointer;
}

.comment-reply:hover {
  color: #ff2442;
}

.comment-translate {
  cursor: pointer;
  color: #2f6bff;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.comment-translate:hover {
  color: #2454ce;
}

.comment-delete {
  cursor: pointer;
}

.comment-delete:hover {
  color: #ff2442;
}

.reply-comment {
  margin-left: 42px;
}

.reply-to {
  color: #ff2442;
  margin-right: 5px;
}

.reply-info {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #ff2442;
  margin-bottom: 5px;
}

.reply-info i {
  cursor: pointer;
}

.modal-footer {
  padding: 10px;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
}

.interaction-buttons {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 15px;
}

.interaction-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  background: none;
  border: none;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.3s;
}

.interaction-btn:hover {
  color: #ff2442;
}

.interaction-btn i {
  font-size: 20px;
}

.interaction-btn .bx-star.collected {
  color: #ff2442;
}

.comment-input {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 20px;
}

.comment-input input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  color: #333;
}

.send-btn {
  padding: 6px 12px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
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

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
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
  max-width: 90%;
  max-height: 90%;
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

.share-btn {
  margin-left: auto;
}

.report-btn {
  color: #b91c1c;
}

.report-btn:hover {
  background: #fee2e2;
}

.share-modal {
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

.share-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
}

.share-content {
  position: relative;
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 400px;
  max-width: 90vw;
  z-index: 1;
}

.share-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.share-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.share-close {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #666;
  transition: all 0.3s;
}

.share-close:hover {
  background: #eee;
  color: #333;
}

.share-options {
  display: flex;
  justify-content: space-around;
  margin-bottom: 24px;
}

.share-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.3s;
}

.share-option:hover {
  transform: translateY(-4px);
}

.share-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
}

.share-icon.wechat {
  background: #07c160;
}

.share-icon.qq {
  background: #12b7f5;
}

.share-icon.weibo {
  background: #e6162d;
}

.share-icon.link {
  background: #666;
}

.share-option span {
  font-size: 12px;
  color: #666;
}

.share-link-preview {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 12px;
}

.share-link-preview p {
  margin: 0;
  font-size: 12px;
  color: #666;
  word-break: break-all;
}

/* 评论高亮动画 */
.comment-highlight {
  animation: commentHighlight 1.5s ease-in-out 2;
  border-radius: 8px;
}

@keyframes commentHighlight {
  0% {
    background-color: transparent;
  }
  50% {
    background-color: rgba(116, 148, 236, 0.3);
  }
  100% {
    background-color: transparent;
  }
}

@media (max-width: 768px) {
  .modal-content {
    flex-direction: column;
    width: 100vw;
    height: 100vh;
    max-width: none;
    border-radius: 0;
  }
  
  .modal-left {
    width: 100%;
    height: 40vh;
    min-height: 200px;
    max-height: 300px;
  }
  
  .modal-right {
    width: 100%;
    flex: 1;
    overflow: hidden;
  }
  
  .modal-post-view {
    flex: 1;
    overflow-y: auto;
  }
  
  .modal-header {
    padding: 12px;
  }
  
  .modal-author-avatar {
    width: 36px;
    height: 36px;
  }
  
  .modal-author-info h4 {
    font-size: 14px;
  }
  
  .modal-author-info p {
    font-size: 11px;
  }
  
  .contact-btn {
    padding: 4px 10px;
    font-size: 12px;
  }
  
  .modal-post-content {
    padding: 12px;
  }
  
  .modal-post-content h3 {
    font-size: 15px;
  }
  
  .modal-post-content p {
    font-size: 13px;
  }
  
  .modal-comments {
    padding: 12px;
  }
  
  .modal-comments h4 {
    font-size: 14px;
    margin-bottom: 10px;
  }
  
  .comment-item {
    margin-bottom: 10px;
  }
  
  .comment-avatar {
    width: 28px;
    height: 28px;
  }
  
  .comment-author {
    font-size: 12px;
  }
  
  .comment-time {
    font-size: 10px;
  }
  
  .comment-text {
    font-size: 12px;
  }
  
  .comment-actions {
    font-size: 10px;
  }
  
  .modal-footer {
    padding: 8px;
    flex-shrink: 0;
  }
  
  .interaction-buttons {
    gap: 12px;
    margin-bottom: 10px;
  }
  
  .interaction-btn {
    gap: 3px;
    font-size: 12px;
  }
  
  .interaction-btn i {
    font-size: 16px;
  }
  
  .comment-input {
    padding: 8px;
  }
  
  .comment-input input {
    font-size: 13px;
  }
  
  .send-btn {
    padding: 5px 10px;
    font-size: 11px;
  }
  
  .slider-btn {
    width: 32px;
    height: 32px;
    font-size: 18px;
  }
  
  .prev-btn {
    left: 10px;
  }
  
  .next-btn {
    right: 10px;
  }
  
  .image-indicators {
    bottom: 10px;
  }
  
  .indicator-dot {
    width: 6px;
    height: 6px;
  }
  
  .indicator-dot.active {
    width: 16px;
  }
  
  .image-counter {
    font-size: 11px;
    padding: 4px 10px;
  }
  
  .translate-toggle-btn {
    font-size: 11px;
    padding: 4px 8px;
  }
  
  .hashtag {
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .modal-left {
    height: 35vh;
    min-height: 180px;
  }
  
  .modal-header {
    padding: 10px;
  }
  
  .modal-author-avatar {
    width: 32px;
    height: 32px;
  }
  
  .contact-btn {
    padding: 3px 8px;
    font-size: 11px;
  }
  
  .modal-post-content {
    padding: 10px;
  }
  
  .modal-post-content h3 {
    font-size: 14px;
  }
  
  .modal-post-content p {
    font-size: 12px;
  }
  
  .modal-comments {
    padding: 10px;
  }
  
  .interaction-buttons {
    gap: 8px;
    flex-wrap: wrap;
  }
  
  .interaction-btn {
    font-size: 11px;
  }
  
  .interaction-btn i {
    font-size: 14px;
  }
  
  .share-content {
    width: 95vw;
    padding: 16px;
  }
  
  .share-options {
    flex-wrap: wrap;
    gap: 16px;
  }
  
  .share-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }
}
</style>
