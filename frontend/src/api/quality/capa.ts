import { fetchExceptionRecords, type ExceptionRecordQuery } from '@/api/exception/exceptionRecord'
import type { ExceptionRecordItem } from '@/types/domain'
import type { PageResult } from '@/types/http'

export type CapaItem = ExceptionRecordItem
export type CapaQuery = ExceptionRecordQuery

export const fetchCapas = async (query?: CapaQuery): Promise<PageResult<CapaItem>> => {
  const response = await fetchExceptionRecords({
    ...query,
    exceptionType: query?.exceptionType || 'QUALITY',
  })
  return {
    list: response.list.filter((item) => ['HIGH', 'CRITICAL'].includes(item.exceptionLevel)),
    total: response.list.filter((item) => ['HIGH', 'CRITICAL'].includes(item.exceptionLevel)).length,
  }
}
