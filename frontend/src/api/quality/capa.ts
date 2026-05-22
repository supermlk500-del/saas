import request, { type RuoYiListResponse } from '@/utils/request'

export type CapaItem = {
  key: string
  capaNo: string
  ncrNo: string
  title: string
  rootCause: string
  correctiveAction: string
  owner: string
  deadline: string
  status: string
}

export type CapaQuery = {
  keyword?: string
  status?: string
}

export function listCapas(query?: CapaQuery) {
  return request<RuoYiListResponse<CapaItem>>({
    url: '/business/capa/list',
    method: 'get',
    params: query,
  })
}

export const fetchCapas = async (query?: CapaQuery): Promise<CapaItem[]> => {
  const res = await listCapas(query)
  return res.rows ?? []
}
