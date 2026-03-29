<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>商家申请审核</h2>
      </div>
      <div class="header-side">
        <div class="filter-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            :class="['tab-btn', { active: currentTab === tab.value }]"
            @click="switchTab(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="search-box">
          <input v-model.trim="keyword" type="text" placeholder="搜索店铺/姓名/账号/电话/非遗品类" />
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="filteredList.length === 0" class="empty">{{ emptyText }}</div>

    <div v-else class="card-list">
      <div v-for="item in pagedList" :key="item.id" class="merchant-card">
        <div class="card-header">
          <div class="user-info">
            <img :src="item.avatar || defaultAvatar" class="avatar" alt="用户头像" />
            <div>
              <div class="name">{{ item.nickname || item.username || '未知用户' }}</div>
              <div class="sub">@{{ item.username || '-' }}</div>
            </div>
          </div>
          <span :class="['status-badge', item.applicationStatus]">
            {{ statusLabel(item.applicationStatus) }}
          </span>
        </div>

        <div class="card-body">
          <div class="info-row">
            <span class="label">真实姓名</span>
            <strong>{{ item.realName || '-' }}</strong>
          </div>
          <div class="info-row">
            <span class="label">联系电话</span>
            <strong>{{ item.phone || '-' }}</strong>
          </div>
          <div class="info-row">
            <span class="label">店铺名称</span>
            <strong>{{ item.shopName || '-' }}</strong>
          </div>
          <div class="info-row">
            <span class="label">非遗品类</span>
            <strong>{{ item.heritageType || '-' }}</strong>
          </div>
          <div class="info-row full">
            <span class="label">申请说明</span>
            <p>{{ item.heritageDescription || '-' }}</p>
          </div>
          <div class="info-row full">
            <span class="label">店铺地址</span>
            <p>{{ item.shopAddress || '-' }}</p>
          </div>
          <div v-if="item.auditRemark" class="info-row full remark">
            <span class="label">审核备注</span>
            <p>{{ item.auditRemark }}</p>
          </div>
          <div class="info-row full">
            <span class="label">申请时间</span>
            <p>{{ formatTime(item.createTime) }}</p>
          </div>
        </div>

        <div v-if="item.applicationStatus === 'pending'" class="card-actions">
          <button v-if="item.applicationStatus === 'pending'" class="btn approve" @click="handleApprove(item)">通过</button>
          <button class="btn reject" @click="openReject(item)">驳回</button>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="page === 1" @click="changePage(-1)">上一页</button>
      <span class="page-status">第 {{ page }} / {{ totalPages }} 页</span>
      <button class="page-btn" :disabled="page === totalPages" @click="changePage(1)">下一页</button>
    </div>

    <div v-if="auditModal.show" class="modal-mask" @click.self="auditModal.show = false">
      <div class="modal">
        <h3>填写驳回原因</h3>
        <p class="modal-intro">
          商家：{{ auditModal.item?.shopName || '-' }}（{{ auditModal.item?.realName || '-' }}）
        </p>

        <div class="modal-field">
          <label class="field-label">驳回原因</label>
          <select v-model="auditModal.reasonPreset" class="reason-select">
            <option value="">请选择驳回原因</option>
            <option
              v-for="option in merchantRejectReasonOptions"
              :key="option"
              :value="option"
            >
              {{ option }}
            </option>
          </select>
          <textarea
            v-model.trim="auditModal.remark"
            placeholder="可补充说明（选填）"
            rows="4"
          />
          <small class="field-hint">建议先选择预设原因，如有需要可在下方补充说明。</small>
        </div>

        <div class="modal-actions">
          <button class="btn cancel" @click="auditModal.show = false">取消</button>
          <button class="btn reject" @click="submitReject">确认驳回</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from 'vue'
import { auditMerchant, getAdminMerchants } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const defaultAvatar = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%23e0e0e0'/%3E%3C/svg%3E"
const merchantRejectReasonOptions = [
  '资质信息不完整或不清晰',
  '店铺信息不真实',
  '联系方式格式不规范',
  '认证材料与类型不匹配',
  '疑似广告或引流内容',
  '其他'
]
const tabs = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' }
]

