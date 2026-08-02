<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, type TableColumnsType } from 'ant-design-vue'
import {
  createProcessRoute,
  createProcessStep,
  createRouteStep,
  deleteProcessRoute,
  deleteProcessStep,
  patchProcessStepStatus,
  fetchProcesses,
  fetchProcessSteps,
  fetchRouteSteps,
  getProcessRoute,
  updateProcessRoute,
  updateProcessStep,
  updateRouteStep,
  deleteRouteStep,
  type ProcessQuery,
  type ProcessRouteUpsertRequest,
  type ProcessStepQuery,
  type ProcessStepUpsertRequest,
  type RouteStepUpsertRequest,
} from '@/api/master/process'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { enabledStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { IdValue, ProcessRouteItem, ProcessStepItem, RouteStepItem } from '@/types/domain'

type ProcessRouteFormModel = {
  routeId?: IdValue
  routeName: string
  description: string
  isActive: number | undefined
}

type RouteStepFormModel = {
  routeStepId?: IdValue
  stepId: IdValue | undefined
  sortOrder: number | null
  isMandatory: number | undefined
}

type ProcessStepFormModel = {
  stepId?: IdValue
  stepCode: string
  stepName: string
  stepType: string
  sortOrder: number | null
  defaultHours: number | null
  description: string
  isActive: number | undefined
}

const binaryStatusOptions = [
  { label: '是', value: 1 },
  { label: '否', value: 0 },
]

const searchForm = reactive({
  routeName: '',
  isActive: undefined as number | undefined,
})

const searchFields = [
  { label: '路线名称', name: 'routeName', type: 'input' as const, placeholder: '请输入路线名称', width: '240px' },
  {
    label: '启用状态',
    name: 'isActive',
    type: 'select' as const,
    placeholder: '全部',
    options: enabledStatusOptions,
    width: '160px',
  },
]

const columns: TableColumnsType<ProcessRouteItem> = [
  { title: '路线ID', dataIndex: 'routeId', key: 'routeId', width: 90, responsive: ['xl'] },
  { title: '路线名称', dataIndex: 'routeName', key: 'routeName', width: 190 },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '启用状态', dataIndex: 'isActive', key: 'isActive', width: 90, align: 'center' as const },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 220 },
]

const routeStepColumns: TableColumnsType<RouteStepItem> = [
  { title: '顺序号', dataIndex: 'sortOrder', key: 'sortOrder', width: 90 },
  { title: '工序编码', dataIndex: 'stepCode', key: 'stepCode', width: 140 },
  { title: '工序名称', dataIndex: 'stepName', key: 'stepName', width: 180 },
  { title: '必选', dataIndex: 'isMandatory', key: 'isMandatory', width: 90 },
  { title: '操作', key: 'action', width: 150 },
]

const processStepColumns: TableColumnsType<ProcessStepItem> = [
  { title: '工序ID', dataIndex: 'stepId', key: 'stepId', width: 100 },
  { title: '工序编码', dataIndex: 'stepCode', key: 'stepCode', width: 140 },
  { title: '工序名称', dataIndex: 'stepName', key: 'stepName', width: 160 },
  { title: '工序类型', dataIndex: 'stepType', key: 'stepType', width: 140 },
  { title: '默认顺序', dataIndex: 'sortOrder', key: 'sortOrder', width: 100 },
  { title: '默认工时', dataIndex: 'defaultHours', key: 'defaultHours', width: 100 },
  { title: '启用状态', dataIndex: 'isActive', key: 'isActive', width: 100 },
  { title: '操作', key: 'action', width: 230 },
]

const { data, loading, pagination } = useTable<ProcessRouteItem>()

const modalOpen = ref(false)
const submitting = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const formRef = ref()

const routeStepDrawerOpen = ref(false)
const routeStepLoading = ref(false)
const currentRoute = ref<ProcessRouteItem | null>(null)
const routeSteps = ref<RouteStepItem[]>([])

const routeStepModalOpen = ref(false)
const routeStepSubmitting = ref(false)
const routeStepMode = ref<'create' | 'edit'>('create')
const routeStepFormRef = ref()

const processStepLibraryOpen = ref(false)
const processStepLoading = ref(false)
const processSteps = ref<ProcessStepItem[]>([])
const processStepModalOpen = ref(false)
const processStepSubmitting = ref(false)
const processStepMode = ref<'create' | 'edit'>('create')
const processStepFormRef = ref()

const formModel = reactive<ProcessRouteFormModel>({
  routeName: '',
  description: '',
  isActive: 1,
})

