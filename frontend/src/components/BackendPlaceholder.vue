<script setup lang="ts">
defineProps<{
  title: string
  description: string
  endpoints?: string[]
}>()
</script>

<template>
  <a-card class="page-card backend-placeholder" :bordered="false">
    <div class="placeholder-header">
      <h2 class="placeholder-title">{{ title }}</h2>
      <p class="placeholder-description">{{ description }}</p>
    </div>

    <a-empty description="当前页面不再提供前端演示数据">
      <template #image>
        <div class="placeholder-badge">API</div>
      </template>
    </a-empty>

    <div v-if="endpoints?.length" class="endpoint-list">
      <div class="endpoint-title">待接入接口</div>
      <a-space wrap>
        <a-tag v-for="endpoint in endpoints" :key="endpoint" color="orange">
          {{ endpoint }}
        </a-tag>
      </a-space>
    </div>
  </a-card>
</template>

<style scoped>
.backend-placeholder {
  min-height: 420px;
  padding: 24px 20px;
}

.placeholder-header {
  margin-bottom: 32px;
}

.placeholder-title {
  margin: 0 0 10px;
  color: #1f2937;
  font-size: 22px;
  font-weight: 700;
}

.placeholder-description {
  margin: 0;
  max-width: 780px;
  color: #667085;
  line-height: 1.7;
}

.placeholder-badge {
  width: 88px;
  height: 88px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(214, 111, 34, 0.12), rgba(214, 111, 34, 0.24));
  color: #d66f22;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.endpoint-list {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(145, 158, 171, 0.16);
}

.endpoint-title {
  margin-bottom: 12px;
  color: #344054;
  font-weight: 600;
}
</style>
