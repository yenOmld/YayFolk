<template>
  <div>
    <!-- 悬浮小人 -->
    <div 
      v-if="!isLoginPage"
      id="floating-doll" 
      class="floating-doll"
      @mousedown="dragStart"
      @touchstart="dragStart"
    >
      <img src="../assets/醒狮.png" alt="醒狮" class="lion-image">
    </div>

    <!-- AI对话侧边栏 -->
    <div v-if="!isLoginPage" class="ai-sidebar" :class="{ open: sidebarOpen }">
      <div class="ai-sidebar-header">
        <h3>智能助手 - Yaya</h3>
        <button id="close-sidebar" @click="closeSidebar">&times;</button>
      </div>

      <!-- 模式切换 -->
      <div class="ai-sidebar-modes">
        <button 
          class="mode-btn" 
          :class="{ active: currentMode === 'knowledge' }"
          @click="switchMode('knowledge')"
        >
          知识问答
        </button>
        <button 
          class="mode-btn" 
          :class="{ active: currentMode === 'explore' }"
          @click="switchMode('explore')"
        >
          探索资源
        </button>
        <button 
          class="mode-btn" 
          :class="{ active: currentMode === 'service' }"
          @click="switchMode('service')"
        >
          AI客服
        </button>
      </div>

      <div v-if="currentMode === 'knowledge'" class="explore-history-bar">
        <button class="new-chat-btn" @click="newKnowledgeChat">+ 新对话</button>
        <div v-if="loadingKnowledgeHistory" class="history-loading">加载中...</div>
        <div v-else-if="knowledgeConversations.length === 0" class="history-empty">暂无历史对话</div>
        <div v-else class="history-list">
          <div
            v-for="conv in knowledgeConversations"
            :key="conv.id"
            class="history-item"
            :class="{ active: knowledgeConversationId === conv.id }"
            @click="loadKnowledgeHistory(conv.id)"
          >
            <span class="history-title">{{ conv.title || '对话' }}</span>
            <button class="history-delete" @click.stop="handleDeleteKnowledgeConversation(conv.id)">×</button>
          </div>
        </div>
      </div>

      <div v-if="currentMode === 'explore'" class="explore-history-bar">
        <button class="new-chat-btn" @click="newExploreChat">+ 新对话</button>
        <div v-if="loadingExploreHistory" class="history-loading">加载中...</div>
        <div v-else-if="exploreConversations.length === 0" class="history-empty">暂无历史对话</div>
        <div v-else class="history-list">
          <div
            v-for="conv in exploreConversations"
            :key="conv.id"
            class="history-item"
            :class="{ active: exploreConversationId === conv.id }"
            @click="loadExploreHistory(conv.id)"
          >
            <span class="history-title">{{ conv.title || '对话' }}</span>
            <button class="history-delete" @click.stop="handleDeleteExploreConversation(conv.id)">×</button>
          </div>
        </div>
      </div>

      <div v-if="currentMode === 'service'" class="explore-history-bar">
        <div class="service-status" v-if="serviceConversationId">
          <span class="status-label">当前模式：</span>
          <span class="status-badge" :class="serviceMode === 'ai' ? 'ai-mode' : 'human-mode'">
            {{ serviceMode === 'ai' ? 'AI客服' : '人工客服' }}
          </span>
        </div>
      </div>

      <!-- 对话区域 -->
      <div class="ai-sidebar-content">
        <div class="message-list" ref="messageList">
          <template v-for="(message, index) in messagesWithDividers" :key="index">
            <div v-if="message.isDivider" class="service-divider">
              <div class="divider-line"></div>
              <span class="divider-text">{{ message.text }}</span>
              <div class="divider-line"></div>
            </div>
            <div
              v-else
              class="message-container"
              :class="message.type === 'user' ? 'user-message-container' : 'bot-message-container'"
            >
            <div 
              class="message"
              :class="message.type === 'user' ? 'user-message' : 'bot-message'"
            >
              {{ message.content }}
              <!-- 资源卡片 -->
              <div v-if="message.resources" class="resource-cards">
                <!-- 行程规划 -->
                <div v-if="message.resources.intent === 'ITINERARY_PLANNING'" class="resource-section">
                  <h4>行程规划</h4>
                  <div class="itinerary-card">
                    <div class="itinerary-text" style="white-space: pre-line;">{{ message.resources.itinerary }}</div>
                  </div>
                  <div v-if="message.resources.activities && message.resources.activities.length > 0" style="margin-top: 8px;">
                    <h4>相关活动</h4>
                    <div class="card-list">
                      <div 
                        v-for="(activity, idx) in message.resources.activities" 
                        :key="idx"
                        class="resource-card"
                        @click="navigateToResource('activity', activity.id)"
                      >
                        <h5>{{ activity.title }}</h5>
                        <p>{{ activity.location }}</p>
                        <p>{{ activity.startTime }} · {{ activity.price === 0 ? '免费' : activity.price + '元' }}</p>
                      </div>
                    </div>
                  </div>
                  <div v-if="message.resources.heritages && message.resources.heritages.length > 0" style="margin-top: 8px;">
                    <h4>相关非遗项目</h4>
                    <div class="card-list">
                      <div 
                        v-for="(heritage, idx) in message.resources.heritages" 
                        :key="idx"
                        class="resource-card"
                        @click="navigateToResource('heritage', heritage.id)"
                      >
                        <h5>{{ heritage.name }}</h5>
                        <p>{{ heritage.category }} · {{ heritage.region }}</p>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 结构化查询 -->
                <div v-if="message.resources.intent === 'STRUCTURED_QUERY'" class="resource-section">
                  <div v-if="message.resources.activities && message.resources.activities.length > 0">
                    <h4>为您找到 {{ message.resources.total }} 个活动</h4>
                    <div class="card-list">
                      <div 
                        v-for="(activity, idx) in message.resources.activities" 
                        :key="idx"
                        class="resource-card"
                        @click="navigateToResource('activity', activity.id)"
                      >
                        <h5>{{ activity.title }}</h5>
                        <p v-if="activity.subtitle">{{ activity.subtitle }}</p>
                        <p>{{ activity.location }}</p>
                        <p>{{ activity.startTime }} · {{ activity.price === 0 ? '免费' : activity.price + '元' }}</p>
                      </div>
                    </div>
                  </div>
                  <div v-else>
                    <p>暂无符合条件的活动，试试其他条件吧~</p>
                  </div>
                </div>
                <!-- 知识问答 -->
                <div v-if="message.resources.intent === 'KNOWLEDGE_QA'" class="resource-section">
                  <div v-if="message.resources.posts && message.resources.posts.length > 0">
                    <h4>相关帖子</h4>
                    <div class="card-list">
                      <div 
                        v-for="(post, idx) in message.resources.posts" 
                        :key="idx"
                        class="resource-card"
                        @click="navigateToResource('post', post.id)"
                      >
                        <h5>{{ post.title }}</h5>
                        <p>{{ post.createTime }}</p>
                      </div>
                    </div>
                  </div>
                  <div v-if="message.resources.heritages && message.resources.heritages.length > 0" style="margin-top: 8px;">
                    <h4>相关非遗项目</h4>
                    <div class="card-list">
                      <div 
                        v-for="(heritage, idx) in message.resources.heritages" 
                        :key="idx"
                        class="resource-card"
                        @click="navigateToResource('heritage', heritage.id)"
                      >
                        <h5>{{ heritage.name }}</h5>
                        <p>{{ heritage.category }} · {{ heritage.region }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          </template>
        </div>

        <div class="input-area">
          <input
            type="text"
            id="user-input"
            v-model="userInput"
            placeholder="请输入您的问题..."
            @keyup.enter="sendMessage"
          >
          <button 
            id="send-btn"
            @click="sendMessage"
            :disabled="!userInput.trim()"
          >
            发送
          </button>
        </div>
      </div>
    </div>

    <!-- 遮罩层 -->
    <div 
      class="overlay" 
      :class="{ active: sidebarOpen }"
      @click="closeSidebar"
    ></div>

    <!-- 删除确认弹窗 -->
    <ConfirmModal ref="deleteConfirmModal" />

    <!-- 帖子详情弹窗 -->
    <PostDetailModal 
      :visible="showPostModal" 
      :post="selectedPost" 
      @close="showPostModal = false"
      @update="handlePostUpdate"
    />

    <!-- 活动详情弹窗 -->
    <ActivityDetailModal 
      :visible="showActivityModal" 
      :activity-id="selectedActivityId" 
      @close="showActivityModal = false"
    />

    <!-- 非遗详情弹窗 -->
    <HeritageDetailModal 
      :visible="showHeritageModal" 
      :heritage="selectedHeritage" 
      @close="showHeritageModal = false"
    />
  </div>
</template>

<script>
import { useRoute } from 'vue-router'
import { exploreResources, getExploreConversations, getExploreMessages, deleteExploreConversation, getDiscoverPostDetail, getPublicHeritageDetail, getKnowledgeConversations, createKnowledgeConversation, getKnowledgeMessages, sendKnowledgeMessage, deleteKnowledgeConversation, createCustomerServiceConversation, getMessages, sendMessage, getServiceMode, closeHumanService } from '../api/app'
import ConfirmModal from './ConfirmModal.vue'
import PostDetailModal from './PostDetailModal.vue'
import ActivityDetailModal from './ActivityDetailModal.vue'
import HeritageDetailModal from './HeritageDetailModal.vue'

export default {
  name: 'FloatingDoll',
  components: {
    ConfirmModal,
    PostDetailModal,
    ActivityDetailModal,
    HeritageDetailModal
  },
  setup() {
    const route = useRoute()
    return {
      route
    }
  },
  data() {
    return {
      sidebarOpen: false,
      currentMode: 'knowledge',
      userInput: '',
      messages: [
        {
          type: 'bot',
          content: '你好！我是智能助手Yaya，很高兴为你解答关于中国传统非物质文化遗产的问题。'
        }
      ],
      isDragging: false,
      hasMoved: false,
      currentX: 0,
      currentY: 0,
      initialX: 0,
      initialY: 0,
      xOffset: 0,
      yOffset: 0,
      exploreConversationId: null,
      exploreConversations: [],
      loadingExploreHistory: false,
      knowledgeConversationId: null,
      knowledgeConversations: [],
      loadingKnowledgeHistory: false,
      showPostModal: false,
      selectedPost: null,
      showActivityModal: false,
      selectedActivityId: null,
      showHeritageModal: false,
      selectedHeritage: null,
      // 知识问答模式相关数据
      sending: false,
      currentKnowledgeConversation: null,
      // 客服模式相关数据
      serviceConversationId: null,
      serviceMode: 'ai',
      servicePolling: null,
      uiText: {
        title: '知识问答',
        loadingList: '正在加载会话...',
        emptyList: '暂无知识会话',
        emptyMessages: '暂无消息记录',
        inputPlaceholder: '输入消息...',
        send: '发送',
        loadMessagesFailed: '加载消息失败',
        createConversationFailed: '创建知识会话失败',
        loadConversationsFailed: '加载会话失败',
        sendFailed: '发送失败',
        aiThinking: 'AI正在思考...'
      }
    }
  },
  computed: {
    messagesWithDividers() {
      if (this.currentMode !== 'service') {
        return this.messages.map(msg => ({ ...msg, isDivider: false }))
      }

      const result = []
      let lastServiceSource = null

      for (const msg of this.messages) {
        const currentSource = msg.source || null

        if (currentSource && lastServiceSource && currentSource !== lastServiceSource &&
            !(currentSource === 'user' || lastServiceSource === 'user')) {
          if (lastServiceSource === 'ai' && currentSource === 'admin') {
            result.push({ isDivider: true, text: '人工客服已接入' })
          } else if (lastServiceSource === 'admin' && currentSource === 'ai') {
            result.push({ isDivider: true, text: '已切换回AI客服' })
          }
        }

        if (currentSource && currentSource !== 'user') {
          lastServiceSource = currentSource
        }

        result.push({ ...msg, isDivider: false })
      }

      return result
    },
    isLoginPage() {
      return this.route.path === '/login' || this.route.path === '/register'
    }
  },
  mounted() {
    // 添加全局事件监听器
    document.addEventListener('mousemove', this.drag);
    document.addEventListener('mouseup', this.dragEnd);
    document.addEventListener('touchmove', this.drag);
    document.addEventListener('touchend', this.dragEnd);
  },
  beforeUnmount() {
    // 移除全局事件监听器
    document.removeEventListener('mousemove', this.drag);
    document.removeEventListener('mouseup', this.dragEnd);
    document.removeEventListener('touchmove', this.drag);
    document.removeEventListener('touchend', this.dragEnd);
    // 停止轮询
    this.stopServicePolling();
  },
  methods: {
    toggleSidebar() {
      // 只有当没有移动时才打开侧边栏
      if (!this.hasMoved) {
        this.sidebarOpen = !this.sidebarOpen;
      }
    },
    closeSidebar() {
      this.sidebarOpen = false;
    },
    switchMode(mode) {
      this.currentMode = mode;
      this.messages = [
        {
          type: 'bot',
          content: this.getWelcomeMessage(mode)
        }
      ];
      
      if (mode === 'knowledge') {
        this.knowledgeConversationId = null;
        this.loadKnowledgeConversations();
      } else if (mode === 'explore') {
        this.exploreConversationId = null;
        this.loadExploreConversations();
      } else if (mode === 'service') {
        this.serviceConversationId = null;
        this.serviceMode = 'ai';
        this.createServiceConversation();
      }
    },
    loadKnowledgeConversations() {
      this.loadingKnowledgeHistory = true;
      getKnowledgeConversations()
        .then(response => {
          if (response.code === 200) {
            this.knowledgeConversations = response.data || [];
          }
        })
        .catch(() => {})
        .finally(() => {
          this.loadingKnowledgeHistory = false;
        });
    },
    loadKnowledgeHistory(conversationId) {
      getKnowledgeMessages(conversationId)
        .then(response => {
          if (response.code === 200) {
            this.knowledgeConversationId = conversationId;
            this.messages = (response.data || []).map(msg => {
              if (msg.isSelf) {
                return { type: 'user', content: msg.content };
              } else {
                return {
                  type: 'bot',
                  content: msg.content
                };
              }
            });
            this.scrollToBottom();
          }
        })
        .catch(() => {});
    },
    newKnowledgeChat() {
      this.knowledgeConversationId = null;
      this.currentKnowledgeConversation = null;
      this.messages = [
        {
          type: 'bot',
          content: '你好！我是智能助手Yaya，很高兴为你解答关于中国传统非物质文化遗产的问题。'
        }
      ];
    },
    handleDeleteKnowledgeConversation(conversationId) {
      this.$refs.deleteConfirmModal.show({
        title: '删除对话',
        message: '确定要删除这条对话记录吗？此操作不可恢复。',
        confirmText: '删除',
        cancelText: '取消',
        onConfirm: () => {
          deleteKnowledgeConversation(conversationId)
            .then(response => {
              if (response.code === 200) {
                this.loadKnowledgeConversations();
                if (this.knowledgeConversationId === conversationId) {
                  this.newKnowledgeChat();
                }
              }
            })
            .catch(() => {});
        }
      });
    },
    loadExploreConversations() {
      this.loadingExploreHistory = true;
      getExploreConversations()
        .then(response => {
          if (response.code === 200) {
            this.exploreConversations = response.data || [];
          }
        })
        .catch(() => {})
        .finally(() => {
          this.loadingExploreHistory = false;
        });
    },
    loadExploreHistory(conversationId) {
      getExploreMessages(conversationId)
        .then(response => {
          if (response.code === 200) {
            this.exploreConversationId = conversationId;
            this.messages = (response.data || []).map(msg => {
              if (msg.role === 'user') {
                return { type: 'user', content: msg.content };
              } else {
                return {
                  type: 'bot',
                  content: msg.content,
                  resources: msg.resources || null
                };
              }
            });
            this.scrollToBottom();
          }
        })
        .catch(() => {});
    },
    newExploreChat() {
      this.exploreConversationId = null;
      this.messages = [
        {
          type: 'bot',
          content: '你好！我是非遗探索助手，可以帮你搜索活动、规划行程、回答非遗相关问题。试试问我吧！'
        }
      ];
    },
    handleDeleteExploreConversation(conversationId) {
      this.$refs.deleteConfirmModal.show({
        title: '删除对话',
        message: '确定要删除这条对话记录吗？此操作不可恢复。',
        confirmText: '删除',
        cancelText: '取消',
        onConfirm: () => {
          deleteExploreConversation(conversationId)
            .then(response => {
              if (response.code === 200) {
                this.loadExploreConversations();
                if (this.exploreConversationId === conversationId) {
                  this.newExploreChat();
                }
              }
            })
            .catch(() => {});
        }
      });
    },
    // 创建客服会话
    createServiceConversation() {
      createCustomerServiceConversation()
        .then(response => {
          if (response.code === 200) {
            this.serviceConversationId = response.data.id;
            this.loadServiceMessages(response.data.id);
            this.getServiceModeStatus(response.data.id);
          }
        })
        .catch(error => {
          console.error('创建客服会话失败:', error);
          this.messages.push({
            type: 'bot',
            content: '抱歉，创建客服会话失败，请稍后再试。'
          });
          this.scrollToBottom();
        });
    },
    // 加载客服消息
    loadServiceMessages(conversationId) {
      getMessages(conversationId)
        .then(response => {
          if (response.code === 200) {
            this.messages = (response.data || []).map(msg => {
              if (msg.isSelf) {
                return { type: 'user', content: msg.content, source: 'user' };
              } else {
                return {
                  type: 'bot',
                  content: msg.content,
                  source: msg.source || 'ai'
                };
              }
            });
            this.scrollToBottom();
          }
        })
        .catch(error => {
          console.error('加载客服消息失败:', error);
        });
    },
    // 获取客服模式状态
    getServiceModeStatus(conversationId) {
      getServiceMode(conversationId)
        .then(response => {
          if (response.code === 200) {
            this.serviceMode = response.data.serviceMode || 'ai';
          }
        })
        .catch(error => {
          console.error('获取客服模式失败:', error);
        });
    },
    getWelcomeMessage(mode) {
      switch (mode) {
        case 'knowledge':
          return '你好！我是智能助手Yaya，很高兴为你解答关于中国传统非物质文化遗产的问题。';
        case 'explore':
          return '欢迎探索站内资源！我可以帮你了解更多关于中国非遗的内容。';
        case 'service':
          return '你好！我是AI客服助手Yaya，有什么可以帮助你的吗？';
        default:
          return '你好！我是智能助手Yaya，很高兴为你服务。';
      }
    },
    sendMessage() {
      if (!this.userInput.trim()) return;

      // 清空输入框
      const messageContent = this.userInput.trim();
      this.userInput = '';

      // 根据模式处理消息
      if (this.currentMode === 'knowledge') {
        this.sendMessageKnowledgeMode(messageContent);
      } else if (this.currentMode === 'explore') {
        this.sendMessageExploreMode(messageContent);
      } else if (this.currentMode === 'service') {
        this.sendMessageServiceMode(messageContent);
      } else {
        // 其他模式使用模拟回复
        this.messages.push({
          type: 'user',
          content: messageContent
        });
        // 滚动到底部
        this.scrollToBottom();
        this.sendMockResponse(messageContent);
      }
    },
    // 知识问答模式发送消息
    sendMessageKnowledgeMode(messageContent) {
      if (this.sending) {
        return;
      }

      const tempMessageId = Date.now();
      const tempMessage = {
        id: tempMessageId,
        type: 'user',
        content: messageContent,
        isSending: true
      };
      this.messages.push(tempMessage);
      this.scrollToBottom();

      this.sending = true;
      try {
        const thinkingMessage = {
          id: Date.now(),
          type: 'bot',
          content: this.uiText.aiThinking,
          isThinking: true
        };
        this.messages.push(thinkingMessage);
        this.scrollToBottom();

        if (!this.knowledgeConversationId) {
          createKnowledgeConversation({ userInput: messageContent })
            .then(response => {
              if (response.code === 200) {
                this.knowledgeConversationId = response.data.id;
                this.loadKnowledgeConversations();
                this.sendKnowledgeMessageRequest(response.data.id, messageContent, tempMessageId);
              } else {
                throw new Error(response.message || this.uiText.createConversationFailed);
              }
            })
            .catch(error => {
              console.error('创建知识会话失败:', error);
              const thinkingIndex = this.messages.findIndex(msg => msg.isThinking);
              if (thinkingIndex !== -1) {
                this.messages[thinkingIndex] = {
                  type: 'bot',
                  content: '抱歉，创建会话失败，请稍后再试。'
                };
              }
              this.sending = false;
            });
        } else {
          this.sendKnowledgeMessageRequest(this.knowledgeConversationId, messageContent, tempMessageId);
        }
      } catch (error) {
        this.sending = false;
        console.error('发送消息错误:', error);
      }
    },
    sendKnowledgeMessageRequest(conversationId, messageContent, tempMessageId) {
      const user = this.readStoredUser();
      const token = user?.token || localStorage.getItem('token');

      let aiMessageContent = '';
      const aiMessageId = Date.now();

      const eventSource = new EventSource(`/api/knowledge/conversations/${conversationId}/stream?content=${encodeURIComponent(messageContent)}&token=${encodeURIComponent(token)}`);

      eventSource.onmessage = (event) => {
        const data = event.data;

        if (data === '[DONE]' || data === '') {
          eventSource.close();

          const tempIndex = this.messages.findIndex(msg => msg.id === tempMessageId);
          if (tempIndex !== -1) {
            this.messages[tempIndex].isSending = false;
          }

          // 流结束时才将 isThinking 设为 false
          const thinkingIndex = this.messages.findIndex(msg => msg.isThinking);
          if (thinkingIndex !== -1) {
            this.messages[thinkingIndex].isThinking = false;
          }

          const convIndex = this.knowledgeConversations.findIndex(c => c.id === conversationId);
          if (convIndex !== -1) {
            this.knowledgeConversations[convIndex].lastMessage = messageContent;
            this.knowledgeConversations[convIndex].lastMessageTime = new Date().toISOString();
          }

          this.sending = false;
          this.scrollToBottom();
          return;
        }

        if (data && data.trim() !== '') {
          aiMessageContent += data;

          // 始终使用 isThinking 属性查找 AI 消息，直到流结束才设为 false
          const thinkingIndex = this.messages.findIndex(msg => msg.isThinking);
          if (thinkingIndex !== -1) {
            this.messages[thinkingIndex].content = aiMessageContent;
          }
          this.scrollToBottom();
        }
      };

      eventSource.onerror = (error) => {
        console.error('SSE connection error:', error);

        if (eventSource.readyState === 2) {
          eventSource.close();
          return;
        }

        eventSource.close();

        const tempIndex = this.messages.findIndex(msg => msg.id === tempMessageId);
        if (tempIndex !== -1) {
          this.messages[tempIndex].isSending = false;
          this.messages[tempIndex].isFailed = true;
        }

        const thinkingIndex = this.messages.findIndex(msg => msg.isThinking);
        if (thinkingIndex !== -1) {
          if (aiMessageContent) {
            this.messages[thinkingIndex].content = aiMessageContent;
            this.messages[thinkingIndex].isThinking = false;
          } else {
            this.messages[thinkingIndex] = {
              type: 'bot',
              content: '抱歉，AI暂时无法回复，请稍后再试。'
            };
          }
        }

        this.sending = false;
        this.scrollToBottom();
      };

      setTimeout(() => {
        if (eventSource.readyState !== EventSource.CLOSED) {
          eventSource.close();
          this.sending = false;
        }
      }, 60000);
    },
    readStoredUser() {
      try {
        const raw = localStorage.getItem('user') || localStorage.getItem('userInfo');
        return raw ? JSON.parse(raw) : {};
      } catch {
        return {};
      }
    },
    // 探索资源模式发送消息
    sendMessageExploreMode(messageContent) {
      // 显示用户输入的消息
      this.messages.push({
        type: 'user',
        content: messageContent
      });
      // 滚动到底部
      this.scrollToBottom();
      
      // 显示加载状态
      this.messages.push({
        type: 'bot',
        content: '正在搜索相关资源...',
        isLoading: true
      });
      // 滚动到底部
      this.scrollToBottom();
      
      // 调用 exploreResources API
      exploreResources({ userInput: messageContent, conversationId: this.exploreConversationId })
        .then(response => {
          if (response.code === 200) {
            if (response.data.conversationId) {
              this.exploreConversationId = response.data.conversationId;
            }

            const loadingIndex = this.messages.findIndex(msg => msg.isLoading);
            if (loadingIndex !== -1) {
              this.messages.splice(loadingIndex, 1);
            }
            
            this.messages.push({
              type: 'bot',
              content: this.buildResourceMessage(response.data),
              resources: response.data
            });
          } else {
            // 显示错误消息
            const loadingIndex = this.messages.findIndex(msg => msg.isLoading);
            if (loadingIndex !== -1) {
              this.messages[loadingIndex] = {
                type: 'bot',
                content: `获取资源失败：${response.message || '未知错误'}`
              };
            }
          }
        })
        .catch(error => {
          // 显示错误消息
          const loadingIndex = this.messages.findIndex(msg => msg.isLoading);
          if (loadingIndex !== -1) {
            this.messages[loadingIndex] = {
              type: 'bot',
              content: `获取资源失败：${error.message || '网络错误'}`
            };
          }
        })
        .finally(() => {
          // 滚动到底部
          this.scrollToBottom();
        });
    },
    // 构建资源消息
    buildResourceMessage(data) {
      if (!data) return '暂无相关资源';
      
      if (data.intent === 'ITINERARY_PLANNING') {
        return `为您规划了${data.days}天的${data.destination}非遗之旅，详见下方行程卡片`;
      } else if (data.intent === 'STRUCTURED_QUERY') {
        if (data.activities && data.activities.length > 0) {
          return `为您找到 ${data.total} 个相关活动，详见下方卡片`;
        }
        return '暂无符合条件的活动，试试其他条件吧~';
      } else if (data.intent === 'KNOWLEDGE_QA') {
        if (data.answer) {
          return data.answer;
        }
        return '为您找到以下相关内容，详见下方卡片';
      }
      
      return '暂无相关资源';
    },
    // 客服模式发送消息
    sendMessageServiceMode(messageContent) {
      if (this.sending) {
        return;
      }

      // 显示用户消息
      this.messages.push({
        type: 'user',
        content: messageContent
      });
      this.scrollToBottom();

      this.sending = true;

      // 显示加载状态
      this.messages.push({
        type: 'bot',
        content: '正在处理您的消息...',
        isLoading: true
      });
      this.scrollToBottom();

      if (!this.serviceConversationId) {
        this.createServiceConversation();
        this.sending = false;
        return;
      }

      // 发送消息
      sendMessage(this.serviceConversationId, { content: messageContent })
        .then(response => {
          if (response.code === 200) {
            // 清除加载状态
            const loadingIndex = this.messages.findIndex(msg => msg.isLoading);
            if (loadingIndex !== -1) {
              this.messages.splice(loadingIndex, 1);
            }

            // 等待 AI 回复或人工回复
            setTimeout(() => {
              this.loadServiceMessages(this.serviceConversationId);
              this.getServiceModeStatus(this.serviceConversationId);
            }, 500);

            // 启动轮询（如果是人工模式）
            this.startServicePolling();
          } else {
            const loadingIndex = this.messages.findIndex(msg => msg.isLoading);
            if (loadingIndex !== -1) {
              this.messages[loadingIndex] = {
                type: 'bot',
                content: `发送失败：${response.message || '未知错误'}`
              };
            }
          }
        })
        .catch(error => {
          const loadingIndex = this.messages.findIndex(msg => msg.isLoading);
          if (loadingIndex !== -1) {
            this.messages[loadingIndex] = {
              type: 'bot',
              content: `发送失败：${error.message || '网络错误'}`
            };
          }
        })
        .finally(() => {
          this.sending = false;
          this.scrollToBottom();
        });
    },
    // 启动客服消息轮询
    startServicePolling() {
      // 清除之前的轮询
      if (this.servicePolling) {
        clearInterval(this.servicePolling);
      }

      // 仅在人工模式下轮询
      if (this.serviceMode === 'human') {
        this.servicePolling = setInterval(() => {
          this.loadServiceMessages(this.serviceConversationId);
        }, 3000); // 每3秒轮询一次
      }
    },
    // 停止客服消息轮询
    stopServicePolling() {
      if (this.servicePolling) {
        clearInterval(this.servicePolling);
        this.servicePolling = null;
      }
    },
    // 发送模拟回复
    sendMockResponse(messageContent) {
      setTimeout(() => {
        let response = '';
        switch (this.currentMode) {
          case 'explore':
            response = '这是一个关于探索站内资源的回复。';
            break;
          case 'service':
            response = '这是一个AI客服的回复。';
            break;
          default:
            response = '这是一个默认回复。';
        }
        this.messages.push({
          type: 'bot',
          content: response
        });
        // 滚动到底部
        this.scrollToBottom();
      }, 1000);
    },
    scrollToBottom() {
      setTimeout(() => {
        const messageList = this.$refs.messageList;
        if (messageList) {
          messageList.scrollTop = messageList.scrollHeight;
        }
      }, 100);
    },
    // AI辅助生成：豆包，2026-04-22
    dragStart(e) {
      e.preventDefault();
      
      if (e.type === 'touchstart') {
        this.initialX = e.touches[0].clientX;
        this.initialY = e.touches[0].clientY;
      } else {
        this.initialX = e.clientX;
        this.initialY = e.clientY;
      }

      this.isDragging = true;
    },
    drag(e) {
      if (this.isDragging) {
        e.preventDefault();

        if (e.type === 'touchmove') {
          this.currentX = e.touches[0].clientX - this.initialX + this.xOffset;
          this.currentY = e.touches[0].clientY - this.initialY + this.yOffset;
        } else {
          this.currentX = e.clientX - this.initialX + this.xOffset;
          this.currentY = e.clientY - this.initialY + this.yOffset;
        }

        this.setTranslate(this.currentX, this.currentY);
      }
    },
    dragEnd(e) {
      // 计算移动距离
      const dx = this.currentX - this.xOffset;
      const dy = this.currentY - this.yOffset;
      const distance = Math.sqrt(dx * dx + dy * dy);
      
      if (this.isDragging) {
        if (distance < 5) {
          // 移动距离小于5px，视为点击
          this.animateDoll();
          this.toggleSidebar();
        } else {
          // 移动距离大于等于5px，视为拖拽，更新偏移量
          this.xOffset = this.currentX;
          this.yOffset = this.currentY;
        }
      }
      
      this.isDragging = false;
      this.hasMoved = false;
    },
    setTranslate(xPos, yPos) {
      const floatingDoll = document.getElementById('floating-doll');
      if (floatingDoll) {
        floatingDoll.style.transform = `translate3d(${xPos}px, ${yPos}px, 0)`;
      }
    },
    animateDoll() {
      const floatingDoll = document.getElementById('floating-doll');
      if (floatingDoll) {
        floatingDoll.classList.remove('active', 'blink');
        
        setTimeout(() => {
          floatingDoll.classList.add('active', 'blink');
        }, 10);
        
        setTimeout(() => {
          floatingDoll.classList.remove('active', 'blink');
        }, 600);
      }
    },
    // 导航到资源详情页
    navigateToResource(type, id) {
      switch (type) {
        case 'activity':
          this.selectedActivityId = id;
          this.showActivityModal = true;
          break;
        case 'post':
          getDiscoverPostDetail(id)
            .then(response => {
              if (response.code === 200 && response.data) {
                this.selectedPost = this.formatPostData(response.data);
                this.showPostModal = true;
              }
            })
            .catch(err => {
              console.error('获取帖子详情失败:', err);
            });
          break;
        case 'heritage':
          getPublicHeritageDetail(id)
            .then(response => {
              if (response.code === 200 && response.data) {
                this.selectedHeritage = response.data;
                this.showHeritageModal = true;
              }
            })
            .catch(err => {
              console.error('获取非遗详情失败:', err);
            });
          break;
        default:
          console.warn('未知资源类型:', type);
      }
    },
    formatPostData(data) {
      let images = [];
      if (Array.isArray(data.images)) {
        images = data.images;
      } else if (typeof data.images === 'string') {
        images = data.images.split(',').filter(Boolean);
      }
      let hashtags = [];
      if (Array.isArray(data.tags)) {
        hashtags = data.tags;
      } else if (typeof data.tags === 'string') {
        try {
          hashtags = JSON.parse(data.tags).filter(Boolean);
        } catch {
          hashtags = [];
        }
      }
      return {
        id: data.id,
        title: data.title,
        content: data.content,
        images: images,
        time: data.createTime ? new Date(data.createTime).toLocaleString('zh-CN') : '',
        author: {
          id: data.userId,
          name: data.authorName || data.username || '匿名用户',
          avatar: data.authorAvatar || 'https://yayfolk.bhyy.online/avatars/default.png',
          location: data.authorLocation || '未知'
        },
        hashtags: hashtags,
        comments: data.commentCount || 0,
        commentList: data.comments || [],
        collects: data.collectCount || 0,
        bookmarked: data.bookmarked || false,
        sourceLang: data.sourceLang || '',
        auditStatus: data.auditStatus || '',
        highlightCommentId: data.highlightCommentId || null
      };
    },
    handlePostUpdate(updatedPost) {
      this.selectedPost = updatedPost;
    }
  }
}
</script>

<style scoped>
.floating-doll {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 150px;
  height: 150px;
  cursor: pointer;
  z-index: 9999;
  user-select: none;
}

.lion-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.2s ease;
}

