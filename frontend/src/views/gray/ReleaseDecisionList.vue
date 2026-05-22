<script setup lang="ts">
import BackendPlaceholder from '@/components/BackendPlaceholder.vue'
</script>

<template>
  <BackendPlaceholder
    title="放行决策"
    description="原有前端写死的放行/冻结/拒收记录已全部删除。当前页面不再渲染任何演示决策数据，等待后端确认放行决策接口和字段后再接入。"
    :endpoints="['/api/batches/{batchId}', '/api/qc-records', '/api/exception-records']"
  />
</template>
