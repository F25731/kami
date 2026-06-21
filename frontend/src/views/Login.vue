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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
}

.login-card {
  width: 380px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  text-align: center;
}

.login-button {
  width: 100%;
}
</style>
