import request, { type RuoYiResponse } from '@/utils/request'

export interface CaptchaData {
  uuid: string
  image: string
  enabled: boolean
}

export interface LoginForm {
  username: string
  password: string
  captchaUuid?: string
  captchaCode?: string
}

export interface LoginToken {
  token: string
  expiresIn: number
}

export interface CurrentUser {
  userId: number
  userName: string
  nickName: string
  deptId: number
  postId: number
  roleId: number
}

export interface CurrentUserData {
  user: CurrentUser
  roleKey: string
  permissions: string[]
  menuKeys: string[]
}

export const getCaptcha = () => request<RuoYiResponse<CaptchaData>>({ url: '/auth/captcha', method: 'get', silentError: true })
export const login = (data: LoginForm) => request<RuoYiResponse<LoginToken>>({ url: '/auth/login', method: 'post', data })
export const getCurrentUser = () => request<RuoYiResponse<CurrentUserData>>({ url: '/auth/me', method: 'get', silentError: true })
export const changePassword = (data: { currentPassword: string; newPassword: string }) => request<RuoYiResponse>({ url: '/auth/password', method: 'put', data })
export const logout = () => request<RuoYiResponse>({ url: '/auth/logout', method: 'post', silentError: true })