import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  downloadBrowserModel,
  fetchBrowserInferenceManifest,
} from '@/api/quality/browserInference'
import { BrowserInferenceClient } from '@/inference/browserInferenceClient'
import type {
  BrowserInferenceResult,
  BrowserModelManifest,
  WorkerRequest,
  WorkerResponse,
} from '@/inference/types'

vi.mock('@/api/quality/browserInference', () => ({
  fetchBrowserInferenceManifest: vi.fn(),
  downloadBrowserModel: vi.fn(),
}))

const manifest: BrowserModelManifest = {
  modelName: 'best.onnx',
  modelVersion: 'test',
  modelUrl: '/api/ai/browser-inference/model?sha256=' + 'a'.repeat(64),
  sha256: 'a'.repeat(64),
  fileSize: 8,
  inputName: 'images',
  outputName: 'output0',
  inputWidth: 640,
  inputHeight: 640,
  inputLayout: 'NCHW',
  inputColor: 'RGB',
  normalizationScale: 255,
  letterboxFill: 114,
  decoder: {
    type: 'yolo-raw',
    outputLayout: 'BCN',
    boxFormat: 'cxcywh',
    hasObjectness: false,
    coordinatesNormalized: false,
  },
  classes: [{ index: 0, code: 'hole', name: 'hole', color: '#ef4444' }],
  confidenceThreshold: 0.5,
  iouThreshold: 0.45,
  realtimeConfidenceThreshold: 0.25,
  targetInferenceFps: 10,
  continuousHitFrames: 3,
  evidenceImageCount: 1,
  preferredExecutionProviders: ['webgpu', 'wasm'],
}

const inferenceResult: BrowserInferenceResult = {
  imageWidth: 640,
  imageHeight: 640,
  detections: [],
  resultJudge: 'PASS',
  confidenceScore: null,
  defectType: null,
  resultValue: 'PASS',
  preprocessTimeMs: 1,
  inferenceTimeMs: 2,
  postprocessTimeMs: 1,
  providerStrategy: 'WASM',
  modelSha256: manifest.sha256,
}

class FakeWorker {
  onmessage: ((event: MessageEvent<WorkerResponse>) => void) | null = null
  onerror: ((event: ErrorEvent) => void) | null = null
  onmessageerror: ((event: MessageEvent) => void) | null = null
  initCount = 0
  activeInferenceCount = 0
  maxActiveInferenceCount = 0
  terminated = false
  respondToInference = true

  postMessage(request: WorkerRequest) {
    if (request.type === 'init') {
      this.initCount += 1
      queueMicrotask(() => {
        this.emit({
          type: 'ready',
          requestId: request.requestId,
          payload: {
            modelSha256: request.manifest.sha256,
            providerStrategy: 'WASM',
            initTimeMs: 5,
            warmupTimeMs: 1,
          },
        })
      })
      return
    }
    if (request.type === 'dispose') {
      queueMicrotask(() => this.emit({ type: 'disposed', requestId: request.requestId }))
      return
    }
    this.activeInferenceCount += 1
    this.maxActiveInferenceCount = Math.max(this.maxActiveInferenceCount, this.activeInferenceCount)
    if (!this.respondToInference) {
      return
    }
    globalThis.setTimeout(() => {
      this.activeInferenceCount -= 1
      this.emit({ type: 'result', requestId: request.requestId, payload: inferenceResult })
    }, 5)
  }

  terminate() {
    this.terminated = true
  }

  private emit(message: WorkerResponse) {
    if (!this.terminated) {
      this.onmessage?.({ data: message } as MessageEvent<WorkerResponse>)
    }
  }
}

const bitmap = () => ({ close: vi.fn() }) as unknown as ImageBitmap

describe('BrowserInferenceClient', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(fetchBrowserInferenceManifest).mockResolvedValue(structuredClone(manifest))
    vi.mocked(downloadBrowserModel).mockResolvedValue(new ArrayBuffer(8))
  })

  it('serializes concurrent infer calls and avoids a manifest request per frame', async () => {
    const worker = new FakeWorker()
    const client = new BrowserInferenceClient({
      workerFactory: () => worker as unknown as Worker,
    })

    await Promise.all([client.infer(bitmap()), client.infer(bitmap())])

    expect(worker.maxActiveInferenceCount).toBe(1)
    expect(fetchBrowserInferenceManifest).toHaveBeenCalledTimes(1)
    expect(downloadBrowserModel).toHaveBeenCalledTimes(1)
  })

  it('refreshes metadata without recreating a session for the same SHA', async () => {
    const worker = new FakeWorker()
    const client = new BrowserInferenceClient({
      workerFactory: () => worker as unknown as Worker,
    })

    await client.ensureReady()
    await client.ensureReady(true)

    expect(fetchBrowserInferenceManifest).toHaveBeenCalledTimes(2)
    expect(downloadBrowserModel).toHaveBeenCalledTimes(1)
    expect(worker.initCount).toBe(1)
  })

  it('terminates and resets the worker after inference timeout', async () => {
    const workers: FakeWorker[] = []
    const client = new BrowserInferenceClient({
      workerFactory: () => {
        const worker = new FakeWorker()
        worker.respondToInference = workers.length > 0
        workers.push(worker)
        return worker as unknown as Worker
      },
      inferenceTimeoutMs: 10,
    })

    await expect(client.infer(bitmap())).rejects.toThrow(/INFERENCE_TIMEOUT/)

    expect(workers[0]?.terminated).toBe(true)
    expect(client.currentReady).toBeNull()

    await client.infer(bitmap())
    expect(workers).toHaveLength(2)
  })
})
