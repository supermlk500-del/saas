<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { SettingOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import QcDetectionResultPanel from '@/components/quality/QcDetectionResultPanel.vue'
import { fetchPlanSteps } from '@/api/plan/planStep'
import { fetchInspectionData } from '@/api/quality/inspectionData'
import { fetchQcCameras } from '@/api/quality/qcCamera'
import {
  closeQcRecord,
  detectImageQcRecord,
  fetchQcRecords,
  getQcRecord,
  reviewQcRecord,
  type QcRecordCloseRequest,
  type QcRecordReviewRequest,
} from '@/api/quality/qcRecord'
import {
  buildQcStreamSocketUrl,
  closeQcStreamSession,
  createQcStreamSession,
  snapshotQcStreamSession,
  type QcStreamResultMessage,
  type QcStreamSessionInfo,
} from '@/api/quality/qcStreamSession'
import { fetchQcItems } from '@/api/quality/qcItem'
import type {
  InspectionDataItem,
  PlanStepItem,
  QcCameraItem,
  QcDetectionBox,
  QcDetectionResult,
  QcItem,
  QcRecordItem,
} from '@/types/domain'
import type { ResultJudge } from '@/types/dictionary'
import { formatDateTime } from '@/utils/date'
import { isLikelyResultImage, isLikelySourceImage, resolveImageUrl } from '@/utils/image'

type IdValue = number | string
type StreamSocketState = 'idle' | 'connecting' | 'open' | 'closed' | 'error'

type MonitorFormModel = {
  planStepId?: IdValue
  qcItemId?: IdValue
  cameraId?: IdValue
  inspector: string
  remark: string
}

const MAX_UPLOAD_SIZE_MB = 50
const MAX_UPLOAD_SIZE = MAX_UPLOAD_SIZE_MB * 1024 * 1024
const STREAM_FRAME_INTERVAL_MS = 500
const STREAM_FRAME_TIMEOUT_MS = 2500
const MAX_STREAM_FRAME_EDGE = 360
const MAX_STREAM_FRAME_BYTES = 7 * 1024

const LOCAL_FALLBACK_CAMERA: QcCameraItem = {
  cameraId: 'LOCAL_BROWSER',
  cameraCode: 'LOCAL_BROWSER',
  cameraName: '电脑摄像头',
  cameraType: 'local_webcam',
  location: '浏览器本机摄像头',
  status: 1,
  remark: '前端默认本机摄像头选项；正式流式检测仍以 Java 返回的 cameraId 为准',
}

const judgeOptions: { label: string; value: ResultJudge; color: string }[] = [
  { label: '通过', value: 'PASS', color: 'success' },
  { label: '不通过', value: 'FAIL', color: 'error' },
  { label: '待复核', value: 'RECHECK', color: 'warning' },
]

const socketStateLabelMap: Record<StreamSocketState, string> = {
  idle: '未连接',
  connecting: '连接中',
  open: '已连接',
  closed: '已关闭',
  error: '连接异常',
}

const route = useRoute()

const monitorFormRef = ref()
const reviewFormRef = ref()

const videoRef = ref<HTMLVideoElement | null>(null)
const frameCanvasRef = ref<HTMLCanvasElement | null>(null)
const overlayCanvasRef = ref<HTMLCanvasElement | null>(null)

const loading = ref(false)
const detecting = ref(false)
const cameraLoading = ref(false)
const cameraActive = ref(false)
const streamPreparing = ref(false)
const streamRunning = ref(false)
const streamClosing = ref(false)
const snapshotSaving = ref(false)
const detailLoading = ref(false)
const reviewModalOpen = ref(false)
const settingsModalOpen = ref(false)
const reviewSubmitting = ref(false)
const closeSubmitting = ref(false)

const planSteps = ref<PlanStepItem[]>([])
const qcItems = ref<QcItem[]>([])
const qcCameras = ref<QcCameraItem[]>([])
const recentRecords = ref<QcRecordItem[]>([])

const activeRecord = ref<QcRecordItem | null>(null)
const activeAttachments = ref<InspectionDataItem[]>([])
const detectionResult = ref<QcDetectionResult | null>(null)
const streamResult = ref<QcStreamResultMessage | null>(null)

const selectedFile = ref<File | null>(null)
const localSourcePreview = ref('')
const frameSourcePreview = ref('')
const closeRemark = ref('')
const mediaStream = ref<MediaStream | null>(null)

const streamSession = ref<QcStreamSessionInfo | null>(null)
const streamSocket = ref<WebSocket | null>(null)
const streamSocketState = ref<StreamSocketState>('idle')
const streamStatusText = ref('未开始实时检测')
const streamErrorText = ref('')
const latestFrameTime = ref('')
const latestFrameLatency = ref<number | null>(null)
const latestFramePayloadSize = ref(0)
const latestSavedFrameTime = ref('')
const latestFrameFile = ref<File | null>(null)
const frameSending = ref(false)
const frameAwaitingResponse = ref(false)
const streamMessageCount = ref(0)
const renderMode = ref('overlay')
const intentionalSocketClose = ref(false)

const overlayBoxes = ref<QcDetectionBox[]>([])
const overlayJudge = ref<ResultJudge | undefined>()
const overlayStatusText = ref('点击“开始检测”后显示实时叠框')
const overlaySourceSize = ref({ width: 0, height: 0 })

const frameLoopTimerId = ref<number | null>(null)
const frameResponseTimeoutId = ref<number | null>(null)
const lastFrameSentAt = ref(0)

const monitorForm = reactive<MonitorFormModel>({
  inspector: '',
  remark: '',
})

const reviewForm = reactive({
  reviewResult: undefined as ResultJudge | undefined,
  reviewer: '',
  reviewRemark: '',
})

const baseRules = {
  planStepId: [{ required: true, message: '请选择工序计划' }],
  qcItemId: [{ required: true, message: '请选择检测标准' }],
}

const monitorRules = {
  ...baseRules,
  cameraId: [{ required: true, message: '请选择摄像头' }],
}

const reviewRules = {
  reviewResult: [{ required: true, message: '请选择复核结果' }],
}

const planStepOptions = computed(() =>
  planSteps.value.map((item) => ({
    label: `#${item.planStepId} / ${item.stepName || item.stepId} / ${item.machineName || '未分配设备'}`,
    value: item.planStepId,
  })),
)

const qcItemOptions = computed(() =>
  qcItems.value.map((item) => ({
    label: `${item.qcItemCode} / ${item.qcItemName}`,
    value: item.qcItemId,
  })),
)

const cameraList = computed(() => {
  const enabledCameras = qcCameras.value.filter((item) => String(item.status ?? 1) !== '0')
  return enabledCameras.length ? enabledCameras : [LOCAL_FALLBACK_CAMERA]
})

const cameraOptions = computed(() =>
  cameraList.value.map((item) => ({
    label: `${item.cameraName} / ${item.cameraType}`,
    value: item.cameraId,
  })),
)

const selectedMonitorPlanStep = computed(() =>
  planSteps.value.find((item) => String(item.planStepId) === String(monitorForm.planStepId)),
)

const selectedCamera = computed(() =>
  cameraList.value.find((item) => String(item.cameraId) === String(monitorForm.cameraId)),
)

const getFirstCameraId = (): IdValue | undefined => {
  const cameraId = cameraList.value.find((item) => item.cameraId !== null && item.cameraId !== undefined)?.cameraId
  return cameraId === null || cameraId === undefined ? undefined : cameraId
}

const currentJudge = computed(() => detectionResult.value?.resultJudge ?? activeRecord.value?.resultJudge)
const currentConfidence = computed(() => detectionResult.value?.confidenceScore ?? activeRecord.value?.confidenceScore)
const currentResultValue = computed(() => detectionResult.value?.resultValue ?? activeRecord.value?.resultValue)
const currentDefectType = computed(() => detectionResult.value?.defectType ?? activeRecord.value?.defectType)
const currentBoxes = computed<QcDetectionBox[]>(() => detectionResult.value?.boxes ?? [])

const sourceAttachment = computed(() =>
  activeAttachments.value.find((item) => isLikelySourceImage(item.filePath)) ??
  activeAttachments.value.find((item) => item.fileType === 'image' || item.fileType === 'frame'),
)

const resultAttachment = computed(() =>
  activeAttachments.value.find((item) => isLikelyResultImage(item.filePath) || item.fileType.includes('result')),
)

const sourceImagePath = computed(() => detectionResult.value?.sourceImageUrl || sourceAttachment.value?.filePath || '')
const resultImagePath = computed(() => detectionResult.value?.imageUrl || activeRecord.value?.imageUrl || resultAttachment.value?.filePath || '')
const offlineSourcePreviewUrl = computed(() => resolveImageUrl(sourceImagePath.value) || localSourcePreview.value)
const streamSnapshotSourcePreviewUrl = computed(() => resolveImageUrl(sourceImagePath.value) || frameSourcePreview.value)
const resultPreviewUrl = computed(() => resolveImageUrl(resultImagePath.value))

const streamJudgeMeta = computed(() => judgeOptions.find((item) => item.value === streamResult.value?.resultJudge))
const streamJudgeLabel = computed(() => streamJudgeMeta.value?.label || streamResult.value?.resultJudge || '-')
const streamConfidenceText = computed(() =>
  streamResult.value?.confidenceScore !== undefined && streamResult.value?.confidenceScore !== null
    ? Number(streamResult.value.confidenceScore).toFixed(2)
    : '-',
)
const streamResultValueText = computed(() => streamResult.value?.resultValue || '-')
const streamDefectTypeText = computed(() => streamResult.value?.defectType || '-')
const streamBoxes = computed(() => streamResult.value?.boxes ?? [])
const streamSocketStateLabel = computed(() => socketStateLabelMap[streamSocketState.value])

const getJudgeMeta = (value?: string) => judgeOptions.find((item) => item.value === value)

const revokeObjectUrl = (target: 'local' | 'frame' | 'all' = 'all') => {
  if ((target === 'local' || target === 'all') && localSourcePreview.value) {
    URL.revokeObjectURL(localSourcePreview.value)
    localSourcePreview.value = ''
  }
  if ((target === 'frame' || target === 'all') && frameSourcePreview.value) {
    URL.revokeObjectURL(frameSourcePreview.value)
    frameSourcePreview.value = ''
  }
}

const clearStoredDetectionState = () => {
  detectionResult.value = null
  activeRecord.value = null
  activeAttachments.value = []
  closeRemark.value = ''
}

const syncOverlayCanvasSize = () => {
  const video = videoRef.value
  const overlayCanvas = overlayCanvasRef.value
  if (!video || !overlayCanvas) {
    return
  }

  const width = Math.max(Math.round(video.clientWidth), 1)
  const height = Math.max(Math.round(video.clientHeight), 1)

  if (overlayCanvas.width !== width || overlayCanvas.height !== height) {
    overlayCanvas.width = width
    overlayCanvas.height = height
  }
}

const clearOverlay = (statusText = '点击“开始检测”后显示实时叠框') => {
  const overlayCanvas = overlayCanvasRef.value
  const context = overlayCanvas?.getContext('2d')
  if (overlayCanvas && context) {
    context.clearRect(0, 0, overlayCanvas.width, overlayCanvas.height)
  }
  overlayBoxes.value = []
  overlayJudge.value = undefined
  overlayStatusText.value = statusText
}

const drawBoxesOnOverlay = (boxes: QcDetectionBox[], judge?: ResultJudge) => {
  syncOverlayCanvasSize()
  const video = videoRef.value
  const overlayCanvas = overlayCanvasRef.value
  const context = overlayCanvas?.getContext('2d')

  if (!video || !overlayCanvas || !context) {
    return
  }

  context.clearRect(0, 0, overlayCanvas.width, overlayCanvas.height)
  overlayBoxes.value = boxes
  overlayJudge.value = judge

  if (!boxes.length || judge === 'PASS') {
    overlayStatusText.value = judge === 'PASS' ? '当前判定为通过，未返回缺陷框' : '当前未返回缺陷框'
    return
  }

  const sourceWidth = overlaySourceSize.value.width || video.videoWidth
  const sourceHeight = overlaySourceSize.value.height || video.videoHeight
  if (!sourceWidth || !sourceHeight) {
    overlayStatusText.value = '视频尺寸未就绪，暂时无法映射框线'
    return
  }

  const displayWidth = overlayCanvas.width
  const displayHeight = overlayCanvas.height
  const scale = Math.min(displayWidth / sourceWidth, displayHeight / sourceHeight)
  const renderedWidth = sourceWidth * scale
  const renderedHeight = sourceHeight * scale
  const offsetX = (displayWidth - renderedWidth) / 2
  const offsetY = (displayHeight - renderedHeight) / 2

  context.lineWidth = 2
  context.font = '12px sans-serif'
  context.textBaseline = 'top'

  boxes.forEach((box) => {
    const x1 = offsetX + ((box.x1 ?? 0) / sourceWidth) * renderedWidth
    const y1 = offsetY + ((box.y1 ?? 0) / sourceHeight) * renderedHeight
    const x2 = offsetX + ((box.x2 ?? 0) / sourceWidth) * renderedWidth
    const y2 = offsetY + ((box.y2 ?? 0) / sourceHeight) * renderedHeight
    const width = Math.max(x2 - x1, 1)
    const height = Math.max(y2 - y1, 1)
    const strokeColor = judge === 'FAIL' ? '#ef4444' : '#f59e0b'
    const label = [box.label, box.score !== undefined ? Number(box.score).toFixed(2) : ''].filter(Boolean).join(' ')

    context.strokeStyle = strokeColor
    context.fillStyle = 'rgba(239, 68, 68, 0.14)'
    context.strokeRect(x1, y1, width, height)
    context.fillRect(x1, y1, width, height)

    if (label) {
      const textWidth = context.measureText(label).width
      const tagHeight = 20
      context.fillStyle = strokeColor
      context.fillRect(x1, Math.max(y1 - tagHeight, 0), textWidth + 12, tagHeight)
      context.fillStyle = '#ffffff'
      context.fillText(label, x1 + 6, Math.max(y1 - tagHeight + 4, 2))
    }
  })

  overlayStatusText.value = `实时叠加 ${boxes.length} 个检测框`
}

const redrawOverlay = () => {
  if (!cameraActive.value) {
    clearOverlay('点击“开始检测”后显示实时叠框')
    return
  }
  drawBoxesOnOverlay(overlayBoxes.value, overlayJudge.value)
}

const handleViewportResize = () => {
  syncOverlayCanvasSize()
  redrawOverlay()
}

const reloadRecentRecords = async () => {
  const qcRecordRes = await fetchQcRecords({ pageNum: 1, pageSize: 10 })
  recentRecords.value = qcRecordRes.list
}

const loadBaseData = async () => {
  loading.value = true
  try {
    const [planStepResult, activeQcItemResult, qcRecordResult, cameraResult] = await Promise.allSettled([
      fetchPlanSteps({ pageNum: 1, pageSize: 200 }),
      fetchQcItems({ pageNum: 1, pageSize: 200, isActive: 1 }),
      fetchQcRecords({ pageNum: 1, pageSize: 10 }),
      fetchQcCameras({ pageNum: 1, pageSize: 50 }),
    ])

    if (planStepResult.status === 'fulfilled') {
      planSteps.value = planStepResult.value.list
    } else {
      planSteps.value = []
      message.error('工序计划加载失败，请检查 /api/plan-steps 接口')
    }

    if (activeQcItemResult.status === 'fulfilled') {
      if (activeQcItemResult.value.list.length) {
        qcItems.value = activeQcItemResult.value.list
      } else {
        const fallbackQcItems = await fetchQcItems({ pageNum: 1, pageSize: 200 })
        qcItems.value = fallbackQcItems.list
      }
    } else {
      qcItems.value = []
      message.error('质检项加载失败，请检查 /api/qc-items 接口')
    }

    if (qcRecordResult.status === 'fulfilled') {
      recentRecords.value = qcRecordResult.value.list
    } else {
      recentRecords.value = []
    }

    if (cameraResult.status === 'fulfilled') {
      qcCameras.value = cameraResult.value.list
    } else {
      qcCameras.value = []
      message.warning('摄像头接口未返回数据，已回退到前端默认“电脑摄像头”选项')
    }

    const routePlanStepId = route.query.planStepId ? String(route.query.planStepId) : ''
    const firstPlanStepId = planSteps.value[0]?.planStepId
    const firstQcItemId = qcItems.value[0]?.qcItemId
    const firstCameraId = getFirstCameraId()

    if (!monitorForm.planStepId) {
      monitorForm.planStepId = routePlanStepId || firstPlanStepId
    }
    if (!monitorForm.qcItemId) {
      monitorForm.qcItemId = firstQcItemId
    }
    if (!monitorForm.cameraId) {
      monitorForm.cameraId = firstCameraId
    }
  } catch (error) {
    message.error('实时质检基础数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadRecordDetail = async (inspectionId: IdValue) => {
  detailLoading.value = true
  try {
    const [record, attachments] = await Promise.all([
      getQcRecord(inspectionId),
      fetchInspectionData({ pageNum: 1, pageSize: 100, qcRecordId: inspectionId }),
    ])
    activeRecord.value = record.data
    activeAttachments.value = attachments.list
  } finally {
    detailLoading.value = false
  }
}

const acceptStoredResult = async (result: QcDetectionResult) => {
  detectionResult.value = result
  if (result.qcRecord) {
    activeRecord.value = result.qcRecord
    activeAttachments.value = result.inspectionDataList ?? []
  }
  if (result.inspectionId) {
    await loadRecordDetail(result.inspectionId)
  }
  await reloadRecentRecords()
}

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  if (file.size > MAX_UPLOAD_SIZE) {
    message.warning(`图片不能超过 ${MAX_UPLOAD_SIZE_MB}MB，请压缩后再上传`)
    input.value = ''
    return
  }
  revokeObjectUrl('local')
  selectedFile.value = file
  localSourcePreview.value = URL.createObjectURL(file)
  clearStoredDetectionState()
}

const resetImageInspection = () => {
  selectedFile.value = null
  revokeObjectUrl('local')
  clearStoredDetectionState()
}

const handleOfflineDetect = async () => {
  if (!monitorForm.planStepId || !monitorForm.qcItemId) {
    message.warning('请先选择工序计划和检测标准')
    return
  }
  if (!selectedFile.value) {
    message.warning('请先选择一张待检测图片')
    return
  }

  detecting.value = true
  try {
    clearStoredDetectionState()
    const result = await detectImageQcRecord({
      file: selectedFile.value,
      planStepId: monitorForm.planStepId as IdValue,
      qcItemId: monitorForm.qcItemId as IdValue,
      inspector: monitorForm.inspector,
      remark: monitorForm.remark,
    })
    await acceptStoredResult(result)
    message.success('图片离线质检完成')
  } finally {
    detecting.value = false
  }
}

const handleVideoReady = () => {
  syncOverlayCanvasSize()
  redrawOverlay()
}

const stopMediaStream = () => {
  mediaStream.value?.getTracks().forEach((track) => track.stop())
  mediaStream.value = null
  cameraActive.value = false
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
}

const ensureCameraPreview = async () => {
  if (cameraActive.value && mediaStream.value) {
    return
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    throw new Error('当前浏览器不支持摄像头调用')
  }

  cameraLoading.value = true
  try {
    stopMediaStream()
    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false })
    mediaStream.value = stream
    await nextTick()
    if (videoRef.value) {
      videoRef.value.srcObject = stream
      await videoRef.value.play()
    }
    cameraActive.value = true
    syncOverlayCanvasSize()
    clearOverlay('视频预览已就绪，等待启动检测')
  } finally {
    cameraLoading.value = false
  }
}

