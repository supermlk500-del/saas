<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchReports, type ReportItem } from '@/api/execution/report'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '报工单号', name: 'reportNo', type: 'input' as const, placeholder: '请输入报工单号' },
  { label: '工单号', name: 'woNo', type: 'input' as const, placeholder: '请输入工单号' },
  { label: '工序', name: 'processName', type: 'input' as const, placeholder: '请输入工序' },
  {
    label: '类型',
    name: 'type',
    type: 'select' as const,
    placeholder: '请选择类型',
    options: [
      { label: '开工', value: '开工' },
      { label: '完工', value: '完工' },
      { label: '异常', value: '异常' },
    ],
  },
  { label: '操作人', name: 'operator', type: 'input' as const, placeholder: '请输入操作人' },
  { label: '数量', name: 'qty', type: 'input' as const, placeholder: '请输入数量' },
]

const formModel = reactive({ reportNo: '', woNo: '', processName: '', type: '', operator: '', qty: '' })

const rules = {
  reportNo: [{ required: true, message: '请输入报工单号' }],
  woNo: [{ required: true, message: '请输入工单号' }],
  processName: [{ required: true, message: '请输入工序' }],
  type: [{ required: true, message: '请选择类型' }],
  operator: [{ required: true, message: '请输入操作人' }],
}

const searchForm = reactive({ keyword: '', type: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '报工单号/工单号', width: '200px' },
  {
    label: '类型',
    name: 'type',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '开工', value: '开工' },
      { label: '完工', value: '完工' },
      { label: '异常', value: '异常' },
    ],
  },
]

const columns = [
  { title: '报工单号', dataIndex: 'reportNo', key: 'reportNo' },
  { title: '工单号', dataIndex: 'woNo', key: 'woNo' },
  { title: '工序', dataIndex: 'processName', key: 'processName' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' },
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
]

const { data, loading, pagination } = useTable<ReportItem>()

const modalOpen = ref(false)

const actionBar = [
  { key: 'new', label: '新增报工', type: 'primary' },
]

const handlePageAction = (key: string) => {
  if (key === 'new') openModal()
}

const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }

const loadData = async () => {
  loading.value = true
  data.value = await fetchReports({ keyword: searchForm.keyword, type: searchForm.type })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.type = undefined
  loadData()
}

onMounted(loadData)
</script>

<template>
  <TablePage title="报工" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>
  </TablePage>

  <FormModal v-model:open="modalOpen" title="新增报工" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
