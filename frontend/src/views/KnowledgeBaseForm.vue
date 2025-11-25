<template>
  <div class="knowledge-base-form">
    <div class="header">
      <h1>{{ isEdit ? '编辑知识库' : '创建知识库' }}</h1>
      <router-link to="/knowledge-bases" class="back-btn">返回列表</router-link>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>

    <form @submit.prevent="handleSubmit" class="form-container">
      <div class="form-group">
        <label for="name">知识库名称 <span class="required">*</span></label>
        <input
          id="name"
          v-model="form.name"
          type="text"
          required
          maxlength="100"
          placeholder="请输入知识库名称"
          :disabled="loading"
        />
        <span class="hint">{{ form.name.length }}/100</span>
      </div>

      <div class="form-group">
        <label for="description">描述</label>
        <textarea
          id="description"
          v-model="form.description"
          rows="4"
          maxlength="1000"
          placeholder="请输入知识库描述（可选）"
          :disabled="loading"
        ></textarea>
        <span class="hint">{{ form.description.length }}/1000</span>
      </div>

      <div class="form-actions">
        <button type="button" @click="$router.push('/knowledge-bases')" :disabled="loading" class="cancel-btn">
          取消
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
  createKnowledgeBase,
  updateKnowledgeBase,
  getKnowledgeBase,
  type CreateKnowledgeBaseRequest,
} from '../api/knowledgebase'

const router = useRouter()
const route = useRoute()

const knowledgeBaseId = computed(() => {
  const id = route.params.id
  return id && id !== 'new' ? Number(id) : null
})

const isEdit = computed(() => !!knowledgeBaseId.value && route.name !== 'KnowledgeBaseCreate')

const form = ref({
  name: '',
  description: '',
})

const loading = ref(false)
const error = ref<string | null>(null)

// 加载知识库详情（编辑模式）
const loadKnowledgeBase = async () => {
  if (!knowledgeBaseId.value) return

  loading.value = true
  error.value = null
  try {
    const kb = await getKnowledgeBase(knowledgeBaseId.value)
    form.value = {
      name: kb.name,
      description: kb.description || '',
    }
  } catch (e: any) {
    error.value = e.message || '加载知识库详情失败'
    console.error('加载知识库详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    error.value = '请填写知识库名称'
    return
  }

  loading.value = true
  error.value = null

  try {
    const data: CreateKnowledgeBaseRequest = {
      name: form.value.name.trim(),
      description: form.value.description.trim() || undefined,
    }

    if (isEdit.value) {
      await updateKnowledgeBase(knowledgeBaseId.value!, data)
    } else {
      await createKnowledgeBase(data)
    }
    router.push('/knowledge-bases')
  } catch (e: any) {
    error.value = e.message || (isEdit.value ? '更新知识库失败' : '创建知识库失败')
    console.error('保存失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (isEdit.value) {
    loadKnowledgeBase()
  }
})
</script>

<style scoped>
.knowledge-base-form {
  padding: 40px;
  max-width: 800px;
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
textarea {
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
textarea:focus {
  outline: none;
  border-color: #42b983;
}

input:disabled,
textarea:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

textarea {
  resize: vertical;
  min-height: 100px;
}

.hint {
  display: block;
  margin-top: 5px;
  font-size: 12px;
  color: #999;
  text-align: right;
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
  .knowledge-base-form {
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
}
</style>

