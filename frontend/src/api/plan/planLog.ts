// 接口暂时停用：/business/plan-log/list

export type PlanLogItem = {
  key: string
  planNo: string
  action: string
  operator: string
  time: string
  detail: string
}

export type PlanLogQuery = {
  keyword?: string
  action?: string
}

export const fetchPlanLogs = async (_query?: PlanLogQuery): Promise<PlanLogItem[]> => {
  return []
}