.floating-doll:hover .lion-image {
  transform: scale(1.1);
}

.floating-doll.active .lion-image {
  animation: pawWave 0.5s ease-in-out;
  transform-origin: 30% 85%;
}

@keyframes pawWave {
  0%, 100% { 
    transform: rotate(0deg) translateX(0);
  }
  30% { 
    transform: rotate(-6deg) translateX(-3px);
  }
  70% { 
    transform: rotate(6deg) translateX(3px);
  }
}

.floating-doll.blink .lion-image {
  animation: eyeBlink 0.35s ease-in-out;
}

@keyframes eyeBlink {
  0%, 100% { 
    filter: brightness(1) contrast(1);
  }
  45%, 55% { 
    filter: brightness(0.6) contrast(1.4);
  }
}

.floating-doll:active {
  cursor: grabbing;
}

/* AI对话侧边栏样式 */
.ai-sidebar {
  position: fixed;
  top: 0;
  right: -400px;
  width: 400px;
  height: 100vh;
  background: white;
  box-shadow: -5px 0 15px rgba(0, 0, 0, 0.1);
  z-index: 10000;
  transition: right 0.3s ease;
  display: flex;
  flex-direction: column;
}

.ai-sidebar.open {
  right: 0;
}

.ai-sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  background: #f8f9fa;
}

