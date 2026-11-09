<template>
  <div>
    <div class="toolbar">
      <el-input v-model="q.keyword" placeholder="搜索名称/ID" clearable style="width:220px" />
      <el-select v-model="q.appName" placeholder="全部服务" clearable style="width:160px">
        <el-option v-for="a in apps" :key="a" :label="a" :value="a" />
      </el-select>
      <el-select v-model="q.enabled" placeholder="全部状态" clearable style="width:140px">
        <el-option label="运行中" :value="true" />
        <el-option label="已停止" :value="false" />
      </el-select>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="filtered" v-loading="loading" stripe>
      <el-table-column label="任务名称" min-width="180">
        <template #default="{row}">
          <div style="font-weight:600">{{ row.taskName }}</div>
          <div class="tid">{{ row.taskId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="所属服务" width="150">
        <template #default="{row}"><el-tag color="#f3eefe" style="color:#7c3aed;border:none">{{ row.appName }}</el-tag></template>
      </el-table-column>
      <el-table-column label="周期" min-width="140">
        <template #default="{row}"><code>{{ periodText(row) }}</code></template>
      </el-table-column>
      <el-table-column label="路由策略" width="120">
        <template #default="{row}"><el-tag color="#fff4e6" style="color:#d97706;border:none">{{ routeLabel[row.routeStrategy] }}</el-tag></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{row}"><el-tag :type="row.enabled?'success':'info'" size="small">{{ row.enabled?'运行中':'已停止' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="在线实例" width="90">
        <template #default="{row}"><span :style="{color: row.onlineInstances>0?'#16a34a':'#dc2626'}">{{ row.onlineInstances }}</span></template>
      </el-table-column>
      <el-table-column prop="nextExecTime" label="下次执行" min-width="160">
        <template #default="{row}">{{ row.nextExecTime || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{row}">
          <el-button link type="primary" @click="edit(row)">编辑</el-button>
          <el-button link :type="row.enabled?'danger':'success'" @click="toggle(row)">{{ row.enabled?'停止':'启动' }}</el-button>
          <el-button link type="primary" @click="trigger(row)">执行一次</el-button>
          <el-button link @click="showLog(row)">日志</el-button>
        </template>
      </el-table-column>
    </el-table>
    <ScheduleEditDialog v-model="dlg" :task="cur" @saved="load" />
    <LogDrawer v-model="logVisible" :task="cur" />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTasks, startTask, stopTask, triggerTask } from '@/api/scheduler'
import ScheduleEditDialog from './ScheduleEditDialog.vue'
import LogDrawer from './LogDrawer.vue'

const allTasks = ref([]), loading = ref(false), dlg = ref(false), logVisible = ref(false), cur = ref(null)
const q = reactive({ keyword:'', appName:'', enabled:'' })
const apps = computed(() => [...new Set(allTasks.value.map(t => t.appName))])
const routeLabel = { FIRST:'第一个', LAST:'最后一个', ROUND:'轮询', RANDOM:'随机', CONSISTENT_HASH:'一致性HASH', FAILOVER:'故障转移', SHARDING_BROADCAST:'分片广播' }
const periodText = t => t.scheduleType==='CRON' ? t.cronExpression : t.scheduleType==='FIXED_RATE' ? `每 ${t.fixedRate/1000} 秒` : `结束后 ${t.fixedDelay/1000} 秒`

const filtered = computed(() => allTasks.value.filter(t => {
  if (q.keyword && !(`${t.taskName} ${t.taskId}`.toLowerCase().includes(q.keyword.toLowerCase()))) return false
  if (q.appName && t.appName !== q.appName) return false
  if (q.enabled !== '' && t.enabled !== q.enabled) return false
  return true
}))

async function load() {
  loading.value = true
  try { allTasks.value = await listTasks() }
  catch (e) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function edit(row) { cur.value = row; dlg.value = true }
function showLog(row) { cur.value = row; logVisible.value = true }

async function toggle(row) {
  try {
    if (row.enabled) {
      await ElMessageBox.confirm(`确认停止任务「${row.taskName}」？调度中心将不再触发。`, '提示', { type:'warning' })
      await stopTask(row.taskId, row.version)
      ElMessage.success('任务已停止')
    } else {
      await startTask(row.taskId, row.version)
      ElMessage.success('任务已启动')
    }
    load()
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

async function trigger(row) {
  try { await triggerTask(row.taskId); ElMessage.success(`已触发「${row.taskName}」`); setTimeout(load, 800) }
  catch (e) { ElMessage.error(e.message) }
}

let timer
onMounted(() => { load(); timer = setInterval(load, 10000) })
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.toolbar { display:flex; gap:10px; margin-bottom:12px }
.tid { color:#8a9099; font-size:11px; font-family:monospace }
</style>