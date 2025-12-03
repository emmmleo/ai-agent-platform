// HTTP 请求工具函数

const BASE_URL = "/api"
const TOKEN_KEY = 'auth_token'

// 获取 token
export const getToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY)
}

// 保存 token
export const setToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token)
}

// 清除 token
export const removeToken = (): void => {
  localStorage.removeItem(TOKEN_KEY)
}

export interface RequestOptions extends RequestInit {
  params?: Record<string, any>
  skipAuth?: boolean // 是否跳过认证（用于登录、注册等接口）
}

export const request = async <T = any>(
  url: string,
  options?: RequestOptions
): Promise<T> => {
  // 处理查询参数
  let finalUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`
  
  if (options?.params) {
    const params = new URLSearchParams()
    Object.entries(options.params).forEach(([key, value]) => {
      if (value !== null && value !== undefined) {
        params.append(key, String(value))
      }
    })
    const queryString = params.toString()
    if (queryString) {
      finalUrl += `?${queryString}`
    }
    delete options.params
  }

  // 设置默认请求头
  const headers = new Headers(options?.headers)
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  // 添加 JWT Token（如果需要认证且未跳过）
  if (!options?.skipAuth) {
    const token = getToken()
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
  }

  const response = await fetch(finalUrl, {
    ...options,
    headers,
  })

  if (!response.ok) {
    // 401 未授权，清除 token
    if (response.status === 401) {
      removeToken()
    }
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new Error(error.message || `HTTP error! status: ${response.status}`)
  }

  return response.json()
}

// GET 请求
export const get = <T = any>(url: string, params?: Record<string, any>): Promise<T> => {
  return request<T>(url, { method: 'GET', params })
}

// POST 请求
export const post = <T = any>(url: string, data?: any, options?: RequestOptions): Promise<T> => {
  return request<T>(url, {
    method: 'POST',
    body: JSON.stringify(data),
    ...options,
  })
}

// PUT 请求
export const put = <T = any>(url: string, data?: any): Promise<T> => {
  return request<T>(url, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

// DELETE 请求
export const del = <T = any>(url: string): Promise<T> => {
  return request<T>(url, { method: 'DELETE' })
}

