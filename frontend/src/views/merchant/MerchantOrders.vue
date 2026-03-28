<template>
  <div class="merchant-page">
    <div class="page-header">
      <div>
        <h2>订单管理</h2>
        <p class="subtitle">查看用户购买记录与收货信息</p>
      </div>
      <div class="stats">
        <span class="stat">订单数 {{ orders.length }}</span>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="orders.length === 0" class="empty">
      <div class="empty-icon">📦</div>
      <p>暂无订单</p>
    </div>

    <div v-else class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-card">
        <div class="card-header">
          <div>
            <div class="order-no">订单号：{{ order.orderNo }}</div>
            <div class="order-time">{{ formatDate(order.createTime) }}</div>
          </div>
          <span :class="['status-tag', order.status]">{{ getStatusText(order.status) }}</span>
        </div>

        <div class="card-body">
          <div class="info-row">
            <span class="label">商品</span>
            <span>{{ order.productName }}</span>
          </div>
          <div class="info-row">
            <span class="label">数量</span>
            <span>{{ order.quantity }}</span>
          </div>
          <div class="info-row">
            <span class="label">订单金额</span>
            <span>¥{{ formatPrice(order.totalAmount) }}</span>
          </div>
          <div class="info-row">
            <span class="label">实付金额</span>
            <span>¥{{ formatPrice(order.payAmount) }}</span>
          </div>
          <div class="info-row">
            <span class="label">收货人</span>
            <span>{{ order.receiverName || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="label">联系电话</span>
            <span>{{ order.receiverPhone || '-' }}</span>
          </div>
          <div class="info-row full">
            <span class="label">收货地址</span>
            <span>{{ fullAddress(order) }}</span>
          </div>
          <div v-if="order.remark" class="info-row full">
            <span class="label">买家备注</span>
            <span>{{ order.remark }}</span>
          </div>
          <div v-if="order.logisticsNo" class="info-row full">
            <span class="label">物流信息</span>
            <span>{{ order.logisticsCompany || '物流单号' }} {{ order.logisticsNo }}</span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMerchantOrders } from '../../api/app'

const loading = ref(false)
const orders = ref([])

const loadOrders = async () => {
  loading.value = true
  try {
    const response = await getMerchantOrders()
    orders.value = response.data || []
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatPrice = (amount) => ((amount || 0) / 100).toFixed(2)

const fullAddress = (order) => {
  const parts = [order.receiverProvince, order.receiverCity, order.receiverDistrict, order.receiverAddress]
    .filter(Boolean)
  return parts.length ? parts.join(' ') : '-'
}

const getStatusText = (status) => {
  const statusMap = {
    pending_payment: '待支付',
    paid: '已支付',
    pending_shipment: '待发货',
    shipped: '已发货',
    pending_receipt: '待收货',
    completed: '已完成',
    cancelled: '已取消',
    refunded: '已退款'
  }
  return statusMap[status] || status
}

onMounted(loadOrders)
</script>

<style scoped>
.merchant-page { max-width: 900px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; gap: 12px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a2e; margin: 0 0 4px; }
.subtitle, .stat { font-size: 13px; color: #6b7280; }
.loading, .empty { text-align: center; padding: 60px; color: #9ca3af; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.order-list { display: flex; flex-direction: column; gap: 14px; }
.order-card { background: #fff; border-radius: 14px; border: 1px solid #e5e7eb; overflow: hidden; }
.card-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; padding: 18px 20px; border-bottom: 1px solid #f3f4f6; background: #fafafa; }
.order-no { font-size: 15px; font-weight: 600; color: #111827; }
.order-time { margin-top: 4px; font-size: 12px; color: #9ca3af; }
.status-tag { padding: 4px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.status-tag.pending_payment { background: #fff7ed; color: #d97706; }
.status-tag.paid, .status-tag.completed { background: #d1fae5; color: #059669; }
.status-tag.cancelled, .status-tag.refunded { background: #fee2e2; color: #dc2626; }
.status-tag.pending_shipment, .status-tag.pending_receipt, .status-tag.shipped { background: #dbeafe; color: #2563eb; }
.card-body { padding: 18px 20px; display: grid; grid-template-columns: 1fr 1fr; gap: 12px 20px; }
.info-row { display: flex; flex-direction: column; gap: 4px; font-size: 14px; color: #374151; }
.info-row.full { grid-column: 1 / -1; }
.label { font-size: 12px; color: #9ca3af; }
</style>