.ai-sidebar-header h3 {
  color: #8B4513;
  font-size: 18px;
  margin: 0;
}

#close-sidebar {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s;
}

#close-sidebar:hover {
  background: #e0e0e0;
}

/* 模式切换 */
.ai-sidebar-modes {
  display: flex;
  border-bottom: 1px solid #e0e0e0;
  background: #f8f9fa;
}

.mode-btn {
  flex: 1;
  padding: 12px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}

.mode-btn:hover {
  background: #e9ecef;
}

.mode-btn.active {
  color: #8B4513;
  border-bottom-color: #8B4513;
  font-weight: 500;
}

/* 对话区域 */
.ai-sidebar-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow: hidden;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 20px;
  max-height: calc(100vh - 200px);
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.message-list::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Opera */
}

.message-container {
  margin-bottom: 15px;
  display: flex;
  width: 100%;
}

.user-message-container {
  justify-content: flex-end;
}

.bot-message-container {
  justify-content: flex-start;
}

.message {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.4;
  display: block;
}

.user-message {
  background: #e3f2fd;
  color: #1976d2;
  border-bottom-right-radius: 4px;
}

.bot-message {
  background: #f1f0f0;
  color: #333;
  border-bottom-left-radius: 4px;
}

.input-area {
  display: flex;
  border-top: 1px solid #e0e0e0;
  padding-top: 15px;
}

