// 管理员相关 API
import { get, del } from '../utils/request'
import type { UserProfile } from './user'

// 重新导出 UserProfile 类型供其他模块使用
export type { UserProfile }

// 用户列表响应
export interface UserListResponse {
  code: number
  message: string
  data: UserProfile[]
}

// 获取所有用户列表
export const getAllUsers = async (): Promise<UserProfile[]> => {
  const response = await get<UserListResponse>('/v1/admin/users')
  if (response.code === 200 && response.data) {
    return response.data
  }
  throw new Error(response.message || '获取用户列表失败')
}

// 删除用户
export const deleteUser = async (username: string): Promise<void> => {
  await del(`/v1/admin/users/${username}`)
}
