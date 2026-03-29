<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>活动审核</h2>
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
          <input v-model.trim="keyword" type="text" placeholder="搜索活动/商家/地点/类型/非遗品类" />
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="filteredList.length === 0" class="empty">{{ emptyText }}</div>

    <div v-else class="card-list">
      <article v-for="item in pagedList" :key="item.id" class="activity-card">
        <img v-if="item.coverImage" :src="item.coverImage" class="cover" alt="活动封面" />
        <div v-else class="cover placeholder">活动</div>

        <div class="content">
          <div class="title-row">
            <h3>{{ item.title || '未命名活动' }}</h3>
            <div class="badges">
              <span :class="['audit-tag', item.auditStatus]">{{ auditLabel(item.auditStatus) }}</span>
              <span :class="['status-tag', item.status]">{{ statusLabel(item.status) }}</span>
            </div>
          </div>

          <div class="detail-grid">
            <p class="meta-line">商家：{{ item.merchantName || item.merchantUsername || '-' }}</p>
            <p class="meta-line">非遗品类：{{ item.heritageType || '-' }}</p>
            <p class="meta-line">活动类型：{{ item.activityType || '-' }}</p>
            <p class="meta-line">时间：{{ formatTime(item.startTime) }} - {{ formatTime(item.endTime) }}</p>
            <p class="meta-line">地点：{{ [item.locationCity, item.locationDetail].filter(Boolean).join(' ') || '-' }}</p>
            <p class="meta-line">人数：{{ item.currentParticipants || 0 }} / {{ item.maxParticipants || '不限' }}</p>
            <p class="meta-line">价格：{{ formatPrice(item.price) }}</p>
          </div>

          <div class="summary-block">
            <span class="section-label">活动简介</span>
            <p class="summary">{{ item.content || '暂无活动介绍' }}</p>
          </div>

          <div v-if="item.auditRemark" class="remark-block">
            <span class="section-label">审核备注</span>
            <p class="remark">{{ item.auditRemark }}</p>
          </div>
        </div>

        <div v-if="item.auditStatus === 'pending' || item.auditStatus === 'approved'" class="actions">
          <button v-if="item.auditStatus === 'pending'" class="btn approve" @click="handleApprove(item)">通过</button>
          <button class="btn reject" @click="openReject(item)">{{ item.auditStatus === 'approved' ? '下架' : '驳回' }}</button>
        </div>
      </article>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="page === 1" @click="changePage(-1)">上一页</button>
      <span class="page-status">第 {{ page }} / {{ totalPages }} 页</span>
      <button class="page-btn" :disabled="page === totalPages" @click="changePage(1)">下一页</button>
    </div>

    <div v-if="auditModal.show" class="modal-mask" @click.self="auditModal.show = false">
      <div class="modal">
        <h3>填写驳回原因</h3>
        <p class="modal-intro">{{ auditModal.item?.title || '-' }}</p>

        <div class="modal-field">
          <label class="field-label">驳回原因</label>
          <select v-model="auditModal.reasonPreset" class="reason-select">
            <option value="">请选择驳回原因</option>
            <option
              v-for="option in activityRejectReasonOptions"
              :key="option"
              :value="option"
            >
              {{ option }}
            </option>
          </select>
          <textarea
            v-model.trim="auditModal.remark"
            rows="4"
            placeholder="可补充说明（选填）"
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
import { auditAdminActivity, getAdminActivities } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const activityRejectReasonOptions = [
  '活动主题与非遗分类不匹配',
  '活动时间或地点信息不完整',
  '活动内容存在营销引流风险',
  '活动描述夸大或信息不真实',
  '存在安全合规风险',
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
      item.title,
      item.merchantName,
      item.merchantUsername,
      item.heritageType,
      item.activityType,
      item.locationCity,
      item.locationDetail,
      item.content,
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
const emptyText = computed(() => keyword.value ? '没有匹配的活动记录' : '暂无活动记录')

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminActivities(currentTab.value)
    if (res.code !== 200) {
      throw new Error(res.message || '加载活动失败')
    }
    list.value = Array.isArray(res.data) ? res.data : []
    page.value = 1
  } catch (error) {
    list.value = []
    notify(error.message || '加载活动失败', 'error')
  } finally {
    loading.value = false
  }
}

const switchTab = (value) => {
  currentTab.value = value
  page.value = 1
  load()
}

const openReject = (item) => {
  auditModal.value = {
    show: true,
    item,
    reasonPreset: '',
    remark: ''
  }
}

const changePage = (step) => {
  const nextPage = page.value + step
  if (nextPage < 1 || nextPage > totalPages.value) {
    return
  }
  page.value = nextPage
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
    const res = await auditAdminActivity(item.id, true)
    if (res.code !== 200) {
      throw new Error(res.message || '审核失败')
    }
    notify('活动已通过审核', 'success')
    await load()
  } catch (error) {
    notify(error.message || '审核失败', 'error')
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
    const res = await auditAdminActivity(item.id, false, finalRemark)
    if (res.code !== 200) {
      throw new Error(res.message || '审核失败')
    }
    notify('活动已驳回', 'success')
    closeReject()
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

watch(keyword, () => {
  page.value = 1
})

onMounted(load)
</script>

<style scoped>
.admin-page {
  max-width: 1040px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #111827;
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
  border-radius: 999px;
  border: 1px solid #d1d5db;
  background: #fff;
  cursor: pointer;
}

.tab-btn.active {
  background: #0f766e;
  color: #fff;
  border-color: #0f766e;
}

.search-box {
  display: flex;
}

.search-box input {
  width: 280px;
  max-width: 72vw;
  padding: 8px 14px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
}

.search-box input:focus {
  outline: none;
  border-color: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.12);
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

.activity-card {
  display: flex;
  gap: 18px;
  padding: 20px;
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
  min-width: 0;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
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

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
}

.meta-line {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
}

.summary-block,
.remark-block {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #eef2f7;
}

.section-label {
  display: block;
  margin-bottom: 6px;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.summary,
.remark {
  margin: 0;
  line-height: 1.7;
}

.summary {
  color: #6b7280;
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
  width: 460px;
  max-width: 92vw;
  background: #fff;
  border-radius: 18px;
  padding: 24px;
}

.modal h3 {
  margin: 0 0 12px;
}

.modal-intro {
  margin: 0 0 16px;
  color: #6b7280;
  line-height: 1.7;
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
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  padding: 12px;
  min-height: 120px;
  resize: vertical;
  line-height: 1.7;
}

.reason-select {
  width: 100%;
  height: 44px;
  box-sizing: border-box;
  border: 1px solid #94a3b8;
  border-radius: 12px;
  padding: 0 40px 0 12px;
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
  border-color: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.12);
}

.reason-select:hover {
  border-color: #64748b;
}

.reason-select:focus {
  outline: none;
  border-color: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.12);
}

.reason-select option {
  color: #0f172a;
  background: #fff;
}

.field-hint {
  color: #94a3b8;
  line-height: 1.6;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
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

  .title-row,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
