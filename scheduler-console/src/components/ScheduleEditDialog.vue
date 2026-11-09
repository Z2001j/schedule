<template>
  <el-dialog v-model="visible" title="编辑任务" width="540px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="任务">
        <div style="font-weight:600">{{ task?.taskName }}（{{ task?.appName }}）</div>
        <div style="color:#8a9099;font-size:12px">{{ task?.taskId }}</div>
      </el-form-item>
      <el-form-item label="调度类型" required>
        <el-radio-group v-model="form.type">
          <el-radio-button value="CRON">Cron</el-radio-button>
          <el-radio-button value="FIXED_RATE">固定频率</el-radio-button>
          <el-radio-button value="FIXED_DELAY">固定延迟</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.type==='CRON'" label="Cron 表达式" required>
        <el-input v-model="form.cron" placeholder="如 0 */5 * * * ?" />
      </el-form-item>
      <el-form-item v-if="form.type==='FIXED_RATE'" label="固定频率(ms)" required>
        <el-input-number v-model="form.fixedRate" :min="1000" :step="1000" />
      </el-form-item>
      <el-form-item v-if="form.type==='FIXED_DELAY'" label="固定延迟(ms)" required>
        <el-input-number v-model="form.fixedDelay" :min="1000" :step="1000" />
      </el-form-item>
      <el-form-item label="路由策略">
        <el-select v-model="form.routeStrategy" style="width:100%">
          <el-option v-for="r in routes" :key="r.v" :label="r.l" :value="r.v" />
        </el-select>
        <div v-if="form.routeStrategy==='SHARDING_BROADCAST'" class="help">将触发该服务所有在线实例并行处理</div>
      </el-form-item>
      <el-form-item label="阻塞策略">
        <el-select v-model="form.blockStrategy" style="width:100%">
          <el-option label="串行排队" value="SERIAL" />
          <el-option label="丢弃本次" value="DISCARD" />
          <el-option label="覆盖前次" value="OVERRIDE" />
        </el-select>
      </el-form-item>
      <el-form-item label="初始延迟(ms)">
        <el-input-number v-model="form.initialDelay" :min="0" :step="1000" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible=false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存并生效</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { updateSchedule, updateRoute } from '@/api/scheduler'

const props = defineProps({ modelValue: Boolean, task: Object })
const emit = defineEmits(['update:modelValue', 'saved'])
const visible = ref(false), saving = ref(false)
const routes = [
  { v:'FIRST', l:'第一个' },{ v:'LAST', l:'最后一个' },{ v:'ROUND', l:'轮询' },
  { v:'RANDOM', l:'随机' },{ v:'CONSISTENT_HASH', l:'一致性HASH' },
  { v:'FAILOVER', l:'故障转移' },{ v:'SHARDING_BROADCAST', l:'分片广播' }
]
const form = reactive({ type:'CRON', cron:'', fixedRate:60000, fixedDelay:60000, initialDelay:0, routeStrategy:'FAILOVER', blockStrategy:'SERIAL' })

watch(() => props.modelValue, v => { visible.value = v; if (v) fill() })
watch(visible, v => emit('update:modelValue', v))

function fill() {
  const t = props.task || {}
  Object.assign(form, {
    type: t.scheduleType || 'CRON', cron: t.cronExpression || '',
    fixedRate: t.fixedRate || 60000, fixedDelay: t.fixedDelay || 60000,
    initialDelay: t.initialDelay || 0, routeStrategy: t.routeStrategy || 'FAILOVER',
    blockStrategy: t.blockStrategy || 'SERIAL'
  })
}

async function save() {
  if (form.type === 'CRON' && !form.cron?.trim()) return ElMessage.error('请填写 Cron 表达式')
  saving.value = true
  try {
    const updated = await updateSchedule(props.task.taskId, {
      type: form.type,
      cron: form.type === 'CRON' ? form.cron.trim() : null,
      fixedRate: form.type === 'FIXED_RATE' ? form.fixedRate : null,
      fixedDelay: form.type === 'FIXED_DELAY' ? form.fixedDelay : null,
      initialDelay: form.initialDelay
    }, props.task.version)
    // 周期更新后 version 自增，路由更新须用新 version
    await updateRoute(props.task.taskId, {
      routeStrategy: form.routeStrategy, blockStrategy: form.blockStrategy, retryCount: props.task.retryCount || 0
    }, updated.version)
    ElMessage.success('已保存并即时生效（业务服务无感）')
    emit('saved'); visible.value = false
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>.help { font-size:12px; color:#d97706; margin-top:4px }</style>