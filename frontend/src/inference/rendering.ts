import type { BrowserDetection } from '@/inference/types'

export const renderDetectionsToBlob = async (
  source: CanvasImageSource,
  width: number,
  height: number,
  detections: BrowserDetection[],
) => {
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) {
    throw new Error('Canvas 2D context is not available')
  }
  context.drawImage(source, 0, 0, width, height)
  context.lineWidth = Math.max(2, Math.round(Math.min(width, height) / 320))
  context.font = `${Math.max(14, Math.round(width / 80))}px sans-serif`
  context.textBaseline = 'top'

  detections.forEach((box) => {
    const color = box.color || '#ef4444'
    context.strokeStyle = color
    context.fillStyle = `${color}33`
    context.strokeRect(box.x1, box.y1, box.width, box.height)
    context.fillRect(box.x1, box.y1, box.width, box.height)
    const label = `${box.label} ${box.score.toFixed(2)}`
    const textWidth = context.measureText(label).width
    const tagHeight = Math.max(22, Math.round(width / 70))
    context.fillStyle = color
    context.fillRect(box.x1, Math.max(0, box.y1 - tagHeight), textWidth + 12, tagHeight)
    context.fillStyle = '#fff'
    context.fillText(label, box.x1 + 6, Math.max(2, box.y1 - tagHeight + 4))
  })

  return new Promise<Blob>((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) resolve(blob)
      else reject(new Error('Failed to render detection result image'))
    }, 'image/jpeg', 0.9)
  })
}
