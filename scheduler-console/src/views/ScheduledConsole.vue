<template>
  <el-card shadow="never">
    <el-tabs v-model="tab">
      <el-tab-pane label="任务管理" name="task">
        <TaskList v-if="tab==='task'" />
      </el-tab-pane>
      <el-tab-pane label="执行器监控" name="executor">
        <ExecutorMonitor v-if="tab==='executor'" />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import TaskList from '@/components/TaskList.vue'
import ExecutorMonitor from '@/components/ExecutorMonitor.vue'
const tab = ref('task')
</script>