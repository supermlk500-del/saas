<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  addDictType,
  changeDictTypeStatus,
  deleteDictType,
  listDictTypes,
  updateDictType,
  type SysDictTypeForm,
  type SysDictTypeItem,
} from '@/api/system/dict'

const loading = ref(false)
const rows = ref<SysDictTypeItem[]>([])
const selectedRowKeys = ref<number[]>([])

const pager = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const query = reactive({
  dictName: '',
  dictType: '',
  status: undefined as string | undefined,
})

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
]

const columns = computed<TableColumnsType<SysDictTypeItem>>(() => [
  { title: '字典编号', dataIndex: 'dictId', key: 'dictId', width: 90 },
  { title: '字典名称', dataIndex: 'dictName', key: 'dictName', width: 180 },
  { title: '字典类型', dataIndex: 'dictType', key: 'dictType', width: 220 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 200 },
  { title: '操作', key: 'action', width: 220 },
])

const modalOpen = ref(false)
const modalLoading = ref(false)
const editingDictId = ref<number | null>(null)

const form = reactive<SysDictTypeForm>({
  dictName: '',
  dictType: '',
  status: '0',
  remark: '',
})

const modalTitle = computed(() => (editingDictId.value ? '修改字典类型' : '新增字典类型'))

const fetchDictTypeList = async () => {
  loading.value = true
  try {
    const res = await listDictTypes({
      pageNum: pager.current,
      pageSize: pager.pageSize,
      dictName: query.dictName || undefined,
      dictType: query.dictType || undefined,
      status: query.status,
    })
    rows.value = res.rows ?? []
    pager.total = res.total ?? 0
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  pager.current = 1
  await fetchDictTypeList()
}

const handleReset = async () => {
  query.dictName = ''
  query.dictType = ''
  query.status = undefined
  pager.current = 1
  await fetchDictTypeList()
}

const handleTableChange = async (page: number, pageSize: number) => {
  pager.current = page
  pager.pageSize = pageSize
  await fetchDictTypeList()
}

const resetForm = () => {
  form.dictName = ''
  form.dictType = ''
  form.status = '0'
  form.remark = ''
}

const openCreateModal = () => {
  editingDictId.value = null
  resetForm()
  modalOpen.value = true
}

const openEditModal = (record: SysDictTypeItem) => {
  editingDictId.value = record.dictId
  form.dictName = record.dictName || ''
  form.dictType = record.dictType || ''
  form.status = record.status || '0'
  form.remark = record.remark || ''
  modalOpen.value = true
}

const validateForm = () => {
  if (!form.dictName.trim()) {
    message.warning('请输入字典名称')
    return false
  }
  if (!form.dictType.trim()) {
    message.warning('请输入字典类型')
    return false
  }
  return true
}

const submitModal = async () => {
  if (!validateForm()) return

  modalLoading.value = true
  try {
    const payload: SysDictTypeForm = {
      dictId: editingDictId.value ?? undefined,
      dictName: form.dictName.trim(),
      dictType: form.dictType.trim(),
      status: form.status,
      remark: form.remark?.trim(),
    }

    if (editingDictId.value) {
      await updateDictType(payload)
      message.success('修改成功')
    } else {
      await addDictType(payload)
      message.success('新增成功')
    }

    modalOpen.value = false
    await fetchDictTypeList()
  } finally {
    modalLoading.value = false
  }
}

const handleDelete = (record: SysDictTypeItem) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除字典类型「${record.dictName}」吗？`,
    onOk: async () => {
      await deleteDictType(record.dictId)
      message.success('删除成功')
      await fetchDictTypeList()
    },
  })
}

const handleStatusChange = async (record: SysDictTypeItem, checked: boolean) => {
  const nextStatus = checked ? '0' : '1'
  try {
    await changeDictTypeStatus(record.dictId, nextStatus)
    record.status = nextStatus
    message.success('状态已更新')
  } catch {
    record.status = nextStatus === '0' ? '1' : '0'
  }
}

const selectedLabel = computed(() => (selectedRowKeys.value.length ? `已选 ${selectedRowKeys.value.length} 项` : ''))
const selectedOne = computed(() => rows.value.find((item) => item.dictId === selectedRowKeys.value[0]))

const handleSelectChange = (keys: (string | number)[]) => {
  selectedRowKeys.value = keys as number[]
}

const handleToolbarEdit = () => {
  if (!selectedOne.value) return
  openEditModal(selectedOne.value)
}

const handleBatchDelete = () => {
  if (!selectedRowKeys.value.length) return
  Modal.confirm({
    title: '批量删除',
    content: `确定删除已选中的 ${selectedRowKeys.value.length} 个字典类型吗？`,
    onOk: async () => {
      await Promise.all(selectedRowKeys.value.map((id) => deleteDictType(id)))
      selectedRowKeys.value = []
      message.success('删除成功')
      await fetchDictTypeList()
    },
  })
}

onMounted(fetchDictTypeList)
</script>

<template>
  <a-space direction="vertical" size="middle" style="width: 100%">
    <a-card :bordered="false">
      <a-form layout="inline">
        <a-form-item label="字典名称">
          <a-input v-model:value="query.dictName" placeholder="请输入字典名称" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="字典类型">
          <a-input v-model:value="query.dictType" placeholder="请输入字典类型" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="query.status"
            placeholder="字典状态"
            allow-clear
            :options="statusOptions"
            style="width: 140px"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :bordered="false">
      <a-space style="margin-bottom: 12px">
        <a-button type="primary" @click="openCreateModal">新增</a-button>
        <a-button :disabled="selectedRowKeys.length !== 1" @click="handleToolbarEdit">修改</a-button>
        <a-button danger :disabled="selectedRowKeys.length === 0" @click="handleBatchDelete">删除</a-button>
        <a-button>导出</a-button>
        <span class="selected-tip">{{ selectedLabel }}</span>
      </a-space>

      <a-table
        row-key="dictId"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="{
          current: pager.current,
          pageSize: pager.pageSize,
          total: pager.total,
          showSizeChanger: true,
          showTotal: (total: number) => `共 ${total} 条`,
          onChange: handleTableChange,
        }"
        :row-selection="{
          selectedRowKeys,
          onChange: handleSelectChange,
        }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-switch
              :checked="record.status === '0'"
              checked-children="正常"
              un-checked-children="停用"
              @change="(checked: boolean) => handleStatusChange(record, checked)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" @click="openEditModal(record)">修改</a-button>
              <a-button type="link" danger @click="handleDelete(record)">删除</a-button>
              <a-button type="link">字典数据</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      width="640px"
      @ok="submitModal"
    >
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="字典名称" required>
              <a-input v-model:value="form.dictName" placeholder="请输入字典名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="字典类型" required>
              <a-input v-model:value="form.dictType" placeholder="请输入字典类型" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="状态">
          <a-select v-model:value="form.status" :options="statusOptions" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="3" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-space>
</template>

<style scoped>
.selected-tip {
  color: #64748b;
  font-size: 13px;
}
</style>
