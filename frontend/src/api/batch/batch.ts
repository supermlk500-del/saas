import request, {
  type ApiListResponse,
  type ApiSuccessResponse,
  type RequestConfig,
} from '@/utils/request'
import type { BatchItem, IdValue } from '@/types/domain'
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

export const listBatchResourcePool = (query?: BatchQuery) =>
  request<ApiListResponse<BatchItem>>({
    url: '/api/batches/resource-pool',
    method: 'get',
    params: query,
  })

export const getBatch = (batchId: IdValue, config?: RequestConfig) =>
  request<ApiSuccessResponse<BatchItem>>({
    url: `/api/batches/${batchId}`,
    method: 'get',
    ...config,
  })

export const createBatch = (payload: BatchUpsertRequest) =>
  request<ApiSuccessResponse<{ batchId: IdValue; batchNo: string }>>({
    url: '/api/batches',
    method: 'post',
    data: payload,
  })

export const updateBatch = (batchId: IdValue, payload: BatchUpsertRequest) =>
  request<ApiSuccessResponse<{ batchId: IdValue; batchNo: string }>>({
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

export const fetchBatchResourcePool = async (query?: BatchQuery): Promise<PageResult<BatchItem>> => {
  const response = await listBatchResourcePool(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
