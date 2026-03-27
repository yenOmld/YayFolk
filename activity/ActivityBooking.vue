<template>
  <div class="activity-booking-page">
    <div class="header-nav">
      <div class="back-button" @click="goBack">
        <i class='bx bx-arrow-back'></i>
        <span>返回</span>
      </div>
      <div class="breadcrumb">
        <span @click="router.push('/home/activity')">活动列表</span>
        <i class='bx bx-chevron-right'></i>
        <span @click="goBack">活动详情</span>
        <i class='bx bx-chevron-right'></i>
        <span>活动预约</span>
      </div>
    </div>

    <div class="booking-container">
      <div class="main-form">
        <h2>填写预约信息</h2>
        
        <!-- 活动简要信息卡片 -->
        <div class="activity-summary" v-if="activity.id">
          <img :src="activity.image || 'https://via.placeholder.com/400'" alt="cover" />
          <div class="summary-info">
            <h3>{{ activity.title }}</h3>
            <p><i class='bx bx-map'></i> {{ activity.location }}</p>
            <div class="price">
              <span v-if="activity.price === 0" class="free">免费</span>
              <span v-else>¥ {{ activity.price?.toFixed(2) }}</span>
            </div>
          </div>
        </div>

        <form class="form-content" @submit.prevent="submitBooking">
          <!-- 日期选择 -->
          <div class="form-group">
            <label class="required">选择预约日期</label>
            <div class="date-options">
              <div 
                v-for="date in availableDates" 
                :key="date.value"
                class="date-card"
                :class="{ active: form.date === date.value, disabled: date.full }"
                @click="!date.full && (form.date = date.value)"
              >
                <div class="date-day">{{ date.day }}</div>
                <div class="date-desc">{{ date.desc }}</div>
                <div class="date-status" v-if="date.full">已满</div>
              </div>
            </div>
          </div>

          <!-- 参与人数 -->
          <div class="form-group">
            <label class="required">参与人数</label>
            <div class="quantity-control">
              <button type="button" @click="form.persons > 1 && form.persons--"><i class='bx bx-minus'></i></button>
              <input type="number" v-model.number="form.persons" min="1" :max="maxPersons" readonly />
              <button type="button" @click="form.persons < maxPersons && form.persons++"><i class='bx bx-plus'></i></button>
            </div>
            <span class="hint">每单最多可预约 {{ maxPersons }} 人</span>
          </div>

          <!-- 联系人信息 -->
          <div class="form-group">
            <label class="required">联系人姓名</label>
            <input type="text" v-model="form.name" placeholder="请输入真实姓名" required />
          </div>

          <div class="form-group">
            <label class="required">联系电话</label>
            <input type="tel" v-model="form.phone" placeholder="请输入手机号码" required pattern="^1[3-9]\\d{9}$" />
          </div>

          <div class="form-group">
            <label>备注留言 (选填)</label>
            <textarea v-model="form.remark" placeholder="如有特殊需求请留言告诉我们" rows="3"></textarea>
          </div>
        </form>
      </div>

      <!-- 右侧：订单摘要与支付 -->
      <div class="order-summary">
        <h3>订单摘要</h3>
        
        <div class="summary-list">
          <div class="summary-item">
            <span>活动单价</span>
            <span>{{ activity.price === 0 ? '免费' : `¥ ${activity.price?.toFixed(2)}` }}</span>
          </div>
          <div class="summary-item">
            <span>预约人数</span>
            <span>x {{ form.persons }}</span>
          </div>
          <div class="summary-item" v-if="form.date">
            <span>预约日期</span>
            <span>{{ form.date }}</span>
          </div>
        </div>

        <div class="total-price-box">
          <span>总计应付</span>
          <span class="total-amount">
            {{ totalPrice === 0 ? '免费' : `¥ ${totalPrice.toFixed(2)}` }}
          </span>
        </div>

        <button 
          class="pay-btn" 
          :disabled="!isFormValid || submitting"
          @click="submitBooking"
        >
          {{ submitting ? '处理中...' : (totalPrice === 0 ? '确认预约' : '去支付') }}
        </button>
      </div>
    </div>

    <!-- 支付成功弹窗 -->
    <div class="modal" v-if="showSuccessModal">
      <div class="modal-content success-modal">
        <i class='bx bx-check-circle success-icon'></i>
        <h2>预约成功！</h2>
        <p>您的预约信息已提交，请准时参加活动。</p>
        <button class="btn-primary" @click="router.push('/home/activity')">返回活动列表</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getActivityById, createActivityReserve } from '../api/app'

const route = useRoute()
const router = useRouter()
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify || { success: alert, error: alert, warning: alert }

const activity = ref({})
const availableDates = ref([])
const maxPersons = ref(10)
const userInfo = ref({})

const form = ref({
  date: '',
  persons: 1,
  name: '',
  phone: '',
  remark: ''
})

