import request, { type RuoYiListResponse } from '@/utils/request'

export type PlanItem = {
  key: string
  planNo: string
  orderNo: string
  line: string
  status: string
}


export type PlanQuery = {
  planNo?: string
  orderNo?: string
}

export function listPlans(query?: PlanQuery) {
  return request<RuoYiListResponse<PlanItem>>({
    url: '/business/plan/list',
    method: 'get',
    params: query,
  })
}

export const fetchPlans = async (query?: PlanQuery): Promise<PlanItem[]> => {
  const res = await listPlans(query)
  return res.rows ?? []
}
