<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { createBatch, fetchBatches, updateBatch, type BatchQuery, type BatchUpsertRequest } from '@/api/batch/batch'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { batchStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { BatchItem } from '@/types/domain'

type BatchFormModel = {
  batchId?: number
  batchNo: string
  supplier: string
  inDate: string
  weight: number | null
  width: number | null
  composition: string
  note: string
}

const searchForm = reactive({
  batchNo: '',
  supplier: '',
})

const searchFields = [
  { label: '批次编号', name: 'batchNo', type: 'input' as const, placeholder: '请输入批次编号', width: '220px' },
  { label: '供应商', name: 'supplier', type: 'input' as const, placeholder: '请输入供应商', width: '220px' },
]

const columns = [
  { title: '批次编号', dataIndex: 'batchNo', key: 'batchNo', width: 180 },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier', width: 220 },
  { title: '入厂时间', dataIndex: 'inDate', key: 'inDate', width: 180 },
  { title: '重量(kg)', dataIndex: 'weight', key: 'weight', width: 120 },
  { title: '门幅(cm)', dataIndex: 'width', key: 'width', width: 120 },
  { title: '成分', dataIndex: 'composition', key: 'composition' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const },
]

const { data, loading, pagination } = useTable<BatchItem>()

const modalOpen = ref(false)
const submitting = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const formRef = ref()

const formModel = reactive<BatchFormModel>({
  batchNo: '',
  supplier: '',
  inDate: '',
  weight: null,
  width: null,
  composition: '',
  note: '',
})

const rules = {
  batchNo: [{ required: true, message: '请输入批次编号' }],
  supplier: [{ required: true, message: '请输入供应商' }],
  inDate: [{ required: true, message: '请选择入厂时间' }],
  weight: [
    {
      validator: async (_rule: unknown, value: number | null) => {
        if (value != null && value < 0) {
          throw new Error('重量不能小于 0')
        }
      },
    },
  ],
  width: [
    {
      validator: async (_rule: unknown, value: number | null) => {
        if (value != null && value < 0) {
          throw new Error('门幅不能小于 0')
        }
      },
    },
  ],
}

const loadData = async () => {
  loading.value = true

  try {
    const query: BatchQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      batchNo: searchForm.batchNo || undefined,
      supplier: searchForm.supplier || undefined,
    }

    const response = await fetchBatches(query)
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
  searchForm.batchNo = ''
  searchForm.supplier = ''
  pagination.current = 1
  void loadData()
}

const getStatusOption = (status?: string) =>
  batchStatusOptions.find((item) => item.value === status)

const resetFormModel = () => {
  formModel.batchId = undefined
  formModel.batchNo = ''
  formModel.supplier = ''
  formModel.inDate = ''
  formModel.weight = null
  formModel.width = null
  formModel.composition = ''
  formModel.note = ''
}

const openCreateModal = () => {
  modalMode.value = 'create'
  resetFormModel()
  modalOpen.value = true
}

const openEditModal = (record: BatchItem) => {
  modalMode.value = 'edit'
  formModel.batchId = record.batchId
  formModel.batchNo = record.batchNo
  formModel.supplier = record.supplier
  formModel.inDate = record.inDate
  formModel.weight = record.weight != null ? Number(record.weight) : null
  formModel.width = record.width != null ? Number(record.width) : null
  formModel.composition = record.composition || ''
  formModel.note = record.note || ''
  modalOpen.value = true
}

const buildPayload = (): BatchUpsertRequest => ({
  batchNo: formModel.batchNo.trim(),
  supplier: formModel.supplier.trim(),
  inDate: formModel.inDate,
  weight: formModel.weight,
  width: formModel.width,
  composition: formModel.composition.trim() || undefined,
  note: formModel.note.trim() || undefined,
})

const handleSubmit = async () => {
  await formRef.value?.validate()

  submitting.value = true
  try {
    const payload = buildPayload()

    if (modalMode.value === 'create') {
      await createBatch(payload)
      message.success('新增来料成功')
    } else if (formModel.batchId) {
      await updateBatch(formModel.batchId, payload)
      message.success('编辑来料成功')
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
  <TablePage title="来料批次" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #actions>
      <a-button type="primary" @click="openCreateModal">新增来料</a-button>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'weight'">
        {{ record.weight ?? '-' }}
      </template>
      <template v-else-if="column.key === 'width'">
        {{ record.width ?? '-' }}
      </template>
      <template v-else-if="column.key === 'composition'">
        {{ record.composition || '-' }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag v-if="getStatusOption(record.status)" :color="getStatusOption(record.status)?.color">
          {{ getStatusOption(record.status)?.label }}
        </a-tag>
        <span v-else>{{ record.status || '-' }}</span>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-button type="link" @click="openEditModal(record)">编辑</a-button>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="modalOpen"
    :title="modalMode === 'create' ? '新增来料' : '编辑来料'"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="submitting"
    width="720px"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form ref="formRef" :model="formModel" :rules="rules" layout="vertical">
      <div class="form-grid">
        <a-form-item label="批次编号" name="batchNo">
          <a-input v-model:value="formModel.batchNo" placeholder="请输入批次编号" />
        </a-form-item>

        <a-form-item label="供应商" name="supplier">
          <a-input v-model:value="formModel.supplier" placeholder="请输入供应商" />
        </a-form-item>

        <a-form-item label="入厂时间" name="inDate">
          <a-date-picker
            v-model:value="formModel.inDate"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择入厂时间"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="重量(kg)" name="weight">
          <a-input-number
            v-model:value="formModel.weight"
            :min="0"
            :precision="2"
            placeholder="请输入重量"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="门幅(cm)" name="width">
          <a-input-number
            v-model:value="formModel.width"
            :min="0"
            :precision="2"
            placeholder="请输入门幅"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="成分" name="composition">
          <a-input v-model:value="formModel.composition" placeholder="请输入成分" />
        </a-form-item>
      </div>

      <a-form-item label="备注" name="note">
        <a-textarea v-model:value="formModel.note" :rows="3" placeholder="请输入备注" />
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
