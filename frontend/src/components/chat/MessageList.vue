<template>
  <div class="message-list" ref="messageList">
    <template v-for="(message, index) in messages" :key="index">
      <div v-if="message.isDivider" class="service-divider">
        <div class="divider-line"></div>
        <span class="divider-text">{{ message.text }}</span>
        <div class="divider-line"></div>
      </div>
      <MessageBubble
        v-else
        :message="message"
        @navigate="(type, id) => $emit('navigate', type, id)"
      />
    </template>
  </div>
</template>

<script>
import MessageBubble from './MessageBubble.vue'

export default {
  name: 'MessageList',
  components: { MessageBubble },
  props: {
    messages: { type: Array, default: () => [] }
  },
  emits: ['navigate'],
  methods: {
    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.messageList
        if (el) el.scrollTop = el.scrollHeight
      })
    }
  },
  watch: {
    messages: {
      handler() { this.scrollToBottom() },
      deep: true
    }
  },
  mounted() { this.scrollToBottom() }
}
</script>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 20px;
  max-height: calc(100vh - 200px);
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.message-list::-webkit-scrollbar { display: none; }

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
</style>
