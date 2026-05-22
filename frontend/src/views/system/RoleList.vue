<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  addRole,
  changeRoleStatus,
  deleteRole,
  listRoles,
  updateRole,
  type SysRoleForm,
  type SysRoleItem,
} from '@/api/system/role'

const loading = ref(false)
const roles = ref<SysRoleItem[]>([])
const selectedRowKeys = ref<number[]>([])

const pager = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const query = reactive({
  roleName: '',
  roleKey: '',
  status: undefined as string | undefined,
})

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
]

const dataScopeOptions = [
  { label: '全部数据权限', value: '1' },
  { label: '自定数据权限', value: '2' },
  { label: '本部门数据权限', value: '3' },
  { label: '本部门及以下数据权限', value: '4' },
  { label: '仅本人数据权限', value: '5' },
]

const columns = computed<TableColumnsType<SysRoleItem>>(() => [
  { title: '角色编号', dataIndex: 'roleId', key: 'roleId', width: 90 },
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 150 },
  { title: '权限字符', dataIndex: 'roleKey', key: 'roleKey', width: 180 },
  { title: '显示顺序', dataIndex: 'roleSort', key: 'roleSort', width: 120 },
  {
    title: '数据范围',
    dataIndex: 'dataScope',
    key: 'dataScope',
    width: 180,
    customRender: ({ record }) => dataScopeOptions.find((item) => item.value === record.dataScope)?.label || '-',
  },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 240 },
])

const modalOpen = ref(false)
const modalLoading = ref(false)
const editingRoleId = ref<number | null>(null)

const form = reactive<SysRoleForm>({
  roleName: '',
  roleKey: '',
  roleSort: 1,
  status: '0',
  dataScope: '1',
  remark: '',
})

const modalTitle = computed(() => (editingRoleId.value ? '修改角色' : '新增角色'))

const fetchRoleList = async () => {
  loading.value = true
  try {
    const res = await listRoles({
      pageNum: pager.current,
      pageSize: pager.pageSize,
      roleName: query.roleName || undefined,
      roleKey: query.roleKey || undefined,
      status: query.status,
    })
    roles.value = res.rows ?? []
    pager.total = res.total ?? 0
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  pager.current = 1
  await fetchRoleList()
}

const handleReset = async () => {
  query.roleName = ''
  query.roleKey = ''
  query.status = undefined
  pager.current = 1
  await fetchRoleList()
}

const handleTableChange = async (page: number, pageSize: number) => {
  pager.current = page
  pager.pageSize = pageSize
  await fetchRoleList()
}

const resetForm = () => {
  form.roleName = ''
  form.roleKey = ''
  form.roleSort = 1
  form.status = '0'
  form.dataScope = '1'
  form.remark = ''
}

const openCreateModal = () => {
  editingRoleId.value = null
  resetForm()
  modalOpen.value = true
}

const openEditModal = (record: SysRoleItem) => {
  editingRoleId.value = record.roleId
  form.roleName = record.roleName || ''
  form.roleKey = record.roleKey || ''
  form.roleSort = record.roleSort ?? 1
  form.status = record.status || '0'
  form.dataScope = record.dataScope || '1'
  form.remark = record.remark || ''
  modalOpen.value = true
}

const validateForm = () => {
  if (!form.roleName?.trim()) {
    message.warning('请输入角色名称')
    return false
  }
  if (!form.roleKey?.trim()) {
    message.warning('请输入权限字符')
    return false
  }
  if (form.roleSort < 1) {
    message.warning('显示顺序必须大于 0')
    return false
  }
  return true
}

const submitModal = async () => {
  if (!validateForm()) return

  modalLoading.value = true
  try {
    if (editingRoleId.value) {
      await updateRole({
        roleId: editingRoleId.value,
        roleName: form.roleName.trim(),
        roleKey: form.roleKey.trim(),
        roleSort: form.roleSort,
        status: form.status,
        dataScope: form.dataScope,
        remark: form.remark,
      })
      message.success('修改成功')
    } else {
      await addRole({
        roleName: form.roleName.trim(),
        roleKey: form.roleKey.trim(),
        roleSort: form.roleSort,
        status: form.status,
        dataScope: form.dataScope,
        remark: form.remark,
      })
      message.success('新增成功')
    }

    modalOpen.value = false
    await fetchRoleList()
  } finally {
    modalLoading.value = false
  }
}

const handleDelete = (record: SysRoleItem) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除角色「${record.roleName}」吗？`,
    onOk: async () => {
      await deleteRole(record.roleId)
      message.success('删除成功')
      await fetchRoleList()
    },
  })
}

const handleStatusChange = async (record: SysRoleItem, checked: boolean) => {
  const nextStatus = checked ? '0' : '1'
  try {
    await changeRoleStatus(record.roleId, nextStatus)
    record.status = nextStatus
    message.success('状态已更新')
  } catch {
    record.status = nextStatus === '0' ? '1' : '0'
  }
}

const selectedLabel = computed(() => (selectedRowKeys.value.length ? `已选 ${selectedRowKeys.value.length} 项` : ''))
const selectedOne = computed(() => roles.value.find((item) => item.roleId === selectedRowKeys.value[0]))

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
    content: `确定删除已选中的 ${selectedRowKeys.value.length} 个角色吗？`,
    onOk: async () => {
      await Promise.all(selectedRowKeys.value.map((id) => deleteRole(id)))
      selectedRowKeys.value = []
      message.success('删除成功')
      await fetchRoleList()
    },
  })
}

onMounted(fetchRoleList)
</script>

<template>
  <a-space direction="vertical" size="middle" style="width: 100%">
    <a-card :bordered="false">
      <a-form layout="inline">
        <a-form-item label="角色名称">
          <a-input v-model:value="query.roleName" placeholder="请输入角色名称" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="权限字符">
          <a-input v-model:value="query.roleKey" placeholder="请输入权限字符" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="query.status"
            placeholder="角色状态"
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
        row-key="roleId"
        :columns="columns"
        :data-source="roles"
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
              <a-button type="link">数据权限</a-button>
              <a-button type="link">分配用户</a-button>
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
            <a-form-item label="角色名称" required>
              <a-input v-model:value="form.roleName" placeholder="请输入角色名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="权限字符" required>
              <a-input v-model:value="form.roleKey" placeholder="请输入权限字符" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="显示顺序">
              <a-input-number v-model:value="form.roleSort" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="角色状态">
              <a-select v-model:value="form.status" :options="statusOptions" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="数据范围">
          <a-select v-model:value="form.dataScope" :options="dataScopeOptions" />
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
