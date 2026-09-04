import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { IdValue, StepMachineCapabilityItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type CapabilityQuery = PageQuery & {
  stepId?: IdValue
  machineId?: IdValue
  isActive?: number
}

export type CapabilityUpsertRequest = {
  stepId: IdValue
  machineId: IdValue
  minWidth?: number
  maxWidth?: number
  maxSpeed?: number
  maxBatchWeight?: number
  isActive: number
}

export const listCapabilities = (query?: CapabilityQuery) =>
  request<ApiListResponse<StepMachineCapabilityItem>>({
    url: '/api/step-machine-capabilities',
    method: 'get',
    params: query,
  })

export const createCapability = (payload: CapabilityUpsertRequest) =>
  request<ApiSuccessResponse<StepMachineCapabilityItem>>({
    url: '/api/step-machine-capabilities',
    method: 'post',
    data: payload,
  })

export const updateCapability = (capId: IdValue, payload: CapabilityUpsertRequest) =>
  request<ApiSuccessResponse<StepMachineCapabilityItem>>({
    url: `/api/step-machine-capabilities/${capId}`,
    method: 'put',
    data: payload,
  })

export const deleteCapability = (capId: IdValue) =>
  request<ApiSuccessResponse<null>>({
    url: `/api/step-machine-capabilities/${capId}`,
    method: 'delete',
  })

export const fetchCapabilities = async (query?: CapabilityQuery): Promise<PageResult<StepMachineCapabilityItem>> => {
  const response = await listCapabilities(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
