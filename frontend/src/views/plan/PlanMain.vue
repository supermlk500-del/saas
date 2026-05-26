<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchOrderItems, fetchOrders } from '@/api/order'
import { fetchBatches } from '@/api/batch/batch'
import { fetchEquipments } from '@/api/master/equipment'
import { fetchProcesses } from '@/api/master/process'
import {
  createPlan,
  fetchPlans,
  getPlan,
  patchPlanStatus,
  reschedulePlan,
  updatePlan,
  type PlanQuery,
  type PlanRescheduleRequest,
  type PlanStatusPatchRequest,
  type ProductionPlanCreateRequest,
  type ProductionPlanUpdateRequest,
} from '@/api/plan/plan'
import {
  patchPlanStepMachine,
  patchPlanStepStatus,
  updatePlanStep,
  type PlanStepMachinePatchRequest,
  type PlanStepUpdateRequest,
  type StatusPatchRequest,
} from '@/api/plan/planStep'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import {
  batchStatusOptions,
  machineStatusOptions,
  planStatusOptions,
  planStepStatusOptions,
} from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type {
  BatchItem,
  IdValue,
  MachineItem,
  OrderLineItem,
  OrderSummaryItem,
  PlanStepItem,
  ProcessRouteItem,
  ProductionPlanDetailItem,
  ProductionPlanItem,
} from '@/types/domain'

type PlanFormModel = {
  planId?: IdValue
  orderId?: IdValue
  orderItemId?: IdValue
  batchId?: IdValue
  routeId?: IdValue
  planStartTime: string
  planEndTime: string
  remark: string
  orderNo: string
  customerName: string
  productSummary: string
  batchNo: string
  routeName: string
}

type PlanStatusForm = {
  planId?: IdValue
  status?: string
  reason: string
}

type RescheduleForm = {
  planId?: IdValue
  rescheduleReason: string
  startTime: string
}

type StepEditForm = {
  planStepId?: IdValue
  planStartTime: string
  planEndTime: string
  planHours: number | null
  sequenceNo: number | null
  status?: string
  remark: string
}

type StepMachineForm = {
  planStepId?: IdValue
  machineId?: IdValue
}

type StepStatusForm = {
  planStepId?: IdValue
  status?: string
  reason: string
}

const router = useRouter()
const route = useRoute()

const searchForm = reactive({
  orderNo: '',
  customerName: '',
  batchId: '',
  status: undefined as string | undefined,
})

const searchFields = [
  { label: '订单号', name: 'orderNo', type: 'input' as const, placeholder: '请输入订单号', width: '180px' },
  { label: '客户', name: 'customerName', type: 'input' as const, placeholder: '请输入客户名称', width: '180px' },
  { label: '批次ID', name: 'batchId', type: 'input' as const, placeholder: '请输入批次ID', width: '180px' },
  {
    label: '计划状态',
    name: 'status',
    type: 'select' as const,
    placeholder: '全部',
    options: planStatusOptions,
    width: '160px',
  },
]

const columns = [
  { title: '计划ID', dataIndex: 'planId', key: 'planId', width: 100 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 180 },
  { title: '产品/规格', dataIndex: 'productSummary', key: 'productSummary', width: 220 },
  { title: '批次编号', dataIndex: 'batchNo', key: 'batchNo', width: 180 },
  { title: '路线名称', dataIndex: 'routeName', key: 'routeName', width: 180 },
  { title: '计划开始', dataIndex: 'planStartTime', key: 'planStartTime', width: 180 },
  { title: '计划结束', dataIndex: 'planEndTime', key: 'planEndTime', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '操作', key: 'action', width: 320, fixed: 'right' as const },
]

