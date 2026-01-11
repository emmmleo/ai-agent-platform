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
          <button @click="handleAutoLayout" class="toolbar-btn">节点整理</button>
          <button @click="handleClear" class="toolbar-btn">清空</button>
        </div>

        <div
          class="editor-canvas"
          @drop="handleDrop"
          @dragover.prevent
          @click="handleCanvasClick"
          @mousemove="handleMouseMove"
          @mouseup="handleGlobalMouseUp"
        >
          <svg class="edges-layer">
            <defs>
              <marker
                id="arrowhead"
                markerWidth="10"
                markerHeight="7"
                refX="9"
                refY="3.5"
                orient="auto"
              >
                <polygon points="0 0, 10 3.5, 0 7" fill="#999" />
              </marker>
            </defs>
            <path
              v-for="edge in edges"
              :key="edge.id"
              :d="getEdgePath(edge)"
              stroke="#999"
              stroke-width="2"
              fill="none"
              marker-end="url(#arrowhead)"
              @click="handleEdgeClick(edge)"
              class="workflow-edge"
            />
            <path
              v-if="tempEdge"
              :d="getTempEdgePath()"
              stroke="#999"
              stroke-width="2"
              stroke-dasharray="5,5"
              fill="none"
              marker-end="url(#arrowhead)"
            />
          </svg>

          <div
            v-for="node in nodes"
            :key="node.id"
            class="workflow-node"
            :class="{ 'is-selected': sourceNode?.id === node.id }"
            :style="{ left: node.position?.x + 'px', top: node.position?.y + 'px' }"
            @mousedown="handleNodeMouseDown($event, node)"
            @dblclick.stop="handleEditNode(node)"
          >
            <div class="node-port input" @mouseup="handlePortMouseUp($event, node, 'input')"></div>
            <div class="node-header">
              <span class="node-type-badge">{{ getNodeTypeName(node.type) }}</span>
              <div class="node-actions">
                <button @click.stop="handleEditNode(node)" class="node-edit-btn" title="配置">⚙️</button>
                <button @click.stop="handleDeleteNode(node)" class="node-delete-btn" title="删除">×</button>
              </div>
            </div>
            <div class="node-body">{{ node.name }}</div>
            
            <!-- Standard Output Port -->
            <div v-if="node.type !== 'condition'" class="node-port output" @mousedown.stop="handlePortMouseDown($event, node, 'output')"></div>
            
            <!-- Condition Node Output Ports -->
            <div v-else class="condition-ports" style="position: absolute; right: -6px; top: 40px; display: flex; flex-direction: column; gap: 15px;">
               <div 
                 v-for="(branch, idx) in (node.data?.branches || [{id: 'true', name: 'IF'}, {id: 'false', name: 'ELSE'}])" 
                 :key="idx"
                 class="node-port output branch-port"
                 :title="branch.name"
                 @mousedown.stop="handlePortMouseDown($event, node, 'output', branch.id)"
                 style="position: relative; right: 0; top: 0;"
               >
                 <span style="position: absolute; right: 12px; top: -2px; font-size: 10px; white-space: nowrap; color: #666;">{{ branch.name }}</span>
               </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Node Configuration Modal -->
    <div v-if="editingNode" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal-content">
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

          <!-- Start Node Fields -->
          <div v-if="editingNode.type === 'start'">
            <div class="form-group">
              <label>输入字段配置</label>
              <div class="help-text">定义应用启动时需要用户提供的信息</div>
              
              <div class="input-field-list">
                <div v-for="(field, index) in editForm.data.inputFields" :key="index" class="input-field-item" style="border: 1px solid #eee; padding: 10px; margin-bottom: 10px; border-radius: 4px; background: #f9f9f9;">
                  <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                    <strong>字段 #{{ index + 1 }}</strong>
                    <button @click="removeInputField(index)" style="color: #ff4d4f; border: none; background: none; cursor: pointer;">删除</button>
                  </div>
                  
                  <div class="form-row" style="display: flex; gap: 10px; margin-bottom: 8px;">
                    <div style="flex: 1;">
                      <label style="font-size: 12px;">变量名 (Key)</label>
                      <input v-model="field.variable" type="text" placeholder="例如: user_name" style="font-size: 13px;" />
                    </div>
                    <div style="flex: 1;">
                      <label style="font-size: 12px;">显示名称 (Label)</label>
                      <input v-model="field.label" type="text" placeholder="例如: 用户姓名" style="font-size: 13px;" />
                    </div>
                  </div>

                  <div class="form-row" style="display: flex; gap: 10px; margin-bottom: 8px;">
                    <div style="flex: 1;">
                      <label style="font-size: 12px;">类型</label>
                      <select v-model="field.type" style="font-size: 13px;">
                        <option value="text">短文本 (Text)</option>
                        <option value="paragraph">长文本 (Paragraph)</option>
                        <option value="select">下拉选项 (Select)</option>
                        <option value="number">数字 (Number)</option>
                      </select>
                    </div>
                    <div style="flex: 0 0 60px; display: flex; align-items: center; padding-top: 20px;">
                      <label style="font-size: 12px; display: flex; align-items: center; cursor: pointer;">
                        <input type="checkbox" v-model="field.required" style="margin-right: 4px;" /> 必填
                      </label>
                    </div>
                  </div>

                  <!-- Options for Select type -->
                  <div v-if="field.type === 'select'" style="margin-top: 8px;">
                    <label style="font-size: 12px;">选项列表 (每行一个)</label>
                    <textarea v-model="field.options" rows="3" placeholder="选项A&#10;选项B&#10;选项C" style="font-size: 13px;"></textarea>
                  </div>
                </div>
              </div>

              <button @click="addInputField" style="width: 100%; padding: 8px; background: #e6f7ff; border: 1px dashed #1890ff; color: #1890ff; border-radius: 4px; cursor: pointer; margin-top: 8px;">+ 添加输入字段</button>
            </div>
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
              <textarea id="llm-user-prompt" v-model="editForm.data.user_prompt" rows="5" placeholder="请输入问题... 支持变量 {input.question}"></textarea>
              <div style="margin-top: 5px; margin-bottom: 5px;">
                <select style="width: 100%; padding: 4px;" @change="handleVariableSelect($event, 'user_prompt', 'llm-user-prompt')">
                  <option value="">插入变量...</option>
                  <option v-for="v in availableVariables" :key="v.value" :value="v.value">{{ v.label }}</option>
                </select>
              </div>
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
              <textarea id="http-body" v-model="editForm.data.body" rows="5" placeholder='{ "key": "value" } or Raw Text'></textarea>
              <div style="margin-top: 5px; margin-bottom: 5px;">
                <select style="width: 100%; padding: 4px;" @change="handleVariableSelect($event, 'body', 'http-body')">
                  <option value="">插入变量...</option>
                  <option v-for="v in availableVariables" :key="v.value" :value="v.value">{{ v.label }}</option>
                </select>
              </div>
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
               <input id="knowledge-query" v-model="editForm.data.query" type="text" placeholder="请输入查询内容，支持变量 {input.q}" />
               <div style="margin-top: 5px; margin-bottom: 5px;">
                 <select style="width: 100%; padding: 4px;" @change="handleVariableSelect($event, 'query', 'knowledge-query')">
                  <option value="">插入变量...</option>
                  <option v-for="v in availableVariables" :key="v.value" :value="v.value">{{ v.label }}</option>
                </select>
               </div>
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
             <div class="condition-branches">
                <div v-for="(branch, bIndex) in editForm.data.branches" :key="bIndex" class="branch-item" style="border: 1px solid #eee; padding: 10px; margin-bottom: 10px; border-radius: 4px; background: #f9f9f9;">
                   <div class="branch-header" style="display: flex; justify-content: space-between; margin-bottom: 8px; font-weight: bold;">
                      <span>{{ branch.name }}</span>
                      <button v-if="branch.name !== 'ELSE' && bIndex > 0" @click="removeBranch(bIndex)" style="color: #ff4d4f; border: none; background: none; cursor: pointer;">删除</button>
                   </div>
                   
                   <div v-if="branch.name !== 'ELSE'">
                       <div class="form-group">
                          <label style="font-size: 12px;">条件逻辑</label>
                          <select v-model="branch.logic" style="margin-bottom: 8px; width: 100%; padding: 4px; border: 1px solid #ddd; border-radius: 4px; font-size: 12px;">
                             <option value="AND">满足所有条件 (AND)</option>
                             <option value="OR">满足任一条件 (OR)</option>
                          </select>
                       </div>

                       <div v-for="(item, cIndex) in branch.conditions" :key="cIndex" class="condition-row" style="display: flex; gap: 5px; margin-bottom: 5px;">
                          <div style="flex: 1; min-width: 0;">
                             <input v-model="item.variable" type="text" placeholder="变量" list="variable-suggestions" style="width: 100%; font-size: 12px; padding: 4px;" />
                             <datalist id="variable-suggestions">
                               <option v-for="v in availableVariables" :key="v.value" :value="v.value">{{ v.label }}</option>
                             </datalist>
                          </div>
                          <select v-model="item.operator" style="width: 85px; flex-shrink: 0; font-size: 12px; padding: 4px;">
                             <option value="contains">包含</option>
                             <option value="not_contains">不包含</option>
                             <option value="start_with">开始是</option>
                             <option value="end_with">结束是</option>
                             <option value="is">是</option>
                             <option value="is_not">不是</option>
                             <option value="is_empty">为空</option>
                             <option value="is_not_empty">不为空</option>
                          </select>
                          <input v-if="!['is_empty', 'is_not_empty'].includes(item.operator)" v-model="item.value" type="text" placeholder="值" style="flex: 1; min-width: 0; font-size: 12px; padding: 4px;" />
                          <button @click="removeBranchCondition(bIndex, cIndex)" style="color: #999; border: none; background: none; cursor: pointer;">×</button>
                       </div>
                       
                       <button @click="addBranchCondition(bIndex)" style="font-size: 12px; color: #42b983; background: none; border: none; cursor: pointer; padding: 0;">+ 添加条件</button>
                   </div>
                   <div v-else class="help-text">
                       当以上所有条件都不满足时，执行此路径。
                   </div>
                </div>
             </div>
             
             <button @click="addBranch" style="width: 100%; padding: 8px; background: #e6f7ff; border: 1px dashed #1890ff; color: #1890ff; border-radius: 4px; cursor: pointer; margin-top: 8px;">+ 添加分支 (ELIF)</button>
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

          <!-- Reply Node Fields -->
          <div v-if="editingNode.type === 'reply'">
                <div class="form-group">
                   <label>回复内容</label>
                   <div class="variable-selector" style="margin-bottom: 5px;">
                      <select @change="handleVariableSelect($event, 'content', 'reply-content-input')" style="padding: 4px; border-radius: 4px; border: 1px solid #ddd;">
                        <option value="">插入变量...</option>
                        <option v-for="v in availableVariables" :key="v.value" :value="v.value">{{ v.label }}</option>
                      </select>
                   </div>
                   <textarea
                     id="reply-content-input"
                     v-model="editForm.data.content"
                     rows="6"
                     placeholder="在此输入回复内容，支持使用变量..."
                     style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; resize: vertical;"
                   ></textarea>
                   <div class="help-text" style="font-size: 12px; color: #666; margin-top: 4px;" v-pre>
                     支持纯文本或使用变量（如 {{node_id.output}}）。
                   </div>
                </div>
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

