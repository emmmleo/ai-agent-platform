<template>
  <div class="plugin-list">
    <div class="header">
      <h1>插件管理</h1>
      <router-link to="/plugins/new" class="create-btn">注册插件</router-link>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="plugins.length === 0" class="empty-state">
      <p>还没有插件，<router-link to="/plugins/new">注册一个</router-link></p>
    </div>
    <div v-else class="plugin-grid">
      <div
        v-for="plugin in plugins"
        :key="plugin.id"
        class="plugin-card"
        @click="handleViewDetail(plugin.id)"
      >
        <div class="card-header">
          <h3>{{ plugin.name }}</h3>
          <span :class="['status-badge', plugin.enabled ? 'enabled' : 'disabled']">
            {{ plugin.enabled ? '已启用' : '已禁用' }}
          </span>
        </div>
        <p class="card-description">{{ plugin.description || '暂无描述' }}</p>
        <div class="card-footer">
          <span class="card-date">创建时间：{{ formatDate(plugin.createdAt) }}</span>
          <div class="card-actions" @click.stop>
            <button
              @click.stop="handleToggle(plugin)"
              :class="['toggle-btn', plugin.enabled ? 'disable' : 'enable']"
            >
              {{ plugin.enabled ? '禁用' : '启用' }}
            </button>
            <button @click.stop="handleEdit(plugin)" class="edit-btn">编辑</button>
            <button @click.stop="handleDelete(plugin)" class="delete-btn">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getPlugins,
  togglePlugin,
  deletePlugin,
  type Plugin,
} from '../api/plugin'

const router = useRouter()
const plugins = ref<Plugin[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// 加载插件列表
const loadPlugins = async () => {
  loading.value = true
  error.value = null
  try {
    const list = await getPlugins()
    plugins.value = list
  } catch (e: any) {
    error.value = e.message || '加载插件列表失败'
    console.error('加载插件列表失败:', e)
  } finally {
    loading.value = false
  }
}

// 查看详情
const handleViewDetail = (id: number) => {
  router.push(`/plugins/${id}`)
}

// 编辑
const handleEdit = (plugin: Plugin) => {
  router.push(`/plugins/${plugin.id}/edit`)
}

// 启用/禁用
const handleToggle = async (plugin: Plugin) => {
  try {
    await togglePlugin(plugin.id, !plugin.enabled)
    successMessage.value = plugin.enabled ? '插件已禁用' : '插件已启用'
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
    loadPlugins()
  } catch (e: any) {
    error.value = e.message || '切换插件状态失败'
    console.error('切换插件状态失败:', e)
  }
}

// 删除
const handleDelete = async (plugin: Plugin) => {
  if (!confirm(`确定要删除插件"${plugin.name}"吗？`)) {
    return
  }

  try {
    await deletePlugin(plugin.id)
    successMessage.value = '删除成功'
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
    loadPlugins()
  } catch (e: any) {
    error.value = e.message || '删除失败'
    console.error('删除插件失败:', e)
  }
}

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadPlugins()
})
</script>

<style scoped>
.plugin-list {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 80px);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

h1 {
  color: #2c3e50;
  margin: 0;
  font-size: 28px;
}

.create-btn {
  padding: 12px 24px;
  background: #42b983;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-size: 16px;
  transition: background 0.3s;
}

.create-btn:hover {
  background: #35a372;
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

.loading,
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.empty-state a {
  color: #42b983;
  text-decoration: none;
}

.empty-state a:hover {
  text-decoration: underline;
}

.plugin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.plugin-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.plugin-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-header h3 {
  color: #2c3e50;
  margin: 0;
  font-size: 20px;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.enabled {
  background: #e8f5e9;
  color: #4caf50;
}

.status-badge.disabled {
  background: #ffebee;
  color: #f44336;
}

.card-description {
  color: #666;
  margin-bottom: 15px;
  line-height: 1.6;
  min-height: 48px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.card-date {
  font-size: 12px;
  color: #999;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.toggle-btn,
.edit-btn,
.delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.toggle-btn.enable {
  background: #4caf50;
  color: white;
}

.toggle-btn.enable:hover {
  background: #45a049;
}

.toggle-btn.disable {
  background: #f57c00;
  color: white;
}

.toggle-btn.disable:hover {
  background: #e65100;
}

.edit-btn {
  background: #42b983;
  color: white;
}

.edit-btn:hover {
  background: #35a372;
}

.delete-btn {
  background: #f44336;
  color: white;
}

.delete-btn:hover {
  background: #d32f2f;
}

@media (max-width: 768px) {
  .plugin-list {
    padding: 20px;
  }

  .header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .plugin-grid {
    grid-template-columns: 1fr;
  }
}
</style>

