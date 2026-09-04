<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { fetchEquipments } from '@/api/master/equipment'
import {
  createCapability,
  deleteCapability,
  fetchCapabilities,
  updateCapability,
  type CapabilityQuery,
  type CapabilityUpsertRequest,
} from '@/api/master/capability'
import { fetchProcessSteps } from '@/api/master/process'
import { enabledStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { IdValue, MachineItem, ProcessStepItem, StepMachineCapabilityItem } from '@/types/domain'

type CapabilityFormModel = {
  capId?: IdValue
  stepId?: IdValue
  machineId?: IdValue
  minWidth: number | null
  maxWidth: number | null
  maxSpeed: number | null
  maxBatchWeight: number | null
  isActive: number
}

type CapabilityRow = StepMachineCapabilityItem & {
  stepCode?: string
  stepName?: string
  machineCode?: string
  machineName?: string
}

const searchForm = reactive({
  stepId: undefined as IdValue | undefined,
  machineId: undefined as IdValue | undefined,
  isActive: undefined as number | undefined,
})

const processSteps = ref<ProcessStepItem[]>([])
const machines = ref<MachineItem[]>([])
const modalOpen = ref(false)
const submitting = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const formRef = ref()
const formModel = reactive<CapabilityFormModel>({
  minWidth: 150,
  maxWidth: 200,
  maxSpeed: 100,
  maxBatchWeight: 3000,
  isActive: 1,
})

const columns = [
  { title: '工序', key: 'step', width: 210 },
  { title: '设备', key: 'machine', width: 230 },
  { title: '门幅范围(cm)', key: 'widthRange', width: 150 },
  { title: '最大速度', dataIndex: 'maxSpeed', key: 'maxSpeed', width: 110 },
  { title: '最大批重(kg)', dataIndex: 'maxBatchWeight', key: 'maxBatchWeight', width: 130 },
  { title: '状态', dataIndex: 'isActive', key: 'isActive', width: 90 },
  { title: '操作', key: 'action', width: 150 },
]

const { data, loading, pagination } = useTable<CapabilityRow>()

const stepOptions = computed(() => processSteps.value.map((item) => ({
  label: `${item.stepCode} / ${item.stepName}`,
  value: item.stepId,
})))

const machineOptions = computed(() => machines.value.map((item) => ({
  label: `${item.machineCode} / ${item.machineName}`,
  value: item.machineId,
})))

const searchFields = computed(() => [
  {
    label: '工序',
    name: 'stepId',
    type: 'select' as const,
    placeholder: '全部工序',
    options: stepOptions.value,
    width: '220px',
  },
  {
    label: '设备',
    name: 'machineId',
    type: 'select' as const,
    placeholder: '全部设备',
    options: machineOptions.value,
    width: '240px',
  },
  {
    label: '状态',
    name: 'isActive',
    type: 'select' as const,
    placeholder: '全部状态',
    options: enabledStatusOptions,
    width: '140px',
  },
])

const stepMap = computed(() => new Map(processSteps.value.map((item) => [String(item.stepId), item])))
const machineMap = computed(() => new Map(machines.value.map((item) => [String(item.machineId), item])))

const REFERENCE_PAGE_SIZE = 200

const rowWithLabels = (item: StepMachineCapabilityItem): CapabilityRow => {
  const step = stepMap.value.get(String(item.stepId))
  const machine = machineMap.value.get(String(item.machineId))
  return {
    ...item,
    stepCode: step?.stepCode,
    stepName: step?.stepName,
    machineCode: machine?.machineCode,
    machineName: machine?.machineName,
  }
}

const loadReferences = async () => {
  const [stepResult, machineResult] = await Promise.all([
    fetchProcessSteps({ pageNum: 1, pageSize: REFERENCE_PAGE_SIZE }),
    fetchEquipments({ pageNum: 1, pageSize: REFERENCE_PAGE_SIZE }),
  ])
  processSteps.value = stepResult.list
  machines.value = machineResult.list
}

const loadData = async () => {
  loading.value = true
  try {
    const query: CapabilityQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      stepId: searchForm.stepId,
      machineId: searchForm.machineId,
      isActive: searchForm.isActive,
    }
    const response = await fetchCapabilities(query)
    data.value = response.list.map(rowWithLabels)
    pagination.total = response.total
  } finally {
    loading.value = false
  }
}

