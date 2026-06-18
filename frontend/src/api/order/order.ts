import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type {
  IdValue,
  OrderBatchLinkItem,
  OrderDetailAggregate,
  OrderLineItem,
  OrderSchedulePoolItem,
  OrderSummaryItem,
} from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type OrderQuery = PageQuery & {
  orderNo?: string
  customerName?: string
  status?: string
  deliveryDateFrom?: string
  deliveryDateTo?: string
}

export type OrderSchedulePoolQuery = PageQuery & {
  orderNo?: string
  customerName?: string
  status?: string
  priority?: string
  readyForSchedule?: boolean
}

export type OrderUpsertRequest = {
  orderNo: string
  customerName: string
  orderDate: string
  deliveryDate: string
  priority?: string
  status?: string
  remark?: string
}

export type OrderStatusPatchRequest = {
  status: string
  reason?: string
}

export type OrderItemUpsertRequest = {
  productCode?: string
  productName?: string
  specification?: string
  color?: string
  quantity?: number | null
  unit?: string
  requiredWidth?: number | null
  requiredWeight?: number | null
  remark?: string
}

export type OrderBatchLinkUpsertRequest = {
  orderItemId?: IdValue
  batchId: IdValue
  allocatedWeight?: number | null
  allocatedQuantity?: number | null
  remark?: string
}

export const listOrders = (query?: OrderQuery) =>
  request<ApiListResponse<OrderSummaryItem>>({
    url: '/api/orders',
    method: 'get',
    params: query,
  })

export const listOrderSchedulePool = (query?: OrderSchedulePoolQuery) =>
  request<ApiListResponse<OrderSchedulePoolItem>>({
    url: '/api/order-schedule-pool',
    method: 'get',
    params: query,
  })

export const getOrder = (orderId: IdValue) =>
  request<ApiSuccessResponse<OrderDetailAggregate>>({
    url: `/api/orders/${orderId}`,
    method: 'get',
  })

export const createOrder = (payload: OrderUpsertRequest) =>
  request<ApiSuccessResponse<OrderSummaryItem>>({
    url: '/api/orders',
    method: 'post',
    data: payload,
  })

export const updateOrder = (orderId: IdValue, payload: OrderUpsertRequest) =>
  request<ApiSuccessResponse<OrderSummaryItem>>({
    url: `/api/orders/${orderId}`,
    method: 'put',
    data: payload,
  })

export const deleteOrder = (orderId: IdValue) =>
  request<ApiSuccessResponse<null>>({
    url: `/api/orders/${orderId}`,
    method: 'delete',
  })

export const patchOrderStatus = (orderId: IdValue, payload: OrderStatusPatchRequest) =>
  request<ApiSuccessResponse<OrderSummaryItem>>({
    url: `/api/orders/${orderId}/status`,
    method: 'patch',
    data: payload,
  })

export const listOrderItems = (orderId: IdValue) =>
  request<ApiSuccessResponse<OrderLineItem[]>>({
    url: `/api/orders/${orderId}/items`,
    method: 'get',
  })

export const createOrderItem = (orderId: IdValue, payload: OrderItemUpsertRequest) =>
  request<ApiSuccessResponse<OrderLineItem>>({
    url: `/api/orders/${orderId}/items`,
    method: 'post',
    data: payload,
  })

export const updateOrderItem = (orderId: IdValue, orderItemId: IdValue, payload: OrderItemUpsertRequest) =>
  request<ApiSuccessResponse<OrderLineItem>>({
    url: `/api/orders/${orderId}/items/${orderItemId}`,
    method: 'put',
    data: payload,
  })

export const deleteOrderItem = (orderId: IdValue, orderItemId: IdValue) =>
  request<ApiSuccessResponse<null>>({
    url: `/api/orders/${orderId}/items/${orderItemId}`,
    method: 'delete',
  })

export const listOrderBatches = (orderId: IdValue) =>
  request<ApiSuccessResponse<OrderBatchLinkItem[]>>({
    url: `/api/orders/${orderId}/batches`,
    method: 'get',
  })

export const createOrderBatchLink = (orderId: IdValue, payload: OrderBatchLinkUpsertRequest) =>
  request<ApiSuccessResponse<OrderBatchLinkItem>>({
    url: `/api/orders/${orderId}/batches`,
    method: 'post',
    data: payload,
  })

export const updateOrderBatchLink = (orderId: IdValue, linkId: IdValue, payload: OrderBatchLinkUpsertRequest) =>
  request<ApiSuccessResponse<OrderBatchLinkItem>>({
    url: `/api/orders/${orderId}/batches/${linkId}`,
    method: 'put',
    data: payload,
  })

export const deleteOrderBatchLink = (orderId: IdValue, linkId: IdValue) =>
  request<ApiSuccessResponse<null>>({
    url: `/api/orders/${orderId}/batches/${linkId}`,
    method: 'delete',
  })

export const fetchOrders = async (query?: OrderQuery): Promise<PageResult<OrderSummaryItem>> => {
  const response = await listOrders(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}

export const fetchOrderSchedulePool = async (query?: OrderSchedulePoolQuery): Promise<PageResult<OrderSchedulePoolItem>> => {
  const response = await listOrderSchedulePool(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}

export const fetchOrderItems = async (orderId: IdValue): Promise<OrderLineItem[]> => {
  const response = await listOrderItems(orderId)
  return response.data ?? []
}

export const fetchOrderBatches = async (orderId: IdValue): Promise<OrderBatchLinkItem[]> => {
  const response = await listOrderBatches(orderId)
  return response.data ?? []
}
