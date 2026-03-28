<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>活动审核</h2>
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
    <div v-else-if="list.length === 0" class="empty">暂无活动记录</div>

    <div v-else class="card-list">
      <article v-for="item in list" :key="item.id" class="activity-card">
        <img v-if="item.coverImage" :src="item.coverImage" class="cover" />
        <div v-else class="cover placeholder">活动</div>

        <div class="content">
          <div class="title-row">
            <h3>{{ item.title || '未命名活动' }}</h3>
            <div class="badges">
              <span :class="['audit-tag', item.auditStatus]">{{ auditLabel(item.auditStatus) }}</span>
              <span :class="['status-tag', item.status]">{{ statusLabel(item.status) }}</span>
            </div>
          </div>

          <p class="meta-line">商家：{{ item.merchantName || item.merchantUsername || '-' }}</p>
          <p class="meta-line">非遗品类：{{ item.heritageType || '-' }} / 类型：{{ item.activityType || '-' }}</p>
          <p class="meta-line">时间：{{ formatTime(item.startTime) }} - {{ formatTime(item.endTime) }}</p>
          <p class="meta-line">地点：{{ [item.locationCity, item.locationDetail].filter(Boolean).join(' ') || '-' }}</p>
          <p class="meta-line">人数：{{ item.currentParticipants || 0 }} / {{ item.maxParticipants || '不限' }}</p>
          <p class="meta-line">价格：{{ formatPrice(item.price) }}</p>
          <p class="summary">{{ item.content || '暂无活动介绍' }}</p>
          <p v-if="item.auditRemark" class="remark">审核备注：{{ item.auditRemark }}</p>
        </div>

        <div v-if="item.auditStatus === 'pending'" class="actions">
          <button class="btn approve" @click="openAudit(item, true)">通过</button>
          <button class="btn reject" @click="openAudit(item, false)">驳回</button>
        </div>
      </article>
    </div>

    <div v-if="auditModal.show" class="modal-mask" @click.self="auditModal.show = false">
      <div class="modal">
        <h3>{{ auditModal.approve ? '通过活动审核' : '驳回活动审核' }}</h3>
        <p>{{ auditModal.item?.title || '-' }}</p>
        <textarea v-model="auditModal.remark" rows="3" placeholder="审核备注（可选）" />
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
import { auditAdminActivity, getAdminActivities } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

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
    const res = await getAdminActivities(currentTab.value)
    if (res.code !== 200) {
      throw new Error(res.message || '加载活动失败')
    }
    list.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    list.value = []
    notify(error.message || '加载活动失败', 'error')
  } finally {
    loading.value = false
  }
}

const switchTab = (value) => {
  currentTab.value = value
  load()
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
    const res = await auditAdminActivity(item.id, approve, remark)
    if (res.code !== 200) {
      throw new Error(res.message || '审核失败')
    }
    notify(approve ? '活动已通过审核' : '活动已驳回', 'success')
    auditModal.value.show = false
    await load()
  } catch (error) {
    notify(error.message || '审核失败', 'error')
  }
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN')
}

const formatPrice = (price) => {
  if (!price) {
    return '免费'
  }
  return `￥${(Number(price) / 100).toFixed(2)}`
}

const auditLabel = (status) => {
  return {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回'
  }[status] || status || '未知状态'
}

const statusLabel = (status) => {
  return {
    signup: '报名中',
    full: '已满员',
    ongoing: '进行中',
    ended: '已结束'
  }[status] || status || '未知状态'
}

onMounted(load)
</script>

<style scoped>
.admin-page {
  max-width: 980px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 6px 16px;
  border-radius: 999px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
}

.tab-btn.active {
  background: #0f766e;
  color: #fff;
  border-color: #0f766e;
}

.loading,
.empty {
  padding: 60px;
  text-align: center;
  color: #94a3b8;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-card {
  display: flex;
  gap: 16px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.cover {
  width: 120px;
  height: 120px;
  border-radius: 14px;
  object-fit: cover;
  flex-shrink: 0;
}

.cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 14px;
  font-weight: 600;
}

.content {
  flex: 1;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.title-row h3 {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.audit-tag,
.status-tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.audit-tag.pending {
  background: #fff7ed;
  color: #c2410c;
}

.audit-tag.approved {
  background: #ecfdf5;
  color: #047857;
}

.audit-tag.rejected {
  background: #fef2f2;
  color: #dc2626;
}

.status-tag.signup {
  background: #eff6ff;
  color: #2563eb;
}

.status-tag.full,
.status-tag.ended {
  background: #f3f4f6;
  color: #6b7280;
}

.status-tag.ongoing {
  background: #ecfdf5;
  color: #059669;
}

.meta-line,
.summary,
.remark {
  margin: 6px 0 0;
  color: #6b7280;
  line-height: 1.6;
}

.summary {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.remark {
  color: #b91c1c;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.btn {
  padding: 9px 16px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
}

.btn.approve {
  background: #059669;
  color: #fff;
}

.btn.reject {
  background: #dc2626;
  color: #fff;
}

.btn.cancel {
  background: #e5e7eb;
  color: #374151;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 420px;
  max-width: 92vw;
  background: #fff;
  border-radius: 18px;
  padding: 24px;
}

.modal h3 {
  margin: 0 0 12px;
}

.modal p {
  margin: 0 0 12px;
  color: #6b7280;
}

.modal textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  padding: 10px 12px;
  resize: vertical;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}

@media (max-width: 768px) {
  .activity-card {
    flex-direction: column;
  }

  .cover {
    width: 100%;
    height: 200px;
  }

  .actions {
    flex-direction: row;
  }

  .title-row {
    flex-direction: column;
  }
}
</style>