const planStepColumns = [
  { title: '序号', dataIndex: 'sequenceNo', key: 'sequenceNo', width: 80 },
  { title: '工序名称', dataIndex: 'stepName', key: 'stepName', width: 160 },
  { title: '设备', dataIndex: 'machineName', key: 'machineName', width: 160 },
  { title: '计划开始', dataIndex: 'planStartTime', key: 'planStartTime', width: 180 },
  { title: '计划结束', dataIndex: 'planEndTime', key: 'planEndTime', width: 180 },
  { title: '工时', dataIndex: 'planHours', key: 'planHours', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '操作', key: 'action', width: 220 },
]

const { data, loading, pagination } = useTable<ProductionPlanItem>()
const displayRows = computed(() =>
  data.value.filter((item) => {
    const hitOrderNo = !searchForm.orderNo || (item.orderNo || '').includes(searchForm.orderNo)
    const hitCustomer = !searchForm.customerName || (item.customerName || '').includes(searchForm.customerName)
    return hitOrderNo && hitCustomer
  }),
)

const planModalOpen = ref(false)
const planSubmitting = ref(false)
const planModalMode = ref<'create' | 'edit'>('create')
const planFormRef = ref()

const planStatusModalOpen = ref(false)
const planStatusSubmitting = ref(false)
const planStatusFormRef = ref()

const rescheduleModalOpen = ref(false)
const rescheduleSubmitting = ref(false)
const rescheduleFormRef = ref()

const detailDrawerOpen = ref(false)
const detailLoading = ref(false)
const currentPlanDetail = ref<ProductionPlanDetailItem | null>(null)

const stepEditModalOpen = ref(false)
const stepEditSubmitting = ref(false)
const stepEditFormRef = ref()

const stepMachineModalOpen = ref(false)
const stepMachineSubmitting = ref(false)
const stepMachineFormRef = ref()

const stepStatusModalOpen = ref(false)
const stepStatusSubmitting = ref(false)
const stepStatusFormRef = ref()

const routeOptions = ref<ProcessRouteItem[]>([])
const batchOptions = ref<BatchItem[]>([])
const machineOptions = ref<MachineItem[]>([])
const orderOptions = ref<OrderSummaryItem[]>([])
const orderItemOptions = ref<OrderLineItem[]>([])

const planForm = reactive<PlanFormModel>({
  orderNo: '',
  customerName: '',
  productSummary: '',
  planStartTime: '',
  planEndTime: '',
  remark: '',
  batchNo: '',
  routeName: '',
})

const planStatusForm = reactive<PlanStatusForm>({
  reason: '',
})

const rescheduleForm = reactive<RescheduleForm>({
  rescheduleReason: '',
  startTime: '',
})

const stepEditForm = reactive<StepEditForm>({
  planStartTime: '',
  planEndTime: '',
  planHours: null,
  sequenceNo: null,
  remark: '',
})

const stepMachineForm = reactive<StepMachineForm>({})

const stepStatusForm = reactive<StepStatusForm>({
  reason: '',
})

const planRules = {
  orderId: [{ required: true, message: '请选择订单' }],
  orderItemId: [{ required: true, message: '请选择订单明细' }],
  batchId: [{ required: true, message: '请选择批次' }],
  routeId: [{ required: true, message: '请选择工艺路线' }],
  planStartTime: [{ required: true, message: '请选择计划开始时间' }],
}

const planStatusRules = {
  status: [{ required: true, message: '请选择计划状态' }],
}

const rescheduleRules = {
  rescheduleReason: [{ required: true, message: '请输入重排原因' }],
}

const stepEditRules = {
  sequenceNo: [{ required: true, message: '请输入工序顺序' }],
}

const stepMachineRules = {
  machineId: [{ required: true, message: '请选择设备' }],
}

const stepStatusRules = {
  status: [{ required: true, message: '请选择工序状态' }],
}

const readyBatchOptions = computed(() =>
  batchOptions.value.filter((item) => item.status === 'READY').map((item) => ({
    label: `${item.batchNo} / ${item.supplier}`,
    value: item.batchId,
  })),
)

