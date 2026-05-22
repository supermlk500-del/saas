<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchDefects, type DefectItem } from '@/api/quality/defect'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: '缺陷编码', name: 'defectCode', type: 'input' as const, placeholder: '请输入缺陷编码' },
  { label: '缺陷名称', name: 'defectName', type: 'input' as const, placeholder: '请输入缺陷名称' },
  { label: '分类', name: 'category', type: 'input' as const, placeholder: '请输入分类' },
  { label: '严重等级', name: 'level', type: 'input' as const, placeholder: '请输入严重等级' },
]

const formModel = reactive({ defectCode: '', defectName: '', category: '', level: '' })

const rules = {
  defectCode: [{ required: true, message: '请输入缺陷编码' }],
  defectName: [{ required: true, message: '请输入缺陷名称' }],
  category: [{ required: true, message: '请输入分类' }],
  level: [{ required: true, message: '请输入严重等级' }],
}

const searchForm = reactive({ keyword: '', category: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '编码/名称', width: '200px' },
  { label: '分类', name: 'category', type: 'select', placeholder: '全部', options: [{ label: '外观', value: '外观' }, { label: '尺寸', value: '尺寸' }, { label: '功能', value: '功能' }] },
]

const columns = [
  { title: '缺陷编码', dataIndex: 'defectCode', key: 'defectCode' },
  { title: '缺陷名称', dataIndex: 'defectName', key: 'defectName' },
  { title: '分类', dataIndex: 'category', key: 'category' },
  { title: '严重等级', dataIndex: 'level', key: 'level' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<DefectItem>()
const modalOpen = ref(false)
const actionBar = [{ key: 'new', label: '新增缺陷代码', type: 'primary' }]
const handlePageAction = (key: string) => { if (key === 'new') openModal() }
const openModal = () => { modalOpen.value = true }
const handleOk = () => { modalOpen.value = false }
const loadData = async () => {
  loading.value = true
  data.value = await fetchDefects({ keyword: searchForm.keyword, category: searchForm.category })
  pagination.total = data.value.length
  loading.value = false
}
const resetSearch = () => { searchForm.keyword = ''; searchForm.category = undefined; loadData() }
onMounted(loadData)
</script>

<template>
  <TablePage title="缺陷代码" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search><SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" /></template>
    <template #actions><ActionBar :actions="actionBar" @action="handlePageAction" /></template>
  </TablePage>
  <FormModal v-model:open="modalOpen" title="新增缺陷代码" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
