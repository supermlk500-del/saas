<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPlans, getPlan, getPlanGantt, type GanttResponse } from '@/api/plan/plan'
import { planStepStatusOptions } from '@/constants/dictionaries'
import type { GanttTaskItem, IdValue, ProductionPlanDetailItem, ProductionPlanItem } from '@/types/domain'
import { formatDateTime as formatCommonDateTime, parseDateTime } from '@/utils/date'
import { formatPlanId, formatPlanStepId } from '@/utils/idFormat'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const planLoading = ref(false)
const planOptions = ref<ProductionPlanItem[]>([])
const gantt = ref<GanttResponse | null>(null)
const currentPlanDetail = ref<ProductionPlanDetailItem | null>(null)

const HOUR_MS = 60 * 60 * 1000
const DAY_MS = 24 * HOUR_MS

type GanttRow = GanttTaskItem & {
  left: number
  width: number
  statusLabel: string
  statusClass: string
  durationText: string
}

type TimelineTick = {
  time: number
  left: number
  label: string
  subLabel: string
  major: boolean
}

const filterForm = reactive({
  planId: undefined as IdValue | undefined,
})

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

const loadPlanOptions = async () => {
  planLoading.value = true
  try {
    const response = await fetchPlans({
      pageNum: 1,
      pageSize: 200,
    })
    planOptions.value = response.list
  } finally {
    planLoading.value = false
  }
}

const formatDateTime = (value?: string | null) => formatCommonDateTime(value, false)

