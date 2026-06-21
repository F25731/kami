<template>
  <div class="app-layout">
    <aside class="sidebar glass-panel" :class="{ collapsed: sidebarCollapsed }">
      <div class="logo-section">
        <div class="logo">
          <i class="el-icon-key"></i>
          <span v-show="!sidebarCollapsed" class="logo-text">License Center</span>
        </div>
        <el-button circle size="small" @click="toggleSidebar" class="collapse-btn glass-btn">
          <i :class="sidebarCollapsed ? 'el-icon-arrow-right' : 'el-icon-arrow-left'"></i>
        </el-button>
      </div>

      <div class="project-selector glass-section">
        <div class="section-header" @click="projectsExpanded = !projectsExpanded">
          <div class="header-left">
            <i class="el-icon-folder-opened"></i>
            <span v-show="!sidebarCollapsed">项目</span>
          </div>
          <i v-show="!sidebarCollapsed" :class="projectsExpanded ? 'el-icon-arrow-down' : 'el-icon-arrow-right'"></i>
        </div>

        <div v-if="currentProject" class="current-project glass-card" @click="toggleProjectList">
          <div class="project-icon">{{ currentProject.projectName?.charAt(0) || 'P' }}</div>
          <div v-show="!sidebarCollapsed" class="project-info">
            <div class="project-name">{{ currentProject.projectName }}</div>
            <div class="project-token">{{ currentProject.projectToken }}</div>
          </div>
          <i v-show="!sidebarCollapsed" class="el-icon-arrow-down"></i>
        </div>

        <transition name="slide-fade">
          <div v-show="projectsExpanded && !sidebarCollapsed" class="project-list">
            <div
              v-for="project in projects"
              :key="project.id"
              class="project-item"
              :class="{ active: currentProject?.id === project.id }"
              @click="selectProject(project)"
            >
              <div class="project-icon small">{{ project.projectName?.charAt(0) || 'P' }}</div>
              <div class="project-info">
                <div class="project-name">{{ project.projectName }}</div>
                <div class="project-code">{{ project.projectCode }}</div>
              </div>
              <i v-if="currentProject?.id === project.id" class="el-icon-check"></i>
              <el-button
                v-if="project.projectCode !== 'default'"
                class="project-delete-btn"
                text
                type="danger"
                @click.stop="deleteProject(project)"
              >删除</el-button>
            </div>

            <el-button class="create-project-btn glass-btn" @click="showCreateProjectDialog = true">
              <i class="el-icon-plus"></i>
              创建新项目
            </el-button>
          </div>
        </transition>
      </div>

      <nav class="nav-menu" v-show="!sidebarCollapsed">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item glass-card"
          :class="{ active: $route.path.includes(item.path) }"
        >
          <i :class="item.icon"></i>
          <span>{{ item.label }}</span>
          <span v-if="item.badge" class="badge">{{ item.badge }}</span>
        </router-link>
      </nav>

      <div class="user-section glass-section" v-show="!sidebarCollapsed">
        <div class="user-info">
          <el-avatar :size="36">{{ username?.charAt(0) || 'U' }}</el-avatar>
          <div class="user-details">
            <div class="username">{{ username }}</div>
            <div class="user-role">管理员</div>
          </div>
        </div>
        <el-button class="logout-btn glass-btn" @click="logout">
          <i class="el-icon-switch-button"></i>
        </el-button>
      </div>
    </aside>

    <main class="main-content">
      <header class="top-bar glass-panel">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">控制台</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentProject">{{ currentProject.projectName }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title || '页面' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="top-actions">
          <el-button class="glass-btn" circle><i class="el-icon-bell"></i></el-button>
          <el-button class="glass-btn" circle><i class="el-icon-setting"></i></el-button>
        </div>
      </header>

      <div class="page-content">
        <router-view v-if="currentProject"></router-view>
        <div v-else class="empty-state">
          <i class="el-icon-folder-add"></i>
          <p>请选择或创建一个项目</p>
          <el-button type="primary" @click="showCreateProjectDialog = true">创建项目</el-button>
        </div>
      </div>
    </main>

    <el-dialog v-model="showCreateProjectDialog" title="创建新项目" width="500px" class="glass-dialog">
      <el-form :model="newProject" label-width="100px">
        <el-form-item label="项目名称">
          <el-input v-model="newProject.projectName" placeholder="例如：我的网站"></el-input>
        </el-form-item>
        <el-form-item label="项目标识">
          <el-input v-model="newProject.projectCode" placeholder="例如：my-website"></el-input>
        </el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="newProject.projectType" placeholder="请选择">
            <el-option label="网站" value="website"></el-option>
            <el-option label="Windows 软件" value="windows"></el-option>
            <el-option label="Android 应用" value="android"></el-option>
            <el-option label="API 服务" value="api"></el-option>
            <el-option label="其他" value="other"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="使用模式">
          <el-radio-group v-model="newProject.usageMode">
            <el-radio label="direct_license">直接授权（软件/APP）</el-radio>
            <el-radio label="redeem_to_account">兑换到账户（网站）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateProjectDialog = false">取消</el-button>
        <el-button type="primary" :loading="creatingProject" @click="createProject">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const projectStore = useProjectStore()
const sidebarCollapsed = ref(false)
const projectsExpanded = ref(false)
const showCreateProjectDialog = ref(false)
const creatingProject = ref(false)
const username = ref('Admin')

const newProject = ref({
  projectName: '',
  projectCode: '',
  projectType: 'website',
  usageMode: 'direct_license'
})

const projects = computed(() => projectStore.projects)
const currentProject = computed(() => projectStore.currentProject)
const menuItems = computed(() => {
  if (!currentProject.value) return []

  return [
    { path: '/dashboard', icon: 'el-icon-data-line', label: '数据看板', badge: null },
    { path: '/cards', icon: 'el-icon-tickets', label: '卡密管理', badge: null },
    { path: '/packages', icon: 'el-icon-box', label: '套餐模板', badge: null },
    { path: '/api-keys', icon: 'el-icon-key', label: 'API密钥', badge: null },
    { path: '/api-docs', icon: 'el-icon-document', label: 'API文档', badge: null },
    { path: '/orders', icon: 'el-icon-list', label: '发卡订单', badge: null },
    { path: '/entitlements', icon: 'el-icon-user', label: '用户权益', badge: null },
    { path: '/logs', icon: 'el-icon-document-copy', label: '调用日志', badge: null },
    { path: '/settings', icon: 'el-icon-setting', label: '项目设置', badge: null }
  ]
})

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function toggleProjectList() {
  projectsExpanded.value = !projectsExpanded.value
}

function selectProject(project) {
  projectStore.switchProject(project)
  ElMessage.success('已切换到项目：' + project.projectName)
}

async function deleteProject(project) {
  try {
    await ElMessageBox.confirm('确定要删除项目"' + project.projectName + '"吗？删除后无法恢复。', '删除确认', { type: 'warning' })
    const result = await projectStore.deleteProject(project.id)
    if (result.success) {
      ElMessage.success('项目已删除')
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || error.message || '删除失败')
    }
  }
}

