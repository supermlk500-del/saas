/// <reference lib="webworker" />

import * as ort from 'onnxruntime-web/webgpu'
import { decodeDetections } from '@/inference/postprocess'
import { preprocessImage } from '@/inference/preprocess'
import type {
  BrowserInferenceResult,
  BrowserModelManifest,
  InferenceReady,
  ProviderStrategy,
  WorkerRequest,
  WorkerResponse,
} from '@/inference/types'

type NavigatorWithGpu = Navigator & {
  gpu?: {
    requestAdapter: () => Promise<unknown>
  }
}

let session: ort.InferenceSession | null = null
let manifest: BrowserModelManifest | null = null
let providerStrategy: ProviderStrategy = 'WASM'
let requestTail: Promise<void> = Promise.resolve()

ort.env.wasm.wasmPaths = '/ort/'
ort.env.wasm.proxy = false
// ORT already runs inside this dedicated Worker. A nested pthread pool can
// deadlock in Chromium headless and on some embedded WebViews, so WASM uses
// one stable worker thread; supported clients still prefer WebGPU.
ort.env.wasm.numThreads = 1

self.onmessage = (event: MessageEvent<WorkerRequest>) => {
  const request = event.data
  requestTail = requestTail.then(
    () => processRequestSafely(request),
    () => processRequestSafely(request),
  )
}

const post = (response: WorkerResponse) => {
  self.postMessage(response)
}

const processRequestSafely = async (request: WorkerRequest) => {
  try {
    if (request.type === 'init') {
      await handleInit(request.requestId, request.manifest, request.modelBytes, request.forceWasm === true)
      return
    }
    if (request.type === 'infer') {
      await handleInfer(request.requestId, request.bitmap, request.confidenceThreshold, request.iouThreshold)
      return
    }
    await handleDispose(request.requestId)
  } catch (error) {
    post({
      type: 'error',
      requestId: request.requestId,
      message: error instanceof Error ? error.message : String(error),
    })
  }
}

const handleInit = async (
  requestId: string,
  nextManifest: BrowserModelManifest,
  modelBytes: ArrayBuffer,
  forceWasm: boolean,
) => {
  const initStartedAt = performance.now()
  post({ type: 'progress', requestId, payload: { stage: 'VERIFYING_SHA256' } })
  const calculatedSha = await calculateSha256(modelBytes)
  if (calculatedSha !== nextManifest.sha256.toLowerCase()) {
    throw new Error(`MODEL_HASH_MISMATCH: expected ${nextManifest.sha256}, got ${calculatedSha}`)
  }

  await releaseSession()
  manifest = nextManifest

  const providers = await resolveExecutionProviders(forceWasm)
  post({ type: 'progress', requestId, payload: { stage: 'CREATING_SESSION' } })
  try {
    session = await ort.InferenceSession.create(modelBytes, {
      executionProviders: providers,
      graphOptimizationLevel: 'all',
    })
    providerStrategy = providers[0] === 'webgpu' ? 'WebGPU' : 'WASM'
  } catch (error) {
    if (!providers.includes('webgpu')) {
      throw error
    }
    session = await ort.InferenceSession.create(modelBytes, {
      executionProviders: ['wasm'],
      graphOptimizationLevel: 'all',
    })
    providerStrategy = 'WASM'
  }

  const warmupStartedAt = performance.now()
  post({ type: 'progress', requestId, payload: { stage: 'WARMING_UP' } })
  const input = new Float32Array(nextManifest.inputWidth * nextManifest.inputHeight * 3)
  const tensor = new ort.Tensor('float32', input, [1, 3, nextManifest.inputHeight, nextManifest.inputWidth])
  let warmupOutput: ort.InferenceSession.ReturnType | null = null
  try {
    warmupOutput = await session.run({ [nextManifest.inputName]: tensor })
  } finally {
    tensor.dispose()
    disposeOutputs(warmupOutput)
  }
  const warmupTimeMs = Math.round(performance.now() - warmupStartedAt)

  const payload: InferenceReady = {
    modelSha256: nextManifest.sha256,
    providerStrategy,
    initTimeMs: Math.round(performance.now() - initStartedAt),
    warmupTimeMs,
  }
  post({ type: 'ready', requestId, payload })
}

