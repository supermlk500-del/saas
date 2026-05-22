<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchSizeCharts, type SizeChartItem } from '@/api/master/sizeChart'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '尺码表编码', name: 'code', type: 'input' as const, placeholder: '请输入尺码表编码' },
  { label: '尺码表名称', name: 'name', type: 'input' as const, placeholder: '请输入尺码表名称' },
  { label: '适用范围', name: 'range', type: 'input' as const, placeholder: '如 成人/童装' },
  { label: '尺码列表', name: 'sizes', type: 'input' as const, placeholder: '如 XS/S/M/L/XL' },
]

const formModel = reactive({ code: '', name: '', range: '', sizes: '' })

const rules = {
  code: [{ required: true, message: '请输入尺码表编码' }],
  name: [{ required: true, message: '请输入尺码表名称' }],
  range: [{ required: true, message: '请输入适用范围' }],
  sizes: [{ required: true, message: '请输入尺码列表' }],
}

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '编码/名称', width: '200px' },
  { label: '状态', name: 'status', type: 'select', placeholder: '全部', options: [{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }] },
]

const columns = [
  { title: '尺码表编码', dataIndex: 'code', key: 'code' },
  { title: '尺码表名称', dataIndex: 'name', key: 'name' },
  { title: '适用范围', dataIndex: 'range', key: 'range' },
  { title: '尺码列表', dataIndex: 'sizes', key: 'sizes' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<SizeChartItem>()
const modalOpen = ref(false)
const actionBar = [{ key: 'new', label: '新增尺码表', type: 'primary' }]
const handlePageAction = (key: string) => { if (key === 'new') openModal() }
const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }
const loadData = async () => {
  loading.value = true
  data.value = await fetchSizeCharts({ keyword: searchForm.keyword, status: searchForm.status })
  pagination.total = data.value.length
  loading.value = false
}
const resetSearch = () => { searchForm.keyword = ''; searchForm.status = undefined; loadData() }
onMounted(loadData)
</script>

<template>
  <TablePage title="尺码表管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search><SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" /></template>
    <template #actions><ActionBar :actions="actionBar" @action="handlePageAction" /></template>
  </TablePage>
  <FormModal v-model:open="modalOpen" title="新增尺码表" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
