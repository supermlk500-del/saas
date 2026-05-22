<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPlans, getPlanGantt, type GanttResponse } from '@/api/plan/plan'
import { planStepStatusOptions } from '@/constants/dictionaries'
import type { GanttTaskItem, ProductionPlanItem } from '@/types/domain'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const planLoading = ref(false)
const planOptions = ref<ProductionPlanItem[]>([])
const gantt = ref<GanttResponse | null>(null)
const ganttCardRef = ref<HTMLElement | null>(null)
const trackWidth = ref(0)
let resizeObserver: ResizeObserver | null = null

type GanttRow = GanttTaskItem & {
  left: number
  width: number
  statusLabel: string
  statusClass: string
}

const filterForm = reactive({
  planId: undefined as number | undefined,
})

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

const parseDate = (value?: string | null) => {
  if (!value) {
    return null
  }

  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

const formatDateTime = (value?: string | null) => {
  const date = parseDate(value)
  if (!date) {
    return '-'
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const getStatusMeta = (status?: string) =>
  planStepStatusOptions.find((item) => item.value === status)

const getStatusMinWidthPx = (status?: string) => {
  switch (status) {
    case 'PENDING':
      return 118
    case 'READY':
    case 'RUNNING':
    case 'PAUSED':
    case 'FINISHED':
      return 108
    case 'ABNORMAL':
      return 96
    default:
      return 108
  }
}

const taskTimes = computed(() => {
  const tasks = gantt.value?.tasks ?? []
  const timestamps = tasks.flatMap((item) => {
    const start = parseDate(item.start)?.getTime()
    const end = parseDate(item.end)?.getTime()
    return [start, end].filter((value): value is number => typeof value === 'number')
  })

  if (!timestamps.length) {
    return null
  }

  return {
    min: Math.min(...timestamps),
    max: Math.max(...timestamps),
  }
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
    }))
  }

  const span = range.max - range.min

  return tasks.map((task) => {
    const start = parseDate(task.start)?.getTime() ?? range.min
    const end = parseDate(task.end)?.getTime() ?? range.min
    const rawLeft = ((start - range.min) / span) * 100
    const rawWidth = ((end - start) / span) * 100
    const statusMeta = getStatusMeta(task.status)
    const minWidthPx = getStatusMinWidthPx(task.status)
    const minWidthPercent = trackWidth.value > 0 ? Math.min((minWidthPx / trackWidth.value) * 100, 100) : 14
    const desiredWidth = Math.max(rawWidth, minWidthPercent)
    const safeLeft = Math.max(0, Math.min(rawLeft, 100 - minWidthPercent))
    const safeWidth = Math.min(desiredWidth, 100 - safeLeft)

    return {
      ...task,
      left: safeLeft,
      width: safeWidth,
      statusLabel: statusMeta?.label || task.status,
      statusClass: task.status?.toLowerCase() || 'default',
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

const updateTrackWidth = () => {
  const firstTrack = ganttCardRef.value?.querySelector('.task-track') as HTMLElement | null
  trackWidth.value = firstTrack?.clientWidth ?? 0
}

const fetchGanttData = async (planId: number) => {
  loading.value = true
  try {
    const response = await getPlanGantt(planId)
    gantt.value = response.data
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!filterForm.planId) {
    gantt.value = null
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
  await router.replace({
    path: '/schedule/board',
    query: {},
  })
}

watch(
  () => route.query.planId,
  async (planId) => {
    if (!planId) {
      return
    }

    const numericId = Number(planId)
    if (!Number.isNaN(numericId) && numericId > 0) {
      filterForm.planId = numericId
      await fetchGanttData(numericId)
    }
  },
  { immediate: true },
)

watch(
  () => ganttRows.value.length,
  async () => {
    await nextTick()
    updateTrackWidth()
  },
)

onMounted(async () => {
  await nextTick()
  updateTrackWidth()

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      updateTrackWidth()
    })

    if (ganttCardRef.value) {
      resizeObserver.observe(ganttCardRef.value)
    }
  }

  await loadPlanOptions()

  if (!filterForm.planId && planOptions.value.length === 1) {
    filterForm.planId = planOptions.value[0]?.planId
    if (filterForm.planId) {
      await handleSearch()
    }
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
})
</script>

<template>
  <div class="gantt-page">
    <a-card class="page-card filter-card" :bordered="false">
      <a-form layout="inline">
        <a-form-item label="生产计划">
          <a-select
            v-model:value="filterForm.planId"
            :options="planOptions.map((item) => ({ label: `#${item.planId} / ${item.batchNo || item.batchId}`, value: item.planId }))"
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

    <a-card ref="ganttCardRef" class="page-card gantt-card" :bordered="false">
      <template v-if="ganttRows.length">
        <div class="gantt-header">
          <div>工序</div>
          <div>设备</div>
          <div>开始 / 结束</div>
          <div>调度条</div>
        </div>

        <div class="gantt-list">
          <div v-for="task in ganttRows" :key="task.planStepId" class="gantt-row">
            <div class="task-step">
              <div class="task-title">{{ task.stepName || `工序 ${task.planStepId}` }}</div>
              <div class="task-sub">工序计划ID：{{ task.planStepId }}</div>
            </div>

            <div class="task-machine">{{ task.machineName || '未分配设备' }}</div>

            <div class="task-time">
              <div>{{ formatDateTime(task.start) }}</div>
              <div>{{ formatDateTime(task.end) }}</div>
            </div>

            <div class="task-track">
              <div
                class="task-bar"
                :class="`status-${task.statusClass}`"
                :style="{ left: `${task.left}%`, width: `${task.width}%` }"
              >
                <span class="task-bar-label">{{ task.statusLabel }}</span>
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
}

.filter-card {
  padding-bottom: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  min-height: 120px;
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

.gantt-header,
.gantt-row {
  display: grid;
  grid-template-columns: 240px 180px 240px 1fr;
  gap: 16px;
  align-items: center;
}

.gantt-header {
  padding: 0 0 16px;
  color: #667085;
  font-size: 13px;
  font-weight: 700;
  border-bottom: 1px solid rgba(145, 158, 171, 0.16);
}

.gantt-list {
  display: flex;
  flex-direction: column;
}

.gantt-row {
  min-height: 88px;
  padding: 18px 0;
  border-bottom: 1px solid rgba(145, 158, 171, 0.12);
}

.task-title {
  color: #1f2937;
  font-weight: 700;
}

.task-sub {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.task-machine,
.task-time {
  color: #344054;
}

.task-track {
  position: relative;
  height: 44px;
  border-radius: 999px;
  background:
    linear-gradient(90deg, rgba(214, 111, 34, 0.06) 0%, rgba(214, 111, 34, 0.02) 100%),
    #f8fafc;
  overflow: hidden;
}

.task-bar {
  position: absolute;
  top: 6px;
  height: 32px;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border-radius: 999px;
  overflow: hidden;
  box-sizing: border-box;
}

.task-bar-label {
  color: inherit;
  font-size: 13px;
  font-weight: 700;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-bar.status-pending {
  color: #925834;
  background: rgba(214, 111, 34, 0.14);
  box-shadow: inset 0 0 0 1px rgba(214, 111, 34, 0.28);
}

.task-bar.status-ready {
  color: #2452d6;
  background: rgba(59, 130, 246, 0.14);
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.28);
}

.task-bar.status-running {
  color: #117a56;
  background: rgba(34, 197, 94, 0.16);
  box-shadow: inset 0 0 0 1px rgba(34, 197, 94, 0.28);
}

.task-bar.status-paused {
  color: #9a6700;
  background: rgba(245, 158, 11, 0.16);
  box-shadow: inset 0 0 0 1px rgba(245, 158, 11, 0.30);
}

.task-bar.status-finished {
  color: #0f766e;
  background: rgba(20, 184, 166, 0.16);
  box-shadow: inset 0 0 0 1px rgba(20, 184, 166, 0.28);
}

.task-bar.status-abnormal {
  color: #b42318;
  background: rgba(239, 68, 68, 0.15);
  box-shadow: inset 0 0 0 1px rgba(239, 68, 68, 0.28);
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }

  .gantt-header {
    display: none;
  }

  .gantt-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
