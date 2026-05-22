<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  createEquipment,
  fetchEquipments,
  updateEquipment,
  type EquipmentQuery,
  type MachineUpsertRequest,
} from '@/api/master/equipment'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { machineStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { MachineItem } from '@/types/domain'

type MachineFormModel = {
  machineId?: number
  machineCode: string
  machineName: string
  machineType: string
  description: string
  status: string | undefined
}

const searchForm = reactive({
  machineCode: '',
  machineName: '',
  machineType: '',
  status: undefined as string | undefined,
})

const searchFields = [
  { label: '设备编码', name: 'machineCode', type: 'input' as const, placeholder: '请输入设备编码', width: '180px' },
  { label: '设备名称', name: 'machineName', type: 'input' as const, placeholder: '请输入设备名称', width: '180px' },
  { label: '设备类型', name: 'machineType', type: 'input' as const, placeholder: '请输入设备类型', width: '180px' },
  {
    label: '状态',
    name: 'status',
    type: 'select' as const,
    placeholder: '全部',
    options: machineStatusOptions,
    width: '160px',
  },
]

const columns = [
  { title: '设备ID', dataIndex: 'machineId', key: 'machineId', width: 100 },
  { title: '设备编码', dataIndex: 'machineCode', key: 'machineCode', width: 160 },
  { title: '设备名称', dataIndex: 'machineName', key: 'machineName', width: 180 },
  { title: '设备类型', dataIndex: 'machineType', key: 'machineType', width: 180 },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const },
]

const { data, loading, pagination } = useTable<MachineItem>()

const modalOpen = ref(false)
const submitting = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const formRef = ref()

const formModel = reactive<MachineFormModel>({
  machineCode: '',
  machineName: '',
  machineType: '',
  description: '',
  status: 'IDLE',
})

const rules = {
  machineCode: [{ required: true, message: '请输入设备编码' }],
  machineName: [{ required: true, message: '请输入设备名称' }],
  status: [{ required: true, message: '请选择设备状态' }],
}

const loadData = async () => {
  loading.value = true

  try {
    const query: EquipmentQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      machineCode: searchForm.machineCode || undefined,
      machineName: searchForm.machineName || undefined,
      machineType: searchForm.machineType || undefined,
      status: searchForm.status,
    }

    const response = await fetchEquipments(query)
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
  searchForm.machineCode = ''
  searchForm.machineName = ''
  searchForm.machineType = ''
  searchForm.status = undefined
  pagination.current = 1
  void loadData()
}

const getStatusOption = (status?: string) =>
  machineStatusOptions.find((item) => item.value === status)

const resetFormModel = () => {
  formModel.machineId = undefined
  formModel.machineCode = ''
  formModel.machineName = ''
  formModel.machineType = ''
  formModel.description = ''
  formModel.status = 'IDLE'
}

const openCreateModal = () => {
  modalMode.value = 'create'
  resetFormModel()
  modalOpen.value = true
}

const openEditModal = (record: MachineItem) => {
  modalMode.value = 'edit'
  formModel.machineId = record.machineId
  formModel.machineCode = record.machineCode
  formModel.machineName = record.machineName
  formModel.machineType = record.machineType || ''
  formModel.description = record.description || ''
  formModel.status = record.status
  modalOpen.value = true
}

const buildPayload = (): MachineUpsertRequest => ({
  machineCode: formModel.machineCode.trim(),
  machineName: formModel.machineName.trim(),
  machineType: formModel.machineType.trim() || undefined,
  description: formModel.description.trim() || undefined,
  status: formModel.status || 'IDLE',
})

const handleSubmit = async () => {
  await formRef.value?.validate()

  submitting.value = true
  try {
    const payload = buildPayload()

    if (modalMode.value === 'create') {
      await createEquipment(payload)
      message.success('新增设备成功')
    } else if (formModel.machineId) {
      await updateEquipment(formModel.machineId, payload)
      message.success('编辑设备成功')
    }

    modalOpen.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  modalOpen.value = false
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <TablePage title="设备管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #actions>
      <a-button type="primary" @click="openCreateModal">新增设备</a-button>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'machineType'">
        {{ record.machineType || '-' }}
      </template>
      <template v-else-if="column.key === 'description'">
        {{ record.description || '-' }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="getStatusOption(record.status)?.color">
          {{ getStatusOption(record.status)?.label || record.status }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'createTime'">
        {{ record.createTime || '-' }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-button type="link" @click="openEditModal(record)">编辑</a-button>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="modalOpen"
    :title="modalMode === 'create' ? '新增设备' : '编辑设备'"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="submitting"
    width="680px"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form ref="formRef" :model="formModel" :rules="rules" layout="vertical">
      <div class="form-grid">
        <a-form-item label="设备编码" name="machineCode">
          <a-input v-model:value="formModel.machineCode" placeholder="请输入设备编码" />
        </a-form-item>

        <a-form-item label="设备名称" name="machineName">
          <a-input v-model:value="formModel.machineName" placeholder="请输入设备名称" />
        </a-form-item>

        <a-form-item label="设备类型" name="machineType">
          <a-input v-model:value="formModel.machineType" placeholder="请输入设备类型" />
        </a-form-item>

        <a-form-item label="设备状态" name="status">
          <a-select
            v-model:value="formModel.status"
            :options="machineStatusOptions"
            placeholder="请选择设备状态"
          />
        </a-form-item>
      </div>

      <a-form-item label="描述" name="description">
        <a-textarea v-model:value="formModel.description" :rows="4" placeholder="请输入设备描述" />
      </a-form-item>
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
