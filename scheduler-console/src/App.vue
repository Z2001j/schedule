<template>
  <div class="app">
    <header class="header">
      <div class="brand">
        <span class="logo">⏱</span>
        <div>
          <div class="title">定时任务调度中心</div>
          <div class="subtitle">调度集中 · 执行就地 · 改周期无需重启业务服务</div>
        </div>
      </div>
    </header>
    <main class="main">
      <ScheduledConsole />
    </main>
  </div>
</template>

<script setup>
import ScheduledConsole from '@/views/ScheduledConsole.vue'
</script>

<style>
* { box-sizing: border-box; }
body { margin: 0; background: #f5f6f8; font-family: -apple-system, "Segoe UI", "Microsoft YaHei", sans-serif; }
.header { background: #fff; border-bottom: 1px solid #e6e8eb; padding: 14px 24px; }
.brand { display: flex; align-items: center; gap: 12px; }
.logo { font-size: 26px; }
.title { font-size: 17px; font-weight: 700; color: #1f2329; }
.subtitle { font-size: 12px; color: #8a9099; margin-top: 2px; }
.main { padding: 18px 24px; }
</style>