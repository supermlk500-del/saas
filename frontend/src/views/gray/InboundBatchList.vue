<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { createBatch, fetchBatchResourcePool, updateBatch, type BatchQuery, type BatchUpsertRequest } from '@/api/batch/batch'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { useTable } from '@/hooks/useTable'
import type { BatchItem, IdValue } from '@/types/domain'

type BatchFormModel = {
  batchId?: IdValue
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
  { label: '资源批次编号', name: 'batchNo', type: 'input' as const, placeholder: '请输入批次编号', width: '220px' },
  { label: '供应商', name: 'supplier', type: 'input' as const, placeholder: '请输入供应商', width: '220px' },
]

const columns = [
  { title: '批次编号', dataIndex: 'batchNo', key: 'batchNo', width: 132 },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier', width: 140 },
  { title: '入厂时间', dataIndex: 'inDate', key: 'inDate', width: 142 },
  { title: '物料规格', key: 'materialSpec', width: 168 },
  { title: '库存分配', key: 'inventory', width: 174 },
  { title: '关联订单', dataIndex: 'linkedOrders', key: 'linkedOrders', width: 124 },
  { title: '排产状态', key: 'scheduleStatus', width: 118 },
  { title: '操作', key: 'action', width: 94 },
]

const { data, loading, pagination } = useTable<BatchItem>()
const router = useRouter()

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

const resourceStatusMap: Record<string, { label: string; color: string }> = {
  UNALLOCATED: { label: '待分配', color: 'blue' },
  PARTIALLY_ALLOCATED: { label: '部分分配', color: 'gold' },
  ALLOCATED: { label: '已分配', color: 'cyan' },
  IN_EXECUTION: { label: '执行中', color: 'processing' },
  CONSUMED: { label: '已消耗', color: 'success' },
  CLOSED: { label: '已关闭', color: 'error' },
}

const resourceRows = computed(() => data.value)

const loadData = async () => {
  loading.value = true

  try {
    const query: BatchQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      batchNo: searchForm.batchNo || undefined,
      supplier: searchForm.supplier || undefined,
    }

    const response = await fetchBatchResourcePool(query)
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

const getStatusOption = (status?: string, label?: string) => {
  if (!status) {
    return undefined
  }
  const local = resourceStatusMap[status]
  return local ? { ...local, label: label || local.label } : { label: label || status, color: 'default' }
}

const formatCompactWeight = (value?: number | null) =>
  value != null ? `${Number(value).toFixed(2)} kg` : '-'

const formatLinkedOrders = (record: BatchItem) =>
  (record.linkedOrders ?? [])
    .map((item) => item.orderNo || `订单 ${item.orderId}`)
    .join('、')

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

const jumpToOrderSchedulePool = () => {
  void router.push({ name: 'schedule-order-pool' })
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
      message.success('新增资源成功')
    } else if (formModel.batchId) {
      await updateBatch(formModel.batchId, payload)
      message.success('编辑资源成功')
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
  <TablePage
    title="来料资源池"
    :columns="columns"
    :data="resourceRows"
    :loading="loading"
    :pagination="pagination"
    row-key="batchId"
    table-layout="fixed"
    class="batch-resource-page"
  >
    <template #search>
      <div class="search-stack">
        <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
        <a-alert
          type="info"
          show-icon
          class="resource-alert"
          message="本页维护来料资源池。批次在这里是订单执行的资源对象，不是独立排产主体；真正的排产入口请前往“订单排产池”。"
        />
      </div>
    </template>

    <template #actions>
      <a-space>
        <a-button @click="jumpToOrderSchedulePool">去订单排产池</a-button>
        <a-button type="primary" @click="openCreateModal">新增来料资源</a-button>
      </a-space>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'batchNo'">
        <a-tooltip :title="record.note || record.batchNo" placement="topLeft">
          <span class="single-line-cell">{{ record.batchNo }}</span>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'supplier'">
        <a-tooltip :title="record.supplier" placement="topLeft">
          <span class="single-line-cell">{{ record.supplier }}</span>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'inDate'">
        <span class="date-cell">{{ record.inDate || '-' }}</span>
      </template>
      <template v-else-if="column.key === 'materialSpec'">
        <div class="stacked-cell">
          <strong>{{ record.composition || '未记录成分' }}</strong>
          <span>门幅 {{ record.width != null ? `${record.width} cm` : '-' }}</span>
        </div>
      </template>
      <template v-else-if="column.key === 'inventory'">
        <div class="inventory-cell">
          <span><em>总量</em>{{ formatCompactWeight(record.weight) }}</span>
          <span><em>已分配</em>{{ formatCompactWeight(record.allocatedWeight) }}</span>
          <span class="remaining"><em>剩余</em>{{ formatCompactWeight(record.remainingWeight) }}</span>
        </div>
      </template>
      <template v-else-if="column.key === 'linkedOrders'">
        <a-tooltip
          v-if="record.linkedOrders?.length"
          :title="formatLinkedOrders(record)"
        >
          <a-tag color="blue">{{ record.linkedOrderCount }} 个订单</a-tag>
        </a-tooltip>
        <span v-else class="muted-text">未分配订单</span>
      </template>
      <template v-else-if="column.key === 'scheduleStatus'">
        <div class="status-cell">
          <a-tag
            v-if="getStatusOption(record.resourceStatus, record.resourceStatusLabel)"
            :color="getStatusOption(record.resourceStatus, record.resourceStatusLabel)?.color"
          >
            {{ getStatusOption(record.resourceStatus, record.resourceStatusLabel)?.label }}
          </a-tag>
          <span :class="{ locked: record.lockedByPlan }">
            {{ record.lockedByPlan ? '计划占用中' : '当前未锁定' }}
          </span>
        </div>
      </template>
      <template v-else-if="column.key === 'action'">
        <div class="action-cell">
          <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
          <a-button type="link" size="small" @click="jumpToOrderSchedulePool">去排产</a-button>
        </div>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="modalOpen"
    :title="modalMode === 'create' ? '新增来料资源' : '编辑来料资源'"
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
.batch-resource-page :deep(.ant-table-cell) {
  overflow-wrap: normal;
  word-break: normal;
}

.batch-resource-page :deep(.ant-table-content) {
  overflow-x: clip !important;
}

.batch-resource-page :deep(.ant-table-tbody > tr > td) {
  padding-top: 12px;
  padding-bottom: 12px;
  vertical-align: middle;
}

.single-line-cell,
.date-cell {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.date-cell {
  font-size: 13px;
}

.stacked-cell,
.status-cell,
.action-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.stacked-cell strong {
  overflow: hidden;
  width: 100%;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.stacked-cell span,
.status-cell span {
  color: #64748b;
  font-size: 12px;
}

.inventory-cell {
  display: grid;
  gap: 3px;
  font-size: 12px;
}

.inventory-cell span {
  display: grid;
  grid-template-columns: 48px 1fr;
  white-space: nowrap;
}

.inventory-cell em {
  color: #94a3b8;
  font-style: normal;
}

.inventory-cell .remaining {
  color: #1677ff;
  font-weight: 600;
}

.status-cell .locked {
  color: #d97706;
}

.action-cell {
  gap: 0;
}

.action-cell :deep(.ant-btn) {
  height: 26px;
  padding-inline: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.resource-alert {
  margin-top: 12px;
}

.muted-text {
  color: #94a3b8;
}

.search-stack {
  display: flex;
  flex-direction: column;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