const routeSelectOptions = computed(() =>
  routeOptions.value.map((item) => ({
    label: item.routeName,
    value: item.routeId,
  })),
)

const orderSelectOptions = computed(() =>
  orderOptions.value.map((item) => ({
    label: `${item.orderNo} / ${item.customerName}`,
    value: item.orderId,
  })),
)

const orderItemSelectOptions = computed(() =>
  orderItemOptions.value.map((item) => ({
    label: [item.productCode || item.productName, item.specification, item.color].filter(Boolean).join(' / '),
    value: item.orderItemId,
  })),
)

const machineSelectOptions = computed(() =>
  machineOptions.value.map((item) => ({
    label: `${item.machineCode} / ${item.machineName}`,
    value: item.machineId,
  })),
)

const getPlanStatusMeta = (status?: string) =>
  planStatusOptions.find((item) => item.value === status)

const getStepStatusMeta = (status?: string) =>
  planStepStatusOptions.find((item) => item.value === status)

const getBatchStatusMeta = (status?: string) =>
  batchStatusOptions.find((item) => item.value === status)

const getMachineStatusMeta = (status?: string) =>
  machineStatusOptions.find((item) => item.value === status)

const hasIdValue = (value: unknown): value is IdValue =>
  value !== undefined && value !== null && String(value).trim() !== ''

const normalizeQueryId = (value: unknown): IdValue | undefined => {
  if (Array.isArray(value)) {
    return normalizeQueryId(value[0])
  }

  if (typeof value === 'string' || typeof value === 'number') {
    return value
  }

  return undefined
}

const loadData = async () => {
  loading.value = true

  try {
    const query: PlanQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      orderId: undefined,
      orderItemId: undefined,
      batchId: searchForm.batchId || undefined,
      status: searchForm.status,
    }

    const response = await fetchPlans(query)
    data.value = response.list
    pagination.total = response.total
  } finally {
    loading.value = false
  }
}

const loadPlanResources = async () => {
  const [batchResponse, routeResponse, machineResponse] = await Promise.all([
    fetchBatches({ pageNum: 1, pageSize: 200 }),
    fetchProcesses({ pageNum: 1, pageSize: 200, isActive: 1 }),
    fetchEquipments({ pageNum: 1, pageSize: 200 }),
  ])

  batchOptions.value = batchResponse.list
  routeOptions.value = routeResponse.list
  machineOptions.value = machineResponse.list

  try {
    const orderResponse = await fetchOrders({ pageNum: 1, pageSize: 100 })
    orderOptions.value = orderResponse.list
  } catch (error) {
    orderOptions.value = []
  }
}

const loadOrderItemOptions = async (orderId?: IdValue) => {
  if (!orderId) {
    orderItemOptions.value = []
    return
  }

  try {
    orderItemOptions.value = await fetchOrderItems(orderId)
  } catch (error) {
    orderItemOptions.value = []
  }
}

const reloadCurrentPlanDetail = async () => {
  if (!currentPlanDetail.value) {
    return
  }

  const response = await getPlan(currentPlanDetail.value.planId)
  currentPlanDetail.value = response.data
}

pagination.onChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  void loadData()
}

const resetSearch = () => {
  searchForm.orderNo = ''
  searchForm.customerName = ''
  searchForm.batchId = ''
  searchForm.status = undefined
  pagination.current = 1
  void loadData()
}

const resetPlanForm = () => {
  planForm.planId = undefined
  planForm.orderId = undefined
  planForm.orderItemId = undefined
  planForm.batchId = undefined
  planForm.routeId = undefined
  planForm.planStartTime = ''
  planForm.planEndTime = ''
  planForm.remark = ''
  planForm.orderNo = ''
  planForm.customerName = ''
  planForm.productSummary = ''
  planForm.batchNo = ''
  planForm.routeName = ''
  orderItemOptions.value = []
}

