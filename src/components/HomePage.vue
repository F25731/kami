<template>
  <main class="home-page">
    <header class="topbar">
      <div class="brand">
        <img src="../assets/icon.png" alt="授权中心" />
        <span>云逸卡密授权中心</span>
      </div>
      <div class="actions">
        <button type="button" class="ghost" @click="$emit('go-online-unbind')">在线解绑</button>
        <button type="button" class="primary" @click="$emit('show-login')">登录后台</button>
      </div>
    </header>

    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">Private License Center</p>
        <h1>多项目卡密授权中心</h1>
        <p class="subtitle">用于项目授权、卡密发放、外部商城发卡记录和接口密钥管理。</p>
        <div class="hero-actions">
          <button type="button" class="primary large" @click="$emit('show-login')">进入管理后台</button>
          <button type="button" class="ghost large" @click="$emit('go-online-unbind')">用户在线解绑</button>
        </div>
      </div>
      <div class="status-panel">
        <div class="panel-title">当前入口</div>
        <div class="status-item"><span>后台管理</span><strong>项目 / 卡密 / API</strong></div>
        <div class="status-item"><span>订单模块</span><strong>发卡记录</strong></div>
        <div class="status-item"><span>前台商城</span><strong>已关闭</strong></div>
      </div>
    </section>
  </main>
</template>

<script setup>
defineEmits(['show-login', 'go-online-unbind'])
</script>

<style scoped>
.home-page { min-height: 100vh; background: #f6f8fb; color: #111827; }
.topbar { height: 64px; display: flex; align-items: center; justify-content: space-between; padding: 0 32px; background: #fff; border-bottom: 1px solid #e5e7eb; }
.brand, .actions, .hero-actions { display: flex; align-items: center; gap: 12px; }
.brand img { width: 32px; height: 32px; }
.brand span { font-weight: 700; font-size: 18px; }
.hero { max-width: 1120px; margin: 0 auto; padding: 96px 32px; display: grid; grid-template-columns: minmax(0, 1.3fr) 360px; gap: 48px; align-items: center; }
.eyebrow { margin: 0 0 12px; color: #2563eb; font-size: 13px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
h1 { margin: 0; font-size: 48px; line-height: 1.08; }
.subtitle { margin: 20px 0 28px; max-width: 640px; color: #4b5563; font-size: 18px; line-height: 1.8; }
button { border: 0; border-radius: 8px; padding: 10px 16px; cursor: pointer; font-weight: 600; }
button.primary { color: #fff; background: #2563eb; }
button.ghost { color: #111827; background: #eef2f7; }
button.large { padding: 13px 20px; font-size: 15px; }
.status-panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 22px; box-shadow: 0 12px 32px rgba(15, 23, 42, .08); }
.panel-title { font-weight: 700; margin-bottom: 14px; }
.status-item { display: flex; justify-content: space-between; gap: 16px; padding: 14px 0; border-top: 1px solid #f3f4f6; }
.status-item span { color: #6b7280; }
.status-item strong { text-align: right; }
@media (max-width: 820px) { .topbar { padding: 0 16px; } .brand span { font-size: 16px; } .hero { grid-template-columns: 1fr; padding: 56px 20px; } h1 { font-size: 34px; } .actions { gap: 8px; } }
</style>