<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import ActionBar from '@/components/ActionBar.vue'
import {
  createOrder,
  createOrderItem,
  deleteOrder,
  fetchOrders,
  updateOrder,
  type OrderQuery,
  type OrderItemUpsertRequest,
  type OrderUpsertRequest,
} from '@/api/order'
import { orderStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { IdValue, OrderSummaryItem } from '@/types/domain'
import type { OrderStatus } from '@/types/dictionary'
import { formatDateTime } from '@/utils/date'

type OrderItemDraft = {
  key: number
  productCode: string
  productName: string
  specification: string
  color: string
  quantity: number | null
  unit: string
  requiredWidth: number | null
  requiredWeight: number | null
  remark: string
}

const router = useRouter()

const searchForm = reactive({
  orderNo: '',
  customerName: '',
  status: undefined as OrderStatus | undefined,
  deliveryDateFrom: '',
  deliveryDateTo: '',
})

const searchFields = [
  { label: '订单号', name: 'orderNo', type: 'input' as const, placeholder: '请输入订单号', width: '200px' },
  { label: '客户', name: 'customerName', type: 'input' as const, placeholder: '请输入客户名称', width: '200px' },
  {
    label: '状态',
    name: 'status',
    type: 'select' as const,
    placeholder: '全部',
    options: orderStatusOptions,
    width: '160px',
  },
  { label: '交期起', name: 'deliveryDateFrom', type: 'datetime' as const, width: '220px' },
  { label: '交期止', name: 'deliveryDateTo', type: 'datetime' as const, width: '220px' },
]

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 116 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 146 },
  { title: '下单时间', dataIndex: 'orderDate', key: 'orderDate', width: 140 },
  { title: '交期', dataIndex: 'deliveryDate', key: 'deliveryDate', width: 140 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 76 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 82 },
  { title: '执行进度', key: 'executionProgress', width: 112 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 162 },
  { title: '操作', key: 'action', width: 88 },
]

const { data, loading, pagination } = useTable<OrderSummaryItem>()

const modalOpen = ref(false)
const submitting = ref(false)
const formRef = ref()
const modalMode = ref<'create' | 'edit'>('create')
const editingOrderId = ref<IdValue>()
const itemDraftSeq = ref(0)
const orderItemDrafts = ref<OrderItemDraft[]>([])
const formModel = reactive({
  orderNo: '',
  customerName: '',
  orderDate: '',
  deliveryDate: '',
  priority: 'NORMAL',
  status: 'NEW' as OrderStatus,
  remark: '',
})

const rules = {
  orderNo: [{ required: true, message: '请输入订单号' }],
  customerName: [{ required: true, message: '请输入客户名称' }],
  orderDate: [{ required: true, message: '请选择下单时间' }],
  deliveryDate: [{ required: true, message: '请选择交期' }],
}

const actionBar = [
  { key: 'new', label: '新建订单', type: 'primary' },
]

const priorityOptions = [
  { label: '高', value: 'HIGH' },
  { label: '普通', value: 'NORMAL' },
  { label: '低', value: 'LOW' },
]

const priorityMeta: Record<string, { label: string; color: string }> = {
  HIGH: { label: '高', color: 'red' },
  NORMAL: { label: '普通', color: 'blue' },
  LOW: { label: '低', color: 'default' },
}

const createBlankItemDraft = (): OrderItemDraft => ({
  key: itemDraftSeq.value++,
  productCode: '',
  productName: '',
  specification: '',
  color: '',
  quantity: null,
  unit: 'm',
  requiredWidth: null,
  requiredWeight: null,
  remark: '',
})

const getStatusMeta = (status?: string) =>
  orderStatusOptions.find((item) => item.value === status)

const loadData = async () => {
  loading.value = true
  try {
    const query: OrderQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      orderNo: searchForm.orderNo || undefined,
      customerName: searchForm.customerName || undefined,
      status: searchForm.status,
      deliveryDateFrom: searchForm.deliveryDateFrom || undefined,
      deliveryDateTo: searchForm.deliveryDateTo || undefined,
    }
    const response = await fetchOrders(query)
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
  searchForm.orderNo = ''
  searchForm.customerName = ''
  searchForm.status = undefined
  searchForm.deliveryDateFrom = ''
  searchForm.deliveryDateTo = ''
  pagination.current = 1
  void loadData()
}

const openCreateModal = () => {
  modalMode.value = 'create'
  editingOrderId.value = undefined
  formModel.orderNo = ''
  formModel.customerName = ''
  formModel.orderDate = ''
  formModel.deliveryDate = ''
  formModel.priority = 'NORMAL'
  formModel.status = 'NEW'
  formModel.remark = ''
  orderItemDrafts.value = [createBlankItemDraft()]
  modalOpen.value = true
}

