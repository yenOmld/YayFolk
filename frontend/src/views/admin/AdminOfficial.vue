﻿<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>官网内容管理</h2>
        <p class="page-subtitle">
          管理活动、非遗科学卡片和精选作品的数据源表,然后将选中的项目发布到首页。
        </p>
      </div>
      <button class="publish-btn" :disabled="publishing || loading" @click="publishSelection">
        {{ publishing ? '发布中...' : `发布 ${currentTab.limit} 项` }}
      </button>
    </div>

    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        <span>{{ tab.label }}</span>
        <small>{{ datasetCount(tab.key) }} 条记录</small>
      </button>
    </div>

    <div class="panel-card">
      <div class="panel-toolbar">
        <div>
          <h3>{{ currentTab.label }}</h3>
          <p>{{ currentTab.description }}</p>
        </div>
        <div class="toolbar-actions">
          <div class="search-box">
            <span class="search-label">搜索</span>
            <div class="search-control">
              <i class='bx bx-search'></i>
              <input v-model.trim="keyword" class="search-input" type="text" :placeholder="currentTab.searchPlaceholder" />
              <button v-if="keyword" class="clear-search-btn" type="button" @click="keyword = ''">清除</button>
            </div>
          </div>
          <button class="secondary-btn" :disabled="loading" @click="loadAll">刷新</button>
          <button class="primary-btn" @click="openCreate">{{ currentTab.createLabel }}</button>
        </div>
      </div>

      <div class="selection-hint">
        <span>已选 {{ selectedIds.length }} / {{ currentTab.limit }}</span>
        <span v-if="activeTab === 'works'">从热门前20条作品中选择6条。</span>
        <span v-else-if="activeTab === 'heritages'">选择6个非遗项目。每个选中的项目应包含图片URL和视频URL。</span>
        <span v-else>为首页活动模块选择3个活动。</span>
      </div>

      <div v-if="loading" class="state-block">加载中...</div>
      <div v-else-if="filteredRows.length === 0" class="state-block">未找到记录。</div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr v-if="activeTab === 'activities'">
              <th class="select-col">选择</th>
              <th>封面</th>
              <th>活动</th>
              <th>时间安排</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
            <tr v-else-if="activeTab === 'heritages'">
              <th class="select-col">选择</th>
              <th>封面</th>
              <th>非遗</th>
              <th>视频</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
            <tr v-else>
              <th class="select-col">选择</th>
              <th>封面</th>
              <th>作品</th>
              <th>作者 / 热度</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in pagedRows" :key="row.id">
              <td class="select-col">
                <input
                  type="checkbox"
                  :checked="selectedIds.includes(row.id)"
                  :disabled="!selectedIds.includes(row.id) && selectedIds.length >= currentTab.limit"
                  @change="toggleSelection(row.id, $event.target.checked)"
                />
              </td>

              <template v-if="activeTab === 'activities'">
                <td><img :src="row.coverImage || fallbackCover" class="thumb" alt="活动封面" /></td>
                <td>
                  <div class="cell-title">{{ row.title || '未命名活动' }}</div>
                  <div class="cell-subtitle">{{ row.subtitle || row.heritageType || '无副标题' }}</div>
                  <div class="cell-text">{{ truncate(row.content, 72) }}</div>
                </td>
                <td>
                  <div class="cell-text">{{ formatRange(row.startTime, row.endTime) }}</div>
                  <div class="cell-text muted">{{ formatLocation(row) }}</div>
                </td>
                <td>
                  <div class="badge-row">
                    <span class="badge publish" :class="{ active: row.published }">{{ row.published ? '已发布' : '草稿' }}</span>
                    <span class="badge neutral">{{ activityStatusLabel(row.status) }}</span>
                  </div>
                </td>
              </template>

              <template v-else-if="activeTab === 'heritages'">
                <td><img :src="row.coverImage || fallbackCover" class="thumb" alt="非遗封面" /></td>
                <td>
                  <div class="cell-title">{{ row.name || '未命名非遗项目' }}</div>
                  <div class="cell-subtitle">{{ row.category || '无分类' }} / {{ row.region || '无地区' }}</div>
                  <div class="cell-text">{{ truncate(row.introduction, 72) }}</div>
                </td>
                <td>
                  <div class="cell-text">{{ row.videoUrl || '无视频URL' }}</div>
                  <div class="cell-text muted">首页卡片将显示保存的图片和视频URL。</div>
                </td>
                <td>
                  <div class="badge-row">
                    <span class="badge publish" :class="{ active: row.published }">{{ row.published ? '已发布' : '草稿' }}</span>
                    <span class="badge neutral">浏览 {{ row.viewCount || 0 }}</span>
                  </div>
                </td>
              </template>

              <template v-else>
                <td><img :src="row.coverImage || fallbackCover" class="thumb" alt="作品封面" /></td>
                <td>
                  <div class="cell-title">{{ row.title || '未命名作品' }}</div>
                  <div class="cell-subtitle">{{ row.category || '无分类' }}</div>
                  <div class="cell-text">{{ truncate(row.content || row.description, 72) }}</div>
                </td>
                <td>
                  <div class="cell-text">{{ row.authorName || '未知作者' }}</div>
                  <div class="badge-row">
                    <span class="badge publish" :class="{ active: row.published }">{{ row.published ? '已发布' : '草稿' }}</span>
                    <span class="badge neutral">热度 {{ row.heat || 0 }}</span>
                  </div>
                </td>
                <td><div class="cell-text">{{ formatTime(row.createTime) }}</div></td>
              </template>

              <td>
                <div class="action-row">
                  <button class="link-btn" @click="openEdit(row)">编辑</button>
                  <button class="danger-btn" @click="openDeleteModal(row)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="filteredRows.length > 0" class="pagination-bar">
        <div class="pagination-summary">
          第 {{ activePage }} / {{ totalPages }} 页
          <span class="pagination-muted">显示 {{ pageStart }}-{{ pageEnd }} 条，共 {{ filteredRows.length }} 条</span>
        </div>
        <div class="pagination-actions">
          <button class="secondary-btn" :disabled="activePage <= 1" @click="changePage(-1)">上一页</button>
          <button class="secondary-btn" :disabled="activePage >= totalPages" @click="changePage(1)">下一页</button>
        </div>
      </div>
    </div>

    <div v-if="dialogVisible" class="dialog-mask" @click.self="closeDialog">
      <div class="dialog-card">
        <div class="dialog-head">
          <h3>{{ dialogMode === 'create' ? currentTab.createLabel : `编辑 ${currentTab.shortLabel}` }}</h3>
          <button class="close-btn" @click="closeDialog">×</button>
        </div>

        <div v-if="activeTab === 'activities'" class="grid cols-2">
          <label class="field"><span>标题</span><input v-model.trim="activityForm.title" type="text" maxlength="200" /></label>
          <label class="field"><span>副标题</span><input v-model.trim="activityForm.subtitle" type="text" maxlength="100" /></label>
          <label class="field"><span>非遗类型</span><input v-model.trim="activityForm.heritageType" type="text" /></label>
          <label class="field"><span>活动类型</span><select v-model="activityForm.activityType"><option value="offline">线下</option><option value="online">线上</option><option value="exhibition">展览</option></select></label>
          <label class="field"><span>开始时间</span><input v-model="activityForm.startTime" type="datetime-local" /></label>
          <label class="field"><span>结束时间</span><input v-model="activityForm.endTime" type="datetime-local" /></label>
          <label class="field"><span>省份</span><input v-model.trim="activityForm.locationProvince" type="text" /></label>
          <label class="field"><span>城市</span><input v-model.trim="activityForm.locationCity" type="text" /></label>
          <label class="field"><span>区县</span><input v-model.trim="activityForm.locationDistrict" type="text" /></label>
          <label class="field"><span>地址</span><input v-model.trim="activityForm.locationDetail" type="text" maxlength="200" /></label>
          <label class="field"><span>价格</span><input v-model.trim="activityForm.price" type="number" min="0" /></label>
          <label class="field"><span>最大人数</span><input v-model.trim="activityForm.maxParticipants" type="number" min="1" /></label>
          <label class="field cols-full"><span>封面图片URL</span><input v-model.trim="activityForm.coverImage" type="text" /></label>
          <label class="field cols-full"><span>图片URL</span><textarea v-model="activityForm.imagesText" rows="3" placeholder="每行一个URL或用逗号分隔"></textarea></label>
          <label class="field cols-full"><span>描述</span><textarea v-model="activityForm.content" rows="6"></textarea></label>
        </div>
        <div v-else-if="activeTab === 'heritages'" class="grid cols-2">
          <label class="field"><span>名称</span><input v-model.trim="heritageForm.name" type="text" maxlength="200" placeholder="请输入非遗名称" /></label>
          <label class="field"><span>分类</span><input v-model.trim="heritageForm.category" type="text" placeholder="传统工艺" /></label>
          <label class="field"><span>子分类</span><input v-model.trim="heritageForm.subcategory" type="text" placeholder="陶瓷烧制" /></label>
          <label class="field"><span>朝代</span><input v-model.trim="heritageForm.dynasty" type="text" placeholder="北宋" /></label>
          <label class="field"><span>地区</span><input v-model.trim="heritageForm.region" type="text" placeholder="河南汝州" /></label>
          <label class="field"><span>级别</span><select v-model="heritageForm.level"><option v-for="option in heritageLevelOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
          <label class="field cols-full">
            <span>图片URL</span>
            <div class="upload-row">
              <button class="secondary-btn" type="button" :disabled="uploadingHeritageImage" @click="triggerHeritageImageUpload">{{ uploadingHeritageImage ? '上传中...' : '上传图片到OSS' }}</button>
              <input ref="heritageImageInput" class="upload-input" type="file" accept="image/*" multiple @change="handleHeritageImageUpload" />
              <span class="upload-tip">支持多张图片。上传的URL将自动填充。</span>
            </div>
            <textarea v-model="heritageForm.imagesText" rows="3" placeholder="每行一个URL或用逗号分隔"></textarea>
            <div v-if="heritagePreviewImages.length" class="preview-grid">
              <img v-for="(url, index) in heritagePreviewImages" :key="url + '-' + index" :src="url" :alt="'非遗图片 ' + (index + 1)" />
            </div>
          </label>
          <label class="field cols-full">
            <span>视频URL</span>
            <div class="upload-row">
              <button class="secondary-btn" type="button" :disabled="uploadingHeritageVideo" @click="triggerHeritageVideoUpload">{{ uploadingHeritageVideo ? '上传中...' : '上传视频到OSS' }}</button>
              <input ref="heritageVideoInput" class="upload-input" type="file" accept="video/*" @change="handleHeritageVideoUpload" />
              <span class="upload-tip">上传的视频URL将自动填充。</span>
            </div>
            <input v-model.trim="heritageForm.videoUrl" type="text" />
            <div v-if="heritageForm.videoUrl" class="video-preview">
              <video :src="heritageForm.videoUrl" controls preload="metadata"></video>
            </div>
          </label>
          <label class="field cols-full"><span>简介</span><textarea v-model="heritageForm.introduction" rows="4" placeholder="为首页弹窗编写简洁的介绍。"></textarea></label>
          <label class="field cols-full"><span>历史</span><textarea v-model="heritageForm.history" rows="4" placeholder="描述历史渊源和发展。"></textarea></label>
          <label class="field cols-full"><span>传承价值</span><textarea v-model="heritageForm.inheritanceValue" rows="4" placeholder="说明此非遗项目当今的重要性。"></textarea></label>
          <label class="field"><span>代表性传承人</span><input v-model.trim="heritageForm.representativeInheritor" type="text" maxlength="100" placeholder="代表性传承人" /></label>
          <label class="field inline-field"><span>推荐</span><input v-model="heritageForm.isFeatured" type="checkbox" /></label>
        </div>

        <div v-else class="grid cols-2">
          <label class="field"><span>标题</span><input v-model.trim="workForm.title" type="text" maxlength="120" /></label>
          <label class="field"><span>分类</span><input v-model.trim="workForm.category" type="text" /></label>
          <label class="field"><span>源语言</span><input v-model.trim="workForm.sourceLang" type="text" /></label>
          <label class="field"><span>浏览量</span><input v-model.trim="workForm.viewCount" type="number" min="0" /></label>
          <label class="field"><span>收藏数</span><input v-model.trim="workForm.collectCount" type="number" min="0" /></label>
          <label class="field"><span>评论数</span><input v-model.trim="workForm.commentCount" type="number" min="0" /></label>
          <label class="field cols-full"><span>图片URL</span><textarea v-model="workForm.imagesText" rows="3" placeholder="每行一个URL或用逗号分隔"></textarea></label>
          <label class="field cols-full"><span>标签</span><textarea v-model="workForm.tagsText" rows="2" placeholder="每行一个标签或用逗号分隔"></textarea></label>
          <label class="field cols-full"><span>内容</span><textarea v-model="workForm.content" rows="6"></textarea></label>
        </div>

        <div class="dialog-actions">
          <button class="secondary-btn" @click="closeDialog">取消</button>
          <button class="primary-btn" :disabled="saving" @click="submitDialog">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="deleteModal.show" class="dialog-mask" @click.self="closeDeleteModal">
      <div class="dialog-card">
        <div class="dialog-head">
          <h3>确认删除</h3>
          <button class="close-btn" @click="closeDeleteModal">×</button>
        </div>
        <p class="dialog-intro">确认删除 {{ deleteModal.item?.title || deleteModal.item?.name || '此项' }} 吗？</p>
        <div class="dialog-actions">
          <button class="secondary-btn" @click="closeDeleteModal">取消</button>
          <button class="primary-btn danger" @click="submitDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from 'vue'
