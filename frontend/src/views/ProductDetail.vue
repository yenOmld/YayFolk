<template>
  <div class="product-detail">
    <div class="detail-header">
      <button class="back-btn" @click="goBack">闁?閺夆晜鏌ㄥú?/button>
      <div>
        <h1>{{ product?.name || '闁哥喎妫楅幖褏鎷犻敂钘夊壈' }}</h1>
        <p class="subtitle">{{ product?.heritageType || '闂傚牏鍋ゆ禒鎰板棘閸パ冪仭' }}</p>
      </div>
    </div>

    <div v-if="loading" class="loading">闁告梻濮惧ù鍥ㄧ▔?..</div>
    <div v-else-if="!product" class="empty">闁哥喎妫楅幖褎绋夊鍛憼闁革负鍔嶉崹銊ヮ啅闊厾鐟撻柡?/div>
    <div v-else class="detail-content">
      <section class="gallery-panel">
        <img :src="currentImage" alt="闁哥喎妫楅幖褎绋夌拠鍙夌" class="main-image" />
        <div class="thumb-list" v-if="gallery.length > 1">
          <button
            v-for="img in gallery"
            :key="img"
            :class="['thumb', { active: currentImage === img }]"
            @click="currentImage = img"
          >
            <img :src="img" alt="闁哥喎妫楅幖褏绱撻埡鍐╂闁? />
          </button>
        </div>
      </section>

      <section class="info-panel">
        <div class="price-row">
          <span class="price">濡ょ磵{ formatPrice(product.price) }}</span>
          <span v-if="product.originalPrice" class="original-price">濡ょ磵{ formatPrice(product.originalPrice) }}</span>
        </div>

        <div class="meta-grid">
          <div class="meta-item">
            <span class="label">閹煎瓨鎸搁悺?/span>
            <strong>{{ product.stock ?? 0 }}</strong>
          </div>
          <div class="meta-item">
            <span class="label">闂佸簱鍋撻梺?/span>
            <strong>{{ product.sales ?? 0 }}</strong>
          </div>
          <div class="meta-item">
            <span class="label">閹煎瓨顨婇幗?/span>
            <strong>{{ product.merchantName || '闁告艾鐗呯紞鏃堝疮閸℃鍟€' }}</strong>
          </div>
          <div class="meta-item">
            <span class="label">闁告繀鑳剁悮?/span>
            <strong>{{ product.heritageType || '闂傚牏鍋ゆ禒鎰媴濠婂啯鎯? }}</strong>
          </div>
        </div>

        <div class="section-card">
          <h3>闁哥喎妫楅幖褏绮婚埀顒佺?/h3>
          <p>{{ product.description || '闁哄棗鍊瑰Λ銈囩不閳ь剚绂? }}</p>
        </div>

        <div class="section-card">
          <h3>闁哥喎妫楅宥嗙┍閳╁啩绱?/h3>
          <p>{{ product.merchantIntro || '閻犲洢鍎遍弲銏⑩偓瑙勬构缁楁挸鈻旈妸銈囪壘闂傚牏鍋ゆ禒鎰板棘閸パ冾嚙濞达絾鎹囬悰娆愮▔鎼存繄绋婇柛婵呯閸ㄨ鲸鎷呭┃搴撳亾? }}</p>
        </div>

        <div class="purchase-card">
          <div class="quantity-control">
            <button @click="decreaseQuantity" :disabled="quantity <= 1">-</button>
            <span>{{ quantity }}</span>
            <button @click="increaseQuantity" :disabled="quantity >= (product.stock || 0)">+</button>
          </div>
          <button class="buy-btn" @click="showOrderModal = true" :disabled="!product.stock">缂佹柨顑呭畵鍡樼▔鐎ｎ亜绀?/button>
        </div>
      </section>
    </div>

    <section v-if="product" class="description-panel">
      <h3>闁哥喎妫楅幖褏鎷犻敂钘夊壈</h3>
      <div v-if="product.detail" class="detail-text">{{ product.detail }}</div>
      <p v-else>{{ product.description || '闁哄棗鍊瑰Λ銈囨嫚閿旇棄鍓伴悹鍥х摠濡? }}</p>
    </section>

    <div v-if="showOrderModal && product" class="modal-mask" @click.self="showOrderModal = false">
      <div class="modal">
        <h3>濠靛鍋勯崯鎾诲绩閹増褰涘ǎ鍥ｅ墲娴?/h3>
        <div class="form-grid">
          <div class="form-group">
            <label>闁衡偓閹増褰涘ù?*</label>
            <input v-model="orderForm.receiverName" placeholder="閻犲洨鏌夌欢顓㈠礂閵夛附鏆悹鎰屽倹鐪藉┑顔芥尭閹? />
          </div>
          <div class="form-group">
            <label>闁艰鲸姊婚柈鎾偨娴ｅ啰妯?*</label>
            <input v-model="orderForm.receiverPhone" placeholder="閻犲洨鏌夌欢顓㈠礂閵夈劋绮撶紒顖濆吹閺佸摜鎷? />
          </div>
          <div class="form-group">
            <label>闁活亙妞掗崬?/label>
            <input v-model="orderForm.receiverProvince" placeholder="濠碘€冲亰缁辨澘霉濞嗘劗娼ら柣? />
          </div>
          <div class="form-group">
            <label>闁糕晛楠哥粩?*</label>
            <input v-model="orderForm.receiverCity" placeholder="濠碘€冲亰缁变即寮堕鐐电嵁閻? />
          </div>
          <div class="form-group">
            <label>闁告牕鎼獮?/label>
            <input v-model="orderForm.receiverDistrict" placeholder="濠碘€冲亰缁辨壆鎲查幐搴ｎ啇闁? />
          </div>
          <div class="form-group">
            <label>閻犳劦鍘洪幏閬嶅极娴兼潙娅?/label>
            <input :value="quantity" disabled />
          </div>
          <div class="form-group full">
            <label>閻犲浄濡囩划蹇涘捶閺夋寧绲?*</label>
            <input v-model="orderForm.receiverAddress" placeholder="閻犲洨鏌夌欢顓㈠礂閵夘煈鍤婄紓浣告濠€鎾锤閳? />
          </div>
          <div class="form-group full">
            <label>濞戞梹婢橀宥嗗緞閸ャ劍鏆?/label>
            <textarea v-model="orderForm.remark" rows="3" placeholder="闁告瑯鍨伴敐鐐哄礃濞嗘挴鍋撴担鎭掍粻閻犲洤鐡ㄥΣ鎴﹀箣閺嶎厼甯抽梺顐＄椤︻剙鈻?></textarea>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn cancel" @click="showOrderModal = false">闁告瑦鐗楃粔?/button>
          <button class="btn submit" @click="submitOrder">闁圭粯鍔掑锔炬媼閵忕姴绀?/button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createOrder, getPublicProductDetail, payOrder } from '../api/app'
import { isAlipayPagePayment, submitAlipayForm } from '../utils/alipay'

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
      console.error('閻熸瑱绲鹃悗浠嬪疮閸℃鎯傞柛銉ュ⒔婢ф牗寰勬潏顐バ?, error)
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
    notify(error.response?.data?.message || '闁兼儳鍢茶ぐ鍥疮閸℃鎯傞悹鍥烽檮閸庡繑寰勬潏顐バ?, 'error')
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
    notify('閻犲洤鍢查敐鐐哄礃濞嗗繒鏆氶柡浣割嚟濞堟垿寮ㄩ幆鐗堝經濞ｅ洠鍓濇导?, 'error')
    return
  }

  try {
    const response = await createOrder({
      productId: product.value.id,
      quantity: quantity.value,
      ...orderForm.value
    })

    if (response?.data?.canPay) {
      const paymentResponse = await payOrder(response.data.id, { paymentType: 'alipay' })
      if (paymentResponse.code !== 200 || !paymentResponse.data) {
        throw new Error(paymentResponse.message || 'Failed to create Alipay payment')
      }
      if (isAlipayPagePayment(paymentResponse.data)) {
        submitAlipayForm(paymentResponse.data.formHtml)
        return
      }
    }

    notify(`Order created: ${response.data.orderNo}`, 'success')
    showOrderModal.value = false
    product.value.stock = Math.max((product.value.stock || 0) - quantity.value, 0)
  } catch (error) {
    notify(error.response?.data?.message || '濞戞挸顑呭畷鐔稿緞鏉堫偉袝', 'error')
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
