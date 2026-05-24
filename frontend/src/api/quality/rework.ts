import { fetchExceptionRecords, type ExceptionRecordQuery } from '@/api/exception/exceptionRecord'
import type { ExceptionRecordItem } from '@/types/domain'
import type { PageResult } from '@/types/http'

export type ReworkItem = ExceptionRecordItem
export type ReworkQuery = ExceptionRecordQuery

export const fetchReworks = async (query?: ReworkQuery): Promise<PageResult<ReworkItem>> => {
  const response = await fetchExceptionRecords({
    ...query,
    exceptionType: query?.exceptionType || 'QUALITY',
  })
  return response
}
