<template>
  <div class="project-page">
    <div class="page-header">
      <div>
        <h2>项目管理</h2>
        <p>为每个软件、App 或外部商城建立独立授权项目，并管理项目级 OpenAPI 凭据。</p>
      </div>
      <button class="primary" type="button" @click="openCreate">新增项目</button>
    </div>

    <div class="toolbar">
      <input v-model.trim="keyword" placeholder="搜索项目名称、编码或备注" />
      <button type="button" @click="loadProjects" :disabled="loading">刷新</button>
    </div>

    <div v-if="loading" class="state">正在加载项目...</div>
    <div v-else-if="filteredProjects.length === 0" class="state">暂无项目，点击右上角新增。</div>

    <div v-else class="project-grid">
      <article v-for="project in filteredProjects" :key="project.id" class="project-card">
        <div class="card-head">
          <div>
            <h3>{{ project.project_name }}</h3>
            <span class="project-code">{{ project.project_code }}</span>
          </div>
          <span class="status" :class="project.status">{{ statusText(project.status) }}</span>
        </div>

        <div class="meta-grid">
          <div><span>项目类型</span><strong>{{ typeText(project.project_type) }}</strong></div>
          <div><span>环境</span><strong>{{ envText(project.environment) }}</strong></div>
          <div><span>卡密</span><strong>{{ project.card_count || 0 }}</strong></div>
          <div><span>今日调用</span><strong>{{ project.today_call_count || 0 }}</strong></div>
        </div>

        <div class="api-box">
          <span>OpenAPI 地址</span>
          <code>{{ apiBase(project) }}</code>
          <button type="button" @click="copy(apiBase(project))">复制</button>
        </div>

        <p v-if="project.remark" class="remark">{{ project.remark }}</p>

        <div class="card-actions">
          <button type="button" @click="openEdit(project)">编辑</button>
          <button type="button" @click="openKeys(project)">API Key</button>
          <button type="button" @click="regenerateToken(project)">重置 Token</button>
          <button type="button" @click="toggleStatus(project)">{{ project.status === 'enabled' ? '停用' : '启用' }}</button>
          <button v-if="project.project_code !== 'default'" type="button" class="danger" @click="deleteProject(project)">删除</button>
        </div>
      </article>
    </div>

    <el-dialog v-model="showForm" :title="editingId ? '编辑项目' : '新增项目'" width="640px" :close-on-click-modal="false">
      <el-form :model="form" label-width="110px">
        <el-form-item label="项目名称" required><el-input v-model="form.project_name" placeholder="例如：Windows 客户端" /></el-form-item>
        <el-form-item label="项目编码" required><el-input v-model="form.project_code" :disabled="!!editingId" placeholder="例如：windows_client" /></el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="form.project_type" style="width: 100%">
            <el-option label="Windows" value="windows" />
            <el-option label="Android" value="android" />
            <el-option label="Web" value="web" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="环境">
          <el-select v-model="form.environment" style="width: 100%">
            <el-option label="生产" value="production" />
            <el-option label="测试" value="testing" />
            <el-option label="开发" value="development" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权模式">
          <el-select v-model="form.usage_mode" style="width: 100%">
            <el-option label="直接授权" value="direct_license" />
            <el-option label="外部商城发卡" value="external_issue" />
            <el-option label="混合模式" value="mixed" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备绑定"><el-switch v-model="form.enable_device_bind" /></el-form-item>
        <el-form-item label="绑定方式">
          <el-select v-model="form.device_bind_mode" style="width: 100%" :disabled="!form.enable_device_bind">
            <el-option label="机器码" value="machine_code" />
            <el-option label="Android ID" value="android_id" />
            <el-option label="自定义设备码" value="custom" />
            <el-option label="不绑定" value="none" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口签名"><el-switch v-model="form.enable_signature" /></el-form-item>
        <el-form-item label="IP 白名单"><el-switch v-model="form.enable_ip_whitelist" /></el-form-item>
        <el-form-item label="每分钟限流"><el-input-number v-model="form.rate_limit_per_minute" :min="1" :max="100000" /></el-form-item>
        <el-form-item label="单次发卡上限"><el-input-number v-model="form.max_generate_per_request" :min="1" :max="10000" /></el-form-item>
        <el-form-item label="每日发卡上限"><el-input-number v-model="form.max_generate_per_day" :min="1" :max="1000000" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="saveProject" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showKeys" :title="selectedProject ? selectedProject.project_name + ' - API Key' : 'API Key'" width="760px">
      <div class="key-toolbar">
        <el-input v-model="newKeyName" placeholder="新 Key 名称" />
        <el-button type="primary" @click="createApiKey" :loading="keySaving">生成 API Key</el-button>
      </div>
      <el-alert v-if="newSecret" type="warning" show-icon :closable="false" class="secret-alert">
        <template #title>请立即保存 api_secret，关闭后不会再次显示。</template>
        <div class="secret-line"><code>{{ newSecret.api_key }}</code><button @click="copy(newSecret.api_key)">复制 Key</button></div>
        <div class="secret-line"><code>{{ newSecret.api_secret }}</code><button @click="copy(newSecret.api_secret)">复制 Secret</button></div>
      </el-alert>
      <div v-if="keysLoading" class="state small">正在加载 API Key...</div>
      <table v-else class="key-table">
        <thead><tr><th>名称</th><th>API Key</th><th>状态</th><th>调用次数</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="key in apiKeys" :key="key.id">
            <td>{{ key.key_name }}</td>
            <td><code>{{ key.api_key }}</code></td>
            <td>{{ key.status === 'active' ? '启用' : '停用' }}</td>
            <td>{{ key.use_count || 0 }}</td>
            <td>
              <button type="button" @click="copy(key.api_key)">复制</button>
              <button type="button" @click="rotateKey(key)">轮换</button>
              <button type="button" @click="toggleKeyStatus(key)">{{ key.status === 'active' ? '停用' : '启用' }}</button>
            </td>
          </tr>
          <tr v-if="apiKeys.length === 0"><td colspan="5" class="empty-row">暂无 API Key</td></tr>
        </tbody>
      </table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { projectApi } from '../services/api.js'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const projects = ref([])
