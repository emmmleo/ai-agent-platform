// 路由配置
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '../stores/user'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
  }
}

const routes: RouteRecordRaw[] = [
    {
      path: '/',
      name: 'Home',
      component: () => import('../views/Home.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/Register.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/account/profile',
      name: 'AccountProfile',
      component: () => import('../views/AccountProfile.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/users',
      name: 'UserManagement',
      component: () => import('../views/UserManagement.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/agents',
      name: 'AgentList',
      component: () => import('../views/AgentList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/agents/new',
      name: 'AgentCreate',
      component: () => import('../views/AgentForm.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/agents/:id/edit',
      name: 'AgentEdit',
      component: () => import('../views/AgentForm.vue'),
      meta: { requiresAuth: true },
    },
          {
             path: '/agents/:id/test',
             name: 'AgentTest',
             component: () => import('../views/AgentTest.vue'),
             meta: { requiresAuth: true },
           },
          {
             path: '/agents/:id/chat',
             name: 'AgentChat',
             component: () => import('../views/AgentChat.vue'),
             meta: { requiresAuth: true },
           },
           {
             path: '/knowledge-bases',
             name: 'KnowledgeBaseList',
             component: () => import('../views/KnowledgeBaseList.vue'),
             meta: { requiresAuth: true },
           },
           {
             path: '/knowledge-bases/new',
             name: 'KnowledgeBaseCreate',
             component: () => import('../views/KnowledgeBaseForm.vue'),
             meta: { requiresAuth: true },
           },
           {
             path: '/knowledge-bases/:id',
             name: 'KnowledgeBaseDetail',
             component: () => import('../views/KnowledgeBaseDetail.vue'),
             meta: { requiresAuth: true },
           },
           {
             path: '/workflows',
             name: 'WorkflowList',
             component: () => import('../views/WorkflowList.vue'),
             meta: { requiresAuth: true },
           },
           {
             path: '/workflows/new',
             name: 'WorkflowCreate',
             component: () => import('../views/WorkflowEditor.vue'),
             meta: { requiresAuth: true },
           },
           {
             path: '/workflows/:id/edit',
             name: 'WorkflowEdit',
             component: () => import('../views/WorkflowEditor.vue'),
             meta: { requiresAuth: true },
           },
           {
             path: '/workflows/:id/execute',
             name: 'WorkflowExecute',
             component: () => import('../views/WorkflowExecute.vue'),
             meta: { requiresAuth: true },
           },
           {
             path: '/plugins',
             name: 'PluginList',
             component: () => import('../views/PluginList.vue'),
             meta: { requiresAuth: true },
           },
           {
             path: '/plugins/new',
             name: 'PluginRegister',
             component: () => import('../views/PluginRegister.vue'),
             meta: { requiresAuth: true },
           },
           {
             path: '/plugins/:id',
             name: 'PluginDetail',
             component: () => import('../views/PluginDetail.vue'),
             meta: { requiresAuth: true },
           },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    // 如果未登录，跳转到登录页
    if (!userStore.isAuthenticated) {
      next('/login')
      return
    }
    // 如果已登录但用户信息未加载，尝试加载
    if (!userStore.user && userStore.token) {
      try {
        await userStore.fetchUserProfile()
      } catch (error) {
        // 加载失败，清除 token 并跳转到登录页
        userStore.logout()
        next('/login')
        return
      }
    }
    // 如果路由需要管理员权限
    if (to.meta.requiresAdmin && !userStore.isAdmin) {
      // 非管理员，跳转到首页
      next('/')
      return
    }
  } else {
    // 如果已登录，访问登录/注册页时跳转到首页
    if (userStore.isAuthenticated && (to.path === '/login' || to.path === '/register')) {
      next('/')
      return
    }
  }

  next()
})

export default router
