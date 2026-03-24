import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import TranslateView from '../views/TranslateView.vue'
import DiscoverView from '../views/DiscoverView.vue'
import PersonalCenter from '../views/PersonalCenter.vue'
import Settings from '../views/Settings.vue'
import EditProfile from '../views/EditProfile.vue'
import CameraTranslate from '../views/CameraTranslate.vue'
import VoiceTranslate from '../views/VoiceTranslate.vue'
import ConversationMode from '../views/ConversationMode.vue'
import NotificationView from '../views/NotificationView.vue'
import ShareView from '../views/ShareView.vue'

const routes = [
  {
    path: '/login',
    name: 'login-page',
    component: LoginView,
    meta: { title: '登录 - Travelate' }
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/share',
    name: 'share',
    component: ShareView,
    meta: { title: '分享 - Travelate' }
  },
  {
    path: '/home',
    component: HomeView,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'translate',
        name: 'translate',
        component: TranslateView,
        meta: { title: '首页 - Travelate' }
      },
      {
        path: 'discover',
        name: 'discover',
        component: DiscoverView,
        meta: { title: '发现 - Travelate' }
      },
      {
        path: 'personal',
        name: 'personal',
        component: PersonalCenter,
        meta: { title: '个人中心 - Travelate' }
      }
    ]
  },
  {
    path: '/landmark-recognition',
    name: 'landmark-recognition',
    component: () => import('../views/LandmarkRecognition.vue'),
    meta: { title: '地标识别 - Travelate', requiresAuth: true }
  },
  {
    path: '/camera-translate',
    name: 'camera-translate',
    component: CameraTranslate,
    meta: { title: '拍照翻译 - Travelate', requiresAuth: true }
  },
  {
    path: '/voice-translate',
    name: 'voice-translate',
    component: VoiceTranslate,
    meta: { title: '语音翻译 - Travelate', requiresAuth: true }
  },
  {
    path: '/conversation',
    name: 'conversation',
    component: ConversationMode,
    meta: { title: '对话模式 - Travelate', requiresAuth: true }
  },
  {
    path: '/personal/settings',
    name: 'settings',
    component: Settings,
    meta: { title: '设置 - Travelate', requiresAuth: true }
  },
  {
    path: '/personal/edit-profile',
    name: 'edit-profile',
    component: EditProfile,
    meta: { title: '编辑资料 - Travelate', requiresAuth: true }
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
    path: '/personal/history',
    name: 'history',
    component: () => import('../views/History.vue'),
    meta: { title: '浏览历史 - Travelate', requiresAuth: true }
  },
  {
    path: '/notification',
    name: 'notification',
    component: NotificationView,
    meta: { title: '消息通知 - Travelate', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title || 'Travelate - 国际旅行翻译助手'
  
  // 检查是否需要登录
  const isAuthenticated = localStorage.getItem('token')
  if (to.meta.requiresAuth && !isAuthenticated) {
    // 未登录，跳转到登录页
    next('/')
  } else {
    next()
  }
})

export default router
