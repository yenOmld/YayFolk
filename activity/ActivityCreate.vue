<template>
  <div class="activity-create-page">
    <div class="header-nav">
      <div class="back-button" @click="router.push('/home/merchant-products')">
        <i class='bx bx-arrow-back'></i>
        <span>{{ isEdit ? '返回活动管理' : '返回活动列表' }}</span>
      </div>
    </div>

    <div class="form-container">
      <div class="form-header">
        <h2>{{ isEdit ? '编辑活动' : '发布新活动' }}</h2>
        <p>{{ isEdit ? '修改活动信息以更新内容' : '详细的活动信息有助于吸引更多用户报名参与' }}</p>
      </div>

      <form class="create-form" @submit.prevent="submitActivity">
        <div class="form-section">
          <h3>基本信息</h3>
          
          <div class="form-group">
            <label class="required">活动名称</label>
            <input type="text" v-model="form.title" placeholder="请输入活动名称 (建议包含核心亮点)" required />
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label class="required">活动分类</label>
              <select v-model="form.categoryId" required>
                <option value="">请选择分类</option>
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                  {{ cat.name }}
                </option>
              </select>
            </div>
            <div class="form-group flex-1">
              <label class="required">单人价格 (元)</label>
              <input type="number" v-model="form.price" min="0" step="0.01" placeholder="填0表示免费" required />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label class="required">活动总名额</label>
              <input type="number" v-model.number="form.capacity" min="1" placeholder="限制最多报名人数" required />
            </div>
            <div class="form-group flex-1">
              <label class="required">活动时长</label>
              <input type="text" v-model="form.duration" placeholder="如: 2小时、半天等" required />
            </div>
          </div>
          
          <div class="form-group">
            <label class="required">活动地点</label>
            <input type="text" v-model="form.location" placeholder="请输入详细的活动举办地址" required />
          </div>

          <div class="form-group">
            <label class="required">活动图片</label>
            <p class="section-hint">最多上传9张图片，第一张将作为封面图</p>
            <div class="multi-upload-area">
              <div class="image-grid">
                <div 
                  v-for="(image, index) in form.images" 
                  :key="index"
                  class="image-item"
                >
                  <img :src="image.url" class="preview-img" />
                  <div class="image-mask">
                    <button type="button" class="delete-btn" @click.stop="removeImage(index)">
                      <i class='bx bx-trash'></i>
                    </button>
                  </div>
                </div>
                <div 
                  v-if="form.images.length < 9" 
                  class="upload-item" 
                  @click="$refs.imagesInput.click()"
                >
                  <i class='bx bx-plus'></i>
                  <span>添加图片</span>
                </div>
              </div>
              <input 
                ref="imagesInput"
                type="file" 
                accept="image/*" 
                multiple
                @change="handleMultiImageUpload"
                style="display: none;"
              />
            </div>
          </div>
        </div>

        <div class="form-section">
          <h3>预约时间与流程</h3>
          
          <div class="form-group">
            <label class="required">可预约时间段设置</label>
            <p class="section-hint">添加用户可选择报名的时间段</p>
            
            <div class="dynamic-list">
              <div class="dynamic-item" v-for="(time, index) in form.availableTimes" :key="index">
                <input type="date" v-model="time.date" required />
                <input type="time" v-model="time.startTime" required />
                <span class="separator">-</span>
                <input type="time" v-model="time.endTime" required />
                <button type="button" class="icon-btn danger" @click="removeTime(index)" v-if="form.availableTimes.length > 1">
                  <i class='bx bx-trash'></i>
                </button>
              </div>
              <button type="button" class="add-line-btn" @click="addTime">
                <i class='bx bx-plus'></i> 添加可预约时间段
              </button>
            </div>
          </div>

          <div class="form-group">
            <label class="required">活动大致流程</label>
            <p class="section-hint">清晰的流程安排让参与者更放心</p>
            
            <div class="dynamic-list">
              <div class="dynamic-item" v-for="(process, index) in form.processes" :key="index">
                <input type="text" v-model="process.time" placeholder="如: 14:00-14:30" class="time-input" required />
                <input type="text" v-model="process.content" placeholder="流程描述，如: 集合签到" class="desc-input" required />
                <button type="button" class="icon-btn danger" @click="removeProcess(index)" v-if="form.processes.length > 1">
                  <i class='bx bx-trash'></i>
                </button>
              </div>
              <button type="button" class="add-line-btn" @click="addProcess">
                <i class='bx bx-plus'></i> 添加流程节点
              </button>
            </div>
          </div>
        </div>

        <div class="form-section">
          <h3>详细介绍</h3>
          <div class="form-group quill-group">
            <div class="quill-container">
              <QuillEditor v-model:content="form.description" contentType="html" theme="snow" placeholder="详细介绍活动的亮点、内容、注意事项等..." />
            </div>
          </div>
        </div>

        <div class="form-actions">
          <button type="button" class="btn-secondary" @click="router.push('/home/merchant-products')">取消</button>
          <button type="submit" class="btn-primary" :disabled="submitting">
            {{ submitting ? (isEdit ? '更新中...' : '发布中...') : (isEdit ? '确认更新活动' : '确认发布活动') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { getFirstLevelActivityCategories, createActivity, getActivityById, updateActivity, getActivitySchedules, uploadActivityImage } from '../api/app'

const router = useRouter()
const route = useRoute()
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify || { success: alert, error: alert, warning: alert }

const categories = ref([])
const userInfo = ref({})
const isEdit = ref(false)
const activityId = ref(null)

const loadCategories = async () => {
  try {
    const res = await getFirstLevelActivityCategories()
    if (res.code === 200 || res.data) {
      categories.value = (res.data || []).filter(cat => cat.id !== 1)
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadActivityData = async (id) => {
  try {
    const activityRes = await getActivityById(id)
    if (activityRes.code === 200 && activityRes.data) {
      const activity = activityRes.data
      
      form.value.title = activity.title || ''
      form.value.categoryId = activity.categoryId || ''
      form.value.price = activity.price ? (activity.price / 100).toString() : ''
      form.value.capacity = activity.maxStock || ''
      form.value.location = activity.address || ''
      form.value.description = activity.detail || ''
      
      form.value.images = []
      if (activity.images) {
        try {
          const imageUrls = JSON.parse(activity.images)
          if (Array.isArray(imageUrls)) {
            form.value.images = imageUrls.map(url => ({ url, file: null }))
          }
        } catch (e) {
          console.error('解析活动图片失败:', e)
        }
      }
      if (!form.value.images.length && activity.mainImage) {
        form.value.images.push({ url: activity.mainImage, file: null })
      }
      
      if (activity.startTime) {
        const startDate = new Date(activity.startTime)
        const endDate = activity.endTime ? new Date(activity.endTime) : startDate
        
        form.value.availableTimes = [{
          date: startDate.toISOString().split('T')[0],
          startTime: startDate.toTimeString().slice(0, 5),
          endTime: endDate.toTimeString().slice(0, 5)
        }]
      }
      
      try {
        const schedulesRes = await getActivitySchedules(id)
        if (schedulesRes.code === 200 && schedulesRes.data) {
          form.value.processes = schedulesRes.data.map(s => ({
            time: s.stepTime || '',
            content: s.stepDesc || ''
          }))
          if (form.value.processes.length === 0) {
            form.value.processes = [{ time: '', content: '' }]
          }
        }
      } catch (e) {
        console.error('加载活动流程失败:', e)
      }
    }
  } catch (error) {
    console.error('加载活动数据失败:', error)
    notify.error('加载活动数据失败')
  }
}

onMounted(async () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    userInfo.value = JSON.parse(userStr)
  }
  
  if (userInfo.value.role !== 'merchant') {
    notify.warning('只有商家可以发布活动')
    router.push('/home/activity')
    return
  }
  
  await loadCategories()
  
  if (route.query.edit && route.query.id) {
    isEdit.value = true
    activityId.value = route.query.id
    await loadActivityData(route.query.id)
  }
})

const form = ref({
  title: '',
  categoryId: '',
  price: '',
  capacity: '',
  duration: '',
  location: '',
  images: [],
  availableTimes: [
    { date: '', startTime: '', endTime: '' }
  ],
  processes: [
    { time: '', content: '' }
  ],
  description: ''
})

const submitting = ref(false)

const handleMultiImageUpload = async (e) => {
  const files = Array.from(e.target.files)
  if (!files || files.length === 0) return
  
  const remainingSlots = 9 - form.value.images.length
  const filesToProcess = files.slice(0, remainingSlots)
  
  try {
    submitting.value = true
    for (const file of filesToProcess) {
      const formData = new FormData()
      formData.append('file', file)
      
      const res = await uploadActivityImage(formData)
      if (res.code === 200 && res.data && res.data.url) {
        form.value.images.push({
          url: res.data.url,
          file: file
        })
      } else {
        notify.error('部分图片上传失败，请重试')
      }
    }
    notify.success('图片上传成功！')
  } catch (error) {
    console.error('图片上传失败:', error)
    notify.error('图片上传失败，请重试')
  } finally {
    submitting.value = false
    e.target.value = ''
  }
}

const removeImage = (index) => {
  form.value.images.splice(index, 1)
}

const addTime = () => {
  form.value.availableTimes.push({ date: '', startTime: '', endTime: '' })
}

const removeTime = (index) => {
  form.value.availableTimes.splice(index, 1)
}

const addProcess = () => {
  form.value.processes.push({ time: '', content: '' })
}

const removeProcess = (index) => {
  form.value.processes.splice(index, 1)
}

const submitActivity = async () => {
  if (!form.value.description || form.value.description === '<p><br></p>') {
    notify.warning('请填写活动详细介绍')
    return
  }

  if (!form.value.images || form.value.images.length === 0) {
    notify.warning('请至少上传一张活动图片')
    return
  }

  submitting.value = true
  
  try {
    const imageUrls = form.value.images.map(img => img.url)
    const activityData = {
      merchantId: userInfo.value.id,
      categoryId: form.value.categoryId,
      title: form.value.title,
      detail: form.value.description,
      address: form.value.location,
      city: '杭州',
      price: Math.round(form.value.price * 100),
      maxStock: form.value.capacity,
      mainImage: imageUrls[0] || '',
      images: JSON.stringify(imageUrls),
      schedules: form.value.processes.map((p, index) => ({
        stepTitle: (p.content || '流程步骤').substring(0, 128),
        stepTime: p.time,
        stepDesc: p.content,
        sortOrder: index
      }))
    }
    
    if (form.value.availableTimes.length > 0) {
      const firstTime = form.value.availableTimes[0]
      activityData.startTime = new Date(`${firstTime.date}T${firstTime.startTime}`).toISOString().replace('T', ' ').substring(0, 19)
      activityData.endTime = new Date(`${firstTime.date}T${firstTime.endTime}`).toISOString().replace('T', ' ').substring(0, 19)
    }
    
    let response
    if (isEdit.value && activityId.value) {
      response = await updateActivity(activityId.value, activityData)
    } else {
      response = await createActivity(activityData)
    }
    
    if (response.code === 200) {
      submitting.value = false
      notify.success(isEdit.value ? '活动更新成功！' : '活动发布成功！')
      router.push('/home/merchant-products')
    } else {
      notify.error(response.message || (isEdit.value ? '更新失败，请稍后重试' : '发布失败，请稍后重试'))
      submitting.value = false
    }
  } catch (error) {
    console.error(isEdit.value ? '更新活动失败:' : '发布活动失败:', error)
    notify.error('网络错误，请稍后重试')
    submitting.value = false
  }
}
</script>

<style scoped>
.activity-create-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

.header-nav {
  margin-bottom: 24px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.back-button:hover {
  color: #7494ec;
  box-shadow: 0 4px 12px rgba(116, 148, 236, 0.15);
  transform: translateX(-2px);
}

.form-container {
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.form-header {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.form-header h2 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 22px;
}

.form-header p {
  color: #999;
  font-size: 14px;
  margin: 0;
}

.form-section {
  margin-bottom: 40px;
}

.form-section h3 {
  font-size: 16px;
  color: #333;
  margin: 0 0 20px 0;
  padding-left: 10px;
  border-left: 4px solid #7494ec;
}

.form-row {
  display: flex;
  gap: 20px;
}

.flex-1 {
  flex: 1;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.form-group label.required::after {
  content: '*';
  color: #ff4d4f;
  margin-left: 4px;
}

.section-hint {
  font-size: 12px;
  color: #999;
  margin: -4px 0 12px 0;
}

.form-group input[type="text"],
.form-group input[type="number"],
.form-group input[type="date"],
.form-group input[type="time"],
.form-group select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
  background: #fafafa;
}

.form-group input:focus,
.form-group select:focus {
  border-color: #7494ec;
  background: white;
}

/* 动态列表样式 */
.dynamic-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dynamic-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time-input {
  width: 140px !important;
}

.desc-input {
  flex: 1;
}

.separator {
  color: #999;
}

.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-btn.danger {
  color: #ff4d4f;
}

.icon-btn.danger:hover {
  border-color: #ff4d4f;
  background: #fff1f0;
}

.add-line-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #f0f4ff;
  color: #7494ec;
  border: 1px dashed #7494ec;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  align-self: flex-start;
}

.add-line-btn:hover {
  background: #7494ec;
  color: white;
}

/* 多图片上传区样式 */
.multi-upload-area {
  width: 100%;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.image-item, .upload-item {
  aspect-ratio: 1;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.image-item {
  border: 1px solid #e0e0e0;
}

.image-item .preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-item:hover .image-mask {
  opacity: 1;
}

.delete-btn {
  background: #ff4d4f;
  color: white;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all 0.3s;
}

.delete-btn:hover {
  background: #ff7875;
  transform: scale(1.1);
}

.upload-item {
  border: 2px dashed #e0e0e0;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.upload-item:hover {
  border-color: #7494ec;
  background: #f0f4ff;
}

.upload-item i {
  font-size: 28px;
  color: #999;
}

.upload-item span {
  font-size: 13px;
  color: #666;
}

/* 富文本编辑器容器 */
.quill-container {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
}

:deep(.ql-container) {
  min-height: 250px;
  font-size: 14px;
  border: none !important;
  border-top: 1px solid #e0e0e0 !important;
}

:deep(.ql-toolbar) {
  border: none !important;
  background: #fafafa;
}

/* 底部操作区 */
.form-actions {
  display: flex;
  gap: 20px;
  padding-top: 30px;
  border-top: 1px solid #f0f0f0;
  margin-top: 20px;
}

.btn-secondary, .btn-primary {
  padding: 12px 32px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  flex: 1;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #e0e0e0;
}

.btn-secondary:hover {
  background: #e8e8e8;
}

.btn-primary {
  background: #7494ec;
  color: white;
  border: none;
}

.btn-primary:hover:not(:disabled) {
  background: #5b7de0;
  box-shadow: 0 4px 12px rgba(116, 148, 236, 0.3);
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    gap: 0;
  }
  
  .dynamic-item {
    flex-wrap: wrap;
  }
  
  .time-input {
    width: 100% !important;
  }
}
</style>