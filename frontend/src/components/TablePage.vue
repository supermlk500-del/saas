<script setup lang="ts">
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue'

defineProps<{
  title: string
  columns: TableColumnsType
  data: object[]
  loading?: boolean
  pagination?: false | TablePaginationConfig
}>()
</script>

<template>
  <div class="table-page">
    <div class="page-header">
      <div class="title-section">
        <div class="title-dot"></div>
        <h2 class="page-title">{{ title }}</h2>
      </div>
      <div v-if="$slots.actions" class="actions-section">
        <slot name="actions" />
      </div>
    </div>

    <a-card v-if="$slots.search" class="search-card" :bordered="false">
      <slot name="search" />
    </a-card>

    <a-card class="table-card" :bordered="false">
      <a-table
        v-bind="$attrs"
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        class="custom-table"
      >
        <template v-if="$slots.bodyCell" #bodyCell="slotProps">
          <slot name="bodyCell" v-bind="slotProps" />
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.table-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-dot {
  width: 4px;
  height: 18px;
  background: #ff7a45;
  border-radius: 2px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.search-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.table-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  padding: 8px;
}

:deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
}

:deep(.ant-table-tbody > tr > td) {
  color: #1e293b;
}

:deep(.ant-pagination-item-active) {
  border-color: #ff7a45;
}

:deep(.ant-pagination-item-active a) {
  color: #ff7a45;
}
</style>
