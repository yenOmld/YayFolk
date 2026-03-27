<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>用户管理</h2>
      <div class="search-box">
        <input v-model="keyword" placeholder="搜索用户名/昵称/邮箱" @keyup.enter="load" />
        <button class="search-btn" @click="load">搜索</button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else>
      <div class="stats-bar">共 {{ total }} 名用户</div>
      <div class="table-wrap">
        <table class="user-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>邮箱/手机</th>
              <th>角色</th>
              <th>状态</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in list" :key="u.id">
              <td>
                <div class="user-cell">
                  <span class="uname">{{ u.nickname || u.username }}</span>
                  <span class="usub">@{{ u.username }}</span>
                </div>
              </td>
              <td class="small-text">{{ u.email || u.phone || '-' }}</td>
              <td>
                <span :class="['role-tag', u.role]">{{ roleLabel(u.role) }}</span>
              </td>
              <td>
                <span :class="['status-dot', u.status === 1 ? 'active' : 'banned']">
                  {{ u.status === 1 ? '正常' : '已封禁' }}
                </span>
              </td>
              <td class="small-text">{{ formatTime(u.createTime) }}</td>
              <td>
                <button v-if="u.status === 1" class="action-btn ban" @click="handleBan(u)">封禁</button>
                <button v-else class="action-btn unban" @click="handleUnban(u)">解封</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="total > pageSize">
        <button :disabled="page === 0" @click="changePage(-1)" class="page-btn">上一页</button>
        <span>第 {{ page + 1 }} 页</span>
        <button :disabled="(page + 1) * pageSize >= total" @click="changePage(1)" class="page-btn">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { getAdminUsers, banUser, unbanUser } from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const list = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const page = ref(0)
const pageSize = 20

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminUsers(page.value, pageSize, keyword.value)
    const data = res.data || {}
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    notify('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const changePage = (dir) => { page.value += dir; load() }

const handleBan = async (u) => {
  if (!confirm(`确认封禁用户 ${u.username}？`)) return
  try {
    await banUser(u.id)
    notify('封禁成功', 'success')
    u.status = 0
  } catch (e) { notify('操作失败', 'error') }
}

const handleUnban = async (u) => {
  try {
    await unbanUser(u.id)
    notify('解封成功', 'success')
    u.status = 1
  } catch (e) { notify('操作失败', 'error') }
}

const roleLabel = (r) => ({ user: '普通用户', merchant: '商家', admin: '管理员' }[r] || r)
const formatTime = (t) => t ? new Date(t).toLocaleDateString('zh-CN') : '-'

onMounted(load)
</script>

<style scoped>
.admin-page { max-width: 1000px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a2e; margin: 0; }
.search-box { display: flex; gap: 8px; }
.search-box input { padding: 8px 14px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; width: 220px; }
.search-btn { padding: 8px 16px; background: #7c3aed; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; }
.loading { text-align: center; padding: 60px; color: #9ca3af; }
.stats-bar { font-size: 13px; color: #6b7280; margin-bottom: 12px; }
.table-wrap { overflow-x: auto; background: #fff; border-radius: 12px; border: 1px solid #e5e7eb; }
.user-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.user-table th { padding: 12px 16px; text-align: left; background: #f9fafb; font-weight: 600; color: #374151; border-bottom: 1px solid #e5e7eb; }
.user-table td { padding: 12px 16px; border-bottom: 1px solid #f3f4f6; color: #374151; }
.user-table tr:last-child td { border-bottom: none; }
.user-cell { display: flex; flex-direction: column; gap: 2px; }
.uname { font-weight: 500; }
.usub { font-size: 12px; color: #9ca3af; }
.small-text { font-size: 13px; color: #6b7280; }
.role-tag { padding: 2px 8px; border-radius: 10px; font-size: 12px; }
.role-tag.user { background: #f3f4f6; color: #6b7280; }
.role-tag.merchant { background: #dbeafe; color: #2563eb; }
.role-tag.admin { background: #fce7f3; color: #db2777; }
.status-dot { font-size: 13px; }
.status-dot.active { color: #059669; }
.status-dot.banned { color: #dc2626; }
.action-btn { padding: 5px 12px; border-radius: 6px; border: none; cursor: pointer; font-size: 12px; font-weight: 500; }
.action-btn.ban { background: #fee2e2; color: #dc2626; }
.action-btn.ban:hover { background: #dc2626; color: #fff; }
.action-btn.unban { background: #d1fae5; color: #059669; }
.action-btn.unban:hover { background: #059669; color: #fff; }
.pagination { display: flex; align-items: center; gap: 12px; justify-content: center; margin-top: 20px; font-size: 14px; color: #6b7280; }
.page-btn { padding: 6px 14px; border: 1px solid #d1d5db; background: #fff; border-radius: 8px; cursor: pointer; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