const openEditModal = (record: OrderSummaryItem) => {
  modalMode.value = 'edit'
  editingOrderId.value = record.orderId
  formModel.orderNo = record.orderNo
  formModel.customerName = record.customerName
  formModel.orderDate = record.orderDate || ''
  formModel.deliveryDate = record.deliveryDate || ''
  formModel.priority = record.priority || 'NORMAL'
  formModel.status = (record.status || 'NEW') as OrderStatus
  formModel.remark = record.remark || ''
  orderItemDrafts.value = []
  modalOpen.value = true
}

const handlePageAction = (key: string) => {
  if (key === 'new') {
    openCreateModal()
  }
}

const goToDetail = (record: OrderSummaryItem) => {
  void router.push({
    name: 'order-detail',
    params: { id: String(record.orderId) },
  })
}

const addItemDraft = () => {
  orderItemDrafts.value.push(createBlankItemDraft())
}

const removeItemDraft = (key: number) => {
  orderItemDrafts.value = orderItemDrafts.value.filter((item) => item.key !== key)
}

const normalizeItemPayload = (item: OrderItemDraft): OrderItemUpsertRequest => ({
  productCode: item.productCode.trim(),
  productName: item.productName.trim(),
  specification: item.specification.trim() || undefined,
  color: item.color.trim() || undefined,
  quantity: item.quantity,
  unit: item.unit.trim() || undefined,
  requiredWidth: item.requiredWidth,
  requiredWeight: item.requiredWeight,
  remark: item.remark.trim() || undefined,
})

const validateItemDrafts = () => {
  const invalidIndex = orderItemDrafts.value.findIndex((item) =>
    !item.productCode.trim() || !item.productName.trim() || item.quantity == null || item.quantity <= 0,
  )
  if (invalidIndex >= 0) {
    message.warning(`请完整填写第 ${invalidIndex + 1} 条订单明细的产品编码、产品名称和数量`)
    return false
  }
  return true
}

