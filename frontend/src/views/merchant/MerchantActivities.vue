<template>
  <div class="merchant-page">
    <div class="page-header">
      <div>
        <h2>Activity Management</h2>
        <p>Review, edit, and organize your merchant activities with a paged workspace.</p>
      </div>
      <button class="create-btn" @click="goCreate">+ Create Activity</button>
    </div>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>Loading activities...</p>
    </div>

    <div v-else-if="list.length === 0" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>No activities yet. Create your first activity to get started.</p>
    </div>

    <template v-else>
      <div class="activity-list">
        <article v-for="item in list" :key="item.id" class="activity-card">
          <img v-if="item.coverImage" :src="item.coverImage" class="card-cover" :alt="item.title" />
          <div v-else class="cover-placeholder">Activity</div>

          <div class="card-info">
            <div class="card-head">
              <h3>{{ item.title }}</h3>
              <div class="tag-row">
                <span class="tag heritage">{{ item.heritageType || 'Uncategorized' }}</span>
                <span class="tag" :class="item.status">{{ statusLabel(item.status) }}</span>
                <span class="tag audit" :class="item.auditStatus">{{ auditLabel(item.auditStatus) }}</span>
              </div>
            </div>

            <div class="meta-grid">
              <span>Time: {{ formatDateTime(item.startTime) }}</span>
              <span>Location: {{ formatLocation(item) }}</span>
              <span>Price: {{ formatPrice(item.price) }}</span>
              <span>Bookings: {{ item.currentParticipants || 0 }}/{{ item.maxParticipants || 'Unlimited' }}</span>
            </div>

            <p v-if="item.subtitle" class="subtitle">{{ item.subtitle }}</p>
            <p v-if="item.auditRemark" class="audit-remark">Audit Note: {{ item.auditRemark }}</p>
            <p v-else-if="item.auditStatus === 'pending'" class="audit-tip">
              Saving changes will send the activity back into audit review before it appears publicly.
            </p>
          </div>

          <div class="card-actions">
            <button class="act-btn edit" @click="goEdit(item.id)">Edit</button>
            <button class="act-btn bookings" @click="viewBookings(item)">Bookings ({{ item.currentParticipants || 0 }})</button>
            <button class="act-btn delete" @click="handleDelete(item.id)">Delete</button>
          </div>
        </article>
      </div>

      <div class="pagination-card">
        <div class="pagination-summary">
          <strong>{{ total }}</strong>
          <span>{{ pageSummary }}</span>
        </div>

        <div class="pagination-controls">
          <label class="page-size-field">
            <span>Per Page</span>
            <select v-model="pageSize" @change="handlePageSizeChange">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
            </select>
          </label>

          <div class="pager-buttons">
            <button class="pager-btn" :disabled="currentPage <= 1 || loading" @click="changePage(currentPage - 1)">Previous</button>
            <span class="page-indicator">Page {{ currentPage }} / {{ totalPages || 1 }}</span>
            <button class="pager-btn" :disabled="currentPage >= totalPages || loading" @click="changePage(currentPage + 1)">Next</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteMerchantActivity, getMerchantActivities } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)
const route = useRoute()
const router = useRouter()

const DEFAULT_PAGE_SIZE = 3
const pageSizeOptions = [3, 6, 9, 12]

const list = ref([])
const loading = ref(false)
const total = ref(0)
const totalPages = ref(0)
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)

const pageSummary = computed(() => {
  if (!total.value) {
    return 'No activity records available.'
  }
  const start = (currentPage.value - 1) * pageSize.value + 1
  const end = Math.min(currentPage.value * pageSize.value, total.value)
  return `Showing ${start}-${end} of ${total.value} activities.`
})

const syncStateFromRoute = () => {
  const page = Number(route.query.page || 1)
  const size = Number(route.query.size || DEFAULT_PAGE_SIZE)
  currentPage.value = Number.isFinite(page) && page > 0 ? page : 1
  pageSize.value = pageSizeOptions.includes(size) ? size : DEFAULT_PAGE_SIZE
}

