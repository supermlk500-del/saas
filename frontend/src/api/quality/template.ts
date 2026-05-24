import { fetchQcItems, type QcItemQuery } from '@/api/quality/qcItem'
import type { QcItem } from '@/types/domain'
import type { PageResult } from '@/types/http'

export type TemplateItem = QcItem
export type TemplateQuery = QcItemQuery

export const fetchTemplates = async (query?: TemplateQuery): Promise<PageResult<TemplateItem>> => {
  return fetchQcItems(query)
}
