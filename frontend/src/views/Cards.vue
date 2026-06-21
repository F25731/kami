<template>
  <div class="cards-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>卡密管理</h2>
        <p>生成和管理项目卡密</p>
      </div>
      <el-button type="primary" @click="showGenerateDialog = true">
        <i class="el-icon-plus"></i>
        生成卡密
      </el-button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filters glass-panel">
      <el-input
        v-model="searchQuery"
        placeholder="搜索卡密、订单号"
        prefix-icon="el-icon-search"
        style="width: 300px"
        @input="handleSearch"
      ></el-input>

      <el-select v-model="filterStatus" placeholder="状态" style="width: 150px" @change="loadCards">
        <el-option label="全部状态" value=""></el-option>
        <el-option label="未使用" :value="0"></el-option>
        <el-option label="使用中" :value="1"></el-option>
        <el-option label="已过期" :value="2"></el-option>
        <el-option label="已耗尽" :value="3"></el-option>
      </el-select>

      <el-select v-model="filterPackage" placeholder="套餐" style="width: 200px" @change="loadCards">
        <el-option label="全部套餐" value=""></el-option>
        <el-option
          v-for="pkg in packages"
          :key="pkg.id"
          :label="pkg.packageName"
          :value="pkg.id"
        ></el-option>
      </el-select>

      <el-button @click="resetFilters">重置</el-button>
    </div>

    <!-- 卡密列表 -->
    <div class="cards-table glass-panel">
      <el-table
        :data="cards"
        style="width: 100%"
        class="glass-table"
        v-loading="loading"
      >
        <el-table-column prop="cardKey" label="卡密" width="200">
          <template #default="{ row }">
            <div class="card-key">
              <code>{{ row.cardKey }}</code>
              <el-button size="small" text @click="copyToClipboard(row.cardKey)">
                <i class="el-icon-copy-document"></i>
              </el-button>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="packageName" label="套餐" width="150"></el-table-column>

        <el-table-column prop="cardType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.cardType === 'count' ? 'primary' : 'success'">
              {{ row.cardType === 'count' ? '次数卡' : '时长卡' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="剩余" width="120">
          <template #default="{ row }">
            <span v-if="row.cardType === 'count'">
              {{ row.remainingCount || 0 }} 次
            </span>
            <span v-else>
              {{ row.remainingDays || 0 }} 天
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="bindDeviceId" label="绑定设备" width="150">
          <template #default="{ row }">
            <span v-if="row.bindDeviceId">{{ row.bindDeviceId.slice(0, 12) }}...</span>
            <span v-else style="color: rgba(255,255,255,0.5)">未绑定</span>
          </template>
        </el-table-column>

        <el-table-column prop="expireTime" label="过期时间" width="180">
          <template #default="{ row }">
            {{ row.expireTime ? formatDate(row.expireTime) : '永不过期' }}
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewCardDetail(row)">详情</el-button>
            <el-button size="small" type="warning" @click="unbindDevice(row)" v-if="row.bindDeviceId">
              解绑
            </el-button>
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

    <!-- 生成卡密对话框 -->
    <el-dialog
      v-model="showGenerateDialog"
      title="生成卡密"
      width="600px"
    >
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="套餐模板">
          <el-select v-model="generateForm.packageId" placeholder="选择套餐" style="width: 100%">
            <el-option
              v-for="pkg in packages"
              :key="pkg.id"
              :label="pkg.packageName"
              :value="pkg.id"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="生成数量">
          <el-input-number v-model="generateForm.quantity" :min="1" :max="1000"></el-input-number>
        </el-form-item>

        <el-form-item label="订单号">
          <el-input v-model="generateForm.orderNo" placeholder="可选，用于关联业务订单"></el-input>
        </el-form-item>

        <el-form-item label="过期时间">
          <el-date-picker
            v-model="generateForm.expireTime"
            type="datetime"
            placeholder="留空表示永不过期"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>

        <el-form-item label="卡密前缀">
          <el-input v-model="generateForm.prefix" placeholder="可选，例如：VIP"></el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button type="primary" @click="generateCards" :loading="generating">生成</el-button>
      </template>
    </el-dialog>

    <!-- 卡密详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="卡密详情"
      width="700px"
    >
      <div v-if="currentCard" class="card-detail">
        <div class="detail-row">
          <label>卡密</label>
          <div class="detail-value">
            <code>{{ currentCard.cardKey }}</code>
            <el-button size="small" @click="copyToClipboard(currentCard.cardKey)">复制</el-button>
          </div>
        </div>

        <div class="detail-row">
          <label>套餐</label>
          <span>{{ currentCard.packageName }}</span>
        </div>

        <div class="detail-row">
          <label>类型</label>
          <span>{{ currentCard.cardType === 'count' ? '次数卡' : '时长卡' }}</span>
        </div>

        <div class="detail-row">
          <label>总量/剩余</label>
          <span v-if="currentCard.cardType === 'count'">
            {{ currentCard.totalCount }} / {{ currentCard.remainingCount }} 次
          </span>
          <span v-else>
            {{ currentCard.totalDays }} / {{ currentCard.remainingDays }} 天
          </span>
        </div>

        <div class="detail-row">
          <label>状态</label>
          <el-tag :type="getStatusType(currentCard.status)">
            {{ getStatusText(currentCard.status) }}
          </el-tag>
        </div>

        <div class="detail-row" v-if="currentCard.bindDeviceId">
          <label>绑定设备</label>
          <span>{{ currentCard.bindDeviceId }}</span>
        </div>

        <div class="detail-row" v-if="currentCard.orderNo">
          <label>订单号</label>
          <span>{{ currentCard.orderNo }}</span>
        </div>

        <div class="detail-row">
          <label>创建时间</label>
          <span>{{ formatDate(currentCard.createdAt) }}</span>
        </div>

        <div class="detail-row" v-if="currentCard.activatedAt">
          <label>激活时间</label>
          <span>{{ formatDate(currentCard.activatedAt) }}</span>
        </div>

        <div class="detail-row" v-if="currentCard.expireTime">
          <label>过期时间</label>
          <span>{{ formatDate(currentCard.expireTime) }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useProjectStore } from '@/stores/project'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const showDetailDialog = ref(false)
const currentCard = ref(null)

const generateForm = ref({
  packageId: null,
  quantity: 10,
  orderNo: '',
  expireTime: null,
  prefix: ''
})

function getStatusType(status) {
  const types = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
  return types[status] || 'info'
}

function getStatusText(status) {
  const texts = { 0: '未使用', 1: '使用中', 2: '已过期', 3: '已耗尽' }
  return texts[status] || '未知'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadCards() {
  loading.value = true
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const params = {
      page: currentPage.value,
      size: pageSize.value,
      search: searchQuery.value,
      status: filterStatus.value,
      packageId: filterPackage.value
    }

    const { data } = await axios.get(`/admin/projects/${projectId}/cards`, { params })
    if (data.success) {
      cards.value = data.data.list || []
      total.value = data.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loading.value = false
  }
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
    console.error('Failed to load packages:', error)
  }
}

async function generateCards() {
  if (!generateForm.value.packageId) {
    ElMessage.warning('请选择套餐模板')
    return
  }

  generating.value = true
  try {
    const projectId = projectStore.currentProjectId
    const { data } = await axios.post(`/admin/projects/${projectId}/cards/generate`, generateForm.value)

    if (data.success) {
      ElMessage.success(`成功生成 ${generateForm.value.quantity} 张卡密`)
      showGenerateDialog.value = false
      generateForm.value = {
        packageId: null,
        quantity: 10,
        orderNo: '',
        expireTime: null,
        prefix: ''
      }
      await loadCards()
    }
  } catch (error) {
    ElMessage.error('生成失败：' + error.message)
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
    await ElMessageBox.confirm('确认解绑此卡密的设备绑定？', '确认解绑', { type: 'warning' })

    const projectId = projectStore.currentProjectId
    await axios.post(`/admin/projects/${projectId}/cards/${card.id}/unbind-device`)

    ElMessage.success('已解绑')
    await loadCards()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('解绑失败：' + error.message)
    }
  }
}

async function deleteCard(card) {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确认删除此卡密？', '确认删除', { type: 'warning' })

    const projectId = projectStore.currentProjectId
    await axios.delete(`/admin/projects/${projectId}/cards/${card.id}`)

    ElMessage.success('已删除')
    await loadCards()
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

onMounted(() => {
  loadCards()
  loadPackages()
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

.filters {
  display: flex;
  gap: 12px;
  padding: 20px;
  margin-bottom: 24px;
  border-radius: 16px;
}

.cards-table {
  padding: 24px;
  border-radius: 16px;

  .card-key {
    display: flex;
    align-items: center;
    gap: 8px;

    code {
      font-family: 'Courier New', monospace;
      font-size: 13px;
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;

    :deep(.el-pagination) {
      .el-pager li, .btn-prev, .btn-next {
        background: rgba(255, 255, 255, 0.1);
        color: white;

        &.active {
          background: rgba(79, 172, 254, 0.5);
        }
      }
    }
  }
}

.card-detail {
  .detail-row {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #eee;

    &:last-child {
      border-bottom: none;
    }

    label {
      width: 120px;
      font-weight: 500;
      color: #666;
    }

    .detail-value {
      display: flex;
      align-items: center;
      gap: 12px;

      code {
        font-family: 'Courier New', monospace;
        background: #f5f5f5;
        padding: 4px 8px;
        border-radius: 4px;
      }
    }
  }
}

:deep(.glass-table) {
  background: transparent !important;

  .el-table__header, .el-table__body {
    background: transparent !important;
  }

  th, td {
    background: transparent !important;
    border-color: rgba(255, 255, 255, 0.1) !important;
    color: white !important;
  }

  th {
    color: rgba(255, 255, 255, 0.7) !important;
  }
}
</style>
