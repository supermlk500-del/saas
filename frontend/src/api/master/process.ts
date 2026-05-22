import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { ProcessRouteItem, ProcessStepItem, RouteStepItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'

export type ProcessItem = ProcessRouteItem

export type ProcessQuery = PageQuery & {
  routeName?: string
  isActive?: number
}

export type ProcessRouteUpsertRequest = {
  routeName: string
  description?: string
  isActive: number
}

export type RouteStepUpsertRequest = {
  stepId: number
  sortOrder: number
  isMandatory: number
}

export type ProcessStepQuery = PageQuery & {
  stepCode?: string
  stepName?: string
  stepType?: string
  isActive?: number
}

export type ProcessStepUpsertRequest = {
  stepCode: string
  stepName: string
  stepType?: string
  sortOrder?: number | null
  defaultHours?: number | null
  description?: string
  isActive: number
}

export const listProcesses = (query?: ProcessQuery) =>
  request<ApiListResponse<ProcessItem>>({
    url: '/api/process-routes',
    method: 'get',
    params: query,
  })

export const getProcessRoute = (routeId: number) =>
  request<ApiSuccessResponse<ProcessRouteItem & { steps?: RouteStepItem[] }>>({
    url: `/api/process-routes/${routeId}`,
    method: 'get',
  })

export const createProcessRoute = (payload: ProcessRouteUpsertRequest) =>
  request<ApiSuccessResponse<ProcessRouteItem>>({
    url: '/api/process-routes',
    method: 'post',
    data: payload,
  })

export const updateProcessRoute = (routeId: number, payload: ProcessRouteUpsertRequest) =>
  request<ApiSuccessResponse<ProcessRouteItem>>({
    url: `/api/process-routes/${routeId}`,
    method: 'put',
    data: payload,
  })

export const deleteProcessRoute = (routeId: number) =>
  request<ApiSuccessResponse<null>>({
    url: `/api/process-routes/${routeId}`,
    method: 'delete',
  })

export const listRouteSteps = (routeId: number) =>
  request<ApiSuccessResponse<RouteStepItem[]>>({
    url: `/api/process-routes/${routeId}/steps`,
    method: 'get',
  })

export const createRouteStep = (routeId: number, payload: RouteStepUpsertRequest) =>
  request<ApiSuccessResponse<RouteStepItem>>({
    url: `/api/process-routes/${routeId}/steps`,
    method: 'post',
    data: payload,
  })

export const updateRouteStep = (routeId: number, routeStepId: number, payload: RouteStepUpsertRequest) =>
  request<ApiSuccessResponse<RouteStepItem>>({
    url: `/api/process-routes/${routeId}/steps/${routeStepId}`,
    method: 'put',
    data: payload,
  })

export const deleteRouteStep = (routeId: number, routeStepId: number) =>
  request<ApiSuccessResponse<null>>({
    url: `/api/process-routes/${routeId}/steps/${routeStepId}`,
    method: 'delete',
  })

export const listProcessSteps = (query?: ProcessStepQuery) =>
  request<ApiListResponse<ProcessStepItem>>({
    url: '/api/process-steps',
    method: 'get',
    params: query,
  })

export const getProcessStep = (stepId: number) =>
  request<ApiSuccessResponse<ProcessStepItem>>({
    url: `/api/process-steps/${stepId}`,
    method: 'get',
  })

export const createProcessStep = (payload: ProcessStepUpsertRequest) =>
  request<ApiSuccessResponse<ProcessStepItem>>({
    url: '/api/process-steps',
    method: 'post',
    data: payload,
  })

export const updateProcessStep = (stepId: number, payload: ProcessStepUpsertRequest) =>
  request<ApiSuccessResponse<ProcessStepItem>>({
    url: `/api/process-steps/${stepId}`,
    method: 'put',
    data: payload,
  })

export const patchProcessStepStatus = (stepId: number, isActive: number) =>
  request<ApiSuccessResponse<ProcessStepItem>>({
    url: `/api/process-steps/${stepId}/status`,
    method: 'patch',
    data: { isActive },
  })

export const fetchProcesses = async (query?: ProcessQuery): Promise<PageResult<ProcessItem>> => {
  const response = await listProcesses(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}

export const fetchRouteSteps = async (routeId: number): Promise<RouteStepItem[]> => {
  const response = await listRouteSteps(routeId)
  return response.data ?? []
}

export const fetchProcessSteps = async (query?: ProcessStepQuery): Promise<PageResult<ProcessStepItem>> => {
  const response = await listProcessSteps(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
