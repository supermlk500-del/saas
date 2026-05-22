<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchStyleTrace, type StyleTraceItem } from '@/api/trace/styleTrace'
import { useTable } from '@/hooks/useTable'
import SearchBar from '@/components/SearchBar.vue'

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '款式名/订单号/客户', width: '200px' },
  {
    label: '状态',
    name: 'status',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '待开始', value: '待开始' },
      { label: '生产中', value: '生产中' },
      { label: '已完成', value: '已完成' },
    ],
  },
]

const columns = [
  { title: '款式名称', dataIndex: 'styleName', key: 'styleName' },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '客户', dataIndex: 'customer', key: 'customer' },
  { title: '颜色', dataIndex: 'color', key: 'color' },
  { title: '尺码', dataIndex: 'size', key: 'size' },
  { title: '计划数量', dataIndex: 'totalQty', key: 'totalQty' },
  { title: '完成数量', dataIndex: 'completedQty', key: 'completedQty' },
  { title: '缺陷数量', dataIndex: 'defectQty', key: 'defectQty' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<StyleTraceItem>()

const loadData = async () => {
  loading.value = true
  data.value = await fetchStyleTrace({ keyword: searchForm.keyword, status: searchForm.status })
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
  <TablePage title="款式追溯" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
  </TablePage>
</template>