const syncRoute = () => {
  router.replace({
    path: '/merchant/activities',
    query: {
      ...(currentPage.value > 1 ? { page: String(currentPage.value) } : {}),
      ...(pageSize.value !== DEFAULT_PAGE_SIZE ? { size: String(pageSize.value) } : {})
    }
  })
}

const load = async () => {
  loading.value = true
  try {
    const res = await getMerchantActivities({
      page: currentPage.value - 1,
      size: pageSize.value
    })
    if (res.code !== 200) {
      throw new Error(res.message || 'Failed to load activities')
    }

    const payload = res.data || {}
    list.value = Array.isArray(payload.items) ? payload.items : []
    total.value = Number(payload.total || 0)
    totalPages.value = Math.max(Number(payload.totalPages || 0), list.value.length ? 1 : 0)

    if (totalPages.value > 0 && currentPage.value > totalPages.value) {
      currentPage.value = totalPages.value
      syncRoute()
      await load()
    }
  } catch (error) {
    list.value = []
    total.value = 0
    totalPages.value = 0
    notify(error.message || 'Failed to load activities', 'error')
  } finally {
    loading.value = false
  }
}

const goCreate = () => {
  router.push({ name: 'merchant-activity-create' })
}

const goEdit = (id) => {
  router.push({
    name: 'merchant-activity-edit',
    params: { id },
    query: route.query
  })
}

const viewBookings = (item) => {
  router.push({ path: '/merchant/bookings', query: { activityId: item.id, title: item.title } })
}

const handleDelete = async (id) => {
  if (!window.confirm('Are you sure you want to delete this activity?')) {
    return
  }

  try {
    const res = await deleteMerchantActivity(id)
    if (res.code !== 200) {
      throw new Error(res.message || 'Failed to delete activity')
    }

    notify('Activity deleted successfully.', 'success')

    if (list.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
      syncRoute()
    }

    await load()
  } catch (error) {
    notify(error.message || 'Failed to delete activity', 'error')
  }
}

const changePage = async (page) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) {
    return
  }
  currentPage.value = page
  syncRoute()
  await load()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  syncRoute()
  await load()
}

const formatDateTime = (value) => value ? new Date(value).toLocaleString('zh-CN') : 'TBD'
const formatLocation = (item) => [item.locationProvince, item.locationCity, item.locationDetail].filter(Boolean).join(' / ') || 'TBD'
const formatPrice = (price) => !price ? 'Free' : `CNY ${(Number(price) / 100).toFixed(2)}`

const statusLabel = (status) => ({
  signup: 'Open',
  full: 'Full',
  ongoing: 'Ongoing',
  ended: 'Ended'
}[status] || 'Draft')

const auditLabel = (status) => ({
  pending: 'Pending Audit',
  approved: 'Approved',
  rejected: 'Rejected'
}[status] || 'Pending Submit')

watch(
  () => route.fullPath,
  async () => {
    syncStateFromRoute()
    await load()
  }
)

onMounted(async () => {
  syncStateFromRoute()
  await load()
})
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

.pagination-card {
  margin-top: 18px;
  padding: 18px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.pagination-summary {
  display: flex;
  align-items: baseline;
  gap: 10px;
  color: #64748b;
}

.pagination-summary strong {
  color: #111827;
  font-size: 24px;
}

.pagination-controls,
.pager-buttons,
.page-size-field {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-size-field span,
.page-indicator {
  color: #64748b;
  font-size: 13px;
}

.page-size-field select,
.pager-btn {
  min-height: 40px;
  border-radius: 10px;
  border: 1px solid #d4d9e0;
  background: #fff;
}

.page-size-field select {
  padding: 0 12px;
}

.pager-btn {
  padding: 0 14px;
  cursor: pointer;
  color: #334155;
}

.pager-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
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
  .page-header,
  .pagination-card {
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

@media (max-width: 640px) {
  .pagination-controls,
  .pager-buttons,
  .page-size-field {
    width: 100%;
    flex-wrap: wrap;
  }

  .pager-btn,
  .page-size-field select {
    width: 100%;
  }
}
</style>