// New State for Dragging Connections
const tempEdge = ref<{ start: { x: number; y: number }; end: { x: number; y: number } } | null>(null)
const draggedPort = ref<{ nodeId: string; type: 'input' | 'output'; branchId?: string } | null>(null)

// Node Editing State
const editingNode = ref<WorkflowNode | null>(null)
const editForm = ref<any>({
  name: '',
  data: {}
})

// 计算可用变量
const availableVariables = computed(() => {
  if (!editingNode.value) return []
  
  try {
    const vars: { label: string; value: string }[] = []
    const visited = new Set<string>()
    
    // 简单的上游遍历（广度优先，反向）
    // 注意：edges 是 source -> target
    // 我们需要找到所有 target 为当前节点的边，然后获取 source
    
    const upstreamNodes = new Set<string>()
    const findUpstream = (nodeId: string) => {
      if (!nodeId) return
      const incomingEdges = edges.value.filter(e => e.target === nodeId)
      for (const edge of incomingEdges) {
        if (edge.source && !upstreamNodes.has(edge.source)) {
          upstreamNodes.add(edge.source)
          findUpstream(edge.source)
        }
      }
    }
    
    findUpstream(editingNode.value.id)
    
    upstreamNodes.forEach(sourceId => {
      const node = nodes.value.find(n => n.id === sourceId)
      if (!node) return
      
      // 根据节点类型定义输出变量
      switch (node.type) {
        case 'start':
          // Add default input variable (legacy support)
          vars.push({ label: `[起始] ${node.name}.input`, value: `{{${node.id}.input}}` })
          
          // Add configured input fields
          if (node.data && node.data.inputFields) {
            node.data.inputFields.forEach((field: any) => {
              if (field.variable) {
                vars.push({ 
                  label: `[起始] ${node.name}.${field.variable} (${field.label || field.variable})`, 
                  value: `{{${node.id}.${field.variable}}}` 
                })
              }
            })
          }
          break
        case 'agent':
          vars.push({ label: `[LLM] ${node.name}.output`, value: `{{${node.id}.output}}` })
          break
        case 'knowledge_retrieval':
          vars.push({ label: `[知识库] ${node.name}.results`, value: `{{${node.id}.results}}` })
          break
        case 'http':
          vars.push({ label: `[HTTP] ${node.name}.status`, value: `{{${node.id}.status}}` })
          vars.push({ label: `[HTTP] ${node.name}.body`, value: `{{${node.id}.body}}` })
          break
        case 'action':
          vars.push({ label: `[动作] ${node.name}.result`, value: `{{${node.id}.result}}` })
          break
      }
    })
    
    return vars
  } catch (e) {
    console.error('Error calculating availableVariables:', e)
    return []
  }
})

