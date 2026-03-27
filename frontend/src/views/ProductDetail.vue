<template>
  <div class="product-detail">
    <div class="detail-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <div>
        <h1>{{ product?.name || '商品详情' }}</h1>
        <p class="subtitle">{{ product?.heritageType || '非遗文创' }}</p>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!product" class="empty">商品不存在或已下架</div>
    <div v-else class="detail-content">
      <section class="gallery-panel">
        <img :src="currentImage" alt="商品主图" class="main-image" />
        <div class="thumb-list" v-if="gallery.length > 1">
          <button
            v-for="img in gallery"
            :key="img"
            :class="['thumb', { active: currentImage === img }]"
            @click="currentImage = img"
          >
            <img :src="img" alt="商品缩略图" />
          </button>
        </div>
      </section>

      <section class="info-panel">
        <div class="price-row">
          <span class="price">¥{{ formatPrice(product.price) }}</span>
          <span v-if="product.originalPrice" class="original-price">¥{{ formatPrice(product.originalPrice) }}</span>
        </div>

        <div class="meta-grid">
          <div class="meta-item">
            <span class="label">库存</span>
            <strong>{{ product.stock ?? 0 }}</strong>
          </div>
          <div class="meta-item">
            <span class="label">销量</span>
            <strong>{{ product.sales ?? 0 }}</strong>
          </div>
          <div class="meta-item">
            <span class="label">店铺</span>
            <strong>{{ product.merchantName || '合作商家' }}</strong>
          </div>
          <div class="meta-item">
            <span class="label">品类</span>
            <strong>{{ product.heritageType || '非遗作品' }}</strong>
          </div>
        </div>

        <div class="section-card">
          <h3>商品简介</h3>
          <p>{{ product.description || '暂无简介' }}</p>
        </div>

        <div class="section-card">
          <h3>商家信息</h3>
          <p>{{ product.merchantIntro || '该商家专注于非遗文化体验与作品创作。' }}</p>
        </div>

        <div class="purchase-card">
          <div class="quantity-control">
            <button @click="decreaseQuantity" :disabled="quantity <= 1">-</button>
            <span>{{ quantity }}</span>
            <button @click="increaseQuantity" :disabled="quantity >= (product.stock || 0)">+</button>
          </div>
          <button class="buy-btn" @click="showOrderModal = true" :disabled="!product.stock">立即下单</button>
        </div>
      </section>
    </div>

    <section v-if="product" class="description-panel">
      <h3>商品详情</h3>
      <div v-if="product.detail" class="detail-text">{{ product.detail }}</div>
      <p v-else>{{ product.description || '暂无详情说明' }}</p>
    </section>

    <div v-if="showOrderModal && product" class="modal-mask" @click.self="showOrderModal = false">
      <div class="modal">
        <h3>填写收货信息</h3>
        <div class="form-grid">
          <div class="form-group">
            <label>收货人 *</label>
            <input v-model="orderForm.receiverName" placeholder="请输入收货人姓名" />
          </div>
          <div class="form-group">
            <label>联系电话 *</label>
            <input v-model="orderForm.receiverPhone" placeholder="请输入联系电话" />
          </div>
          <div class="form-group">
            <label>省份</label>
            <input v-model="orderForm.receiverProvince" placeholder="如：浙江省" />
          </div>
          <div class="form-group">
            <label>城市 *</label>
            <input v-model="orderForm.receiverCity" placeholder="如：杭州市" />
          </div>
          <div class="form-group">
            <label>区县</label>
            <input v-model="orderForm.receiverDistrict" placeholder="如：西湖区" />
          </div>
          <div class="form-group">
            <label>购买数量</label>
            <input :value="quantity" disabled />
          </div>
          <div class="form-group full">
            <label>详细地址 *</label>
            <input v-model="orderForm.receiverAddress" placeholder="请输入详细地址" />
          </div>
          <div class="form-group full">
            <label>买家备注</label>
            <textarea v-model="orderForm.remark" rows="3" placeholder="可填写送礼说明或配送备注"></textarea>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn cancel" @click="showOrderModal = false">取消</button>
          <button class="btn submit" @click="submitOrder">提交订单</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createOrder, getPublicProductDetail } from '../api/app'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const router = useRouter()
const route = useRoute()
const product = ref(null)
const loading = ref(false)
const quantity = ref(1)
const currentImage = ref('')
const showOrderModal = ref(false)
const orderForm = ref({
  receiverName: '',
  receiverPhone: '',
  receiverProvince: '',
  receiverCity: '',
  receiverDistrict: '',
  receiverAddress: '',
  remark: ''
})

const gallery = computed(() => {
  if (!product.value) {
    return []
  }

  const images = []
  if (product.value.mainImage) {
    images.push(product.value.mainImage)
  }

  if (product.value.images) {
    try {
      const parsed = JSON.parse(product.value.images)
      if (Array.isArray(parsed)) {
        images.push(...parsed.filter(Boolean))
      }
    } catch (error) {
      console.error('解析商品图片失败', error)
    }
  }

  return [...new Set(images)].filter(Boolean)
})