import {
  createOfficialActivity,
  createOfficialHeritage,
  createOfficialWork,
  deleteOfficialActivity,
  deleteOfficialHeritage,
  deleteOfficialWork,
  getOfficialActivities,
  getOfficialHeritages,
  getOfficialWorks,
  publishToHomepage,
  updateOfficialActivity,
  updateOfficialHeritage,
  updateOfficialWork,
  uploadMedia
} from '@/api/app.js'

const { appContext } = getCurrentInstance()
const notify = (message, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(message)
const fallbackCover = 'https://picsum.photos/seed/yayfolk-official/320/220'
const heritageLevelOptions = [
  { value: 'national', label: '国家级' },
  { value: 'provincial', label: '省级' },
  { value: 'municipal', label: '市级' }
]
const maxHeritageImageSize = 10 * 1024 * 1024
const maxHeritageVideoSize = 100 * 1024 * 1024

const tabs = [
  { key: 'activities', shortLabel: '活动', label: '活动数据源表', description: '从活动表管理首页活动候选。最多可发布3项。', createLabel: '添加活动', limit: 3, publishType: 'activity', searchPlaceholder: '搜索标题 / 位置 / 非遗类型' },
  { key: 'heritages', shortLabel: '非遗', label: '非遗数据源表', description: '从非遗库管理首页非遗科学卡片。最多可发布6项。', createLabel: '添加非遗', limit: 6, publishType: 'heritage', searchPlaceholder: '搜索名称 / 分类 / 地区' },
  { key: 'works', shortLabel: '作品', label: '作品数据源表', description: '从热门前20条帖子管理首页作品。最多可发布6项。', createLabel: '添加作品', limit: 6, publishType: 'work', searchPlaceholder: '搜索标题 / 作者 / 内容' }
]

const activeTab = ref('activities')
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const selectedIds = ref([])
const datasets = ref({ activities: [], heritages: [], works: [] })
const currentPage = ref({ activities: 1, heritages: 1, works: 1 })
const pageSize = ref(5)
const activityForm = ref(createActivityForm())
const heritageForm = ref(createHeritageForm())
const workForm = ref(createWorkForm())
const heritageImageInput = ref(null)
const heritageVideoInput = ref(null)
const uploadingHeritageImage = ref(false)
const uploadingHeritageVideo = ref(false)
const deleteModal = ref({
  show: false,
  item: null
})

const currentTab = computed(() => tabs.find((tab) => tab.key === activeTab.value) || tabs[0])
const currentRows = computed(() => datasets.value[activeTab.value] || [])
const activePage = computed(() => currentPage.value[activeTab.value] || 1)
const heritagePreviewImages = computed(() => splitLines(heritageForm.value.imagesText))

const unwrap = (response, fallbackMessage) => {
  if (!response || response.code !== 200) throw new Error(response?.message || fallbackMessage)
  return response.data
}

const filteredRows = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  if (!search) return currentRows.value
  return currentRows.value.filter((row) => buildSearchText(row).includes(search))
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value)))
const pageStart = computed(() => (filteredRows.value.length === 0 ? 0 : (activePage.value - 1) * pageSize.value + 1))
const pageEnd = computed(() => Math.min(activePage.value * pageSize.value, filteredRows.value.length))
const pagedRows = computed(() => {
  const start = (activePage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})
function createActivityForm() {
  return { title: '', subtitle: '', heritageType: '', activityType: 'offline', startTime: '', endTime: '', locationProvince: '', locationCity: '', locationDistrict: '', locationDetail: '', price: '0', maxParticipants: '', coverImage: '', imagesText: '', content: '' }
}
function createHeritageForm() {
  return { name: '', category: '', subcategory: '', dynasty: '', region: '', level: 'national', imagesText: '', videoUrl: '', introduction: '', history: '', inheritanceValue: '', representativeInheritor: '', isFeatured: false }
}
function createWorkForm() {
  return { title: '', category: '', sourceLang: 'zh', viewCount: '0', collectCount: '0', commentCount: '0', imagesText: '', tagsText: '', content: '' }
}
function datasetCount(key) { return (datasets.value[key] || []).length }
function resetPage(key = activeTab.value) { currentPage.value = { ...currentPage.value, [key]: 1 } }
function switchTab(key) { activeTab.value = key; keyword.value = ''; resetPage(key); syncSelectedFromPublished() }
function syncSelectedFromPublished() { selectedIds.value = currentRows.value.filter((row) => row.published).map((row) => row.id) }
function buildSearchText(row) {
  if (activeTab.value === 'activities') return [row.title, row.subtitle, row.heritageType, row.locationProvince, row.locationCity, row.locationDistrict, row.locationDetail, row.content].filter(Boolean).join(' ').toLowerCase()
  if (activeTab.value === 'heritages') return [row.name, row.category, row.subcategory, row.region, row.level, row.introduction, row.history].filter(Boolean).join(' ').toLowerCase()
  return [row.title, row.authorName, row.category, row.content, row.description].filter(Boolean).join(' ').toLowerCase()
}
function toggleSelection(id, checked) { if (checked) { if (!selectedIds.value.includes(id) && selectedIds.value.length < currentTab.value.limit) selectedIds.value = [...selectedIds.value, id]; return } selectedIds.value = selectedIds.value.filter((item) => item !== id) }
function changePage(step) { const nextPage = activePage.value + step; if (nextPage < 1 || nextPage > totalPages.value) return; currentPage.value = { ...currentPage.value, [activeTab.value]: nextPage } }
function truncate(value, length = 60) { const text = typeof value === 'string' ? value : ''; if (!text) return '-'; return text.length > length ? `${text.slice(0, length)}...` : text }
function formatTime(value) { return value ? new Date(value).toLocaleString('zh-CN') : '-' }
function formatRange(startTime, endTime) { if (!startTime && !endTime) return '时间待定'; return `${formatTime(startTime)} - ${formatTime(endTime)}` }
function formatLocation(row) { return [row.locationProvince, row.locationCity, row.locationDistrict, row.locationDetail].filter(Boolean).join(' / ') || '位置待定' }
function formatDateInput(value) { if (!value) return ''; const date = new Date(value); const offset = date.getTimezoneOffset(); const localDate = new Date(date.getTime() - offset * 60000); return localDate.toISOString().slice(0, 16) }
function activityStatusLabel(status) { return { signup: '开放报名', ongoing: '进行中', ended: '已结束', full: '已满' }[status] || (status || '未知') }
function splitLines(value) { return String(value || '').split(/\r?\n|,/).map((item) => item.trim()).filter(Boolean) }
function normalizeNumber(value) { if (value === '' || value === null || value === undefined) return null; const number = Number(value); return Number.isFinite(number) ? number : null }
function formatListInput(value) { if (Array.isArray(value)) return value.join('\n'); return typeof value === 'string' ? value : '' }
function normalizeHeritageLevel(value) { return heritageLevelOptions.some((option) => option.value === value) ? value : 'national' }
function getRequestErrorMessage(error, fallbackMessage) { return error?.response?.data?.message || error?.message || fallbackMessage }
function validateHeritageMedia(file, kind) {
  if (!file) throw new Error(kind === 'video' ? '请选择视频文件' : '请选择图片文件')
  const isVideo = kind === 'video'
  const expectedTypePrefix = isVideo ? 'video/' : 'image/'
  const maxSize = isVideo ? maxHeritageVideoSize : maxHeritageImageSize
  const maxSizeLabel = isVideo ? '100MB' : '10MB'
  if (!String(file.type || '').startsWith(expectedTypePrefix)) throw new Error(isVideo ? '请上传有效的视频文件' : '请上传有效的图片文件')
  if (file.size > maxSize) throw new Error(`${isVideo ? '视频' : '图片'}大小必须为${maxSizeLabel}或更小`)
}
function triggerHeritageImageUpload() { heritageImageInput.value?.click() }
function triggerHeritageVideoUpload() { heritageVideoInput.value?.click() }
function mergeMultilineUrls(currentText, urls) { return Array.from(new Set([...splitLines(currentText), ...urls.filter(Boolean)])).join('\n') }
function clearUploadInput(event) { if (event?.target) event.target.value = '' }
async function handleHeritageImageUpload(event) {
  const files = Array.from(event.target?.files || [])
  if (files.length === 0) return
  uploadingHeritageImage.value = true
  try {
    const uploadedUrls = []
    for (const file of files) {
      validateHeritageMedia(file, 'image')
      const formData = new FormData()
      formData.append('file', file)
      const data = unwrap(await uploadMedia(formData, 'heritage/images'), '图片上传失败')
      if (!data?.url) throw new Error('图片上传失败')
      uploadedUrls.push(data.url)
    }
    heritageForm.value = { ...heritageForm.value, imagesText: mergeMultilineUrls(heritageForm.value.imagesText, uploadedUrls) }
    notify('已上传 ' + uploadedUrls.length + ' 张图片', 'success')
  } catch (error) {
    notify(getRequestErrorMessage(error, '图片上传失败'), 'error')
  } finally {
    uploadingHeritageImage.value = false
    clearUploadInput(event)
  }
}
async function handleHeritageVideoUpload(event) {
  const file = event.target?.files?.[0]
  if (!file) return
  uploadingHeritageVideo.value = true
  try {
    validateHeritageMedia(file, 'video')
    const formData = new FormData()
    formData.append('file', file)
    const data = unwrap(await uploadMedia(formData, 'heritage/videos'), '视频上传失败')
    if (!data?.url) throw new Error('视频上传失败')
    heritageForm.value = { ...heritageForm.value, videoUrl: data.url }
    notify('视频上传成功', 'success')
  } catch (error) {
    notify(getRequestErrorMessage(error, '视频上传失败'), 'error')
  } finally {
    uploadingHeritageVideo.value = false
    clearUploadInput(event)
  }
}
function resetCurrentForm() { if (activeTab.value === 'activities') activityForm.value = createActivityForm(); else if (activeTab.value === 'heritages') heritageForm.value = createHeritageForm(); else workForm.value = createWorkForm() }
function openCreate() { dialogMode.value = 'create'; editingId.value = null; resetCurrentForm(); dialogVisible.value = true }
function closeDialog() { dialogVisible.value = false; saving.value = false }
function openEdit(row) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  if (activeTab.value === 'activities') activityForm.value = { title: row.title || '', subtitle: row.subtitle || '', heritageType: row.heritageType || '', activityType: row.activityType || 'offline', startTime: formatDateInput(row.startTime), endTime: formatDateInput(row.endTime), locationProvince: row.locationProvince || '', locationCity: row.locationCity || '', locationDistrict: row.locationDistrict || '', locationDetail: row.locationDetail || '', price: row.price ?? 0, maxParticipants: row.maxParticipants ?? '', coverImage: row.coverImage || '', imagesText: formatListInput(row.images || row.imageList), content: row.content || '' }
  else if (activeTab.value === 'heritages') heritageForm.value = { name: row.name || '', category: row.category || '', subcategory: row.subcategory || '', dynasty: row.dynasty || '', region: row.region || '', level: normalizeHeritageLevel(row.level), imagesText: formatListInput(row.images || row.imageList), videoUrl: row.videoUrl || '', introduction: row.introduction || '', history: row.history || '', inheritanceValue: row.inheritanceValue || '', representativeInheritor: row.representativeInheritor || '', isFeatured: Number(row.isFeatured || 0) === 1 }
  else workForm.value = { title: row.title || '', category: row.category || '', sourceLang: row.sourceLang || 'zh', viewCount: row.viewCount ?? 0, collectCount: row.collectCount ?? 0, commentCount: row.commentCount ?? 0, imagesText: formatListInput(row.images), tagsText: formatListInput(row.tags), content: row.content || row.description || '' }
  dialogVisible.value = true
}
async function loadAll() {
  loading.value = true
  try {
    const [activityRes, heritageRes, workRes] = await Promise.all([getOfficialActivities(), getOfficialHeritages(), getOfficialWorks()])
    datasets.value = { activities: unwrap(activityRes, '加载活动失败') || [], heritages: unwrap(heritageRes, '加载非遗项目失败') || [], works: unwrap(workRes, '加载作品失败') || [] }
    currentPage.value = { activities: 1, heritages: 1, works: 1 }
    syncSelectedFromPublished()
  } catch (error) { notify(error.message || '加载失败', 'error') } finally { loading.value = false }
}
function buildActivityPayload() { return { title: activityForm.value.title, subtitle: activityForm.value.subtitle, heritageType: activityForm.value.heritageType, activityType: activityForm.value.activityType, startTime: activityForm.value.startTime || null, endTime: activityForm.value.endTime || null, locationProvince: activityForm.value.locationProvince, locationCity: activityForm.value.locationCity, locationDistrict: activityForm.value.locationDistrict, locationDetail: activityForm.value.locationDetail, price: normalizeNumber(activityForm.value.price) ?? 0, maxParticipants: normalizeNumber(activityForm.value.maxParticipants), coverImage: activityForm.value.coverImage, images: splitLines(activityForm.value.imagesText), content: activityForm.value.content } }
function buildHeritagePayload() { return { name: heritageForm.value.name, category: heritageForm.value.category, subcategory: heritageForm.value.subcategory, dynasty: heritageForm.value.dynasty, region: heritageForm.value.region, level: normalizeHeritageLevel(heritageForm.value.level), images: splitLines(heritageForm.value.imagesText), videoUrl: heritageForm.value.videoUrl, introduction: heritageForm.value.introduction, history: heritageForm.value.history, inheritanceValue: heritageForm.value.inheritanceValue, representativeInheritor: heritageForm.value.representativeInheritor, isFeatured: heritageForm.value.isFeatured ? 1 : 0 } }
function buildWorkPayload() { return { title: workForm.value.title, category: workForm.value.category, sourceLang: workForm.value.sourceLang, viewCount: normalizeNumber(workForm.value.viewCount) ?? 0, collectCount: normalizeNumber(workForm.value.collectCount) ?? 0, commentCount: normalizeNumber(workForm.value.commentCount) ?? 0, images: splitLines(workForm.value.imagesText), tags: splitLines(workForm.value.tagsText), content: workForm.value.content } }
async function submitDialog() {
  saving.value = true
  try {
    if (activeTab.value === 'activities') {
      const payload = buildActivityPayload()
      if (!payload.title || !payload.startTime || !payload.endTime) throw new Error('请填写标题、开始时间和结束时间')
      if (dialogMode.value === 'create') unwrap(await createOfficialActivity(payload), '创建活动失败')
      else unwrap(await updateOfficialActivity(editingId.value, payload), '更新活动失败')
    } else if (activeTab.value === 'heritages') {
      const payload = buildHeritagePayload()
      if (!payload.name) throw new Error('请输入非遗名称')
      if (payload.images.length === 0) throw new Error('请至少上传一张非遗图片')
      if (!payload.videoUrl) throw new Error('请上传非遗视频')
      if (dialogMode.value === 'create') unwrap(await createOfficialHeritage(payload), '创建非遗项目失败')
      else unwrap(await updateOfficialHeritage(editingId.value, payload), '更新非遗项目失败')
    } else {
      const payload = buildWorkPayload()
      if (!payload.title || !payload.content) throw new Error('请填写作品标题和内容')
      if (dialogMode.value === 'create') unwrap(await createOfficialWork(payload), '创建作品失败')
      else unwrap(await updateOfficialWork(editingId.value, payload), '更新作品失败')
    }
    notify(dialogMode.value === 'create' ? '创建成功' : '保存成功', 'success')
    closeDialog()
    await loadAll()
  } catch (error) { notify(getRequestErrorMessage(error, '保存失败'), 'error') } finally { saving.value = false }
}
// 打开删除确认模态框
function openDeleteModal(row) {
  deleteModal.value = {
    show: true,
    item: row
  }
}

