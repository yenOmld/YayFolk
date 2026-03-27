<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>内容审核</h2>
      <span class="count-badge">待审：{{ list.length }} 条</span>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="list.length === 0" class="empty">暂无待审内容 🎉</div>

    <div v-else class="post-list">
      <div v-for="post in list" :key="post.id" class="post-card">
        <div class="post-meta">
          <span class="user-name">{{ post.nickname || post.username }}</span>
          <span class="post-time">{{ formatTime(post.createTime) }}</span>
          <span :class="['audit-badge', post.auditStatus]">{{ auditLabel(post.auditStatus) }}</span>
        </div>
        <div class="post-title">{{ post.title }}</div>
        <div class="post-content">{{ post.content }}</div>
        <div v-if="post.images" class="post-images">
          <img v-for="(img, i) in parseImages(post.images)" :key="i" :src="img" class="thumb" />
        </div>
        <div class="post-actions">
          <button class="btn approve" @click="handleAudit(post.id, true)">✓ 通过</button>
          <button class="btn reject" @click="handleAudit(post.id, false)">✗ 拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { getAdminPendingPosts, auditPost } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const list = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminPendingPosts()
    list.value = res.data || []
  } catch (e) {
    notify('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const handleAudit = async (id, pass) => {
  try {
    await auditPost(id, pass)
    notify(pass ? '已通过' : '已拒绝', 'success')
    list.value = list.value.filter(p => p.id !== id)
  } catch (e) {
    notify('操作失败', 'error')
  }
}

const parseImages = (images) => {
  try { return JSON.parse(images) } catch { return [] }
}
const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : '-'
const auditLabel = (s) => ({ pending: '待审核', passed: '已通过', rejected: '已拒绝' }[s] || s)

onMounted(load)
</script>

<style scoped>
.admin-page { max-width: 800px; }
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a2e; margin: 0; }
.count-badge { background: #fee2e2; color: #dc2626; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.loading, .empty { text-align: center; padding: 60px; color: #9ca3af; }
.post-list { display: flex; flex-direction: column; gap: 16px; }
.post-card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); border: 1px solid #e5e7eb; }
.post-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; flex-wrap: wrap; }
.user-name { font-weight: 600; font-size: 14px; color: #374151; }
.post-time { font-size: 12px; color: #9ca3af; }
.audit-badge { padding: 2px 8px; border-radius: 10px; font-size: 12px; }
.audit-badge.pending { background: #fef3c7; color: #d97706; }
.audit-badge.passed { background: #d1fae5; color: #059669; }
.audit-badge.rejected { background: #fee2e2; color: #dc2626; }
.post-title { font-size: 16px; font-weight: 600; color: #111827; margin-bottom: 8px; }
.post-content { font-size: 14px; color: #6b7280; line-height: 1.6; margin-bottom: 12px; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.post-images { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.thumb { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; border: 1px solid #e5e7eb; }
.post-actions { display: flex; gap: 10px; border-top: 1px solid #f3f4f6; padding-top: 14px; }
.btn { padding: 7px 18px; border-radius: 8px; border: none; cursor: pointer; font-size: 13px; font-weight: 500; transition: all 0.2s; }
.btn.approve { background: #059669; color: #fff; }
.btn.approve:hover { background: #047857; }
.btn.reject { background: #dc2626; color: #fff; }
.btn.reject:hover { background: #b91c1c; }
</style>
