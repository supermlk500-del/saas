<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import { fetchProducts, type ProductItem } from '@/api/master/product'
import { useTable } from '@/hooks/useTable'
import ActionBar from '@/components/ActionBar.vue'
import FormModal from '@/components/FormModal.vue'

const formFields = [
  { label: '款号', name: 'code', type: 'input' as const, placeholder: '请输入款号' },
  { label: '款式名称', name: 'name', type: 'input' as const, placeholder: '请输入款式名称' },
  { label: '尺码/颜色', name: 'spec', type: 'input' as const, placeholder: '如 M码/白色/棉' },
]

const formModel = reactive({
  code: '',
  name: '',
  spec: '',
})

const rules = {
  code: [{ required: true, message: '请输入款号' }],
  name: [{ required: true, message: '请输入款式名称' }],
  spec: [{ required: true, message: '请输入尺码/颜色' }],
}

const searchForm = reactive({
  keyword: '',
  status: undefined as string | undefined,
})

const columns = [
  { title: '款号', dataIndex: 'code', key: 'code' },
  { title: '款式名称', dataIndex: 'name', key: 'name' },
  { title: '尺码/颜色', dataIndex: 'spec', key: 'spec' },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    customRender: ({ text }: { text: string }) =>
      text === '启用' ? '启用' : '停用',
  },
]

const data = ref<ProductItem[]>([])
const loading = ref(false)

const modalOpen = ref(false)

const actionBar = [
  { key: 'new', label: '新增', type: 'primary' },
  { key: 'import', label: '导入' },
]

const handlePageAction = (key: string) => {
  if (key === 'new') {
    openModal()
  }
}

const openModal = () => {
  modalOpen.value = true
}

const handleOk = () => {
  modalOpen.value = false
}

const loadData = async () => {
  loading.value = true
  data.value = await fetchProducts()
  loading.value = false
}

onMounted(loadData)
</script>

<template>
  <TablePage title="款式管理" :columns="columns" :data="data" :loading="loading">
    <template #search>
      <a-form layout="inline" :model="searchForm">
        <a-form-item label="关键字">
          <a-input v-model:value="searchForm.keyword" placeholder="名称/编码" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="全部" style="width: 120px">
            <a-select-option value="启用">启用</a-select-option>
            <a-select-option value="停用">停用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary">搜索</a-button>
          <a-button style="margin-left: 8px">重置</a-button>
        </a-form-item>
      </a-form>
    </template>

    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>
  </TablePage>

  <FormModal v-model:open="modalOpen" title="新增款式" :model="formModel" :rules="rules" :fields="formFields"
    @ok="handleOk" />

</template>
