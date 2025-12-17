<template>
  <div class="workflow-editor">
    <div class="header">
      <h1>{{ isEdit ? '编辑工作流' : '创建工作流' }}</h1>
      <div class="header-actions">
        <button @click="handleSave" :disabled="loading" class="save-btn">保存</button>
        <router-link to="/workflows" class="back-btn">返回列表</router-link>
      </div>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

    <div class="editor-container">
      <div class="editor-sidebar">
        <h3>节点类型</h3>
        <div class="node-types">
          <div
            v-for="nodeType in nodeTypes"
            :key="nodeType.type"
            class="node-type-item"
            draggable="true"
            @dragstart="handleDragStart($event, nodeType)"
          >
            <span class="node-icon">{{ nodeType.icon }}</span>
            <span>{{ nodeType.name }}</span>
          </div>
        </div>
      </div>

      <div class="editor-main">
        <div class="editor-toolbar">
          <input
            v-model="workflowName"
            type="text"
            placeholder="工作流名称"
            class="workflow-name-input"
            :disabled="loading"
          />
          <button @click="handleAddStartNode" class="toolbar-btn">添加起始节点</button>
          <button @click="handleAddEndNode" class="toolbar-btn">添加结束节点</button>
          <button @click="handleClear" class="toolbar-btn">清空</button>
        </div>

        <div
          class="editor-canvas"
          @drop="handleDrop"
          @dragover.prevent
          @click="handleCanvasClick"
        >
          <svg class="edges-layer">
            <line
              v-for="edge in edges"
              :key="edge.id"
              :x1="getNodeX(edge.source)"
              :y1="getNodeY(edge.source)"
              :x2="getNodeX(edge.target)"
              :y2="getNodeY(edge.target)"
              stroke="#666"
              stroke-width="4"
              marker-end="url(#arrowhead)"
              @click="handleEdgeClick(edge)"
              class="workflow-edge"
            />
            <defs>
              <marker
                id="arrowhead"
                markerWidth="10"
                markerHeight="10"
                refX="9"
                refY="3"
                orient="auto"
              >
                <polygon points="0 0, 10 3, 0 6" fill="#666" />
              </marker>
            </defs>
          </svg>

          <div
            v-for="node in nodes"
            :key="node.id"
            class="workflow-node"
            :class="{ 'is-selected': sourceNode?.id === node.id }"
            :style="{ left: node.position?.x + 'px', top: node.position?.y + 'px' }"
            @mousedown="handleNodeMouseDown($event, node)"
            @click.stop="handleNodeClick(node)"
            @dblclick.stop="handleEditNode(node)"
          >
            <div class="node-header">
              <span class="node-type-badge">{{ getNodeTypeName(node.type) }}</span>
              <div class="node-actions">
                <button @click.stop="handleEditNode(node)" class="node-edit-btn" title="配置">⚙️</button>
                <button @click.stop="handleDeleteNode(node)" class="node-delete-btn" title="删除">×</button>
              </div>
            </div>
            <div class="node-body">{{ node.name }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Node Configuration Modal -->
    <div v-if="editingNode" class="modal-overlay" @click="closeEditModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>配置节点: {{ editingNode.name }}</h3>
          <button class="close-btn" @click="closeEditModal">×</button>
        </div>

        <div class="modal-body">
          <!-- Display Node ID for reference -->
          <div class="node-id-info" style="margin-bottom: 15px; padding: 8px; background: #f5f7fa; border-radius: 4px; font-size: 12px; color: #666;">
            <strong>节点 ID:</strong> <code style="user-select: all; background: #eee; padding: 2px 4px; border-radius: 3px;">{{ editingNode.id }}</code>
            <span style="margin-left: 10px; color: #999;">(可在其他节点引用此 ID)</span>
          </div>

          <!-- Common Fields -->
          <div class="form-group">
            <label>节点名称</label>
            <input v-model="editForm.name" type="text" placeholder="请输入节点名称" />
          </div>

          <!-- Agent/LLM Node Fields -->
          <div v-if="editingNode.type === 'agent' || editingNode.type === 'llm'">
            <div class="form-group">
              <label>模型提供商 (API Base)</label>
              <input v-model="editForm.data.api_base" type="text" placeholder="例如: https://api.deepseek.com (留空使用默认)" />
              <div class="help-text">支持DeepSeek, Moonshot (https://api.moonshot.cn/v1) 等OpenAI兼容接口</div>
            </div>
            <div class="form-group">
              <label>API Key</label>
              <input v-model="editForm.data.api_key" type="password" placeholder="sk-..." />
              <div class="help-text">留空则使用系统默认配置</div>
            </div>
            <div class="form-group">
              <label>模型名称 (Model)</label>
               <input v-model="editForm.data.model" type="text" placeholder="例如: deepseek-chat, moonshot-v1-8k" />
            </div>
            <div class="form-group">
              <label>系统提示词 (System Prompt)</label>
              <textarea v-model="editForm.data.system_prompt" rows="3" placeholder="你是一个有用的助手..."></textarea>
            </div>
            <div class="form-group">
              <label>用户提示词 (User Prompt)</label>
              <textarea v-model="editForm.data.user_prompt" rows="5" placeholder="请输入问题... 支持变量 {input.question}"></textarea>
              <div class="help-text">支持变量: {input.param}, {nodeId.output}</div>
            </div>
            <div class="form-group">
              <label>温度 (Temperature): {{ editForm.data.temperature || 0.7 }}</label>
              <input type="range" v-model="editForm.data.temperature" min="0" max="2" step="0.1" />
            </div>
          </div>

          <!-- HTTP Node Fields -->
          <div v-if="editingNode.type === 'http'">
            <div class="form-group">
              <label>请求 URL</label>
              <input v-model="editForm.data.url" type="text" placeholder="https://api.example.com/data" />
            </div>
            <div class="form-group">
              <label>请求方法 (Method)</label>
              <select v-model="editForm.data.method">
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
                <option value="PATCH">PATCH</option>
              </select>
            </div>
            <div class="form-group">
              <label>Headers (JSON格式)</label>
              <textarea v-model="editForm.data.headers" rows="3" placeholder='{ "Content-Type": "application/json" }'></textarea>
              <div class="help-text">请输入标准 JSON 格式的 Key-Value 对</div>
            </div>
            <div class="form-group">
              <label>Body (请求体)</label>
              <textarea v-model="editForm.data.body" rows="5" placeholder='{ "key": "value" } or Raw Text'></textarea>
              <div class="help-text">支持变量: {input.param}, {nodeId.data.field}</div>
            </div>
            <div class="form-row" style="display: flex; gap: 10px;">
                <div class="form-group" style="flex: 1;">
                  <label>超时时间 (ms)</label>
                  <input v-model.number="editForm.data.timeout" type="number" placeholder="10000" />
                </div>
                <div class="form-group" style="flex: 1;">
                  <label>重试次数</label>
                  <input v-model.number="editForm.data.retryCount" type="number" placeholder="0" />
                </div>
            </div>
            <div class="form-group">
               <label>
                 <input type="checkbox" v-model="editForm.data.validateSSL" /> 验证 SSL 证书
               </label>
            </div>
          </div>

          <!-- Knowledge Retrieval Node Fields -->
          <div v-if="editingNode.type === 'knowledge_retrieval'">
             <div class="form-group">
               <label>查询文本 (Query)</label>
               <input v-model="editForm.data.query" type="text" placeholder="请输入查询内容，支持变量 {input.q}" />
             </div>
             
             <div class="form-group">
               <label>选择知识库 (多选)</label>
               <div style="max-height: 150px; overflow-y: auto; border: 1px solid #ddd; padding: 5px; border-radius: 4px;">
                  <div v-for="kb in knowledgeBases" :key="kb.id" style="margin-bottom: 5px;">
                     <label style="display: flex; align-items: center; gap: 8px; font-weight: normal; margin: 0;">
                        <input type="checkbox" :value="kb.id" v-model="editForm.data.knowledgeBaseIds" />
                        {{ kb.name }}
                     </label>
                  </div>
                  <div v-if="knowledgeBases.length === 0" style="color: #999; font-size: 12px; text-align: center;">无可用知识库</div>
               </div>
             </div>

             <div class="form-row" style="display: flex; gap: 10px;">
                <div class="form-group" style="flex: 1;">
                  <label>返回数量 (Top K)</label>
                  <input v-model.number="editForm.data.topK" type="number" min="1" max="20" />
                </div>
                <div class="form-group" style="flex: 1;">
                  <label>最小相似度 (Min Score)</label>
                  <input v-model.number="editForm.data.minScore" type="number" min="0" max="1" step="0.1" />
                </div>
             </div>
          </div>

          <!-- Condition Node Fields -->
          <div v-if="editingNode.type === 'condition'">
             <!-- Placeholder for condition config -->
             <div class="form-group">
                <label>条件表达式</label>
                <input v-model="editForm.data.condition" type="text" placeholder="e.g. variable == 'value'" />
             </div>
          </div>

          <!-- Action Node Fields -->
          <div v-if="editingNode.type === 'action'">
             <div class="form-group">
                <label>动作类型</label>
                <select v-model="editForm.data.actionType">
                    <option value="http">HTTP请求</option>
                    <option value="email">发送邮件</option>
                </select>
             </div>
             <!-- ... more fields ... -->
          </div>

        </div>

        <div class="modal-footer">
          <button class="cancel-btn" @click="closeEditModal">取消</button>
          <button class="confirm-btn" @click="saveNodeConfig">保存配置</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  createWorkflow,
  updateWorkflow,
  getWorkflow,
  type WorkflowNode,
  type WorkflowEdge,
  type WorkflowDefinition,
} from '../api/workflow'
import { getKnowledgeBases, type KnowledgeBase } from '../api/knowledgebase'

const router = useRouter()
const route = useRoute()

const workflowId = computed(() => {
  const id = route.params.id
  return id && id !== 'new' ? Number(id) : null
})

const isEdit = computed(() => !!workflowId.value)

const workflowName = ref('')
const nodes = ref<WorkflowNode[]>([])
const edges = ref<WorkflowEdge[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const draggedNodeType = ref<string | null>(null)
const selectedNode = ref<WorkflowNode | null>(null)
// 用来记录当前正在连线的起点节点
const sourceNode = ref<WorkflowNode | null>(null)
const nodeOffset = ref({ x: 0, y: 0 })

// Node Editing State
const editingNode = ref<WorkflowNode | null>(null)
const editForm = ref<any>({
  name: '',
  data: {}
})

// 打开编辑弹窗
const handleEditNode = (node: WorkflowNode) => {
  console.log('Editing node:', node.id, node.type, JSON.stringify(node.data))
  editingNode.value = node
  // Deep copy to avoid direct mutation
  const data = JSON.parse(JSON.stringify(node.data || {}))
  
  // HTTP Node: Convert headers object back to string for textarea
  if (node.type === 'http') {
      if (data.headers && typeof data.headers === 'object') {
          data.headers = JSON.stringify(data.headers, null, 2)
      }
      // Defaults
      if (data.method === undefined) data.method = 'GET'
      if (data.timeout === undefined) data.timeout = 10000
      if (data.retryCount === undefined) data.retryCount = 0
      if (data.validateSSL === undefined) data.validateSSL = true
  }

  // Knowledge Node Defaults
  if (node.type === 'knowledge_retrieval') {
     if (!data.knowledgeBaseIds) data.knowledgeBaseIds = []
     if (data.topK === undefined) data.topK = 3
     if (data.minScore === undefined) data.minScore = 0.6
  }
  
  editForm.value = {
    name: node.name,
    data: data
  }
  console.log('Initialized editForm:', JSON.stringify(editForm.value))
}

// 关闭编辑弹窗
const closeEditModal = () => {
  editingNode.value = null
}

// 保存节点配置
const saveNodeConfig = () => {
  if (!editingNode.value) return
  
  console.log('Saving node config from form:', JSON.stringify(editForm.value.data))
  
  // Update node properties
  editingNode.value.name = editForm.value.name
  
  // HTTP Node Special Handling
  if (editingNode.value.type === 'http') {
      try {
          // Attempt to parse headers if string
          if (typeof editForm.value.data.headers === 'string' && editForm.value.data.headers.trim()) {
             editForm.value.data.headers = JSON.parse(editForm.value.data.headers)
          }
      } catch (e) {
          alert('Headers 格式错误，请输入正确的 JSON')
          return
      }
      
      // Attempt to parse body if it looks like JSON? 
      // Actually backend handles String body and parses it if needed, or treats as Raw. 
      // But for "variable substitution in Map", we might want to store as Object.
      // For now, let's keep Body as String or whatever user calls. 
      // If user types JSON string, we can verify it but easier to just save as string.
      
      // Ensure specific types
      if (editForm.value.data.timeout) editForm.value.data.timeout = Number(editForm.value.data.timeout)
      if (editForm.value.data.retryCount) editForm.value.data.retryCount = Number(editForm.value.data.retryCount)
      if (editForm.value.data.validateSSL === undefined) editForm.value.data.validateSSL = true
  }

  editingNode.value.data = JSON.parse(JSON.stringify(editForm.value.data))
  
  closeEditModal()
}

const nodeTypes = [
  { type: 'start', name: '起始', icon: '▶' },
  { type: 'end', name: '结束', icon: '■' },
  { type: 'agent', name: 'LLM调用', icon: '🤖' },
  { type: 'knowledge_retrieval', name: '知识库检索', icon: '📚' },
  { type: 'http', name: 'HTTP请求', icon: '🌐' },
  { type: 'condition', name: '条件', icon: '❓' },
  { type: 'action', name: '动作', icon: '⚡' },
]

// 加载工作流详情（编辑模式）
const loadWorkflow = async () => {
  if (!workflowId.value) return

  loading.value = true
  error.value = null
  try {
    const workflow = await getWorkflow(workflowId.value)
    workflowName.value = workflow.name
    if (workflow.definition) {
      nodes.value = workflow.definition.nodes || []
      edges.value = workflow.definition.edges || []
    }
  } catch (e: any) {
    error.value = e.message || '加载工作流详情失败'
    console.error('加载工作流详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 拖拽开始
const handleDragStart = (event: DragEvent, nodeType: any) => {
  draggedNodeType.value = nodeType.type
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'copy'
  }
}

// 拖拽放置
const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  if (!draggedNodeType.value) return

  const canvas = event.currentTarget as HTMLElement
  const rect = canvas.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  const nodeType = nodeTypes.find(nt => nt.type === draggedNodeType.value)
  if (!nodeType) return

  const newNode: WorkflowNode = {
    id: `node_${Date.now()}`,
    type: draggedNodeType.value,
    name: nodeType.name,
    position: { x: x - 75, y: y - 40 },
  }

  nodes.value.push(newNode)
  draggedNodeType.value = null
}

// 节点鼠标按下
const handleNodeMouseDown = (event: MouseEvent, node: WorkflowNode) => {
  selectedNode.value = node
  const nodeElement = event.currentTarget as HTMLElement
  const rect = nodeElement.getBoundingClientRect()
  nodeOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  }

  const handleMouseMove = (e: MouseEvent) => {
    if (selectedNode.value && selectedNode.value.id === node.id) {
      const canvas = document.querySelector('.editor-canvas') as HTMLElement
      const canvasRect = canvas.getBoundingClientRect()
      const x = e.clientX - canvasRect.left - nodeOffset.value.x
      const y = e.clientY - canvasRect.top - nodeOffset.value.y
      selectedNode.value.position = { x, y }
    }
  }

  const handleMouseUp = () => {
    selectedNode.value = null
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

// 节点点击事件（修改版：支持连线）
const handleNodeClick = (node: WorkflowNode) => {
  // 如果当前没有选中起点，那么这个点就是起点
  if (!sourceNode.value) {
    sourceNode.value = node
    // 给个提示（实际项目中可以用 Toast）
    console.log('已选中起点，请点击下一个节点进行连线')
    alert(`已选中起点 [${node.name}]，请点击另一个节点连线，或者再次点击取消`)
  }
  // 如果已经有了起点，且点击的不是自己，那就连线！
  else if (sourceNode.value.id !== node.id) {
    // 创建一条新线
    const newEdge: WorkflowEdge = {
      id: `edge_${Date.now()}`,
      source: sourceNode.value.id,
      target: node.id,
    }

    // 检查是否已经连过线了，防止重复连
    const exists = edges.value.some(e => e.source === newEdge.source && e.target === newEdge.target)
    if (!exists) {
      edges.value.push(newEdge)
    }

    // 连完线，清空起点，准备下一次操作
    sourceNode.value = null
  }
  // 如果点击的还是自己，那就取消选中（或者保留原来的改名功能）
  else {
    sourceNode.value = null
    // 如果你想保留双击改名，可以把改名逻辑放这里，或者单独做一个编辑按钮
    const name = prompt('请输入节点名称:', node.name)
    if (name !== null) {
      node.name = name
    }
  }
}

// 删除节点
const handleDeleteNode = (node: WorkflowNode) => {
  if (confirm(`确定要删除节点"${node.name}"吗？`)) {
    nodes.value = nodes.value.filter(n => n.id !== node.id)
    edges.value = edges.value.filter(e => e.source !== node.id && e.target !== node.id)
  }
}

// 删除连线：双击连线删除
const handleEdgeClick = (edge: WorkflowEdge) => {
  if (confirm('确定要删除这条连线吗？')) {
    edges.value = edges.value.filter(e => e.id !== edge.id)
  }
}

// 添加起始节点
const handleAddStartNode = () => {
  const newNode: WorkflowNode = {
    id: `node_${Date.now()}`,
    type: 'start',
    name: '起始',
    position: { x: 100, y: 100 },
  }
  nodes.value.push(newNode)
}

// 添加结束节点
const handleAddEndNode = () => {
  const newNode: WorkflowNode = {
    id: `node_${Date.now()}`,
    type: 'end',
    name: '结束',
    position: { x: 300, y: 100 },
  }
  nodes.value.push(newNode)
}

// 清空
const handleClear = () => {
  if (confirm('确定要清空所有节点吗？')) {
    nodes.value = []
    edges.value = []
  }
}

// 画布点击
const handleCanvasClick = (_event: MouseEvent) => {
  // 可以在这里实现连线功能
}

// 获取节点X坐标
const getNodeX = (nodeId: string) => {
  const node = nodes.value.find(n => n.id === nodeId)
  return node?.position?.x ? node.position.x + 75 : 0
}

// 获取节点Y坐标
const getNodeY = (nodeId: string) => {
  const node = nodes.value.find(n => n.id === nodeId)
  return node?.position?.y ? node.position.y + 40 : 0
}

// 获取节点类型名称
const getNodeTypeName = (type: string) => {
  const nodeType = nodeTypes.find(nt => nt.type === type)
  return nodeType?.name || type
}

// 保存
const handleSave = async () => {
  if (!workflowName.value.trim()) {
    error.value = '请填写工作流名称'
    return
  }

  if (nodes.value.length === 0) {
    error.value = '请至少添加一个节点'
    return
  }

  loading.value = true
  error.value = null
  successMessage.value = null

  try {
    const definition: WorkflowDefinition = {
      nodes: nodes.value,
      edges: edges.value,
    }

    const data = {
      name: workflowName.value.trim(),
      description: '',
      definition,
    }

    if (isEdit.value) {
      await updateWorkflow(workflowId.value!, data)
      successMessage.value = '更新成功'
    } else {
      await createWorkflow(data)
      successMessage.value = '创建成功'
    }

    setTimeout(() => {
      router.push('/workflows')
    }, 1500)
  } catch (e: any) {
    error.value = e.message || (isEdit.value ? '更新失败' : '创建失败')
    console.error('保存失败:', e)
  } finally {
    loading.value = false
  }
}

const knowledgeBases = ref<KnowledgeBase[]>([])
const loadKnowledgeBases = async () => {
    try {
        knowledgeBases.value = await getKnowledgeBases()
    } catch (e) {
        console.error("加载知识库列表失败", e)
    }
}

onMounted(() => {
  if (isEdit.value) {
    loadWorkflow()
  }
  // Load Knowledge Bases for selection
  loadKnowledgeBases()
})
</script>

<style scoped>
.workflow-editor {
  padding: 20px;
  height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

h1 {
  color: #2c3e50;
  margin: 0;
  font-size: 24px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.save-btn,
.back-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  text-decoration: none;
  transition: background 0.3s;
}

.save-btn {
  background: #42b983;
  color: white;
}

.save-btn:hover:not(:disabled) {
  background: #35a372;
}

.save-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.back-btn {
  background: #6c757d;
  color: white;
  display: inline-block;
}

.back-btn:hover {
  background: #5a6268;
}

.error-message,
.success-message {
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.error-message {
  background: #ffebee;
  color: #f44336;
  border: 1px solid #f44336;
}

.success-message {
  background: #e8f5e9;
  color: #4caf50;
  border: 1px solid #4caf50;
}

.editor-container {
  display: flex;
  flex: 1;
  gap: 20px;
  overflow: hidden;
}

.editor-sidebar {
  width: 200px;
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.editor-sidebar h3 {
  margin: 0 0 15px 0;
  color: #2c3e50;
  font-size: 16px;
}

.node-types {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.node-type-item {
  padding: 10px;
  background: #f5f5f5;
  border-radius: 4px;
  cursor: move;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: background 0.3s;
}

.node-type-item:hover {
  background: #e0e0e0;
}

.node-icon {
  font-size: 20px;
}

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.editor-toolbar {
  padding: 15px;
  border-bottom: 1px solid #eee;
  display: flex;
  gap: 10px;
  align-items: center;
}

.workflow-name-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.toolbar-btn {
  padding: 8px 16px;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.toolbar-btn:hover {
  background: #35a372;
}

.editor-canvas {
  flex: 1;
  position: relative;
  overflow: auto;
  background: #f9f9f9;
}

.edges-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.workflow-node {
  position: absolute;
  width: 150px;
  background: white;
  border: 2px solid #42b983;
  border-radius: 8px;
  cursor: move;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.3s;
}

.workflow-node:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background: #42b983;
  color: white;
  border-radius: 6px 6px 0 0;
}

.node-type-badge {
  font-size: 12px;
  font-weight: 500;
}

.node-delete-btn {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  padding: 0;
  width: 20px;
  height: 20px;
}

.node-actions {
  display: flex;
  gap: 4px;
}

.node-edit-btn,
.node-delete-btn {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 16px;
  padding: 0 4px;
}

.node-edit-btn:hover,
.node-delete-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

.node-delete-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
}
/* Ensure precedence */
.node-actions .node-edit-btn,
.node-actions .node-delete-btn {
  display: inline-block;
  pointer-events: auto;
}

.node-body {
  padding: 12px;
  text-align: center;
  color: #2c3e50;
  font-weight: 500;
}

/* --- 新增的连线样式 --- */

/* Node Config Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 500px;
  max-width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #2c3e50;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #42b983;
  outline: none;
}

.help-text {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.modal-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-btn,
.confirm-btn {
  padding: 8px 16px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn {
  background: #eee;
  color: #666;
}

.confirm-btn {
  background: #42b983;
  color: white;
}

.confirm-btn:hover {
  background: #35a372;
}

.workflow-node.is-selected {
  border-color: #ff9800 !important;
  box-shadow: 0 0 10px rgba(255, 152, 0, 0.6);
  z-index: 100;
}

.workflow-edge {
  cursor: pointer;
  pointer-events: auto;
  transition: all 0.3s;
}

.workflow-edge:hover {
  stroke: #ff9800;
  stroke-width: 6;
}

</style>

