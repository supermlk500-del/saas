import request, { type ApiSuccessResponse } from '@/utils/request'
import type {
  BatchItem,
  ExceptionRecordItem,
  ProductionPlanItem,
  QcRecordItem,
} from '@/types/domain'

export type DashboardOverview = {
  pendingBatches: BatchItem[]
  todayPendingCount: number
  yesterdayPendingCount: number
  currentWeekQcRecords: QcRecordItem[]
  previousWeekQcRecords: QcRecordItem[]
  recentQcRecords: QcRecordItem[]
  exceptionRecords: ExceptionRecordItem[]
  productionPlans: ProductionPlanItem[]
}

export const getDashboardOverview = () =>
  request<ApiSuccessResponse<DashboardOverview>>({
    url: '/api/dashboard/overview',
    method: 'get',
  })
