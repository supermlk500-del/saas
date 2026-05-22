// 接口暂时停用：/business/order/list

export type OrderItem = {
  key: string
  no: string
  customer: string
  fabricType: string
  swatchColor: string
  targetQty: number
  dueDate: string
  status: string
  qcAlert?: boolean
}

export type OrderQuery = {
  orderNo?: string
  customer?: string
  status?: string
}

export const fetchOrders = async (_query?: OrderQuery): Promise<OrderItem[]> => {
  return []
}
