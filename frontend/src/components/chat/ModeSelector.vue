<template>
  <div class="ai-sidebar-modes">
    <button
      v-for="mode in modes"
      :key="mode.key"
      class="mode-btn"
      :class="{ active: currentMode === mode.key }"
      @click="$emit('change', mode.key)"
    >
      {{ mode.label }}
    </button>
  </div>
</template>

<script>
export default {
  name: 'ModeSelector',
  props: {
    currentMode: { type: String, required: true },
    modes: {
      type: Array,
      default: () => [
        { key: 'knowledge', label: '知识问答' },
        { key: 'explore', label: '探索资源' },
        { key: 'service', label: 'AI客服' }
      ]
    }
  },
  emits: ['change']
}
</script>

<style scoped>
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

.mode-btn:hover { background: #e9ecef; }

.mode-btn.active {
  color: #8B4513;
  border-bottom-color: #8B4513;
  font-weight: 500;
}
</style>
