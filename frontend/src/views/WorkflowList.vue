<template>
  <div class="workflow-list">
    <div class="header">
      <h1>工作流管理</h1>
      <router-link to="/workflows/new" class="create-btn">创建工作流</router-link>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="workflows.length === 0" class="empty-state">
      <p>还没有工作流，<router-link to="/workflows/new">创建一个</router-link></p>
    </div>
    <div v-else class="workflow-grid">
      <div
        v-for="workflow in workflows"
        :key="workflow.id"
        class="workflow-card"
        @click="handleViewDetail(workflow.id)"
      >
        <div class="card-header">
          <h3>{{ workflow.name }}</h3>
          <span :class="['status-badge', workflow.status === 'published' ? 'published' : 'draft']">
            {{ workflow.status === 'published' ? '已发布' : '草稿' }}
          </span>
        </div>
        <p class="card-description">{{ workflow.description || '暂无描述' }}</p>
        <div class="card-footer">
          <span class="card-date">创建时间：{{ formatDate(workflow.createdAt) }}</span>
          <div class="card-actions" @click.stop>
            <button @click.stop="handleEdit(workflow)" class="edit-btn">编辑</button>
            <button @click.stop="handleExecute(workflow)" class="execute-btn">执行</button>
            <button @click.stop="handleDelete(workflow)" class="delete-btn">删除</button>
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
  getWorkflows,
  deleteWorkflow,
  type Workflow,
} from '../api/workflow'

const router = useRouter()
const workflows = ref<Workflow[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// 加载工作流列表
const loadWorkflows = async () => {
  loading.value = true
  error.value = null
  try {
    const list = await getWorkflows()
    workflows.value = list
  } catch (e: any) {
    error.value = e.message || '加载工作流列表失败'
    console.error('加载工作流列表失败:', e)
  } finally {
    loading.value = false
  }
}

// 查看详情（编辑）
const handleViewDetail = (id: number) => {
  router.push(`/workflows/${id}/edit`)
}

// 编辑
const handleEdit = (workflow: Workflow) => {
  router.push(`/workflows/${workflow.id}/edit`)
}

// 执行
const handleExecute = (workflow: Workflow) => {
  router.push(`/workflows/${workflow.id}/execute`)
}

// 删除
const handleDelete = async (workflow: Workflow) => {
  if (!confirm(`确定要删除工作流"${workflow.name}"吗？`)) {
    return
  }

  try {
    await deleteWorkflow(workflow.id)
    successMessage.value = '删除成功'
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
    loadWorkflows()
  } catch (e: any) {
    error.value = e.message || '删除失败'
    console.error('删除工作流失败:', e)
  }
}

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadWorkflows()
})
</script>

<style scoped>
.workflow-list {
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

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.workflow-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.workflow-card:hover {
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

.status-badge.published {
  background: #e8f5e9;
  color: #4caf50;
}

.status-badge.draft {
  background: #fff3e0;
  color: #f57c00;
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

.edit-btn,
.execute-btn,
.delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.edit-btn {
  background: #42b983;
  color: white;
}

.edit-btn:hover {
  background: #35a372;
}

.execute-btn {
  background: #2196f3;
  color: white;
}

.execute-btn:hover {
  background: #1976d2;
}

.delete-btn {
  background: #f44336;
  color: white;
}

.delete-btn:hover {
  background: #d32f2f;
}

@media (max-width: 768px) {
  .workflow-list {
    padding: 20px;
  }

  .header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .workflow-grid {
    grid-template-columns: 1fr;
  }
}
</style>

