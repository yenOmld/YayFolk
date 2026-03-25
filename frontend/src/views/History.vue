<template>
  <div class="history-page">
    <div class="settings-header">
      <button class="back-btn" @click="goBack">
        <i class='bx bxs-chevron-left'></i>
      </button>
      <h1>{{ $t('history.title') }}</h1>
      <button class="clear-btn" @click="clearHistory" v-if="history.length > 0">
        {{ $t('common.clear') }}
      </button>
    </div>

    <div class="history-content">
      <div class="empty-state" v-if="history.length === 0">
        <i class='bx bxs-history'></i>
        <h3>{{ $t('history.emptyTitle') }}</h3>
        <p>{{ $t('history.emptyHint') }}</p>
        <button class="btn primary" @click="goToTranslate">{{ $t('history.goTranslate') }}</button>
      </div>

      <div class="history-grid" v-else>
        <div 
          class="history-card" 
          v-for="item in history" 
          :key="item.id"
          @click="goToPost(item.id)"
        >
          <div class="card-image" v-if="item.images && item.images.length > 0">
            <img :src="item.images[0]" alt="Post image" />
          </div>
          <div class="card-image placeholder" v-else>
            <i class='bx bx-image'></i>
          </div>
          <div class="card-info">
            <h4 class="card-title">{{ item.title || $t('history.untitled') }}</h4>
            <div class="card-meta">
              <span class="card-time">{{ item.viewedAt }}</span>
              <span class="card-views" v-if="item.viewCount">
                <i class='bx bx-show'></i> {{ item.viewCount }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { clearMyDiscoverHistory, getMyDiscoverHistory } from '../api/app'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm

const router = useRouter()
const { t } = useI18n()
const history = ref([])

const goBack = () => {
  router.back()
}

const goToTranslate = () => {
  router.push('/home/discover')
}

const goToPost = (postId) => {
  router.push({
    path: '/home/discover',
    query: { postId }
  })
}

const loadHistory = async () => {
  try {
    const response = await getMyDiscoverHistory()
    if (response.code === 200) {
      history.value = Array.isArray(response.data) ? response.data : []
    } else {
      notify.error(response.message || t('history.loadFailed'))
    }
  } catch (error) {
    notify.error(t('history.loadFailedRetry'))
  }
}

const clearHistory = async () => {
  confirm({
    title: t('history.confirmClearTitle'),
    message: t('history.confirmClear'),
    confirmText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onConfirm: async () => {
      try {
        const response = await clearMyDiscoverHistory()
        if (response.code === 200) {
          history.value = []
          notify.success(t('history.clearSuccess'))
        } else {
          notify.error(response.message || t('history.clearFailed'))
        }
      } catch (error) {
        notify.error(t('history.clearFailedRetry'))
      }
    }
  })
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.history-page {
  min-height: 100vh;
  width: 100%;
  margin: 0 auto;
}

@media (min-width: 768px) {
  .history-page {
    width: 85%;
    margin: 0 auto;
  }
}

@media (min-width: 1024px) {
  .history-page {
    width: 70%;
    margin: 0 auto;
  }
}

.settings-header {
  background: white;
  padding: 15px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  border-radius: 0px 0px 12px 12px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #333;
  margin-right: 15px;
}

.settings-header h1 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  flex: 1;
}

.clear-btn {
  background: none;
  border: none;
  font-size: 14px;
  color: #ff4757;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.clear-btn:hover {
  background: #ffe6e8;
}

.history-content {
  padding: 20px;
}

.empty-state {
  background: white;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.empty-state i {
  font-size: 60px;
  color: #7494ec;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-state h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.empty-state p {
  margin: 0 0 30px 0;
  color: #666;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn.primary {
  background: #7494ec;
  color: white;
}

.btn.primary:hover {
  background: #6381d9;
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

@media (min-width: 768px) {
  .history-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1024px) {
  .history-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.history-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.history-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.15);
}

.card-image {
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-image.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.card-image.placeholder i {
  font-size: 40px;
  color: #ccc;
}

.card-info {
  padding: 12px;
}

.card-title {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.card-views {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-views i {
  font-size: 14px;
}

@media (max-width: 768px) {
  .history-content {
    padding: 10px;
  }
  
  .history-grid {
    gap: 10px;
  }
  
  .card-info {
    padding: 10px;
  }
  
  .card-title {
    font-size: 13px;
  }
}
</style>
