<template>
  <div class="activity-booking-page">
    <div class="header-nav">
      <button class="back-button" @click="goBack">
        <i class="bx bx-arrow-back"></i>
        <span>返回详情</span>
      </button>
      <div class="breadcrumb">
        <span @click="router.push('/home/activity')">活动列表</span>
        <i class="bx bx-chevron-right"></i>
        <span @click="goBack">活动详情</span>
        <i class="bx bx-chevron-right"></i>
        <span>提交报名</span>
      </div>
    </div>

    <div v-if="loading" class="state-card">
      <i class="bx bx-loader-alt bx-spin"></i>
      <p>加载中...</p>
    </div>

    <div v-else-if="!activity" class="state-card">
      <i class="bx bx-calendar-x"></i>
      <p>活动不存在或暂不可预约</p>
    </div>

    <div v-else class="booking-layout">
      <section class="booking-main">
        <div class="section-card">
          <h1>填写报名信息</h1>
          <p class="section-desc">提交后会生成活动报名记录，商家可在后台查看并核销。</p>

          <div class="activity-summary">
            <img :src="activity.coverImage || placeholderCover" :alt="activity.title" />
            <div class="summary-info">
              <h2>{{ activity.title }}</h2>
              <div class="summary-tags">
                <span>{{ activity.heritageType || '非遗活动' }}</span>
                <span>{{ activityTypeLabel(activity.activityType) }}</span>
              </div>
              <p>{{ formatDateTime(activity.startTime) }}</p>
              <p>{{ formatLocation(activity) }}</p>
            </div>
          </div>
        </div>

        <div class="section-card">
          <div class="form-grid">
            <label class="field">
              <span>联系人姓名</span>
              <input v-model.trim="form.participantName" type="text" placeholder="请输入联系人姓名" />
            </label>

            <label class="field">
              <span>联系电话</span>
              <input v-model.trim="form.participantPhone" type="tel" placeholder="请输入手机号" />
            </label>

            <label class="field">
              <span>报名人数</span>
              <div class="quantity-box">
                <button type="button" @click="changeCount(-1)">-</button>
                <input v-model.number="form.participantCount" type="number" min="1" :max="remainingSlots" />
                <button type="button" @click="changeCount(1)">+</button>
              </div>
              <small>当前剩余名额 {{ remainingSlots }} 人</small>
            </label>

            <label class="field full">
              <span>备注</span>
              <textarea
                v-model.trim="form.remark"
                rows="4"
                maxlength="200"
                placeholder="可填写同行人说明、到场备注或其他需求"
              ></textarea>
            </label>
          </div>
        </div>
      </section>

      <aside class="booking-side">
        <div class="section-card sticky-card">
          <h3>报名摘要</h3>

          <div class="summary-line">
            <span>活动单价</span>
            <strong>{{ priceText }}</strong>
          </div>
          <div class="summary-line">
            <span>报名人数</span>
            <strong>{{ form.participantCount }} 人</strong>
          </div>
          <div class="summary-line">
            <span>活动时间</span>
            <strong>{{ shortDateTime(activity.startTime) }}</strong>
          </div>

          <div class="total-box">
            <span>合计</span>
            <strong>{{ totalPriceText }}</strong>
          </div>

          <button class="submit-btn" :disabled="submitting || !canSubmit" @click="submitBooking">
            {{ submitting ? '提交中...' : '确认报名' }}
          </button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { bookActivity, getPublicActivityDetail } from '../../api/app'

const route = useRoute()
const router = useRouter()
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const placeholderCover = 'https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=1200&q=80'

const loading = ref(false)
const submitting = ref(false)
const activity = ref(null)
const form = ref({
  participantName: '',
  participantPhone: '',
  participantCount: 1,
  remark: ''
})

const readStoredUser = () => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return {}
  }
  try {
    return JSON.parse(raw)
  } catch (error) {
    return {}
  }
}

const remainingSlots = computed(() => {
  if (!activity.value?.maxParticipants) {
    return 99
  }
  const current = Number(activity.value.currentParticipants || 0)
  const max = Number(activity.value.maxParticipants || 0)
  return Math.max(0, max - current)
})

const canSubmit = computed(() => {
  const phone = /^1[3-9]\d{9}$/
  return Boolean(
    activity.value &&
    form.value.participantName &&
    phone.test(form.value.participantPhone) &&
    form.value.participantCount >= 1 &&
    form.value.participantCount <= remainingSlots.value
  )
})

const priceValue = computed(() => Number(activity.value?.price || 0) / 100)
const priceText = computed(() => (priceValue.value > 0 ? `¥${priceValue.value.toFixed(2)}` : '免费'))
const totalPriceText = computed(() => {
  const total = priceValue.value * Number(form.value.participantCount || 0)
  return total > 0 ? `¥${total.toFixed(2)}` : '免费'
})

const showError = (message) => notify?.error?.(message) ?? window.alert(message)
const showSuccess = (message) => notify?.success?.(message) ?? window.alert(message)

