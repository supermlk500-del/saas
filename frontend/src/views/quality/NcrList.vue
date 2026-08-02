<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  closeExceptionRecord,
  createExceptionRecord,
  fetchExceptionRecords,
  patchExceptionRecordStatus,
  type ExceptionCloseRequest,
  type ExceptionRecordQuery,
  type ExceptionRecordUpsertRequest,
  type ExceptionStatusPatchRequest,
} from '@/api/exception/exceptionRecord'
import { fetchPlanSteps } from '@/api/plan/planStep'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { exceptionLevelOptions, exceptionStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { ExceptionRecordItem, IdValue, PlanStepItem } from '@/types/domain'
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
  { title: '异常类型', dataIndex: 'exceptionType', key: 'exceptionType', width: 120 },
  { title: '异常等级', dataIndex: 'exceptionLevel', key: 'exceptionLevel', width: 120 },
  { title: '异常描述', dataIndex: 'description', key: 'description' },
  { title: '处理结果', dataIndex: 'handleResult', key: 'handleResult' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const },
]

const { data, loading, pagination } = useTable<ExceptionRecordItem>()

const createModalOpen = ref(false)
const createSubmitting = ref(false)
const createFormRef = ref()
const planSteps = ref<PlanStepItem[]>([])

const createForm = reactive({
  planStepId: undefined as IdValue | undefined,
  exceptionLevel: 'MEDIUM',
  description: '',
  createTime: formatDateTime(new Date().toISOString()),
})

const createRules = {
  planStepId: [{ required: true, message: '请选择工序计划' }],
  exceptionLevel: [{ required: true, message: '请选择异常等级' }],
  description: [{ required: true, message: '请输入异常描述' }],
  createTime: [{ required: true, message: '请选择创建时间' }],
}

const statusModalOpen = ref(false)
const statusSubmitting = ref(false)
const statusFormRef = ref()
const statusForm = reactive({
  exceptionId: undefined as IdValue | undefined,
  status: undefined as string | undefined,
  remark: '',
})

const statusRules = {
  status: [{ required: true, message: '请选择异常状态' }],
}

const closeModalOpen = ref(false)
const closeSubmitting = ref(false)
const closeForm = reactive({
  exceptionId: undefined as IdValue | undefined,
  handleResult: '',
  closeRemark: '',
})

const closeRules = {
  handleResult: [{ required: true, message: '请输入处理结果' }],
}

const loadPlanSteps = async () => {
  const response = await fetchPlanSteps({ pageNum: 1, pageSize: 200 })
  planSteps.value = response.list
}

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

const getLevelMeta = (value?: string) =>
  exceptionLevelOptions.find((item) => item.value === value)

const getStatusMeta = (value?: string) =>
  exceptionStatusOptions.find((item) => item.value === value)

const openCreateModal = async () => {
  if (!planSteps.value.length) {
    await loadPlanSteps()
  }
  createForm.planStepId = undefined
  createForm.exceptionLevel = 'MEDIUM'
  createForm.description = ''
  createForm.createTime = formatDateTime(new Date().toISOString())
  createModalOpen.value = true
}

const handleCreate = async () => {
  await createFormRef.value?.validate()
  createSubmitting.value = true
  try {
    if (createForm.planStepId === undefined) {
      return
    }

    const payload: ExceptionRecordUpsertRequest = {
      planStepId: createForm.planStepId,
      exceptionType: 'QUALITY',
      exceptionLevel: createForm.exceptionLevel,
      description: createForm.description.trim(),
      createTime: createForm.createTime,
      status: 'OPEN',
    }
    await createExceptionRecord(payload)
    createModalOpen.value = false
    message.success('质量异常登记成功')
    await loadData()
  } finally {
    createSubmitting.value = false
  }
}

const openStatusModal = (record: ExceptionRecordItem) => {
  statusForm.exceptionId = record.exceptionId
  statusForm.status = record.status
  statusForm.remark = ''
  statusModalOpen.value = true
}

const handleStatusSubmit = async () => {
  await statusFormRef.value?.validate()
  statusSubmitting.value = true
  try {
    if (statusForm.exceptionId === undefined) {
      return
    }

    const payload: ExceptionStatusPatchRequest = {
      status: statusForm.status ?? 'OPEN',
      remark: statusForm.remark.trim() || undefined,
    }
    await patchExceptionRecordStatus(statusForm.exceptionId, payload)
    statusModalOpen.value = false
    message.success('异常状态更新成功')
    await loadData()
  } finally {
    statusSubmitting.value = false
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
    message.success('异常关闭成功')
    await loadData()
  } finally {
    closeSubmitting.value = false
  }
}

onMounted(async () => {
  await loadData()
})
</script>

<template>
  <TablePage title="异常闭环" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #actions>
      <a-button v-permission="'exception:record:handle'" type="primary" @click="openCreateModal">登记质量异常</a-button>
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
          <a-button v-permission="'exception:record:handle'" type="link" @click="openStatusModal(record)">状态</a-button>
          <a-button v-permission="'exception:record:handle'" type="link" @click="openCloseModal(record)">关闭</a-button>
        </a-space>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="createModalOpen"
    title="登记质量异常"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="createSubmitting"
    width="640px"
    @ok="handleCreate"
  >
    <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
      <a-form-item label="工序计划" name="planStepId">
        <a-select
          v-model:value="createForm.planStepId"
          :options="planSteps.map((item) => ({ label: `${formatPlanStepId(item.planStepId)} / ${item.stepName || item.stepId}`, value: item.planStepId }))"
          placeholder="请选择工序计划"
          show-search
          option-filter-prop="label"
        />
      </a-form-item>
      <a-form-item label="异常等级" name="exceptionLevel">
        <a-select v-model:value="createForm.exceptionLevel" :options="exceptionLevelOptions" />
      </a-form-item>
      <a-form-item label="创建时间" name="createTime">
        <a-date-picker
          v-model:value="createForm.createTime"
          show-time
          value-format="YYYY-MM-DD HH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="异常描述" name="description">
        <a-textarea v-model:value="createForm.description" :rows="4" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="statusModalOpen"
    title="更新异常状态"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="statusSubmitting"
    width="520px"
    @ok="handleStatusSubmit"
  >
    <a-form ref="statusFormRef" :model="statusForm" :rules="statusRules" layout="vertical">
      <a-form-item label="异常状态" name="status">
        <a-select v-model:value="statusForm.status" :options="exceptionStatusOptions" />
      </a-form-item>
      <a-form-item label="变更说明" name="remark">
        <a-textarea v-model:value="statusForm.remark" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="closeModalOpen"
    title="关闭异常"
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
