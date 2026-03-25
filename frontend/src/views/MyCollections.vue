<template>
  <div class="my-collections-page">
    <div class="settings-header">
      <button class="back-btn" @click="goBack">
        <i class='bx bxs-chevron-left'></i>
      </button>
      <h1>{{ $t('myCollections.title') }}</h1>
    </div>

    <div class="collections-content">
      <div class="empty-state" v-if="collections.length === 0">
        <i class='bx bxs-star'></i>
        <h3>{{ $t('myCollections.emptyTitle') }}</h3>
        <p>{{ $t('myCollections.emptyHint') }}</p>
        <button class="btn primary" @click="goToTranslate">{{ $t('myCollections.goTranslate') }}</button>
      </div>

      <div class="collections-grid" v-else>
        <div 
          class="collection-card" 
          v-for="item in collections" 
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
            <h4 class="card-title">{{ item.title || $t('myCollections.untitled') }}</h4>
            <div class="card-meta">
              <span class="card-time">{{ item.collectedAt }}</span>
              <button class="uncollect-btn" @click.stop="uncollect(item.id)">
                <i class='bx bxs-star'></i>
              </button>
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
import { getMyDiscoverCollections, removeMyDiscoverCollection } from '../api/app'

// 获取通知实例
const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify
const confirm = appContext.config.globalProperties.$confirm

const router = useRouter()
const { t } = useI18n()
const collections = ref([])

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

const loadCollections = async () => {
  try {
    const response = await getMyDiscoverCollections()
    if (response.code === 200) {
      collections.value = Array.isArray(response.data) ? response.data : []
    } else {
      notify.error(response.message || t('myCollections.loadFailed'))
    }
  } catch (error) {
    notify.error(t('myCollections.loadFailedRetry'))
  }
}

const uncollect = async (id) => {
  confirm({
    title: t('myCollections.confirmUncollectTitle'),
    message: t('myCollections.confirmUncollect'),
    confirmText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onConfirm: async () => {
      try {
        const response = await removeMyDiscoverCollection(id)
        if (response.code === 200) {
          collections.value = collections.value.filter(item => item.id !== id)
          notify.success(t('myCollections.uncollectSuccess'))
        } else {
          notify.error(response.message || t('myCollections.uncollectFailed'))
        }
      } catch (error) {
        notify.error(t('myCollections.uncollectFailedRetry'))
      }
    }
  })
}

onMounted(() => {
  loadCollections()
})
</script>

<style scoped>
.my-collections-page {
  min-height: 100vh;
  width: 100%;
  margin: 0 auto;
}

@media (min-width: 768px) {
  .my-collections-page {
    width: 85%;
    margin: 0 auto;
  }
}

@media (min-width: 1024px) {
  .my-collections-page {
    width: 70%;
    margin: 0 auto;
  }
}

.settings-header {
  background: white;
  padding: 15px 20px;
  display: flex;
  align-items: center;
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
}

.collections-content {
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

.collections-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

@media (min-width: 768px) {
  .collections-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1024px) {
  .collections-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.collection-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.collection-card:hover {
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

.uncollect-btn {
  background: none;
  border: none;
  color: #ffc107;
  cursor: pointer;
  padding: 4px;
  transition: transform 0.2s;
}

.uncollect-btn:hover {
  transform: scale(1.2);
}

.uncollect-btn i {
  font-size: 16px;
}

@media (max-width: 768px) {
  .collections-content {
    padding: 10px;
  }
  
  .collections-grid {
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
