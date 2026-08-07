<template>
  <div class="explore-mode">
    <!-- Conversation History Bar -->
    <div class="explore-history-bar">
      <button class="new-chat-btn" @click="$emit('new-conversation')">+ 新对话</button>
      <div v-if="loadingHistory" class="history-loading">加载中...</div>
      <div v-else-if="conversations.length === 0" class="history-empty">暂无历史对话</div>
      <div v-else class="history-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="history-item"
          :class="{ active: conversationId === conv.id }"
          @click="$emit('load-conversation', conv.id)"
        >
          <span class="history-title">{{ conv.title || '对话' }}</span>
          <button class="history-delete" @click.stop="$emit('delete-conversation', conv.id)">×</button>
        </div>
      </div>
    </div>

    <!-- Messages -->
    <MessageList
      :messages="messages"
      @navigate="(type, id) => $emit('navigate', type, id)"
    />

    <!-- Suggestions -->
    <div v-if="showSuggestions" class="suggestion-chips">
      <div class="suggestion-label">你可以这样问我：</div>
      <div class="suggestion-list">
        <div
          v-for="(suggestion, idx) in suggestions"
          :key="idx"
          class="suggestion-chip"
          @click="$emit('send', suggestion)"
        >
          {{ suggestion }}
        </div>
      </div>
    </div>

    <!-- Input -->
    <ChatInput v-model="inputText" @send="$emit('send', inputText); inputText = ''" />
  </div>
</template>

<script>
import MessageList from '../MessageList.vue'
import ChatInput from '../ChatInput.vue'

export default {
  name: 'ExploreMode',
  components: { MessageList, ChatInput },
  props: {
    messages: { type: Array, default: () => [] },
    conversations: { type: Array, default: () => [] },
    conversationId: { type: [Number, String], default: null },
    loadingHistory: { type: Boolean, default: false },
    showSuggestions: { type: Boolean, default: true },
    suggestions: {
      type: Array,
      default: () => [
        '帮我推荐北京的非遗活动',
        '我想了解苏州刺绣相关的内容',
        '帮我规划一个3天的非遗之旅'
      ]
    }
  },
  emits: ['send', 'load-conversation', 'new-conversation', 'delete-conversation', 'navigate'],
  data() {
    return { inputText: '' }
  }
}
</script>

<style scoped>
.explore-mode { display: flex; flex-direction: column; flex: 1; overflow: hidden; }

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

.new-chat-btn:hover { background: #6B3410; }

.history-loading, .history-empty { text-align: center; color: #999; font-size: 12px; padding: 4px 0; }

.history-list { display: flex; flex-direction: column; gap: 4px; }

.history-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 8px; border-radius: 6px; cursor: pointer;
  font-size: 12px; color: #555; background: white;
  border: 1px solid #e0d5c5; transition: all 0.2s;
}

.history-item:hover { background: #f0e8dc; }

.history-item.active { background: #8B4513; color: white; border-color: #8B4513; }

.history-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.history-delete {
  background: none; border: none; color: #999; cursor: pointer;
  font-size: 16px; padding: 0 4px; line-height: 1;
}

.history-item.active .history-delete { color: #ddd; }
.history-delete:hover { color: #e74c3c; }

.suggestion-chips { padding: 0 0 15px 0; border-top: 1px solid #f0f0f0; }
.suggestion-label { font-size: 12px; color: #999; margin-bottom: 10px; }
.suggestion-list { display: flex; flex-direction: column; gap: 8px; }

.suggestion-chip {
  display: inline-block; padding: 10px 14px;
  background: #faf5ef; border: 1px solid #e0d5c5;
  border-radius: 12px; font-size: 13px; color: #8B4513;
  cursor: pointer; transition: all 0.2s ease; line-height: 1.4;
}

.suggestion-chip:hover { background: #f0e8dc; border-color: #8B4513; transform: translateX(4px); }
.suggestion-chip:active { transform: translateX(4px) scale(0.98); }
</style>
