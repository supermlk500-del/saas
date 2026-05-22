// 接口暂时停用：/business/task/list

export type TaskItem = {
  key: string
  taskNo: string
  woNo: string
  templateName: string
  inspector: string
  deadline: string
  status: string
}

export type TaskQuery = {
  keyword?: string
  status?: string
}

export const fetchTasks = async (_query?: TaskQuery): Promise<TaskItem[]> => {
  return []
}
