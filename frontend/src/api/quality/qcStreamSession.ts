import request, { type ApiSuccessResponse } from '@/utils/request'
import { readAccessToken } from '@/utils/authToken'
import type { IdValue, QcDetectionBox, QcDetectionResult } from '@/types/domain'
import type { BrowserInferenceResult } from '@/inference/types'

export type QcStreamSessionCreateRequest = {
  planStepId: IdValue
  qcItemId: IdValue
  cameraId?: IdValue | null
  inspector?: string
  remark?: string
}

export type QcStreamSessionInfo = {
  sessionId: string
  streamMode?: string
  cameraId?: IdValue | null
  startedAt?: string
}

export type QcStreamResultMessage = {
  inspectionId?: IdValue
  sessionId?: string
  frameTime?: string
  resultJudge?: string
  confidenceScore?: number | null
  resultValue?: string | null
  defectType?: string | null
  boxes?: QcDetectionBox[]
  renderMode?: string
  imageUrl?: string | null
  sourceImageUrl?: string | null
  autoSaved?: boolean
}

export type QcStreamFramePayload = {
  frameData: string
  frameTime?: string
}

export type QcStreamSnapshotRequest = {
  file: File
  frameTime?: string
  resultJudge?: string
  confidenceScore?: number | null
  resultValue?: string | null
  remark?: string
}

export type QcStreamClientEventRequest = {
  eventId: string
  frameIndex: number
  sourceFile: File
  resultFile: File
  result: BrowserInferenceResult
}

const appendFormValue = (formData: FormData, key: string, value?: string | number | null) => {
  if (value !== undefined && value !== null && value !== '') {
    formData.append(key, String(value))
  }
}

export const createQcStreamSession = (payload: QcStreamSessionCreateRequest) =>
  request<ApiSuccessResponse<QcStreamSessionInfo>>({
    url: '/api/qc-stream-sessions',
    method: 'post',
    data: payload,
  })

export const snapshotQcStreamSession = async (
  sessionId: string,
  payload: QcStreamSnapshotRequest,
): Promise<QcDetectionResult> => {
  const formData = new FormData()
  formData.append('file', payload.file)
  appendFormValue(formData, 'frameTime', payload.frameTime)
  appendFormValue(formData, 'resultJudge', payload.resultJudge)
  appendFormValue(formData, 'confidenceScore', payload.confidenceScore)
  appendFormValue(formData, 'resultValue', payload.resultValue)
  appendFormValue(formData, 'remark', payload.remark?.trim())

  const response = await request<ApiSuccessResponse<QcDetectionResult>>({
    url: `/api/qc-stream-sessions/${sessionId}/snapshot`,
    method: 'post',
    data: formData,
  })

  return response.data
}

export const closeQcStreamSession = (sessionId: string) =>
  request<ApiSuccessResponse<null>>({
    url: `/api/qc-stream-sessions/${sessionId}/close`,
    method: 'post',
  })

export const saveQcStreamClientEvent = async (
  sessionId: string,
  payload: QcStreamClientEventRequest,
): Promise<QcDetectionResult> => {
  const formData = new FormData()
  formData.append('sourceFile', payload.sourceFile)
  formData.append('resultFile', payload.resultFile)
  formData.append('payload', new Blob([JSON.stringify({
    eventId: payload.eventId,
    modelSha256: payload.result.modelSha256,
    frameIndex: payload.frameIndex,
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

  const response = await request<ApiSuccessResponse<QcDetectionResult>>({
    url: `/api/qc-stream-sessions/${sessionId}/client-events`,
    method: 'post',
    data: formData,
    timeout: 60_000,
  })
  return response.data
}

export const buildQcStreamSocketUrl = (sessionId: string) => {
  const baseApi = (import.meta.env.VITE_APP_BASE_API ?? '/prod-api').replace(/\/+$/, '')
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const tokenQuery = `?access_token=${encodeURIComponent(readAccessToken())}`

  if (/^https?:\/\//i.test(baseApi)) {
    return `${baseApi.replace(/^http/i, 'ws')}/ws/qc-stream/${sessionId}${tokenQuery}`
  }

  return `${wsProtocol}//${window.location.host}${baseApi}/ws/qc-stream/${sessionId}${tokenQuery}`
}
