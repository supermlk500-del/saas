<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchTracks, type TrackItem } from '@/api/execution/track'
import { useTable } from '@/hooks/useTable'
import SearchBar from '@/components/SearchBar.vue'

const searchForm = reactive({ keyword: '', processName: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '工单号/产品名', width: '200px' },
  {
    label: '工序',
    name: 'processName',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '织物检验', value: '织物检验' },
      { label: '裁剪', value: '裁剪' },
      { label: '缝纫', value: '缝纫' },
      { label: '整烫', value: '整烫' },
      { label: '包装', value: '包装' },
    ],
  },
]

const columns = [
  { title: '工单号', dataIndex: 'woNo', key: 'woNo' },
  { title: '款式名称', dataIndex: 'productName', key: 'productName' },
  { title: '工序', dataIndex: 'processName', key: 'processName' },
  { title: '序号', dataIndex: 'step', key: 'step' },
  { title: '设备', dataIndex: 'equipment', key: 'equipment' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' },
  { title: '过站时间', dataIndex: 'passTime', key: 'passTime' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
]

const { data, loading, pagination } = useTable<TrackItem>()

const loadData = async () => {
  loading.value = true
  data.value = await fetchTracks({ keyword: searchForm.keyword, processName: searchForm.processName })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.processName = undefined
  loadData()
}

onMounted(loadData)
</script>

<template>
  <TablePage title="工序过站" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
  </TablePage>
</template>
