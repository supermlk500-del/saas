<script setup lang="ts">
import { reactive, onMounted, ref } from 'vue'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import FormModal from '@/components/FormModal.vue'
import { useTable } from '@/hooks/useTable'
import { PlusOutlined } from '@ant-design/icons-vue'

type MachineItem = {
  key: string
  code: string
  name: string
  type: string
  specs: string
  workshop: string
  status: 'Running' | 'Error' | 'Maintenance'
}

const searchForm = reactive({
  keyword: '',
  workshop: undefined as string | undefined,
  machineType: undefined as string | undefined,
})

const searchFields = [
  { label: '关键词', name: 'keyword', type: 'input' as const, placeholder: '设备编号/名称', width: '200px' },
  {
    label: '车间',
    name: 'workshop',
    type: 'select' as const,
    placeholder: '选择车间',
    options: [
      { label: '一号织造车间', value: '一号织造车间' },
      { label: '二号准备车间', value: '二号准备车间' },
      { label: '三号整理车间', value: '三号整理车间' },
    ],
  },
  {
    label: '机器类型',
    name: 'machineType',
    type: 'select' as const,
    placeholder: '选择类型',
    options: [
      { label: '经轴机 (Warping Machine)', value: '经轴机' },
      { label: '喷气织机 (Air-jet Loom)', value: '喷气织机' },
      { label: '剑杆织机 (Rapier Loom)', value: '剑杆织机' },
      { label: '整经机 (Beaming Machine)', value: '整经机' },
    ],
  },
]

const columns = [
  { title: '设备编号', dataIndex: 'code', key: 'code', width: '120px' },
  { title: '设备名称', dataIndex: 'name', key: 'name' },
  { title: '设备类型', dataIndex: 'type', key: 'type' },
  { title: '核心规格', dataIndex: 'specs', key: 'specs' },
  { title: '车间', dataIndex: 'workshop', key: 'workshop' },
  { title: '状态', dataIndex: 'status', key: 'status', width: '120px' },
  { title: '操作', key: 'action', width: '150px' },
]

const { data, loading, pagination } = useTable<MachineItem>()

const allData: MachineItem[] = [
  { key: '1', code: 'W-001', name: '高速整经机', type: '经轴机', specs: '1200 RPM, 180cm Width, Auto-tension', workshop: '二号准备车间', status: 'Running' },
  { key: '2', code: 'L-102', name: '津田驹喷气织机', type: '喷气织机', specs: '800 RPM, 200cm Width, Air-jet', workshop: '一号织造车间', status: 'Running' },
  { key: '3', code: 'L-105', name: '必佳乐剑杆织机', type: '剑杆织机', specs: '550 RPM, 220cm Width, Rapier', workshop: '一号织造车间', status: 'Error' },
  { key: '4', code: 'B-201', name: '分条整经机', type: '整经机', specs: '600 RPM, 240cm Width, Sizing Integration', workshop: '二号准备车间', status: 'Maintenance' },
  { key: '5', code: 'L-108', name: '丰田喷气织机', type: '喷气织机', specs: '850 RPM, 190cm Width, Electronic Let-off', workshop: '一号织造车间', status: 'Running' },
  { key: '6', code: 'W-003', name: '超宽幅整经机', type: '经轴机', specs: '1000 RPM, 360cm Width, Multi-yarn', workshop: '二号准备车间', status: 'Running' },
]

const loadData = () => {
  loading.value = true
  // 模拟 API 请求
  setTimeout(() => {
    data.value = allData.filter((item) => {
      const hitKeyword = !searchForm.keyword || item.code.includes(searchForm.keyword) || item.name.includes(searchForm.keyword)
      const hitWorkshop = !searchForm.workshop || item.workshop === searchForm.workshop
      const hitType = !searchForm.machineType || item.type === searchForm.machineType
      return hitKeyword && hitWorkshop && hitType
    })
    pagination.total = data.value.length
    loading.value = false
  }, 300)
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.workshop = undefined
  searchForm.machineType = undefined
  loadData()
}

onMounted(loadData)

const getStatusColor = (status: string) => {
  switch (status) {
    case 'Running': return 'green'
    case 'Error': return 'red'
    case 'Maintenance': return 'default'
    default: return 'blue'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'Running': return '运行中'
    case 'Error': return '故障'
    case 'Maintenance': return '维护中'
    default: return status
  }
}

// Modal related
const modalOpen = ref(false)
const formModel = reactive({
  code: '',
  name: '',
  type: undefined as string | undefined,
  rpm: '',
  width: '',
  extra: '',
  workshop: undefined as string | undefined,
})

const formFields = [
  { label: '设备编号', name: 'code', type: 'input' as const, placeholder: '请输入设备编号' },
  { label: '设备名称', name: 'name', type: 'input' as const, placeholder: '请输入设备名称' },
  {
    label: '设备类型',
    name: 'type',
    type: 'select' as const,
    placeholder: '请选择设备类型',
    options: searchFields[2]?.options ?? [],
  },
  { label: '运行转速 (RPM)', name: 'rpm', type: 'input' as const, placeholder: '例如：800' },
  { label: '加工门幅 (cm)', name: 'width', type: 'input' as const, placeholder: '例如：200' },
  { label: '技术特征/备注', name: 'extra', type: 'input' as const, placeholder: '例如：Air-jet, Auto-tension' },
  {
    label: '车间',
    name: 'workshop',
    type: 'select' as const,
    placeholder: '请选择车间',
    options: searchFields[1]?.options ?? [],
  },
]

const handleAdd = () => {
  modalOpen.value = true
  // Reset form
  Object.assign(formModel, {
    code: '',
    name: '',
    type: undefined,
    rpm: '',
    width: '',
    extra: '',
    workshop: undefined,
  })
}

const handleOk = () => {
  console.log('Form data:', formModel)
  // Combine specs for table display
  const combinedSpecs = [
    formModel.rpm ? `${formModel.rpm} RPM` : '',
    formModel.width ? `${formModel.width}cm Width` : '',
    formModel.extra,
  ]
    .filter(Boolean)
    .join(', ')

  // Simulate adding
  const newItem: MachineItem = {
    key: String(allData.length + 1),
    code: formModel.code,
    name: formModel.name,
    type: formModel.type || '',
    specs: combinedSpecs,
    workshop: formModel.workshop || '',
    status: 'Running',
  }
  allData.unshift(newItem)
  loadData()
  modalOpen.value = false
}
</script>

<template>
  <TablePage title="机器管理" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #actions>
      <a-button type="primary" class="add-btn" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        新增设备
      </a-button>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'status'">
        <a-tag :color="getStatusColor(record.status)">
          {{ getStatusText(record.status) }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a-button type="link" size="small">详情</a-button>
          <a-button type="link" size="small">编辑</a-button>
        </a-space>
      </template>
    </template>
  </TablePage>

  <FormModal
    v-model:open="modalOpen"
    title="新增设备"
    :model="formModel"
    :fields="formFields"
    @ok="handleOk"
  />
</template>

<style scoped>
.add-btn {
  background-color: #ff7a45;
  border-color: #ff7a45;
}

.add-btn:hover {
  background-color: #ff9c6e;
  border-color: #ff9c6e;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #ffffff;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f1f5f9;
}
</style>
