import { fetchExceptionRecords } from '@/api/exception/exceptionRecord'
import { getBatch } from '@/api/batch/batch'
import {
  fetchOrderBatches,
  fetchOrderItems,
  getOrder,
} from '@/api/order/order'
import { fetchPlans, getPlan } from '@/api/plan/plan'
import { fetchQcRecords } from '@/api/quality/qcRecord'
import type {
  IdValue,
  OrderDetailAggregate,
  OrderExceptionSummaryItem,
  OrderPlanSummaryItem,
  OrderQualitySummaryItem,
  PlanStepItem,
} from '@/types/domain'

const normalizeOrderDetail = (detail: OrderDetailAggregate): OrderDetailAggregate => ({
  ...detail,
  items: (detail.items ?? []).map((item) => ({
    ...item,
    targetWidth: item.targetWidth ?? item.requiredWidth ?? null,
    targetWeight: item.targetWeight ?? item.requiredWeight ?? null,
  })),
  linkedBatches: detail.linkedBatches ?? [],
  planSummary: detail.planSummary ?? [],
  planSteps: detail.planSteps ?? [],
  routeSummary: detail.routeSummary ?? [],
  machineSummary: detail.machineSummary ?? [],
  qcSummary: detail.qcSummary ?? [],
  latestQcRecord: detail.latestQcRecord ?? detail.qcSummary?.[0] ?? null,
  qualitySummary: detail.qualitySummary ?? detail.qcSummary ?? [],
  exceptionSummary: detail.exceptionSummary ?? [],
  latestException: detail.latestException ?? detail.exceptionSummary?.[0] ?? null,
})

const shouldEnrichPlanSummaryItem = (plan: OrderPlanSummaryItem) =>
  !plan.orderNo ||
  !plan.batchNo ||
  !plan.routeName

const enrichPlanSummary = async (plans: OrderPlanSummaryItem[]): Promise<OrderPlanSummaryItem[]> => {
  const detailResults = await Promise.allSettled(
    plans.map((item) =>
      shouldEnrichPlanSummaryItem(item)
        ? getPlan(item.planId, { silentError: true })
        : Promise.resolve(null),
    ),
  )

  return plans.map((item, index) => {
    const detailResult = detailResults[index]
    if (detailResult?.status !== 'fulfilled' || !detailResult.value) {
      return item
    }

    const planDetail = detailResult.value.data
    return {
      ...item,
      orderId: item.orderId ?? planDetail.orderInfo?.orderId,
      orderNo: item.orderNo ?? planDetail.orderInfo?.orderNo,
      customerName: item.customerName ?? planDetail.orderInfo?.customerName,
      orderItemId: item.orderItemId ?? planDetail.orderItemInfo?.orderItemId,
      productCode: item.productCode ?? planDetail.orderItemInfo?.productCode,
      productName: item.productName ?? planDetail.orderItemInfo?.productName,
      specification: item.specification ?? planDetail.orderItemInfo?.specification,
      color: item.color ?? planDetail.orderItemInfo?.color,
      batchId: item.batchId ?? planDetail.batchInfo?.batchId,
      batchNo: item.batchNo ?? planDetail.batchInfo?.batchNo,
      routeId: item.routeId ?? planDetail.routeInfo?.routeId,
      routeName: item.routeName ?? planDetail.routeInfo?.routeName,
      planStartTime: item.planStartTime ?? planDetail.planStartTime,
      planEndTime: item.planEndTime ?? planDetail.planEndTime,
      status: item.status ?? planDetail.status,
      remark: item.remark ?? planDetail.remark,
      planSteps: item.planSteps?.length ? item.planSteps : (planDetail.planSteps ?? []),
    }
  })
}

const buildPlanStepIndex = (
  planSummary: OrderPlanSummaryItem[],
  fallbackPlanSteps: PlanStepItem[] = [],
) => {
  const fallbackStepsByPlan = new Map<string, PlanStepItem[]>()
  fallbackPlanSteps.forEach((step) => {
    const key = String(step.planId)
    const group = fallbackStepsByPlan.get(key)
    if (group) {
      group.push(step)
      return
    }
    fallbackStepsByPlan.set(key, [step])
  })

  const planSteps = planSummary.length
    ? planSummary.flatMap((item) => item.planSteps?.length ? item.planSteps : (fallbackStepsByPlan.get(String(item.planId)) ?? []))
    : fallbackPlanSteps
  const stepIdSet = new Set(planSteps.map((item) => String(item.planStepId)))
  const stepNameMap = new Map<string, string>()
  const batchNoMap = new Map<string, string>()
  const planSummaryMap = new Map(planSummary.map((item) => [String(item.planId), item] as const))

  planSteps.forEach((step) => {
    stepNameMap.set(String(step.planStepId), step.stepName || '')
    const batchNo = planSummaryMap.get(String(step.planId))?.batchNo
    if (batchNo) {
      batchNoMap.set(String(step.planStepId), batchNo)
    }
  })

  return { planSteps, stepIdSet, stepNameMap, batchNoMap }
}

