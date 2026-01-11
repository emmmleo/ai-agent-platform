// 用户相关 API
import { get, patch } from '../utils/request'

export interface UserProfile {
  id: number
  username: string
  role: string
  createdAt: string
  updatedAt: string
  school?: string
  phone?: string
  email?: string
  bio?: string
  avatarUrl?: string
  gender?: string
  birthday?: string
}

// 获取用户信息
export const getUserProfile = async (): Promise<UserProfile> => {
  return get<UserProfile>('/v1/user/profile')
}

export interface UserProfileUpdateRequest {
  school?: string
  phone?: string
  email?: string
  bio?: string
  avatarUrl?: string
  gender?: string
  birthday?: string
}

export const updateUserProfile = async (payload: UserProfileUpdateRequest): Promise<UserProfile> => {
  return patch<UserProfile>('/v1/user/profile', payload)
}
