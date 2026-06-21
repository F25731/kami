<template>
  <div class="packages-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>套餐模板</h2>
        <p>配置卡密套餐类型，生成卡密时使用</p>
      </div>
      <el-button type="primary" @click="showCreateDialog = true">
        <i class="el-icon-plus"></i>
        创建套餐
      </el-button>
    </div>

    <!-- 套餐列表 -->
    <div class="packages-grid">
      <div v-for="pkg in packages" :key="pkg.id" class="package-card glass-card">
        <div class="package-header">
          <div class="package-title">
            <h3>{{ pkg.packageName }}</h3>
            <el-tag :type="pkg.cardType === 'count' ? 'primary' : 'success'" size="small">
              {{ pkg.cardType === 'count' ? '次数卡' : '时长卡' }}
            </el-tag>
          </div>
          <div class="package-actions">
            <el-button size="small" @click="editPackage(pkg)">
              <i class="el-icon-edit"></i>
            </el-button>
            <el-button size="small" type="danger" @click="deletePackage(pkg)">
              <i class="el-icon-delete"></i>
            </el-button>
          </div>
        </div>

        <div class="package-content">
          <div class="package-info">
            <div class="info-item">
              <i class="el-icon-tickets"></i>
              <span>套餐代码：<code>{{ pkg.packageCode }}</code></span>
            </div>

            <div class="info-item" v-if="pkg.cardType === 'count'">
              <i class="el-icon-data-line"></i>
              <span>可用次数：{{ pkg.totalCount }} 次</span>
            </div>

            <div class="info-item" v-else>
              <i class="el-icon-timer"></i>
              <span>有效天数：{{ pkg.totalDays }} 天</span>
            </div>

            <div class="info-item" v-if="pkg.price">
              <i class="el-icon-money"></i>
              <span>价格：¥{{ pkg.price }}</span>
            </div>

            <div class="info-item">
              <i class="el-icon-document-copy"></i>
              <span>已生成卡密：{{ pkg.cardCount || 0 }} 张</span>
            </div>
          </div>

          <div class="package-desc" v-if="pkg.description">
            <p>{{ pkg.description }}</p>
          </div>

          <div class="package-meta">
            <span class="created-time">
              <i class="el-icon-time"></i>
              {{ formatDate(pkg.createdAt) }}
            </span>
          </div>
        </div>
      </div>

      <el-empty v-if="packages.length === 0" description="暂无套餐模板"></el-empty>
    </div>

    <!-- 创建/编辑套餐对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editMode ? '编辑套餐' : '创建套餐'"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="packageForm" label-width="100px">
        <el-form-item label="套餐名称">
          <el-input v-model="packageForm.packageName" placeholder="例如：月度会员"></el-input>
        </el-form-item>

        <el-form-item label="套餐代码">
          <el-input v-model="packageForm.packageCode" placeholder="例如：monthly-vip">
            <template #append>
              <el-button @click="generateCode">自动生成</el-button>
            </template>
          </el-input>
          <div class="form-tip">用于API调用时识别套餐，项目内唯一</div>
        </el-form-item>

        <el-form-item label="卡密类型">
          <el-radio-group v-model="packageForm.cardType">
            <el-radio label="count">次数卡</el-radio>
            <el-radio label="time">时长卡</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="可用次数" v-if="packageForm.cardType === 'count'">
          <el-input-number v-model="packageForm.totalCount" :min="1" :max="999999"></el-input-number>
        </el-form-item>

        <el-form-item label="有效天数" v-else>
          <el-input-number v-model="packageForm.totalDays" :min="1" :max="3650"></el-input-number>
        </el-form-item>

        <el-form-item label="价格">
          <el-input-number v-model="packageForm.price" :precision="2" :min="0" :max="999999"></el-input-number>
          <div class="form-tip">可选，用于记录和统计</div>
        </el-form-item>

        <el-form-item label="套餐描述">
          <el-input
            v-model="packageForm.description"
            type="textarea"
            rows="3"
            placeholder="可选"
          ></el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="savePackage" :loading="saving">
          {{ editMode ? '保存' : '创建' }}
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