const fetchOrderQualitySummary = async (
  planSummary: OrderPlanSummaryItem[],
  planSteps: PlanStepItem[] = [],
): Promise<OrderQualitySummaryItem[]> => {
  const { stepIdSet, stepNameMap, batchNoMap } = buildPlanStepIndex(planSummary, planSteps)
  if (!stepIdSet.size) {
    return []
  }

  const response = await fetchQcRecords({ pageNum: 1, pageSize: 100 })
  return response.list
    .filter((item) => stepIdSet.has(String(item.planStepId)))
    .map((item) => ({
      ...item,
      stepName: stepNameMap.get(String(item.planStepId)) || undefined,
      batchNo: batchNoMap.get(String(item.planStepId)) || undefined,
    }))
    .sort((left, right) => (right.inspectTime || '').localeCompare(left.inspectTime || ''))
    .slice(0, 10)
}

const fetchOrderExceptionSummary = async (
  planSummary: OrderPlanSummaryItem[],
  planSteps: PlanStepItem[] = [],
): Promise<OrderExceptionSummaryItem[]> => {
  const { stepIdSet, stepNameMap, batchNoMap } = buildPlanStepIndex(planSummary, planSteps)
  if (!stepIdSet.size) {
    return []
  }

  const response = await fetchExceptionRecords({ pageNum: 1, pageSize: 100 })
  return response.list
    .filter((item) => stepIdSet.has(String(item.planStepId)))
    .map((item) => ({
      ...item,
      stepName: stepNameMap.get(String(item.planStepId)) || undefined,
      batchNo: batchNoMap.get(String(item.planStepId)) || undefined,
    }))
    .sort((left, right) => (right.createTime || '').localeCompare(left.createTime || ''))
    .slice(0, 10)
}

const needsBatchDetailEnrichment = (detail: OrderDetailAggregate) =>
  (detail.linkedBatches ?? []).some((item) =>
    item.weight == null ||
    item.width == null ||
    item.composition == null ||
    item.status == null ||
    item.remainingWeight == null ||
    item.remainingQuantity == null ||
    item.resourceStatus == null ||
    item.resourceStatusLabel == null ||
    item.currentPlanId === undefined ||
    item.currentPlanStatus === undefined ||
    item.lockedByPlan === undefined ||
    item.readyForSchedule === undefined,
  )

