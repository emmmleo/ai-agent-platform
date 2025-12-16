// 插件相关 API
import { get, del } from '../utils/request'

export interface Plugin {
  id: number
  userId: number
  name: string
  description?: string
  openapiSpec?: any // OpenAPI规范对象
  enabled: boolean
  createdAt: string
  updatedAt: string
}

// API 响应格式
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 注册插件
export const registerPlugin = async (
  name: string,
  description: string,
  file: File
): Promise<Plugin> => {
  const formData = new FormData()
  formData.append('name', name)
  formData.append('description', description || '')
  formData.append('openapiFile', file)

  const token = localStorage.getItem('auth_token')
  const headers: HeadersInit = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const response = await fetch(`/api/v1/plugins/register`, {
    method: 'POST',
    headers,
    body: formData,
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new Error(error.message || '注册插件失败')
  }

  const result = await response.json()
  if (result.code === 200 && result.data) {
    return result.data
  }
  throw new Error(result.message || '注册插件失败')
}

// 获取插件列表
export const getPlugins = async (): Promise<Plugin[]> => {
  const response = await get<ApiResponse<Plugin[]>>('/v1/plugins')
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取插件列表失败')
}

// 获取启用的插件列表
export const getEnabledPlugins = async (): Promise<Plugin[]> => {
  const response = await get<ApiResponse<Plugin[]>>('/v1/plugins/enabled')
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取启用插件列表失败')
}

// 获取插件详情
export const getPlugin = async (id: number): Promise<Plugin> => {
  const response = await get<ApiResponse<Plugin>>(`/v1/plugins/${id}`)
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取插件详情失败')
}

// 更新插件
export const updatePlugin = async (
  id: number,
  name: string,
  description: string,
  file?: File
): Promise<Plugin> => {
  const formData = new FormData()
  formData.append('name', name)
  formData.append('description', description || '')
  if (file) {
    formData.append('openapiFile', file)
  }

  const token = localStorage.getItem('auth_token')
  const headers: HeadersInit = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`/api/v1/plugins/${id}`, {
    method: 'PUT',
    headers,
    body: formData,
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new Error(error.message || '更新插件失败')
  }

  const result = await response.json()
  if (result.code === 200 && result.data) {
    return result.data
  }
  throw new Error(result.message || '更新插件失败')
}

// 启用/禁用插件
export const togglePlugin = async (id: number, enabled: boolean): Promise<Plugin> => {
  const token = localStorage.getItem('auth_token')
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`/api/v1/plugins/${id}/toggle?enabled=${enabled}`, {
    method: 'PATCH',
    headers,
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new Error(error.message || '切换插件状态失败')
  }

  const result = await response.json()
  if (result.code === 200 && result.data) {
    return result.data
  }
  throw new Error(result.message || '切换插件状态失败')
}

// 删除插件
export const deletePlugin = async (id: number): Promise<void> => {
  const response = await del<ApiResponse<void>>(`/v1/plugins/${id}`)
  if (response.code !== 200) {
    throw new Error(response.message || '删除插件失败')
  }
}

