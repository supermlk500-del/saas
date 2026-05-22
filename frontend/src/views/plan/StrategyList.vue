<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchStrategies, type StrategyItem } from '@/api/plan/strategy'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '策略名称', name: 'name', type: 'input' as const, placeholder: '请输入策略名称' },
  { label: '规则描述', name: 'rule', type: 'input' as const, placeholder: '请输入规则描述' },
  { label: '优先级', name: 'priority', type: 'input' as const, placeholder: '请输入优先级数字' },
]

const formModel = reactive({ name: '', rule: '', priority: '' })

const rules = {
  name: [{ required: true, message: '请输入策略名称' }],
  rule: [{ required: true, message: '请输入规则描述' }],
  priority: [{ required: true, message: '请输入优先级' }],
}

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '名称/规则', width: '200px' },
  {
    label: '状态',
    name: 'status',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '启用', value: '启用' },
      { label: '停用', value: '停用' },
    ],
  },
]

const columns = [
  { title: '策略名称', dataIndex: 'name', key: 'name' },
  { title: '规则描述', dataIndex: 'rule', key: 'rule' },
  { title: '优先级', dataIndex: 'priority', key: 'priority' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<StrategyItem>()

const modalOpen = ref(false)

const actionBar = [
  { key: 'new', label: '新增策略', type: 'primary' },
]

const handlePageAction = (key: string) => {
  if (key === 'new') openModal()
}

const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }

const loadData = async () => {
  loading.value = true
  data.value = await fetchStrategies({ keyword: searchForm.keyword, status: searchForm.status })
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
  <TablePage title="排产策略" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>
  </TablePage>

  <FormModal v-model:open="modalOpen" title="新增排产策略" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
