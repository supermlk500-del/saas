import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { PlanStepItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type PlanStepQuery = PageQuery & {
  planId?: number
  stepId?: number
  machineId?: number
  status?: string
  dateFrom?: string
  dateTo?: string
}

export type PlanStepUpdateRequest = {
  planStartTime?: string
  planEndTime?: string
  planHours?: number | null
  sequenceNo?: number | null
  status?: string
  remark?: string
}

export type PlanStepMachinePatchRequest = {
  machineId: number
}

export type StatusPatchRequest = {
  status: string
  reason?: string
}

export const listPlanSteps = (query?: PlanStepQuery) =>
  request<ApiListResponse<PlanStepItem>>({
    url: '/api/plan-steps',
    method: 'get',
    params: query,
  })

export const getPlanStep = (planStepId: number) =>
  request<ApiSuccessResponse<PlanStepItem>>({
    url: `/api/plan-steps/${planStepId}`,
    method: 'get',
  })

export const updatePlanStep = (planStepId: number, payload: PlanStepUpdateRequest) =>
  request<ApiSuccessResponse<PlanStepItem>>({
    url: `/api/plan-steps/${planStepId}`,
    method: 'put',
    data: payload,
  })

export const patchPlanStepMachine = (planStepId: number, payload: PlanStepMachinePatchRequest) =>
  request<ApiSuccessResponse<PlanStepItem>>({
    url: `/api/plan-steps/${planStepId}/machine`,
    method: 'patch',
    data: payload,
  })

export const patchPlanStepStatus = (planStepId: number, payload: StatusPatchRequest) =>
  request<ApiSuccessResponse<PlanStepItem>>({
    url: `/api/plan-steps/${planStepId}/status`,
    method: 'patch',
    data: payload,
  })

export const fetchPlanSteps = async (query?: PlanStepQuery): Promise<PageResult<PlanStepItem>> => {
  const response = await listPlanSteps(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
