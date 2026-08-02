import { describe, expect, it } from 'vitest'
import { decodeDetections } from '@/inference/postprocess'
import type { BrowserModelManifest, LetterboxTransform } from '@/inference/types'

const manifest: BrowserModelManifest = {
  modelName: 'best.onnx',
  modelVersion: 'test',
  modelUrl: '/model',
  sha256: 'a'.repeat(64),
  fileSize: 1,
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
  classes: [
    { index: 0, code: 'hole', name: 'hole', color: '#ef4444' },
    { index: 1, code: 'stain', name: 'stain', color: '#f97316' },
  ],
  confidenceThreshold: 0.5,
  iouThreshold: 0.45,
  realtimeConfidenceThreshold: 0.25,
  targetInferenceFps: 10,
  continuousHitFrames: 3,
  evidenceImageCount: 1,
  preferredExecutionProviders: ['webgpu', 'wasm'],
}

const identityTransform: LetterboxTransform = {
  originalWidth: 640,
  originalHeight: 640,
  modelWidth: 640,
  modelHeight: 640,
  scale: 1,
  padX: 0,
  padY: 0,
}

describe('decodeDetections', () => {
  it('decodes BCN raw YOLO output and applies class-aware NMS', () => {
    const data = new Float32Array([
      100, 102, 400,
      100, 102, 400,
      50, 50, 80,
      50, 50, 80,
      0.9, 0.8, 0.1,
      0.1, 0.2, 0.95,
    ])

    const detections = decodeDetections(data, [1, 6, 3], identityTransform, manifest, 0.5, 0.45)

    expect(detections).toHaveLength(2)
    expect(detections[0]?.label).toBe('stain')
    expect(detections[1]?.label).toBe('hole')
  })

  it('decodes BNC output with objectness confidence', () => {
    const objectnessManifest: BrowserModelManifest = {
      ...manifest,
      decoder: {
        ...manifest.decoder,
        outputLayout: 'BNC',
        hasObjectness: true,
      },
    }
    const data = new Float32Array([
      200, 220, 80, 40, 0.8, 0.9, 0.1,
      450, 450, 60, 60, 0.4, 0.9, 0.1,
    ])

    const detections = decodeDetections(data, [1, 2, 7], identityTransform, objectnessManifest, 0.5, 0.45)

    expect(detections).toHaveLength(1)
    expect(detections[0]).toMatchObject({
      label: 'hole',
      x1: 160,
      y1: 200,
      x2: 240,
      y2: 240,
    })
    expect(detections[0]?.score).toBeCloseTo(0.72, 5)
  })

  it('restores normalized coordinates through letterbox padding', () => {
    const normalizedManifest: BrowserModelManifest = {
      ...manifest,
      decoder: {
        ...manifest.decoder,
        outputLayout: 'BNC',
        coordinatesNormalized: true,
      },
    }
    const transform: LetterboxTransform = {
      originalWidth: 1280,
      originalHeight: 720,
      modelWidth: 640,
      modelHeight: 640,
      scale: 0.5,
      padX: 0,
      padY: 140,
    }
    const data = new Float32Array([
      0.5, 0.5, 0.5, 0.25, 0.9, 0.1,
    ])

    const detections = decodeDetections(data, [1, 1, 6], transform, normalizedManifest, 0.5, 0.45)

    expect(detections[0]).toMatchObject({
      x1: 320,
      y1: 200,
      x2: 960,
      y2: 520,
    })
  })

  it('decodes an already-NMS xyxy6 tensor', () => {
    const nmsManifest: BrowserModelManifest = {
      ...manifest,
      decoder: {
        type: 'nms-xyxy6',
        outputLayout: 'BNC',
        boxFormat: 'xyxy',
        hasObjectness: false,
        coordinatesNormalized: false,
      },
    }
    const data = new Float32Array([
      10, 20, 110, 220, 0.92, 1,
      300, 300, 400, 420, 0.2, 0,
    ])

    const detections = decodeDetections(data, [1, 2, 6], identityTransform, nmsManifest, 0.5, 0.45)

    expect(detections).toHaveLength(1)
    expect(detections[0]).toMatchObject({
      label: 'stain',
      x1: 10,
      y1: 20,
      x2: 110,
      y2: 220,
    })
    expect(detections[0]?.score).toBeCloseTo(0.92, 5)
  })

  it('does not suppress overlapping boxes from different classes', () => {
    const data = new Float32Array([
      100, 100,
      100, 100,
      80, 80,
      80, 80,
      0.9, 0.1,
      0.1, 0.95,
    ])

    const detections = decodeDetections(data, [1, 6, 2], identityTransform, manifest, 0.5, 0.45)

    expect(detections).toHaveLength(2)
  })

  it('rejects an output shape that contradicts the model contract', () => {
    expect(() =>
      decodeDetections(
        new Float32Array(12),
        [1, 3, 4],
        identityTransform,
        manifest,
        0.5,
        0.45,
      ),
    ).toThrow(/UNSUPPORTED_OUTPUT/)
  })
})
