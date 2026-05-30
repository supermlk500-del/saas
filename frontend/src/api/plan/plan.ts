import request, {
  type ApiListResponse,
  type ApiSuccessResponse,
  type RequestConfig,
} from '@/utils/request'
import type { GanttTaskItem, IdValue, PlanKpiItem, ProductionPlanDetailItem, ProductionPlanItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type PlanItem = ProductionPlanItem

export type PlanQuery = PageQuery & {
  orderId?: IdValue
  orderItemId?: IdValue
  batchId?: IdValue
  routeId?: IdValue
  status?: string
  planStartFrom?: string
  planStartTo?: string
}

export type ProductionPlanCreateRequest = {
  orderId: IdValue
  orderItemId: IdValue
  batchId: IdValue
  routeId: IdValue
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
  planId: IdValue
  tasks: GanttTaskItem[]
}

export const listPlans = (query?: PlanQuery, config?: RequestConfig) =>
  request<ApiListResponse<PlanItem>>({
    url: '/api/production-plans',
    method: 'get',
    params: query,
    ...config,
  })

export const getPlan = (planId: IdValue, config?: RequestConfig) =>
  request<ApiSuccessResponse<ProductionPlanDetailItem>>({
    url: `/api/production-plans/${planId}`,
    method: 'get',
    ...config,
  })

export const createPlan = (payload: ProductionPlanCreateRequest) =>
  request<ApiSuccessResponse<{ planId: IdValue; planSteps: number }>>({
    url: '/api/production-plans',
    method: 'post',
    data: payload,
  })

export const updatePlan = (planId: IdValue, payload: ProductionPlanUpdateRequest) =>
  request<ApiSuccessResponse<PlanItem>>({
    url: `/api/production-plans/${planId}`,
    method: 'put',
    data: payload,
  })

export const patchPlanStatus = (planId: IdValue, payload: PlanStatusPatchRequest) =>
  request<ApiSuccessResponse<PlanItem>>({
    url: `/api/production-plans/${planId}/status`,
    method: 'patch',
    data: payload,
  })

export const reschedulePlan = (planId: IdValue, payload: PlanRescheduleRequest) =>
  request<ApiSuccessResponse<{ planId: IdValue; newStartTime?: string; newEndTime?: string }>>({
    url: `/api/production-plans/${planId}/reschedule`,
    method: 'post',
    data: payload,
  })

export const getPlanGantt = (planId: IdValue, config?: RequestConfig) =>
  request<ApiSuccessResponse<GanttResponse>>({
    url: `/api/production-plans/${planId}/gantt`,
    method: 'get',
    ...config,
  })

export const getPlanKpi = (planId: IdValue, config?: RequestConfig) =>
  request<ApiSuccessResponse<PlanKpiItem>>({
    url: `/api/production-plans/${planId}/kpi`,
    method: 'get',
    ...config,
  })

export const fetchPlans = async (query?: PlanQuery, config?: RequestConfig): Promise<PageResult<PlanItem>> => {
  const response = await listPlans(query, config)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