// Condition Node Logic
const addCondition = () => {
  if (!editForm.value.data.conditions) {
    editForm.value.data.conditions = []
  }
  editForm.value.data.conditions.push({
    variable: '',
    operator: '==',
    value: ''
  })
}

const removeCondition = (index: number) => {
  if (editForm.value.data.conditions) {
    editForm.value.data.conditions.splice(index, 1)
  }
}

// Branch Management
const addBranch = () => {
    if (!editForm.value.data.branches) editForm.value.data.branches = []
    // Insert before ELSE
    const elseIndex = editForm.value.data.branches.findIndex((b: any) => b.name === 'ELSE')
    const newBranch = { 
        id: `branch_${Date.now()}`, 
        name: `ELIF ${editForm.value.data.branches.length}`, // Simple naming
        logic: 'AND', 
        conditions: [] 
    }
    
    if (elseIndex !== -1) {
        editForm.value.data.branches.splice(elseIndex, 0, newBranch)
    } else {
        editForm.value.data.branches.push(newBranch)
        // Ensure ELSE exists
        editForm.value.data.branches.push({ id: 'branch_else', name: 'ELSE', conditions: [] })
    }
    
    // Renumber ELIFs
    updateBranchNames()
}

const removeBranch = (index: number) => {
    editForm.value.data.branches.splice(index, 1)
    updateBranchNames()
}

