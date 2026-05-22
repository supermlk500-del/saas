<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import { addConfig, deleteConfig, listConfigs, updateConfig, type SysConfigForm, type SysConfigItem } from '@/api/system/config'

const loading = ref(false)
const rows = ref<SysConfigItem[]>([])
const selectedRowKeys = ref<number[]>([])

const pager = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const query = reactive({
  configName: '',
  configKey: '',
  configType: undefined as string | undefined,
})

const configTypeOptions = [
  { label: '是', value: 'Y' },
  { label: '否', value: 'N' },
]

const columns = computed<TableColumnsType<SysConfigItem>>(() => [
  { title: '参数编号', dataIndex: 'configId', key: 'configId', width: 90 },
  { title: '参数名称', dataIndex: 'configName', key: 'configName', width: 180 },
  { title: '参数键名', dataIndex: 'configKey', key: 'configKey', width: 220 },
  { title: '参数键值', dataIndex: 'configValue', key: 'configValue', width: 180 },
  { title: '系统内置', dataIndex: 'configType', key: 'configType', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 200 },
  { title: '操作', key: 'action', width: 200 },
])

const modalOpen = ref(false)
const modalLoading = ref(false)
const editingConfigId = ref<number | null>(null)

const form = reactive<SysConfigForm>({
  configName: '',
  configKey: '',
  configValue: '',
  configType: 'N',
  remark: '',
})

const modalTitle = computed(() => (editingConfigId.value ? '修改参数' : '新增参数'))

const fetchConfigList = async () => {
  loading.value = true
  try {
    const res = await listConfigs({
      pageNum: pager.current,
      pageSize: pager.pageSize,
      configName: query.configName || undefined,
      configKey: query.configKey || undefined,
      configType: query.configType,
    })
    rows.value = res.rows ?? []
    pager.total = res.total ?? 0
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  pager.current = 1
  await fetchConfigList()
}

const handleReset = async () => {
  query.configName = ''
  query.configKey = ''
  query.configType = undefined
  pager.current = 1
  await fetchConfigList()
}

const handleTableChange = async (page: number, pageSize: number) => {
  pager.current = page
  pager.pageSize = pageSize
  await fetchConfigList()
}

const resetForm = () => {
  form.configName = ''
  form.configKey = ''
  form.configValue = ''
  form.configType = 'N'
  form.remark = ''
}

const openCreateModal = () => {
  editingConfigId.value = null
  resetForm()
  modalOpen.value = true
}

const openEditModal = (record: SysConfigItem) => {
  editingConfigId.value = record.configId
  form.configName = record.configName || ''
  form.configKey = record.configKey || ''
  form.configValue = record.configValue || ''
  form.configType = record.configType || 'N'
  form.remark = record.remark || ''
  modalOpen.value = true
}

const validateForm = () => {
  if (!form.configName.trim()) {
    message.warning('请输入参数名称')
    return false
  }
  if (!form.configKey.trim()) {
    message.warning('请输入参数键名')
    return false
  }
  if (!form.configValue.trim()) {
    message.warning('请输入参数键值')
    return false
  }
  return true
}

const submitModal = async () => {
  if (!validateForm()) return

  modalLoading.value = true
  try {
    const payload: SysConfigForm = {
      configId: editingConfigId.value ?? undefined,
      configName: form.configName.trim(),
      configKey: form.configKey.trim(),
      configValue: form.configValue.trim(),
      configType: form.configType,
      remark: form.remark?.trim(),
    }

    if (editingConfigId.value) {
      await updateConfig(payload)
      message.success('修改成功')
    } else {
      await addConfig(payload)
      message.success('新增成功')
    }

    modalOpen.value = false
    await fetchConfigList()
  } finally {
    modalLoading.value = false
  }
}

const handleDelete = (record: SysConfigItem) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除参数「${record.configName}」吗？`,
    onOk: async () => {
      await deleteConfig(record.configId)
      message.success('删除成功')
      await fetchConfigList()
    },
  })
}

const selectedLabel = computed(() => (selectedRowKeys.value.length ? `已选 ${selectedRowKeys.value.length} 项` : ''))
const selectedOne = computed(() => rows.value.find((item) => item.configId === selectedRowKeys.value[0]))

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
    content: `确定删除已选中的 ${selectedRowKeys.value.length} 个参数吗？`,
    onOk: async () => {
      await Promise.all(selectedRowKeys.value.map((id) => deleteConfig(id)))
      selectedRowKeys.value = []
      message.success('删除成功')
      await fetchConfigList()
    },
  })
}

onMounted(fetchConfigList)
</script>

<template>
  <a-space direction="vertical" size="middle" style="width: 100%">
    <a-card :bordered="false">
      <a-form layout="inline">
        <a-form-item label="参数名称">
          <a-input v-model:value="query.configName" placeholder="请输入参数名称" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="参数键名">
          <a-input v-model:value="query.configKey" placeholder="请输入参数键名" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="系统内置">
          <a-select
            v-model:value="query.configType"
            placeholder="是否内置"
            allow-clear
            :options="configTypeOptions"
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
        row-key="configId"
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
          <template v-if="column.key === 'configType'">
            <a-tag :color="record.configType === 'Y' ? 'blue' : 'default'">
              {{ record.configType === 'Y' ? '是' : '否' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" @click="openEditModal(record)">修改</a-button>
              <a-button type="link" danger @click="handleDelete(record)">删除</a-button>
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
            <a-form-item label="参数名称" required>
              <a-input v-model:value="form.configName" placeholder="请输入参数名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="参数键名" required>
              <a-input v-model:value="form.configKey" placeholder="请输入参数键名" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="参数键值" required>
          <a-input v-model:value="form.configValue" placeholder="请输入参数键值" />
        </a-form-item>
        <a-form-item label="系统内置">
          <a-select v-model:value="form.configType" :options="configTypeOptions" />
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
