<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  BarChartOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  InboxOutlined,
} from '@ant-design/icons-vue'
import { RouterLink } from 'vue-router'
import { getDashboardOverview } from '@/api/dashboard/dashboard'
import {
  batchStatusOptions,
  exceptionLevelOptions,
  exceptionStatusOptions,
  planStatusOptions,
  resultJudgeOptions,
} from '@/constants/dictionaries'
import type { BatchItem, ExceptionRecordItem, ProductionPlanItem, QcRecordItem } from '@/types/domain'
import { parseDateTime } from '@/utils/date'
import { formatInspectionId, formatPlanId, formatPlanStepId } from '@/utils/idFormat'

type MetricCard = {
  title: string
  value: string
  trend: string
  trendType: 'up' | 'down'
  icon: typeof InboxOutlined
  iconBg: string
  iconColor: string
}

const loading = ref(false)
const pendingBatches = ref<BatchItem[]>([])
const todayPendingCount = ref(0)
const yesterdayPendingCount = ref(0)
const recentQcRecords = ref<QcRecordItem[]>([])
const currentWeekQcRecords = ref<QcRecordItem[]>([])
const previousWeekQcRecords = ref<QcRecordItem[]>([])
const exceptionRecords = ref<ExceptionRecordItem[]>([])
const productionPlans = ref<ProductionPlanItem[]>([])

const ACTIVE_EXCEPTION_STATUSES = new Set(['OPEN', 'PROCESSING'])

const formatShortTime = (value?: string | null) => {
  const date = parseDateTime(value)
  if (!date) {
    return '--'
  }

  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

const formatDateLabel = (value?: string | null) => {
  const date = parseDateTime(value)
  if (!date) {
    return '--'
  }

  return date.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
  })
}

const formatRate = (value: number) => `${value.toFixed(1)}%`

const formatTrend = (current: number, previous: number) => {
  if (previous <= 0) {
    return {
      trend: current > 0 ? `+${current}` : '0',
      trendType: 'up' as const,
    }
  }

  const delta = ((current - previous) / previous) * 100
  return {
    trend: `${Math.abs(delta).toFixed(1)}%`,
    trendType: delta >= 0 ? ('up' as const) : ('down' as const),
  }
}

const formatWeightCompact = (totalWeight: number) => {
  if (totalWeight >= 1000) {
    return `${(totalWeight / 1000).toFixed(totalWeight >= 10000 ? 0 : 1)}k`
  }
  return `${Math.round(totalWeight)}`
}

const getStatusMeta = (status?: string | null) =>
  batchStatusOptions.find((item) => item.value === status)

const getJudgeMeta = (judge?: string | null) =>
  resultJudgeOptions.find((item) => item.value === judge)

const getExceptionMeta = (level?: string | null) =>
  exceptionLevelOptions.find((item) => item.value === level)

const getPlanMeta = (status?: string | null) =>
  planStatusOptions.find((item) => item.value === status)

const getPendingBatchWeight = (batches: BatchItem[]) =>
  batches.reduce((total, item) => total + Number(item.weight ?? 0), 0)

const getPassRate = (records: QcRecordItem[]) => {
  if (!records.length) {
    return 0
  }

  const passCount = records.filter((item) => item.resultJudge === 'PASS').length
  return (passCount / records.length) * 100
}

const schedulePoolLoad = computed(() => getPendingBatchWeight(pendingBatches.value))
const activeExceptionCount = computed(
  () => exceptionRecords.value.filter((item) => ACTIVE_EXCEPTION_STATUSES.has(item.status)).length,
)
const highSeverityExceptionCount = computed(
  () =>
    exceptionRecords.value.filter((item) =>
      ACTIVE_EXCEPTION_STATUSES.has(item.status) && ['HIGH', 'CRITICAL'].includes(item.exceptionLevel),
    ).length,
)
const weeklyPassRate = computed(() => getPassRate(currentWeekQcRecords.value))
const previousWeeklyPassRate = computed(() => getPassRate(previousWeekQcRecords.value))
const activePlanCount = computed(
  () => productionPlans.value.filter((item) => !['COMPLETED', 'CANCELLED'].includes(item.status)).length,
)

