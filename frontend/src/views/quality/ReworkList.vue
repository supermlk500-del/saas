<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchReworks, type ReworkItem } from '@/api/quality/rework'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '返工单号', name: 'reworkNo', type: 'input' as const, placeholder: '请输入返工单号' },
  { label: 'NCR编号', name: 'ncrNo', type: 'input' as const, placeholder: '请输入NCR编号' },
  { label: '工单号', name: 'woNo', type: 'input' as const, placeholder: '请输入工单号' },
  { label: '产品名称', name: 'productName', type: 'input' as const, placeholder: '请输入产品名称' },
  { label: '数量', name: 'qty', type: 'input' as const, placeholder: '请输入数量' },
  { label: '原因', name: 'reason', type: 'input' as const, placeholder: '请输入返工原因' },
]

const formModel = reactive({ reworkNo: '', ncrNo: '', woNo: '', productName: '', qty: '', reason: '' })

const rules = {
  reworkNo: [{ required: true, message: '请输入返工单号' }],
  ncrNo: [{ required: true, message: '请输入NCR编号' }],
  woNo: [{ required: true, message: '请输入工单号' }],
  productName: [{ required: true, message: '请输入产品名称' }],
  qty: [{ required: true, message: '请输入数量' }],
  reason: [{ required: true, message: '请输入返工原因' }],
}

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '返工单号/工单号', width: '200px' },
  { label: '状态', name: 'status', type: 'select', placeholder: '全部', options: [{ label: '待返工', value: '待返工' }, { label: '返工中', value: '返工中' }, { label: '已完成', value: '已完成' }] },
]

const columns = [
  { title: '返工单号', dataIndex: 'reworkNo', key: 'reworkNo' },
  { title: 'NCR编号', dataIndex: 'ncrNo', key: 'ncrNo' },
  { title: '工单号', dataIndex: 'woNo', key: 'woNo' },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
  { title: '原因', dataIndex: 'reason', key: 'reason' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<ReworkItem>()
const modalOpen = ref(false)
const actionBar = [{ key: 'new', label: '新建返工单', type: 'primary' }]
const handlePageAction = (key: string) => { if (key === 'new') openModal() }
const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }
const loadData = async () => {
  loading.value = true
  data.value = await fetchReworks({ keyword: searchForm.keyword, status: searchForm.status })
  pagination.total = data.value.length
  loading.value = false
}
const resetSearch = () => { searchForm.keyword = ''; searchForm.status = undefined; loadData() }
onMounted(loadData)
</script>

<template>
  <TablePage title="返工单" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search><SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" /></template>
    <template #actions><ActionBar :actions="actionBar" @action="handlePageAction" /></template>
  </TablePage>
  <FormModal v-model:open="modalOpen" title="新建返工单" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