const packages = ref([])
const showCreateDialog = ref(false)
const editMode = ref(false)
const saving = ref(false)

const packageForm = ref({
  packageName: '',
  packageCode: '',
  cardType: 'count',
  totalCount: 100,
  totalDays: 30,
  price: null,
  description: ''
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

function generateCode() {
  const name = packageForm.value.packageName
  if (!name) {
    ElMessage.warning('请先输入套餐名称')
    return
  }
  // 简单的拼音转换（实际项目可以用 pinyin 库）
  const code = name.toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^a-z0-9\-]/g, '')
  packageForm.value.packageCode = code || `package-${Date.now()}`
}

async function loadPackages() {
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const { data } = await axios.get(`/admin/projects/${projectId}/packages`)
    if (data.success) {
      packages.value = data.data || []
    }
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  }
}

function editPackage(pkg) {
  packageForm.value = {
    id: pkg.id,
    packageName: pkg.packageName,
    packageCode: pkg.packageCode,
    cardType: pkg.cardType,
    totalCount: pkg.totalCount,
    totalDays: pkg.totalDays,
    price: pkg.price,
    description: pkg.description
  }
  editMode.value = true
  showCreateDialog.value = true
}

async function savePackage() {
  if (!packageForm.value.packageName) {
    ElMessage.warning('请输入套餐名称')
    return
  }
  if (!packageForm.value.packageCode) {
    ElMessage.warning('请输入套餐代码')
    return
  }

  saving.value = true
  try {
    const projectId = projectStore.currentProjectId
    const payload = { ...packageForm.value }

    if (editMode.value) {
      await axios.put(`/admin/projects/${projectId}/packages/${payload.id}`, payload)
      ElMessage.success('保存成功')
    } else {
      await axios.post(`/admin/projects/${projectId}/packages`, payload)
      ElMessage.success('创建成功')
    }

    showCreateDialog.value = false
    await loadPackages()
  } catch (error) {
    ElMessage.error('操作失败：' + error.message)
  } finally {
    saving.value = false
  }
}

async function deletePackage(pkg) {
  try {
    await ElMessageBox.confirm(
      `删除套餐"${pkg.packageName}"后，已生成的卡密不会受影响。确认删除？`,
      '确认删除',
      { type: 'warning' }
    )

    const projectId = projectStore.currentProjectId
    await axios.delete(`/admin/projects/${projectId}/packages/${pkg.id}`)

    ElMessage.success('已删除')
    await loadPackages()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

function resetForm() {
  packageForm.value = {
    packageName: '',
    packageCode: '',
    cardType: 'count',
    totalCount: 100,
    totalDays: 30,
    price: null,
    description: ''
  }
  editMode.value = false
}

onMounted(() => {
  loadPackages()
})
</script>

<style scoped lang="scss">
.packages-page {
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

.packages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.package-card {
  padding: 24px;
  color: white;

  .package-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .package-title {
      display: flex;
      align-items: center;
      gap: 12px;

      h3 {
        margin: 0;
        font-size: 20px;
        font-weight: 600;
      }
    }

    .package-actions {
      display: flex;
      gap: 8px;
    }
  }

  .package-content {
    .package-info {
      margin-bottom: 16px;

      .info-item {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
        font-size: 14px;

        i {
          color: #4facfe;
          font-size: 16px;
        }

        code {
          font-family: 'Courier New', monospace;
          background: rgba(255, 255, 255, 0.1);
          padding: 2px 6px;
          border-radius: 4px;
        }
      }
    }

    .package-desc {
      margin-bottom: 16px;
      padding: 12px;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 8px;
      font-size: 13px;
      color: rgba(255, 255, 255, 0.8);

      p {
        margin: 0;
      }
    }

    .package-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      color: rgba(255, 255, 255, 0.5);

      .created-time {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

:deep(.el-empty) {
  padding: 60px 0;
  grid-column: 1 / -1;

  .el-empty__description {
    color: rgba(255, 255, 255, 0.6);
  }
}
</style>
