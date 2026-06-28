import request, { type RuoYiListResponse, type RuoYiResponse } from '@/utils/request'
export type DataScope = 'ALL' | 'CUSTOM' | 'DEPT' | 'DEPT_AND_CHILD' | 'SELF'
export interface SysRoleItem { roleId: number; roleName: string; roleKey: string; roleSort: number; status?: string; dataScope: DataScope; createTime?: string; remark?: string }
export interface SysRoleListParams { pageNum?: number; pageSize?: number; roleName?: string; roleKey?: string; status?: string }
export interface SysRoleForm { roleId?: number; roleName: string; roleKey: string; roleSort: number; status?: string; dataScope: DataScope; remark?: string; menuIds: number[]; deptIds: number[] }
export interface SysRoleDetail { role: SysRoleItem; menuIds: number[]; deptIds: number[] }
export const listRoles = (params: SysRoleListParams) => request<RuoYiListResponse<SysRoleItem>>({ url: '/system/role/list', method: 'get', params })
export const getRole = (roleId: number) => request<RuoYiResponse<SysRoleDetail>>({ url: `/system/role/${roleId}`, method: 'get' })
export const addRole = (data: SysRoleForm) => request<RuoYiResponse<SysRoleItem>>({ url: '/system/role', method: 'post', data })
export const updateRole = (data: SysRoleForm) => request<RuoYiResponse<SysRoleItem>>({ url: '/system/role', method: 'put', data })
export const deleteRole = (roleId: number) => request<RuoYiResponse>({ url: `/system/role/${roleId}`, method: 'delete' })
export const changeRoleStatus = (roleId: number, status: string) => request<RuoYiResponse>({ url: '/system/role/changeStatus', method: 'put', data: { roleId, status } })