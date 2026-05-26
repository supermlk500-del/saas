export interface DictionaryOption<T extends string | number = string> {
  label: string
  value: T
  color?: string
}

export type OrderStatus = 'NEW' | 'READY' | 'PLANNING' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'
export type BatchStatus = 'NEW' | 'READY' | 'PLANNED' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'
export type PlanStatus = 'DRAFT' | 'RELEASED' | 'RUNNING' | 'COMPLETED' | 'CANCELLED'
export type PlanStepStatus = 'PENDING' | 'READY' | 'RUNNING' | 'PAUSED' | 'FINISHED' | 'ABNORMAL'
export type MachineStatus = 'IDLE' | 'RUNNING' | 'MAINTENANCE' | 'DISABLED'
export type InspectType = 'offline' | 'video'
export type ResultJudge = 'PASS' | 'FAIL' | 'RECHECK'
export type ExceptionLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type ExceptionStatus = 'OPEN' | 'PROCESSING' | 'CLOSED'
export type EnabledStatus = 0 | 1
