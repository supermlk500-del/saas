<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchMaterialTrace, type MaterialTraceItem } from '@/api/trace/materialTrace'
import { useTable } from '@/hooks/useTable'
import SearchBar from '@/components/SearchBar.vue'

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '面料名/编码/款式', width: '200px' },
  {
    label: '状态',
    name: 'status',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '待使用', value: '待使用' },
      { label: '已使用', value: '已使用' },
    ],
  },
]

const columns = [
  { title: '面料名称', dataIndex: 'fabricName', key: 'fabricName' },
  { title: '面料编码', dataIndex: 'fabricCode', key: 'fabricCode' },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier' },
  { title: '使用款式', dataIndex: 'usedBy', key: 'usedBy' },
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo' },
  { title: '用量', dataIndex: 'qty', key: 'qty' },
  { title: '单位', dataIndex: 'unit', key: 'unit' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<MaterialTraceItem>()

const loadData = async () => {
  loading.value = true
  data.value = await fetchMaterialTrace({ keyword: searchForm.keyword, status: searchForm.status })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.status = undefined
  loadData()
}

onMounted(loadData)
</script>

<template>
  <TablePage title="面料追溯" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
  </TablePage>
</template>
