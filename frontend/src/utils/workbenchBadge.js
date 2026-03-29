import { reactive } from 'vue'
import {
  getAdminActivities,
  getAdminMerchants,
  getAdminPosts,
  getAdminUnbanApplications,
  getConversations,
  getMerchantActivities,
  getMyApplication
} from '@/api/app.js'

const BADGE_STORAGE_KEY = 'yayfolk_workbench_badges_v1'

export const workbenchBadgeState = reactive({
  admin: {
    totalCount: 0,
    merchantsCount: 0,
    activitiesCount: 0,
    postsCount: 0,
    usersCount: 0,
    serviceCount: 0,
    lastUpdatedAt: 0
  },
  merchant: {
    totalCount: 0,
    activitiesCount: 0,
    applicationCount: 0,
    currentActivityFingerprints: {},
    currentApplicationFingerprint: '',
    lastUpdatedAt: 0
  }
})

const resetAdminBadgeState = () => {
  workbenchBadgeState.admin.totalCount = 0
  workbenchBadgeState.admin.merchantsCount = 0
  workbenchBadgeState.admin.activitiesCount = 0
  workbenchBadgeState.admin.postsCount = 0
  workbenchBadgeState.admin.usersCount = 0
  workbenchBadgeState.admin.serviceCount = 0
  workbenchBadgeState.admin.lastUpdatedAt = Date.now()
}

const resetMerchantBadgeState = () => {
  workbenchBadgeState.merchant.totalCount = 0
  workbenchBadgeState.merchant.activitiesCount = 0
  workbenchBadgeState.merchant.applicationCount = 0
  workbenchBadgeState.merchant.currentActivityFingerprints = {}
  workbenchBadgeState.merchant.currentApplicationFingerprint = ''
  workbenchBadgeState.merchant.lastUpdatedAt = Date.now()
}

const readStoredUser = () => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    console.error('Failed to read workbench badge user info', error)
    return null
  }
}

const resolveUserBadgeKey = (user) => {
  if (!user) {
    return ''
  }
  if (user.id !== null && user.id !== undefined && String(user.id).trim() !== '') {
    return String(user.id)
  }
  return String(user.username || '')
}