const showForm = ref(false)
const editingId = ref(null)
const showKeys = ref(false)
const selectedProject = ref(null)
const apiKeys = ref([])
const keysLoading = ref(false)
const keySaving = ref(false)
const newKeyName = ref('')
const newSecret = ref(null)

const defaultForm = () => ({
  project_name: '',
  project_code: '',
  project_type: 'windows',
  environment: 'production',
  usage_mode: 'direct_license',
  status: 'enabled',
  remark: '',
  enable_device_bind: true,
  device_bind_mode: 'machine_code',
  enable_signature: true,
  enable_ip_whitelist: false,
  rate_limit_per_minute: 120,
  max_generate_per_request: 100,
  max_generate_per_day: 10000
})
const form = reactive(defaultForm())

const filteredProjects = computed(() => {
  const q = keyword.value.toLowerCase()
  return projects.value
    .filter(project => project.status !== 'deleted')
    .filter(project => !q || [project.project_name, project.project_code, project.remark].some(value => String(value || '').toLowerCase().includes(q)))
})

const loadProjects = async () => {
  loading.value = true
  try {
    const res = await projectApi.getProjects()
    projects.value = res.success ? (res.data || []) : []
  } catch (error) {
    ElMessage.error('加载项目失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const resetForm = () => Object.assign(form, defaultForm())
const openCreate = () => {
  editingId.value = null
  resetForm()
  showForm.value = true
}
const openEdit = (project) => {
  editingId.value = project.id
  resetForm()
  Object.assign(form, {
    ...project,
    enable_device_bind: Boolean(project.enable_device_bind),
    enable_signature: Boolean(project.enable_signature),
    enable_ip_whitelist: Boolean(project.enable_ip_whitelist)
  })
  showForm.value = true
}

const saveProject = async () => {
  if (!form.project_name || !form.project_code) {
    ElMessage.warning('项目名称和项目编码不能为空')
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    const res = editingId.value
      ? await projectApi.updateProject(editingId.value, payload)
      : await projectApi.createProject(payload)
    if (!res.success) throw new Error(res.message || '保存失败')
    ElMessage.success(editingId.value ? '项目已更新' : '项目已创建')
    showForm.value = false
    await loadProjects()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (project) => {
  const next = project.status === 'enabled' ? 'disabled' : 'enabled'
  await projectApi.updateStatus(project.id, next)
  ElMessage.success(next === 'enabled' ? '项目已启用' : '项目已停用')
  await loadProjects()
}

const regenerateToken = async (project) => {
  await ElMessageBox.confirm('重置后旧 OpenAPI 地址会失效，确定继续吗？', '重置项目 Token', { type: 'warning' })
  const res = await projectApi.regenerateToken(project.id)
  if (!res.success) throw new Error(res.message || '重置失败')
  ElMessage.success('项目 Token 已重置')
  if (res.project_token) copy(`${location.origin}/api/p/${res.project_token}`)
  await loadProjects()
}

const deleteProject = async (project) => {
  await ElMessageBox.confirm(`确定删除项目「${project.project_name}」吗？`, '删除项目', { type: 'warning' })
  const res = await projectApi.deleteProject(project.id)
  if (!res.success) throw new Error(res.message || '删除失败')
  ElMessage.success('项目已删除')
  await loadProjects()
}

const openKeys = async (project) => {
  selectedProject.value = project
  newSecret.value = null
  newKeyName.value = `${project.project_name} API Key`
  showKeys.value = true
  await loadApiKeys()
}

const loadApiKeys = async () => {
  if (!selectedProject.value) return
  keysLoading.value = true
  try {
    const res = await projectApi.getApiKeys(selectedProject.value.id)
    apiKeys.value = res.success ? (res.data || []) : []
  } catch (error) {
    ElMessage.error('加载 API Key 失败: ' + (error.message || '未知错误'))
  } finally {
    keysLoading.value = false
  }
}

const createApiKey = async () => {
  if (!selectedProject.value) return
  keySaving.value = true
  try {
    const res = await projectApi.createApiKey(selectedProject.value.id, {
      key_name: newKeyName.value || 'API Key',
      environment: selectedProject.value.environment || 'production'
    })
    if (!res.success) throw new Error(res.message || '生成失败')
    newSecret.value = res.data
    ElMessage.success('API Key 已生成')
    await loadApiKeys()
  } catch (error) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    keySaving.value = false
  }
}

const rotateKey = async (key) => {
  await ElMessageBox.confirm('轮换后旧 API Key 和签名密钥会失效，确定继续吗？', '轮换 API Key', { type: 'warning' })
  const res = await projectApi.rotateApiKey(selectedProject.value.id, key.id)
  if (!res.success) throw new Error(res.message || '轮换失败')
  newSecret.value = res.data
  ElMessage.success('API Key 已轮换')
  await loadApiKeys()
}

const toggleKeyStatus = async (key) => {
  const next = key.status === 'active' ? 'disabled' : 'active'
  const res = await projectApi.updateApiKeyStatus(selectedProject.value.id, key.id, next)
  if (!res.success) throw new Error(res.message || '操作失败')
  ElMessage.success(next === 'active' ? 'API Key 已启用' : 'API Key 已停用')
  await loadApiKeys()
}

const apiBase = (project) => `${location.origin}/api/p/${project.project_token}`
const statusText = (status) => ({ enabled: '启用', active: '启用', disabled: '停用', deleted: '已删除' }[status] || status || '未知')
const typeText = (type) => ({ windows: 'Windows', android: 'Android', web: 'Web', other: '其他' }[type] || type || '其他')
const envText = (env) => ({ production: '生产', testing: '测试', development: '开发', prod: '生产' }[env] || env || '-')

const copy = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch (error) {
    ElMessage.warning('复制失败，请手动复制')
  }
}

onMounted(loadProjects)
</script>

<style scoped>
.project-page { padding: 1rem; color: #111827; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
h2 { margin: 0 0 8px; font-size: 20px; }
p { margin: 0; color: #6b7280; }
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.toolbar input { min-width: 280px; max-width: 420px; flex: 1; }
input, select, textarea { border: 1px solid #d1d5db; border-radius: 8px; padding: 9px 10px; font: inherit; }
button { border: 1px solid #d1d5db; border-radius: 8px; background: #fff; padding: 8px 12px; cursor: pointer; }
button.primary, .primary { color: #fff; border-color: #2563eb; background: #2563eb; }
button.danger { color: #b91c1c; border-color: #fecaca; background: #fff5f5; }
.state { padding: 34px; text-align: center; color: #6b7280; background: #fff; border: 1px dashed #d1d5db; border-radius: 8px; }
.state.small { padding: 18px; }
.project-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 14px; }
.project-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; display: grid; gap: 14px; }
.card-head { display: flex; justify-content: space-between; gap: 12px; }
h3 { margin: 0 0 5px; font-size: 17px; }
.project-code { color: #6b7280; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
.status { align-self: start; border-radius: 999px; padding: 4px 9px; background: #e5e7eb; color: #374151; font-size: 12px; }
.status.enabled, .status.active { background: #dcfce7; color: #166534; }
.status.disabled { background: #fee2e2; color: #991b1b; }
.meta-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.meta-grid div { background: #f9fafb; border-radius: 8px; padding: 9px; display: grid; gap: 4px; }
.meta-grid span, .api-box span { color: #6b7280; font-size: 12px; }
.meta-grid strong { font-size: 14px; }
.api-box { display: grid; gap: 7px; padding: 10px; border-radius: 8px; background: #f8fafc; border: 1px solid #e5e7eb; }
code { overflow-wrap: anywhere; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; color: #111827; }
.remark { padding: 10px; border-left: 3px solid #2563eb; background: #eff6ff; color: #1f2937; border-radius: 6px; }
.card-actions, .key-toolbar, .secret-line { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.key-toolbar { margin-bottom: 12px; }
.secret-alert { margin-bottom: 12px; }
.secret-line { margin-top: 8px; }
.key-table { width: 100%; border-collapse: collapse; }
.key-table th, .key-table td { border-bottom: 1px solid #e5e7eb; padding: 10px; text-align: left; vertical-align: top; }
.key-table th { background: #f9fafb; color: #374151; font-size: 13px; }
.empty-row { text-align: center; color: #6b7280; }
@media (max-width: 760px) {
  .page-header, .toolbar { flex-direction: column; }
  .toolbar input { min-width: 0; width: 100%; }
  .project-grid { grid-template-columns: 1fr; }
  .meta-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
