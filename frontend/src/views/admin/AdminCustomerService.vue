<template>
  <div class="admin-page admin-service-page">
    <div class="page-header">
      <div>
        <p class="eyebrow">Customer Service</p>
        <h2>客服工作台</h2>
        <p>处理用户与商家的咨询消息，仅普通管理员可见。</p>
      </div>
    </div>

    <div class="service-layout">
      <section class="table-card service-sidebar">
        <div class="toolbar-row">
          <div class="search-box">
            <input
              v-model.trim="keyword"
              type="text"
              placeholder="搜索用户昵称、账号或最近消息"
            />
          </div>
          <button class="search-btn" @click="loadConversations" :disabled="loadingList">刷新</button>
        </div>

        <div v-if="loadingList" class="loading-card sidebar-state">正在加载客服会话...</div>
        <div v-else-if="filteredConversations.length === 0" class="loading-card sidebar-state">暂无客服咨询</div>
        <div v-else class="conversation-list">
          <button
            v-for="conversation in filteredConversations"
            :key="conversation.id"
            type="button"
            class="service-item"
            :class="{ active: currentConversation?.id === conversation.id }"
            @click="selectConversation(conversation)"
          >
            <img :src="conversation.avatar || defaultAvatar" alt="avatar" class="avatar" />
            <div class="service-copy">
              <div class="service-head">
                <div class="name-section">
                  <strong>{{ conversation.name || conversation.otherUserName || '用户' }}</strong>
                  <span v-if="conversation.serviceMode" class="mode-badge" :class="conversation.serviceMode === 'ai' ? 'ai-mode' : 'human-mode'">
                    {{ conversation.serviceMode === 'ai' ? 'AI' : '人工' }}
                  </span>
                </div>
                <span v-if="conversation.unreadCount" class="count-badge">{{ conversation.unreadCount }}</span>
              </div>
              <span class="sub">@{{ conversation.otherUsername || 'unknown' }}</span>
              <p>{{ conversation.lastMessage || '暂无消息' }}</p>
            </div>
          </button>
        </div>
      </section>

      <section class="table-card service-panel">
        <template v-if="currentConversation">
          <div class="panel-header">
            <div>
              <h3>{{ currentConversation.name || currentConversation.otherUserName || '用户' }}</h3>
              <div class="header-info">
                <p>@{{ currentConversation.otherUsername || 'unknown' }}</p>
                <span v-if="currentConversation.serviceMode" class="mode-badge" :class="currentConversation.serviceMode === 'ai' ? 'ai-mode' : 'human-mode'">
                  {{ currentConversation.serviceMode === 'ai' ? 'AI客服' : '人工客服' }}
                </span>
              </div>
            </div>
            <div class="header-actions">
              <button v-if="currentConversation.serviceMode === 'human'" class="secondary-btn" @click="closeHumanServiceHandler">
                结束人工服务
              </button>
            </div>
          </div>

          <div class="message-list" ref="messagesContainer">
            <template v-for="(item, idx) in messagesWithDividers" :key="idx">
              <div v-if="item.isDivider" class="service-divider">
                <div class="divider-line"></div>
                <span class="divider-text">{{ item.text }}</span>
                <div class="divider-line"></div>
              </div>
              <div
                v-else
                class="message-row"
                :class="{ 
                  self: item.isSelf, 
                  thinking: item.isThinking,
                  'ai-message': !item.isSelf && item.source === 'ai',
                  'human-message': !item.isSelf && item.source === 'admin',
                  'user-message': !item.isSelf && item.source === 'user'
                }"
              >
                <div class="message-bubble" :class="{ 
                  'thinking-bubble': item.isThinking,
                  'ai-bubble': !item.isSelf && item.source === 'ai',
                  'human-bubble': !item.isSelf && item.source === 'admin',
                  'user-bubble': !item.isSelf && item.source === 'user'
                }">
                  <div class="message-content">{{ item.content }}</div>
                  <div v-if="!item.isThinking" class="message-time">{{ item.time }}</div>
                </div>
              </div>
            </template>
            <div v-if="messages.length === 0" class="panel-empty">当前会话还没有消息</div>
          </div>

          <div class="composer">
            <textarea
              v-model="draft"
              placeholder="输入回复内容，按 Ctrl + Enter 发送"
              @keydown.ctrl.enter.prevent="sendReply"
            ></textarea>
            <div class="composer-actions">
              <button class="primary-btn" @click="sendReply" :disabled="sending || !draft.trim()">回复</button>
            </div>
          </div>
        </template>
        <div v-else class="panel-empty">从左侧选择一条客服会话开始处理</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { getConversations, getMessages, markAsRead, sendMessage, closeHumanService, getServiceMode } from '../../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const defaultAvatar = 'https://yayfolk.bhyy.online/avatars/default.png'
