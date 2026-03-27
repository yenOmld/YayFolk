<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>官方内容管理</h2>
      <button class="create-btn" @click="openCreate">+ 发布内容</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="list.length === 0" class="empty">暂无官方内容，点击右上角发布</div>

    <div v-else class="content-list">
      <div v-for="item in list" :key="item.id" class="content-card">
        <div class="card-main">
          <img v-if="item.coverImage" :src="item.coverImage" class="cover" />
          <div class="card-info">
            <div class="card-title">{{ item.title }}</div>
            <div class="card-body">{{ item.content.substring(0, 120) }}{{ item.content.length > 120 ? '...' : '' }}</div>
            <div class="card-meta">
              <span :class="['cat-tag', item.category]">{{ categoryLabel(item.category) }}</span>
              <span class="time">{{ formatTime(item.createTime) }}</span>
              <span class="views">👁 {{ item.viewCount }}</span>
            </div>
          </div>
        </div>
        <button class="del-btn" @click="handleDelete(item.id)">删除</button>
      </div>
    </div>

    <!-- 发布弹窗 -->
    <div v-if="createModal" class="modal-mask" @click.self="createModal = false">
      <div class="modal">
        <h3>发布官方内容</h3>
        <label>标题</label>
        <input v-model="form.title" placeholder="请输入标题" />
        <label>分类</label>
        <select v-model="form.category">
          <option value="introduction">非遗介绍</option>
          <option value="news">文化资讯</option>
          <option value="announcement">平台公告</option>
        </select>
        <label>封面图（URL）</label>
        <input v-model="form.coverImage" placeholder="可选，填入图片URL" />
        <label>内容</label>
        <textarea v-model="form.content" placeholder="请输入正文内容" rows="6"></textarea>
        <div class="modal-actions">
          <button class="btn cancel" @click="createModal = false">取消</button>
          <button class="btn submit" @click="submitCreate">发布</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { getAdminOfficial, createOfficialContent, deleteOfficialContent } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const list = ref([])
const loading = ref(false)
const createModal = ref(false)
const form = ref({ title: '', content: '', category: 'introduction', coverImage: '' })

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminOfficial()
    list.value = res.data || []
  } catch (e) {
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
    notify('标题和内容不能为空', 'error'); return
  }
  try {
    await createOfficialContent(form.value)
    notify('发布成功', 'success')
    createModal.value = false
    load()
  } catch (e) {
    notify('发布失败', 'error')
  }
}

const handleDelete = async (id) => {
  if (!confirm('确认删除该内容？')) return
  try {
    await deleteOfficialContent(id)
    notify('已删除', 'success')
    list.value = list.value.filter(c => c.id !== id)
  } catch (e) {
    notify('删除失败', 'error')
  }
}

const categoryLabel = (c) => ({ introduction: '非遗介绍', news: '文化资讯', announcement: '平台公告' }[c] || c)
const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : '-'

onMounted(load)
</script>

<style scoped>
.admin-page { max-width: 900px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a2e; margin: 0; }
.create-btn { padding: 9px 20px; background: #7c3aed; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 500; }
.create-btn:hover { background: #6d28d9; }
.loading, .empty { text-align: center; padding: 60px; color: #9ca3af; }
.content-list { display: flex; flex-direction: column; gap: 12px; }
.content-card { background: #fff; border-radius: 12px; padding: 16px 20px; border: 1px solid #e5e7eb; display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.card-main { display: flex; gap: 14px; flex: 1; min-width: 0; }
.cover { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.card-info { flex: 1; min-width: 0; }
.card-title { font-size: 16px; font-weight: 600; color: #111827; margin-bottom: 6px; }
.card-body { font-size: 13px; color: #6b7280; line-height: 1.6; margin-bottom: 8px; }
.card-meta { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.cat-tag { padding: 2px 8px; border-radius: 10px; font-size: 12px; }
.cat-tag.introduction { background: #ede9fe; color: #7c3aed; }
.cat-tag.news { background: #dbeafe; color: #2563eb; }
.cat-tag.announcement { background: #fef3c7; color: #d97706; }
.time { font-size: 12px; color: #9ca3af; }
.views { font-size: 12px; color: #9ca3af; }
.del-btn { padding: 6px 14px; background: #fee2e2; color: #dc2626; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; flex-shrink: 0; }
.del-btn:hover { background: #dc2626; color: #fff; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: #fff; border-radius: 16px; padding: 28px; width: 520px; max-width: 90vw; max-height: 90vh; overflow-y: auto; }
.modal h3 { margin: 0 0 20px; font-size: 18px; }
.modal label { display: block; font-size: 13px; font-weight: 500; color: #374151; margin-bottom: 4px; margin-top: 14px; }
.modal input, .modal select { width: 100%; padding: 9px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; box-sizing: border-box; }
.modal textarea { width: 100%; padding: 10px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; resize: vertical; box-sizing: border-box; }
.modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }
.btn { padding: 9px 20px; border-radius: 8px; border: none; cursor: pointer; font-size: 14px; font-weight: 500; }
.btn.cancel { background: #f3f4f6; color: #374151; }
.btn.submit { background: #7c3aed; color: #fff; }
.btn.submit:hover { background: #6d28d9; }
</style>
