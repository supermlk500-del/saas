<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchShifts, type ShiftItem } from '@/api/master/shift'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '班次名称', name: 'shiftName', type: 'input' as const, placeholder: '请输入班次名称' },
  { label: '开始时间', name: 'startTime', type: 'input' as const, placeholder: '如 08:00' },
  { label: '结束时间', name: 'endTime', type: 'input' as const, placeholder: '如 17:00' },
  {
    label: '类型',
    name: 'type',
    type: 'select' as const,
    placeholder: '请选择类型',
    options: [
      { label: '常日班', value: '常日班' },
      { label: '轮班', value: '轮班' },
    ],
  },
]

const formModel = reactive({ shiftName: '', startTime: '', endTime: '', type: '' })

const rules = {
  shiftName: [{ required: true, message: '请输入班次名称' }],
  startTime: [{ required: true, message: '请输入开始时间' }],
  endTime: [{ required: true, message: '请输入结束时间' }],
  type: [{ required: true, message: '请选择类型' }],
}

const searchForm = reactive({ keyword: '', type: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '班次名称', width: '200px' },
  {
    label: '类型',
    name: 'type',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '常日班', value: '常日班' },
      { label: '轮班', value: '轮班' },
    ],
  },
]

const columns = [
  { title: '班次名称', dataIndex: 'shiftName', key: 'shiftName' },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime' },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<ShiftItem>()

const modalOpen = ref(false)

const actionBar = [
  { key: 'new', label: '新增', type: 'primary' },
]

const handlePageAction = (key: string) => {
  if (key === 'new') openModal()
}

const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }

const loadData = async () => {
  loading.value = true
  data.value = await fetchShifts({ keyword: searchForm.keyword, type: searchForm.type })
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
  <TablePage title="班次管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>
  </TablePage>

  <FormModal v-model:open="modalOpen" title="新增班次" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
