import request, { type RuoYiResponse } from '@/utils/request'

export type OrderDetailItem = {
  key: string
  itemNo: number
  productName: string
  spec: string
  qty: number
  unit: string
  dueDate: string
  status: string
}

export function getOrderDetail(orderId: string) {
  return request<RuoYiResponse<OrderDetailItem[]>>({
    url: '/business/order-detail/' + orderId,
    method: 'get',
  })
}

export const fetchOrderDetail = async (orderId: string): Promise<OrderDetailItem[]> => {
  const res = await getOrderDetail(orderId)
  return res.data ?? []
}
