<template>
  <div>
    <div class="customer-service-modal" v-if="visible">
      <div class="modal-overlay" @click="$emit('close')"></div>
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ uiText.title }}</h3>
          <button class="close-btn" @click="$emit('close')">
            <i class='bx bx-x'></i>
          </button>
        </div>

        <div class="service-layout">
          <section class="service-panel service-panel--solo">
            <div v-if="loadingList" class="panel-empty">{{ uiText.loadingList }}</div>
            <template v-else-if="currentConversation">
              <div class="message-list" ref="messagesContainer">
                <div
                  v-for="message in messages"
                  :key="message.id"
                  class="message-row"
                  :class="{ self: message.isSelf }"
                >
                  <img
                    :src="message.isSelf ? currentUserAvatar : serviceAvatar"
                    :alt="message.isSelf ? 'user avatar' : 'service avatar'"
                    class="message-avatar"
                  />
                  <div class="message-bubble">
                    <div class="message-content">{{ message.content }}</div>
                    <div class="message-time">{{ message.time }}</div>
                  </div>
                </div>
                <div v-if="messages.length === 0" class="panel-empty">{{ uiText.emptyMessages }}</div>
              </div>

              <div class="composer">
                <input
                  v-model="draft"
                  type="text"
                  :placeholder="uiText.inputPlaceholder"
                  @keyup.enter="sendReply"
                />
                <button @click="sendReply" :disabled="sending || !draft.trim()">{{ uiText.send }}</button>
              </div>
            </template>
            <div v-else class="panel-empty">{{ uiText.emptyList }}</div>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  createCustomerServiceConversation,
  getConversations,
  getMessages,
  markAsRead,
  sendMessage
} from '../api/app'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const route = useRoute()
const { locale } = useI18n()

const uiText = computed(() => {
  if (String(locale.value || '').startsWith('zh')) {
    return {
      title: '\u5728\u7ebf\u5ba2\u670d',
      loadingList: '\u6b63\u5728\u52a0\u8f7d\u4f1a\u8bdd...',
      emptyList: '\u6682\u65e0\u5ba2\u670d\u4f1a\u8bdd',
      emptyMessages: '\u6682\u65e0\u6d88\u606f\u8bb0\u5f55',
      inputPlaceholder: '\u8f93\u5165\u6d88\u606f...',
      send: '\u53d1\u9001',
      loadMessagesFailed: '\u52a0\u8f7d\u6d88\u606f\u5931\u8d25',
      createConversationFailed: '\u521b\u5efa\u5ba2\u670d\u4f1a\u8bdd\u5931\u8d25',
      loadConversationsFailed: '\u52a0\u8f7d\u4f1a\u8bdd\u5931\u8d25',
      sendFailed: '\u53d1\u9001\u5931\u8d25'
    }
  }

  return {
    title: 'Customer Service',
    loadingList: 'Loading conversations...',
    emptyList: 'No service conversations yet',
    emptyMessages: 'No messages yet',
    inputPlaceholder: 'Type a message...',
    send: 'Send',
    loadMessagesFailed: 'Failed to load messages',
    createConversationFailed: 'Failed to create the service conversation',
    loadConversationsFailed: 'Failed to load conversations',
    sendFailed: 'Failed to send'
  }
})

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=travelate-user'
const loadingList = ref(false)
const sending = ref(false)
const conversations = ref([])
const currentConversation = ref(null)
const messages = ref([])
const draft = ref('')
const messagesContainer = ref(null)

const readStoredUser = () => {
  try {
    const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

const currentUserAvatar = computed(() => {
  const user = readStoredUser()
  return user?.avatar || defaultAvatar
})

const serviceAvatar = computed(() => currentConversation.value?.avatar || defaultAvatar)

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const loadMessagesForConversation = async (conversationId) => {
  const response = await getMessages(conversationId)
  if (response.code !== 200) {
    throw new Error(response.message || uiText.value.loadMessagesFailed)
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
    throw new Error(response.message || uiText.value.createConversationFailed)
  }
  return response.data
}

const loadConversationList = async () => {
  const response = await getConversations()
  if (response.code !== 200) {
    throw new Error(response.message || uiText.value.loadConversationsFailed)
  }
  conversations.value = (Array.isArray(response.data) ? response.data : []).filter(item => item.type === 'service')
}

const initPage = async () => {
  loadingList.value = true
  currentConversation.value = null
  messages.value = []

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
    notify.error(error.message || uiText.value.loadConversationsFailed)
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
      throw new Error(response.message || uiText.value.sendFailed)
    }
    messages.value.push(response.data)
    currentConversation.value.lastMessage = response.data.content
    currentConversation.value.lastMessageTime = response.data.time
    draft.value = ''
    scrollToBottom()
  } catch (error) {
    notify.error(error.message || uiText.value.sendFailed)
  } finally {
    sending.value = false
  }
}

watch(() => props.visible, (newValue) => {
  if (newValue) {
    initPage()
  } else {
    currentConversation.value = null
    messages.value = []
    draft.value = ''
  }
})

onMounted(() => {
  if (props.visible) {
    initPage()
  }
})
</script>

<style scoped>
.customer-service-modal {
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
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(2px);
}

.modal-content {
  position: relative;
  width: min(840px, calc(100vw - 48px));
  height: min(760px, calc(100dvh - 56px));
  min-height: 500px;
  background: white;
  border-radius: 12px;
  z-index: 1001;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e8eef5;
  background: white;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #243b53;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #666;
  transition: all 0.3s;
}

.close-btn:hover {
  background: #e8e8e8;
  color: #333;
}

.service-layout {
  height: 100%;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.service-panel {
  height: 100%;
  background: white;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.service-panel--solo {
  width: 100%;
}

.panel-empty {
  display: grid;
  place-items: center;
  min-height: 180px;
  color: #7b8794;
  text-align: center;
  flex: 1;
  padding: 24px;
}

.message-list {
  flex: 1;
  min-height: 0;
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
  background: #f8fafc;
  scrollbar-width: thin;
  scrollbar-color: rgba(116, 148, 236, 0.65) transparent;
}

.message-list::-webkit-scrollbar {
  width: 10px;
}

.message-list::-webkit-scrollbar-track {
  background: transparent;
}

.message-list::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(116, 148, 236, 0.78), rgba(99, 129, 217, 0.68));
  border-radius: 999px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.message-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(116, 148, 236, 0.95), rgba(99, 129, 217, 0.88));
  border: 2px solid transparent;
  background-clip: padding-box;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 16px;
}

.message-row.self {
  justify-content: flex-end;
}

.message-row.self .message-avatar {
  order: 2;
}

.message-row.self .message-bubble {
  order: 1;
  background: #7494ec;
  color: white;
}

.message-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: #e8eef5;
}

.message-bubble {
  max-width: min(70%, 560px);
  padding: 12px 14px;
  border-radius: 16px;
  background: white;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
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
  flex-shrink: 0;
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
  transition: all 0.3s;
}

.composer button:hover:not(:disabled) {
  background: #6381d9;
}

.composer button:disabled {
  background: #cbd2d9;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .modal-content {
    width: 100vw;
    height: 100dvh;
    min-height: 100dvh;
    border-radius: 0;
  }

  .message-list,
  .composer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .message-bubble {
    max-width: 82%;
  }
}
</style>
