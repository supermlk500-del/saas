<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { fetchTasks, type TaskItem, type TaskQuery } from '@/api/quality/task'
import { useTable } from '@/hooks/useTable'
import type { IdValue } from '@/types/domain'

const router = useRouter()

const searchForm = reactive({
  keyword: '',
  status: undefined as string | undefined,
})

const searchFields = [
  { label: '关键字', name: 'keyword', type: 'input' as const, placeholder: '任务号 / 工序名称 / 工序计划ID', width: '240px' },
  {
    label: '任务状态',
    name: 'status',
    type: 'select' as const,
    placeholder: '全部',
    options: [
      { label: '待检验', value: '待检验' },
      { label: '待处理', value: '待处理' },
      { label: '已检验', value: '已检验' },
    ],
    width: '150px',
  },
]

const columns = [
  { title: '任务号', dataIndex: 'taskNo', key: 'taskNo', width: 140 },
  { title: '工序计划ID', dataIndex: 'planStepId', key: 'planStepId', width: 110 },
  { title: '所属计划ID', dataIndex: 'planId', key: 'planId', width: 110 },
  { title: '工序名称', dataIndex: 'stepName', key: 'stepName', width: 160 },
  { title: '设备', dataIndex: 'machineName', key: 'machineName', width: 160 },
  { title: '最近检验时间', dataIndex: 'latestInspectTime', key: 'latestInspectTime', width: 180 },
  { title: '最近判定', dataIndex: 'latestJudge', key: 'latestJudge', width: 120 },
  { title: '任务状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const },
]

const { data, loading, pagination } = useTable<TaskItem>()

const loadData = async () => {
  loading.value = true
  try {
    const query: TaskQuery = {
      keyword: searchForm.keyword || undefined,
      status: searchForm.status,
    }
    const response = await fetchTasks(query)
    data.value = response.list
    pagination.total = response.total
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.status = undefined
  void loadData()
}

const getStatusColor = (status: TaskItem['status']) => {
  switch (status) {
    case '待检验':
      return 'blue'
    case '待处理':
      return 'orange'
    default:
      return 'green'
  }
}

const jumpToInspect = (planStepId: IdValue) => {
  void router.push({
    path: '/quality/realtime',
    query: { planStepId: String(planStepId) },
  })
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <TablePage title="IQC任务" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'stepName'">
        {{ record.stepName || '-' }}
      </template>
      <template v-else-if="column.key === 'machineName'">
        {{ record.machineName || '-' }}
      </template>
      <template v-else-if="column.key === 'latestInspectTime'">
        {{ record.latestInspectTime || '-' }}
      </template>
      <template v-else-if="column.key === 'latestJudge'">
        {{ record.latestJudge || '-' }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="getStatusColor(record.status)">{{ record.status }}</a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a-button type="link" @click="jumpToInspect(record.planStepId)">去检验</a-button>
          <a-button type="link" @click="jumpToInspect(record.planStepId)">查看工作台</a-button>
        </a-space>
      </template>
    </template>
  </TablePage>
</template>