const metricCards = computed<MetricCard[]>(() => {
  const todayTrend = formatTrend(todayPendingCount.value, yesterdayPendingCount.value)
  const weeklyTrend = formatTrend(weeklyPassRate.value, previousWeeklyPassRate.value)

  return [
    {
      title: '今日待检批次',
      value: String(todayPendingCount.value),
      trend: todayTrend.trend,
      trendType: todayTrend.trendType,
      icon: InboxOutlined,
      iconBg: '#fff7ed',
      iconColor: '#ff7a45',
    },
    {
      title: '异常拒收数',
      value: String(activeExceptionCount.value),
      trend: `高危 ${highSeverityExceptionCount.value}`,
      trendType: highSeverityExceptionCount.value > 0 ? 'up' : 'down',
      icon: ExclamationCircleOutlined,
      iconBg: '#fef2f2',
      iconColor: '#ef4444',
    },
    {
      title: '本周放行率',
      value: formatRate(weeklyPassRate.value),
      trend: weeklyTrend.trend,
      trendType: weeklyTrend.trendType,
      icon: CheckCircleOutlined,
      iconBg: '#f0fdf4',
      iconColor: '#22c55e',
    },
    {
      title: '排产池负荷',
      value: formatWeightCompact(schedulePoolLoad.value),
      trend: `${activePlanCount.value}项`,
      trendType: activePlanCount.value > 0 ? 'up' : 'down',
      icon: BarChartOutlined,
      iconBg: '#fff7ed',
      iconColor: '#ff7a45',
    },
  ]
})

const qualityActivities = computed(() =>
  [...recentQcRecords.value]
    .sort((left, right) => {
      const leftTime = parseDateTime(left.inspectTime)?.getTime() ?? 0
      const rightTime = parseDateTime(right.inspectTime)?.getTime() ?? 0
      return rightTime - leftTime
    })
    .slice(0, 2),
)

const upcomingBatches = computed(() =>
  [...pendingBatches.value]
    .sort((left, right) => {
      const leftTime = parseDateTime(left.inDate)?.getTime() ?? 0
      const rightTime = parseDateTime(right.inDate)?.getTime() ?? 0
      return leftTime - rightTime
    })
    .slice(0, 2),
)