// 关闭删除确认模态框
function closeDeleteModal() {
  deleteModal.value = {
    show: false,
    item: null
  }
}

// 提交删除
async function submitDelete() {
  const { item } = deleteModal.value
  if (!item) return
  
  try {
    if (activeTab.value === 'activities') unwrap(await deleteOfficialActivity(item.id), '删除活动失败')
    else if (activeTab.value === 'heritages') unwrap(await deleteOfficialHeritage(item.id), '删除非遗项目失败')
    else unwrap(await deleteOfficialWork(item.id), '删除作品失败')
    notify('删除成功', 'success')
    closeDeleteModal()
    await loadAll()
  } catch (error) { notify(getRequestErrorMessage(error, '删除失败'), 'error') }
}
async function publishSelection() {
  if (selectedIds.value.length === 0) return notify('请先选择项目', 'error')
  if (selectedIds.value.length > currentTab.value.limit) return notify('最多可选择 ' + currentTab.value.limit + ' 项', 'error')
  publishing.value = true
  try { unwrap(await publishToHomepage(currentTab.value.publishType, selectedIds.value), '发布到首页失败'); notify('首页选中项已更新', 'success'); await loadAll() } catch (error) { notify(getRequestErrorMessage(error, '发布失败'), 'error') } finally { publishing.value = false }
}
watch(keyword, () => { resetPage() })
watch(filteredRows, () => {
  if (activePage.value > totalPages.value) {
    currentPage.value = { ...currentPage.value, [activeTab.value]: totalPages.value }
  }
})
onMounted(loadAll)
</script>

