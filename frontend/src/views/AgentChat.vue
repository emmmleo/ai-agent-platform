<template>
  <div class="agent-chat">
    <div class="header">
      <router-link to="/agents" class="back-link">← 返回列表</router-link>
      <h1 v-if="agent">{{ agent.name }}</h1>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="agent" class="chat-container">
      <div class="agent-info">
        <h3>智能体信息</h3>
        <p><strong>名称：</strong>{{ agent.name }}</p>
        <p v-if="agent.description"><strong>描述：</strong>{{ agent.description }}</p>
        <p v-if="agent.systemPrompt"><strong>系统提示词：</strong>{{ truncateText(agent.systemPrompt, 100) }}</p>
      </div>

      <div class="chat-section">
        <div class="messages-container">
          <div v-if="messages.length === 0" class="empty-chat">
            <p>开始与智能体对话吧！</p>
          </div>
          <div v-else class="messages">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              :class="['message', msg.type]"
            >
              <div class="message-header">
                <span class="message-role">{{ msg.type === 'user' ? '你' : '智能体' }}</span>
                <span v-if="msg.source" class="message-source">{{ getSourceText(msg.source) }}</span>
              </div>
              <div
                v-if="msg.type === 'assistant' && msg.plugins && msg.plugins.length"
                class="plugin-bubble"
              >
                调用插件：{{ msg.plugins.join('、') }}
              </div>
              <div class="message-content">{{ msg.content }}</div>
            </div>
          </div>
        </div>

        <div class="input-section">
          <textarea
            v-model="inputQuestion"
            @keydown.enter.exact.prevent="handleSend"
            @keydown.shift.enter.exact="inputQuestion += '\n'"
            rows="3"
            placeholder="输入你的问题..."
            class="input-textarea"
            :disabled="sending"
          ></textarea>
          <button @click="handleSend" :disabled="sending || !inputQuestion.trim()" class="send-btn">
            {{ sending ? '发送中...' : '发送' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getAgent, chatWithAgent, type Agent } from '../api/agent'

const router = useRouter()
const route = useRoute()

const agentId = Number(route.params.id)
const agent = ref<Agent | null>(null)
const messages = ref<Array<{ type: string; content: string; source?: string; plugins?: string[] }>>([])
const inputQuestion = ref('')
const loading = ref(false)
const sending = ref(false)
const error = ref<string | null>(null)

// 加载智能体详情
const loadAgent = async () => {
  loading.value = true
  error.value = null
  try {
    const a = await getAgent(agentId)
    agent.value = a

    // 检查智能体是否已发布
    if (a.status !== 'published') {
      error.value = '智能体未发布，无法使用。请先发布智能体。'
    }
  } catch (e: any) {
    error.value = e.message || '加载智能体详情失败'
    console.error('加载智能体详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 发送消息
const handleSend = async () => {
  if (!inputQuestion.value.trim() || sending.value) {
    return
  }

  const question = inputQuestion.value.trim()
  inputQuestion.value = ''

  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: question,
  })

  sending.value = true
  error.value = null

  try {
    const response = await chatWithAgent(agentId, question)
    
    // 添加智能体回复
    messages.value.push({
      type: 'assistant',
      content: response.answer,
      source: response.source,
      plugins: response.pluginsUsed && response.pluginsUsed.length ? response.pluginsUsed : undefined,
    })

    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (e: any) {
    error.value = e.message || '对话失败'
    console.error('对话失败:', e)
    
    // 添加错误消息
    messages.value.push({
      type: 'assistant',
      content: '抱歉，发生了错误：' + (e.message || '未知错误'),
    })
  } finally {
    sending.value = false
  }
}

// 滚动到底部
const scrollToBottom = () => {
  const container = document.querySelector('.messages-container')
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

// 截断文本
const truncateText = (text: string, maxLength: number) => {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

// 获取来源文本
const getSourceText = (source: string) => {
  const sourceMap: Record<string, string> = {
    direct: '直接回答',
    rag: '知识库检索',
    workflow: '工作流执行',
  }
  return sourceMap[source] || source
}

onMounted(() => {
  loadAgent()
})
</script>

<style scoped>
.agent-chat {
  padding: 20px;
  height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  margin-bottom: 20px;
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
  font-size: 24px;
}

.error-message {
  background: #ffebee;
  color: #f44336;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #f44336;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}

.chat-container {
  display: flex;
  gap: 20px;
  flex: 1;
  overflow: hidden;
}

.agent-info {
  width: 300px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  height: fit-content;
}

.agent-info h3 {
  color: #2c3e50;
  margin: 0 0 15px 0;
  font-size: 18px;
}

.agent-info p {
  color: #666;
  margin-bottom: 10px;
  line-height: 1.6;
  font-size: 14px;
}

.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f9f9f9;
}

.empty-chat {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message {
  max-width: 80%;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  align-self: flex-end;
}

.message.assistant {
  align-self: flex-start;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.message-role {
  font-weight: 500;
  font-size: 12px;
  color: #666;
}

.message-source {
  font-size: 11px;
  color: #999;
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
}

.message-content {
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.message.user .message-content {
  background: #42b983;
  color: white;
  border-bottom-right-radius: 2px;
}

.message.assistant .message-content {
  background: white;
  color: #2c3e50;
  border: 1px solid #e0e0e0;
  border-bottom-left-radius: 2px;
}

.plugin-bubble {
  display: inline-block;
  background: #e8f5e9;
  border: 1px solid #c8e6c9;
  color: #2c3e50;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  margin-bottom: 6px;
}

.input-section {
  padding: 20px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.input-textarea {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  transition: border-color 0.3s;
}

.input-textarea:focus {
  outline: none;
  border-color: #42b983;
}

.input-textarea:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.send-btn {
  padding: 12px 24px;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
  white-space: nowrap;
}

.send-btn:hover:not(:disabled) {
  background: #35a372;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .agent-chat {
    padding: 10px;
  }

  .chat-container {
    flex-direction: column;
  }

  .agent-info {
    width: 100%;
  }

  .message {
    max-width: 90%;
  }
}
</style>

