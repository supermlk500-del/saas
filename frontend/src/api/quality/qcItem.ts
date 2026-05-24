import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { QcItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type QcItemQuery = PageQuery & {
  qcItemCode?: string
  qcItemName?: string
  qcType?: string
  isActive?: number
}

export type QcItemUpsertRequest = {
  qcItemCode: string
  qcItemName: string
  qcType?: string
  unit?: string
  standardMin?: number | null
  standardMax?: number | null
  isActive: number
  description?: string
}

export const listQcItems = (query?: QcItemQuery) =>
  request<ApiListResponse<QcItem>>({
    url: '/api/qc-items',
    method: 'get',
    params: query,
  })

export const getQcItem = (qcItemId: number | string) =>
  request<ApiSuccessResponse<QcItem>>({
    url: `/api/qc-items/${qcItemId}`,
    method: 'get',
  })

export const createQcItem = (payload: QcItemUpsertRequest) =>
  request<ApiSuccessResponse<QcItem>>({
    url: '/api/qc-items',
    method: 'post',
    data: payload,
  })

export const updateQcItem = (qcItemId: number | string, payload: QcItemUpsertRequest) =>
  request<ApiSuccessResponse<QcItem>>({
    url: `/api/qc-items/${qcItemId}`,
    method: 'put',
    data: payload,
  })

export const patchQcItemStatus = (qcItemId: number | string, isActive: number) =>
  request<ApiSuccessResponse<QcItem>>({
    url: `/api/qc-items/${qcItemId}/status`,
    method: 'patch',
    data: { isActive },
  })

export const fetchQcItems = async (query?: QcItemQuery): Promise<PageResult<QcItem>> => {
  const response = await listQcItems(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
