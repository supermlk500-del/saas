import type { BrowserDetection, BrowserModelClass, BrowserModelManifest, LetterboxTransform } from '@/inference/types'

type Candidate = BrowserDetection

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)

const isFiniteBox = (box: Candidate) =>
  Number.isFinite(box.x1) &&
  Number.isFinite(box.y1) &&
  Number.isFinite(box.x2) &&
  Number.isFinite(box.y2) &&
  box.x2 > box.x1 &&
  box.y2 > box.y1

const toOriginalBox = (
  values: { x1: number; y1: number; x2: number; y2: number },
  transform: LetterboxTransform,
) => {
  const x1 = clamp((values.x1 - transform.padX) / transform.scale, 0, transform.originalWidth)
  const y1 = clamp((values.y1 - transform.padY) / transform.scale, 0, transform.originalHeight)
  const x2 = clamp((values.x2 - transform.padX) / transform.scale, 0, transform.originalWidth)
  const y2 = clamp((values.y2 - transform.padY) / transform.scale, 0, transform.originalHeight)
  return { x1, y1, x2, y2 }
}

const resolveClass = (classes: BrowserModelClass[], classIndex: number) =>
  classes.find((item) => item.index === classIndex)

const createDetection = (
  classMeta: BrowserModelClass,
  score: number,
  box: { x1: number; y1: number; x2: number; y2: number },
): BrowserDetection | null => {
  const detection = {
    classIndex: classMeta.index,
    code: classMeta.code,
    label: classMeta.name,
    color: classMeta.color,
    score,
    x1: Math.round(box.x1),
    y1: Math.round(box.y1),
    x2: Math.round(box.x2),
    y2: Math.round(box.y2),
    width: Math.max(0, Math.round(box.x2 - box.x1)),
    height: Math.max(0, Math.round(box.y2 - box.y1)),
    area: Math.max(0, Math.round((box.x2 - box.x1) * (box.y2 - box.y1))),
  }
  return isFiniteBox(detection) ? detection : null
}

const iou = (a: Candidate, b: Candidate) => {
  const left = Math.max(a.x1, b.x1)
  const top = Math.max(a.y1, b.y1)
  const right = Math.min(a.x2, b.x2)
  const bottom = Math.min(a.y2, b.y2)
  const intersection = Math.max(0, right - left) * Math.max(0, bottom - top)
  const union = a.area + b.area - intersection
  return union <= 0 ? 0 : intersection / union
}

export const nonMaximumSuppression = (candidates: Candidate[], iouThreshold: number) => {
  const sorted = [...candidates].sort((a, b) => b.score - a.score)
  const result: Candidate[] = []
  const removed = new Array(sorted.length).fill(false)
  for (let i = 0; i < sorted.length; i += 1) {
    if (removed[i]) continue
    const current = sorted[i]
    if (!current) continue
    result.push(current)
    for (let j = i + 1; j < sorted.length; j += 1) {
      if (removed[j]) continue
      const next = sorted[j]
      if (!next) continue
      if (current.classIndex === next.classIndex && iou(current, next) >= iouThreshold) {
        removed[j] = true
      }
    }
  }
  return result
}

const asMatrix = (
  data: Float32Array,
  dims: readonly number[],
  configuredLayout: BrowserModelManifest['decoder']['outputLayout'],
  expectedFeatureCounts: readonly number[],
) => {
  if (dims.length !== 3 || dims[0] !== 1) {
    throw new Error(`UNSUPPORTED_OUTPUT: expected a rank-3 tensor, got [${dims.join(',')}]`)
  }
  const first = dims[1]
  const second = dims[2]
  if (!first || !second || first < 1 || second < 1 || data.length !== first * second) {
    throw new Error(`UNSUPPORTED_OUTPUT: invalid tensor shape [${dims.join(',')}]`)
  }
  const firstMatches = expectedFeatureCounts.includes(first)
  const secondMatches = expectedFeatureCounts.includes(second)
  const layout = configuredLayout === 'AUTO'
    ? firstMatches && !secondMatches
      ? 'BCN'
      : secondMatches && !firstMatches
        ? 'BNC'
        : null
    : configuredLayout
  if (!layout) {
    throw new Error(`UNSUPPORTED_OUTPUT: cannot resolve layout for [${dims.join(',')}]`)
  }
  const candidateCount = layout === 'BCN' ? second : first
  const featureCount = layout === 'BCN' ? first : second
  if (!expectedFeatureCounts.includes(featureCount)) {
    throw new Error(
      `UNSUPPORTED_OUTPUT: feature count ${featureCount} does not match ${expectedFeatureCounts.join(' or ')}`,
    )
  }
  const get = (candidateIndex: number, featureIndex: number) =>
    layout === 'BCN'
      ? data[featureIndex * candidateCount + candidateIndex] ?? 0
      : data[candidateIndex * featureCount + featureIndex] ?? 0
  return { candidateCount, featureCount, get }
}

