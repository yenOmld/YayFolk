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
  background-color: rgba(44, 22, 11, 0.45);
  backdrop-filter: blur(6px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease-out;
}

.confirm-modal {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(255, 251, 246, 0.96));
  border: 1px solid rgba(190, 157, 124, 0.28);
  border-radius: 24px;
  box-shadow:
    0 24px 56px rgba(74, 46, 23, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(16px);
  padding: 32px;
  max-width: 400px;
  width: 90%;
  animation: slideUp 0.3s ease-out;
}

.confirm-header {
  margin-bottom: 18px;
  text-align: center;
}

.confirm-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #3f220f;
  text-align: center;
}

.confirm-body {
  margin-bottom: 28px;
}

.confirm-message {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: #6b4a2c;
  text-align: center;
}

.confirm-footer {
  display: flex;
  gap: 14px;
  justify-content: center;
}

.btn-cancel,
.btn-confirm {
  padding: 13px 32px;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 110px;
}

.btn-cancel {
  background: rgba(255, 251, 246, 0.9);
  color: #6b4a2c;
  border: 1px solid rgba(190, 157, 124, 0.35);
  box-shadow: 0 4px 14px rgba(74, 46, 23, 0.08);
}

.btn-cancel:hover {
  background: rgba(255, 245, 235, 0.95);
  border-color: rgba(190, 157, 124, 0.5);
  transform: translateY(-1px);
}

.btn-confirm {
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: white;
  border: none;
  box-shadow: 0 6px 20px rgba(157, 41, 41, 0.28);
}

.btn-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(157, 41, 41, 0.35);
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
    transform: translateY(40px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@media (max-width: 480px) {
  .confirm-modal {
    padding: 24px 20px;
    margin: 0 16px;
  }

  .btn-cancel,
  .btn-confirm {
    flex: 1;
    min-width: 0;
    padding: 12px 20px;
  }
}
</style>
