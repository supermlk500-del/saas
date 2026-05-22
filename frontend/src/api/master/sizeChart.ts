import request, { type RuoYiListResponse } from '@/utils/request'

export type SizeChartItem = {
  key: string
  code: string
  name: string
  range: string
  sizes: string
  status: string
}

export type SizeChartQuery = {
  keyword?: string
  status?: string
}

export function listSizeCharts(query?: SizeChartQuery) {
  return request<RuoYiListResponse<SizeChartItem>>({
    url: '/business/size-chart/list',
    method: 'get',
    params: query,
  })
}

export const fetchSizeCharts = async (query?: SizeChartQuery): Promise<SizeChartItem[]> => {
  const res = await listSizeCharts(query)
  return res.rows ?? []
}
