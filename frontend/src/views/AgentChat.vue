<template>
  <div class="agent-chat" @click="closeSessionMenu">
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

        <div class="sessions-panel">
          <div class="sessions-header">
            <h4>会话列表</h4>
            <button class="new-session-btn" @click.stop="handleCreateSession" :disabled="sending">+ 新建</button>
          </div>
          <div v-if="sessions.length === 0" class="empty-sessions">暂无会话</div>
          <ul v-else class="session-list">
            <li
              v-for="session in sessions"
              :key="session.id"
              :class="['session-item', { active: session.id === selectedSessionId }]"
              @click.stop="handleSelectSession(session.id)"
            >
              <span class="session-title">{{ session.title }}</span>
              <button class="session-menu-btn" @click.stop="toggleSessionMenu(session.id)">...</button>
              <div
                v-if="sessionMenuOpen === session.id"
                class="session-menu-popover"
                @click.stop
              >
                <button class="session-menu-item" @click="handleDeleteSession(session.id)">删除会话</button>
              </div>
            </li>
          </ul>
        </div>
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
                <span class="bubble-icon">🔧</span> 已调用插件：{{ msg.plugins.join('、') }}
              </div>
              
              <!-- RAG 引用展示 -->
              <div 
                v-if="msg.type === 'assistant' && msg.ragContext && msg.ragContext.success" 
                class="rag-bubble-container"
              >
                <div class="rag-bubble">
                  <span class="bubble-icon">📚</span> 
                  已检索知识库：获取了 {{ msg.ragContext.references.length }} 个相关片段
                  <span class="rag-status-badge" :class="{ success: msg.ragContext.success }">
                    {{ msg.ragContext.success ? '✓' : '✗' }}
                  </span>
                </div>
                <details class="rag-details">
                  <summary class="rag-summary">
                    <span class="summary-icon">▶</span>
                    查看引用详情
                  </summary>
                  <div class="references-list">
                    <div v-for="(ref, refIndex) in msg.ragContext.references" :key="refIndex" class="reference-item">
                      <div class="reference-header">
                        <span class="reference-index">{{ refIndex + 1 }}</span>
                        <span class="reference-meta">
                          <span class="reference-score">{{ (ref.score * 100).toFixed(1) }}% 匹配</span>
                          <span class="reference-separator">•</span>
                          <span class="reference-id">文档 #{{ ref.documentId }}</span>
                          <span class="reference-separator">•</span>
                          <span class="reference-kb">库 #{{ ref.knowledgeBaseId }}</span>
                        </span>
                      </div>
                      <div class="reference-content">{{ ref.content }}</div>
                    </div>
                  </div>
                </details>
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
import {
  getAgent,
  getAgentConversation,
  getAgentSessions,
  createAgentSession,
  deleteAgentSession,
  chatWithAgent,
  type Agent,
  type ChatSession,
  type RagContext
} from '../api/agent'

const router = useRouter()
const route = useRoute()

const agentId = Number(route.params.id)
const agent = ref<Agent | null>(null)
const messages = ref<Array<{ 
  type: string; 
  content: string; 
  source?: string; 
  plugins?: string[];
  ragContext?: RagContext;
}>>([])
const sessions = ref<ChatSession[]>([])
const selectedSessionId = ref<number | null>(null)
const sessionMenuOpen = ref<number | null>(null)
const inputQuestion = ref('')
const loading = ref(false)
const sending = ref(false)
const error = ref<string | null>(null)

// 加载智能体详情
const loadAgent = async () => {
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
  }
}

