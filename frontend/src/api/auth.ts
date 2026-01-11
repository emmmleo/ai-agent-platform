// 认证相关 API
import { post, type RequestOptions } from '../utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
}

export interface RegisterRequest {
  username: string
  password: string
}

export interface UserProfile {
  id: number
  username: string
  role: string
  createdAt: string
  updatedAt: string
}

// 登录
export const login = async (data: LoginRequest): Promise<LoginResponse> => {
  const options: RequestOptions = { skipAuth: true }
  return post<LoginResponse>('/v1/auth/login', data, options)
}

// 注册
export const register = async (data: RegisterRequest): Promise<UserProfile> => {
  const options: RequestOptions = { skipAuth: true }
  return post<UserProfile>('/v1/auth/register', data, options)
}

