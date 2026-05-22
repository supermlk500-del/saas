import request, { type RuoYiListResponse } from '@/utils/request'

export type MaterialTraceItem = {
  key: string
  fabricName: string
  fabricCode: string
  supplier: string
  usedBy: string
  batchNo: string
  qty: number
  unit: string
  status: string
}

export type MaterialTraceQuery = {
  keyword?: string
  status?: string
}

export function listMaterialTrace(query?: MaterialTraceQuery) {
  return request<RuoYiListResponse<MaterialTraceItem>>({
    url: '/business/material-trace/list',
    method: 'get',
    params: query,
  })
}

export const fetchMaterialTrace = async (query?: MaterialTraceQuery): Promise<MaterialTraceItem[]> => {
  const res = await listMaterialTrace(query)
  return res.rows ?? []
}