const currentTab = ref('pending')
const list = ref([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = 3
const auditModal = ref({
  show: false,
  item: null,
  reasonPreset: '',
  remark: ''
})

const filteredList = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  if (!search) {
    return list.value
  }
  return list.value.filter(item => {
    const fields = [
      item.shopName,
      item.realName,
      item.username,
      item.nickname,
      item.phone,
      item.heritageType,
      item.shopAddress,
      item.heritageDescription,
      item.auditRemark
    ]
    return fields.some(field => String(field || '').toLowerCase().includes(search))
  })
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredList.value.length / pageSize)))
const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredList.value.slice(start, start + pageSize)
})
const emptyText = computed(() => keyword.value ? '没有匹配的商家申请' : '暂无申请记录')

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminMerchants(currentTab.value)
    if (res.code !== 200) {
      throw new Error(res.message || '加载失败')
    }
    list.value = Array.isArray(res.data) ? res.data : []
    page.value = 1
  } catch (error) {
    list.value = []
    notify(error.message || '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const switchTab = (value) => {
  currentTab.value = value
  page.value = 1
  load()
}

const statusLabel = (status) => {
  return {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回'
  }[status] || status || '未知状态'
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN')
}

const openReject = (item) => {
  auditModal.value = {
    show: true,
    item,
    reasonPreset: '',
    remark: ''
  }
}

const closeReject = () => {
  auditModal.value = {
    show: false,
    item: null,
    reasonPreset: '',
    remark: ''
  }
}

const buildRejectRemark = (reasonPreset, remark) => {
  const preset = String(reasonPreset || '').trim()
  const extra = String(remark || '').trim()
  if (!preset && !extra) {
    return ''
  }
  if (!preset) {
    return extra
  }
  if (!extra || extra === preset) {
    return preset
  }
  return `${preset}; ${extra}`
}

const handleApprove = async (item) => {
  try {
    const res = await auditMerchant(item.id, true)
    if (res.code !== 200) {
      throw new Error(res.message || '操作失败')
    }
    notify('已通过申请', 'success')
    await load()
  } catch (error) {
    notify(error.message || '操作失败', 'error')
  }
}

const submitReject = async () => {
  const { item, reasonPreset, remark } = auditModal.value
  const finalRemark = buildRejectRemark(reasonPreset, remark)
  if (!finalRemark) {
    notify('请先填写驳回原因', 'warning')
    return
  }
  try {
    const res = await auditMerchant(item.id, false, finalRemark)
    if (res.code !== 200) {
      throw new Error(res.message || '操作失败')
    }
    notify('已驳回申请', 'success')
    closeReject()
    await load()
  } catch (error) {
    notify(error.message || '操作失败', 'error')
  }
}

const changePage = (step) => {
  const nextPage = page.value + step
  if (nextPage < 1 || nextPage > totalPages.value) {
    return
  }
  page.value = nextPage
}

watch(keyword, () => {
  page.value = 1
})

onMounted(load)
</script>

<style scoped>
.admin-page {
  max-width: 980px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.header-side {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid #d1d5db;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  color: #6b7280;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #7c3aed;
  color: #fff;
  border-color: #7c3aed;
}

.search-box {
  display: flex;
}

.search-box input {
  width: 260px;
  max-width: 72vw;
  padding: 8px 14px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
}

.search-box input:focus {
  outline: none;
  border-color: #7c3aed;
  box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.12);
}

.loading,
.empty {
  text-align: center;
  padding: 60px;
  color: #9ca3af;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}

.page-status {
  color: #94a3b8;
  font-size: 14px;
}

.page-btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: #fff;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.merchant-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #e5e7eb;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background: #e5e7eb;
}

.name {
  font-weight: 600;
  font-size: 15px;
  color: #111827;
}

.sub {
  font-size: 12px;
  color: #9ca3af;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.pending {
  background: #fef3c7;
  color: #d97706;
}

.status-badge.approved {
  background: #d1fae5;
  color: #059669;
}

.status-badge.rejected {
  background: #fee2e2;
  color: #dc2626;
}

.card-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  color: #374151;
}

.info-row.full {
  grid-column: 1 / -1;
}

.info-row p,
.info-row strong {
  margin: 0;
  line-height: 1.7;
}

.info-row.remark p {
  color: #dc2626;
}

.label {
  color: #9ca3af;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.card-actions {
  display: flex;
  gap: 10px;
  border-top: 1px solid #f3f4f6;
  padding-top: 16px;
}

.btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s;
}

.btn.approve {
  background: #059669;
  color: #fff;
}

.btn.approve:hover {
  background: #047857;
}

.btn.reject {
  background: #dc2626;
  color: #fff;
}

.btn.reject:hover {
  background: #b91c1c;
}

.btn.cancel {
  background: #f3f4f6;
  color: #374151;
}

.btn.cancel:hover {
  background: #e5e7eb;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  width: 460px;
  max-width: 90vw;
}

.modal h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

.modal-intro {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.7;
  margin: 0 0 16px;
}

.modal-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  color: #374151;
  font-size: 13px;
  font-weight: 600;
}

.modal textarea {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 12px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
  min-height: 120px;
  line-height: 1.7;
}

.reason-select {
  width: 100%;
  height: 44px;
  border: 1px solid #94a3b8;
  border-radius: 10px;
  padding: 0 40px 0 12px;
  box-sizing: border-box;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.4;
  color: #0f172a;
  appearance: none;
  -webkit-appearance: none;
  background-color: #fff;
  background-image:
    linear-gradient(45deg, transparent 50%, #64748b 50%),
    linear-gradient(135deg, #64748b 50%, transparent 50%);
  background-position:
    calc(100% - 16px) calc(50% - 2px),
    calc(100% - 11px) calc(50% - 2px);
  background-size: 6px 6px, 6px 6px;
  background-repeat: no-repeat;
  cursor: pointer;
}

.modal textarea:focus {
  outline: none;
  border-color: #7c3aed;
  box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.12);
}

.reason-select:hover {
  border-color: #64748b;
}

.reason-select:focus {
  outline: none;
  border-color: #7c3aed;
  box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.12);
}

.reason-select option {
  color: #0f172a;
  background: #fff;
}

.field-hint {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.6;
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .card-body {
    grid-template-columns: 1fr;
  }
}
</style>
