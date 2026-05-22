import type {
  BatchStatus,
  DictionaryOption,
  EnabledStatus,
  ExceptionLevel,
  ExceptionStatus,
  InspectType,
  MachineStatus,
  PlanStatus,
  PlanStepStatus,
  ResultJudge,
} from '@/types/dictionary'

export const batchStatusOptions: DictionaryOption<BatchStatus>[] = [
  { label: '新建', value: 'NEW', color: 'default' },
  { label: '待排产', value: 'READY', color: 'blue' },
  { label: '已排产', value: 'PLANNED', color: 'cyan' },
  { label: '生产中', value: 'IN_PROGRESS', color: 'processing' },
  { label: '已完成', value: 'DONE', color: 'success' },
  { label: '已作废', value: 'CANCELLED', color: 'error' },
]

export const planStatusOptions: DictionaryOption<PlanStatus>[] = [
  { label: '草稿', value: 'DRAFT', color: 'default' },
  { label: '已下发', value: 'RELEASED', color: 'blue' },
  { label: '执行中', value: 'RUNNING', color: 'processing' },
  { label: '已完成', value: 'COMPLETED', color: 'success' },
  { label: '已取消', value: 'CANCELLED', color: 'error' },
]

export const planStepStatusOptions: DictionaryOption<PlanStepStatus>[] = [
  { label: '待处理', value: 'PENDING', color: 'default' },
  { label: '已就绪', value: 'READY', color: 'blue' },
  { label: '执行中', value: 'RUNNING', color: 'processing' },
  { label: '已暂停', value: 'PAUSED', color: 'warning' },
  { label: '已完成', value: 'FINISHED', color: 'success' },
  { label: '异常', value: 'ABNORMAL', color: 'error' },
]

export const machineStatusOptions: DictionaryOption<MachineStatus>[] = [
  { label: '空闲', value: 'IDLE', color: 'default' },
  { label: '运行中', value: 'RUNNING', color: 'processing' },
  { label: '维护中', value: 'MAINTENANCE', color: 'warning' },
  { label: '已停用', value: 'DISABLED', color: 'error' },
]

export const inspectTypeOptions: DictionaryOption<InspectType>[] = [
  { label: '人工检验', value: 'offline' },
  { label: '视频检验', value: 'video' },
]

export const resultJudgeOptions: DictionaryOption<ResultJudge>[] = [
  { label: '通过', value: 'PASS', color: 'success' },
  { label: '不通过', value: 'FAIL', color: 'error' },
  { label: '待复检', value: 'RECHECK', color: 'warning' },
]

export const exceptionLevelOptions: DictionaryOption<ExceptionLevel>[] = [
  { label: '低', value: 'LOW', color: 'default' },
  { label: '中', value: 'MEDIUM', color: 'blue' },
  { label: '高', value: 'HIGH', color: 'warning' },
  { label: '紧急', value: 'CRITICAL', color: 'error' },
]

export const exceptionStatusOptions: DictionaryOption<ExceptionStatus>[] = [
  { label: '待处理', value: 'OPEN', color: 'error' },
  { label: '处理中', value: 'PROCESSING', color: 'processing' },
  { label: '已关闭', value: 'CLOSED', color: 'success' },
]

export const enabledStatusOptions: DictionaryOption<EnabledStatus>[] = [
  { label: '启用', value: 1, color: 'success' },
  { label: '停用', value: 0, color: 'default' },
]
