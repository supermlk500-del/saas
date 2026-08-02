import { downloadBrowserModel, fetchBrowserInferenceManifest } from '@/api/quality/browserInference'
import type {
  BrowserInferenceResult,
  BrowserModelManifest,
  InferenceInitializationStage,
  InferenceReady,
  WorkerRequest,
  WorkerResponse,
} from '@/inference/types'

type PendingRequest<T> = {
  resolve: (value: T) => void
  reject: (reason?: unknown) => void
  timerId: ReturnType<typeof globalThis.setTimeout>
  requestType: WorkerRequest['type']
}

export interface BrowserInferenceClientOptions {
  workerFactory?: () => Worker
  initializationTimeoutMs?: number
  inferenceTimeoutMs?: number
  disposeTimeoutMs?: number
  forceWasm?: boolean | (() => boolean)
}

export class BrowserInferenceClient {
  private worker: Worker | null = null
  private manifest: BrowserModelManifest | null = null
  private ready: InferenceReady | null = null
  private pending = new Map<string, PendingRequest<unknown>>()
  private operationTail: Promise<void> = Promise.resolve()
  private initializationStage: InferenceInitializationStage | null = null
  private readonly stageListeners = new Set<(stage: InferenceInitializationStage | null) => void>()
  private readonly workerFactory: () => Worker
  private readonly initializationTimeoutMs: number
  private readonly inferenceTimeoutMs: number
  private readonly disposeTimeoutMs: number
  private readonly forceWasm: boolean | (() => boolean)

  constructor(options: BrowserInferenceClientOptions = {}) {
    this.workerFactory = options.workerFactory
      ?? (() => new Worker(new URL('./inference.worker.ts', import.meta.url), { type: 'module' }))
    this.initializationTimeoutMs = options.initializationTimeoutMs ?? 180_000
    this.inferenceTimeoutMs = options.inferenceTimeoutMs ?? 30_000
    this.disposeTimeoutMs = options.disposeTimeoutMs ?? 5_000
    this.forceWasm = options.forceWasm ?? false
  }

  get currentManifest() {
    return this.manifest
  }

  get currentReady() {
    return this.ready
  }

  get currentInitializationStage() {
    return this.initializationStage
  }

  subscribeInitializationStage(listener: (stage: InferenceInitializationStage | null) => void) {
    this.stageListeners.add(listener)
    listener(this.initializationStage)
    return () => this.stageListeners.delete(listener)
  }

  ensureReady(forceRefresh = false) {
    return this.enqueueOperation(() => this.ensureReadyInternal(forceRefresh, true))
  }

  infer(bitmap: ImageBitmap, options?: { confidenceThreshold?: number; iouThreshold?: number }) {
    return this.enqueueOperation(async () => {
      try {
        await this.ensureReadyInternal(false, false)
      } catch (error) {
        bitmap.close()
        throw error
      }
      if (!this.manifest) {
        bitmap.close()
        throw new Error('Browser inference manifest is not ready')
      }
      return this.postToWorker<BrowserInferenceResult>({
        type: 'infer',
        requestId: this.createRequestId(),
        bitmap,
        confidenceThreshold: options?.confidenceThreshold ?? this.manifest.confidenceThreshold,
        iouThreshold: options?.iouThreshold ?? this.manifest.iouThreshold,
      }, this.inferenceTimeoutMs, [bitmap])
    })
  }

  dispose() {
    return this.enqueueOperation(async () => {
      if (!this.worker) {
        this.resetState()
        return
      }
      try {
        await this.postToWorker<void>({
          type: 'dispose',
          requestId: this.createRequestId(),
        }, this.disposeTimeoutMs)
      } finally {
        this.terminateWorker()
      }
    })
  }

  private enqueueOperation<T>(operation: () => Promise<T>) {
    const result = this.operationTail.then(operation)
    this.operationTail = result.then(
      () => undefined,
      () => undefined,
    )
    return result
  }

