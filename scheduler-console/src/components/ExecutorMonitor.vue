<template>
  <div>
    <div class="toolbar">
      <span class="hint">同服务多实例由调度中心统一路由协调；心跳超时自动下线</span>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="appName" label="服务" width="160" />
      <el-table-column prop="address" label="执行器地址" min-width="220">
        <template #default="{row}"><code>{{ row.address }}</code></template>
      </el-table-column>
      <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status==='ONLINE'?'success':'info'" size="small">{{ row.status==='ONLINE'?'在线':'下线' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listExecutors } from '@/api/scheduler'

const rows = ref([]), loading = ref(false)
async function load() {
  loading.value = true
  try { rows.value = await listExecutors() }
  catch (e) { ElMessage.error(e.message) }
  finally { loading.value = false }
}
let timer
onMounted(() => { load(); timer = setInterval(load, 10000) })
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px }
.hint { font-size:12px; color:#8a9099 }
</style>