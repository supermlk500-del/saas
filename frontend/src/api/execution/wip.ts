import request, { type RuoYiListResponse } from '@/utils/request'

export type WipItem = {
  key: string
  woNo: string
  productName: string
  processName: string
  qty: number
  line: string
  status: string
}

export type WipQuery = {
  keyword?: string
  line?: string
}

export function listWipItems(query?: WipQuery) {
  return request<RuoYiListResponse<WipItem>>({
    url: '/business/wip/list',
    method: 'get',
    params: query,
  })
}

export const fetchWipItems = async (query?: WipQuery): Promise<WipItem[]> => {
  const res = await listWipItems(query)
  return res.rows ?? []
}
