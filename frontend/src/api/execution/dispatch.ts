import request, { type RuoYiListResponse } from '@/utils/request'

export type DispatchItem = {
  key: string
  dispatchNo: string
  woNo: string
  equipment: string
  operator: string
  startTime: string
  status: string
}

export type DispatchQuery = {
  keyword?: string
  status?: string
}

export function listDispatches(query?: DispatchQuery) {
  return request<RuoYiListResponse<DispatchItem>>({
    url: '/business/dispatch/list',
    method: 'get',
    params: query,
  })
}

export const fetchDispatches = async (query?: DispatchQuery): Promise<DispatchItem[]> => {
  const res = await listDispatches(query)
  return res.rows ?? []
}
