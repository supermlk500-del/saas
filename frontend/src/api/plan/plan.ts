import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { GanttTaskItem, ProductionPlanDetailItem, ProductionPlanItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type PlanItem = ProductionPlanItem

export type PlanQuery = PageQuery & {
  batchId?: number
  routeId?: number
  status?: string
  planStartFrom?: string
  planStartTo?: string
}

export type ProductionPlanCreateRequest = {
  batchId: number
  routeId: number
  planStartTime: string
  planEndTime?: string
  remark?: string
}

export type ProductionPlanUpdateRequest = {
  planStartTime?: string
  planEndTime?: string
  status?: string
  remark?: string
}

export type PlanStatusPatchRequest = {
  status: string
  reason?: string
}

export type PlanRescheduleRequest = {
  rescheduleReason: string
  startTime?: string
}

export type GanttResponse = {
  planId: number
  tasks: GanttTaskItem[]
}

export const listPlans = (query?: PlanQuery) =>
  request<ApiListResponse<PlanItem>>({
    url: '/api/production-plans',
    method: 'get',
    params: query,
  })

export const getPlan = (planId: number) =>
  request<ApiSuccessResponse<ProductionPlanDetailItem>>({
    url: `/api/production-plans/${planId}`,
    method: 'get',
  })

export const createPlan = (payload: ProductionPlanCreateRequest) =>
  request<ApiSuccessResponse<{ planId: number; planSteps: number }>>({
    url: '/api/production-plans',
    method: 'post',
    data: payload,
  })

export const updatePlan = (planId: number, payload: ProductionPlanUpdateRequest) =>
  request<ApiSuccessResponse<PlanItem>>({
    url: `/api/production-plans/${planId}`,
    method: 'put',
    data: payload,
  })

export const patchPlanStatus = (planId: number, payload: PlanStatusPatchRequest) =>
  request<ApiSuccessResponse<PlanItem>>({
    url: `/api/production-plans/${planId}/status`,
    method: 'patch',
    data: payload,
  })

export const reschedulePlan = (planId: number, payload: PlanRescheduleRequest) =>
  request<ApiSuccessResponse<{ planId: number; newStartTime?: string; newEndTime?: string }>>({
    url: `/api/production-plans/${planId}/reschedule`,
    method: 'post',
    data: payload,
  })

export const getPlanGantt = (planId: number) =>
  request<ApiSuccessResponse<GanttResponse>>({
    url: `/api/production-plans/${planId}/gantt`,
    method: 'get',
  })

export const fetchPlans = async (query?: PlanQuery): Promise<PageResult<PlanItem>> => {
  const response = await listPlans(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
