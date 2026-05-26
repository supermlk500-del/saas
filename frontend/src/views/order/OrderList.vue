<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import ActionBar from '@/components/ActionBar.vue'
import {
  createOrder,
  fetchOrders,
  type OrderQuery,
  type OrderUpsertRequest,
} from '@/api/order'
import { orderStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'
import type { OrderSummaryItem } from '@/types/domain'
import type { OrderStatus } from '@/types/dictionary'

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
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 220 },
  { title: '下单时间', dataIndex: 'orderDate', key: 'orderDate', width: 180 },
  { title: '交期', dataIndex: 'deliveryDate', key: 'deliveryDate', width: 180 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '关联批次', dataIndex: 'linkedBatchCount', key: 'linkedBatchCount', width: 120, align: 'right' as const },
  { title: '已生成计划', dataIndex: 'generatedPlanCount', key: 'generatedPlanCount', width: 120, align: 'right' as const },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 280 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const },
]

const { data, loading, pagination } = useTable<OrderSummaryItem>()

const modalOpen = ref(false)
const submitting = ref(false)
const formRef = ref()
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
  formModel.orderNo = ''
  formModel.customerName = ''
  formModel.orderDate = ''
  formModel.deliveryDate = ''
  formModel.priority = 'NORMAL'
  formModel.status = 'NEW'
  formModel.remark = ''
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

const handleSubmit = async () => {
  await formRef.value?.validate()
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
    await createOrder(payload)
    modalOpen.value = false
    message.success('订单创建成功')
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
    :scroll="{ x: 1700 }"
  >
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'orderNo'">
        <a-button type="link" @click="goToDetail(record)">
          {{ record.orderNo }}
        </a-button>
      </template>
      <template v-else-if="column.key === 'priority'">
        {{ record.priority || '-' }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="getStatusMeta(record.status)?.color">
          {{ getStatusMeta(record.status)?.label || record.status || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'linkedBatchCount'">
        {{ record.linkedBatchCount ?? 0 }}
      </template>
      <template v-else-if="column.key === 'generatedPlanCount'">
        {{ record.generatedPlanCount ?? 0 }}
      </template>
      <template v-else-if="column.key === 'remark'">
        <a-tooltip :title="record.remark || '-'">
          <div class="remark-cell">
            {{ record.remark || '-' }}
          </div>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a-button type="link" @click="goToDetail(record)">详情</a-button>
        </a-space>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="modalOpen"
    title="新建订单"
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
    </a-form>
  </a-modal>
</template>

<style scoped>
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

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
