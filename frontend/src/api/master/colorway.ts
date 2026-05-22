import request, { type RuoYiListResponse } from '@/utils/request'

export type ColorwayItem = {
  key: string
  code: string
  name: string
  colorNo: string
  status: string
}

export type ColorwayQuery = {
  keyword?: string
  status?: string
}

export function listColorways(query?: ColorwayQuery) {
  return request<RuoYiListResponse<ColorwayItem>>({
    url: '/business/colorway/list',
    method: 'get',
    params: query,
  })
}

export const fetchColorways = async (query?: ColorwayQuery): Promise<ColorwayItem[]> => {
  const res = await listColorways(query)
  return res.rows ?? []
}
