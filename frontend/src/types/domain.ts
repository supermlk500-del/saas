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

export interface BatchItem {
  batchId: number
  batchNo: string
  supplier: string
  inDate: string
  weight?: number
  width?: number
  composition?: string
  note?: string
  status?: BatchStatus
}

export interface ProcessRouteItem {
  routeId: number
  routeName: string
  description?: string
  isActive: EnabledStatus
  createTime?: string
}

export interface ProcessStepItem {
  stepId: number
  stepCode: string
  stepName: string
  stepType?: string
  sortOrder?: number
  defaultHours?: number
  description?: string
  isActive: EnabledStatus
}

export interface RouteStepItem {
  routeStepId: number
  routeId: number
  stepId: number
  sortOrder: number
  isMandatory: EnabledStatus
  stepCode?: string
  stepName?: string
}

export interface MachineItem {
  machineId: number
  machineCode: string
  machineName: string
  machineType?: string
  description?: string
  status: MachineStatus
  createTime?: string
}

export interface StepMachineCapabilityItem {
  capId: number
  stepId: number
  machineId: number
  minWidth?: number
  maxWidth?: number
  maxSpeed?: number
  maxBatchWeight?: number
  isActive: EnabledStatus
}

export interface ProductionPlanItem {
  planId: number
  batchId: number
  batchNo?: string
  routeId: number
  routeName?: string
  planStartTime: string
  planEndTime?: string
  status: PlanStatus
  createTime?: string
  remark?: string
}

export interface ProductionPlanDetailItem {
  planId: number
  batchInfo: BatchItem
  routeInfo: ProcessRouteItem
  planStartTime: string
  planEndTime?: string
  status: PlanStatus
  remark?: string
  planSteps: PlanStepItem[]
}

export interface PlanStepItem {
  planStepId: number
  planId: number
  stepId: number
  stepName?: string
  machineId?: number
  machineName?: string
  planStartTime?: string
  planEndTime?: string
  planHours?: number
  sequenceNo?: number
  status: PlanStepStatus
  remark?: string
}

export interface GanttTaskItem {
  planStepId: number
  stepName?: string
  machineName?: string
  start?: string
  end?: string
  status: PlanStepStatus
}

export interface ProcessParameterItem {
  paramId: number
  planStepId: number
  paramName: string
  paramValue: string
  unit?: string
  paramType?: string
  recordTime: string
  remark?: string
}

export interface QcItem {
  qcItemId: number | string
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
  inspectionId: number | string
  planStepId: number | string
  qcItemId: number | string
  inspectTime: string
  inspectType: InspectType
  cameraId?: number | string | null
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
  inspectionId?: number | string
  planStepId?: number | string
  qcItemId?: number | string
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
  cameraId?: number | string | null
  cameraCode: string
  cameraName: string
  cameraType: 'local_webcam' | 'ip_camera' | 'industrial_camera' | string
  ipAddress?: string | null
  location?: string | null
  status?: number | string
  remark?: string | null
}

export interface InspectionDataItem {
  dataId: number | string
  qcRecordId: number | string
  cameraId?: number | string
  fileType: string
  filePath: string
  fileName: string
  captureTime?: string
  resultSummary?: string
  remark?: string
}

export interface ExceptionRecordItem {
  exceptionId: number
  planStepId: number
  exceptionType: string
  exceptionLevel: ExceptionLevel
  description: string
  handleResult?: string
  createTime: string
  status: ExceptionStatus
}
