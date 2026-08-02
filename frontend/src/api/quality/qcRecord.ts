import request, { type ApiListResponse, type ApiSuccessResponse } from '@/utils/request'
import type { InspectType } from '@/types/dictionary'
import type { IdValue, InspectionDataItem, QcDetectionResult, QcRecordItem } from '@/types/domain'
import type { PageQuery, PageResult } from '@/types/http'
import type { BrowserInferenceResult } from '@/inference/types'

export type QcRecordQuery = PageQuery & {
  planStepId?: IdValue
  qcItemId?: IdValue
  inspectType?: string
  resultJudge?: string
  inspectTimeFrom?: string
  inspectTimeTo?: string
}

export type QcRecordUpsertRequest = {
  planStepId: IdValue
  qcItemId: IdValue
  inspectTime: string
  inspectType: string
  cameraId?: IdValue | null
  frameTime?: string | null
  imageUrl?: string | null
  confidenceScore?: number | null
  resultValue?: string | null
  resultJudge: string
  inspector?: string
  remark?: string
}

export type QcRecordReviewRequest = {
  reviewResult: string
  reviewer?: string
  reviewRemark?: string
}

export type QcRecordCloseRequest = {
  closeRemark?: string
}

export type QcDetectRequest = {
  file: File
  planStepId: IdValue
  qcItemId: IdValue
  cameraId?: IdValue | null
  inspectType?: InspectType
  frameTime?: string | null
  inspector?: string
  remark?: string
}

export type ClientDetectResultRequest = {
  sourceFile: File
  resultFile: File
  result: BrowserInferenceResult
  planStepId: IdValue
  qcItemId: IdValue
  cameraId?: IdValue | null
  inspector?: string
  remark?: string
}

type RawDetectResponse = QcDetectionResult & {
  algorithmResult?: QcDetectionResult
  qcRecord?: QcRecordItem
  inspectionDataList?: InspectionDataItem[]
}

const appendFormValue = (formData: FormData, key: string, value?: string | number | null) => {
  if (value !== undefined && value !== null && value !== '') {
    formData.append(key, String(value))
  }
}

const normalizeDetectResponse = (raw: RawDetectResponse): QcDetectionResult => {
  const algorithmResult = raw.algorithmResult ?? raw
  const qcRecord = raw.qcRecord

  return {
    inspectionId: raw.inspectionId ?? qcRecord?.inspectionId,
    planStepId: raw.planStepId ?? qcRecord?.planStepId,
    qcItemId: raw.qcItemId ?? qcRecord?.qcItemId,
    inspectType: raw.inspectType ?? qcRecord?.inspectType ?? algorithmResult.inspectType,
    resultJudge: raw.resultJudge ?? qcRecord?.resultJudge ?? algorithmResult.resultJudge,
    confidenceScore: raw.confidenceScore ?? qcRecord?.confidenceScore ?? algorithmResult.confidenceScore,
    defectType: raw.defectType ?? qcRecord?.defectType ?? algorithmResult.defectType,
    resultValue: raw.resultValue ?? qcRecord?.resultValue ?? algorithmResult.resultValue,
    imageUrl: raw.imageUrl ?? qcRecord?.imageUrl ?? algorithmResult.imageUrl,
    sourceImageUrl: raw.sourceImageUrl ?? qcRecord?.sourceImageUrl ?? algorithmResult.sourceImageUrl,
    boxes: raw.boxes ?? algorithmResult.boxes ?? [],
    qcRecord,
    inspectionDataList: raw.inspectionDataList ?? [],
  }
}

export const listQcRecords = (query?: QcRecordQuery) =>
  request<ApiListResponse<QcRecordItem>>({
    url: '/api/qc-records',
    method: 'get',
    params: query,
  })

export const getQcRecord = (inspectionId: IdValue) =>
  request<ApiSuccessResponse<QcRecordItem>>({
    url: `/api/qc-records/${inspectionId}`,
    method: 'get',
  })

