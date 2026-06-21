<template>
  <div class="packages-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>套餐模板</h2>
        <p>先创建套餐规则，生成卡密时按套餐批量生成。</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        创建套餐
      </el-button>
    </div>

    <div class="packages-grid" v-loading="loading">
      <div v-for="pkg in packages" :key="pkg.id" class="package-card glass-card">
        <div class="package-header">
          <div class="package-title">
            <h3>{{ pkg.packageName }}</h3>
            <el-tag :type="pkg.cardType === 'count' ? 'primary' : 'success'" size="small">
              {{ pkg.cardType === 'count' ? '次数卡' : '时长卡' }}
            </el-tag>
          </div>
          <div class="package-actions">
            <el-button size="small" @click="editPackage(pkg)">编辑</el-button>
            <el-button size="small" type="danger" @click="deletePackage(pkg)">删除</el-button>
          </div>
        </div>

        <div class="package-content">
          <div class="info-item">
            <span>套餐代码：</span>
            <code>{{ pkg.packageCode }}</code>
            <el-button size="small" text @click="copyToClipboard(pkg.packageCode)">复制</el-button>
          </div>
          <div class="info-item" v-if="pkg.cardType === 'count'">
            <span>可用次数：{{ pkg.totalCount || pkg.countValue || 0 }} 次</span>
          </div>
          <div class="info-item" v-else>
            <span>有效天数：{{ pkg.totalDays || pkg.durationDays || 0 }} 天</span>
          </div>
          <div class="info-item" v-if="pkg.price !== null && pkg.price !== undefined">
            <span>价格：￥{{ pkg.price }}</span>
          </div>
          <div class="info-item">
            <span>已生成卡密：{{ pkg.cardCount || 0 }} 张</span>
          </div>
          <p v-if="pkg.description" class="package-desc">{{ pkg.description }}</p>
        </div>
      </div>

      <el-empty v-if="!loading && packages.length === 0" description="暂无套餐，请先创建一个套餐模板"></el-empty>
    </div>

    <el-dialog v-model="showDialog" :title="editMode ? '编辑套餐' : '创建套餐'" width="600px" @close="resetForm">
      <el-form :model="packageForm" label-width="100px">
        <el-form-item label="套餐名称">
          <el-input v-model="packageForm.packageName" placeholder="例如：月卡 / 100次卡"></el-input>
        </el-form-item>
        <el-form-item label="套餐代码">
          <el-input v-model="packageForm.packageCode" placeholder="例如：monthly-vip">
            <template #append>
              <el-button @click="generateCode">自动生成</el-button>
            </template>
          </el-input>
          <div class="form-tip">API 生成卡密时要传这个代码，例如 package_code=monthly-vip。</div>
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
        </el-form-item>
        <el-form-item label="套餐描述">
          <el-input v-model="packageForm.description" type="textarea" rows="3" placeholder="可选"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="savePackage" :loading="saving">{{ editMode ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useProjectStore } from '@/stores/project'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

const projectStore = useProjectStore()
const packages = ref([])
const loading = ref(false)
const showDialog = ref(false)
const editMode = ref(false)
const saving = ref(false)

const packageForm = ref(defaultForm())

function defaultForm() {
  return {
    packageName: '',
    packageCode: '',
    cardType: 'count',
    totalCount: 100,
    totalDays: 30,
    price: 0,
    description: ''
  }
}

function openCreateDialog() {
  resetForm()
  showDialog.value = true
}

function editPackage(pkg) {
  packageForm.value = {
    id: pkg.id,
    packageName: pkg.packageName,
    packageCode: pkg.packageCode,
    cardType: pkg.cardType || 'count',
    totalCount: pkg.totalCount || pkg.countValue || 100,
    totalDays: pkg.totalDays || pkg.durationDays || 30,
    price: pkg.price ?? 0,
    description: pkg.description || pkg.remark || ''
  }
  editMode.value = true
  showDialog.value = true
}

function resetForm() {
  packageForm.value = defaultForm()
  editMode.value = false
}

function generateCode() {
  const name = packageForm.value.packageName.trim()
  const fallback = 'package-' + Date.now()
  packageForm.value.packageCode = name
    ? name.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '') || fallback
    : fallback
}

async function loadPackages() {
  const projectId = projectStore.currentProjectId
  if (!projectId) return
  loading.value = true
  try {
    const { data } = await axios.get('/admin/projects/' + projectId + '/packages')
    if (data.success) {
      packages.value = data.data || []
    } else {
      ElMessage.error(data.message || '加载套餐失败')
    }
  } catch (error) {
    ElMessage.error('加载套餐失败：' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

async function savePackage() {
  if (!packageForm.value.packageName.trim()) {
    ElMessage.warning('请输入套餐名称')
    return
  }
  if (!packageForm.value.packageCode.trim()) {
    ElMessage.warning('请输入套餐代码')
    return
  }
  const projectId = projectStore.currentProjectId
  if (!projectId) return

  saving.value = true
  try {
    const payload = { ...packageForm.value }
    const request = editMode.value
      ? axios.put('/admin/projects/' + projectId + '/packages/' + payload.id, payload)
      : axios.post('/admin/projects/' + projectId + '/packages', payload)
    const { data } = await request
    if (!data.success) {
      ElMessage.error(data.message || '操作失败')
      return
    }
    ElMessage.success(editMode.value ? '保存成功' : '创建成功')
    showDialog.value = false
    await loadPackages()
  } catch (error) {
    ElMessage.error('操作失败：' + (error.response?.data?.message || error.message))
  } finally {
    saving.value = false
  }
}

async function deletePackage(pkg) {
  try {
    await ElMessageBox.confirm('确定删除套餐“' + pkg.packageName + '”？已生成的卡密不会被删除。', '删除确认', { type: 'warning' })
    const projectId = projectStore.currentProjectId
    const { data } = await axios.delete('/admin/projects/' + projectId + '/packages/' + pkg.id)
    if (!data.success) {
      ElMessage.error(data.message || '删除失败')
      return
    }
    ElMessage.success('已删除')
    await loadPackages()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败：' + (error.response?.data?.message || error.message))
  }
}

async function copyToClipboard(text) {
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

watch(() => projectStore.currentProjectId, () => loadPackages(), { immediate: true })
onMounted(loadPackages)
</script>

<style scoped lang="scss">
.packages-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 24px;
  margin-bottom: 24px;
  border-radius: 16px;

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

.packages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.package-card {
  padding: 24px;
}

.package-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.22);
}

.package-title {
  display: flex;
  align-items: center;
  gap: 12px;

  h3 {
    margin: 0;
    color: #111827;
    font-size: 20px;
  }
}

.package-actions {
  display: flex;
  gap: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #111827;

  code {
    background: rgba(255, 255, 255, 0.28);
    border-radius: 6px;
    padding: 3px 8px;
    font-family: 'Courier New', monospace;
  }
}

.package-desc {
  margin: 14px 0 0;
  color: #374151;
  line-height: 1.6;
}

.form-tip {
  color: #374151;
  font-size: 12px;
  margin-top: 5px;
}

:deep(.el-empty) {
  grid-column: 1 / -1;
}
</style>
