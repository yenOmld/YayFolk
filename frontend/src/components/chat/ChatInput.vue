<template>
  <div class="input-area">
    <input
      type="text"
      class="chat-input"
      :placeholder="placeholder"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup.enter="$emit('send')"
    >
    <button
      class="send-btn"
      @click="$emit('send')"
      :disabled="!modelValue.trim()"
    >
      {{ sendLabel }}
    </button>
  </div>
</template>

<script>
export default {
  name: 'ChatInput',
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '请输入您的问题...' },
    sendLabel: { type: String, default: '发送' }
  },
  emits: ['update:modelValue', 'send']
}
</script>

<style scoped>
.input-area {
  display: flex;
  border-top: 1px solid #e0e0e0;
  padding-top: 15px;
}

.chat-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 24px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.chat-input:focus { border-color: #8B4513; }

.send-btn {
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

.send-btn:hover { background: #a0522d; }
.send-btn:disabled { background: #ccc; cursor: not-allowed; }
</style>
