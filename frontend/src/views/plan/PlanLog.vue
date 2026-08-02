<script setup lang="ts">
import BackendPlaceholder from '@/components/BackendPlaceholder.vue'
</script>

<template>
  <BackendPlaceholder
    title="排产日志"
    description="原页面仅调用返回空数组的前端占位接口，后端排产日志接口尚未接入。当前页面保留为占位，避免继续展示空表格假功能。"
    :endpoints="['/api/plan-logs']"
  />
</template>