const clearFrameLoopTimer = () => {
  if (frameLoopTimerId.value !== null) {
    window.clearTimeout(frameLoopTimerId.value)
    frameLoopTimerId.value = null
  }
}

const clearFrameResponseTimeout = () => {
  if (frameResponseTimeoutId.value !== null) {
    window.clearTimeout(frameResponseTimeoutId.value)
    frameResponseTimeoutId.value = null
  }
}

const resetStreamVisualState = (statusText = '点击“开始检测”后显示实时叠框') => {
  streamResult.value = null
  latestFrameTime.value = ''
  latestFrameLatency.value = null
  latestFramePayloadSize.value = 0
  streamMessageCount.value = 0
  renderMode.value = 'overlay'
  overlaySourceSize.value = { width: 0, height: 0 }
  clearOverlay(statusText)
}

const renderFrameBlob = async (canvas: HTMLCanvasElement, quality: number) =>
  new Promise<Blob>((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('视频帧生成失败'))
        return
      }
      resolve(blob)
    }, 'image/jpeg', quality)
  })

const captureCurrentFrameFile = async () => {
  const video = videoRef.value
  const canvas = frameCanvasRef.value
  if (!video || !canvas || !video.videoWidth || !video.videoHeight) {
    throw new Error('摄像头画面未就绪')
  }

  const sourceWidth = video.videoWidth
  const sourceHeight = video.videoHeight
  const context = canvas.getContext('2d')
  if (!context) {
    throw new Error('无法创建视频帧画布')
  }
  const maxEdge = Math.max(sourceWidth, sourceHeight)
  let scale = maxEdge > MAX_STREAM_FRAME_EDGE ? MAX_STREAM_FRAME_EDGE / maxEdge : 1
  let quality = 0.62
  let blob: Blob | null = null

  for (let attempt = 0; attempt < 5; attempt += 1) {
    const targetWidth = Math.max(Math.round(sourceWidth * scale), 1)
    const targetHeight = Math.max(Math.round(sourceHeight * scale), 1)
    canvas.width = targetWidth
    canvas.height = targetHeight
    context.clearRect(0, 0, targetWidth, targetHeight)
    context.drawImage(video, 0, 0, targetWidth, targetHeight)

    blob = await renderFrameBlob(canvas, quality)
    overlaySourceSize.value = { width: targetWidth, height: targetHeight }

    if (blob.size <= MAX_STREAM_FRAME_BYTES) {
      break
    }

    scale *= 0.82
    quality *= 0.82
  }

  if (!blob || blob.size > MAX_STREAM_FRAME_BYTES) {
    throw new Error(`视频帧压缩后仍超过 ${Math.round(MAX_STREAM_FRAME_BYTES / 1024)}KB，无法发送实时检测`)
  }

  return new File([blob], `stream_${Date.now()}.jpg`, { type: 'image/jpeg' })
}

