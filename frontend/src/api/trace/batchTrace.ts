import request, { type RuoYiListResponse } from '@/utils/request'

export type BatchTraceItem = {
  key: string
  batchNo: string
  styleName: string
  color: string
  size: string
  qty: number
  cuttingDate: string
  sewingDate: string
  ironDate: string
  packDate: string
  qcResult: string
  status: string
}

export type BatchTraceQuery = {
  keyword?: string
  qcResult?: string
}

export function listBatchTrace(query?: BatchTraceQuery) {
  return request<RuoYiListResponse<BatchTraceItem>>({
    url: '/business/batch-trace/list',
    method: 'get',
    params: query,
  })
}

export const fetchBatchTrace = async (query?: BatchTraceQuery): Promise<BatchTraceItem[]> => {
  const res = await listBatchTrace(query)
  return res.rows ?? []
}
