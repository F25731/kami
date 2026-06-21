<template>
  <div class="settings-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>项目设置</h2>
        <p>配置当前项目的基础信息、设备绑定和 WebHook 通知</p>
      </div>
      <el-button type="primary" @click="saveSettings" :loading="saving" :disabled="!currentProjectId">
        <el-icon><Check /></el-icon>
        保存设置
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="settings-tabs">
      <el-tab-pane label="基础设置" name="basic">
        <div class="settings-section glass-panel">
          <h3>项目信息</h3>
          <el-form :model="settings" label-width="140px">
            <el-form-item label="项目名称">
              <el-input v-model="settings.projectName" placeholder="例如：我的网站"></el-input>
            </el-form-item>
            <el-form-item label="项目标识">
              <el-input v-model="settings.projectCode" placeholder="例如：my-website"></el-input>
            </el-form-item>
            <el-form-item label="项目 Token">
              <div class="token-field">
                <code>{{ settings.projectToken || '-' }}</code>
                <el-button size="small" @click="copyToken" :disabled="!settings.projectToken">复制</el-button>
                <el-button size="small" @click="regenerateToken" :disabled="!currentProjectId">重新生成</el-button>
              </div>
              <div class="form-tip">Token 用于 API 调用，重新生成后旧 Token 会立即失效。</div>
            </el-form-item>
            <el-form-item label="使用模式">
              <el-radio-group v-model="settings.usageMode">
                <el-radio label="direct_license">直接授权（软件/APP 直接验证卡密）</el-radio>
                <el-radio label="redeem_to_account">兑换到账户（网站/商城，用户兑换卡密到账户）</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="项目状态">
              <el-switch v-model="settings.status" active-value="enabled" inactive-value="disabled" active-text="启用" inactive-text="停用"></el-switch>
            </el-form-item>
            <el-form-item label="项目描述">
              <el-input v-model="settings.description" type="textarea" rows="3" placeholder="可选"></el-input>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="设备绑定" name="device">
        <div class="settings-section glass-panel">
          <h3>设备绑定配置</h3>
          <el-form :model="settings" label-width="140px">
            <el-form-item label="启用设备绑定">
              <el-switch v-model="settings.deviceBindEnabled"></el-switch>
              <div class="form-tip">启用后，卡密首次使用时会绑定设备，后续只能在同一设备使用。</div>
            </el-form-item>
            <el-form-item label="绑定类型" v-if="settings.deviceBindEnabled">
              <el-select v-model="settings.bindType" placeholder="选择绑定类型">
                <el-option label="机器码" value="machine_code"></el-option>
                <el-option label="Android 设备 ID" value="android_id"></el-option>
                <el-option label="自定义设备 ID" value="custom"></el-option>
              </el-select>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="WebHook" name="webhook">
        <div class="settings-section glass-panel">
          <h3>WebHook 通知</h3>
          <el-form :model="settings" label-width="140px">
            <el-form-item label="启用 WebHook">
              <el-switch v-model="settings.webhookEnabled"></el-switch>
              <div class="form-tip">启用后，卡密生成、消费、兑换、用户权益消费等事件会 POST 到你的地址。</div>
            </el-form-item>
            <el-form-item label="WebHook URL" v-if="settings.webhookEnabled">
              <el-input v-model="settings.webhookUrl" placeholder="https://your-domain.com/webhook"></el-input>
            </el-form-item>
            <el-form-item label="签名密钥" v-if="settings.webhookEnabled">
              <el-input v-model="settings.webhookSecret" placeholder="留空表示不签名">
                <template #append>
                  <el-button @click="generateWebhookSecret">生成密钥</el-button>
                </template>
              </el-input>
              <div class="form-tip">配置后请求头会携带 X-Webhook-Signature，签名内容为 payloadJson + secret 的 SHA256。</div>
            </el-form-item>
            <el-form-item label="通知事件" v-if="settings.webhookEnabled">
              <el-checkbox-group v-model="settings.webhookEvents">
                <el-checkbox label="card.generated">卡密生成</el-checkbox>
                <el-checkbox label="card.consumed">卡密消费</el-checkbox>
                <el-checkbox label="card.redeemed">卡密兑换</el-checkbox>
                <el-checkbox label="entitlement.consumed">权益消费</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item v-if="settings.webhookEnabled">
              <el-button @click="testWebhook" :loading="testingWebhook" :disabled="!settings.webhookUrl">保存并测试 WebHook</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="高级设置" name="advanced">
        <div class="settings-section glass-panel danger-zone">
          <h3>危险操作</h3>
          <div class="danger-item">
            <div class="danger-info">
              <h4>删除项目</h4>
              <p>删除后项目不会再显示。数据库会保留持久化数据，避免误清空。</p>
            </div>
            <el-button type="danger" @click="deleteProject" :disabled="!currentProjectId">删除项目</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const projectStore = useProjectStore()

