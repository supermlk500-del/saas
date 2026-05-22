<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  addUser,
  changeUserStatus,
  deleteUser,
  listUsers,
  resetUserPassword,
  updateUser,
  type SysUserForm,
  type SysUserItem,
} from '@/api/system/user'

const loading = ref(false)
const users = ref<SysUserItem[]>([])
const selectedRowKeys = ref<number[]>([])

const pager = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const query = reactive({
  userName: '',
  phonenumber: '',
  status: undefined as string | undefined,
})

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
]

const columns = computed<TableColumnsType<SysUserItem>>(() => [
  { title: '用户编号', dataIndex: 'userId', key: 'userId', width: 90 },
  { title: '用户名称', dataIndex: 'userName', key: 'userName', width: 130 },
  { title: '用户昵称', dataIndex: 'nickName', key: 'nickName', width: 130 },
  {
    title: '部门',
    dataIndex: 'dept',
    key: 'dept',
    customRender: ({ record }) => record.dept?.deptName || '-',
    width: 140,
  },
  { title: '手机号码', dataIndex: 'phonenumber', key: 'phonenumber', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 280 },
])

const modalOpen = ref(false)
const modalLoading = ref(false)
const editingUserId = ref<number | null>(null)

const form = reactive<SysUserForm>({
  userName: '',
  nickName: '',
  password: '',
  phonenumber: '',
  email: '',
  status: '0',
  remark: '',
})

const modalTitle = computed(() => (editingUserId.value ? '修改用户' : '新增用户'))

const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await listUsers({
      pageNum: pager.current,
      pageSize: pager.pageSize,
      userName: query.userName || undefined,
      phonenumber: query.phonenumber || undefined,
      status: query.status,
    })
    users.value = res.rows ?? []
    pager.total = res.total ?? 0
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  pager.current = 1
  await fetchUserList()
}

const handleReset = async () => {
  query.userName = ''
  query.phonenumber = ''
  query.status = undefined
  pager.current = 1
  await fetchUserList()
}

const handleTableChange = async (page: number, pageSize: number) => {
  pager.current = page
  pager.pageSize = pageSize
  await fetchUserList()
}

const resetForm = () => {
  form.userName = ''
  form.nickName = ''
  form.password = ''
  form.phonenumber = ''
  form.email = ''
  form.status = '0'
  form.remark = ''
}

const openCreateModal = () => {
  editingUserId.value = null
  resetForm()
  modalOpen.value = true
}

const openEditModal = (record: SysUserItem) => {
  editingUserId.value = record.userId
  form.userName = record.userName || ''
  form.nickName = record.nickName || ''
  form.password = ''
  form.phonenumber = record.phonenumber || ''
  form.email = record.email || ''
  form.status = record.status || '0'
  form.remark = ''
  modalOpen.value = true
}

const validateForm = () => {
  if (!form.userName?.trim()) {
    message.warning('请输入用户名称')
    return false
  }
  if (!form.nickName?.trim()) {
    message.warning('请输入用户昵称')
    return false
  }
  if (!editingUserId.value && !form.password?.trim()) {
    message.warning('新增用户时请填写初始密码')
    return false
  }
  return true
}

const submitModal = async () => {
  if (!validateForm()) return

  modalLoading.value = true
  try {
    if (editingUserId.value) {
      await updateUser({
        userId: editingUserId.value,
        userName: form.userName.trim(),
        nickName: form.nickName.trim(),
        phonenumber: form.phonenumber?.trim(),
        email: form.email?.trim(),
        status: form.status,
        remark: form.remark,
      })
      message.success('修改成功')
    } else {
      await addUser({
        userName: form.userName.trim(),
        nickName: form.nickName.trim(),
        password: form.password?.trim(),
        phonenumber: form.phonenumber?.trim(),
        email: form.email?.trim(),
        status: form.status,
        remark: form.remark,
      })
      message.success('新增成功')
    }
    modalOpen.value = false
    await fetchUserList()
  } finally {
    modalLoading.value = false
  }
}

const handleDelete = (record: SysUserItem) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除用户「${record.userName}」吗？`,
    onOk: async () => {
      await deleteUser(record.userId)
      message.success('删除成功')
      await fetchUserList()
    },
  })
}

const handleResetPassword = (record: SysUserItem) => {
  Modal.confirm({
    title: '重置密码',
    content: `确定将用户「${record.userName}」重置为默认密码 123456 吗？`,
    onOk: async () => {
      await resetUserPassword(record.userId, '123456')
      message.success('重置成功')
    },
  })
}

const handleStatusChange = async (record: SysUserItem, checked: boolean) => {
  const nextStatus = checked ? '0' : '1'
  try {
    await changeUserStatus(record.userId, nextStatus)
    record.status = nextStatus
    message.success('状态已更新')
  } catch {
    record.status = nextStatus === '0' ? '1' : '0'
  }
}

const selectedLabel = computed(() => (selectedRowKeys.value.length ? `已选 ${selectedRowKeys.value.length} 项` : ''))

const selectedOne = computed(() => users.value.find((item) => item.userId === selectedRowKeys.value[0]))

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
    content: `确定删除已选中的 ${selectedRowKeys.value.length} 个用户吗？`,
    onOk: async () => {
      await Promise.all(selectedRowKeys.value.map((id) => deleteUser(id)))
      selectedRowKeys.value = []
      message.success('删除成功')
      await fetchUserList()
    },
  })
}

onMounted(fetchUserList)
</script>

<template>
  <a-space direction="vertical" size="middle" style="width: 100%">
    <a-card :bordered="false">
      <a-form layout="inline">
        <a-form-item label="用户名称">
          <a-input v-model:value="query.userName" placeholder="请输入用户名称" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="手机号码">
          <a-input v-model:value="query.phonenumber" placeholder="请输入手机号码" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="query.status"
            placeholder="用户状态"
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
        row-key="userId"
        :columns="columns"
        :data-source="users"
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
              <a-button type="link" @click="handleResetPassword(record)">重置密码</a-button>
              <a-button type="link">分配角色</a-button>
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
            <a-form-item label="用户名称" required>
              <a-input v-model:value="form.userName" placeholder="请输入用户名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="用户昵称" required>
              <a-input v-model:value="form.nickName" placeholder="请输入用户昵称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item :label="editingUserId ? '新密码（可选）' : '初始密码'" :required="!editingUserId">
              <a-input-password v-model:value="form.password" placeholder="请输入密码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机号码">
              <a-input v-model:value="form.phonenumber" placeholder="请输入手机号码" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input v-model:value="form.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="form.status" :options="statusOptions" />
            </a-form-item>
          </a-col>
        </a-row>
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
