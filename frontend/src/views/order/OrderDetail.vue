<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { fetchBatchResourcePool } from '@/api/batch/batch'
import {
  createOrderBatchLink,
  createOrderItem,
  deleteOrderBatchLink,
  deleteOrderItem,
  type OrderBatchLinkUpsertRequest,
  type OrderItemUpsertRequest,
  updateOrderBatchLink,
  updateOrderItem,
} from '@/api/order/order'
import { fetchOrderDetail, buildOrderPlanStepGroups, type OrderPlanStepGroup } from '@/api/order/orderDetail'
import {
  exceptionStatusOptions,
  orderStatusOptions,
  planStatusOptions,
  planStepStatusOptions,
  resultJudgeOptions,
} from '@/constants/dictionaries'
import type {
  BatchItem,
  IdValue,
  OrderBatchLinkItem,
  OrderDetailAggregate,
  OrderLineItem,
  OrderPlanSummaryItem,
} from '@/types/domain'

type RouteStepFlowItem = {
  key: string
  stepName: string
  sequenceNo: number | null
}

type MachineTimelineRow = {
  machineId: string
  machineCode?: string
  machineName?: string
  machineType?: string
  status?: string
  occupiedRange: string
  stepNames: string
}

type OrderItemForm = {
  productCode: string
  productName: string
  specification: string
  color: string
  quantity: number | null
  unit: string
  requiredWidth: number | null
  requiredWeight: number | null
  remark: string
}

type BatchLinkForm = {
  orderItemId?: IdValue
  batchId?: IdValue
  allocatedWeight: number | null
  allocatedQuantity: number | null
  remark: string
}

const route = useRoute()
const router = useRouter()

const orderId = route.params.id as string
const loading = ref(false)
const loadFailed = ref(false)
const detail = ref<OrderDetailAggregate | null>(null)
const orderItemModalOpen = ref(false)
const orderItemSubmitting = ref(false)
const orderItemFormRef = ref()
const orderItemModalMode = ref<'create' | 'edit'>('create')
const editingOrderItemId = ref<IdValue>()
const batchLinkModalOpen = ref(false)
const batchLinkSubmitting = ref(false)
const batchLinkFormRef = ref()
const batchLinkModalMode = ref<'create' | 'edit'>('create')
const editingBatchLinkId = ref<IdValue>()
const batchResourceOptions = ref<BatchItem[]>([])

const itemColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 108 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 150 },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 82 },
  { title: '颜色', dataIndex: 'color', key: 'color', width: 64 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 74 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 54 },
  { title: '门幅(cm)', dataIndex: 'requiredWidth', key: 'requiredWidth', width: 86 },
  { title: '重量(kg)', dataIndex: 'requiredWeight', key: 'requiredWeight', width: 86 },
  { title: '操作', key: 'action', width: 82 },
]

const batchColumns = [
  { title: '批次编号', dataIndex: 'batchNo', key: 'batchNo', width: 138 },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier', width: 126 },
  { title: '重量(kg)', dataIndex: 'weight', key: 'weight', width: 82 },
  { title: '门幅(cm)', dataIndex: 'width', key: 'width', width: 78 },
  { title: '成分', dataIndex: 'composition', key: 'composition', width: 88 },
  { title: '本单分配', dataIndex: 'allocatedWeight', key: 'allocatedWeight', width: 98 },
  { title: '批次剩余', dataIndex: 'remainingWeight', key: 'remainingWeight', width: 92 },
  { title: '状态', dataIndex: 'resourceStatus', key: 'resourceStatus', width: 84 },
  { title: '占用', dataIndex: 'lockedByPlan', key: 'lockedByPlan', width: 92 },
  { title: '操作', key: 'action', width: 82 },
]

const orderItemForm = reactive<OrderItemForm>({
  productCode: '',
  productName: '',
  specification: '',
  color: '',
  quantity: null,
  unit: 'm',
  requiredWidth: null,
  requiredWeight: null,
  remark: '',
})

const batchLinkForm = reactive<BatchLinkForm>({
  orderItemId: undefined,
  batchId: undefined,
  allocatedWeight: null,
  allocatedQuantity: null,
  remark: '',
})

const orderItemRules = {
  productCode: [{ required: true, message: '请输入产品编码' }],
  productName: [{ required: true, message: '请输入产品名称' }],
  quantity: [{ required: true, message: '请输入数量' }],
}

const batchLinkRules = {
  orderItemId: [{ required: true, message: '请选择订单明细' }],
  batchId: [{ required: true, message: '请选择批次' }],
  allocatedWeight: [{ required: true, message: '请输入分配重量' }],
}

const planColumns = [
  { title: '计划ID', dataIndex: 'planId', key: 'planId', width: 118 },
  { title: '工艺路线', dataIndex: 'routeName', key: 'routeName', width: 154 },
  { title: '批次', dataIndex: 'batchNo', key: 'batchNo', width: 132 },
  { title: '开始时间', dataIndex: 'planStartTime', key: 'planStartTime', width: 126 },
  { title: '结束时间', dataIndex: 'planEndTime', key: 'planEndTime', width: 126 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 84 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 120 },
]

