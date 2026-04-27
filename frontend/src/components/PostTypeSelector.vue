<template>
  <TransitionGroup name="selector" tag="div" class="post-type-selector-overlay">
    <div class="selector-container" :key="'container'" v-if="visible">
      <!-- Three selection buttons -->
      <div class="options-wrapper">
        <button class="option-btn share-btn" type="button" @click="selectType('share')">
          <div class="option-inner">
            <i class="bx bx-edit-alt"></i>
            <span class="option-text">分享</span>
          </div>
        </button>

        <button class="option-btn review-btn" type="button" @click="selectType('review')">
          <div class="option-inner">
            <i class="bx bx-star"></i>
            <span class="option-text">评价</span>
          </div>
        </button>

        <button class="option-btn ai-btn" type="button" @click="selectType('ai-heritage')">
          <div class="option-inner">
            <i class="bx bx-movie"></i>
            <span class="option-text">AI大片</span>
          </div>
        </button>
      </div>
    </div>
  </TransitionGroup>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select'])

const selectType = (type) => {
  emit('select', type)
}
</script>

<style scoped>
.post-type-selector-overlay {
  position: absolute;
  left: 50%;
  bottom: 100%;
  transform: translateX(-50%);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.selector-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

/* 选项容器 */
.options-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 252, 247, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 20px;
  box-shadow: 
    0 8px 32px rgba(139, 109, 89, 0.18),
    0 2px 8px rgba(139, 109, 89, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(204, 175, 145, 0.4);
}

/* 选项按钮基础样式 */
.option-btn {
  position: relative;
  width: 72px;
  height: 72px;
  border: none;
  border-radius: 18px;
  padding: 0;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.option-btn:hover {
  transform: scale(1.1) translateY(-3px);
}

.option-btn:active {
  transform: scale(0.95);
}

/* 内部内容 */
.option-inner {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.option-inner i {
  font-size: 28px;
  color: #fff;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.15));
}

.option-text {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.3px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

/* 各类型按钮背景 - 导航栏红色系 */
.share-btn {
  background: linear-gradient(145deg, #b33d2d 0%, #9d2929 50%, #7a1d1d 100%);
}

.review-btn {
  background: linear-gradient(145deg, #c2410c 0%, #a63a10 50%, #8b3009 100%);
}

.ai-btn {
  background: linear-gradient(145deg, #9d2929 0%, #8b2018 50%, #7a1d1d 100%);
}

/* 进入动画 */
.selector-enter-active {
  transition: opacity 0.25s ease, transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.selector-enter-from {
  opacity: 0;
  transform: scale(0.8) translateY(10px);
}

.selector-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.selector-leave-to {
  opacity: 0;
  transform: scale(0.85) translateY(5px);
}

/* 响应式 */
@media (max-width: 400px) {
  .option-btn {
    width: 64px;
    height: 64px;
    border-radius: 16px;
  }

  .option-inner i {
    font-size: 24px;
  }

  .option-text {
    font-size: 11px;
  }

  .options-wrapper {
    gap: 10px;
    padding: 10px 14px;
  }
}
</style>