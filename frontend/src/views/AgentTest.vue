<template>
  <div class="agent-test">
    <div class="header">
      <h1>测试智能体：{{ agent?.name || '加载中...' }}</h1>
      <router-link to="/agents" class="back-btn">返回列表</router-link>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>

    <div v-if="loadingAgent" class="loading">加载智能体信息中...</div>
    <div v-else-if="agent" class="test-container">
      <div class="agent-info">
        <h3>智能体信息</h3>
        <p><strong>名称：</strong>{{ agent.name }}</p>
        <p v-if="agent.description"><strong>描述：</strong>{{ agent.description }}</p>
        <div v-if="agent.systemPrompt" class="prompt-section">
          <strong>系统提示词：</strong>
          <div class="prompt-content">{{ agent.systemPrompt }}</div>
        </div>
        <div v-if="agent.userPromptTemplate" class="prompt-section">
          <strong>用户提示词模板：</strong>
          <div class="prompt-content">{{ agent.userPromptTemplate }}</div>
        </div>
      </div>

      <div class="test-section">
        <h3>测试对话</h3>
        <div class="chat-container">
          <div v-if="messages.length === 0" class="empty-chat">
            <p>输入问题开始测试智能体</p>
          </div>
          <div v-else class="messages">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              :class="['message', msg.type]"
            >
              <div class="message-header">
                <span class="message-label">{{ msg.type === 'user' ? '用户' : '智能体' }}</span>
                <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
              </div>
              <div
                v-if="msg.type === 'agent' && msg.plugins && msg.plugins.length"
                class="plugin-bubble"
              >
                已调用插件：{{ msg.plugins.join('、') }}
              </div>
              <div class="message-content">{{ msg.content }}</div>
            </div>
          </div>
        </div>

        <div class="input-section">
          <textarea
            v-model="question"
            rows="3"
            placeholder="输入你的问题..."
            :disabled="testing"
            @keydown.ctrl.enter="handleTest"
            @keydown.meta.enter="handleTest"
          ></textarea>
          <div class="input-actions">
            <span class="hint">按 Ctrl+Enter 发送</span>
            <button @click="handleTest" :disabled="testing || !question.trim()" class="send-btn">
              {{ testing ? '测试中...' : '发送' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getAgent, testAgent, type Agent } from '../api/agent'

const router = useRouter()
const route = useRoute()

const agentId = computed(() => Number(route.params.id))

interface Message {
  type: 'user' | 'agent'
  content: string
  timestamp: Date
  plugins?: string[]
}

const agent = ref<Agent | null>(null)
const question = ref('')
const messages = ref<Message[]>([])
const loadingAgent = ref(false)
const testing = ref(false)
const error = ref<string | null>(null)

// 加载智能体详情
const loadAgent = async () => {
  loadingAgent.value = true
  error.value = null
  try {
    agent.value = await getAgent(agentId.value)
  } catch (e: any) {
    error.value = e.message || '加载智能体详情失败'
    console.error('加载智能体详情失败:', e)
  } finally {
    loadingAgent.value = false
  }
}

// 测试智能体
const handleTest = async () => {
  if (!question.value.trim() || testing.value) return

  const userQuestion = question.value.trim()
  question.value = ''

  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: userQuestion,
    timestamp: new Date(),
  })

  testing.value = true
  error.value = null

  try {
    const response = await testAgent(agentId.value, userQuestion)
    
    // 添加智能体回答
    messages.value.push({
      type: 'agent',
      content: response.answer,
      plugins: response.pluginsUsed && response.pluginsUsed.length ? response.pluginsUsed : undefined,
      timestamp: new Date(),
    })
  } catch (e: any) {
    error.value = e.message || '测试失败'
    console.error('测试失败:', e)
  } finally {
    testing.value = false
  }
}

// 格式化时间
const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

onMounted(() => {
  loadAgent()
})
</script>

<style scoped>
.agent-test {
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

.loading {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.test-container {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 30px;
}

.agent-info,
.test-section {
  background: white;
  padding: 25px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.agent-info h3,
.test-section h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #2c3e50;
  font-size: 20px;
}

.agent-info p {
  margin-bottom: 15px;
  color: #666;
  line-height: 1.6;
}

.prompt-section {
  margin-top: 20px;
}

.prompt-content {
  margin-top: 10px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 4px;
  color: #555;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}

.chat-container {
  height: 400px;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow-y: auto;
  margin-bottom: 20px;
  background: #fafafa;
}

.empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.messages {
  padding: 15px;
}

.message {
  margin-bottom: 20px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 12px;
}

.message-label {
  font-weight: 500;
  color: #666;
}

.message-time {
  color: #999;
}

.message-content {
  padding: 12px;
  border-radius: 4px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message.user .message-content {
  background: #e3f2fd;
  color: #1976d2;
  margin-left: 20%;
}

.message.agent .message-content {
  background: #f5f5f5;
  color: #333;
  margin-right: 20%;
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
  margin-top: 20px;
}

.input-section textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
}

.input-section textarea:focus {
  outline: none;
  border-color: #42b983;
}

.input-section textarea:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.hint {
  font-size: 12px;
  color: #999;
}

.send-btn {
  padding: 10px 20px;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.send-btn:hover:not(:disabled) {
  background: #35a372;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

@media (max-width: 968px) {
  .test-container {
    grid-template-columns: 1fr;
  }

  .message.user .message-content,
  .message.agent .message-content {
    margin-left: 0;
    margin-right: 0;
  }
}

@media (max-width: 768px) {
  .agent-test {
    padding: 20px;
  }

  .header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
}
</style>