<style scoped>
.admin-page { display: flex; flex-direction: column; gap: 18px; }
.page-header, .panel-toolbar { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.page-header h2, .panel-toolbar h3 { margin: 0; color: #111827; }
.page-subtitle, .panel-toolbar p { margin: 8px 0 0; color: #6b7280; line-height: 1.7; }
.tab-bar { display: flex; gap: 12px; flex-wrap: wrap; }
.tab-btn { min-width: 160px; padding: 14px 18px; border-radius: 14px; border: 1px solid #e5e7eb; background: #fff; cursor: pointer; display: flex; flex-direction: column; gap: 6px; text-align: left; }
.tab-btn span { font-size: 15px; font-weight: 700; color: #111827; }
.tab-btn small { color: #6b7280; }
.tab-btn.active { border-color: #7c3aed; box-shadow: 0 10px 24px rgba(124, 58, 237, 0.14); background: linear-gradient(180deg, #faf5ff 0%, #fff 100%); }
.panel-card { background: #fff; border-radius: 18px; border: 1px solid #e5e7eb; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.toolbar-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-box { display: flex; flex-direction: column; gap: 6px; min-width: 320px; }
.search-label { font-size: 12px; font-weight: 700; color: #475569; letter-spacing: 0.02em; }
.search-control { display: flex; align-items: center; gap: 8px; padding: 0 12px; border: 1px solid #d1d5db; border-radius: 12px; background: #fff; }
.search-control i { color: #64748b; font-size: 18px; }
.search-input, .field input, .field select, .field textarea { width: 100%; border: 1px solid #d1d5db; border-radius: 12px; padding: 10px 12px; font-size: 14px; box-sizing: border-box; }
.field select { color: #111827; background: #fff; font-weight: 600; }
.field select option { color: #111827; background: #fff; }
.search-input { width: 100%; border: none; padding: 10px 0; }
.clear-search-btn { border: none; background: transparent; color: #7c3aed; font-size: 13px; font-weight: 600; cursor: pointer; padding: 0; }
.field textarea { resize: vertical; line-height: 1.6; }
.search-input:focus, .field input:focus, .field select:focus, .field textarea:focus { outline: none; border-color: #7c3aed; box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.08); }
.search-control:focus-within { border-color: #7c3aed; box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.08); }
.primary-btn, .secondary-btn, .publish-btn, .link-btn, .danger-btn, .close-btn { border: none; cursor: pointer; transition: 0.2s ease; }
.primary-btn, .publish-btn { background: #7c3aed; color: #fff; }
.primary-btn, .secondary-btn, .publish-btn { padding: 10px 16px; border-radius: 12px; font-size: 14px; font-weight: 600; }
.secondary-btn { background: #f3f4f6; color: #374151; }
.primary-btn:disabled, .secondary-btn:disabled, .publish-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.selection-hint { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; padding: 12px 14px; border-radius: 12px; background: #f8fafc; color: #475569; font-size: 13px; }
.state-block { padding: 54px 20px; text-align: center; color: #94a3b8; }
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 14px 12px; border-bottom: 1px solid #eef2f7; text-align: left; vertical-align: top; }
.data-table thead th { color: #64748b; font-size: 13px; font-weight: 700; background: #fafafa; }
.pagination-bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; padding-top: 4px; }
.pagination-summary { color: #475569; font-size: 13px; }
.pagination-muted { color: #94a3b8; margin-left: 8px; }
.pagination-actions { display: flex; gap: 10px; }
.select-col { width: 70px; }
.thumb { width: 84px; height: 84px; object-fit: cover; border-radius: 12px; background: #f3f4f6; }
.cell-title { font-size: 15px; font-weight: 700; color: #111827; margin-bottom: 6px; }
.cell-subtitle { color: #7c3aed; font-size: 13px; margin-bottom: 8px; }
.cell-text { color: #374151; font-size: 13px; line-height: 1.7; }
.cell-text.muted { color: #6b7280; }
.badge-row, .action-row { display: flex; gap: 8px; flex-wrap: wrap; }
.badge { display: inline-flex; align-items: center; padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }
.badge.publish { background: #e5e7eb; color: #4b5563; }
.badge.publish.active { background: #dcfce7; color: #166534; }
.badge.neutral { background: #eff6ff; color: #1d4ed8; }
.link-btn { background: #eef2ff; color: #4338ca; padding: 8px 12px; border-radius: 10px; }
.danger-btn { background: #fee2e2; color: #dc2626; padding: 8px 12px; border-radius: 10px; }
.dialog-mask { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 20px; }
.dialog-card { width: min(920px, 100%); max-height: 90vh; overflow-y: auto; background: #fff; border-radius: 20px; padding: 22px; }
.dialog-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 18px; }
.close-btn {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #f3f4f6;
  color: #374151;
  font-size: 20px;
}

.dialog-intro {
  margin: 0 0 20px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
}

.primary-btn.danger {
  background: #dc2626;
  color: #fff;
}

.primary-btn.danger:hover {
  background: #b91c1c;
}
.grid { display: grid; gap: 14px; }
.grid.cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.field { display: flex; flex-direction: column; gap: 8px; }
.field span { font-size: 13px; font-weight: 600; color: #374151; }
.cols-full { grid-column: 1 / -1; }
.inline-field { justify-content: flex-end; }
.inline-field input { width: auto; align-self: flex-start; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.upload-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.upload-input { display: none; }
.upload-tip { color: #6b7280; font-size: 12px; }
.preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; margin-top: 10px; }
.preview-grid img { width: 100%; height: 96px; object-fit: cover; border-radius: 12px; border: 1px solid #e5e7eb; background: #f8fafc; }
.video-preview { margin-top: 10px; }
.video-preview video { width: 100%; max-height: 240px; border-radius: 14px; background: #000; }
@media (max-width: 960px) { .page-header, .panel-toolbar, .pagination-bar { flex-direction: column; align-items: stretch; } .toolbar-actions, .pagination-actions { width: 100%; } .search-input { width: 100%; } .grid.cols-2 { grid-template-columns: 1fr; } }
</style>




