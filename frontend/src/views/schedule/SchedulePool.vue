<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { useTable } from '@/hooks/useTable'

type SchedulePoolItem = {
  key: string
  batchNo: string
  grade: string
  availableQty: number
  locked: string
  priority: number
  readyForSchedule: string
}

const searchForm = reactive({ batchNo: '', readyForSchedule: undefined as string | undefined })

const searchFields = [
  { label: '批次号', name: 'batchNo', type: 'input' as const, placeholder: '请输入批次号', width: '220px' },
  {
    label: '可排产',
    name: 'readyForSchedule',
    type: 'select' as const,
    placeholder: '全部',
    options: [
      { label: '是', value: '是' },
      { label: '否', value: '否' },
    ],
  },
]

const columns = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo' },
  { title: '判级', dataIndex: 'grade', key: 'grade' },
  { title: '可排产量(kg)', dataIndex: 'availableQty', key: 'availableQty' },
  { title: '锁定状态', dataIndex: 'locked', key: 'locked' },
  { title: '优先级', dataIndex: 'priority', key: 'priority' },
  { title: '可参与排产', dataIndex: 'readyForSchedule', key: 'readyForSchedule' },
]

const { data, loading, pagination } = useTable<SchedulePoolItem>()

const allData: SchedulePoolItem[] = [
  { key: '1', batchNo: 'GB-20260401-01', grade: 'A', availableQty: 398, locked: '未锁定', priority: 1, readyForSchedule: '是' },
  { key: '2', batchNo: 'GB-20260401-02', grade: 'B', availableQty: 360, locked: '未锁定', priority: 2, readyForSchedule: '是' },
  { key: '3', batchNo: 'GB-20260402-01', grade: 'C', availableQty: 0, locked: '已锁定', priority: 9, readyForSchedule: '否' },
]

const loadData = () => {
  loading.value = true
  data.value = allData.filter((item) => {
    const hitBatch = !searchForm.batchNo || item.batchNo.includes(searchForm.batchNo)
    const hitReady = !searchForm.readyForSchedule || item.readyForSchedule === searchForm.readyForSchedule
    return hitBatch && hitReady
  })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.batchNo = ''
  searchForm.readyForSchedule = undefined
  loadData()
}

onMounted(loadData)
</script>

<template>
  <TablePage title="排产池" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
  </TablePage>
</template>