const keyword = ref('')
const loadingList = ref(false)
const sending = ref(false)
const conversations = ref([])
const pollingTimer = ref(null)
const currentConversation = ref(null)
const messages = ref([])
const draft = ref('')
const messagesContainer = ref(null)

const filteredConversations = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  const humanServiceConversations = conversations.value.filter(
    item => item.type === 'service' && item.serviceMode === 'human'
  )
  if (!text) {
    return humanServiceConversations
  }
  return humanServiceConversations.filter(item => {
    const fields = [item.name, item.otherUserName, item.otherUsername, item.lastMessage]
    return fields.some(field => String(field || '').toLowerCase().includes(text))
  })
})

const messagesWithDividers = computed(() => {
  const result = []
  let lastServiceSource = null

  for (const msg of messages.value) {
    let currentServiceSource = null
    if (msg.isSelf) {
      currentServiceSource = 'admin'
    } else if (msg.source === 'ai') {
      currentServiceSource = 'ai'
    } else if (msg.source === 'admin') {
      currentServiceSource = 'admin'
    }

    if (currentServiceSource && lastServiceSource && currentServiceSource !== lastServiceSource) {
      if (lastServiceSource === 'ai' && currentServiceSource === 'admin') {
        result.push({ isDivider: true, text: '人工客服已接管' })
      } else if (lastServiceSource === 'admin' && currentServiceSource === 'ai') {
        result.push({ isDivider: true, text: '已切换回AI客服' })
      }
    }

    if (currentServiceSource) {
      lastServiceSource = currentServiceSource
    }

    result.push({ ...msg, isDivider: false })
  }

  return result
})

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
    throw new Error(response.message || '加载消息失败')
  }
  messages.value = response.data || []
  scrollToBottom()
}

const selectConversation = async (conversation) => {
  currentConversation.value = conversation
  try {
    await loadMessagesForConversation(conversation.id)
    if (conversation.unreadCount > 0) {
      await markAsRead(conversation.id)
      conversation.unreadCount = 0
    }
  } catch (error) {
    notify.error(error.message || '加载消息失败')
  }
}

const loadConversations = async () => {
  loadingList.value = true
  try {
    const response = await getConversations()
    if (response.code !== 200) {
      throw new Error(response.message || '加载客服会话失败')
    }
    conversations.value = response.data || []

    const humanServiceConversations = conversations.value.filter(
      item => item.type === 'service' && item.serviceMode === 'human'
    )
    if (!humanServiceConversations.length) {
      currentConversation.value = null
      messages.value = []
      return
    }

    const currentId = currentConversation.value?.id
    const stillHuman = humanServiceConversations.find(item => item.id === currentId)
    const targetConversation = stillHuman || humanServiceConversations[0]
    await selectConversation(targetConversation)
  } catch (error) {
    notify.error(error.message || '加载客服会话失败')
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
      throw new Error(response.message || '发送失败')
    }
    messages.value.push(response.data)
    currentConversation.value.lastMessage = response.data.content
    currentConversation.value.lastMessageTime = response.data.time
    draft.value = ''
    scrollToBottom()
    
    // 重新加载消息列表，确保显示最新消息
    setTimeout(async () => {
      await loadMessagesForConversation(currentConversation.value.id)
    }, 500)
  } catch (error) {
    notify.error(error.message || '发送失败')
  } finally {
    sending.value = false
  }
}

