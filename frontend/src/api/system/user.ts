import request, { type RuoYiListResponse, type RuoYiResponse } from '@/utils/request'

export interface SysUserItem {
  userId: number; deptId: number; deptName?: string; postId: number; postName?: string
  roleId: number; roleName?: string; roleKey?: string; userName: string; nickName: string
  email?: string; phonenumber?: string; status?: string; loginIp?: string; loginDate?: string
  createTime?: string; remark?: string
}
export interface SysUserListParams { pageNum?: number; pageSize?: number; userName?: string; phonenumber?: string; status?: string; deptId?: number }
export interface SysUserForm {
  userId?: number; userName: string; nickName: string; password?: string; phonenumber?: string
  email?: string; status?: string; remark?: string; deptId: number; postId: number; roleId: number
}
export const listUsers = (params: SysUserListParams) => request<RuoYiListResponse<SysUserItem>>({ url: '/system/user/list', method: 'get', params })
export const getUser = (userId: number) => request<RuoYiResponse<SysUserItem>>({ url: `/system/user/${userId}`, method: 'get' })
export const addUser = (data: SysUserForm) => request<RuoYiResponse<SysUserItem>>({ url: '/system/user', method: 'post', data })
export const updateUser = (data: SysUserForm) => request<RuoYiResponse<SysUserItem>>({ url: '/system/user', method: 'put', data })
export const deleteUser = (userId: number) => request<RuoYiResponse>({ url: `/system/user/${userId}`, method: 'delete' })
export const changeUserStatus = (userId: number, status: string) => request<RuoYiResponse>({ url: '/system/user/changeStatus', method: 'put', data: { userId, status } })
export const resetUserPassword = (userId: number, password: string) => request<RuoYiResponse>({ url: '/system/user/resetPwd', method: 'put', data: { userId, password } })