<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { fetchCapas, type CapaItem, type CapaQuery } from '@/api/quality/capa'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { exceptionLevelOptions, exceptionStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import { formatDateTime } from '@/utils/date'
import { formatPlanStepId } from '@/utils/idFormat'

const searchForm = reactive({
  status: undefined as string | undefined,
})

const searchFields = [
  { label: '状态', name: 'status', type: 'select' as const, placeholder: '全部', options: exceptionStatusOptions, width: '160px' },
]

const columns = [
  { title: '异常ID', dataIndex: 'exceptionId', key: 'exceptionId', width: 100 },
  { title: '工序计划ID', dataIndex: 'planStepId', key: 'planStepId', width: 96 },
  { title: '异常等级', dataIndex: 'exceptionLevel', key: 'exceptionLevel', width: 120 },
  { title: '异常描述', dataIndex: 'description', key: 'description' },
  { title: '处理结果', dataIndex: 'handleResult', key: 'handleResult' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
]

const { data, loading, pagination } = useTable<CapaItem>()

const getLevelMeta = (value?: string) =>
  exceptionLevelOptions.find((item) => item.value === value)

const getStatusMeta = (value?: string) =>
  exceptionStatusOptions.find((item) => item.value === value)

const loadData = async () => {
  loading.value = true
  try {
    const query: CapaQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      status: searchForm.status,
    }
    const response = await fetchCapas(query)
    data.value = response.list
    pagination.total = response.total
  } finally {
    loading.value = false
  }
}

pagination.onChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  void loadData()
}

const resetSearch = () => {
  searchForm.status = undefined
  pagination.current = 1
  void loadData()
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <TablePage title="CAPA管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'planStepId'">
        <a-tooltip :title="String(record.planStepId)">
          {{ formatPlanStepId(record.planStepId) }}
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'exceptionLevel'">
        <a-tag :color="getLevelMeta(record.exceptionLevel)?.color">
          {{ getLevelMeta(record.exceptionLevel)?.label || record.exceptionLevel }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'handleResult'">
        {{ record.handleResult || '-' }}
      </template>
      <template v-else-if="column.key === 'createTime'">
        {{ formatDateTime(record.createTime) }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="getStatusMeta(record.status)?.color">
          {{ getStatusMeta(record.status)?.label || record.status }}
        </a-tag>
      </template>
    </template>
  </TablePage>
</template>