const formatShortTime = (time: number) => {
  const date = new Date(time)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatMonthDay = (time: number) => {
  const date = new Date(time)
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const formatDuration = (start?: string | null, end?: string | null) => {
  const startDate = parseDateTime(start)
  const endDate = parseDateTime(end)
  if (!startDate || !endDate || endDate <= startDate) {
    return '-'
  }

  const totalMinutes = Math.round((endDate.getTime() - startDate.getTime()) / 60000)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60

  if (hours && minutes) {
    return `${hours}h ${minutes}m`
  }
  if (hours) {
    return `${hours}h`
  }
  return `${minutes}m`
}

const getStatusMeta = (status?: string) =>
  planStepStatusOptions.find((item) => item.value === status)

const taskTimes = computed(() => {
  const tasks = gantt.value?.tasks ?? []
  const timestamps = tasks.flatMap((item) => {
    const start = parseDateTime(item.start)?.getTime()
    const end = parseDateTime(item.end)?.getTime()
    return [start, end].filter((value): value is number => typeof value === 'number')
  })

  if (!timestamps.length) {
    return null
  }

  const min = Math.min(...timestamps)
  const max = Math.max(...timestamps)
  const padding = Math.max((max - min) * 0.04, HOUR_MS)

  return {
    min: min - padding,
    max: max + padding,
    taskMin: min,
    taskMax: max,
  }
})

const timelineTicks = computed<TimelineTick[]>(() => {
  const range = taskTimes.value
  if (!range || range.max <= range.min) {
    return []
  }

  const span = range.max - range.min
  const step =
    span <= 12 * HOUR_MS
      ? 2 * HOUR_MS
      : span <= 30 * HOUR_MS
        ? 4 * HOUR_MS
        : span <= 72 * HOUR_MS
          ? 6 * HOUR_MS
          : DAY_MS
  const first = Math.floor(range.min / step) * step
  const ticks: TimelineTick[] = []

  for (let time = first; time <= range.max + step; time += step) {
    if (time < range.min) {
      continue
    }

    const date = new Date(time)
    const previous = ticks[ticks.length - 1]
    const major = !previous || new Date(previous.time).getDate() !== date.getDate() || date.getHours() === 0

    ticks.push({
      time,
      left: ((time - range.min) / span) * 100,
      label: step >= DAY_MS ? formatMonthDay(time) : formatShortTime(time),
      subLabel: major ? formatMonthDay(time) : '',
      major,
    })
  }

  return ticks
})

const ganttRows = computed<GanttRow[]>(() => {
  const tasks = gantt.value?.tasks ?? []
  const range = taskTimes.value

  if (!range || range.max === range.min) {
    return tasks.map((task) => ({
      ...task,
      left: 0,
      width: 100,
      statusLabel: getStatusMeta(task.status)?.label || task.status,
      statusClass: task.status?.toLowerCase() || 'default',
      durationText: formatDuration(task.start, task.end),
    }))
  }

  const span = range.max - range.min

  return tasks.map((task) => {
    const start = parseDateTime(task.start)?.getTime() ?? range.min
    const end = parseDateTime(task.end)?.getTime() ?? range.min
    const rawLeft = ((start - range.min) / span) * 100
    const rawWidth = ((Math.max(end, start + 15 * 60 * 1000) - start) / span) * 100
    const statusMeta = getStatusMeta(task.status)
    const minWidthPercent = task.status === 'ABNORMAL' ? 4 : 5
    const desiredWidth = Math.max(rawWidth, minWidthPercent)
    const safeWidth = Math.min(desiredWidth, 100)
    const safeLeft = Math.max(0, Math.min(rawLeft, 100 - safeWidth))

    return {
      ...task,
      left: safeLeft,
      width: safeWidth,
      statusLabel: statusMeta?.label || task.status,
      statusClass: task.status?.toLowerCase() || 'default',
      durationText: formatDuration(task.start, task.end),
    }
  })
})

const ganttSummary = computed(() => {
  const tasks = gantt.value?.tasks ?? []
  const range = taskTimes.value

  return {
    totalTasks: tasks.length,
    runningTasks: tasks.filter((item) => item.status === 'RUNNING').length,
    abnormalTasks: tasks.filter((item) => item.status === 'ABNORMAL').length,
    start: range ? formatDateTime(new Date(range.min).toISOString()) : '-',
    end: range ? formatDateTime(new Date(range.max).toISOString()) : '-',
  }
})

const fetchGanttData = async (planId: IdValue) => {
  loading.value = true
  try {
    const [ganttResponse, detailResponse] = await Promise.all([
      getPlanGantt(planId),
      getPlan(planId),
    ])
    gantt.value = ganttResponse.data
    currentPlanDetail.value = detailResponse.data
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!hasIdValue(filterForm.planId)) {
    gantt.value = null
    currentPlanDetail.value = null
    return
  }

  await router.replace({
    path: '/schedule/board',
    query: { planId: String(filterForm.planId) },
  })

  await fetchGanttData(filterForm.planId)
}

const resetSearch = async () => {
  filterForm.planId = undefined
  gantt.value = null
  currentPlanDetail.value = null
  await router.replace({
    path: '/schedule/board',
    query: {},
  })
}

watch(
  () => route.query.planId,
  async (planId) => {
    const normalizedPlanId = normalizeQueryId(planId)
    if (!hasIdValue(normalizedPlanId)) {
      return
    }

    filterForm.planId = normalizedPlanId
    await fetchGanttData(normalizedPlanId)
  },
  { immediate: true },
)

onMounted(async () => {
  await loadPlanOptions()

  if (!filterForm.planId && planOptions.value.length === 1) {
    filterForm.planId = planOptions.value[0]?.planId
    if (filterForm.planId) {
      await handleSearch()
    }
  }
})
</script>

<template>
  <div class="gantt-page">
    <a-card class="page-card filter-card" :bordered="false">
      <a-form layout="inline">
        <a-form-item label="生产计划">
          <a-select
            v-model:value="filterForm.planId"
            :options="planOptions.map((item) => ({ label: `${formatPlanId(item.planId)} / ${item.orderNo || '未绑定订单'} / ${item.batchNo || item.batchId}`, value: item.planId }))"
            :loading="planLoading"
            placeholder="请选择生产计划"
            show-search
            option-filter-prop="label"
            style="width: 320px"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card v-if="currentPlanDetail" class="page-card" :bordered="false">
      <a-descriptions :column="4" bordered size="small">
        <a-descriptions-item label="订单号">{{ currentPlanDetail.orderInfo?.orderNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ currentPlanDetail.orderInfo?.customerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="订单明细">
          {{ [currentPlanDetail.orderItemInfo?.productCode || currentPlanDetail.orderItemInfo?.productName, currentPlanDetail.orderItemInfo?.specification, currentPlanDetail.orderItemInfo?.color].filter(Boolean).join(' / ') || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="批次">{{ currentPlanDetail.batchInfo?.batchNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="工艺路线">{{ currentPlanDetail.routeInfo?.routeName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="计划开始">{{ currentPlanDetail.planStartTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="计划结束">{{ currentPlanDetail.planEndTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="计划状态">{{ currentPlanDetail.status || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <div class="summary-grid">
      <a-card class="page-card summary-card" :bordered="false">
        <div class="summary-label">工序任务总数</div>
        <div class="summary-value">{{ ganttSummary.totalTasks }}</div>
      </a-card>
      <a-card class="page-card summary-card" :bordered="false">
        <div class="summary-label">执行中工序</div>
        <div class="summary-value">{{ ganttSummary.runningTasks }}</div>
      </a-card>
      <a-card class="page-card summary-card" :bordered="false">
        <div class="summary-label">异常工序</div>
        <div class="summary-value">{{ ganttSummary.abnormalTasks }}</div>
      </a-card>
      <a-card class="page-card summary-card" :bordered="false">
        <div class="summary-label">时间跨度</div>
        <div class="summary-range">{{ ganttSummary.start }} 至 {{ ganttSummary.end }}</div>
      </a-card>
    </div>

    <a-card class="page-card gantt-card" :bordered="false">
      <template v-if="ganttRows.length">
        <div class="gantt-shell">
          <div class="gantt-sidebar">
            <div class="sidebar-head">工序 / 设备</div>
            <div v-for="task in ganttRows" :key="`side-${task.planStepId}`" class="sidebar-row">
              <div class="task-step">
                <div class="task-title">{{ task.stepName || `工序 ${task.planStepId}` }}</div>
                <div class="task-sub">{{ formatPlanStepId(task.planStepId) }} · {{ task.durationText }}</div>
              </div>
              <div class="task-machine">{{ task.machineName || '未分配设备' }}</div>
            </div>
          </div>

          <div class="gantt-timeline">
            <div class="timeline-axis">
              <div
                v-for="tick in timelineTicks"
                :key="tick.time"
                class="timeline-tick"
                :class="{ major: tick.major }"
                :style="{ left: `${tick.left}%` }"
              >
                <span>{{ tick.label }}</span>
                <small v-if="tick.subLabel">{{ tick.subLabel }}</small>
              </div>
            </div>

            <div class="timeline-body">
              <div
                v-for="tick in timelineTicks"
                :key="`grid-${tick.time}`"
                class="timeline-grid-line"
                :class="{ major: tick.major }"
                :style="{ left: `${tick.left}%` }"
              />

              <div v-for="task in ganttRows" :key="task.planStepId" class="timeline-row">
                <a-tooltip :title="`${task.stepName || '-'}｜${formatDateTime(task.start)} - ${formatDateTime(task.end)}｜${task.machineName || '未分配设备'}｜${task.statusLabel}`">
                  <div
                    class="task-bar"
                    :class="`status-${task.statusClass}`"
                    :style="{ left: `${task.left}%`, width: `${task.width}%` }"
                  >
                    <span class="task-bar-title">{{ task.stepName || task.statusLabel }}</span>
                    <span class="task-bar-meta">{{ task.durationText }}</span>
                  </div>
                </a-tooltip>
              </div>
            </div>
          </div>
        </div>
      </template>

      <a-empty v-else description="请选择生产计划后查看甘特图" />
    </a-card>
  </div>
</template>

<style scoped>
.gantt-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  min-width: 0;
  overflow-x: hidden;
}

.filter-card {
  padding-bottom: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  min-width: 0;
}

.summary-card {
  min-height: 120px;
  min-width: 0;
}

.summary-label {
  color: #667085;
  font-size: 14px;
}

.summary-value {
  margin-top: 12px;
  color: #1f2937;
  font-size: 34px;
  font-weight: 700;
}

.summary-range {
  margin-top: 12px;
  color: #1f2937;
  font-size: 16px;
  line-height: 1.7;
  font-weight: 600;
}

.gantt-card :deep(.ant-card-body) {
  overflow: hidden;
  padding: 18px;
}

.gantt-shell {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e6e2da;
  border-radius: 18px;
  background: #fffaf3;
}

.gantt-sidebar {
  min-width: 0;
  z-index: 4;
  border-right: 1px solid #e2ded5;
  background: linear-gradient(180deg, #fffdf8 0%, #fff9ef 100%);
  box-shadow: 14px 0 28px rgba(60, 45, 29, 0.06);
}

.sidebar-head,
.sidebar-row {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.sidebar-head {
  height: 56px;
  padding: 0 16px;
  color: #7b6751;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  background: rgba(255, 248, 236, 0.88);
  border-bottom: 1px solid #e2ded5;
}

.sidebar-row {
  height: 64px;
  padding: 8px 16px;
  border-bottom: 1px solid rgba(226, 222, 213, 0.8);
}

.gantt-timeline {
  width: 100%;
  min-width: 0;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88) 0%, rgba(255, 251, 245, 0.92) 100%);
}

.timeline-axis {
  position: relative;
  z-index: 3;
  height: 56px;
  overflow: hidden;
  border-bottom: 1px solid #e2ded5;
  background: linear-gradient(180deg, #fffdf8 0%, #fbf6ee 100%);
}

.timeline-tick {
  position: absolute;
  top: 0;
  height: 56px;
  transform: translateX(-1px);
  border-left: 1px solid rgba(174, 160, 140, 0.32);
  color: #7a6a58;
  font-size: 12px;
  max-width: 68px;
}

.timeline-tick.major {
  border-left-color: rgba(214, 111, 34, 0.42);
}

.timeline-tick span,
.timeline-tick small {
  display: block;
  margin-left: 8px;
  white-space: nowrap;
}

.timeline-tick span {
  margin-top: 10px;
  font-weight: 800;
}

.timeline-tick small {
  margin-top: 4px;
  color: #a7794f;
  font-size: 11px;
}

.timeline-body {
  position: relative;
  overflow: hidden;
  padding-bottom: 0;
}

.timeline-grid-line {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: rgba(174, 160, 140, 0.18);
  pointer-events: none;
}

.timeline-grid-line.major {
  background: rgba(214, 111, 34, 0.22);
}

.timeline-row {
  position: relative;
  height: 64px;
  overflow: hidden;
  border-bottom: 1px solid rgba(226, 222, 213, 0.72);
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0)),
    repeating-linear-gradient(90deg, rgba(120, 103, 82, 0.035) 0 1px, transparent 1px 72px);
}

.task-title {
  color: #1f2937;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.task-sub {
  margin-top: 2px;
  color: #667085;
  font-size: 12px;
}

.task-machine {
  margin-top: 4px;
  color: #344054;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.task-step {
  min-width: 0;
}

.task-bar {
  position: absolute;
  top: 11px;
  height: 40px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  border-radius: 12px;
  overflow: hidden;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.48);
  box-shadow: 0 10px 24px rgba(104, 72, 38, 0.16);
}

.task-bar-title,
.task-bar-meta {
  color: inherit;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.1;
}

.task-bar-title {
  font-size: 12px;
  font-weight: 800;
}

.task-bar-meta {
  margin-top: 2px;
  font-size: 10px;
  opacity: 0.78;
}

.task-bar.status-pending {
  color: #925834;
  background: linear-gradient(135deg, #fff0df 0%, #f7d7bc 100%);
}

.task-bar.status-ready {
  color: #2452d6;
  background: linear-gradient(135deg, #eaf2ff 0%, #cfe0ff 100%);
}

.task-bar.status-running {
  color: #117a56;
  background: linear-gradient(135deg, #defbea 0%, #b9efd0 100%);
}

.task-bar.status-paused {
  color: #9a6700;
  background: linear-gradient(135deg, #fff6d7 0%, #f9dfa0 100%);
}

.task-bar.status-finished {
  color: #0f766e;
  background: linear-gradient(135deg, #dff8f4 0%, #b9ece5 100%);
}

.task-bar.status-abnormal {
  color: #b42318;
  background: linear-gradient(135deg, #ffe5e5 0%, #ffc1c1 100%);
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }

  .gantt-shell {
    grid-template-columns: 220px minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
