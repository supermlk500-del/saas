// 接口暂时停用：/business/gray-fabric/list

export type GrayFabricItem = {
  key: string
  code: string
  name: string
  yarn: string
  weight: string
  width: string
  weave: string
  supplier: string
  status: string
}

export type GrayFabricQuery = {
  keyword?: string
  supplier?: string
}

export const fetchGrayFabrics = async (_query?: GrayFabricQuery): Promise<GrayFabricItem[]> => {
  return []
}
