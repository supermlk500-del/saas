import request, { type RuoYiListResponse } from '@/utils/request'

export type DefectItem = {
  key: string
  defectCode: string
  defectName: string
  category: string
  level: string
  status: string
}

export type DefectQuery = {
  keyword?: string
  category?: string
}

export function listDefects(query?: DefectQuery) {
  return request<RuoYiListResponse<DefectItem>>({
    url: '/business/defect/list',
    method: 'get',
    params: query,
  })
}

export const fetchDefects = async (query?: DefectQuery): Promise<DefectItem[]> => {
  const res = await listDefects(query)
  return res.rows ?? []
}
