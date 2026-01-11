<template>
  <div class="plugin-register">
    <div class="header">
      <h1>{{ isEdit ? '编辑插件' : '注册插件' }}</h1>
      <router-link to="/plugins" class="back-btn">返回列表</router-link>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

    <form @submit.prevent="handleSubmit" class="form-container">
      <div class="form-group">
        <label for="name">插件名称 <span class="required">*</span></label>
        <input
          id="name"
          v-model="form.name"
          type="text"
          required
          maxlength="100"
          placeholder="请输入插件名称"
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
          placeholder="请输入插件描述（可选）"
          :disabled="loading"
        ></textarea>
        <span class="hint">{{ form.description.length }}/1000</span>
      </div>

      <div class="form-group">
        <label for="openapiFile">
          OpenAPI规范文件 <span class="required">*</span>
          <span v-if="isEdit" class="optional-hint">（可选，不选择则保留原文件）</span>
        </label>
        <input
          id="openapiFile"
          type="file"
          accept=".json"
          @change="handleFileSelect"
          :disabled="loading"
        />
        <p class="help-text">请上传JSON格式的OpenAPI规范文件（最大5MB）</p>
        <div v-if="selectedFile" class="file-info">
          <span>已选择文件：{{ selectedFile.name }}</span>
          <button type="button" @click="clearFile" class="clear-file-btn">清除</button>
        </div>
        <div v-if="isEdit && !selectedFile" class="file-info">
          <span>将保留原有OpenAPI规范文件</span>
        </div>
      </div>

      <div class="form-actions">
        <button type="button" @click="$router.push('/plugins')" :disabled="loading" class="cancel-btn">
          取消
        </button>
        <button type="submit" :disabled="loading || (!isEdit && !selectedFile)" class="submit-btn">
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
  registerPlugin,
  updatePlugin,
  getPlugin,
} from '../api/plugin'

const router = useRouter()
const route = useRoute()

const pluginId = computed(() => {
  const id = route.params.id
  return id && id !== 'new' ? Number(id) : null
})

const isEdit = computed(() => !!pluginId.value && route.name !== 'PluginRegister')

const form = ref({
  name: '',
  description: '',
})

const selectedFile = ref<File | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// 加载插件详情（编辑模式）
const loadPlugin = async () => {
  if (!pluginId.value) return

  loading.value = true
  error.value = null
  try {
    const plugin = await getPlugin(pluginId.value)
    form.value = {
      name: plugin.name,
      description: plugin.description || '',
    }
  } catch (e: any) {
    error.value = e.message || '加载插件详情失败'
    console.error('加载插件详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 文件选择
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) {
    selectedFile.value = null
    return
  }

  // 验证文件类型
  if (!file.name.toLowerCase().endsWith('.json')) {
    error.value = '只支持JSON格式的文件'
    selectedFile.value = null
    return
  }

  // 验证文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    error.value = '文件大小不能超过5MB'
    selectedFile.value = null
    return
  }

  selectedFile.value = file
  error.value = null
}

// 清除文件
const clearFile = () => {
  selectedFile.value = null
  const fileInput = document.getElementById('openapiFile') as HTMLInputElement
  if (fileInput) {
    fileInput.value = ''
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    error.value = '请填写插件名称'
    return
  }

  if (!isEdit.value && !selectedFile.value) {
    error.value = '请选择OpenAPI规范文件'
    return
  }

  loading.value = true
  error.value = null
  successMessage.value = null

  try {
    if (isEdit.value) {
      await updatePlugin(
        pluginId.value!,
        form.value.name.trim(),
        form.value.description.trim() || '',
        selectedFile.value || undefined
      )
      successMessage.value = '更新成功'
    } else {
      await registerPlugin(
        form.value.name.trim(),
        form.value.description.trim() || '',
        selectedFile.value!
      )
      successMessage.value = '注册成功'
    }

    setTimeout(() => {
      router.push('/plugins')
    }, 1500)
  } catch (e: any) {
    error.value = e.message || (isEdit.value ? '更新失败' : '注册失败')
    console.error('保存失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (isEdit.value) {
    loadPlugin()
  }
})
</script>

<style scoped>
.plugin-register {
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

.success-message {
  background: #e8f5e9;
  color: #4caf50;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #4caf50;
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

.optional-hint {
  color: #999;
  font-weight: normal;
  font-size: 12px;
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

input[type="file"] {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
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

.file-info {
  margin-top: 10px;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-info span {
  color: #666;
  font-size: 14px;
}

.clear-file-btn {
  padding: 4px 8px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.clear-file-btn:hover {
  background: #d32f2f;
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
  .plugin-register {
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

