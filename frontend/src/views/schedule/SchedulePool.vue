<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { createPlan, type ProductionPlanCreateRequest } from '@/api/plan/plan'
import { fetchBatches, type BatchQuery } from '@/api/batch/batch'
import { fetchProcesses } from '@/api/master/process'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { batchStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { BatchItem, ProcessRouteItem } from '@/types/domain'

type SchedulePoolRow = BatchItem & {
  readyForSchedule: boolean
}

type PlanCreateForm = {
  batchId?: number
  batchNo: string
  routeId?: number
  planStartTime: string
  planEndTime: string
  remark: string
}

const searchForm = reactive({
  batchNo: '',
  readyForSchedule: undefined as string | undefined,
})

const searchFields = [
  { label: '批次编号', name: 'batchNo', type: 'input' as const, placeholder: '请输入批次编号', width: '220px' },
  {
    label: '可参与排产',
    name: 'readyForSchedule',
    type: 'select' as const,
    placeholder: '全部',
    options: [
      { label: '是', value: 'yes' },
      { label: '否', value: 'no' },
    ],
    width: '160px',
  },
]

const columns = [
  { title: '批次编号', dataIndex: 'batchNo', key: 'batchNo', width: 180 },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier', width: 220 },
  { title: '重量(kg)', dataIndex: 'weight', key: 'weight', width: 120 },
  { title: '门幅(cm)', dataIndex: 'width', key: 'width', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '入厂时间', dataIndex: 'inDate', key: 'inDate', width: 180 },
  { title: '可参与排产', dataIndex: 'readyForSchedule', key: 'readyForSchedule', width: 140 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const },
]

const { data, loading, pagination } = useTable<SchedulePoolRow>()

const backendRows = computed(() =>
  data.value.filter((item) => {
    if (!searchForm.readyForSchedule) {
      return true
    }

    return searchForm.readyForSchedule === 'yes' ? item.readyForSchedule : !item.readyForSchedule
  }),
)

const modalOpen = ref(false)
const submitting = ref(false)
const formRef = ref()
const routeOptions = ref<ProcessRouteItem[]>([])

const formModel = reactive<PlanCreateForm>({
  batchNo: '',
  planStartTime: '',
  planEndTime: '',
  remark: '',
})

const rules = {
  routeId: [{ required: true, message: '请选择工艺路线' }],
  planStartTime: [{ required: true, message: '请选择计划开始时间' }],
}

const loadData = async () => {
  loading.value = true

  try {
    const query: BatchQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      batchNo: searchForm.batchNo || undefined,
    }

    const response = await fetchBatches(query)
    data.value = response.list.map((item) => ({
      ...item,
      readyForSchedule: item.status === 'READY',
    }))
    pagination.total = response.total
  } finally {
    loading.value = false
  }
}

const loadRouteOptions = async () => {
  const response = await fetchProcesses({
    pageNum: 1,
    pageSize: 200,
    isActive: 1,
  })
  routeOptions.value = response.list
}

pagination.onChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  void loadData()
}

const resetSearch = () => {
  searchForm.batchNo = ''
  searchForm.readyForSchedule = undefined
  pagination.current = 1
  void loadData()
}

const getStatusOption = (status?: string) =>
  batchStatusOptions.find((item) => item.value === status)

const openCreatePlanModal = async (record: SchedulePoolRow) => {
  formModel.batchId = record.batchId
  formModel.batchNo = record.batchNo
  formModel.routeId = undefined
  formModel.planStartTime = ''
  formModel.planEndTime = ''
  formModel.remark = ''
  modalOpen.value = true

  if (!routeOptions.value.length) {
    await loadRouteOptions()
  }
}

const handleCreatePlan = async () => {
  await formRef.value?.validate()

  submitting.value = true
  try {
    const payload: ProductionPlanCreateRequest = {
      batchId: formModel.batchId ?? 0,
      routeId: formModel.routeId ?? 0,
      planStartTime: formModel.planStartTime,
      planEndTime: formModel.planEndTime || undefined,
      remark: formModel.remark.trim() || undefined,
    }

    await createPlan(payload)
    modalOpen.value = false
    message.success('生产计划创建成功')
    await loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <TablePage title="排产池" :columns="columns" :data="backendRows" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'weight'">
        {{ record.weight ?? '-' }}
      </template>
      <template v-else-if="column.key === 'width'">
        {{ record.width ?? '-' }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag v-if="getStatusOption(record.status)" :color="getStatusOption(record.status)?.color">
          {{ getStatusOption(record.status)?.label }}
        </a-tag>
        <span v-else>{{ record.status || '-' }}</span>
      </template>
      <template v-else-if="column.key === 'readyForSchedule'">
        <a-tag :color="record.readyForSchedule ? 'success' : 'default'">
          {{ record.readyForSchedule ? '是' : '否' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-button type="link" :disabled="!record.readyForSchedule" @click="openCreatePlanModal(record)">创建计划</a-button>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="modalOpen"
    title="创建生产计划"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="submitting"
    width="720px"
    @ok="handleCreatePlan"
  >
    <a-form ref="formRef" :model="formModel" :rules="rules" layout="vertical">
      <div class="form-grid">
        <a-form-item label="批次编号">
          <a-input :value="formModel.batchNo" disabled />
        </a-form-item>

        <a-form-item label="工艺路线" name="routeId">
          <a-select
            v-model:value="formModel.routeId"
            :options="routeOptions.map((item) => ({ label: item.routeName, value: item.routeId }))"
            placeholder="请选择工艺路线"
          />
        </a-form-item>

        <a-form-item label="计划开始时间" name="planStartTime">
          <a-date-picker
            v-model:value="formModel.planStartTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择计划开始时间"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="计划结束时间" name="planEndTime">
          <a-date-picker
            v-model:value="formModel.planEndTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="可选"
            style="width: 100%"
          />
        </a-form-item>
      </div>

      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formModel.remark" :rows="3" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
