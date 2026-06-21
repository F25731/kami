<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card glass-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
          <i class="el-icon-tickets"></i>
        </div>
        <div class="stat-content">
          <div class="stat-label">总卡密数</div>
          <div class="stat-value">{{ stats.totalCards }}</div>
          <div class="stat-change positive">
            <i class="el-icon-top"></i> +{{ stats.newCardsToday }} 今日新增
          </div>
        </div>
      </div>

      <div class="stat-card glass-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
          <i class="el-icon-data-line"></i>
        </div>
        <div class="stat-content">
          <div class="stat-label">API调用次数</div>
          <div class="stat-value">{{ formatNumber(stats.totalApiCalls) }}</div>
          <div class="stat-change positive">
            <i class="el-icon-top"></i> +{{ stats.apiCallsToday }} 今日
          </div>
        </div>
      </div>

      <div class="stat-card glass-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
          <i class="el-icon-success"></i>
        </div>
        <div class="stat-content">
          <div class="stat-label">成功率</div>
          <div class="stat-value">{{ stats.successRate }}%</div>
          <div class="stat-change" :class="stats.successRate >= 99 ? 'positive' : 'negative'">
            <i :class="stats.successRate >= 99 ? 'el-icon-top' : 'el-icon-bottom'"></i>
            {{ stats.successRate >= 99 ? '优秀' : '需优化' }}
          </div>
        </div>
      </div>

      <div class="stat-card glass-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
          <i class="el-icon-timer"></i>
        </div>
        <div class="stat-content">
          <div class="stat-label">平均延迟</div>
          <div class="stat-value">{{ stats.avgLatency }}ms</div>
          <div class="stat-change" :class="stats.avgLatency < 100 ? 'positive' : 'negative'">
            <i :class="stats.avgLatency < 100 ? 'el-icon-bottom' : 'el-icon-top'"></i>
            P99: {{ stats.p99Latency }}ms
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <!-- API调用趋势 -->
      <div class="chart-card glass-panel">
        <div class="chart-header">
          <h3>API调用趋势</h3>
          <el-radio-group v-model="timeRange" size="small" @change="loadApiCallTrend">
            <el-radio-button label="24h">24小时</el-radio-button>
            <el-radio-button label="7d">7天</el-radio-button>
            <el-radio-button label="30d">30天</el-radio-button>
          </el-radio-group>
        </div>
        <div class="chart-content" ref="apiCallChart"></div>
      </div>

      <!-- 延迟分布 -->
      <div class="chart-card glass-panel">
        <div class="chart-header">
          <h3>响应延迟分布</h3>
          <div class="latency-metrics">
            <span>P50: {{ stats.p50Latency }}ms</span>
            <span>P95: {{ stats.p95Latency }}ms</span>
            <span>P99: {{ stats.p99Latency }}ms</span>
          </div>
        </div>
        <div class="chart-content" ref="latencyChart"></div>
      </div>
    </div>

    <!-- 卡密使用情况 -->
    <div class="usage-section">
      <div class="usage-card glass-panel">
        <div class="chart-header">
          <h3>卡密使用情况</h3>
        </div>
        <div class="usage-stats">
          <div class="usage-item">
            <div class="usage-label">未使用</div>
            <div class="usage-bar">
              <div class="usage-fill" :style="{ width: cardUsagePercent.unused + '%', background: '#43e97b' }"></div>
            </div>
            <div class="usage-value">{{ cardUsage.unused }} ({{ cardUsagePercent.unused }}%)</div>
          </div>
          <div class="usage-item">
            <div class="usage-label">使用中</div>
            <div class="usage-bar">
              <div class="usage-fill" :style="{ width: cardUsagePercent.active + '%', background: '#4facfe' }"></div>
            </div>
            <div class="usage-value">{{ cardUsage.active }} ({{ cardUsagePercent.active }}%)</div>
          </div>
          <div class="usage-item">
            <div class="usage-label">已过期</div>
            <div class="usage-bar">
              <div class="usage-fill" :style="{ width: cardUsagePercent.expired + '%', background: '#f5576c' }"></div>
            </div>
            <div class="usage-value">{{ cardUsage.expired }} ({{ cardUsagePercent.expired }}%)</div>
          </div>
          <div class="usage-item">
            <div class="usage-label">已耗尽</div>
            <div class="usage-bar">
              <div class="usage-fill" :style="{ width: cardUsagePercent.depleted + '%', background: '#a8a8a8' }"></div>
            </div>
            <div class="usage-value">{{ cardUsage.depleted }} ({{ cardUsagePercent.depleted }}%)</div>
          </div>
        </div>
      </div>

      <!-- 最近订单 -->
      <div class="recent-orders glass-panel">
        <div class="chart-header">
          <h3>最近发卡订单</h3>
          <router-link to="/orders" class="view-all-link">查看全部</router-link>
        </div>
        <el-table :data="recentOrders" style="width: 100%" class="glass-table">
          <el-table-column prop="orderNo" label="订单号" width="180"></el-table-column>
          <el-table-column prop="packageName" label="套餐" width="150"></el-table-column>
          <el-table-column prop="quantity" label="数量" width="80"></el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180"></el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'completed' ? 'success' : 'info'" size="small">
                {{ row.status === 'completed' ? '已完成' : '处理中' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <el-button class="action-btn glass-card" @click="$router.push('/cards')">
        <i class="el-icon-plus"></i>
        <span>生成卡密</span>
      </el-button>
      <el-button class="action-btn glass-card" @click="$router.push('/packages')">
        <i class="el-icon-box"></i>
        <span>创建套餐</span>
      </el-button>
      <el-button class="action-btn glass-card" @click="$router.push('/api-keys')">
        <i class="el-icon-key"></i>
        <span>管理API密钥</span>
      </el-button>
      <el-button class="action-btn glass-card" @click="$router.push('/api-docs')">
        <i class="el-icon-document"></i>
        <span>查看API文档</span>
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useProjectStore } from '@/stores/project'
import * as echarts from 'echarts'
import axios from 'axios'

const projectStore = useProjectStore()
const timeRange = ref('24h')

// 统计数据
const stats = ref({
  totalCards: 0,
  newCardsToday: 0,
  totalApiCalls: 0,
  apiCallsToday: 0,
  successRate: 99.8,
  avgLatency: 45,
  p50Latency: 32,
  p95Latency: 89,
  p99Latency: 156
})

const cardUsage = ref({
  unused: 0,
  active: 0,
  expired: 0,
  depleted: 0
})

const cardUsagePercent = computed(() => {
  const total = cardUsage.value.unused + cardUsage.value.active + cardUsage.value.expired + cardUsage.value.depleted
  if (total === 0) return { unused: 0, active: 0, expired: 0, depleted: 0 }
  return {
    unused: Math.round((cardUsage.value.unused / total) * 100),
    active: Math.round((cardUsage.value.active / total) * 100),
    expired: Math.round((cardUsage.value.expired / total) * 100),
    depleted: Math.round((cardUsage.value.depleted / total) * 100)
  }
})

const recentOrders = ref([])

const apiCallChart = ref(null)
const latencyChart = ref(null)

// 格式化数字
function formatNumber(num) {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

// 加载统计数据
async function loadStats() {
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const { data } = await axios.get(`/admin/projects/${projectId}/stats`)
    if (data.success) {
      stats.value = data.data
    }
  } catch (error) {
    console.error('Failed to load stats:', error)
  }
}

// 加载卡密使用情况
async function loadCardUsage() {
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const { data } = await axios.get(`/admin/projects/${projectId}/cards/usage`)
    if (data.success) {
      cardUsage.value = data.data
    }
  } catch (error) {
    console.error('Failed to load card usage:', error)
  }
}

