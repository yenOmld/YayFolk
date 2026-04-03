﻿<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2>用户管理</h2>
        <div class="tab-switcher">
          <button 
            :class="['tab-btn', activeTab === 'users' ? 'active' : '']" 
            @click="switchTab('users')"
          >
            用户管理
          </button>
          <button 
            :class="['tab-btn', activeTab === 'appeals' ? 'active' : '']" 
            @click="switchTab('appeals')"
          >
            解封申请审核
          </button>
        </div>
      </div>
      <div class="search-box" v-if="activeTab === 'users'">
        <input v-model.trim="keyword" placeholder="搜索用户名/昵称/邮箱" @keyup.enter="reloadUsers" />
        <button class="search-btn" @click="reloadUsers">搜索</button>
      </div>
      <div class="search-box" v-if="activeTab === 'appeals'">
        <select v-model="appealStatus" @change="loadApplications">
          <option value="pending">待处理</option>
          <option value="approved">已通过</option>
          <option value="rejected">已驳回</option>
          <option value="">全部</option>
        </select>
        <button class="search-btn" @click="loadApplications">刷新</button>
      </div>
    </div>

    <div v-if="activeTab === 'users'">
      <div v-if="loadingUsers" class="loading">加载中...</div>
      <div v-else>
        <div class="stats-bar">共 {{ total }} 名用户</div>
        <div class="table-wrap">
          <table class="user-table">
            <thead>
              <tr>
                <th>用户</th>
                <th>邮箱/手机</th>
                <th>状态</th>
                <th>封禁原因</th>
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
                  <span :class="['status-dot', u.status === 1 ? 'active' : 'banned']">
                    {{ u.status === 1 ? '正常' : '已封禁' }}
                  </span>
                </td>
                <td class="reason-cell" :title="u.banReason || ''">{{ u.banReason || '-' }}</td>
                <td class="small-text">{{ formatTime(u.createTime) }}</td>
                <td>
                  <button v-if="u.status === 1" class="action-btn ban" @click="openBanModal(u)">封禁</button>
                  <button v-else class="action-btn unban" @click="openUnbanModal(u)">直接解封</button>
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

    <div v-if="activeTab === 'appeals'" class="appeal-section">
      <div v-if="loadingApplications" class="loading">加载申请中...</div>
      <div v-else class="table-wrap">
        <table class="user-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>申请原因</th>
              <th>申请时间</th>
              <th>状态</th>
              <th>管理员备注</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in applications" :key="item.id">
              <td>
                <div class="user-cell">
                  <span class="uname">{{ item.nickname || item.username || '-' }}</span>
                  <span class="usub">@{{ item.username || '-' }}</span>
                </div>
              </td>
              <td class="reason-cell" :title="item.applyReason || ''">{{ item.applyReason || '-' }}</td>
              <td class="small-text">{{ formatTime(item.createTime) }}</td>
              <td>
                <span :class="['status-chip', item.status]">{{ appealStatusLabel(item.status) }}</span>
              </td>
              <td class="reason-cell" :title="item.adminRemark || ''">{{ item.adminRemark || '-' }}</td>
              <td>
                <template v-if="item.status === 'pending'">
                  <button class="action-btn unban" @click="openApproveModal(item)">通过并解封</button>
                  <button class="action-btn ban" @click="openRejectModal(item)">驳回</button>
                </template>
                <span v-else class="small-text">已处理</span>
              </td>
            </tr>
            <tr v-if="!applications.length">
              <td colspan="6" class="empty-cell">暂无申请记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 封禁用户弹窗 -->
    <div v-if="banModal.show" class="modal-mask" @click.self="closeBanModal">
      <div class="modal">
        <h3>封禁用户</h3>
        <p class="modal-intro">{{ banModal.user?.username }}</p>
        <div class="modal-field">
          <label class="field-label">封禁原因</label>
          <textarea 
            v-model.trim="banModal.reason" 
            rows="4" 
            placeholder="请输入封禁原因"
          ></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn cancel" @click="closeBanModal">取消</button>
          <button class="btn ban" @click="submitBan">确认封禁</button>
        </div>
      </div>
    </div>

    <!-- 解封用户弹窗 -->
    <div v-if="unbanModal.show" class="modal-mask" @click.self="closeUnbanModal">
      <div class="modal">
        <h3>确认解封</h3>
        <p class="modal-intro">确定要直接解封用户 {{ unbanModal.user?.username }} 吗？</p>
        <div class="modal-actions">
          <button class="btn cancel" @click="closeUnbanModal">取消</button>
          <button class="btn unban" @click="submitUnban">确认解封</button>
        </div>
      </div>
    </div>

    <!-- 通过解封申请弹窗 -->
    <div v-if="approveModal.show" class="modal-mask" @click.self="closeApproveModal">
      <div class="modal">
        <h3>通过解封申请</h3>
        <p class="modal-intro">{{ approveModal.item?.username }}</p>
        <div class="modal-field">
          <label class="field-label">解封备注（选填）</label>
          <textarea 
            v-model.trim="approveModal.remark" 
            rows="3" 
            placeholder="可填写解封备注，留空可直接通过"
          ></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn cancel" @click="closeApproveModal">取消</button>
          <button class="btn unban" @click="submitApprove">确认通过</button>
        </div>
      </div>
    </div>

    <!-- 驳回解封申请弹窗 -->
    <div v-if="rejectModal.show" class="modal-mask" @click.self="closeRejectModal">
      <div class="modal">
        <h3>驳回解封申请</h3>
        <p class="modal-intro">{{ rejectModal.item?.username }}</p>
        <div class="modal-field">
          <label class="field-label">驳回原因</label>
          <textarea 
            v-model.trim="rejectModal.remark" 
            rows="4" 
            placeholder="请输入驳回原因"
          ></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn cancel" @click="closeRejectModal">取消</button>
          <button class="btn ban" @click="submitReject">确认驳回</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import {
  getAdminUsers,
  banUser,
  unbanUser,
  getAdminUnbanApplications,
  auditAdminUnbanApplication
} from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const list = ref([])
const total = ref(0)
const loadingUsers = ref(false)
const keyword = ref('')
const page = ref(0)
const pageSize = 10

