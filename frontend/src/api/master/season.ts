import request, { type RuoYiListResponse } from '@/utils/request'

export type SeasonItem = {
  key: string
  code: string
  name: string
  year: string
  status: string
}

export type SeasonQuery = {
  keyword?: string
  status?: string
}

export function listSeasons(query?: SeasonQuery) {
  return request<RuoYiListResponse<SeasonItem>>({
    url: '/business/season/list',
    method: 'get',
    params: query,
  })
}

export const fetchSeasons = async (query?: SeasonQuery): Promise<SeasonItem[]> => {
  const res = await listSeasons(query)
  return res.rows ?? []
}
