<template>
  <div class="settings-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>项目设置</h2>
        <p>配置项目的基础信息和高级选项</p>
      </div>
      <el-button type="primary" @click="saveSettings" :loading="saving">
        <i class="el-icon-check"></i>
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
                <code>{{ settings.projectToken }}</code>
                <el-button size="small" @click="regenerateToken">重新生成</el-button>
                <el-tooltip content="Token 用于 API 调用，重新生成后旧 Token 会立即失效" placement="top">
                  <i class="el-icon-warning-outline"></i>
                </el-tooltip>
              </div>
            </el-form-item>
            <el-form-item label="使用模式">
              <el-radio-group v-model="settings.usageMode">
                <el-radio label="direct_license">直接授权（软件/APP 直接验证卡密）</el-radio>
                <el-radio label="redeem_to_account">兑换到账户（网站/商城，用户兑换卡密到账户）</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="项目状态">
              <el-switch v-model="settings.status" active-value="active" inactive-value="disabled" active-text="启用" inactive-text="停用"></el-switch>
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
              <div class="form-tip">启用后，卡密首次使用时将绑定设备，后续只能在同一设备使用</div>
            </el-form-item>
            <el-form-item label="绑定类型" v-if="settings.deviceBindEnabled">
              <el-select v-model="settings.bindType" placeholder="选择绑定类型">
                <el-option label="机器码（推荐）" value="machine_code"></el-option>
                <el-option label="Android 设备 ID" value="android_id"></el-option>
                <el-option label="自定义设备 ID" value="custom"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="允许换绑" v-if="settings.deviceBindEnabled">
              <el-switch v-model="settings.allowRebind"></el-switch>
              <div class="form-tip">是否允许通过 API 解绑并重新绑定设备</div>
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
              <div class="form-tip">启用后，在卡密生成、消耗、兑换等事件发生时推送通知</div>
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
              <div class="form-tip">用于验证 WebHook 请求来源，通过 X-Webhook-Signature 请求头传递</div>
            </el-form-item>
            <el-form-item label="通知事件" v-if="settings.webhookEnabled">
              <el-checkbox-group v-model="settings.webhookEvents">
                <el-checkbox label="card.generated">卡密生成</el-checkbox>
                <el-checkbox label="card.consumed">卡密消耗</el-checkbox>
                <el-checkbox label="card.redeemed">卡密兑换</el-checkbox>
                <el-checkbox label="entitlement.consumed">权益消耗</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item v-if="settings.webhookEnabled">
              <el-button @click="testWebhook">测试 WebHook</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="高级设置" name="advanced">
        <div class="settings-section glass-panel">
          <h3>高级配置</h3>
          <el-form :model="settings" label-width="140px">
            <el-form-item label="卡密前缀">
              <el-input v-model="settings.cardPrefix" placeholder="例如：VIP" maxlength="10">
                <template #append><span>-XXXX-XXXX-XXXX</span></template>
              </el-input>
              <div class="form-tip">生成卡密时的默认前缀，留空则无前缀</div>
            </el-form-item>
            <el-form-item label="卡密长度">
              <el-input-number v-model="settings.cardLength" :min="12" :max="32"></el-input-number>
              <div class="form-tip">卡密的总字符长度（不含前缀和分隔符）</div>
            </el-form-item>
            <el-form-item label="默认过期时间">
              <el-input-number v-model="settings.defaultExpireDays" :min="0" :max="3650"></el-input-number>
              <span style="margin-left: 8px">天</span>
              <div class="form-tip">生成卡密时的默认有效期，0 表示永不过期</div>
            </el-form-item>
          </el-form>
        </div>

        <div class="settings-section glass-panel danger-zone">
          <h3>危险操作</h3>
          <div class="danger-item">
            <div class="danger-info">
              <h4>删除项目</h4>
              <p>删除项目后，所有卡密、订单、日志将被永久删除且无法恢复</p>
            </div>
            <el-button type="danger" @click="deleteProject">删除项目</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const projectStore = useProjectStore()

const activeTab = ref('basic')
const saving = ref(false)

