import request, { type RuoYiListResponse } from '@/utils/request'

export type OperationItem = {
  key: string
  orderNo: string
  processName: string
  step: number
  equipment: string
  planStart: string
  planEnd: string
  status: string
}

export type OperationQuery = {
  keyword?: string
  status?: string
}

export function listOperations(query?: OperationQuery) {
  return request<RuoYiListResponse<OperationItem>>({
    url: '/business/operation/list',
    method: 'get',
    params: query,
  })
}

export const fetchOperations = async (query?: OperationQuery): Promise<OperationItem[]> => {
  const res = await listOperations(query)
  return res.rows ?? []
}
