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

// ========== 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌ｉ幋锝呅撻柛銈呭閺屾盯顢曢敐鍡欙紩闂侀€炲苯澧剧紒鐘虫尭閻ｇ兘鎮℃惔顔惧數濠电娀娼ч悧濠囧吹閳ь剙顪冮妶搴′簼缂佽瀚伴獮鎴﹀礋椤栨稒鍎柣鐘叉礌閸撴繆鍊村┑鐘垫暩婵兘寮幖浣哥；婵炴垶顭傚☉妯锋瀻闁规儳纾鍥⒑瑜版帗锛熼柣鎺炵畵瀹曟洖螖娴ｉ绠氶梺闈涚墕閹冲繘宕冲ú顏呯厽闁规儳鍟块弳锝夋煛鐏炵偓绀冪€垫澘瀚板畷鐓庘攽閸♀晝鈧兘鏌ｉ悙瀵稿暡缂佺姵鎸搁～蹇撁洪鍕暰閻熸粌绻掔划濠氭倻濡晲绨诲銈嗗姂閸╁嫬危濞差亝鐓冪憸婊堝礈濮樺崬鏋堢€广儱鎳愰弳鍡涙煙闂傚顦﹂柦鍐枛閺岋綁寮崶顭戜哗缂佺偓鍎抽崥瀣┍婵犲浂鏁嶆繛鎴炵懀娴犮垽姊洪崫銉ヤ哗婵炲鐩崺鐐哄箣閻愯尙鐤囬梻浣侯焾閿曘儱煤閻旇偐宓佸┑鐘叉噽閻も偓濠电偞鍨堕悷銉︾濡ゅ懏鈷戠紓浣股戦悡銉╂煕濮橆剦鍎旈柟顕嗙節瀹曟﹢顢欓悾灞藉箞婵犵數濞€濞佳兾涘Δ鍜佹晜闁冲搫鎳忛悡娑㈡倵閿濆啫濡奸柍褜鍓氱换鍫濐嚕婵犳碍鏅插璺猴功椤斿﹤鈹戞幊閸婃洟宕导鏉戞辈妞ゆ劧闄勯埛鎴︽煕濠靛棗顏繝鈧幍顔剧＜閻庯綆鍋勯悘鎾煕閳瑰灝鐏╂い鎾炽偢瀹曞爼濡搁妷褍閰遍梻鍌欑閹诧繝鎮烽妷鈹у洦瀵奸弶鎴犵暫濠电偛妫欓崹鍦閽樺褰掓晲閸涱喗鍎撻柡宥佸墲缁绘繈鎮介棃娑楃捕闂佹寧娲︽禍婊堫敋閿濆棛绡€婵﹩鍓欏畵鍡涙⒑缂佹ɑ顥堟い銉︽尵缁綁鎮欓悜妯锋嫼闂佸憡鎸昏ぐ鍐╃濠靛牏纾奸悹鍥ㄥ絻閳ь剙缍婇獮鍫ュΩ閿旇棄鍔呴梺鎸庣箓濞层劑鏁?==========
export const getPublicActivities = (params) => request.get('/public/activities', { params })
export const getPublicActivityDetail = (id) => request.get(`/public/activities/${id}`)
export const getOfficialContents = (category) => request.get('/public/official', { params: { category } })
export const getHomepageOfficialContents = () => request.get('/public/official/homepage')
export const submitUnbanApplication = (account, reason) => request.post('/public/unban-applications', { account, reason })
export const getLatestUnbanApplication = (account) => request.get('/public/unban-applications/latest', { params: { account } })

// ========== 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌ｉ幋锝呅撻柛銈呭閺屾盯顢曢敐鍡欙紩闂侀€炲苯澧剧紒鐘虫尭閻ｇ兘鎮℃惔顔惧數濠电娀娼ч悧濠囧吹閳ь剙顪冮妶搴′簼缂佽瀚伴獮鎴﹀礋椤栨稒鍎柣鐘叉礌閸撴繆鍊村┑鐘垫暩婵兘寮幖浣哥；婵炴垶顭傚☉妯锋瀻闁规儳纾鍥⒑瑜版帗锛熼柣鎺炵畵瀹曟洖螖娴ｉ绠氶梺闈涚墕閹冲繘宕冲ú顏呯厽闁规儳鍟块弳锝夋煛鐏炵偓绀冪€垫澘瀚板畷鐓庘攽閸♀晝鈧兘鏌ｉ悙瀵稿暡缂佺姵鎸搁～蹇撁洪鍕暰閻熸粌绻掔划濠氭倻濡晲绨诲銈嗗姂閸╁嫬危濞差亝鐓冪憸婊堝礈濮樺崬鏋堢€广儱鎳愰弳鍡涙煙闂傚顦﹂柦鍐枛閺岋綁寮崶顭戜哗缂佺偓鍎抽崥瀣┍婵犲浂鏁嶆繛鎴炵懀娴犮垽姊洪崫銉ヤ哗婵炲鐩崺鐐哄箣閻愯尙鐤囬梻浣侯焾閿曘儱煤閻旇偐宓佸┑鐘叉噽閻も偓濠电偞鍨堕悷銉︾濡ゅ懏鈷戠紓浣股戦悡銉╂煕濮橆剦鍎旈柟顕嗙節瀹曟﹢顢欓悾灞藉箞婵犵數濞€濞佳兾涘Δ鍜佹晜闁冲搫鎳忛悡娑㈡倵閿濆啫濡奸柍褜鍓氱换鍫濐嚕婵犳碍鏅插璺猴功椤斿﹤鈹戞幊閸婃洟宕导鏉戞辈妞ゆ劧闄勯埛鎴︽煕濠靛棗顏繝鈧幍顔剧＜閻庯綆鍋勯悘鎾煕閳瑰灝鐏╂い鎾炽偢瀹曞爼濡搁妷褍閰遍梻鍌欑閹诧繝鎮烽妷鈹у洦瀵奸弶鎴犵暫濠电偛妫欓崹鍦閽樺褰掓晲閸涱喗鍎撻柡宥佸墲缁绘繈鎮介棃娑楃捕闂佹寧娲︽禍婊堫敋閿濆棛绡€婵﹩鍓欏畵鍡涙⒑缂佹ɑ顥堟い銉︽尵缁綁鎮欓悜妯锋嫼闂佸憡鎸昏ぐ鍐╃濠靛牏纾奸悹鍥ㄥ絻閳ь剙缍婇獮鍫ュΩ閿旇棄鍔呴梺鎸庣箓濞层劑鏁?==========
export const applyMerchant = (data) => request.post('/merchant/apply', data)
export const getMyApplication = () => request.get('/merchant/apply/status')

