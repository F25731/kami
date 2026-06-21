<template>
  <aside class="sidebar" :class="{ 'sidebar-collapsed': isCollapsed }">
    <div class="sidebar-header">
      <div class="logo">
        <img src="../assets/icon.png" alt="云逸授权中心" class="logo-img">
        <span v-if="!isCollapsed" class="logo-text">云逸授权中心</span>
      </div>
      <button class="collapse-btn" type="button" @click="toggleCollapse">{{ isCollapsed ? '›' : '‹' }}</button>
    </div>

    <nav class="sidebar-nav">
      <a v-for="item in navItems" :key="item.tab" href="javascript:void(0)" :class="{ active: activeTab === item.tab }" @click="emit('tab-change', item.tab)">
        <span class="nav-icon">{{ item.icon }}</span>
        <span v-if="!isCollapsed">{{ item.label }}</span>
      </a>
    </nav>

    <div class="sidebar-footer">
      <button type="button" class="user-trigger" @click="openAdminModal">
        <span class="avatar">{{ (userInfo?.username || 'A').slice(0, 1).toUpperCase() }}</span>
        <span v-if="!isCollapsed" class="user-name">{{ userInfo?.username || '管理员' }}</span>
      </button>
      <button type="button" class="logout-btn" @click="emit('logout')">
        <span class="nav-icon">↩</span>
        <span v-if="!isCollapsed">退出登录</span>
      </button>
    </div>
  </aside>

  <el-dialog v-model="showAdminModal" title="管理员账号设置" width="400px" :close-on-click-modal="false" append-to-body>
    <el-form :model="adminForm" label-width="80px">
      <el-form-item label="用户名"><el-input v-model="adminForm.username" placeholder="请输入新用户名" /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="adminForm.password" type="password" placeholder="留空则不修改" show-password /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showAdminModal = false">取消</el-button>
      <el-button type="primary" @click="updateAdminProfile" :loading="updating">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '../services/api.js'

const props = defineProps({ userInfo: Object, activeTab: String })
const emit = defineEmits(['logout', 'tab-change', 'collapse-change'])

const navItems = [
  { tab: 'projects', label: '项目管理', icon: '▣' },
  { tab: 'overview', label: '概览', icon: '⌁' },
  { tab: 'keys', label: '卡密管理', icon: '◆' },
  { tab: 'pricing', label: '套餐模板', icon: '▦' },
  { tab: 'orders', label: '发卡记录', icon: '≡' },
  { tab: 'api', label: 'API 管理', icon: '</>' },
  { tab: 'settings', label: '基础设置', icon: '⚙' }
]

const isCollapsed = ref(false)
const showAdminModal = ref(false)
const updating = ref(false)
const adminForm = reactive({ username: '', password: '' })

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  emit('collapse-change', isCollapsed.value)
}

const openAdminModal = () => {
  adminForm.username = props.userInfo?.username || ''
  adminForm.password = ''
  showAdminModal.value = true
}

const updateAdminProfile = async () => {
  if (!adminForm.username) {
    ElMessage.warning('用户名不能为空')
    return
  }
  try {
    updating.value = true
    const res = await authApi.updateAdmin({ id: props.userInfo.id, ...adminForm })
    if (res.success) {
      ElMessage.success('更新成功，请重新登录')
      showAdminModal.value = false
      emit('logout')
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error) {
    ElMessage.error('更新失败: ' + (error.message || '未知错误'))
  } finally {
    updating.value = false
  }
}
</script>

<style scoped>
.sidebar { position: fixed; inset: 0 auto 0 0; width: 220px; background: #fff; border-right: 1px solid #e5e7eb; display: flex; flex-direction: column; z-index: 100; transition: width .2s ease; overflow: hidden; }
.sidebar-collapsed { width: 64px; }
.sidebar-header { height: 64px; padding: 0 14px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #f3f4f6; }
.logo { display: flex; align-items: center; gap: 10px; min-width: 0; }
.logo-img { width: 30px; height: 30px; flex: 0 0 auto; }
.logo-text { font-weight: 700; color: #111827; white-space: nowrap; }
.collapse-btn, .logout-btn, .user-trigger { border: 0; background: transparent; cursor: pointer; }
.collapse-btn { width: 28px; height: 28px; border-radius: 6px; color: #6b7280; font-size: 22px; }
.collapse-btn:hover, .logout-btn:hover, .user-trigger:hover { background: #f3f4f6; }
.sidebar-nav { flex: 1; overflow-y: auto; padding: 8px; display: flex; flex-direction: column; gap: 2px; }
.sidebar-nav a, .logout-btn, .user-trigger { color: #6b7280; text-decoration: none; min-height: 40px; padding: 0 10px; border-radius: 6px; display: flex; align-items: center; gap: 10px; font-size: 14px; font-weight: 500; }
.sidebar-nav a:hover { background: #f3f4f6; color: #111827; }
.sidebar-nav a.active { background: #111827; color: #fff; }
.nav-icon { width: 24px; text-align: center; flex: 0 0 auto; }
.sidebar-footer { border-top: 1px solid #f3f4f6; padding: 8px; display: grid; gap: 4px; }
.user-trigger, .logout-btn { width: 100%; text-align: left; }
.avatar { width: 24px; height: 24px; border-radius: 999px; display: inline-grid; place-items: center; background: #111827; color: #fff; font-size: 12px; }
.user-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>

