import { fetchExceptionRecords, type ExceptionRecordQuery } from '@/api/exception/exceptionRecord'
import type { ExceptionRecordItem } from '@/types/domain'
import type { PageResult } from '@/types/http'

export type NcrItem = ExceptionRecordItem
export type NcrQuery = ExceptionRecordQuery

export const fetchNcrs = async (query?: NcrQuery): Promise<PageResult<NcrItem>> => {
  const response = await fetchExceptionRecords({
    ...query,
    exceptionType: query?.exceptionType || 'QUALITY',
  })
  return response
}
