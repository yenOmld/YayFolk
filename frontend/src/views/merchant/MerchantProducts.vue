<template>
  <div class="merchant-page">
    <div class="page-header">
      <h2>商品管理</h2>
      <button class="create-btn" @click="openForm()">+ 添加商品</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="products.length === 0" class="empty">
      <div class="empty-icon">🛍️</div>
      <p>还没有商品，先发布一件非遗作品吧</p>
    </div>

    <div v-else class="product-grid">
      <article v-for="product in products" :key="product.id" class="product-card">
        <div class="product-image">
          <img v-if="product.mainImage" :src="product.mainImage" alt="商品主图" />
          <div v-else class="image-placeholder">🛍️</div>
        </div>
        <div class="product-info">
          <div class="product-head">
            <div>
              <h3>{{ product.name }}</h3>
              <p class="subtitle">{{ product.subtitle || product.heritageType || '非遗文创商品' }}</p>
            </div>
            <span :class="['status-tag', product.status]">{{ product.status === 'on_sale' ? '在售' : '下架' }}</span>
          </div>
          <p class="description">{{ product.description || '暂无商品简介' }}</p>
          <div class="meta-row">
            <span>价格：¥{{ formatPrice(product.price) }}</span>
            <span>库存：{{ product.stock ?? 0 }}</span>
            <span>销量：{{ product.sales ?? 0 }}</span>
          </div>
          <div class="action-row">
            <button class="action-btn edit" @click="openForm(product)">编辑</button>
            <button class="action-btn delete" @click="handleDelete(product.id)">删除</button>
          </div>
        </div>
      </article>
    </div>

    <div v-if="showModal" class="modal-mask" @click.self="closeForm">
      <div class="modal">
        <h3>{{ editingId ? '编辑商品' : '添加商品' }}</h3>
        <div class="form-grid">
          <div class="form-group full">
            <label>商品名称 *</label>
            <input v-model="form.name" placeholder="请输入商品名称" />
          </div>
          <div class="form-group full">
            <label>副标题</label>
            <input v-model="form.subtitle" placeholder="一句话说明商品卖点" />
          </div>
          <div class="form-group">
            <label>非遗品类</label>
            <input v-model="form.heritageType" placeholder="如：剪纸、陶瓷" />
          </div>
          <div class="form-group">
            <label>状态</label>
            <select v-model="form.status">
              <option value="on_sale">在售</option>
              <option value="off_sale">下架</option>
            </select>
          </div>
          <div class="form-group">
            <label>售价（元） *</label>
            <input v-model.number="form.priceYuan" type="number" min="0" placeholder="0" />
          </div>
          <div class="form-group">
            <label>划线价（元）</label>
            <input v-model.number="form.originalPriceYuan" type="number" min="0" placeholder="可选" />
          </div>
          <div class="form-group">
            <label>库存 *</label>
            <input v-model.number="form.stock" type="number" min="0" placeholder="0" />
          </div>
          <div class="form-group full">
            <label>主图 URL</label>
            <input v-model="form.mainImage" placeholder="可选，填入商品图片 URL" />
          </div>
          <div class="form-group full">
            <label>商品简介</label>
            <textarea v-model="form.description" rows="3" placeholder="用于列表页展示"></textarea>
          </div>
          <div class="form-group full">
            <label>商品详情</label>
            <textarea v-model="form.detail" rows="5" placeholder="可填写制作工艺、材质、故事背景等"></textarea>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn cancel" @click="closeForm">取消</button>
          <button class="btn submit" @click="submitForm">{{ editingId ? '保存' : '发布' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import {
  getMerchantProducts,
  createMerchantProduct,
  updateMerchantProduct,
  deleteMerchantProduct
} from '../../api/app'

const { appContext } = getCurrentInstance()
const notify = (msg, type = 'info') => appContext.config.globalProperties.$notify?.[type]?.(msg)

const loading = ref(false)
const products = ref([])
const showModal = ref(false)
const editingId = ref(null)

const defaultForm = () => ({
  name: '',
  subtitle: '',
  description: '',
  detail: '',
  priceYuan: 0,
  originalPriceYuan: null,
  stock: 0,
  mainImage: '',
  heritageType: '',
  status: 'on_sale'
})

const form = ref(defaultForm())

const loadProducts = async () => {
  loading.value = true
  try {
    const response = await getMerchantProducts()
    products.value = response.data || []
  } catch (error) {
    notify('获取商品列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const openForm = (product = null) => {
  if (product) {
    editingId.value = product.id
    form.value = {
      name: product.name || '',
      subtitle: product.subtitle || '',
      description: product.description || '',
      detail: product.detail || '',
      priceYuan: product.price ? product.price / 100 : 0,
      originalPriceYuan: product.originalPrice ? product.originalPrice / 100 : null,
      stock: product.stock ?? 0,
      mainImage: product.mainImage || '',
      heritageType: product.heritageType || '',
      status: product.status || 'on_sale'
    }
  } else {
    editingId.value = null
    form.value = defaultForm()
  }
  showModal.value = true
}

const closeForm = () => {
  showModal.value = false
  editingId.value = null
  form.value = defaultForm()
}

const submitForm = async () => {
  if (!form.value.name || form.value.priceYuan == null || form.value.stock == null) {
    notify('请填写商品名称、价格和库存', 'error')
    return
  }

  const payload = {
    name: form.value.name,
    subtitle: form.value.subtitle,
    description: form.value.description,
    detail: form.value.detail,
    price: Math.round((form.value.priceYuan || 0) * 100),
    originalPrice: form.value.originalPriceYuan == null ? null : Math.round(form.value.originalPriceYuan * 100),
    stock: Number(form.value.stock) || 0,
    mainImage: form.value.mainImage,
    heritageType: form.value.heritageType,
    status: form.value.status
  }

  try {
    if (editingId.value) {
      await updateMerchantProduct(editingId.value, payload)
      notify('商品更新成功', 'success')
    } else {
      await createMerchantProduct(payload)
      notify('商品发布成功', 'success')
    }
    closeForm()
    await loadProducts()
  } catch (error) {
    notify(error.response?.data?.message || '保存商品失败', 'error')
  }
}

const handleDelete = async (productId) => {
  if (!confirm('确定要删除这个商品吗？')) {
    return
  }

  try {
    await deleteMerchantProduct(productId)
    notify('商品已删除', 'success')
    products.value = products.value.filter(product => product.id !== productId)
  } catch (error) {
    notify(error.response?.data?.message || '删除商品失败', 'error')
  }
}

const formatPrice = (price) => ((price || 0) / 100).toFixed(2)

onMounted(loadProducts)
</script>

<style scoped>
.merchant-page { max-width: 980px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a2e; margin: 0; }
.create-btn { padding: 9px 20px; background: #7c3aed; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 500; }
.create-btn:hover { background: #6d28d9; }
.loading, .empty { text-align: center; padding: 60px; color: #9ca3af; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.product-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 14px; overflow: hidden; }
.product-image { height: 200px; background: #f3f4f6; display: flex; align-items: center; justify-content: center; }
.product-image img { width: 100%; height: 100%; object-fit: cover; }
.image-placeholder { font-size: 48px; }
.product-info { padding: 18px; }
.product-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.product-head h3 { margin: 0 0 4px; font-size: 17px; color: #111827; }
.subtitle { margin: 0; font-size: 12px; color: #9ca3af; }
.status-tag { padding: 4px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.status-tag.on_sale { background: #d1fae5; color: #059669; }
.status-tag.off_sale { background: #f3f4f6; color: #6b7280; }
.description { color: #6b7280; font-size: 14px; line-height: 1.6; margin: 0 0 14px; min-height: 44px; }
.meta-row { display: flex; flex-wrap: wrap; gap: 10px 16px; font-size: 13px; color: #4b5563; margin-bottom: 16px; }
.action-row { display: flex; gap: 10px; }
.action-btn { flex: 1; padding: 8px 0; border-radius: 8px; border: none; cursor: pointer; font-size: 13px; font-weight: 500; }
.action-btn.edit { background: #ede9fe; color: #7c3aed; }
.action-btn.delete { background: #fee2e2; color: #dc2626; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: #fff; border-radius: 16px; padding: 28px; width: 640px; max-width: 95vw; max-height: 90vh; overflow-y: auto; }
.modal h3 { margin: 0 0 20px; font-size: 18px; font-weight: 600; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group.full { grid-column: 1 / -1; }
.form-group label { font-size: 13px; color: #374151; font-weight: 500; }
.form-group input, .form-group select, .form-group textarea { padding: 10px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; }
.form-group textarea { resize: vertical; font-family: inherit; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.btn { padding: 9px 20px; border-radius: 8px; border: none; cursor: pointer; font-size: 14px; font-weight: 500; }
.btn.cancel { background: #f3f4f6; color: #374151; }
.btn.submit { background: #7c3aed; color: #fff; }
</style>
