<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { useTable } from '@/hooks/useTable'

type ReleaseDecisionItem = {
  key: string
  batchNo: string
  decision: string
  lockStatus: string
  availableQty: number
  effectiveTime: string
  operator: string
}

const searchForm = reactive({ batchNo: '', decision: undefined as string | undefined })

const searchFields = [
  { label: '批次号', name: 'batchNo', type: 'input' as const, placeholder: '请输入批次号', width: '220px' },
  {
    label: '决策',
    name: 'decision',
    type: 'select' as const,
    placeholder: '全部',
    options: [
      { label: '放行', value: '放行' },
      { label: '让步放行', value: '让步放行' },
      { label: '拒收', value: '拒收' },
      { label: '冻结', value: '冻结' },
    ],
  },
]

const columns = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo' },
  { title: '放行决策', dataIndex: 'decision', key: 'decision' },
  { title: '锁批状态', dataIndex: 'lockStatus', key: 'lockStatus' },
  { title: '可排产量(kg)', dataIndex: 'availableQty', key: 'availableQty' },
  { title: '生效时间', dataIndex: 'effectiveTime', key: 'effectiveTime' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' },
]

const { data, loading, pagination } = useTable<ReleaseDecisionItem>()

const allData: ReleaseDecisionItem[] = [
  { key: '1', batchNo: 'GB-20260401-01', decision: '放行', lockStatus: '未锁定', availableQty: 398, effectiveTime: '2026-04-01 18:30', operator: '李工' },
  { key: '2', batchNo: 'GB-20260401-02', decision: '让步放行', lockStatus: '未锁定', availableQty: 360, effectiveTime: '2026-04-01 19:10', operator: '王检' },
  { key: '3', batchNo: 'GB-20260402-01', decision: '冻结', lockStatus: '已锁定', availableQty: 0, effectiveTime: '2026-04-02 17:40', operator: '赵检' },
]

const loadData = () => {
  loading.value = true
  data.value = allData.filter((item) => {
    const hitBatch = !searchForm.batchNo || item.batchNo.includes(searchForm.batchNo)
    const hitDecision = !searchForm.decision || item.decision === searchForm.decision
    return hitBatch && hitDecision
  })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.batchNo = ''
  searchForm.decision = undefined
  loadData()
}

onMounted(loadData)
</script>

<template>
  <TablePage title="放行决策" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
  </TablePage>
</template>