const openCreateModal = async () => {
  await loadPlanResources()
  planModalMode.value = 'create'
  resetPlanForm()
  planModalOpen.value = true
}

const openEditModal = (record: ProductionPlanItem) => {
  planModalMode.value = 'edit'
  planForm.planId = record.planId
  planForm.orderId = record.orderId
  planForm.orderItemId = record.orderItemId
  planForm.batchId = record.batchId
  planForm.routeId = record.routeId
  planForm.planStartTime = record.planStartTime
  planForm.planEndTime = record.planEndTime || ''
  planForm.remark = record.remark || ''
  planForm.orderNo = record.orderNo || String(record.orderId || '')
  planForm.customerName = record.customerName || ''
  planForm.productSummary = [record.productCode || record.productName, record.specification, record.color].filter(Boolean).join(' / ')
  planForm.batchNo = record.batchNo || String(record.batchId)
  planForm.routeName = record.routeName || String(record.routeId)
  planModalOpen.value = true
}

const openStatusModal = (record: ProductionPlanItem) => {
  planStatusForm.planId = record.planId
  planStatusForm.status = record.status
  planStatusForm.reason = ''
  planStatusModalOpen.value = true
}

const openRescheduleModal = (record: ProductionPlanItem) => {
  rescheduleForm.planId = record.planId
  rescheduleForm.rescheduleReason = ''
  rescheduleForm.startTime = record.planStartTime
  rescheduleModalOpen.value = true
}

const openDetailDrawerByPlanId = async (planId: IdValue) => {
  detailDrawerOpen.value = true
  detailLoading.value = true

  try {
    if (!machineOptions.value.length) {
      await loadPlanResources()
    }
    const response = await getPlan(planId)
    currentPlanDetail.value = response.data
  } finally {
    detailLoading.value = false
  }
}

const openDetailDrawer = async (record: ProductionPlanItem) => {
  await openDetailDrawerByPlanId(record.planId)
}

const openStepEditModal = (record: PlanStepItem) => {
  stepEditForm.planStepId = record.planStepId
  stepEditForm.planStartTime = record.planStartTime || ''
  stepEditForm.planEndTime = record.planEndTime || ''
  stepEditForm.planHours = record.planHours != null ? Number(record.planHours) : null
  stepEditForm.sequenceNo = record.sequenceNo ?? null
  stepEditForm.status = record.status
  stepEditForm.remark = record.remark || ''
  stepEditModalOpen.value = true
}

const openStepMachineModal = async (record: PlanStepItem) => {
  if (!machineOptions.value.length) {
    await loadPlanResources()
  }

  stepMachineForm.planStepId = record.planStepId
  stepMachineForm.machineId = record.machineId
  stepMachineModalOpen.value = true
}

const openStepStatusModal = (record: PlanStepItem) => {
  stepStatusForm.planStepId = record.planStepId
  stepStatusForm.status = record.status
  stepStatusForm.reason = ''
  stepStatusModalOpen.value = true
}

const handleOrderChange = async (value?: IdValue) => {
  planForm.orderId = value
  planForm.orderItemId = undefined
  const selectedOrder = orderOptions.value.find((item) => String(item.orderId) === String(value))
  planForm.orderNo = selectedOrder?.orderNo || ''
  planForm.customerName = selectedOrder?.customerName || ''
  planForm.productSummary = ''
  await loadOrderItemOptions(value)
}

const handleOrderItemChange = (value?: IdValue) => {
  planForm.orderItemId = value
  const selectedItem = orderItemOptions.value.find((item) => String(item.orderItemId) === String(value))
  planForm.productSummary = [selectedItem?.productCode || selectedItem?.productName, selectedItem?.specification, selectedItem?.color]
    .filter(Boolean)
    .join(' / ')
}

const jumpToGantt = (record: ProductionPlanItem) => {
  void router.push({
    path: '/schedule/board',
    query: { planId: String(record.planId) },
  })
}

