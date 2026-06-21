<template>
  <div class="records-page">
    <div class="page-header">
      <div>
        <h2>发卡订单</h2>
        <p>查看外部商城或接口同步过来的发卡记录，保留卡密追踪和补处理入口。</p>
      </div>
      <button type="button" @click="loadRecords" :disabled="loading">刷新</button>
    </div>

    <section class="filters">
      <label>记录号<input v-model="filters.orderId" placeholder="ISS/外部单号" /></label>
      <label>用户<input v-model="filters.username" placeholder="用户名" /></label>
      <label>状态
        <select v-model="filters.status">
          <option value="">全部</option>
          <option value="completed">已发卡</option>
          <option value="pending">待处理</option>
          <option value="failed">失败</option>
          <option value="refunded">已退回</option>
        </select>
      </label>
      <label>卡类型
        <select v-model="filters.cardType">
          <option value="">全部</option>
          <option value="time">时间卡</option>
          <option value="count">次数卡</option>
        </select>
      </label>
      <button type="button" class="primary" @click="loadRecords">筛选</button>
      <button type="button" @click="resetFilters">重置</button>
    </section>

    <section class="stats">
      <div><span>总记录</span><strong>{{ records.length }}</strong></div>
      <div><span>已发卡</span><strong>{{ completedCount }}</strong></div>
      <div><span>待处理</span><strong>{{ pendingCount }}</strong></div>
      <div><span>卡密数</span><strong>{{ cardCount }}</strong></div>
    </section>

    <section class="table-wrap">
      <div v-if="loading" class="empty">正在加载...</div>
      <div v-else-if="records.length === 0" class="empty">暂无发卡记录。</div>
      <table v-else>
        <thead>
          <tr>
            <th>记录号</th>
            <th>外部单号</th>
            <th>用户</th>
            <th>卡密类型</th>
            <th>数量</th>
            <th>来源</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in records" :key="record.order_id">
            <td><code>{{ record.order_id }}</code></td>
            <td>{{ record.external_order_no || '-' }}</td>
            <td>{{ record.username || '-' }}</td>
            <td>{{ cardTypeText(record.card_type) }} / {{ record.card_spec }}</td>
            <td>{{ record.quantity }}</td>
            <td>{{ sourceText(record.source) }}</td>
            <td><span :class="['status', record.status]">{{ statusText(record.status) }}</span></td>
            <td>{{ record.purchase_time || record.create_time || '-' }}</td>
            <td><button type="button" @click="openDetail(record)">详情</button></td>
          </tr>
        </tbody>
      </table>
    </section>

    <div v-if="selectedRecord" class="modal" @click.self="selectedRecord = null">
      <div class="dialog">
        <div class="dialog-header">
          <h3>发卡记录详情</h3>
          <button type="button" @click="selectedRecord = null">关闭</button>
        </div>
        <div class="detail-grid">
          <div><span>记录号</span><strong>{{ selectedRecord.order_id }}</strong></div>
          <div><span>外部单号</span><strong>{{ selectedRecord.external_order_no || '-' }}</strong></div>
          <div><span>来源</span><strong>{{ sourceText(selectedRecord.source) }}</strong></div>
          <div><span>状态</span><strong>{{ statusText(selectedRecord.status) }}</strong></div>
        </div>
        <h4>卡密</h4>
        <div v-if="selectedKeys.length === 0" class="empty small">暂无卡密。</div>
        <div v-else class="keys">
          <div v-for="key in selectedKeys" :key="key" class="key-row">
            <code>{{ key }}</code>
            <button type="button" @click="copy(key)">复制</button>
          </div>
        </div>
        <div class="dialog-actions" v-if="selectedRecord.status === 'pending'">
          <button type="button" class="primary" @click="setStatus(selectedRecord, 'completed')">标记已发卡</button>
          <button type="button" class="danger" @click="setStatus(selectedRecord, 'failed')">标记失败</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { orderApi } from '../services/api.js'

const loading = ref(false)
const records = ref([])
const selectedRecord = ref(null)
const filters = reactive({ orderId: '', username: '', status: '', cardType: '' })

