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
