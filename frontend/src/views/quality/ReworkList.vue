<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { closeExceptionRecord, fetchExceptionRecords, reworkExceptionRecord, type ExceptionCloseRequest, type ExceptionRecordQuery, type ExceptionReworkRequest } from '@/api/exception/exceptionRecord'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { exceptionLevelOptions, exceptionStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { ExceptionRecordItem, IdValue } from '@/types/domain'
import { formatDateTime } from '@/utils/date'
import { formatPlanStepId } from '@/utils/idFormat'

const searchForm = reactive({
  planStepId: undefined as IdValue | undefined,
  exceptionLevel: undefined as string | undefined,
  status: undefined as string | undefined,
})

const searchFields = [
  { label: '工序计划ID', name: 'planStepId', type: 'number' as const, width: '180px' },
  { label: '异常等级', name: 'exceptionLevel', type: 'select' as const, placeholder: '全部', options: exceptionLevelOptions, width: '150px' },
  { label: '异常状态', name: 'status', type: 'select' as const, placeholder: '全部', options: exceptionStatusOptions, width: '150px' },
]

const columns = [
  { title: '异常ID', dataIndex: 'exceptionId', key: 'exceptionId', width: 100 },
  { title: '工序计划ID', dataIndex: 'planStepId', key: 'planStepId', width: 96 },
  { title: '异常等级', dataIndex: 'exceptionLevel', key: 'exceptionLevel', width: 120 },
  { title: '异常描述', dataIndex: 'description', key: 'description' },
  { title: '处理结果', dataIndex: 'handleResult', key: 'handleResult' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const },
]

const { data, loading, pagination } = useTable<ExceptionRecordItem>()

const reworkModalOpen = ref(false)
const reworkSubmitting = ref(false)
const reworkFormRef = ref()
const reworkForm = reactive({
  exceptionId: undefined as IdValue | undefined,
  reworkPlan: '',
  reworkOwner: '',
  expectedFinishTime: '',
})

const closeModalOpen = ref(false)
const closeSubmitting = ref(false)
const closeForm = reactive({
  exceptionId: undefined as IdValue | undefined,
  handleResult: '',
  closeRemark: '',
})

const reworkRules = {
  reworkPlan: [{ required: true, message: '请输入返工方案' }],
}

const closeRules = {
  handleResult: [{ required: true, message: '请输入处理结果' }],
}

const getLevelMeta = (value?: string) =>
  exceptionLevelOptions.find((item) => item.value === value)

const getStatusMeta = (value?: string) =>
  exceptionStatusOptions.find((item) => item.value === value)

const loadData = async () => {
  loading.value = true
  try {
    const query: ExceptionRecordQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      planStepId: searchForm.planStepId,
      exceptionType: 'QUALITY',
      exceptionLevel: searchForm.exceptionLevel,
      status: searchForm.status,
    }
    const response = await fetchExceptionRecords(query)
    data.value = response.list
    pagination.total = response.total
  } finally {
    loading.value = false
  }
}

pagination.onChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  void loadData()
}

const resetSearch = () => {
  searchForm.planStepId = undefined
  searchForm.exceptionLevel = undefined
  searchForm.status = undefined
  pagination.current = 1
  void loadData()
}

const openReworkModal = (record: ExceptionRecordItem) => {
  reworkForm.exceptionId = record.exceptionId
  reworkForm.reworkPlan = ''
  reworkForm.reworkOwner = ''
  reworkForm.expectedFinishTime = ''
  reworkModalOpen.value = true
}

const handleReworkSubmit = async () => {
  await reworkFormRef.value?.validate()
  reworkSubmitting.value = true
  try {
    if (reworkForm.exceptionId === undefined) {
      return
    }

    const payload: ExceptionReworkRequest = {
      reworkPlan: reworkForm.reworkPlan.trim(),
      reworkOwner: reworkForm.reworkOwner.trim() || undefined,
      expectedFinishTime: reworkForm.expectedFinishTime || undefined,
    }
    await reworkExceptionRecord(reworkForm.exceptionId, payload)
    reworkModalOpen.value = false
    message.success('返工处理已发起')
    await loadData()
  } finally {
    reworkSubmitting.value = false
  }
}

const openCloseModal = (record: ExceptionRecordItem) => {
  closeForm.exceptionId = record.exceptionId
  closeForm.handleResult = record.handleResult || ''
  closeForm.closeRemark = ''
  closeModalOpen.value = true
}

const handleCloseSubmit = async () => {
  closeSubmitting.value = true
  try {
    if (closeForm.exceptionId === undefined) {
      return
    }

    const payload: ExceptionCloseRequest = {
      handleResult: closeForm.handleResult.trim(),
      closeRemark: closeForm.closeRemark.trim() || undefined,
    }
    await closeExceptionRecord(closeForm.exceptionId, payload)
    closeModalOpen.value = false
    message.success('返工异常已关闭')
    await loadData()
  } finally {
    closeSubmitting.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <TablePage title="返工处理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'planStepId'">
        <a-tooltip :title="String(record.planStepId)">
          {{ formatPlanStepId(record.planStepId) }}
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'exceptionLevel'">
        <a-tag :color="getLevelMeta(record.exceptionLevel)?.color">
          {{ getLevelMeta(record.exceptionLevel)?.label || record.exceptionLevel }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'handleResult'">
        {{ record.handleResult || '-' }}
      </template>
      <template v-else-if="column.key === 'createTime'">
        {{ formatDateTime(record.createTime) }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="getStatusMeta(record.status)?.color">
          {{ getStatusMeta(record.status)?.label || record.status }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a-button v-permission="'exception:record:rework'" type="link" @click="openReworkModal(record)">发起返工</a-button>
          <a-button v-permission="'exception:record:handle'" type="link" @click="openCloseModal(record)">关闭</a-button>
        </a-space>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="reworkModalOpen"
    title="发起返工处理"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="reworkSubmitting"
    width="560px"
    @ok="handleReworkSubmit"
  >
    <a-form ref="reworkFormRef" :model="reworkForm" :rules="reworkRules" layout="vertical">
      <a-form-item label="返工方案" name="reworkPlan">
        <a-textarea v-model:value="reworkForm.reworkPlan" :rows="3" />
      </a-form-item>
      <a-form-item label="责任人" name="reworkOwner">
        <a-input v-model:value="reworkForm.reworkOwner" />
      </a-form-item>
      <a-form-item label="预计完成时间" name="expectedFinishTime">
        <a-date-picker
          v-model:value="reworkForm.expectedFinishTime"
          show-time
          value-format="YYYY-MM-DD HH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="closeModalOpen"
    title="关闭返工异常"
    ok-text="关闭"
    cancel-text="取消"
    :confirm-loading="closeSubmitting"
    width="560px"
    @ok="handleCloseSubmit"
  >
    <a-form :model="closeForm" :rules="closeRules" layout="vertical">
      <a-form-item label="处理结果" name="handleResult">
        <a-textarea v-model:value="closeForm.handleResult" :rows="3" />
      </a-form-item>
      <a-form-item label="关闭说明" name="closeRemark">
        <a-textarea v-model:value="closeForm.closeRemark" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
