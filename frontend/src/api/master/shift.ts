// 接口暂时停用：/business/shift/list

export type ShiftItem = {
  key: string
  shiftName: string
  startTime: string
  endTime: string
  type: string
  status: string
}

export type ShiftQuery = {
  keyword?: string
  type?: string
}

export const fetchShifts = async (_query?: ShiftQuery): Promise<ShiftItem[]> => {
  return []
}