const parseSocketPayload = async (data: string | ArrayBuffer | Blob) => {
  if (typeof data === 'string') {
    return JSON.parse(data)
  }
  if (data instanceof Blob) {
    return JSON.parse(await data.text())
  }
  return JSON.parse(new TextDecoder().decode(data))
}

const normalizeStreamMessage = (payload: unknown): QcStreamResultMessage => {
  const record = typeof payload === 'object' && payload !== null ? payload as Record<string, unknown> : {}
  const data = typeof record.data === 'object' && record.data !== null ? record.data as Record<string, unknown> : record
  return {
    sessionId: typeof data.sessionId === 'string' ? data.sessionId : streamSession.value?.sessionId,
    frameTime: typeof data.frameTime === 'string' ? data.frameTime : '',
    resultJudge: typeof data.resultJudge === 'string' ? data.resultJudge : undefined,
    confidenceScore: typeof data.confidenceScore === 'number' ? data.confidenceScore : null,
    resultValue: typeof data.resultValue === 'string' ? data.resultValue : null,
    defectType: typeof data.defectType === 'string' ? data.defectType : null,
    boxes: Array.isArray(data.boxes) ? data.boxes as QcDetectionBox[] : [],
    renderMode: typeof data.renderMode === 'string' ? data.renderMode : 'overlay',
    imageUrl: typeof data.imageUrl === 'string' ? data.imageUrl : null,
    sourceImageUrl: typeof data.sourceImageUrl === 'string' ? data.sourceImageUrl : null,
  }
}