const activeTab = ref('basic')
const saving = ref(false)
const testingWebhook = ref(false)

const settings = ref(defaultSettings())
const currentProjectId = computed(() => projectStore.currentProject?.id || null)

function defaultSettings() {
  return {
    projectName: '',
    projectCode: '',
    projectToken: '',
    usageMode: 'direct_license',
    status: 'enabled',
    description: '',
    deviceBindEnabled: false,
    bindType: 'machine_code',
    webhookEnabled: false,
    webhookUrl: '',
    webhookSecret: '',
    webhookEvents: ['card.generated', 'card.consumed', 'card.redeemed', 'entitlement.consumed']
  }
}

function applyProject(project) {
  if (!project) {
    settings.value = defaultSettings()
    return
  }
  const normalized = projectStore.normalizeProject(project)
  settings.value = {
    ...settings.value,
    projectName: normalized.projectName,
    projectCode: normalized.projectCode,
    projectToken: normalized.projectToken,
    usageMode: normalized.usageMode || 'direct_license',
    status: normalized.status || 'enabled',
    description: normalized.description || normalized.remark || '',
    deviceBindEnabled: !!normalized.deviceBindEnabled,
    bindType: normalized.bindType || 'machine_code',
    webhookEnabled: !!normalized.webhookEnabled,
    webhookUrl: normalized.webhookUrl || '',
    webhookSecret: normalized.webhookSecret || '',
    webhookEvents: normalizeWebhookEvents(normalized.webhookEvents)
  }
}

function normalizeWebhookEvents(value) {
  if (Array.isArray(value)) return value.length ? value : defaultSettings().webhookEvents
  if (!value) return defaultSettings().webhookEvents
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed.length ? parsed : defaultSettings().webhookEvents
  } catch {}
  return String(value).split(',').map(item => item.trim()).filter(Boolean)
}

async function loadSettings() {
  const id = currentProjectId.value
  if (!id) return

  applyProject(projectStore.currentProject)
  try {
    const { data } = await axios.get('/admin/projects/' + id)
    if (data.success) {
      applyProject(data.data?.project || data.data)
    }
  } catch (error) {
    ElMessage.error('加载设置失败：' + (error.response?.data?.message || error.message))
  }
}

async function saveSettings(showMessage = true) {
  const id = currentProjectId.value
  if (!id) return false
  if (!settings.value.projectName.trim() || !settings.value.projectCode.trim()) {
    ElMessage.warning('请填写项目名称和项目标识')
    return false
  }
  if (settings.value.webhookEnabled && !settings.value.webhookUrl.trim()) {
    ElMessage.warning('启用 WebHook 后请填写 WebHook URL')
    return false
  }

  saving.value = true
  try {
    const payload = {
      ...projectStore.toApiProject(settings.value),
      webhook_events: JSON.stringify(settings.value.webhookEvents)
    }
    const { data } = await axios.put('/admin/projects/' + id, payload)
    if (!data.success) {
      ElMessage.error(data.message || '保存失败')
      return false
    }
    await projectStore.loadProjects()
    const updated = projectStore.projects.find(project => project.id === id)
    if (updated) projectStore.switchProject(updated)
    applyProject(updated || projectStore.currentProject)
    if (showMessage) ElMessage.success('设置已保存')
    return true
  } catch (error) {
    ElMessage.error('保存失败：' + (error.response?.data?.message || error.message))
    return false
  } finally {
    saving.value = false
  }
}

