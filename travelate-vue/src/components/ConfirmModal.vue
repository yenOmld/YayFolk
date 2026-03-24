<template>
  <div v-if="visible" class="confirm-modal-overlay" @click.self="handleCancel">
    <div class="confirm-modal">
      <div class="confirm-header">
        <h3 class="confirm-title">{{ title }}</h3>
      </div>
      <div class="confirm-body">
        <p class="confirm-message">{{ message }}</p>
      </div>
      <div class="confirm-footer">
        <button class="btn-cancel" @click="handleCancel">
          {{ cancelText }}
        </button>
        <button class="btn-confirm" @click="handleConfirm">
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ConfirmModal',
  data() {
    return {
      visible: false,
      title: '',
      message: '',
      onConfirm: null,
      onCancel: null,
      confirmText: '确定',
      cancelText: '取消'
    }
  },
  methods: {
    show(options) {
      this.title = options.title || '确认操作'
      this.message = options.message || '您确定要执行此操作吗？'
      this.confirmText = options.confirmText || '确定'
      this.cancelText = options.cancelText || '取消'
      this.onConfirm = options.onConfirm || null
      this.onCancel = options.onCancel || null
      this.visible = true
    },
    
    handleConfirm() {
      this.visible = false
      if (this.onConfirm) {
        this.onConfirm()
      }
    },
    
    handleCancel() {
      this.visible = false
      if (this.onCancel) {
        this.onCancel()
      }
    }
  }
}
</script>

<style scoped>
.confirm-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease-out;
}

.confirm-modal {
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  padding: 30px;
  max-width: 400px;
  width: 90%;
  animation: slideUp 0.3s ease-out;
}

.confirm-header {
  margin-bottom: 20px;
}

.confirm-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  text-align: center;
}

.confirm-body {
  margin-bottom: 30px;
}

.confirm-message {
  margin: 0;
  font-size: 16px;
  line-height: 1.6;
  color: #86868b;
  text-align: center;
}

.confirm-footer {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-cancel,
.btn-confirm {
  padding: 12px 30px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 100px;
}

.btn-cancel {
  background: #f5f5f7;
  color: #1d1d1f;
  border: none;
}

.btn-cancel:hover {
  background: #e5e5ea;
  transform: translateY(-1px);
}

.btn-confirm {
  background: linear-gradient(135deg, #7494ec, #6381d9);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(116, 148, 236, 0.3);
}

.btn-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(116, 148, 236, 0.4);
}

.btn-confirm:active {
  transform: translateY(0);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
</style>
