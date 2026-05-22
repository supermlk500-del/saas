<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchTrims, type TrimItem } from '@/api/master/trim'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '辅料编码', name: 'code', type: 'input' as const, placeholder: '请输入辅料编码' },
  { label: '辅料名称', name: 'name', type: 'input' as const, placeholder: '请输入辅料名称' },
  { label: '类型', name: 'type', type: 'input' as const, placeholder: '如 拉链/纽扣/罗纹' },
  { label: '供应商', name: 'supplier', type: 'input' as const, placeholder: '请输入供应商' },
]

const formModel = reactive({ code: '', name: '', type: '', supplier: '' })

const rules = {
  code: [{ required: true, message: '请输入辅料编码' }],
  name: [{ required: true, message: '请输入辅料名称' }],
  type: [{ required: true, message: '请输入类型' }],
  supplier: [{ required: true, message: '请输入供应商' }],
}

const searchForm = reactive({ keyword: '', type: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '编码/名称', width: '200px' },
  { label: '类型', name: 'type', type: 'select', placeholder: '全部', options: [{ label: '拉链', value: '拉链' }, { label: '纽扣', value: '纽扣' }, { label: '罗纹', value: '罗纹' }] },
]

const columns = [
  { title: '辅料编码', dataIndex: 'code', key: 'code' },
  { title: '辅料名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<TrimItem>()
const modalOpen = ref(false)
const actionBar = [{ key: 'new', label: '新增辅料', type: 'primary' }]
const handlePageAction = (key: string) => { if (key === 'new') openModal() }
const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }
const loadData = async () => {
  loading.value = true
  data.value = await fetchTrims({ keyword: searchForm.keyword, type: searchForm.type })
  pagination.total = data.value.length
  loading.value = false
}
const resetSearch = () => { searchForm.keyword = ''; searchForm.type = undefined; loadData() }
onMounted(loadData)
</script>

<template>
  <TablePage title="辅料管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search><SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" /></template>
    <template #actions><ActionBar :actions="actionBar" @action="handlePageAction" /></template>
  </TablePage>
  <FormModal v-model:open="modalOpen" title="新增辅料" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
