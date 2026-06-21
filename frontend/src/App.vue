<template>
  <router-view />
</template>

<script setup>
</script>

<style>

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}


.el-button {
  border-radius: 8px;
}

.el-input__inner, .el-textarea__inner {
  border-radius: 8px;
}

.el-dialog {
  border-radius: 16px;
}

.el-message-box {
  border-radius: 16px;
}


.glass-dialog .el-dialog {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
}


::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}


.app-layout {
  background: linear-gradient(135deg, #e0f2fe 0%, #eef2ff 50%, #f8fafc 100%) !important;
}

.glass-panel,
.glass-card,
.glass-section {
  background: rgba(255, 255, 255, 0.94) !important;
  border: 1px solid rgba(15, 23, 42, 0.12) !important;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.1) !important;
}

.page-header h1,
.page-header h2,
.page-header h3,
.glass-panel h1,
.glass-panel h2,
.glass-panel h3,
.glass-card h1,
.glass-card h2,
.glass-card h3,
.glass-panel p,
.glass-card p,
.settings-page,
.dashboard-page,
.cards-page,
.api-keys-page,
.orders-page,
.logs-page,
.packages-page,
.entitlements-page,
.api-docs-page {
  color: #111827 !important;
}

.page-header p,
.form-tip,
.project-token,
.project-code,
.stat-trend,
.metric-label,
.endpoint-desc,
.empty-hint {
  color: #4b5563 !important;
}

.glass-btn {
  background: #ffffff !important;
  border-color: #d1d5db !important;
  color: #111827 !important;
}

.glass-btn:hover {
  background: #f3f4f6 !important;
}

.nav-item.active,
.nav-item.router-link-active {
  background: #2563eb !important;
  color: #ffffff !important;
}

.nav-item.active *,
.nav-item.router-link-active * {
  color: #ffffff !important;
}

.el-table,
.el-table th,
.el-table tr,
.el-table td {
  color: #111827 !important;
}

code,
pre {
  color: #111827 !important;
  background: #f3f4f6 !important;
}
</style>
