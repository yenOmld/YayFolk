import { createRouter, createWebHistory } from 'vue-router'
import WelcomeView from '../views/WelcomeView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import HomeView from '../views/HomeView.vue'
import ToolsView from '../views/Tools.vue'
import TextTranslate from '../views/TextTranslate.vue'
import DiscoverView from '../views/DiscoverView.vue'
import PersonalCenter from '../views/PersonalCenter.vue'
import Settings from '../views/Settings.vue'
import EditProfile from '../views/EditProfile.vue'
import EditHomepage from '../views/EditHomepage.vue'
import UserHomepage from '../views/UserHomepage.vue'
import CameraTranslate from '../views/CameraTranslate.vue'
import VoiceTranslate from '../views/VoiceTranslate.vue'
import ConversationMode from '../views/ConversationMode.vue'
import NotificationView from '../views/NotificationView.vue'
import ShareView from '../views/ShareView.vue'
import IntangibleCulturalHeritage from '../views/IntangibleCulturalHeritage.vue'
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
    console.error('读取用户信息失败', error)
    return {}
  }
}

const isSuperAdminUser = (userInfo) => Number(userInfo?.isSuperAdmin || 0) === 1

const routes = [
  {
    path: '/login',
    name: 'login-page',
    component: WelcomeView,
    meta: { title: '登录 - YayFolk' }
  },
  {
    path: '/admin-login',
    name: 'admin-login',
    component: AdminLoginView,
    meta: { title: '管理员登录 - YayFolk' }
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/share',
    name: 'share',
    component: ShareView,
    meta: { title: '分享 - YayFolk' }
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
        meta: { title: '中国非遗文化 - YayFolk' }
      },
      {
        path: 'activity',
        name: 'activity',
        component: ActivityList,
        meta: { title: '精彩活动 - YayFolk' }
      },
      {
        path: 'tools',
        name: 'tools',
        component: ToolsView,
        meta: { title: '工具箱 - YayFolk' }
      },
      {
        path: 'discover',
        name: 'discover',
        component: DiscoverView,
        meta: { title: '发现 - YayFolk' }
      },
      {
        path: 'personal',
        name: 'personal',
        component: PersonalCenter,
        meta: { title: '个人中心 - YayFolk' }
      }
    ]
  },
  {
    path: '/text-translate',
    name: 'text-translate',
    component: TextTranslate,
    meta: { title: '文本翻译 - YayFolk', requiresAuth: true }
  },
  {
    path: '/camera-translate',
    name: 'camera-translate',
    component: CameraTranslate,
    meta: { title: '拍照翻译 - YayFolk', requiresAuth: true }
  },
  {
    path: '/voice-translate',
    name: 'voice-translate',
    component: VoiceTranslate,
    meta: { title: '语音翻译 - YayFolk', requiresAuth: true }
  },
  {
    path: '/ai-heritage-route',
    name: 'ai-heritage-route',
    component: () => import('../views/AIHeritageRoute.vue'),
    meta: { title: 'AI 非遗体验路线规划 - YayFolk', requiresAuth: true }
  },
  {
    path: '/conversation',
    name: 'conversation',
    component: ConversationMode,
    meta: { title: '对话模式 - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/settings',
    name: 'settings',
    component: Settings,
    meta: { title: '设置 - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/edit-profile',
    name: 'edit-profile',
    component: EditProfile,
    meta: { title: '编辑资料 - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/edit-homepage',
    name: 'edit-homepage',
    component: EditHomepage,
    meta: { title: 'YayFolk 编辑个人主页', requiresAuth: true }
  },
  {
    path: '/user-homepage/:userId',
    name: 'user-homepage',
    component: UserHomepage,
    meta: { title: 'YayFolk 用户主页', requiresAuth: true }
  },
  {
    path: '/personal/my-posts',
    name: 'my-posts',
    component: () => import('../views/MyPosts.vue'),
    meta: { title: '我的发布 - Travelate', requiresAuth: true }
  },
  {
    path: '/personal/my-collections',
    name: 'my-collections',
    component: () => import('../views/MyCollections.vue'),
    meta: { title: '我的收藏 - Travelate', requiresAuth: true }
  },
  {
    path: '/personal/activities',
    name: 'my-activities',
    component: () => import('../views/MyActivities.vue'),
    meta: { title: '我的活动 - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/achievements',
    name: 'my-achievements',
    component: () => import('../views/MyAchievements.vue'),
    meta: { title: '打卡成就 - YayFolk', requiresAuth: true }
  },
  {
    path: '/personal/history',
    name: 'history',
    component: () => import('../views/History.vue'),
    meta: { title: '浏览历史 - YayFolk', requiresAuth: true }
  },
  {
    path: '/notification',
    name: 'notification',
    component: NotificationView,
    meta: { title: '消息通知 - YayFolk', requiresAuth: true }
  },
  {
    path: '/activity/:id',
    name: 'activity-detail',
    component: ActivityDetail,
    meta: { title: '活动详情 - YayFolk', requiresAuth: true }
  },
  {
    path: '/activity/:id/booking',
    name: 'activity-booking',
    component: ActivityBooking,
    meta: { title: '娲诲姩鎶ュ悕 - YayFolk', requiresAuth: true }
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
        meta: { title: '管理员管理 - 管理后台', requiresSuperAdmin: true }
      },
      {
        path: 'merchants',
        name: 'admin-merchants',
        component: () => import('../views/admin/AdminMerchants.vue'),
        meta: { title: '商家审核 - 管理后台' }
      },
      {
        path: 'activities',
        name: 'admin-activities',
        component: () => import('../views/admin/AdminActivities.vue'),
        meta: { title: '活动审核 - 管理后台' }
      },
      {
        path: 'posts',
        name: 'admin-posts',
        component: () => import('../views/admin/AdminPosts.vue'),
        meta: { title: '内容审核 - 管理后台' }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('../views/admin/AdminUsers.vue'),
        meta: { title: '用户管理 - 管理后台' }
      },
      {
        path: 'official',
        name: 'admin-official',
        component: () => import('../views/admin/AdminOfficial.vue'),
        meta: { title: '官方内容 - 管理后台' }
      }
    ]
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
        meta: { title: '商家认证 - 商家中心' }
      },
      {
        path: 'activities',
        name: 'merchant-activities',
        component: () => import('../views/merchant/MerchantActivities.vue'),
        meta: { title: '活动管理 - 商家中心' }
      },
      {
        path: 'activities/create',
        name: 'merchant-activity-create',
        component: () => import('../views/activity/ActivityCreate.vue'),
        meta: { title: '鍒涘缓娲诲姩 - 鍟嗗涓績' }
      },
      {
        path: 'activities/:id/edit',
        name: 'merchant-activity-edit',
        component: () => import('../views/activity/ActivityCreate.vue'),
        meta: { title: '缂栬緫娲诲姩 - 鍟嗗涓績' }
      },
      {
        path: 'bookings',
        name: 'merchant-bookings',
        component: () => import('../views/merchant/MerchantBookings.vue'),
        meta: { title: '预约管理 - 商家中心' }
      },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || 'YayFolk - 非遗文化传承平台'

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
