<template>
  <div>
    <!-- 悬浮小人 -->
    <div 
      id="floating-doll" 
      class="floating-doll"
      @mousedown="dragStart"
      @touchstart="dragStart"
    >
      <img src="../assets/醒狮.png" alt="醒狮" class="lion-image">
    </div>

    <!-- AI对话侧边栏 -->
    <div class="ai-sidebar" :class="{ open: sidebarOpen }">
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

      <!-- 对话区域 -->
      <div class="ai-sidebar-content">
        <div class="message-list" ref="messageList">
          <div 
            v-for="(message, index) in messages" 
            :key="index"
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
import {
  createCustomerServiceConversation,
  getConversations,
  getMessages,
  markAsRead,
  sendMessage,
  exploreResources,
  getExploreConversations,
  getExploreMessages,
  deleteExploreConversation,
  getDiscoverPostDetail
} from '../api/app'
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
      showPostModal: false,
      selectedPost: null,
      showActivityModal: false,
      selectedActivityId: null,
      showHeritageModal: false,
      selectedHeritage: null,
      // 知识问答模式相关数据
      loadingList: false,
      sending: false,
      conversations: [],
      currentConversation: null,
      uiText: {
        title: '在线客服',
        loadingList: '正在加载会话...',
        emptyList: '暂无客服会话',
        emptyMessages: '暂无消息记录',
        inputPlaceholder: '输入消息...',
        send: '发送',
        loadMessagesFailed: '加载消息失败',
        createConversationFailed: '创建客服会话失败',
        loadConversationsFailed: '加载会话失败',
        sendFailed: '发送失败',
        aiThinking: 'AI客服正在思考...'
      }
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
        this.initKnowledgeMode();
      } else if (mode === 'explore') {
        this.exploreConversationId = null;
        this.loadExploreConversations();
      }
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
    // 读取存储的用户信息
    readStoredUser() {
      try {
        const raw = localStorage.getItem('user') || localStorage.getItem('userInfo');
        return raw ? JSON.parse(raw) : {};
      } catch {
        return {};
      }
    },
    // 加载会话消息
    loadMessagesForConversation(conversationId) {
      return new Promise((resolve, reject) => {
        getMessages(conversationId)
          .then(response => {
            if (response.code !== 200) {
              throw new Error(response.message || this.uiText.loadMessagesFailed);
            }
            this.messages = Array.isArray(response.data) ? response.data.map(msg => ({
              type: msg.isSelf ? 'user' : 'bot',
              content: msg.content,
              time: msg.time
            })) : [];
            this.scrollToBottom();
            resolve();
          })
          .catch(error => {
            console.error('加载消息失败:', error);
            reject(error);
          });
      });
    },
    // 选择会话
    selectConversation(conversation) {
      this.currentConversation = conversation;
      return this.loadMessagesForConversation(conversation.id)
        .then(() => {
          if (conversation.unreadCount > 0) {
            return markAsRead(conversation.id)
              .then(() => {
                conversation.unreadCount = 0;
              });
          }
        });
    },
    // 确保客服会话存在
    ensureCustomerServiceConversation() {
      return createCustomerServiceConversation()
        .then(response => {
          if (response.code !== 200) {
            throw new Error(response.message || this.uiText.createConversationFailed);
          }
          return response.data;
        });
    },
    // 加载会话列表
    loadConversationList() {
      return getConversations()
        .then(response => {
          if (response.code !== 200) {
            throw new Error(response.message || this.uiText.loadConversationsFailed);
          }
          this.conversations = (Array.isArray(response.data) ? response.data : []).filter(item => item.type === 'service');
        });
    },
    // 初始化知识问答模式
    initKnowledgeMode() {
      this.loadingList = true;
      this.currentConversation = null;
      this.messages = [];

      this.loadConversationList()
        .then(() => {
          if (this.conversations.length === 0) {
            return this.ensureCustomerServiceConversation()
              .then(created => {
                return this.loadConversationList()
                  .then(() => {
                    const target = this.conversations.find(item => String(item.id) === String(created.id)) || this.conversations[0];
                    if (target) {
                      return this.selectConversation(target);
                    }
                  });
              });
          } else {
            const target = this.conversations[0];
            if (target) {
              return this.selectConversation(target);
            }
          }
        })
        .catch(error => {
          console.error('初始化知识问答模式失败:', error);
          this.messages = [
            {
              type: 'bot',
              content: '抱歉，初始化知识问答模式失败，请稍后再试。'
            }
          ];
        })
        .finally(() => {
          this.loadingList = false;
          this.scrollToBottom();
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
      if (!this.currentConversation || this.sending) {
        return;
      }

      // 生成临时消息ID
      const tempMessageId = Date.now();
      // 创建临时消息对象
      const tempMessage = {
        id: tempMessageId,
        type: 'user',
        content: messageContent,
        time: new Date().toLocaleString('zh-CN'),
        isSending: true
      };
      // 立即添加到消息列表
      this.messages.push(tempMessage);
      // 滚动到底部
      this.scrollToBottom();
      // 更新会话信息
      this.currentConversation.lastMessage = messageContent;
      this.currentConversation.lastMessageTime = tempMessage.time;

      this.sending = true;
      try {
        // 立即显示AI客服正在思考的提示（不等待后端响应）
        const thinkingMessage = {
          id: Date.now(),
          type: 'bot',
          content: this.uiText.aiThinking,
          time: new Date().toLocaleString('zh-CN'),
          isThinking: true
        };
        this.messages.push(thinkingMessage);
        this.scrollToBottom();
        
        // 并行处理：同时保存用户消息和建立SSE连接
        Promise.all([
          // 保存用户消息到后端
          sendMessage(this.currentConversation.id, {
            content: messageContent
          }),
          // 立即开始流式AI回复（不等待消息保存完成）
          this.streamAIResponse(this.currentConversation.id, messageContent)
        ])
        .then(([response]) => {
          if (response.code !== 200) {
            throw new Error(response.message || this.uiText.sendFailed);
          }
          
          // 替换临时消息为实际消息
          const index = this.messages.findIndex(msg => msg.id === tempMessageId);
          if (index !== -1) {
            this.messages.splice(index, 1, {
              type: 'user',
              content: response.data.content,
              time: response.data.time
            });
          }
        })
        .catch(error => {
          // 发送失败，更新临时消息状态
          const index = this.messages.findIndex(msg => msg.id === tempMessageId);
          if (index !== -1) {
            this.messages[index].isSending = false;
            this.messages[index].isFailed = true;
            this.messages[index].content = `发送失败: ${messageContent}`;
          }
          console.error('发送消息失败:', error);
          
          // 显示错误信息
          const thinkingIndex = this.messages.findIndex(msg => msg.isThinking);
          if (thinkingIndex !== -1) {
            this.messages[thinkingIndex] = {
              type: 'bot',
              content: '抱歉，AI客服暂时无法回复，请稍后再试。',
              time: new Date().toLocaleString('zh-CN')
            };
          }
        })
        .finally(() => {
          this.sending = false;
          this.scrollToBottom();
        });
      } catch (error) {
        this.sending = false;
        console.error('发送消息错误:', error);
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
    // 流式AI回复
    streamAIResponse(conversationId, messageContent) {
      return new Promise((resolve, reject) => {
        try {
          // 获取token
          const user = this.readStoredUser();
          const token = user?.token || localStorage.getItem('token');
          
          // 使用思考消息的ID，确保消息一致性
          const thinkingIndex = this.messages.findIndex(msg => msg.isThinking);
          const aiMessageId = thinkingIndex !== -1 ? this.messages[thinkingIndex].id : Date.now();
          let aiMessageContent = '';
          
          // 使用EventSource处理SSE连接
          const eventSource = new EventSource(`/api/messages/conversations/${conversationId}/stream?content=${encodeURIComponent(messageContent)}&token=${encodeURIComponent(token)}`);
          
          // 监听消息事件
          eventSource.onmessage = (event) => {
            const rawData = event.data;
            console.log('Received SSE raw data:', rawData);
            
            // 直接使用原始数据，因为后端已经处理了SSE格式
            let data = rawData;
            
            // 检查是否为结束标记
            if (data === '[DONE]' || data === '') {
              console.log('SSE stream completed');
              eventSource.close();
              resolve();
              return;
            }
            
            if (data && data.trim() !== '') {
              // 更新AI消息内容
              aiMessageContent += data;
              console.log('Updated AI message content:', aiMessageContent);
              
              // 找到思考消息的索引
              const thinkingIndex = this.messages.findIndex(msg => msg.isThinking);
              if (thinkingIndex !== -1) {
                // 直接更新思考消息的内容，而不是替换整个对象
                this.messages[thinkingIndex].content = aiMessageContent;
                this.messages[thinkingIndex].isThinking = false;
              } else {
                // 如果思考消息不存在，检查是否已经有AI消息（使用相同的ID）
                const existingAIMessageIndex = this.messages.findIndex(msg => msg.id === aiMessageId && msg.type === 'bot');
                if (existingAIMessageIndex !== -1) {
                  // 更新现有AI消息的内容
                  this.messages[existingAIMessageIndex].content = aiMessageContent;
                }
              }
              this.scrollToBottom();
            }
          };
          
          // 监听打开事件
          eventSource.onopen = (event) => {
            console.log('SSE connection opened:', event);
          };
          
          // 监听错误事件
          eventSource.onerror = (error) => {
            console.error('SSE connection error:', error);
            console.error('EventSource readyState:', eventSource.readyState);
            
            // 忽略连接被服务器主动关闭的情况（readyState为2表示连接已关闭）
            if (eventSource.readyState === 2) {
              console.log('SSE connection closed normally');
              eventSource.close();
              resolve();
              return;
            }
            
            eventSource.close();
            
            // 显示错误信息
            const thinkingIndex = this.messages.findIndex(msg => msg.isThinking);
            if (thinkingIndex !== -1) {
              this.messages[thinkingIndex] = {
                type: 'bot',
                content: '抱歉，AI客服暂时无法回复，请稍后再试。',
                time: new Date().toLocaleString('zh-CN')
              };
            }
            this.scrollToBottom();
            reject(error);
          };
          
          // 监听完成事件
          eventSource.addEventListener('done', () => {
            console.log('Stream completed');
            eventSource.close();
            resolve();
          });
          
          // 设置超时，防止连接一直保持
          setTimeout(() => {
            if (eventSource.readyState !== EventSource.CLOSED) {
              console.log('SSE timeout, closing connection');
              eventSource.close();
              resolve();
            }
          }, 60000); // 60秒超时
          
        } catch (error) {
          console.error('Stream AI response error:', error);
          reject(error);
        }
      });
    },
    scrollToBottom() {
      setTimeout(() => {
        const messageList = this.$refs.messageList;
        if (messageList) {
          messageList.scrollTop = messageList.scrollHeight;
        }
      }, 100);
    },
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
          this.selectedHeritage = { id };
          this.showHeritageModal = true;
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
          avatar: data.authorAvatar || '/src/assets/default-avatar.png',
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