<template>
  <div class="agent-form">
    <div class="header">
      <h1>{{ isEdit ? '编辑智能体' : '创建智能体' }}</h1>
      <router-link to="/agents" class="back-btn">返回列表</router-link>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>

    <form @submit.prevent="handleSubmit" class="form-container">
      <div class="form-group">
        <label for="name">智能体名称 <span class="required">*</span></label>
        <input
          id="name"
          v-model="form.name"
          type="text"
          required
          maxlength="100"
          placeholder="请输入智能体名称"
          :disabled="loading"
        />
        <span class="hint">{{ form.name.length }}/100</span>
      </div>

      <div class="form-group">
        <label for="description">描述</label>
        <textarea
          id="description"
          v-model="form.description"
          rows="3"
          maxlength="5000"
          placeholder="请输入智能体描述（可选）"
          :disabled="loading"
        ></textarea>
        <span class="hint">{{ form.description.length }}/5000</span>
      </div>

      <div class="form-group">
        <label for="systemPrompt">系统提示词</label>
        <textarea
          id="systemPrompt"
          v-model="form.systemPrompt"
          rows="10"
          maxlength="10000"
          placeholder="请输入系统提示词，这将指导AI如何回答用户的问题"
          :disabled="loading"
          class="prompt-textarea"
        ></textarea>
        <span class="hint">{{ form.systemPrompt.length }}/10000</span>
        <p class="help-text">系统提示词是指导AI行为的关键，请详细描述你希望AI扮演的角色和回答风格。</p>
      </div>

      <div class="form-group">
        <label for="userPromptTemplate">用户提示词模板</label>
        <textarea
          id="userPromptTemplate"
          v-model="form.userPromptTemplate"
          rows="5"
          maxlength="5000"
          placeholder="请输入用户提示词模板（可选）"
          :disabled="loading"
          class="prompt-textarea"
        ></textarea>
        <span class="hint">{{ form.userPromptTemplate.length }}/5000</span>
        <p class="help-text">用户提示词模板用于格式化用户输入，可以使用占位符如 {question}。</p>
      </div>

      <div class="form-group">
        <label for="modelConfig">模型配置（JSON）</label>
        <textarea
          id="modelConfig"
          v-model="form.modelConfig"
          rows="5"
          placeholder='请输入模型配置JSON，例如：{"model": "gpt-4", "temperature": 0.7}'
          :disabled="loading"
          class="json-textarea"
        ></textarea>
        <p class="help-text">模型配置为JSON格式，包含模型名称、温度等参数。</p>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label for="workflowId">工作流ID</label>
          <input
            id="workflowId"
            v-model.number="form.workflowId"
            type="number"
            placeholder="关联工作流ID（可选）"
            :disabled="loading"
          />
        </div>

        <div v-if="isEdit" class="form-group">
          <label for="status">状态</label>
          <select id="status" v-model="form.status" :disabled="loading">
            <option value="draft">草稿</option>
            <option value="published">已发布</option>
          </select>
        </div>
      </div>

      <div class="form-group">
        <label for="knowledgeBaseIds">知识库ID列表（JSON数组）</label>
        <input
          id="knowledgeBaseIds"
          v-model="form.knowledgeBaseIds"
          type="text"
          placeholder='例如：[1, 2, 3]'
          :disabled="loading"
        />
        <p class="help-text">知识库ID列表，JSON数组格式。</p>
      </div>

      <div class="form-group">
        <label for="pluginIds">插件ID列表（JSON数组）</label>
        <input
          id="pluginIds"
          v-model="form.pluginIds"
          type="text"
          placeholder='例如：["plugin1", "plugin2"]'
          :disabled="loading"
        />
        <p class="help-text">插件ID列表，JSON数组格式。</p>
      </div>

      <div class="form-actions">
        <button type="button" @click="$router.push('/agents')" :disabled="loading" class="cancel-btn">
          取消
        </button>
        <button
          v-if="isEdit && form.status === 'draft'"
          type="button"
          @click="handlePublish"
          :disabled="loading || publishing"
          class="publish-btn"
        >
          {{ publishing ? '发布中...' : '保存并发布' }}
        </button>
        <button type="submit" :disabled="loading" class="submit-btn">
          {{ loading ? '保存中...' : '保存' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  createAgent,
  updateAgent,
  getAgent,
  publishAgent,
  type CreateAgentRequest,
  type UpdateAgentRequest,
} from '../api/agent'

const router = useRouter()
const route = useRoute()

const agentId = computed(() => {
  const id = route.params.id
  return id && id !== 'new' ? Number(id) : null
})

const isEdit = computed(() => !!agentId.value)

const form = ref({
  name: '',
  description: '',
  systemPrompt: '',
  userPromptTemplate: '',
  modelConfig: '',
  workflowId: null as number | null,
  knowledgeBaseIds: '',
  pluginIds: '',
  status: 'draft',
})

const loading = ref(false)
const publishing = ref(false)
const error = ref<string | null>(null)

// 加载智能体详情（编辑模式）
const loadAgent = async () => {
  if (!agentId.value) return

  loading.value = true
  error.value = null
  try {
    const agent = await getAgent(agentId.value)
    form.value = {
      name: agent.name,
      description: agent.description || '',
      systemPrompt: agent.systemPrompt || '',
      userPromptTemplate: agent.userPromptTemplate || '',
      modelConfig: agent.modelConfig || '',
      workflowId: agent.workflowId || null,
      knowledgeBaseIds: agent.knowledgeBaseIds || '',
      pluginIds: agent.pluginIds || '',
      status: agent.status || 'draft',
    }
  } catch (e: any) {
    error.value = e.message || '加载智能体详情失败'
    console.error('加载智能体详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    error.value = '请填写智能体名称'
    return
  }

  loading.value = true
  error.value = null

  try {
    if (isEdit.value) {
      const updateData: UpdateAgentRequest = {
        name: form.value.name.trim(),
        description: form.value.description.trim() || undefined,
        systemPrompt: form.value.systemPrompt.trim() || undefined,
        userPromptTemplate: form.value.userPromptTemplate.trim() || undefined,
        modelConfig: form.value.modelConfig.trim() || undefined,
        workflowId: form.value.workflowId || undefined,
        knowledgeBaseIds: form.value.knowledgeBaseIds.trim() || undefined,
        pluginIds: form.value.pluginIds.trim() || undefined,
        status: form.value.status,
      }
      await updateAgent(agentId.value!, updateData)
      router.push('/agents')
    } else {
      const createData: CreateAgentRequest = {
        name: form.value.name.trim(),
        description: form.value.description.trim() || undefined,
        systemPrompt: form.value.systemPrompt.trim() || undefined,
        userPromptTemplate: form.value.userPromptTemplate.trim() || undefined,
        modelConfig: form.value.modelConfig.trim() || undefined,
        workflowId: form.value.workflowId || undefined,
        knowledgeBaseIds: form.value.knowledgeBaseIds.trim() || undefined,
        pluginIds: form.value.pluginIds.trim() || undefined,
      }
      await createAgent(createData)
      router.push('/agents')
    }
  } catch (e: any) {
    error.value = e.message || (isEdit.value ? '更新智能体失败' : '创建智能体失败')
    console.error('保存失败:', e)
  } finally {
    loading.value = false
  }
}

// 发布智能体
const handlePublish = async () => {
  if (!form.value.name.trim()) {
    error.value = '请填写智能体名称'
    return
  }

  publishing.value = true
  error.value = null

  try {
    // 先保存
    const data: UpdateAgentRequest = {
      name: form.value.name.trim(),
      description: form.value.description.trim() || undefined,
      systemPrompt: form.value.systemPrompt.trim() || undefined,
      userPromptTemplate: form.value.userPromptTemplate.trim() || undefined,
      modelConfig: form.value.modelConfig.trim() || undefined,
      workflowId: form.value.workflowId || undefined,
      knowledgeBaseIds: form.value.knowledgeBaseIds.trim() || undefined,
      pluginIds: form.value.pluginIds.trim() || undefined,
      status: form.value.status,
    }
    await updateAgent(agentId.value!, data)

    // 再发布
    await publishAgent(agentId.value!)
    router.push('/agents')
  } catch (e: any) {
    error.value = e.message || '发布失败'
    console.error('发布失败:', e)
  } finally {
    publishing.value = false
  }
}

onMounted(() => {
  if (isEdit.value) {
    loadAgent()
  }
})
</script>

<style scoped>
.agent-form {
  padding: 40px;
  max-width: 1000px;
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

.back-btn {
  padding: 10px 20px;
  background: #6c757d;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-size: 14px;
  transition: background 0.3s;
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

.form-container {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.form-group {
  margin-bottom: 25px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

label {
  display: block;
  margin-bottom: 8px;
  color: #555;
  font-weight: 500;
  font-size: 14px;
}

.required {
  color: #f44336;
}

input[type="text"],
input[type="number"],
textarea,
select {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

input[type="text"]:focus,
input[type="number"]:focus,
textarea:focus,
select:focus {
  outline: none;
  border-color: #42b983;
}

input:disabled,
textarea:disabled,
select:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

textarea {
  resize: vertical;
  min-height: 80px;
}

.prompt-textarea {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.json-textarea {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.hint {
  display: block;
  margin-top: 5px;
  font-size: 12px;
  color: #999;
  text-align: right;
}

.help-text {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  line-height: 1.5;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.cancel-btn,
.submit-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}

.cancel-btn {
  background: #6c757d;
  color: white;
}

.cancel-btn:hover:not(:disabled) {
  background: #5a6268;
}

.submit-btn {
  background: #42b983;
  color: white;
}

.submit-btn:hover:not(:disabled) {
  background: #35a372;
}

.cancel-btn:disabled,
.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .agent-form {
    padding: 20px;
  }

  .header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .form-container {
    padding: 20px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