const updateBranchNames = () => {
    let elifCount = 1
    editForm.value.data.branches.forEach((b: any, idx: number) => {
        if (idx === 0) b.name = 'IF'
        else if (idx === editForm.value.data.branches.length - 1) b.name = 'ELSE'
        else b.name = `ELIF ${elifCount++}`
    })
}

const addBranchCondition = (branchIndex: number) => {
    if (!editForm.value.data.branches[branchIndex].conditions) {
        editForm.value.data.branches[branchIndex].conditions = []
    }
    editForm.value.data.branches[branchIndex].conditions.push({
        variable: '',
        operator: '==',
        value: ''
    })
}

const removeBranchCondition = (branchIndex: number, conditionIndex: number) => {
    editForm.value.data.branches[branchIndex].conditions.splice(conditionIndex, 1)
}

// Start Node Logic
const addInputField = () => {
  if (!editForm.value.data.inputFields) {
    editForm.value.data.inputFields = []
  }
  editForm.value.data.inputFields.push({
    variable: '',
    label: '',
    type: 'text',
    required: false,
    options: ''
  })
}

const removeInputField = (index: number) => {
  if (editForm.value.data.inputFields) {
    editForm.value.data.inputFields.splice(index, 1)
  }
}

// 打开编辑弹窗
const handleEditNode = (node: WorkflowNode) => {
  if (!node) return
  
  try {
    console.log('Editing node:', node.id, node.type, JSON.stringify(node.data))
    
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

    // Condition Node Defaults
    if (node.type === 'condition') {
       if (!data.branches) {
           // Migrate legacy conditions or create default
           if (data.conditions && data.conditions.length > 0) {
               data.branches = [
                   { id: 'branch_if', name: 'IF', logic: data.logic || 'AND', conditions: data.conditions },
                   { id: 'branch_else', name: 'ELSE', conditions: [] }
               ]
           } else {
               data.branches = [
                   { id: 'branch_if', name: 'IF', logic: 'AND', conditions: [] },
                   { id: 'branch_else', name: 'ELSE', conditions: [] }
               ]
           }
       }
    }

    // Start Node Defaults
    if (node.type === 'start') {
      if (!data.inputFields) data.inputFields = []
    }

    // Reply Node Defaults
    if (node.type === 'reply') {
      if (data.content === undefined) data.content = ''
    }
    
    // Update form FIRST to ensure data is ready before modal renders
    editForm.value = {
      name: node.name,
      data: data
    }
    
    // Then set editingNode to trigger modal display
    editingNode.value = node
    console.log('Initialized editForm:', JSON.stringify(editForm.value))
  } catch (e) {
    console.error('Error in handleEditNode:', e)
    // Prevent white screen by not opening modal if error occurs
    editingNode.value = null
  }
}

