<template>
  <div class="activity-page">
    <template v-if="!route.params.id">
      <section class="hero">
        <div>
          <h1>非遗活动与体验</h1>
          <p>浏览平台商家发布的非遗体验、展演和课程，支持在线报名与到店核销。</p>
        </div>
      </section>

      <section class="filter-bar">
        <input v-model="filters.keyword" placeholder="搜索活动标题或非遗品类" @keyup.enter="loadActivities" />
        <input v-model="filters.city" placeholder="按城市筛选，如：北京" @keyup.enter="loadActivities" />
        <button @click="loadActivities">搜索</button>
      </section>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="activities.length === 0" class="empty">暂无符合条件的活动</div>
      <section v-else class="activity-list">
        <article v-for="item in activities" :key="item.id" class="activity-card" @click="goDetail(item.id)">
          <img v-if="item.coverImage" :src="item.coverImage" class="cover" alt="活动封面" />
          <div v-else class="cover placeholder">🎭</div>
          <div class="card-body">
            <div class="card-top">
              <h3>{{ item.title }}</h3>
              <span :class="['status-tag', item.status]">{{ activityStatus(item.status) }}</span>
            </div>
            <p class="subtitle">{{ item.subtitle || item.heritageType || '非遗文化活动' }}</p>
            <div class="meta-row">
              <span>📍 {{ item.locationCity || '城市待定' }}</span>
              <span>🕒 {{ formatDate(item.startTime) }}</span>
              <span>💰 {{ item.price ? `¥${(item.price / 100).toFixed(0)}` : '免费' }}</span>
            </div>
            <p class="content">{{ item.content || '暂无活动说明' }}</p>
            <div class="merchant-row">商家：{{ item.merchantName || '合作商家' }}</div>
          </div>
        </article>
      </section>
    </template>

    <template v-else>
      <div class="detail-header">
        <button class="back-btn" @click="goBack">← 返回列表</button>
        <div>
          <h1>{{ detail?.title || '活动详情' }}</h1>
          <p class="subtitle">{{ detail?.merchantName || '合作商家' }}</p>
        </div>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="!detail" class="empty">活动不存在</div>
      <div v-else class="detail-layout">
        <section class="detail-main">
          <img v-if="detail.coverImage" :src="detail.coverImage" class="detail-cover" alt="活动封面" />
          <div v-else class="detail-cover placeholder">🎭</div>
          <div class="detail-card">
            <h3>活动介绍</h3>
            <p>{{ detail.content || '暂无活动介绍' }}</p>
          </div>
          <div class="detail-card">
            <h3>店铺与非遗信息</h3>
            <p>商家：{{ detail.merchantName || '合作商家' }}</p>
            <p>非遗品类：{{ detail.heritageType || '待补充' }}</p>
            <p>商家简介：{{ detail.merchantIntro || '该商家提供线下非遗体验与文化讲解。' }}</p>
          </div>
        </section>

        <aside class="detail-side">
          <div class="booking-card">
            <div class="price">{{ detail.price ? `¥${(detail.price / 100).toFixed(2)}` : '免费' }}</div>
            <div class="meta-item">时间：{{ formatDateTime(detail.startTime) }}</div>
            <div class="meta-item">地点：{{ [detail.locationCity, detail.locationDetail].filter(Boolean).join(' · ') || '待定' }}</div>
            <div class="meta-item">人数：{{ detail.currentParticipants || 0 }}/{{ detail.maxParticipants || '不限' }}</div>
            <button class="book-btn" @click="showBookingModal = true">立即报名</button>
          </div>
        </aside>
      </div>

      <div v-if="showBookingModal && detail" class="modal-mask" @click.self="showBookingModal = false">
        <div class="modal">
          <h3>提交报名</h3>
          <div class="form-group">
            <label>联系人</label>
            <input v-model="bookingForm.participantName" placeholder="请输入联系人姓名" />
          </div>
          <div class="form-group">
            <label>联系电话</label>
            <input v-model="bookingForm.participantPhone" placeholder="请输入联系电话" />
          </div>
          <div class="form-group">
            <label>备注</label>
            <textarea v-model="bookingForm.remark" rows="3" placeholder="可填写同行人数或特殊需求"></textarea>
          </div>
          <div class="modal-actions">
            <button class="btn cancel" @click="showBookingModal = false">取消</button>
            <button class="btn submit" @click="submitBooking">确认报名</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { getCurrentInstance, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { bookActivity, getPublicActivities, getPublicActivityDetail } from '../api/app'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activities = ref([])
const detail = ref(null)
const showBookingModal = ref(false)
const filters = ref({ keyword: '', city: '' })
const bookingForm = ref({
  participantName: '',
  participantPhone: '',
  remark: ''
})

const loadActivities = async () => {
  loading.value = true
  try {
    const response = await getPublicActivities(filters.value)
    activities.value = response.data || []
  } catch (error) {
    notify('获取活动列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const loadDetail = async () => {
  if (!route.params.id) {
    detail.value = null
    return
  }

  loading.value = true
  try {
    const response = await getPublicActivityDetail(route.params.id)
    detail.value = response.data || null
  } catch (error) {
    notify(error.response?.data?.message || '获取活动详情失败', 'error')
    detail.value = null
  } finally {
    loading.value = false
  }
}

const goDetail = (id) => router.push(`/activity/${id}`)
const goBack = () => router.push('/home/activity')

const submitBooking = async () => {
  if (!detail.value) {
    return
  }

  if (!bookingForm.value.participantName || !bookingForm.value.participantPhone) {
    notify('请填写联系人和联系电话', 'error')
    return
  }

  try {
    await bookActivity(detail.value.id, bookingForm.value)
    notify('报名成功，等待商家核销', 'success')
    showBookingModal.value = false
    detail.value.currentParticipants = (detail.value.currentParticipants || 0) + 1
  } catch (error) {
    notify(error.response?.data?.message || '报名失败', 'error')
  }
}

const activityStatus = (status) => ({
  signup: '报名中',
  ongoing: '进行中',
  full: '已满员',
  ended: '已结束'
}[status] || status)

const formatDate = (value) => value ? new Date(value).toLocaleDateString('zh-CN') : '待定'
const formatDateTime = (value) => value ? new Date(value).toLocaleString('zh-CN') : '待定'

watch(() => route.params.id, async () => {
  if (route.params.id) {
    await loadDetail()
  } else {
    await loadActivities()
  }
}, { immediate: true })
</script>

<style scoped>
.activity-page { min-height: 100vh; background: #f5f7fa; padding: 20px 20px 90px; }
.hero { max-width: 1200px; margin: 0 auto 20px; padding: 28px; border-radius: 18px; background: linear-gradient(135deg, #0f766e, #14b8a6); color: #fff; }
.hero h1, .detail-header h1 { margin: 0 0 8px; font-size: 28px; }
.hero p, .subtitle { margin: 0; color: rgba(255,255,255,0.88); }
.filter-bar { max-width: 1200px; margin: 0 auto 20px; display: grid; grid-template-columns: 1fr 240px 120px; gap: 12px; }
.filter-bar input, .filter-bar button { padding: 12px 14px; border: 1px solid #d1d5db; border-radius: 10px; font-size: 14px; }
.filter-bar button { background: #0f766e; color: #fff; cursor: pointer; border: none; }
.loading, .empty { max-width: 1200px; margin: 0 auto; text-align: center; padding: 80px 20px; color: #9ca3af; }
.activity-list { max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.activity-card { background: #fff; border-radius: 16px; overflow: hidden; border: 1px solid #e5e7eb; cursor: pointer; }
.cover { width: 100%; height: 210px; object-fit: cover; background: #f3f4f6; }
.cover.placeholder, .detail-cover.placeholder { display: flex; align-items: center; justify-content: center; font-size: 60px; }
.card-body { padding: 18px; }
.card-top { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; margin-bottom: 8px; }
.card-top h3 { margin: 0; font-size: 18px; color: #111827; }
.status-tag { padding: 4px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.status-tag.signup { background: #d1fae5; color: #059669; }
.status-tag.ongoing { background: #dbeafe; color: #2563eb; }
.status-tag.full, .status-tag.ended { background: #f3f4f6; color: #6b7280; }
.subtitle { color: #6b7280; }
.meta-row { display: flex; flex-wrap: wrap; gap: 10px 16px; font-size: 13px; color: #4b5563; margin: 10px 0 12px; }
.content { color: #6b7280; line-height: 1.6; min-height: 66px; }
.merchant-row { font-size: 13px; color: #0f766e; font-weight: 500; margin-top: 12px; }
.detail-header { max-width: 1200px; margin: 0 auto 20px; display: flex; gap: 16px; align-items: flex-start; }
.back-btn { padding: 9px 14px; background: #fff; border: 1px solid #d1d5db; border-radius: 10px; cursor: pointer; }
.detail-layout { max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: 1.4fr 0.9fr; gap: 20px; }
.detail-cover { width: 100%; height: 320px; object-fit: cover; border-radius: 18px; margin-bottom: 16px; background: #f3f4f6; }
.detail-card, .booking-card { background: #fff; border-radius: 16px; padding: 20px; border: 1px solid #e5e7eb; margin-bottom: 16px; }
.detail-card h3 { margin: 0 0 12px; font-size: 17px; color: #111827; }
.detail-card p { color: #4b5563; line-height: 1.7; white-space: pre-line; }
.price { font-size: 30px; font-weight: 700; color: #dc2626; margin-bottom: 14px; }
.meta-item { font-size: 14px; color: #4b5563; margin-bottom: 10px; }
.book-btn { width: 100%; padding: 12px 18px; background: #0f766e; color: #fff; border: none; border-radius: 10px; cursor: pointer; font-size: 15px; font-weight: 600; margin-top: 12px; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { width: 420px; max-width: 92vw; background: #fff; border-radius: 16px; padding: 24px; }
.modal h3 { margin: 0 0 18px; }
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.form-group input, .form-group textarea { padding: 10px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }
.btn { padding: 9px 18px; border: none; border-radius: 8px; cursor: pointer; }
.btn.cancel { background: #f3f4f6; color: #374151; }
.btn.submit { background: #0f766e; color: #fff; }
@media (max-width: 900px) {
  .filter-bar { grid-template-columns: 1fr; }
  .detail-layout { grid-template-columns: 1fr; }
}
</style>