export const fetchOrderDetail = async (orderId: IdValue): Promise<OrderDetailAggregate> => {
  const baseResponse = await getOrder(orderId)
  const detail = normalizeOrderDetail(baseResponse.data)

  if (!detail.items?.length) {
    try {
      detail.items = await fetchOrderItems(orderId)
    } catch (error) {
      detail.items = detail.items ?? []
    }
  }

  if (!detail.linkedBatches?.length) {
    try {
      detail.linkedBatches = await fetchOrderBatches(orderId)
    } catch (error) {
      detail.linkedBatches = detail.linkedBatches ?? []
    }
  }

  if (detail.linkedBatches?.length && needsBatchDetailEnrichment(detail)) {
    const batchResults = await Promise.allSettled(
      detail.linkedBatches.map((item) =>
        getBatch(item.batchId, { silentError: true }),
      ),
    )
    detail.linkedBatches = detail.linkedBatches.map((item, index) => {
      const batchResult = batchResults[index]
      if (batchResult?.status !== 'fulfilled') {
        return item
      }
      return {
        ...item,
        weight: item.weight ?? batchResult.value.data.weight,
        width: item.width ?? batchResult.value.data.width,
        composition: item.composition ?? batchResult.value.data.composition,
        status: item.status ?? batchResult.value.data.status,
        remainingWeight: item.remainingWeight ?? batchResult.value.data.remainingWeight,
        remainingQuantity: item.remainingQuantity ?? batchResult.value.data.remainingQuantity,
        resourceStatus: item.resourceStatus ?? batchResult.value.data.resourceStatus,
        resourceStatusLabel: item.resourceStatusLabel ?? batchResult.value.data.resourceStatusLabel,
        currentPlanId: item.currentPlanId ?? batchResult.value.data.currentPlanId,
        currentPlanStatus: item.currentPlanStatus ?? batchResult.value.data.currentPlanStatus,
        lockedByPlan: item.lockedByPlan ?? batchResult.value.data.lockedByPlan,
        readyForSchedule: item.readyForSchedule ?? batchResult.value.data.readyForSchedule,
      }
    })
  }

  if (!detail.planSummary?.length) {
    try {
      const planResponse = await fetchPlans(
        { pageNum: 1, pageSize: 100, orderId },
        { silentError: true },
      )
      detail.planSummary = planResponse.list.map((item) => ({
        planId: item.planId,
        orderId: item.orderId,
        orderNo: item.orderNo,
        orderItemId: item.orderItemId,
        batchId: item.batchId,
        batchNo: item.batchNo,
        routeId: item.routeId,
        routeName: item.routeName,
        customerName: item.customerName,
        productCode: item.productCode,
        productName: item.productName,
        specification: item.specification,
        color: item.color,
        planStartTime: item.planStartTime,
        planEndTime: item.planEndTime,
        status: item.status,
        remark: item.remark,
      }))
    } catch (error) {
      detail.planSummary = detail.planSummary ?? []
    }
  }

  detail.planSummary = await enrichPlanSummary(detail.planSummary ?? [])

  if (!detail.planSummary?.length && detail.planSteps?.length) {
    const groupedSteps = new Map<string, OrderPlanSummaryItem>()
    detail.planSteps.forEach((step) => {
      const key = String(step.planId)
      if (!groupedSteps.has(key)) {
        groupedSteps.set(key, {
          planId: step.planId,
          batchNo: '',
          routeName: '',
          status: step.status,
          planSteps: [],
        })
      }
      groupedSteps.get(key)?.planSteps?.push(step)
    })
    detail.planSummary = Array.from(groupedSteps.values())
  }

  if (detail.qcSummary?.length) {
    const { stepNameMap, batchNoMap } = buildPlanStepIndex(detail.planSummary ?? [], detail.planSteps ?? [])
    detail.qualitySummary = detail.qcSummary.map((item) => ({
      ...item,
      stepName: stepNameMap.get(String(item.planStepId)) || undefined,
      batchNo: batchNoMap.get(String(item.planStepId)) || undefined,
    }))
  }

  if (detail.exceptionSummary?.length) {
    const { stepNameMap, batchNoMap } = buildPlanStepIndex(detail.planSummary ?? [], detail.planSteps ?? [])
    detail.exceptionSummary = detail.exceptionSummary.map((item) => ({
      ...item,
      stepName: stepNameMap.get(String(item.planStepId)) || undefined,
      batchNo: batchNoMap.get(String(item.planStepId)) || undefined,
    }))
  }

  if (!detail.qualitySummary?.length) {
    try {
      detail.qualitySummary = await fetchOrderQualitySummary(detail.planSummary ?? [], detail.planSteps ?? [])
    } catch (error) {
      detail.qualitySummary = detail.qualitySummary ?? []
    }
  }

  if (!detail.exceptionSummary?.length) {
    try {
      detail.exceptionSummary = await fetchOrderExceptionSummary(detail.planSummary ?? [], detail.planSteps ?? [])
    } catch (error) {
      detail.exceptionSummary = detail.exceptionSummary ?? []
    }
  }

  detail.latestQcRecord = detail.latestQcRecord ?? detail.qualitySummary?.[0] ?? null
  detail.latestException = detail.latestException ?? detail.exceptionSummary?.[0] ?? null

  return detail
}

export type OrderPlanStepGroup = {
  planId: IdValue
  orderNo?: string
  routeName?: string
  batchNo?: string
  steps: PlanStepItem[]
}

export const buildOrderPlanStepGroups = (detail: OrderDetailAggregate): OrderPlanStepGroup[] =>
  (detail.planSummary ?? []).map((item) => ({
    planId: item.planId,
    orderNo: item.orderNo,
    routeName: item.routeName,
    batchNo: item.batchNo,
    steps: item.planSteps?.length
      ? item.planSteps
      : (detail.planSteps ?? []).filter((step) => String(step.planId) === String(item.planId)),
  }))
