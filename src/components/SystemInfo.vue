<template>
  <div class="system-info-page">
    <div class="page-header">
      <h2>系统信息</h2>
      <p>查看当前授权中心的本地运行信息。</p>
    </div>
    <div class="info-grid">
      <div><span>系统名称</span><strong>云逸卡密授权中心</strong></div>
      <div><span>用途</span><strong>多项目卡密授权管理</strong></div>
      <div><span>前台商城</span><strong>已关闭</strong></div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.system-info-page { padding: 1rem; color: #111827; }
.page-header { margin-bottom: 20px; }
h2 { margin: 0 0 8px; font-size: 20px; }
p { margin: 0; color: #6b7280; }
.info-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.info-grid div { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
.info-grid span { display: block; color: #6b7280; margin-bottom: 8px; }
.info-grid strong { font-size: 18px; }
@media (max-width: 760px) { .info-grid { grid-template-columns: 1fr; } }
</style>