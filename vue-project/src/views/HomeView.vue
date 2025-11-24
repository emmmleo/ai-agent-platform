<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
    <!-- 导航栏 -->
    <nav class="bg-white shadow-md">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <div class="flex items-center">
            <h1 class="text-2xl font-bold text-indigo-600">应用中心</h1>
          </div>
          <div class="flex items-center space-x-4">
            <span class="text-gray-700">欢迎，{{ username }}</span>
            <button
              @click="handleLogout"
              class="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition"
            >
              退出登录
            </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- 主内容 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div class="text-center">
        <h2 class="text-6xl font-bold text-gray-800 mb-4">HOME</h2>
        <p class="text-xl text-gray-600 mb-8">欢迎来到首页</p>
        
        <!-- 功能卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mt-12">
          <router-link
            to="/apps"
            class="bg-white rounded-xl shadow-lg p-8 hover:shadow-xl transition transform hover:-translate-y-1"
          >
            <div class="text-4xl mb-4">📱</div>
            <h3 class="text-xl font-semibold text-gray-800 mb-2">应用列表</h3>
            <p class="text-gray-600">浏览所有可用的应用</p>
          </router-link>

          <router-link
            to="/profile"
            class="bg-white rounded-xl shadow-lg p-8 hover:shadow-xl transition transform hover:-translate-y-1"
          >
            <div class="text-4xl mb-4">👤</div>
            <h3 class="text-xl font-semibold text-gray-800 mb-2">个人信息</h3>
            <p class="text-gray-600">查看和编辑您的资料</p>
          </router-link>

          <div class="bg-white rounded-xl shadow-lg p-8">
            <div class="text-4xl mb-4">⚙️</div>
            <h3 class="text-xl font-semibold text-gray-800 mb-2">设置</h3>
            <p class="text-gray-600">管理您的偏好设置</p>
          </div>

          <!-- 管理员专用功能卡片 -->
          <router-link
            v-if="isAdminUser"
            to="/admin/users"
            class="bg-gradient-to-br from-purple-500 to-indigo-600 text-white rounded-xl shadow-lg p-8 hover:shadow-xl transition transform hover:-translate-y-1"
          >
            <div class="text-4xl mb-4">👥</div>
            <h3 class="text-xl font-semibold mb-2">管理用户</h3>
            <p class="text-purple-100">查看和管理所有用户</p>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, clearAuth, isAdmin } from '@/utils/auth'

const router = useRouter()
const username = ref('')
const isAdminUser = ref(false)

onMounted(() => {
  console.log('[HomeView] 组件加载')
  const userInfo = getUserInfo()
  console.log('[HomeView] 获取到的用户信息:', userInfo)
  if (userInfo) {
    username.value = userInfo.username
    isAdminUser.value = isAdmin()
    console.log('[HomeView] isAdminUser设置为:', isAdminUser.value)
  } else {
    console.log('[HomeView] 未获取到用户信息')
  }
})

const handleLogout = () => {
  clearAuth()
  router.push('/login')
}
</script>
