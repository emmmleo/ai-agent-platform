<template>
  <div class="user-management">
    <div class="header">
      <h1>用户管理</h1>
      <div class="header-actions">
        <button @click="refreshUsers" :disabled="loading" class="refresh-btn">
          {{ loading ? '加载中...' : '刷新' }}
        </button>
        <router-link to="/" class="back-btn">返回首页</router-link>
      </div>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

    <div class="user-table-container">
      <table class="user-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>角色</th>
            <th>创建时间</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && users.length === 0">
            <td colspan="6" class="loading-cell">加载中...</td>
          </tr>
          <tr v-else-if="users.length === 0">
            <td colspan="6" class="empty-cell">暂无用户</td>
          </tr>
          <tr v-else v-for="user in users" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>
              <span :class="['role-badge', user.role === 'ROLE_ADMIN' ? 'admin' : 'user']">
                {{ user.role === 'ROLE_ADMIN' ? '管理员' : '普通用户' }}
              </span>
            </td>
            <td>{{ formatDate(user.createdAt) }}</td>
            <td>{{ formatDate(user.updatedAt) }}</td>
            <td>
              <button
                @click="handleDelete(user)"
                :disabled="deleting === user.username || user.username === currentUsername"
                class="delete-btn"
                :title="user.username === currentUsername ? '不能删除自己的账号' : ''"
              >
                {{ deleting === user.username ? '删除中...' : '删除' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="stats">
      <div class="stat-item">
        <span class="stat-label">总用户数：</span>
        <span class="stat-value">{{ users.length }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">管理员：</span>
        <span class="stat-value">{{ adminCount }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">普通用户：</span>
        <span class="stat-value">{{ userCount }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '../stores/user'
import { getAllUsers, deleteUser, type UserProfile } from '../api/admin'

const userStore = useUserStore()
const users = ref<UserProfile[]>([])
const loading = ref(false)
const deleting = ref<string | null>(null)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const currentUsername = computed(() => userStore.user?.username || '')

const adminCount = computed(() => {
  return users.value.filter((u: UserProfile) => u.role === 'ROLE_ADMIN').length
})

const userCount = computed(() => {
  return users.value.filter((u: UserProfile) => u.role === 'ROLE_USER').length
})

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  } catch (e) {
    return dateStr
  }
}

// 加载用户列表
const loadUsers = async () => {
  loading.value = true
  error.value = null
  successMessage.value = null
  try {
    users.value = await getAllUsers()
  } catch (e: any) {
    error.value = e.message || '加载用户列表失败'
    console.error('加载用户列表失败:', e)
  } finally {
    loading.value = false
  }
}

// 刷新用户列表
const refreshUsers = () => {
  loadUsers()
}

// 删除用户
const handleDelete = async (user: UserProfile) => {
  if (user.username === currentUsername.value) {
    error.value = '不能删除自己的账号'
    setTimeout(() => {
      error.value = null
    }, 3000)
    return
  }

  if (!confirm(`确定要删除用户 "${user.username}" 吗？\n\n此操作不可恢复，请谨慎操作！`)) {
    return
  }

  deleting.value = user.username
  error.value = null
  successMessage.value = null

  try {
    await deleteUser(user.username)
    successMessage.value = `用户 "${user.username}" 已成功删除`
    // 删除成功，刷新列表
    await loadUsers()
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
  } catch (e: any) {
    error.value = e.message || '删除用户失败'
    console.error('删除用户失败:', e)
    setTimeout(() => {
      error.value = null
    }, 5000)
  } finally {
    deleting.value = null
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management {
  padding: 40px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 80px);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  flex-wrap: wrap;
  gap: 15px;
}

h1 {
  color: #2c3e50;
  margin: 0;
  font-size: 28px;
}

.header-actions {
  display: flex;
  gap: 15px;
  align-items: center;
}

.refresh-btn,
.back-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  text-decoration: none;
  display: inline-block;
  transition: all 0.3s;
  text-align: center;
}

.refresh-btn {
  background: #42b983;
  color: white;
}

.refresh-btn:hover:not(:disabled) {
  background: #35a372;
}

.refresh-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.back-btn {
  background: #6c757d;
  color: white;
}

.back-btn:hover {
  background: #5a6268;
}

.error-message {
  background: #ffebee;
  color: #f44336;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #f44336;
}

.success-message {
  background: #e8f5e9;
  color: #4caf50;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #4caf50;
}

.user-table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow-x: auto;
  margin-bottom: 30px;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;
}

.user-table thead {
  background: #f8f9fa;
}

.user-table th {
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #dee2e6;
  white-space: nowrap;
}

.user-table td {
  padding: 15px;
  border-bottom: 1px solid #dee2e6;
  vertical-align: middle;
}

.user-table tbody tr:hover {
  background: #f8f9fa;
}

.loading-cell,
.empty-cell {
  text-align: center;
  color: #666;
  padding: 40px !important;
  font-size: 16px;
}

.role-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.role-badge.admin {
  background: #e3f2fd;
  color: #1976d2;
}

.role-badge.user {
  background: #f3e5f5;
  color: #7b1fa2;
}

.delete-btn {
  padding: 6px 12px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
}

.delete-btn:hover:not(:disabled) {
  background: #d32f2f;
}

.delete-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.stats {
  display: flex;
  gap: 30px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.stat-value {
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .user-management {
    padding: 20px;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .user-table {
    font-size: 14px;
  }

  .user-table th,
  .user-table td {
    padding: 10px 8px;
  }

  .stats {
    flex-direction: column;
    gap: 15px;
  }
}
</style>
