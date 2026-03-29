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
                <strong>{{ conversation.name || conversation.otherUserName || '用户' }}</strong>
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
              <p>@{{ currentConversation.otherUsername || 'unknown' }}</p>
            </div>
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
            <div v-if="messages.length === 0" class="panel-empty">当前会话还没有消息</div>
          </div>

          <div class="composer">
            <textarea
              v-model="draft"
              placeholder="输入回复内容，按 Ctrl + Enter 发送"
              @keydown.ctrl.enter.prevent="sendReply"
            ></textarea>
            <div class="composer-actions">
              <button class="primary-btn" @click="sendReply" :disabled="sending || !draft.trim()">发送回复</button>
            </div>
          </div>
        </template>
        <div v-else class="panel-empty">从左侧选择一条客服会话开始处理</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onMounted, ref } from 'vue'
import { getConversations, getMessages, markAsRead, sendMessage } from '../../api/app'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=travelate-user'
const keyword = ref('')
const loadingList = ref(false)
const sending = ref(false)
const conversations = ref([])
const currentConversation = ref(null)
const messages = ref([])
const draft = ref('')
const messagesContainer = ref(null)

const filteredConversations = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  const serviceConversations = conversations.value.filter(item => item.type === 'service')
  if (!text) {
    return serviceConversations
  }
  return serviceConversations.filter(item => {
    const fields = [item.name, item.otherUserName, item.otherUsername, item.lastMessage]
    return fields.some(field => String(field || '').toLowerCase().includes(text))
  })
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

    const serviceConversations = conversations.value.filter(item => item.type === 'service')
    if (!serviceConversations.length) {
      currentConversation.value = null
      messages.value = []
      return
    }

    const currentId = currentConversation.value?.id
    const targetConversation = serviceConversations.find(item => item.id === currentId) || serviceConversations[0]
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
  } catch (error) {
    notify.error(error.message || '发送失败')
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  loadConversations()
})
</script>

<style scoped>
.service-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 20px;
  min-height: calc(100vh - 220px);
}

.service-sidebar,
.service-panel {
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.toolbar-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.search-box {
  flex: 1;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
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

.service-head strong {
  color: #fff7ef;
  font-size: 15px;
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
}

.panel-header {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.panel-header h3 {
  margin: 0;
  font-size: 22px;
}

.panel-header p {
  margin: 6px 0 0;
  color: rgba(247, 244, 238, 0.62);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
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

.composer {
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
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

@media (max-width: 1100px) {
  .service-layout {
    grid-template-columns: 1fr;
  }

  .service-sidebar {
    min-height: 280px;
  }
}
</style>