const loadPage = async () => {
  await loadReferences()
  await loadData()
}

pagination.onChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  void loadData()
}

const resetSearch = () => {
  searchForm.stepId = undefined
  searchForm.machineId = undefined
  searchForm.isActive = undefined
  pagination.current = 1
  void loadData()
}

const resetForm = () => {
  formModel.capId = undefined
  formModel.stepId = undefined
  formModel.machineId = undefined
  formModel.minWidth = 150
  formModel.maxWidth = 200
  formModel.maxSpeed = 100
  formModel.maxBatchWeight = 3000
  formModel.isActive = 1
}

const openCreateModal = () => {
  modalMode.value = 'create'
  resetForm()
  modalOpen.value = true
}

const openEditModal = (record: CapabilityRow) => {
  modalMode.value = 'edit'
  formModel.capId = record.capId
  formModel.stepId = record.stepId
  formModel.machineId = record.machineId
  formModel.minWidth = record.minWidth ?? null
  formModel.maxWidth = record.maxWidth ?? null
  formModel.maxSpeed = record.maxSpeed ?? null
  formModel.maxBatchWeight = record.maxBatchWeight ?? null
  formModel.isActive = Number(record.isActive)
  modalOpen.value = true
}

const buildPayload = (): CapabilityUpsertRequest => ({
  stepId: formModel.stepId as IdValue,
  machineId: formModel.machineId as IdValue,
  minWidth: formModel.minWidth ?? undefined,
  maxWidth: formModel.maxWidth ?? undefined,
  maxSpeed: formModel.maxSpeed ?? undefined,
  maxBatchWeight: formModel.maxBatchWeight ?? undefined,
  isActive: formModel.isActive,
})

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (
    formModel.minWidth != null
    && formModel.maxWidth != null
    && formModel.minWidth > formModel.maxWidth
  ) {
    message.warning('最小门幅不能大于最大门幅')
    return
  }

  submitting.value = true
  try {
    const payload = buildPayload()
    if (modalMode.value === 'create') {
      await createCapability(payload)
      message.success('设备能力新增成功')
    } else if (formModel.capId != null) {
      await updateCapability(formModel.capId, payload)
      message.success('设备能力更新成功')
    }
    modalOpen.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = (record: CapabilityRow) => {
  Modal.confirm({
    title: '删除设备能力',
    content: `确定删除“${record.stepName || record.stepId} - ${record.machineName || record.machineId}”这条能力关系吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteCapability(record.capId)
      message.success('设备能力删除成功')
      await loadData()
    },
  })
}

onMounted(() => {
  void loadPage()
})
</script>

<template>
  <div class="capability-page">
    <div class="capability-hero">
      <div>
        <div class="capability-eyebrow">PROCESS CENTER / CAPABILITY MATRIX</div>
        <h1>设备能力管理</h1>
        <p>维护工序与设备的可生产关系，为订单排产和智能机台推荐提供基础数据。</p>
      </div>
      <div class="capability-summary">
        <span>当前能力记录</span>
        <strong>{{ pagination.total }}</strong>
        <small>建议每道生产工序至少配置一台启用设备</small>
      </div>
    </div>

    <TablePage
      title="设备能力列表"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      row-key="capId"
      table-layout="fixed"
      class="capability-table-page"
    >
      <template #search>
        <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
      </template>

      <template #actions>
        <a-button v-permission="'process:machine:edit'" type="primary" @click="openCreateModal">
          新增设备能力
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'step'">
          <div class="relation-cell">
            <strong>{{ record.stepName || '-' }}</strong>
            <span>{{ record.stepCode || record.stepId }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'machine'">
          <div class="relation-cell machine-relation">
            <strong>{{ record.machineName || '-' }}</strong>
            <span>{{ record.machineCode || record.machineId }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'widthRange'">
          {{ record.minWidth ?? '-' }} — {{ record.maxWidth ?? '-' }}
        </template>
        <template v-else-if="column.key === 'maxSpeed'">
          {{ record.maxSpeed ?? '-' }}
        </template>
        <template v-else-if="column.key === 'maxBatchWeight'">
          {{ record.maxBatchWeight ?? '-' }}
        </template>
        <template v-else-if="column.key === 'isActive'">
          <a-tag :color="record.isActive === 1 ? 'success' : 'default'">
            {{ record.isActive === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a-button v-permission="'process:machine:edit'" type="link" @click="openEditModal(record)">编辑</a-button>
            <a-button v-permission="'process:machine:edit'" type="link" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </TablePage>

    <a-modal
      v-model:open="modalOpen"
      :title="modalMode === 'create' ? '新增设备能力' : '编辑设备能力'"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="submitting"
      width="720px"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="formModel" layout="vertical">
        <div class="form-grid">
          <a-form-item label="工序" name="stepId" :rules="[{ required: true, message: '请选择工序' }]">
            <a-select
              v-model:value="formModel.stepId"
              :options="stepOptions"
              placeholder="请选择工序模板"
              show-search
              option-filter-prop="label"
            />
          </a-form-item>
          <a-form-item label="设备" name="machineId" :rules="[{ required: true, message: '请选择设备' }]">
            <a-select
              v-model:value="formModel.machineId"
              :options="machineOptions"
              placeholder="请选择设备"
              show-search
              option-filter-prop="label"
            />
          </a-form-item>
          <a-form-item label="最小门幅(cm)">
            <a-input-number v-model:value="formModel.minWidth" :min="0" :precision="2" style="width: 100%" />
          </a-form-item>
          <a-form-item label="最大门幅(cm)">
            <a-input-number v-model:value="formModel.maxWidth" :min="0" :precision="2" style="width: 100%" />
          </a-form-item>
          <a-form-item label="最大速度">
            <a-input-number v-model:value="formModel.maxSpeed" :min="0" :precision="2" style="width: 100%" />
          </a-form-item>
          <a-form-item label="最大批次重量(kg)">
            <a-input-number v-model:value="formModel.maxBatchWeight" :min="0" :precision="2" style="width: 100%" />
          </a-form-item>
          <a-form-item label="状态" name="isActive" :rules="[{ required: true, message: '请选择状态' }]">
            <a-select v-model:value="formModel.isActive" :options="enabledStatusOptions" />
          </a-form-item>
        </div>
        <div class="capability-form-hint">
          只有启用状态且满足订单门幅、批次重量约束的设备，才会被订单排产池推荐。
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.capability-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.capability-hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 28px;
  border: 1px solid #f2dfc8;
  border-radius: 18px;
  background:
    radial-gradient(circle at 85% 20%, rgba(255, 208, 149, 0.42), transparent 32%),
    linear-gradient(135deg, #fffaf3 0%, #f8fbff 100%);
  box-shadow: 0 12px 30px rgba(62, 78, 96, 0.08);
}

.capability-eyebrow {
  color: #c56b27;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.capability-hero h1 {
  margin: 8px 0 6px;
  color: #1f2937;
  font-size: 27px;
  line-height: 1.2;
}

.capability-hero p {
  max-width: 620px;
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.capability-summary {
  display: flex;
  min-width: 190px;
  flex-direction: column;
  justify-content: center;
  padding: 8px 0 8px 24px;
  border-left: 1px solid rgba(197, 107, 39, 0.2);
}

.capability-summary span,
.capability-summary small {
  color: #8a6d54;
}

.capability-summary strong {
  margin: 3px 0;
  color: #b85d1a;
  font-size: 32px;
  line-height: 1;
}

.capability-summary small {
  line-height: 1.5;
}

.relation-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.relation-cell strong,
.relation-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-cell strong {
  color: #26364a;
}

.relation-cell span {
  color: #8a98a9;
  font-size: 12px;
}

.machine-relation strong {
  color: #2f5265;
}

.capability-table-page :deep(.ant-table-cell) {
  overflow-wrap: normal;
  word-break: keep-all;
}

.capability-table-page :deep(.ant-table-tbody > tr > td) {
  padding-top: 14px;
  padding-bottom: 14px;
  vertical-align: middle;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.capability-form-hint {
  margin-top: 4px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fff8ed;
  color: #94612f;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .capability-hero {
    flex-direction: column;
  }

  .capability-summary {
    min-width: 0;
    padding: 14px 0 0;
    border-top: 1px solid rgba(197, 107, 39, 0.2);
    border-left: 0;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
