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

// ========== 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢锝嗙５闁逞屽墾缁犳挸鐣烽悡搴唵婵犻潧鐗婇崵鈧銈庡亝缁诲嫰骞戦崟顖涙優閻犲洠鍓濊倴婵犵數濮烽弫鎼佸磻濞戞娑樷枎閹惧磭顔囬梺褰掓？閻掞箓宕曞Δ浣风箚闁靛牆鎳忛崳娲煟閹惧啿鏆ｉ柡灞炬礃瀵板嫰宕煎┑鍡╃€烽柣鐐寸啲缁犳挸顫忓ú顏勭畾鐟滃繒绮婚悜妯镐簻妞ゆ劑鍩勫Σ娲煃瑜滈崜姘卞枈瀹ュ懐鏆嗛柟闂寸閽冪喖鏌ｉ弮鍥仩缁炬儳鍚嬫穱濠囶敍濞戞瑣浠㈤梺鍝ュ仩濞夋盯鍩為幋鐐茬疇闂佺锕ュú鐔肩嵁婵犲懐鐤€婵炴垶鐟ユ禍妤呮⒑缂佹ɑ鐓ラ柛姘儔閹繝宕橀鐣屽幈濠电娀娼уΛ妤咁敂閳哄懏鐓涢悗锝冨妼閳ь剚绻堝濠氭晲婢跺﹦顔婂┑掳鍊曢崯浼村汲椤愶附鈷戦柛婵嗗濠€鎵磼鐎ｎ偄鐏撮柛鈹垮灩椤撳ジ宕堕妸銉у酱闂備礁鎲￠悷銉┧囨导鏉戠畺婵炲棙鍨圭壕钘壝归敐鍛儓閺嶁剝绻濋悽闈涗粶闁挎洦浜滈锝嗙節濮橆剙宓嗛梺缁樻椤ユ挾绮ｉ悙鐑樷拺闁告挻褰冩禍婵堢磼鐠囨彃鈧綊骞堥妸锔藉劅闁挎繂娲ㄩ敍?==========
export const getPublicActivities = (params) => request.get('/public/activities', { params })
export const getPublicActivityDetail = (id) => request.get(`/public/activities/${id}`)
export const getOfficialContents = (category) => request.get('/public/official', { params: { category } })
export const getHomepageOfficialContents = () => request.get('/public/official/homepage')
export const submitUnbanApplication = (account, reason) => request.post('/public/unban-applications', { account, reason })
export const getLatestUnbanApplication = (account) => request.get('/public/unban-applications/latest', { params: { account } })

// ========== 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢锝嗙５闁逞屽墾缁犳挸鐣烽悡搴唵婵犻潧鐗婇崵鈧銈庡亝缁诲嫰骞戦崟顖涙優閻犲洠鍓濊倴婵犵數濮烽弫鎼佸磻濞戞娑樷枎閹惧磭顔囬梺褰掓？閻掞箓宕曞Δ浣风箚闁靛牆鎳忛崳娲煟閹惧啿鏆ｉ柡灞炬礃瀵板嫰宕煎┑鍡╃€烽柣鐐寸啲缁犳挸顫忓ú顏勭畾鐟滃繒绮婚悜妯镐簻妞ゆ劑鍩勫Σ娲煃瑜滈崜姘卞枈瀹ュ懐鏆嗛柟闂寸閽冪喖鏌ｉ弮鍥仩缁炬儳鍚嬫穱濠囶敍濞戞瑣浠㈤梺鍝ュ仩濞夋盯鍩為幋鐐茬疇闂佺锕ュú鐔肩嵁婵犲懐鐤€婵炴垶鐟ユ禍妤呮⒑缂佹ɑ鐓ラ柛姘儔閹繝宕橀鐣屽幈濠电娀娼уΛ妤咁敂閳哄懏鐓涢悗锝冨妼閳ь剚绻堝濠氭晲婢跺﹦顔婂┑掳鍊曢崯浼村汲椤愶附鈷戦柛婵嗗濠€鎵磼鐎ｎ偄鐏撮柛鈹垮灩椤撳ジ宕堕妸銉у酱闂備礁鎲￠悷銉┧囨导鏉戠畺婵炲棙鍨圭壕钘壝归敐鍛儓閺嶁剝绻濋悽闈涗粶闁挎洦浜滈锝嗙節濮橆剙宓嗛梺缁樻椤ユ挾绮ｉ悙鐑樷拺闁告挻褰冩禍婵堢磼鐠囨彃鈧綊骞堥妸锔藉劅闁挎繂娲ㄩ敍?==========
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
export const getMerchantStats = () => request.get('/merchant/stats')



// ========== 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為〃鍛粹€﹂妸鈺侀唶婵犻潧鐗忓畷鍫曟⒑绾懎浜归悶娑栧劦瀹曞綊骞嗚濡插牊淇婇娑氱煁婵☆偄鍟悾鐑筋敂閸涱喖顎撻柣鐔哥懃鐎氼參鎮甸敃鍌涒拺閻犲洦鐓￠妤冪磼閻樿尙效鐎规洘娲熷畷锟犳倶缂佹ɑ銇濆┑鈩冩倐閺佸倿鏌ㄩ娑氫簷闂佽楠哥粻宥夊磿闁单鍥ㄥ鐎涙ê浜楅梺闈涱檧婵″洨绮绘ィ鍐╃厵閻庣敻鏅茬槐铏亜韫囨挾澧遍柡浣稿€归妵鍕疀閹捐泛顤€闂?==========
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

// ========== 闂傚倸鍊搁崐宄懊归崶顒夋晪鐟滃秹锝炲┑瀣櫇闁稿矉濡囩粙蹇旂節閵忥絾纭鹃悗娑掓櫊楠炲﹤鐣濋崟顑芥嫽婵炶揪绲介幉锟犲箚閸儲鐓熸い鎺嗗亾闁靛牏顭堥悾閿嬪閺夋垵鍞ㄩ悷婊冪箻瀵娊鏁傞懞銉ュ伎濠碉紕鍋犻褎绂嶆ィ鍐╁€甸悷娆忓绾炬悂鏌涢弮鈧崹鍧楀Υ娴ｈ倽鐔封枎閻愵儷鈺呮⒒娴ｅ憡鎯堥柟閿嬪灩瀵板﹦鎹勯妸锔绘綗闂佽鍎抽悺銊﹀垔閹绢喗鐓曟繝闈涙椤忔挳鏌嶈閸撴岸骞愰崜褎顫曢柟鐑樻尰缂嶅洭鏌曟繝蹇擃洭妞わ附婢樿灃?==========
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

// ========== 闂傚倸鍊搁崐鎼佸磹瀹勬噴褰掑炊瑜忛弳锕傛煕椤垵浜濋柛娆忕箻閺岀喖骞嗛弶鍟冩捇鏌嶉柨瀣拻闁逞屽墮缁犲秹宕曢柆宓ュ洭顢涢悙鎻掔€梺绋跨灱閸嬬偤鎮￠悢鍏肩厽闁哄啫鍋嗛悞鐐亜閵夈儺鍎旈柡?& 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢锝嗙缂佺姷濞€閺岀喖宕滆鐢盯鏌涚€ｃ劌鈧繈寮婚弴鐔虹闁绘劦鍓氶悵鏃傜磽娴ｆ彃浜炬繝銏ｅ煐閸旀牠鎮″☉銏＄厱闁靛鍨哄▍鍛存煕閹惧綊鍝虹紒?==========
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
