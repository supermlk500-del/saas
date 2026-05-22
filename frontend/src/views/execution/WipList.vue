<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchWipItems, type WipItem } from '@/api/execution/wip'
import { useTable } from '@/hooks/useTable'
import SearchBar from '@/components/SearchBar.vue'

const searchForm = reactive({ keyword: '', line: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '工单号/款式名', width: '200px' },
  {
    label: '车间',
    name: 'line',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '裁剪车间', value: '裁剪车间' },
      { label: '缝纫车间', value: '缝纫车间' },
      { label: '整烫车间', value: '整烫车间' },
    ],
  },
]

const columns = [
  { title: '工单号', dataIndex: 'woNo', key: 'woNo' },
  { title: '款式名称', dataIndex: 'productName', key: 'productName' },
  { title: '当前工序', dataIndex: 'processName', key: 'processName' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
  { title: '车间', dataIndex: 'line', key: 'line' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<WipItem>()

const loadData = async () => {
  loading.value = true
  data.value = await fetchWipItems({ keyword: searchForm.keyword, line: searchForm.line })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.line = undefined
  loadData()
}

onMounted(loadData)
</script>

<template>
  <TablePage title="WIP在制品" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
  </TablePage>
</template>
