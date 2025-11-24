// Token 管理工具

const TOKEN_KEY = 'auth_token'
const USER_KEY = 'user_info'

/**
 * 保存 token
 */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 获取 token
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 删除 token
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 保存用户信息
 */
export function setUserInfo(userInfo) {
  localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  const userInfo = localStorage.getItem(USER_KEY)
  console.log('[getUserInfo] localStorage原始数据:', userInfo)
  
  if (!userInfo) {
    console.log('[getUserInfo] 无用户信息')
    return null
  }
  
  try {
    const parsed = JSON.parse(userInfo)
    console.log('[getUserInfo] 解析后的数据:', parsed)
    
    // 验证数据完整性
    if (!parsed || typeof parsed !== 'object') {
      console.error('[getUserInfo] 用户信息格式错误')
      clearAuth()
      return null
    }
    
    // 检查必需字段
    if (!parsed.userId || !parsed.username || !parsed.role) {
      console.error('[getUserInfo] 用户信息缺少必需字段:', parsed)
      console.error('[getUserInfo] 清除不完整的认证信息')
      clearAuth()
      return null
    }
    
    return parsed
  } catch (error) {
    console.error('[getUserInfo] 解析用户信息失败:', error)
    clearAuth()
    return null
  }
}

/**
 * 删除用户信息
 */
export function removeUserInfo() {
  localStorage.removeItem(USER_KEY)
}

/**
 * 清除所有认证信息
 */
export function clearAuth() {
  removeToken()
  removeUserInfo()
}

/**
 * 检查是否已登录
 */
export function isAuthenticated() {
  return !!getToken()
}

/**
 * 检查当前用户是否为管理员
 */
export function isAdmin() {
  const userInfo = getUserInfo()
  console.log('[isAdmin] 用户信息:', userInfo)
  console.log('[isAdmin] role字段:', userInfo?.role)
  console.log('[isAdmin] 是否为admin:', userInfo && userInfo.role === 'admin')
  return userInfo && userInfo.role === 'admin'
}

