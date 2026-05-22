<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { useTable } from '@/hooks/useTable'

type InboundBatchItem = {
  key: string
  batchNo: string
  rollNo: string
  fabricCode: string
  receivedQty: number
  warehouse: string
  inboundTime: string
  inboundStatus: string
}

const searchForm = reactive({ batchNo: '', inboundStatus: undefined as string | undefined })

const searchFields = [
  { label: '批次号', name: 'batchNo', type: 'input' as const, placeholder: '请输入批次号', width: '220px' },
  {
    label: '状态',
    name: 'inboundStatus',
    type: 'select' as const,
    placeholder: '全部',
    options: [
      { label: '待检验', value: '待检验' },
      { label: '已检验', value: '已检验' },
      { label: '已冻结', value: '已冻结' },
    ],
  },
]

const columns = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo' },
  { title: '卷号', dataIndex: 'rollNo', key: 'rollNo' },
  { title: '胚布编码', dataIndex: 'fabricCode', key: 'fabricCode' },
  { title: '到货数量(kg)', dataIndex: 'receivedQty', key: 'receivedQty' },
  { title: '仓库', dataIndex: 'warehouse', key: 'warehouse' },
  { title: '入库时间', dataIndex: 'inboundTime', key: 'inboundTime' },
  { title: '状态', dataIndex: 'inboundStatus', key: 'inboundStatus' },
]

const { data, loading, pagination } = useTable<InboundBatchItem>()

const allData: InboundBatchItem[] = [
  { key: '1', batchNo: 'GB-20260401-01', rollNo: 'R-001', fabricCode: 'GSF-001', receivedQty: 420, warehouse: '一号原料仓', inboundTime: '2026-04-01 09:30', inboundStatus: '待检验' },
  { key: '2', batchNo: 'GB-20260401-02', rollNo: 'R-002', fabricCode: 'GSF-001', receivedQty: 380, warehouse: '一号原料仓', inboundTime: '2026-04-01 11:20', inboundStatus: '已检验' },
  { key: '3', batchNo: 'GB-20260402-01', rollNo: 'R-003', fabricCode: 'GSF-002', receivedQty: 500, warehouse: '二号原料仓', inboundTime: '2026-04-02 14:10', inboundStatus: '已冻结' },
]

const loadData = () => {
  loading.value = true
  data.value = allData.filter((item) => {
    const hitBatch = !searchForm.batchNo || item.batchNo.includes(searchForm.batchNo)
    const hitStatus = !searchForm.inboundStatus || item.inboundStatus === searchForm.inboundStatus
    return hitBatch && hitStatus
  })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.batchNo = ''
  searchForm.inboundStatus = undefined
  loadData()
}

onMounted(loadData)
</script>

<template>
  <TablePage title="来料批次" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
  </TablePage>
</template>
