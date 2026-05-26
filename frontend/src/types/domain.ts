import type {
  BatchStatus,
  EnabledStatus,
  ExceptionLevel,
  ExceptionStatus,
  InspectType,
  MachineStatus,
  PlanStatus,
  PlanStepStatus,
  ResultJudge,
} from '@/types/dictionary'

export type IdValue = string | number

export interface BatchItem {
  batchId: IdValue
  batchNo: string
  supplier: string
  inDate: string
  weight?: number
  width?: number
  composition?: string
  note?: string
  status?: BatchStatus
  orderId?: IdValue
  orderNo?: string
  orderItemId?: IdValue
  customerName?: string
  priority?: string
  productCode?: string
  productName?: string
  specification?: string
  color?: string
  allocatedWeight?: number | null
  allocatedQuantity?: number | null
  remainingWeight?: number | null
  remainingQuantity?: number | null
  linkedOrderCount?: number
  linkedOrders?: BatchLinkedOrderItem[]
  resourceStatus?: string
  resourceStatusLabel?: string
  currentPlanId?: IdValue | null
  currentPlanStatus?: string | null
  currentOrderId?: IdValue | null
  currentOrderNo?: string | null
  lockedByPlan?: boolean
  readyForSchedule?: boolean
  routeId?: IdValue
  routeName?: string
}

export interface BatchLinkedOrderItem {
  linkId?: IdValue
  orderId?: IdValue
  orderNo?: string
  customerName?: string
  orderStatus?: string
  orderItemId?: IdValue
  productCode?: string
  productName?: string
  specification?: string
  allocatedWeight?: number | null
  allocatedQuantity?: number | null
  remark?: string
}

export interface OrderSummaryItem {
  orderId: IdValue
  orderNo: string
  customerName: string
  orderDate?: string
  deliveryDate?: string
  priority?: string
  status?: string
  remark?: string
  linkedBatchCount?: number
  generatedPlanCount?: number
}

export interface OrderSchedulePoolItem {
  orderId: IdValue
  orderNo: string
  customerName: string
  orderItemId?: IdValue
  productName?: string
  specification?: string
  quantity?: number | null
  unit?: string
  deliveryDate?: string
  priority?: string
  linkedBatchCount?: number
  batchSummary?: string
  recommendedRouteId?: IdValue
  recommendedRouteName?: string
  recommendedMachines?: string[]
  status?: string
  readyForSchedule?: boolean
}

export interface OrderLineItem {
  orderItemId: IdValue
  orderId: IdValue
  productCode?: string
  productName?: string
  specification?: string
  color?: string
  quantity?: number | null
  unit?: string
  requiredWidth?: number | null
  requiredWeight?: number | null
  targetWidth?: number | null
  targetWeight?: number | null
  remark?: string
}

export interface OrderBatchLinkItem {
  linkId?: IdValue
  id?: IdValue
  orderId: IdValue
  orderItemId?: IdValue
  batchId: IdValue
  batchNo?: string
  supplier?: string
  weight?: number | null
  width?: number | null
  composition?: string
  allocatedWeight?: number | null
  allocatedQuantity?: number | null
  remainingWeight?: number | null
  remainingQuantity?: number | null
  status?: string
  resourceStatus?: string
  resourceStatusLabel?: string
  currentPlanId?: IdValue | null
  currentPlanStatus?: string | null
  lockedByPlan?: boolean
  readyForSchedule?: boolean
  remark?: string
}

export interface OrderPlanSummaryItem {
  planId: IdValue
  orderId?: IdValue
  orderNo?: string
  orderItemId?: IdValue
  batchId?: IdValue
  batchNo?: string
  routeId?: IdValue
  routeName?: string
  customerName?: string
  productCode?: string
  productName?: string
  specification?: string
  color?: string
  planStartTime?: string
  planEndTime?: string
  status?: string
  remark?: string
  planSteps?: PlanStepItem[]
}

export interface OrderQualitySummaryItem {
  inspectionId: IdValue
  planStepId: IdValue
  qcItemId?: IdValue
  inspectTime?: string
  inspectType?: InspectType
  cameraId?: IdValue | null
  frameTime?: string | null
  imageUrl?: string | null
  sourceImageUrl?: string | null
  confidenceScore?: number | null
  defectType?: string | null
  resultValue?: string | null
  resultJudge?: ResultJudge
  inspector?: string
  remark?: string
  stepName?: string
  batchNo?: string
}

export interface OrderExceptionSummaryItem {
  exceptionId: IdValue
  planStepId: IdValue
  exceptionType?: string
  exceptionLevel?: ExceptionLevel
  description?: string
  handleResult?: string
  createTime?: string
  status?: ExceptionStatus
  stepName?: string
  batchNo?: string
}

export interface OrderRouteStepItem {
  routeStepId: IdValue
  stepId?: IdValue
  stepCode?: string
  stepName?: string
  sortOrder?: number
  isMandatory?: EnabledStatus
}

export interface OrderRouteSummaryItem {
  routeId: IdValue
  routeName: string
  description?: string
  steps?: OrderRouteStepItem[]
}

export interface OrderMachineOccupiedRangeItem {
  planId?: IdValue
  planStepId?: IdValue
  stepName?: string
  planStartTime?: string
  planEndTime?: string
}

export interface OrderMachineSummaryItem {
  machineId: IdValue
  machineCode?: string
  machineName?: string
  machineType?: string
  status?: string
  relatedPlanIds?: IdValue[]
  relatedPlanStepIds?: IdValue[]
  occupiedTimeRanges?: OrderMachineOccupiedRangeItem[]
}

