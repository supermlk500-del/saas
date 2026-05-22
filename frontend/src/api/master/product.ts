import request, { type RuoYiListResponse } from '@/utils/request'

export type ProductItem = {
  key: string
  code: string
  name: string
  spec: string
  status: string
}

export type ProductQuery = {
  keyword?: string
  status?: string
}

export function listProducts(query?: ProductQuery) {
  return request<RuoYiListResponse<ProductItem>>({
    url: '/business/master/list',
    method: 'get',
    params: query,
  })
}

export const fetchProducts = async (query?: ProductQuery): Promise<ProductItem[]> => {
  const res = await listProducts(query)
  return res.rows ?? []
}
