<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'
import ActionBar from '@/components/ActionBar.vue'
import { fetchOrders, type OrderItem } from '@/api/order'
import { useTable } from '@/hooks/useTable'

const searchForm = reactive({
  orderNo: '',
  customer: '',
  status: undefined as string | undefined,
})

const searchFields = [
  { label: '订单号', name: 'orderNo', type: 'input', placeholder: '请输入订单号', width: '200px' },
  { label: '客户', name: 'customer', type: 'input', placeholder: '请输入客户名称', width: '200px' },
  {
    label: '状态',
    name: 'status',
    type: 'select',
    placeholder: '请选择状态',
    width: '150px',
    options: [
      { label: '待排产', value: '待排产' },
      { label: '进行中', value: '进行中' },
      { label: '已超期', value: '已超期' },
      { label: '已完成', value: '已完成' },
    ],
  },
]

const columns = [
  { title: '订单号', dataIndex: 'no', key: 'no', width: '150px' },
  { title: '布种规格', dataIndex: 'fabricType', key: 'fabricType' },
  { title: '客户', dataIndex: 'customer', key: 'customer' },
  { title: '目标数量 (m)', dataIndex: 'targetQty', key: 'targetQty', align: 'right' },
  { title: '交期', dataIndex: 'dueDate', key: 'dueDate' },
  { title: '状态', dataIndex: 'status', key: 'status', width: '120px' },
]

const { data, loading, pagination } = useTable<OrderItem>()

const getStatusColor = (status: string) => {
  switch (status) {
    case '待排产': return 'orange'
    case '进行中': return 'blue'
    case '已超期': return 'red'
    case '已完成': return 'green'
    default: return 'default'
  }
}

const modalOpen = ref(false)
const formRef = ref()
const formModel = reactive({
  orderNo: '',
  customer: '',
  dueDate: undefined as string | undefined,
})
const rules = {
  orderNo: [{ required: true, message: '请输入订单号' }],
  customer: [{ required: true, message: '请输入客户名称' }],
  dueDate: [{ required: true, message: '请选择交期' }],
}

const formFields = [
  { label: '订单号', name: 'orderNo', type: 'input', placeholder: '请输入订单号' },
  { label: '客户', name: 'customer', type: 'input', placeholder: '请输入客户名称' },
  { label: '交期', name: 'dueDate', type: 'date' },
]

const actionBar = [
  { key: 'new', label: '新建订单', type: 'primary' },
  { key: 'export', label: '导出' },
]

const handlePageAction = (key: string) => {
  if (key === 'new') {
    openModal()
  }
}

const openModal = () => {
  modalOpen.value = true
}

const handleOk = async () => {
  try {
    await formRef.value?.validate()
    modalOpen.value = false
  } catch (error) {
    return
  }
}

const loadData = async () => {
  loading.value = true
  data.value = await fetchOrders({
    orderNo: searchForm.orderNo,
    customer: searchForm.customer,
    status: searchForm.status,
  })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.orderNo = ''
  searchForm.customer = ''
  searchForm.status = undefined
  loadData()
}

const getRowClassName = (record: OrderItem) => {
  return record.qcAlert ? 'qc-alert-row' : ''
}

onMounted(loadData)
</script>

<template>
  <TablePage
    title="订单管理"
    :columns="columns"
    :data="data"
    :loading="loading"
    :pagination="pagination"
    :row-class-name="getRowClassName"
  >
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'fabricType'">
        <a-space>
          <div
            class="swatch-icon"
            :style="{ backgroundColor: record.swatchColor }"
          ></div>
          {{ record.fabricType }}
        </a-space>
      </template>
      <template v-else-if="column.key === 'targetQty'">
        {{ record.targetQty.toLocaleString() }} m
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="getStatusColor(record.status)">
          {{ record.status }}
        </a-tag>
      </template>
    </template>
  </TablePage>

  <FormModal
    v-model:open="modalOpen"
    title="新建订单"
    :model="formModel"
    :rules="rules"
    :fields="formFields"
    @ok="handleOk"
  />
</template>

<style scoped>
.swatch-icon {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

:deep(.qc-alert-row) {
  background-color: #fff1f0;
}

:deep(.qc-alert-row:hover > td) {
  background-color: #fff1f0 !important;
}
</style>
