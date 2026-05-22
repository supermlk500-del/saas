<script setup lang="ts">
import BackendPlaceholder from '@/components/BackendPlaceholder.vue'
</script>

<template>
  <BackendPlaceholder
    title="来料主数据"
    description="原有供应商指标、成分分布和主数据示意表均为前端演示内容，现已全部删除。当前页面保留为后端接入占位，避免继续展示任何假统计或假主数据。"
    :endpoints="['/api/batches', '/api/orders']"
  />
</template>