  private async ensureReadyInternal(forceRefresh: boolean, refreshManifest: boolean) {
    if (!forceRefresh && !refreshManifest && this.ready && this.manifest) {
      return this.ready
    }

    const manifest = await fetchBrowserInferenceManifest()
    if (this.ready && this.manifest?.sha256 === manifest.sha256) {
      this.manifest = manifest
      this.emitInitializationStage('READY')
      return this.ready
    }
    this.emitInitializationStage('DOWNLOADING')
    const modelBytes = await downloadBrowserModel(manifest.modelUrl)
    this.ensureWorker()
    const ready = await this.postToWorker<InferenceReady>({
      type: 'init',
      requestId: this.createRequestId(),
      manifest,
      modelBytes,
      forceWasm: typeof this.forceWasm === 'function' ? this.forceWasm() : this.forceWasm,
    }, this.initializationTimeoutMs, [modelBytes])
    this.manifest = manifest
    this.ready = ready
    this.emitInitializationStage('READY')
    return ready
  }

  private ensureWorker() {
    if (this.worker) {
      return
    }
    this.worker = this.workerFactory()
    this.worker.onmessage = (event: MessageEvent<WorkerResponse>) => this.handleWorkerMessage(event.data)
    this.worker.onerror = (event) => {
      this.rejectAll(new Error(event.message || 'Browser inference worker error'))
      this.terminateWorker()
    }
    this.worker.onmessageerror = () => {
      this.rejectAll(new Error('Browser inference worker message error'))
      this.terminateWorker()
    }
  }

  private postToWorker<T>(request: WorkerRequest, timeoutMs: number, transfer?: Transferable[]) {
    this.ensureWorker()
    return new Promise<T>((resolve, reject) => {
      const timerId = globalThis.setTimeout(() => {
        this.pending.delete(request.requestId)
        this.terminateWorker()
        reject(new Error(`INFERENCE_TIMEOUT: ${request.type} exceeded ${timeoutMs}ms`))
      }, timeoutMs)
      this.pending.set(request.requestId, {
        resolve: resolve as (value: unknown) => void,
        reject,
        timerId,
        requestType: request.type,
      })
      try {
        this.worker?.postMessage(request, transfer ?? [])
      } catch (error) {
        globalThis.clearTimeout(timerId)
        this.pending.delete(request.requestId)
        this.terminateWorker()
        reject(error)
      }
    })
  }

  private handleWorkerMessage(message: WorkerResponse) {
    const pending = this.pending.get(message.requestId)
    if (!pending) {
      return
    }
    if (message.type === 'progress') {
      this.emitInitializationStage(message.payload.stage)
      return
    }
    globalThis.clearTimeout(pending.timerId)
    this.pending.delete(message.requestId)
    if (message.type === 'error') {
      const error = new Error(message.code ? `${message.code}: ${message.message}` : message.message)
      pending.reject(error)
      this.terminateWorker()
      return
    }
    if (message.type === 'disposed') {
      pending.resolve(undefined)
      return
    }
    pending.resolve(message.payload)
  }

  private terminateWorker() {
    if (this.worker) {
      this.worker.terminate()
      this.worker = null
    }
    this.rejectAll(new Error('Browser inference worker reset'))
    this.resetState()
  }

  private resetState() {
    this.manifest = null
    this.ready = null
    this.emitInitializationStage(null)
  }

  private emitInitializationStage(stage: InferenceInitializationStage | null) {
    this.initializationStage = stage
    if (import.meta.env.DEV) {
      console.debug(`[BrowserInference] ${stage ?? 'RESET'}`)
    }
    this.stageListeners.forEach((listener) => listener(stage))
  }

  private rejectAll(error: Error) {
    this.pending.forEach((pending) => {
      globalThis.clearTimeout(pending.timerId)
      pending.reject(error)
    })
    this.pending.clear()
  }

  private createRequestId() {
    return `${Date.now()}-${Math.random().toString(16).slice(2)}`
  }
}

export const browserInferenceClient = new BrowserInferenceClient({
  // Compatibility diagnostic switch. It is intentionally read at init time so
  // operators can verify the real WASM fallback without rebuilding the app.
  forceWasm: () => globalThis.localStorage?.getItem('zhihuitong.browserInference.forceWasm') === 'true',
})
