<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchBatchTrace, type BatchTraceItem } from '@/api/trace/batchTrace'
import { useTable } from '@/hooks/useTable'
import SearchBar from '@/components/SearchBar.vue'

const searchForm = reactive({ keyword: '', qcResult: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '批次号/款式名', width: '200px' },
  {
    label: '检验结果',
    name: 'qcResult',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '合格', value: '合格' },
      { label: '不合格', value: '不合格' },
      { label: '待检', value: '待检' },
    ],
  },
]

const columns = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo' },
  { title: '款式名称', dataIndex: 'styleName', key: 'styleName' },
  { title: '颜色', dataIndex: 'color', key: 'color' },
  { title: '尺码', dataIndex: 'size', key: 'size' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
  { title: '裁剪日期', dataIndex: 'cuttingDate', key: 'cuttingDate' },
  { title: '缝纫日期', dataIndex: 'sewingDate', key: 'sewingDate' },
  { title: '整烫日期', dataIndex: 'ironDate', key: 'ironDate' },
  { title: '包装日期', dataIndex: 'packDate', key: 'packDate' },
  { title: '检验结果', dataIndex: 'qcResult', key: 'qcResult' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<BatchTraceItem>()

const loadData = async () => {
  loading.value = true
  data.value = await fetchBatchTrace({ keyword: searchForm.keyword, qcResult: searchForm.qcResult })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.qcResult = undefined
  loadData()
}

onMounted(loadData)
</script>

<template>
  <TablePage title="批次追溯" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
  </TablePage>
</template>