const applications = ref([])
const loadingApplications = ref(false)
const appealStatus = ref('pending')
const activeTab = ref('users')

// 模态框状态
const banModal = ref({
  show: false,
  user: null,
  reason: ''
})

const unbanModal = ref({
  show: false,
  user: null
})

const approveModal = ref({
  show: false,
  item: null,
  remark: ''
})

const rejectModal = ref({
  show: false,
  item: null,
  remark: ''
})

const switchTab = (tab) => {
  activeTab.value = tab
}

const notifyMsg = (type, message) => notify?.[type]?.(message)

const formatTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN')
}

const appealStatusLabel = (status) => ({
  pending: '待处理',
  approved: '已通过',
  rejected: '已驳回'
}[status] || status || '-')

const reloadUsers = async () => {
  page.value = 0
  await loadUsers()
}

const loadUsers = async () => {
  loadingUsers.value = true
  try {
    const res = await getAdminUsers(page.value, pageSize, keyword.value)
    const data = res.data || {}
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    notifyMsg('error', '加载用户失败')
  } finally {
    loadingUsers.value = false
  }
}

const changePage = async (delta) => {
  page.value += delta
  await loadUsers()
}

const loadApplications = async () => {
  loadingApplications.value = true
  try {
    const res = await getAdminUnbanApplications(appealStatus.value || undefined)
    applications.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    notifyMsg('error', '加载解封申请失败')
  } finally {
    loadingApplications.value = false
  }
}

// 打开封禁模态框
const openBanModal = (user) => {
  banModal.value = {
    show: true,
    user,
    reason: user.banReason || ''
  }
}

// 关闭封禁模态框
const closeBanModal = () => {
  banModal.value = {
    show: false,
    user: null,
    reason: ''
  }
}

// 提交封禁
const submitBan = async () => {
  const { user, reason } = banModal.value
  if (!user) return
  
  const trimmedReason = reason.trim()
  if (!trimmedReason) {
    notifyMsg('warning', '封禁原因不能为空')
    return
  }

  try {
    const res = await banUser(user.id, trimmedReason)
    if (res.code !== 200) {
      throw new Error(res.message || '封禁失败')
    }
    user.status = 0
    user.banReason = trimmedReason
    notifyMsg('success', '封禁成功，已通知用户邮箱')
    closeBanModal()
    await loadApplications()
  } catch (error) {
    notifyMsg('error', error.message || '封禁失败')
  }
}

// 打开解封模态框
const openUnbanModal = (user) => {
  unbanModal.value = {
    show: true,
    user
  }
}

// 关闭解封模态框
const closeUnbanModal = () => {
  unbanModal.value = {
    show: false,
    user: null
  }
}

// 提交解封
const submitUnban = async () => {
  const { user } = unbanModal.value
  if (!user) return

  try {
    const res = await unbanUser(user.id)
    if (res.code !== 200) {
      throw new Error(res.message || '解封失败')
    }
    user.status = 1
    user.banReason = null
    notifyMsg('success', '解封成功，已通知用户邮箱')
    closeUnbanModal()
    await loadApplications()
  } catch (error) {
    notifyMsg('error', error.message || '解封失败')
  }
}

// 打开通过申请模态框
const openApproveModal = (item) => {
  approveModal.value = {
    show: true,
    item,
    remark: item.adminRemark || ''
  }
}

// 关闭通过申请模态框
const closeApproveModal = () => {
  approveModal.value = {
    show: false,
    item: null,
    remark: ''
  }
}

// 提交通过申请
const submitApprove = async () => {
  const { item, remark } = approveModal.value
  if (!item) return

  try {
    const res = await auditAdminUnbanApplication(item.id, true, remark.trim())
    if (res.code !== 200) {
      throw new Error(res.message || '处理申请失败')
    }
    notifyMsg('success', '申请已通过，用户已解封并发送邮件通知')
    closeApproveModal()
    await loadApplications()
    await loadUsers()
  } catch (error) {
    notifyMsg('error', error.message || '处理申请失败')
  }
}

