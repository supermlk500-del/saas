import request, { type RuoYiListResponse } from '@/utils/request'

export type SnTraceItem = {
  key: string
  sn: string
  styleName: string
  color: string
  size: string
  batchNo: string
  cutting: string
  sewing: string
  iron: string
  pack: string
  qcResult: string
  status: string
}

export type SnTraceQuery = {
  keyword?: string
  qcResult?: string
}

export function listSnTrace(query?: SnTraceQuery) {
  return request<RuoYiListResponse<SnTraceItem>>({
    url: '/business/sn-trace/list',
    method: 'get',
    params: query,
  })
}

export const fetchSnTrace = async (query?: SnTraceQuery): Promise<SnTraceItem[]> => {
  const res = await listSnTrace(query)
  return res.rows ?? []
}