const loadConversationHistory = async (sessionId?: number | null) => {
  const targetId = sessionId ?? selectedSessionId.value
  if (!targetId) {
    messages.value = []
    return
  }
  try {
    const history = await getAgentConversation(agentId, targetId)
    const restored = (history.messages || [])
      .filter((msg) => msg.type === 'user' || msg.type === 'assistant')
      .map((msg) => ({
        type: msg.type,
        content: msg.content,
        plugins: msg.plugins && msg.plugins.length ? [...msg.plugins] : undefined,
        ragContext: msg.ragContext
      }))
    messages.value = restored
    selectedSessionId.value = history.sessionId ?? targetId
    await nextTick()
    scrollToBottom()
  } catch (e: any) {
    console.error('加载对话历史失败:', e)
  }
}

const loadSessions = async (preferredSessionId?: number | null, reloadMessages = true) => {
  try {
    const result = await getAgentSessions(agentId)
    sessions.value = result.sessions || []
    if (sessions.value.length === 0) {
      selectedSessionId.value = null
      messages.value = []
      return
    }
    let targetId = preferredSessionId ?? selectedSessionId.value ?? sessions.value[0].id
    if (!sessions.value.some((session) => session.id === targetId)) {
      targetId = sessions.value[0].id
    }
    selectedSessionId.value = targetId
    if (reloadMessages) {
      await loadConversationHistory(targetId)
    }
  } catch (e: any) {
    console.error('加载会话列表失败:', e)
  }
}

const handleSelectSession = async (sessionId: number) => {
  if (selectedSessionId.value === sessionId) {
    sessionMenuOpen.value = null
    return
  }
  selectedSessionId.value = sessionId
  sessionMenuOpen.value = null
  await loadConversationHistory(sessionId)
}

const handleCreateSession = async () => {
  try {
    const session = await createAgentSession(agentId)
    await loadSessions(session.id)
    sessionMenuOpen.value = null
  } catch (e: any) {
    error.value = e.message || '创建会话失败'
    console.error('创建会话失败:', e)
  }
}

const handleDeleteSession = async (sessionId: number) => {
  if (!window.confirm('确认删除该会话吗？')) {
    return
  }
  try {
    await deleteAgentSession(agentId, sessionId)
    sessionMenuOpen.value = null
    if (selectedSessionId.value === sessionId) {
      selectedSessionId.value = null
    }
    await loadSessions(selectedSessionId.value)
  } catch (e: any) {
    error.value = e.message || '删除会话失败'
    console.error('删除会话失败:', e)
  }
}

const ensureSessionSelected = async (): Promise<number> => {
  if (selectedSessionId.value) {
    return selectedSessionId.value
  }
  try {
    const session = await createAgentSession(agentId)
    await loadSessions(session.id)
    return session.id
  } catch (e: any) {
    error.value = e.message || '创建会话失败'
    console.error('创建会话失败:', e)
    throw e
  }
}

const toggleSessionMenu = (sessionId: number) => {
  sessionMenuOpen.value = sessionMenuOpen.value === sessionId ? null : sessionId
}

const closeSessionMenu = () => {
  sessionMenuOpen.value = null
}

const initializeChat = async () => {
  loading.value = true
  error.value = null
  try {
    await loadAgent()
    if (!error.value) {
      await loadSessions()
    }
  } finally {
    loading.value = false
  }
}

