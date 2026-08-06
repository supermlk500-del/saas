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
  fetchQcRecords,
  getQcRecord,
  reviewQcRecord,
  saveClientDetectResult,
  type QcRecordCloseRequest,
  type QcRecordReviewRequest,
} from '@/api/quality/qcRecord'
import {
  closeQcStreamSession,
  createQcStreamSession,
  saveQcStreamClientEvent,
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
import { formatInspectionId, formatPlanId, formatPlanStepId } from '@/utils/idFormat'
import { isLikelyResultImage, isLikelySourceImage, resolveImageUrl } from '@/utils/image'
import { browserInferenceClient } from '@/inference/browserInferenceClient'
import {
  EvidenceUploadQueue,
  type EvidenceQueueSnapshot,
} from '@/inference/evidenceUploadQueue'
import { renderDetectionsToBlob } from '@/inference/rendering'
import { RealtimeHitTracker } from '@/inference/realtimeHitTracker'
import type { BrowserInferenceResult, InferenceInitializationStage } from '@/inference/types'

type IdValue = number | string
type LocalInferenceState = 'idle' | 'initializing' | 'ready' | 'closed' | 'error'
type MotionCompensationDirection = 'none' | 'right' | 'left' | 'down' | 'up'

type MonitorFormModel = {
  planStepId?: IdValue
  qcItemId?: IdValue
  cameraId?: IdValue
  inspector: string
  remark: string
}

const MAX_UPLOAD_SIZE_MB = 50
const MAX_UPLOAD_SIZE = MAX_UPLOAD_SIZE_MB * 1024 * 1024
const DEFAULT_TARGET_INFERENCE_FPS = 10
const MAX_REALTIME_FRAME_EDGE = 1280
const REALTIME_EVIDENCE_JPEG_QUALITY = 0.88

const LOCAL_FALLBACK_CAMERA: QcCameraItem = {
  cameraId: 'LOCAL_BROWSER',
  cameraCode: 'LOCAL_BROWSER',
  cameraName: '电脑摄像头',
  cameraType: 'local_webcam',
  location: '浏览器本机摄像头',
  status: 1,
  remark: '前端默认本机摄像头选项；浏览器本地推理模式可不绑定后端 cameraId',
}

const judgeOptions: { label: string; value: ResultJudge; color: string }[] = [
  { label: '通过', value: 'PASS', color: 'success' },
  { label: '不通过', value: 'FAIL', color: 'error' },
  { label: '待复核', value: 'RECHECK', color: 'warning' },
]

const localInferenceStateLabelMap: Record<LocalInferenceState, string> = {
  idle: '未连接',
  initializing: '初始化中',
  ready: '本地推理就绪',
  closed: '已关闭',
  error: '本地推理异常',
}

const initializationStageLabelMap: Record<InferenceInitializationStage, string> = {
  DOWNLOADING: '正在下载模型',
  VERIFYING_SHA256: '正在校验模型完整性',
  CREATING_SESSION: '正在创建推理会话',
  WARMING_UP: '正在预热模型',
  READY: '本地推理就绪',
}

const motionDirectionOptions: { label: string; value: MotionCompensationDirection }[] = [
  { label: '不补偿', value: 'none' },
  { label: '向右', value: 'right' },
  { label: '向左', value: 'left' },
  { label: '向下', value: 'down' },
  { label: '向上', value: 'up' },
]

const route = useRoute()

const monitorFormRef = ref()
const reviewFormRef = ref()

const videoRef = ref<HTMLVideoElement | null>(null)
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
const localInferenceState = ref<LocalInferenceState>('idle')
const initializationStage = ref<InferenceInitializationStage | null>(null)
const activeProvider = ref('-')
const activeModelVersion = ref('-')
const streamStatusText = ref('未开始实时检测')
const streamErrorText = ref('')
const latestFrameTime = ref('')
const latestFrameLatency = ref<number | null>(null)
const latestFramePayloadSize = ref(0)
const latestSavedFrameTime = ref('')
const latestFrameFile = ref<File | null>(null)
const frameSending = ref(false)
const streamMessageCount = ref(0)
const streamSkippedFrameCount = ref(0)
const renderMode = ref('overlay')
const motionCompensationEnabled = ref(true)
const motionCompensationDirection = ref<MotionCompensationDirection>('none')
const motionCompensationSpeed = ref(0)

const overlayBoxes = ref<QcDetectionBox[]>([])
const overlayJudge = ref<ResultJudge | undefined>()
const overlayStatusText = ref('点击“开始检测”后显示实时叠框')
const overlaySourceSize = ref({ width: 0, height: 0 })

const frameLoopAnimationId = ref<number | null>(null)
const realtimeFrameIndex = ref(0)
const latestBrowserInferenceResult = ref<BrowserInferenceResult | null>(null)
const streamRunToken = ref(0)
const evidenceQueueSnapshot = ref<EvidenceQueueSnapshot>({
  queued: 0,
  uploading: 0,
  succeeded: 0,
  failed: 0,
  dropped: 0,
})
const evidenceQueueError = ref('')
const recentRecordsRefreshTimerId = ref<number | null>(null)
let lastInferenceStartedAt = 0

const realtimeHitTracker = new RealtimeHitTracker()
let unsubscribeInitializationStage: (() => void) | null = null

const evidenceUploadQueue = new EvidenceUploadQueue<QcDetectionResult>({
  concurrency: 1,
  maxPending: 2,
  maxRetries: 2,
  retryDelayMs: 500,
  onStatus: (event) => {
    evidenceQueueSnapshot.value = event.snapshot
    if (event.status === 'failed') {
      evidenceQueueError.value = event.error instanceof Error ? event.error.message : '证据保存失败'
    } else if (event.status === 'succeeded') {
      evidenceQueueError.value = ''
    } else if (event.status === 'dropped') {
      evidenceQueueError.value = '证据队列已满，已跳过本次重复证据'
    }
  },
})

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
}