async function copyText(text) {
  if (!text) return
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

function copyToken() {
  copyText(settings.value.projectToken)
}

async function regenerateToken() {
  try {
    await ElMessageBox.confirm(
      '重新生成 Token 后，旧 Token 会立即失效。确认继续？',
      '确认重新生成',
      { type: 'warning' }
    )
    const { data } = await axios.post('/admin/projects/' + currentProjectId.value + '/regenerate-token')
    if (data.success) {
      settings.value.projectToken = data.project_token || data.data?.projectToken || settings.value.projectToken
      await projectStore.loadProjects()
      const updated = projectStore.projects.find(project => project.id === currentProjectId.value)
      if (updated) projectStore.switchProject(updated)
      ElMessage.success('Token 已重新生成')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('生成失败：' + (error.response?.data?.message || error.message))
    }
  }
}

function generateWebhookSecret() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let secret = ''
  for (let i = 0; i < 32; i++) secret += chars.charAt(Math.floor(Math.random() * chars.length))
  settings.value.webhookSecret = secret
  ElMessage.success('已生成签名密钥')
}

async function testWebhook() {
  testingWebhook.value = true
  try {
    const saved = await saveSettings(false)
    if (!saved) return
    const { data } = await axios.post('/admin/projects/' + currentProjectId.value + '/test-webhook')
    if (data.success) {
      ElMessage.success('测试 WebHook 已发送成功')
    } else {
      ElMessage.error(data.message || '测试发送失败')
    }
  } catch (error) {
    ElMessage.error('测试发送失败：' + (error.response?.data?.message || error.message))
  } finally {
    testingWebhook.value = false
  }
}

async function deleteProject() {
  try {
    await ElMessageBox.prompt(
      '请输入项目名称“' + settings.value.projectName + '”确认删除。',
      '确认删除项目',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        inputPattern: new RegExp('^' + settings.value.projectName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$'),
        inputErrorMessage: '项目名称不匹配',
        type: 'error'
      }
    )
    await axios.delete('/admin/projects/' + currentProjectId.value)
    ElMessage.success('项目已删除')
    await projectStore.loadProjects()
    router.push('/')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + (error.response?.data?.message || error.message))
    }
  }
}

watch(
  () => projectStore.currentProject?.id,
  () => loadSettings(),
  { immediate: true }
)

onMounted(async () => {
  if (!projectStore.currentProject && projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
  loadSettings()
})
</script>

<style scoped lang="scss">
.settings-page {
  padding: 24px;
  color: #111827;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 24px;
  margin-bottom: 24px;
  border-radius: 16px;

  .header-left {
    h2 {
      color: #111827;
      font-size: 24px;
      margin: 0 0 8px 0;
    }

    p {
      color: #374151;
      margin: 0;
    }
  }
}

.settings-tabs {
  :deep(.el-tabs__item) {
    color: #111827;
    font-weight: 600;
  }

  :deep(.el-tabs__item.is-active) {
    color: #2563eb;
  }

  :deep(.el-tabs__active-bar) {
    background: #2563eb;
  }
}

.settings-section {
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 24px;
  color: #111827;

  h3 {
    margin: 0 0 24px 0;
    color: #111827;
    font-size: 20px;
  }
}

.token-field {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;

  code {
    flex: 1;
    min-width: 180px;
    padding: 8px 12px;
    background: rgba(255, 255, 255, 0.35);
    border: 1px solid rgba(255, 255, 255, 0.45);
    border-radius: 8px;
    color: #111827;
    font-family: 'Courier New', monospace;
    overflow-wrap: anywhere;
  }
}

.form-tip {
  margin-top: 8px;
  font-size: 13px;
  color: #374151;
  line-height: 1.5;
}

.danger-zone {
  border: 1px solid rgba(239, 68, 68, 0.35);

  h3 {
    color: #991b1b;
  }
}

.danger-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: rgba(254, 242, 242, 0.45);
  border-radius: 12px;

  .danger-info {
    h4 {
      margin: 0 0 8px 0;
      color: #991b1b;
    }

    p {
      margin: 0;
      color: #7f1d1d;
    }
  }
}
</style>