async function createProject() {
  if (!newProject.value.projectName.trim() || !newProject.value.projectCode.trim()) {
    ElMessage.warning('请填写项目名称和项目标识')
    return
  }

  creatingProject.value = true
  try {
    const result = await projectStore.createProject(newProject.value)
    if (result.success) {
      const created = projectStore.normalizeProject(result.data?.project || result.data || {})
      if (created.id) projectStore.switchProject(created)
      ElMessage.success('项目创建成功')
      showCreateProjectDialog.value = false
      projectsExpanded.value = true
      newProject.value = {
        projectName: '',
        projectCode: '',
        projectType: 'website',
        usageMode: 'direct_license'
      }
      return
    }
    ElMessage.error(result.message || '创建失败')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '创建失败')
  } finally {
    creatingProject.value = false
  }
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('isLoggedIn')
  router.push('/login')
}

onMounted(() => {
  projectStore.loadProjects()
})
</script>

<style scoped lang="scss">
.app-layout {
  display: flex;
  height: 100vh;
  background: transparent;
  overflow: hidden;
}

.glass-panel,
.glass-card,
.glass-section {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(30px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(30px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1) !important;
}

.glass-btn {
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: #000000 !important;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.glass-btn:hover {
  background: rgba(255, 255, 255, 0.3) !important;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
}

.sidebar {
  width: 280px;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.sidebar.collapsed {
  width: 80px;
}

.logo-section,
.section-header,
.current-project,
.project-item,
.user-section,
.user-info,
.top-bar,
.nav-item {
  display: flex;
  align-items: center;
}

.logo-section,
.section-header,
.top-bar,
.user-section {
  justify-content: space-between;
}

.logo-section {
  margin-bottom: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  font-weight: bold;
  color: #000000;
}

.logo i {
  font-size: 32px;
}

.project-selector,
.nav-item {
  margin-bottom: 12px;
}

.project-selector {
  padding: 16px;
  border-radius: 16px;
}

.section-header {
  cursor: pointer;
  color: #000000;
  font-size: 14px;
  font-weight: 600;
}

.header-left,
.project-info,
.user-details {
  min-width: 0;
}

.header-left,
.current-project,
.project-item,
.user-info,
.nav-item {
  gap: 12px;
}

.current-project,
.project-item {
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  margin-top: 10px;
}

.project-item.active,
.nav-item.active {
  background: rgba(255, 255, 255, 0.3) !important;
  color: #000000 !important;
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2) !important;
}

.project-item.active *,
.nav-item.active * {
  color: #000000 !important;
}

.project-delete-btn {
  margin-left: auto;
  padding: 4px 6px;
}

.project-item.active .project-delete-btn {
  color: #ffffff !important;
}

.project-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0.1));
  color: #000000;
  font-weight: 700;
  flex: 0 0 auto;
  border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
}

.project-icon.small {
  width: 28px;
  height: 28px;
  font-size: 12px;
}

.project-name,
.username,
.breadcrumb,
.nav-item {
  color: #000000 !important;
}

.project-token,
.project-code,
.user-role {
  color: #1a1a1a !important;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.create-project-btn {
  width: 100%;
  margin-top: 10px;
}

.nav-menu {
  flex: 1;
  overflow-y: auto;
  margin-top: 12px;
}

.nav-item {
  padding: 12px 16px;
  border-radius: 12px;
  text-decoration: none;
  font-size: 14px;
  position: relative;
}

.nav-item i {
  font-size: 18px;
}

.badge {
  margin-left: auto;
  background: #ef4444;
  color: #ffffff;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 11px;
}

.user-section {
  margin-top: auto;
  padding: 14px;
  border-radius: 16px;
}

.logout-btn {
  padding: 8px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 24px 24px 24px 0;
}

.top-bar {
  padding: 16px 24px;
  margin-bottom: 24px;
  border-radius: 16px;
}

.top-actions {
  display: flex;
  gap: 12px;
}

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #000000;
}

.empty-state i {
  color: #000000;
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.7;
}

.empty-state p {
  color: #1a1a1a;
  font-size: 18px;
  margin-bottom: 24px;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}
</style>