// 加载最近订单
async function loadRecentOrders() {
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const { data } = await axios.get(`/admin/projects/${projectId}/orders?limit=5`)
    if (data.success) {
      recentOrders.value = data.data
    }
  } catch (error) {
    console.error('Failed to load recent orders:', error)
  }
}

// 加载API调用趋势图表
async function loadApiCallTrend() {
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const { data } = await axios.get(`/admin/projects/${projectId}/api-calls/trend?range=${timeRange.value}`)

    await nextTick()

    const chart = echarts.init(apiCallChart.value)
    chart.setOption({
      backgroundColor: 'transparent',
      grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.data?.labels || [],
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.3)' } },
        axisLabel: { color: 'rgba(255, 255, 255, 0.7)' }
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.3)' } },
        axisLabel: { color: 'rgba(255, 255, 255, 0.7)' },
        splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } }
      },
      series: [{
        data: data.data?.values || [],
        type: 'line',
        smooth: true,
        itemStyle: { color: '#4facfe' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(79, 172, 254, 0.5)' },
            { offset: 1, color: 'rgba(79, 172, 254, 0.1)' }
          ])
        }
      }],
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(0, 0, 0, 0.7)', borderColor: 'transparent' }
    })
  } catch (error) {
    console.error('Failed to load API call trend:', error)
  }
}

