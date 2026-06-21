<template>
  <div class="admin-login-page">
    <form class="login-card" @submit.prevent="handleLogin">
      <div class="brand-mark">YK</div>
      <h1>云逸授权中心</h1>
      <p>私有后台，仅限管理员登录</p>

      <label>
        管理员账号
        <input v-model.trim="form.username" autocomplete="username" placeholder="admin" :disabled="loading" />
      </label>

      <label>
        密码
        <input v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入密码" :disabled="loading" />
      </label>

      <button type="submit" :disabled="loading || !form.username || !form.password">
        {{ loading ? '登录中...' : '登录后台' }}
      </button>

      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { authApi } from '../services/api.js'

const emit = defineEmits(['login-success'])

const loading = ref(false)
const errorMessage = ref('')
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  if (!form.username || !form.password) return
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await authApi.loginAdmin(form.username, form.password)
    if (!response?.success || !response.data?.token) {
      throw new Error(response?.message || '用户名或密码错误')
    }

    const userInfo = response.data.userInfo || { username: form.username, role: 'admin' }
    localStorage.setItem('token', response.data.token)
    localStorage.setItem('refreshToken', response.data.refreshToken || '')
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    localStorage.setItem('isLoggedIn', 'true')
    emit('login-success', { userInfo })
  } catch (error) {
    errorMessage.value = error.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #f3f6fb;
}

.login-card {
  width: min(420px, 100%);
  display: grid;
  gap: 18px;
  padding: 34px;
  border: 1px solid #dfe5ef;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.brand-mark {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #111827;
  color: #fff;
  font-weight: 800;
  letter-spacing: 0.04em;
}

h1 { margin: 0; font-size: 24px; color: #111827; }
p { margin: -8px 0 4px; color: #6b7280; }
label { display: grid; gap: 8px; color: #374151; font-size: 14px; }
input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 12px 13px;
  font: inherit;
}
input:focus { outline: none; border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12); }
button {
  border: 0;
  border-radius: 8px;
  padding: 12px 16px;
  background: #2563eb;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}
button:disabled { opacity: 0.65; cursor: not-allowed; }
.error-message {
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 12px;
  color: #b91c1c;
  background: #fef2f2;
}
</style>
