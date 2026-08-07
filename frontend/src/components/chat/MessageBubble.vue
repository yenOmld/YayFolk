<template>
  <div class="message-container" :class="message.type === 'user' ? 'user-message-container' : 'bot-message-container'">
    <div class="message" :class="message.type === 'user' ? 'user-message' : 'bot-message'">
      {{ message.content }}
      <!-- Resource Cards -->
      <div v-if="message.resources" class="resource-cards">
        <ResourceCards :resources="message.resources" @navigate="(type, id) => $emit('navigate', type, id)" />
      </div>
    </div>
  </div>
</template>

<script>
import ResourceCards from './ResourceCards.vue'

export default {
  name: 'MessageBubble',
  components: { ResourceCards },
  props: {
    message: {
      type: Object,
      required: true
      // { type: 'user'|'bot', content: String, resources: Object }
    }
  },
  emits: ['navigate']
}
</script>

<style scoped>
.message-container {
  margin-bottom: 15px;
  display: flex;
  width: 100%;
}

.user-message-container { justify-content: flex-end; }
.bot-message-container { justify-content: flex-start; }

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

.resource-cards {
  margin-top: 15px;
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
}
</style>