export const getMerchantActivities = (params) => request.get('/merchant/activities', { params })
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



// ========== 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾剧懓顪冪€ｎ亝鎹ｉ柣顓炴閵嗘帒顫濋敐鍛婵°倗濮烽崑鐐恒€冮崨绮光偓锕傚Ω閳轰線鍞跺┑鐘绘涧閻楀繐鐣烽崼鏇熲拺缁绢厼鎳庢禍褰掓偠濞戞牕鍔︾€规洖缍婇獮鍡氼槷婵℃彃鐗婃穱濠囶敍濞戞氨鐓佸┑鈽嗗亜閸燁偊鎮鹃悜绛嬫晜闁告侗鍠栭鎾绘煟閻斿摜鎳冮悗姘煎弮閹敻鏁冮崒娑掓嫼闁荤姴娲﹂悡锟狀敁濡ゅ啰纾奸柣妯垮皺鏁堥悗瑙勬礃濞茬喎鐣烽敓鐘冲€剁紓浣股戦妵婵嗏攽閳╁啯鍊愰柡浣稿€块弻銊╊敍濞戞矮绨烽梻浣筋嚙妤犲摜绮诲澶婄？闂侇剙鍗曢崶銊ヮ嚤閻庢稒锚娴滄姊洪棃娑辨濠碘€虫川缁粯銈ｉ崘鈺冨幍闁诲海鏁婚弲鑼閾忣偁浜滈煫鍥ㄦ尵婢ч亶鏌℃担绋库偓褰掑Φ閸曨喚鐤€闁规崘娉涢·鈧梻?==========
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

// ========== 闂傚倸鍊搁崐鎼佸磹瀹勬噴褰掑炊椤掑鏅悷婊冪Ч閿濈偛鈹戠€ｎ偅娅囬梺绋跨焿婵″洨绮欒箛鏃傜瘈闁靛骏绲剧涵楣冩倵濞戞帗娅婃鐐诧工閻ｆ繈宕熼鑺ュ濠电偠鎻徊浠嬪箟閿熺姴绠氶柛顐犲劜閻撶喐銇勯幒鍡椾壕闂侀潧鐗忛…鍫ユ偩闁垮顕遍柡澶嬪灥閸炪劑鎮峰鍐鐎殿喗濞婇弫鍌炴嚍閵夈儱浼庢繝纰夌磿閸嬬娀顢氳缁傚秵銈ｉ崘鈺佲偓鐢告偡濞嗗繐顏痪鐐倐閺屾盯寮埀顒勫垂閸ф违濞达綀鍊介悢灏佹瀻闁绘劦鍎烽埡鍛拻濞达絽鎲￠幆鍫ユ煙闁垮鐏╃€垫澘锕﹂幑鍕Ω閿旂粯缍楅梻浣筋潐閸庢娊鎮洪妸锕€鍨旈柟缁㈠枟閻撴洘绻濋棃娑欘棞妞ゅ繑鎸抽弻宥堫檨闁告挻宀搁獮鎰板礈瑜庨～鏇㈡煙閻戞ɑ灏扮紓宥呮喘閺屾洘绻濊箛鎿冩喘濡炪倧闄勫妯跨亙?==========
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

// ========== 闂傚倸鍊搁崐鎼佸磹閹间礁纾圭€瑰嫭鍣磋ぐ鎺戠倞鐟滃繘寮抽敃鍌涚厱妞ゎ厽鍨垫禍婵嬫煕濞嗗繒绠婚柡宀€鍠栭獮鍡涘级閸熷啯鎹囬弻宥夋煥鐎ｎ偀鎷婚梺閫炲苯澧紒鐘茬Ч瀹曟洟鏌嗗畵銉ユ喘椤㈡盯鎮欓幓鎺斺偓顓㈡⒑缁嬭法鐏遍柛瀣仱閹繝鎮㈤崗鑲╁幗闂佸搫鍟崑鍡涙倿閻愵兙浜滈柕澶堝労閸庢棃鏌?& 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌ｉ幋锝呅撻柛銈呭閺屾盯顢曢敐鍡欘槬缂備胶濮锋繛鈧柡宀€鍠栧畷婊嗩槾閻㈩垱鐩弻娑氣偓锝冨妼閳ь剚绻堝濠氬即閻旇櫣顔曢梺缁樺姦閸撴岸鎮甸弮鍌滅＝濞达絾褰冩禍鐐節閵忥絽鐓愰柛鏃€鐗犻幃鈥斥槈閵忥紕鍘遍梺闈涱槶閸ㄥ搫鈻嶉崨瀛樼厱闁规儳缍婇崫铏圭磼?==========
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
