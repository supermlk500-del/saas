<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchOperations, type OperationItem } from '@/api/plan/operation'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '订单号', name: 'orderNo', type: 'input' as const, placeholder: '请输入订单号' },
  { label: '工序名称', name: 'processName', type: 'input' as const, placeholder: '请输入工序名称' },
  { label: '序号', name: 'step', type: 'input' as const, placeholder: '请输入工序序号' },
  { label: '设备', name: 'equipment', type: 'input' as const, placeholder: '请输入设备' },
  { label: '计划开始', name: 'planStart', type: 'date' as const },
  { label: '计划结束', name: 'planEnd', type: 'date' as const },
]

const formModel = reactive({ orderNo: '', processName: '', step: '', equipment: '', planStart: undefined, planEnd: undefined })

const rules = {
  orderNo: [{ required: true, message: '请输入订单号' }],
  processName: [{ required: true, message: '请输入工序名称' }],
  step: [{ required: true, message: '请输入工序序号' }],
  equipment: [{ required: true, message: '请输入设备' }],
  planStart: [{ required: true, message: '请选择计划开始日期' }],
  planEnd: [{ required: true, message: '请选择计划结束日期' }],
}

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '订单号/工序', width: '200px' },
  {
    label: '状态',
    name: 'status',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '待执行', value: '待执行' },
      { label: '执行中', value: '执行中' },
      { label: '已完成', value: '已完成' },
    ],
  },
]

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '工序名称', dataIndex: 'processName', key: 'processName' },
  { title: '序号', dataIndex: 'step', key: 'step' },
  { title: '设备', dataIndex: 'equipment', key: 'equipment' },
  { title: '计划开始', dataIndex: 'planStart', key: 'planStart' },
  { title: '计划结束', dataIndex: 'planEnd', key: 'planEnd' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<OperationItem>()

const modalOpen = ref(false)

const actionBar = [
  { key: 'new', label: '新增排产', type: 'primary' },
]

const handlePageAction = (key: string) => {
  if (key === 'new') openModal()
}

const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }

const loadData = async () => {
  loading.value = true
  data.value = await fetchOperations({ keyword: searchForm.keyword, status: searchForm.status })
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
  <TablePage title="工序排产" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>
  </TablePage>

  <FormModal v-model:open="modalOpen" title="新增工序排产" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
