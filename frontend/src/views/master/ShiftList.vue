<script setup lang="ts">
import BackendPlaceholder from '@/components/BackendPlaceholder.vue'
</script>

<template>
  <BackendPlaceholder
    title="班次管理"
    description="原页面仅展示前端空表格和不可落库的新增弹窗，后端班次接口尚未接入。当前页面保留为占位，避免继续展示不可用操作。"
    :endpoints="['/api/shifts']"
  />
</template>
