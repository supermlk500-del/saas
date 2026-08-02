export type ExecutionProvider = 'webgpu' | 'wasm'
export type ProviderStrategy = 'WebGPU' | 'WASM'
export type InferenceInitializationStage =
  | 'DOWNLOADING'
  | 'VERIFYING_SHA256'
  | 'CREATING_SESSION'
  | 'WARMING_UP'
  | 'READY'

export interface BrowserModelClass {
  index: number
  code: string
  name: string
  color: string
}

export interface BrowserModelManifest {
  modelName: string
  modelVersion: string
  modelUrl: string
  sha256: string
  fileSize: number
  inputName: string
  outputName: string
  inputWidth: number
  inputHeight: number
  inputLayout: 'NCHW'
  inputColor: 'RGB'
  normalizationScale: number
  letterboxFill: number
  decoder: {
    type: 'yolo-raw' | 'nms-xyxy6'
    outputLayout: 'AUTO' | 'BCN' | 'BNC'
    boxFormat: 'cxcywh' | 'xyxy'
    hasObjectness: boolean
    coordinatesNormalized: boolean
  }
  classes: BrowserModelClass[]
  confidenceThreshold: number
  iouThreshold: number
  realtimeConfidenceThreshold: number
  targetInferenceFps: number
  continuousHitFrames: number
  evidenceImageCount: number
  preferredExecutionProviders: ExecutionProvider[]
}

export interface LetterboxTransform {
  originalWidth: number
  originalHeight: number
  modelWidth: number
  modelHeight: number
  scale: number
  padX: number
  padY: number
}

export interface BrowserDetection {
  classIndex: number
  code: string
  label: string
  color: string
  score: number
  x1: number
  y1: number
  x2: number
  y2: number
  width: number
  height: number
  area: number
}

export interface BrowserInferenceResult {
  imageWidth: number
  imageHeight: number
  detections: BrowserDetection[]
  resultJudge: 'PASS' | 'FAIL' | 'RECHECK'
  confidenceScore: number | null
  defectType: string | null
  resultValue: string
  preprocessTimeMs: number
  inferenceTimeMs: number
  postprocessTimeMs: number
  providerStrategy: ProviderStrategy
  modelSha256: string
}

export interface InferenceReady {
  modelSha256: string
  providerStrategy: ProviderStrategy
  initTimeMs: number
  warmupTimeMs: number
}

export type WorkerRequest =
  | {
      type: 'init'
      requestId: string
      manifest: BrowserModelManifest
      modelBytes: ArrayBuffer
      forceWasm?: boolean
    }
  | {
      type: 'infer'
      requestId: string
      bitmap: ImageBitmap
      confidenceThreshold: number
      iouThreshold: number
    }
  | {
      type: 'dispose'
      requestId: string
    }

export type WorkerResponse =
  | {
      type: 'progress'
      requestId: string
      payload: { stage: Exclude<InferenceInitializationStage, 'DOWNLOADING' | 'READY'> }
    }
  | { type: 'ready'; requestId: string; payload: InferenceReady }
  | { type: 'result'; requestId: string; payload: BrowserInferenceResult }
  | { type: 'disposed'; requestId: string }
  | { type: 'error'; requestId: string; message: string; code?: string }