const handlePlanSubmit = async () => {
  await planFormRef.value?.validate()

  planSubmitting.value = true
  try {
    if (planModalMode.value === 'create') {
      if (
        !hasIdValue(planForm.orderId) ||
        !hasIdValue(planForm.orderItemId) ||
        !hasIdValue(planForm.batchId) ||
        !hasIdValue(planForm.routeId)
      ) {
        return
      }

      const payload: ProductionPlanCreateRequest = {
        orderId: planForm.orderId,
        orderItemId: planForm.orderItemId,
        batchId: planForm.batchId,
        routeId: planForm.routeId,
        planStartTime: planForm.planStartTime,
        planEndTime: planForm.planEndTime || undefined,
        remark: planForm.remark.trim() || undefined,
      }

      await createPlan(payload)
      message.success('生产计划创建成功')
    } else if (hasIdValue(planForm.planId)) {
      const payload: ProductionPlanUpdateRequest = {
        planStartTime: planForm.planStartTime || undefined,
        planEndTime: planForm.planEndTime || undefined,
        remark: planForm.remark.trim() || undefined,
      }

      await updatePlan(planForm.planId, payload)
      message.success('生产计划更新成功')
    }

    planModalOpen.value = false
    await loadData()
    await reloadCurrentPlanDetail()
  } finally {
    planSubmitting.value = false
  }
}

const handlePlanStatusSubmit = async () => {
  await planStatusFormRef.value?.validate()

  planStatusSubmitting.value = true
  try {
    if (!hasIdValue(planStatusForm.planId)) {
      return
    }

    const payload: PlanStatusPatchRequest = {
      status: planStatusForm.status ?? 'DRAFT',
      reason: planStatusForm.reason.trim() || undefined,
    }

    await patchPlanStatus(planStatusForm.planId, payload)
    planStatusModalOpen.value = false
    message.success('计划状态更新成功')
    await loadData()
    await reloadCurrentPlanDetail()
  } finally {
    planStatusSubmitting.value = false
  }
}

const handleRescheduleSubmit = async () => {
  await rescheduleFormRef.value?.validate()

  rescheduleSubmitting.value = true
  try {
    if (!hasIdValue(rescheduleForm.planId)) {
      return
    }

    const payload: PlanRescheduleRequest = {
      rescheduleReason: rescheduleForm.rescheduleReason.trim(),
      startTime: rescheduleForm.startTime || undefined,
    }

    await reschedulePlan(rescheduleForm.planId, payload)
    rescheduleModalOpen.value = false
    message.success('计划重排成功')
    await loadData()
    await reloadCurrentPlanDetail()
  } finally {
    rescheduleSubmitting.value = false
  }
}

const handleStepEditSubmit = async () => {
  await stepEditFormRef.value?.validate()

  stepEditSubmitting.value = true
  try {
    if (!hasIdValue(stepEditForm.planStepId)) {
      return
    }

    const payload: PlanStepUpdateRequest = {
      planStartTime: stepEditForm.planStartTime || undefined,
      planEndTime: stepEditForm.planEndTime || undefined,
      planHours: stepEditForm.planHours,
      sequenceNo: stepEditForm.sequenceNo,
      status: stepEditForm.status || undefined,
      remark: stepEditForm.remark.trim() || undefined,
    }

    await updatePlanStep(stepEditForm.planStepId, payload)
    stepEditModalOpen.value = false
    message.success('工序计划更新成功')
    await reloadCurrentPlanDetail()
  } finally {
    stepEditSubmitting.value = false
  }
}

const handleStepMachineSubmit = async () => {
  await stepMachineFormRef.value?.validate()

  stepMachineSubmitting.value = true
  try {
    if (!hasIdValue(stepMachineForm.planStepId) || !hasIdValue(stepMachineForm.machineId)) {
      return
    }

    const payload: PlanStepMachinePatchRequest = {
      machineId: stepMachineForm.machineId,
    }

    await patchPlanStepMachine(stepMachineForm.planStepId, payload)
    stepMachineModalOpen.value = false
    message.success('工序设备分配成功')
    await reloadCurrentPlanDetail()
  } finally {
    stepMachineSubmitting.value = false
  }
}

