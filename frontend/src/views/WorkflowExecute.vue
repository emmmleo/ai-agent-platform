<template>
  <div class="workflow-chat">
    <div class="chat-header">
      <router-link to="/workflows" class="back-link">← 返回列表</router-link>
      <h1 v-if="workflow">{{ workflow.name }}</h1>
    </div>

    <div class="chat-container">
      <!-- Chat History -->
      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-chat">
          <div class="empty-icon">💬</div>
          <p>开始与工作流对话吧</p>
        </div>
        
        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
          <div class="message-avatar">
            {{ msg.role === 'user' ? '👤' : '🤖' }}
          </div>
          <div class="message-content">
            <div v-if="msg.type === 'text'" class="text-content">{{ msg.content }}</div>
            <div v-else-if="msg.type === 'error'" class="error-content">{{ msg.content }}</div>
            <div v-else-if="msg.type === 'json'" class="json-content">
              <pre>{{ JSON.stringify(msg.content, null, 2) }}</pre>
            </div>
            <div class="message-footer">
              <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
              <button 
                v-if="msg.role === 'assistant' && msg.executionId" 
                class="detail-btn"
                @click="handleViewDetail(msg.executionId)"
              >
                查看详情
              </button>
            </div>
          </div>
        </div>

        <div v-if="executing" class="message assistant">
          <div class="message-avatar">🤖</div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="chat-input-area">
        <!-- Dynamic Form Inputs -->
        <div v-if="startNode && startNode.data && startNode.data.inputFields && startNode.data.inputFields.length > 0" class="dynamic-form-inputs">
          <div v-for="(field, index) in startNode.data.inputFields" :key="index" class="input-group">
            <input 
              v-if="field.type === 'text' || field.type === 'number'"
              v-model="formModel[field.variable]"
              :type="field.type"
              :placeholder="field.label || field.variable"
              class="chat-input"
              @keyup.enter="handleSend"
            />
            <textarea
              v-else-if="field.type === 'paragraph'"
              v-model="formModel[field.variable]"
              :placeholder="field.label || field.variable"
              class="chat-input"
              rows="1"
              @keyup.enter.ctrl="handleSend"
            ></textarea>
            <select
              v-else-if="field.type === 'select'"
              v-model="formModel[field.variable]"
              class="chat-input"
            >
              <option value="" disabled selected>{{ field.label || field.variable }}</option>
              <option v-for="opt in parseOptions(field.options)" :key="opt" :value="opt">{{ opt }}</option>
            </select>
          </div>
        </div>
        
        <!-- Default JSON Input -->
        <div v-else class="json-input-wrapper">
          <textarea
            v-model="inputMessage"
            placeholder="输入消息..."
            class="chat-input"
            rows="1"
            @keyup.enter="handleSend"
          ></textarea>
        </div>

        <button @click="handleSend" :disabled="executing" class="send-btn">
          发送
        </button>
      </div>
    </div>

    <!-- Execution Detail Modal -->
    <div v-if="showExecutionDetail" class="modal-overlay" @click.self="showExecutionDetail = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>执行详情 #{{ currentExecutionId }}</h3>
          <button class="close-btn" @click="showExecutionDetail = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="executionDetailLoading" class="loading">加载中...</div>
          <div v-else-if="executionDetail" class="detail-content">
            <div class="detail-section">
              <h4>基本信息</h4>
              <p><strong>状态:</strong> {{ getStatusText(executionDetail.status) }}</p>
              <p><strong>开始时间:</strong> {{ formatDate(executionDetail.startedAt) }}</p>
              <p><strong>完成时间:</strong> {{ formatDate(executionDetail.completedAt) }}</p>
            </div>
            
            <div class="detail-section">
              <h4>输入参数</h4>
              <pre>{{ JSON.stringify(executionDetail.inputParams, null, 2) }}</pre>
            </div>

            <div class="detail-section">
              <h4>输出结果</h4>
              <pre>{{ JSON.stringify(executionDetail.outputResult, null, 2) }}</pre>
            </div>

            <div v-if="executionDetail.errorMessage" class="detail-section error">
              <h4>错误信息</h4>
              <p>{{ executionDetail.errorMessage }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// @ts-ignore
import { ref, onMounted, computed, watch, nextTick } from 'vue'
// @ts-ignore
import { useRouter, useRoute } from 'vue-router'
import {
  getWorkflow,
  executeWorkflow,
  getWorkflowExecution,
  type Workflow,
  type WorkflowNode
} from '../api/workflow'

interface ChatMessage {
  role: 'user' | 'assistant'
  type: 'text' | 'json' | 'error'
  content: any
  timestamp: number
  executionId?: number // Add executionId to link to details
}

const router = useRouter()
const route = useRoute()

const workflowId = Number(route.params.id)
const workflow = ref<Workflow | null>(null)
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const formModel = ref<Record<string, any>>({})
const executing = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const showExecutionDetail = ref(false)
const currentExecutionId = ref<number | null>(null)
const executionDetail = ref<any>(null)
const executionDetailLoading = ref(false)

// 获取开始节点
const startNode = computed(() => {
  if (!workflow.value || !workflow.value.definition || !workflow.value.definition.nodes) return null
  return workflow.value.definition.nodes.find((n: WorkflowNode) => n.type === 'start')
})

// 解析选项
const parseOptions = (optionsStr: string) => {
  if (!optionsStr) return []
  return optionsStr.split('\n').map(s => s.trim()).filter(s => s)
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 监听工作流加载，初始化表单
watch(startNode, (node) => {
  if (node && node.data && node.data.inputFields) {
    const initialData: Record<string, any> = {}
    node.data.inputFields.forEach((field: any) => {
      if (field.variable) {
        initialData[field.variable] = ''
      }
    })
    formModel.value = initialData
  }
})

// 加载工作流详情
const loadWorkflow = async () => {
  try {
    const wf = await getWorkflow(workflowId)
    workflow.value = wf
  } catch (e: any) {
    messages.value.push({
      role: 'assistant',
      type: 'error',
      content: '加载工作流失败: ' + e.message,
      timestamp: Date.now()
    })
  }
}

// 发送消息
const handleSend = async () => {
  if (executing.value) return

  let inputParams: Record<string, any> = {}
  let displayContent = ''

  // 处理表单输入
  if (startNode.value && startNode.value.data && startNode.value.data.inputFields && startNode.value.data.inputFields.length > 0) {
    // 验证必填项
    for (const field of startNode.value.data.inputFields) {
      if (field.required && !formModel.value[field.variable]) {
        alert(`请输入 ${field.label || field.variable}`)
        return
      }
    }
    inputParams = { ...formModel.value }
    displayContent = Object.entries(inputParams)
      .map(([k, v]) => `${k}: ${v}`)
      .join('\n')
    
    // 清空表单（可选，视需求而定，这里不清空方便连续测试）
    // formModel.value = {} 
  } else {
    // 默认文本输入作为 question 参数（兼容常见 Agent 模式）
    // 或者尝试解析 JSON
    const text = inputMessage.value.trim()
    if (!text) return

    try {
      inputParams = JSON.parse(text)
      displayContent = text
    } catch {
      // 默认为 question 参数
      inputParams = { question: text }
      displayContent = text
    }
    inputMessage.value = ''
  }

  // 添加用户消息
  messages.value.push({
    role: 'user',
    type: 'text',
    content: displayContent,
    timestamp: Date.now()
  })
  scrollToBottom()

  executing.value = true

  try {
    const result = await executeWorkflow(workflowId, { inputParams })
    
    // Poll for completion
    let execution = result
    let attempts = 0
    const maxAttempts = 60 // 60 seconds timeout
    
    while (['pending', 'running'].includes(execution.status) && attempts < maxAttempts) {
        await new Promise(resolve => setTimeout(resolve, 1000))
        execution = await getWorkflowExecution(workflowId, result.id)
        attempts++
    }
    
    if (execution.status === 'failed') {
        throw new Error(execution.errorMessage || '执行失败')
    }
    
    // 解析输出结果
    let outputContent: any = execution.outputResult
    let msgType: 'text' | 'json' = 'json'

    // 尝试提取文本内容
    if (outputContent) {
      // 1. 优先查找 'output' 字段
      if (outputContent.output && typeof outputContent.output === 'string') {
        outputContent = outputContent.output
        msgType = 'text'
      } 
      // 2. 查找 'content' 字段
      else if (outputContent.content && typeof outputContent.content === 'string') {
        outputContent = outputContent.content
        msgType = 'text'
      }
      // 3. 查找 'result' 字段
      else if (outputContent.result && typeof outputContent.result === 'string') {
        outputContent = outputContent.result
        msgType = 'text'
      }
      // 4. 如果只有一个键值对且值是字符串
      else if (typeof outputContent === 'object' && Object.keys(outputContent).length === 1) {
        const val = Object.values(outputContent)[0]
        if (typeof val === 'string') {
          outputContent = val
          msgType = 'text'
        }
      }
      // 5. 如果是直接回复节点，可能直接返回了字符串（虽然通常是JSON）
      else if (typeof outputContent === 'string') {
          // Check if it's a JSON string that needs parsing
          try {
              const parsed = JSON.parse(outputContent);
              if (typeof parsed === 'object') {
                  // It was a JSON string, keep as object for further inspection or display as JSON
                  // But if we want to display text, we might need to extract from parsed
                  // But if we want to display text, we might need to extract from parsed
                  if (parsed.content || parsed.output) {
                      outputContent = parsed.content || parsed.output;
                      msgType = 'text';
                  } else {
                      // Just display the object as JSON
                      outputContent = parsed;
                      msgType = 'json';
                  }
              } else {
                  // It was a simple string (or number/bool)
                  msgType = 'text';
              }
          } catch (e) {
              // Not a JSON string, treat as plain text
              msgType = 'text';
          }
      }
      // 6. Special handling for "Direct Reply" node which might return specific structure
      // The backend "reply" node returns: result.put("content", processedContent); result.put("output", processedContent);
      // So cases 1 and 2 should cover it.
      // However, if the workflow execution result is a map of node results, we need to find the last node's output.
      // Currently backend returns `execution.setOutputResult(objectMapper.writeValueAsString(result));`
      // where `result` is `nodeResults` map from `executeWorkflowDefinition`.
      // So `outputResult` is actually a Map<NodeId, NodeResult>.
      // We need to find the "reply" node or the last executed node.
      
      if (typeof outputContent === 'object' && !outputContent.output && !outputContent.content) {
          // It might be a map of node results. Let's try to find a "reply" node output.
          // Or find the end node output.
          
          // Strategy: Look for any value that has "nodeType": "reply"
          let replyNodeResult: any = null;
          // We need to find the LAST executed reply node.
          // Since keys are unordered, we might need to rely on timestamps if available, or just iterate.
          // But wait, if there are multiple branches, only one path is executed.
          // So there should be only one reply node in the active path?
          // Or maybe multiple if sequential.
          
          // Let's collect all reply nodes
          const replyNodes: any[] = [];
          for (const key in outputContent) {
              const nodeRes = outputContent[key];
              if (nodeRes && nodeRes.nodeType === 'reply') {
                  replyNodes.push(nodeRes);
              }
          }
          
          // If we found reply nodes, pick the last one (or the only one)
          // Since we don't have execution order here easily, let's just pick one.
          // Ideally we should pick the one that is NOT empty.
          if (replyNodes.length > 0) {
              // Prefer the one with content
              // replyNodeResult = replyNodes.find(n => n.content || n.output) || replyNodes[0];
              // Actually, we should probably just take the last one found if we assume order, 
              // or maybe the one that is NOT empty is a good heuristic.
              // But if the user explicitly returns empty string, that's valid too.
              // Let's try to find the one that corresponds to the end of the flow?
              // Without graph info here, it's hard.
              // Let's stick to "has content" for now, but if multiple have content, maybe the last one?
              
              // Filter out nodes that might be skipped/not executed? 
              // Backend only returns executed nodes.
              
              // If there are multiple reply nodes executed (e.g. sequential), we probably want the last one.
              // Since we can't guarantee order of keys, this is tricky.
              // But usually only one reply node is terminal.
              
              // Let's just pick the first one found for now, as usually there's only one active reply node.
              // Or if we have multiple, maybe join them?
              
              // Reverting to simple logic: just pick the first one found.
              // The previous logic of "find(n => n.content || n.output)" is actually good because 
              // it filters out potentially empty/dummy reply nodes if any.
              // But user says "should choose correctly executed workflow's direct reply, not just one with content".
              // This implies that maybe an "empty" reply was the correct one?
              // Or maybe there's a reply node from a previous run? No, this is a fresh execution result.
              
              // If the user means "don't just pick any random node with content, pick the RIGHT one",
              // and since we only have executed nodes, ANY reply node in this list IS a "correctly executed" one.
              // The issue might be if we have multiple reply nodes in the map.
              
              // Let's try to be more robust:
              // If there is only 1 reply node, use it.
              // If there are multiple, use the one with content.
              // If all have content, use the last one (if we could determine order).
              
              if (replyNodes.length === 1) {
                  replyNodeResult = replyNodes[0];
              } else {
                  // If multiple, prefer the one with non-empty content
                  const withContent = replyNodes.filter(n => (n.content !== undefined && n.content !== '') || (n.output !== undefined && n.output !== ''));
                  if (withContent.length > 0) {
                      replyNodeResult = withContent[withContent.length - 1]; // Pick last one found
                  } else {
                      replyNodeResult = replyNodes[replyNodes.length - 1];
                  }
              }
          }
          
          if (replyNodeResult) {
              if (replyNodeResult.content !== undefined) {
                  outputContent = replyNodeResult.content;
                  msgType = 'text';
              } else if (replyNodeResult.output !== undefined) {
                  outputContent = replyNodeResult.output;
                  msgType = 'text';
              }
          } else {
              // If no reply node, maybe look for LLM node
              let llmNodeResult: any = null;
              for (const key in outputContent) {
                  const nodeRes = outputContent[key];
                  if (nodeRes && (nodeRes.nodeType === 'llm' || nodeRes.nodeType === 'agent')) {
                      llmNodeResult = nodeRes;
                  }
              }
              
              if (llmNodeResult) {
                   if (llmNodeResult.content) {
                      outputContent = llmNodeResult.content;
                      msgType = 'text';
                  } else if (llmNodeResult.output) {
                      outputContent = llmNodeResult.output;
                      msgType = 'text';
                  }
              }
          }
      }
    }

    messages.value.push({
      role: 'assistant',
      type: msgType,
      content: outputContent,
      timestamp: Date.now(),
      executionId: result.id // Store execution ID
    })
  } catch (e: any) {
    messages.value.push({
      role: 'assistant',
      type: 'error',
      content: e.message || '执行失败',
      timestamp: Date.now()
    })
  } finally {
    executing.value = false
    scrollToBottom()
  }
}

// 查看执行详情
const handleViewDetail = async (executionId: number) => {
  if (!executionId) return
  currentExecutionId.value = executionId
  showExecutionDetail.value = true
  executionDetailLoading.value = true
  executionDetail.value = null
  
  try {
    const detail = await getWorkflowExecution(workflowId, executionId)
    executionDetail.value = detail
  } catch (e: any) {
    console.error('加载执行详情失败:', e)
    alert('加载执行详情失败: ' + e.message)
  } finally {
    executionDetailLoading.value = false
  }
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
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  loadWorkflow()
})
</script>

<style scoped>
.workflow-chat {
  height: calc(100vh - 40px); /* Adjust based on layout */
  display: flex;
  flex-direction: column;
  background: #f0f2f5; /* Lighter background */
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.chat-header {
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  z-index: 10;
}

.back-link {
  color: #666;
  text-decoration: none;
  font-size: 14px;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}

.back-link:hover {
  color: #1890ff;
}

.chat-header h1 {
  margin: 0;
  font-size: 18px;
  color: #1f1f1f;
  font-weight: 600;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  max-width: 900px; /* Wider container */
  margin: 20px auto; /* Add margin */
  width: 95%;
  background: white;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06); /* Softer shadow */
  border-radius: 12px; /* Rounded corners */
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: #fff;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.message {
  display: flex;
  gap: 16px;
  max-width: 85%;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
}

.message.user .message-avatar {
  background: linear-gradient(135deg, #1890ff, #096dd9);
  color: white;
}

.message.assistant .message-avatar {
  background: linear-gradient(135deg, #52c41a, #389e0d);
  color: white;
}

.message-content {
  background: #f5f5f5;
  padding: 14px 18px;
  border-radius: 16px;
  position: relative;
  min-width: 80px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  line-height: 1.6;
  font-size: 15px;
}

.message.user .message-content {
  background: #e6f7ff;
  color: #1f1f1f;
  border-bottom-right-radius: 4px;
  border: 1px solid #bae7ff;
}

.message.assistant .message-content {
  background: white;
  border: 1px solid #f0f0f0;
  border-bottom-left-radius: 4px;
}

.text-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.json-content pre {
  margin: 0;
  font-size: 13px;
  background: #fafafa;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  border: 1px solid #eee;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
}

.error-content {
  color: #ff4d4f;
  background: #fff1f0;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #ffccc7;
}

.message-time {
  font-size: 11px;
  color: #bfbfbf;
  margin-top: 6px;
  text-align: right;
}

.message.user .message-time {
  text-align: left;
}

.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 6px 4px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #bfbfbf;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.chat-input-area {
  padding: 24px;
  background: white;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 16px;
  align-items: flex-end;
}

.dynamic-form-inputs {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.input-group {
  flex: 1;
  min-width: 240px;
}

.chat-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 24px;
  font-size: 15px;
  outline: none;
  transition: all 0.3s;
  resize: none;
  background: #fafafa;
}

.chat-input:focus {
  border-color: #1890ff;
  background: white;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.send-btn {
  padding: 0 28px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 24px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s;
  height: 46px;
  font-size: 15px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.send-btn:hover:not(:disabled) {
  background: #40a9ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.send-btn:active:not(:disabled) {
  transform: translateY(0);
}

.send-btn:disabled {
  background: #d9d9d9;
  color: #bfbfbf;
  cursor: not-allowed;
  box-shadow: none;
}

/* Scrollbar styling */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #e8e8e8;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #d9d9d9;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(0,0,0,0.05);
}

.detail-btn {
  font-size: 12px;
  color: #1890ff;
  background: rgba(24, 144, 255, 0.1);
  border: 1px solid rgba(24, 144, 255, 0.2);
  border-radius: 12px;
  cursor: pointer;
  padding: 4px 12px;
  text-decoration: none;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.detail-btn:hover {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(24, 144, 255, 0.25);
}

.detail-btn:active {
  transform: translateY(0);
}

.detail-btn::before {
  content: '📄';
  font-size: 12px;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 700px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease-out;
  overflow: hidden;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #1f1f1f;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: color 0.2s;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.close-btn:hover {
  color: #ff4d4f;
  background: rgba(0,0,0,0.05);
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 15px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.detail-section h4 {
  margin: 0 0 16px 0;
  font-size: 15px;
  color: #1f1f1f;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-section h4::before {
  content: '';
  display: block;
  width: 4px;
  height: 16px;
  background: #1890ff;
  border-radius: 2px;
}

.detail-section p {
  margin: 8px 0;
  color: #595959;
  font-size: 14px;
  line-height: 1.6;
}

.detail-section strong {
  color: #1f1f1f;
  font-weight: 500;
  margin-right: 8px;
}

.detail-section pre {
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0;
  font-size: 13px;
  color: #595959;
  border: 1px solid #f0f0f0;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  line-height: 1.5;
}

.detail-section.error {
  border-color: #ffccc7;
  background: #fff2f0;
}

.detail-section.error h4 {
  color: #cf1322;
  border-bottom-color: #ffccc7;
}

.detail-section.error h4::before {
  background: #ff4d4f;
}

.detail-section.error p {
  color: #cf1322;
}
</style>