const routeStepForm = reactive<RouteStepFormModel>({
  stepId: undefined,
  sortOrder: 1,
  isMandatory: 1,
})

const processStepForm = reactive<ProcessStepFormModel>({
  stepCode: '',
  stepName: '',
  stepType: '',
  sortOrder: 0,
  defaultHours: null,
  description: '',
  isActive: 1,
})

const rules = {
  routeName: [{ required: true, message: '请输入路线名称' }],
  isActive: [{ required: true, message: '请选择启用状态' }],
}

const routeStepRules = {
  stepId: [{ required: true, message: '请选择工序模板' }],
  sortOrder: [{ required: true, message: '请输入顺序号' }],
  isMandatory: [{ required: true, message: '请选择是否必选' }],
}

const processStepRules = {
  stepCode: [{ required: true, message: '请输入工序编码' }],
  stepName: [{ required: true, message: '请输入工序名称' }],
  isActive: [{ required: true, message: '请选择启用状态' }],
}

const processStepOptions = computed(() =>
  processSteps.value.map((item) => ({
    label: `${item.stepCode} - ${item.stepName}`,
    value: item.stepId,
  })),
)

const loadData = async () => {
  loading.value = true

  try {
    const query: ProcessQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      routeName: searchForm.routeName || undefined,
      isActive: searchForm.isActive,
    }

    const response = await fetchProcesses(query)
    data.value = response.list
    pagination.total = response.total
  } finally {
    loading.value = false
  }
}

const loadProcessStepLibrary = async () => {
  processStepLoading.value = true

  try {
    const query: ProcessStepQuery = {
      pageNum: 1,
      pageSize: 200,
    }

    const response = await fetchProcessSteps(query)
    processSteps.value = response.list
  } finally {
    processStepLoading.value = false
  }
}

const loadRouteDetail = async (routeId: IdValue) => {
  routeStepLoading.value = true

  try {
    const [detail, steps] = await Promise.all([
      getProcessRoute(routeId),
      fetchRouteSteps(routeId),
      loadProcessStepLibrary(),
    ])

    currentRoute.value = detail.data
    routeSteps.value = steps
  } finally {
    routeStepLoading.value = false
  }
}

pagination.onChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  void loadData()
}

const resetSearch = () => {
  searchForm.routeName = ''
  searchForm.isActive = undefined
  pagination.current = 1
  void loadData()
}

const getEnabledLabel = (value: number) =>
  enabledStatusOptions.find((item) => item.value === value)

const resetFormModel = () => {
  formModel.routeId = undefined
  formModel.routeName = ''
  formModel.description = ''
  formModel.isActive = 1
}

const resetRouteStepForm = () => {
  routeStepForm.routeStepId = undefined
  routeStepForm.stepId = undefined
  routeStepForm.sortOrder = routeSteps.value.length + 1
  routeStepForm.isMandatory = 1
}

const resetProcessStepForm = () => {
  processStepForm.stepId = undefined
  processStepForm.stepCode = ''
  processStepForm.stepName = ''
  processStepForm.stepType = ''
  processStepForm.sortOrder = 0
  processStepForm.defaultHours = null
  processStepForm.description = ''
  processStepForm.isActive = 1
}

const openCreateModal = () => {
  modalMode.value = 'create'
  resetFormModel()
  modalOpen.value = true
}

const openEditModal = (record: ProcessRouteItem) => {
  modalMode.value = 'edit'
  formModel.routeId = record.routeId
  formModel.routeName = record.routeName
  formModel.description = record.description || ''
  formModel.isActive = record.isActive
  modalOpen.value = true
}

const openRouteStepDrawer = async (record: ProcessRouteItem) => {
  routeStepDrawerOpen.value = true
  currentRoute.value = record
  await loadRouteDetail(record.routeId)
}

const openRouteStepModal = async () => {
  if (!processSteps.value.length) {
    await loadProcessStepLibrary()
  }

  if (!processSteps.value.length) {
    message.warning('请先在工序模板库中创建工序')
    return
  }

  routeStepMode.value = 'create'
  resetRouteStepForm()
  routeStepModalOpen.value = true
}

const openEditRouteStepModal = async (record: RouteStepItem) => {
  if (!processSteps.value.length) {
    await loadProcessStepLibrary()
  }

  routeStepMode.value = 'edit'
  routeStepForm.routeStepId = record.routeStepId
  routeStepForm.stepId = record.stepId
  routeStepForm.sortOrder = record.sortOrder
  routeStepForm.isMandatory = record.isMandatory
  routeStepModalOpen.value = true
}

const openProcessStepLibrary = async () => {
  processStepLibraryOpen.value = true
  await loadProcessStepLibrary()
}

