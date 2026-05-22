import request, { type RuoYiListResponse, type RuoYiResponse } from '@/utils/request'

export interface SysConfigItem {
  configId: number
  configName: string
  configKey: string
  configValue: string
  configType?: string
  createTime?: string
  remark?: string
}

export interface SysConfigListParams {
  pageNum?: number
  pageSize?: number
  configName?: string
  configKey?: string
  configType?: string
}

export function listConfigs(params: SysConfigListParams) {
  return request<RuoYiListResponse<SysConfigItem>>({
    url: '/system/config/list',
    method: 'get',
    params,
  })
}

export interface SysConfigForm {
  configId?: number
  configName: string
  configKey: string
  configValue: string
  configType?: string
  remark?: string
}

export function addConfig(data: SysConfigForm) {
  return request<RuoYiResponse>({
    url: '/system/config',
    method: 'post',
    data,
  })
}

export function updateConfig(data: SysConfigForm) {
  return request<RuoYiResponse>({
    url: '/system/config',
    method: 'put',
    data,
  })
}

export function deleteConfig(configId: number) {
  return request<RuoYiResponse>({
    url: `/system/config/${configId}`,
    method: 'delete',
  })
}