// 关闭编辑弹窗
const closeEditModal = () => {
  editingNode.value = null
}

// 插入变量到指定输入框
const insertVariable = (fieldPath: string, variable: string, elementId: string) => {
  if (!variable) return
  
  const textarea = document.getElementById(elementId) as HTMLTextAreaElement | HTMLInputElement
  if (textarea) {
    const start = textarea.selectionStart || 0
    const end = textarea.selectionEnd || 0
    const text = textarea.value
    const newText = text.substring(0, start) + variable + text.substring(end)
    
    // 更新 DOM 值
    textarea.value = newText
    
    // 触发 input 事件以更新 v-model
    textarea.dispatchEvent(new Event('input'))
    
    // 恢复焦点并移动光标
    textarea.focus()
    setTimeout(() => {
      textarea.selectionStart = textarea.selectionEnd = start + variable.length
    }, 0)
  }
}

const handleVariableSelect = (event: Event, fieldPath: string, elementId: string) => {
  const target = event.target as HTMLSelectElement
  if (target && target.value) {
    insertVariable(fieldPath, target.value, elementId)
    target.value = ''
  }
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
  { type: 'reply', name: '直接回复', icon: '💬' },
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

const handlePortMouseDown = (e: MouseEvent, node: WorkflowNode, type: 'input' | 'output', branchId?: string) => {
  if (type !== 'output') return 
  
  let startX = (node.position?.x || 0) + 150
  let startY = (node.position?.y || 0) + 40
  
  // Adjust for branch ports
  if (node.type === 'condition' && branchId) {
      const branches = node.data?.branches || [{id: 'true'}, {id: 'false'}]
      const index = branches.findIndex((b: any) => b.id === branchId)
      if (index !== -1) {
          startY = (node.position?.y || 0) + 40 + (index * 25) // 25px gap + offset
      }
  }
  
  draggedPort.value = { nodeId: node.id, type, branchId }
  tempEdge.value = {
    start: { x: startX, y: startY },
    end: { x: startX, y: startY }
  }
}

const handleMouseMove = (e: MouseEvent) => {
  if (!tempEdge.value) return
  
  const canvas = document.querySelector('.editor-canvas') as HTMLElement
  const canvasRect = canvas.getBoundingClientRect()
  
  tempEdge.value.end = {
    x: e.clientX - canvasRect.left + canvas.scrollLeft,
    y: e.clientY - canvasRect.top + canvas.scrollTop
  }
}

const handlePortMouseUp = (e: MouseEvent, node: WorkflowNode, type: 'input' | 'output') => {
  if (!draggedPort.value || !tempEdge.value) return
  
  if (type === 'input' && draggedPort.value.type === 'output' && draggedPort.value.nodeId !== node.id) {
    const exists = edges.value.some(
      edge => edge.source === draggedPort.value!.nodeId && edge.target === node.id && edge.condition === draggedPort.value!.branchId
    )
    
    if (!exists) {
      edges.value.push({
        id: `edge_${Date.now()}`,
        source: draggedPort.value.nodeId,
        target: node.id,
        condition: draggedPort.value.branchId // Save branch ID
      })
    }
  }
  
  tempEdge.value = null
  draggedPort.value = null
}

const handleGlobalMouseUp = () => {
  if (tempEdge.value) {
    tempEdge.value = null
    draggedPort.value = null
  }
}

const getEdgePath = (edge: WorkflowEdge) => {
  const source = nodes.value.find(n => n.id === edge.source)
  const target = nodes.value.find(n => n.id === edge.target)
  if (!source || !target) return ''
  
  let startX = (source.position?.x || 0) + 150
  let startY = (source.position?.y || 0) + 40
  
  // Adjust for branch ports
  if (source.type === 'condition' && edge.condition) {
      const branches = source.data?.branches || [{id: 'true'}, {id: 'false'}]
      const index = branches.findIndex((b: any) => b.id === edge.condition)
      if (index !== -1) {
          startY = (source.position?.y || 0) + 40 + (index * 25)
      }
  }
  
  const endX = target.position?.x || 0
  const endY = (target.position?.y || 0) + 40
  
  const deltaX = Math.abs(endX - startX)
  const controlPointOffset = Math.max(deltaX * 0.5, 50)
  
  return `M ${startX} ${startY} C ${startX + controlPointOffset} ${startY}, ${endX - controlPointOffset} ${endY}, ${endX} ${endY}`
}

const getTempEdgePath = () => {
  if (!tempEdge.value) return ''
  const { start, end } = tempEdge.value
  
  const deltaX = Math.abs(end.x - start.x)
  const controlPointOffset = Math.max(deltaX * 0.5, 50)
  
  return `M ${start.x} ${start.y} C ${start.x + controlPointOffset} ${start.y}, ${end.x - controlPointOffset} ${end.y}, ${end.x} ${end.y}`
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

// 自动整理节点
const handleAutoLayout = () => {
  if (nodes.value.length === 0) return

  // 1. Build Graph
  const adj = new Map<string, string[]>()
  const inDegree = new Map<string, number>()
  
  nodes.value.forEach(node => {
    adj.set(node.id, [])
    inDegree.set(node.id, 0)
  })
  
  edges.value.forEach(edge => {
    if (adj.has(edge.source) && adj.has(edge.target)) {
      adj.get(edge.source)!.push(edge.target)
      inDegree.set(edge.target, (inDegree.get(edge.target) || 0) + 1)
    }
  })

  // 2. Assign Levels (BFS)
  const levels = new Map<string, number>()
  const queue: string[] = []
  
  // Find start nodes (in-degree 0)
  nodes.value.forEach(node => {
    if (inDegree.get(node.id) === 0) {
      levels.set(node.id, 0)
      queue.push(node.id)
    }
  })
  
  // If no start nodes (cycle), pick the first one
  if (queue.length === 0 && nodes.value.length > 0) {
      const firstId = nodes.value[0].id
      levels.set(firstId, 0)
      queue.push(firstId)
  }

  while (queue.length > 0) {
    const u = queue.shift()!
    const level = levels.get(u)!
    
    const neighbors = adj.get(u) || []
    neighbors.forEach(v => {
      if (!levels.has(v)) {
        levels.set(v, level + 1)
        queue.push(v)
      }
    })
  }
  
  // Handle unvisited nodes (disconnected components)
  nodes.value.forEach(node => {
      if (!levels.has(node.id)) {
          levels.set(node.id, 0)
      }
  })

  // 3. Group by Level
  const levelNodes = new Map<number, string[]>()
  levels.forEach((level, nodeId) => {
    if (!levelNodes.has(level)) {
      levelNodes.set(level, [])
    }
    levelNodes.get(level)!.push(nodeId)
  })
  
  // 4. Assign Positions
  const LEVEL_WIDTH = 250
  const NODE_HEIGHT = 120
  const START_X = 100
  const START_Y = 100
  
  levelNodes.forEach((nodeIds, level) => {
    // Sort nodeIds to minimize crossing? 
    // Simple heuristic: sort by ID to be deterministic
    nodeIds.sort() 
    
    nodeIds.forEach((nodeId, index) => {
      const node = nodes.value.find(n => n.id === nodeId)
      if (node) {
        node.position = {
          x: START_X + level * LEVEL_WIDTH,
          y: START_Y + index * NODE_HEIGHT
        }
      }
    })
  })
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
  } else {
    // Initialize with a Start node for new workflows
    nodes.value.push({
      id: `node_${Date.now()}`,
      type: 'start',
      name: '起始',
      position: { x: 100, y: 200 },
      data: { inputFields: [] }
    })
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
.node-port {
  position: absolute;
  width: 12px;
  height: 12px;
  background: #fff;
  border: 2px solid #42b983;
  border-radius: 50%;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  cursor: crosshair;
  transition: all 0.2s;
}

.node-port:hover {
  background: #42b983;
  transform: translateY(-50%) scale(1.2);
}

.node-port.input {
  left: -6px;
}

.node-port.output {
  right: -6px;
}

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