const scheduleNextFrameSend = (delay = STREAM_FRAME_INTERVAL_MS) => {
  clearFrameLoopTimer()
  if (!streamRunning.value) {
    return
  }
  frameLoopTimerId.value = window.setTimeout(() => {
    void pushCurrentFrame()
  }, delay)
}

const handleStreamMessage = async (event: MessageEvent<string | ArrayBuffer | Blob>) => {
  try {
    const payload = await parseSocketPayload(event.data)
    const messageData = normalizeStreamMessage(payload)
    streamResult.value = messageData
    latestFrameTime.value = messageData.frameTime || formatDateTime(new Date().toISOString())
    renderMode.value = messageData.renderMode || 'overlay'
    streamMessageCount.value += 1
    frameAwaitingResponse.value = false
    clearFrameResponseTimeout()
    latestFrameLatency.value = lastFrameSentAt.value ? Date.now() - lastFrameSentAt.value : null
    drawBoxesOnOverlay(messageData.boxes ?? [], messageData.resultJudge as ResultJudge | undefined)
    streamStatusText.value = '实时检测进行中'
  } catch (error) {
    streamErrorText.value = '实时检测结果解析失败'
    streamStatusText.value = '结果解析失败'
  } finally {
    scheduleNextFrameSend()
  }
}

