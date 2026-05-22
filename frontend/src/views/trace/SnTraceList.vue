<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchSnTrace, type SnTraceItem } from '@/api/trace/snTrace'
import { useTable } from '@/hooks/useTable'
import SearchBar from '@/components/SearchBar.vue'

const searchForm = reactive({ keyword: '', qcResult: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '序列号/款式名/批次号', width: '200px' },
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
  { title: '序列号', dataIndex: 'sn', key: 'sn' },
  { title: '款式名称', dataIndex: 'styleName', key: 'styleName' },
  { title: '颜色', dataIndex: 'color', key: 'color' },
  { title: '尺码', dataIndex: 'size', key: 'size' },
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo' },
  { title: '裁剪', dataIndex: 'cutting', key: 'cutting' },
  { title: '缝纫', dataIndex: 'sewing', key: 'sewing' },
  { title: '整烫', dataIndex: 'iron', key: 'iron' },
  { title: '包装', dataIndex: 'pack', key: 'pack' },
  { title: '检验结果', dataIndex: 'qcResult', key: 'qcResult' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<SnTraceItem>()

const loadData = async () => {
  loading.value = true
  data.value = await fetchSnTrace({ keyword: searchForm.keyword, qcResult: searchForm.qcResult })
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
  <TablePage title="序列号追溯" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
  </TablePage>
</template>
