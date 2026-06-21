<template>
  <div class="orders-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>发卡订单</h2>
        <p>通过API批量生成卡密的订单记录</p>
      </div>
    </div>

    <!-- 搜索筛选 -->
    <div class="filters glass-panel">
      <el-input
        v-model="searchQuery"
        placeholder="搜索订单号"
        prefix-icon="el-icon-search"
        style="width: 300px"
        @input="handleSearch"
      ></el-input>

      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @change="loadOrders"
      ></el-date-picker>

      <el-button @click="resetFilters">重置</el-button>
    </div>

    <!-- 订单列表 -->
    <div class="orders-table glass-panel">
      <el-table
        :data="orders"
        style="width: 100%"
        class="glass-table"
        v-loading="loading"
      >
        <el-table-column prop="orderNo" label="订单号" width="200">
          <template #default="{ row }">
            <code>{{ row.orderNo }}</code>
          </template>
        </el-table-column>

        <el-table-column prop="packageName" label="套餐" width="180"></el-table-column>

        <el-table-column prop="quantity" label="数量" width="100">
          <template #default="{ row }">
            <span class="quantity-text">{{ row.quantity }} 张</span>
          </template>
        </el-table-column>

        <el-table-column prop="source" label="来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.source === 'api' ? 'primary' : 'info'">
              {{ row.source === 'api' ? 'API调用' : '手动生成' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="completedAt" label="完成时间" width="180">
          <template #default="{ row }">
            {{ row.completedAt ? formatDate(row.completedAt) : '-' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewOrderDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadOrders"
        @current-change="loadOrders"
        class="pagination"
      />
    </div>

    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="订单详情"
      width="900px"
    >
      <div v-if="currentOrder" class="order-detail">
        <div class="detail-section">
          <h4>订单信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>订单号</label>
              <code>{{ currentOrder.orderNo }}</code>
            </div>
            <div class="detail-item">
              <label>套餐</label>
              <span>{{ currentOrder.packageName }}</span>
            </div>
            <div class="detail-item">
              <label>数量</label>
              <span>{{ currentOrder.quantity }} 张</span>
            </div>
            <div class="detail-item">
              <label>来源</label>
              <el-tag size="small" :type="currentOrder.source === 'api' ? 'primary' : 'info'">
                {{ currentOrder.source === 'api' ? 'API调用' : '手动生成' }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>状态</label>
              <el-tag :type="getStatusType(currentOrder.status)">
                {{ getStatusText(currentOrder.status) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>创建时间</label>
              <span>{{ formatDate(currentOrder.createdAt) }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-header">
            <h4>生成的卡密列表</h4>
            <el-button size="small" @click="exportCards(currentOrder)">
              <i class="el-icon-download"></i>
              导出卡密
            </el-button>
          </div>
          <div class="cards-list">
            <div v-for="card in orderCards" :key="card.id" class="card-item">
              <code>{{ card.cardKey }}</code>
              <el-button size="small" text @click="copyToClipboard(card.cardKey)">
                <i class="el-icon-copy-document"></i>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useProjectStore } from '@/stores/project'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const projectStore = useProjectStore()

const orders = ref([])
const orderCards = ref([])
const loading = ref(false)

const searchQuery = ref('')
const dateRange = ref([])

const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const showDetailDialog = ref(false)
const currentOrder = ref(null)

function getStatusType(status) {
  const types = { completed: 'success', processing: 'warning', failed: 'danger' }
  return types[status] || 'info'
}

function getStatusText(status) {
  const texts = { completed: '已完成', processing: '处理中', failed: '失败' }
  return texts[status] || '未知'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadOrders() {
  loading.value = true
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const params = {
      page: currentPage.value,
      size: pageSize.value,
      search: searchQuery.value
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0].toISOString()
      params.endDate = dateRange.value[1].toISOString()
    }

    const { data } = await axios.get(`/admin/projects/${projectId}/orders`, { params })
    if (data.success) {
      orders.value = data.data.list || []
      total.value = data.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loading.value = false
  }
}

async function viewOrderDetail(order) {
  currentOrder.value = order
  showDetailDialog.value = true

  // 加载该订单的卡密列表
  try {
    const projectId = projectStore.currentProjectId
    const { data } = await axios.get(`/admin/projects/${projectId}/orders/${order.id}/cards`)
    if (data.success) {
      orderCards.value = data.data || []
    }
  } catch (error) {
    ElMessage.error('加载卡密列表失败：' + error.message)
  }
}

function exportCards(order) {
  const cardsText = orderCards.value.map(c => c.cardKey).join('\n')
  const blob = new Blob([cardsText], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `cards_${order.orderNo}.txt`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出')
}

function copyToClipboard(text) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

function handleSearch() {
  currentPage.value = 1
  loadOrders()
}

function resetFilters() {
  searchQuery.value = ''
  dateRange.value = []
  currentPage.value = 1
  loadOrders()
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped lang="scss">
.orders-page {
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

.orders-table {
  padding: 24px;
  border-radius: 16px;

  .quantity-text {
    font-weight: 500;
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

.order-detail {
  .detail-section {
    margin-bottom: 30px;

    &:last-child {
      margin-bottom: 0;
    }

    h4 {
      font-size: 16px;
      font-weight: 600;
      margin: 0 0 16px 0;
      color: #333;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      h4 {
        margin: 0;
      }
    }
  }

  .detail-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;

    .detail-item {
      label {
        display: block;
        font-size: 13px;
        color: #666;
        margin-bottom: 6px;
      }

      code {
        font-family: 'Courier New', monospace;
        background: #f5f5f5;
        padding: 4px 8px;
        border-radius: 4px;
        font-size: 13px;
      }
    }
  }

  .cards-list {
    max-height: 400px;
    overflow-y: auto;
    background: #f9f9f9;
    border-radius: 8px;
    padding: 16px;

    .card-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 12px;
      margin-bottom: 8px;
      background: white;
      border-radius: 6px;

      &:last-child {
        margin-bottom: 0;
      }

      code {
        font-family: 'Courier New', monospace;
        font-size: 13px;
        color: #333;
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