const goBack = () => router.back()

const decreaseQuantity = () => {
  if (quantity.value > 1) {
    quantity.value -= 1
  }
}

const increaseQuantity = () => {
  const stock = product.value?.stock || 0
  if (!stock || quantity.value < stock) {
    quantity.value += 1
  }
}

const formatPrice = (amount) => ((amount || 0) / 100).toFixed(2)

const loadProduct = async () => {
  loading.value = true
  try {
    const response = await getPublicProductDetail(route.params.id)
    product.value = response.data || null
    currentImage.value = gallery.value[0] || ''
  } catch (error) {
    notify(error.response?.data?.message || '获取商品详情失败', 'error')
    product.value = null
  } finally {
    loading.value = false
  }
}

const submitOrder = async () => {
  if (!product.value) {
    return
  }

  if (!orderForm.value.receiverName || !orderForm.value.receiverPhone || !orderForm.value.receiverCity || !orderForm.value.receiverAddress) {
    notify('请填写完整的收货信息', 'error')
    return
  }

  try {
    const response = await createOrder({
      productId: product.value.id,
      quantity: quantity.value,
      ...orderForm.value
    })

    notify(`下单成功，订单号：${response.data.orderNo}`, 'success')
    showOrderModal.value = false
    product.value.stock = Math.max((product.value.stock || 0) - quantity.value, 0)
  } catch (error) {
    notify(error.response?.data?.message || '下单失败', 'error')
  }
}

onMounted(loadProduct)
</script>

<style scoped>
.product-detail { min-height: 100vh; background: #f5f6fa; padding-bottom: 32px; }
.detail-header { padding: 24px 20px; background: #fff; display: flex; gap: 16px; align-items: flex-start; border-bottom: 1px solid #e5e7eb; }
.back-btn { padding: 8px 12px; background: #fff; border: 1px solid #d1d5db; border-radius: 8px; cursor: pointer; }
.detail-header h1 { margin: 0 0 6px; font-size: 22px; color: #111827; }
.subtitle { margin: 0; color: #6b7280; }
.loading, .empty { text-align: center; padding: 80px 20px; color: #9ca3af; }
.detail-content { display: grid; grid-template-columns: 1.1fr 1fr; gap: 20px; padding: 20px; max-width: 1200px; margin: 0 auto; }
.gallery-panel, .info-panel, .description-panel { background: #fff; border-radius: 16px; padding: 20px; }
.main-image { width: 100%; height: 420px; object-fit: cover; border-radius: 12px; background: #f3f4f6; }
.thumb-list { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 14px; }
.thumb { width: 72px; height: 72px; padding: 0; border: 2px solid transparent; border-radius: 10px; background: transparent; overflow: hidden; cursor: pointer; }
.thumb.active { border-color: #7c3aed; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.price-row { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.price { font-size: 30px; font-weight: 700; color: #dc2626; }
.original-price { color: #9ca3af; text-decoration: line-through; }
.meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 18px; }
.meta-item { background: #f9fafb; border-radius: 12px; padding: 12px 14px; display: flex; flex-direction: column; gap: 4px; }
.label { font-size: 12px; color: #9ca3af; }
.section-card { border: 1px solid #eef2f7; border-radius: 12px; padding: 16px; margin-bottom: 14px; }
.section-card h3, .description-panel h3 { margin: 0 0 10px; font-size: 16px; color: #111827; }
.section-card p, .description-panel p, .detail-text { margin: 0; color: #4b5563; line-height: 1.7; white-space: pre-line; }
.purchase-card { display: flex; gap: 12px; align-items: center; margin-top: 18px; }
.quantity-control { display: flex; align-items: center; gap: 12px; border: 1px solid #d1d5db; border-radius: 10px; padding: 6px; }
.quantity-control button { width: 34px; height: 34px; border: none; border-radius: 8px; background: #f3f4f6; cursor: pointer; }
.buy-btn { flex: 1; padding: 12px 18px; border: none; border-radius: 10px; background: #7c3aed; color: #fff; cursor: pointer; font-size: 15px; font-weight: 600; }
.buy-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.description-panel { max-width: 1200px; margin: 0 auto; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { width: 640px; max-width: 95vw; max-height: 90vh; overflow-y: auto; background: #fff; border-radius: 16px; padding: 28px; }
.modal h3 { margin: 0 0 20px; font-size: 18px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group.full { grid-column: 1 / -1; }
.form-group label { font-size: 13px; color: #374151; font-weight: 500; }
.form-group input, .form-group textarea { padding: 10px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.btn { padding: 9px 20px; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; }
.btn.cancel { background: #f3f4f6; color: #374151; }
.btn.submit { background: #7c3aed; color: #fff; }
@media (max-width: 900px) {
  .detail-content { grid-template-columns: 1fr; }
  .main-image { height: 300px; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