const closeHumanServiceHandler = async () => {
  if (!currentConversation.value) return

  try {
    const response = await closeHumanService(currentConversation.value.id)
    if (response.code === 200) {
      notify.success('已结束人工服务，切换回智能客服')
      const closedId = currentConversation.value.id
      conversations.value = conversations.value.filter(c => c.id !== closedId)
      currentConversation.value = null
      messages.value = []
      const remaining = filteredConversations.value
      if (remaining.length > 0) {
        await selectConversation(remaining[0])
      }
    } else {
      throw new Error(response.message || '操作失败')
    }
  } catch (error) {
    notify.error(error.message || '操作失败')
  }
}

onMounted(() => {
  loadConversations()
  pollingTimer.value = setInterval(checkCurrentServiceMode, 8000)
})

onUnmounted(() => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
})

const checkCurrentServiceMode = async () => {
  if (!currentConversation.value) return
  try {
    const response = await getServiceMode(currentConversation.value.id)
    if (response.code === 200 && response.data && response.data.serviceMode !== 'human') {
      const removedId = currentConversation.value.id
      conversations.value = conversations.value.filter(c => c.id !== removedId)
      currentConversation.value = null
      messages.value = []
      const remaining = filteredConversations.value
      if (remaining.length > 0) {
        await selectConversation(remaining[0])
      }
    }
  } catch (e) {
    // ignore polling errors
  }
}
</script>

<style scoped>
.admin-service-page {
  --service-layout-height: clamp(640px, calc(100dvh - 180px), 940px);
}

.service-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 20px;
  height: var(--service-layout-height);
  min-height: var(--service-layout-height);
  overflow: hidden;
}

.service-sidebar,
.service-panel {
  display: flex;
  flex-direction: column;
  padding: 20px;
  min-height: 0;
  overflow: hidden;
}

.toolbar-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.search-box input {
  width: 100%;
  min-width: 0;
}

.search-box {
  flex: 3;
}

.search-btn {
  flex: 1;
  min-width: 80px;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: rgba(251, 216, 181, 0.55) transparent;
}

.conversation-list::-webkit-scrollbar {
  width: 10px;
}

.conversation-list::-webkit-scrollbar-track {
  background: transparent;
}

.conversation-list::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(251, 216, 181, 0.75), rgba(219, 176, 139, 0.66));
  border-radius: 999px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.conversation-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(251, 216, 181, 0.95), rgba(219, 176, 139, 0.86));
  border: 2px solid transparent;
  background-clip: padding-box;
}

.service-item {
  display: flex;
  gap: 14px;
  width: 100%;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.06);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.service-item:hover,
.service-item.active {
  transform: translateY(-1px);
  border-color: rgba(251, 216, 181, 0.2);
  background: rgba(255, 255, 255, 0.1);
}

.avatar {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  object-fit: cover;
  flex-shrink: 0;
}

.service-copy {
  min-width: 0;
  flex: 1;
}

