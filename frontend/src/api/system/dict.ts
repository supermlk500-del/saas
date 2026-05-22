import request, { type RuoYiListResponse, type RuoYiResponse } from '@/utils/request'

export interface SysDictTypeItem {
  dictId: number
  dictName: string
  dictType: string
  status?: string
  createTime?: string
  remark?: string
}

export interface SysDictTypeListParams {
  pageNum?: number
  pageSize?: number
  dictName?: string
  dictType?: string
  status?: string
}

export function listDictTypes(params: SysDictTypeListParams) {
  return request<RuoYiListResponse<SysDictTypeItem>>({
    url: '/system/dict/type/list',
    method: 'get',
    params,
  })
}

export interface SysDictTypeForm {
  dictId?: number
  dictName: string
  dictType: string
  status?: string
  remark?: string
}

export function addDictType(data: SysDictTypeForm) {
  return request<RuoYiResponse>({
    url: '/system/dict/type',
    method: 'post',
    data,
  })
}

export function updateDictType(data: SysDictTypeForm) {
  return request<RuoYiResponse>({
    url: '/system/dict/type',
    method: 'put',
    data,
  })
}

export function deleteDictType(dictId: number) {
  return request<RuoYiResponse>({
    url: `/system/dict/type/${dictId}`,
    method: 'delete',
  })
}

export function changeDictTypeStatus(dictId: number, status: string) {
  return request<RuoYiResponse>({
    url: '/system/dict/type/changeStatus',
    method: 'put',
    data: {
      dictId,
      status,
    },
  })
}
