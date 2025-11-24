<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-50 via-indigo-50 to-purple-100">
    <nav class="bg-white/90 backdrop-blur shadow-sm">
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="h-16 flex items-center justify-between">
          <div class="flex items-center space-x-4">
            <router-link to="/home" class="text-sm text-slate-500 hover:text-indigo-600 transition">
              ← 返回首页
            </router-link>
            <h1 class="text-xl font-semibold text-slate-800">个人信息</h1>
          </div>
          <div class="text-sm text-slate-500 hidden sm:block">
            用户 ID：<span class="font-medium text-slate-700">{{ form.userId ?? '-' }}</span>
          </div>
        </div>
      </div>
    </nav>

    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div class="bg-white rounded-2xl shadow-xl overflow-hidden border border-slate-100">
        <div class="px-8 py-6 bg-gradient-to-r from-indigo-500/90 to-purple-600/90 text-white">
          <h2 class="text-2xl font-semibold">账户资料</h2>
          <p class="text-sm text-indigo-100 mt-1">查看与维护您的基础资料，确保信息最新准确。</p>
        </div>

        <div class="p-8">
          <div v-if="loading" class="flex flex-col items-center justify-center py-20 space-y-4 text-slate-500">
            <div class="animate-spin rounded-full h-12 w-12 border-4 border-indigo-500 border-t-transparent"></div>
            <p class="text-sm">正在加载个人资料...</p>
          </div>

          <form v-else @submit.prevent="handleSave" class="space-y-8">
            <div v-if="errorMessage" class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
              {{ errorMessage }}
            </div>
            <div v-if="successMessage" class="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-600">
              {{ successMessage }}
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-medium text-slate-600 mb-2">用户名</label>
                <input
                  v-model="form.username"
                  type="text"
                  required
                  class="w-full rounded-xl border border-slate-200 px-4 py-3 text-slate-800 shadow-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 outline-none transition"
                  placeholder="请输入用户名"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-slate-600 mb-2">账户角色</label>
                <input
                  :value="roleLabel"
                  type="text"
                  disabled
                  class="w-full rounded-xl border border-slate-200 px-4 py-3 text-slate-500 bg-slate-50 shadow-sm cursor-not-allowed"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-slate-600 mb-2">邮箱</label>
                <input
                  v-model="form.email"
                  type="email"
                  class="w-full rounded-xl border border-slate-200 px-4 py-3 text-slate-800 shadow-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 outline-none transition"
                  placeholder="name@example.com"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-slate-600 mb-2">联系电话</label>
                <input
                  v-model="form.phone"
                  type="tel"
                  class="w-full rounded-xl border border-slate-200 px-4 py-3 text-slate-800 shadow-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 outline-none transition"
                  placeholder="请输入常用联系方式"
                />
              </div>

              <div class="md:col-span-2">
                <label class="block text-sm font-medium text-slate-600 mb-2">所属单位</label>
                <input
                  v-model="form.organization"
                  type="text"
                  class="w-full rounded-xl border border-slate-200 px-4 py-3 text-slate-800 shadow-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 outline-none transition"
                  placeholder="请输入所属单位或部门"
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-600 mb-2">个人简介</label>
              <textarea
                v-model="form.bio"
                rows="4"
                class="w-full rounded-xl border border-slate-200 px-4 py-3 text-slate-800 shadow-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 outline-none transition resize-none"
                placeholder="介绍一下自己吧，工作方向、兴趣爱好等"
              ></textarea>
            </div>

            <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pt-4 border-t border-slate-100">
              <div class="text-xs text-slate-500">
                上次更新：{{ lastUpdatedText }}
              </div>
              <div class="flex flex-col sm:flex-row gap-3">
                <button
                  type="button"
                  class="w-full sm:w-auto rounded-xl border border-indigo-100 bg-indigo-50 px-6 py-3 text-sm font-semibold text-indigo-600 hover:bg-indigo-100 transition"
                  @click="handleManageUsers"
                >
                  管理用户
                </button>
                <button
                  type="submit"
                  :disabled="saving"
                  class="w-full sm:w-auto rounded-xl bg-gradient-to-r from-indigo-500 to-purple-600 px-8 py-3 text-sm font-semibold text-white shadow-lg hover:from-indigo-600 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {{ saving ? '保存中...' : '保存修改' }}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/utils/http'
import { setToken, setUserInfo, getUserInfo } from '@/utils/auth'

const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const successMessage = ref('')
const errorMessage = ref('')
const lastUpdated = ref(null)

const form = reactive({
  userId: null,
  username: '',
  email: '',
  organization: '',
  phone: '',
  bio: '',
  role: ''
})

const roleLabel = computed(() => (form.role === 'admin' ? '管理员' : '普通用户'))
const isAdminUser = computed(() => form.role === 'admin')

const lastUpdatedText = computed(() => {
  if (!lastUpdated.value) return '—'
  return new Date(lastUpdated.value).toLocaleString('zh-CN')
})

const syncLocalUserInfo = (data) => {
  setUserInfo({
    userId: data.userId,
    username: data.username,
    role: data.role,
    email: data.email ?? null,
    organization: data.organization ?? null,
    phone: data.phone ?? null,
    bio: data.bio ?? null
  })
}

const assignForm = (data) => {
  form.userId = data.userId ?? null
  form.username = data.username ?? ''
  form.email = data.email ?? ''
  form.organization = data.organization ?? ''
  form.phone = data.phone ?? ''
  form.bio = data.bio ?? ''
  form.role = data.role ?? ''
}

const fetchProfile = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await http.get('/auth/me')
    const data = response.data?.data
    if (!data) {
      throw new Error('未获取到个人信息')
    }
    assignForm(data)
    syncLocalUserInfo(data)
    lastUpdated.value = Date.now()
  } catch (error) {
    console.error('[UserProfile] 加载失败:', error)
    errorMessage.value = error.message || '加载个人信息失败'
  } finally {
    loading.value = false
  }
}

const normalizeNullable = (value) => {
  if (value === null || value === undefined) return null
  const trimmed = String(value).trim()
  return trimmed.length ? trimmed : null
}

const handleSave = async () => {
  errorMessage.value = ''
  successMessage.value = ''

  if (!form.username || !form.username.trim()) {
    errorMessage.value = '用户名不能为空'
    return
  }

  saving.value = true
  try {
    const payload = {
      username: form.username.trim(),
      email: normalizeNullable(form.email),
      organization: normalizeNullable(form.organization),
      phone: normalizeNullable(form.phone),
      bio: normalizeNullable(form.bio)
    }
    const response = await http.put('/auth/me', payload)
    const data = response.data?.data
    if (!data) {
      throw new Error('更新个人信息失败')
    }

    assignForm(data)
    syncLocalUserInfo(data)
    if (data.token) {
      setToken(data.token)
    }
    lastUpdated.value = Date.now()
    successMessage.value = '个人信息已更新'
  } catch (error) {
    console.error('[UserProfile] 保存失败:', error)
    errorMessage.value = error.message || '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

const handleManageUsers = () => {
  successMessage.value = ''
  const localInfo = getUserInfo()
  if (!isAdminUser.value && (!localInfo || localInfo.role !== 'admin')) {
    errorMessage.value = '权限不足'
    return
  }
  router.push('/admin/users')
}

onMounted(() => {
  const cached = getUserInfo()
  if (cached) {
    form.userId = cached.userId ?? null
    form.role = cached.role ?? ''
  }
  fetchProfile()
})
</script>