const handleInfer = async (
  requestId: string,
  bitmap: ImageBitmap,
  confidenceThreshold: number,
  iouThreshold: number,
) => {
  if (!session || !manifest) {
    bitmap.close()
    throw new Error('Inference session is not ready')
  }

  let tensor: ort.Tensor | null = null
  let output: ort.InferenceSession.ReturnType | null = null
  try {
    const preprocessStartedAt = performance.now()
    const preprocessResult = preprocessImage(bitmap, manifest)
    const preprocessTimeMs = Math.round(performance.now() - preprocessStartedAt)

    const inferenceStartedAt = performance.now()
    tensor = new ort.Tensor('float32', preprocessResult.input, [1, 3, manifest.inputHeight, manifest.inputWidth])
    output = await session.run({ [manifest.inputName]: tensor })
    const inferenceTimeMs = Math.round(performance.now() - inferenceStartedAt)

    const postprocessStartedAt = performance.now()
    const outputTensor = output[manifest.outputName] ?? Object.values(output)[0]
    if (!outputTensor || outputTensor.type !== 'float32') {
      throw new Error('UNSUPPORTED_OUTPUT: output tensor is missing or not float32')
    }
    const detections = decodeDetections(
      outputTensor.data as Float32Array,
      outputTensor.dims,
      preprocessResult.transform,
      manifest,
      confidenceThreshold,
      iouThreshold,
    )
    const postprocessTimeMs = Math.round(performance.now() - postprocessStartedAt)

    const top = detections[0]
    const resultJudge = !top ? 'PASS' : top.score >= confidenceThreshold ? 'FAIL' : 'RECHECK'
    const payload: BrowserInferenceResult = {
      imageWidth: preprocessResult.transform.originalWidth,
      imageHeight: preprocessResult.transform.originalHeight,
      detections,
      resultJudge,
      confidenceScore: top?.score ?? null,
      defectType: top?.label ?? null,
      resultValue: detections.length
        ? detections.slice(0, 3).map((item) => `${item.label}:${item.score.toFixed(2)}`).join(',')
        : 'PASS',
      preprocessTimeMs,
      inferenceTimeMs,
      postprocessTimeMs,
      providerStrategy,
      modelSha256: manifest.sha256,
    }
    post({ type: 'result', requestId, payload })
  } finally {
    bitmap.close()
    tensor?.dispose()
    disposeOutputs(output)
  }
}

const handleDispose = async (requestId: string) => {
  await releaseSession()
  manifest = null
  providerStrategy = 'WASM'
  post({ type: 'disposed', requestId })
}

const releaseSession = async () => {
  if (session) {
    await session.release()
    session = null
  }
}

const disposeOutputs = (output: ort.InferenceSession.ReturnType | null) => {
  if (!output) {
    return
  }
  Object.values(output).forEach((value) => value.dispose())
}

const resolveExecutionProviders = async (
  forceWasm: boolean,
): Promise<ort.InferenceSession.ExecutionProviderConfig[]> => {
  if (forceWasm) {
    return ['wasm']
  }
  const gpuNavigator = navigator as NavigatorWithGpu
  if (!gpuNavigator.gpu) {
    return ['wasm']
  }
  try {
    const adapter = await Promise.race([
      gpuNavigator.gpu.requestAdapter(),
      new Promise<null>((resolve) => setTimeout(() => resolve(null), 1500)),
    ])
    return adapter ? ['webgpu', 'wasm'] : ['wasm']
  } catch {
    return ['wasm']
  }
}

const calculateSha256 = async (bytes: ArrayBuffer) => {
  const digest = await crypto.subtle.digest('SHA-256', bytes.slice(0))
  return Array.from(new Uint8Array(digest))
    .map((item) => item.toString(16).padStart(2, '0'))
    .join('')
}
