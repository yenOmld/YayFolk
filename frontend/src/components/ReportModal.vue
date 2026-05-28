<template>
  <Teleport to="body">
    <div v-if="visible" class="report-modal-overlay" @click.self="handleCancel">
      <div class="report-modal">
        <div class="report-header">
          <h3 class="report-title">{{ title }}</h3>
          <i class='bx bx-x close-btn' @click="handleCancel"></i>
        </div>
        
        <div class="report-body">
          <p class="report-message">{{ message }}</p>
          
          <div class="reason-section">
            <h4 class="reason-title">选择举报原因</h4>
            <div class="reason-options">
              <div 
                v-for="reason in reasonOptions" 
                :key="reason.value"
                class="reason-option"
                :class="{ active: selectedReason === reason.value }"
                @click="selectedReason = reason.value"
              >
                <div class="reason-checkbox">
                  <i v-if="selectedReason === reason.value" class='bx bx-check'></i>
                </div>
                <span class="reason-text">{{ reason.label }}</span>
              </div>
            </div>
            
            <div class="custom-reason">
              <h5 class="custom-title">其他原因（选填）</h5>
              <textarea 
                v-model="customReason" 
                placeholder="请输入具体举报原因..."
                class="reason-textarea"
                maxlength="200"
              ></textarea>
              <div class="char-count">{{ customReason.length }}/200</div>
            </div>
          </div>
        </div>
        
        <div class="report-footer">
          <button class="btn-cancel" @click="handleCancel">
            {{ cancelText }}
          </button>
          <button class="btn-confirm" @click="handleConfirm" :disabled="submitting">
            <span v-if="submitting" class="loading-text">
              <i class='bx bx-loader-alt bx-spin'></i>
              提交中...
            </span>
            <span v-else>{{ confirmText }}</span>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script>
export default {
  name: 'ReportModal',
  data() {
    return {
      visible: false,
      title: '',
      message: '',
      selectedReason: '',
      customReason: '',
      submitting: false,
      onConfirm: null,
      onCancel: null,
      confirmText: '确认举报',
      cancelText: '取消',
      reasonOptions: [
        { value: 'spam', label: '垃圾广告或营销内容' },
        { value: 'inappropriate', label: '不适当或冒犯性内容' },
        { value: 'misinformation', label: '虚假信息或误导性内容' },
        { value: 'harassment', label: '骚扰或霸凌行为' },
        { value: 'illegal', label: '违法或违规内容' },
        { value: 'other', label: '其他原因' }
      ]
    }
  },
  methods: {
    show(options) {
      this.title = options.title || '举报内容'
      this.message = options.message || '请选择举报原因并填写详细信息'
      this.confirmText = options.confirmText || '确认举报'
      this.cancelText = options.cancelText || '取消'
      this.onConfirm = options.onConfirm || null
      this.onCancel = options.onCancel || null
      this.selectedReason = ''
      this.customReason = ''
      this.submitting = false
      this.visible = true
    },
    
    handleConfirm() {
      if (!this.selectedReason) {
        this.$notify.warning('请选择举报原因')
        return
      }
      
      this.submitting = true
      
      // 构建举报原因
      let reason = ''
      const selectedOption = this.reasonOptions.find(r => r.value === this.selectedReason)
      if (selectedOption) {
        reason = selectedOption.label
        if (this.customReason.trim()) {
          reason += `：${this.customReason.trim()}`
        }
      }
      
      if (this.onConfirm) {
        this.onConfirm(reason, () => {
          this.submitting = false
          this.visible = false
        })
      } else {
        this.submitting = false
        this.visible = false
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
.report-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(44, 22, 11, 0.45);
  backdrop-filter: blur(6px);
  z-index: 10004;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease-out;
}

.report-modal {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(255, 251, 246, 0.96));
  border: 1px solid rgba(190, 157, 124, 0.28);
  border-radius: 24px;
  box-shadow:
    0 24px 56px rgba(74, 46, 23, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(16px);
  padding: 32px;
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease-out;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.report-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--yf-ink);
  margin: 0;
}

.close-btn {
  font-size: 24px;
  color: var(--yf-muted);
  cursor: pointer;
  transition: color 0.3s ease;
}

.close-btn:hover {
  color: var(--yf-accent);
}

.report-message {
  font-size: 14px;
  color: var(--yf-ink-soft);
  line-height: 1.5;
  margin-bottom: 24px;
}

.reason-section {
  margin-bottom: 24px;
}

.reason-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--yf-ink);
  margin-bottom: 16px;
}

.reason-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.reason-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--yf-paper);
  border: 1px solid var(--yf-border);
  border-radius: var(--yf-radius-md);
  cursor: pointer;
  transition: all 0.3s ease;
}

.reason-option:hover {
  border-color: var(--yf-accent);
  transform: translateY(-1px);
}

.reason-option.active {
  border-color: var(--yf-accent);
  background: linear-gradient(135deg, rgba(157, 41, 41, 0.05), rgba(111, 28, 28, 0.02));
}

.reason-checkbox {
  width: 20px;
  height: 20px;
  border: 2px solid var(--yf-border);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.reason-option.active .reason-checkbox {
  background: var(--yf-accent);
  border-color: var(--yf-accent);
  color: white;
}

.reason-text {
  font-size: 14px;
  color: var(--yf-ink);
  font-weight: 500;
}

.custom-reason {
  margin-top: 16px;
}

.custom-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--yf-ink);
  margin-bottom: 12px;
}

.reason-textarea {
  width: 100%;
  min-height: 80px;
  padding: 12px 16px;
  border: 1px solid var(--yf-border);
  border-radius: var(--yf-radius-md);
  background: var(--yf-paper);
  font-size: 14px;
  color: var(--yf-ink);
  resize: vertical;
  transition: border-color 0.3s ease;
}

.reason-textarea:focus {
  outline: none;
  border-color: var(--yf-accent);
  box-shadow: 0 0 0 2px rgba(157, 41, 41, 0.1);
}

.char-count {
  font-size: 12px;
  color: var(--yf-muted);
  text-align: right;
  margin-top: 4px;
}

.report-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-cancel, .btn-confirm {
  padding: 10px 24px;
  border: none;
  border-radius: var(--yf-radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 100px;
}

.btn-cancel {
  background: var(--yf-border);
  color: var(--yf-ink-soft);
}

.btn-cancel:hover {
  background: rgba(47, 36, 29, 0.1);
  transform: translateY(-1px);
}

.btn-confirm {
  background: linear-gradient(135deg, var(--yf-accent) 0%, var(--yf-accent-deep) 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(157, 41, 41, 0.2);
}

.btn-confirm:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(157, 41, 41, 0.3);
}

.btn-confirm:disabled {
  background: var(--yf-border);
  color: var(--yf-muted);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.loading-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 768px) {
  .report-modal {
    padding: 24px;
    margin: 20px;
  }
  
  .report-footer {
    flex-direction: column-reverse;
  }
  
  .btn-cancel, .btn-confirm {
    width: 100%;
  }
}
</style>