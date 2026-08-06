import request, { type ApiSuccessResponse } from '@/utils/request'
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

export type QcStreamClientEventRequest = {
  eventId: string
  frameIndex: number
  frameTime?: string
  sourceFile: File
  resultFile: File
  result: BrowserInferenceResult
}

export const createQcStreamSession = (payload: QcStreamSessionCreateRequest) =>
  request<ApiSuccessResponse<QcStreamSessionInfo>>({
    url: '/api/qc-stream-sessions',
    method: 'post',
    data: payload,
  })

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
    frameTime: payload.frameTime,
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
