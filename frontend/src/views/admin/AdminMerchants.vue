<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>商家申请审核</h2>
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
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="list.length === 0" class="empty">暂无申请记录</div>

    <div v-else class="card-list">
      <div v-for="item in list" :key="item.id" class="merchant-card">
        <div class="card-header">
          <div class="user-info">
            <img :src="item.avatar || defaultAvatar" class="avatar" />
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
          <div class="info-row"><span class="label">真实姓名：</span>{{ item.realName || '-' }}</div>
          <div class="info-row"><span class="label">联系电话：</span>{{ item.phone || '-' }}</div>
          <div class="info-row"><span class="label">店铺名称：</span>{{ item.shopName || '-' }}</div>
          <div class="info-row"><span class="label">非遗品类：</span>{{ item.heritageType || '-' }}</div>
          <div class="info-row full"><span class="label">申请说明：</span>{{ item.heritageDescription || '-' }}</div>
          <div class="info-row full"><span class="label">店铺地址：</span>{{ item.shopAddress || '-' }}</div>
          <div v-if="item.auditRemark" class="info-row full remark">
            <span class="label">审核备注：</span>{{ item.auditRemark }}
          </div>
          <div class="info-row full"><span class="label">申请时间：</span>{{ formatTime(item.createTime) }}</div>
        </div>

        <div v-if="item.applicationStatus === 'pending'" class="card-actions">
          <button class="btn approve" @click="openAudit(item, true)">通过</button>
          <button class="btn reject" @click="openAudit(item, false)">驳回</button>
        </div>
      </div>
    </div>

    <div v-if="auditModal.show" class="modal-mask" @click.self="auditModal.show = false">
      <div class="modal">
        <h3>{{ auditModal.approve ? '通过申请' : '驳回申请' }}</h3>
        <p>商家：{{ auditModal.item?.shopName || '-' }}（{{ auditModal.item?.realName || '-' }}）</p>
        <textarea v-model="auditModal.remark" placeholder="审核备注（可选）" rows="3" />
        <div class="modal-actions">
          <button class="btn cancel" @click="auditModal.show = false">取消</button>
          <button :class="['btn', auditModal.approve ? 'approve' : 'reject']" @click="submitAudit">
            确认{{ auditModal.approve ? '通过' : '驳回' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { auditMerchant, getAdminMerchants } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const defaultAvatar = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%23e0e0e0'/%3E%3C/svg%3E"
const tabs = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' }
]

const currentTab = ref('pending')
const list = ref([])
const loading = ref(false)
const auditModal = ref({
  show: false,
  item: null,
  approve: true,
  remark: ''
})

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminMerchants(currentTab.value)
    if (res.code !== 200) {
      throw new Error(res.message || '加载失败')
    }
    list.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    list.value = []
    notify(error.message || '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const switchTab = (value) => {
  currentTab.value = value
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

const openAudit = (item, approve) => {
  auditModal.value = {
    show: true,
    item,
    approve,
    remark: ''
  }
}

const submitAudit = async () => {
  const { item, approve, remark } = auditModal.value
  try {
    const res = await auditMerchant(item.id, approve, remark)
    if (res.code !== 200) {
      throw new Error(res.message || '操作失败')
    }
    notify(approve ? '已通过申请' : '已驳回申请', 'success')
    auditModal.value.show = false
    await load()
  } catch (error) {
    notify(error.message || '操作失败', 'error')
  }
}

onMounted(load)
</script>

<style scoped>
.admin-page {
  max-width: 900px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 6px 16px;
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
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 44px;
  height: 44px;
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
  font-weight: 500;
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
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 16px;
}

.info-row {
  font-size: 13px;
  color: #374151;
}

.info-row.full {
  grid-column: 1 / -1;
}

.info-row.remark {
  color: #dc2626;
}

.label {
  color: #9ca3af;
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
  font-weight: 500;
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
  width: 420px;
  max-width: 90vw;
}

.modal h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

.modal p {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 16px;
}

.modal textarea {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .card-body {
    grid-template-columns: 1fr;
  }
}
</style>
