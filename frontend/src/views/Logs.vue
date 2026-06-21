<template>
  <div class="logs-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>API调用日志</h2>
        <p>查看所有API请求的调用记录和统计</p>
      </div>
      <el-button @click="loadLogs">
        <i class="el-icon-refresh"></i>
        刷新
      </el-button>
    </div>

    <!-- 搜索筛选 -->
    <div class="filters glass-panel">
      <el-input
        v-model="searchQuery"
        placeholder="搜索API路径、API Key"
        prefix-icon="el-icon-search"
        style="width: 300px"
        @input="handleSearch"
      ></el-input>

      <el-select v-model="filterStatus" placeholder="状态" style="width: 150px" @change="loadLogs">
        <el-option label="全部状态" value=""></el-option>
        <el-option label="成功" value="success"></el-option>
        <el-option label="失败" value="failed"></el-option>
      </el-select>

      <el-date-picker
        v-model="dateRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        @change="loadLogs"
        style="width: 400px"
      ></el-date-picker>

      <el-button @click="resetFilters">重置</el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card glass-card">
        <div class="stat-label">今日调用</div>
        <div class="stat-value">{{ stats.todayCalls }}</div>
      </div>
      <div class="stat-card glass-card">
        <div class="stat-label">今日成功率</div>
        <div class="stat-value">{{ stats.todaySuccessRate }}%</div>
      </div>
      <div class="stat-card glass-card">
        <div class="stat-label">平均响应时间</div>
        <div class="stat-value">{{ stats.avgResponseTime }}ms</div>
      </div>
      <div class="stat-card glass-card">
        <div class="stat-label">总调用次数</div>
        <div class="stat-value">{{ formatNumber(stats.totalCalls) }}</div>
      </div>
    </div>

    <!-- 日志列表 -->
    <div class="logs-table glass-panel">
      <el-table
        :data="logs"
        style="width: 100%"
        class="glass-table"
        v-loading="loading"
      >
        <el-table-column prop="createdAt" label="请求时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="apiPath" label="API路径" width="200">
          <template #default="{ row }">
            <code class="api-path">{{ row.apiPath }}</code>
          </template>
        </el-table-column>

        <el-table-column prop="httpMethod" label="方法" width="80">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.httpMethod }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="httpStatus" label="HTTP状态码" width="120">
          <template #default="{ row }">
            <code :class="getHttpStatusClass(row.httpStatus)">{{ row.httpStatus }}</code>
          </template>
        </el-table-column>

        <el-table-column prop="responseTime" label="响应时间" width="120">
          <template #default="{ row }">
            <span :class="getLatencyClass(row.responseTime)">{{ row.responseTime }}ms</span>
          </template>
        </el-table-column>

        <el-table-column prop="apiKeyName" label="API Key" width="150">
          <template #default="{ row }">
            <span v-if="row.apiKeyName">{{ row.apiKeyName }}</span>
            <span v-else style="color: rgba(255,255,255,0.5)">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="clientIp" label="客户端IP" width="140">
          <template #default="{ row }">
            <code>{{ row.clientIp }}</code>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewLogDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadLogs"
        @current-change="loadLogs"
        class="pagination"
      />
    </div>

    <!-- 日志详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="API调用详情"
      width="900px"
    >
      <div v-if="currentLog" class="log-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>请求时间</label>
              <span>{{ formatDate(currentLog.createdAt) }}</span>
            </div>
            <div class="detail-item">
              <label>API路径</label>
              <code>{{ currentLog.apiPath }}</code>
            </div>
            <div class="detail-item">
              <label>HTTP方法</label>
              <el-tag size="small">{{ currentLog.httpMethod }}</el-tag>
            </div>
            <div class="detail-item">
              <label>状态</label>
              <el-tag :type="currentLog.status === 'success' ? 'success' : 'danger'">
                {{ currentLog.status === 'success' ? '成功' : '失败' }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>HTTP状态码</label>
              <code>{{ currentLog.httpStatus }}</code>
            </div>
            <div class="detail-item">
              <label>响应时间</label>
              <span>{{ currentLog.responseTime }}ms</span>
            </div>
            <div class="detail-item">
              <label>客户端IP</label>
              <code>{{ currentLog.clientIp }}</code>
            </div>
            <div class="detail-item">
              <label>API Key</label>
              <span>{{ currentLog.apiKeyName || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="currentLog.requestBody">
          <h4>请求体</h4>
          <div class="code-block">
            <pre><code>{{ formatJson(currentLog.requestBody) }}</code></pre>
          </div>
        </div>

        <div class="detail-section" v-if="currentLog.responseBody">
          <h4>响应体</h4>
          <div class="code-block">
            <pre><code>{{ formatJson(currentLog.responseBody) }}</code></pre>
          </div>
        </div>

        <div class="detail-section" v-if="currentLog.errorMessage">
          <h4>错误信息</h4>
          <el-alert
            :title="currentLog.errorMessage"
            type="error"
            :closable="false"
          ></el-alert>
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

const logs = ref([])
const loading = ref(false)

const searchQuery = ref('')
const filterStatus = ref('')
const dateRange = ref([])

const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const showDetailDialog = ref(false)
const currentLog = ref(null)

const stats = ref({
  todayCalls: 0,
  todaySuccessRate: 0,
  avgResponseTime: 0,
  totalCalls: 0
})

function getHttpStatusClass(status) {
  if (status >= 200 && status < 300) return 'status-success'
  if (status >= 400 && status < 500) return 'status-client-error'
  if (status >= 500) return 'status-server-error'
  return ''
}

function getLatencyClass(ms) {
  if (ms < 100) return 'latency-fast'
  if (ms < 500) return 'latency-normal'
  return 'latency-slow'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function formatNumber(num) {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function formatJson(str) {
  try {
    const obj = typeof str === 'string' ? JSON.parse(str) : str
    return JSON.stringify(obj, null, 2)
  } catch {
    return str
  }
}

async function loadStats() {
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const { data } = await axios.get(`/admin/projects/${projectId}/api-calls/stats`)
    if (data.success) {
      stats.value = data.data
    }
  } catch (error) {
    console.error('Failed to load stats:', error)
  }
}

async function loadLogs() {
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

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0].toISOString()
      params.endDate = dateRange.value[1].toISOString()
    }

    const { data } = await axios.get(`/admin/projects/${projectId}/api-calls`, { params })
    if (data.success) {
      logs.value = data.data.list || []
      total.value = data.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loading.value = false
  }
}

function viewLogDetail(log) {
  currentLog.value = log
  showDetailDialog.value = true
}

function handleSearch() {
  currentPage.value = 1
  loadLogs()
}

function resetFilters() {
  searchQuery.value = ''
  filterStatus.value = ''
  dateRange.value = []
  currentPage.value = 1
  loadLogs()
}

onMounted(() => {
  loadStats()
  loadLogs()
})
</script>

<style scoped lang="scss">
.logs-page {
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
  flex-wrap: wrap;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;

  .stat-card {
    padding: 20px;
    text-align: center;
    color: white;

    .stat-label {
      font-size: 13px;
      color: rgba(255, 255, 255, 0.7);
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 28px;
      font-weight: bold;
    }
  }
}

.logs-table {
  padding: 24px;
  border-radius: 16px;

  .api-path {
    font-family: 'Courier New', monospace;
    font-size: 12px;
  }

  .status-success {
    color: #43e97b;
  }

  .status-client-error {
    color: #f5a623;
  }

  .status-server-error {
    color: #f5576c;
  }

  .latency-fast {
    color: #43e97b;
    font-weight: 500;
  }

  .latency-normal {
    color: #4facfe;
    font-weight: 500;
  }

  .latency-slow {
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

.log-detail {
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

  .code-block {
    background: #f5f5f5;
    border-radius: 8px;
    padding: 16px;
    overflow-x: auto;
    max-height: 400px;

    pre {
      margin: 0;

      code {
        font-family: 'Courier New', monospace;
        font-size: 13px;
        line-height: 1.6;
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
