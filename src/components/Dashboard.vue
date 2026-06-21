<template>
  <div class="dashboard" :class="{ 'dashboard-sidebar-collapsed': sidebarCollapsed }">
    <NavigationBar
      :user-info="userInfo"
      :active-tab="activeTab"
      @tab-change="handleTabChange"
      @logout="emit('logout')"
      @collapse-change="sidebarCollapsed = $event"
    />

    <div v-if="createProgress.visible" class="create-progress-bar">
      <div class="progress-content">
        <div class="progress-info">
          <span class="progress-icon" :class="{ spinning: !createProgress.done }">{{ createProgress.done ? '✓' : '↻' }}</span>
          <span class="progress-text">
            <template v-if="createProgress.done">
              全部创建完成，成功 {{ createProgress.success }} 条<template v-if="createProgress.fail > 0">，失败 {{ createProgress.fail }} 条</template>
            </template>
            <template v-else>
              正在创建卡密... {{ createProgress.current }} / {{ createProgress.total }}
            </template>
          </span>
        </div>
        <div class="progress-track"><div class="progress-fill" :style="{ width: createProgress.percent + '%' }"></div></div>
        <button v-if="createProgress.done" class="progress-close" @click="createProgress.visible = false">&times;</button>
      </div>
    </div>

    <main class="dashboard-main">
      <ProjectManagePage v-if="activeTab === 'projects'" />
      <OverviewPage
        v-if="activeTab === 'overview'"
        :stats="stats"
        :carousel-data="carouselData"
        :features="features"
        @prev-slide="prevSlide"
        @next-slide="nextSlide"
        @slide-change="currentSlide = $event"
      />
      <KeysManagePage
        v-if="activeTab === 'keys'"
        :keys="keys"
        @create-keys="handleCreateKeys"
        @delete-key="handleDeleteKey"
        @toggle-key-status="handleToggleKeyStatus"
        @update-key="handleUpdateKey"
      />
      <PricingManagePage v-if="activeTab === 'pricing'" />
      <OrdersManagePage v-if="activeTab === 'orders'" />
      <ApiManagePage
        v-if="activeTab === 'api'"
        :api-keys="apiKeys"
        @generate-api-key="handleGenerateApiKey"
        @delete-api-key="handleDeleteApiKey"
        @update-api-key="handleUpdateApiKey"
        @toggle-api-key="handleToggleApiKey"
      />
      <SettingsPage v-if="activeTab === 'settings'" @save-settings="handleSaveSettings" />
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { mockApiKeys, mockFeatures, mockSlides } from '../data/mockData.js'
import { cardApi, statsApi } from '../services/api.js'
import NavigationBar from './NavigationBar.vue'
import OverviewPage from './OverviewPage.vue'
import ProjectManagePage from './ProjectManagePage.vue'
import KeysManagePage from './KeysManagePage.vue'
import PricingManagePage from './PricingManagePage.vue'
import OrdersManagePage from './OrdersManagePage.vue'
import ApiManagePage from './ApiManagePage.vue'
import SettingsPage from './SettingsPage.vue'

const props = defineProps({ userInfo: Object })
const emit = defineEmits(['logout'])

const activeTab = ref('projects')
const sidebarCollapsed = ref(false)
const currentSlide = ref(0)
const carouselData = ref(mockSlides)
const features = ref(mockFeatures)
const keys = ref([])
const apiKeys = ref(mockApiKeys)

const stats = reactive({ totalKeys: 0, usedKeys: 0, activeKeys: 0, totalUsers: 0 })
const createProgress = reactive({ visible: false, current: 0, total: 0, success: 0, fail: 0, done: false, percent: 0 })

const handleTabChange = (tab) => {
  activeTab.value = tab
}

const prevSlide = () => {
  if (!carouselData.value.length) return
  currentSlide.value = currentSlide.value === 0 ? carouselData.value.length - 1 : currentSlide.value - 1
}

const nextSlide = () => {
  if (!carouselData.value.length) return
  currentSlide.value = currentSlide.value === carouselData.value.length - 1 ? 0 : currentSlide.value + 1
}

