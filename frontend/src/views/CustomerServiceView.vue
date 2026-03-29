<template>
  <div class="customer-service-page">
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
      <div class="service-layout">
        <aside class="service-sidebar">
          <div class="sidebar-header">
            <h3>{{ $t('notification.customerService') }}</h3>
            <p>??????????????</p>
          </div>

          <div v-if="loadingList" class="sidebar-empty">???...</div>
          <div v-else-if="conversations.length === 0" class="sidebar-empty">??????</div>
          <div v-else class="conversation-list">
            <button
              v-for="conversation in conversations"
              :key="conversation.id"
              type="button"
              class="conversation-item"
              :class="{ active: currentConversation?.id === conversation.id }"
              @click="selectConversation(conversation)"
            >
              <div class="conversation-avatar">
                <img :src="conversation.avatar || defaultAvatar" alt="avatar" />
              </div>
              <div class="conversation-copy">
                <strong>{{ getConversationTitle(conversation) }}</strong>
                <span>{{ conversation.lastMessage || '????' }}</span>
              </div>
              <div v-if="conversation.unreadCount > 0" class="conversation-badge">
                {{ conversation.unreadCount > 99 ? '99+' : conversation.unreadCount }}
              </div>
            </button>
          </div>
        </aside>

        <section class="service-panel">
          <template v-if="currentConversation">
            <div class="panel-header">
              <h3>{{ getConversationTitle(currentConversation) }}</h3>
              <p>????</p>
            </div>

            <div class="message-list" ref="messagesContainer">
              <div
                v-for="message in messages"
                :key="message.id"
                class="message-row"
                :class="{ self: message.isSelf }"
              >
                <div class="message-bubble">
                  <div class="message-content">{{ message.content }}</div>
                  <div class="message-time">{{ message.time }}</div>
                </div>
              </div>
              <div v-if="messages.length === 0" class="panel-empty">??????</div>
            </div>

            <div class="composer">
              <input
                v-model="draft"
                type="text"
                :placeholder="$t('notification.inputPlaceholder')"
                @keyup.enter="sendReply"
              />
              <button @click="sendReply" :disabled="sending || !draft.trim()">{{ $t('notification.send') }}</button>
            </div>
          </template>
          <div v-else class="panel-empty">???????</div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { getCurrentInstance, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  createCustomerServiceConversation,
  getConversations,
  getMessages,
  markAsRead,
  sendMessage
} from '../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=travelate-user'
const loadingList = ref(false)
const sending = ref(false)
const conversations = ref([])
const currentConversation = ref(null)
const messages = ref([])
const draft = ref('')
const messagesContainer = ref(null)

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const getConversationTitle = (conversation) => {
  if (!conversation) {
    return t('notification.customerService')
  }
  return conversation.name || t('notification.customerService')
}

const loadMessagesForConversation = async (conversationId) => {
  const response = await getMessages(conversationId)
  if (response.code !== 200) {
    throw new Error(response.message || '????????')
  }
  messages.value = Array.isArray(response.data) ? response.data : []
  scrollToBottom()
}

const selectConversation = async (conversation) => {
  currentConversation.value = conversation
  await loadMessagesForConversation(conversation.id)
  if (conversation.unreadCount > 0) {
    await markAsRead(conversation.id)
    conversation.unreadCount = 0
  }
}

const ensureCustomerServiceConversation = async () => {
  const response = await createCustomerServiceConversation()
  if (response.code !== 200) {
    throw new Error(response.message || '????????')
  }
  return response.data
}

const loadConversationList = async () => {
  const response = await getConversations()
  if (response.code !== 200) {
    throw new Error(response.message || '????????')
  }
  conversations.value = (Array.isArray(response.data) ? response.data : []).filter(item => item.type === 'service')
}

