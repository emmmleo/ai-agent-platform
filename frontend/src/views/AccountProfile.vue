<template>
  <div class="profile">
    <div class="page-header">
      <h2>个人信息</h2>
      <button type="button" class="btn-secondary" @click="goBack">返回菜单</button>
    </div>

    <!-- 资料卡（展示） -->
    <div v-if="!isEditing" class="cards">
      <div class="section-card profile-card">
        <div class="avatar">
          <img v-if="userStore.user?.avatarUrl" :src="userStore.user?.avatarUrl" alt="avatar" />
          <div v-else class="avatar-fallback">{{ initials }}</div>
        </div>
        <div class="profile-meta">
          <div class="name">{{ userStore.user?.username || '-' }}</div>
          <div class="sub">{{ roleLabel }}</div>
        </div>
        <div class="profile-actions">
          <button type="button" @click="startEdit">修改个人信息</button>
        </div>
      </div>

      <div class="section-card">
        <div class="section-title">基础信息</div>
        <div class="info-grid">
          <div class="info-item">
            <div class="info-label">性别</div>
            <div class="info-value">{{ genderLabel(userStore.user?.gender) }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">生日</div>
            <div class="info-value">{{ userStore.user?.birthday || '未填写' }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">学校</div>
            <div class="info-value">{{ userStore.user?.school || '未填写' }}</div>
          </div>
        </div>
      </div>

      <div class="section-card">
        <div class="section-title">联系方式</div>
        <div class="info-grid">
          <div class="info-item">
            <div class="info-label">电话</div>
            <div class="info-value">{{ userStore.user?.phone || '未填写' }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">邮箱</div>
            <div class="info-value">{{ userStore.user?.email || '未填写' }}</div>
          </div>
        </div>
      </div>

      <div class="section-card">
        <div class="section-title">个性签名</div>
        <div class="bio">{{ userStore.user?.bio || '未填写' }}</div>
      </div>

      <div class="footer">
        <span v-if="message" class="message" :class="messageType">{{ message }}</span>
      </div>
    </div>

    <!-- 编辑卡片 -->
    <form v-else class="cards" @submit.prevent="onSubmit">
      <div class="section-card">
        <div class="section-title">头像</div>
        <div class="avatar-edit">
          <div class="avatar avatar-lg">
            <img v-if="form.avatarUrl" :src="form.avatarUrl" alt="avatar" />
            <div v-else class="avatar-fallback">{{ initials }}</div>
          </div>
          <div class="avatar-controls">
            <div class="form-item">
              <label>头像地址（URL 或 DataURL）</label>
              <input v-model="form.avatarUrl" type="text" placeholder="https://... 或 data:image/..." maxlength="512" />
            </div>
            <input ref="avatarFileInput" class="hidden" type="file" accept="image/*" @change="onPickAvatar" />
            <div class="row-actions">
              <button type="button" class="btn-secondary" @click="pickAvatar">选择图片</button>
              <button type="button" class="btn-secondary" @click="clearAvatar">移除头像</button>
            </div>
          </div>
        </div>
      </div>

      <div class="section-card">
        <div class="section-title">基础信息</div>
        <div class="form-grid">
          <div class="form-item">
            <label>用户名</label>
            <input type="text" :value="userStore.user?.username" disabled />
          </div>
          <div class="form-item">
            <label>性别</label>
            <select v-model="form.gender">
              <option value="">未填写</option>
              <option value="UNKNOWN">保密</option>
              <option value="MALE">男</option>
              <option value="FEMALE">女</option>
            </select>
          </div>
          <div class="form-item">
            <label>生日</label>
            <input v-model="form.birthday" type="date" />
          </div>
          <div class="form-item">
            <label>学校</label>
            <input v-model="form.school" type="text" placeholder="填写学校" maxlength="128" />
          </div>
        </div>
      </div>

      <div class="section-card">
        <div class="section-title">联系方式</div>
        <div class="form-grid">
          <div class="form-item">
            <label>电话号码</label>
            <input v-model="form.phone" type="text" placeholder="填写电话号码" maxlength="32" />
          </div>
          <div class="form-item">
            <label>邮箱</label>
            <input v-model="form.email" type="email" placeholder="填写邮箱" maxlength="128" />
          </div>
        </div>
      </div>

      <div class="section-card">
        <div class="section-title">个性签名</div>
        <textarea v-model="form.bio" rows="3" placeholder="填写个性签名" maxlength="512" />
      </div>

      <div class="footer actions">
        <button type="submit" :disabled="saving">
          {{ saving ? '保存中...' : '保存' }}
        </button>
        <button type="button" class="btn-secondary" :disabled="saving" @click="cancelEdit">
          取消
        </button>
        <span v-if="message" class="message" :class="messageType">{{ message }}</span>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { updateUserProfile } from '../api/user'

const userStore = useUserStore()
const router = useRouter()
const saving = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const isEditing = ref(false)

const avatarFileInput = ref<HTMLInputElement | null>(null)

const form = reactive({
  avatarUrl: '',
  gender: '',
  birthday: '',
  school: '',
  phone: '',
  email: '',
  bio: '',
})

const initials = computed(() => {
  const name = userStore.user?.username || ''
  return name ? name.slice(0, 1).toUpperCase() : 'U'
})

const roleLabel = computed(() => {
  const role = userStore.user?.role
  if (role === 'ROLE_ADMIN') return '管理员'
  if (role === 'ROLE_USER') return '普通用户'
  return role || ''
})

const genderLabel = (gender?: string) => {
  if (!gender) return '未填写'
  const upper = gender.toUpperCase()
  if (upper === 'MALE') return '男'
  if (upper === 'FEMALE') return '女'
  if (upper === 'UNKNOWN') return '保密'
  return gender
}

const loadInitial = () => {
  form.avatarUrl = userStore.user?.avatarUrl || ''
  form.gender = userStore.user?.gender || ''
  form.birthday = (userStore.user?.birthday || '').slice(0, 10)
  form.school = userStore.user?.school || ''
  form.phone = userStore.user?.phone || ''
  form.email = userStore.user?.email || ''
  form.bio = userStore.user?.bio || ''
}

onMounted(() => {
  if (!userStore.user) {
    userStore.fetchUserProfile().then(() => {
      loadInitial()
    })
  } else {
    loadInitial()
  }
})

const startEdit = () => {
  loadInitial()
  message.value = ''
  isEditing.value = true
}

const cancelEdit = () => {
  loadInitial()
  message.value = ''
  isEditing.value = false
}

const goBack = () => {
  router.push('/')
}

const pickAvatar = () => {
  avatarFileInput.value?.click()
}

const clearAvatar = () => {
  form.avatarUrl = ''
}

const onPickAvatar = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  // 兜底限制：避免把超大图片转成 DataURL 后提交触发 413
  const maxInputBytes = 5 * 1024 * 1024
  if (file.size > maxInputBytes) {
    messageType.value = 'error'
    message.value = '图片过大（>5MB），请换小一点的头像'
    input.value = ''
    return
  }

  const dataUrl = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('读取图片失败'))
    reader.readAsDataURL(file)
  })

  // 压缩与缩放到合理尺寸，减少请求体大小
  const compressed = await compressImageDataUrl(dataUrl, 256, 0.82)
  form.avatarUrl = compressed
  input.value = ''
}

