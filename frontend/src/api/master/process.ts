import request, { type RuoYiListResponse } from '@/utils/request'

export type ProcessItem = {
  key: string
  processCode: string
  processName: string
  productName: string
  version: string
  status: string
}

export type ProcessQuery = {
  keyword?: string
  status?: string
}

export function listProcesses(query?: ProcessQuery) {
  return request<RuoYiListResponse<ProcessItem>>({
    url: '/business/process/list',
    method: 'get',
    params: query,
  })
}

export const fetchProcesses = async (query?: ProcessQuery): Promise<ProcessItem[]> => {
  const res = await listProcesses(query)
  return res.rows ?? []
}
