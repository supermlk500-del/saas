import request, { type RuoYiListResponse } from '@/utils/request'

export type WorkOrderItem = {
  key: string
  woNo: string
  orderNo: string
  productName: string
  qty: number
  line: string
  status: string
}

export type WorkOrderQuery = {
  keyword?: string
  status?: string
}

export function listWorkOrders(query?: WorkOrderQuery) {
  return request<RuoYiListResponse<WorkOrderItem>>({
    url: '/business/workorder/list',
    method: 'get',
    params: query,
  })
}

export const fetchWorkOrders = async (query?: WorkOrderQuery): Promise<WorkOrderItem[]> => {
  const res = await listWorkOrders(query)
  return res.rows ?? []
}
