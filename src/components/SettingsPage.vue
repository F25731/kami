<template>
  <div class="settings-page">
    <div class="settings-header">
      <h2>基础设置</h2>
      <p>只保留私有授权中心需要的名称、地址和数据库备份设置。</p>
    </div>

    <section class="settings-section">
      <h3>系统信息</h3>
      <div class="settings-grid">
        <label>系统名称<input v-model="settings.systemName" placeholder="云逸授权中心" /></label>
        <label>站点地址<input v-model="settings.site_url" placeholder="https://example.com" /></label>
        <label class="wide">系统描述<textarea v-model="settings.systemDescription" rows="3" /></label>
        <label>默认语言
          <select v-model="settings.defaultLanguage">
            <option value="zh-CN">简体中文</option>
            <option value="en-US">English</option>
          </select>
        </label>
        <label>时区
          <select v-model="settings.timezone">
            <option value="Asia/Shanghai">Asia/Shanghai</option>
            <option value="UTC">UTC</option>
          </select>
        </label>
      </div>
    </section>

    <section class="settings-section">
      <h3>数据库备份</h3>
      <div class="settings-grid">
        <label class="check"><input type="checkbox" v-model="settings.autoBackup" /> 自动备份</label>
        <label>备份频率
          <select v-model="settings.backupFrequency" :disabled="!settings.autoBackup">
            <option value="daily">每日</option>
            <option value="weekly">每周</option>
            <option value="monthly">每月</option>
          </select>
        </label>
        <label>保留数量<input type="number" min="1" max="30" v-model="settings.backupRetention" :disabled="!settings.autoBackup" /></label>
        <label class="check"><input type="checkbox" v-model="settings.dataCompression" /> 数据压缩</label>
      </div>
    </section>

    <div class="actions">
      <button type="button" class="primary" @click="saveSettings" :disabled="saving">{{ saving ? '保存中...' : '保存设置' }}</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { settingsApi } from '../services/api.js'

const emit = defineEmits(['save-settings'])

const saving = ref(false)
const settings = reactive({
  systemName: '云逸授权中心',
  systemDescription: '私有化多项目卡密授权中心',
  site_url: '',
  defaultLanguage: 'zh-CN',
  timezone: 'Asia/Shanghai',
  autoBackup: false,
  backupFrequency: 'daily',
  backupRetention: 7,
  dataCompression: true
})

const loadSettings = async () => {
  try {
    const res = await settingsApi.getAllSettings()
    if (res.success && res.data) {
      Object.assign(settings, res.data)
      settings.autoBackup = String(settings.autoBackup) === 'true' || settings.autoBackup === true
      settings.dataCompression = String(settings.dataCompression) === 'true' || settings.dataCompression === true
    }
  } catch (error) {
    ElMessage.warning('读取设置失败，将使用默认值')
  }
}

const saveSettings = async () => {
  saving.value = true
  try {
    const payload = { ...settings }
    ;['autoBackup', 'dataCompression'].forEach(key => {
      payload[key] = String(payload[key])
    })
    const res = await settingsApi.saveSettings(payload)
    if (res.success) {
      ElMessage.success('设置已保存')
      emit('save-settings', payload)
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<style scoped>
.settings-page { padding: 1rem; color: #111827; }
.settings-header { margin-bottom: 22px; }
h2 { margin: 0 0 8px; font-size: 20px; }
p { margin: 0; color: #6b7280; }
.settings-section { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 18px; margin-bottom: 18px; }
h3 { margin: 0 0 16px; font-size: 16px; }
.settings-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
label { display: grid; gap: 6px; color: #374151; font-size: 14px; }
label.wide { grid-column: 1 / -1; }
label.check { display: flex; align-items: center; gap: 8px; }
input, select, textarea { width: 100%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 8px; padding: 9px 10px; font: inherit; }
input[type="checkbox"] { width: auto; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
button { border: 1px solid #d1d5db; border-radius: 8px; background: #fff; padding: 9px 14px; cursor: pointer; }
button.primary { color: #fff; border-color: #2563eb; background: #2563eb; }
@media (max-width: 760px) { .settings-grid { grid-template-columns: 1fr; } .actions { justify-content: flex-start; } }
</style>
