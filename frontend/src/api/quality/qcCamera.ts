import request, { type ApiListResponse } from '@/utils/request'
import type { QcCameraItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type QcCameraQuery = PageQuery & {
  cameraCode?: string
  cameraName?: string
  cameraType?: string
  status?: number | string
}

export const listQcCameras = (query?: QcCameraQuery) =>
  request<ApiListResponse<QcCameraItem>>({
    url: '/api/qc-cameras',
    method: 'get',
    params: query,
  })

export const fetchQcCameras = async (query?: QcCameraQuery): Promise<PageResult<QcCameraItem>> => {
  const response = await listQcCameras(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
