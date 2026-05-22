<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchResults, type ResultItem } from '@/api/quality/result'
import { useTable } from '@/hooks/useTable'
import SearchBar from '@/components/SearchBar.vue'

const searchForm = reactive({ keyword: '', result: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '结果号/任务号', width: '200px' },
  { label: '结果', name: 'result', type: 'select', placeholder: '全部', options: [{ label: '合格', value: '合格' }, { label: '不合格', value: '不合格' }] },
]

const columns = [
  { title: '结果号', dataIndex: 'resultNo', key: 'resultNo' },
  { title: '任务号', dataIndex: 'taskNo', key: 'taskNo' },
  { title: '工单号', dataIndex: 'woNo', key: 'woNo' },
  { title: '检验员', dataIndex: 'inspector', key: 'inspector' },
  { title: '结果', dataIndex: 'result', key: 'result' },
  { title: '缺陷数', dataIndex: 'defectCount', key: 'defectCount' },
  { title: '时间', dataIndex: 'time', key: 'time' },
]

const { data, loading, pagination } = useTable<ResultItem>()

const loadData = async () => {
  loading.value = true
  data.value = await fetchResults({ keyword: searchForm.keyword, result: searchForm.result })
  pagination.total = data.value.length
  loading.value = false
}
const resetSearch = () => { searchForm.keyword = ''; searchForm.result = undefined; loadData() }
onMounted(loadData)
</script>

<template>
  <TablePage title="检验结果" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search><SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" /></template>
  </TablePage>
</template>