const denormalizeBox = (
  box: { x1: number; y1: number; x2: number; y2: number },
  transform: LetterboxTransform,
) => {
  return {
    x1: box.x1 * transform.modelWidth,
    y1: box.y1 * transform.modelHeight,
    x2: box.x2 * transform.modelWidth,
    y2: box.y2 * transform.modelHeight,
  }
}

const decodeRawYolo = (
  data: Float32Array,
  dims: readonly number[],
  transform: LetterboxTransform,
  manifest: BrowserModelManifest,
  confidenceThreshold: number,
) => {
  const expectedFeatureCount = manifest.classes.length + (manifest.decoder.hasObjectness ? 5 : 4)
  const { candidateCount, featureCount, get } = asMatrix(
    data,
    dims,
    manifest.decoder.outputLayout,
    [expectedFeatureCount],
  )
  const hasObjectness = manifest.decoder.hasObjectness
  const classStart = hasObjectness ? 5 : 4
  const classCount = featureCount - classStart
  if (classCount !== manifest.classes.length) {
    throw new Error(
      `UNSUPPORTED_OUTPUT: model has ${classCount} class scores but manifest defines ${manifest.classes.length}`,
    )
  }

  const candidates: Candidate[] = []
  for (let candidateIndex = 0; candidateIndex < candidateCount; candidateIndex += 1) {
    const objectness = hasObjectness ? get(candidateIndex, 4) : 1
    let bestClassIndex = -1
    let bestClassScore = 0
    for (let classOffset = 0; classOffset < classCount; classOffset += 1) {
      const classScore = get(candidateIndex, classStart + classOffset)
      if (classScore > bestClassScore) {
        bestClassScore = classScore
        bestClassIndex = classOffset
      }
    }
    const score = objectness * bestClassScore
    if (bestClassIndex < 0 || score < confidenceThreshold) {
      continue
    }
    const cx = get(candidateIndex, 0)
    const cy = get(candidateIndex, 1)
    const width = get(candidateIndex, 2)
    const height = get(candidateIndex, 3)
    const modelBox = manifest.decoder.boxFormat === 'xyxy'
      ? { x1: cx, y1: cy, x2: width, y2: height }
      : { x1: cx - width / 2, y1: cy - height / 2, x2: cx + width / 2, y2: cy + height / 2 }
    const classMeta = resolveClass(manifest.classes, bestClassIndex)
    if (!classMeta) {
      throw new Error(`UNSUPPORTED_OUTPUT: class index ${bestClassIndex} is absent from the manifest`)
    }
    const sourceBox = manifest.decoder.coordinatesNormalized
      ? denormalizeBox(modelBox, transform)
      : modelBox
    const detection = createDetection(classMeta, score, toOriginalBox(sourceBox, transform))
    if (detection) {
      candidates.push(detection)
    }
  }
  return candidates
}

const decodeNmsXyxy6 = (
  data: Float32Array,
  dims: readonly number[],
  transform: LetterboxTransform,
  manifest: BrowserModelManifest,
  confidenceThreshold: number,
) => {
  const { candidateCount, get } = asMatrix(data, dims, manifest.decoder.outputLayout, [6])
  const candidates: Candidate[] = []
  for (let candidateIndex = 0; candidateIndex < candidateCount; candidateIndex += 1) {
    const score = get(candidateIndex, 4)
    if (!Number.isFinite(score) || score < confidenceThreshold || score > 1) {
      continue
    }
    const rawClassIndex = get(candidateIndex, 5)
    const classIndex = Math.round(rawClassIndex)
    if (!Number.isFinite(rawClassIndex) || Math.abs(rawClassIndex - classIndex) > 0.001) {
      throw new Error(`UNSUPPORTED_OUTPUT: invalid class index ${rawClassIndex}`)
    }
    const classMeta = resolveClass(manifest.classes, classIndex)
    if (!classMeta) {
      throw new Error(`UNSUPPORTED_OUTPUT: class index ${classIndex} is absent from the manifest`)
    }
    const modelBox = {
      x1: get(candidateIndex, 0),
      y1: get(candidateIndex, 1),
      x2: get(candidateIndex, 2),
      y2: get(candidateIndex, 3),
    }
    const sourceBox = manifest.decoder.coordinatesNormalized
      ? denormalizeBox(modelBox, transform)
      : modelBox
    const detection = createDetection(classMeta, score, toOriginalBox(sourceBox, transform))
    if (detection) {
      candidates.push(detection)
    }
  }
  return candidates
}

export const decodeDetections = (
  data: Float32Array,
  dims: readonly number[],
  transform: LetterboxTransform,
  manifest: BrowserModelManifest,
  confidenceThreshold: number,
  iouThreshold: number,
) => {
  const candidates = manifest.decoder.type === 'nms-xyxy6'
    ? decodeNmsXyxy6(data, dims, transform, manifest, confidenceThreshold)
    : decodeRawYolo(data, dims, transform, manifest, confidenceThreshold)
  return nonMaximumSuppression(candidates, iouThreshold)
}