const loadActivity = async () => {
  loading.value = true
  try {
    const response = await getPublicActivityDetail(route.params.id)
    if (response.code !== 200) {
      throw new Error(response.message || '加载活动失败')
    }

    const detail = response.data || null
    const current = Number(detail?.currentParticipants || 0)
    const max = Number(detail?.maxParticipants || 0)
    if (detail?.status === 'ended' || detail?.status === 'full' || (detail?.maxParticipants && current >= max)) {
      throw new Error('该活动当前不可报名')
    }

    activity.value = detail
    form.value.participantCount = Math.min(Math.max(form.value.participantCount, 1), Math.max(remainingSlots.value, 1))
  } catch (error) {
    activity.value = null
    showError(error.message || '加载活动失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push(`/activity/${route.params.id}`)
}

const changeCount = (delta) => {
  const next = Number(form.value.participantCount || 1) + delta
  form.value.participantCount = Math.min(Math.max(next, 1), remainingSlots.value || 1)
}

const submitBooking = async () => {
  if (!canSubmit.value || !activity.value) {
    showError('请完善报名信息后再提交')
    return
  }

  submitting.value = true
  try {
    const payload = {
      participantName: form.value.participantName,
      participantPhone: form.value.participantPhone,
      participantCount: form.value.participantCount,
      remark: form.value.remark || undefined
    }
    const response = await bookActivity(activity.value.id, payload)
    if (response.code !== 200) {
      throw new Error(response.message || '报名失败')
    }

    showSuccess('报名成功')
    router.push('/personal/orders')
  } catch (error) {
    showError(error.message || '报名失败')
  } finally {
    submitting.value = false
  }
}

const formatDateTime = (value) => value ? new Date(value).toLocaleString('zh-CN') : '时间待定'
const shortDateTime = (value) => value ? new Date(value).toLocaleString('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit'
}) : '时间待定'
const formatLocation = (item) => [item.locationProvince, item.locationCity, item.locationDistrict, item.locationDetail]
  .filter(Boolean)
  .join(' / ') || '地点待定'

const activityTypeLabel = (type) => ({
  offline: '线下体验',
  online: '线上课程',
  exhibition: '展览'
}[type] || '活动')

onMounted(() => {
  const user = readStoredUser()
  form.value.participantName = user.nickname || user.username || ''
  form.value.participantPhone = user.phone || ''
  loadActivity()
})
</script>

<style scoped>
.activity-booking-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  background: #f8f5f0;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: 1px solid #d9cfc1;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8b8074;
}

.breadcrumb span {
  cursor: default;
}

.breadcrumb span:first-child,
.breadcrumb span:nth-child(3) {
  cursor: pointer;
}

.state-card,
.section-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #d9cfc1;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(44, 44, 44, 0.06);
}

.state-card {
  text-align: center;
  padding: 80px 20px;
  color: #8b8074;
}

.state-card i {
  font-size: 40px;
}

.booking-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) 340px;
  gap: 24px;
}

.booking-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  padding: 24px;
}

.section-card h1,
.section-card h3 {
  margin: 0 0 10px;
  color: #2c2c2c;
}

.section-desc {
  margin: 0;
  color: #8b8074;
}

.activity-summary {
  margin-top: 20px;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
}

.activity-summary img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  border-radius: 12px;
}

.summary-info h2 {
  margin: 0 0 10px;
  font-size: 22px;
  color: #2c2c2c;
}

.summary-info p {
  margin: 8px 0 0;
  color: #5a5045;
}

.summary-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-tags span {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(157, 41, 41, 0.08);
  color: #9d2929;
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field.full {
  grid-column: 1 / -1;
}

.field span {
  color: #2c2c2c;
  font-weight: 600;
}

.field input,
.field textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #d9cfc1;
  border-radius: 12px;
  background: #fff;
  font-size: 14px;
  outline: none;
}

.field input:focus,
.field textarea:focus {
  border-color: #9d2929;
}

.field small {
  color: #8b8074;
}

.quantity-box {
  display: inline-grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: stretch;
  max-width: 180px;
  border: 1px solid #d9cfc1;
  border-radius: 12px;
  overflow: hidden;
}

.quantity-box button,
.quantity-box input {
  border: none;
  background: transparent;
  height: 46px;
  text-align: center;
}

.quantity-box button {
  cursor: pointer;
  background: #f6efe5;
  font-size: 18px;
}

.sticky-card {
  position: sticky;
  top: 24px;
}

.summary-line,
.total-box {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.summary-line {
  padding: 12px 0;
  color: #5a5045;
  border-bottom: 1px solid rgba(217, 207, 193, 0.7);
}

.total-box {
  padding: 20px 0;
  color: #2c2c2c;
  font-size: 18px;
  font-weight: 700;
}

.submit-btn {
  width: 100%;
  padding: 14px 18px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #9d2929, #b33030);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.submit-btn:disabled {
  background: #cbbfb1;
  cursor: not-allowed;
}

@media (max-width: 960px) {
  .booking-layout {
    grid-template-columns: 1fr;
  }

  .sticky-card {
    position: static;
  }
}

@media (max-width: 768px) {
  .activity-booking-page {
    padding: 12px;
  }

  .activity-summary,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
