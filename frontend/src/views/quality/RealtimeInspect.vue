<script setup lang="ts">
import BackendPlaceholder from '@/components/BackendPlaceholder.vue'
</script>

<template>
  <BackendPlaceholder
    title="实时质检"
    description="原有前端写死的摄像头列表、缺陷图片、上传缩略图、示例统计和相机参数面板已全部删除。当前页面不再展示任何假监控画面，只等待后端实时质检与附件接口接入。"
    :endpoints="['/api/qc-records', '/api/inspection-data', '/api/qc-items']"
  />
</template>
