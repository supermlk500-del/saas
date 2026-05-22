import request, { type RuoYiListResponse } from '@/utils/request'

export type SupplierItem = {
  key: string
  code: string
  name: string
  contact: string
  phone: string
  status: string
}

export type SupplierQuery = {
  keyword?: string
  status?: string
}

export function listSuppliers(query?: SupplierQuery) {
  return request<RuoYiListResponse<SupplierItem>>({
    url: '/business/supplier/list',
    method: 'get',
    params: query,
  })
}

export const fetchSuppliers = async (query?: SupplierQuery): Promise<SupplierItem[]> => {
  const res = await listSuppliers(query)
  return res.rows ?? []
}
