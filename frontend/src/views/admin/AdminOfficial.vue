<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>官方内容管理</h2>
        <p class="page-subtitle">维护前台展示的官方内容，发布前建议先核对标题、分类与正文摘要。</p>
      </div>
      <button class="create-btn" @click="openCreate">+ 发布内容</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="list.length === 0" class="empty">暂无官方内容，点击右上角开始发布</div>

    <div v-else class="content-list">
      <div v-for="item in pagedList" :key="item.id" class="content-card">
        <div class="card-main">
          <img v-if="item.coverImage" :src="item.coverImage" class="cover" alt="内容封面" />
          <div class="card-info">
            <div class="card-title">{{ item.title }}</div>
            <div class="card-body">{{ item.content.substring(0, 120) }}{{ item.content.length > 120 ? '...' : '' }}</div>
            <div class="card-meta">
              <span :class="['cat-tag', item.category]">{{ categoryLabel(item.category) }}</span>
              <span class="time">{{ formatTime(item.createTime) }}</span>
              <span class="views">浏览 {{ item.viewCount }}</span>
            </div>
          </div>
        </div>
        <button class="del-btn" @click="handleDelete(item.id)">删除</button>
      </div>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="page === 1" @click="changePage(-1)">上一页</button>
      <span class="page-status">第 {{ page }} / {{ totalPages }} 页</span>
      <button class="page-btn" :disabled="page === totalPages" @click="changePage(1)">下一页</button>
    </div>

    <div v-if="createModal" class="modal-mask" @click.self="createModal = false">
      <div class="modal">
        <h3>发布官方内容</h3>
        <p class="modal-intro">发布后会直接出现在前台官方内容区，建议先检查封面链接和正文排版。</p>

        <div class="modal-form">
          <div class="modal-field">
            <label>标题</label>
            <input v-model="form.title" placeholder="请输入标题" />
            <small class="modal-hint">标题会出现在列表卡片和详情入口中，尽量简洁明确。</small>
          </div>

          <div class="modal-field">
            <label>分类</label>
            <select v-model="form.category">
              <option value="introduction">非遗介绍</option>
              <option value="news">文化资讯</option>
              <option value="announcement">平台公告</option>
            </select>
          </div>

          <div class="modal-field">
            <label>封面图 URL</label>
            <input v-model="form.coverImage" placeholder="可选，填入图片 URL" />
            <small class="modal-hint">如果不填写，前台会以无图卡片形式展示。</small>
          </div>

          <div class="modal-field">
            <label>正文内容</label>
            <textarea v-model="form.content" placeholder="请输入正文内容" rows="7"></textarea>
            <small class="modal-hint">建议分段录入，提升前台阅读体验。</small>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn cancel" @click="createModal = false">取消</button>
          <button class="btn submit" @click="submitCreate">发布</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { createOfficialContent, deleteOfficialContent, getAdminOfficial } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const list = ref([])
const loading = ref(false)
const createModal = ref(false)
const form = ref({ title: '', content: '', category: 'introduction', coverImage: '' })
const page = ref(1)
const pageSize = 3

const totalPages = computed(() => Math.max(1, Math.ceil(list.value.length / pageSize)))
const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize
  return list.value.slice(start, start + pageSize)
})

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminOfficial()
    list.value = res.data || []
    page.value = 1
  } catch (error) {
    notify('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  form.value = { title: '', content: '', category: 'introduction', coverImage: '' }
  createModal.value = true
}

const submitCreate = async () => {
  if (!form.value.title || !form.value.content) {
    notify('标题和内容不能为空', 'error')
    return
  }

  try {
    await createOfficialContent(form.value)
    notify('发布成功', 'success')
    createModal.value = false
    load()
  } catch (error) {
    notify('发布失败', 'error')
  }
}

const handleDelete = async (id) => {
  if (!confirm('确认删除该内容吗？')) {
    return
  }

  try {
    await deleteOfficialContent(id)
    notify('已删除', 'success')
    list.value = list.value.filter((item) => item.id !== id)
    if (page.value > totalPages.value) {
      page.value = totalPages.value
    }
  } catch (error) {
    notify('删除失败', 'error')
  }
}

const changePage = (step) => {
  const nextPage = page.value + step
  if (nextPage < 1 || nextPage > totalPages.value) {
    return
  }
  page.value = nextPage
}

const categoryLabel = (category) => ({
  introduction: '非遗介绍',
  news: '文化资讯',
  announcement: '平台公告'
}[category] || category)

const formatTime = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '-')

onMounted(load)
</script>

<style scoped>
.admin-page {
  max-width: 940px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #6b7280;
  line-height: 1.7;
}

.create-btn {
  padding: 10px 20px;
  background: #7c3aed;
  color: #fff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.create-btn:hover {
  background: #6d28d9;
}

.loading,
.empty {
  text-align: center;
  padding: 60px;
  color: #9ca3af;
}

.content-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
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

.content-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-main {
  display: flex;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.cover {
  width: 88px;
  height: 88px;
  object-fit: cover;
  border-radius: 10px;
  flex-shrink: 0;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 6px;
}

.card-body {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.7;
  margin-bottom: 10px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.cat-tag {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.cat-tag.introduction {
  background: #ede9fe;
  color: #7c3aed;
}

.cat-tag.news {
  background: #dbeafe;
  color: #2563eb;
}

.cat-tag.announcement {
  background: #fef3c7;
  color: #d97706;
}

.time,
.views {
  font-size: 12px;
  color: #9ca3af;
}

.del-btn {
  padding: 8px 14px;
  background: #fee2e2;
  color: #dc2626;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  flex-shrink: 0;
}

.del-btn:hover {
  background: #dc2626;
  color: #fff;
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
  width: 560px;
  max-width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
}

.modal h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

.modal-intro {
  margin: 0 0 18px;
  font-size: 14px;
  line-height: 1.7;
  color: #6b7280;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.modal label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.modal-hint {
  font-size: 12px;
  line-height: 1.6;
  color: #94a3b8;
}

.modal input,
.modal select {
  width: 100%;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
  box-sizing: border-box;
}

.modal textarea {
  width: 100%;
  min-height: 160px;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
  line-height: 1.7;
}

.modal input:focus,
.modal select:focus,
.modal textarea:focus {
  outline: none;
  border-color: #7c3aed;
  box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.12);
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.btn {
  padding: 9px 20px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.btn.cancel {
  background: #f3f4f6;
  color: #374151;
}

.btn.submit {
  background: #7c3aed;
  color: #fff;
}

.btn.submit:hover {
  background: #6d28d9;
}
</style>
