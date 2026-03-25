<template>
  <div class="ai-heritage-route">
    <div class="route-header">
      <h1>AI 非遗体验路线规划</h1>
      <p>基于 NLP + RAG 技术的智能非遗文化体验路线规划</p>
    </div>
    
    <div class="route-content">
      <div class="input-section">
        <div class="form-group">
          <label>输入你的需求</label>
          <textarea 
            v-model="userInput" 
            placeholder="例如：我想去北京，玩3天，喜欢传统工艺和美食" 
            rows="4"
          ></textarea>
        </div>
        <div v-if="error" class="error-message">{{ error }}</div>
        <button class="generate-btn" @click="generateRoute" :disabled="loading">
          {{ loading ? '生成中...' : '生成路线' }}
        </button>
      </div>
      
      <div class="result-section" v-if="routeResult">
        <h2>推荐路线</h2>
        
        <!-- 路线概览 -->
        <div class="route-overview">
          <div class="overview-item">
            <span class="label">目的地：</span>
            <span class="value">{{ routeResult.destination }}</span>
          </div>
          <div class="overview-item">
            <span class="label">旅行天数：</span>
            <span class="value">{{ routeResult.days }}天</span>
          </div>
          <div class="overview-item">
            <span class="label">兴趣偏好：</span>
            <span class="value">{{ routeResult.preferences.join('、') }}</span>
          </div>
        </div>
        
        <!-- 每日路线 -->
        <div class="daily-routes">
          <div class="day-route" v-for="(day, index) in routeResult.itinerary" :key="index">
            <h3>第 {{ index + 1 }} 天</h3>
            
            <div class="day-activities">
              <div class="activity" v-for="(activity, actIndex) in day.activities" :key="actIndex">
                <div class="activity-time">{{ activity.time }}</div>
                <div class="activity-info">
                  <h4>{{ activity.name }}</h4>
                  <p class="activity-description">{{ activity.description }}</p>
                  
                  <!-- 商家/店铺信息 -->
                  <div class="business-info" v-if="activity.business">
                    <div class="business-name">{{ activity.business.name }}</div>
                    <div class="business-address">{{ activity.business.address }}</div>
                    <div class="business-phone">{{ activity.business.phone }}</div>
                  </div>
                  
                  <!-- 活动信息 -->
                  <div class="activity-details" v-if="activity.details">
                    <div class="detail-item" v-for="(detail, detailIndex) in activity.details" :key="detailIndex">
                      <span class="detail-label">{{ detail.label }}：</span>
                      <span class="detail-value">{{ detail.value }}</span>
                    </div>
                  </div>
                  
                  <div class="activity-tags">
                    <span class="tag" v-for="(tag, tagIndex) in activity.tags" :key="tagIndex">{{ tag }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 推荐商家 -->
        <div class="recommended-businesses" v-if="routeResult.recommendedBusinesses">
          <h3>推荐商家</h3>
          <div class="business-list">
            <div class="business-card" v-for="(business, index) in routeResult.recommendedBusinesses" :key="index">
              <h4>{{ business.name }}</h4>
              <p class="business-address">{{ business.address }}</p>
              <p class="business-description">{{ business.description }}</p>
              <div class="business-tags">
                <span class="tag" v-for="(tag, tagIndex) in business.tags" :key="tagIndex">{{ tag }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 活动信息 -->
        <div class="activities-section" v-if="routeResult.activities">
          <h3>相关活动</h3>
          <div class="activity-list">
            <div class="activity-card" v-for="(activity, index) in routeResult.activities" :key="index">
              <h4>{{ activity.title }}</h4>
              <p class="activity-date">{{ activity.date }}</p>
              <p class="activity-description">{{ activity.description }}</p>
              <div class="activity-location">{{ activity.location }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { generateHeritageRoute } from '../api/app'

const userInput = ref('')
const routeResult = ref(null)
const loading = ref(false)
const error = ref('')

const generateRoute = async () => {
  if (!userInput.value.trim()) {
    error.value = '请输入你的需求'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    const response = await generateHeritageRoute({
      userInput: userInput.value
    })
    
    routeResult.value = response.data
  } catch (err) {
    error.value = '生成路线失败，请稍后重试'
    console.error('路线生成失败:', err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-heritage-route {
  min-height: 100vh;
  padding: 20px;
  background: #f5f5f5;
}

.route-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px 0;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.route-header h1 {
  color: #333;
  margin-bottom: 10px;
}

.route-header p {
  color: #666;
  font-size: 16px;
}

.route-content {
  max-width: 1200px;
  margin: 0 auto;
}

.input-section {
  background: white;
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.form-group textarea {
  width: 100%;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 16px;
  resize: vertical;
  font-family: inherit;
}

.generate-btn {
  width: 100%;
  padding: 12px;
  background: #7494ec;
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.generate-btn:hover {
  background: #5a78d1;
}

.generate-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.error-message {
  color: #ff4d4f;
  margin-bottom: 15px;
  font-size: 14px;
}

.result-section {
  background: white;
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.result-section h2 {
  color: #333;
  margin-bottom: 20px;
}

.result-section h3 {
  color: #5a78d1;
  margin-bottom: 15px;
  font-size: 18px;
}

/* 路线概览 */
.route-overview {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 30px;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.overview-item .label {
  font-weight: 500;
  color: #666;
}

.overview-item .value {
  color: #333;
}

/* 每日路线 */
.daily-routes {
  margin-bottom: 40px;
}

.day-route {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.day-route:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.day-activities {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.activity {
  display: flex;
  gap: 15px;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
}

.activity-time {
  min-width: 120px;
  font-weight: 500;
  color: #5a78d1;
}

.activity-info h4 {
  margin-bottom: 5px;
  color: #333;
}

.activity-description {
  margin-bottom: 10px;
  color: #666;
  font-size: 14px;
}

/* 商家信息 */
.business-info {
  margin: 10px 0;
  padding: 10px;
  background: #e6ecff;
  border-radius: 5px;
  font-size: 14px;
}

.business-name {
  font-weight: 500;
  margin-bottom: 5px;
  color: #5a78d1;
}

.business-address,
.business-phone {
  margin-bottom: 3px;
  color: #666;
}

/* 活动详情 */
.activity-details {
  margin: 10px 0;
  font-size: 14px;
}

.detail-item {
  margin-bottom: 5px;
}

.detail-label {
  font-weight: 500;
  color: #666;
}

.detail-value {
  color: #333;
}

/* 标签 */
.activity-tags {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.tag {
  padding: 3px 8px;
  background: #e6ecff;
  color: #5a78d1;
  border-radius: 12px;
  font-size: 12px;
}

/* 推荐商家 */
.recommended-businesses {
  margin-top: 40px;
  margin-bottom: 40px;
}

.business-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.business-card {
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.business-card h4 {
  margin-bottom: 10px;
  color: #333;
}

.business-card .business-address {
  margin-bottom: 10px;
  color: #666;
  font-size: 14px;
}

.business-card .business-description {
  margin-bottom: 15px;
  color: #666;
  font-size: 14px;
  line-height: 1.4;
}

.business-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 活动信息 */
.activities-section {
  margin-top: 40px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.activity-card {
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.activity-card h4 {
  margin-bottom: 10px;
  color: #333;
}

.activity-date {
  margin-bottom: 10px;
  color: #5a78d1;
  font-weight: 500;
  font-size: 14px;
}

.activity-card .activity-description {
  margin-bottom: 15px;
  color: #666;
  font-size: 14px;
  line-height: 1.4;
}

.activity-location {
  color: #666;
  font-size: 14px;
}

@media (max-width: 768px) {
  .input-section,
  .result-section {
    padding: 20px;
  }
  
  .route-overview {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .activity {
    flex-direction: column;
    gap: 10px;
  }
  
  .activity-time {
    min-width: auto;
  }
  
  .business-list {
    grid-template-columns: 1fr;
  }
}
</style>