const completedCount = computed(() => records.value.filter(item => item.status === 'completed').length)
const pendingCount = computed(() => records.value.filter(item => item.status === 'pending').length)
const cardCount = computed(() => records.value.reduce((sum, item) => sum + splitKeys(item.card_keys).length, 0))
const selectedKeys = computed(() => selectedRecord.value ? splitKeys(selectedRecord.value.card_keys) : [])

const splitKeys = (value) => {
  if (Array.isArray(value)) return value.filter(Boolean)
  if (typeof value === 'string' && value.trim()) return value.split(',').map(item => item.trim()).filter(Boolean)
  return []
}

const loadRecords = async () => {
  loading.value = true
  try {
    const response = await orderApi.getAllOrders(filters)
    records.value = response.success ? response.data : []
  } catch (error) {
    ElMessage.error('获取发卡记录失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  Object.assign(filters, { orderId: '', username: '', status: '', cardType: '' })
  loadRecords()
}

const openDetail = (record) => {
  selectedRecord.value = record
}

const setStatus = async (record, status) => {
  try {
    const response = await orderApi.updateOrderStatus(record.order_id, status)
    if (response.success) {
      ElMessage.success('状态已更新')
      selectedRecord.value = null
      await loadRecords()
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error) {
    ElMessage.error('更新失败: ' + (error.message || '未知错误'))
  }
}

const copy = async (text) => {
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

const statusText = (status) => ({ completed: '已发卡', pending: '待处理', failed: '失败', refunded: '已退回' }[status] || status || '未知')
const cardTypeText = (type) => type === 'time' ? '时间卡' : type === 'count' ? '次数卡' : '卡密'
const sourceText = (source) => source === 'external_shop' || !source ? '外部商城' : source

onMounted(loadRecords)
</script>

<style scoped>
.records-page { padding: 1rem; color: #111827; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
h2, h3, h4, p { margin-top: 0; }
.page-header p { margin-bottom: 0; color: #6b7280; }
button { border: 1px solid #d1d5db; border-radius: 8px; background: #fff; padding: 8px 12px; cursor: pointer; }
button.primary { color: #fff; background: #2563eb; border-color: #2563eb; }
button.danger { color: #fff; background: #dc2626; border-color: #dc2626; }
.filters, .stats, .table-wrap { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; margin-bottom: 18px; }
.filters { display: flex; flex-wrap: wrap; align-items: end; gap: 12px; }
.filters label { display: grid; gap: 6px; color: #374151; font-size: 13px; }
input, select { min-width: 150px; border: 1px solid #d1d5db; border-radius: 8px; padding: 8px 10px; }
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.stats div { background: #f9fafb; border-radius: 8px; padding: 14px; }
.stats span { color: #6b7280; display: block; margin-bottom: 8px; }
.stats strong { font-size: 24px; }
.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 12px 10px; border-bottom: 1px solid #f3f4f6; text-align: left; white-space: nowrap; }
th { color: #4b5563; font-size: 13px; background: #f9fafb; }
.empty { padding: 36px 0; color: #6b7280; text-align: center; }
.empty.small { padding: 16px 0; }
.status { display: inline-flex; padding: 3px 8px; border-radius: 999px; font-size: 12px; background: #e5e7eb; }
.status.completed { color: #166534; background: #dcfce7; }
.status.pending { color: #92400e; background: #fef3c7; }
.status.failed { color: #991b1b; background: #fee2e2; }
.modal { position: fixed; inset: 0; background: rgba(15, 23, 42, .45); display: grid; place-items: center; z-index: 1000; padding: 20px; }
.dialog { width: min(760px, 100%); max-height: 86vh; overflow: auto; background: #fff; border-radius: 8px; padding: 20px; }
.dialog-header { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.detail-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 18px; }
.detail-grid div { background: #f9fafb; border-radius: 8px; padding: 12px; }
.detail-grid span { display: block; color: #6b7280; margin-bottom: 6px; }
.keys { display: grid; gap: 10px; }
.key-row { display: flex; justify-content: space-between; gap: 12px; align-items: center; padding: 10px; background: #f9fafb; border-radius: 8px; }
.key-row code { word-break: break-all; white-space: normal; }
.dialog-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 18px; }
@media (max-width: 760px) { .stats, .detail-grid { grid-template-columns: 1fr; } .page-header { flex-direction: column; } }
</style>