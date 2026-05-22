<script setup lang="ts">
import BackendPlaceholder from '@/components/BackendPlaceholder.vue'
</script>

<template>
  <BackendPlaceholder
    title="工艺档案中心"
    description="原有工艺树、工序画布和示意数据均为前端演示内容，现已删除。当前页面仅保留后端接入占位，避免继续使用静态工艺档案。"
    :endpoints="['/api/process-routes', '/api/process-routes/{routeId}/steps', '/api/process-steps']"
  />
</template>
