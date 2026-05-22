import request, { type RuoYiListResponse } from '@/utils/request'

export type StrategyItem = {
  key: string
  name: string
  rule: string
  priority: number
  status: string
}

export type StrategyQuery = {
  keyword?: string
  status?: string
}

export function listStrategies(query?: StrategyQuery) {
  return request<RuoYiListResponse<StrategyItem>>({
    url: '/business/strategy/list',
    method: 'get',
    params: query,
  })
}

export const fetchStrategies = async (query?: StrategyQuery): Promise<StrategyItem[]> => {
  const res = await listStrategies(query)
  return res.rows ?? []
}
