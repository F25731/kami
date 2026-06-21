<template>
  <div class="api-keys">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>API密钥管理</h2>
        <p>管理项目的API密钥，用于调用OpenAPI接口</p>
      </div>
      <el-button type="primary" @click="showCreateDialog = true">
        <i class="el-icon-plus"></i>
        创建新密钥
      </el-button>
    </div>

    <!-- API密钥列表 -->
    <div class="keys-list">
      <div v-for="key in apiKeys" :key="key.id" class="key-card glass-card">
        <div class="key-header">
          <div class="key-info">
            <div class="key-name">
              <i class="el-icon-key"></i>
              {{ key.keyName }}
            </div>
            <div class="key-meta">
              <el-tag :type="key.status === 'active' ? 'success' : 'info'" size="small">
                {{ key.status === 'active' ? '启用中' : '已禁用' }}
              </el-tag>
              <span class="key-env">{{ key.environment === 'production' ? '生产环境' : '测试环境' }}</span>
              <span class="key-created">创建于 {{ formatDate(key.createdAt) }}</span>
            </div>
          </div>
          <div class="key-actions">
            <el-button size="small" @click="rotateKey(key)">
              <i class="el-icon-refresh"></i>
              轮换密钥
            </el-button>
            <el-switch
              v-model="key.status"
              active-value="active"
              inactive-value="disabled"
              @change="toggleStatus(key)"
            ></el-switch>
            <el-button size="small" type="danger" @click="deleteKey(key)">
              <i class="el-icon-delete"></i>
            </el-button>
          </div>
        </div>

        <div class="key-content">
          <div class="key-field">
            <label>API Key</label>
            <div class="key-value">
              <code>{{ key.apiKey }}</code>
              <el-button size="small" text @click="copyToClipboard(key.apiKey)">
                <i class="el-icon-copy-document"></i>
              </el-button>
            </div>
          </div>

          <div class="key-field">
            <label>API Secret</label>
            <div class="key-value">
              <code>••••••••••••••••••••</code>
              <el-tooltip content="API Secret仅在创建时显示一次，无法查看" placement="top">
                <i class="el-icon-warning"></i>
              </el-tooltip>
            </div>
          </div>

          <div class="key-stats">
            <div class="stat-item">
              <span class="stat-label">使用次数</span>
              <span class="stat-value">{{ key.useCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最后使用</span>
              <span class="stat-value">{{ key.lastUsedAt ? formatDate(key.lastUsedAt) : '从未使用' }}</span>
            </div>
            <div class="stat-item" v-if="key.expiredAt">
              <span class="stat-label">过期时间</span>
              <span class="stat-value">{{ formatDate(key.expiredAt) }}</span>
            </div>
          </div>

          <el-collapse accordion class="key-details">
            <el-collapse-item title="权限配置" name="permissions">
              <div class="permissions-list">
                <el-tag v-for="perm in parsePermissions(key.permissions)" :key="perm" size="small">
                  {{ perm }}
                </el-tag>
              </div>
            </el-collapse-item>
            <el-collapse-item title="IP白名单" name="ips">
              <div class="ip-list">
                {{ key.allowedIps || '不限制' }}
              </div>
            </el-collapse-item>
            <el-collapse-item title="备注" name="remark" v-if="key.remark">
              <div class="remark-text">
                {{ key.remark }}
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>

      <el-empty v-if="apiKeys.length === 0" description="暂无API密钥"></el-empty>
    </div>

    <!-- 创建密钥对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建API密钥"
      width="600px"
      @close="resetCreateForm"
    >
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="密钥名称">
          <el-input v-model="createForm.keyName" placeholder="例如：生产环境密钥"></el-input>
        </el-form-item>

        <el-form-item label="环境">
          <el-radio-group v-model="createForm.environment">
            <el-radio label="production">生产环境</el-radio>
            <el-radio label="test">测试环境</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="权限">
          <el-checkbox-group v-model="createForm.permissionsArray">
            <el-checkbox label="*">全部权限</el-checkbox>
            <el-checkbox label="cards:generate">生成卡密</el-checkbox>
            <el-checkbox label="cards:verify">验证卡密</el-checkbox>
            <el-checkbox label="cards:consume">消耗卡密</el-checkbox>
            <el-checkbox label="cards:redeem">兑换卡密</el-checkbox>
            <el-checkbox label="cards:status">查询状态</el-checkbox>
            <el-checkbox label="entitlements:consume">消耗权益</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="IP白名单">
          <el-input
            v-model="createForm.allowedIps"
            type="textarea"
            rows="3"
            placeholder="留空表示不限制，多个IP用逗号分隔，支持*通配符"
          ></el-input>
        </el-form-item>

        <el-form-item label="过期时间">
          <el-date-picker
            v-model="createForm.expiredAt"
            type="datetime"
            placeholder="留空表示永不过期"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="createForm.remark"
            type="textarea"
            rows="2"
            placeholder="可选"
          ></el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createKey" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 密钥创建成功对话框 -->
    <el-dialog
      v-model="showSecretDialog"
      title="⚠️ 请保存您的API Secret"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-alert
        title="重要提示"
        type="warning"
        :closable="false"
        style="margin-bottom: 20px"
      >
        API Secret仅显示一次，请立即复制保存。关闭此窗口后将无法再次查看。
      </el-alert>

      <div class="secret-display">
        <div class="secret-field">
          <label>API Key</label>
          <div class="secret-value">
            <code>{{ newKeyData.api_key }}</code>
            <el-button size="small" @click="copyToClipboard(newKeyData.api_key)">
              复制
            </el-button>
          </div>
        </div>

        <div class="secret-field">
          <label>API Secret</label>
          <div class="secret-value">
            <code>{{ newKeyData.api_secret }}</code>
            <el-button size="small" type="primary" @click="copyToClipboard(newKeyData.api_secret)">
              复制
            </el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-checkbox v-model="secretSaved">我已安全保存API Secret</el-checkbox>
        <el-button type="primary" @click="closeSecretDialog" :disabled="!secretSaved">
          确认关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useProjectStore } from '@/stores/project'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const projectStore = useProjectStore()

const apiKeys = ref([])
const showCreateDialog = ref(false)
const showSecretDialog = ref(false)
const creating = ref(false)
const secretSaved = ref(false)

const createForm = ref({
  keyName: '',
  environment: 'production',
  permissionsArray: ['cards:verify', 'cards:consume', 'cards:status'],
  allowedIps: '',
  expiredAt: null,
  remark: ''
})

const newKeyData = ref({
  api_key: '',
  api_secret: ''
})

function parsePermissions(perms) {
  try {
    return JSON.parse(perms || '[]')
  } catch {
    return []
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadApiKeys() {
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const { data } = await axios.get(`/admin/projects/${projectId}/api-keys`)
    if (data.success) {
      apiKeys.value = data.data || []
    }
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  }
}

async function createKey() {
  if (!createForm.value.keyName) {
    ElMessage.warning('请输入密钥名称')
    return
  }

  creating.value = true
  try {
    const projectId = projectStore.currentProjectId
    const payload = {
      ...createForm.value,
      permissions: JSON.stringify(createForm.value.permissionsArray)
    }
    delete payload.permissionsArray

    const { data } = await axios.post(`/admin/projects/${projectId}/api-keys`, payload)
    if (data.success) {
      newKeyData.value = data.data
      showCreateDialog.value = false
      showSecretDialog.value = true
      await loadApiKeys()
    }
  } catch (error) {
    ElMessage.error('创建失败：' + error.message)
  } finally {
    creating.value = false
  }
}

async function rotateKey(key) {
  try {
    await ElMessageBox.confirm('轮换密钥后，旧的API Key和Secret将立即失效。确认继续？', '确认轮换', {
      type: 'warning'
    })

    const { data } = await axios.post(`/admin/projects/${projectStore.currentProjectId}/api-keys/${key.id}/rotate`)
    if (data.success) {
      newKeyData.value = data.data
      showSecretDialog.value = true
      await loadApiKeys()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('轮换失败：' + error.message)
    }
  }
}

async function toggleStatus(key) {
  try {
    await axios.patch(`/admin/projects/${projectStore.currentProjectId}/api-keys/${key.id}/status`, {
      status: key.status
    })
    ElMessage.success('状态已更新')
  } catch (error) {
    ElMessage.error('更新失败：' + error.message)
    await loadApiKeys()
  }
}

async function deleteKey(key) {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确认删除此API密钥？', '确认删除', {
      type: 'warning'
    })

    await axios.delete(`/admin/projects/${projectStore.currentProjectId}/api-keys/${key.id}`)
    ElMessage.success('已删除')
    await loadApiKeys()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

function copyToClipboard(text) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

function resetCreateForm() {
  createForm.value = {
    keyName: '',
    environment: 'production',
    permissionsArray: ['cards:verify', 'cards:consume', 'cards:status'],
    allowedIps: '',
    expiredAt: null,
    remark: ''
  }
}

function closeSecretDialog() {
  showSecretDialog.value = false
  secretSaved.value = false
  newKeyData.value = { api_key: '', api_secret: '' }
}

onMounted(() => {
  loadApiKeys()
})
</script>

<style scoped lang="scss">
.api-keys {
  padding: 24px;
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
      color: white;
      font-size: 24px;
      margin: 0 0 8px 0;
    }

    p {
      color: rgba(255, 255, 255, 0.7);
      font-size: 14px;
      margin: 0;
    }
  }
}

