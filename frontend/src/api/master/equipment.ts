import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { MachineItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type EquipmentItem = MachineItem
export type MachineUpsertRequest = {
  machineCode: string
  machineName: string
  machineType?: string
  description?: string
  status: string
}

export type EquipmentQuery = PageQuery & {
  machineCode?: string
  machineName?: string
  machineType?: string
  status?: string
}

export const listEquipments = (query?: EquipmentQuery) =>
  request<ApiListResponse<EquipmentItem>>({
    url: '/api/machines',
    method: 'get',
    params: query,
  })

export const getEquipment = (machineId: number) =>
  request<ApiSuccessResponse<EquipmentItem>>({
    url: `/api/machines/${machineId}`,
    method: 'get',
  })

export const createEquipment = (payload: MachineUpsertRequest) =>
  request<ApiSuccessResponse<EquipmentItem>>({
    url: '/api/machines',
    method: 'post',
    data: payload,
  })

export const updateEquipment = (machineId: number, payload: MachineUpsertRequest) =>
  request<ApiSuccessResponse<EquipmentItem>>({
    url: `/api/machines/${machineId}`,
    method: 'put',
    data: payload,
  })

export const fetchEquipments = async (query?: EquipmentQuery): Promise<PageResult<EquipmentItem>> => {
  const response = await listEquipments(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
