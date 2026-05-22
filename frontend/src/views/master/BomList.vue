<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchBoms, type BomItem } from '@/api/master/bom'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'

const formFields = [
  { label: 'BOM编码', name: 'bomCode', type: 'input' as const, placeholder: '请输入BOM编码' },
  { label: '款式名称', name: 'productName', type: 'input' as const, placeholder: '请输入款式名称' },
  { label: '版本', name: 'version', type: 'input' as const, placeholder: '请输入版本号' },
]

const formModel = reactive({ bomCode: '', productName: '', version: '' })

const rules = {
  bomCode: [{ required: true, message: '请输入BOM编码' }],
  productName: [{ required: true, message: '请输入款式名称' }],
  version: [{ required: true, message: '请输入版本号' }],
}

const searchForm = reactive({ keyword: '', status: undefined as string | undefined })

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input', placeholder: '编码/款式名', width: '200px' },
  {
    label: '状态',
    name: 'status',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '生效', value: '生效' },
      { label: '废弃', value: '废弃' },
    ],
  },
]

const columns = [
  { title: 'BOM编码', dataIndex: 'bomCode', key: 'bomCode' },
  { title: '款式名称', dataIndex: 'productName', key: 'productName' },
  { title: '版本', dataIndex: 'version', key: 'version' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<BomItem>()

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
  data.value = await fetchBoms({ keyword: searchForm.keyword, status: searchForm.status })
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
  <TablePage title="BOM管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>
  </TablePage>

  <FormModal v-model:open="modalOpen" title="新增BOM" :model="formModel" :rules="rules" :fields="formFields" @ok="handleOk" />
</template>
