<template>
  <div>
    <div v-if="loading" class="empty-state loading-state">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>加载商家信息中...</p>
    </div>

    <form v-else class="apply-form" @submit.prevent="$emit('submit')">
      <div class="form-grid">
        <label class="form-field">
          <span>真实姓名 <em>*</em></span>
          <input v-model.trim="form.realName" type="text" maxlength="50" required>
        </label>

        <label class="form-field">
          <span>身份证号</span>
          <input v-model.trim="form.idCard" type="text" maxlength="18">
        </label>

        <label class="form-field">
          <span>手机号码 <em>*</em></span>
          <input v-model.trim="form.phone" type="text" maxlength="20" required>
        </label>

        <label class="form-field">
          <span>非遗类型 <em>*</em></span>
          <select v-model="form.heritageType" required>
            <option value="">请选择</option>
            <option v-for="item in heritageTypes" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>

        <label class="form-field">
          <span>店铺名称 <em>*</em></span>
          <input v-model.trim="form.shopName" type="text" maxlength="100" required>
        </label>

        <label class="form-field">
          <span>省份</span>
          <input v-model.trim="form.province" type="text" maxlength="32">
        </label>

        <label class="form-field">
          <span>城市</span>
          <input v-model.trim="form.city" type="text" maxlength="32">
        </label>

        <label class="form-field full-span">
          <span>店铺地址</span>
          <input v-model.trim="form.shopAddress" type="text" maxlength="200">
        </label>

        <label class="form-field full-span">
          <span>非遗项目描述 <em>*</em></span>
          <textarea v-model.trim="form.heritageDescription" rows="5" maxlength="1000" required></textarea>
        </label>

        <label class="form-field full-span">
          <span>店铺介绍</span>
          <textarea v-model.trim="form.intro" rows="4" maxlength="255"></textarea>
        </label>

        <div class="form-field full-span">
          <span>证明材料</span>
          <div class="uploader-card">
            <div class="uploader-actions">
              <button
                type="button"
                class="secondary-btn"
                :disabled="uploadingProofs || submitting"
                @click="openProofPicker"
              >
                {{ uploadingProofs ? '上传中...' : '上传图片' }}
              </button>
              <button
                type="button"
                class="ghost-btn"
                :disabled="!form.proofImages.length || submitting"
                @click="$emit('clear-proof-images')"
              >
                清空全部
              </button>
            </div>

            <input
              ref="proofInput"
              class="hidden-input"
              type="file"
              accept="image/*"
              multiple
              @change="handleProofChange"
            >

            <div v-if="form.proofImages.length" class="proof-grid editable-proof-grid">
              <div
                v-for="(image, index) in form.proofImages"
                :key="`${image}-${index}`"
                class="proof-thumb editable-thumb"
              >
                <img :src="image" :alt="`已选择的证明材料 ${index + 1}`">
                <button
                  type="button"
                  class="remove-proof-btn"
                  :disabled="submitting"
                  @click="$emit('remove-proof-image', index)"
                >
                  删除
                </button>
              </div>
            </div>
            <p v-else class="proof-empty">请上传营业执照、身份证明或其他资质文件。</p>
          </div>
        </div>
      </div>

      <div class="form-footer">
        <p class="form-hint">{{ submitHint }}</p>
        <div class="form-actions">
          <button
            v-if="hasApplication"
            type="button"
            class="ghost-btn"
            :disabled="submitting"
            @click="$emit('reset')"
          >
            重置
          </button>
          <button type="submit" class="primary-btn" :disabled="submitting || uploadingProofs">
            {{ submitButtonText }}
          </button>
        </div>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  form: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  submitting: {
    type: Boolean,
    default: false
  },
  uploadingProofs: {
    type: Boolean,
    default: false
  },
  hasApplication: {
    type: Boolean,
    default: false
  },
  heritageTypes: {
    type: Array,
    default: () => []
  },
  submitHint: {
    type: String,
    default: ''
  },
  submitButtonText: {
    type: String,
    default: 'Submit'
  }
})

const emit = defineEmits(['submit', 'reset', 'proof-change', 'clear-proof-images', 'remove-proof-image'])

const proofInput = ref(null)

const openProofPicker = () => {
  proofInput.value?.click()
}

const handleProofChange = (event) => {
  emit('proof-change', event)
}
</script>

<style scoped>
.apply-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-field span {
  color: #374151;
  font-size: 14px;
  font-weight: 700;
}

.form-field em {
  color: #ef4444;
  font-style: normal;
}

.form-field input,
.form-field select,
.form-field textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #d4d9e0;
  background: #ffffff;
  color: #1f2937;
  font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-field textarea {
  resize: vertical;
  font-family: inherit;
}

.form-field input:focus,
.form-field select:focus,
.form-field textarea:focus {
  outline: none;
  border-color: #c2410c;
  box-shadow: 0 0 0 4px rgba(194, 65, 12, 0.12);
}

.uploader-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px dashed #cbd5e1;
  background: #fafaf9;
}

.uploader-actions,
.form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hidden-input {
  display: none;
}

.proof-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.editable-proof-grid {
  margin-top: 14px;
}

.proof-thumb {
  position: relative;
  display: block;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.proof-thumb img {
  display: block;
  width: 100%;
  height: 148px;
  object-fit: cover;
}

.proof-empty,
.empty-state {
  margin: 0;
  text-align: center;
  color: #64748b;
}

.empty-state {
  padding: 42px 20px;
  border-radius: 20px;
  background: #f8fafc;
}

.empty-state i {
  display: block;
  font-size: 32px;
  color: #94a3b8;
  margin-bottom: 12px;
}

.loading-state p {
  color: #64748b;
}

.remove-proof-btn,
.primary-btn,
.secondary-btn,
.ghost-btn {
  border: none;
  border-radius: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease, box-shadow 0.2s ease;
}

.primary-btn,
.secondary-btn,
.ghost-btn {
  min-height: 46px;
  padding: 0 18px;
}

.primary-btn {
  background: linear-gradient(135deg, #9d2929, #d97706);
  color: #ffffff;
}

.secondary-btn {
  background: rgba(22, 97, 171, 0.1);
  color: #1661ab;
}

.ghost-btn {
  background: #f3f4f6;
  color: #374151;
}

.remove-proof-btn {
  position: absolute;
  right: 10px;
  bottom: 10px;
  min-height: 32px;
  padding: 0 12px;
  background: rgba(15, 23, 42, 0.82);
  color: #ffffff;
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.form-hint {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.full-span {
  grid-column: 1 / -1;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

@media (max-width: 920px) {
  .form-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .form-actions,
  .uploader-actions {
    width: 100%;
    flex-wrap: wrap;
    justify-content: stretch;
  }

  .form-actions > *,
  .uploader-actions > * {
    flex: 1 1 180px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .proof-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .form-actions > *,
  .uploader-actions > * {
    width: 100%;
  }
}
</style>
