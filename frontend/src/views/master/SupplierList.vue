<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchSuppliers, type SupplierItem } from '@/api/master/supplier'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '供应商编码', name: 'code', type: 'input' as const, placeholder: '请输入供应商编码' },
  { label: '供应商名称', name: 'name', type: 'input' as const, placeholder: '请输入供应商名称' },
  { label: '联系人', name: 'contact', type: 'input' as const, placeholder: '请输入联系人' },
  { label: '电话', name: 'phone', type: 'input' as const, placeholder: '请输入电话' },
]

const formModel = reactive({ code: '', name: '', contact: '', phone: '' })

const rules = {
  code: [{ required: true, message: '请输入供应商编码' }],
  name: [{ required: true, message: '请输入供应商名称' }],
  contact: [{ required: true, message: '请输入联系人' }],
  phone: [{ required: true, message: '请输入电话' }],
}

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '编码/名称', width: '200px' },
  { label: '状态', name: 'status', type: 'select', placeholder: '全部', options: [{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }] },
]

const columns = [
  { title: '供应商编码', dataIndex: 'code', key: 'code' },
  { title: '供应商名称', dataIndex: 'name', key: 'name' },
  { title: '联系人', dataIndex: 'contact', key: 'contact' },
  { title: '电话', dataIndex: 'phone', key: 'phone' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<SupplierItem>()
const modalOpen = ref(false)
const actionBar = [{ key: 'new', label: '新增供应商', type: 'primary' }]
const handlePageAction = (key: string) => { if (key === 'new') openModal() }
const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }
const loadData = async () => {
  loading.value = true
  data.value = await fetchSuppliers({ keyword: searchForm.keyword, status: searchForm.status })
  pagination.total = data.value.length
  loading.value = false
}
const resetSearch = () => { searchForm.keyword = ''; searchForm.status = undefined; loadData() }
onMounted(loadData)
</script>

<template>
  <TablePage title="供应商管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search><SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" /></template>
    <template #actions><ActionBar :actions="actionBar" @action="handlePageAction" /></template>
  </TablePage>
  <FormModal v-model:open="modalOpen" title="新增供应商" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