const openCreateProcessStepModal = () => {
  processStepMode.value = 'create'
  resetProcessStepForm()
  processStepModalOpen.value = true
}

const openEditProcessStepModal = (record: ProcessStepItem) => {
  processStepMode.value = 'edit'
  processStepForm.stepId = record.stepId
  processStepForm.stepCode = record.stepCode
  processStepForm.stepName = record.stepName
  processStepForm.stepType = record.stepType || ''
  processStepForm.sortOrder = record.sortOrder ?? 0
  processStepForm.defaultHours = record.defaultHours != null ? Number(record.defaultHours) : null
  processStepForm.description = record.description || ''
  processStepForm.isActive = record.isActive
  processStepModalOpen.value = true
}

const buildPayload = (): ProcessRouteUpsertRequest => ({
  routeName: formModel.routeName.trim(),
  description: formModel.description.trim() || undefined,
  isActive: formModel.isActive ?? 1,
})

const buildRouteStepPayload = (): RouteStepUpsertRequest => ({
  stepId: routeStepForm.stepId ?? 0,
  sortOrder: routeStepForm.sortOrder ?? 1,
  isMandatory: routeStepForm.isMandatory ?? 1,
})

const buildProcessStepPayload = (): ProcessStepUpsertRequest => ({
  stepCode: processStepForm.stepCode.trim(),
  stepName: processStepForm.stepName.trim(),
  stepType: processStepForm.stepType.trim() || undefined,
  sortOrder: processStepForm.sortOrder,
  defaultHours: processStepForm.defaultHours,
  description: processStepForm.description.trim() || undefined,
  isActive: processStepForm.isActive ?? 1,
})

