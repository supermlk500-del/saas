import request, { type RuoYiListResponse } from '@/utils/request'

export type MaterialItem = {
  key: string
  code: string
  name: string
  type: string
  unit: string
  status: string
}

export type MaterialQuery = {
  keyword?: string
  type?: string
}

export function listMaterials(query?: MaterialQuery) {
  return request<RuoYiListResponse<MaterialItem>>({
    url: '/business/material/list',
    method: 'get',
    params: query,
  })
}

export const fetchMaterials = async (query?: MaterialQuery): Promise<MaterialItem[]> => {
  const res = await listMaterials(query)
  return res.rows ?? []
}
