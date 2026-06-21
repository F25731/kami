<template>
  <main class="user-page">
    <header class="user-header">
      <div class="brand">
        <img src="../assets/icon.png" alt="授权中心" />
        <div>
          <strong>云逸卡密授权中心</strong>
          <span>我的授权与发卡记录</span>
        </div>
      </div>
      <button type="button" class="logout" @click="$emit('logout')">退出登录</button>
    </header>

    <section class="summary">
      <div><span>登录账号</span><strong>{{ userInfo?.username || '-' }}</strong></div>
      <div><span>发卡记录</span><strong>{{ records.length }}</strong></div>
      <div><span>卡密数量</span><strong>{{ cards.length }}</strong></div>
    </section>

    <section class="content-grid">
      <div class="panel">
        <div class="panel-header">
          <h2>我的卡密</h2>
          <button type="button" @click="loadData" :disabled="loading">刷新</button>
        </div>
        <div v-if="loading" class="empty">正在加载...</div>
        <div v-else-if="cards.length === 0" class="empty">暂无卡密，请联系管理员或外部商城发卡。</div>
        <div v-else class="key-list">
          <div v-for="card in cards" :key="card" class="key-row">
            <code>{{ card }}</code>
            <button type="button" @click="copy(card)">复制</button>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-header"><h2>发卡记录</h2></div>
        <div v-if="records.length === 0" class="empty">暂无发卡记录。</div>
        <div v-else class="record-list">
          <article v-for="record in records" :key="record.order_id" class="record-card">
            <div class="record-top">
              <strong>{{ record.order_id }}</strong>
              <span :class="['status', record.status]">{{ statusText(record.status) }}</span>
            </div>
            <p>{{ cardTypeText(record.card_type) }} / {{ record.card_spec }} / {{ record.quantity }} 张</p>
            <small>{{ record.purchase_time || record.create_time || '-' }}</small>
          </article>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { orderApi } from '../services/api.js'

const props = defineProps({ userInfo: Object })
defineEmits(['logout'])

const loading = ref(false)
const records = ref([])

const cards = computed(() => records.value.flatMap(record => {
  if (Array.isArray(record.card_keys)) return record.card_keys
  if (typeof record.card_keys === 'string' && record.card_keys.trim()) return record.card_keys.split(',')
  return []
}).filter(Boolean))

const loadData = async () => {
  if (!props.userInfo?.id) return
  loading.value = true
  try {
    const data = await orderApi.getOrders(props.userInfo.id)
    records.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error('获取发卡记录失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const copy = async (text) => {
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

const statusText = (status) => ({ completed: '已发卡', pending: '待处理', failed: '失败', refunded: '已退回' }[status] || status || '未知')
const cardTypeText = (type) => type === 'time' ? '时间卡' : type === 'count' ? '次数卡' : '卡密'

onMounted(loadData)
</script>

<style scoped>
.user-page { min-height: 100vh; background: #f6f8fb; padding: 24px; color: #111827; }
.user-header, .summary, .content-grid { max-width: 1180px; margin: 0 auto; }
.user-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.brand { display: flex; align-items: center; gap: 12px; }
.brand img { width: 36px; height: 36px; }
.brand div { display: flex; flex-direction: column; gap: 4px; }
.brand span { color: #6b7280; font-size: 13px; }
.logout, .panel-header button, .key-row button { border: 0; border-radius: 8px; padding: 9px 14px; cursor: pointer; background: #111827; color: #fff; }
.summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
.summary > div, .panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 18px; }
.summary span { display: block; color: #6b7280; margin-bottom: 8px; }
.summary strong { font-size: 24px; }
.content-grid { display: grid; grid-template-columns: 1.1fr .9fr; gap: 20px; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
h2 { margin: 0; font-size: 18px; }
.empty { padding: 36px 0; color: #6b7280; text-align: center; }
.key-list, .record-list { display: grid; gap: 10px; }
.key-row { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px; background: #f9fafb; border-radius: 8px; }
.key-row code { word-break: break-all; }
.record-card { padding: 14px; background: #f9fafb; border-radius: 8px; }
.record-top { display: flex; justify-content: space-between; gap: 12px; }
.record-card p { margin: 10px 0; color: #374151; }
.record-card small { color: #6b7280; }
.status { padding: 3px 8px; border-radius: 999px; font-size: 12px; background: #e5e7eb; }
.status.completed { color: #166534; background: #dcfce7; }
.status.pending { color: #92400e; background: #fef3c7; }
.status.failed { color: #991b1b; background: #fee2e2; }
@media (max-width: 900px) { .summary, .content-grid { grid-template-columns: 1fr; } .user-header { align-items: flex-start; gap: 12px; } }
</style>