const handleSubmit = async () => {
  await formRef.value?.validate()

  submitting.value = true
  try {
    const payload = buildPayload()

    if (modalMode.value === 'create') {
      await createProcessRoute(payload)
      message.success('新增工艺路线成功')
    } else if (formModel.routeId) {
      await updateProcessRoute(formModel.routeId, payload)
      message.success('编辑工艺路线成功')
    }

    modalOpen.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

const handleRouteStepSubmit = async () => {
  if (!currentRoute.value) {
    return
  }

  await routeStepFormRef.value?.validate()

  routeStepSubmitting.value = true
  try {
    const payload = buildRouteStepPayload()

    if (routeStepMode.value === 'create') {
      await createRouteStep(currentRoute.value.routeId, payload)
      message.success('新增路线工序成功')
    } else if (routeStepForm.routeStepId) {
      await updateRouteStep(currentRoute.value.routeId, routeStepForm.routeStepId, payload)
      message.success('编辑路线工序成功')
    }

    routeStepModalOpen.value = false
    await loadRouteDetail(currentRoute.value.routeId)
  } finally {
    routeStepSubmitting.value = false
  }
}

const handleDeleteRouteStep = async (record: RouteStepItem) => {
  if (!currentRoute.value) {
    return
  }

  await deleteRouteStep(currentRoute.value.routeId, record.routeStepId)
  message.success('删除路线工序成功')
  await loadRouteDetail(currentRoute.value.routeId)
}

const handleDeleteProcessRoute = async (record: ProcessRouteItem) => {
  await deleteProcessRoute(record.routeId)
  message.success('删除工艺路线成功')
  await loadData()
}

const handleProcessStepSubmit = async () => {
  await processStepFormRef.value?.validate()

  processStepSubmitting.value = true
  try {
    const payload = buildProcessStepPayload()

    if (processStepMode.value === 'create') {
      await createProcessStep(payload)
      message.success('新增工序模板成功')
    } else if (processStepForm.stepId) {
      await updateProcessStep(processStepForm.stepId, payload)
      message.success('编辑工序模板成功')
    }

    processStepModalOpen.value = false
    await loadProcessStepLibrary()
  } finally {
    processStepSubmitting.value = false
  }
}

const handlePatchProcessStepStatus = async (record: ProcessStepItem) => {
  const nextStatus = record.isActive === 1 ? 0 : 1
  await patchProcessStepStatus(record.stepId, nextStatus)
  message.success(nextStatus === 1 ? '启用工序模板成功' : '停用工序模板成功')
  await loadProcessStepLibrary()
}

const handleDeleteProcessStep = async (record: ProcessStepItem) => {
  await deleteProcessStep(record.stepId)
  message.success('删除工序模板成功')
  await loadProcessStepLibrary()
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <TablePage
    title="工艺路线"
    :columns="columns"
    :data="data"
    :loading="loading"
    :pagination="pagination"
    table-layout="fixed"
    class="process-route-page"
  >
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #actions>
      <a-space>
        <a-button @click="openProcessStepLibrary">工序模板库</a-button>
        <a-button v-permission="'process:route:edit'" type="primary" @click="openCreateModal">新增路线</a-button>
      </a-space>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'routeName'">
        <a-tooltip :title="record.routeName">
          <span class="route-name-cell">{{ record.routeName }}</span>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'description'">
        <a-tooltip :title="record.description || '-'">
          <span class="route-description-cell">{{ record.description || '-' }}</span>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'isActive'">
        <a-tag class="route-status-tag" :color="getEnabledLabel(record.isActive)?.color">
          {{ getEnabledLabel(record.isActive)?.label || record.isActive }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'createTime'">
        {{ record.createTime || '-' }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a-button v-permission="'process:route:edit'" type="link" @click="openRouteStepDrawer(record)">工序配置</a-button>
          <a-button v-permission="'process:route:edit'" type="link" @click="openEditModal(record)">编辑</a-button>
          <a-popconfirm title="确认删除该工艺路线吗？" @confirm="handleDeleteProcessRoute(record)">
            <a-button v-permission="'process:route:edit'" type="link" danger>删除</a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="modalOpen"
    :title="modalMode === 'create' ? '新增工艺路线' : '编辑工艺路线'"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="submitting"
    width="640px"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formModel" :rules="rules" layout="vertical">
      <a-form-item label="路线名称" name="routeName">
        <a-input v-model:value="formModel.routeName" placeholder="请输入路线名称" />
      </a-form-item>

      <a-form-item label="描述" name="description">
        <a-textarea v-model:value="formModel.description" :rows="4" placeholder="请输入路线描述" />
      </a-form-item>

      <a-form-item label="启用状态" name="isActive">
        <a-select
          v-model:value="formModel.isActive"
          :options="enabledStatusOptions"
          placeholder="请选择启用状态"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-drawer
    v-model:open="routeStepDrawerOpen"
    width="920px"
    title="路线工序配置"
    :destroy-on-close="true"
  >
    <a-spin :spinning="routeStepLoading">
      <div v-if="currentRoute" class="route-detail">
        <div class="route-detail-header">
          <div>
            <div class="route-title">{{ currentRoute.routeName }}</div>
            <div class="route-subtitle">{{ currentRoute.description || '暂无路线描述' }}</div>
          </div>
          <a-space>
            <a-button @click="openProcessStepLibrary">工序模板库</a-button>
            <a-button v-permission="'process:route:edit'" type="primary" @click="openRouteStepModal">新增路线工序</a-button>
          </a-space>
        </div>

        <a-descriptions :column="3" bordered size="small" class="route-summary">
          <a-descriptions-item label="路线ID">{{ currentRoute.routeId }}</a-descriptions-item>
          <a-descriptions-item label="启用状态">
            {{ getEnabledLabel(currentRoute.isActive)?.label || currentRoute.isActive }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ currentRoute.createTime || '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-table
          :columns="routeStepColumns"
          :data-source="routeSteps"
          :pagination="false"
          row-key="routeStepId"
          class="route-step-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'isMandatory'">
              <a-tag :color="record.isMandatory === 1 ? 'success' : 'default'">
                {{ record.isMandatory === 1 ? '是' : '否' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" @click="openEditRouteStepModal(record)">编辑</a-button>
                <a-popconfirm title="确认删除该路线工序吗？" @confirm="handleDeleteRouteStep(record)">
                  <a-button v-permission="'process:route:edit'" type="link" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </a-spin>
  </a-drawer>

  <a-modal
    v-model:open="routeStepModalOpen"
    :title="routeStepMode === 'create' ? '新增路线工序' : '编辑路线工序'"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="routeStepSubmitting"
    width="560px"
    @ok="handleRouteStepSubmit"
  >
    <a-form ref="routeStepFormRef" :model="routeStepForm" :rules="routeStepRules" layout="vertical">
      <a-form-item label="工序模板" name="stepId">
        <a-select
          v-model:value="routeStepForm.stepId"
          :options="processStepOptions"
          placeholder="请选择工序模板"
          show-search
          option-filter-prop="label"
        />
      </a-form-item>

      <a-form-item label="顺序号" name="sortOrder">
        <a-input-number v-model:value="routeStepForm.sortOrder" :min="1" style="width: 100%" />
      </a-form-item>

      <a-form-item label="是否必选" name="isMandatory">
        <a-select
          v-model:value="routeStepForm.isMandatory"
          :options="binaryStatusOptions"
          placeholder="请选择是否必选"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="processStepLibraryOpen"
    title="工序模板库"
    width="1080px"
    :footer="null"
    destroy-on-close
  >
    <div class="process-step-toolbar">
      <div class="process-step-hint">当前页面用于维护工艺路线依赖的工序模板资源。</div>
      <a-button v-permission="'process:route:edit'" type="primary" @click="openCreateProcessStepModal">新增工序模板</a-button>
    </div>

    <a-table
      :columns="processStepColumns"
      :data-source="processSteps"
      :loading="processStepLoading"
      :pagination="{ pageSize: 8, showSizeChanger: false }"
      row-key="stepId"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'stepType'">
          {{ record.stepType || '-' }}
        </template>
        <template v-else-if="column.key === 'sortOrder'">
          {{ record.sortOrder ?? '-' }}
        </template>
        <template v-else-if="column.key === 'defaultHours'">
          {{ record.defaultHours ?? '-' }}
        </template>
        <template v-else-if="column.key === 'isActive'">
          <a-tag :color="getEnabledLabel(record.isActive)?.color">
            {{ getEnabledLabel(record.isActive)?.label || record.isActive }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" @click="openEditProcessStepModal(record)">编辑</a-button>
            <a-popconfirm
              :title="record.isActive === 1 ? '确认停用该工序模板吗？' : '确认启用该工序模板吗？'"
              @confirm="handlePatchProcessStepStatus(record)"
            >
              <a-button type="link">
                {{ record.isActive === 1 ? '停用' : '启用' }}
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              title="确认删除该工序模板吗？已被路线、设备能力或生产计划引用时将无法删除。"
              @confirm="handleDeleteProcessStep(record)"
            >
              <a-button v-permission="'process:route:edit'" type="link" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-modal>

  <a-modal
    v-model:open="processStepModalOpen"
    :title="processStepMode === 'create' ? '新增工序模板' : '编辑工序模板'"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="processStepSubmitting"
    width="720px"
    @ok="handleProcessStepSubmit"
  >
    <a-form ref="processStepFormRef" :model="processStepForm" :rules="processStepRules" layout="vertical">
      <div class="form-grid">
        <a-form-item label="工序编码" name="stepCode">
          <a-input v-model:value="processStepForm.stepCode" placeholder="请输入工序编码" />
        </a-form-item>

        <a-form-item label="工序名称" name="stepName">
          <a-input v-model:value="processStepForm.stepName" placeholder="请输入工序名称" />
        </a-form-item>

        <a-form-item label="工序类型" name="stepType">
          <a-input v-model:value="processStepForm.stepType" placeholder="请输入工序类型" />
        </a-form-item>

        <a-form-item label="默认顺序" name="sortOrder">
          <a-input-number v-model:value="processStepForm.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>

        <a-form-item label="默认工时" name="defaultHours">
          <a-input-number
            v-model:value="processStepForm.defaultHours"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="启用状态" name="isActive">
          <a-select
            v-model:value="processStepForm.isActive"
            :options="enabledStatusOptions"
            placeholder="请选择启用状态"
          />
        </a-form-item>
      </div>

      <a-form-item label="描述" name="description">
        <a-textarea v-model:value="processStepForm.description" :rows="4" placeholder="请输入工序描述" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.route-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.route-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.route-title {
  color: #1e293b;
  font-size: 20px;
  font-weight: 700;
}

.route-subtitle {
  margin-top: 6px;
  color: #64748b;
}

.route-summary {
  margin-bottom: 6px;
}

.route-step-table {
  margin-top: 4px;
}

.process-route-page {
  width: 100%;
  min-width: 0;
}

.process-route-page :deep(.table-card),
.process-route-page :deep(.ant-card-body),
.process-route-page :deep(.ant-table-wrapper),
.process-route-page :deep(.ant-spin-nested-loading),
.process-route-page :deep(.ant-spin-container) {
  min-width: 0;
}

.process-route-page :deep(.ant-table-tbody > tr > td) {
  height: 68px;
  vertical-align: middle;
}

.route-name-cell {
  display: block;
  overflow: hidden;
  color: #253248;
  font-weight: 600;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-description-cell {
  display: -webkit-box;
  overflow: hidden;
  color: #526074;
  line-height: 1.65;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.route-status-tag {
  min-width: 48px;
  margin-inline-end: 0;
  text-align: center;
}

.process-route-page :deep(.ant-table-cell) {
  overflow: hidden;
}

.process-route-page :deep(.ant-table-tbody .ant-space) {
  flex-wrap: nowrap;
}

.process-step-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.process-step-hint {
  color: #64748b;
  line-height: 1.6;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .route-detail-header,
  .process-step-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
