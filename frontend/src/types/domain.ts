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
  qcItemId: number
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
  inspectionId: number
  planStepId: number
  qcItemId: number
  inspectTime: string
  inspectType: InspectType
  cameraId?: number | null
  frameTime?: string | null
  imageUrl?: string | null
  confidenceScore?: number | null
  resultValue?: string | null
  resultJudge: ResultJudge
  inspector?: string
  remark?: string
}

export interface InspectionDataItem {
  dataId: number
  qcRecordId: number
  cameraId?: number
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
