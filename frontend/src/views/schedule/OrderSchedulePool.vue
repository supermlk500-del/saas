<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchOrderSchedulePool } from '@/api/order'
import { fetchOrderDetail } from '@/api/order/orderDetail'
import { fetchProcesses } from '@/api/master/process'
import { createPlan, type ProductionPlanCreateRequest } from '@/api/plan/plan'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { orderStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type {
  OrderBatchLinkItem,
  OrderDetailAggregate,
  OrderLineItem,
  OrderSchedulePoolItem,
  ProcessRouteItem,
} from '@/types/domain'
import type { OrderStatus } from '@/types/dictionary'

type ReadyForScheduleFilterValue = 'true' | 'false'

type OrderSchedulePoolRow = OrderSchedulePoolItem & {
  rowKey: string
  productSummary: string
  recommendedRouteText: string
  machineGroupText: string
  canSchedule: boolean
}

type OrderPlanCreateForm = {
  orderId?: number | string
  orderNo: string
  customerName: string
  orderItemId?: number | string
  batchId?: number | string
  routeId?: number | string
  productSummary: string
  planStartTime: string
  planEndTime: string
  remark: string
}

const searchForm = reactive({
  orderNo: '',
  customerName: '',
  status: undefined as OrderStatus | undefined,
  priority: undefined as string | undefined,
  readyForSchedule: undefined as ReadyForScheduleFilterValue | undefined,
})

const priorityOptions = [
  { label: '高', value: 'HIGH' },
  { label: '普通', value: 'NORMAL' },
  { label: '低', value: 'LOW' },
]

const readyForScheduleOptions = [
  { label: '可排产', value: 'true' },
  { label: '暂不可排产', value: 'false' },
]

const searchFields = [
  { label: '订单号', name: 'orderNo', type: 'input' as const, placeholder: '请输入订单号', width: '180px' },
  { label: '客户', name: 'customerName', type: 'input' as const, placeholder: '请输入客户名称', width: '180px' },
  {
    label: '状态',
    name: 'status',
    type: 'select' as const,
    placeholder: '全部',
    options: orderStatusOptions,
    width: '160px',
  },
  {
    label: '优先级',
    name: 'priority',
    type: 'select' as const,
    placeholder: '全部',
    options: priorityOptions,
    width: '140px',
  },
  {
    label: '可排产',
    name: 'readyForSchedule',
    type: 'select' as const,
    placeholder: '全部',
    options: readyForScheduleOptions,
    width: '140px',
  },
]

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 180 },
  { title: '产品/规格', dataIndex: 'productSummary', key: 'productSummary', width: 240 },
  { title: '交期', dataIndex: 'deliveryDate', key: 'deliveryDate', width: 180 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 100 },
  { title: '已绑定批次', dataIndex: 'linkedBatchCount', key: 'linkedBatchCount', width: 120, align: 'right' as const },
  { title: '推荐工艺路线', dataIndex: 'recommendedRouteText', key: 'recommendedRouteText', width: 200 },
  { title: '推荐设备组', dataIndex: 'machineGroupText', key: 'machineGroupText', width: 220 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '可排产', dataIndex: 'canSchedule', key: 'canSchedule', width: 120 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

const { data, loading, pagination } = useTable<OrderSchedulePoolRow>()
const routeOptions = ref<ProcessRouteItem[]>([])
const currentOrderDetail = ref<OrderDetailAggregate | null>(null)
const orderItemOptions = ref<OrderLineItem[]>([])
const batchOptions = ref<OrderBatchLinkItem[]>([])
const orderDetailCache = new Map<string, OrderDetailAggregate>()
const modalOpen = ref(false)
const submitting = ref(false)
const formRef = ref()

const formModel = reactive<OrderPlanCreateForm>({
  orderNo: '',
  customerName: '',
  productSummary: '',
  planStartTime: '',
  planEndTime: '',
  remark: '',
})

const rules = {
  orderItemId: [{ required: true, message: '请选择订单明细' }],
  batchId: [{ required: true, message: '请选择关联批次' }],
  routeId: [{ required: true, message: '请选择工艺路线' }],
  planStartTime: [{ required: true, message: '请选择计划开始时间' }],
}

const orderItemSelectOptions = computed(() =>
  orderItemOptions.value.map((item) => ({
    label: [item.productCode || item.productName, item.specification, item.color].filter(Boolean).join(' / '),
    value: item.orderItemId,
  })),
)

const linkedBatchOptions = computed(() =>
  batchOptions.value.map((item) => ({
    label: `${item.batchNo || item.batchId} / ${item.supplier || '未知供应商'}`,
    value: item.batchId,
  })),
)

const routeSelectOptions = computed(() => {
  if (currentOrderDetail.value?.routeSummary?.length) {
    return currentOrderDetail.value.routeSummary.map((item) => ({
      label: item.routeName,
      value: item.routeId,
    }))
  }

  return routeOptions.value.map((item) => ({
    label: item.routeName,
    value: item.routeId,
  }))
})

const getStatusMeta = (status?: string) =>
  orderStatusOptions.find((item) => item.value === status)

const buildRowFromSchedulePool = (item: OrderSchedulePoolItem): OrderSchedulePoolRow => ({
  ...item,
  rowKey: `${item.orderId}-${item.orderItemId ?? 'order'}`,
  productSummary: [item.productName, item.specification].filter(Boolean).join(' / ') || '-',
  recommendedRouteText: item.recommendedRouteName || '-',
  machineGroupText: item.recommendedMachines?.join(' / ') || '-',
  canSchedule: item.readyForSchedule ?? false,
})

const loadOrderDetail = async (orderId: number | string) => {
  const cacheKey = String(orderId)
  const cached = orderDetailCache.get(cacheKey)
  if (cached) {
    return cached
  }

  const detail = await fetchOrderDetail(orderId)
  orderDetailCache.set(cacheKey, detail)
  return detail
}

const loadData = async () => {
  loading.value = true
  try {
    const response = await fetchOrderSchedulePool({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      orderNo: searchForm.orderNo || undefined,
      customerName: searchForm.customerName || undefined,
      status: searchForm.status,
      priority: searchForm.priority || undefined,
      readyForSchedule:
        searchForm.readyForSchedule === undefined
          ? undefined
          : searchForm.readyForSchedule === 'true',
    })

    data.value = response.list.map(buildRowFromSchedulePool)
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
  searchForm.orderNo = ''
  searchForm.customerName = ''
  searchForm.status = undefined
  searchForm.priority = undefined
  searchForm.readyForSchedule = undefined
  pagination.current = 1
  void loadData()
}

const loadRouteOptions = async () => {
  const response = await fetchProcesses({
    pageNum: 1,
    pageSize: 200,
    isActive: 1,
  })
  routeOptions.value = response.list
}

const resetForm = () => {
  formModel.orderId = undefined
  formModel.orderNo = ''
  formModel.customerName = ''
  formModel.orderItemId = undefined
  formModel.batchId = undefined
  formModel.routeId = undefined
  formModel.productSummary = ''
  formModel.planStartTime = ''
  formModel.planEndTime = ''
  formModel.remark = ''
  currentOrderDetail.value = null
  orderItemOptions.value = []
  batchOptions.value = []
}

const handleOrderItemChange = (value?: number | string) => {
  formModel.orderItemId = value
  const selectedItem = orderItemOptions.value.find((item) => String(item.orderItemId) === String(value))
  formModel.productSummary = [selectedItem?.productCode || selectedItem?.productName, selectedItem?.specification, selectedItem?.color]
    .filter(Boolean)
    .join(' / ')
  batchOptions.value = (currentOrderDetail.value?.linkedBatches ?? []).filter((item) =>
    !item.orderItemId || String(item.orderItemId) === String(value),
  )
  if (!batchOptions.value.find((item) => String(item.batchId) === String(formModel.batchId))) {
    formModel.batchId = batchOptions.value[0]?.batchId
  }
}

const openCreatePlanModal = async (record: OrderSchedulePoolRow) => {
  resetForm()
  formModel.orderId = record.orderId
  formModel.orderNo = record.orderNo
  formModel.customerName = record.customerName
  formModel.remark = ''
  modalOpen.value = true

  try {
    if (!routeOptions.value.length) {
      await loadRouteOptions()
    }

    currentOrderDetail.value = await loadOrderDetail(record.orderId)
    orderItemOptions.value = currentOrderDetail.value.items ?? []
    batchOptions.value = currentOrderDetail.value.linkedBatches ?? []
    formModel.orderItemId =
      orderItemOptions.value.find((item) => String(item.orderItemId) === String(record.orderItemId))?.orderItemId
      ?? orderItemOptions.value[0]?.orderItemId
    handleOrderItemChange(formModel.orderItemId)
    formModel.routeId =
      currentOrderDetail.value.routeSummary?.find((item) => String(item.routeId) === String(record.recommendedRouteId))?.routeId
      ?? record.recommendedRouteId
      ?? currentOrderDetail.value.routeSummary?.[0]?.routeId
      ?? routeOptions.value.find((item) => String(item.routeId) === String(record.recommendedRouteId))?.routeId
      ?? routeOptions.value[0]?.routeId
  } catch (error) {
    modalOpen.value = false
    message.error('订单详情加载失败，请稍后重试')
  }
}

const handleCreatePlan = async () => {
  await formRef.value?.validate()

  submitting.value = true
  try {
    if (
      formModel.orderId === undefined ||
      formModel.orderItemId === undefined ||
      formModel.batchId === undefined ||
      formModel.routeId === undefined
    ) {
      return
    }

    const payload: ProductionPlanCreateRequest = {
      orderId: formModel.orderId,
      orderItemId: formModel.orderItemId,
      batchId: formModel.batchId,
      routeId: formModel.routeId,
      planStartTime: formModel.planStartTime,
      planEndTime: formModel.planEndTime || undefined,
      remark: formModel.remark.trim() || undefined,
    }
    await createPlan(payload)
    modalOpen.value = false
    orderDetailCache.delete(String(formModel.orderId))
    message.success('订单驱动生产计划创建成功')
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
  <TablePage title="订单排产池" :columns="columns" :data="data" :loading="loading" :pagination="pagination" row-key="rowKey">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'status'">
        <a-tag :color="getStatusMeta(record.status)?.color">
          {{ getStatusMeta(record.status)?.label || record.status || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'linkedBatchCount'">
        {{ record.linkedBatchCount ?? 0 }}
      </template>
      <template v-else-if="column.key === 'recommendedRouteText'">
        {{ record.recommendedRouteText }}
      </template>
      <template v-else-if="column.key === 'machineGroupText'">
        {{ record.machineGroupText }}
      </template>
      <template v-else-if="column.key === 'canSchedule'">
        <a-tag :color="record.canSchedule ? 'success' : 'default'">
          {{ record.canSchedule ? '是' : '否' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-button type="link" :disabled="!record.canSchedule" @click="openCreatePlanModal(record)">创建计划</a-button>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="modalOpen"
    title="创建订单驱动计划"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="submitting"
    width="760px"
    @ok="handleCreatePlan"
  >
    <a-form ref="formRef" :model="formModel" :rules="rules" layout="vertical">
      <div class="form-grid">
        <a-form-item label="订单号">
          <a-input :value="formModel.orderNo" disabled />
        </a-form-item>
        <a-form-item label="客户">
          <a-input :value="formModel.customerName" disabled />
        </a-form-item>
        <a-form-item label="订单明细" name="orderItemId">
          <a-select
            v-model:value="formModel.orderItemId"
            :options="orderItemSelectOptions"
            placeholder="请选择订单明细"
            show-search
            option-filter-prop="label"
            @change="handleOrderItemChange"
          />
        </a-form-item>
        <a-form-item label="关联批次" name="batchId">
          <a-select
            v-model:value="formModel.batchId"
            :options="linkedBatchOptions"
            placeholder="请选择关联批次"
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
        <a-form-item label="推荐工艺路线" name="routeId">
          <a-select
            v-model:value="formModel.routeId"
            :options="routeSelectOptions"
            placeholder="请选择工艺路线"
          />
        </a-form-item>
        <a-form-item label="产品/规格">
          <a-input :value="formModel.productSummary || '待选择订单明细'" disabled />
        </a-form-item>
        <a-form-item label="计划开始时间" name="planStartTime">
          <a-date-picker
            v-model:value="formModel.planStartTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
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
