<script setup lang="ts">
import BackendPlaceholder from '@/components/BackendPlaceholder.vue'
</script>

<template>
  <BackendPlaceholder
    title="布料瑕疵种类"
    description="原有缺陷类别、微调数据集和静态缺陷样本均为演示内容，现已全部移除。当前页面不再预置任何缺陷记录，等待后端与真实模型流程确认后再接入。"
    :endpoints="['/api/qc-items', '/api/qc-records', '/api/inspection-data']"
  />
</template>
