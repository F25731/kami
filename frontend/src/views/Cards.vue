<template>
  <div class="cards-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>卡密管理</h2>
        <p>按套餐批量生成卡密，并管理绑定、状态和发卡记录。</p>
      </div>
      <el-button type="primary" @click="openGenerateDialog">
        <el-icon><Plus /></el-icon>
        生成卡密
      </el-button>
    </div>

    <div class="filters glass-panel">
      <el-input
        v-model="searchQuery"
        placeholder="搜索卡密"
        clearable
        style="width: 300px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />

      <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 150px" @change="loadCards">
        <el-option label="未使用" :value="0" />
        <el-option label="使用中" :value="1" />
        <el-option label="已停用/过期" :value="2" />
        <el-option label="已耗尽" :value="3" />
      </el-select>

      <el-select v-model="filterPackage" placeholder="套餐" clearable style="width: 220px" @change="loadCards">
        <el-option
          v-for="pkg in packages"
          :key="pkg.id"
          :label="pkg.packageName + '（' + pkg.packageCode + '）'"
          :value="pkg.id"
        />
      </el-select>

      <el-button @click="handleSearch">搜索</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <div class="cards-table glass-panel">
      <el-table :data="cards" style="width: 100%" class="glass-table" v-loading="loading">
        <el-table-column prop="cardKey" label="卡密" min-width="220">
          <template #default="{ row }">
            <div class="card-key">
              <code>{{ row.cardKey }}</code>
              <el-button size="small" text @click="copyToClipboard(row.cardKey)">复制</el-button>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="packageName" label="套餐" min-width="160">
          <template #default="{ row }">
            <div>{{ row.packageName || '-' }}</div>
            <code v-if="row.packageCode" class="muted-code">{{ row.packageCode }}</code>
          </template>
        </el-table-column>

        <el-table-column prop="cardType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.cardType === 'count' ? 'primary' : 'success'">
              {{ getTypeText(row.cardType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="剩余" width="120">
          <template #default="{ row }">
            <span v-if="row.cardType === 'count'">{{ row.remainingCount ?? 0 }} 次</span>
            <span v-else>{{ row.remainingDays || row.duration || 0 }} 天</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="bindDeviceId" label="绑定设备" min-width="150">
          <template #default="{ row }">
            <span v-if="row.bindDeviceId || row.deviceId">{{ shortText(row.bindDeviceId || row.deviceId) }}</span>
            <span v-else class="empty-text">未绑定</span>
          </template>
        </el-table-column>

        <el-table-column prop="expireTime" label="过期时间" min-width="170">
          <template #default="{ row }">{{ row.expireTime ? formatDate(row.expireTime) : '永久' }}</template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.createTime || row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewCardDetail(row)">详情</el-button>
            <el-button size="small" type="warning" @click="unbindDevice(row)" v-if="row.bindDeviceId || row.deviceId">解绑</el-button>
            <el-button size="small" type="danger" @click="deleteCard(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadCards"
        @current-change="loadCards"
        class="pagination"
      />
    </div>

    <el-dialog v-model="showGenerateDialog" title="生成卡密" width="620px" @close="resetGenerateForm">
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="套餐模板">
          <el-select v-model="generateForm.packageId" placeholder="先选择套餐" style="width: 100%">
            <el-option
              v-for="pkg in packages"
              :key="pkg.id"
              :label="pkg.packageName + '（' + pkg.packageCode + '）'"
              :value="pkg.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生成数量">
          <el-input-number v-model="generateForm.quantity" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="generateForm.orderNo" placeholder="可选，用于关联外部商城订单" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="generateForm.expireTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="留空表示永久"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="卡密前缀">
          <el-input v-model="generateForm.prefix" placeholder="可选，例如 VIP" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button type="primary" @click="generateCards" :loading="generating">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showGeneratedDialog" title="生成结果" width="720px">
      <p class="dialog-tip">本次共生成 {{ generatedCards.length }} 张卡密，请复制后交给发卡渠道或外部商城。</p>
      <el-input :model-value="generatedCards.join('\n')" type="textarea" :rows="12" readonly />
      <template #footer>
        <el-button @click="copyToClipboard(generatedCards.join('\n'))">复制全部卡密</el-button>
        <el-button type="primary" @click="showGeneratedDialog = false">完成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="卡密详情" width="700px">
      <div v-if="currentCard" class="card-detail">
        <div class="detail-row">
          <label>卡密</label>
          <div class="detail-value">
            <code>{{ currentCard.cardKey }}</code>
            <el-button size="small" @click="copyToClipboard(currentCard.cardKey)">复制</el-button>
          </div>
        </div>
        <div class="detail-row"><label>套餐</label><span>{{ currentCard.packageName || '-' }}</span></div>
        <div class="detail-row"><label>套餐代码</label><code>{{ currentCard.packageCode || '-' }}</code></div>
        <div class="detail-row"><label>类型</label><span>{{ getTypeText(currentCard.cardType) }}</span></div>
        <div class="detail-row">
          <label>总量/剩余</label>
          <span v-if="currentCard.cardType === 'count'">{{ currentCard.totalCount || 0 }} / {{ currentCard.remainingCount || 0 }} 次</span>
          <span v-else>{{ currentCard.duration || currentCard.totalDays || 0 }} 天</span>
        </div>
        <div class="detail-row"><label>状态</label><el-tag :type="getStatusType(currentCard.status)">{{ getStatusText(currentCard.status) }}</el-tag></div>
        <div class="detail-row" v-if="currentCard.bindDeviceId || currentCard.deviceId"><label>绑定设备</label><span>{{ currentCard.bindDeviceId || currentCard.deviceId }}</span></div>
        <div class="detail-row" v-if="currentCard.orderNo"><label>订单号</label><span>{{ currentCard.orderNo }}</span></div>
        <div class="detail-row"><label>创建时间</label><span>{{ formatDate(currentCard.createTime || currentCard.createdAt) }}</span></div>
        <div class="detail-row" v-if="currentCard.activatedAt || currentCard.activateTime"><label>激活时间</label><span>{{ formatDate(currentCard.activatedAt || currentCard.activateTime) }}</span></div>
        <div class="detail-row" v-if="currentCard.expireTime"><label>过期时间</label><span>{{ formatDate(currentCard.expireTime) }}</span></div>
      </div>
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
const cards = ref([])
const packages = ref([])
const loading = ref(false)
const generating = ref(false)
const searchQuery = ref('')
const filterStatus = ref('')
const filterPackage = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showGenerateDialog = ref(false)
const showGeneratedDialog = ref(false)
const showDetailDialog = ref(false)
const currentCard = ref(null)
const generatedCards = ref([])
const generateForm = ref(defaultGenerateForm())

function defaultGenerateForm() {
  return { packageId: null, quantity: 10, orderNo: '', expireTime: null, prefix: '' }
}

function openGenerateDialog() {
  if (packages.value.length === 0) {
    ElMessage.warning('请先创建套餐模板')
    return
  }
  showGenerateDialog.value = true
}

function resetGenerateForm() {
  generateForm.value = defaultGenerateForm()
}

function getTypeText(type) {
  return type === 'time' ? '时长卡' : '次数卡'
}

function getStatusType(status) {
  const types = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger', 4: 'danger' }
  return types[Number(status)] || 'info'
}

function getStatusText(status) {
  const texts = { 0: '未使用', 1: '使用中', 2: '已停用/过期', 3: '已耗尽', 4: '已合并' }
  return texts[Number(status)] || '未知'
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

function shortText(value) {
  const text = String(value || '')
  return text.length > 16 ? text.slice(0, 16) + '...' : text
}

async function loadCards() {
  const projectId = projectStore.currentProjectId
  if (!projectId) return
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      search: searchQuery.value,
      status: filterStatus.value,
      packageId: filterPackage.value
    }
    const { data } = await axios.get('/admin/projects/' + projectId + '/cards', { params })
    if (!data.success) {
      ElMessage.error(data.message || '加载卡密失败')
      return
    }
    cards.value = data.data?.list || []
    total.value = data.data?.total || 0
  } catch (error) {
    ElMessage.error('加载卡密失败：' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

async function loadPackages() {
  const projectId = projectStore.currentProjectId
  if (!projectId) return
  try {
    const { data } = await axios.get('/admin/projects/' + projectId + '/packages')
    if (data.success) packages.value = data.data || []
  } catch (error) {
    ElMessage.error('加载套餐失败：' + (error.response?.data?.message || error.message))
  }
}

async function generateCards() {
  if (!generateForm.value.packageId) {
    ElMessage.warning('请选择套餐模板')
    return
  }
  const projectId = projectStore.currentProjectId
  if (!projectId) return
  generating.value = true
  try {
    const { data } = await axios.post('/admin/projects/' + projectId + '/cards/generate', generateForm.value)
    if (!data.success) {
      ElMessage.error(data.message || '生成失败')
      return
    }
    generatedCards.value = data.data?.cards || []
    showGenerateDialog.value = false
    showGeneratedDialog.value = true
    ElMessage.success('成功生成 ' + (data.data?.count || generatedCards.value.length) + ' 张卡密')
    await loadCards()
  } catch (error) {
    ElMessage.error('生成失败：' + (error.response?.data?.message || error.message))
  } finally {
    generating.value = false
  }
}

function viewCardDetail(card) {
  currentCard.value = card
  showDetailDialog.value = true
}

async function unbindDevice(card) {
  try {
    await ElMessageBox.confirm('确定解除这张卡密的设备绑定？', '解绑确认', { type: 'warning' })
    const projectId = projectStore.currentProjectId
    const { data } = await axios.post('/admin/projects/' + projectId + '/cards/' + card.id + '/unbind-device')
    if (!data.success) {
      ElMessage.error(data.message || '解绑失败')
      return
    }
    ElMessage.success('已解绑')
    await loadCards()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('解绑失败：' + (error.response?.data?.message || error.message))
  }
}

async function deleteCard(card) {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定删除这张卡密？', '删除确认', { type: 'warning' })
    const projectId = projectStore.currentProjectId
    const { data } = await axios.delete('/admin/projects/' + projectId + '/cards/' + card.id)
    if (!data.success) {
      ElMessage.error(data.message || '删除失败')
      return
    }
    ElMessage.success('已删除')
    await loadCards()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败：' + (error.response?.data?.message || error.message))
  }
}

async function copyToClipboard(text) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

function handleSearch() {
  currentPage.value = 1
  loadCards()
}

function resetFilters() {
  searchQuery.value = ''
  filterStatus.value = ''
  filterPackage.value = ''
  currentPage.value = 1
  loadCards()
}

watch(() => projectStore.currentProjectId, async () => {
  await loadPackages()
  await loadCards()
}, { immediate: true })

onMounted(async () => {
  await loadPackages()
  await loadCards()
})
</script>

<style scoped lang="scss">
.cards-page {
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

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 20px;
  margin-bottom: 24px;
  border-radius: 16px;
}

.cards-table {
  padding: 24px;
  border-radius: 16px;
}

.card-key,
.detail-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

code,
.muted-code {
  font-family: 'Courier New', monospace;
  background: rgba(255, 255, 255, 0.28);
  border-radius: 6px;
  padding: 3px 8px;
  color: #111827;
}

.empty-text,
.dialog-tip {
  color: #374151;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.card-detail {
  .detail-row {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 0;
    border-bottom: 1px solid rgba(17, 24, 39, 0.12);

    &:last-child {
      border-bottom: none;
    }

    label {
      width: 110px;
      font-weight: 600;
      color: #111827;
    }

    span {
      color: #111827;
    }
  }
}

:deep(.glass-table) {
  background: transparent !important;

  .el-table__header,
  .el-table__body,
  th,
  td {
    background: transparent !important;
  }

  th,
  td {
    border-color: rgba(255, 255, 255, 0.18) !important;
    color: #111827 !important;
  }
}
</style>
