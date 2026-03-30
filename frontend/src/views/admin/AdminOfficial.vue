<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>Homepage Official Content</h2>
        <p class="page-subtitle">
          Manage the source tables for activities, heritage science cards, and featured works, then publish the selected items to the homepage.
        </p>
      </div>
      <button class="publish-btn" :disabled="publishing || loading" @click="publishSelection">
        {{ publishing ? 'Publishing...' : `Publish ${currentTab.limit} item(s)` }}
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
        <small>{{ datasetCount(tab.key) }} records</small>
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
            <span class="search-label">Search</span>
            <div class="search-control">
              <i class='bx bx-search'></i>
              <input v-model.trim="keyword" class="search-input" type="text" :placeholder="currentTab.searchPlaceholder" />
              <button v-if="keyword" class="clear-search-btn" type="button" @click="keyword = ''">Clear</button>
            </div>
          </div>
          <button class="secondary-btn" :disabled="loading" @click="loadAll">Refresh</button>
          <button class="primary-btn" @click="openCreate">{{ currentTab.createLabel }}</button>
        </div>
      </div>

      <div class="selection-hint">
        <span>Selected {{ selectedIds.length }} / {{ currentTab.limit }}</span>
        <span v-if="activeTab === 'works'">Choose 6 works from the top 20 hot posts.</span>
        <span v-else-if="activeTab === 'heritages'">Choose 6 heritage items. Each selected item should have image URLs and a video URL.</span>
        <span v-else>Choose 3 activities for the homepage activity module.</span>
      </div>

      <div v-if="loading" class="state-block">Loading...</div>
      <div v-else-if="filteredRows.length === 0" class="state-block">No records found.</div>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr v-if="activeTab === 'activities'">
              <th class="select-col">Select</th>
              <th>Cover</th>
              <th>Activity</th>
              <th>Schedule</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
            <tr v-else-if="activeTab === 'heritages'">
              <th class="select-col">Select</th>
              <th>Cover</th>
              <th>Heritage</th>
              <th>Video</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
            <tr v-else>
              <th class="select-col">Select</th>
              <th>Cover</th>
              <th>Work</th>
              <th>Author / Heat</th>
              <th>Created At</th>
              <th>Actions</th>
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
                <td><img :src="row.coverImage || fallbackCover" class="thumb" alt="Activity cover" /></td>
                <td>
                  <div class="cell-title">{{ row.title || 'Untitled activity' }}</div>
                  <div class="cell-subtitle">{{ row.subtitle || row.heritageType || 'No subtitle' }}</div>
                  <div class="cell-text">{{ truncate(row.content, 72) }}</div>
                </td>
                <td>
                  <div class="cell-text">{{ formatRange(row.startTime, row.endTime) }}</div>
                  <div class="cell-text muted">{{ formatLocation(row) }}</div>
                </td>
                <td>
                  <div class="badge-row">
                    <span class="badge publish" :class="{ active: row.published }">{{ row.published ? 'Published' : 'Draft' }}</span>
                    <span class="badge neutral">{{ activityStatusLabel(row.status) }}</span>
                  </div>
                </td>
              </template>

              <template v-else-if="activeTab === 'heritages'">
                <td><img :src="row.coverImage || fallbackCover" class="thumb" alt="Heritage cover" /></td>
                <td>
                  <div class="cell-title">{{ row.name || 'Untitled heritage item' }}</div>
                  <div class="cell-subtitle">{{ row.category || 'No category' }} / {{ row.region || 'No region' }}</div>
                  <div class="cell-text">{{ truncate(row.introduction, 72) }}</div>
                </td>
                <td>
                  <div class="cell-text">{{ row.videoUrl || 'No video URL' }}</div>
                  <div class="cell-text muted">Homepage cards will display the saved image and video URLs.</div>
                </td>
                <td>
                  <div class="badge-row">
                    <span class="badge publish" :class="{ active: row.published }">{{ row.published ? 'Published' : 'Draft' }}</span>
                    <span class="badge neutral">Views {{ row.viewCount || 0 }}</span>
                  </div>
                </td>
              </template>

              <template v-else>
                <td><img :src="row.coverImage || fallbackCover" class="thumb" alt="Work cover" /></td>
                <td>
                  <div class="cell-title">{{ row.title || 'Untitled work' }}</div>
                  <div class="cell-subtitle">{{ row.category || 'No category' }}</div>
                  <div class="cell-text">{{ truncate(row.content || row.description, 72) }}</div>
                </td>
                <td>
                  <div class="cell-text">{{ row.authorName || 'Unknown author' }}</div>
                  <div class="badge-row">
                    <span class="badge publish" :class="{ active: row.published }">{{ row.published ? 'Published' : 'Draft' }}</span>
                    <span class="badge neutral">Heat {{ row.heat || 0 }}</span>
                  </div>
                </td>
                <td><div class="cell-text">{{ formatTime(row.createTime) }}</div></td>
              </template>

              <td>
                <div class="action-row">
                  <button class="link-btn" @click="openEdit(row)">Edit</button>
                  <button class="danger-btn" @click="removeRow(row)">Delete</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="filteredRows.length > 0" class="pagination-bar">
        <div class="pagination-summary">
          Page {{ activePage }} / {{ totalPages }}
          <span class="pagination-muted">Showing {{ pageStart }}-{{ pageEnd }} of {{ filteredRows.length }}</span>
        </div>
        <div class="pagination-actions">
          <button class="secondary-btn" :disabled="activePage <= 1" @click="changePage(-1)">Previous</button>
          <button class="secondary-btn" :disabled="activePage >= totalPages" @click="changePage(1)">Next</button>
        </div>
      </div>
    </div>

    <div v-if="dialogVisible" class="dialog-mask" @click.self="closeDialog">
      <div class="dialog-card">
        <div class="dialog-head">
          <h3>{{ dialogMode === 'create' ? currentTab.createLabel : `Edit ${currentTab.shortLabel}` }}</h3>
          <button class="close-btn" @click="closeDialog">×</button>
        </div>

        <div v-if="activeTab === 'activities'" class="grid cols-2">
          <label class="field"><span>Title</span><input v-model.trim="activityForm.title" type="text" maxlength="200" /></label>
          <label class="field"><span>Subtitle</span><input v-model.trim="activityForm.subtitle" type="text" maxlength="100" /></label>
          <label class="field"><span>Heritage Type</span><input v-model.trim="activityForm.heritageType" type="text" /></label>
          <label class="field"><span>Activity Type</span><select v-model="activityForm.activityType"><option value="offline">Offline</option><option value="online">Online</option><option value="exhibition">Exhibition</option></select></label>
          <label class="field"><span>Start Time</span><input v-model="activityForm.startTime" type="datetime-local" /></label>
          <label class="field"><span>End Time</span><input v-model="activityForm.endTime" type="datetime-local" /></label>
          <label class="field"><span>Province</span><input v-model.trim="activityForm.locationProvince" type="text" /></label>
          <label class="field"><span>City</span><input v-model.trim="activityForm.locationCity" type="text" /></label>
          <label class="field"><span>District</span><input v-model.trim="activityForm.locationDistrict" type="text" /></label>
          <label class="field"><span>Address</span><input v-model.trim="activityForm.locationDetail" type="text" maxlength="200" /></label>
          <label class="field"><span>Price</span><input v-model.trim="activityForm.price" type="number" min="0" /></label>
          <label class="field"><span>Max Participants</span><input v-model.trim="activityForm.maxParticipants" type="number" min="1" /></label>
          <label class="field cols-full"><span>Cover Image URL</span><input v-model.trim="activityForm.coverImage" type="text" /></label>
          <label class="field cols-full"><span>Image URLs</span><textarea v-model="activityForm.imagesText" rows="3" placeholder="One URL per line or separated by commas"></textarea></label>
          <label class="field cols-full"><span>Description</span><textarea v-model="activityForm.content" rows="6"></textarea></label>
        </div>
        <div v-else-if="activeTab === 'heritages'" class="grid cols-2">
          <label class="field"><span>Name</span><input v-model.trim="heritageForm.name" type="text" maxlength="200" placeholder="Enter the heritage name" /></label>
          <label class="field"><span>Category</span><input v-model.trim="heritageForm.category" type="text" placeholder="Traditional craftsmanship" /></label>
          <label class="field"><span>Subcategory</span><input v-model.trim="heritageForm.subcategory" type="text" placeholder="Ceramic firing" /></label>
          <label class="field"><span>Dynasty</span><input v-model.trim="heritageForm.dynasty" type="text" placeholder="Northern Song" /></label>
          <label class="field"><span>Region</span><input v-model.trim="heritageForm.region" type="text" placeholder="Ruzhou, Henan" /></label>
          <label class="field"><span>Level</span><select v-model="heritageForm.level"><option v-for="option in heritageLevelOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
          <label class="field cols-full">
            <span>Image URLs</span>
            <div class="upload-row">
              <button class="secondary-btn" type="button" :disabled="uploadingHeritageImage" @click="triggerHeritageImageUpload">{{ uploadingHeritageImage ? 'Uploading images...' : 'Upload images to OSS' }}</button>
              <input ref="heritageImageInput" class="upload-input" type="file" accept="image/*" multiple @change="handleHeritageImageUpload" />
              <span class="upload-tip">Supports multiple images. Uploaded URLs will be filled in automatically.</span>
            </div>
            <textarea v-model="heritageForm.imagesText" rows="3" placeholder="One URL per line or separated by commas"></textarea>
            <div v-if="heritagePreviewImages.length" class="preview-grid">
              <img v-for="(url, index) in heritagePreviewImages" :key="url + '-' + index" :src="url" :alt="'Heritage image ' + (index + 1)" />
            </div>
          </label>
          <label class="field cols-full">
            <span>Video URL</span>
            <div class="upload-row">
              <button class="secondary-btn" type="button" :disabled="uploadingHeritageVideo" @click="triggerHeritageVideoUpload">{{ uploadingHeritageVideo ? 'Uploading video...' : 'Upload video to OSS' }}</button>
              <input ref="heritageVideoInput" class="upload-input" type="file" accept="video/*" @change="handleHeritageVideoUpload" />
              <span class="upload-tip">The uploaded video URL will be filled in automatically.</span>
            </div>
            <input v-model.trim="heritageForm.videoUrl" type="text" />
            <div v-if="heritageForm.videoUrl" class="video-preview">
              <video :src="heritageForm.videoUrl" controls preload="metadata"></video>
            </div>
          </label>
          <label class="field cols-full"><span>Introduction</span><textarea v-model="heritageForm.introduction" rows="4" placeholder="Write a concise introduction for the homepage modal."></textarea></label>
          <label class="field cols-full"><span>History</span><textarea v-model="heritageForm.history" rows="4" placeholder="Describe the historical origin and development."></textarea></label>
          <label class="field cols-full"><span>Inheritance Value</span><textarea v-model="heritageForm.inheritanceValue" rows="4" placeholder="Explain why this heritage item matters today."></textarea></label>
          <label class="field"><span>Representative Inheritor</span><input v-model.trim="heritageForm.representativeInheritor" type="text" maxlength="100" placeholder="Representative inheritor" /></label>
          <label class="field inline-field"><span>Featured</span><input v-model="heritageForm.isFeatured" type="checkbox" /></label>
        </div>

        <div v-else class="grid cols-2">
          <label class="field"><span>Title</span><input v-model.trim="workForm.title" type="text" maxlength="120" /></label>
          <label class="field"><span>Category</span><input v-model.trim="workForm.category" type="text" /></label>
          <label class="field"><span>Source Language</span><input v-model.trim="workForm.sourceLang" type="text" /></label>
          <label class="field"><span>View Count</span><input v-model.trim="workForm.viewCount" type="number" min="0" /></label>
          <label class="field"><span>Favorite Count</span><input v-model.trim="workForm.collectCount" type="number" min="0" /></label>
          <label class="field"><span>Comment Count</span><input v-model.trim="workForm.commentCount" type="number" min="0" /></label>
          <label class="field cols-full"><span>Image URLs</span><textarea v-model="workForm.imagesText" rows="3" placeholder="One URL per line or separated by commas"></textarea></label>
          <label class="field cols-full"><span>Tags</span><textarea v-model="workForm.tagsText" rows="2" placeholder="One tag per line or separated by commas"></textarea></label>
          <label class="field cols-full"><span>Content</span><textarea v-model="workForm.content" rows="6"></textarea></label>
        </div>

        <div class="dialog-actions">
          <button class="secondary-btn" @click="closeDialog">Cancel</button>
          <button class="primary-btn" :disabled="saving" @click="submitDialog">{{ saving ? 'Saving...' : 'Save' }}</button>
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
  { value: 'national', label: 'National' },
  { value: 'provincial', label: 'Provincial' },
  { value: 'municipal', label: 'Municipal' }
]
const maxHeritageImageSize = 10 * 1024 * 1024
const maxHeritageVideoSize = 100 * 1024 * 1024

