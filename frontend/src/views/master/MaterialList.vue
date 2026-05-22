<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchMaterials, type MaterialItem } from '@/api/master/material'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '面料编码', name: 'code', type: 'input' as const, placeholder: '请输入面料编码' },
  { label: '面料名称', name: 'name', type: 'input' as const, placeholder: '请输入面料名称' },
  { label: '类型', name: 'type', type: 'input' as const, placeholder: '面料/辅料' },
  { label: '单位', name: 'unit', type: 'input' as const, placeholder: '如 米/颗/条' },
]

const formModel = reactive({ code: '', name: '', type: '', unit: '' })

const rules = {
  code: [{ required: true, message: '请输入面料编码' }],
  name: [{ required: true, message: '请输入面料名称' }],
  type: [{ required: true, message: '请输入类型' }],
  unit: [{ required: true, message: '请输入单位' }],
}

const searchForm = reactive({ keyword: '', type: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '编码/名称', width: '200px' },
  {
    label: '类型',
    name: 'type',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '面料', value: '面料' },
      { label: '辅料', value: '辅料' },
    ],
  },
]

const columns = [
  { title: '面料编码', dataIndex: 'code', key: 'code' },
  { title: '面料名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '单位', dataIndex: 'unit', key: 'unit' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<MaterialItem>()

const modalOpen = ref(false)

const actionBar = [
  { key: 'new', label: '新增', type: 'primary' },
  { key: 'import', label: '导入' },
]

const handlePageAction = (key: string) => {
  if (key === 'new') openModal()
}

const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }

const loadData = async () => {
  loading.value = true
  data.value = await fetchMaterials({ keyword: searchForm.keyword, type: searchForm.type })
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
  <TablePage title="面料管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>
  </TablePage>

  <FormModal v-model:open="modalOpen" title="新增面料" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
