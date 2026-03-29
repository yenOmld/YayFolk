<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>内容审核</h2>
        <p class="page-subtitle">在这里管理用户帖子，待审核内容可直接处理，已通过内容也支持再次下架。</p>
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
          <input v-model.trim="keyword" type="text" placeholder="搜索标题/内容/用户/分类/驳回原因" />
        </div>
        <span class="count-badge">{{ countLabel }} {{ filteredList.length }} 条</span>
      </div>
    </div>

    <div v-if="loading" class="loading">{{ loadingText }}</div>
    <div v-else-if="filteredList.length === 0" class="empty">{{ emptyText }}</div>

    <div v-else class="post-list">
      <article v-for="post in pagedList" :key="post.id" class="post-card">
        <div class="post-meta">
          <div class="meta-main">
            <span class="user-name">{{ post.nickname || post.username || '匿名用户' }}</span>
            <span class="post-time">{{ formatTime(post.createTime) }}</span>
          </div>
          <div class="meta-side">
            <span v-if="post.category" class="category-tag">{{ post.category }}</span>
            <span :class="['audit-badge', post.auditStatus]">{{ auditLabel(post.auditStatus) }}</span>
          </div>
        </div>

        <h3 class="post-title">{{ post.title || '未命名帖子' }}</h3>
        <p class="post-content">{{ post.content || '暂无正文内容' }}</p>

        <div v-if="parseImages(post.images).length" class="post-images">
          <img
            v-for="(image, index) in parseImages(post.images)"
            :key="`${post.id}-${index}`"
            :src="image"
            class="thumb"
            alt="帖子图片"
          />
        </div>

        <div v-if="post.auditRemark" class="remark-block">
          <span class="remark-label">驳回原因</span>
          <p class="remark-text">{{ post.auditRemark }}</p>
        </div>

        <div v-if="post.auditStatus === 'pending' || post.auditStatus === 'passed'" class="post-actions">
          <button v-if="post.auditStatus === 'pending'" class="btn approve" @click="handleApprove(post.id)">通过</button>
          <button class="btn reject" @click="openReject(post)">{{ post.auditStatus === 'passed' ? '下架' : '驳回' }}</button>
        </div>
      </article>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="page === 1" @click="changePage(-1)">上一页</button>
      <span class="page-status">第 {{ page }} / {{ totalPages }} 页</span>
      <button class="page-btn" :disabled="page === totalPages" @click="changePage(1)">下一页</button>
    </div>

    <div v-if="rejectModal.show" class="modal-mask" @click.self="closeRejectModal">
      <div class="modal">
        <h3>{{ rejectModal.post?.auditStatus === 'passed' ? '填写下架原因' : '填写驳回原因' }}</h3>
        <p class="modal-intro">{{ rejectModal.post?.title || '未命名帖子' }}</p>

        <div class="modal-field">
          <label class="field-label">{{ rejectModal.post?.auditStatus === 'passed' ? '下架原因' : '驳回原因' }}</label>
          <textarea
            v-model.trim="rejectModal.remark"
            rows="4"
            :placeholder="rejectModal.post?.auditStatus === 'passed' ? '请填写下架原因，方便用户了解处理结果' : '请填写驳回原因，方便用户修改后重新提交'"
          />
          <small class="field-hint">通过不需要填写原因，只有驳回或下架时必须填写。</small>
        </div>

        <div class="modal-actions">
          <button class="btn cancel" @click="closeRejectModal">取消</button>
          <button class="btn reject" @click="submitReject">确认{{ rejectModal.post?.auditStatus === 'passed' ? '下架' : '驳回' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from 'vue'
import { auditPost, getAdminPosts } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (message, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(message)

const tabs = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'passed' },
  { label: '已驳回', value: 'rejected' }
]

const currentTab = ref('pending')
const list = ref([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = 4
const rejectModal = ref({
  show: false,
  post: null,
  remark: ''
})

const filteredList = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  if (!search) {
    return list.value
  }
  return list.value.filter(post => {
    const fields = [
      post.title,
      post.content,
      post.username,
      post.nickname,
      post.category,
      post.auditRemark
    ]
    return fields.some(field => String(field || '').toLowerCase().includes(search))
  })
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredList.value.length / pageSize)))
const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredList.value.slice(start, start + pageSize)
})
const currentTabLabel = computed(() => tabs.find(tab => tab.value === currentTab.value)?.label || '全部')
const countLabel = computed(() => currentTab.value ? currentTabLabel.value : '全部内容')
const loadingText = computed(() => `正在加载${currentTab.value ? currentTabLabel.value : '内容列表'}...`)
const emptyText = computed(() => keyword.value ? `没有匹配的${currentTab.value ? currentTabLabel.value : '内容'}帖子` : (currentTab.value ? `当前没有${currentTabLabel.value}帖子` : '当前没有内容记录'))

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminPosts(currentTab.value, 0, 200)
    if (res.code !== 200) {
      throw new Error(res.message || '加载帖子失败')
    }
    list.value = Array.isArray(res.data) ? res.data : []
    page.value = 1
  } catch (error) {
    list.value = []
    notify(error.message || '加载帖子失败', 'error')
  } finally {
    loading.value = false
  }
}

