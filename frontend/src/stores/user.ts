// 用户状态管理
import { defineStore } from 'pinia'
import { setToken, removeToken, getToken } from '../utils/request'
import { login as apiLogin, register as apiRegister } from '../api/auth'
import { getUserProfile } from '../api/user'

export interface User {
  id: number
  username: string
  role: string
  createdAt: string
  updatedAt: string
  school?: string
  phone?: string
  email?: string
  bio?: string
  avatarUrl?: string
  gender?: string
  birthday?: string
}

export const useUserStore = defineStore('user', {
  state: (): any => ({
    user: null as User | null,
    token: getToken(),
    isAuthenticated: !!getToken(),
  }),
  getters: {
    userName: (state) => state.user?.username || 'Guest',
    userRole: (state) => state.user?.role || '',
    isAdmin: (state) => state.user?.role === 'ROLE_ADMIN',
  },
  actions: {
    // 登录
    async login(username: string, password: string) {
      try {
        const response = await apiLogin({ username, password })
        setToken(response.token)
        const state = this as any
        state.token = response.token
        state.isAuthenticated = true
        // 获取用户信息
        await this.fetchUserProfile()
        return response
      } catch (error: any) {
        throw new Error(error.message || '登录失败')
      }
    },

    // 注册
    async register(username: string, password: string) {
      try {
        const user = await apiRegister({ username, password })
        const state = this as any
        state.user = user
        // 注册后自动登录
        await this.login(username, password)
        return user
      } catch (error: any) {
        throw new Error(error.message || '注册失败')
      }
    },

    // 获取用户信息
    async fetchUserProfile() {
      try {
        const user = await getUserProfile()
        const state = this as any
        state.user = user
        return user
      } catch (error: any) {
        this.logout()
        throw error
      }
    },

    // 设置用户信息
    setUser(user: User) {
      const state = this as any
      state.user = user
      state.isAuthenticated = true
    },

    // 登出
    logout() {
      const state = this as any
      state.user = null
      state.token = null
      state.isAuthenticated = false
      removeToken()
    },

    // 初始化（从 token 恢复登录状态）
    async init() {
      const state = this as any
      if (state.token) {
        try {
          await this.fetchUserProfile()
        } catch (error) {
          this.logout()
        }
      }
    },
  },
})
