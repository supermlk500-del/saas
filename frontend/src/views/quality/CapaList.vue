<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchCapas, type CapaItem } from '@/api/quality/capa'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: 'CAPA编号', name: 'capaNo', type: 'input' as const, placeholder: '请输入CAPA编号' },
  { label: 'NCR编号', name: 'ncrNo', type: 'input' as const, placeholder: '请输入NCR编号' },
  { label: '标题', name: 'title', type: 'input' as const, placeholder: '请输入标题' },
  { label: '根因分析', name: 'rootCause', type: 'input' as const, placeholder: '请输入根因' },
  { label: '纠正措施', name: 'correctiveAction', type: 'input' as const, placeholder: '请输入纠正措施' },
  { label: '负责人', name: 'owner', type: 'input' as const, placeholder: '请输入负责人' },
  { label: '完成期限', name: 'deadline', type: 'date' as const },
]

const formModel = reactive({ capaNo: '', ncrNo: '', title: '', rootCause: '', correctiveAction: '', owner: '', deadline: undefined })

const rules = {
  capaNo: [{ required: true, message: '请输入CAPA编号' }],
  ncrNo: [{ required: true, message: '请输入NCR编号' }],
  title: [{ required: true, message: '请输入标题' }],
  rootCause: [{ required: true, message: '请输入根因分析' }],
  correctiveAction: [{ required: true, message: '请输入纠正措施' }],
  owner: [{ required: true, message: '请输入负责人' }],
  deadline: [{ required: true, message: '请选择完成期限' }],
}

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: 'CAPA编号/标题', width: '200px' },
  { label: '状态', name: 'status', type: 'select', placeholder: '全部', options: [{ label: '执行中', value: '执行中' }, { label: '待确认', value: '待确认' }, { label: '已关闭', value: '已关闭' }] },
]

const columns = [
  { title: 'CAPA编号', dataIndex: 'capaNo', key: 'capaNo' },
  { title: 'NCR编号', dataIndex: 'ncrNo', key: 'ncrNo' },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '根因分析', dataIndex: 'rootCause', key: 'rootCause' },
  { title: '纠正措施', dataIndex: 'correctiveAction', key: 'correctiveAction' },
  { title: '负责人', dataIndex: 'owner', key: 'owner' },
  { title: '完成期限', dataIndex: 'deadline', key: 'deadline' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<CapaItem>()
const modalOpen = ref(false)
const actionBar = [{ key: 'new', label: '新建CAPA', type: 'primary' }]
const handlePageAction = (key: string) => { if (key === 'new') openModal() }
const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }
const loadData = async () => {
  loading.value = true
  data.value = await fetchCapas({ keyword: searchForm.keyword, status: searchForm.status })
  pagination.total = data.value.length
  loading.value = false
}
const resetSearch = () => { searchForm.keyword = ''; searchForm.status = undefined; loadData() }
onMounted(loadData)
</script>

<template>
  <TablePage title="CAPA管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search><SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" /></template>
    <template #actions><ActionBar :actions="actionBar" @action="handlePageAction" /></template>
  </TablePage>
  <FormModal v-model:open="modalOpen" title="新建CAPA" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