const switchTab = (value) => {
  currentTab.value = value
  page.value = 1
  load()
}

const handleApprove = async (id) => {
  try {
    const res = await auditPost(id, true)
    if (res.code !== 200) {
      throw new Error(res.message || '审核操作失败')
    }
    notify('已通过审核', 'success')
    await load()
  } catch (error) {
    notify(error.message || '审核操作失败', 'error')
  }
}

const openReject = (post) => {
  rejectModal.value = {
    show: true,
    post,
    remark: ''
  }
}

const closeRejectModal = () => {
  rejectModal.value = {
    show: false,
    post: null,
    remark: ''
  }
}

const submitReject = async () => {
  const post = rejectModal.value.post
  const remark = rejectModal.value.remark.trim()
  if (!post) {
    return
  }
  if (!remark) {
    notify(`请先填写${post.auditStatus === 'passed' ? '下架原因' : '驳回原因'}`, 'warning')
    return
  }

  try {
    const res = await auditPost(post.id, false, remark)
    if (res.code !== 200) {
      throw new Error(res.message || '处理失败')
    }
    closeRejectModal()
    notify(post.auditStatus === 'passed' ? '已下架内容' : '已驳回内容', 'success')
    await load()
  } catch (error) {
    notify(error.message || '处理失败', 'error')
  }
}

const changePage = (step) => {
  const nextPage = page.value + step
  if (nextPage < 1 || nextPage > totalPages.value) {
    return
  }
  page.value = nextPage
}

const parseImages = (images) => {
  if (Array.isArray(images)) {
    return images
  }
  if (!images) {
    return []
  }
  try {
    const parsed = JSON.parse(images)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN')
}

const auditLabel = (status) => {
  const map = {
    pending: '待审核',
    passed: '已通过',
    rejected: '已驳回'
  }
  return map[status] || status || '待审核'
}

watch(keyword, () => {
  page.value = 1
})

onMounted(load)
</script>

<style scoped>
.admin-page {
  width: 100%;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.header-side {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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

.tab-btn {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #475569;
  cursor: pointer;
}

.tab-btn.active {
  background: #0f766e;
  border-color: #0f766e;
  color: #fff;
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: #fff1f2;
  color: #be123c;
  font-size: 13px;
  font-weight: 600;
}

.loading,
.empty {
  padding: 72px 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 15px;
}

.post-list {
  display: grid;
  gap: 18px;
}

.post-card {
  padding: 22px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.post-meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.meta-main,
.meta-side {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.user-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.post-time {
  font-size: 13px;
  color: #64748b;
}

.category-tag,
.audit-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.category-tag {
  background: #eff6ff;
  color: #2563eb;
}

.audit-badge.pending {
  background: #fef3c7;
  color: #b45309;
}

.audit-badge.passed {
  background: #dcfce7;
  color: #15803d;
}

.audit-badge.rejected {
  background: #fee2e2;
  color: #b91c1c;
}

.post-title {
  margin: 0 0 10px;
  font-size: 18px;
  line-height: 1.5;
  color: #0f172a;
}

.post-content {
  margin: 0;
  color: #475569;
  line-height: 1.75;
  white-space: pre-wrap;
}

.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.thumb {
  width: 88px;
  height: 88px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.remark-block {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
}

.remark-label {
  display: block;
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.remark-text {
  margin: 0;
  color: #b91c1c;
  line-height: 1.7;
}

.post-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.btn,
.page-btn {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s ease;
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
  background: #e2e8f0;
  color: #334155;
}

.btn.cancel:hover {
  background: #cbd5e1;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}

.page-btn {
  background: #fff;
  border: 1px solid #dbe2ea;
  color: #334155;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.page-status {
  color: #64748b;
  font-size: 14px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 60;
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

.modal textarea:focus {
  outline: none;
  border-color: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.12);
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
  .page-header,
  .post-meta {
    flex-direction: column;
  }

  .header-side {
    width: 100%;
    justify-content: flex-start;
  }

  .thumb {
    width: 76px;
    height: 76px;
  }
}
</style>
