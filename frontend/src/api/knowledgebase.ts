// 知识库相关 API
import { get, post, put, del } from '../utils/request'

export interface KnowledgeBase {
  id: number
  userId: number
  name: string
  description?: string
  createdAt: string
  updatedAt: string
}

export interface CreateKnowledgeBaseRequest {
  name: string
  description?: string
}

export interface KnowledgeDocument {
  id: number
  knowledgeBaseId: number
  userId: number
  fileName: string
  fileType: string
  fileSize: number
  status: string // processing/processed/failed
  chunkCount: number
  vectorized: boolean
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

// API 响应格式
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 获取知识库列表
export const getKnowledgeBases = async (): Promise<KnowledgeBase[]> => {
  const response = await get<ApiResponse<KnowledgeBase[]>>('/v1/knowledge-bases')
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取知识库列表失败')
}

// 获取知识库详情
export const getKnowledgeBase = async (id: number): Promise<KnowledgeBase> => {
  const response = await get<ApiResponse<KnowledgeBase>>(`/v1/knowledge-bases/${id}`)
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取知识库详情失败')
}

// 创建知识库
export const createKnowledgeBase = async (data: CreateKnowledgeBaseRequest): Promise<KnowledgeBase> => {
  const response = await post<ApiResponse<KnowledgeBase>>('/v1/knowledge-bases', data)
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '创建知识库失败')
}

// 更新知识库
export const updateKnowledgeBase = async (id: number, data: CreateKnowledgeBaseRequest): Promise<KnowledgeBase> => {
  const response = await put<ApiResponse<KnowledgeBase>>(`/v1/knowledge-bases/${id}`, data)
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '更新知识库失败')
}

// 删除知识库
export const deleteKnowledgeBase = async (id: number): Promise<void> => {
  const response = await del<ApiResponse<void>>(`/v1/knowledge-bases/${id}`)
  if (response.code !== 200) {
    throw new Error(response.message || '删除知识库失败')
  }
}

// 获取知识库的文档列表
export const getDocuments = async (knowledgeBaseId: number): Promise<KnowledgeDocument[]> => {
  const response = await get<ApiResponse<KnowledgeDocument[]>>(
    `/v1/knowledge-bases/${knowledgeBaseId}/documents`
  )
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取文档列表失败')
}

// 上传文档
export const uploadDocument = async (
  knowledgeBaseId: number,
  file: File
): Promise<KnowledgeDocument> => {
  const formData = new FormData()
  formData.append('file', file)

  const token = localStorage.getItem('auth_token')
  const headers: HeadersInit = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(
    `/api/v1/knowledge-bases/${knowledgeBaseId}/documents/upload`,
    {
      method: 'POST',
      headers,
      body: formData,
    }
  )

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new Error(error.message || '上传文档失败')
  }

  const result = await response.json()
  if (result.code === 200 && result.data) {
    return result.data
  }
  throw new Error(result.message || '上传文档失败')
}

// 删除文档
export const deleteDocument = async (knowledgeBaseId: number, documentId: number): Promise<void> => {
  const response = await del<ApiResponse<void>>(
    `/v1/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`
  )
  if (response.code !== 200) {
    throw new Error(response.message || '删除文档失败')
  }
}