.service-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.name-section {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.service-head strong {
  color: #fff7ef;
  font-size: 15px;
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mode-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  flex-shrink: 0;
}

.ai-mode {
  background: rgba(30, 136, 229, 0.2);
  color: #64b5f6;
  border: 1px solid rgba(30, 136, 229, 0.3);
}

.human-mode {
  background: rgba(76, 175, 80, 0.2);
  color: #81c784;
  border: 1px solid rgba(76, 175, 80, 0.3);
}

.service-copy .sub {
  display: block;
  margin-top: 4px;
  color: rgba(247, 244, 238, 0.6);
  font-size: 12px;
}

.service-copy p {
  margin: 8px 0 0;
  color: rgba(247, 244, 238, 0.8);
  font-size: 13px;
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-state,
.panel-empty {
  display: grid;
  place-items: center;
  min-height: 180px;
  color: rgba(247, 244, 238, 0.72);
  text-align: center;
  flex: 1;
}

.panel-header {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.panel-header h3 {
  margin: 0;
  font-size: 22px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
}

.header-info p {
  margin: 0;
  color: rgba(247, 244, 238, 0.62);
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.secondary-btn {
  padding: 8px 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  color: #fff7ef;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.secondary-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.3);
}

.secondary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 0;
  scrollbar-width: thin;
  scrollbar-color: rgba(251, 216, 181, 0.55) transparent;
}

.message-list::-webkit-scrollbar {
  width: 10px;
}

.message-list::-webkit-scrollbar-track {
  background: transparent;
}

.message-list::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(251, 216, 181, 0.75), rgba(219, 176, 139, 0.66));
  border-radius: 999px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.message-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(251, 216, 181, 0.95), rgba(219, 176, 139, 0.86));
  border: 2px solid transparent;
  background-clip: padding-box;
}

.message-row {
  display: flex;
  margin-bottom: 14px;
}

.message-row.self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(560px, 82%);
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.message-row.self .message-bubble {
  background: rgba(122, 35, 35, 0.72);
  border-color: rgba(255, 255, 255, 0.04);
}

.ai-bubble {
  background: rgba(30, 136, 229, 0.15);
  border-color: rgba(30, 136, 229, 0.2);
  color: #e3f2fd;
}

.human-bubble {
  background: rgba(76, 175, 80, 0.15);
  border-color: rgba(76, 175, 80, 0.2);
  color: #e8f5e8;
}

.user-bubble {
  background: rgba(255, 255, 255, 0.09);
  border-color: rgba(255, 255, 255, 0.08);
  color: #fff7ef;
}

.service-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
  padding: 0 8px;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(251, 216, 181, 0.3), transparent);
}

.divider-text {
  font-size: 12px;
  color: rgba(247, 244, 238, 0.5);
  white-space: nowrap;
  padding: 3px 12px;
  border-radius: 12px;
  background: rgba(251, 216, 181, 0.08);
  border: 1px solid rgba(251, 216, 181, 0.15);
}

.message-content {
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  margin-top: 8px;
  color: rgba(247, 244, 238, 0.52);
  font-size: 12px;
  text-align: right;
}

.message-row.thinking .message-bubble {
  background: rgba(100, 100, 100, 0.3);
  border-color: rgba(150, 150, 150, 0.3);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
  100% {
    opacity: 1;
  }
}

.composer {
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.composer textarea {
  width: 100%;
  min-height: 120px;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.primary-btn {
  background: linear-gradient(135deg, #ff7f35 0%, #ff5722 100%);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(255, 127, 53, 0.3);
}

.primary-btn:hover {
  background: linear-gradient(135deg, #ff5722 0%, #e64a19 100%);
  box-shadow: 0 4px 12px rgba(255, 127, 53, 0.4);
  transform: translateY(-1px);
}

.primary-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(255, 127, 53, 0.3);
}

.primary-btn:disabled {
  background: linear-gradient(135deg, #ffb74d 0%, #ff9800 100%);
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
  transform: none;
}

@media (max-width: 1100px) {
  .admin-service-page {
    --service-layout-height: clamp(560px, calc(100dvh - 150px), 840px);
  }

  .service-layout {
    grid-template-columns: 1fr;
    height: var(--service-layout-height);
    min-height: var(--service-layout-height);
  }

  .service-sidebar {
    min-height: 240px;
  }
}

@media (max-width: 768px) {
  .admin-service-page {
    --service-layout-height: calc(100dvh - 120px);
  }
}
</style>