const handleStepStatusSubmit = async () => {
  await stepStatusFormRef.value?.validate()

  stepStatusSubmitting.value = true
  try {
    if (!hasIdValue(stepStatusForm.planStepId)) {
      return
    }

    const payload: StatusPatchRequest = {
      status: stepStatusForm.status ?? 'PENDING',
      reason: stepStatusForm.reason.trim() || undefined,
    }

    await patchPlanStepStatus(stepStatusForm.planStepId, payload)
    stepStatusModalOpen.value = false
    message.success('工序状态更新成功')
    await reloadCurrentPlanDetail()
  } finally {
    stepStatusSubmitting.value = false
  }
}

watch(
  () => route.query.planId,
  async (planId) => {
    const normalizedPlanId = normalizeQueryId(planId)
    if (hasIdValue(normalizedPlanId)) {
      await openDetailDrawerByPlanId(normalizedPlanId)
    }
  },
  { immediate: true },
)

onMounted(() => {
  void loadData()
})
</script>

<template>
  <TablePage title="生产计划" :columns="columns" :data="displayRows" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #actions>
      <a-button type="primary" @click="openCreateModal">新建计划</a-button>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'orderNo'">
        {{ record.orderNo || '-' }}
      </template>
      <template v-else-if="column.key === 'customerName'">
        {{ record.customerName || '-' }}
      </template>
      <template v-else-if="column.key === 'productSummary'">
        {{ [record.productCode || record.productName, record.specification, record.color].filter(Boolean).join(' / ') || '-' }}
      </template>
      <template v-else-if="column.key === 'batchNo'">
        {{ record.batchNo || '-' }}
      </template>
      <template v-else-if="column.key === 'routeName'">
        {{ record.routeName || '-' }}
      </template>
      <template v-else-if="column.key === 'planEndTime'">
        {{ record.planEndTime || '-' }}
      </template>
      <template v-else-if="column.key === 'createTime'">
        {{ record.createTime || '-' }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="getPlanStatusMeta(record.status)?.color">
          {{ getPlanStatusMeta(record.status)?.label || record.status }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'remark'">
        {{ record.remark || '-' }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a-button type="link" @click="openDetailDrawer(record)">详情</a-button>
          <a-button type="link" @click="openEditModal(record)">编辑</a-button>
          <a-button type="link" @click="openStatusModal(record)">状态</a-button>
          <a-button type="link" @click="openRescheduleModal(record)">重排</a-button>
          <a-button type="link" @click="jumpToGantt(record)">甘特图</a-button>
        </a-space>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="planModalOpen"
    :title="planModalMode === 'create' ? '新建生产计划' : '编辑生产计划'"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="planSubmitting"
    width="760px"
    @ok="handlePlanSubmit"
  >
    <a-form ref="planFormRef" :model="planForm" :rules="planRules" layout="vertical">
      <div class="form-grid">
        <a-form-item v-if="planModalMode === 'create'" label="订单" name="orderId">
          <a-select
            v-model:value="planForm.orderId"
            :options="orderSelectOptions"
            placeholder="请选择订单"
            show-search
            option-filter-prop="label"
            @change="handleOrderChange"
          />
        </a-form-item>

        <a-form-item v-else label="订单">
          <a-input :value="`${planForm.orderNo || '-'} / ${planForm.customerName || '-'}`" disabled />
        </a-form-item>

        <a-form-item v-if="planModalMode === 'create'" label="订单明细" name="orderItemId">
          <a-select
            v-model:value="planForm.orderItemId"
            :options="orderItemSelectOptions"
            placeholder="请选择订单明细"
            show-search
            option-filter-prop="label"
            @change="handleOrderItemChange"
          />
        </a-form-item>

        <a-form-item v-else label="订单明细">
          <a-input :value="planForm.productSummary || '-'" disabled />
        </a-form-item>

        <a-form-item v-if="planModalMode === 'create'" label="批次" name="batchId">
          <a-select
            v-model:value="planForm.batchId"
            :options="readyBatchOptions"
            placeholder="请选择待排产批次"
            show-search
            option-filter-prop="label"
          />
        </a-form-item>

        <a-form-item v-else label="批次">
          <a-input :value="planForm.batchNo" disabled />
        </a-form-item>

        <a-form-item v-if="planModalMode === 'create'" label="工艺路线" name="routeId">
          <a-select
            v-model:value="planForm.routeId"
            :options="routeSelectOptions"
            placeholder="请选择工艺路线"
          />
        </a-form-item>

        <a-form-item v-else label="工艺路线">
          <a-input :value="planForm.routeName" disabled />
        </a-form-item>

        <a-form-item label="计划开始时间" name="planStartTime">
          <a-date-picker
            v-model:value="planForm.planStartTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择计划开始时间"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="计划结束时间" name="planEndTime">
          <a-date-picker
            v-model:value="planForm.planEndTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="可选"
            style="width: 100%"
          />
        </a-form-item>
      </div>

      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="planForm.remark" :rows="3" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="planStatusModalOpen"
    title="更新计划状态"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="planStatusSubmitting"
    width="520px"
    @ok="handlePlanStatusSubmit"
  >
    <a-form ref="planStatusFormRef" :model="planStatusForm" :rules="planStatusRules" layout="vertical">
      <a-form-item label="计划状态" name="status">
        <a-select
          v-model:value="planStatusForm.status"
          :options="planStatusOptions"
          placeholder="请选择计划状态"
        />
      </a-form-item>
      <a-form-item label="变更原因" name="reason">
        <a-textarea v-model:value="planStatusForm.reason" :rows="3" placeholder="可选" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="rescheduleModalOpen"
    title="计划重排"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="rescheduleSubmitting"
    width="560px"
    @ok="handleRescheduleSubmit"
  >
    <a-form ref="rescheduleFormRef" :model="rescheduleForm" :rules="rescheduleRules" layout="vertical">
      <a-form-item label="重排原因" name="rescheduleReason">
        <a-textarea v-model:value="rescheduleForm.rescheduleReason" :rows="3" placeholder="请输入重排原因" />
      </a-form-item>
      <a-form-item label="新计划起始" name="startTime">
        <a-date-picker
          v-model:value="rescheduleForm.startTime"
          show-time
          value-format="YYYY-MM-DD HH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          placeholder="可选"
          style="width: 100%"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-drawer
    v-model:open="detailDrawerOpen"
    title="计划详情"
    width="1180px"
    :destroy-on-close="true"
  >
    <a-spin :spinning="detailLoading">
      <div v-if="currentPlanDetail" class="plan-detail">
        <a-descriptions :column="4" bordered size="small">
          <a-descriptions-item label="计划ID">{{ currentPlanDetail.planId }}</a-descriptions-item>
          <a-descriptions-item label="订单号">{{ currentPlanDetail.orderInfo?.orderNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="客户">{{ currentPlanDetail.orderInfo?.customerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="订单明细">
            {{ [currentPlanDetail.orderItemInfo?.productCode || currentPlanDetail.orderItemInfo?.productName, currentPlanDetail.orderItemInfo?.specification, currentPlanDetail.orderItemInfo?.color].filter(Boolean).join(' / ') || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="批次编号">{{ currentPlanDetail.batchInfo.batchNo }}</a-descriptions-item>
          <a-descriptions-item label="工艺路线">{{ currentPlanDetail.routeInfo.routeName }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            {{ getPlanStatusMeta(currentPlanDetail.status)?.label || currentPlanDetail.status }}
          </a-descriptions-item>
          <a-descriptions-item label="计划开始">{{ currentPlanDetail.planStartTime }}</a-descriptions-item>
          <a-descriptions-item label="计划结束">{{ currentPlanDetail.planEndTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="批次状态">
            {{ getBatchStatusMeta(currentPlanDetail.batchInfo.status)?.label || currentPlanDetail.batchInfo.status || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="备注">{{ currentPlanDetail.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-table
          :columns="planStepColumns"
          :data-source="currentPlanDetail.planSteps"
          :pagination="false"
          row-key="planStepId"
          class="plan-step-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'machineName'">
              {{ record.machineName || '-' }}
            </template>
            <template v-else-if="column.key === 'planEndTime'">
              {{ record.planEndTime || '-' }}
            </template>
            <template v-else-if="column.key === 'planHours'">
              {{ record.planHours ?? '-' }}
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="getStepStatusMeta(record.status)?.color">
                {{ getStepStatusMeta(record.status)?.label || record.status }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'remark'">
              {{ record.remark || '-' }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" @click="openStepEditModal(record)">编辑</a-button>
                <a-button type="link" @click="openStepMachineModal(record)">分配设备</a-button>
                <a-button type="link" @click="openStepStatusModal(record)">状态</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </a-spin>
  </a-drawer>

  <a-modal
    v-model:open="stepEditModalOpen"
    title="编辑工序计划"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="stepEditSubmitting"
    width="760px"
    @ok="handleStepEditSubmit"
  >
    <a-form ref="stepEditFormRef" :model="stepEditForm" :rules="stepEditRules" layout="vertical">
      <div class="form-grid">
        <a-form-item label="计划开始" name="planStartTime">
          <a-date-picker
            v-model:value="stepEditForm.planStartTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="计划结束" name="planEndTime">
          <a-date-picker
            v-model:value="stepEditForm.planEndTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="计划工时" name="planHours">
          <a-input-number v-model:value="stepEditForm.planHours" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>

        <a-form-item label="工序顺序" name="sequenceNo">
          <a-input-number v-model:value="stepEditForm.sequenceNo" :min="1" style="width: 100%" />
        </a-form-item>

        <a-form-item label="工序状态" name="status">
          <a-select
            v-model:value="stepEditForm.status"
            :options="planStepStatusOptions"
            placeholder="请选择工序状态"
          />
        </a-form-item>
      </div>

      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="stepEditForm.remark" :rows="3" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="stepMachineModalOpen"
    title="分配设备"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="stepMachineSubmitting"
    width="520px"
    @ok="handleStepMachineSubmit"
  >
    <a-form ref="stepMachineFormRef" :model="stepMachineForm" :rules="stepMachineRules" layout="vertical">
      <a-form-item label="目标设备" name="machineId">
        <a-select
          v-model:value="stepMachineForm.machineId"
          :options="machineSelectOptions"
          placeholder="请选择设备"
          show-search
          option-filter-prop="label"
        />
      </a-form-item>
      <div class="machine-hint">
        当前仅展示设备基础信息；设备可分配性最终由后端 `PATCH /api/plan-steps/{planStepId}/machine`
        根据能力约束校验。
      </div>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="stepStatusModalOpen"
    title="更新工序状态"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="stepStatusSubmitting"
    width="520px"
    @ok="handleStepStatusSubmit"
  >
    <a-form ref="stepStatusFormRef" :model="stepStatusForm" :rules="stepStatusRules" layout="vertical">
      <a-form-item label="工序状态" name="status">
        <a-select
          v-model:value="stepStatusForm.status"
          :options="planStepStatusOptions"
          placeholder="请选择工序状态"
        />
      </a-form-item>
      <a-form-item label="变更说明" name="reason">
        <a-textarea v-model:value="stepStatusForm.reason" :rows="3" placeholder="可选" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.plan-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.plan-step-table {
  margin-top: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.machine-hint {
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
