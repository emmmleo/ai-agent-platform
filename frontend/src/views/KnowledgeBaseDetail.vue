<template>
  <div class="knowledge-base-detail">
    <div class="header">
      <div>
        <router-link to="/knowledge-bases" class="back-link">← 返回列表</router-link>
        <h1 v-if="knowledgeBase">{{ knowledgeBase.name }}</h1>
      </div>
      <button v-if="knowledgeBase" @click="handleEdit" class="edit-btn">编辑</button>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="knowledgeBase" class="content">
      <div class="info-section">
        <h2>知识库信息</h2>
        <p><strong>描述：</strong>{{ knowledgeBase.description || '暂无描述' }}</p>
        <p><strong>创建时间：</strong>{{ formatDate(knowledgeBase.createdAt) }}</p>
      </div>

      <div class="documents-section">
        <div class="section-header">
          <h2>文档列表</h2>
          <label class="upload-btn">
            上传文档
            <input
              type="file"
              accept=".txt,.md,.markdown"
              @change="handleFileSelect"
              style="display: none"
              :disabled="uploading"
            />
          </label>
        </div>

        <div v-if="uploading" class="uploading">上传中...</div>

        <div v-if="documentsLoading" class="loading">加载文档中...</div>
        <div v-else-if="documents.length === 0" class="empty-state">
          <p>还没有文档，上传一个文档开始使用</p>
        </div>
        <div v-else class="documents-table">
          <table>
            <thead>
              <tr>
                <th>文件名</th>
                <th>类型</th>
                <th>大小</th>
                <th>状态</th>
                <th>分块数</th>
                <th>上传时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="doc in documents" :key="doc.id">
                <td>{{ doc.fileName }}</td>
                <td>{{ doc.fileType.toUpperCase() }}</td>
                <td>{{ formatFileSize(doc.fileSize) }}</td>
                <td>
                  <span :class="['status-badge', doc.status]">
                    {{ getStatusText(doc.status) }}
                  </span>
                </td>
                <td>{{ doc.chunkCount }}</td>
                <td>{{ formatDate(doc.createdAt) }}</td>
                <td>
                  <button @click="handleDeleteDocument(doc)" class="delete-btn">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  getKnowledgeBase,
  getDocuments,
  uploadDocument,
  deleteDocument,
  type KnowledgeBase,
  type KnowledgeDocument,
} from '../api/knowledgebase'

const router = useRouter()
const route = useRoute()

const knowledgeBaseId = Number(route.params.id)
const knowledgeBase = ref<KnowledgeBase | null>(null)
const documents = ref<KnowledgeDocument[]>([])
const loading = ref(false)
const documentsLoading = ref(false)
const uploading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// 加载知识库详情
const loadKnowledgeBase = async () => {
  loading.value = true
  error.value = null
  try {
    const kb = await getKnowledgeBase(knowledgeBaseId)
    knowledgeBase.value = kb
  } catch (e: any) {
    error.value = e.message || '加载知识库详情失败'
    console.error('加载知识库详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 加载文档列表
const loadDocuments = async () => {
  documentsLoading.value = true
  try {
    const docs = await getDocuments(knowledgeBaseId)
    documents.value = docs
  } catch (e: any) {
    error.value = e.message || '加载文档列表失败'
    console.error('加载文档列表失败:', e)
  } finally {
    documentsLoading.value = false
  }
}

// 文件选择
const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型
  const fileName = file.name.toLowerCase()
  const isValidType = fileName.endsWith('.txt') || fileName.endsWith('.md') || fileName.endsWith('.markdown')
  
  if (!isValidType) {
    error.value = '只支持TXT和Markdown格式的文件'
    return
  }

  // 验证文件大小（10MB）
  if (file.size > 10 * 1024 * 1024) {
    error.value = '文件大小不能超过10MB'
    return
  }

  uploading.value = true
  error.value = null
  successMessage.value = null

  try {
    await uploadDocument(knowledgeBaseId, file)
    successMessage.value = '文档上传成功，正在处理中'
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
    loadDocuments()
  } catch (e: any) {
    error.value = e.message || '上传文档失败'
    console.error('上传文档失败:', e)
  } finally {
    uploading.value = false
    target.value = '' // 清空文件选择
  }
}

// 删除文档
const handleDeleteDocument = async (doc: KnowledgeDocument) => {
  if (!confirm(`确定要删除文档"${doc.fileName}"吗？`)) {
    return
  }

  try {
    await deleteDocument(knowledgeBaseId, doc.id)
    successMessage.value = '删除成功'
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
    loadDocuments()
  } catch (e: any) {
    error.value = e.message || '删除失败'
    console.error('删除文档失败:', e)
  }
}

// 编辑知识库
const handleEdit = () => {
  router.push(`/knowledge-bases/${knowledgeBaseId}/edit`)
}

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    processing: '处理中',
    processed: '已处理',
    failed: '处理失败',
  }
  return statusMap[status] || status
}

onMounted(() => {
  loadKnowledgeBase()
  loadDocuments()
})
</script>

<style scoped>
.knowledge-base-detail {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 80px);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.edit-btn {
  padding: 10px 20px;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.edit-btn:hover {
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
.uploading {
  text-align: center;
  padding: 20px;
  color: #666;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.info-section,
.documents-section {
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

.info-section p {
  color: #666;
  margin-bottom: 10px;
  line-height: 1.6;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.upload-btn {
  padding: 10px 20px;
  background: #42b983;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
  display: inline-block;
}

.upload-btn:hover {
  background: #35a372;
}

.upload-btn:has(input:disabled) {
  background: #ccc;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.documents-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f5f5f5;
}

th,
td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

th {
  font-weight: 600;
  color: #555;
}

td {
  color: #666;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.processing {
  background: #fff3e0;
  color: #f57c00;
}

.status-badge.processed {
  background: #e8f5e9;
  color: #4caf50;
}

.status-badge.failed {
  background: #ffebee;
  color: #f44336;
}

.delete-btn {
  padding: 6px 12px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.delete-btn:hover {
  background: #d32f2f;
}

@media (max-width: 768px) {
  .knowledge-base-detail {
    padding: 20px;
  }

  .header {
    flex-direction: column;
    gap: 15px;
  }

  .section-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .documents-table {
    overflow-x: scroll;
  }

  table {
    font-size: 14px;
  }

  th,
  td {
    padding: 8px;
  }
}
</style>