const readBadgeStorage = () => {
  try {
    const raw = localStorage.getItem(BADGE_STORAGE_KEY)
    if (!raw) {
      return {}
    }
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (error) {
    console.error('Failed to read workbench badge cache', error)
    return {}
  }
}

const writeBadgeStorage = (nextStorage) => {
  try {
    localStorage.setItem(BADGE_STORAGE_KEY, JSON.stringify(nextStorage))
  } catch (error) {
    console.error('Failed to write workbench badge cache', error)
  }
}

const readMerchantSeenState = (user) => {
  const badgeKey = resolveUserBadgeKey(user)
  if (!badgeKey) {
    return {
      applicationFingerprint: '',
      activities: {}
    }
  }

  const storage = readBadgeStorage()
  const userState = storage[badgeKey]?.merchant
  return {
    applicationFingerprint: userState?.applicationFingerprint || '',
    activities: userState?.activities && typeof userState.activities === 'object'
      ? userState.activities
      : {}
  }
}

const writeMerchantSeenState = (user, nextState) => {
  const badgeKey = resolveUserBadgeKey(user)
  if (!badgeKey) {
    return
  }

  const storage = readBadgeStorage()
  storage[badgeKey] = {
    ...(storage[badgeKey] || {}),
    merchant: {
      applicationFingerprint: nextState?.applicationFingerprint || '',
      activities: nextState?.activities && typeof nextState.activities === 'object'
        ? nextState.activities
        : {}
    }
  }
  writeBadgeStorage(storage)
}

const buildActivityFingerprint = (item) => {
  if (!item?.id) {
    return ''
  }

  return [
    item.id,
    item.auditStatus || '',
    item.auditRemark || ''
  ].join('|')
}

const resolveApplicationStatus = (payload) => {
  const status = String(payload?.applicationStatus || payload?.shopStatus || '').trim().toLowerCase()
  return ['approved', 'rejected'].includes(status) ? status : ''
}

const buildApplicationFingerprint = (payload) => {
  const status = resolveApplicationStatus(payload)
  if (!status) {
    return ''
  }

  return [
    payload?.applicationId || 0,
    status,
    payload?.auditRemark || '',
    payload?.shopName || ''
  ].join('|')
}

const buildActivityFingerprintMap = (activities) => {
  return (Array.isArray(activities) ? activities : []).reduce((accumulator, item) => {
    const fingerprint = buildActivityFingerprint(item)
    if (fingerprint && ['approved', 'rejected'].includes(String(item?.auditStatus || '').toLowerCase())) {
      accumulator[String(item.id)] = fingerprint
    }
    return accumulator
  }, {})
}

const sumServiceUnreadCount = (conversations) => {
  return (Array.isArray(conversations) ? conversations : [])
    .filter(item => item?.type === 'service')
    .reduce((total, item) => total + Number(item?.unreadCount || 0), 0)
}

const countActionablePosts = (posts) => {
  return (Array.isArray(posts) ? posts : []).filter(item => {
    const auditStatus = String(item?.auditStatus || '').toLowerCase()
    return ['pending', 'manual_review'].includes(auditStatus) || Number(item?.pendingReportCount || 0) > 0
  }).length
}

const refreshAdminWorkbenchBadges = async (user) => {
  if (!user || user.role !== 'admin') {
    resetAdminBadgeState()
    return
  }

  const isSuperAdmin = Number(user?.isSuperAdmin || 0) === 1
  const [
    merchantsResponse,
    activitiesResponse,
    postsResponse,
    usersResponse,
    conversationsResponse
  ] = await Promise.all([
    getAdminMerchants('pending').catch(() => null),
    getAdminActivities('pending').catch(() => null),
    getAdminPosts(undefined, 0, 500).catch(() => null),
    getAdminUnbanApplications('pending').catch(() => null),
    isSuperAdmin ? Promise.resolve(null) : getConversations().catch(() => null)
  ])

  const merchantsCount = Array.isArray(merchantsResponse?.data) ? merchantsResponse.data.length : 0
  const activitiesCount = Array.isArray(activitiesResponse?.data) ? activitiesResponse.data.length : 0
  const postsCount = countActionablePosts(postsResponse?.data)
  const usersCount = Array.isArray(usersResponse?.data) ? usersResponse.data.length : 0
  const serviceCount = isSuperAdmin ? 0 : sumServiceUnreadCount(conversationsResponse?.data)

  workbenchBadgeState.admin.merchantsCount = merchantsCount
  workbenchBadgeState.admin.activitiesCount = activitiesCount
  workbenchBadgeState.admin.postsCount = postsCount
  workbenchBadgeState.admin.usersCount = usersCount
  workbenchBadgeState.admin.serviceCount = serviceCount
  workbenchBadgeState.admin.totalCount = merchantsCount + activitiesCount + postsCount + usersCount + serviceCount
  workbenchBadgeState.admin.lastUpdatedAt = Date.now()
}

const refreshMerchantWorkbenchBadges = async (user) => {
  if (!user || user.role === 'admin') {
    resetMerchantBadgeState()
    return
  }

  const [applicationResponse, activitiesResponse] = await Promise.all([
    getMyApplication().catch(() => null),
    getMerchantActivities().catch(() => null)
  ])

  const applicationPayload = applicationResponse?.code === 200 ? (applicationResponse.data || {}) : {}
  const activities = Array.isArray(activitiesResponse?.data) ? activitiesResponse.data : []
  const currentActivityFingerprints = buildActivityFingerprintMap(activities)
  const currentApplicationFingerprint = buildApplicationFingerprint(applicationPayload)
  const seenState = readMerchantSeenState(user)

  const activitiesCount = Object.entries(currentActivityFingerprints).filter(([activityId, fingerprint]) => {
    return seenState.activities?.[activityId] !== fingerprint
  }).length

  const applicationCount = currentApplicationFingerprint && seenState.applicationFingerprint !== currentApplicationFingerprint
    ? 1
    : 0

  workbenchBadgeState.merchant.currentActivityFingerprints = currentActivityFingerprints
  workbenchBadgeState.merchant.currentApplicationFingerprint = currentApplicationFingerprint
  workbenchBadgeState.merchant.activitiesCount = activitiesCount
  workbenchBadgeState.merchant.applicationCount = applicationCount
  workbenchBadgeState.merchant.totalCount = activitiesCount + applicationCount
  workbenchBadgeState.merchant.lastUpdatedAt = Date.now()
}

export const refreshWorkbenchBadges = async () => {
  const user = readStoredUser()
  await Promise.all([
    refreshAdminWorkbenchBadges(user),
    refreshMerchantWorkbenchBadges(user)
  ])
}

export const markMerchantActivityUpdatesSeen = () => {
  const user = readStoredUser()
  if (!user) {
    return
  }

  const seenState = readMerchantSeenState(user)
  writeMerchantSeenState(user, {
    ...seenState,
    activities: {
      ...seenState.activities,
      ...workbenchBadgeState.merchant.currentActivityFingerprints
    }
  })

  workbenchBadgeState.merchant.activitiesCount = 0
  workbenchBadgeState.merchant.totalCount =
    workbenchBadgeState.merchant.activitiesCount + workbenchBadgeState.merchant.applicationCount
}

export const markMerchantApplicationUpdateSeen = () => {
  const user = readStoredUser()
  if (!user || !workbenchBadgeState.merchant.currentApplicationFingerprint) {
    return
  }

  const seenState = readMerchantSeenState(user)
  writeMerchantSeenState(user, {
    ...seenState,
    applicationFingerprint: workbenchBadgeState.merchant.currentApplicationFingerprint
  })

  workbenchBadgeState.merchant.applicationCount = 0
  workbenchBadgeState.merchant.totalCount =
    workbenchBadgeState.merchant.activitiesCount + workbenchBadgeState.merchant.applicationCount
}
