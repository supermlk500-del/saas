import request, { type ApiSuccessResponse } from '@/utils/request'
import type { BrowserModelManifest } from '@/inference/types'

export const fetchBrowserInferenceManifest = async () => {
  const response = await request<ApiSuccessResponse<BrowserModelManifest>>({
    url: '/api/ai/browser-inference/manifest',
    method: 'get',
    timeout: 30_000,
  })
  return response.data
}

export const downloadBrowserModel = (modelUrl: string) =>
  request<ArrayBuffer>({
    url: modelUrl,
    method: 'get',
    responseType: 'arraybuffer',
    timeout: 180_000,
    silentError: true,
  })