// 加载延迟分布图表
async function loadLatencyChart() {
  try {
    const projectId = projectStore.currentProjectId
    if (!projectId) return

    const { data } = await axios.get(`/admin/projects/${projectId}/api-calls/latency-distribution`)

    await nextTick()

    const chart = echarts.init(latencyChart.value)
    chart.setOption({
      backgroundColor: 'transparent',
      grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.data?.ranges || ['0-50ms', '50-100ms', '100-200ms', '200-500ms', '500ms+'],
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.3)' } },
        axisLabel: { color: 'rgba(255, 255, 255, 0.7)' }
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.3)' } },
        axisLabel: { color: 'rgba(255, 255, 255, 0.7)' },
        splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } }
      },
      series: [{
        data: data.data?.counts || [1200, 850, 320, 45, 12],
        type: 'bar',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#43e97b' },
            { offset: 1, color: '#38f9d7' }
          ]),
          borderRadius: [8, 8, 0, 0]
        },
        barWidth: '50%'
      }],
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(0, 0, 0, 0.7)', borderColor: 'transparent' }
    })
  } catch (error) {
    console.error('Failed to load latency chart:', error)
  }
}

onMounted(() => {
  loadStats()
  loadCardUsage()
  loadRecentOrders()
  loadApiCallTrend()
  loadLatencyChart()
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  color: white;

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    flex-shrink: 0;
  }

  .stat-content {
    flex: 1;

    .stat-label {
      font-size: 13px;
      color: rgba(255, 255, 255, 0.7);
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 28px;
      font-weight: bold;
      margin-bottom: 8px;
    }

    .stat-change {
      font-size: 12px;
      display: flex;
      align-items: center;
      gap: 4px;

      &.positive {
        color: #43e97b;
      }

      &.negative {
        color: #f5576c;
      }
    }
  }
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  padding: 20px;
  border-radius: 16px;

  .chart-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    h3 {
      color: white;
      font-size: 16px;
      font-weight: 600;
      margin: 0;
    }

    .latency-metrics {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: rgba(255, 255, 255, 0.7);

      span {
        padding: 4px 8px;
        background: rgba(255, 255, 255, 0.1);
        border-radius: 4px;
      }
    }
  }

  .chart-content {
    width: 100%;
    height: 280px;
  }
}

.usage-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.usage-card, .recent-orders {
  padding: 20px;
  border-radius: 16px;
}

.usage-stats {
  .usage-item {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }

    .usage-label {
      color: rgba(255, 255, 255, 0.7);
      font-size: 14px;
      margin-bottom: 8px;
    }

    .usage-bar {
      height: 32px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      overflow: hidden;
      margin-bottom: 8px;

      .usage-fill {
        height: 100%;
        transition: width 0.5s ease;
        border-radius: 8px;
      }
    }

    .usage-value {
      color: white;
      font-size: 13px;
      font-weight: 500;
    }
  }
}

.view-all-link {
  color: #4facfe;
  font-size: 13px;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.quick-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;

  .action-btn {
    flex: 1;
    min-width: 200px;
    height: 80px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: white !important;
    font-size: 15px;
    font-weight: 500;

    i {
      font-size: 28px;
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