// 发送消息
const handleSend = async () => {
  if (!inputQuestion.value.trim() || sending.value) {
    return
  }

  let sessionId = selectedSessionId.value
  if (!sessionId) {
    sessionId = await ensureSessionSelected()
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
    const response = await chatWithAgent(agentId, question, sessionId)
    
    // 添加智能体回复
    messages.value.push({
      type: 'assistant',
      content: response.answer,
      source: response.source,
      plugins: response.pluginsUsed && response.pluginsUsed.length ? response.pluginsUsed : undefined,
      ragContext: response.ragContext
    })

    if (response.sessionId) {
      selectedSessionId.value = response.sessionId
      sessionId = response.sessionId
    }

    await loadSessions(sessionId, false)
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
  initializeChat()
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

.sessions-panel {
  margin-top: 20px;
  border-top: 1px solid #f0f0f0;
  padding-top: 15px;
}

.sessions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.sessions-header h4 {
  font-size: 14px;
  color: #2c3e50;
  margin: 0;
}

.new-session-btn {
  border: none;
  background: #f0f7f4;
  color: #42b983;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.new-session-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.session-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-item {
  position: relative;
  padding: 8px 10px;
  border: 1px solid #eee;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.session-item.active {
  border-color: #42b983;
  background: #f4fffa;
}

.session-title {
  flex: 1;
  color: #2c3e50;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 10px;
}

.session-menu-btn {
  border: none;
  background: transparent;
  color: #666;
  font-size: 16px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .session-menu-btn {
  opacity: 1;
}

.session-menu-popover {
  position: absolute;
  top: 34px;
  right: 10px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 5;
}

.session-menu-item {
  border: none;
  background: transparent;
  color: #f44336;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
}

.empty-sessions {
  font-size: 12px;
  color: #999;
  padding: 8px 0;
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
  background: linear-gradient(135deg, #e8f5e9 0%, #f1f8f4 100%);
  border: 1px solid #a5d6a7;
  color: #2e7d32;
  font-size: 12px;
  padding: 6px 14px;
  border-radius: 999px;
  margin-bottom: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  font-weight: 500;
}

.bubble-icon {
  margin-right: 4px;
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

/* RAG 知识库调用样式 */
.rag-bubble-container {
  margin-bottom: 10px;
}

.rag-bubble {
  display: inline-block;
  background: linear-gradient(135deg, #e3f2fd 0%, #f1f8ff 100%);
  border: 1px solid #90caf9;
  color: #1565c0;
  font-size: 12px;
  padding: 6px 14px;
  border-radius: 999px;
  margin-bottom: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.rag-status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #e0e0e0;
  color: #757575;
  font-size: 11px;
  margin-left: 4px;
}

.rag-status-badge.success {
  background: #4caf50;
  color: white;
}

.rag-details {
  margin-top: 6px;
}

.rag-summary {
  padding: 8px 12px;
  cursor: pointer;
  font-size: 11px;
  color: #1565c0;
  user-select: none;
  background: #f5f9ff;
  border: 1px solid #e3f2fd;
  border-radius: 6px;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.rag-summary:hover {
  background: #e3f2fd;
  border-color: #90caf9;
}

.rag-details[open] .rag-summary {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  margin-bottom: 0;
  background: #e3f2fd;
}

.summary-icon {
  display: inline-block;
  font-size: 10px;
  transition: transform 0.2s ease;
}

.rag-details[open] .summary-icon {
  transform: rotate(90deg);
}

.references-list {
  padding: 12px;
  background: #f5f9ff;
  border: 1px solid #e3f2fd;
  border-top: none;
  border-bottom-left-radius: 6px;
  border-bottom-right-radius: 6px;
}

.reference-item {
  margin-bottom: 12px;
  background: white;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  transition: box-shadow 0.2s ease;
}

.reference-item:last-child {
  margin-bottom: 0;
}

.reference-item:hover {
  box-shadow: 0 2px 6px rgba(0,0,0,0.12);
}

.reference-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 11px;
}

.reference-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  background: linear-gradient(135deg, #1976d2 0%, #42a5f5 100%);
  color: white;
  border-radius: 50%;
  font-weight: 600;
  font-size: 11px;
}

.reference-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #757575;
  flex: 1;
}

.reference-score {
  color: #1976d2;
  font-weight: 600;
  background: #e3f2fd;
  padding: 2px 8px;
  border-radius: 4px;
}

.reference-separator {
  color: #bdbdbd;
}

.reference-id,
.reference-kb {
  font-size: 10px;
  color: #9e9e9e;
}

.reference-content {
  background: #fafafa;
  padding: 10px;
  border-radius: 6px;
  border-left: 3px solid #1976d2;
  color: #424242;
  line-height: 1.6;
  white-space: pre-wrap;
  font-size: 12px;
}
</style>

