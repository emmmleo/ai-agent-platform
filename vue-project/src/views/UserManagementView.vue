<template>
  <div class="min-h-screen bg-gradient-to-br from-purple-50 to-indigo-100">
    <!-- 导航栏 -->
    <nav class="bg-white shadow-md">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <div class="flex items-center space-x-4">
            <router-link to="/home" class="text-gray-600 hover:text-indigo-600 transition">
              ← 返回首页
            </router-link>
            <h1 class="text-2xl font-bold text-indigo-600">用户管理</h1>
          </div>
          <div class="flex items-center space-x-4">
            <span class="text-gray-700">管理员：{{ username }}</span>
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
      <div class="bg-white rounded-xl shadow-lg overflow-hidden">
        <div class="px-6 py-4 bg-gradient-to-r from-purple-500 to-indigo-600">
          <h2 class="text-2xl font-bold text-white">普通用户列表</h2>
          <p class="text-purple-100 mt-1">管理系统中的所有普通用户</p>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="p-8 text-center">
          <div class="inline-block animate-spin rounded-full h-12 w-12 border-4 border-indigo-500 border-t-transparent"></div>
          <p class="mt-4 text-gray-600">加载中...</p>
        </div>

        <!-- 用户列表 -->
        <div v-else-if="users.length > 0" class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50 border-b-2 border-gray-200">
              <tr>
                <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  用户ID
                </th>
                <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  用户名
                </th>
                <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  注册时间
                </th>
                <th class="px-6 py-4 text-center text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  操作
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="user in users" :key="user.id" class="hover:bg-gray-50 transition">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ user.id }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="flex items-center">
                    <div class="flex-shrink-0 h-10 w-10 bg-indigo-100 rounded-full flex items-center justify-center">
                      <span class="text-indigo-600 font-semibold">{{ user.username.charAt(0).toUpperCase() }}</span>
                    </div>
                    <div class="ml-4">
                      <div class="text-sm font-medium text-gray-900">{{ user.username }}</div>
                    </div>
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{ formatDate(user.createdAt) }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-center">
                  <button
                    @click="confirmDelete(user)"
                    class="px-4 py-2 bg-red-500 text-white text-sm rounded-lg hover:bg-red-600 transition transform hover:scale-105"
                  >
                    删除
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 空状态 -->
        <div v-else class="p-12 text-center">
          <div class="text-6xl mb-4">👥</div>
          <p class="text-xl text-gray-600">暂无普通用户</p>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div
      v-if="showDeleteDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="showDeleteDialog = false"
    >
      <div class="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full mx-4 transform transition-all">
        <div class="text-center">
          <div class="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-red-100 mb-4">
            <span class="text-3xl">⚠️</span>
          </div>
          <h3 class="text-2xl font-bold text-gray-900 mb-2">确认删除</h3>
          <p class="text-gray-600 mb-6">
            确定要删除用户 <span class="font-semibold text-red-600">{{ userToDelete?.username }}</span> 吗？
            <br />此操作不可恢复！
          </p>
          <div class="flex space-x-4">
            <button
              @click="showDeleteDialog = false"
              class="flex-1 px-4 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition font-medium"
            >
              取消
            </button>
            <button
              @click="deleteUser"
              :disabled="deleting"
              class="flex-1 px-4 py-3 bg-red-500 text-white rounded-lg hover:bg-red-600 transition font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ deleting ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, clearAuth } from '@/utils/auth'
import { http } from '@/utils/http'

const router = useRouter()
const username = ref('')
const users = ref([])
const loading = ref(true)
const showDeleteDialog = ref(false)
const userToDelete = ref(null)
const deleting = ref(false)

onMounted(async () => {
  const userInfo = getUserInfo()
  if (userInfo) {
    username.value = userInfo.username
  }
  await loadUsers()
})

const loadUsers = async () => {
  try {
    loading.value = true
    const response = await http.get('/admin/users')
    if (response.data.code === 0) {
      users.value = response.data.data
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    alert('加载用户列表失败，请重试')
  } finally {
    loading.value = false
  }
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const confirmDelete = (user) => {
  userToDelete.value = user
  showDeleteDialog.value = true
}

const deleteUser = async () => {
  if (!userToDelete.value) return
  
  try {
    deleting.value = true
    const response = await http.delete(`/admin/users/${userToDelete.value.id}`)
    if (response.data.code === 0) {
      // 从列表中移除已删除的用户
      users.value = users.value.filter(u => u.id !== userToDelete.value.id)
      showDeleteDialog.value = false
      userToDelete.value = null
    } else {
      alert(response.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除用户失败:', error)
    alert(error.response?.data?.message || '删除用户失败，请重试')
  } finally {
    deleting.value = false
  }
}

const handleLogout = () => {
  clearAuth()
  router.push('/login')
}
</script>

