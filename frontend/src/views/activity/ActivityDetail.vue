<template>
  <div class="activity-detail-route">
    <ActivityDetailModal
      v-if="isVisible"
      :visible="isVisible"
      :activity-id="route.params.id"
      @close="handleClose"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import ActivityDetailModal from '@/components/ActivityDetailModal.vue'

const route = useRoute()
const router = useRouter()
const isVisible = ref(true)

onBeforeRouteLeave(() => {
  isVisible.value = false
})

function handleClose() {
  if (typeof route.query.backTo === 'string' && route.query.backTo) {
    router.push(route.query.backTo)
    return
  }

  if (window.history.length > 1) {
    router.back()
    return
  }

  router.push('/home/activity')
}
</script>

<style scoped>
.activity-detail-route {
  min-height: 100vh;
}
</style>