const reviewRules = {
  reviewResult: [{ required: true, message: '请选择复核结果' }],
}

const planStepOptions = computed(() =>
  planSteps.value.map((item) => ({
    label: `${formatPlanStepId(item.planStepId)} / ${item.stepName || item.stepId} / ${item.machineName || '未分配设备'}`,
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
const streamSnapshotSourcePreviewUrl = computed(() =>
  resolveImageUrl(streamResult.value?.sourceImageUrl || sourceImagePath.value) || frameSourcePreview.value,
)
const resultPreviewUrl = computed(() => resolveImageUrl(resultImagePath.value))
const streamResultPreviewUrl = computed(() => resolveImageUrl(streamResult.value?.imageUrl || resultImagePath.value))

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
const localInferenceStateLabel = computed(() =>
  initializationStage.value
    ? initializationStageLabelMap[initializationStage.value]
    : localInferenceStateLabelMap[localInferenceState.value],
)
const motionCompensationOffset = computed(() => {
  if (
    !motionCompensationEnabled.value ||
    motionCompensationDirection.value === 'none' ||
    !latestFrameLatency.value ||
    !motionCompensationSpeed.value
  ) {
    return { dx: 0, dy: 0, distance: 0 }
  }

  const distance = (motionCompensationSpeed.value * latestFrameLatency.value) / 1000
  if (motionCompensationDirection.value === 'right') {
    return { dx: distance, dy: 0, distance }
  }
  if (motionCompensationDirection.value === 'left') {
    return { dx: -distance, dy: 0, distance }
  }
  if (motionCompensationDirection.value === 'down') {
    return { dx: 0, dy: distance, distance }
  }
  return { dx: 0, dy: -distance, distance }
})
const motionCompensationSummary = computed(() => {
  if (!motionCompensationEnabled.value || motionCompensationDirection.value === 'none') {
    return '未启用'
  }
  if (!latestFrameLatency.value || !motionCompensationSpeed.value) {
    return '等待延迟或速度'
  }
  return `${Math.round(motionCompensationOffset.value.distance)} px`
})

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

const clampBoxCoordinate = (value: number, max: number) => Math.min(Math.max(value, 0), Math.max(max, 0))

const getCompensatedBoxes = (boxes: QcDetectionBox[], sourceWidth: number, sourceHeight: number) => {
  const { dx, dy } = motionCompensationOffset.value
  if (!dx && !dy) {
    return boxes
  }

  return boxes.map((box) => ({
    ...box,
    x1: clampBoxCoordinate((box.x1 ?? 0) + dx, sourceWidth),
    y1: clampBoxCoordinate((box.y1 ?? 0) + dy, sourceHeight),
    x2: clampBoxCoordinate((box.x2 ?? 0) + dx, sourceWidth),
    y2: clampBoxCoordinate((box.y2 ?? 0) + dy, sourceHeight),
  }))
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
  const renderBoxes = getCompensatedBoxes(boxes, sourceWidth, sourceHeight)

  context.lineWidth = 2
  context.font = '12px sans-serif'
  context.textBaseline = 'top'

  renderBoxes.forEach((box) => {
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

  const compensationText =
    motionCompensationOffset.value.distance > 0 ? `，补偿 ${Math.round(motionCompensationOffset.value.distance)} px` : ''
  overlayStatusText.value = `实时叠加 ${boxes.length} 个检测框${compensationText}`
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

const scheduleRecentRecordsRefresh = () => {
  if (recentRecordsRefreshTimerId.value !== null) {
    return
  }
  recentRecordsRefreshTimerId.value = window.setTimeout(() => {
    recentRecordsRefreshTimerId.value = null
    void reloadRecentRecords().catch(() => undefined)
  }, 1000)
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
  if (!['image/jpeg', 'image/png', 'image/bmp', 'image/gif'].includes(file.type)) {
    message.warning('仅支持 JPG、PNG、BMP 或 GIF 图片')
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
    const bitmap = await createImageBitmap(selectedFile.value)
    const browserResult = await browserInferenceClient.infer(bitmap)
    activeProvider.value = browserResult.providerStrategy
    activeModelVersion.value = browserResult.modelSha256.slice(0, 12)
    const renderBitmap = await createImageBitmap(selectedFile.value)
    let resultBlob: Blob
    try {
      resultBlob = await renderDetectionsToBlob(
        renderBitmap,
        browserResult.imageWidth,
        browserResult.imageHeight,
        browserResult.detections,
      )
    } finally {
      renderBitmap.close()
    }
    const resultFile = new File([resultBlob], `browser_result_${Date.now()}.jpg`, { type: 'image/jpeg' })
    const result = await saveClientDetectResult({
      sourceFile: selectedFile.value,
      resultFile,
      result: browserResult,
      planStepId: monitorForm.planStepId as IdValue,
      qcItemId: monitorForm.qcItemId as IdValue,
      inspector: monitorForm.inspector,
      remark: monitorForm.remark,
    })
    await acceptStoredResult(result)
    message.success(`浏览器质检完成（${browserResult.providerStrategy}，${browserResult.inferenceTimeMs}ms）`)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '浏览器图片质检失败')
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

const clearFrameLoop = () => {
  if (frameLoopAnimationId.value !== null) {
    window.cancelAnimationFrame(frameLoopAnimationId.value)
    frameLoopAnimationId.value = null
  }
  lastInferenceStartedAt = 0
}

const resetStreamVisualState = (statusText = '点击“开始检测”后显示实时叠框') => {
  streamResult.value = null
  latestFrameTime.value = ''
  latestFrameLatency.value = null
  latestFramePayloadSize.value = 0
  latestBrowserInferenceResult.value = null
  streamMessageCount.value = 0
  streamSkippedFrameCount.value = 0
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

type CapturedRealtimeFrame = {
  bitmap: ImageBitmap
}

const getRealtimeCaptureSize = (video: HTMLVideoElement) => {
  const maxEdge = Math.max(video.videoWidth, video.videoHeight)
  const scale = maxEdge > MAX_REALTIME_FRAME_EDGE ? MAX_REALTIME_FRAME_EDGE / maxEdge : 1
  return {
    width: Math.max(Math.round(video.videoWidth * scale), 1),
    height: Math.max(Math.round(video.videoHeight * scale), 1),
  }
}

const captureCurrentFrame = async (): Promise<CapturedRealtimeFrame> => {
  const video = videoRef.value
  if (!video || !video.videoWidth || !video.videoHeight) {
    throw new Error('摄像头画面未就绪')
  }

  const { width, height } = getRealtimeCaptureSize(video)
  const bitmap = await createImageBitmap(video, {
    resizeWidth: width,
    resizeHeight: height,
    resizeQuality: 'low',
  })
  return {
    bitmap,
  }
}

const captureEvidenceCanvas = () => {
  const video = videoRef.value
  const canvas = document.createElement('canvas')
  if (!video || !video.videoWidth || !video.videoHeight) {
    throw new Error('摄像头画面未就绪')
  }
  const { width, height } = getRealtimeCaptureSize(video)
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) {
    throw new Error('无法创建证据图画布')
  }
  context.clearRect(0, 0, width, height)
  context.drawImage(video, 0, 0, width, height)
  return canvas
}

const createEvidenceSourceFile = async (canvas: HTMLCanvasElement, prefix = 'stream') => {
  const blob = await renderFrameBlob(canvas, REALTIME_EVIDENCE_JPEG_QUALITY)
  return new File([blob], `${prefix}_${Date.now()}.jpg`, { type: 'image/jpeg' })
}

const disposeEvidenceCanvas = (canvas: HTMLCanvasElement) => {
  canvas.width = 1
  canvas.height = 1
  canvas.remove()
}

const applySavedStreamEvent = (
  sessionId: string,
  frameTime: string,
  frameFile: File,
  resultFile: File,
  savedResult: QcDetectionResult,
  runToken: number,
) => {
  if (runToken !== streamRunToken.value) {
    return
  }
  latestFrameFile.value = frameFile
  latestFramePayloadSize.value = frameFile.size + resultFile.size
  latestSavedFrameTime.value = frameTime
  detectionResult.value = savedResult
  if (savedResult.qcRecord) {
    activeRecord.value = savedResult.qcRecord
  }
  if (savedResult.inspectionDataList) {
    activeAttachments.value = savedResult.inspectionDataList
  }
  if (streamResult.value?.sessionId === sessionId) {
    streamResult.value = {
      ...streamResult.value,
      inspectionId: savedResult.inspectionId,
      imageUrl: savedResult.imageUrl,
      sourceImageUrl: savedResult.sourceImageUrl,
      autoSaved: true,
    }
  }
  streamStatusText.value = '缺陷证据已异步保存'
  scheduleRecentRecordsRefresh()
}

const enqueueEvidenceSave = (
  sessionId: string,
  frameTime: string,
  frameIndex: number,
  sourceCanvas: HTMLCanvasElement,
  result: BrowserInferenceResult,
  runToken: number,
) => {
  const eventId = `${sessionId}-${frameIndex}-${Date.now()}`
  return evidenceUploadQueue.enqueue({
    id: eventId,
    dispose: () => disposeEvidenceCanvas(sourceCanvas),
    run: async () => {
      const frameFile = await createEvidenceSourceFile(sourceCanvas)
      const renderBitmap = await createImageBitmap(sourceCanvas)
      let resultBlob: Blob
      try {
        resultBlob = await renderDetectionsToBlob(renderBitmap, result.imageWidth, result.imageHeight, result.detections)
      } finally {
        renderBitmap.close()
      }
      const resultFile = new File([resultBlob], `stream_result_${Date.now()}.jpg`, { type: 'image/jpeg' })
      const savedResult = await saveQcStreamClientEvent(sessionId, {
        eventId,
        frameIndex,
        frameTime,
        sourceFile: frameFile,
        resultFile,
        result,
      })
      applySavedStreamEvent(sessionId, frameTime, frameFile, resultFile, savedResult, runToken)
      return savedResult
    },
  })
}

const getRealtimeFrameIntervalMs = () => {
  const targetFps = browserInferenceClient.currentManifest?.targetInferenceFps
  const safeTargetFps = targetFps && targetFps > 0 ? targetFps : DEFAULT_TARGET_INFERENCE_FPS
  return Math.max(16, Math.round(1000 / safeTargetFps))
}

const startFrameLoop = () => {
  clearFrameLoop()
  lastInferenceStartedAt = performance.now() - getRealtimeFrameIntervalMs()

  const tick = (timestamp: number) => {
    if (!streamRunning.value) {
      return
    }
    const intervalMs = getRealtimeFrameIntervalMs()
    if (frameSending.value) {
      streamSkippedFrameCount.value += 1
    } else if (timestamp - lastInferenceStartedAt >= intervalMs) {
      lastInferenceStartedAt = timestamp
      void pushCurrentFrame()
    }
    frameLoopAnimationId.value = window.requestAnimationFrame(tick)
  }

  frameLoopAnimationId.value = window.requestAnimationFrame(tick)
}

const pushCurrentFrame = async () => {
  const sessionId = streamSession.value?.sessionId
  if (!streamRunning.value || !sessionId) {
    return
  }
  if (frameSending.value) {
    return
  }

  const runToken = streamRunToken.value
  frameSending.value = true
  try {
    const frame = await captureCurrentFrame()
    const frameTime = formatDateTime(new Date().toISOString())
    streamStatusText.value = '正在浏览器本地推理当前帧'

    const result = await browserInferenceClient.infer(frame.bitmap, {
      confidenceThreshold: browserInferenceClient.currentManifest?.realtimeConfidenceThreshold,
    })
    if (!streamRunning.value || runToken !== streamRunToken.value) {
      return
    }
    streamErrorText.value = ''
    activeProvider.value = result.providerStrategy
    activeModelVersion.value = result.modelSha256.slice(0, 12)
    latestBrowserInferenceResult.value = result
    realtimeFrameIndex.value += 1
    latestFrameLatency.value = result.preprocessTimeMs + result.inferenceTimeMs + result.postprocessTimeMs
    latestFrameTime.value = frameTime
    streamMessageCount.value += 1
    renderMode.value = 'browser'

    const messageData: QcStreamResultMessage = {
      sessionId,
      frameTime,
      resultJudge: result.resultJudge,
      confidenceScore: result.confidenceScore,
      resultValue: result.resultValue,
      defectType: result.defectType,
      boxes: result.detections,
      renderMode: 'browser',
      autoSaved: false,
    }
    streamResult.value = messageData
    overlaySourceSize.value = { width: result.imageWidth, height: result.imageHeight }
    drawBoxesOnOverlay(result.detections, result.resultJudge)

    const hitState = realtimeHitTracker.consume(result.detections.length > 0, browserInferenceClient.currentManifest?.continuousHitFrames ?? 3)
    if (hitState.shouldPersist) {
      let accepted = false
      try {
        const evidenceCanvas = captureEvidenceCanvas()
        accepted = enqueueEvidenceSave(sessionId, frameTime, realtimeFrameIndex.value, evidenceCanvas, result, runToken)
      } catch (error) {
        evidenceQueueError.value = error instanceof Error ? error.message : '证据图快照失败'
      }
      streamStatusText.value = accepted ? '连续缺陷成立，证据进入异步保存队列' : '连续缺陷成立，但证据队列暂时不可用'
    } else {
      streamStatusText.value = result.detections.length ? `检测到缺陷，连续 ${hitState.hitStreak} 帧` : '实时检测进行中'
    }
  } catch (error) {
    if (runToken === streamRunToken.value) {
      streamErrorText.value = error instanceof Error ? error.message : '浏览器本地推理失败'
      streamStatusText.value = '浏览器本地推理失败'
    }
  } finally {
    frameSending.value = false
  }
}

const ensureMonitorReady = async () => {
  await monitorFormRef.value?.validate()
  if (!monitorForm.planStepId || !monitorForm.qcItemId) {
    throw new Error('请先补全工序计划和质检项')
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
      cameraId: String(monitorForm.cameraId) === String(LOCAL_FALLBACK_CAMERA.cameraId)
        ? null
        : monitorForm.cameraId as IdValue,
      inspector: monitorForm.inspector.trim() || undefined,
      remark: monitorForm.remark.trim() || undefined,
    })
    streamSession.value = sessionResponse.data
    streamStatusText.value = '实时检测会话已创建，正在初始化浏览器推理'
    localInferenceState.value = 'initializing'
    const ready = await browserInferenceClient.ensureReady()
    activeProvider.value = ready.providerStrategy
    activeModelVersion.value = ready.modelSha256.slice(0, 12)
    localInferenceState.value = 'ready'
    streamRunToken.value += 1
    streamRunning.value = true
    realtimeFrameIndex.value = 0
    realtimeHitTracker.reset()
    startFrameLoop()
    message.success('浏览器本地实时质检已启动')
  } catch (error) {
    streamErrorText.value = error instanceof Error ? error.message : '实时视频流检测启动失败'
    streamStatusText.value = '实时检测启动失败'
    await stopRealtimeDetection({ silent: true, preserveStatusText: true })
    message.error(streamErrorText.value)
  } finally {
    streamPreparing.value = false
  }
}

const stopRealtimeDetection = async ({
  silent = false,
  preserveStatusText = false,
  disposeInference = false,
}: { silent?: boolean; preserveStatusText?: boolean; disposeInference?: boolean } = {}) => {
  if (streamClosing.value) {
    return
  }
  streamClosing.value = true

  const sessionId = streamSession.value?.sessionId
  streamRunToken.value += 1
  streamRunning.value = false
  clearFrameLoop()
  frameSending.value = false
  realtimeHitTracker.reset()
  evidenceUploadQueue.stopAccepting()
  await evidenceUploadQueue.drain(3_000)

  try {
    if (sessionId) {
      try {
        await closeQcStreamSession(sessionId)
      } catch (error) {
        if (!silent) {
          message.error('检测会话关闭失败，请稍后重试')
        }
      }
    }
    if (disposeInference) {
      try {
        await browserInferenceClient.dispose()
      } catch (error) {
        if (!silent) {
          message.error('浏览器推理资源释放失败，刷新页面后可重新初始化')
        }
      }
    }
  } finally {
    streamSession.value = null
    localInferenceState.value = 'closed'
    stopMediaStream()
    resetStreamVisualState(preserveStatusText ? streamStatusText.value : '点击“开始检测”后显示实时叠框')
    if (!preserveStatusText) {
      streamStatusText.value = '实时检测已停止'
      streamErrorText.value = ''
    }
    if (evidenceUploadQueue.isIdle) {
      evidenceUploadQueue.reset()
    } else {
      // A slow request may still be finishing after the bounded stop wait.
      // Allow the next session to enqueue work without resetting the active task.
      evidenceUploadQueue.resumeAccepting()
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
  let evidenceCanvas: HTMLCanvasElement | null = null
  try {
    evidenceCanvas = captureEvidenceCanvas()
    const frameBitmap = await createImageBitmap(evidenceCanvas)
    const result = await browserInferenceClient.infer(frameBitmap, {
      confidenceThreshold: browserInferenceClient.currentManifest?.realtimeConfidenceThreshold,
    })
    const frameFile = await createEvidenceSourceFile(evidenceCanvas, 'stream_manual_source')
    latestBrowserInferenceResult.value = result
    latestFrameFile.value = frameFile
    revokeObjectUrl('frame')
    frameSourcePreview.value = URL.createObjectURL(frameFile)

    const renderBitmap = await createImageBitmap(evidenceCanvas)
    const resultBlob = await renderDetectionsToBlob(renderBitmap, result.imageWidth, result.imageHeight, result.detections)
    renderBitmap.close()
    const resultFile = new File([resultBlob], `stream_manual_${Date.now()}.jpg`, { type: 'image/jpeg' })
    latestFramePayloadSize.value = frameFile.size + resultFile.size
    const snapshotResult = await saveQcStreamClientEvent(sessionId, {
      eventId: `${sessionId}-manual-${Date.now()}`,
      frameIndex: realtimeFrameIndex.value,
      frameTime: latestFrameTime.value || formatDateTime(new Date().toISOString()),
      sourceFile: frameFile,
      resultFile,
      result,
    })

    latestSavedFrameTime.value = latestFrameTime.value || formatDateTime(new Date().toISOString())
    await acceptStoredResult(snapshotResult)
    message.success('关键帧已保存为正式质检记录')
  } catch (error) {
    message.error('关键帧保存失败，请确认实时会话与证据保存接口')
  } finally {
    if (evidenceCanvas) {
      disposeEvidenceCanvas(evidenceCanvas)
    }
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
  evidenceQueueError.value = ''
  evidenceQueueSnapshot.value = evidenceUploadQueue.snapshot
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
  unsubscribeInitializationStage = browserInferenceClient.subscribeInitializationStage((stage) => {
    initializationStage.value = stage
  })
  window.addEventListener('resize', handleViewportResize)
  await loadBaseData()
  if (recentRecords.value[0]?.inspectionId) {
    await loadRecordDetail(recentRecords.value[0].inspectionId)
  }
})

onBeforeUnmount(() => {
  unsubscribeInitializationStage?.()
  window.removeEventListener('resize', handleViewportResize)
  if (recentRecordsRefreshTimerId.value !== null) {
    window.clearTimeout(recentRecordsRefreshTimerId.value)
    recentRecordsRefreshTimerId.value = null
  }
  void stopRealtimeDetection({ silent: true, disposeInference: true })
  revokeObjectUrl()
})
</script>

<template>
  <div class="realtime-page">
    <div class="mode-shell">
          <div class="workbench-grid">
            <a-card class="page-card realtime-capture-card" :bordered="false" title="实时视频流采集">
              <template #extra>
                <a-button class="settings-button" @click="settingsModalOpen = true">
                  <template #icon><SettingOutlined /></template>
                  设置
                </a-button>
              </template>
                <a-spin :spinning="loading || streamPreparing">
                  <div class="capture-workspace">
                    <div class="camera-section">
                      <div class="camera-preview">
                        <div class="video-frame">
                          <video ref="videoRef" muted playsinline @loadedmetadata="handleVideoReady" />
                          <canvas ref="overlayCanvasRef" class="overlay-canvas" />
                          <div v-if="cameraActive" class="overlay-status">
                            {{ overlayStatusText }}
                          </div>
                          <a-empty v-if="!cameraActive" description="点击“开始检测”后调用电脑摄像头并进入实时流检测" />
                        </div>
                        <div class="camera-actions">
                          <a-button :loading="cameraLoading" :disabled="cameraActive" @click="ensureCameraPreview">打开视频预览</a-button>
                          <a-button :disabled="!cameraActive && !streamRunning" @click="stopRealtimeDetection">停止检测</a-button>
                          <a-button v-permission="'quality:realtime:detect'" type="primary" :loading="streamPreparing" :disabled="streamRunning" @click="startRealtimeDetection">
                            开始检测
                          </a-button>
                          <a-button v-permission="'quality:realtime:detect'" type="primary" ghost :loading="snapshotSaving" :disabled="!streamRunning" @click="saveCurrentFrameSnapshot">
                            保存当前帧
                          </a-button>
                        </div>
                      </div>
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

                <div class="image-grid result-detail-grid">
                  <a-card size="small" title="关键帧留档" class="inner-card">
                    <div class="image-grid snapshot-image-grid">
                      <a-card size="small" title="关键帧原图">
                        <div class="image-frame">
                          <a-image v-if="streamSnapshotSourcePreviewUrl" :src="streamSnapshotSourcePreviewUrl" alt="关键帧原图" />
                          <a-empty v-else description="保存关键帧后显示原图" />
                        </div>
                      </a-card>
                      <a-card size="small" title="关键帧结果图">
                        <div class="image-frame">
                          <a-image v-if="streamResultPreviewUrl" :src="streamResultPreviewUrl" alt="关键帧结果图" />
                          <a-empty v-else description="后端生成结果图后显示" />
                        </div>
                      </a-card>
                    </div>
                    <div class="path-text">
                      {{ latestSavedFrameTime ? `最近保存时间：${latestSavedFrameTime}` : '检测到缺陷后会自动保存原图和结果图到 photo' }}
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
              </div>
            </a-card>

            <a-card class="page-card image-inspection-card" :bordered="false" title="单张图片质检">
              <div class="image-check-section">
                <div class="image-check-grid">
                  <div class="image-check-upload">
                    <input type="file" accept="image/jpeg,image/png,image/bmp,image/gif" @change="handleFileChange" />
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
                      图片质检复用设置中的工序计划、检测标准、检验人和备注，结果会在下方归档详情里展示。
                    </div>
                    <div class="camera-actions compact-actions">
                      <a-button @click="resetImageInspection">清空图片</a-button>
                      <a-button v-permission="'quality:realtime:detect'" type="primary" :loading="detecting" @click="handleOfflineDetect">上传并检测</a-button>
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
            </a-card>
          </div>
    </div>

    <a-card class="page-card" :bordered="false" title="最近质检记录">
      <div class="recent-list">
        <div v-for="item in recentRecords" :key="item.inspectionId" class="recent-item" @click="loadRecordDetail(item.inspectionId)">
          <div>
            <div class="recent-title">
              {{ formatInspectionId(item.inspectionId) }} / 工序计划 {{ formatPlanStepId(item.planStepId) }}
            </div>
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

      <div class="motion-settings">
        <div class="section-heading">移动补偿</div>
        <div class="motion-settings-grid">
          <a-form-item label="启用补偿">
            <a-switch v-model:checked="motionCompensationEnabled" @change="redrawOverlay" />
          </a-form-item>
          <a-form-item label="移动方向">
            <a-select
              v-model:value="motionCompensationDirection"
              :options="motionDirectionOptions"
              @change="redrawOverlay"
            />
          </a-form-item>
          <a-form-item label="像素速度">
            <a-input-number
              v-model:value="motionCompensationSpeed"
              :min="0"
              :max="3000"
              :step="10"
              addon-after="px/s"
              style="width: 100%"
              @change="redrawOverlay"
            />
          </a-form-item>
          <div class="motion-readout">
            <span>最近延迟</span>
            <strong>{{ latestFrameLatency !== null ? `${latestFrameLatency} ms` : '-' }}</strong>
          </div>
          <div class="motion-readout">
            <span>当前补偿</span>
            <strong>{{ motionCompensationSummary }}</strong>
          </div>
        </div>
        <div class="form-hint">按检测帧像素速度补偿：偏移距离 = 像素速度 × 最近延迟。</div>
      </div>

      <div class="settings-session">
        <div class="section-heading">实时会话</div>
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item label="计划ID">
            <a-tooltip v-if="selectedMonitorPlanStep?.planId" :title="String(selectedMonitorPlanStep.planId)">
              {{ formatPlanId(selectedMonitorPlanStep.planId) }}
            </a-tooltip>
            <template v-else>-</template>
          </a-descriptions-item>
          <a-descriptions-item label="工序计划ID">
            <a-tooltip v-if="selectedMonitorPlanStep?.planStepId" :title="String(selectedMonitorPlanStep.planStepId)">
              {{ formatPlanStepId(selectedMonitorPlanStep.planStepId) }}
            </a-tooltip>
            <template v-else>-</template>
          </a-descriptions-item>
          <a-descriptions-item label="工序">{{ selectedMonitorPlanStep?.stepName || '未选择' }}</a-descriptions-item>
          <a-descriptions-item label="当前摄像头">{{ selectedCamera?.cameraName || '未选择' }}</a-descriptions-item>
          <a-descriptions-item label="摄像头类型">{{ selectedCamera?.cameraType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="会话 ID">{{ streamSession?.sessionId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="本地推理状态">{{ localInferenceStateLabel }}</a-descriptions-item>
          <a-descriptions-item label="模型版本">{{ activeModelVersion }}</a-descriptions-item>
          <a-descriptions-item label="实际 Provider">{{ activeProvider }}</a-descriptions-item>
          <a-descriptions-item label="会话开始时间">{{ streamSession?.startedAt || '-' }}</a-descriptions-item>
          <a-descriptions-item label="最近帧时间">{{ latestFrameTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="本地推理间隔">{{ getRealtimeFrameIntervalMs() }}ms / 次（串行、无积压）</a-descriptions-item>
          <a-descriptions-item label="提交链路">浏览器 Worker 本地推理；连续缺陷后 POST /api/qc-stream-sessions/{sessionId}/client-events</a-descriptions-item>
        </a-descriptions>
        <a-card size="small" title="实时状态" class="settings-status-card">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="状态">{{ streamStatusText }}</a-descriptions-item>
            <a-descriptions-item label="错误信息">{{ streamErrorText || '-' }}</a-descriptions-item>
            <a-descriptions-item label="实时消息数">{{ streamMessageCount }}</a-descriptions-item>
            <a-descriptions-item label="调度跳过帧数">{{ streamSkippedFrameCount }}</a-descriptions-item>
            <a-descriptions-item label="证据队列">
              待处理 {{ evidenceQueueSnapshot.queued }} / 保存中 {{ evidenceQueueSnapshot.uploading }} / 失败 {{ evidenceQueueSnapshot.failed }} / 丢弃 {{ evidenceQueueSnapshot.dropped }}
            </a-descriptions-item>
            <a-descriptions-item label="证据状态">{{ evidenceQueueError || '实时推理不等待证据保存' }}</a-descriptions-item>
            <a-descriptions-item label="最近延迟">
              {{ latestFrameLatency !== null ? `${latestFrameLatency} ms` : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="预处理耗时">
              {{ latestBrowserInferenceResult ? `${latestBrowserInferenceResult.preprocessTimeMs} ms` : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="模型推理耗时">
              {{ latestBrowserInferenceResult ? `${latestBrowserInferenceResult.inferenceTimeMs} ms` : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="后处理耗时">
              {{ latestBrowserInferenceResult ? `${latestBrowserInferenceResult.postprocessTimeMs} ms` : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="最近证据图大小">
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

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(420px, 0.8fr);
  align-items: stretch;
  gap: 16px;
}

.realtime-capture-card {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.realtime-capture-card :deep(.ant-card-body) {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.realtime-capture-card :deep(.ant-spin-nested-loading),
.realtime-capture-card :deep(.ant-spin-container),
.capture-workspace,
.camera-section,
.camera-preview {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.realtime-capture-card .capture-workspace {
  justify-content: center;
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

.image-inspection-card {
  grid-column: 1 / -1;
}

.image-inspection-card .image-check-section {
  margin-top: 0;
  padding-top: 0;
  border-top: 0;
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

.realtime-capture-card .camera-section {
  display: flex;
  flex: 1;
  flex-direction: column;
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

.motion-settings {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(145, 158, 171, 0.14);
}

.motion-settings-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  align-items: end;
}

.motion-settings-grid :deep(.ant-form-item) {
  margin-bottom: 0;
}

.motion-readout {
  min-height: 56px;
  padding: 8px 12px;
  border: 1px solid rgba(145, 158, 171, 0.16);
  border-radius: 8px;
  background: #f8fafc;
}

.motion-readout span {
  display: block;
  margin-bottom: 6px;
  color: #667085;
  font-size: 12px;
}

.motion-readout strong {
  color: #172033;
  font-size: 15px;
}

.form-hint {
  margin-top: 10px;
  color: #667085;
  font-size: 12px;
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.image-grid > * {
  min-width: 0;
}

.result-detail-grid {
  grid-template-columns: minmax(0, 1fr);
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

@media (min-width: 1025px) {
  .realtime-page {
    gap: 14px;
  }

  .mode-shell,
  .page-card {
    border-radius: 11px;
  }

  .workbench-grid,
  .camera-section,
  .image-check-grid,
  .image-grid {
    gap: 14px;
  }

  .image-check-section {
    margin-top: 16px;
    padding-top: 14px;
  }

  .image-result-card,
  .inner-card {
    margin-top: 14px;
  }

  .compact-preview-box {
    min-height: 144px;
    margin-top: 9px;
    border-radius: 11px;
  }

  .image-frame,
  .video-frame {
    min-height: 234px;
    margin-top: 10px;
    border-radius: 11px;
  }

  .video-frame {
    height: clamp(378px, calc(100vh - 378px), 504px);
  }

  .camera-actions,
  .summary-actions,
  .submit-row {
    gap: 9px;
    margin-top: 12px;
  }

  .settings-session,
  .motion-settings {
    margin-top: 16px;
    padding-top: 14px;
  }

  .stream-overview {
    gap: 10px;
    margin-bottom: 14px;
  }

  .judge-block,
  .metric {
    min-height: 66px;
    padding: 12px;
    border-radius: 11px;
  }

  .judge-block {
    gap: 9px;
  }

  .metric span,
  .label {
    margin-bottom: 7px;
  }

  .metric strong {
    font-size: 16px;
  }

  .recent-list {
    gap: 9px;
  }

  .recent-item {
    gap: 10px;
    padding: 10px 12px;
    border-radius: 11px;
  }

  .recent-sub {
    font-size: 12px;
  }
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
  .motion-settings-grid,
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