const pushCurrentFrame = async () => {
  const socket = streamSocket.value
  if (!streamRunning.value || !socket || socket.readyState !== WebSocket.OPEN) {
    return
  }
  if (frameSending.value || frameAwaitingResponse.value) {
    scheduleNextFrameSend(120)
    return
  }

  frameSending.value = true
  try {
    const frameFile = await captureCurrentFrameFile()
    latestFrameFile.value = frameFile
    latestFramePayloadSize.value = frameFile.size
    const frameTime = formatDateTime(new Date().toISOString())
    const frameBuffer = await frameFile.arrayBuffer()

    lastFrameSentAt.value = Date.now()
    frameAwaitingResponse.value = true
    streamStatusText.value = '视频帧已发送，等待检测结果'

    clearFrameResponseTimeout()
    frameResponseTimeoutId.value = window.setTimeout(() => {
      frameAwaitingResponse.value = false
      streamStatusText.value = '等待结果超时，继续采样下一帧'
      scheduleNextFrameSend()
    }, STREAM_FRAME_TIMEOUT_MS)

    socket.send(frameBuffer)
  } catch (error) {
    frameAwaitingResponse.value = false
    streamErrorText.value = error instanceof Error ? error.message : '发送视频帧失败'
    streamStatusText.value = '发送视频帧失败'
    scheduleNextFrameSend(STREAM_FRAME_INTERVAL_MS * 2)
  } finally {
    frameSending.value = false
  }
}

const connectStreamSocket = async (sessionId: string) =>
  new Promise<void>((resolve, reject) => {
    try {
      const socket = new WebSocket(buildQcStreamSocketUrl(sessionId))
      streamSocket.value = socket
      streamSocketState.value = 'connecting'
      let settled = false

      socket.onopen = () => {
        streamSocketState.value = 'open'
        streamStatusText.value = '实时检测通道已连接'
        streamErrorText.value = ''
        streamRunning.value = true
        settled = true
        scheduleNextFrameSend(0)
        resolve()
      }

      socket.onmessage = (event) => {
        void handleStreamMessage(event)
      }

      socket.onerror = () => {
        streamSocketState.value = 'error'
        streamErrorText.value = 'WebSocket 连接异常'
        streamStatusText.value = '实时检测连接异常'
        if (!settled) {
          settled = true
          reject(new Error('实时检测 WebSocket 连接失败'))
        }
      }

      socket.onclose = () => {
        streamSocket.value = null
        clearFrameLoopTimer()
        clearFrameResponseTimeout()
        frameAwaitingResponse.value = false
        frameSending.value = false
        streamSocketState.value = streamSocketState.value === 'error' ? 'error' : 'closed'
        if (intentionalSocketClose.value) {
          streamStatusText.value = '实时检测已停止'
          intentionalSocketClose.value = false
          return
        }
        if (streamRunning.value) {
          streamRunning.value = false
          streamStatusText.value = '实时检测连接已断开'
          streamErrorText.value = 'WebSocket 已断开，请重新开始检测'
          clearOverlay('实时连接已断开')
        }
      }
    } catch (error) {
      reject(error)
    }
  })

const ensureMonitorReady = async () => {
  await monitorFormRef.value?.validate()
  if (!monitorForm.planStepId || !monitorForm.qcItemId || !monitorForm.cameraId) {
    throw new Error('请先补全工序计划、质检项和摄像头')
  }
  if (String(monitorForm.cameraId) === String(LOCAL_FALLBACK_CAMERA.cameraId)) {
    throw new Error('后端未返回可用 cameraId，请先确认 /api/qc-cameras 接口')
  }
}

const startRealtimeDetection = async () => {
  if (streamRunning.value || streamPreparing.value) {
    return
  }
  streamPreparing.value = true
  streamErrorText.value = ''
  streamStatusText.value = '正在准备实时检测会话'
  try {
    await ensureMonitorReady()
    await ensureCameraPreview()
    clearStoredDetectionState()
    resetStreamVisualState('正在创建实时检测会话')

    const sessionResponse = await createQcStreamSession({
      planStepId: monitorForm.planStepId as IdValue,
      qcItemId: monitorForm.qcItemId as IdValue,
      cameraId: monitorForm.cameraId as IdValue,
      inspector: monitorForm.inspector.trim() || undefined,
      remark: monitorForm.remark.trim() || undefined,
    })
    streamSession.value = sessionResponse.data
    streamStatusText.value = '实时检测会话已创建，正在连接数据通道'

    await connectStreamSocket(sessionResponse.data.sessionId)
    message.success('实时视频流质检已启动')
  } catch (error) {
    streamErrorText.value = error instanceof Error ? error.message : '实时视频流检测启动失败'
    streamStatusText.value = '实时检测启动失败'
    await stopRealtimeDetection({ silent: true, preserveStatusText: true })
    message.error(streamErrorText.value)
  } finally {
    streamPreparing.value = false
  }
}

const stopRealtimeDetection = async ({ silent = false, preserveStatusText = false }: { silent?: boolean; preserveStatusText?: boolean } = {}) => {
  if (streamClosing.value) {
    return
  }
  streamClosing.value = true

  const sessionId = streamSession.value?.sessionId
  streamRunning.value = false
  clearFrameLoopTimer()
  clearFrameResponseTimeout()
  frameAwaitingResponse.value = false
  frameSending.value = false

  try {
    if (streamSocket.value) {
      intentionalSocketClose.value = true
      streamSocket.value.close(1000, 'client-close')
      streamSocket.value = null
    }
    if (sessionId) {
      try {
        await closeQcStreamSession(sessionId)
      } catch (error) {
        if (!silent) {
          message.error('检测会话关闭失败，请稍后重试')
        }
      }
    }
  } finally {
    streamSession.value = null
    streamSocketState.value = 'idle'
    stopMediaStream()
    resetStreamVisualState(preserveStatusText ? streamStatusText.value : '点击“开始检测”后显示实时叠框')
    if (!preserveStatusText) {
      streamStatusText.value = '实时检测已停止'
      streamErrorText.value = ''
    }
    streamClosing.value = false
  }
}

