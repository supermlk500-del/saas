import { fetchQcRecords, type QcRecordQuery } from '@/api/quality/qcRecord'
import type { QcRecordItem } from '@/types/domain'
import type { PageResult } from '@/types/http'

export type ResultItem = QcRecordItem
export type ResultQuery = QcRecordQuery

export const fetchResults = async (query?: ResultQuery): Promise<PageResult<ResultItem>> => {
  return fetchQcRecords(query)
}
