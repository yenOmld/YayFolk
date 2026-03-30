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

export const reportDiscoverPost = (postId, reason) => {
  return request.post(`/discover/posts/${postId}/report`, { reason })
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

export const createCustomerServiceConversation = () => {
  return request.post('/messages/customer-service')
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

export const classifyDiscoverImage = (formData) => {
  return request.post('/classification/predict', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 120000
  })
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
export const saveHeritageRoute = (data) => request.post('/ai/heritage-route/favorites', data)
export const getSavedHeritageRoutes = () => request.get('/ai/heritage-route/favorites')
export const getSavedHeritageRouteDetail = (id) => request.get(`/ai/heritage-route/favorites/${id}`)
export const deleteSavedHeritageRoute = (id) => request.delete(`/ai/heritage-route/favorites/${id}`)

// ========== 鍏叡娲诲姩鐩稿叧鎺ュ彛 ==========
export const getPublicActivities = (params) => request.get('/public/activities', { params })
export const getPublicActivityDetail = (id) => request.get(`/public/activities/${id}`)
export const getOfficialContents = (category) => request.get('/public/official', { params: { category } })
export const getHomepageOfficialContents = () => request.get('/public/official/homepage')
export const submitUnbanApplication = (account, reason) => request.post('/public/unban-applications', { account, reason })
export const getLatestUnbanApplication = (account) => request.get('/public/unban-applications/latest', { params: { account } })

// ========== 鍏叡娲诲姩鐩稿叧鎺ュ彛 ==========
export const applyMerchant = (data) => request.post('/merchant/apply', data)
export const getMyApplication = () => request.get('/merchant/apply/status')

export const getMerchantActivities = () => request.get('/merchant/activities')
export const createMerchantActivity = (data) => request.post('/merchant/activities', data)
export const updateMerchantActivity = (id, data) => request.put(`/merchant/activities/${id}`, data)
export const deleteMerchantActivity = (id) => request.delete(`/merchant/activities/${id}`)
export const getMerchantActivityBookings = (id) => request.get(`/merchant/activities/${id}/bookings`)
export const checkinBooking = (id) => request.post(`/merchant/bookings/${id}/checkin`)
export const rejectBooking = (id) => request.post(`/merchant/bookings/${id}/reject`)



// ========== 鍟嗗绠＄悊 ==========
export const getAdminMerchants = (status) => request.get('/admin/merchants', { params: { status } })
export const auditMerchant = (id, approve, remark) => request.post(`/admin/merchants/${id}/audit`, { approve, remark })
export const getAdminActivities = (auditStatus) => request.get('/admin/activities', { params: { auditStatus } })
export const auditAdminActivity = (id, approve, remark) => request.post(`/admin/activities/${id}/audit`, { approve, remark })
export const getAdminPosts = (auditStatus, page = 0, size = 20) => request.get('/admin/posts', { params: { auditStatus, page, size } })
export const auditPost = (id, pass, remark) => request.post(`/admin/posts/${id}/audit`, { pass, remark })
export const batchAuditPosts = (ids, pass, remark) => request.post('/admin/posts/batch-audit', { ids, pass, remark })
export const getAdminUsers = (page = 0, size = 20, keyword) => request.get('/admin/users', { params: { page, size, keyword } })
export const banUser = (id, reason) => request.post(`/admin/users/${id}/ban`, { reason })
export const unbanUser = (id) => request.post(`/admin/users/${id}/unban`)
export const getAdminUnbanApplications = (status) => request.get('/admin/users/unban-applications', { params: { status } })
export const auditAdminUnbanApplication = (id, approve, remark) => request.post(`/admin/users/unban-applications/${id}/audit`, { approve, remark })
export const getAdminOfficial = () => request.get('/admin/official')
export const createOfficialContent = (data) => request.post('/admin/official', data)
export const deleteOfficialContent = (id) => request.delete(`/admin/official/${id}`)
export const getAdminAdmins = () => request.get('/admin/admins')
export const createAdminAccount = (data) => request.post('/admin/admins', data)
export const updateAdminAccount = (id, data) => request.put(`/admin/admins/${id}`, data)
export const updateAdminAccountPassword = (id, data) => request.put(`/admin/admins/${id}/password`, data)
export const deleteAdminAccount = (id) => request.delete(`/admin/admins/${id}`)

// ========== 官方内容管理 ==========
export const getOfficialActivities = () => request.get('/admin/official/activities')
export const createOfficialActivity = (data) => request.post('/admin/official/activities', data)
export const updateOfficialActivity = (id, data) => request.put(`/admin/official/activities/${id}`, data)
export const deleteOfficialActivity = (id) => request.delete(`/admin/official/activities/${id}`)
export const getOfficialHeritages = () => request.get('/admin/official/heritages')
export const createOfficialHeritage = (data) => request.post('/admin/official/heritages', data)
export const updateOfficialHeritage = (id, data) => request.put(`/admin/official/heritages/${id}`, data)
export const deleteOfficialHeritage = (id) => request.delete(`/admin/official/heritages/${id}`)
export const getOfficialWorks = () => request.get('/admin/official/works')
export const getOfficialPublishedState = () => request.get('/admin/official/published')
export const createOfficialWork = (data) => request.post('/admin/official/works', data)
export const updateOfficialWork = (id, data) => request.put(`/admin/official/works/${id}`, data)
export const deleteOfficialWork = (id) => request.delete(`/admin/official/works/${id}`)
export const publishToHomepage = (type, ids) => request.post('/admin/official/publish', { type, ids })

// ========== 璁㈠崟 & 鎴愬氨 ==========
export const getMyOrderOverview = () => request.get('/orders/overview')
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

export const uploadMedia = (formData, folder = 'media') => request.post(`/upload/media?folder=${encodeURIComponent(folder)}`, formData, {
  headers: {
    'Content-Type': 'multipart/form-data'
  }
})








