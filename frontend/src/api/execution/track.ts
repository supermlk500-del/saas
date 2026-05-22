import request, { type RuoYiListResponse } from '@/utils/request'

export type TrackItem = {
  key: string
  woNo: string
  productName: string
  processName: string
  step: number
  equipment: string
  operator: string
  passTime: string
  qty: number
}

export type TrackQuery = {
  keyword?: string
  processName?: string
}

export function listTracks(query?: TrackQuery) {
  return request<RuoYiListResponse<TrackItem>>({
    url: '/business/track/list',
    method: 'get',
    params: query,
  })
}

export const fetchTracks = async (query?: TrackQuery): Promise<TrackItem[]> => {
  const res = await listTracks(query)
  return res.rows ?? []
}
