<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-title">License Center</div>
      </template>
      <el-form :model="form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" placeholder="管理员账号" autocomplete="username" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="primary" class="login-button" :loading="loading" @click="login">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function login() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const { data } = await axios.post('/auth/admin/login', form)
    if (!data.success) {
      throw new Error(data.message || '登录失败')
    }
    const payload = data.data || {}
    if (payload.token) localStorage.setItem('token', payload.token)
    if (payload.refreshToken) localStorage.setItem('refreshToken', payload.refreshToken)
    if (payload.userInfo) localStorage.setItem('userInfo', JSON.stringify(payload.userInfo))
    localStorage.setItem('isLoggedIn', 'true')
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.login-card {
  width: 420px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 20px;
  transition: all 0.3s ease;
}

.login-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 25px 70px rgba(0, 0, 0, 0.4);
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  color: #000000 !important;
  text-shadow: 0 2px 4px rgba(255, 255, 255, 0.3);
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.25) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  color: #000000 !important;
  transition: all 0.3s ease;
}

.login-button:hover {
  background: rgba(255, 255, 255, 0.35) !important;
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.3);
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-input__inner) {
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: #000000 !important;
  backdrop-filter: blur(10px);
  height: 44px;
  font-size: 15px;
}

:deep(.el-input__inner::placeholder) {
  color: #333333 !important;
}

:deep(.el-card__header) {
  background: transparent !important;
  border: none !important;
  padding: 20px 20px 10px;
}

:deep(.el-card__body) {
  padding: 10px 20px 30px;
}
</style>
