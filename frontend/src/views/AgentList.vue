<template>
  <div class="agent-list">
    <div class="header">
      <div class="header-content">
        <h1>🤖 智能体管理</h1>
        <p class="header-subtitle">创建和管理您的AI智能体</p>
      </div>
      <div class="header-actions">
        <router-link to="/" class="back-btn">
          <span class="btn-icon">⬅️</span>
          返回首页
        </router-link>
        <button @click="refreshAgents" :disabled="loading" class="refresh-btn">
          <span class="btn-icon">🔄</span>
          {{ loading ? '加载中...' : '刷新' }}
        </button>
        <router-link to="/agents/new" class="create-btn">
          <span class="btn-icon">➕</span>
          创建智能体
        </router-link>
      </div>
    </div>

    <div v-if="error" class="message error-message">
      <span class="message-icon">⚠️</span>
      {{ error }}
    </div>
    <div v-if="successMessage" class="message success-message">
      <span class="message-icon">✅</span>
      {{ successMessage }}
    </div>

    <div v-if="loading && agents.length === 0" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="agents.length === 0" class="empty-state">
      <div class="empty-icon">🤖</div>
      <h2>还没有创建任何智能体</h2>
      <p>开始创建您的第一个AI智能体吧！</p>
      <router-link to="/agents/new" class="create-link">
        <span>➕</span>
        立即创建
      </router-link>
    </div>
    <div v-else class="agent-grid">
      <div
        v-for="agent in agents"
        :key="agent.id"
        class="agent-card"
        :class="{ 'published': agent.status === 'published' }"
      >
        <div class="card-header">
          <div class="agent-icon">🤖</div>
          <div class="agent-title-section">
            <h3 class="agent-name">{{ agent.name }}</h3>
            <span :class="['status-badge', agent.status === 'published' ? 'published' : 'draft']">
              <span class="status-dot"></span>
              {{ agent.status === 'published' ? '已发布' : '草稿' }}
            </span>
          </div>
        </div>

        <div class="card-body">
          <p class="agent-description">
            {{ agent.description || '暂无描述' }}
          </p>

          <div v-if="agent.systemPrompt" class="agent-prompt">
            <div class="prompt-header">
              <span class="prompt-icon">💡</span>
              <span class="prompt-label">系统提示词</span>
            </div>
            <p class="prompt-text">{{ truncatePrompt(agent.systemPrompt) }}</p>
          </div>

          <div class="agent-info">
            <div class="info-item">
              <span class="info-icon">📅</span>
              <span class="info-text">{{ formatDate(agent.createdAt) }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <div class="action-buttons">
            <button
              v-if="agent.status === 'draft'"
              @click="handlePublish(agent)"
              class="action-btn publish-btn"
              title="发布智能体"
            >
              <span>🚀</span>
              <span>发布</span>
            </button>
            <button
              v-if="agent.status === 'published'"
              @click="handleChat(agent)"
              class="action-btn chat-btn"
              title="与智能体对话"
            >
              <span>💬</span>
              <span>对话</span>
            </button>
            <button
              @click="handleTest(agent)"
              class="action-btn test-btn"
              title="测试智能体"
            >
              <span>🧪</span>
              <span>测试</span>
            </button>
            <button
              @click="handleEdit(agent)"
              class="action-btn edit-btn"
              title="编辑智能体"
            >
              <span>✏️</span>
              <span>编辑</span>
            </button>
            <button
              @click="handleDelete(agent)"
              class="action-btn delete-btn"
              title="删除智能体"
            >
              <span>🗑️</span>
              <span>删除</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAgents, deleteAgent, publishAgent, type Agent } from '../api/agent'

const router = useRouter()
const agents = ref<Agent[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// 加载智能体列表
const loadAgents = async () => {
  loading.value = true
  error.value = null
  successMessage.value = null
  try {
    agents.value = await getAgents()
  } catch (e: any) {
    error.value = e.message || '加载智能体列表失败'
    console.error('加载智能体列表失败:', e)
  } finally {
    loading.value = false
  }
}

// 刷新列表
const refreshAgents = () => {
  loadAgents()
}

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
    })
  } catch (e) {
    return dateStr
  }
}

// 截断提示词
const truncatePrompt = (prompt: string) => {
  if (!prompt) return ''
  return prompt.length > 100 ? prompt.substring(0, 100) + '...' : prompt
}

// 测试智能体
const handleTest = (agent: Agent) => {
  router.push(`/agents/${agent.id}/test`)
}

// 发布智能体
const handlePublish = async (agent: Agent) => {
  if (!confirm(`确定要发布智能体"${agent.name}"吗？发布后智能体可以被使用。`)) {
    return
  }

  error.value = null
  successMessage.value = null

  try {
    await publishAgent(agent.id)
    successMessage.value = '发布成功'
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
    loadAgents()
  } catch (e: any) {
    error.value = e.message || '发布失败'
    console.error('发布智能体失败:', e)
  }
}

