import request, { type RuoYiResponse } from '@/utils/request'

export interface SysMenuItem {
  menuId: number
  menuName: string
  parentId?: number
  orderNum?: number
  path?: string
  component?: string
  query?: string
  isFrame?: number
  isCache?: number
  menuType?: 'M' | 'C' | 'F'
  visible?: string
  status?: string
  perms?: string
  icon?: string
  createTime?: string
  children?: SysMenuItem[]
}

export interface SysMenuListParams {
  menuName?: string
  status?: string
}

export function listMenus(params?: SysMenuListParams) {
  return request<RuoYiResponse<SysMenuItem[]>>({
    url: '/system/menu/list',
    method: 'get',
    params,
  })
}

export interface SysMenuForm {
  menuId?: number
  menuName: string
  parentId: number
  orderNum: number
  path?: string
  component?: string
  query?: string
  isFrame?: number
  isCache?: number
  menuType: 'M' | 'C' | 'F'
  visible?: string
  status?: string
  perms?: string
  icon?: string
  remark?: string
}

export function addMenu(data: SysMenuForm) {
  return request<RuoYiResponse>({
    url: '/system/menu',
    method: 'post',
    data,
  })
}

export function updateMenu(data: SysMenuForm) {
  return request<RuoYiResponse>({
    url: '/system/menu',
    method: 'put',
    data,
  })
}

export function deleteMenu(menuId: number) {
  return request<RuoYiResponse>({
    url: `/system/menu/${menuId}`,
    method: 'delete',
  })
}