const loadDashboardStats = async () => {
  try {
    const result = await statsApi.getDashboardStats()
    if (result) {
      stats.totalKeys = result.totalKeys || 0
      stats.usedKeys = result.usedKeys || 0
      stats.activeKeys = result.activeKeys || 0
      stats.totalUsers = 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadKeys = async () => {
  try {
    const result = await cardApi.getAllCards()
    if (result.success) keys.value = result.data || []
    await loadDashboardStats()
  } catch (error) {
    console.error('加载卡密数据失败:', error)
  }
}

const handleCreateKeys = async (keyData) => {
  const totalCount = keyData.count || 1
  if (totalCount <= 3) {
    try {
      const result = await cardApi.createCards(keyData)
      if (result.success) {
        await loadKeys()
        ElMessage.success('成功创建 ' + totalCount + ' 条卡密')
      } else {
        ElMessage.error(result.message || '生成卡密失败')
      }
    } catch (error) {
      ElMessage.error('生成卡密失败: ' + (error.message || '未知错误'))
    }
    return
  }

  Object.assign(createProgress, { visible: true, current: 0, total: totalCount, success: 0, fail: 0, done: false, percent: 0 })
  const singleData = { ...keyData, count: 1 }
  for (let i = 0; i < totalCount; i++) {
    try {
      const result = await cardApi.createCards(singleData)
      if (result.success) createProgress.success++
      else createProgress.fail++
    } catch (error) {
      createProgress.fail++
    }
    createProgress.current = i + 1
    createProgress.percent = Math.round(((i + 1) / totalCount) * 100)
  }
  createProgress.done = true
  await loadKeys()
  setTimeout(() => { if (createProgress.done) createProgress.visible = false }, 5000)
}

const handleDeleteKey = async (keyId) => {
  try {
    const result = await cardApi.deleteCard(keyId)
    if (result.success) {
      ElMessage.success(result.message || '卡密已删除')
      await loadKeys()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error('删除卡密失败: ' + (error.message || '未知错误'))
  }
}

const handleUpdateKey = async (keyData) => {
  try {
    const result = await cardApi.updateCard(keyData.id, keyData)
    if (result.success) {
      ElMessage.success(result.message || '卡密更新成功')
      await loadKeys()
    } else {
      ElMessage.error(result.message || '更新失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '更新卡密失败')
  }
}

const handleToggleKeyStatus = async ({ id, status }) => {
  try {
    const result = await cardApi.updateAdminStatus(id, status)
    if (result.success) {
      ElMessage.success(result.message || '状态已更新')
      await loadKeys()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '更新卡密状态失败')
  }
}

const handleGenerateApiKey = () => {
  apiKeys.value.push({ id: Date.now(), name: 'API 密钥 ' + (apiKeys.value.length + 1), key: 'ak_' + Math.random().toString(36).slice(2, 34), isActive: true })
}
const handleDeleteApiKey = (keyId) => { apiKeys.value = apiKeys.value.filter(key => key.id !== keyId) }
const handleUpdateApiKey = (updatedKey) => {
  const index = apiKeys.value.findIndex(key => key.id === updatedKey.id)
  if (index !== -1) apiKeys.value[index] = { ...apiKeys.value[index], ...updatedKey }
}
const handleToggleApiKey = (keyId) => {
  const key = apiKeys.value.find(key => key.id === keyId)
  if (key) key.isActive = !key.isActive
}

const handleSaveSettings = (settingsData) => {
  console.log('保存设置:', settingsData)
}

onMounted(loadKeys)
</script>

<style scoped>
.dashboard { min-height: 100vh; background: #f9fafb; width: 100%; margin: 0; padding: 0 0 0 220px; display: flex; transition: padding-left .3s ease; box-sizing: border-box; }
.dashboard.dashboard-sidebar-collapsed { padding-left: 64px; }
.dashboard-main { padding: 2rem; max-width: 1400px; margin: 0 auto; width: 100%; box-sizing: border-box; flex: 1; }
.create-progress-bar { position: fixed; top: 0; left: 220px; right: 0; z-index: 999; background: #fff; border-bottom: 1px solid #e5e7eb; box-shadow: 0 2px 8px rgba(0,0,0,.08); transition: left .3s ease; }
.dashboard.dashboard-sidebar-collapsed .create-progress-bar { left: 64px; }
.progress-content { max-width: 1400px; margin: 0 auto; padding: 12px 2rem; position: relative; }
.progress-info { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.progress-icon { color: #2563eb; font-weight: 700; }
.spinning { animation: spin 1.2s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.progress-text { font-size: .85rem; font-weight: 500; color: #374151; }
.progress-track { width: 100%; height: 6px; background: #e5e7eb; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #2563eb, #0ea5e9); border-radius: 3px; transition: width .3s ease; }
.progress-close { position: absolute; top: 8px; right: 2rem; background: none; border: none; font-size: 1.25rem; color: #9ca3af; cursor: pointer; padding: 4px 8px; line-height: 1; border-radius: 4px; }
.progress-close:hover { background: #f3f4f6; color: #374151; }
@media (max-width: 768px) { .dashboard, .dashboard.dashboard-sidebar-collapsed { padding-left: 64px; } .dashboard-main { padding: 1rem; } .create-progress-bar { left: 64px; } }
</style>
