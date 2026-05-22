import { reactive, ref } from 'vue'

export const useTable = <T>() => {
  const loading = ref(false)
  const data = ref<T[]>([])

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    onChange: (page: number, pageSize: number) => {
      pagination.current = page
      pagination.pageSize = pageSize
    },
  })

  return { loading, data, pagination }
}
