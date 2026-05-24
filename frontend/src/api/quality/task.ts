import { fetchPlanSteps } from '@/api/plan/planStep'
import { fetchQcRecords } from '@/api/quality/qcRecord'
import type { PlanStepItem } from '@/types/domain'
import type { PageResult } from '@/types/http'

export type TaskItem = {
  taskNo: string
  planStepId: number | string
  planId: number
  stepName?: string
  machineName?: string
  latestInspectTime?: string
  latestJudge?: string
  status: '待检验' | '待处理' | '已检验'
}

export type TaskQuery = {
  keyword?: string
  status?: string
}

const buildTaskStatus = (planStep: PlanStepItem, latestJudge?: string) => {
  if (!latestJudge) {
    return '待检验' as const
  }
  if (latestJudge === 'FAIL' || latestJudge === 'RECHECK') {
    return '待处理' as const
  }
  return '已检验' as const
}

export const fetchTasks = async (query?: TaskQuery): Promise<PageResult<TaskItem>> => {
  const [planStepRes, qcRecordRes] = await Promise.all([
    fetchPlanSteps({ pageNum: 1, pageSize: 500 }),
    fetchQcRecords({ pageNum: 1, pageSize: 500 }),
  ])

  const qcMap = new Map<string, { inspectTime?: string; resultJudge?: string }>()
  for (const item of qcRecordRes.list) {
    const planStepKey = String(item.planStepId)
    const current = qcMap.get(planStepKey)
    if (!current || (item.inspectTime || '') > (current.inspectTime || '')) {
      qcMap.set(planStepKey, {
        inspectTime: item.inspectTime,
        resultJudge: item.resultJudge,
      })
    }
  }

  const tasks = planStepRes.list
    .map((item) => {
      const latest = qcMap.get(String(item.planStepId))
      return {
        taskNo: `IQC-${item.planStepId}`,
        planStepId: item.planStepId,
        planId: item.planId,
        stepName: item.stepName,
        machineName: item.machineName,
        latestInspectTime: latest?.inspectTime,
        latestJudge: latest?.resultJudge,
        status: buildTaskStatus(item, latest?.resultJudge),
      }
    })
    .filter((item) => {
      const hitKeyword =
        !query?.keyword ||
        item.taskNo.includes(query.keyword) ||
        String(item.planStepId).includes(query.keyword) ||
        (item.stepName || '').includes(query.keyword)
      const hitStatus = !query?.status || item.status === query.status
      return hitKeyword && hitStatus
    })

  return {
    list: tasks,
    total: tasks.length,
  }
}
