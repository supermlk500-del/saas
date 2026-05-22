import request, { type RuoYiListResponse } from '@/utils/request'

export type CustomerItem = {
  key: string
  code: string
  name: string
  contact: string
  phone: string
  status: string
}

export type CustomerQuery = {
  keyword?: string
  status?: string
}

export function listCustomers(query?: CustomerQuery) {
  return request<RuoYiListResponse<CustomerItem>>({
    url: '/business/customer/list',
    method: 'get',
    params: query,
  })
}

export const fetchCustomers = async (query?: CustomerQuery): Promise<CustomerItem[]> => {
  const res = await listCustomers(query)
  return res.rows ?? []
}
