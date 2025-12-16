// 用户相关 API
import { get } from '../utils/request'

export interface UserProfile {
  id: number
  username: string
  role: string
  createdAt: string
  updatedAt: string
}

// 获取用户信息
export const getUserProfile = async (): Promise<UserProfile> => {
  return get<UserProfile>('/v1/user/profile')
}

