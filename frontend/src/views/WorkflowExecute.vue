<template>
  <div class="workflow-execute">
    <div class="header">
      <router-link to="/workflows" class="back-link">← 返回列表</router-link>
      <h1 v-if="workflow">{{ workflow.name }}</h1>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="workflow" class="content">
      <div class="execute-section">
        <h2>执行工作流</h2>
        <div class="input-section">
          <label>输入参数（JSON格式）</label>
          <textarea
            v-model="inputParamsJson"
            rows="10"
            placeholder='{"key": "value"}'
            class="input-textarea"
            :disabled="executing"
          ></textarea>
        </div>
        <button @click="handleExecute" :disabled="executing" class="execute-btn">
          {{ executing ? '执行中...' : '执行工作流' }}
        </button>
      </div>

      <div class="executions-section">
        <h2>执行记录</h2>
        <div v-if="executionsLoading" class="loading">加载执行记录中...</div>
        <div v-else-if="executions.length === 0" class="empty-state">暂无执行记录</div>
        <div v-else class="executions-list">
          <div
            v-for="execution in executions"
            :key="execution.id"
            class="execution-item"
            @click="handleViewExecution(execution)"
          >
            <div class="execution-header">
              <span class="execution-id">执行 #{{ execution.id }}</span>
              <span :class="['status-badge', execution.status]">
                {{ getStatusText(execution.status) }}
              </span>
            </div>
            <div class="execution-meta">
              <span>创建时间：{{ formatDate(execution.createdAt) }}</span>
              <span v-if="execution.startedAt">开始时间：{{ formatDate(execution.startedAt) }}</span>
              <span v-if="execution.completedAt">完成时间：{{ formatDate(execution.completedAt) }}</span>
            </div>
            <div v-if="execution.errorMessage" class="execution-error">
              错误：{{ execution.errorMessage }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  getWorkflow,
  executeWorkflow,
  getWorkflowExecutions,
  type Workflow,
  type WorkflowExecution,
} from '../api/workflow'

const router = useRouter()
const route = useRoute()

const workflowId = Number(route.params.id)
const workflow = ref<Workflow | null>(null)
const executions = ref<WorkflowExecution[]>([])
const inputParamsJson = ref('{}')
const loading = ref(false)
const executionsLoading = ref(false)
const executing = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// 加载工作流详情
const loadWorkflow = async () => {
  loading.value = true
  error.value = null
  try {
    const wf = await getWorkflow(workflowId)
    workflow.value = wf
  } catch (e: any) {
    error.value = e.message || '加载工作流详情失败'
    console.error('加载工作流详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 加载执行记录
const loadExecutions = async () => {
  executionsLoading.value = true
  try {
    const execs = await getWorkflowExecutions(workflowId)
    executions.value = execs
  } catch (e: any) {
    error.value = e.message || '加载执行记录失败'
    console.error('加载执行记录失败:', e)
  } finally {
    executionsLoading.value = false
  }
}

// 执行工作流
const handleExecute = async () => {
  let inputParams: Record<string, any> = {}
  try {
    inputParams = JSON.parse(inputParamsJson.value)
  } catch (e) {
    error.value = '输入参数格式错误，请输入有效的JSON'
    return
  }

  executing.value = true
  error.value = null
  successMessage.value = null

  try {
    await executeWorkflow(workflowId, { inputParams })
    successMessage.value = '工作流执行已启动'
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
    inputParamsJson.value = '{}'
    // 刷新执行记录
    setTimeout(() => {
      loadExecutions()
    }, 1000)
  } catch (e: any) {
    error.value = e.message || '执行工作流失败'
    console.error('执行工作流失败:', e)
  } finally {
    executing.value = false
  }
}

// 查看执行详情
const handleViewExecution = (execution: WorkflowExecution) => {
  alert(`执行状态：${getStatusText(execution.status)}\n\n输入参数：${JSON.stringify(execution.inputParams, null, 2)}\n\n输出结果：${JSON.stringify(execution.outputResult, null, 2)}`)
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    pending: '等待中',
    running: '运行中',
    completed: '已完成',
    failed: '失败',
  }
  return statusMap[status] || status
}

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadWorkflow()
  loadExecutions()
})
</script>

<style scoped>
.workflow-execute {
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

.execute-section,
.executions-section {
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

.input-section {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 8px;
  color: #555;
  font-weight: 500;
}

.input-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
}

.input-textarea:focus {
  outline: none;
  border-color: #42b983;
}

.input-textarea:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.execute-btn {
  padding: 12px 24px;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}

.execute-btn:hover:not(:disabled) {
  background: #35a372;
}

.execute-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.executions-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.execution-item {
  padding: 15px;
  background: #f9f9f9;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.execution-item:hover {
  background: #f0f0f0;
}

.execution-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.execution-id {
  font-weight: 500;
  color: #2c3e50;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.pending {
  background: #fff3e0;
  color: #f57c00;
}

.status-badge.running {
  background: #e3f2fd;
  color: #2196f3;
}

.status-badge.completed {
  background: #e8f5e9;
  color: #4caf50;
}

.status-badge.failed {
  background: #ffebee;
  color: #f44336;
}

.execution-meta {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #666;
  margin-bottom: 10px;
}

.execution-error {
  padding: 10px;
  background: #ffebee;
  color: #f44336;
  border-radius: 4px;
  font-size: 14px;
}

@media (max-width: 768px) {
  .workflow-execute {
    padding: 20px;
  }

  .execute-section,
  .executions-section {
    padding: 20px;
  }

  .execution-meta {
    flex-direction: column;
    gap: 5px;
  }
}
</style>

