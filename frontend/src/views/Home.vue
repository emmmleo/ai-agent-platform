<template>
  <div class="home">
    <div class="header">
      <div class="header-content">
        <h1>欢迎回来, {{ userStore.userName }}!</h1>
        <p class="welcome-text">选择下方模块开始使用系统</p>
      </div>
      <div class="user-info">
        <div class="user-badge">
          <span class="role-badge">{{ userStore.userRole }}</span>
          <span v-if="userStore.isAdmin" class="admin-badge">管理员</span>
        </div>
        <button @click="handleLogout" class="logout-btn">
          <span class="logout-icon">🚪</span>
          退出登录
        </button>
      </div>
    </div>

    <div class="content">
      <div v-if="loadingMenus" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="menus.length > 0" class="modules-grid">
        <div
          v-for="menu in menus"
          :key="menu.id"
          class="module-card"
          @click="handleModuleClick(menu)"
        >
          <div class="card-icon">{{ getModuleIcon(menu.title) }}</div>
          <div class="card-content">
            <h3 class="card-title">{{ menu.title }}</h3>
            <p class="card-description">{{ getModuleDescription(menu.title) }}</p>
          </div>
          <div class="card-arrow">→</div>
        </div>
      </div>
      <div v-else class="no-menu">
        <div class="empty-state">
          <div class="empty-icon">📋</div>
          <p class="empty-title">暂无菜单</p>
          <p class="empty-text">请联系管理员配置菜单权限</p>
          <button @click="loadMenus" class="retry-btn">重试加载</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getMenus, type Menu } from '../api/menu'

const router = useRouter()
const userStore = useUserStore()

const menus = ref<Menu[]>([])
const loadingMenus = ref(false)

// 加载菜单
const loadMenus = async () => {
  loadingMenus.value = true
  try {
    const menuList = await getMenus()
    menus.value = menuList
  } catch (e: any) {
    console.error('加载菜单失败:', e)
  } finally {
    loadingMenus.value = false
  }
}

// 获取模块图标
const getModuleIcon = (title: string): string => {
  const iconMap: Record<string, string> = {
    '仪表盘': '📊',
    '个人信息': '👤',
    '智能体管理': '🤖',
    '知识库管理': '📚',
    '工作流管理': '⚙️',
    '插件管理': '🔌',
    '用户管理': '👥',
  }
  return iconMap[title] || '📦'
}

// 获取模块描述
const getModuleDescription = (title: string): string => {
  const descMap: Record<string, string> = {
    '仪表盘': '查看系统概览和统计数据',
    '个人信息': '管理个人账户信息和设置',
    '智能体管理': '创建和管理AI智能体',
    '知识库管理': '上传和管理知识文档',
    '工作流管理': '设计和管理工作流程',
    '插件管理': '注册和管理系统插件',
    '用户管理': '管理系统用户和权限',
  }
  return descMap[title] || '访问该功能模块'
}

// 处理模块点击
const handleModuleClick = (menu: Menu) => {
  router.push(menu.path)
}

// 退出登录
const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadMenus()
})
</script>

<style scoped>
.home {
  min-height: 100vh;
  padding: 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 30px 40px;
  border-radius: 16px;
  margin-bottom: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.header-content h1 {
  color: #2c3e50;
  margin: 0 0 8px 0;
  font-size: 32px;
  font-weight: 700;
}

.welcome-text {
  color: #666;
  margin: 0;
  font-size: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-badge {
  display: flex;
  gap: 10px;
  align-items: center;
}

.role-badge,
.admin-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.role-badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.admin-badge {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.3);
}

.logout-btn:hover {
  background: #d32f2f;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(244, 67, 54, 0.4);
}

.logout-icon {
  font-size: 16px;
}

.content {
  max-width: 1400px;
  margin: 0 auto;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: white;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.module-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 30px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.module-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}

.module-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.module-card:hover::before {
  transform: scaleX(1);
}

.card-icon {
  font-size: 64px;
  margin-bottom: 20px;
  transition: transform 0.3s ease;
}

.module-card:hover .card-icon {
  transform: scale(1.1) rotate(5deg);
}

.card-content {
  flex: 1;
  width: 100%;
}

.card-title {
  color: #2c3e50;
  margin: 0 0 10px 0;
  font-size: 22px;
  font-weight: 600;
}

.card-description {
  color: #666;
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  min-height: 44px;
}

.card-arrow {
  position: absolute;
  top: 20px;
  right: 20px;
  color: #667eea;
  font-size: 24px;
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.3s ease;
}

.module-card:hover .card-arrow {
  opacity: 1;
  transform: translateX(0);
}

.no-menu {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.empty-state {
  text-align: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 60px 40px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.empty-title {
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 10px 0;
}

.empty-text {
  color: #666;
  font-size: 16px;
  margin: 0 0 30px 0;
}

.retry-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.retry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .home {
    padding: 20px;
  }

  .header {
    flex-direction: column;
    gap: 20px;
    padding: 25px;
    text-align: center;
  }

  .header-content h1 {
    font-size: 24px;
  }

  .user-info {
    flex-direction: column;
    width: 100%;
  }

  .user-badge {
    justify-content: center;
  }

  .modules-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .module-card {
    padding: 24px;
  }

  .card-icon {
    font-size: 48px;
  }

  .card-title {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 20px;
  }

  .header-content h1 {
    font-size: 20px;
  }

  .welcome-text {
    font-size: 14px;
  }

  .module-card {
    padding: 20px;
  }

  .card-icon {
    font-size: 40px;
    margin-bottom: 15px;
  }

  .card-title {
    font-size: 18px;
  }

  .card-description {
    font-size: 13px;
  }
}
</style>
