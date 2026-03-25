import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import ToolsView from '../views/Tools.vue'
import TextTranslate from '../views/TextTranslate.vue'
import DiscoverView from '../views/DiscoverView.vue'
import PersonalCenter from '../views/PersonalCenter.vue'
import Settings from '../views/Settings.vue'
import EditProfile from '../views/EditProfile.vue'
import CameraTranslate from '../views/CameraTranslate.vue'
import VoiceTranslate from '../views/VoiceTranslate.vue'
import ConversationMode from '../views/ConversationMode.vue'
import NotificationView from '../views/NotificationView.vue'
import ShareView from '../views/ShareView.vue'
import IntangibleCulturalHeritage from '../views/IntangibleCulturalHeritage.vue'

const routes = [
  {
    path: '/login',
    name: 'login-page',
    component: LoginView,
    meta: { title: '登录 - YayFolk' }
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
        component: () => import('../views/IntangibleCulturalHeritage.vue'),
        meta: { title: '中国非遗文化 - YayFolk' }
      },
      {
        path: 'activity',
        name: 'activity',
        component: () => import('../views/ActivityView.vue'),
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
    meta: { title: '浏览历史 - YayFolk', requiresAuth: true }
  },
  {
    path: '/notification',
    name: 'notification',
    component: NotificationView,
    meta: { title: '消息通知 - YayFolk', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title || 'YayFolk - 非遗文化传承平台'
  
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
