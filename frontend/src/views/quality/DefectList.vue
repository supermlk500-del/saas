<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { fetchDefects, type DefectQuery, type DefectItem } from '@/api/quality/defect'
import { enabledStatusOptions } from '@/constants/dictionaries'
import { useTable } from '@/hooks/useTable'

const searchForm = reactive({
  qcItemCode: '',
  qcItemName: '',
  qcType: '',
})

const searchFields = [
  { label: '缺陷编码', name: 'qcItemCode', type: 'input' as const, width: '160px' },
  { label: '缺陷名称', name: 'qcItemName', type: 'input' as const, width: '180px' },
  { label: '分类/类型', name: 'qcType', type: 'input' as const, width: '160px' },
]

const columns = [
  { title: '缺陷编码', dataIndex: 'qcItemCode', key: 'qcItemCode', width: 140 },
  { title: '缺陷名称', dataIndex: 'qcItemName', key: 'qcItemName', width: 180 },
  { title: '分类/类型', dataIndex: 'qcType', key: 'qcType', width: 160 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 100 },
  { title: '标准下限', dataIndex: 'standardMin', key: 'standardMin', width: 100 },
  { title: '标准上限', dataIndex: 'standardMax', key: 'standardMax', width: 100 },
  { title: '状态', dataIndex: 'isActive', key: 'isActive', width: 100 },
  { title: '说明', dataIndex: 'description', key: 'description' },
]

const { data, loading, pagination } = useTable<DefectItem>()

const loadData = async () => {
  loading.value = true
  try {
    const query: DefectQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      qcItemCode: searchForm.qcItemCode || undefined,
      qcItemName: searchForm.qcItemName || undefined,
      qcType: searchForm.qcType || undefined,
    }
    const response = await fetchDefects(query)
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
  searchForm.qcItemCode = ''
  searchForm.qcItemName = ''
  searchForm.qcType = ''
  pagination.current = 1
  void loadData()
}

const getStatusMeta = (value: number) =>
  enabledStatusOptions.find((item) => item.value === value)

onMounted(() => {
  void loadData()
})
</script>

<template>
  <TablePage title="缺陷代码" :columns="columns" :data="data" :loading="loading" :pagination="pagination">
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'qcType'">
        {{ record.qcType || '-' }}
      </template>
      <template v-else-if="column.key === 'unit'">
        {{ record.unit || '-' }}
      </template>
      <template v-else-if="column.key === 'standardMin'">
        {{ record.standardMin ?? '-' }}
      </template>
      <template v-else-if="column.key === 'standardMax'">
        {{ record.standardMax ?? '-' }}
      </template>
      <template v-else-if="column.key === 'isActive'">
        <a-tag :color="getStatusMeta(record.isActive)?.color">{{ getStatusMeta(record.isActive)?.label || record.isActive }}</a-tag>
      </template>
      <template v-else-if="column.key === 'description'">
        {{ record.description || '-' }}
      </template>
    </template>
  </TablePage>
</template>
