import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import type { ApiErrorResponse } from '@/types/http'
import { readAccessToken, removeAccessToken } from '@/utils/authToken'

export type RequestConfig = AxiosRequestConfig & { silentError?: boolean }

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API ?? '/prod-api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' },
})

service.interceptors.request.use((config) => {
  const token = readAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  if (config.data instanceof FormData) delete config.headers?.['Content-Type']
  return config
})

service.interceptors.response.use(
  (response) => {
    const data = response.data
    const code = data?.code ?? 200
    if (code !== 200) {
      const errorMessage = data?.msg || '请求失败'
      const config = response.config as RequestConfig
      if (!config.silentError) message.error(errorMessage)
      return Promise.reject(new Error(errorMessage))
    }
    return data
  },
  (error: AxiosError) => {
    const status = error.response?.status
    const errorMessage = error.response?.data && typeof error.response.data === 'object' && 'msg' in error.response.data
      ? String((error.response.data as ApiErrorResponse).msg)
      : error.message || '网络请求失败'
    const config = error.config as RequestConfig | undefined
    if (status === 401) {
      removeAccessToken()
      if (location.pathname === '/login') {
        if (!config?.silentError) message.error(errorMessage)
      } else {
        const redirect = encodeURIComponent(location.pathname + location.search)
        location.replace(`/login?redirect=${redirect}`)
      }
    } else if (!config?.silentError) {
      message.error(errorMessage)
    }
    return Promise.reject(error)
  },
)

const request = <T = unknown>(config: RequestConfig): Promise<T> => service.request<unknown, T>(config)
export const download = <T = Blob>(config: RequestConfig) => service.request<unknown, T>({ responseType: 'blob', ...config })
export type { ApiListResponse, ApiSuccessResponse } from '@/types/http'
export type RuoYiResponse<T = unknown> = import('@/types/http').ApiSuccessResponse<T>
export type RuoYiListResponse<T = unknown> = import('@/types/http').ApiListResponse<T>
export default request
