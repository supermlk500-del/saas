// 接口暂时停用：/business/rework/list

export type ReworkItem = {
  key: string
  reworkNo: string
  ncrNo: string
  woNo: string
  productName: string
  qty: number
  reason: string
  status: string
}

export type ReworkQuery = {
  keyword?: string
  status?: string
}

export const fetchReworks = async (_query?: ReworkQuery): Promise<ReworkItem[]> => {
  return []
}