// 打开驳回申请模态框
const openRejectModal = (item) => {
  rejectModal.value = {
    show: true,
    item,
    remark: item.adminRemark || ''
  }
}

// 关闭驳回申请模态框
const closeRejectModal = () => {
  rejectModal.value = {
    show: false,
    item: null,
    remark: ''
  }
}

// 提交驳回申请
const submitReject = async () => {
  const { item, remark } = rejectModal.value
  if (!item) return

  const trimmedRemark = remark.trim()
  if (!trimmedRemark) {
    notifyMsg('warning', '驳回原因不能为空')
    return
  }

  try {
    const res = await auditAdminUnbanApplication(item.id, false, trimmedRemark)
    if (res.code !== 200) {
      throw new Error(res.message || '驳回失败')
    }
    notifyMsg('success', '已驳回申请')
    closeRejectModal()
    await loadApplications()
  } catch (error) {
    notifyMsg('error', error.message || '驳回失败')
  }
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadApplications()])
})
</script>

<style scoped>
.admin-page {
  max-width: 1180px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.tab-switcher {
  display: flex;
  border-radius: 8px;
  background: #f3f4f6;
  padding: 2px;
}

.tab-btn {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  color: #374151;
}

.tab-btn.active {
  background: #fff;
  color: #1d4ed8;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.search-box {
  display: flex;
  gap: 8px;
}

.search-box input {
  padding: 8px 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  width: 240px;
}

.search-box select {
  padding: 8px 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  min-width: 120px;
  color: #374151;
  cursor: pointer;
  appearance: auto;
}

.search-box select:focus {
  outline: none;
  border-color: #1d4ed8;
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.1);
}

.search-box select option {
  padding: 8px 12px;
  font-size: 14px;
  color: #374151;
  background: #fff;
}

.search-btn {
  padding: 8px 16px;
  background: #1d4ed8;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.loading {
  text-align: center;
  padding: 28px;
  color: #6b7280;
}

.stats-bar {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 12px;
}

.table-wrap {
  overflow-x: auto;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.user-table th {
  padding: 12px 16px;
  text-align: left;
  background: #f8fafc;
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
}

.user-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
  color: #374151;
  vertical-align: top;
}

.user-table tr:last-child td {
  border-bottom: none;
}

.user-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.uname {
  font-weight: 500;
}

.usub {
  font-size: 12px;
  color: #9ca3af;
}

.small-text {
  font-size: 13px;
  color: #6b7280;
}

.reason-cell {
  max-width: 280px;
  color: #4b5563;
  word-break: break-word;
}

.status-dot {
  font-size: 13px;
  font-weight: 600;
}

.status-dot.active {
  color: #059669;
}

.status-dot.banned {
  color: #dc2626;
}

.status-chip {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-chip.pending {
  background: #fff7ed;
  color: #c2410c;
}

.status-chip.approved {
  background: #dcfce7;
  color: #166534;
}

.status-chip.rejected {
  background: #fee2e2;
  color: #b91c1c;
}

.action-btn {
  padding: 5px 12px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  margin-right: 8px;
  margin-bottom: 8px;
}

.action-btn.ban {
  background: #fee2e2;
  color: #dc2626;
}

.action-btn.unban {
  background: #d1fae5;
  color: #059669;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
  margin-top: 20px;
  font-size: 14px;
  color: #6b7280;
}

.page-btn {
  padding: 6px 14px;
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.appeal-section {
  margin-top: 20px;
}

.empty-cell {
  text-align: center;
  color: #9ca3af;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    width: 100%;
  }

  .tab-switcher {
    width: 100%;
  }

  .tab-btn {
    flex: 1;
    text-align: center;
  }

  .search-box {
    width: 100%;
  }

  .search-box input {
    width: 100%;
  }

  .search-box select {
    width: 100%;
  }

  .reason-cell {
    max-width: 180px;
  }
}

/* 模态框样式 */
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
  border-radius: 12px;
  padding: 24px;
  width: 480px;
  max-width: 90vw;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

.modal h3 {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.modal-intro {
  margin: 0 0 20px;
  color: #6b7280;
  font-size: 14px;
}

.modal-field {
  margin-bottom: 20px;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.modal textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 12px;
  min-height: 100px;
  resize: vertical;
  font-size: 14px;
  line-height: 1.5;
}

.modal textarea:focus {
  outline: none;
  border-color: #1d4ed8;
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.1);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal .btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.modal .btn.cancel {
  background: #f3f4f6;
  color: #374151;
}

.modal .btn.ban {
  background: #fee2e2;
  color: #dc2626;
}

.modal .btn.unban {
  background: #d1fae5;
  color: #059669;
}

.modal .btn:hover {
  opacity: 0.9;
}
</style>
