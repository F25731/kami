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
  background: linear-gradient(135deg, #b569f2 0%, #f47be6 45%, #88a8ff 100%);
  background-attachment: fixed;
}

.el-button {
  border-radius: 10px;
}

.el-input__inner, .el-textarea__inner {
  border-radius: 10px;
}

.el-dialog {
  border-radius: 20px;
  overflow: hidden;
}

.el-message-box {
  border-radius: 20px;
  overflow: hidden;
}

.glass-dialog .el-dialog {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

.app-layout {
  background: transparent !important;
}

.glass-panel,
.glass-card,
.glass-section {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(30px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(30px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1) !important;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.glass-panel:hover,
.glass-card:hover {
  background: rgba(255, 255, 255, 0.2) !important;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15) !important;
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
.api-docs-page,
.glass-panel *,
.glass-card *,
.glass-section * {
  color: #000000 !important;
}

.page-header p,
.form-tip,
.stat-trend,
.metric-label,
.endpoint-desc,
.empty-hint {
  color: #1a1a1a !important;
}

.project-token,
.project-code {
  color: #2a2a2a !important;
}

.glass-btn {
  background: rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(10px) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: #000000 !important;
  transition: all 0.3s ease;
}

.glass-btn:hover {
  background: rgba(255, 255, 255, 0.3) !important;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
}

.glass-btn .el-icon,
.el-button .el-icon,
.el-button i[class^="el-icon-"],
.el-button i[class*=" el-icon-"],
i[class^="el-icon-"],
i[class*=" el-icon-"] {
  color: #000000 !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1em;
  min-height: 1em;
  font-style: normal;
  font-weight: 700;
  line-height: 1;
}

.el-button.is-circle,
.el-button.is-text {
  background: rgba(255, 255, 255, 0.28) !important;
  border: 1px solid rgba(255, 255, 255, 0.42) !important;
  color: #000000 !important;
}

.el-icon-plus::before,
.el-icon-folder-add::before { content: "+"; }
.el-icon-arrow-left::before { content: "<"; }
.el-icon-arrow-right::before { content: ">"; }
.el-icon-arrow-down::before { content: "v"; }
.el-icon-check::before,
.el-icon-success::before { content: "OK"; font-size: 11px; }
.el-icon-copy-document::before { content: "C"; }
.el-icon-link::before { content: "L"; }
.el-icon-refresh::before { content: "R"; }
.el-icon-delete::before { content: "X"; }
.el-icon-edit::before { content: "E"; }
.el-icon-key::before { content: "K"; }
.el-icon-setting::before { content: "S"; }
.el-icon-bell::before { content: "N"; }
.el-icon-switch-button::before { content: "Q"; }
.el-icon-folder-opened::before { content: "F"; }
.el-icon-data-line::before { content: "D"; }
.el-icon-tickets::before { content: "T"; }
.el-icon-box::before { content: "B"; }
.el-icon-document::before { content: "A"; }
.el-icon-list::before { content: "O"; }
.el-icon-user::before { content: "U"; }
.el-icon-warning::before,
.el-icon-warning-outline::before { content: "!"; }
.el-icon-search::before { content: ""; }
.el-icon-download::before { content: "DL"; font-size: 11px; }
.el-icon-timer::before,
.el-icon-time::before { content: "T"; }
.el-icon-top::before { content: "^"; }
.el-icon-bottom::before { content: "v"; }
.el-icon-money::before { content: "$"; }

.nav-item.active,
.nav-item.router-link-active {
  background: rgba(255, 255, 255, 0.3) !important;
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  color: #000000 !important;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2) !important;
}

.nav-item.active *,
.nav-item.router-link-active * {
  color: #000000 !important;
}

.el-table,
.el-table th,
.el-table tr,
.el-table td {
  color: #000000 !important;
  background: transparent !important;
}

.el-table th {
  background: rgba(255, 255, 255, 0.1) !important;
}

.el-table tr:hover > td {
  background: rgba(255, 255, 255, 0.15) !important;
}

code,
pre {
  color: #000000 !important;
  background: rgba(255, 255, 255, 0.2) !important;
  border-radius: 8px;
  padding: 2px 6px;
}

.el-input__inner,
.el-textarea__inner,
.el-select .el-input__inner {
  background: rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: #000000 !important;
}

.el-input__inner::placeholder,
.el-textarea__inner::placeholder {
  color: #333333 !important;
}

/* 页面容器样式 */
.dashboard,
.settings-page,
.cards-page,
.api-keys-page,
.api-docs-page,
.orders-page,
.packages-page,
.logs-page,
.entitlements-page {
  padding: 0;
}

/* 统计卡片 */
.stat-card,
.chart-card,
.usage-card,
.recent-orders,
.quick-actions-card {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(30px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(30px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1) !important;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.stat-card:hover,
.chart-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15) !important;
}

/* 文字颜色 */
.stat-label,
.stat-value,
.stat-change,
.chart-header h3,
.usage-label,
.usage-value,
.latency-metrics span,
.view-all-link {
  color: #000000 !important;
}

/* 页面标题 */
.page-header {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(30px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1) !important;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

.page-header h2,
.page-header h3 {
  color: #000000 !important;
  margin: 0 0 8px 0;
}

/* 设置相关 */
.settings-section {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(30px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1) !important;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
}

.settings-section h3 {
  color: #000000 !important;
  margin-bottom: 20px;
}

.form-tip {
  color: #1a1a1a !important;
  font-size: 13px;
  margin-top: 5px;
}

.token-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.token-field code {
  background: rgba(255, 255, 255, 0.25) !important;
  padding: 8px 12px;
  border-radius: 8px;
  color: #000000 !important;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* 表格样式优化 */
.el-table {
  background: rgba(255, 255, 255, 0.1) !important;
  border-radius: 12px;
  overflow: hidden;
}

.el-table::before {
  display: none;
}

.el-table th.el-table__cell {
  background: rgba(255, 255, 255, 0.2) !important;
  color: #000000 !important;
  font-weight: 600;
  border: none !important;
}

.el-table td.el-table__cell {
  border: none !important;
  color: #000000 !important;
}

.el-table--enable-row-hover .el-table__body tr:hover > td {
  background: rgba(255, 255, 255, 0.25) !important;
}

/* 标签样式 */
.el-tag {
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: #000000 !important;
  backdrop-filter: blur(10px);
}

/* 按钮样式 */
.el-button--primary {
  background: rgba(255, 255, 255, 0.25) !important;
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  color: #000000 !important;
  backdrop-filter: blur(10px);
}

.el-button--primary:hover {
  background: rgba(255, 255, 255, 0.35) !important;
  transform: translateY(-2px);
}

/* 选择器和下拉框 */
.el-select-dropdown {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(30px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
}

.el-select-dropdown__item {
  color: #000000 !important;
}

.el-select-dropdown__item:hover {
  background: rgba(255, 255, 255, 0.2) !important;
}

/* Radio 和 Checkbox */
.el-radio__label,
.el-checkbox__label {
  color: #000000 !important;
}

/* 标签页 */
.el-tabs__item {
  color: #000000 !important;
}

.el-tabs__item.is-active {
  color: #000000 !important;
  font-weight: 600;
}

/* 面包屑 */
.el-breadcrumb__inner,
.el-breadcrumb__inner a {
  color: #000000 !important;
}

/* 空状态 */
.el-empty__description p {
  color: #000000 !important;
}

/* 对话框 */
.el-dialog__header,
.el-dialog__body {
  color: #000000 !important;
}

.el-dialog__title {
  color: #000000 !important;
}

/* 消息框 */
.el-message {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(30px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: #000000 !important;
}

.el-message__content {
  color: #000000 !important;
}
</style>
