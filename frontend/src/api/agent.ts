// 智能体相关 API
import { get, post, put, del } from '../utils/request'

export interface Agent {
  id: number
  userId: number
  name: string
  description?: string
  systemPrompt?: string
  userPromptTemplate?: string
  modelConfig?: string
  workflowId?: number
  knowledgeBaseIds?: string
  pluginIds?: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface CreateAgentRequest {
  name: string
  description?: string
  systemPrompt?: string
  userPromptTemplate?: string
  modelConfig?: string
  workflowId?: number
  knowledgeBaseIds?: string
  pluginIds?: string
}

export interface UpdateAgentRequest {
  name: string
  description?: string
  systemPrompt?: string
  userPromptTemplate?: string
  modelConfig?: string
  workflowId?: number
  knowledgeBaseIds?: string
  pluginIds?: string
  status?: string
}

export interface TestAgentRequest {
  question: string
}

export interface TestAgentResponse {
  answer: string
}

export interface ChatRequest {
  question: string
}

export interface ChatResponse {
  answer: string
  source: string // direct/rag/workflow
}

// API 响应格式
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 获取智能体列表
export const getAgents = async (): Promise<Agent[]> => {
  const response = await get<ApiResponse<Agent[]>>('/v1/agents')
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取智能体列表失败')
}

// 获取智能体详情
export const getAgent = async (id: number): Promise<Agent> => {
  const response = await get<ApiResponse<Agent>>(`/v1/agents/${id}`)
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取智能体详情失败')
}

// 创建智能体
export const createAgent = async (data: CreateAgentRequest): Promise<Agent> => {
  const response = await post<ApiResponse<Agent>>('/v1/agents', data)
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '创建智能体失败')
}

// 更新智能体
export const updateAgent = async (id: number, data: UpdateAgentRequest): Promise<Agent> => {
  const response = await put<ApiResponse<Agent>>(`/v1/agents/${id}`, data)
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '更新智能体失败')
}

// 删除智能体
export const deleteAgent = async (id: number): Promise<void> => {
  const response = await del<ApiResponse<void>>(`/v1/agents/${id}`)
  if (response.code !== 200) {
    throw new Error(response.message || '删除智能体失败')
  }
}

// 测试智能体
export const testAgent = async (id: number, question: string): Promise<string> => {
  const response = await post<ApiResponse<TestAgentResponse>>(
    `/v1/agents/${id}/test`,
    { question }
  )
  if (response.code === 200 && response.data) {
    return response.data.answer
  }
  throw new Error(response.message || '测试智能体失败')
}

// 发布智能体
export const publishAgent = async (id: number): Promise<Agent> => {
  const response = await post<ApiResponse<Agent>>(`/v1/agents/${id}/publish`, {})
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '发布智能体失败')
}

// 与智能体对话
export const chatWithAgent = async (id: number, question: string): Promise<ChatResponse> => {
  const response = await post<ApiResponse<ChatResponse>>(
    `/v1/agents/${id}/chat`,
    { question }
  )
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '对话失败')
}
