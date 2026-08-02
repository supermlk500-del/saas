import type { BrowserModelManifest, LetterboxTransform } from '@/inference/types'

export interface PreprocessResult {
  input: Float32Array
  transform: LetterboxTransform
}

let canvas: OffscreenCanvas | null = null
let inputBuffer: Float32Array | null = null

export const preprocessImage = (bitmap: ImageBitmap, manifest: BrowserModelManifest): PreprocessResult => {
  const modelWidth = manifest.inputWidth
  const modelHeight = manifest.inputHeight
  canvas ??= new OffscreenCanvas(modelWidth, modelHeight)
  if (canvas.width !== modelWidth || canvas.height !== modelHeight) {
    canvas.width = modelWidth
    canvas.height = modelHeight
  }

  const context = canvas.getContext('2d', { willReadFrequently: true })
  if (!context) {
    throw new Error('OffscreenCanvas 2D context is not available')
  }

  const scale = Math.min(modelWidth / bitmap.width, modelHeight / bitmap.height)
  const resizedWidth = Math.round(bitmap.width * scale)
  const resizedHeight = Math.round(bitmap.height * scale)
  const padX = Math.floor((modelWidth - resizedWidth) / 2)
  const padY = Math.floor((modelHeight - resizedHeight) / 2)

  context.fillStyle = `rgb(${manifest.letterboxFill}, ${manifest.letterboxFill}, ${manifest.letterboxFill})`
  context.fillRect(0, 0, modelWidth, modelHeight)
  context.drawImage(bitmap, padX, padY, resizedWidth, resizedHeight)

  const imageData = context.getImageData(0, 0, modelWidth, modelHeight).data
  const planeSize = modelWidth * modelHeight
  const requiredLength = planeSize * 3
  if (!inputBuffer || inputBuffer.length !== requiredLength) {
    inputBuffer = new Float32Array(requiredLength)
  }
  const scaleValue = manifest.normalizationScale || 255
  for (let pixelIndex = 0; pixelIndex < planeSize; pixelIndex += 1) {
    const rgbaIndex = pixelIndex * 4
    inputBuffer[pixelIndex] = (imageData[rgbaIndex] ?? 0) / scaleValue
    inputBuffer[planeSize + pixelIndex] = (imageData[rgbaIndex + 1] ?? 0) / scaleValue
    inputBuffer[planeSize * 2 + pixelIndex] = (imageData[rgbaIndex + 2] ?? 0) / scaleValue
  }

  return {
    input: inputBuffer,
    transform: {
      originalWidth: bitmap.width,
      originalHeight: bitmap.height,
      modelWidth,
      modelHeight,
      scale,
      padX,
      padY,
    },
  }
}
