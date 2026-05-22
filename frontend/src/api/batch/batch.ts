import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { BatchItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type BatchUpsertRequest = {
  batchNo: string
  supplier: string
  inDate: string
  weight?: number | null
  width?: number | null
  composition?: string
  note?: string
}

export type BatchQuery = PageQuery & {
  batchNo?: string
  supplier?: string
  dateFrom?: string
  dateTo?: string
}

export const listBatches = (query?: BatchQuery) =>
  request<ApiListResponse<BatchItem>>({
    url: '/api/batches',
    method: 'get',
    params: query,
  })

export const getBatch = (batchId: number) =>
  request<ApiSuccessResponse<BatchItem>>({
    url: `/api/batches/${batchId}`,
    method: 'get',
  })

export const createBatch = (payload: BatchUpsertRequest) =>
  request<ApiSuccessResponse<{ batchId: number; batchNo: string }>>({
    url: '/api/batches',
    method: 'post',
    data: payload,
  })

export const updateBatch = (batchId: number, payload: BatchUpsertRequest) =>
  request<ApiSuccessResponse<{ batchId: number; batchNo: string }>>({
    url: `/api/batches/${batchId}`,
    method: 'put',
    data: payload,
  })

export const fetchBatches = async (query?: BatchQuery): Promise<PageResult<BatchItem>> => {
  const response = await listBatches(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
