import request, { type RuoYiListResponse, type RuoYiResponse } from '@/utils/request'

export interface SysUserItem {
  userId: number
  userName: string
  nickName?: string
  email?: string
  phonenumber?: string
  status?: string
  createTime?: string
  dept?: {
    deptName?: string
  }
}

export interface SysUserListParams {
  pageNum?: number
  pageSize?: number
  userName?: string
  phonenumber?: string
  status?: string
}

export function listUsers(params: SysUserListParams) {
  return request<RuoYiListResponse<SysUserItem>>({
    url: '/system/user/list',
    method: 'get',
    params,
  })
}

export interface SysUserForm {
  userId?: number
  userName: string
  nickName: string
  password?: string
  phonenumber?: string
  email?: string
  status?: string
  remark?: string
}

export function addUser(data: SysUserForm) {
  return request<RuoYiResponse>({
    url: '/system/user',
    method: 'post',
    data,
  })
}

export function updateUser(data: SysUserForm) {
  return request<RuoYiResponse>({
    url: '/system/user',
    method: 'put',
    data,
  })
}

export function deleteUser(userId: number) {
  return request<RuoYiResponse>({
    url: `/system/user/${userId}`,
    method: 'delete',
  })
}

export function changeUserStatus(userId: number, status: string) {
  return request<RuoYiResponse>({
    url: '/system/user/changeStatus',
    method: 'put',
    data: {
      userId,
      status,
    },
  })
}

export function resetUserPassword(userId: number, password: string) {
  return request<RuoYiResponse>({
    url: '/system/user/resetPwd',
    method: 'put',
    data: {
      userId,
      password,
    },
  })
}
