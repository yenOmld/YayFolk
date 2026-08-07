<template>
  <div class="resource-card" @click="$emit('navigate', type, id)">
    <h5>{{ title }}</h5>
    <p v-if="subtitle">{{ subtitle }}</p>
    <p>{{ detail }}</p>
  </div>
</template>

<script>
export default {
  name: 'ResourceCard',
  props: {
    type: { type: String, required: true }, // 'activity' | 'heritage' | 'post'
    id: { type: [Number, String], required: true },
    title: { type: String, default: '' },
    subtitle: { type: String, default: '' },
    detail: { type: String, default: '' }
  },
  emits: ['navigate']
}
</script>

<style scoped>
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

.resource-card h5 { font-size: 14px; color: #333; margin: 0 0 8px 0; font-weight: 500; }
.resource-card p { font-size: 12px; color: #666; margin: 4px 0; line-height: 1.3; }
</style>
