import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { ExceptionRecordItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type ExceptionRecordQuery = PageQuery & {
  planStepId?: number
  exceptionType?: string
  exceptionLevel?: string
  status?: string
}

export type ExceptionRecordUpsertRequest = {
  planStepId: number
  exceptionType: string
  exceptionLevel: string
  description: string
  handleResult?: string
  createTime: string
  status: string
}

export type ExceptionStatusPatchRequest = {
  status: string
  remark?: string
}

export type ExceptionCloseRequest = {
  handleResult: string
  closeRemark?: string
}

export type ExceptionReworkRequest = {
  reworkPlan: string
  reworkOwner?: string
  expectedFinishTime?: string
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

export const createExceptionRecord = (payload: ExceptionRecordUpsertRequest) =>
  request<ApiSuccessResponse<ExceptionRecordItem>>({
    url: '/api/exception-records',
    method: 'post',
    data: payload,
  })

export const updateExceptionRecord = (exceptionId: number, payload: ExceptionRecordUpsertRequest) =>
  request<ApiSuccessResponse<ExceptionRecordItem>>({
    url: `/api/exception-records/${exceptionId}`,
    method: 'put',
    data: payload,
  })

export const patchExceptionRecordStatus = (exceptionId: number, payload: ExceptionStatusPatchRequest) =>
  request<ApiSuccessResponse<ExceptionRecordItem>>({
    url: `/api/exception-records/${exceptionId}/status`,
    method: 'patch',
    data: payload,
  })

export const closeExceptionRecord = (exceptionId: number, payload: ExceptionCloseRequest) =>
  request<ApiSuccessResponse<ExceptionRecordItem>>({
    url: `/api/exception-records/${exceptionId}/close`,
    method: 'patch',
    data: payload,
  })

export const reworkExceptionRecord = (exceptionId: number, payload: ExceptionReworkRequest) =>
  request<ApiSuccessResponse<ExceptionRecordItem>>({
    url: `/api/exception-records/${exceptionId}/rework`,
    method: 'post',
    data: payload,
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
