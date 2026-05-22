<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchPlanLogs, type PlanLogItem } from '@/api/plan/planLog'
import { useTable } from '@/hooks/useTable'
import SearchBar from '@/components/SearchBar.vue'

const searchForm = reactive({ keyword: '', action: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '计划号/详情', width: '200px' },
  {
    label: '动作',
    name: 'action',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '生成', value: '生成' },
      { label: '调整', value: '调整' },
      { label: '重排', value: '重排' },
      { label: '确认', value: '确认' },
    ],
  },
]

const columns = [
  { title: '计划号', dataIndex: 'planNo', key: 'planNo' },
  { title: '动作', dataIndex: 'action', key: 'action' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' },
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: '详情', dataIndex: 'detail', key: 'detail' },
]

const { data, loading, pagination } = useTable<PlanLogItem>()

const loadData = async () => {
  loading.value = true
  data.value = await fetchPlanLogs({ keyword: searchForm.keyword, action: searchForm.action })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.action = undefined
  loadData()
}

onMounted(loadData)
</script>

<template>
  <TablePage title="排产日志" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
  </TablePage>
</template>
