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
  </div>
</template>

<script>
import {
  createCustomerServiceConversation,
  getConversations,
  getMessages,
  markAsRead,
  sendMessage
} from '../api/app'

export default {
  name: 'FloatingDoll',
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
      // 可以根据不同模式显示不同的欢迎信息
      this.messages = [
        {
          type: 'bot',
          content: this.getWelcomeMessage(mode)
        }
      ];
      
      // 如果切换到知识问答模式，初始化会话
      if (mode === 'knowledge') {
        this.initKnowledgeMode();
      }
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
  display: inline-block;
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
}
</style>