const saveCurrentFrameSnapshot = async () => {
  const sessionId = streamSession.value?.sessionId
  if (!sessionId || !streamRunning.value) {
    message.warning('请先启动实时视频流检测')
    return
  }

  snapshotSaving.value = true
  try {
    const frameFile = await captureCurrentFrameFile()
    latestFrameFile.value = frameFile
    revokeObjectUrl('frame')
    frameSourcePreview.value = URL.createObjectURL(frameFile)

    const snapshotResult = await snapshotQcStreamSession(sessionId, {
      file: frameFile,
      frameTime: latestFrameTime.value || formatDateTime(new Date().toISOString()),
      resultJudge: streamResult.value?.resultJudge,
      confidenceScore: streamResult.value?.confidenceScore ?? undefined,
      resultValue: streamResult.value?.resultValue ?? undefined,
      remark: monitorForm.remark.trim() || '实时视频流关键帧留档',
    })

    latestSavedFrameTime.value = latestFrameTime.value || formatDateTime(new Date().toISOString())
    await acceptStoredResult(snapshotResult)
    message.success('关键帧已保存为正式质检记录')
  } catch (error) {
    message.error('关键帧保存失败，请确认实时会话与后端快照接口')
  } finally {
    snapshotSaving.value = false
  }
}

const openReviewModal = () => {
  reviewForm.reviewResult = currentJudge.value as ResultJudge | undefined
  reviewForm.reviewer = ''
  reviewForm.reviewRemark = ''
  reviewModalOpen.value = true
}

const handleReviewSubmit = async () => {
  if (!activeRecord.value) {
    return
  }
  await reviewFormRef.value?.validate()
  reviewSubmitting.value = true
  try {
    const payload: QcRecordReviewRequest = {
      reviewResult: reviewForm.reviewResult ?? 'PASS',
      reviewer: reviewForm.reviewer.trim() || undefined,
      reviewRemark: reviewForm.reviewRemark.trim() || undefined,
    }
    await reviewQcRecord(activeRecord.value.inspectionId, payload)
    reviewModalOpen.value = false
    message.success('复核已保存')
    await loadRecordDetail(activeRecord.value.inspectionId)
    await reloadRecentRecords()
  } finally {
    reviewSubmitting.value = false
  }
}

const handleCloseRecord = async () => {
  if (!activeRecord.value) {
    return
  }
  closeSubmitting.value = true
  try {
    const payload: QcRecordCloseRequest = {
      closeRemark: closeRemark.value.trim() || undefined,
    }
    await closeQcRecord(activeRecord.value.inspectionId, payload)
    closeRemark.value = ''
    message.success('质检记录已关闭')
    await loadRecordDetail(activeRecord.value.inspectionId)
    await reloadRecentRecords()
  } finally {
    closeSubmitting.value = false
  }
}

const resetMonitorForm = async () => {
  await stopRealtimeDetection({ silent: true })
  monitorForm.planStepId = route.query.planStepId ? String(route.query.planStepId) : planSteps.value[0]?.planStepId
  monitorForm.qcItemId = qcItems.value[0]?.qcItemId
  monitorForm.cameraId = getFirstCameraId()
  monitorForm.inspector = ''
  monitorForm.remark = ''
  streamStatusText.value = '未开始实时检测'
  streamErrorText.value = ''
  latestSavedFrameTime.value = ''
  revokeObjectUrl('frame')
  clearStoredDetectionState()
}

const reloadCameras = async () => {
  cameraLoading.value = true
  try {
    const cameraRes = await fetchQcCameras({ pageNum: 1, pageSize: 50 })
    qcCameras.value = cameraRes.list
    monitorForm.cameraId = getFirstCameraId()
    message.success('摄像头列表已刷新')
  } finally {
    cameraLoading.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleViewportResize)
  await loadBaseData()
  if (recentRecords.value[0]?.inspectionId) {
    await loadRecordDetail(recentRecords.value[0].inspectionId)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleViewportResize)
  void stopRealtimeDetection({ silent: true })
  revokeObjectUrl()
})
</script>

