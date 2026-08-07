<template>
  <div class="resource-cards">
    <!-- 行程规划 -->
    <div v-if="resources.intent === 'ITINERARY_PLANNING'" class="resource-section">
      <h4>行程规划</h4>
      <div class="itinerary-card">
        <div class="itinerary-text" style="white-space: pre-line;">{{ resources.itinerary }}</div>
      </div>
      <div v-if="hasActivities" style="margin-top: 8px;">
        <h4>相关活动</h4>
        <div class="card-list">
          <ResourceCard
            v-for="(activity, idx) in resources.activities"
            :key="idx"
            type="activity"
            :id="activity.id"
            :title="activity.title"
            :subtitle="activity.subtitle"
            :detail="activity.location + ' · ' + (activity.startTime || '') + ' · ' + (activity.price === 0 ? '免费' : activity.price + '元')"
            @navigate="(t, id) => $emit('navigate', t, id)"
          />
        </div>
      </div>
      <div v-if="hasHeritages" style="margin-top: 8px;">
        <h4>相关非遗项目</h4>
        <div class="card-list">
          <ResourceCard
            v-for="(heritage, idx) in resources.heritages"
            :key="idx"
            type="heritage"
            :id="heritage.id"
            :title="heritage.name"
            :detail="(heritage.category || '') + ' · ' + (heritage.region || '')"
            @navigate="(t, id) => $emit('navigate', t, id)"
          />
        </div>
      </div>
    </div>

    <!-- 结构化查询 / 资源推荐 -->
    <div v-if="resources.intent === 'STRUCTURED_QUERY' || resources.intent === 'RESOURCE_RECOMMEND'" class="resource-section">
      <div v-if="hasActivities">
        <h4>为您找到 {{ resources.total || resources.activities.length }} 个活动</h4>
        <div class="card-list">
          <ResourceCard
            v-for="(activity, idx) in resources.activities"
            :key="idx"
            type="activity"
            :id="activity.id"
            :title="activity.title"
            :subtitle="activity.subtitle"
            :detail="activity.location + ' · ' + (activity.startTime || '') + ' · ' + (activity.price === 0 ? '免费' : activity.price + '元')"
            @navigate="(t, id) => $emit('navigate', t, id)"
          />
        </div>
      </div>
      <div v-else>
        <p>暂无符合条件的活动，试试其他条件吧~</p>
      </div>
    </div>

    <!-- 知识问答 -->
    <div v-if="resources.intent === 'KNOWLEDGE_QA'" class="resource-section">
      <div v-if="hasPosts">
        <h4>相关帖子</h4>
        <div class="card-list">
          <ResourceCard
            v-for="(post, idx) in resources.posts"
            :key="idx"
            type="post"
            :id="post.id"
            :title="post.title"
            :detail="post.createTime || ''"
            @navigate="(t, id) => $emit('navigate', t, id)"
          />
        </div>
      </div>
      <div v-if="hasHeritages" style="margin-top: 8px;">
        <h4>相关非遗项目</h4>
        <div class="card-list">
          <ResourceCard
            v-for="(heritage, idx) in resources.heritages"
            :key="idx"
            type="heritage"
            :id="heritage.id"
            :title="heritage.name"
            :detail="(heritage.category || '') + ' · ' + (heritage.region || '')"
            @navigate="(t, id) => $emit('navigate', t, id)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import ResourceCard from './ResourceCard.vue'

export default {
  name: 'ResourceCards',
  components: { ResourceCard },
  props: {
    resources: { type: Object, default: () => ({}) }
  },
  emits: ['navigate'],
  computed: {
    hasActivities() {
      return this.resources.activities && this.resources.activities.length > 0
    },
    hasHeritages() {
      return this.resources.heritages && this.resources.heritages.length > 0
    },
    hasPosts() {
      return this.resources.posts && this.resources.posts.length > 0
    }
  }
}
</script>

<style scoped>
.resource-cards { width: 100%; }
.resource-section { margin-bottom: 20px; }
.resource-section h4 { font-size: 16px; color: #8B4513; margin-bottom: 10px; font-weight: 500; }
.card-list { display: flex; flex-direction: column; gap: 10px; }

.itinerary-card {
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
}

.itinerary-text {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}
</style>
