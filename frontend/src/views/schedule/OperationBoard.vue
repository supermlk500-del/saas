<script setup lang="ts">
const boardData = [
  {
    line: '裁剪线-01',
    planNo: 'PLAN-20260401',
    batchNo: 'GB-20260401-01',
    progress: '75%',
    status: '进行中',
  },
  {
    line: '验布线-02',
    planNo: 'PLAN-20260402',
    batchNo: 'GB-20260401-02',
    progress: '40%',
    status: '进行中',
  },
  {
    line: '缝制线-03',
    planNo: 'PLAN-20260403',
    batchNo: 'GB-20260403-01',
    progress: '100%',
    status: '已完成',
  },
]
</script>

<template>
  <a-space direction="vertical" size="large" style="width: 100%">
    <a-typography-title :level="2">工序排程看板</a-typography-title>
    <a-row :gutter="16">
      <a-col :span="8" v-for="item in boardData" :key="item.planNo">
        <a-card :title="item.line" bordered>
          <a-space direction="vertical">
            <div>计划号：{{ item.planNo }}</div>
            <div>批次号：{{ item.batchNo }}</div>
            <div>进度：{{ item.progress }}</div>
            <a-tag :color="item.status === '已完成' ? 'green' : 'blue'">{{ item.status }}</a-tag>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </a-space>
</template>
