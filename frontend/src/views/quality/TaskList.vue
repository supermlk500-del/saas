<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchTasks, type TaskItem } from '@/api/quality/task'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '检验任务号', name: 'taskNo', type: 'input' as const, placeholder: '请输入检验任务号' },
  { label: '工单号', name: 'woNo', type: 'input' as const, placeholder: '请输入工单号' },
  { label: '检验模板', name: 'templateName', type: 'input' as const, placeholder: '请输入检验模板' },
  { label: '检验员', name: 'inspector', type: 'input' as const, placeholder: '请输入检验员' },
  { label: '截止日期', name: 'deadline', type: 'date' as const },
]

const formModel = reactive({ taskNo: '', woNo: '', templateName: '', inspector: '', deadline: undefined })

const rules = {
  taskNo: [{ required: true, message: '请输入检验任务号' }],
  woNo: [{ required: true, message: '请输入工单号' }],
  templateName: [{ required: true, message: '请输入检验模板' }],
  inspector: [{ required: true, message: '请输入检验员' }],
  deadline: [{ required: true, message: '请选择截止日期' }],
}

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '任务号/工单号', width: '200px' },
  { label: '状态', name: 'status', type: 'select', placeholder: '全部', options: [{ label: '待检验', value: '待检验' }, { label: '检验中', value: '检验中' }, { label: '已完成', value: '已完成' }] },
]

const columns = [
  { title: '检验任务号', dataIndex: 'taskNo', key: 'taskNo' },
  { title: '工单号', dataIndex: 'woNo', key: 'woNo' },
  { title: '检验模板', dataIndex: 'templateName', key: 'templateName' },
  { title: '检验员', dataIndex: 'inspector', key: 'inspector' },
  { title: '截止日期', dataIndex: 'deadline', key: 'deadline' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<TaskItem>()
const modalOpen = ref(false)
const actionBar = [{ key: 'new', label: '新增任务', type: 'primary' }]
const handlePageAction = (key: string) => { if (key === 'new') openModal() }
const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }
const loadData = async () => {
  loading.value = true
  data.value = await fetchTasks({ keyword: searchForm.keyword, status: searchForm.status })
  pagination.total = data.value.length
  loading.value = false
}
const resetSearch = () => { searchForm.keyword = ''; searchForm.status = undefined; loadData() }
onMounted(loadData)
</script>

<template>
  <TablePage title="检验任务" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search><SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" /></template>
    <template #actions><ActionBar :actions="actionBar" @action="handlePageAction" /></template>
  </TablePage>
  <FormModal v-model:open="modalOpen" title="新增检验任务" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
