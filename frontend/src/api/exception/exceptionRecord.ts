import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { ExceptionRecordItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type ExceptionRecordQuery = PageQuery & {
  planStepId?: number
  exceptionType?: string
  exceptionLevel?: string
  status?: string
}

export const listExceptionRecords = (query?: ExceptionRecordQuery) =>
  request<ApiListResponse<ExceptionRecordItem>>({
    url: '/api/exception-records',
    method: 'get',
    params: query,
  })

export const getExceptionRecord = (exceptionId: number) =>
  request<ApiSuccessResponse<ExceptionRecordItem>>({
    url: `/api/exception-records/${exceptionId}`,
    method: 'get',
  })

export const fetchExceptionRecords = async (
  query?: ExceptionRecordQuery,
): Promise<PageResult<ExceptionRecordItem>> => {
  const response = await listExceptionRecords(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