// 对话
const handleChat = (agent: Agent) => {
  router.push(`/agents/${agent.id}/chat`)
}

// 编辑智能体
const handleEdit = (agent: Agent) => {
  router.push(`/agents/${agent.id}/edit`)
}

// 删除智能体
const handleDelete = async (agent: Agent) => {
  if (!confirm(`确定要删除智能体 "${agent.name}" 吗？\n\n此操作不可恢复！`)) {
    return
  }

  error.value = null
  successMessage.value = null

  try {
    await deleteAgent(agent.id)
    successMessage.value = `智能体 "${agent.name}" 已成功删除`
    await loadAgents()
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
  } catch (e: any) {
    error.value = e.message || '删除智能体失败'
    setTimeout(() => {
      error.value = null
    }, 5000)
  }
}

onMounted(() => {
  loadAgents()
})
</script>

<style scoped>
.agent-list {
  padding: 30px;
  max-width: 1600px;
  margin: 0 auto;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 30px 40px;
  border-radius: 16px;
  margin-bottom: 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  flex-wrap: wrap;
  gap: 20px;
}

.header-content h1 {
  color: #2c3e50;
  margin: 0 0 8px 0;
  font-size: 32px;
  font-weight: 700;
}

.header-subtitle {
  color: #666;
  margin: 0;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.back-btn,
.refresh-btn,
.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s ease;
  text-decoration: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.back-btn {
  background: #6c757d;
  color: white;
}

.back-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(108, 117, 125, 0.3);
}

.refresh-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.refresh-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.3);
}

.refresh-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
}

.create-btn {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(245, 87, 108, 0.3);
}

.btn-icon {
  font-size: 18px;
}

.message {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  font-size: 15px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.error-message {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  color: white;
}

.success-message {
  background: linear-gradient(135deg, #51cf66 0%, #40c057 100%);
  color: white;
}

.message-icon {
  font-size: 20px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #666;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid rgba(102, 126, 234, 0.2);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-state {
  text-align: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 80px 40px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.empty-state h2 {
  color: #2c3e50;
  margin: 0 0 10px 0;
  font-size: 24px;
}

.empty-state p {
  color: #666;
  margin: 0 0 30px 0;
  font-size: 16px;
}

.create-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.create-link:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 24px;
}

.agent-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  border: 2px solid transparent;
}

.agent-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}

.agent-card.published::before {
  background: linear-gradient(90deg, #51cf66 0%, #40c057 100%);
}

.agent-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  border-color: rgba(102, 126, 234, 0.3);
}

.agent-card:hover::before {
  transform: scaleX(1);
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
}

.agent-icon {
  font-size: 48px;
  line-height: 1;
  flex-shrink: 0;
}

.agent-title-section {
  flex: 1;
  min-width: 0;
}

.agent-name {
  color: #2c3e50;
  margin: 0 0 8px 0;
  font-size: 22px;
  font-weight: 600;
  word-break: break-word;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.published {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  color: #2e7d32;
}

.status-badge.draft {
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  color: #e65100;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  display: inline-block;
}

.card-body {
  margin-bottom: 20px;
}

.agent-description {
  color: #555;
  margin: 0 0 16px 0;
  line-height: 1.6;
  font-size: 14px;
  min-height: 44px;
}

.agent-prompt {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 14px;
  border-radius: 10px;
  margin-bottom: 16px;
  border-left: 3px solid #667eea;
}

.prompt-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.prompt-icon {
  font-size: 16px;
}

.prompt-label {
  font-weight: 600;
  color: #495057;
  font-size: 13px;
}

.prompt-text {
  color: #666;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}

.agent-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 12px;
}

.info-icon {
  font-size: 14px;
}

.card-footer {
  padding-top: 16px;
  border-top: 2px solid #f0f0f0;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.action-btn span:first-child {
  font-size: 16px;
}

.publish-btn {
  background: linear-gradient(135deg, #51cf66 0%, #40c057 100%);
  color: white;
}

.chat-btn {
  background: linear-gradient(135deg, #9c27b0 0%, #7b1fa2 100%);
  color: white;
}

.test-btn {
  background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%);
  color: white;
}

.edit-btn {
  background: linear-gradient(135deg, #42b983 0%, #35a372 100%);
  color: white;
}

.delete-btn {
  background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%);
  color: white;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .agent-list {
    padding: 20px;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px;
  }

  .header-content h1 {
    font-size: 24px;
  }

  .agent-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .agent-card {
    padding: 20px;
  }

  .agent-icon {
    font-size: 40px;
  }

  .agent-name {
    font-size: 20px;
  }

  .action-buttons {
    justify-content: flex-start;
  }

  .action-btn {
    flex: 1;
    min-width: 80px;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .refresh-btn,
  .create-btn {
    width: 100%;
    justify-content: center;
  }

  .action-btn span:last-child {
    display: none;
  }

  .action-btn {
    min-width: 44px;
    padding: 8px;
    justify-content: center;
  }
}
</style>