const submitting = ref(false)
const showSuccessModal = ref(false)

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const fetchActivityDetail = async (id) => {
  try {
    const activityRes = await getActivityById(id)
    if (activityRes.code === 200 || activityRes.data) {
      const activityData = activityRes.data
      activity.value = {
        id: activityData.id,
        title: activityData.title,
        location: activityData.address,
        price: activityData.price / 100,
        image: activityData.mainImage,
        capacity: activityData.maxStock,
        joined: activityData.bookedStock
      }
      
      maxPersons.value = Math.min(10, activityData.maxStock - activityData.bookedStock)
      
      if (activityData.startTime) {
        const startDate = new Date(activityData.startTime)
        const dateValue = startDate.toISOString().split('T')[0]
        const dayStr = startDate.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
        const weekDay = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][startDate.getDay()]
        
        availableDates.value = [{
          value: dateValue,
          day: dayStr,
          desc: weekDay,
          full: activityData.bookedStock >= activityData.maxStock
        }]
        
        form.value.date = dateValue
      }
    }
  } catch (error) {
    console.error('获取活动详情失败:', error)
  }
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    userInfo.value = JSON.parse(userStr)
  }
  
  const id = route.params.id
  fetchActivityDetail(id)
})

const totalPrice = computed(() => {
  return (activity.value.price || 0) * form.value.persons
})

const isFormValid = computed(() => {
  const phoneRegex = /^1[3-9]\d{9}$/
  return form.value.date && form.value.name.trim() && phoneRegex.test(form.value.phone)
})

const goBack = () => {
  router.back()
}

const submitBooking = async () => {
  if (!isFormValid.value) return
  
  submitting.value = true
  
  try {
    const reserveData = {
      userId: userInfo.value.id,
      activityId: activity.value.id,
      participantCount: form.value.persons,
      contactName: form.value.name,
      contactPhone: form.value.phone,
      remark: form.value.remark
    }
    
    const response = await createActivityReserve(reserveData)
    
    if (response.code === 200) {
      submitting.value = false
      showSuccessModal.value = true
    } else {
      notify.error(response.message || '预约失败，请稍后重试')
      submitting.value = false
    }
  } catch (error) {
    console.error('预约失败:', error)
    notify.error('网络错误，请稍后重试')
    submitting.value = false
  }
}
</script>

<style scoped>
.activity-booking-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  padding: 6px 12px;
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

.breadcrumb {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb span:not(:last-child) {
  cursor: pointer;
  transition: color 0.3s;
}

.breadcrumb span:not(:last-child):hover {
  color: #7494ec;
}

.booking-container {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 左侧表单 */
.main-form {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.main-form h2 {
  margin: 0 0 24px 0;
  font-size: 20px;
  color: #333;
}

.activity-summary {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 30px;
}

.activity-summary img {
  width: 120px;
  height: 90px;
  border-radius: 6px;
  object-fit: cover;
}

.summary-info {
  display: flex;
  flex-direction: column;
}

.summary-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #333;
}

.summary-info p {
  margin: 0 0 auto 0;
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
}

.summary-info .price {
  font-size: 18px;
  font-weight: bold;
  color: #ff2442;
}

.summary-info .free {
  color: #52c41a;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 10px;
}

.form-group label.required::after {
  content: '*';
  color: #ff4d4f;
  margin-left: 4px;
}

.form-group input[type="text"],
.form-group input[type="tel"],
.form-group textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: #7494ec;
}

.date-options {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.date-card {
  width: 100px;
  padding: 12px 0;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.date-card:hover:not(.disabled) {
  border-color: #7494ec;
}

.date-card.active {
  border-color: #7494ec;
  background: #f0f4ff;
}

.date-card.disabled {
  background: #f5f5f5;
  color: #999;
  cursor: not-allowed;
  border-color: #f0f0f0;
}

.date-day {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
}

.date-desc {
  font-size: 12px;
}

.date-status {
  position: absolute;
  top: 0;
  right: 0;
  background: #ccc;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-bottom-left-radius: 6px;
}

.quantity-control {
  display: inline-flex;
  align-items: center;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
}

.quantity-control button {
  width: 40px;
  height: 40px;
  background: #f8f9fa;
  border: none;
  cursor: pointer;
  transition: background 0.3s;
}

.quantity-control button:hover {
  background: #e9ecef;
}

.quantity-control input {
  width: 60px;
  height: 40px;
  border: none;
  border-left: 1px solid #e0e0e0;
  border-right: 1px solid #e0e0e0;
  text-align: center;
  outline: none;
  font-size: 15px;
  background: white;
}

.hint {
  margin-left: 12px;
  font-size: 12px;
  color: #999;
}

/* 右侧摘要 */
.order-summary {
  width: 320px;
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 80px;
}

.order-summary h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #333;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #666;
}

.total-price-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px dashed #e0e0e0;
  margin-bottom: 24px;
}

.total-price-box span:first-child {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.total-amount {
  font-size: 24px;
  font-weight: bold;
  color: #ff2442;
}

.pay-btn {
  width: 100%;
  padding: 14px;
  background: #7494ec;
  color: white;
  border: none;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.pay-btn:hover:not(:disabled) {
  background: #5b7de0;
  box-shadow: 0 4px 12px rgba(116, 148, 236, 0.3);
}

.pay-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 弹窗 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.success-modal {
  background: white;
  padding: 40px;
  border-radius: 16px;
  text-align: center;
  width: 90%;
  max-width: 400px;
}

.success-icon {
  font-size: 64px;
  color: #52c41a;
  margin-bottom: 16px;
}

.success-modal h2 {
  margin: 0 0 12px 0;
  color: #333;
}

.success-modal p {
  color: #666;
  margin: 0 0 24px 0;
}

.btn-primary {
  padding: 12px 32px;
  background: #7494ec;
  color: white;
  border: none;
  border-radius: 24px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-primary:hover {
  background: #5b7de0;
}

@media (max-width: 900px) {
  .booking-container {
    flex-direction: column;
  }
  
  .order-summary {
    width: 100%;
    position: static;
  }
}
</style>
