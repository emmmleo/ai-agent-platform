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
}

export const useUserStore = defineStore('user', {
  state: () => ({
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
        this.token = response.token
        this.isAuthenticated = true
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
        this.user = user
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
        this.user = user
        return user
      } catch (error: any) {
        this.logout()
        throw error
      }
    },

    // 设置用户信息
    setUser(user: User) {
      this.user = user
      this.isAuthenticated = true
    },

    // 登出
    logout() {
      this.user = null
      this.token = null
      this.isAuthenticated = false
      removeToken()
    },

    // 初始化（从 token 恢复登录状态）
    async init() {
      if (this.token) {
        try {
          await this.fetchUserProfile()
        } catch (error) {
          this.logout()
        }
      }
    },
  },
})

