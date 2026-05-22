// 接口暂时停用：/business/result/list

export type ResultItem = {
  key: string
  resultNo: string
  taskNo: string
  woNo: string
  inspector: string
  result: string
  defectCount: number
  time: string
}

export type ResultQuery = {
  keyword?: string
  result?: string
}

export const fetchResults = async (_query?: ResultQuery): Promise<ResultItem[]> => {
  return []
}