.keys-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.key-card {
  padding: 24px;
  color: white;

  .key-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .key-info {
      .key-name {
        font-size: 18px;
        font-weight: 600;
        margin-bottom: 8px;
        display: flex;
        align-items: center;
        gap: 8px;

        i {
          color: #4facfe;
        }
      }

      .key-meta {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 13px;
        color: rgba(255, 255, 255, 0.6);

        .key-env {
          padding: 2px 8px;
          background: rgba(255, 255, 255, 0.1);
          border-radius: 4px;
        }
      }
    }

    .key-actions {
      display: flex;
      gap: 12px;
      align-items: center;
    }
  }

  .key-content {
    .key-field {
      margin-bottom: 16px;

      label {
        display: block;
        font-size: 13px;
        color: rgba(255, 255, 255, 0.6);
        margin-bottom: 8px;
      }

      .key-value {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px;
        background: rgba(0, 0, 0, 0.2);
        border-radius: 8px;

        code {
          flex: 1;
          font-family: 'Courier New', monospace;
          font-size: 14px;
          color: #4facfe;
          word-break: break-all;
        }
      }
    }

    .key-stats {
      display: flex;
      gap: 24px;
      margin: 20px 0;

      .stat-item {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .stat-label {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.6);
        }

        .stat-value {
          font-size: 16px;
          font-weight: 500;
          color: white;
        }
      }
    }

    .key-details {
      margin-top: 16px;
      background: transparent;
      border: none;

      :deep(.el-collapse-item__header) {
        background: transparent;
        color: white;
        border-color: rgba(255, 255, 255, 0.1);
      }

      :deep(.el-collapse-item__wrap) {
        background: transparent;
        border-color: rgba(255, 255, 255, 0.1);
      }

      :deep(.el-collapse-item__content) {
        color: rgba(255, 255, 255, 0.8);
        padding: 16px;
      }

      .permissions-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }

      .ip-list, .remark-text {
        font-size: 14px;
      }
    }
  }
}

.secret-display {
  .secret-field {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }

    label {
      display: block;
      font-weight: 500;
      margin-bottom: 8px;
      color: #333;
    }

    .secret-value {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 8px;

      code {
        flex: 1;
        font-family: 'Courier New', monospace;
        font-size: 14px;
        color: #333;
        word-break: break-all;
      }
    }
  }
}

:deep(.el-empty) {
  padding: 60px 0;

  .el-empty__description {
    color: rgba(255, 255, 255, 0.6);
  }
}
</style>
