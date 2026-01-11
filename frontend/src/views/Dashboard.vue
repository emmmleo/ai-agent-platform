<template>
  <div class="dashboard">
    <div class="header">
      <div class="header-content">
        <h1>仪表盘</h1>
        <p class="subtitle">系统概览与统计</p>
      </div>
      <button class="back-btn" @click="router.push('/')">返回菜单</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else class="stats-grid">
      <div class="stat-card agent-card" @click="router.push('/agents')">
        <div class="stat-icon">🤖</div>
        <div class="stat-content">
          <h3>智能体</h3>
          <div class="stat-value">{{ stats.agentCount }}</div>
          <p class="stat-desc">已创建的智能体数量</p>
        </div>
      </div>

      <div class="stat-card kb-card" @click="router.push('/knowledge-bases')">
        <div class="stat-icon">📚</div>
        <div class="stat-content">
          <h3>知识库</h3>
          <div class="stat-value">{{ stats.knowledgeBaseCount }}</div>
          <p class="stat-desc">已创建的知识库数量</p>
        </div>
      </div>

      <div class="stat-card workflow-card" @click="router.push('/workflows')">
        <div class="stat-icon">⚙️</div>
        <div class="stat-content">
          <h3>工作流</h3>
          <div class="stat-value">{{ stats.workflowCount }}</div>
          <p class="stat-desc">已创建的工作流数量</p>
        </div>
      </div>

      <div class="stat-card plugin-card" @click="router.push('/plugins')">
        <div class="stat-icon">🔌</div>
        <div class="stat-content">
          <h3>插件</h3>
          <div class="stat-value">{{ stats.pluginCount }}</div>
          <p class="stat-desc">已注册的插件数量</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardStats, type DashboardStats } from '../api/dashboard'

const router = useRouter()
const loading = ref(false)
const error = ref<string | null>(null)
const stats = ref<DashboardStats>({
  agentCount: 0,
  knowledgeBaseCount: 0,
  workflowCount: 0,
  pluginCount: 0,
})

const loadStats = async () => {
  loading.value = true
  error.value = null
  try {
    const data = await getDashboardStats()
    stats.value = data
  } catch (e: any) {
    error.value = e.message || '加载统计数据失败'
    console.error('加载统计数据失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.dashboard {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  margin-bottom: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h1 {
  color: #2c3e50;
  margin: 0 0 10px 0;
  font-size: 28px;
}

.back-btn {
  padding: 10px 20px;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.back-btn:hover {
  background-color: #f8f9fa;
  color: #333;
  border-color: #d0d0d0;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.back-btn:active {
  transform: translateY(0);
}

.subtitle {
  color: #666;
  margin: 0;
  font-size: 16px;
}

.loading,
.error-message {
  text-align: center;
  padding: 40px;
  color: #666;
}

.error-message {
  color: #f44336;
  background: #ffebee;
  border-radius: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  font-size: 40px;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 50%;
}

.stat-content {
  flex: 1;
}

.stat-content h3 {
  margin: 0 0 8px 0;
  color: #666;
  font-size: 16px;
  font-weight: 500;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 4px;
}

.stat-desc {
  margin: 0;
  color: #999;
  font-size: 12px;
}

/* Card specific colors */
.agent-card .stat-icon {
  background: #e3f2fd;
  color: #2196f3;
}

.kb-card .stat-icon {
  background: #e8f5e9;
  color: #4caf50;
}

.workflow-card .stat-icon {
  background: #fff3e0;
  color: #ff9800;
}

.plugin-card .stat-icon {
  background: #f3e5f5;
  color: #9c27b0;
}
</style>