const tabs = [
  { key: 'activities', shortLabel: 'Activity', label: 'Activity Source Table', description: 'Manage the homepage activity candidates from the activity table. You can publish up to 3 items.', createLabel: 'Add Activity', limit: 3, publishType: 'activity', searchPlaceholder: 'Search title / location / heritage type' },
  { key: 'heritages', shortLabel: 'Heritage', label: 'Heritage Source Table', description: 'Manage the homepage heritage science cards from the heritage library. You can publish up to 6 items.', createLabel: 'Add Heritage', limit: 6, publishType: 'heritage', searchPlaceholder: 'Search name / category / region' },
  { key: 'works', shortLabel: 'Work', label: 'Work Source Table', description: 'Manage the homepage works from the top 20 hot posts. You can publish up to 6 items.', createLabel: 'Add Work', limit: 6, publishType: 'work', searchPlaceholder: 'Search title / author / content' }
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
function formatRange(startTime, endTime) { if (!startTime && !endTime) return 'Schedule TBD'; return `${formatTime(startTime)} - ${formatTime(endTime)}` }
function formatLocation(row) { return [row.locationProvince, row.locationCity, row.locationDistrict, row.locationDetail].filter(Boolean).join(' / ') || 'Location TBD' }
function formatDateInput(value) { if (!value) return ''; const date = new Date(value); const offset = date.getTimezoneOffset(); const localDate = new Date(date.getTime() - offset * 60000); return localDate.toISOString().slice(0, 16) }
function activityStatusLabel(status) { return { signup: 'Open for signup', ongoing: 'Ongoing', ended: 'Ended', full: 'Full' }[status] || (status || 'Unknown') }
function splitLines(value) { return String(value || '').split(/\r?\n|,/).map((item) => item.trim()).filter(Boolean) }
function normalizeNumber(value) { if (value === '' || value === null || value === undefined) return null; const number = Number(value); return Number.isFinite(number) ? number : null }
function formatListInput(value) { if (Array.isArray(value)) return value.join('\n'); return typeof value === 'string' ? value : '' }
function normalizeHeritageLevel(value) { return heritageLevelOptions.some((option) => option.value === value) ? value : 'national' }
function getRequestErrorMessage(error, fallbackMessage) { return error?.response?.data?.message || error?.message || fallbackMessage }
function validateHeritageMedia(file, kind) {
  if (!file) throw new Error(kind === 'video' ? 'Please select a video file' : 'Please select an image file')
  const isVideo = kind === 'video'
  const expectedTypePrefix = isVideo ? 'video/' : 'image/'
  const maxSize = isVideo ? maxHeritageVideoSize : maxHeritageImageSize
  const maxSizeLabel = isVideo ? '100MB' : '10MB'
  if (!String(file.type || '').startsWith(expectedTypePrefix)) throw new Error(isVideo ? 'Please upload a valid video file' : 'Please upload a valid image file')
  if (file.size > maxSize) throw new Error(`${isVideo ? 'Video' : 'Image'} size must be ${maxSizeLabel} or smaller`)
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
      const data = unwrap(await uploadMedia(formData, 'heritage/images'), 'Image upload failed')
      if (!data?.url) throw new Error('Image upload failed')
      uploadedUrls.push(data.url)
    }
    heritageForm.value = { ...heritageForm.value, imagesText: mergeMultilineUrls(heritageForm.value.imagesText, uploadedUrls) }
    notify('Uploaded ' + uploadedUrls.length + ' image(s)', 'success')
  } catch (error) {
    notify(getRequestErrorMessage(error, 'Image upload failed'), 'error')
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
    const data = unwrap(await uploadMedia(formData, 'heritage/videos'), 'Video upload failed')
    if (!data?.url) throw new Error('Video upload failed')
    heritageForm.value = { ...heritageForm.value, videoUrl: data.url }
    notify('Video uploaded successfully', 'success')
  } catch (error) {
    notify(getRequestErrorMessage(error, 'Video upload failed'), 'error')
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
    datasets.value = { activities: unwrap(activityRes, 'Failed to load activities') || [], heritages: unwrap(heritageRes, 'Failed to load heritage items') || [], works: unwrap(workRes, 'Failed to load works') || [] }
    currentPage.value = { activities: 1, heritages: 1, works: 1 }
    syncSelectedFromPublished()
  } catch (error) { notify(error.message || 'Load failed', 'error') } finally { loading.value = false }
}
function buildActivityPayload() { return { title: activityForm.value.title, subtitle: activityForm.value.subtitle, heritageType: activityForm.value.heritageType, activityType: activityForm.value.activityType, startTime: activityForm.value.startTime || null, endTime: activityForm.value.endTime || null, locationProvince: activityForm.value.locationProvince, locationCity: activityForm.value.locationCity, locationDistrict: activityForm.value.locationDistrict, locationDetail: activityForm.value.locationDetail, price: normalizeNumber(activityForm.value.price) ?? 0, maxParticipants: normalizeNumber(activityForm.value.maxParticipants), coverImage: activityForm.value.coverImage, images: splitLines(activityForm.value.imagesText), content: activityForm.value.content } }
function buildHeritagePayload() { return { name: heritageForm.value.name, category: heritageForm.value.category, subcategory: heritageForm.value.subcategory, dynasty: heritageForm.value.dynasty, region: heritageForm.value.region, level: normalizeHeritageLevel(heritageForm.value.level), images: splitLines(heritageForm.value.imagesText), videoUrl: heritageForm.value.videoUrl, introduction: heritageForm.value.introduction, history: heritageForm.value.history, inheritanceValue: heritageForm.value.inheritanceValue, representativeInheritor: heritageForm.value.representativeInheritor, isFeatured: heritageForm.value.isFeatured ? 1 : 0 } }
function buildWorkPayload() { return { title: workForm.value.title, category: workForm.value.category, sourceLang: workForm.value.sourceLang, viewCount: normalizeNumber(workForm.value.viewCount) ?? 0, collectCount: normalizeNumber(workForm.value.collectCount) ?? 0, commentCount: normalizeNumber(workForm.value.commentCount) ?? 0, images: splitLines(workForm.value.imagesText), tags: splitLines(workForm.value.tagsText), content: workForm.value.content } }
async function submitDialog() {
  saving.value = true
  try {
    if (activeTab.value === 'activities') {
      const payload = buildActivityPayload()
      if (!payload.title || !payload.startTime || !payload.endTime) throw new Error('Please fill in the title, start time, and end time')
      if (dialogMode.value === 'create') unwrap(await createOfficialActivity(payload), 'Failed to create activity')
      else unwrap(await updateOfficialActivity(editingId.value, payload), 'Failed to update activity')
    } else if (activeTab.value === 'heritages') {
      const payload = buildHeritagePayload()
      if (!payload.name) throw new Error('Please enter a heritage name')
      if (payload.images.length === 0) throw new Error('Please upload at least one heritage image')
      if (!payload.videoUrl) throw new Error('Please upload a heritage video')
      if (dialogMode.value === 'create') unwrap(await createOfficialHeritage(payload), 'Failed to create heritage item')
      else unwrap(await updateOfficialHeritage(editingId.value, payload), 'Failed to update heritage item')
    } else {
      const payload = buildWorkPayload()
      if (!payload.title || !payload.content) throw new Error('Please fill in the work title and content')
      if (dialogMode.value === 'create') unwrap(await createOfficialWork(payload), 'Failed to create work')
      else unwrap(await updateOfficialWork(editingId.value, payload), 'Failed to update work')
    }
    notify(dialogMode.value === 'create' ? 'Created successfully' : 'Saved successfully', 'success')
    closeDialog()
    await loadAll()
  } catch (error) { notify(getRequestErrorMessage(error, 'Save failed'), 'error') } finally { saving.value = false }
}
async function removeRow(row) {
  const label = activeTab.value === 'activities' ? row.title : (row.name || row.title)
  if (!window.confirm('Delete ' + (label || 'this item') + '?')) return
  try {
    if (activeTab.value === 'activities') unwrap(await deleteOfficialActivity(row.id), 'Failed to delete activity')
    else if (activeTab.value === 'heritages') unwrap(await deleteOfficialHeritage(row.id), 'Failed to delete heritage item')
    else unwrap(await deleteOfficialWork(row.id), 'Failed to delete work')
    notify('Deleted successfully', 'success')
    await loadAll()
  } catch (error) { notify(getRequestErrorMessage(error, 'Delete failed'), 'error') }
}
async function publishSelection() {
  if (selectedIds.value.length === 0) return notify('Please select items first', 'error')
  if (selectedIds.value.length > currentTab.value.limit) return notify('You can select up to ' + currentTab.value.limit + ' item(s)', 'error')
  publishing.value = true
  try { unwrap(await publishToHomepage(currentTab.value.publishType, selectedIds.value), 'Failed to publish to homepage'); notify('Homepage selection updated', 'success'); await loadAll() } catch (error) { notify(getRequestErrorMessage(error, 'Publish failed'), 'error') } finally { publishing.value = false }
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
.close-btn { width: 38px; height: 38px; border-radius: 50%; background: #f3f4f6; color: #374151; font-size: 20px; }
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



