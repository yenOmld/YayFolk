<template>
  <div class="notification-container">
    <div 
      v-for="notification in notifications" 
      :key="notification.id"
      :class="['notification', notification.type]"
    >
      <span class="notification-content">{{ notification.message }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Notification',
  data() {
    return {
      notifications: [],
      nextId: 1
    }
  },
  methods: {
    show(message, type = 'info') {
      const id = this.nextId++
      const notification = {
        id,
        message,
        type,
        index: this.notifications.length
      }
      
      this.notifications.push(notification)
      
      // 3.5秒后开始淡出，4秒后移除
      setTimeout(() => {
        this.remove(id)
      }, 2500)
    },
    
    success(message) {
      this.show(message, 'success')
    },
    
    error(message) {
      this.show(message, 'error')
    },
    
    warning(message) {
      this.show(message, 'warning')
    },
    
    info(message) {
      this.show(message, 'info')
    },
    
    remove(id) {
      const index = this.notifications.findIndex(n => n.id === id)
      if (index !== -1) {
        this.notifications.splice(index, 1)
        // 更新剩余通知的索引
        this.notifications.forEach((n, i) => {
          n.index = i
        })
      }
    }
  }
}
</script>

<style scoped>
.notification-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification {
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: slideIn 0.5s ease-out forwards, fadeOut 0.5s ease-in 1.5s forwards;
  max-width: 300px;
  word-wrap: break-word;
}

.notification-content {
  font-size: 14px;
  font-weight: 500;
}

.notification.success {
  background-color: #f0f9eb;
  color: #67c23a;
  border-left: 4px solid #67c23a;
}

.notification.error {
  background-color: #fef0f0;
  color: #f56c6c;
  border-left: 4px solid #f56c6c;
}

.notification.warning {
  background-color: #fdf6ec;
  color: #e6a23c;
  border-left: 4px solid #e6a23c;
}

.notification.info {
  background-color: #f4f4f5;
  color: #909399;
  border-left: 4px solid #909399;
}

.notification-container {
  top: 18px;
  right: 18px;
  gap: 12px;
}

.notification {
  position: relative;
  min-width: 280px;
  max-width: min(360px, calc(100vw - 32px));
  padding: 14px 18px;
  border-radius: 18px;
  border-left-width: 0;
  border: 1px solid rgba(190, 157, 124, 0.24);
  box-shadow:
    0 20px 38px rgba(74, 46, 23, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.notification::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 4px;
  border-radius: 999px;
}

.notification-content {
  display: block;
  line-height: 1.6;
  color: inherit;
}

.notification.success {
  background:
    linear-gradient(135deg, rgba(53, 120, 83, 0.12), rgba(255, 255, 255, 0.96)),
    rgba(255, 251, 246, 0.96);
  color: #2f5b3f;
}

.notification.success::before {
  background: linear-gradient(180deg, #5c8e5a, #7dad62);
}

.notification.error {
  background:
    linear-gradient(135deg, rgba(157, 41, 41, 0.12), rgba(255, 255, 255, 0.96)),
    rgba(255, 251, 246, 0.96);
  color: #8d2222;
}

.notification.error::before {
  background: linear-gradient(180deg, #9d2929, #cc4b4b);
}

.notification.warning {
  background:
    linear-gradient(135deg, rgba(201, 145, 63, 0.16), rgba(255, 255, 255, 0.96)),
    rgba(255, 251, 246, 0.96);
  color: #94621f;
}

.notification.warning::before {
  background: linear-gradient(180deg, #c9913f, #e5ae57);
}

.notification.info {
  background:
    linear-gradient(135deg, rgba(111, 124, 141, 0.12), rgba(255, 255, 255, 0.96)),
    rgba(255, 251, 246, 0.96);
  color: #5f6670;
}

.notification.info::before {
  background: linear-gradient(180deg, #7b8794, #a0acb9);
}

@media (max-width: 640px) {
  .notification-container {
    top: 12px;
    right: 12px;
    left: 12px;
  }

  .notification {
    min-width: 0;
    max-width: none;
  }
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes fadeOut {
  from {
    opacity: 1;
    transform: translateX(0);
  }
  to {
    opacity: 0;
    transform: translateX(100%);
  }
}
</style>