const stepColumns = [
  { title: '顺序', dataIndex: 'sequenceNo', key: 'sequenceNo', width: 90 },
  { title: '工序', dataIndex: 'stepName', key: 'stepName', width: 160 },
  { title: '设备', dataIndex: 'machineName', key: 'machineName', width: 180 },
  { title: '计划开始', dataIndex: 'planStartTime', key: 'planStartTime', width: 180 },
  { title: '计划结束', dataIndex: 'planEndTime', key: 'planEndTime', width: 180 },
  { title: '计划工时', dataIndex: 'planHours', key: 'planHours', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
]

const qualityColumns = [
  { title: '记录ID', dataIndex: 'inspectionId', key: 'inspectionId', width: 120 },
  { title: '工序', dataIndex: 'stepName', key: 'stepName', width: 160 },
  { title: '批次', dataIndex: 'batchNo', key: 'batchNo', width: 160 },
  { title: '判定', dataIndex: 'resultJudge', key: 'resultJudge', width: 120 },
  { title: '检测方式', dataIndex: 'inspectType', key: 'inspectType', width: 140 },
  { title: '检验时间', dataIndex: 'inspectTime', key: 'inspectTime', width: 180 },
]

const exceptionColumns = [
  { title: '异常ID', dataIndex: 'exceptionId', key: 'exceptionId', width: 120 },
  { title: '工序', dataIndex: 'stepName', key: 'stepName', width: 160 },
  { title: '批次', dataIndex: 'batchNo', key: 'batchNo', width: 160 },
  { title: '异常类型', dataIndex: 'exceptionType', key: 'exceptionType', width: 160 },
  { title: '异常等级', dataIndex: 'exceptionLevel', key: 'exceptionLevel', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
]

const machineTimelineColumns = [
  { title: '设备编码', dataIndex: 'machineCode', key: 'machineCode', width: 140 },
  { title: '设备名称', dataIndex: 'machineName', key: 'machineName', width: 180 },
  { title: '设备类型', dataIndex: 'machineType', key: 'machineType', width: 140 },
  { title: '当前状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '本订单占用时间段', dataIndex: 'occupiedRange', key: 'occupiedRange', width: 260 },
  { title: '涉及工序', dataIndex: 'stepNames', key: 'stepNames' },
]

const stepGroups = computed<OrderPlanStepGroup[]>(() =>
  detail.value ? buildOrderPlanStepGroups(detail.value) : [],
)

const routeStepFlow = computed<RouteStepFlowItem[]>(() => {
  const routeSteps = (detail.value?.routeSummary ?? []).flatMap((routeItem) =>
    (routeItem.steps ?? []).map((step) => ({
      key: `${routeItem.routeId}-${step.routeStepId ?? step.stepId ?? step.stepCode ?? step.stepName}`,
      stepName: step.stepName || step.stepCode || `工序 ${step.stepId ?? '-'}`,
      sequenceNo: step.sortOrder ?? null,
    })),
  )

  if (routeSteps.length) {
    return routeSteps.sort((left, right) => (left.sequenceNo ?? 999) - (right.sequenceNo ?? 999))
  }

  const planSteps = detail.value?.planSteps ?? []
  const uniqueSteps = new Map<string, RouteStepFlowItem>()
  planSteps.forEach((step) => {
    const key = String(step.stepId)
    if (!uniqueSteps.has(key)) {
      uniqueSteps.set(key, {
        key,
        stepName: step.stepName || `工序 ${step.stepId}`,
        sequenceNo: step.sequenceNo ?? null,
      })
    }
  })

  return Array.from(uniqueSteps.values()).sort((left, right) => (left.sequenceNo ?? 999) - (right.sequenceNo ?? 999))
})

const buildMachineTimelineRow = (
  machineId: string,
  machineCode: string | undefined,
  machineName: string | undefined,
  machineType: string | undefined,
  status: string | undefined,
  occupiedRanges: Array<{ planStartTime?: string; planEndTime?: string; stepName?: string }>,
): MachineTimelineRow => {
  const startTimes = occupiedRanges
    .map((item) => item.planStartTime)
    .filter((item): item is string => Boolean(item))
    .sort()
  const endTimes = occupiedRanges
    .map((item) => item.planEndTime)
    .filter((item): item is string => Boolean(item))
    .sort()
  const stepNames = Array.from(new Set(
    occupiedRanges
      .map((item) => item.stepName)
      .filter((item): item is string => Boolean(item)),
  )).join(' / ')

  return {
    machineId,
    machineCode,
    machineName,
    machineType,
    status,
    occupiedRange:
      startTimes.length && endTimes.length
        ? `${startTimes[0]} 至 ${endTimes[endTimes.length - 1]}`
        : '-',
    stepNames: stepNames || '-',
  }
}

const machineTimelineRows = computed<MachineTimelineRow[]>(() => {
  const planSteps = detail.value?.planSteps ?? []
  const machineSummary = detail.value?.machineSummary ?? []

  if (machineSummary.length) {
    return machineSummary.map((machine) => {
      const occupiedRanges = machine.occupiedTimeRanges?.length
        ? machine.occupiedTimeRanges
        : planSteps
          .filter((step) => String(step.machineId) === String(machine.machineId))
          .map((step) => ({
            planStartTime: step.planStartTime,
            planEndTime: step.planEndTime,
            stepName: step.stepName,
          }))

      return buildMachineTimelineRow(
        String(machine.machineId),
        machine.machineCode,
        machine.machineName,
        machine.machineType,
        machine.status,
        occupiedRanges,
      )
    })
  }

  const machineMap = new Map<string, {
    machineCode?: string
    machineName?: string
    machineType?: string
    status?: string
    occupiedRanges: Array<{ planStartTime?: string; planEndTime?: string; stepName?: string }>
  }>()

  planSteps.forEach((step) => {
    if (!step.machineId) {
      return
    }

    const key = String(step.machineId)
    if (!machineMap.has(key)) {
      machineMap.set(key, {
        machineCode: undefined,
        machineName: step.machineName,
        machineType: undefined,
        status: undefined,
        occupiedRanges: [],
      })
    }

    machineMap.get(key)?.occupiedRanges.push({
      planStartTime: step.planStartTime,
      planEndTime: step.planEndTime,
      stepName: step.stepName,
    })
  })

  return Array.from(machineMap.entries()).map(([machineId, machine]) =>
    buildMachineTimelineRow(
      machineId,
      machine.machineCode,
      machine.machineName,
      machine.machineType,
      machine.status,
      machine.occupiedRanges,
    ),
  )
})

const qualitySummaryStats = computed(() => {
  const records = detail.value?.qualitySummary ?? []
  return {
    total: detail.value?.qcRecordCount ?? records.length,
    fail: records.filter((item) => item.resultJudge === 'FAIL').length,
    recheck: records.filter((item) => item.resultJudge === 'RECHECK').length,
  }
})

const orderItemSelectOptions = computed(() =>
  (detail.value?.items ?? []).map((item) => ({
    label: [item.productCode || item.productName, item.specification, item.color].filter(Boolean).join(' / '),
    value: item.orderItemId,
  })),
)

const getBatchRemainingWeight = (item: BatchItem) => item.remainingWeight ?? item.weight ?? 0

const getBatchRemainingQuantity = (item: BatchItem) => item.remainingQuantity ?? null

const getLinkId = (item: OrderBatchLinkItem) => item.id || item.linkId

const getAllocatedWeightForItem = (orderItemId?: IdValue) =>
  (detail.value?.linkedBatches ?? [])
    .filter((link) =>
      String(link.orderItemId) === String(orderItemId)
      && String(getLinkId(link) ?? '') !== String(editingBatchLinkId.value ?? ''),
    )
    .reduce((sum, link) => sum + Number(link.allocatedWeight ?? 0), 0)

const getAllocatedQuantityForItem = (orderItemId?: IdValue) =>
  (detail.value?.linkedBatches ?? [])
    .filter((link) =>
      String(link.orderItemId) === String(orderItemId)
      && String(getLinkId(link) ?? '') !== String(editingBatchLinkId.value ?? ''),
    )
    .reduce((sum, link) => sum + Number(link.allocatedQuantity ?? 0), 0)

const selectedOrderItem = computed(() =>
  (detail.value?.items ?? []).find((item) => String(item.orderItemId) === String(batchLinkForm.orderItemId)),
)

const selectedBatch = computed(() =>
  batchResourceOptions.value.find((item) => String(item.batchId) === String(batchLinkForm.batchId)),
)

const selectedItemRemainingWeight = computed(() => {
  const item = selectedOrderItem.value
  if (item?.requiredWeight == null) {
    return null
  }
  return Math.max(Number(item.requiredWeight) - getAllocatedWeightForItem(item.orderItemId), 0)
})

const selectedItemRemainingQuantity = computed(() => {
  const item = selectedOrderItem.value
  if (item?.quantity == null) {
    return null
  }
  return Math.max(Number(item.quantity) - getAllocatedQuantityForItem(item.orderItemId), 0)
})

const isBatchWidthMatched = (item: BatchItem, orderItem = selectedOrderItem.value) =>
  orderItem?.requiredWidth == null || item.width == null || Number(item.width) >= Number(orderItem.requiredWidth)

const isBatchTextMatched = (item: BatchItem, orderItem = selectedOrderItem.value) => {
  const haystack = [item.composition, item.note, item.batchNo].filter(Boolean).join(' ').toLowerCase()
  const needles = [orderItem?.productName, orderItem?.specification, orderItem?.color]
    .filter(Boolean)
    .map((value) => String(value).toLowerCase())
  return needles.length > 0 && needles.some((value) => haystack.includes(value))
}

const getBatchRecommendLevel = (item: BatchItem) => {
  if (!isBatchWidthMatched(item)) {
    return 0
  }
  return isBatchTextMatched(item) ? 2 : 1
}

const batchSelectOptions = computed(() =>
  batchResourceOptions.value
    .filter((item) =>
      String(item.batchId) === String(batchLinkForm.batchId)
      || (
        !item.lockedByPlan
        && !['CONSUMED', 'CLOSED'].includes(item.resourceStatus || '')
        && getBatchRemainingWeight(item) > 0
        && isBatchWidthMatched(item)
      ),
    )
    .sort((left, right) =>
      getBatchRecommendLevel(right) - getBatchRecommendLevel(left)
      || getBatchRemainingWeight(right) - getBatchRemainingWeight(left),
    )
    .map((item) => ({
      label: `${getBatchRecommendLevel(item) === 2 ? '推荐 / ' : ''}${item.batchNo} / ${item.supplier} / 剩余 ${formatAllocation(getBatchRemainingWeight(item), getBatchRemainingQuantity(item))}`,
      value: item.batchId,
    })),
)

const selectedBatchResourceMeta = computed(() =>
  getResourceStatusMeta(selectedBatch.value?.resourceStatus, selectedBatch.value?.resourceStatusLabel),
)

const exceptionSummaryStats = computed(() => {
  const records = detail.value?.exceptionSummary ?? []
  return {
    total: detail.value?.exceptionCount ?? records.length,
    latestStatus: detail.value?.latestException?.status || records[0]?.status || '-',
  }
})

const resourceStatusMap: Record<string, { label: string; color: string }> = {
  UNALLOCATED: { label: '待分配', color: 'blue' },
  PARTIALLY_ALLOCATED: { label: '部分分配', color: 'gold' },
  ALLOCATED: { label: '已分配', color: 'cyan' },
  IN_EXECUTION: { label: '执行中', color: 'processing' },
  CONSUMED: { label: '已消耗', color: 'success' },
  CLOSED: { label: '已关闭', color: 'error' },
}

const orderStatusMeta = computed(() =>
  orderStatusOptions.find((item) => item.value === detail.value?.status),
)

const getPlanStatusMeta = (status?: string) =>
  planStatusOptions.find((item) => item.value === status)

const getStepStatusMeta = (status?: string) =>
  planStepStatusOptions.find((item) => item.value === status)

const getJudgeMeta = (judge?: string) =>
  resultJudgeOptions.find((item) => item.value === judge)

const getExceptionStatusMeta = (status?: string) =>
  exceptionStatusOptions.find((item) => item.value === status)

const getResourceStatusMeta = (status?: string, label?: string) => {
  if (!status) {
    return undefined
  }
  const meta = resourceStatusMap[status]
  return meta ? { ...meta, label: label || meta.label } : { label: label || status, color: 'default' }
}

const formatWeight = (value?: number | null) => (value != null ? `${Number(value).toFixed(2)} kg` : '未记录重量')

const formatQuantity = (value?: number | null) => (value != null ? `${Number(value).toFixed(2)}` : '未记录数量')

const formatAllocation = (weight?: number | null, quantity?: number | null) => {
  if (weight != null && quantity != null) {
    return `${formatWeight(weight)} / ${formatQuantity(quantity)}`
  }
  if (weight != null) {
    return formatWeight(weight)
  }
  if (quantity != null) {
    return formatQuantity(quantity)
  }
  return '未分配'
}

const formatShortId = (value?: IdValue) => {
  const text = String(value ?? '')
  if (!text) {
    return '-'
  }
  return text.length > 10 ? `...${text.slice(-8)}` : text
}

const resetOrderItemForm = () => {
  orderItemForm.productCode = ''
  orderItemForm.productName = ''
  orderItemForm.specification = ''
  orderItemForm.color = ''
  orderItemForm.quantity = null
  orderItemForm.unit = 'm'
  orderItemForm.requiredWidth = null
  orderItemForm.requiredWeight = null
  orderItemForm.remark = ''
}

const resetBatchLinkForm = () => {
  batchLinkForm.orderItemId = detail.value?.items?.[0]?.orderItemId
  batchLinkForm.batchId = undefined
  batchLinkForm.allocatedWeight = null
  batchLinkForm.allocatedQuantity = null
  batchLinkForm.remark = ''
}

const fillSuggestedAllocation = () => {
  const batch = selectedBatch.value
  if (!batch) {
    batchLinkForm.allocatedWeight = null
    batchLinkForm.allocatedQuantity = null
    return
  }
  const remainingWeight = getBatchRemainingWeight(batch)
  const remainingDemandWeight = selectedItemRemainingWeight.value
  batchLinkForm.allocatedWeight = remainingDemandWeight == null
    ? remainingWeight
    : Math.min(remainingDemandWeight, remainingWeight)

  const remainingBatchQuantity = getBatchRemainingQuantity(batch)
  const remainingDemandQuantity = selectedItemRemainingQuantity.value
  batchLinkForm.allocatedQuantity = remainingBatchQuantity == null
    ? null
    : remainingDemandQuantity == null
      ? remainingBatchQuantity
      : Math.min(remainingDemandQuantity, remainingBatchQuantity)
}

const handleBatchOrderItemChange = () => {
  batchLinkForm.batchId = undefined
  batchLinkForm.allocatedWeight = null
  batchLinkForm.allocatedQuantity = null
}

const handleBatchSelectionChange = () => {
  fillSuggestedAllocation()
}

const openOrderItemModal = () => {
  orderItemModalMode.value = 'create'
  editingOrderItemId.value = undefined
  resetOrderItemForm()
  orderItemModalOpen.value = true
}

const openOrderItemEditModal = (record: OrderLineItem) => {
  orderItemModalMode.value = 'edit'
  editingOrderItemId.value = record.orderItemId
  orderItemForm.productCode = record.productCode || ''
  orderItemForm.productName = record.productName || ''
  orderItemForm.specification = record.specification || ''
  orderItemForm.color = record.color || ''
  orderItemForm.quantity = record.quantity ?? null
  orderItemForm.unit = record.unit || 'm'
  orderItemForm.requiredWidth = record.requiredWidth ?? null
  orderItemForm.requiredWeight = record.requiredWeight ?? null
  orderItemForm.remark = record.remark || ''
  orderItemModalOpen.value = true
}

const openBatchLinkModal = async () => {
  if (!detail.value?.items?.length) {
    message.warning('请先新增订单明细，再分配批次')
    return
  }

  batchLinkModalMode.value = 'create'
  editingBatchLinkId.value = undefined
  resetBatchLinkForm()
  const response = await fetchBatchResourcePool({ pageNum: 1, pageSize: 200 })
  batchResourceOptions.value = response.list
  const firstOption = batchSelectOptions.value[0]
  if (firstOption) {
    batchLinkForm.batchId = firstOption.value
    fillSuggestedAllocation()
  }
  batchLinkModalOpen.value = true
}

const openBatchLinkEditModal = async (record: OrderBatchLinkItem) => {
  batchLinkModalMode.value = 'edit'
  editingBatchLinkId.value = record.id || record.linkId
  batchLinkForm.orderItemId = record.orderItemId
  batchLinkForm.batchId = record.batchId
  batchLinkForm.allocatedWeight = record.allocatedWeight ?? null
  batchLinkForm.allocatedQuantity = record.allocatedQuantity ?? null
  batchLinkForm.remark = record.remark || ''
  const response = await fetchBatchResourcePool({ pageNum: 1, pageSize: 200 })
  batchResourceOptions.value = response.list
  if (!batchResourceOptions.value.some((item) => String(item.batchId) === String(record.batchId))) {
    batchResourceOptions.value.push({
      batchId: record.batchId,
      batchNo: record.batchNo || String(record.batchId),
      supplier: record.supplier || '-',
      inDate: '',
      weight: record.weight ?? undefined,
      width: record.width ?? undefined,
      composition: record.composition,
      readyForSchedule: true,
    })
  }
  batchLinkModalOpen.value = true
}

const handleOrderItemSubmit = async () => {
  await orderItemFormRef.value?.validate()
  orderItemSubmitting.value = true
  try {
    const payload: OrderItemUpsertRequest = {
      productCode: orderItemForm.productCode.trim(),
      productName: orderItemForm.productName.trim(),
      specification: orderItemForm.specification.trim() || undefined,
      color: orderItemForm.color.trim() || undefined,
      quantity: orderItemForm.quantity,
      unit: orderItemForm.unit.trim() || undefined,
      requiredWidth: orderItemForm.requiredWidth,
      requiredWeight: orderItemForm.requiredWeight,
      remark: orderItemForm.remark.trim() || undefined,
    }
    if (orderItemModalMode.value === 'edit' && editingOrderItemId.value) {
      await updateOrderItem(orderId, editingOrderItemId.value, payload)
      message.success('订单明细已更新')
    } else {
      await createOrderItem(orderId, payload)
      message.success('订单明细已新增')
    }
    orderItemModalOpen.value = false
    await loadData()
  } finally {
    orderItemSubmitting.value = false
  }
}

const handleBatchLinkSubmit = async () => {
  await batchLinkFormRef.value?.validate()
  batchLinkSubmitting.value = true
  try {
    if (!batchLinkForm.orderItemId || !batchLinkForm.batchId) {
      return
    }

    const payload: OrderBatchLinkUpsertRequest = {
      orderItemId: batchLinkForm.orderItemId,
      batchId: batchLinkForm.batchId,
      allocatedWeight: batchLinkForm.allocatedWeight,
      allocatedQuantity: batchLinkForm.allocatedQuantity,
      remark: batchLinkForm.remark.trim() || undefined,
    }
    if (batchLinkModalMode.value === 'edit' && editingBatchLinkId.value) {
      await updateOrderBatchLink(orderId, editingBatchLinkId.value, payload)
      message.success('批次分配已更新')
    } else {
      await createOrderBatchLink(orderId, payload)
      message.success('批次已分配给订单明细')
    }
    batchLinkModalOpen.value = false
    await loadData()
  } finally {
    batchLinkSubmitting.value = false
  }
}

const handleDeleteOrderItem = (record: OrderLineItem) => {
  Modal.confirm({
    title: '删除订单明细',
    content: `确定删除 ${record.productCode || record.productName || '该订单明细'} 吗？已分配批次或已排产的明细不能删除。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteOrderItem(orderId, record.orderItemId)
      message.success('订单明细已删除')
      await loadData()
    },
  })
}

const handleDeleteBatchLink = (record: OrderBatchLinkItem) => {
  const linkId = record.id || record.linkId
  if (!linkId) {
    return
  }
  Modal.confirm({
    title: '删除批次分配',
    content: `确定取消批次 ${record.batchNo || record.batchId} 的分配吗？已生成生产计划的分配不能删除。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteOrderBatchLink(orderId, linkId)
      message.success('批次分配已删除')
      await loadData()
    },
  })
}

const goBack = () => {
  void router.push({ name: 'order-list' })
}

const jumpToPlanBoard = (plan: OrderPlanSummaryItem) => {
  void router.push({
    name: 'schedule-board',
    query: { planId: String(plan.planId) },
  })
}

const jumpToPlanMain = (plan: OrderPlanSummaryItem) => {
  void router.push({
    name: 'schedule-main',
    query: { planId: String(plan.planId) },
  })
}

const jumpToQualityResult = () => {
  void router.push({ name: 'gray-iqc-result' })
}

const jumpToException = () => {
  void router.push({ name: 'quality-ncr' })
}

const loadData = async () => {
  loading.value = true
  loadFailed.value = false
  try {
    detail.value = await fetchOrderDetail(orderId)
  } catch (error) {
    loadFailed.value = true
    detail.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <div class="order-detail-page">
    <a-page-header title="订单详情" :sub-title="detail?.orderNo || `订单 ${orderId}`" @back="goBack">
      <template #extra>
        <a-space>
          <a-button @click="jumpToQualityResult">查看质检结果</a-button>
          <a-button @click="jumpToException">查看异常页</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-spin :spinning="loading">
      <a-result
        v-if="loadFailed"
        status="warning"
        title="订单详情暂时无法加载"
        sub-title="请检查订单详情接口返回，或稍后重试。"
      />

      <template v-else-if="detail">
        <div class="section-grid">
          <div class="top-metrics">
            <a-card class="metric-card" :bordered="false">
              <div class="metric-label">关联批次</div>
              <div class="metric-value">{{ detail.linkedBatchCount ?? detail.linkedBatches?.length ?? 0 }}</div>
            </a-card>
            <a-card class="metric-card" :bordered="false">
              <div class="metric-label">生产计划</div>
              <div class="metric-value">{{ detail.generatedPlanCount ?? detail.planSummary?.length ?? 0 }}</div>
            </a-card>
            <a-card class="metric-card" :bordered="false">
              <div class="metric-label">质检记录</div>
              <div class="metric-value">{{ qualitySummaryStats.total }}</div>
            </a-card>
            <a-card class="metric-card" :bordered="false">
              <div class="metric-label">异常记录</div>
              <div class="metric-value">{{ exceptionSummaryStats.total }}</div>
            </a-card>
          </div>

          <a-card class="page-card" :bordered="false" title="订单基本信息">
            <a-descriptions :column="3" bordered size="small">
              <a-descriptions-item label="订单号">{{ detail.orderNo }}</a-descriptions-item>
              <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-tag :color="orderStatusMeta?.color">
                  {{ orderStatusMeta?.label || detail.status || '-' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="下单时间">{{ detail.orderDate || '-' }}</a-descriptions-item>
              <a-descriptions-item label="交期">{{ detail.deliveryDate || '-' }}</a-descriptions-item>
              <a-descriptions-item label="优先级">{{ detail.priority || '-' }}</a-descriptions-item>
              <a-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</a-descriptions-item>
            </a-descriptions>
          </a-card>

          <a-card class="page-card" :bordered="false" title="订单明细">
            <template #extra>
              <a-button type="primary" @click="openOrderItemModal">新增明细</a-button>
            </template>
            <a-table
              :columns="itemColumns"
              :data-source="detail.items || []"
              :pagination="false"
              row-key="orderItemId"
              class="detail-fit-table"
              table-layout="fixed"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'productCode'">
                  <span class="detail-cell-nowrap">{{ record.productCode || '-' }}</span>
                </template>
                <template v-else-if="column.key === 'productName'">
                  <a-tooltip :title="record.productName || '-'">
                    <span class="detail-cell-text detail-cell-two-line">{{ record.productName || '-' }}</span>
                  </a-tooltip>
                </template>
                <template v-else-if="column.key === 'specification'">
                  <span class="detail-cell-nowrap">{{ record.specification || '-' }}</span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <div class="detail-action-grid">
                    <a-button type="link" size="small" @click="openOrderItemEditModal(record)">编辑</a-button>
                    <a-button type="link" size="small" danger @click="handleDeleteOrderItem(record)">删除</a-button>
                  </div>
                </template>
              </template>
            </a-table>
          </a-card>

          <a-card class="page-card" :bordered="false" title="关联批次资源">
            <template #extra>
              <a-button type="primary" @click="openBatchLinkModal">分配批次</a-button>
            </template>
            <a-table
              :columns="batchColumns"
              :data-source="detail.linkedBatches || []"
              :pagination="false"
              row-key="id"
              class="detail-fit-table"
              table-layout="fixed"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'batchNo'">
                  <a-tooltip :title="record.batchNo || '-'">
                    <span class="detail-cell-text detail-cell-two-line">{{ record.batchNo || '-' }}</span>
                  </a-tooltip>
                </template>
                <template v-else-if="column.key === 'supplier'">
                  <a-tooltip :title="record.supplier || '-'">
                    <span class="detail-cell-text">{{ record.supplier || '-' }}</span>
                  </a-tooltip>
                </template>
                <template v-else-if="column.key === 'composition'">
                  <span class="detail-cell-nowrap">{{ record.composition || '-' }}</span>
                </template>
                <template v-else-if="column.key === 'resourceStatus'">
                  <a-tag :color="getResourceStatusMeta(record.resourceStatus, record.resourceStatusLabel)?.color">
                    {{ getResourceStatusMeta(record.resourceStatus, record.resourceStatusLabel)?.label || '-' }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'allocatedWeight'">
                  {{ formatAllocation(record.allocatedWeight, record.allocatedQuantity) }}
                </template>
                <template v-else-if="column.key === 'remainingWeight'">
                  {{ formatAllocation(record.remainingWeight, record.remainingQuantity) }}
                </template>
                <template v-else-if="column.key === 'lockedByPlan'">
                  <a-tooltip :title="record.lockedByPlan ? `计划 ${record.currentPlanId || ''} 占用` : '可排产'">
                    <a-tag :color="record.lockedByPlan ? 'processing' : 'success'">
                      {{ record.lockedByPlan ? '已占用' : '可排产' }}
                    </a-tag>
                  </a-tooltip>
                </template>
                <template v-else-if="column.key === 'action'">
                  <div class="detail-action-grid">
                    <a-button type="link" size="small" @click="openBatchLinkEditModal(record)">编辑</a-button>
                    <a-button type="link" size="small" danger @click="handleDeleteBatchLink(record)">删除</a-button>
                  </div>
                </template>
              </template>
            </a-table>
          </a-card>

          <a-card class="page-card" :bordered="false" title="工艺路线">
            <div v-if="detail.routeSummary?.length" class="route-summary-grid">
              <div v-for="routeItem in detail.routeSummary" :key="String(routeItem.routeId)" class="route-summary-card">
                <div class="route-name">{{ routeItem.routeName }}</div>
                <div class="route-desc">{{ routeItem.description || '暂无路线描述' }}</div>
                <div class="route-meta">{{ routeItem.steps?.length ? `工序 ${routeItem.steps.length} 道` : '未配置工序' }}</div>
              </div>
            </div>
            <a-empty v-else description="当前订单尚未绑定工艺路线摘要" />

            <div class="panel-title flow-title">当前路线工序顺序</div>
            <div v-if="routeStepFlow.length" class="flow-strip">
              <div v-for="step in routeStepFlow" :key="step.key" class="flow-chip">
                <span class="flow-index">{{ step.sequenceNo ?? '-' }}</span>
                <span>{{ step.stepName }}</span>
              </div>
            </div>
            <a-empty v-else description="当前订单尚未展开工序顺序" />
          </a-card>

          <a-card class="page-card" :bordered="false" title="关联计划与工艺路线">
            <div v-if="detail.routeSummary?.length || detail.machineSummary?.length" class="summary-strip">
              <div v-if="detail.routeSummary?.length" class="summary-chip-group">
                <span class="summary-chip-label">路线摘要</span>
                <a-tag v-for="routeItem in detail.routeSummary" :key="String(routeItem.routeId)" color="blue">
                  {{ routeItem.routeName }}
                </a-tag>
              </div>
              <div v-if="detail.machineSummary?.length" class="summary-chip-group">
                <span class="summary-chip-label">设备摘要</span>
                <a-tag v-for="machineItem in detail.machineSummary" :key="String(machineItem.machineId)" color="geekblue">
                  {{ machineItem.machineName || machineItem.machineCode || machineItem.machineId }}
                </a-tag>
              </div>
            </div>
            <a-table
              :columns="planColumns"
              :data-source="detail.planSummary || []"
              :pagination="false"
              row-key="planId"
              class="detail-fit-table"
              table-layout="fixed"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="getPlanStatusMeta(record.status)?.color">
                    {{ getPlanStatusMeta(record.status)?.label || record.status || '-' }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'planId'">
                  <div class="detail-action-grid detail-plan-links">
                    <a-tooltip :title="String(record.planId)">
                      <a-button type="link" size="small" @click="jumpToPlanMain(record)">
                        {{ formatShortId(record.planId) }}
                      </a-button>
                    </a-tooltip>
                    <a-button type="link" size="small" @click="jumpToPlanBoard(record)">甘特图</a-button>
                  </div>
                </template>
                <template v-else-if="column.key === 'routeName'">
                  <a-tooltip :title="record.routeName || '-'">
                    <span class="detail-cell-text detail-cell-two-line">{{ record.routeName || '-' }}</span>
                  </a-tooltip>
                </template>
                <template v-else-if="column.key === 'batchNo'">
                  <a-tooltip :title="record.batchNo || '-'">
                    <span class="detail-cell-text detail-cell-two-line">{{ record.batchNo || '-' }}</span>
                  </a-tooltip>
                </template>
                <template v-else-if="column.key === 'planStartTime' || column.key === 'planEndTime'">
                  <span class="detail-cell-time">{{ record[column.key] || '-' }}</span>
                </template>
                <template v-else-if="column.key === 'remark'">
                  <a-tooltip :title="record.remark || '-'">
                    <span class="detail-cell-text detail-cell-two-line">{{ record.remark || '-' }}</span>
                  </a-tooltip>
                </template>
              </template>
            </a-table>
          </a-card>

          <a-card class="page-card" :bordered="false" title="工序与设备">
            <div class="panel-title">工序计划</div>
            <a-collapse v-if="stepGroups.length" accordion>
              <a-collapse-panel
                v-for="group in stepGroups"
                :key="String(group.planId)"
                :header="`计划 #${group.planId} / ${group.routeName || '未配置路线'} / ${group.batchNo || '未关联批次'}`"
              >
                <a-table
                  :columns="stepColumns"
                  :data-source="group.steps"
                  :pagination="false"
                  row-key="planStepId"
                  :scroll="{ x: 1200 }"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'status'">
                      <a-tag :color="getStepStatusMeta(record.status)?.color">
                        {{ getStepStatusMeta(record.status)?.label || record.status || '-' }}
                      </a-tag>
                    </template>
                    <template v-else-if="column.key === 'machineName'">
                      {{ record.machineName || '未分配设备' }}
                    </template>
                    <template v-else-if="column.key === 'planHours'">
                      {{ record.planHours ?? '-' }}
                    </template>
                  </template>
                </a-table>
              </a-collapse-panel>
            </a-collapse>
            <a-empty v-else description="当前订单暂未关联工序计划" />

            <div class="panel-title machine-title">涉及设备与时间段</div>
            <a-table
              :columns="machineTimelineColumns"
              :data-source="machineTimelineRows"
              :pagination="false"
              row-key="machineId"
              size="small"
              :scroll="{ x: 1100 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag>{{ record.status || '-' }}</a-tag>
                </template>
                <template v-else-if="column.key === 'machineType'">
                  {{ record.machineType || '-' }}
                </template>
                <template v-else-if="column.key === 'machineCode'">
                  {{ record.machineCode || '-' }}
                </template>
              </template>
            </a-table>
          </a-card>

          <a-card class="page-card" :bordered="false" title="质量与异常摘要">
            <div class="summary-strip">
              <div class="summary-chip-group">
                <span class="summary-chip-label">质检摘要</span>
                <a-tag color="blue">总数 {{ qualitySummaryStats.total }}</a-tag>
                <a-tag color="red">FAIL {{ qualitySummaryStats.fail }}</a-tag>
                <a-tag color="gold">RECHECK {{ qualitySummaryStats.recheck }}</a-tag>
              </div>
              <div class="summary-chip-group">
                <span class="summary-chip-label">异常摘要</span>
                <a-tag color="volcano">总数 {{ exceptionSummaryStats.total }}</a-tag>
                <a-tag>{{ exceptionSummaryStats.latestStatus }}</a-tag>
              </div>
            </div>
            <div class="summary-panels">
              <div class="summary-panel">
                <div class="panel-title">最近质检结果</div>
                <a-table
                  :columns="qualityColumns"
                  :data-source="detail.qualitySummary || []"
                  :pagination="false"
                  row-key="inspectionId"
                  size="small"
                  :scroll="{ x: 980 }"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'resultJudge'">
                      <a-tag :color="getJudgeMeta(record.resultJudge)?.color">
                        {{ getJudgeMeta(record.resultJudge)?.label || record.resultJudge || '-' }}
                      </a-tag>
                    </template>
                    <template v-else-if="column.key === 'inspectType'">
                      {{ record.inspectType || '-' }}
                    </template>
                  </template>
                </a-table>
              </div>

              <div class="summary-panel">
                <div class="panel-title">最近异常记录</div>
                <a-table
                  :columns="exceptionColumns"
                  :data-source="detail.exceptionSummary || []"
                  :pagination="false"
                  row-key="exceptionId"
                  size="small"
                  :scroll="{ x: 1080 }"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'status'">
                      <a-tag :color="getExceptionStatusMeta(record.status)?.color">
                        {{ getExceptionStatusMeta(record.status)?.label || record.status || '-' }}
                      </a-tag>
                    </template>
                    <template v-else-if="column.key === 'exceptionLevel'">
                      {{ record.exceptionLevel || '-' }}
                    </template>
                  </template>
                </a-table>
              </div>
            </div>
          </a-card>
        </div>
      </template>
    </a-spin>

    <a-modal
      v-model:open="orderItemModalOpen"
      :title="orderItemModalMode === 'create' ? '新增订单明细' : '编辑订单明细'"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="orderItemSubmitting"
      width="720px"
      @ok="handleOrderItemSubmit"
    >
      <a-form ref="orderItemFormRef" :model="orderItemForm" :rules="orderItemRules" layout="vertical">
        <div class="form-grid">
          <a-form-item label="产品编码" name="productCode">
            <a-input v-model:value="orderItemForm.productCode" placeholder="请输入产品编码" />
          </a-form-item>
          <a-form-item label="产品名称" name="productName">
            <a-input v-model:value="orderItemForm.productName" placeholder="请输入产品名称" />
          </a-form-item>
          <a-form-item label="规格" name="specification">
            <a-input v-model:value="orderItemForm.specification" placeholder="可选" />
          </a-form-item>
          <a-form-item label="颜色" name="color">
            <a-input v-model:value="orderItemForm.color" placeholder="可选" />
          </a-form-item>
          <a-form-item label="数量" name="quantity">
            <a-input-number v-model:value="orderItemForm.quantity" :min="0" class="full-control" placeholder="请输入数量" />
          </a-form-item>
          <a-form-item label="单位" name="unit">
            <a-input v-model:value="orderItemForm.unit" placeholder="如 m、kg" />
          </a-form-item>
          <a-form-item label="要求门幅(cm)" name="requiredWidth">
            <a-input-number v-model:value="orderItemForm.requiredWidth" :min="0" class="full-control" placeholder="可选" />
          </a-form-item>
          <a-form-item label="要求重量(kg)" name="requiredWeight">
            <a-input-number v-model:value="orderItemForm.requiredWeight" :min="0" class="full-control" placeholder="可选" />
          </a-form-item>
        </div>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="orderItemForm.remark" :rows="3" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="batchLinkModalOpen"
      :title="batchLinkModalMode === 'create' ? '分配批次' : '编辑批次分配'"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="batchLinkSubmitting"
      width="680px"
      @ok="handleBatchLinkSubmit"
    >
      <a-form ref="batchLinkFormRef" :model="batchLinkForm" :rules="batchLinkRules" layout="vertical">
        <a-form-item label="订单明细" name="orderItemId">
          <a-select
            v-model:value="batchLinkForm.orderItemId"
            :options="orderItemSelectOptions"
            placeholder="请选择订单明细"
            show-search
            option-filter-prop="label"
            @change="handleBatchOrderItemChange"
          />
        </a-form-item>
        <a-form-item label="来料批次资源" name="batchId">
          <a-select
            v-model:value="batchLinkForm.batchId"
            :options="batchSelectOptions"
            placeholder="从工厂来料资源池选择可用批次"
            show-search
            option-filter-prop="label"
            @change="handleBatchSelectionChange"
          />
        </a-form-item>
        <div v-if="selectedBatch" class="batch-resource-card">
          <div class="batch-resource-main">
            <div>
              <div class="batch-resource-title">
                {{ selectedBatch.batchNo }}
                <a-tag v-if="getBatchRecommendLevel(selectedBatch) === 2" color="green">推荐</a-tag>
                <a-tag v-else color="blue">可用</a-tag>
              </div>
              <div class="batch-resource-sub">{{ selectedBatch.supplier || '-' }}</div>
            </div>
            <a-tag :color="selectedBatchResourceMeta?.color">
              {{ selectedBatchResourceMeta?.label || selectedBatch.resourceStatusLabel || selectedBatch.resourceStatus || '资源可用' }}
            </a-tag>
          </div>
          <div class="batch-resource-grid">
            <div>
              <span>剩余重量</span>
              <strong>{{ formatWeight(getBatchRemainingWeight(selectedBatch)) }}</strong>
            </div>
            <div>
              <span>门幅</span>
              <strong>{{ selectedBatch.width != null ? `${Number(selectedBatch.width).toFixed(2)} cm` : '-' }}</strong>
            </div>
            <div>
              <span>成分</span>
              <strong>{{ selectedBatch.composition || '-' }}</strong>
            </div>
            <div>
              <span>订单占用</span>
              <strong>{{ selectedBatch.linkedOrderCount ? `${selectedBatch.linkedOrderCount} 个订单` : '暂无其他占用' }}</strong>
            </div>
          </div>
          <div class="batch-resource-hint">
            <span>当前明细剩余需求：</span>
            <strong>{{ selectedItemRemainingWeight == null ? '未设置目标重量' : formatWeight(selectedItemRemainingWeight) }}</strong>
            <span v-if="selectedItemRemainingQuantity != null"> / {{ formatQuantity(selectedItemRemainingQuantity) }}</span>
          </div>
        </div>
        <div class="form-grid">
          <a-form-item label="分配重量(kg)" name="allocatedWeight">
            <a-input-number v-model:value="batchLinkForm.allocatedWeight" :min="0" class="full-control" placeholder="请输入分配重量" />
          </a-form-item>
          <a-form-item label="分配数量" name="allocatedQuantity">
            <a-input-number v-model:value="batchLinkForm.allocatedQuantity" :min="0" class="full-control" placeholder="可选" />
          </a-form-item>
        </div>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="batchLinkForm.remark" :rows="3" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.order-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.top-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  border-radius: 12px;
}

.metric-label {
  color: #64748b;
  font-size: 13px;
}

.metric-value {
  margin-top: 10px;
  color: #1f2937;
  font-size: 30px;
  font-weight: 700;
}

.page-card {
  border-radius: 12px;
}

.detail-fit-table {
  width: 100%;
}

.detail-cell-nowrap,
.detail-cell-time,
.detail-cell-text {
  display: block;
  min-width: 0;
  color: #1f2937;
  line-height: 1.45;
  word-break: keep-all;
  overflow-wrap: normal;
}

.detail-cell-nowrap,
.detail-cell-time {
  white-space: nowrap;
}

.detail-cell-time {
  font-size: 12px;
}

.detail-cell-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-cell-two-line {
  display: -webkit-box;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.detail-action-grid {
  display: grid;
  grid-template-columns: repeat(2, max-content);
  gap: 4px 8px;
  align-items: center;
}

.detail-plan-links {
  grid-template-columns: 1fr;
  justify-items: start;
}

.detail-action-grid :deep(.ant-btn) {
  height: 22px;
  padding: 0;
  line-height: 22px;
}

:deep(.detail-fit-table .ant-table) {
  overflow: hidden;
}

:deep(.detail-fit-table .ant-table-container),
:deep(.detail-fit-table .ant-table-content) {
  width: 100%;
  overflow-x: hidden;
}

:deep(.detail-fit-table .ant-table-thead > tr > th),
:deep(.detail-fit-table .ant-table-tbody > tr > td) {
  padding: 12px 10px;
  vertical-align: middle;
  word-break: keep-all;
  overflow-wrap: normal;
}

:deep(.detail-fit-table .ant-table-thead > tr > th) {
  white-space: nowrap;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.full-control {
  width: 100%;
}

.batch-resource-card {
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.batch-resource-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.batch-resource-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1f2937;
  font-weight: 700;
}

.batch-resource-sub {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.batch-resource-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.batch-resource-grid div {
  min-width: 0;
}

.batch-resource-grid span,
.batch-resource-hint span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.batch-resource-grid strong,
.batch-resource-hint strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #1f2937;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-resource-hint {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
}

.batch-resource-hint span,
.batch-resource-hint strong {
  display: inline;
  margin-top: 0;
}

.route-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.route-summary-card {
  padding: 14px 16px;
  border: 1px solid rgba(145, 158, 171, 0.18);
  border-radius: 12px;
  background: #f8fafc;
}

.route-name {
  color: #1f2937;
  font-weight: 700;
}

.route-desc {
  margin-top: 6px;
  color: #64748b;
  line-height: 1.7;
}

.route-meta {
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
}

.summary-panels {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.summary-strip {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.summary-chip-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-chip-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.summary-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-title {
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
}

.flow-title {
  margin-top: 14px;
}

.machine-title {
  margin-top: 16px;
}

.flow-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.flow-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  color: #1f2937;
  border: 1px solid rgba(59, 130, 246, 0.18);
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.06);
}

.flow-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: #2452d6;
  font-size: 12px;
  font-weight: 700;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
}

@media (max-width: 1200px) {
  .top-metrics,
  .route-summary-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .top-metrics,
  .route-summary-grid,
  .batch-resource-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