const settings = ref({
  projectName: '',
  projectCode: '',
  projectToken: '',
  usageMode: 'direct_license',
  status: 'active',
  description: '',
  deviceBindEnabled: false,
  bindType: 'machine_code',
  allowRebind: false,
  webhookEnabled: false,
  webhookUrl: '',
  webhookSecret: '',
  webhookEvents: ['card.generated', 'card.consumed', 'card.redeemed'],
  cardPrefix: '',
  cardLength: 16,
  defaultExpireDays: 0
})

async function loadSettings() {
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const { data } = await axios.get(`/admin/projects/${projectId}`)
    if (data.success) {
      Object.assign(settings.value, projectStore.normalizeProject(data.data?.project || data.data))
      if (typeof settings.value.webhookEvents === 'string') {
        try {
          settings.value.webhookEvents = JSON.parse(settings.value.webhookEvents)
        } catch {
          settings.value.webhookEvents = []
        }
      }
    }
  } catch (error) {
    ElMessage.error('加载设置失败：' + error.message)
  }
}
async function saveSettings() {
  saving.value = true
  try {
    const projectId = projectStore.currentProjectId
    const payload = {
      ...projectStore.toApiProject(settings.value),
      webhook_events: JSON.stringify(settings.value.webhookEvents)
    }

    const { data } = await axios.put(`/admin/projects/${projectId}`, payload)
    if (data.success) {
      ElMessage.success('设置已保存')
      await projectStore.loadProjects()
    }
  } catch (error) {
    ElMessage.error('保存失败：' + error.message)
  } finally {
    saving.value = false
  }
}

async function regenerateToken() {
  try {
    await ElMessageBox.confirm(
      '重新生成 Token 后，旧 Token 会立即失效，所有使用旧 Token 的 API 调用将失败。确认继续？',
      '确认重新生成',
      { type: 'warning' }
    )

    const projectId = projectStore.currentProjectId
    const { data } = await axios.post(`/admin/projects/${projectId}/regenerate-token`)
    if (data.success) {
      settings.value.projectToken = data.data?.projectToken || data.project_token || settings.value.projectToken
      ElMessage.success('Token 已重新生成')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('生成失败：' + error.message)
    }
  }
}

function generateWebhookSecret() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let secret = ''
  for (let i = 0; i < 32; i++) {
    secret += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  settings.value.webhookSecret = secret
  ElMessage.success('已生成签名密钥')
}

async function testWebhook() {
  try {
    const projectId = projectStore.currentProjectId
    await axios.post(`/admin/projects/${projectId}/test-webhook`)
    ElMessage.success('测试通知已发送，请检查 WebHook 服务')
  } catch (error) {
    ElMessage.error('发送失败：' + error.message)
  }
}

async function deleteProject() {
  try {
    await ElMessageBox.prompt(
      `删除项目将永久删除所有数据且无法恢复。请输入项目名称 "${settings.value.projectName}" 以确认删除`,
      '确认删除项目',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        inputPattern: new RegExp(`^${settings.value.projectName}$`),
        inputErrorMessage: '项目名称不匹配',
        type: 'error'
      }
    )

    const projectId = projectStore.currentProjectId
    await axios.delete(`/admin/projects/${projectId}`)

    ElMessage.success('项目已删除')
    await projectStore.loadProjects()
    router.push('/')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

onMounted(() => {
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
      color: #4b5563;
      margin: 0;
    }
  }
}

.settings-tabs {
  :deep(.el-tabs__item) {
    color: #4b5563;
    font-weight: 500;

    &.is-active {
      color: #2563eb;
    }
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

  code {
    flex: 1;
    padding: 8px 12px;
    background: #f3f4f6;
    border-radius: 8px;
    color: #111827;
    font-family: 'Courier New', monospace;
  }
}

.form-tip {
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.danger-zone {
  border: 1px solid rgba(239, 68, 68, 0.35);

  h3 {
    color: #dc2626;
  }
}

.danger-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #fef2f2;
  border-radius: 12px;

  .danger-info {
    h4 {
      margin: 0 0 8px 0;
      color: #dc2626;
    }

    p {
      margin: 0;
      color: #7f1d1d;
    }
  }
}
</style>
