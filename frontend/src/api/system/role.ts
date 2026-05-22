import request, { type RuoYiListResponse, type RuoYiResponse } from '@/utils/request'

export interface SysRoleItem {
  roleId: number
  roleName: string
  roleKey: string
  roleSort?: number
  status?: string
  dataScope?: string
  createTime?: string
  remark?: string
}

export interface SysRoleListParams {
  pageNum?: number
  pageSize?: number
  roleName?: string
  roleKey?: string
  status?: string
}

export function listRoles(params: SysRoleListParams) {
  return request<RuoYiListResponse<SysRoleItem>>({
    url: '/system/role/list',
    method: 'get',
    params,
  })
}

export interface SysRoleForm {
  roleId?: number
  roleName: string
  roleKey: string
  roleSort: number
  status?: string
  dataScope?: string
  remark?: string
}

export function addRole(data: SysRoleForm) {
  return request<RuoYiResponse>({
    url: '/system/role',
    method: 'post',
    data,
  })
}

export function updateRole(data: SysRoleForm) {
  return request<RuoYiResponse>({
    url: '/system/role',
    method: 'put',
    data,
  })
}

export function deleteRole(roleId: number) {
  return request<RuoYiResponse>({
    url: `/system/role/${roleId}`,
    method: 'delete',
  })
}

export function changeRoleStatus(roleId: number, status: string) {
  return request<RuoYiResponse>({
    url: '/system/role/changeStatus',
    method: 'put',
    data: {
      roleId,
      status,
    },
  })
}
