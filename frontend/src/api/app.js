import request from '../utils/request'

export const login = (data) => {
  return request.post('/login', data)
}

export const loginByCode = (params) => {
  return request.post('/login/code', params)
}

export const register = (data) => {
  return request.post('/register', data)
}

export const sendCode = (params) => {
  return request.post('/send-code', params)
}

export const resetPassword = (data) => {
  return request.post('/reset-password', data)
}

export const recognizeLandmark = (formData) => {
  return request.post('/landmark/recognize', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 30000
  })
}

export const recognizeLandmarkBase64 = (data) => {
  const body = typeof data === 'string' ? { image: data } : data
  return request.post('/landmark/recognize-base64', body, {
    timeout: 30000
  })
}

export const getDiscoverPosts = (params) => {
  return request.get('/discover/posts', { params })
}

export const getDiscoverPostDetail = (postId) => {
  return request.get(`/discover/posts/${postId}`)
}

export const getSharePostDetail = (postId) => {
  return request.get(`/discover/share/${postId}`)
}

export const translateDiscoverPost = (postId, targetLang) => {
  return request.get('/post/translate', {
    params: { postId, targetLang }
  })
}

export const translateDiscoverComment = (commentId, targetLang) => {
  return request.get('/comment/translate', {
    params: { commentId, targetLang }
  })
}

export const createDiscoverPost = (data) => {
  return request.post('/discover/posts', data)
}

export const toggleDiscoverPostCollect = (postId) => {
  return request.post(`/discover/posts/${postId}/collect`)
}

export const createDiscoverPostComment = (postId, data) => {
  return request.post(`/discover/posts/${postId}/comments`, data)
}

export const toggleDiscoverCommentLike = (commentId) => {
  return request.post(`/discover/comments/${commentId}/like`)
}

export const deleteDiscoverComment = (commentId) => {
  return request.delete(`/discover/comments/${commentId}`)
}

export const getMyDiscoverPosts = () => {
  return request.get('/discover/my/posts')
}

export const updateDiscoverPost = (postId, data) => {
  return request.put(`/discover/posts/${postId}`, data)
}

export const deleteMyDiscoverPost = (postId) => {
  return request.delete(`/discover/my/posts/${postId}`)
}

export const getMyDiscoverCollections = () => {
  return request.get('/discover/my/collections')
}

export const removeMyDiscoverCollection = (postId) => {
  return request.delete(`/discover/my/collections/${postId}`)
}

export const getMyDiscoverHistory = () => {
  return request.get('/discover/my/history')
}

export const clearMyDiscoverHistory = () => {
  return request.delete('/discover/my/history')
}

export const getMyDiscoverStats = () => {
  return request.get('/discover/my/stats')
}

export const getConversations = () => {
  return request.get('/messages/conversations')
}

export const createConversation = (data) => {
  return request.post('/messages/conversations', data)
}

export const getMessages = (conversationId) => {
  return request.get(`/messages/conversations/${conversationId}`)
}

export const sendMessage = (conversationId, data) => {
  return request.post(`/messages/conversations/${conversationId}`, data)
}

export const markAsRead = (conversationId) => {
  return request.post(`/messages/conversations/${conversationId}/read`)
}

export const getNotifications = (type) => {
  return request.get(`/messages/notifications/${type}`)
}

export const markNotificationsAsRead = (type) => {
  return request.post(`/messages/notifications/${type}/read`)
}

export const getUnreadCount = () => {
  return request.get('/messages/unread-count')
}

export const deleteConversation = (conversationId) => {
  return request.delete(`/messages/conversations/${conversationId}`)
}

export const clearNotifications = (type) => {
  return request.delete(`/messages/notifications/${type}`)
}

export const deleteMessage = (messageId) => {
  return request.delete(`/messages/messages/${messageId}`)
}

export const recallMessage = (messageId) => {
  return request.post(`/messages/messages/${messageId}/recall`)
}

export const translateMessage = (messageId, targetLang) => {
  return request.get('/messages/message/translate', {
    params: { messageId, targetLang }
  })
}

export const getPhrases = () => {
  return request.get('/phrases')
}

export const addPhrase = (data) => {
  return request.post('/phrases', data)
}

export const deletePhrase = (phraseId) => {
  return request.delete(`/phrases/${phraseId}`)
}

