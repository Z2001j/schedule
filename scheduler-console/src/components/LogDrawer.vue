<template>
  <el-drawer v-model="visible" :title="`执行日志 · ${task?.taskName||''}`" size="480px">
    <div class="bar">
      <span class="tid">{{ task?.taskId }}</span>
      <el-button size="small" @click="refresh">刷新</el-button>
    </div>
    <el-empty v-if="!logs.length" description="暂无日志" />
    <div v-for="log in logs" :key="log.id" class="log">
      <div class="top">
        <b>{{ log.triggerTime }}</b>
        <span :class="statusClass(log)">{{ statusText(log) }}</span>
      </div>
      <div class="meta">
        触发：{{ log.triggerType==='MANUAL'?'手动':'调度' }} · 路由：{{ log.routeStrategy }} · 结果：{{ log.triggerResult }}<br/>
        执行器：{{ log.executorAddress || '—' }} · 分片：{{ log.shardIndex ?? 0 }}/{{ log.shardTotal ?? 1 }} · 耗时：{{ log.costMs ?? '—' }}ms
      </div>
      <div v-if="log.errorMsg" class="fail">{{ log.errorMsg }}</div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getLogs } from '@/api/scheduler'

const props = defineProps({ modelValue: Boolean, task: Object })
const emit = defineEmits(['update:modelValue'])
const visible = ref(false), logs = ref([])

async function refresh() {
  if (!props.task) return
  logs.value = await getLogs(props.task.taskId, { limit: 50 })
}
const statusText = log => log.success === true ? '✓ 成功' : log.success === false ? '✗ 失败' : '… 执行中'
const statusClass = log => log.success === true ? 'ok' : log.success === false ? 'fail' : 'pending'

watch(() => props.modelValue, async v => { visible.value = v; if (v) await refresh() })
watch(visible, v => emit('update:modelValue', v))
</script>

<style scoped>
.bar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px }
.tid { font-size:11px; color:#8a9099; font-family:monospace }
.log { border:1px solid #e6e8eb; border-radius:8px; padding:10px 12px; margin-bottom:10px; font-size:12px }
.top { display:flex; justify-content:space-between; margin-bottom:6px }
.meta { color:#8a9099; line-height:1.7 }
.ok { color:#16a34a } .fail { color:#dc2626 } .pending { color:#d97706 }
</style>