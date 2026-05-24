import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { InspectionDataItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type InspectionDataQuery = PageQuery & {
  qcRecordId?: number | string
  cameraId?: number | string
  fileType?: string
}

export type InspectionDataUpsertRequest = {
  qcRecordId: number | string
  cameraId?: number | string | null
  fileType: string
  filePath: string
  fileName: string
  captureTime?: string
  resultSummary?: string
  remark?: string
}

export const listInspectionData = (query?: InspectionDataQuery) =>
  request<ApiListResponse<InspectionDataItem>>({
    url: '/api/inspection-data',
    method: 'get',
    params: query,
  })

export const createInspectionData = (payload: InspectionDataUpsertRequest) =>
  request<ApiSuccessResponse<InspectionDataItem>>({
    url: '/api/inspection-data',
    method: 'post',
    data: payload,
  })

export const deleteInspectionData = (dataId: number | string) =>
  request<ApiSuccessResponse<null>>({
    url: `/api/inspection-data/${dataId}`,
    method: 'delete',
  })

export const fetchInspectionData = async (query?: InspectionDataQuery): Promise<PageResult<InspectionDataItem>> => {
  const response = await listInspectionData(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
