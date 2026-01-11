<template>
  <div class="register-container">
    <div class="register-box">
      <h1>注册</h1>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名</label>
          <input
            v-model="form.username"
            type="text"
            required
            minlength="3"
            maxlength="32"
            placeholder="请输入用户名（3-32个字符）"
            :disabled="loading"
          />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            required
            minlength="6"
            maxlength="64"
            placeholder="请输入密码（6-64个字符）"
            :disabled="loading"
          />
        </div>
        <div v-if="error" class="error-message">{{ error }}</div>
        <button type="submit" :disabled="loading" class="submit-btn">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <div class="login-link">
          已有账号？
          <router-link to="/login">立即登录</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const form = ref({
  username: '',
  password: '',
})

const loading = ref(false)
const error = ref('')

const handleRegister = async () => {
  if (!form.value.username || !form.value.password) {
    error.value = '请填写用户名和密码'
    return
  }

  if (form.value.username.length < 3 || form.value.username.length > 32) {
    error.value = '用户名长度需3-32个字符'
    return
  }

  if (form.value.password.length < 6 || form.value.password.length > 64) {
    error.value = '密码长度需6-64个字符'
    return
  }

  loading.value = true
  error.value = ''

  try {
    await userStore.register(form.value.username, form.value.password)
    // 注册成功，跳转到首页
    router.push('/')
  } catch (e: any) {
    error.value = e.message || '注册失败，用户名可能已存在'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
}

.register-box {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #2c3e50;
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 8px;
  color: #555;
  font-weight: 500;
}

input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  transition: border-color 0.3s;
}

input:focus {
  outline: none;
  border-color: #42b983;
}

input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.error-message {
  color: #f44336;
  margin-bottom: 15px;
  padding: 10px;
  background: #ffebee;
  border-radius: 4px;
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.3s;
  margin-bottom: 15px;
}

.submit-btn:hover:not(:disabled) {
  background: #35a372;
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.login-link {
  text-align: center;
  color: #666;
  font-size: 14px;
}

.login-link a {
  color: #42b983;
  text-decoration: none;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>

