<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchDispatches, type DispatchItem } from '@/api/execution/dispatch'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '派工单号', name: 'dispatchNo', type: 'input' as const, placeholder: '请输入派工单号' },
  { label: '工单号', name: 'woNo', type: 'input' as const, placeholder: '请输入工单号' },
  { label: '设备', name: 'equipment', type: 'input' as const, placeholder: '请输入设备' },
  { label: '操作人', name: 'operator', type: 'input' as const, placeholder: '请输入操作人' },
]

const formModel = reactive({ dispatchNo: '', woNo: '', equipment: '', operator: '' })

const rules = {
  dispatchNo: [{ required: true, message: '请输入派工单号' }],
  woNo: [{ required: true, message: '请输入工单号' }],
  equipment: [{ required: true, message: '请输入设备' }],
  operator: [{ required: true, message: '请输入操作人' }],
}

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '派工单号/工单号', width: '200px' },
  {
    label: '状态',
    name: 'status',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '待开工', value: '待开工' },
      { label: '执行中', value: '执行中' },
      { label: '已完成', value: '已完成' },
    ],
  },
]

const columns = [
  { title: '派工单号', dataIndex: 'dispatchNo', key: 'dispatchNo' },
  { title: '工单号', dataIndex: 'woNo', key: 'woNo' },
  { title: '设备', dataIndex: 'equipment', key: 'equipment' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<DispatchItem>()

const modalOpen = ref(false)

const actionBar = [
  { key: 'new', label: '新建派工', type: 'primary' },
]

const handlePageAction = (key: string) => {
  if (key === 'new') openModal()
}

const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }

const loadData = async () => {
  loading.value = true
  data.value = await fetchDispatches({ keyword: searchForm.keyword, status: searchForm.status })
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
  <TablePage title="派工单" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>
  </TablePage>

  <FormModal v-model:open="modalOpen" title="新建派工" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
