<template>
  <Teleport to="body">
    <div v-if="visible" class="heritage-modal-shell" @click="emit('close')">
      <div class="heritage-modal-card" role="dialog" aria-modal="true" aria-label="Heritage detail" @click.stop>
        <button class="close-button" type="button" @click="emit('close')">
          <i class="bx bx-x"></i>
        </button>

        <div v-if="!heritage" class="state-block">
          <i class="bx bx-book-open"></i>
          <p>非遗详情不可用。</p>
        </div>

        <div v-else>
          <div class="modal-header">
            <div class="modal-icon">
              <i :class="heritage.icon || 'bx bx-book'"></i>
            </div>
            <h2>{{ heritage.title || heritage.name || '未命名非遗' }}</h2>
          </div>

          <div class="modal-body">
            <p>{{ heritage.introduction || heritage.description || '暂无简介信息' }}</p>

            <div class="modal-details">
              <h3>基础信息</h3>
              <p>{{ heritageBaseInfo }}</p>

              <h3>历史渊源</h3>
              <p>{{ heritage.history || '暂无历史信息' }}</p>

              <h3>传承价值</h3>
              <p>{{ heritage.inheritanceValue || '暂无传承价值信息' }}</p>

              <h3>代表性传承人</h3>
              <p>{{ heritage.representativeInheritor || '暂无代表性传承人信息' }}</p>

              <h3>相关诗词</h3>
              <p>{{ formatList(heritage.relatedPoems, '暂无相关诗词信息') }}</p>

              <h3>相关节气</h3>
              <p>{{ formatList(heritage.relatedSolarTerms, '暂无相关节气信息') }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  heritage: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close'])

const heritageBaseInfo = computed(() => {
  const item = props.heritage || {}
  const parts = [
    item.category ? `分类：${item.category}` : '',
    item.subcategory ? `子类：${item.subcategory}` : '',
    item.region ? `地区：${item.region}` : '',
    item.level ? `级别：${item.level}` : '',
    item.dynasty ? `朝代：${item.dynasty}` : '',
    typeof item.viewCount === 'number' ? `浏览量：${item.viewCount}` : ''
  ].filter(Boolean)
  return parts.join(' / ') || '暂无基础信息'
})

const formatList = (value, fallback = '暂无相关信息') => {
  if (Array.isArray(value)) {
    const text = value.filter(Boolean).join('、')
    return text || fallback
  }
  if (typeof value === 'string') {
    return value.trim() || fallback
  }
  return fallback
}

const handleEscape = (event) => {
  if (event.key === 'Escape' && props.visible) {
    emit('close')
  }
}

const lockBody = (locked) => {
  document.body.style.overflow = locked ? 'hidden' : ''
}

watch(
  () => props.visible,
  (visible) => {
    lockBody(visible)
  }
)

document.addEventListener('keydown', handleEscape)

onBeforeUnmount(() => {
  lockBody(false)
  document.removeEventListener('keydown', handleEscape)
})
</script>

<style scoped>
.heritage-modal-shell {
  position: fixed;
  inset: 0;
  z-index: 10003;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.heritage-modal-card {
  position: relative;
  background: linear-gradient(135deg, #fff 0%, #fdf5e6 100%);
  border-radius: 20px;
  max-width: 700px;
  width: 100%;
  max-height: 85vh;
  overflow-y: auto;
  animation: slideUp 0.4s ease;
  box-shadow: 0 25px 80px rgba(79, 9, 21, 0.4);
}

.heritage-modal-card::-webkit-scrollbar {
  display: none;
}

.heritage-modal-card {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.close-button {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #4f0915;
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.3s ease;
  z-index: 10;
}

.close-button:hover {
  background: #6b101d;
  transform: rotate(90deg);
}

.modal-header {
  text-align: center;
  padding: 48px 32px 32px;
  background: linear-gradient(135deg, #4f0915 0%, #6b101d 100%);
  border-radius: 20px 20px 0 0;
}

.modal-icon {
  width: 100px;
  height: 100px;
  background: rgba(218, 165, 32, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
}

.modal-icon i {
  font-size: 48px;
  color: #daa520;
}

.modal-header h2 {
  font-size: 32px;
  color: #daa520;
  margin: 0;
  font-weight: 700;
}

.modal-body {
  padding: 32px;
}

.modal-body > p {
  font-size: 16px;
  color: #333;
  line-height: 1.8;
  margin-bottom: 32px;
  text-align: center;
}

.modal-details h3 {
  font-size: 20px;
  color: #4f0915;
  margin: 24px 0 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-details h3::before {
  content: '';
  width: 4px;
  height: 20px;
  background: #daa520;
  border-radius: 2px;
}

.modal-details p {
  font-size: 15px;
  color: #555;
  line-height: 1.8;
  margin: 0 0 16px 12px;
}

.state-block {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
}

.state-block i {
  font-size: 2rem;
  color: #b65c38;
}

@media (max-width: 768px) {
  .heritage-modal-card {
    max-width: 100%;
    border-radius: 12px;
  }

  .modal-header {
    padding: 36px 20px 24px;
  }

  .modal-body {
    padding: 24px;
  }

  .modal-header h2 {
    font-size: 24px;
  }
}
</style>
