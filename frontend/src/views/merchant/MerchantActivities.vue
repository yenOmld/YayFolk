<template>
  <div class="merchant-page">
    <div class="page-header">
      <div>
        <h2>活动管理</h2>
        <p>集中维护商家活动，创建和编辑都进入独立页面处理。</p>
      </div>
      <button class="create-btn" @click="goCreate">+ 创建活动</button>
    </div>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>加载中...</p>
    </div>

    <div v-else-if="list.length === 0" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>还没有活动，先创建第一个活动。</p>
    </div>

    <div v-else class="activity-list">
      <article v-for="item in list" :key="item.id" class="activity-card">
        <img v-if="item.coverImage" :src="item.coverImage" class="card-cover" :alt="item.title" />
        <div v-else class="cover-placeholder">活动</div>

        <div class="card-info">
          <div class="card-head">
            <h3>{{ item.title }}</h3>
            <div class="tag-row">
              <span class="tag heritage">{{ item.heritageType || '未分类' }}</span>
              <span class="tag" :class="item.status">{{ statusLabel(item.status) }}</span>
              <span class="tag audit" :class="item.auditStatus">{{ auditLabel(item.auditStatus) }}</span>
            </div>
          </div>

          <div class="meta-grid">
            <span>时间：{{ formatDateTime(item.startTime) }}</span>
            <span>地点：{{ formatLocation(item) }}</span>
            <span>价格：{{ formatPrice(item.price) }}</span>
            <span>人数：{{ item.currentParticipants || 0 }}/{{ item.maxParticipants || '不限' }}</span>
          </div>

          <p v-if="item.subtitle" class="subtitle">{{ item.subtitle }}</p>
          <p v-if="item.auditRemark" class="audit-remark">审核备注：{{ item.auditRemark }}</p>
          <p v-else-if="item.auditStatus === 'pending'" class="audit-tip">保存后会重新进入审核，通过后才会展示在前台活动页。</p>
        </div>

        <div class="card-actions">
          <button class="act-btn edit" @click="goEdit(item.id)">编辑</button>
          <button class="act-btn bookings" @click="viewBookings(item)">预约({{ item.currentParticipants || 0 }})</button>
          <button class="act-btn delete" @click="handleDelete(item.id)">删除</button>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { deleteMerchantActivity, getMerchantActivities } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)
const router = useRouter()

const list = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await getMerchantActivities()
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

const goCreate = () => {
  router.push({ name: 'merchant-activity-create' })
}

const goEdit = (id) => {
  router.push({ name: 'merchant-activity-edit', params: { id } })
}

const viewBookings = (item) => {
  router.push({ path: '/merchant/bookings', query: { activityId: item.id, title: item.title } })
}

const handleDelete = async (id) => {
  if (!window.confirm('确认删除该活动吗？')) {
    return
  }

  try {
    const res = await deleteMerchantActivity(id)
    if (res.code !== 200) {
      throw new Error(res.message || '删除失败')
    }
    list.value = list.value.filter(item => item.id !== id)
    notify('活动已删除', 'success')
  } catch (error) {
    notify(error.message || '删除失败', 'error')
  }
}

const formatDateTime = (value) => value ? new Date(value).toLocaleString('zh-CN') : '待定'
const formatLocation = (item) => [item.locationProvince, item.locationCity, item.locationDetail].filter(Boolean).join(' / ') || '待定'
const formatPrice = (price) => !price ? '免费' : `¥${(Number(price) / 100).toFixed(2)}`

const statusLabel = (status) => ({
  signup: '报名中',
  full: '已满员',
  ongoing: '进行中',
  ended: '已结束'
}[status] || '待开始')

const auditLabel = (status) => ({
  pending: '待审核',
  approved: '已通过',
  rejected: '已驳回'
}[status] || '待提交')

onMounted(load)
</script>

<style scoped>
.merchant-page {
  max-width: 1100px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.page-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.create-btn {
  padding: 12px 18px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #059669, #047857);
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}

.state-card {
  padding: 72px 20px;
  text-align: center;
  color: #94a3b8;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
}

.state-card i {
  font-size: 40px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-card {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr) 130px;
  gap: 18px;
  align-items: stretch;
  padding: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
}

.card-cover,
.cover-placeholder {
  width: 160px;
  height: 160px;
  border-radius: 14px;
}

.card-cover {
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #6b7280;
  font-weight: 700;
}

.card-info {
  min-width: 0;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.card-head h3 {
  margin: 0;
  color: #111827;
  font-size: 20px;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.tag {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.tag.heritage {
  background: #ede9fe;
  color: #7c3aed;
}

.tag.signup {
  background: #eff6ff;
  color: #2563eb;
}

.tag.ongoing {
  background: #ecfdf5;
  color: #059669;
}

.tag.full,
.tag.ended {
  background: #f3f4f6;
  color: #6b7280;
}

.tag.audit.pending {
  background: #fff7ed;
  color: #c2410c;
}

.tag.audit.approved {
  background: #ecfdf5;
  color: #047857;
}

.tag.audit.rejected {
  background: #fef2f2;
  color: #dc2626;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
  margin-top: 14px;
  color: #4b5563;
}

.subtitle,
.audit-remark,
.audit-tip {
  margin: 12px 0 0;
  line-height: 1.7;
}

.subtitle {
  color: #6b7280;
}

.audit-remark {
  color: #b91c1c;
}

.audit-tip {
  color: #92400e;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.act-btn {
  padding: 10px 14px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 600;
}

.act-btn.edit {
  background: #dbeafe;
  color: #2563eb;
}

.act-btn.bookings {
  background: #d1fae5;
  color: #059669;
}

.act-btn.delete {
  background: #fee2e2;
  color: #dc2626;
}

@media (max-width: 900px) {
  .activity-card {
    grid-template-columns: 1fr;
  }

  .card-cover,
  .cover-placeholder {
    width: 100%;
    height: 200px;
  }

  .card-head,
  .page-header {
    flex-direction: column;
  }

  .tag-row {
    justify-content: flex-start;
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }

  .card-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