#user-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 24px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

#user-input:focus {
  border-color: #8B4513;
}

#send-btn {
  margin-left: 10px;
  padding: 12px 20px;
  background: #8B4513;
  color: white;
  border: none;
  border-radius: 24px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

#send-btn:hover {
  background: #a0522d;
}

#send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 遮罩层 */
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9998;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
}

.overlay.active {
  opacity: 1;
  visibility: visible;
}

/* 资源卡片样式 */
.resource-cards {
  margin-top: 15px;
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
}

.resource-section {
  margin-bottom: 20px;
}

.resource-section h4 {
  font-size: 16px;
  color: #8B4513;
  margin-bottom: 10px;
  font-weight: 500;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.resource-card {
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.resource-card:hover {
  background: #e9ecef;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.resource-card h5 {
  font-size: 14px;
  color: #333;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.resource-card p {
  font-size: 12px;
  color: #666;
  margin: 4px 0;
  line-height: 1.3;
}

/* 行程规划卡片样式 */
.itinerary-card {
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
}

.itinerary-day {
  margin-bottom: 15px;
}

.itinerary-day:last-child {
  margin-bottom: 0;
}

.itinerary-day h5 {
  font-size: 14px;
  color: #8B4513;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.itinerary-activities {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.itinerary-activity {
  font-size: 12px;
  color: #333;
  padding: 8px;
  background: white;
  border-radius: 4px;
  border-left: 3px solid #8B4513;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.itinerary-time {
  font-size: 10px;
  color: #666;
  margin-left: 10px;
}

.itinerary-heritages {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.itinerary-heritage {
  font-size: 12px;
  color: #333;
  padding: 8px;
  background: white;
  border-radius: 4px;
  border-left: 3px solid #4CAF50;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.itinerary-category {
  font-size: 10px;
  color: #666;
  margin-left: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-sidebar {
    width: 100%;
    right: -100%;
  }
  
  .floating-doll {
    width: 120px;
    height: 120px;
    bottom: 20px;
    right: 20px;
  }
  
  .resource-card {
    padding: 10px;
  }
  
  .resource-card h5 {
    font-size: 13px;
  }
  
  .resource-card p {
    font-size: 11px;
  }
}

.explore-history-bar {
  padding: 8px 12px;
  background: #faf5ef;
  border-bottom: 1px solid #e0d5c5;
  max-height: 120px;
  overflow-y: auto;
}

.new-chat-btn {
  width: 100%;
  padding: 6px 10px;
  background: #8B4513;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  margin-bottom: 6px;
}

.new-chat-btn:hover {
  background: #6B3410;
}

.history-loading,
.history-empty {
  text-align: center;
  color: #999;
  font-size: 12px;
  padding: 4px 0;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  color: #555;
  background: white;
  border: 1px solid #e0d5c5;
  transition: all 0.2s;
}

.history-item:hover {
  background: #f0e8dc;
}

.history-item.active {
  background: #8B4513;
  color: white;
  border-color: #8B4513;
}

.history-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-delete {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 16px;
  padding: 0 4px;
  line-height: 1;
}

.history-item.active .history-delete {
  color: #ddd;
}

.history-delete:hover {
  color: #e74c3c;
}

.service-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}

.status-label {
  font-size: 12px;
  color: #666;
}

.status-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.ai-mode {
  background: #e3f2fd;
  color: #1976d2;
}

.human-mode {
  background: #e8f5e8;
  color: #388e3c;
}

.service-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 8px;
}

.service-divider .divider-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 0, 0, 0.12), transparent);
}

.service-divider .divider-text {
  font-size: 11px;
  color: #999;
  white-space: nowrap;
  padding: 2px 10px;
  border-radius: 10px;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
}

.knowledge-answer {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.itinerary-text {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}
</style>