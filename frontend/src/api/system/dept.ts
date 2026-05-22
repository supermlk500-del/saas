import request, { type RuoYiResponse } from '@/utils/request'

export interface SysDeptItem {
  deptId: number
  parentId?: number
  ancestors?: string
  deptName: string
  orderNum?: number
  leader?: string
  phone?: string
  email?: string
  status?: string
  delFlag?: string
  parentName?: string
  createTime?: string
  children?: SysDeptItem[]
}

export interface SysDeptListParams {
  deptName?: string
  status?: string
}

export function listDepts(params?: SysDeptListParams) {
  return request<RuoYiResponse<SysDeptItem[]>>({
    url: '/system/dept/list',
    method: 'get',
    params,
  })
}

export interface SysDeptForm {
  deptId?: number
  parentId: number
  deptName: string
  orderNum: number
  leader?: string
  phone?: string
  email?: string
  status?: string
}

export function addDept(data: SysDeptForm) {
  return request<RuoYiResponse>({
    url: '/system/dept',
    method: 'post',
    data,
  })
}

export function updateDept(data: SysDeptForm) {
  return request<RuoYiResponse>({
    url: '/system/dept',
    method: 'put',
    data,
  })
}

export function deleteDept(deptId: number) {
  return request<RuoYiResponse>({
    url: `/system/dept/${deptId}`,
    method: 'delete',
  })
}
