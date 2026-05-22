export interface ApiSuccessResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

export interface ApiListResponse<T = unknown> {
  code: number
  msg: string
  rows: T[]
  total: number
}

export interface ApiErrorResponse {
  code: number
  msg: string
}

export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
}
