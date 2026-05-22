<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchNcrs, type NcrItem } from '@/api/quality/ncr'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: 'NCR编号', name: 'ncrNo', type: 'input' as const, placeholder: '请输入NCR编号' },
  { label: '结果号', name: 'resultNo', type: 'input' as const, placeholder: '请输入结果号' },
  { label: '工单号', name: 'woNo', type: 'input' as const, placeholder: '请输入工单号' },
  { label: '缺陷名称', name: 'defectName', type: 'input' as const, placeholder: '请输入缺陷名称' },
  { label: '数量', name: 'qty', type: 'input' as const, placeholder: '请输入数量' },
  { label: '处置方式', name: 'disposition', type: 'input' as const, placeholder: '请输入处置方式' },
]

const formModel = reactive({ ncrNo: '', resultNo: '', woNo: '', defectName: '', qty: '', disposition: '' })

const rules = {
  ncrNo: [{ required: true, message: '请输入NCR编号' }],
  resultNo: [{ required: true, message: '请输入结果号' }],
  woNo: [{ required: true, message: '请输入工单号' }],
  defectName: [{ required: true, message: '请输入缺陷名称' }],
  qty: [{ required: true, message: '请输入数量' }],
  disposition: [{ required: true, message: '请输入处置方式' }],
}

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: 'NCR编号/工单号', width: '200px' },
  { label: '状态', name: 'status', type: 'select', placeholder: '全部', options: [{ label: '待处理', value: '待处理' }, { label: '已处理', value: '已处理' }] },
]

const columns = [
  { title: 'NCR编号', dataIndex: 'ncrNo', key: 'ncrNo' },
  { title: '结果号', dataIndex: 'resultNo', key: 'resultNo' },
  { title: '工单号', dataIndex: 'woNo', key: 'woNo' },
  { title: '缺陷名称', dataIndex: 'defectName', key: 'defectName' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
  { title: '处置方式', dataIndex: 'disposition', key: 'disposition' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<NcrItem>()
const modalOpen = ref(false)
const actionBar = [{ key: 'new', label: '新建NCR', type: 'primary' }]
const handlePageAction = (key: string) => { if (key === 'new') openModal() }
const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }
const loadData = async () => {
  loading.value = true
  data.value = await fetchNcrs({ keyword: searchForm.keyword, status: searchForm.status })
  pagination.total = data.value.length
  loading.value = false
}
const resetSearch = () => { searchForm.keyword = ''; searchForm.status = undefined; loadData() }
onMounted(loadData)
</script>

<template>
  <TablePage title="NCR管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search><SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" /></template>
    <template #actions><ActionBar :actions="actionBar" @action="handlePageAction" /></template>
  </TablePage>
  <FormModal v-model:open="modalOpen" title="新建NCR" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
