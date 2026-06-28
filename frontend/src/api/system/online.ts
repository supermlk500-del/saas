import request, { type RuoYiResponse } from '@/utils/request'

export interface OnlineSession {
  sessionId: string
  userId: number
  username: string
  nickname: string
  roleKey: string
  ipAddress?: string
  userAgent?: string
  loginTime: string
  lastAccessTime: string
  expiresAtEpochMillis: number
}

export const listOnlineUsers = () => request<RuoYiResponse<OnlineSession[]>>({ url: '/system/online', method: 'get' })
export const forceLogout = (sessionId: string) => request<RuoYiResponse>({ url: `/system/online/${sessionId}`, method: 'delete' })