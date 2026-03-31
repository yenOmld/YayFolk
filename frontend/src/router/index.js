import { createRouter, createWebHistory } from 'vue-router'
import WelcomeView from '../views/WelcomeView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import HomeView from '../views/HomeView.vue'
import DiscoverView from '../views/DiscoverView.vue'
import PersonalCenter from '../views/PersonalCenter.vue'
import Settings from '../views/Settings.vue'
import EditProfile from '../views/EditProfile.vue'
import UserHomepage from '../views/UserHomepage.vue'
import ConversationMode from '../views/ConversationMode.vue'
import NotificationView from '../views/NotificationView.vue'
import ShareView from '../views/ShareView.vue'
import IntangibleCulturalHeritage from '../views/IntangibleCulturalHeritage.vue'
import AIHeritageRoute from '../views/AIHeritageRoute.vue'
import ActivityList from '../views/activity/ActivityList.vue'
import ActivityDetail from '../views/activity/ActivityDetail.vue'
import ActivityBooking from '../views/activity/ActivityBooking.vue'

const readStoredUser = () => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return {}
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    console.error('Failed to read stored user info', error)
    return {}
  }
}

const isSuperAdminUser = (userInfo) => Number(userInfo?.isSuperAdmin || 0) === 1

const routes = [
  {
    path: '/login',
    name: 'login-page',
    component: WelcomeView,
    meta: { title: 'Login - YayFolk' }
  },
  {
    path: '/admin-login',
    name: 'admin-login',
    component: AdminLoginView,
    meta: { title: 'Admin Login - YayFolk' }
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/share',
    name: 'share',
    component: ShareView,
    meta: { title: 'Share - YayFolk' }
  },
  {
    path: '/home',
    component: HomeView,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'heritage',
        name: 'heritage',
        component: IntangibleCulturalHeritage,
        meta: { title: 'Heritage - YayFolk' }
      },
      {
        path: 'activity',
        name: 'activity',
        component: ActivityList,
        meta: { title: 'Activities - YayFolk' }
      },
      {
        path: 'tools',
        name: 'tools',
        component: AIHeritageRoute,
        meta: { title: 'Tools - YayFolk' }
      },
      {
        path: 'discover',
        name: 'discover',
        component: DiscoverView,
        meta: { title: 'Discover - YayFolk' }
      },
      {
        path: 'personal',
        name: 'personal',
        component: PersonalCenter,
        meta: { title: 'Personal Center - YayFolk' }
      }
    ]
  },
  {
    path: '/ai-heritage-route',
    redirect: '/home/tools'
  },
  {
    path: '/conversation',
    name: 'conversation',
    component: ConversationMode,
    meta: { title: 'Conversation - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/settings',
    name: 'settings',
    component: Settings,
    meta: { title: 'Settings - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/edit-profile',
    name: 'edit-profile',
    component: EditProfile,
    meta: { title: 'Edit Profile - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/edit-homepage',
    name: 'edit-homepage',
    redirect: '/merchant/apply'
  },
  {
    path: '/user-homepage/:userId',
    name: 'user-homepage',
    component: UserHomepage,
    meta: { title: 'User Homepage - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/my-posts',
    name: 'my-posts',
    component: () => import('../views/MyPosts.vue'),
    meta: { title: 'My Posts - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/my-collections',
    name: 'my-collections',
    component: () => import('../views/MyCollections.vue'),
    meta: { title: 'My Collections - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/activities',
    name: 'my-activities',
    component: () => import('../views/MyActivities.vue'),
    meta: { title: 'My Activities - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/checkins',
    name: 'my-activity-checkins',
    component: () => import('../views/MyActivityCheckins.vue'),
    meta: { title: 'Activity Check-ins - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/activities/:id/detail',
    name: 'activity-booking-detail',
    component: () => import('../views/activity/ActivityBookingOrderDetail.vue'),
    meta: { title: 'Activity Booking Detail - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/activities/:id/pay',
    name: 'activity-booking-payment',
    component: () => import('../views/activity/ActivityBookingPayment.vue'),
    meta: { title: 'Activity Booking Payment - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/activities/:id/checkin',
    name: 'activity-booking-checkin',
    component: () => import('../views/activity/ActivityBookingDetail.vue'),
    meta: { title: 'Activity Check-in - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/activities/:id/pay-result',
    name: 'activity-booking-pay-result',
    component: () => import('../views/activity/ActivityReservePayResult.vue'),
    meta: { title: 'Activity Payment Result - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/orders',
    name: 'my-orders',
    component: () => import('../views/MyOrders.vue'),
    meta: { title: 'My Orders - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/achievements',
    name: 'my-achievements',
    component: () => import('../views/MyAchievements.vue'),
    meta: { title: 'My Achievements - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/history',
    name: 'history',
    component: () => import('../views/History.vue'),
    meta: { title: 'History - YayFolk', requiresAuth: true }
  },
  {
    path: '/notification',
    name: 'notification',
    component: NotificationView,
    meta: { title: 'Notifications - YayFolk', requiresAuth: true }
  },
  {
    path: '/activity-notifications',
    name: 'activity-notifications',
    component: NotificationView,
    meta: { title: 'Activity Notifications - YayFolk', requiresAuth: true }
  },
  {
    path: '/activity/:id',
    name: 'activity-detail',
    component: ActivityDetail,
    meta: { title: 'Activity Detail - YayFolk', requiresAuth: true }
  },
  {
    path: '/activity/:id/booking',
    name: 'activity-booking',
    component: ActivityBooking,
    meta: { title: 'Activity Booking - YayFolk', requiresAuth: true }
  },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'admins',
        name: 'admin-admins',
        component: () => import('../views/admin/AdminAdmins.vue'),
        meta: { title: 'Admin Accounts - Admin Console', requiresSuperAdmin: true }
      },
      {
        path: 'merchants',
        name: 'admin-merchants',
        component: () => import('../views/admin/AdminMerchants.vue'),
        meta: { title: 'Merchant Review - Admin Console' }
      },
      {
        path: 'activities',
        name: 'admin-activities',
        component: () => import('../views/admin/AdminActivities.vue'),
        meta: { title: 'Activity Review - Admin Console' }
      },
      {
        path: 'posts',
        name: 'admin-posts',
        component: () => import('../views/admin/AdminPosts.vue'),
        meta: { title: 'Post Review - Admin Console' }
      },
      {
        path: 'service',
        name: 'admin-service',
        component: () => import('../views/admin/AdminCustomerService.vue'),
        meta: { title: 'Customer Service - Admin Console', normalAdminOnly: true }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('../views/admin/AdminUsers.vue'),
        meta: { title: 'User Management - Admin Console' }
      },
      {
        path: 'official',
        name: 'admin-official',
        component: () => import('../views/admin/AdminOfficial.vue'),
        meta: { title: 'Official Content - Admin Console' }
      }
    ]
  },
  {
    path: '/unban-application',
    name: 'unban-application',
    component: () => import('../views/UnbanApplicationView.vue'),
    meta: { title: 'Unban Application - YayFolk' }
  },
  {
    path: '/merchant',
    component: () => import('../views/merchant/MerchantLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'apply',
        name: 'merchant-apply',
        component: () => import('../views/merchant/MerchantApply.vue'),
        meta: { title: 'Merchant Application - Merchant Center' }
      },
      {
        path: 'activities',
        name: 'merchant-activities',
        component: () => import('../views/merchant/MerchantActivities.vue'),
        meta: { title: 'Merchant Activities - Merchant Center' }
      },
      {
        path: 'activities/create',
        name: 'merchant-activity-create',
        component: () => import('../views/activity/ActivityCreate.vue'),
        meta: { title: 'Create Activity - Merchant Center' }
      },
      {
        path: 'activities/:id/edit',
        name: 'merchant-activity-edit',
        component: () => import('../views/activity/ActivityCreate.vue'),
        meta: { title: 'Edit Activity - Merchant Center' }
      },
      {
        path: 'bookings',
        name: 'merchant-bookings',
        component: () => import('../views/merchant/MerchantBookings.vue'),
        meta: { title: 'Merchant Bookings - Merchant Center' }
      },
      {
        path: 'bookings/:id/detail',
        name: 'merchant-booking-detail',
        component: () => import('../views/merchant/MerchantBookingDetail.vue'),
        meta: { title: 'Merchant Booking Detail - Merchant Center' }
      },
      {
        path: 'bookings/scan',
        name: 'merchant-bookings-scan',
        component: () => import('../views/merchant/MerchantCheckinScanner.vue'),
        meta: { title: 'Booking Scanner - Merchant Center' }
      },
      {
        path: 'bookings/records',
        name: 'merchant-bookings-records',
        component: () => import('../views/merchant/MerchantBookings.vue'),
        meta: { title: 'Booking Records - Merchant Center' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || 'YayFolk'

  const isAuthenticated = localStorage.getItem('token')

  if (to.path === '/admin-login') {
    if (isAuthenticated) {
      const userInfo = readStoredUser()
      if (userInfo.role === 'admin') {
        next(isSuperAdminUser(userInfo) ? '/admin/admins' : '/admin/merchants')
        return
      }
    }
    next()
    return
  }

  if (to.path === '/login' && isAuthenticated) {
    const userInfo = readStoredUser()
    if (userInfo.role === 'admin') {
      next(isSuperAdminUser(userInfo) ? '/admin/admins' : '/admin/merchants')
      return
    }
  }

  if (to.meta.requiresAuth && !isAuthenticated) {
    if (to.path.startsWith('/admin')) {
      next('/admin-login')
      return
    }
    next('/')
    return
  }

  if (to.path.startsWith('/admin')) {
    const userInfo = readStoredUser()
    if (userInfo.role !== 'admin') {
      next('/home/heritage')
      return
    }
    if (to.matched.some(route => route.meta?.requiresSuperAdmin) && !isSuperAdminUser(userInfo)) {
      next('/admin/merchants')
      return
    }
    if (to.matched.some(route => route.meta?.normalAdminOnly) && isSuperAdminUser(userInfo)) {
      next('/admin/admins')
      return
    }
  }

  if (to.path.startsWith('/merchant') && to.name !== 'merchant-apply') {
    const userInfo = readStoredUser()
    if (userInfo.role !== 'merchant' && userInfo.role !== 'admin') {
      next('/merchant/apply')
      return
    }
  }

  next()
})

export default router