export const createQcRecord = (payload: QcRecordUpsertRequest) =>
  request<ApiSuccessResponse<QcRecordItem>>({
    url: '/api/qc-records',
    method: 'post',
    data: payload,
  })

export const updateQcRecord = (inspectionId: IdValue, payload: QcRecordUpsertRequest) =>
  request<ApiSuccessResponse<QcRecordItem>>({
    url: `/api/qc-records/${inspectionId}`,
    method: 'put',
    data: payload,
  })

export const reviewQcRecord = (inspectionId: IdValue, payload: QcRecordReviewRequest) =>
  request<ApiSuccessResponse<QcRecordItem>>({
    url: `/api/qc-records/${inspectionId}/review`,
    method: 'patch',
    data: payload,
  })

export const closeQcRecord = (inspectionId: IdValue, payload: QcRecordCloseRequest) =>
  request<ApiSuccessResponse<QcRecordItem>>({
    url: `/api/qc-records/${inspectionId}/close`,
    method: 'patch',
    data: payload,
  })

const buildDetectFormData = (payload: QcDetectRequest) => {
  const formData = new FormData()
  formData.append('file', payload.file)
  appendFormValue(formData, 'planStepId', payload.planStepId)
  appendFormValue(formData, 'qcItemId', payload.qcItemId)
  appendFormValue(formData, 'cameraId', payload.cameraId)
  appendFormValue(formData, 'inspectType', payload.inspectType)
  appendFormValue(formData, 'frameTime', payload.frameTime)
  appendFormValue(formData, 'inspector', payload.inspector?.trim())
  appendFormValue(formData, 'remark', payload.remark?.trim())
  return formData
}

const postDetect = async (url: string, payload: QcDetectRequest): Promise<QcDetectionResult> => {
  const response = await request<ApiSuccessResponse<RawDetectResponse>>({
    url,
    method: 'post',
    data: buildDetectFormData(payload),
  })

  return normalizeDetectResponse(response.data)
}

export const detectImageQcRecord = (payload: QcDetectRequest) =>
  postDetect('/api/qc-records/detect-image', {
    ...payload,
    inspectType: 'offline',
    cameraId: payload.cameraId ?? undefined,
  })

export const saveClientDetectResult = async (payload: ClientDetectResultRequest): Promise<QcDetectionResult> => {
  const formData = new FormData()
  formData.append('sourceFile', payload.sourceFile)
  formData.append('resultFile', payload.resultFile)
  formData.append('payload', new Blob([JSON.stringify({
    planStepId: payload.planStepId,
    qcItemId: payload.qcItemId,
    cameraId: payload.cameraId ?? null,
    inspector: payload.inspector?.trim(),
    remark: payload.remark?.trim(),
    modelSha256: payload.result.modelSha256,
    imageWidth: payload.result.imageWidth,
    imageHeight: payload.result.imageHeight,
    preprocessTimeMs: payload.result.preprocessTimeMs,
    inferenceTimeMs: payload.result.inferenceTimeMs,
    postprocessTimeMs: payload.result.postprocessTimeMs,
    providerStrategy: payload.result.providerStrategy,
    detections: payload.result.detections.map((item) => ({
      classIndex: item.classIndex,
      code: item.code,
      label: item.label,
      score: item.score,
      x1: item.x1,
      y1: item.y1,
      x2: item.x2,
      y2: item.y2,
    })),
  })], { type: 'application/json' }))

  const response = await request<ApiSuccessResponse<RawDetectResponse>>({
    url: '/api/qc-records/client-detect-results',
    method: 'post',
    data: formData,
    timeout: 60_000,
  })

  return normalizeDetectResponse(response.data)
}

export const detectFrameQcRecord = (payload: QcDetectRequest) =>
  postDetect('/api/qc-records/detect-frame', {
    ...payload,
    inspectType: 'video',
  })

export const fetchQcRecords = async (query?: QcRecordQuery): Promise<PageResult<QcRecordItem>> => {
  const response = await listQcRecords(query)
  return {
    list: response.rows ?? [],
    total: response.total ?? 0,
  }
}
