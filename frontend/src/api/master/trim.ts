import request, { type RuoYiListResponse } from '@/utils/request'

export type TrimItem = {
  key: string
  code: string
  name: string
  type: string
  supplier: string
  status: string
}

export type TrimQuery = {
  keyword?: string
  type?: string
}

export function listTrims(query?: TrimQuery) {
  return request<RuoYiListResponse<TrimItem>>({
    url: '/business/trim/list',
    method: 'get',
    params: query,
  })
}

export const fetchTrims = async (query?: TrimQuery): Promise<TrimItem[]> => {
  const res = await listTrims(query)
  return res.rows ?? []
}
