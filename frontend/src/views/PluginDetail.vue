<template>
  <div class="plugin-detail">
    <div class="header">
      <router-link to="/plugins" class="back-link">← 返回列表</router-link>
      <h1 v-if="plugin">{{ plugin.name }}</h1>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="plugin" class="content">
      <div class="info-section">
        <h2>插件信息</h2>
        <div class="info-item">
          <strong>状态：</strong>
          <span :class="['status-badge', plugin.enabled ? 'enabled' : 'disabled']">
            {{ plugin.enabled ? '已启用' : '已禁用' }}
          </span>
          <button
            @click="handleToggle"
            :class="['toggle-btn', plugin.enabled ? 'disable' : 'enable']"
          >
            {{ plugin.enabled ? '禁用' : '启用' }}
          </button>
        </div>
        <p><strong>描述：</strong>{{ plugin.description || '暂无描述' }}</p>
        <p><strong>创建时间：</strong>{{ formatDate(plugin.createdAt) }}</p>
        <p><strong>更新时间：</strong>{{ formatDate(plugin.updatedAt) }}</p>
      </div>

      <div class="openapi-section">
        <h2>OpenAPI规范</h2>
        <div v-if="plugin.openapiSpec" class="openapi-content">
          <pre class="json-viewer">{{ formatJSON(plugin.openapiSpec) }}</pre>
        </div>
        <div v-else class="empty-state">暂无OpenAPI规范</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  getPlugin,
  togglePlugin,
  type Plugin,
} from '../api/plugin'

const router = useRouter()
const route = useRoute()

const pluginId = Number(route.params.id)
const plugin = ref<Plugin | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// 加载插件详情
const loadPlugin = async () => {
  loading.value = true
  error.value = null
  try {
    const p = await getPlugin(pluginId)
    plugin.value = p
  } catch (e: any) {
    error.value = e.message || '加载插件详情失败'
    console.error('加载插件详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 启用/禁用
const handleToggle = async () => {
  if (!plugin.value) return

  try {
    const updated = await togglePlugin(plugin.value.id, !plugin.value.enabled)
    plugin.value.enabled = updated.enabled
    successMessage.value = updated.enabled ? '插件已启用' : '插件已禁用'
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
  } catch (e: any) {
    error.value = e.message || '切换插件状态失败'
    console.error('切换插件状态失败:', e)
  }
}

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

// 格式化JSON
const formatJSON = (obj: any) => {
  try {
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return String(obj)
  }
}

onMounted(() => {
  loadPlugin()
})
</script>

<style scoped>
.plugin-detail {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 80px);
}

.header {
  margin-bottom: 30px;
}

.back-link {
  color: #42b983;
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 10px;
  display: inline-block;
}

.back-link:hover {
  text-decoration: underline;
}

h1 {
  color: #2c3e50;
  margin: 10px 0 0 0;
  font-size: 28px;
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

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.info-section,
.openapi-section {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

h2 {
  color: #2c3e50;
  margin: 0 0 20px 0;
  font-size: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
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

.toggle-btn {
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

.info-section p {
  color: #666;
  margin-bottom: 10px;
  line-height: 1.6;
}

.openapi-content {
  max-height: 600px;
  overflow: auto;
}

.json-viewer {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #2c3e50;
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

@media (max-width: 768px) {
  .plugin-detail {
    padding: 20px;
  }

  .info-section,
  .openapi-section {
    padding: 20px;
  }

  .json-viewer {
    font-size: 12px;
    padding: 15px;
  }
}
</style>

