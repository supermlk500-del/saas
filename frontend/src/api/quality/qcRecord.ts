import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { QcRecordItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type QcRecordQuery = PageQuery & {
  planStepId?: number
  qcItemId?: number
  inspectType?: string
  resultJudge?: string
  inspectTimeFrom?: string
  inspectTimeTo?: string
}

export const listQcRecords = (query?: QcRecordQuery) =>
  request<ApiListResponse<QcRecordItem>>({
    url: '/api/qc-records',
    method: 'get',
    params: query,
  })

export const getQcRecord = (inspectionId: number) =>
  request<ApiSuccessResponse<QcRecordItem>>({
    url: `/api/qc-records/${inspectionId}`,
    method: 'get',
  })

export const fetchQcRecords = async (query?: QcRecordQuery): Promise<PageResult<QcRecordItem>> => {
  const response = await listQcRecords(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