export const uploadPostImage = (formData, postId, index) => {
  let url = '/upload/post-image'
  if (postId && index) {
    url += `?postId=${postId}&index=${index}`
  }
  return request.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const generateHeritageRoute = (data) => {
  return request.post('/ai/heritage-route', data, {
    timeout: 60000
  })
}

// ========== 公开内容（用户端无需登录） ==========
export const getPublicActivities = (params) => request.get('/public/activities', { params })
export const getPublicActivityDetail = (id) => request.get(`/public/activities/${id}`)
export const getPublicProducts = (params) => request.get('/public/products', { params })
export const getPublicProductDetail = (id) => request.get(`/public/products/${id}`)
export const getOfficialContents = (category) => request.get('/public/official', { params: { category } })

// ========== 商家端 ==========
export const applyMerchant = (data) => request.post('/merchant/apply', data)
export const getMyApplication = () => request.get('/merchant/apply/status')

export const getMerchantActivities = () => request.get('/merchant/activities')
export const createMerchantActivity = (data) => request.post('/merchant/activities', data)
export const updateMerchantActivity = (id, data) => request.put(`/merchant/activities/${id}`, data)
export const deleteMerchantActivity = (id) => request.delete(`/merchant/activities/${id}`)
export const getMerchantActivityBookings = (id) => request.get(`/merchant/activities/${id}/bookings`)
export const checkinBooking = (id) => request.post(`/merchant/bookings/${id}/checkin`)
export const rejectBooking = (id) => request.post(`/merchant/bookings/${id}/reject`)

export const getMerchantProducts = () => request.get('/merchant/products')
export const createMerchantProduct = (data) => request.post('/merchant/products', data)
export const updateMerchantProduct = (id, data) => request.put(`/merchant/products/${id}`, data)
export const deleteMerchantProduct = (id) => request.delete(`/merchant/products/${id}`)

export const getMerchantOrders = () => request.get('/merchant/orders')

// ========== 管理员端 ==========
export const getAdminMerchants = (status) => request.get('/admin/merchants', { params: { status } })
export const auditMerchant = (id, approve, remark) => request.post(`/admin/merchants/${id}/audit`, { approve, remark })
export const getAdminActivities = (auditStatus) => request.get('/admin/activities', { params: { auditStatus } })
export const auditAdminActivity = (id, approve, remark) => request.post(`/admin/activities/${id}/audit`, { approve, remark })
export const getAdminPendingPosts = (page = 0, size = 20) => request.get('/admin/posts/pending', { params: { page, size } })
export const auditPost = (id, pass) => request.post(`/admin/posts/${id}/audit`, { pass })
export const getAdminUsers = (page = 0, size = 20, keyword) => request.get('/admin/users', { params: { page, size, keyword } })
export const banUser = (id) => request.post(`/admin/users/${id}/ban`)
export const unbanUser = (id) => request.post(`/admin/users/${id}/unban`)
export const getAdminOfficial = () => request.get('/admin/official')
export const createOfficialContent = (data) => request.post('/admin/official', data)
export const deleteOfficialContent = (id) => request.delete(`/admin/official/${id}`)
export const getAdminAdmins = () => request.get('/admin/admins')
export const createAdminAccount = (data) => request.post('/admin/admins', data)
export const updateAdminAccount = (id, data) => request.put(`/admin/admins/${id}`, data)
export const updateAdminAccountPassword = (id, data) => request.put(`/admin/admins/${id}/password`, data)
export const deleteAdminAccount = (id) => request.delete(`/admin/admins/${id}`)

// ========== 用户侧订单 & 报名 ==========
export const createOrder = (data) => request.post('/orders', data)
export const getMyOrders = () => request.get('/orders')
export const getMyOrderOverview = () => request.get('/orders/overview')
export const cancelOrder = (id) => request.post(`/orders/${id}/cancel`)
export const bookActivity = (activityId, data) => request.post(`/orders/activities/${activityId}/book`, data)
export const cancelActivityBooking = (id) => request.post(`/orders/bookings/${id}/cancel`)
export const deleteActivityBooking = (id) => request.delete(`/orders/bookings/${id}`)
export const getMyAchievements = () => request.get('/user/achievements')
export const getUserHomepage = (userId) => request.get(`/user/homepage/${userId}`)
export const getHomepageSettings = () => request.get('/user/homepage-settings')
export const updateHomepageSettings = (data) => request.put('/user/homepage-settings', data)
export const followUser = (userId) => request.post(`/user/follow/${userId}`)
export const unfollowUser = (userId) => request.delete(`/user/follow/${userId}`)
export const getFollowStatus = (userId) => request.get(`/user/follow/${userId}/status`)
export const getVisitorRecords = () => request.get('/user/visitor-records')
export const uploadImage = (formData, folder = 'images') => request.post(`/upload/image?folder=${encodeURIComponent(folder)}`, formData, {
  headers: {
    'Content-Type': 'multipart/form-data'
  }
})