const initPage = async () => {
  loadingList.value = true
  try {
    await loadConversationList()
    if (conversations.value.length === 0) {
      const created = await ensureCustomerServiceConversation()
      await loadConversationList()
      const target = conversations.value.find(item => String(item.id) === String(created.id)) || conversations.value[0]
      if (target) {
        await selectConversation(target)
      }
      return
    }

    const conversationId = route.query.conversationId
    const target = conversations.value.find(item => String(item.id) === String(conversationId)) || conversations.value[0]
    if (target) {
      await selectConversation(target)
    }
  } catch (error) {
    notify.error(error.message || t('personal.contactServiceFailed'))
  } finally {
    loadingList.value = false
  }
}

const sendReply = async () => {
  if (!currentConversation.value || !draft.value.trim() || sending.value) {
    return
  }

  sending.value = true
  try {
    const response = await sendMessage(currentConversation.value.id, {
      content: draft.value.trim()
    })
    if (response.code !== 200) {
      throw new Error(response.message || '????')
    }
    messages.value.push(response.data)
    currentConversation.value.lastMessage = response.data.content
    currentConversation.value.lastMessageTime = response.data.time
    draft.value = ''
    scrollToBottom()
  } catch (error) {
    notify.error(error.message || '????')
  } finally {
    sending.value = false
  }
}

const goBack = () => {
  router.push('/home/personal')
}

onMounted(() => {
  initPage()
})
</script>

<style scoped>
.customer-service-page {
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
  max-width: 1320px;
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
  color: #666;
}

.nav-button:hover {
  background: #f0f0f0;
  color: #7494ec;
}

.main-container {
  padding-top: 60px;
  min-height: 100vh;
}

.service-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 60px);
  padding: 20px;
}

.service-sidebar,
.service-panel {
  background: white;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.service-sidebar {
  padding: 18px;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 18px;
  color: #243b53;
}

.sidebar-header p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #7b8794;
}

.sidebar-empty,
.panel-empty {
  display: grid;
  place-items: center;
  min-height: 180px;
  color: #7b8794;
  text-align: center;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  border: 1px solid #e8eef5;
  border-radius: 16px;
  padding: 12px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.conversation-item.active,
.conversation-item:hover {
  border-color: #7494ec;
  background: #f7faff;
}

.conversation-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.conversation-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.conversation-copy {
  flex: 1;
  min-width: 0;
}

.conversation-copy strong {
  display: block;
  font-size: 14px;
  color: #243b53;
}

.conversation-copy span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #7b8794;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-badge {
  min-width: 20px;
  padding: 2px 6px;
  border-radius: 999px;
  background: #7494ec;
  color: #fff;
  font-size: 12px;
  text-align: center;
}

.service-panel {
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 20px 22px;
  border-bottom: 1px solid #e8eef5;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  color: #243b53;
}

.panel-header p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #7b8794;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f8fafc;
}

.message-row {
  display: flex;
  margin-bottom: 14px;
}

.message-row.self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 14px;
  border-radius: 16px;
  background: white;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

.message-row.self .message-bubble {
  background: #7494ec;
  color: white;
}

.message-content {
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  margin-top: 6px;
  font-size: 12px;
  color: #7b8794;
  text-align: right;
}

.message-row.self .message-time {
  color: rgba(255, 255, 255, 0.78);
}

.composer {
  display: flex;
  gap: 12px;
  padding: 18px 20px;
  border-top: 1px solid #e8eef5;
  background: white;
}

.composer input {
  flex: 1;
  min-width: 0;
  height: 44px;
  border: 1px solid #d9e2ec;
  border-radius: 999px;
  padding: 0 16px;
  font-size: 14px;
  outline: none;
}

.composer input:focus {
  border-color: #7494ec;
}

.composer button {
  border: none;
  border-radius: 999px;
  padding: 0 20px;
  background: #7494ec;
  color: white;
  cursor: pointer;
}

.composer button:disabled {
  background: #cbd2d9;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .service-layout {
    grid-template-columns: 1fr;
  }
}
</style>
