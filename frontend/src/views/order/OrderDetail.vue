<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchOrderDetail, buildOrderPlanStepGroups, type OrderPlanStepGroup } from '@/api/order/orderDetail'
import {
  exceptionStatusOptions,
  orderStatusOptions,
  planStatusOptions,
  planStepStatusOptions,
  resultJudgeOptions,
} from '@/constants/dictionaries'
import type { OrderDetailAggregate, OrderPlanSummaryItem } from '@/types/domain'

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

const route = useRoute()
const router = useRouter()

const orderId = route.params.id as string
const loading = ref(false)
const loadFailed = ref(false)
const detail = ref<OrderDetailAggregate | null>(null)

const itemColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 140 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 180 },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 160 },
  { title: '颜色', dataIndex: 'color', key: 'color', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 120 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 100 },
  { title: '要求门幅(cm)', dataIndex: 'requiredWidth', key: 'requiredWidth', width: 130 },
  { title: '要求重量(kg)', dataIndex: 'requiredWeight', key: 'requiredWeight', width: 130 },
]

const batchColumns = [
  { title: '批次编号', dataIndex: 'batchNo', key: 'batchNo', width: 160 },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier', width: 220 },
  { title: '重量(kg)', dataIndex: 'weight', key: 'weight', width: 120 },
  { title: '门幅(cm)', dataIndex: 'width', key: 'width', width: 120 },
  { title: '成分', dataIndex: 'composition', key: 'composition', width: 180 },
  { title: '本订单分配', dataIndex: 'allocatedWeight', key: 'allocatedWeight', width: 150 },
  { title: '批次剩余', dataIndex: 'remainingWeight', key: 'remainingWeight', width: 150 },
  { title: '资源状态', dataIndex: 'resourceStatus', key: 'resourceStatus', width: 130 },
  { title: '计划占用', dataIndex: 'lockedByPlan', key: 'lockedByPlan', width: 130 },
]

const planColumns = [
  { title: '计划ID', dataIndex: 'planId', key: 'planId', width: 100 },
  { title: '工艺路线', dataIndex: 'routeName', key: 'routeName', width: 180 },
  { title: '批次', dataIndex: 'batchNo', key: 'batchNo', width: 160 },
  { title: '开始时间', dataIndex: 'planStartTime', key: 'planStartTime', width: 180 },
  { title: '结束时间', dataIndex: 'planEndTime', key: 'planEndTime', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
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
            <a-table
              :columns="itemColumns"
              :data-source="detail.items || []"
              :pagination="false"
              row-key="orderItemId"
              :scroll="{ x: 1100 }"
            />
          </a-card>

          <a-card class="page-card" :bordered="false" title="关联批次资源">
            <a-table
              :columns="batchColumns"
              :data-source="detail.linkedBatches || []"
              :pagination="false"
              row-key="id"
              :scroll="{ x: 1200 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'resourceStatus'">
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
                  <a-tag :color="record.lockedByPlan ? 'processing' : 'success'">
                    {{ record.lockedByPlan ? `计划 ${record.currentPlanId || ''} 占用` : '可排产' }}
                  </a-tag>
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
              :scroll="{ x: 1200 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="getPlanStatusMeta(record.status)?.color">
                    {{ getPlanStatusMeta(record.status)?.label || record.status || '-' }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'planId'">
                  <a-space>
                    <a-button type="link" @click="jumpToPlanMain(record)">{{ record.planId }}</a-button>
                    <a-button type="link" @click="jumpToPlanBoard(record)">甘特图</a-button>
                  </a-space>
                </template>
                <template v-else-if="column.key === 'remark'">
                  {{ record.remark || '-' }}
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
  .route-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
