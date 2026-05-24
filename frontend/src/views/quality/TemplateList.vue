<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { createQcItem, fetchQcItems, patchQcItemStatus, updateQcItem, type QcItemQuery, type QcItemUpsertRequest } from '@/api/quality/qcItem'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { enabledStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { QcItem } from '@/types/domain'

type TemplateForm = {
  qcItemId?: number | string
  qcItemCode: string
  qcItemName: string
  qcType: string
  unit: string
  standardMin: number | null
  standardMax: number | null
  isActive: number | undefined
  description: string
}

const searchForm = reactive({
  qcItemCode: '',
  qcItemName: '',
  qcType: '',
  isActive: undefined as number | undefined,
})

const searchFields = [
  { label: '模板编码', name: 'qcItemCode', type: 'input' as const, width: '160px' },
  { label: '模板名称', name: 'qcItemName', type: 'input' as const, width: '180px' },
  { label: '模板类型', name: 'qcType', type: 'input' as const, width: '160px' },
  { label: '状态', name: 'isActive', type: 'select' as const, options: enabledStatusOptions, width: '140px' },
]

const columns = [
  { title: '模板编码', dataIndex: 'qcItemCode', key: 'qcItemCode', width: 140 },
  { title: '模板名称', dataIndex: 'qcItemName', key: 'qcItemName', width: 180 },
  { title: '模板类型', dataIndex: 'qcType', key: 'qcType', width: 140 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 100 },
  { title: '标准下限', dataIndex: 'standardMin', key: 'standardMin', width: 100 },
  { title: '标准上限', dataIndex: 'standardMax', key: 'standardMax', width: 100 },
  { title: '状态', dataIndex: 'isActive', key: 'isActive', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const },
]

const { data, loading, pagination } = useTable<QcItem>()
const modalOpen = ref(false)
const submitting = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const formRef = ref()
const formModel = reactive<TemplateForm>({
  qcItemCode: '',
  qcItemName: '',
  qcType: '',
  unit: '',
  standardMin: null,
  standardMax: null,
  isActive: 1,
  description: '',
})

const rules = {
  qcItemCode: [{ required: true, message: '请输入模板编码' }],
  qcItemName: [{ required: true, message: '请输入模板名称' }],
  isActive: [{ required: true, message: '请选择状态' }],
}

const getStatusMeta = (value: number) =>
  enabledStatusOptions.find((item) => item.value === value)

const loadData = async () => {
  loading.value = true
  try {
    const query: QcItemQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      qcItemCode: searchForm.qcItemCode || undefined,
      qcItemName: searchForm.qcItemName || undefined,
      qcType: searchForm.qcType || undefined,
      isActive: searchForm.isActive,
    }
    const response = await fetchQcItems(query)
    data.value = response.list
    pagination.total = response.total
  } finally {
    loading.value = false
  }
}

pagination.onChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  void loadData()
}

const resetSearch = () => {
  searchForm.qcItemCode = ''
  searchForm.qcItemName = ''
  searchForm.qcType = ''
  searchForm.isActive = undefined
  pagination.current = 1
  void loadData()
}

const resetForm = () => {
  formModel.qcItemId = undefined
  formModel.qcItemCode = ''
  formModel.qcItemName = ''
  formModel.qcType = ''
  formModel.unit = ''
  formModel.standardMin = null
  formModel.standardMax = null
  formModel.isActive = 1
  formModel.description = ''
}

const openCreateModal = () => {
  modalMode.value = 'create'
  resetForm()
  modalOpen.value = true
}

const openEditModal = (record: QcItem) => {
  modalMode.value = 'edit'
  formModel.qcItemId = record.qcItemId
  formModel.qcItemCode = record.qcItemCode
  formModel.qcItemName = record.qcItemName
  formModel.qcType = record.qcType || ''
  formModel.unit = record.unit || ''
  formModel.standardMin = record.standardMin != null ? Number(record.standardMin) : null
  formModel.standardMax = record.standardMax != null ? Number(record.standardMax) : null
  formModel.isActive = record.isActive
  formModel.description = record.description || ''
  modalOpen.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: QcItemUpsertRequest = {
      qcItemCode: formModel.qcItemCode.trim(),
      qcItemName: formModel.qcItemName.trim(),
      qcType: formModel.qcType.trim() || undefined,
      unit: formModel.unit.trim() || undefined,
      standardMin: formModel.standardMin,
      standardMax: formModel.standardMax,
      isActive: formModel.isActive ?? 1,
      description: formModel.description.trim() || undefined,
    }
    if (modalMode.value === 'create') {
      await createQcItem(payload)
      message.success('检验模板新增成功')
    } else if (formModel.qcItemId) {
      await updateQcItem(formModel.qcItemId, payload)
      message.success('检验模板更新成功')
    }
    modalOpen.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

const handlePatchStatus = async (record: QcItem) => {
  const nextStatus = record.isActive === 1 ? 0 : 1
  await patchQcItemStatus(record.qcItemId, nextStatus)
  message.success(nextStatus === 1 ? '模板启用成功' : '模板停用成功')
  await loadData()
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <TablePage title="检验模板" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
    <template #actions>
      <a-button type="primary" @click="openCreateModal">新增模板</a-button>
    </template>
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'qcType'">
        {{ record.qcType || '-' }}
      </template>
      <template v-else-if="column.key === 'unit'">
        {{ record.unit || '-' }}
      </template>
      <template v-else-if="column.key === 'standardMin'">
        {{ record.standardMin ?? '-' }}
      </template>
      <template v-else-if="column.key === 'standardMax'">
        {{ record.standardMax ?? '-' }}
      </template>
      <template v-else-if="column.key === 'isActive'">
        <a-tag :color="getStatusMeta(record.isActive)?.color">{{ getStatusMeta(record.isActive)?.label || record.isActive }}</a-tag>
      </template>
      <template v-else-if="column.key === 'description'">
        {{ record.description || '-' }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a-button type="link" @click="openEditModal(record)">编辑</a-button>
          <a-button type="link" @click="handlePatchStatus(record)">{{ record.isActive === 1 ? '停用' : '启用' }}</a-button>
        </a-space>
      </template>
    </template>
  </TablePage>

  <a-modal v-model:open="modalOpen" :title="modalMode === 'create' ? '新增检验模板' : '编辑检验模板'" ok-text="保存" cancel-text="取消" :confirm-loading="submitting" width="720px" @ok="handleSubmit">
    <a-form ref="formRef" :model="formModel" :rules="rules" layout="vertical">
      <div class="form-grid">
        <a-form-item label="模板编码" name="qcItemCode"><a-input v-model:value="formModel.qcItemCode" /></a-form-item>
        <a-form-item label="模板名称" name="qcItemName"><a-input v-model:value="formModel.qcItemName" /></a-form-item>
        <a-form-item label="模板类型" name="qcType"><a-input v-model:value="formModel.qcType" /></a-form-item>
        <a-form-item label="单位" name="unit"><a-input v-model:value="formModel.unit" /></a-form-item>
        <a-form-item label="标准下限" name="standardMin"><a-input-number v-model:value="formModel.standardMin" :min="0" style="width: 100%" /></a-form-item>
        <a-form-item label="标准上限" name="standardMax"><a-input-number v-model:value="formModel.standardMax" :min="0" style="width: 100%" /></a-form-item>
        <a-form-item label="状态" name="isActive"><a-select v-model:value="formModel.isActive" :options="enabledStatusOptions" /></a-form-item>
      </div>
      <a-form-item label="描述" name="description"><a-textarea v-model:value="formModel.description" :rows="3" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
