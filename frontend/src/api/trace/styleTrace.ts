import request, { type RuoYiListResponse } from '@/utils/request'

export type StyleTraceItem = {
  key: string
  styleName: string
  orderNo: string
  customer: string
  color: string
  size: string
  totalQty: number
  completedQty: number
  defectQty: number
  status: string
}

export type StyleTraceQuery = {
  keyword?: string
  status?: string
}

export function listStyleTrace(query?: StyleTraceQuery) {
  return request<RuoYiListResponse<StyleTraceItem>>({
    url: '/business/style-trace/list',
    method: 'get',
    params: query,
  })
}

export const fetchStyleTrace = async (query?: StyleTraceQuery): Promise<StyleTraceItem[]> => {
  const res = await listStyleTrace(query)
  return res.rows ?? []
}
