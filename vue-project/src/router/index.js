import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, isAdmin, getUserInfo, clearAuth } from '@/utils/auth'

const Apps = () => import('@/views/AppsView.vue')
const Login = () => import('@/views/LoginView.vue')
const Register = () => import('@/views/RegisterView.vue')
const Home = () => import('@/views/HomeView.vue')
const UserManagement = () => import('@/views/UserManagementView.vue')
const Profile = () => import('@/views/UserProfileView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/login', name: 'login', component: Login, meta: { guest: true } },
    { path: '/register', name: 'register', component: Register, meta: { guest: true } },
    { path: '/home', name: 'home', component: Home, meta: { requiresAuth: true } },
    { path: '/apps', name: 'apps', component: Apps, meta: { requiresAuth: true } },
    { path: '/profile', name: 'profile', component: Profile, meta: { requiresAuth: true } },
    { path: '/admin/users', name: 'userManagement', component: UserManagement, meta: { requiresAuth: true, requiresAdmin: true } },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  console.log('[路由守卫] 从', from.path, '跳转到', to.path)
  console.log('[路由守卫] 路由元信息:', to.meta)
  
  const authenticated = isAuthenticated()
  console.log('[路由守卫] 是否已认证:', authenticated)

  // 需要认证的页面
  if (to.meta.requiresAuth) {
    if (!authenticated) {
      console.log('[路由守卫] 需要认证但未认证，重定向到登录页')
      next('/login')
      return
    }
    
    // 验证用户信息完整性
    const userInfo = getUserInfo()
    if (!userInfo) {
      console.error('[路由守卫] 用户信息不完整，清除认证并重定向到登录页')
      clearAuth()
      next('/login')
      return
    }
  }

  // 需要管理员权限的页面
  if (to.meta.requiresAdmin) {
    console.log('[路由守卫] 需要管理员权限')
    const adminCheck = isAdmin()
    console.log('[路由守卫] 管理员检查结果:', adminCheck)
    if (!adminCheck) {
      console.log('[路由守卫] 无管理员权限，拦截访问')
      alert('无权限访问该页面')
      next('/home')
      return
    }
    console.log('[路由守卫] 管理员权限验证通过')
  }

  // 已登录用户访问登录/注册页面，重定向到首页
  if (to.meta.guest && authenticated) {
    console.log('[路由守卫] 已登录用户访问游客页面，重定向到首页')
    next('/home')
    return
  }

  console.log('[路由守卫] 放行')
  next()
})

export default router
