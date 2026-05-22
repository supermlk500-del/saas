import request, { type RuoYiListResponse } from '@/utils/request'

export type ReportItem = {
  key: string
  reportNo: string
  woNo: string
  processName: string
  type: string
  operator: string
  time: string
  qty: number
}

export type ReportQuery = {
  keyword?: string
  type?: string
}

export function listReports(query?: ReportQuery) {
  return request<RuoYiListResponse<ReportItem>>({
    url: '/business/report/list',
    method: 'get',
    params: query,
  })
}

export const fetchReports = async (query?: ReportQuery): Promise<ReportItem[]> => {
  const res = await listReports(query)
  return res.rows ?? []
}
