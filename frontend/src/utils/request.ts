import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'

export interface RuoYiResponse<T = unknown> {
  code: number
  msg: string
  data?: T
}

export interface RuoYiListResponse<T = unknown> {
  code: number
  msg: string
  rows?: T[]
  total?: number
}

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API ?? '/prod-api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8',
  },
})

service.interceptors.response.use(
  (response) => {
    const data = response.data
    const code = data?.code ?? 200

    if (code !== 200) {
      const errorMessage = data?.msg || 'Request failed'
      message.error(errorMessage)
      return Promise.reject(new Error(errorMessage))
    }

    return data
  },
  (error: AxiosError) => {
    const errorMessage =
      error.response?.data && typeof error.response.data === 'object' && 'msg' in error.response.data
        ? String(error.response.data.msg)
        : error.message || 'Network request error'

    message.error(errorMessage)
    return Promise.reject(error)
  },
)

const request = <T = unknown>(config: AxiosRequestConfig): Promise<T> => {
  return service.request<unknown, T>(config)
}

export const download = <T = Blob>(config: AxiosRequestConfig) =>
  service.request<unknown, T>({
    responseType: 'blob',
    ...config,
  })

export default request
