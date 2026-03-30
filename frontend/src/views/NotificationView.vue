<template>
  <div class="notification-page">
    <div class="top-nav">
      <div class="nav-content">
        <div class="logo">YayFolk</div>
        <div class="nav-buttons">
          <div class="nav-button" @click="goBack">
            <i class='bx bx-arrow-back'></i>
            <span>{{ $t('common.back') }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="main-container">
      <div class="chat-layout">
        <div class="conversation-list" :class="{ collapsed: isListCollapsed }">
          <div class="list-header">
            <h3 v-if="!isListCollapsed">{{ $t('notification.title') }}</h3>
            <button class="toggle-btn" @click="toggleList">
              <i :class="isListCollapsed ? 'bx bx-chevron-right' : 'bx bx-chevron-left'"></i>
            </button>
          </div>
          <div class="list-content">
            <div
              v-for="conv in conversations"
              :key="conv.id"
              class="conversation-item"
              :class="{ active: currentConversation?.id === conv.id }"
              @click="selectConversation(conv)"
              @contextmenu="handleContextMenu($event, conv)"
            >
              <div class="conv-avatar">
                <i v-if="conv.type === 'comment'" class='bx bx-message-detail'></i>
                <i v-else-if="conv.type === 'collection'" class='bx bx-star'></i>
                <img v-else :src="conv.avatar || defaultAvatar" alt="avatar" />
                <div v-if="conv.unreadCount > 0 && isListCollapsed" class="conv-unread-dot"></div>
              </div>
              <div class="conv-info" v-if="!isListCollapsed">
                <div class="conv-name">{{ getConversationName(conv) }}</div>
                <div class="conv-last-message">{{ conv.lastMessage }}</div>
              </div>
              <div v-if="conv.unreadCount > 0 && !isListCollapsed" class="conv-unread">
                {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
              </div>
            </div>
          </div>
        </div>

        <div class="chat-area">
          <template v-if="currentConversation">
            <div class="chat-header">
              <div class="chat-title">
                <template v-if="currentConversation.type === 'comment'">
                  <i class='bx bx-message-detail'></i>
                  <span>{{ $t('notification.commentNotif') }}</span>
                </template>
                <template v-else-if="currentConversation.type === 'collection'">
                  <i class='bx bx-star'></i>
                  <span>{{ $t('notification.collectionNotif') }}</span>
                </template>
                <template v-else>
                  <span>{{ getConversationName(currentConversation) }}</span>
                </template>
              </div>
              <button 
                v-if="currentConversation.type === 'comment' || currentConversation.type === 'collection'"
                class="clear-btn"
                @click="handleClearNotifications"
              >
                <i class='bx bx-trash'></i>
                <span>{{ $t('notification.clearAll') }}</span>
              </button>
            </div>

            <div class="chat-messages" ref="messagesContainer">
              <template v-if="currentConversation.type === 'comment' || currentConversation.type === 'collection'">
                <div
                  v-for="notif in notifications"
                  :key="notif.id"
                  class="notification-item"
                  :class="{ unread: !notif.isRead, clickable: notif.postId }"
                  @click="goToPostDetail(notif)"
                >
                  <img :src="notif.fromUserAvatar || defaultAvatar" class="notif-avatar" />
                  <div class="notif-content">
                    <div class="notif-header">
                      <span class="notif-user">{{ notif.fromUserName }}</span>
                      <span class="notif-time">{{ notif.time }}</span>
                    </div>
                    <div class="notif-text">{{ getNotificationContent(notif) }}</div>
                  </div>
                  <i v-if="notif.postId" class='bx bx-chevron-right notif-arrow'></i>
                </div>
                <div v-if="notifications.length === 0" class="empty-state">
                  <i class='bx bx-inbox'></i>
                  <p>{{ $t('notification.emptyTitle') }}</p>
                </div>
              </template>
              <template v-else>
                <div
                  v-for="msg in messages"
                  :key="msg.id"
                  class="message-item"
                  :class="{ self: msg.isSelf }"
                  @contextmenu="handleMessageContextMenu($event, msg)"
                >
                  <div class="message-bubble">
                    <div class="message-content">{{ msg.content }}</div>
                    <div v-if="isMessageTranslationVisible(msg)" class="message-translation">{{ getMessageTranslatedText(msg) }}</div>
                    <div class="message-time">{{ msg.time }}</div>
                  </div>
                </div>
                <div v-if="messages.length === 0" class="empty-state">
                  <i class='bx bx-message-rounded-dots'></i>
                  <p>{{ $t('notification.noMessages') }}</p>
                </div>
              </template>
            </div>

            <div v-if="currentConversation.type === 'chat' || currentConversation.type === 'service'" class="chat-input">
              <input
                v-model="newMessage"
                type="text"
                :placeholder="$t('notification.inputPlaceholder')"
                @keyup.enter="sendMessage"
              />
              <button @click="sendMessage" :disabled="sendingMessage">{{ $t('notification.send') }}</button>
            </div>
          </template>
          <template v-else>
            <div class="empty-chat">
              <i class='bx bx-message-square-detail'></i>
              <p>{{ $t('notification.selectConversation') }}</p>
            </div>
          </template>
        </div>
      </div>
    </div>

    <div 
      v-if="showContextMenu && contextMenuType === 'conversation'" 
      class="context-menu" 
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
      @click.stop
    >
      <div class="context-menu-item" @click="handleDeleteConversation">
        <i class='bx bx-trash'></i>
        <span>{{ $t('notification.deleteConversation') }}</span>
      </div>
    </div>

    <div 
      v-if="showContextMenu && contextMenuType === 'message'" 
      class="context-menu" 
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
      @click.stop
    >
      <div 
        v-if="canTranslateMessage(selectedMessage)" 
        class="context-menu-item" 
        @click="handleTranslateMessage"
      >
        <i v-if="isMessageTranslating(selectedMessage)" class='bx bx-loader-alt bx-spin'></i>
        <i v-else class='bx bx-globe'></i>
        <span>{{ isMessageTranslating(selectedMessage) ? $t('notification.translating') : (isMessageTranslationVisible(selectedMessage) ? $t('notification.viewOriginal') : $t('notification.translate')) }}</span>
      </div>
      <div class="context-menu-item" @click="handleDeleteMessage">
        <i class='bx bx-trash'></i>
        <span>{{ $t('notification.delete') }}</span>
      </div>
      <div 
        v-if="selectedMessage?.isSelf && canRecallMessage(selectedMessage)" 
        class="context-menu-item" 
        @click="handleRecallMessage"
      >
        <i class='bx bx-undo'></i>
        <span>{{ $t('notification.recall') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, nextTick, watch, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  getConversations,
  createConversation,
  getMessages,
  sendMessage as sendMessageApi,
  markAsRead,
  getNotifications,
  markNotificationsAsRead,
  deleteConversation,
  clearNotifications,
  deleteMessage,
  recallMessage,
  translateMessage
} from '../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const readStoredUser = () => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return {}
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    return {}
  }
}

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=travelate-user'
const currentUser = readStoredUser()
const isAdminUser = currentUser?.role === 'admin'
const conversations = ref([])
const currentConversation = ref(null)
const messages = ref([])
const notifications = ref([])
const newMessage = ref('')
const sendingMessage = ref(false)
const messagesContainer = ref(null)
const showContextMenu = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuType = ref('')
const selectedConvForDelete = ref(null)
const selectedMessage = ref(null)
const isListCollapsed = ref(false)
const messageTranslationState = ref({})

const getPreferredLanguage = () => {
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

const preferredLanguage = ref(getPreferredLanguage())

const getConversationName = (conv) => {
  if (!conv) {
    return ''
  }
  if (conv.type === 'comment') {
    return t('notification.commentNotif')
  }
  if (conv.type === 'collection') {
    return t('notification.collectionNotif')
  }
  if (conv.type === 'service' && !isAdminUser) {
    return t('notification.customerService')
  }
  return conv.name
}

const getNotificationContent = (notif) => {
  const content = notif.content || ''
  
  const colonIndex = content.indexOf(':')
  let userContent = ''
  let originalPrefix = ''
  
  if (colonIndex > -1) {
    originalPrefix = content.substring(0, colonIndex).trim()
    userContent = content.substring(colonIndex + 1).trim()
  }
  
  const type = currentConversation.value?.type
  if (type === 'comment') {
    if (userContent) {
      return `${t('notification.commentedYourPost')}: ${userContent}`
    }
    return t('notification.commentedYourPost')
  }
  if (type === 'collection') {
    return t('notification.collectedYourPost')
  }
  
  return content
}

const goBack = () => {
  router.push('/home/discover')
}

const toggleList = () => {
  isListCollapsed.value = !isListCollapsed.value
}

const loadConversations = async () => {
  try {
    const response = await getConversations()
    if (response.code === 200) {
      // 过滤掉客服会话，客服消息在专门的客服页面显示
      conversations.value = response.data.filter(item => item.type !== 'service')
    }
    return conversations.value
  } catch (error) {
    console.error('加载会话列表失败', error)
    return []
  }
}

const selectConversation = async (conv) => {
  currentConversation.value = conv

  if (conv.type === 'comment' || conv.type === 'collection') {
    await loadNotifications(conv.type)
    if (conv.unreadCount > 0) {
      await markNotificationsAsRead(conv.type)
      conv.unreadCount = 0
    }
  } else {
    await loadMessages(conv.id)
    if (conv.unreadCount > 0) {
      await markAsRead(conv.id)
      conv.unreadCount = 0
    }
  }

  scrollToBottom()
}

const loadMessages = async (conversationId) => {
  try {
    const response = await getMessages(conversationId)
    if (response.code === 200) {
      messages.value = response.data
    }
  } catch (error) {
    console.error('加载消息失败', error)
  }
}

const loadNotifications = async (type) => {
  try {
    const response = await getNotifications(type)
    if (response.code === 200) {
      notifications.value = response.data
    }
  } catch (error) {
    console.error('加载通知失败', error)
  }
}

const goToPostDetail = (notif) => {
  if (notif.postId) {
    router.push({
      path: '/home/discover',
      query: { 
        postId: notif.postId,
        commentId: notif.commentId 
      }
    })
  }
}

const handleContextMenu = (e, conv) => {
  if (conv.type === 'chat' || conv.type === 'service') {
    e.preventDefault()
    showContextMenu.value = true
    contextMenuX.value = e.clientX
    contextMenuY.value = e.clientY
    contextMenuType.value = 'conversation'
    selectedConvForDelete.value = conv
  }
}

const handleMessageContextMenu = (e, msg) => {
  e.preventDefault()
  showContextMenu.value = true
  contextMenuX.value = e.clientX
  contextMenuY.value = e.clientY
  contextMenuType.value = 'message'
  selectedMessage.value = msg
}

const closeContextMenu = () => {
  showContextMenu.value = false
  contextMenuType.value = ''
  selectedConvForDelete.value = null
  selectedMessage.value = null
}

const canRecallMessage = (msg) => {
  if (!msg || !msg.time) return false
  const msgTime = new Date(msg.time).getTime()
  const now = Date.now()
  return (now - msgTime) < 2 * 60 * 1000
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

const getMessageTranslateState = (messageId) => {
  if (!messageTranslationState.value[messageId]) {
    messageTranslationState.value[messageId] = {
      translatedText: '',
      showTranslation: false,
      loading: false
    }
  }
  return messageTranslationState.value[messageId]
}

const canTranslateMessage = (msg) => {
  if (!msg || !msg.content) return false
  return canTranslateText(msg.sourceLang)
}

const isMessageTranslating = (msg) => {
  if (!msg) return false
  return getMessageTranslateState(msg.id).loading
}

const isMessageTranslationVisible = (msg) => {
  if (!msg) return false
  return getMessageTranslateState(msg.id).showTranslation
}

const getMessageTranslatedText = (msg) => {
  if (!msg) return ''
  return getMessageTranslateState(msg.id).translatedText
}

const handleTranslateMessage = async () => {
  if (!selectedMessage.value || !canTranslateMessage(selectedMessage.value)) {
    closeContextMenu()
    return
  }
  const state = getMessageTranslateState(selectedMessage.value.id)
  if (state.showTranslation) {
    state.showTranslation = false
    closeContextMenu()
    return
  }
  if (state.translatedText) {
    state.showTranslation = true
    closeContextMenu()
    return
  }
  state.loading = true
  try {
    const response = await translateMessage(selectedMessage.value.id, preferredLanguage.value)
    if (response.code === 200) {
      state.translatedText = response.data?.translatedText || ''
      state.showTranslation = true
    } else {
      notify.error(response.message || t('notification.translateFailed'))
    }
  } catch (error) {
    notify.error(t('notification.translateFailedRetry'))
  } finally {
    state.loading = false
    closeContextMenu()
  }
}

const handleDeleteMessage = async () => {
  if (!selectedMessage.value) return
  
  confirm({
    title: t('notification.confirmDeleteMessageTitle'),
    message: t('notification.confirmDeleteMessage'),
    confirmText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onConfirm: async () => {
      try {
        const response = await deleteMessage(selectedMessage.value.id)
        if (response.code === 200) {
          messages.value = messages.value.filter(m => m.id !== selectedMessage.value.id)
          await loadConversations()
          const conv = conversations.value.find(c => c.id === currentConversation.value?.id)
          if (conv) {
            currentConversation.value = conv
          }
        } else {
          notify.error(response.message || t('notification.deleteFailed'))
        }
      } catch (error) {
        notify.error(t('notification.deleteFailedRetry'))
      } finally {
        closeContextMenu()
      }
    }
  })
}

const handleRecallMessage = async () => {
  if (!selectedMessage.value) return
  
  confirm({
    title: t('notification.confirmRecallMessageTitle'),
    message: t('notification.confirmRecallMessage'),
    confirmText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onConfirm: async () => {
      try {
        const response = await recallMessage(selectedMessage.value.id)
        if (response.code === 200) {
          messages.value = messages.value.filter(m => m.id !== selectedMessage.value.id)
          newMessage.value = response.data.content
          await loadConversations()
          const conv = conversations.value.find(c => c.id === currentConversation.value?.id)
          if (conv) {
            currentConversation.value = conv
          }
        } else {
          notify.error(response.message || t('notification.recallFailed'))
        }
      } catch (error) {
        notify.error(t('notification.recallFailedRetry'))
      } finally {
        closeContextMenu()
      }
    }
  })
}

const handleDeleteConversation = async () => {
  if (!selectedConvForDelete.value) return
  
  confirm({
    title: t('notification.confirmDeleteConversationTitle'),
    message: t('notification.confirmDeleteConversation'),
    confirmText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onConfirm: async () => {
      try {
        const response = await deleteConversation(selectedConvForDelete.value.id)
        if (response.code === 200) {
          conversations.value = conversations.value.filter(c => c.id !== selectedConvForDelete.value.id)
          if (currentConversation.value?.id === selectedConvForDelete.value.id) {
            currentConversation.value = null
            messages.value = []
          }
        } else {
          notify.error(response.message || t('notification.deleteFailed'))
        }
      } catch (error) {
        notify.error(t('notification.deleteFailedRetry'))
      } finally {
        closeContextMenu()
      }
    }
  })
}

const handleClearNotifications = async () => {
  if (!currentConversation.value) return
  
  const type = currentConversation.value.type
  const typeName = type === 'comment' ? t('notification.comment') : t('notification.collection')
  
  confirm({
    title: t('notification.confirmClearNotificationsTitle', { type: typeName }),
    message: t('notification.confirmClearNotifications', { type: typeName }),
    confirmText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onConfirm: async () => {
      try {
        const response = await clearNotifications(type)
        if (response.code === 200) {
          notifications.value = []
          const conv = conversations.value.find(c => c.type === type)
          if (conv) {
            conv.unreadCount = 0
          }
        } else {
          notify.error(response.message || t('notification.clearFailed'))
        }
      } catch (error) {
        notify.error(t('notification.clearFailedRetry'))
      }
    }
  })
}

const sendMessage = async () => {
  if (!newMessage.value.trim() || !currentConversation.value) return

  sendingMessage.value = true
  try {
    const response = await sendMessageApi(currentConversation.value.id, {
      content: newMessage.value.trim()
    })
    if (response.code === 200) {
      messages.value.push(response.data)
      newMessage.value = ''
      scrollToBottom()

      const conv = conversations.value.find(c => c.id === currentConversation.value.id)
      if (conv) {
        conv.lastMessage = response.data.content
        conv.lastMessageTime = response.data.time
      }
    }
  } catch (error) {
    console.error('发送消息失败', error)
  } finally {
    sendingMessage.value = false
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const initChatWithUser = async (userId) => {
  try {
    const response = await createConversation({ otherUserId: userId })
    if (response.code === 200) {
      await loadConversations()
      const conv = conversations.value.find(c => c.id === response.data.id)
      if (conv) {
        selectConversation(conv)
      }
    }
  } catch (error) {
    console.error('创建会话失败', error)
  }
}

const openConversationById = async (conversationId) => {
  if (!conversationId) {
    return
  }

  let conv = conversations.value.find(item => String(item.id) === String(conversationId))
  if (!conv) {
    await loadConversations()
    conv = conversations.value.find(item => String(item.id) === String(conversationId))
  }
  if (conv) {
    await selectConversation(conv)
  }
}

watch(() => route.query.userId, (newUserId) => {
  if (newUserId) {
    initChatWithUser(newUserId)
  }
}, { immediate: true })

watch(() => route.query.conversationId, (conversationId) => {
  if (conversationId) {
    openConversationById(conversationId)
  }
}, { immediate: true })

onMounted(() => {
  loadConversations()
  document.addEventListener('click', closeContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
})
</script>

<style scoped>
.notification-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.nav-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #7494ec;
}

.nav-buttons {
  display: flex;
  gap: 15px;
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  color: #666;
}

.nav-button:hover {
  background: #f0f0f0;
  color: #7494ec;
}

.nav-button i {
  font-size: 18px;
}

.main-container {
  padding-top: 60px;
  height: 100vh;
}

.chat-layout {
  display: flex;
  height: calc(100vh - 60px);
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
}

.conversation-list {
  width: 300px;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.conversation-list.collapsed {
  width: 70px;
}

.list-header {
  padding: 15px 20px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conversation-list.collapsed .list-header {
  padding: 15px 15px;
  justify-content: center;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.toggle-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #666;
  transition: all 0.3s;
}

.toggle-btn:hover {
  background: #e8e8e8;
  color: #7494ec;
}

.list-content {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.conversation-list.collapsed .conversation-item {
  justify-content: center;
  padding: 12px 0;
}

.conversation-item:hover {
  background: #f5f5f5;
}

.conversation-item.active {
  background: #e8f4ff;
}

.conv-avatar {
  width: 45px;
  height: 45px;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  margin-right: 12px;
  position: relative;
}

.conversation-list.collapsed .conv-avatar {
  margin-right: 0;
}

.conv-avatar i {
  font-size: 24px;
  color: #7494ec;
}

.conv-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.conv-last-message {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-unread {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  background: #7494ec;
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.conv-unread-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  background: #7494ec;
  border-radius: 50%;
  border: 2px solid white;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 15px 20px;
  border-bottom: 1px solid #e8e8e8;
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.chat-title i {
  font-size: 20px;
  color: #7494ec;
}

.clear-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 16px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.clear-btn:hover {
  background: #7494ec;
  border-color: #7494ec;
  color: white;
}

.clear-btn i {
  font-size: 14px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f9f9f9;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
}

.message-item.self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 60%;
  padding: 10px 15px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message-item.self .message-bubble {
  background: #7494ec;
  color: white;
}

.message-content {
  font-size: 14px;
  line-height: 1.4;
  word-break: break-word;
  white-space: pre-line;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 5px;
  text-align: right;
}

.message-item.self .message-time {
  color: rgba(255, 255, 255, 0.8);
}

.message-translation {
  font-size: 12px;
  line-height: 1.4;
  color: #888;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed #ddd;
  white-space: pre-line;
}

.message-item.self .message-translation {
  color: rgba(255, 255, 255, 0.7);
  border-top-color: rgba(255, 255, 255, 0.3);
}

.notification-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: white;
  margin-bottom: 10px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.notification-item.clickable {
  cursor: pointer;
  transition: all 0.2s;
}

.notification-item.clickable:hover {
  background: #f9f9f9;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.notification-item.unread {
  background: #e8f4ff;
  border-left: 3px solid #7494ec;
}

.notification-item.unread.clickable:hover {
  background: #d6eaff;
}

.notif-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
}

.notif-content {
  flex: 1;
}

.notif-arrow {
  font-size: 18px;
  color: #ccc;
  margin-left: 8px;
}

.notif-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.notif-user {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.notif-time {
  font-size: 12px;
  color: #999;
}

.notif-text {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
}

.chat-input {
  display: flex;
  padding: 15px 20px;
  border-top: 1px solid #e8e8e8;
  background: white;
}

.chat-input input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
}

.chat-input input:focus {
  border-color: #7494ec;
}

.chat-input button {
  margin-left: 10px;
  padding: 10px 20px;
  background: #7494ec;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.chat-input button:hover {
  background: #6381d9;
}

.chat-input button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.empty-chat i {
  font-size: 60px;
  margin-bottom: 15px;
}

.empty-chat p {
  font-size: 14px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px;
  color: #999;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 15px;
}

.empty-state p {
  font-size: 14px;
}

.context-menu {
  position: fixed;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 8px 0;
  min-width: 120px;
  z-index: 1000;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s;
  color: #333;
  font-size: 14px;
}

.context-menu-item:hover {
  background: #f5f5f5;
  color: #7494ec;
}

.context-menu-item i {
  font-size: 16px;
}

.notification-page {
  position: relative;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(157, 41, 41, 0.12), transparent 24%),
    radial-gradient(circle at top right, rgba(201, 145, 63, 0.12), transparent 18%),
    linear-gradient(180deg, rgba(255, 249, 241, 0.94), rgba(244, 235, 222, 0.94));
}

.notification-page::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
  opacity: 0.45;
}

.top-nav,
.main-container,
.context-menu {
  position: relative;
  z-index: 1;
}

.top-nav {
  position: sticky;
  top: 12px;
  left: auto;
  right: auto;
  height: auto;
  max-width: 1240px;
  margin: 0 auto;
  padding: 12px 16px 0;
  background: transparent;
  box-shadow: none;
}

.nav-content {
  border-radius: 24px;
  border: 1px solid rgba(190, 157, 124, 0.28);
  background:
    linear-gradient(135deg, rgba(157, 41, 41, 0.05), rgba(255, 255, 255, 0.88)),
    rgba(255, 251, 246, 0.9);
  box-shadow:
    0 20px 40px rgba(74, 46, 23, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.logo {
  position: relative;
  padding-left: 16px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: #2f241d;
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

.nav-button {
  min-height: 44px;
  padding: 0 16px;
  border-radius: 16px;
  border: 1px solid rgba(157, 41, 41, 0.12);
  background: rgba(255, 251, 246, 0.9);
  color: #7a6858;
  font-weight: 700;
}

.nav-button:hover {
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
  transform: translateY(-1px);
}

.main-container {
  padding: 18px 16px 96px;
  height: auto;
  min-height: calc(100vh - 24px);
}

.chat-layout {
  max-width: 1240px;
  min-height: calc(100vh - 142px);
  border-radius: 28px;
  border: 1px solid rgba(190, 157, 124, 0.28);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 244, 238, 0.92));
  box-shadow:
    0 24px 52px rgba(74, 46, 23, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  overflow: hidden;
}

.conversation-list {
  width: 320px;
  background:
    linear-gradient(180deg, rgba(255, 251, 246, 0.98), rgba(246, 239, 230, 0.94));
  border-right: 1px solid rgba(217, 207, 193, 0.82);
}

.conversation-list.collapsed {
  width: 84px;
}

.list-header {
  min-height: 78px;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(217, 207, 193, 0.82);
}

.list-header h3 {
  font-size: 18px;
  color: #2f241d;
}

.toggle-btn {
  width: 34px;
  height: 34px;
  background: rgba(157, 41, 41, 0.06);
  color: #9d2929;
}

.toggle-btn:hover {
  background: rgba(157, 41, 41, 0.12);
  color: #7a1d1d;
}

.conversation-item {
  min-height: 76px;
  padding: 14px 20px;
  transition: background-color 0.22s ease, transform 0.22s ease;
}

.conversation-item:hover {
  background: rgba(157, 41, 41, 0.06);
}

.conversation-item.active {
  background:
    linear-gradient(135deg, rgba(157, 41, 41, 0.12), rgba(201, 145, 63, 0.08));
}

.conversation-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 16px;
  bottom: 16px;
  width: 4px;
  border-radius: 999px;
  background: linear-gradient(180deg, #9d2929, #c9913f);
}

.conv-avatar {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  border: 1px solid rgba(217, 207, 193, 0.76);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 240, 232, 0.96));
  box-shadow: 0 10px 20px rgba(74, 46, 23, 0.06);
}

.conv-avatar i {
  color: #9d2929;
}

.conv-name {
  font-size: 14px;
  font-weight: 700;
  color: #2f241d;
}

.conv-last-message {
  color: #8f7b69;
}

.conv-unread,
.conv-unread-dot {
  background: linear-gradient(135deg, #c2410c, #e85d3f);
  box-shadow: 0 10px 18px rgba(194, 65, 12, 0.2);
}

.chat-area {
  background:
    radial-gradient(circle at top right, rgba(201, 145, 63, 0.08), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(248, 244, 238, 0.86));
}

.chat-header {
  min-height: 78px;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(217, 207, 193, 0.82);
  background: rgba(255, 251, 246, 0.74);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.chat-title {
  font-size: 18px;
  color: #2f241d;
}

.chat-title i {
  color: #9d2929;
}

.clear-btn {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(157, 41, 41, 0.12);
  background: rgba(255, 251, 246, 0.9);
  color: #8a2b2b;
  font-weight: 700;
}

.clear-btn:hover {
  background: linear-gradient(135deg, #9d2929, #b33030);
  border-color: transparent;
  color: #fff;
}

.chat-messages {
  padding: 24px;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.3), transparent 30%),
    rgba(247, 241, 233, 0.78);
}

.message-item {
  margin-bottom: 18px;
}

.message-bubble {
  max-width: min(68%, 620px);
  padding: 14px 16px 12px;
  border-radius: 20px;
  border: 1px solid rgba(217, 207, 193, 0.76);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 240, 232, 0.96));
  box-shadow:
    0 14px 28px rgba(74, 46, 23, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.message-item.self .message-bubble {
  background:
    linear-gradient(135deg, #7a1d1d 0%, #9d2929 52%, #b33d2d 100%);
  border-color: rgba(157, 41, 41, 0.28);
  box-shadow: 0 18px 30px rgba(157, 41, 41, 0.18);
}

.message-content {
  line-height: 1.75;
  color: #2f241d;
}

.message-translation {
  margin-top: 8px;
  padding-top: 8px;
  color: #6f6255;
  border-top-color: rgba(157, 41, 41, 0.16);
}

.message-item.self .message-content,
.message-item.self .message-translation {
  color: rgba(255, 249, 243, 0.92);
}

.notification-item {
  padding: 18px;
  margin-bottom: 12px;
  border-radius: 20px;
  border: 1px solid rgba(217, 207, 193, 0.76);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 240, 232, 0.96));
  box-shadow: 0 14px 28px rgba(74, 46, 23, 0.08);
}

.notification-item.clickable:hover {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 236, 226, 0.98));
  transform: translateY(-1px);
}

.notification-item.unread {
  background:
    linear-gradient(135deg, rgba(157, 41, 41, 0.08), rgba(255, 255, 255, 0.96));
  border-left: 4px solid #9d2929;
}

.notif-avatar {
  width: 44px;
  height: 44px;
  border: 2px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 18px rgba(74, 46, 23, 0.08);
}

.notif-user {
  color: #2f241d;
  font-weight: 700;
}

.notif-time,
.notif-text {
  color: #8f7b69;
}

.chat-input {
  padding: 18px 24px 20px;
  border-top: 1px solid rgba(217, 207, 193, 0.82);
  background: rgba(255, 251, 246, 0.82);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.chat-input input {
  min-height: 48px;
  border-radius: 18px;
  border-color: rgba(217, 207, 193, 0.9);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 240, 232, 0.96));
}

.chat-input input:focus {
  border-color: rgba(157, 41, 41, 0.42);
  box-shadow: 0 0 0 4px rgba(157, 41, 41, 0.1);
}

.chat-input button {
  min-height: 48px;
  margin-left: 12px;
  padding: 0 22px;
  background: linear-gradient(135deg, #9d2929, #b33030);
  font-weight: 700;
  box-shadow: 0 14px 26px rgba(157, 41, 41, 0.18);
}

.chat-input button:hover {
  background: linear-gradient(135deg, #891f1f, #a72f2f);
}

.empty-chat,
.empty-state {
  color: #8f7b69;
}

.empty-chat i,
.empty-state i {
  color: #c9913f;
}

.context-menu {
  border-radius: 18px;
  border: 1px solid rgba(190, 157, 124, 0.26);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 240, 232, 0.96));
  box-shadow: 0 22px 38px rgba(74, 46, 23, 0.14);
  overflow: hidden;
}

.context-menu-item {
  min-height: 44px;
  font-weight: 600;
  color: #2f241d;
}

.context-menu-item:hover {
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
}

@media (max-width: 900px) {
  .chat-layout {
    min-height: calc(100vh - 132px);
  }

  .conversation-list {
    width: 88px;
  }

  .conversation-list:not(.collapsed) {
    width: 260px;
  }
}

@media (max-width: 768px) {
  .top-nav {
    top: 8px;
    padding: 8px 12px 0;
  }

  .nav-content {
    border-radius: 18px;
    padding: 0 14px;
  }

  .logo {
    font-size: 22px;
  }

  .main-container {
    padding: 14px 12px 90px;
  }

  .chat-layout {
    min-height: calc(100vh - 116px);
    border-radius: 22px;
  }

  .conversation-list {
    width: 78px;
  }

  .conversation-list:not(.collapsed) {
    width: 220px;
  }

  .list-header,
  .chat-header {
    min-height: 70px;
    padding: 14px 16px;
  }

  .chat-messages,
  .chat-input {
    padding-left: 16px;
    padding-right: 16px;
  }

  .message-bubble {
    max-width: 82%;
  }

  .clear-btn span,
  .nav-button span {
    display: none;
  }
}
</style>