<template>
  <div class="realtime-page">
    <div class="mode-shell">
          <div class="workbench-grid">
            <a-card class="page-card" :bordered="false" title="实时视频流采集">
              <a-spin :spinning="loading || streamPreparing">
                <div class="capture-workspace">
                  <div class="camera-section">
                    <div class="camera-preview">
                      <div class="video-toolbar">
                        <div class="section-heading">实时视频源</div>
                        <a-button class="settings-button" @click="settingsModalOpen = true">
                          <template #icon><SettingOutlined /></template>
                          设置
                        </a-button>
                      </div>
                      <div class="video-frame">
                        <video ref="videoRef" muted playsinline @loadedmetadata="handleVideoReady" />
                        <canvas ref="overlayCanvasRef" class="overlay-canvas" />
                        <div v-if="cameraActive" class="overlay-status">
                          {{ overlayStatusText }}
                        </div>
                        <a-empty v-if="!cameraActive" description="点击“开始检测”后调用电脑摄像头并进入实时流检测" />
                      </div>
                      <canvas ref="frameCanvasRef" class="capture-canvas" />
                      <div class="camera-actions">
                        <a-button :loading="cameraLoading" :disabled="cameraActive" @click="ensureCameraPreview">打开视频预览</a-button>
                        <a-button :disabled="!cameraActive && !streamRunning" @click="stopRealtimeDetection">停止检测</a-button>
                        <a-button type="primary" :loading="streamPreparing" :disabled="streamRunning" @click="startRealtimeDetection">
                          开始检测
                        </a-button>
                        <a-button type="primary" ghost :loading="snapshotSaving" :disabled="!streamRunning" @click="saveCurrentFrameSnapshot">
                          保存当前帧
                        </a-button>
                      </div>
                    </div>
                  </div>

                  <div class="image-check-section">
                    <div class="section-heading">单张图片补检</div>
                    <div class="image-check-grid">
                      <div class="image-check-upload">
                        <input type="file" accept="image/*" @change="handleFileChange" />
                        <div class="compact-preview-box">
                          <a-image v-if="localSourcePreview" :src="localSourcePreview" alt="本地原图预览" />
                          <a-empty v-else description="需要单张复检时选择图片" />
                        </div>
                        <div class="path-text">
                          {{ selectedFile?.name || `支持单张图片，建议小于 ${MAX_UPLOAD_SIZE_MB}MB` }}
                        </div>
                      </div>

                      <div class="image-check-actions">
                        <div class="path-text">
                          图片补检复用设置中的工序计划、检测标准、检验人和备注，结果会在下方归档详情里展示。
                        </div>
                        <div class="camera-actions compact-actions">
                          <a-button @click="resetImageInspection">清空图片</a-button>
                          <a-button type="primary" :loading="detecting" @click="handleOfflineDetect">上传并检测</a-button>
                        </div>
                      </div>
                    </div>

                    <a-card size="small" title="归档结果详情" class="image-result-card">
                      <QcDetectionResultPanel
                        v-model:close-remark="closeRemark"
                        :detail-loading="detailLoading"
                        :active-record="activeRecord"
                        :detection-result="detectionResult"
                        :active-plan-step="selectedMonitorPlanStep"
                        :current-judge="currentJudge"
                        :current-confidence="currentConfidence"
                        :current-defect-type="currentDefectType"
                        :current-result-value="currentResultValue"
                        :source-preview-url="streamSnapshotSourcePreviewUrl || offlineSourcePreviewUrl"
                        :result-preview-url="resultPreviewUrl"
                        :source-image-path="sourceImagePath"
                        :result-image-path="resultImagePath"
                        :current-boxes="currentBoxes"
                        :active-attachments="activeAttachments"
                        :close-submitting="closeSubmitting"
                        @review="openReviewModal"
                        @close="handleCloseRecord"
                      />
                    </a-card>
                  </div>
                </div>
              </a-spin>
            </a-card>

            <a-card class="page-card" :bordered="false" title="实时结果与关键帧">
              <div class="stream-result-shell">
                <div class="stream-overview">
                  <div class="judge-block">
                    <span class="label">实时判定</span>
                    <a-tag :color="streamJudgeMeta?.color">{{ streamJudgeLabel }}</a-tag>
                  </div>
                  <div class="metric">
                    <span>置信度</span>
                    <strong>{{ streamConfidenceText }}</strong>
                  </div>
                  <div class="metric">
                    <span>结果值</span>
                    <strong>{{ streamResultValueText }}</strong>
                  </div>
                  <div class="metric">
                    <span>缺陷类型</span>
                    <strong>{{ streamDefectTypeText }}</strong>
                  </div>
                </div>

                <a-card size="small" title="关键帧留档" class="inner-card">
                  <div class="image-grid">
                    <a-card size="small" title="关键帧原图">
                      <div class="image-frame">
                        <a-image v-if="streamSnapshotSourcePreviewUrl" :src="streamSnapshotSourcePreviewUrl" alt="关键帧原图" />
                        <a-empty v-else description="保存关键帧后显示原图" />
                      </div>
                    </a-card>
                    <a-card size="small" title="关键帧结果图">
                      <div class="image-frame">
                        <a-image v-if="resultPreviewUrl" :src="resultPreviewUrl" alt="关键帧结果图" />
                        <a-empty v-else description="后端生成结果图后显示" />
                      </div>
                    </a-card>
                  </div>
                  <div class="path-text">
                    {{ latestSavedFrameTime ? `最近保存时间：${latestSavedFrameTime}` : '“保存当前帧”会调用 /api/qc-stream-sessions/{sessionId}/snapshot 落库' }}
                  </div>
                </a-card>

                <a-card size="small" title="当前帧结构化 boxes" class="inner-card">
                  <a-table
                    v-if="streamBoxes.length"
                    :columns="[
                      { title: '缺陷', dataIndex: 'label', key: 'label' },
                      { title: '置信度', dataIndex: 'score', key: 'score' },
                      { title: 'x1', dataIndex: 'x1', key: 'x1' },
                      { title: 'y1', dataIndex: 'y1', key: 'y1' },
                      { title: 'x2', dataIndex: 'x2', key: 'x2' },
                      { title: 'y2', dataIndex: 'y2', key: 'y2' },
                    ]"
                    :data-source="streamBoxes"
                    :pagination="false"
                    size="small"
                    row-key="label"
                  />
                  <a-empty v-else description="当前帧未返回 boxes，主展示以视频叠框状态为准" />
                </a-card>

              </div>
            </a-card>
          </div>
    </div>

    <a-card class="page-card" :bordered="false" title="最近质检记录">
      <div class="recent-list">
        <div v-for="item in recentRecords" :key="item.inspectionId" class="recent-item" @click="loadRecordDetail(item.inspectionId)">
          <div>
            <div class="recent-title">#{{ item.inspectionId }} / 工序计划 {{ item.planStepId }}</div>
            <div class="recent-sub">{{ formatDateTime(item.inspectTime) }} / {{ item.inspectType }}</div>
          </div>
          <a-tag :color="getJudgeMeta(item.resultJudge)?.color">
            {{ getJudgeMeta(item.resultJudge)?.label || item.resultJudge }}
          </a-tag>
        </div>
      </div>
    </a-card>

    <a-modal
      v-model:open="settingsModalOpen"
      title="实时质检设置"
      width="760px"
      :footer="null"
      :force-render="true"
    >
      <a-form ref="monitorFormRef" :model="monitorForm" :rules="monitorRules" layout="vertical">
        <div class="form-grid settings-form-grid">
          <a-form-item label="工序计划" name="planStepId">
            <a-select
              v-model:value="monitorForm.planStepId"
              :options="planStepOptions"
              placeholder="请选择工序计划"
              show-search
              option-filter-prop="label"
            />
          </a-form-item>

          <a-form-item label="检测标准（质检项）" name="qcItemId">
            <a-select
              v-model:value="monitorForm.qcItemId"
              :options="qcItemOptions"
              placeholder="请选择检测标准"
              show-search
              option-filter-prop="label"
            />
          </a-form-item>

          <a-form-item label="摄像头" name="cameraId">
            <a-select
              v-model:value="monitorForm.cameraId"
              :options="cameraOptions"
              placeholder="请选择摄像头"
              show-search
              option-filter-prop="label"
            />
          </a-form-item>

          <a-form-item label="检验人" name="inspector">
            <a-input v-model:value="monitorForm.inspector" placeholder="请输入检验人" />
          </a-form-item>

          <a-form-item label="备注" name="remark" class="full-field">
            <a-input v-model:value="monitorForm.remark" placeholder="可填写实时检测会话备注" />
          </a-form-item>
        </div>
      </a-form>

      <div class="settings-session">
        <div class="section-heading">实时会话</div>
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item label="计划ID">{{ selectedMonitorPlanStep?.planId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="工序计划ID">{{ selectedMonitorPlanStep?.planStepId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="工序">{{ selectedMonitorPlanStep?.stepName || '未选择' }}</a-descriptions-item>
          <a-descriptions-item label="当前摄像头">{{ selectedCamera?.cameraName || '未选择' }}</a-descriptions-item>
          <a-descriptions-item label="摄像头类型">{{ selectedCamera?.cameraType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="会话 ID">{{ streamSession?.sessionId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="连接状态">{{ streamSocketStateLabel }}</a-descriptions-item>
          <a-descriptions-item label="会话开始时间">{{ streamSession?.startedAt || '-' }}</a-descriptions-item>
          <a-descriptions-item label="最近帧时间">{{ latestFrameTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="推流频率">{{ STREAM_FRAME_INTERVAL_MS }}ms / 次</a-descriptions-item>
          <a-descriptions-item label="提交链路">POST /api/qc-stream-sessions + WebSocket /ws/qc-stream/{sessionId}</a-descriptions-item>
        </a-descriptions>
        <a-card size="small" title="实时状态" class="settings-status-card">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="状态">{{ streamStatusText }}</a-descriptions-item>
            <a-descriptions-item label="错误信息">{{ streamErrorText || '-' }}</a-descriptions-item>
            <a-descriptions-item label="实时消息数">{{ streamMessageCount }}</a-descriptions-item>
            <a-descriptions-item label="最近延迟">
              {{ latestFrameLatency !== null ? `${latestFrameLatency} ms` : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="当前帧大小">
              {{ latestFramePayloadSize ? `${Math.round(latestFramePayloadSize / 1024)} KB` : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="渲染模式">{{ renderMode }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
        <div class="summary-actions">
          <a-button size="small" :loading="cameraLoading" @click="reloadCameras">刷新摄像头列表</a-button>
          <a-button size="small" @click="resetMonitorForm">重置实时模式</a-button>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-model:open="reviewModalOpen"
      title="人工复核"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="reviewSubmitting"
      width="560px"
      @ok="handleReviewSubmit"
    >
      <a-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" layout="vertical">
        <a-form-item label="复核结果" name="reviewResult">
          <a-select v-model:value="reviewForm.reviewResult" :options="judgeOptions" placeholder="请选择复核结果" />
        </a-form-item>
        <a-form-item label="复核人" name="reviewer">
          <a-input v-model:value="reviewForm.reviewer" placeholder="请输入复核人" />
        </a-form-item>
        <a-form-item label="复核说明" name="reviewRemark">
          <a-textarea v-model:value="reviewForm.reviewRemark" :rows="3" placeholder="请输入复核说明" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.realtime-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mode-shell,
.page-card {
  border-radius: 12px;
}

.mode-shell {
  padding: 0 0 4px;
}

.settings-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.video-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.video-toolbar .section-heading {
  margin-bottom: 0;
}

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(420px, 0.8fr);
  align-items: start;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.full-field {
  grid-column: 1 / -1;
}

.image-check-section {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(145, 158, 171, 0.14);
}

.image-check-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(220px, 0.7fr);
  gap: 16px;
}

.image-check-actions {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 100%;
}

.image-result-card {
  margin-top: 16px;
}

.compact-preview-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  margin-top: 10px;
  border: 1px dashed rgba(145, 158, 171, 0.35);
  border-radius: 12px;
  background: #f8fafc;
  overflow: hidden;
}

.compact-actions {
  justify-content: flex-end;
}

.camera-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.72fr);
  gap: 16px;
  margin-top: 4px;
}

.camera-preview {
  grid-column: 1 / -1;
}

.section-heading {
  margin-bottom: 10px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
}

.field-alert {
  margin-bottom: 16px;
}

.image-frame,
.video-frame {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  margin-top: 12px;
  border: 1px dashed rgba(145, 158, 171, 0.35);
  border-radius: 12px;
  background: #f8fafc;
  overflow: hidden;
}

.video-frame {
  position: relative;
  height: clamp(420px, calc(100vh - 420px), 560px);
  min-height: 0;
  background: radial-gradient(circle at top left, rgba(59, 130, 246, 0.18), transparent 38%),
    linear-gradient(145deg, #111827, #293241);
}

.video-frame video {
  width: 100%;
  height: 100%;
  min-height: 0;
  object-fit: contain;
}

.video-frame :deep(.ant-empty) {
  position: absolute;
  color: #fff;
}

.overlay-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.overlay-status {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 2;
  padding: 6px 10px;
  color: #f8fafc;
  font-size: 12px;
  line-height: 1;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  backdrop-filter: blur(8px);
  pointer-events: none;
}

.capture-canvas {
  display: none;
}

:deep(.compact-preview-box img),
:deep(.image-frame img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.camera-actions,
.summary-actions,
.submit-row {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.summary-actions {
  justify-content: flex-end;
}

.settings-session {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(145, 158, 171, 0.14);
}

.settings-session :deep(.ant-descriptions-item-content) {
  word-break: break-word;
}

.settings-status-card {
  margin-top: 14px;
}

.submit-row {
  justify-content: flex-end;
}

.stream-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.judge-block,
.metric {
  min-height: 74px;
  padding: 14px;
  border: 1px solid rgba(145, 158, 171, 0.16);
  border-radius: 12px;
  background: #f8fafc;
}

.judge-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.metric span,
.label {
  display: block;
  margin-bottom: 8px;
  color: #667085;
}

.metric strong {
  color: #1f2937;
  font-size: 18px;
}

.image-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.path-text {
  margin-top: 8px;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}

.inner-card {
  margin-top: 16px;
}

.stream-result-shell {
  display: flex;
  flex-direction: column;
}

.recent-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  cursor: pointer;
}

.recent-title {
  color: #1f2937;
  font-weight: 600;
}

.recent-sub {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

@media (max-width: 1280px) {
  .workbench-grid,
  .camera-section,
  .image-check-grid,
  .image-grid {
    grid-template-columns: 1fr;
  }

  .stream-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .form-grid,
  .recent-list {
    grid-template-columns: 1fr;
  }

  .camera-actions,
  .submit-row,
  .summary-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
