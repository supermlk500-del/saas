import request, { type ApiSuccessResponse } from '@/utils/request'
import type { IdValue } from '@/types/domain'

export interface AiScheduleStep {
  planStepId: IdValue
  stepId: IdValue
  stepName?: string
  machineId: IdValue
  machineCode?: string
  machineName?: string
  recommendationScore: number
  recommendationReason?: string
  startTime: string
  endTime: string
  predictedHours: number
}

export interface AiScheduleOption {
  strategy: 'DELIVERY' | 'UTILIZATION' | 'COST'
  strategyName: string
  solverStatus: string
  startTime?: string
  endTime?: string
  totalMinutes: number
  delayMinutes: number
  machineChanges: number
  estimatedUtilizationScore: number
  explanation?: string
  model?: string
  aiGenerated: boolean
  fallbackReason?: string
  steps: AiScheduleStep[]
}

export interface AiScheduleSuggestion {
  recordId?: IdValue
  planId: IdValue
  orderNo?: string
  batchNo?: string
  deliveryDate?: string
  decisionEngine: string
  explanationModel: string
  notice: string
  recordStatus?: string
  selectedStrategy?: string
  createTime?: string
  appliedTime?: string
  options: AiScheduleOption[]
}

export interface AiScheduleRecordSummary {
  recordId: IdValue
  planId: IdValue
  orderNo?: string
  batchNo?: string
  status: string
  selectedStrategy?: string
  createTime: string
  appliedTime?: string
}

export interface AiQualityAnalysis {
  inspectionId: IdValue
  resultJudge?: string
  defectType?: string
  severity?: string
  riskLevel?: string
  recentSampleCount: number
  recentFailureCount: number
  recentRecheckCount: number
  recentDefectRate: number
  possibleCauses: string[]
  recommendedActions: string[]
  report?: string
  model?: string
  aiGenerated: boolean
  fallbackReason?: string
  notice: string
}

export const getAiScheduleSuggestions = (planId: IdValue) =>
  request<ApiSuccessResponse<AiScheduleSuggestion>>({
    url: `/api/ai/schedule/plans/${planId}/suggestions`,
    method: 'post',
    timeout: 60000,
  })

export const applyAiScheduleSuggestion = (
  planId: IdValue,
  payload: { recordId?: IdValue; strategy: AiScheduleOption['strategy']; confirmationRemark?: string },
) =>
  request<ApiSuccessResponse<{ planId: IdValue; strategy: string; updatedSteps: number }>>({
    url: `/api/ai/schedule/plans/${planId}/apply`,
    method: 'post',
    data: payload,
  })

export const listAiScheduleRecords = (planId?: IdValue) =>
  request<ApiSuccessResponse<AiScheduleRecordSummary[]>>({
    url: '/api/ai/schedule/records',
    method: 'get',
    params: { planId },
  })

export const getAiScheduleRecord = (recordId: IdValue) =>
  request<ApiSuccessResponse<AiScheduleSuggestion>>({
    url: `/api/ai/schedule/records/${recordId}`,
    method: 'get',
  })

export const analyzeQcRecord = (inspectionId: IdValue) =>
  request<ApiSuccessResponse<AiQualityAnalysis>>({
    url: `/api/ai/quality/records/${inspectionId}/analysis`,
    method: 'post',
    timeout: 60000,
  })
