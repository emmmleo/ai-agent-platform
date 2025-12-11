<template>
  <div class="knowledge-base-list">
    <div class="header">
      <h1>知识库管理</h1>
      <div class="header-actions">
        <router-link to="/knowledge-bases/new" class="create-btn">创建知识库</router-link>
        <router-link to="/" class="back-btn">返回首页</router-link>
      </div>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="knowledgeBases.length === 0" class="empty-state">
      <p>还没有知识库，<router-link to="/knowledge-bases/new">创建一个</router-link></p>
    </div>
    <div v-else class="knowledge-base-grid">
      <div
        v-for="kb in knowledgeBases"
        :key="kb.id"
        class="knowledge-base-card"
        @click="handleViewDetail(kb.id)"
      >
        <div class="card-header">
          <h3>{{ kb.name }}</h3>
        </div>
        <p class="card-description">{{ kb.description || '暂无描述' }}</p>
        <div class="card-footer">
          <span class="card-date">创建时间：{{ formatDate(kb.createdAt) }}</span>
          <div class="card-actions" @click.stop>
            <button @click.stop="handleEdit(kb)" class="edit-btn">编辑</button>
            <button @click.stop="handleDelete(kb)" class="delete-btn">删除</button>
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
  getKnowledgeBases,
  deleteKnowledgeBase,
  type KnowledgeBase,
} from '../api/knowledgebase'

const router = useRouter()
const knowledgeBases = ref<KnowledgeBase[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// 加载知识库列表
const loadKnowledgeBases = async () => {
  loading.value = true
  error.value = null
  try {
    const list = await getKnowledgeBases()
    knowledgeBases.value = list
  } catch (e: any) {
    error.value = e.message || '加载知识库列表失败'
    console.error('加载知识库列表失败:', e)
  } finally {
    loading.value = false
  }
}

// 查看详情
const handleViewDetail = (id: number) => {
  router.push(`/knowledge-bases/${id}`)
}

// 编辑
const handleEdit = (kb: KnowledgeBase) => {
  router.push(`/knowledge-bases/${kb.id}/edit`)
}

// 删除
const handleDelete = async (kb: KnowledgeBase) => {
  if (!confirm(`确定要删除知识库"${kb.name}"吗？此操作将删除该知识库下的所有文档。`)) {
    return
  }

  try {
    await deleteKnowledgeBase(kb.id)
    successMessage.value = '删除成功'
    setTimeout(() => {
      successMessage.value = null
    }, 3000)
    loadKnowledgeBases()
  } catch (e: any) {
    error.value = e.message || '删除失败'
    console.error('删除知识库失败:', e)
  }
}

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadKnowledgeBases()
})
</script>

<style scoped>
.knowledge-base-list {
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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

.back-btn {
  padding: 12px 24px;
  background: #6c757d;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-size: 16px;
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

.knowledge-base-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.knowledge-base-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.knowledge-base-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.card-header {
  margin-bottom: 12px;
}

.card-header h3 {
  color: #2c3e50;
  margin: 0;
  font-size: 20px;
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
  gap: 10px;
}

.edit-btn,
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

.delete-btn {
  background: #f44336;
  color: white;
}

.delete-btn:hover {
  background: #d32f2f;
}

@media (max-width: 768px) {
  .knowledge-base-list {
    padding: 20px;
  }

  .header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .knowledge-base-grid {
    grid-template-columns: 1fr;
  }
}
</style>

