import request, { type RuoYiListResponse } from '@/utils/request'

export type BomItem = {
  key: string
  bomCode: string
  productName: string
  version: string
  status: string
}

export type BomQuery = {
  keyword?: string
  status?: string
}

export function listBoms(query?: BomQuery) {
  return request<RuoYiListResponse<BomItem>>({
    url: '/business/bom/list',
    method: 'get',
    params: query,
  })
}

export const fetchBoms = async (query?: BomQuery): Promise<BomItem[]> => {
  const res = await listBoms(query)
  return res.rows ?? []
}