const compressImageDataUrl = (dataUrl: string, maxSize: number, quality: number) => {
  return new Promise<string>((resolve) => {
    const img = new Image()
    img.onload = () => {
      const ratio = Math.min(1, maxSize / Math.max(img.width, img.height))
      const w = Math.max(1, Math.round(img.width * ratio))
      const h = Math.max(1, Math.round(img.height * ratio))
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        resolve(dataUrl)
        return
      }
      ctx.drawImage(img, 0, 0, w, h)
      // 用 jpeg 压缩体积；大多数头像场景可接受
      resolve(canvas.toDataURL('image/jpeg', quality))
    }
    img.onerror = () => resolve(dataUrl)
    img.src = dataUrl
  })
}

const onSubmit = async () => {
  message.value = ''
  saving.value = true
  try {
    const updated = await updateUserProfile({
      school: form.school || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined,
      bio: form.bio || undefined,
      avatarUrl: form.avatarUrl || undefined,
      gender: form.gender || undefined,
      birthday: form.birthday || undefined,
    })
    userStore.setUser(updated as any)
    messageType.value = 'success'
    message.value = '保存成功'
    isEditing.value = false
  } catch (e: any) {
    messageType.value = 'error'
    message.value = e?.message || '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile {
  min-height: 100vh;
  padding: 30px;
  background: #f5f7fb;
}
.page-header {
  max-width: 900px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cards {
  max-width: 900px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}
.section-card {
  background: #fff;
  padding: 18px;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
}
.section-title {
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 12px;
}
.profile-card {
  display: flex;
  align-items: center;
  gap: 16px;
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 999px;
  overflow: hidden;
  background: #ecf0f7;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e0e6ed;
}
.avatar-lg {
  width: 92px;
  height: 92px;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-fallback {
  font-weight: 800;
  color: #5b6b7a;
  font-size: 26px;
}
.profile-meta {
  flex: 1;
}
.name {
  font-size: 20px;
  font-weight: 800;
  color: #2c3e50;
}
.sub {
  margin-top: 4px;
  color: #7f8c8d;
}
.profile-actions {
  display: flex;
  gap: 10px;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.info-item {
  background: #f7f9fd;
  border: 1px solid #e9eef7;
  border-radius: 10px;
  padding: 12px;
}
.info-label {
  color: #7f8c8d;
  font-size: 12px;
}
.info-value {
  margin-top: 6px;
  color: #2c3e50;
  font-weight: 600;
  word-break: break-word;
}
.bio {
  color: #2c3e50;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.form-item {
  margin-bottom: 0;
}
label {
  display: block;
  margin-bottom: 8px;
  color: #34495e;
  font-weight: 600;
}
input, textarea, select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
}
.avatar-edit {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  align-items: start;
}
.avatar-controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.row-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.hidden {
  display: none;
}
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}
.footer {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 12px;
}
button {
  padding: 10px 18px;
  background: #667eea;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-secondary {
  background: #ecf0f7;
  color: #34495e;
}
.message {
  font-size: 14px;
}
.message.success { color: #2ecc71; }
.message.error { color: #e74c3c; }

@media (max-width: 720px) {
  .profile {
    padding: 18px;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .avatar-edit {
    grid-template-columns: 1fr;
  }
}
</style>
