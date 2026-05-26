<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { fetchBatches, type BatchQuery } from '@/api/batch/batch'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { batchStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { BatchItem } from '@/types/domain'

type BatchExecutionRow = BatchItem & {
  readyForSchedule: boolean
}

const searchForm = reactive({
  batchNo: '',
  supplier: '',
  status: undefined as string | undefined,
})

const searchFields = [
  { label: '批次编号', name: 'batchNo', type: 'input' as const, placeholder: '请输入批次编号', width: '180px' },
  { label: '供应商', name: 'supplier', type: 'input' as const, placeholder: '请输入供应商', width: '180px' },
  {
    label: '批次状态',
    name: 'status',
    type: 'select' as const,
    placeholder: '全部',
    options: batchStatusOptions,
    width: '160px',
  },
]

const columns = [
  { title: '批次编号', dataIndex: 'batchNo', key: 'batchNo', width: 180 },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier', width: 220 },
  { title: '重量(kg)', dataIndex: 'weight', key: 'weight', width: 120 },
  { title: '门幅(cm)', dataIndex: 'width', key: 'width', width: 120 },
  { title: '成分', dataIndex: 'composition', key: 'composition', width: 180 },
  { title: '入厂时间', dataIndex: 'inDate', key: 'inDate', width: 180 },
  { title: '批次状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '执行定位', dataIndex: 'executionTag', key: 'executionTag', width: 140 },
]

const { data, loading, pagination } = useTable<BatchExecutionRow>()

const getStatusMeta = (status?: string) =>
  batchStatusOptions.find((item) => item.value === status)

const loadData = async () => {
  loading.value = true
  try {
    const query: BatchQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      batchNo: searchForm.batchNo || undefined,
      supplier: searchForm.supplier || undefined,
    }
    const response = await fetchBatches(query)
    data.value = response.list
      .filter((item) => !searchForm.status || item.status === searchForm.status)
      .map((item) => ({
        ...item,
        readyForSchedule: item.status === 'READY',
      }))
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
  searchForm.batchNo = ''
  searchForm.supplier = ''
  searchForm.status = undefined
  pagination.current = 1
  void loadData()
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <TablePage title="批次执行池" :columns="columns" :data="data" :loading="loading" :pagination="pagination" row-key="batchId">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'weight'">
        {{ record.weight ?? '-' }}
      </template>
      <template v-else-if="column.key === 'width'">
        {{ record.width ?? '-' }}
      </template>
      <template v-else-if="column.key === 'composition'">
        {{ record.composition || '-' }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="getStatusMeta(record.status)?.color">
          {{ getStatusMeta(record.status)?.label || record.status || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'executionTag'">
        <a-tag :color="record.readyForSchedule ? 'success' : 'default'">
          {{ record.readyForSchedule ? '待执行资源' : '执行中资源' }}
        </a-tag>
      </template>
    </template>
  </TablePage>
</template>
