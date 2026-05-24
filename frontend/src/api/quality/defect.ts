import { fetchQcItems, type QcItemQuery } from '@/api/quality/qcItem'
import type { QcItem } from '@/types/domain'
import type { PageResult } from '@/types/http'

export type DefectItem = QcItem
export type DefectQuery = QcItemQuery

export const fetchDefects = async (query?: DefectQuery): Promise<PageResult<DefectItem>> => {
  return fetchQcItems(query)
}
