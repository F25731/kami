<template>
  <div class="entitlements-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>用户权益</h2>
        <p>用户兑换卡密后的权益管理</p>
      </div>
    </div>

    <!-- 搜索筛选 -->
    <div class="filters glass-panel">
      <el-input
        v-model="searchQuery"
        placeholder="搜索用户ID"
        prefix-icon="el-icon-search"
        style="width: 300px"
        @input="handleSearch"
      ></el-input>

      <el-select v-model="filterStatus" placeholder="状态" style="width: 150px" @change="loadEntitlements">
        <el-option label="全部状态" value=""></el-option>
        <el-option label="正常" value="active"></el-option>
        <el-option label="已过期" value="expired"></el-option>
        <el-option label="已耗尽" value="depleted"></el-option>
      </el-select>

      <el-button @click="resetFilters">重置</el-button>
    </div>

    <!-- 权益列表 -->
    <div class="entitlements-table glass-panel">
      <el-table
        :data="entitlements"
        style="width: 100%"
        class="glass-table"
        v-loading="loading"
      >
        <el-table-column prop="userId" label="用户ID" width="180">
          <template #default="{ row }">
            <code>{{ row.userId }}</code>
          </template>
        </el-table-column>

        <el-table-column prop="packageName" label="套餐" width="150"></el-table-column>

        <el-table-column prop="entitlementType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.entitlementType === 'count' ? 'primary' : 'success'">
              {{ row.entitlementType === 'count' ? '次数' : '时长' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="总量" width="100">
          <template #default="{ row }">
            <span v-if="row.entitlementType === 'count'">{{ row.totalCount }} 次</span>
            <span v-else>{{ row.totalDays }} 天</span>
          </template>
        </el-table-column>

        <el-table-column label="剩余" width="100">
          <template #default="{ row }">
            <span v-if="row.entitlementType === 'count'" :class="getRemainClass(row.remainingCount, row.totalCount)">
              {{ row.remainingCount }} 次
            </span>
            <span v-else :class="getRemainClass(row.remainingDays, row.totalDays)">
              {{ row.remainingDays }} 天
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

        <el-table-column prop="redeemedAt" label="兑换时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.redeemedAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="expireTime" label="过期时间" width="180">
          <template #default="{ row }">
            {{ row.expireTime ? formatDate(row.expireTime) : '永不过期' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadEntitlements"
        @current-change="loadEntitlements"
        class="pagination"
      />
    </div>

    <!-- 权益详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="权益详情"
      width="700px"
    >
      <div v-if="currentEntitlement" class="entitlement-detail">
        <div class="detail-row">
          <label>用户ID</label>
          <code>{{ currentEntitlement.userId }}</code>
        </div>

        <div class="detail-row">
          <label>套餐</label>
          <span>{{ currentEntitlement.packageName }}</span>
        </div>

        <div class="detail-row">
          <label>类型</label>
          <el-tag :type="currentEntitlement.entitlementType === 'count' ? 'primary' : 'success'">
            {{ currentEntitlement.entitlementType === 'count' ? '次数权益' : '时长权益' }}
          </el-tag>
        </div>

        <div class="detail-row">
          <label>总量 / 剩余</label>
          <span v-if="currentEntitlement.entitlementType === 'count'">
            {{ currentEntitlement.totalCount }} / {{ currentEntitlement.remainingCount }} 次
          </span>
          <span v-else>
            {{ currentEntitlement.totalDays }} / {{ currentEntitlement.remainingDays }} 天
          </span>
        </div>

        <div class="detail-row">
          <label>状态</label>
          <el-tag :type="getStatusType(currentEntitlement.status)">
            {{ getStatusText(currentEntitlement.status) }}
          </el-tag>
        </div>

        <div class="detail-row">
          <label>兑换时间</label>
          <span>{{ formatDate(currentEntitlement.redeemedAt) }}</span>
        </div>

        <div class="detail-row" v-if="currentEntitlement.expireTime">
          <label>过期时间</label>
          <span>{{ formatDate(currentEntitlement.expireTime) }}</span>
        </div>

        <div class="detail-row" v-if="currentEntitlement.originalCardKey">
          <label>原始卡密</label>
          <code>{{ currentEntitlement.originalCardKey }}</code>
        </div>

        <div class="consume-logs-section">
          <h4>消耗记录</h4>
          <div class="logs-list">
            <div v-for="log in consumeLogs" :key="log.id" class="log-item">
              <div class="log-info">
                <span class="log-time">{{ formatDate(log.consumedAt) }}</span>
                <span class="log-count">消耗 {{ log.consumeCount }} {{ currentEntitlement.entitlementType === 'count' ? '次' : '天' }}</span>
                <code v-if="log.bizId" class="log-biz">{{ log.bizId }}</code>
              </div>
              <el-tag v-if="log.refunded" type="danger" size="small">已退款</el-tag>
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

const entitlements = ref([])
const consumeLogs = ref([])
const loading = ref(false)

const searchQuery = ref('')
const filterStatus = ref('')

const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const showDetailDialog = ref(false)
const currentEntitlement = ref(null)

function getStatusType(status) {
  const types = { active: 'success', expired: 'warning', depleted: 'info' }
  return types[status] || 'info'
}

function getStatusText(status) {
  const texts = { active: '正常', expired: '已过期', depleted: '已耗尽' }
  return texts[status] || '未知'
}

function getRemainClass(remaining, total) {
  const percent = (remaining / total) * 100
  if (percent <= 10) return 'remain-low'
  if (percent <= 30) return 'remain-medium'
  return 'remain-high'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadEntitlements() {
  loading.value = true
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const params = {
      page: currentPage.value,
      size: pageSize.value,
      search: searchQuery.value,
      status: filterStatus.value
    }

    const { data } = await axios.get(`/admin/projects/${projectId}/entitlements`, { params })
    if (data.success) {
      entitlements.value = data.data.list || []
      total.value = data.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loading.value = false
  }
}

async function viewDetail(entitlement) {
  currentEntitlement.value = entitlement
  showDetailDialog.value = true

  // 加载消耗记录
  try {
    const projectId = projectStore.currentProjectId
    const { data } = await axios.get(`/admin/projects/${projectId}/entitlements/${entitlement.id}/consume-logs`)
    if (data.success) {
      consumeLogs.value = data.data || []
    }
  } catch (error) {
    ElMessage.error('加载消耗记录失败：' + error.message)
  }
}

function handleSearch() {
  currentPage.value = 1
  loadEntitlements()
}

function resetFilters() {
  searchQuery.value = ''
  filterStatus.value = ''
  currentPage.value = 1
  loadEntitlements()
}

onMounted(() => {
  loadEntitlements()
})
</script>

<style scoped lang="scss">
.entitlements-page {
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

.entitlements-table {
  padding: 24px;
  border-radius: 16px;

  .remain-high {
    color: #43e97b;
    font-weight: 500;
  }

  .remain-medium {
    color: #f5a623;
    font-weight: 500;
  }

  .remain-low {
    color: #f5576c;
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

.entitlement-detail {
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

    code {
      font-family: 'Courier New', monospace;
      background: #f5f5f5;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 13px;
    }
  }

  .consume-logs-section {
    margin-top: 24px;
    padding-top: 24px;
    border-top: 2px solid #eee;

    h4 {
      font-size: 15px;
      margin: 0 0 16px 0;
      color: #333;
    }

    .logs-list {
      max-height: 300px;
      overflow-y: auto;

      .log-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px;
        margin-bottom: 8px;
        background: #f9f9f9;
        border-radius: 6px;

        &:last-child {
          margin-bottom: 0;
        }

        .log-info {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .log-time {
            font-size: 13px;
            color: #666;
          }

          .log-count {
            font-size: 14px;
            font-weight: 500;
            color: #333;
          }

          .log-biz {
            font-size: 12px;
            font-family: 'Courier New', monospace;
            background: #e8e8e8;
            padding: 2px 6px;
            border-radius: 3px;
          }
        }
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
