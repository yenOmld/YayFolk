<template>
  <div class="admin-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Super Admin Only</p>
        <h2>管理员管理</h2>
      </div>
      <button class="primary-btn" @click="openCreateDialog">新增管理员</button>
    </section>

    <div v-if="loading" class="loading-card">正在加载管理员列表...</div>

    <section v-else class="table-card">
      <div class="table-header">
        <span>共 {{ admins.length }} 个管理员账号</span>
      </div>

      <div class="table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>账号</th>
              <th>昵称</th>
              <th>邮箱</th>
              <th>身份</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pagedAdmins" :key="item.id">
              <td>
                <div class="cell-stack">
                  <strong>{{ item.username }}</strong>
                  <small>@{{ item.username }}</small>
                </div>
              </td>
              <td>{{ item.nickname || '-' }}</td>
              <td>{{ item.email || '-' }}</td>
              <td>
                <span :class="['role-tag', Number(item.isSuperAdmin) === 1 ? 'super' : 'normal']">
                  {{ Number(item.isSuperAdmin) === 1 ? '超级管理员' : '管理员' }}
                </span>
              </td>
              <td>
                <span :class="['status-tag', item.status === 1 ? 'active' : 'disabled']">
                  {{ item.status === 1 ? '正常' : '已停用' }}
                </span>
              </td>
              <td>{{ formatTime(item.createTime) }}</td>
              <td>
                <div class="action-group">
                  <button class="ghost-btn" @click="openEditDialog(item)">编辑</button>
                  <button class="ghost-btn warn" @click="openPasswordDialog(item)">改密码</button>
                  <button
                    class="ghost-btn danger"
                    :disabled="Number(item.isSuperAdmin) === 1"
                    @click="handleDelete(item)"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="admins.length === 0">
              <td colspan="7" class="empty-text">暂无管理员数据</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" :disabled="page === 1" @click="changePage(-1)">上一页</button>
        <span class="page-status">第 {{ page }} / {{ totalPages }} 页</span>
        <button class="page-btn" :disabled="page === totalPages" @click="changePage(1)">下一页</button>
      </div>
    </section>

    <div v-if="createVisible" class="dialog-mask" @click.self="closeCreateDialog">
      <div class="dialog-card">
        <div class="dialog-header">
          <h3>新增管理员</h3>
          <button class="close-btn" @click="closeCreateDialog">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>管理员账号</span>
            <input v-model.trim="createForm.username" placeholder="请输入管理员账号" />
          </label>
          <label>
            <span>昵称</span>
            <input v-model.trim="createForm.nickname" placeholder="请输入昵称" />
          </label>
          <label>
            <span>邮箱</span>
            <input v-model.trim="createForm.email" placeholder="请输入邮箱" />
          </label>
          <label>
            <span>初始密码</span>
            <input v-model="createForm.password" type="password" placeholder="请输入初始密码" />
          </label>
          <label>
            <span>确认密码</span>
            <input v-model="createForm.confirmPassword" type="password" placeholder="请再次输入密码" />
          </label>
        </div>
        <div class="dialog-actions">
          <button class="subtle-btn" @click="closeCreateDialog">取消</button>
          <button class="primary-btn" :disabled="submitting" @click="handleCreate">
            {{ submitting ? '提交中...' : '确认新增' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="editVisible" class="dialog-mask" @click.self="closeEditDialog">
      <div class="dialog-card">
        <div class="dialog-header">
          <h3>编辑管理员</h3>
          <button class="close-btn" @click="closeEditDialog">×</button>
        </div>
        <div class="form-grid">
          <label>
            <span>管理员账号</span>
            <input :value="editForm.username" disabled />
          </label>
          <label>
            <span>昵称</span>
            <input v-model.trim="editForm.nickname" placeholder="请输入昵称" />
          </label>
          <label>
            <span>邮箱</span>
            <input v-model.trim="editForm.email" placeholder="请输入邮箱" />
          </label>
        </div>
        <div class="dialog-actions">
          <button class="subtle-btn" @click="closeEditDialog">取消</button>
          <button class="primary-btn" :disabled="submitting" @click="handleUpdate">
            {{ submitting ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="passwordVisible" class="dialog-mask" @click.self="closePasswordDialog">
      <div class="dialog-card">
        <div class="dialog-header">
          <h3>修改管理员密码</h3>
          <button class="close-btn" @click="closePasswordDialog">×</button>
        </div>
        <div class="dialog-note">当前账号：{{ passwordForm.username }}</div>
        <div class="form-grid">
          <label>
            <span>新密码</span>
            <input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
          </label>
          <label>
            <span>确认新密码</span>
            <input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
          </label>
        </div>
        <div class="dialog-actions">
          <button class="subtle-btn" @click="closePasswordDialog">取消</button>
          <button class="primary-btn" :disabled="submitting" @click="handleUpdatePassword">
            {{ submitting ? '提交中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import {
  createAdminAccount,
  deleteAdminAccount,
  getAdminAdmins,
  updateAdminAccount,
  updateAdminAccountPassword
} from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const admins = ref([])
const loading = ref(false)
const submitting = ref(false)
const page = ref(1)
const pageSize = 5

const createVisible = ref(false)
const editVisible = ref(false)
const passwordVisible = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(admins.value.length / pageSize)))
const pagedAdmins = computed(() => {
  const start = (page.value - 1) * pageSize
  return admins.value.slice(start, start + pageSize)
})

const createForm = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const editForm = reactive({
  id: null,
  username: '',
  nickname: '',
  email: ''
})

const passwordForm = reactive({
  id: null,
  username: '',
  newPassword: '',
  confirmPassword: ''
})

const resetCreateForm = () => {
  createForm.username = ''
  createForm.nickname = ''
  createForm.email = ''
  createForm.password = ''
  createForm.confirmPassword = ''
}

const resetEditForm = () => {
  editForm.id = null
  editForm.username = ''
  editForm.nickname = ''
  editForm.email = ''
}

const resetPasswordForm = () => {
  passwordForm.id = null
  passwordForm.username = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const loadAdmins = async () => {
  loading.value = true
  try {
    const res = await getAdminAdmins()
    admins.value = Array.isArray(res.data) ? res.data : []
    page.value = 1
  } catch (error) {
    notify?.error(error?.response?.data?.message || '管理员列表加载失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  resetCreateForm()
  createVisible.value = true
}

const closeCreateDialog = () => {
  createVisible.value = false
  resetCreateForm()
}

const openEditDialog = (item) => {
  editForm.id = item.id
  editForm.username = item.username
  editForm.nickname = item.nickname || ''
  editForm.email = item.email || ''
  editVisible.value = true
}

const closeEditDialog = () => {
  editVisible.value = false
  resetEditForm()
}

const openPasswordDialog = (item) => {
  passwordForm.id = item.id
  passwordForm.username = item.username
  passwordVisible.value = true
}

const closePasswordDialog = () => {
  passwordVisible.value = false
  resetPasswordForm()
}

const handleCreate = async () => {
  if (!createForm.username || !createForm.password || !createForm.confirmPassword) {
    notify?.warning('请填写完整的管理员账号和密码')
    return
  }

  submitting.value = true
  try {
    const res = await createAdminAccount({ ...createForm })
    notify?.success(res.message || '管理员新增成功')
    closeCreateDialog()
    await loadAdmins()
  } catch (error) {
    notify?.error(error?.response?.data?.message || '管理员新增失败')
  } finally {
    submitting.value = false
  }
}

const handleUpdate = async () => {
  if (!editForm.id) {
    return
  }

  submitting.value = true
  try {
    const res = await updateAdminAccount(editForm.id, {
      nickname: editForm.nickname,
      email: editForm.email
    })
    notify?.success(res.message || '管理员信息更新成功')
    closeEditDialog()
    await loadAdmins()
  } catch (error) {
    notify?.error(error?.response?.data?.message || '管理员信息更新失败')
  } finally {
    submitting.value = false
  }
}

const handleUpdatePassword = async () => {
  if (!passwordForm.id || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    notify?.warning('请填写完整的新密码')
    return
  }

  submitting.value = true
  try {
    const res = await updateAdminAccountPassword(passwordForm.id, {
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    notify?.success(res.message || '管理员密码修改成功')
    closePasswordDialog()
  } catch (error) {
    notify?.error(error?.response?.data?.message || '管理员密码修改失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (item) => {
  if (Number(item.isSuperAdmin) === 1) {
    notify?.warning('超级管理员账号不允许删除')
    return
  }

  if (!window.confirm(`确认删除管理员账号 ${item.username} 吗？`)) {
    return
  }

  try {
    const res = await deleteAdminAccount(item.id)
    notify?.success(res.message || '管理员删除成功')
    await loadAdmins()
  } catch (error) {
    notify?.error(error?.response?.data?.message || '管理员删除失败')
  }
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN')
}

const changePage = (step) => {
  const nextPage = page.value + step
  if (nextPage < 1 || nextPage > totalPages.value) {
    return
  }
  page.value = nextPage
}

onMounted(loadAdmins)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.table-card {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 24px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.06);
}

.hero-card {
  padding: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(251, 191, 36, 0.22), transparent 24%),
    linear-gradient(135deg, #ffffff, #fff7ed);
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #b45309;
  font-weight: 700;
}

.hero-card h2 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #111827;
}

.hero-desc {
  margin: 0;
  max-width: 680px;
  color: #6b7280;
  line-height: 1.7;
}

.primary-btn,
.subtle-btn,
.ghost-btn,
.close-btn {
  border: none;
  cursor: pointer;
  transition: 0.2s ease;
}

.primary-btn {
  padding: 12px 18px;
  border-radius: 14px;
  background: linear-gradient(135deg, #f59e0b, #ea580c);
  color: #fff;
  font-weight: 700;
}

.primary-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-card,
.empty-text {
  text-align: center;
  color: #6b7280;
}

.loading-card {
  padding: 36px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px dashed #cbd5e1;
}

.table-card {
  padding: 20px;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  color: #475569;
  font-size: 14px;
}

.tip-text {
  color: #b45309;
}

.table-wrap {
  overflow-x: auto;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 18px;
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

.admin-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 880px;
}

.admin-table th,
.admin-table td {
  padding: 16px 14px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
  font-size: 14px;
}

.admin-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.cell-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cell-stack small {
  color: #94a3b8;
}

.role-tag,
.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.role-tag.super {
  background: #fff7ed;
  color: #c2410c;
}

.role-tag.normal {
  background: #eef2ff;
  color: #4338ca;
}

.status-tag.active {
  background: #ecfdf5;
  color: #047857;
}

.status-tag.disabled {
  background: #fef2f2;
  color: #dc2626;
}

.action-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ghost-btn {
  padding: 7px 12px;
  border-radius: 10px;
  background: #f8fafc;
  color: #334155;
}

.ghost-btn:hover {
  background: #e2e8f0;
}

.ghost-btn.warn {
  background: #fff7ed;
  color: #c2410c;
}

.ghost-btn.warn:hover {
  background: #ffedd5;
}

.ghost-btn.danger {
  background: #fef2f2;
  color: #dc2626;
}

.ghost-btn.danger:hover {
  background: #fee2e2;
}

.ghost-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
  display: grid;
  place-items: center;
  padding: 20px;
  z-index: 20;
}

.dialog-card {
  width: min(640px, 100%);
  background: #fff;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.dialog-header h3 {
  margin: 0;
  font-size: 22px;
  color: #111827;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #475569;
  font-size: 22px;
}

.dialog-note {
  margin-bottom: 14px;
  color: #b45309;
  font-size: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
}

.form-grid input {
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 14px;
  outline: none;
  font-size: 14px;
}

.form-grid input:focus {
  border-color: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.12);
}

.form-grid input:disabled {
  background: #f8fafc;
  color: #94a3b8;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 22px;
}

.subtle-btn {
  padding: 12px 18px;
  border-radius: 14px;
  background: #f1f5f9;
  color: #334155;
}

@media (max-width: 900px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .dialog-actions {
    flex-direction: column-reverse;
  }
}
</style>
