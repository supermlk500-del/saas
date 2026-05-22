// 接口暂时停用：/business/ncr/list

export type NcrItem = {
  key: string
  ncrNo: string
  resultNo: string
  woNo: string
  defectName: string
  qty: number
  disposition: string
  status: string
}

export type NcrQuery = {
  keyword?: string
  status?: string
}

export const fetchNcrs = async (_query?: NcrQuery): Promise<NcrItem[]> => {
  return []
}