export interface OrderDetailAggregate extends OrderSummaryItem {
  qcRecordCount?: number
  exceptionCount?: number
  items?: OrderLineItem[]
  linkedBatches?: OrderBatchLinkItem[]
  planSummary?: OrderPlanSummaryItem[]
  planSteps?: PlanStepItem[]
  routeSummary?: OrderRouteSummaryItem[]
  machineSummary?: OrderMachineSummaryItem[]
  qcSummary?: OrderQualitySummaryItem[]
  latestQcRecord?: OrderQualitySummaryItem | null
  qualitySummary?: OrderQualitySummaryItem[]
  exceptionSummary?: OrderExceptionSummaryItem[]
  latestException?: OrderExceptionSummaryItem | null
}

export interface ProcessRouteItem {
  routeId: IdValue
  routeName: string
  description?: string
  isActive: EnabledStatus
  createTime?: string
}

export interface ProcessStepItem {
  stepId: IdValue
  stepCode: string
  stepName: string
  stepType?: string
  sortOrder?: number
  defaultHours?: number
  description?: string
  isActive: EnabledStatus
}

export interface RouteStepItem {
  routeStepId: IdValue
  routeId: IdValue
  stepId: IdValue
  sortOrder: number
  isMandatory: EnabledStatus
  stepCode?: string
  stepName?: string
}

export interface MachineItem {
  machineId: IdValue
  machineCode: string
  machineName: string
  machineType?: string
  description?: string
  status: MachineStatus
  createTime?: string
}

export interface StepMachineCapabilityItem {
  capId: IdValue
  stepId: IdValue
  machineId: IdValue
  minWidth?: number
  maxWidth?: number
  maxSpeed?: number
  maxBatchWeight?: number
  isActive: EnabledStatus
}

export interface ProductionPlanItem {
  planId: IdValue
  orderId?: IdValue
  orderNo?: string
  customerName?: string
  orderItemId?: IdValue
  productCode?: string
  productName?: string
  specification?: string
  color?: string
  priority?: string
  batchId: IdValue
  batchNo?: string
  routeId: IdValue
  routeName?: string
  planStartTime: string
  planEndTime?: string
  status: PlanStatus
  createTime?: string
  remark?: string
}

export interface ProductionPlanDetailItem {
  planId: IdValue
  orderInfo?: OrderSummaryItem | null
  orderItemInfo?: OrderLineItem | null
  batchInfo: BatchItem
  routeInfo: ProcessRouteItem
  planStartTime: string
  planEndTime?: string
  status: PlanStatus
  remark?: string
  planSteps: PlanStepItem[]
}

export interface PlanStepItem {
  planStepId: IdValue
  planId: IdValue
  stepId: IdValue
  stepName?: string
  machineId?: IdValue
  machineName?: string
  planStartTime?: string
  planEndTime?: string
  planHours?: number
  sequenceNo?: number
  status: PlanStepStatus
  remark?: string
}

export interface GanttTaskItem {
  planStepId: IdValue
  stepName?: string
  machineName?: string
  start?: string
  end?: string
  status: PlanStepStatus
}

export interface ProcessParameterItem {
  paramId: IdValue
  planStepId: IdValue
  paramName: string
  paramValue: string
  unit?: string
  paramType?: string
  recordTime: string
  remark?: string
}

export interface QcItem {
  qcItemId: IdValue
  qcItemCode: string
  qcItemName: string
  qcType?: string
  unit?: string
  standardMin?: number
  standardMax?: number
  isActive: EnabledStatus
  description?: string
}

export interface QcRecordItem {
  inspectionId: IdValue
  planStepId: IdValue
  qcItemId: IdValue
  inspectTime: string
  inspectType: InspectType
  cameraId?: IdValue | null
  frameTime?: string | null
  imageUrl?: string | null
  sourceImageUrl?: string | null
  confidenceScore?: number | null
  defectType?: string | null
  resultValue?: string | null
  resultJudge: ResultJudge
  inspector?: string
  remark?: string
}

export interface QcDetectionBox {
  label?: string
  score?: number
  x1?: number
  y1?: number
  x2?: number
  y2?: number
}

export interface QcDetectionResult {
  inspectionId?: IdValue
  planStepId?: IdValue
  qcItemId?: IdValue
  inspectType?: InspectType
  resultJudge?: ResultJudge
  confidenceScore?: number | null
  defectType?: string | null
  resultValue?: string | null
  imageUrl?: string | null
  sourceImageUrl?: string | null
  boxes?: QcDetectionBox[]
  qcRecord?: QcRecordItem
  inspectionDataList?: InspectionDataItem[]
}

export interface QcCameraItem {
  cameraId?: IdValue | null
  cameraCode: string
  cameraName: string
  cameraType: 'local_webcam' | 'ip_camera' | 'industrial_camera' | string
  ipAddress?: string | null
  location?: string | null
  status?: number | string
  remark?: string | null
}

export interface InspectionDataItem {
  dataId: IdValue
  qcRecordId: IdValue
  cameraId?: IdValue
  fileType: string
  filePath: string
  fileName: string
  captureTime?: string
  resultSummary?: string
  remark?: string
}

export interface ExceptionRecordItem {
  exceptionId: IdValue
  planStepId: IdValue
  exceptionType: string
  exceptionLevel: ExceptionLevel
  description: string
  handleResult?: string
  createTime: string
  status: ExceptionStatus
}