const handleDeleteOrder = (record: OrderSummaryItem) => {
  Modal.confirm({
    title: '删除订单',
    content: `确定删除订单 ${record.orderNo} 吗？已生成生产计划的订单不能删除。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteOrder(record.orderId)
      message.success('订单已删除')
      await loadData()
    },
  })
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (modalMode.value === 'create' && orderItemDrafts.value.length && !validateItemDrafts()) {
    return
  }
  submitting.value = true
  try {
    const payload: OrderUpsertRequest = {
      orderNo: formModel.orderNo.trim(),
      customerName: formModel.customerName.trim(),
      orderDate: formModel.orderDate,
      deliveryDate: formModel.deliveryDate,
      priority: formModel.priority,
      status: formModel.status,
      remark: formModel.remark.trim() || undefined,
    }
    if (modalMode.value === 'create') {
      const response = await createOrder(payload)
      const createdOrderId = response.data?.orderId
      if (createdOrderId) {
        for (const item of orderItemDrafts.value) {
          await createOrderItem(createdOrderId, normalizeItemPayload(item))
        }
      }
      message.success('订单创建成功')
    } else if (editingOrderId.value) {
      await updateOrder(editingOrderId.value, payload)
      message.success('订单更新成功')
    }
    modalOpen.value = false
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
  <TablePage
    title="订单管理"
    :columns="columns"
    :data="data"
    :loading="loading"
    :pagination="pagination"
    row-key="orderId"
    table-layout="fixed"
    class="order-list-page"
  >
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'orderNo'">
        <a-tooltip :title="record.orderNo" placement="topLeft">
          <a-button type="link" class="order-no-button" @click="goToDetail(record)">
            {{ record.orderNo }}
          </a-button>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'customerName'">
        <a-tooltip :title="record.customerName" placement="topLeft">
          <span class="single-line-cell">{{ record.customerName }}</span>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'orderDate' || column.key === 'deliveryDate'">
        <a-tooltip :title="formatDateTime(record[column.key], false)">
          <span class="date-cell">{{ formatDateTime(record[column.key], false) }}</span>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'priority'">
        <a-tag :color="priorityMeta[record.priority || '']?.color">
          {{ priorityMeta[record.priority || '']?.label || record.priority || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="getStatusMeta(record.status)?.color">
          {{ getStatusMeta(record.status)?.label || record.status || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'executionProgress'">
        <div class="progress-cell">
          <span><em>批次</em>{{ record.linkedBatchCount ?? 0 }}</span>
          <span><em>计划</em>{{ record.generatedPlanCount ?? 0 }}</span>
        </div>
      </template>
      <template v-else-if="column.key === 'remark'">
        <a-tooltip :title="record.remark || '-'">
          <div class="remark-cell">
            {{ record.remark || '-' }}
          </div>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'action'">
        <div class="action-cell">
          <a-button type="link" size="small" @click="goToDetail(record)">详情</a-button>
          <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
          <a-button type="link" size="small" danger @click="handleDeleteOrder(record)">删除</a-button>
        </div>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="modalOpen"
    :title="modalMode === 'create' ? '新建订单' : '编辑订单'"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="submitting"
    width="720px"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formModel" :rules="rules" layout="vertical">
      <div class="form-grid">
        <a-form-item label="订单号" name="orderNo">
          <a-input v-model:value="formModel.orderNo" placeholder="请输入订单号" />
        </a-form-item>
        <a-form-item label="客户名称" name="customerName">
          <a-input v-model:value="formModel.customerName" placeholder="请输入客户名称" />
        </a-form-item>
        <a-form-item label="下单时间" name="orderDate">
          <a-date-picker
            v-model:value="formModel.orderDate"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="交期" name="deliveryDate">
          <a-date-picker
            v-model:value="formModel.deliveryDate"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="优先级" name="priority">
          <a-select v-model:value="formModel.priority" :options="priorityOptions" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="formModel.status" :options="orderStatusOptions" />
        </a-form-item>
      </div>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formModel.remark" :rows="3" placeholder="请输入备注" />
      </a-form-item>

      <div v-if="modalMode === 'create'" class="item-draft-panel">
        <div class="item-draft-header">
          <div>
            <div class="item-draft-title">订单明细</div>
            <div class="item-draft-desc">可在新建订单时同步录入多个产品需求，后续仍可在订单详情页维护。</div>
          </div>
          <a-button type="primary" ghost @click="addItemDraft">添加明细</a-button>
        </div>
        <div class="item-draft-list">
          <div v-for="(item, index) in orderItemDrafts" :key="item.key" class="item-draft-row">
            <div class="item-draft-row-head">
              <span>明细 {{ index + 1 }}</span>
              <a-button v-if="orderItemDrafts.length > 1" type="link" danger @click="removeItemDraft(item.key)">移除</a-button>
            </div>
            <div class="form-grid">
              <a-form-item label="产品编码">
                <a-input v-model:value="item.productCode" placeholder="必填" />
              </a-form-item>
              <a-form-item label="产品名称">
                <a-input v-model:value="item.productName" placeholder="必填" />
              </a-form-item>
              <a-form-item label="规格">
                <a-input v-model:value="item.specification" placeholder="可选" />
              </a-form-item>
              <a-form-item label="颜色">
                <a-input v-model:value="item.color" placeholder="可选" />
              </a-form-item>
              <a-form-item label="数量">
                <a-input-number v-model:value="item.quantity" :min="0" class="full-control" placeholder="必填" />
              </a-form-item>
              <a-form-item label="单位">
                <a-input v-model:value="item.unit" placeholder="如 m、kg" />
              </a-form-item>
              <a-form-item label="要求门幅(cm)">
                <a-input-number v-model:value="item.requiredWidth" :min="0" class="full-control" placeholder="可选" />
              </a-form-item>
              <a-form-item label="要求重量(kg)">
                <a-input-number v-model:value="item.requiredWeight" :min="0" class="full-control" placeholder="可选" />
              </a-form-item>
            </div>
            <a-form-item label="明细备注">
              <a-textarea v-model:value="item.remark" :rows="2" placeholder="可选" />
            </a-form-item>
          </div>
        </div>
      </div>
    </a-form>
  </a-modal>
</template>

<style scoped>
.order-list-page :deep(.ant-table-cell) {
  overflow-wrap: normal;
  word-break: normal;
}

.order-list-page :deep(.ant-table-content) {
  overflow-x: clip !important;
}

.order-list-page :deep(.ant-table-tbody > tr > td) {
  padding-top: 12px;
  padding-bottom: 12px;
  vertical-align: middle;
}

.single-line-cell,
.date-cell {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.date-cell {
  font-size: 13px;
}

.order-no-button {
  display: block;
  overflow: hidden;
  width: 100%;
  padding-inline: 0;
  text-align: left;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.progress-cell {
  display: grid;
  gap: 4px;
  font-size: 12px;
}

.progress-cell span {
  display: grid;
  grid-template-columns: 38px 1fr;
}

.progress-cell em {
  color: #94a3b8;
  font-style: normal;
}

.action-cell {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: center;
  column-gap: 6px;
}

.action-cell :deep(.ant-btn) {
  height: 26px;
  padding-inline: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.remark-cell {
  display: -webkit-box;
  overflow: hidden;
  color: #475569;
  line-height: 1.6;
  word-break: break-word;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-draft-panel {
  margin-top: 4px;
  padding-top: 16px;
  border-top: 1px solid #edf2f7;
}

.item-draft-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.item-draft-title {
  color: #1f2937;
  font-weight: 700;
}

.item-draft-desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.item-draft-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-draft-row {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
}

.item-draft-row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #374151;
  font-weight: 600;
}

.full-control {
  width: 100%;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
