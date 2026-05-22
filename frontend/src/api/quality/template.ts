import request, { type RuoYiListResponse } from '@/utils/request'

export type TemplateItem = {
  key: string
  templateCode: string
  templateName: string
  productName: string
  version: string
  status: string
}

export type TemplateQuery = {
  keyword?: string
  status?: string
}

export function listTemplates(query?: TemplateQuery) {
  return request<RuoYiListResponse<TemplateItem>>({
    url: '/business/template/list',
    method: 'get',
    params: query,
  })
}

export const fetchTemplates = async (query?: TemplateQuery): Promise<TemplateItem[]> => {
  const res = await listTemplates(query)
  return res.rows ?? []
}
