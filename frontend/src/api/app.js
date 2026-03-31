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

export const getActivityNotificationUnreadCount = () => {
  return getUnreadCount()
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

// ========== 闂傚倸鍊搁崐鎼佸磹閻戣姤鍤勯柛顐ｆ磵閳ь剨绠撳畷鐓庮熆濠靛牊鍤€妞ゎ偅绻勯幑鍕惞鐠団剝肖濠电姷鏁搁崑娑樜涘▎鎾崇闁归棿鐒﹂崕妤佷繆閵堝懏鍣洪柣鎾冲暣閺屾洘寰勯崼婵嗩瀷閻炴熬绠撳娲箚瑜忕粻鐑樸亜椤愩埄妲洪柍褜鍓氱喊宥呯暆閹间礁钃熼柣鏃囨绾惧吋淇婇娑欍仢闁哥偠娉涢埞鎴炲箠闁稿﹥娲熼獮濠呯疀濞戞瑥浜楅梺缁樻煥閸氬鎮￠崘顔界厱婵犻潧妫楅鈺呮煛鐎ｃ劌鈧繈寮婚敐澶婄婵°倕鍟伴弳顐︽⒑閸濆嫭婀扮紒瀣灴閸┿垹顓奸崶銊ョ彴闂佸憡鐟ラˇ浼村箖濞嗘垹纾藉ù锝呮惈鏍℃繝鐢靛仜閿曨亜顕ｆ繝姘嵆闁绘棃顥撶粣鐐烘⒑閸撴彃浜濈紒璇插€归幈銊︽償閿濆洨锛?==========
export const getPublicActivities = (params) => request.get('/public/activities', { params })
export const getPublicActivityDetail = (id) => request.get(`/public/activities/${id}`)
export const getOfficialContents = (category) => request.get('/public/official', { params: { category } })
export const getHomepageOfficialContents = () => request.get('/public/official/homepage')
export const submitUnbanApplication = (account, reason) => request.post('/public/unban-applications', { account, reason })
export const getLatestUnbanApplication = (account) => request.get('/public/unban-applications/latest', { params: { account } })

// ========== 闂傚倸鍊搁崐鎼佸磹閻戣姤鍤勯柛顐ｆ磵閳ь剨绠撳畷鐓庮熆濠靛牊鍤€妞ゎ偅绻勯幑鍕惞鐠団剝肖濠电姷鏁搁崑娑樜涘▎鎾崇闁归棿鐒﹂崕妤佷繆閵堝懏鍣洪柣鎾冲暣閺屾洘寰勯崼婵嗩瀷閻炴熬绠撳娲箚瑜忕粻鐑樸亜椤愩埄妲洪柍褜鍓氱喊宥呯暆閹间礁钃熼柣鏃囨绾惧吋淇婇娑欍仢闁哥偠娉涢埞鎴炲箠闁稿﹥娲熼獮濠呯疀濞戞瑥浜楅梺缁樻煥閸氬鎮￠崘顔界厱婵犻潧妫楅鈺呮煛鐎ｃ劌鈧繈寮婚敐澶婄婵°倕鍟伴弳顐︽⒑閸濆嫭婀扮紒瀣灴閸┿垹顓奸崶銊ョ彴闂佸憡鐟ラˇ浼村箖濞嗘垹纾藉ù锝呮惈鏍℃繝鐢靛仜閿曨亜顕ｆ繝姘嵆闁绘棃顥撶粣鐐烘⒑閸撴彃浜濈紒璇插€归幈銊︽償閿濆洨锛?==========
export const applyMerchant = (data) => request.post('/merchant/apply', data)
export const getMyApplication = () => request.get('/merchant/apply/status')

export const getMerchantActivities = () => request.get('/merchant/activities')
export const createMerchantActivity = (data) => request.post('/merchant/activities', data)
export const updateMerchantActivity = (id, data) => request.put(`/merchant/activities/${id}`, data)
export const deleteMerchantActivity = (id) => request.delete(`/merchant/activities/${id}`)
export const getMerchantActivityBookings = (id) => request.get(`/merchant/activities/${id}/bookings`)
export const getMerchantBookings = (params) => request.get('/merchant/bookings', { params })
export const getMerchantBookingDetail = (id) => request.get(`/merchant/bookings/${id}`)
export const lookupMerchantBooking = (code) => request.get('/merchant/bookings/lookup', { params: { code } })
export const lookupMerchantBookingByImage = (imageData) => request.post('/merchant/bookings/lookup-image', { imageData })
export const checkinBooking = (id) => request.post(`/merchant/bookings/${id}/checkin`)
export const refundMerchantBooking = (id, data = {}) => request.post(`/merchant/bookings/${id}/refund`, data)
export const rejectBooking = (id, data = {}) => request.post(`/merchant/bookings/${id}/reject`, data)



// ========== 闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偞顨呴…銊╁醇濠靛牏宕堕梺纭呭亹鐞涖儵宕归幆褉妲堟俊顖涚矋濡啫鐣烽鍛閻熸瑥瀚悵锕傛⒑鐠囨煡顎楃紒鐘茬Ч瀹曟洟宕￠悘缁樻そ婵℃悂鏁傞柨顖涚亙闁诲骸绠嶉崕閬嵥囨导瀛樺亗闁靛濡囩粻楣冩煙鐎甸晲绱虫い蹇撶墱閺佸倹銇勮箛鎾跺闁?==========
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

// ========== 闂傚倸鍊峰ù鍥敋瑜嶉～婵嬫晝閸岋妇绋忔繝銏ｆ硾鐎涒晠骞婂畝鍕拻濞达絽鎲￠幆鍫ユ煟椤掆偓閵堢鐣锋导鏉戝唨鐟滃繘寮抽敂鑺ュ弿婵＄偠顕ф禍楣冩倵鐟欏嫭纾搁柛鏃€鍨块妴浣肝熷▎鐐╅梻浣告惈閹锋垹寰婄捄銊︻潟闁规儳鐡ㄦ刊鎾煕濠靛棗顏撮柍褜鍓氶幐鍓ф閹烘挻缍囬柕濠忓椤︽澘螖?==========
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

// ========== 闂傚倸鍊搁崐宄懊归崶褏鏆﹂柛顭戝亝閸欏繘鏌熼幆鏉啃撻柍閿嬫⒒閳ь剙绠嶉崕閬嵥囬鐐插瀭闁稿瞼鍋為悡鐔兼煟閺冨偆鐒炬い銉ヮ儔閺?& 闂傚倸鍊搁崐鎼佸磹閻戣姤鍤勯柛顐ｆ礀缁犵娀鏌熼崜褏甯涢柛瀣ㄥ€濋弻鏇熺箾閻愵剚鐝旂紓浣插亾濠㈣埖鍔栭悡娑㈡煕閵夈垺娅呴柛鎾归哺缁?==========
export const getMyOrderOverview = () => request.get('/orders/overview')
export const getMyOrders = () => request.get('/orders')
export const payOrder = (id, data = {}) => request.post(`/orders/${id}/pay`, data)
export const reconcileAlipayTrade = (data) => request.post('/alipay/reconcile', data)
export const cancelOrder = (id) => request.post(`/orders/${id}/cancel`)
export const bookActivity = (activityId, data) => request.post(`/orders/activities/${activityId}/book`, data)
export const cancelActivityBooking = (id) => request.post(`/orders/bookings/${id}/cancel`)
export const deleteActivityBooking = (id) => request.delete(`/orders/bookings/${id}`)
export const getActivityBookingDetail = (id) => request.get(`/orders/bookings/${id}`)
export const payActivityBooking = (id, data = {}) => request.post(`/orders/bookings/${id}/pay`, data)
export const getActivityBookingQrCode = (id) => request.get(`/orders/bookings/${id}/qrcode`)
export const submitActivityBookingReview = (id, data) => request.post(`/orders/bookings/${id}/review`, data)
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








export const getUserProfile = () => request.get('/user/profile')
export const updateUserProfile = (data) => request.put('/user/profile', data)
export const getFollowers = (userId) => request.get(`/user/${userId}/followers`)
export const getFollowing = (userId) => request.get(`/user/${userId}/following`)
export const getCollectedBy = (userId) => request.get(`/user/${userId}/collected-by`)
export const updateDiscoverPostVisibility = (postId, visibility) => request.put(`/discover/my/posts/${postId}/visibility`, { visibility })
export const uploadAvatar = (formData) => request.post('/upload/avatar', formData, {
  headers: {
    'Content-Type': 'multipart/form-data'
  },
  timeout: 60000
})

export const uploadVideo = (formData, folder = 'media') => uploadMedia(formData, folder)
