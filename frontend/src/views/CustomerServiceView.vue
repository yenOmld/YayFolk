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
                    :alt="message.isSelf ? '用户头像' : '客服头像'"
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

const uiText = {
  title: '在线客服',
  loadingList: '正在加载会话...',
  emptyList: '暂无客服会话',
  emptyMessages: '暂无消息记录',
  inputPlaceholder: '输入消息...',
  send: '发送',
  loadMessagesFailed: '加载消息失败',
  createConversationFailed: '创建客服会话失败',
  loadConversationsFailed: '加载会话失败',
  sendFailed: '发送失败'
}

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

.modal-overlay {
  background:
    radial-gradient(circle at top, rgba(157, 41, 41, 0.18), transparent 30%),
    rgba(19, 10, 6, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.modal-content {
  border-radius: 28px;
  border: 1px solid rgba(190, 157, 124, 0.26);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 244, 238, 0.94));
  box-shadow:
    0 28px 56px rgba(74, 46, 23, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.modal-header {
  min-height: 76px;
  padding: 18px 22px;
  border-bottom: 1px solid rgba(217, 207, 193, 0.82);
  background:
    linear-gradient(135deg, rgba(157, 41, 41, 0.05), rgba(255, 255, 255, 0.88)),
    rgba(255, 251, 246, 0.92);
}

.modal-header h3 {
  position: relative;
  padding-left: 16px;
  font-size: 20px;
  color: #2f241d;
}

.modal-header h3::before {
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

.close-btn {
  width: 36px;
  height: 36px;
  background: rgba(157, 41, 41, 0.06);
  color: #9d2929;
}

.close-btn:hover {
  background: rgba(157, 41, 41, 0.12);
  color: #7a1d1d;
}

.service-panel {
  background:
    radial-gradient(circle at top right, rgba(201, 145, 63, 0.08), transparent 20%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(248, 244, 238, 0.86));
}

.panel-empty {
  color: #8f7b69;
}

.message-list {
  padding: 24px;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.3), transparent 30%),
    rgba(247, 241, 233, 0.78);
}

.message-row {
  margin-bottom: 18px;
}

.message-avatar {
  width: 38px;
  height: 38px;
  border: 2px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 18px rgba(74, 46, 23, 0.08);
}

.message-bubble {
  max-width: min(68%, 560px);
  padding: 14px 16px 12px;
  border-radius: 20px;
  border: 1px solid rgba(217, 207, 193, 0.76);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 240, 232, 0.96));
  box-shadow:
    0 14px 28px rgba(74, 46, 23, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.message-row.self .message-bubble {
  background:
    linear-gradient(135deg, #7a1d1d 0%, #9d2929 52%, #b33d2d 100%);
  border-color: rgba(157, 41, 41, 0.28);
  box-shadow: 0 18px 30px rgba(157, 41, 41, 0.18);
}

.message-content {
  color: #2f241d;
  line-height: 1.75;
}

.message-time {
  color: #8f7b69;
}

.message-row.self .message-content,
.message-row.self .message-time {
  color: rgba(255, 249, 243, 0.9);
}

.composer {
  padding: 18px 22px 20px;
  border-top: 1px solid rgba(217, 207, 193, 0.82);
  background: rgba(255, 251, 246, 0.82);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.composer input {
  min-height: 48px;
  border-radius: 18px;
  border-color: rgba(217, 207, 193, 0.9);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 240, 232, 0.96));
}

.composer input:focus {
  border-color: rgba(157, 41, 41, 0.42);
  box-shadow: 0 0 0 4px rgba(157, 41, 41, 0.1);
}

.composer button {
  min-height: 48px;
  border-radius: 18px;
  background: linear-gradient(135deg, #9d2929, #b33030);
  font-weight: 700;
  box-shadow: 0 14px 26px rgba(157, 41, 41, 0.18);
}

.composer button:hover:not(:disabled) {
  background: linear-gradient(135deg, #891f1f, #a72f2f);
}

@media (max-width: 768px) {
  .modal-content {
    border-radius: 0;
  }

  .modal-header {
    padding: 16px 18px;
  }

  .message-list,
  .composer {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
