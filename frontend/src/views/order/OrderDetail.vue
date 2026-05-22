<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchOrderDetail, type OrderDetailItem } from '@/api/order/orderDetail'

const route = useRoute()
const router = useRouter()

const orderId = route.params.id as string
const data = ref<OrderDetailItem[]>([])
const loading = ref(false)

const columns = [
  { title: '行号', dataIndex: 'itemNo', key: 'itemNo' },
  { title: '款式名称', dataIndex: 'productName', key: 'productName' },
  { title: '尺码/颜色', dataIndex: 'spec', key: 'spec' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
  { title: '单位', dataIndex: 'unit', key: 'unit' },
  { title: '交期', dataIndex: 'dueDate', key: 'dueDate' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const loadData = async () => {
  loading.value = true
  data.value = await fetchOrderDetail(orderId)
  loading.value = false
}

const goBack = () => {
  router.push({ name: 'order-list' })
}

onMounted(loadData)
</script>

<template>
  <a-space direction="vertical" size="large" style="width: 100%">
    <a-page-header :title="'订单详情 ' + orderId" @back="goBack" />
    <a-table :columns="columns" :data-source="data" :loading="loading" bordered />
  </a-space>
</template>
