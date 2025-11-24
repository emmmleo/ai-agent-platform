<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500">
    <div class="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-md">
      <!-- Logo/Title -->
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-gray-800 mb-2">欢迎回来</h1>
        <p class="text-gray-500">登录您的账户</p>
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
        {{ error }}
      </div>

      <!-- 登录表单 -->
      <form @submit.prevent="handleLogin" class="space-y-6">
        <div>
          <label for="username" class="block text-sm font-medium text-gray-700 mb-2">
            用户名
          </label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            required
            class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
            placeholder="请输入用户名"
          />
        </div>

        <div>
          <label for="password" class="block text-sm font-medium text-gray-700 mb-2">
            密码
          </label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            required
            class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
            placeholder="请输入密码"
          />
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-gradient-to-r from-indigo-500 to-purple-600 text-white py-3 rounded-lg font-semibold hover:from-indigo-600 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 注册链接 -->
      <div class="mt-6 text-center">
        <p class="text-gray-600">
          还没有账户？
          <router-link to="/register" class="text-indigo-600 hover:text-indigo-700 font-semibold">
            立即注册
          </router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/utils/http'
import { setToken, setUserInfo } from '@/utils/auth'

const router = useRouter()

const form = ref({
  username: '',
  password: ''
})

const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  error.value = ''
  loading.value = true

  try {
    const response = await http.post('/auth/login', form.value)
    console.log('[LoginView] 完整响应:', response)
    console.log('[LoginView] response.data:', response.data)
    const data = response.data.data
    console.log('[LoginView] 解析的数据:', data)
    
    // 保存 token 和用户信息
    setToken(data.token)
    const userInfo = {
      userId: data.userId,
      username: data.username,
      role: data.role
    }
    console.log('[LoginView] 准备保存的用户信息:', userInfo)
    setUserInfo(userInfo)
    console.log('[LoginView] localStorage中的用户信息:', localStorage.getItem('user_info'))

    // 跳转到 home 页面
    router.push('/home')
  } catch (err) {
    console.error('[LoginView] 登录错误:', err)
    error.value = err.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