const loadDashboard = async () => {
  loading.value = true
  try {
    const response = await getDashboardOverview()
    const overview = response.data
    pendingBatches.value = overview.pendingBatches ?? []
    todayPendingCount.value = overview.todayPendingCount ?? 0
    yesterdayPendingCount.value = overview.yesterdayPendingCount ?? 0
    currentWeekQcRecords.value = overview.currentWeekQcRecords ?? []
    previousWeekQcRecords.value = overview.previousWeekQcRecords ?? []
    recentQcRecords.value = overview.recentQcRecords ?? []
    exceptionRecords.value = overview.exceptionRecords ?? []
    productionPlans.value = overview.productionPlans ?? []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <a-spin class="dashboard-spin" :spinning="loading">
    <div class="dashboard-page">
      <div class="metric-grid">
        <a-card v-for="item in metricCards" :key="item.title" class="metric-card" :bordered="false">
          <div class="metric-header">
            <div class="metric-icon" :style="{ background: item.iconBg, color: item.iconColor }">
              <component :is="item.icon" />
            </div>
            <div class="metric-trend" :class="item.trendType">
              <component :is="item.trendType === 'up' ? ArrowUpOutlined : ArrowDownOutlined" class="trend-arrow" />
              {{ item.trend }}
            </div>
          </div>
          <div class="metric-title">{{ item.title }}</div>
          <div class="metric-value">{{ item.value }}</div>
        </a-card>
      </div>

      <div class="panel-grid">
        <a-card class="panel-card" :bordered="false">
          <template #title>
            <div class="panel-title">
              <div class="title-dot"></div>
              实时质检动态
            </div>
          </template>
          <template #extra>
            <RouterLink class="panel-link" to="/quality/realtime">查看全部</RouterLink>
          </template>

          <div v-if="qualityActivities.length" class="activity-list">
            <div v-for="item in qualityActivities" :key="item.inspectionId" class="activity-item">
              <div class="activity-left">
                <div class="activity-indicator" :class="(getJudgeMeta(item.resultJudge)?.value || '').toLowerCase()"></div>
                <div>
                  <div class="activity-title">质检记录 {{ formatInspectionId(item.inspectionId) }}</div>
                  <div class="activity-subtitle">工序计划：{{ formatPlanStepId(item.planStepId) }}</div>
                </div>
              </div>

              <div class="activity-right">
                <div class="activity-time">{{ formatShortTime(item.inspectTime) }}</div>
                <a-tag :color="getJudgeMeta(item.resultJudge)?.color || 'default'">
                  {{ getJudgeMeta(item.resultJudge)?.label || item.resultJudge }}
                </a-tag>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无质检动态" />
        </a-card>

        <a-card class="panel-card" :bordered="false">
          <template #title>
            <div class="panel-title">
              <div class="title-dot"></div>
              即将排产（高优先级）
            </div>
          </template>
          <template #extra>
            <RouterLink class="panel-link" to="/schedule/pool">进入排产板</RouterLink>
          </template>

          <div v-if="upcomingBatches.length" class="activity-list">
            <div v-for="item in upcomingBatches" :key="item.batchId" class="activity-item">
              <div class="activity-left">
                <div>
                  <div class="activity-title-row">
                    <div class="activity-title">{{ item.batchNo }}</div>
                    <a-tag :color="getStatusMeta(item.status)?.color || 'default'">
                      {{ getStatusMeta(item.status)?.label || item.status || '待排产' }}
                    </a-tag>
                  </div>
                  <div class="activity-subtitle">{{ item.composition || item.supplier || '未提供批次说明' }}</div>
                </div>
              </div>

              <div class="activity-right">
                <div class="schedule-weight">{{ item.weight ?? '--' }}</div>
                <div class="schedule-caption">入厂：{{ formatDateLabel(item.inDate) }}</div>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无待排产批次" />
        </a-card>
      </div>

      <div class="summary-grid">
        <a-card class="summary-card" :bordered="false">
          <div class="summary-header">异常概览</div>
          <div class="summary-row">
            <span>待处理异常</span>
            <strong>{{ activeExceptionCount }}</strong>
          </div>
          <div class="summary-row">
            <span>高危异常</span>
            <strong>{{ highSeverityExceptionCount }}</strong>
          </div>
          <div class="summary-list">
            <div v-for="item in exceptionRecords.slice(0, 3)" :key="item.exceptionId" class="summary-item">
              <span>#{{ item.exceptionId }}</span>
              <a-tag :color="getExceptionMeta(item.exceptionLevel)?.color || 'default'">
                {{ getExceptionMeta(item.exceptionLevel)?.label || item.exceptionLevel }}
              </a-tag>
              <a-tag :color="exceptionStatusOptions.find((status) => status.value === item.status)?.color || 'default'">
                {{ exceptionStatusOptions.find((status) => status.value === item.status)?.label || item.status }}
              </a-tag>
            </div>
          </div>
        </a-card>

        <a-card class="summary-card" :bordered="false">
          <div class="summary-header">计划概览</div>
          <div class="summary-row">
            <span>活动计划</span>
            <strong>{{ activePlanCount }}</strong>
          </div>
          <div class="summary-row">
            <span>排产池负荷</span>
            <strong>{{ schedulePoolLoad.toFixed(0) }} kg</strong>
          </div>
          <div class="summary-list">
            <div v-for="item in productionPlans.slice(0, 3)" :key="item.planId" class="summary-item">
              <a-tooltip :title="String(item.planId)">
                <span>{{ formatPlanId(item.planId) }}</span>
              </a-tooltip>
              <span>{{ item.batchNo || `批次 ${item.batchId}` }}</span>
              <a-tag :color="getPlanMeta(item.status)?.color || 'default'">
                {{ getPlanMeta(item.status)?.label || item.status }}
              </a-tag>
            </div>
          </div>
        </a-card>
      </div>
    </div>
  </a-spin>
</template>

<style scoped>
.dashboard-spin {
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.dashboard-spin :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.dashboard-page {
  display: flex;
  flex-direction: column;
  height: auto;
  min-height: 0;
  gap: 10px;
  overflow: visible;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  min-height: 0;
}

.metric-card,
.panel-card,
.summary-card {
  border-radius: 13px;
  border: 1px solid rgba(145, 158, 171, 0.12);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
}

.panel-card {
  min-height: 210px;
}

.metric-card {
  min-height: 100px;
}

.metric-card :deep(.ant-card-body) {
  padding: 12px 16px 13px;
}

.panel-card :deep(.ant-card-head),
.summary-card :deep(.ant-card-head) {
  min-height: 41px;
  padding: 0 16px;
}

.panel-card :deep(.ant-card-head-title),
.panel-card :deep(.ant-card-extra) {
  padding: 9px 0;
}

.panel-card :deep(.ant-card-body),
.summary-card :deep(.ant-card-body) {
  padding: 12px 16px;
}

.metric-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.metric-icon {
  width: 32px;
  height: 32px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.metric-trend {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 25px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.metric-trend.up {
  background: rgba(34, 197, 94, 0.12);
  color: #22c55e;
}

.metric-trend.down {
  background: rgba(239, 68, 68, 0.10);
  color: #ef4444;
}

.trend-arrow {
  font-size: 12px;
}

.metric-title {
  margin-bottom: 7px;
  color: #536682;
  font-size: 14px;
  font-weight: 700;
}

.metric-value {
  color: #18243d;
  font-size: 29px;
  font-weight: 800;
  line-height: 1;
}

.panel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  min-height: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #18243d;
  font-size: 14px;
  font-weight: 800;
}

.title-dot {
  width: 5px;
  height: 18px;
  border-radius: 999px;
  background: #ff7a45;
}

.panel-link {
  color: #ff7a45;
  font-size: 12px;
  font-weight: 700;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.activity-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 58px;
  padding: 0 14px;
  border-radius: 11px;
  background: #f8fbff;
  border: 1px solid rgba(145, 158, 171, 0.10);
}

.activity-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.activity-indicator {
  width: 11px;
  height: 11px;
  border-radius: 999px;
  box-shadow: 0 0 0 5px rgba(148, 163, 184, 0.08);
}

.activity-indicator.pass {
  background: #22c55e;
}

.activity-indicator.fail {
  background: #ef4444;
}

.activity-indicator.recheck {
  background: #f59e0b;
}

.activity-title-row {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 4px;
}

.activity-title {
  color: #18243d;
  font-size: 13px;
  font-weight: 800;
}

.activity-subtitle {
  color: #6a7f9b;
  font-size: 11px;
}

.activity-right {
  text-align: right;
}

.activity-time {
  margin-bottom: 4px;
  color: #9aa9bf;
  font-size: 11px;
}

.schedule-weight {
  color: #18243d;
  font-size: 14px;
  font-weight: 800;
}

.schedule-caption {
  margin-top: 4px;
  color: #6a7f9b;
  font-size: 11px;
}

.summary-grid {
  display: grid;
  min-height: 0;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.summary-card {
  min-height: 142px;
}

.summary-header {
  margin-bottom: 8px;
  color: #18243d;
  font-size: 14px;
  font-weight: 800;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid rgba(145, 158, 171, 0.12);
  color: #536682;
}

.summary-row strong {
  color: #18243d;
  font-size: 14px;
  font-weight: 800;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 8px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #536682;
  font-size: 11px;
}

@media (min-width: 1025px) {
  .dashboard-page,
  .metric-grid,
  .panel-grid,
  .summary-grid {
    gap: 9px;
  }

  .metric-card,
  .panel-card,
  .summary-card {
    border-radius: 12px;
    box-shadow: 0 9px 25px rgba(15, 23, 42, 0.05);
  }

  .panel-card {
    min-height: 189px;
  }

  .metric-card {
    min-height: 90px;
  }

  .metric-card :deep(.ant-card-body) {
    padding: 11px 14px 12px;
  }

  .panel-card :deep(.ant-card-head),
  .summary-card :deep(.ant-card-head) {
    min-height: 37px;
    padding: 0 14px;
  }

  .panel-card :deep(.ant-card-head-title),
  .panel-card :deep(.ant-card-extra) {
    padding: 8px 0;
  }

  .panel-card :deep(.ant-card-body),
  .summary-card :deep(.ant-card-body) {
    padding: 11px 14px;
  }

  .metric-header {
    margin-bottom: 9px;
  }

  .metric-icon {
    width: 29px;
    height: 29px;
    border-radius: 10px;
    font-size: 15px;
  }

  .metric-trend {
    min-height: 23px;
    padding: 0 8px;
    font-size: 11px;
  }

  .metric-title {
    margin-bottom: 6px;
    font-size: 13px;
  }

  .metric-value {
    font-size: 26px;
  }

  .panel-title {
    gap: 8px;
    font-size: 13px;
  }

  .title-dot {
    height: 16px;
  }

  .activity-list {
    gap: 7px;
  }

  .activity-item {
    min-height: 52px;
    padding: 0 12px;
    border-radius: 10px;
  }

  .activity-left {
    gap: 9px;
  }

  .activity-title-row {
    gap: 8px;
    margin-bottom: 3px;
  }

  .activity-title {
    font-size: 12px;
  }

  .summary-card {
    min-height: 128px;
  }

  .summary-header {
    margin-bottom: 7px;
    font-size: 13px;
  }

  .summary-row {
    padding: 5px 0;
  }

  .summary-list {
    gap: 5px;
    padding-top: 7px;
  }
}

@media (max-width: 1400px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1024px) {
  .panel-grid,
  .summary-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .activity-item {
    min-height: auto;
    padding: 20px;
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }

  .activity-right {
    text-align: left;
  }
}
</style>
