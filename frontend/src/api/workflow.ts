// 工作流相关 API
import { get, post, put, del } from '../utils/request'

export interface WorkflowNode {
  id: string
  type: string // start/end/agent/condition/action
  name: string
  config?: Record<string, any>
  position?: {
    x: number
    y: number
  }
}

export interface WorkflowEdge {
  id: string
  source: string
  target: string
  condition?: string
}

export interface WorkflowDefinition {
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
}

export interface Workflow {
  id: number
  userId: number
  name: string
  description?: string
  definition: WorkflowDefinition
  status: string
  createdAt: string
  updatedAt: string
}

export interface CreateWorkflowRequest {
  name: string
  description?: string
  definition: WorkflowDefinition
}

export interface ExecuteWorkflowRequest {
  inputParams: Record<string, any>
}

export interface WorkflowExecution {
  id: number
  workflowId: number
  userId: number
  status: string // pending/running/completed/failed
  inputParams?: Record<string, any>
  outputResult?: Record<string, any>
  errorMessage?: string
  startedAt?: string
  completedAt?: string
  createdAt: string
}

// API 响应格式
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 获取工作流列表
export const getWorkflows = async (): Promise<Workflow[]> => {
  const response = await get<ApiResponse<Workflow[]>>('/v1/workflows')
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取工作流列表失败')
}

// 获取工作流详情
export const getWorkflow = async (id: number): Promise<Workflow> => {
  const response = await get<ApiResponse<Workflow>>(`/v1/workflows/${id}`)
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取工作流详情失败')
}

// 创建工作流
export const createWorkflow = async (data: CreateWorkflowRequest): Promise<Workflow> => {
  const response = await post<ApiResponse<Workflow>>('/v1/workflows', data)
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '创建工作流失败')
}

// 更新工作流
export const updateWorkflow = async (id: number, data: CreateWorkflowRequest): Promise<Workflow> => {
  const response = await put<ApiResponse<Workflow>>(`/v1/workflows/${id}`, data)
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '更新工作流失败')
}

// 删除工作流
export const deleteWorkflow = async (id: number): Promise<void> => {
  const response = await del<ApiResponse<void>>(`/v1/workflows/${id}`)
  if (response.code !== 200) {
    throw new Error(response.message || '删除工作流失败')
  }
}

// 执行工作流
export const executeWorkflow = async (
  workflowId: number,
  data: ExecuteWorkflowRequest
): Promise<WorkflowExecution> => {
  const response = await post<ApiResponse<WorkflowExecution>>(
    `/v1/workflows/${workflowId}/executions`,
    data
  )
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '执行工作流失败')
}

// 获取工作流的执行记录
export const getWorkflowExecutions = async (workflowId: number): Promise<WorkflowExecution[]> => {
  const response = await get<ApiResponse<WorkflowExecution[]>>(
    `/v1/workflows/${workflowId}/executions`
  )
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取执行记录失败')
}

// 获取执行记录详情
export const getWorkflowExecution = async (
  workflowId: number,
  executionId: number
): Promise<WorkflowExecution> => {
  const response = await get<ApiResponse<WorkflowExecution>>(
    `/v1/workflows/${workflowId}/executions/${executionId}`
  )
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取执行记录详情失败')
}

