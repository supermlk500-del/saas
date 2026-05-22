import request, { type RuoYiListResponse } from '@/utils/request'

export type EquipmentItem = {
  key: string
  code: string
  name: string
  line: string
  status: string
}

export type EquipmentQuery = {
  keyword?: string
  line?: string
}

export function listEquipments(query?: EquipmentQuery) {
  return request<RuoYiListResponse<EquipmentItem>>({
    url: '/business/equipment/list',
    method: 'get',
    params: query,
  })
}

export const fetchEquipments = async (query?: EquipmentQuery): Promise<EquipmentItem[]> => {
  const res = await listEquipments(query)
  return res.rows ?? []
}
