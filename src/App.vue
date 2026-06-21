<script setup>
import { ref, onMounted } from 'vue'
import LoginForm from './components/loginform.vue'
import Dashboard from './components/Dashboard.vue'
import { authApi } from './services/api.js'

const currentPage = ref('login')
const isLoggedIn = ref(false)
const userInfo = ref(null)
const loading = ref(true)

const clearAuth = () => {
  isLoggedIn.value = false
  userInfo.value = null
  localStorage.removeItem('userInfo')
  localStorage.removeItem('isLoggedIn')
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  currentPage.value = 'login'
}

const checkLoginStatus = () => {
  const storedToken = localStorage.getItem('token')
  const storedUserInfo = localStorage.getItem('userInfo')
  const storedIsLoggedIn = localStorage.getItem('isLoggedIn')

  if (!storedToken || !storedUserInfo || storedIsLoggedIn !== 'true') {
    clearAuth()
    return
  }

  try {
    const parsedUserInfo = JSON.parse(storedUserInfo)
    if (parsedUserInfo.role !== 'admin') {
      clearAuth()
      return
    }
    isLoggedIn.value = true
    userInfo.value = parsedUserInfo
    currentPage.value = 'dashboard'
  } catch (error) {
    clearAuth()
  }
}

const handleLoginSuccess = (data) => {
  if (data.userInfo?.role !== 'admin') {
    clearAuth()
    return
  }
  isLoggedIn.value = true
  userInfo.value = data.userInfo
  currentPage.value = 'dashboard'
}

const handleLogout = async () => {
  try {
    if (userInfo.value) await authApi.logout(userInfo.value.id, 'admin')
  } catch (error) {
    console.error('Logout failed:', error)
  } finally {
    clearAuth()
  }
}

onMounted(() => {
  checkLoginStatus()
  loading.value = false
})
</script>

<template>
  <div id="app">
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在加载...</p>
    </div>

    <LoginForm
      v-else-if="currentPage === 'login'"
      @login-success="handleLoginSuccess"
    />

    <Dashboard
      v-else-if="currentPage === 'dashboard'"
      :user-info="userInfo"
      @logout="handleLogout"
    />
  </div>
</template>

<style>
* { box-sizing: border-box; }
html, body, #app { min-height: 100vh; margin: 0; }
body { font-family: Inter, 'Microsoft YaHei', Arial, sans-serif; background: #f9fafb; color: #111827; }
button, input, select, textarea { font-family: inherit; }
.loading-container {
  min-height: 100vh;
  display: grid;
  place-items: center;
  gap: 12px;
  color: #6b7280;
}
.loading-spinner {
  width: 34px;
  height: 34px;
  border: 3px solid #e5e7eb;
  border-top-color: #2563eb;
  border-radius: 999px;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
