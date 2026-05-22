<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType, TreeSelectProps } from 'ant-design-vue'
import { addDept, deleteDept, listDepts, updateDept, type SysDeptForm, type SysDeptItem } from '@/api/system/dept'

const loading = ref(false)
const rows = ref<SysDeptItem[]>([])

const query = reactive({
  deptName: '',
  status: undefined as string | undefined,
})

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
]

const columns = computed<TableColumnsType<SysDeptItem>>(() => [
  { title: '部门名称', dataIndex: 'deptName', key: 'deptName', width: 220 },
  { title: '排序', dataIndex: 'orderNum', key: 'orderNum', width: 100 },
  { title: '负责人', dataIndex: 'leader', key: 'leader', width: 120 },
  { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 150 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 200 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 220 },
])

const modalOpen = ref(false)
const modalLoading = ref(false)
const editingDeptId = ref<number | null>(null)

const form = reactive<SysDeptForm>({
  parentId: 0,
  deptName: '',
  orderNum: 1,
  leader: '',
  phone: '',
  email: '',
  status: '0',
})

const modalTitle = computed(() => (editingDeptId.value ? '修改部门' : '新增部门'))

const deptTreeData = computed<TreeSelectProps['treeData']>(() => {
  const mapTree = (list: SysDeptItem[]): TreeSelectProps['treeData'] =>
    list.map((item) => ({
      title: item.deptName,
      value: item.deptId,
      key: item.deptId,
      children: item.children ? mapTree(item.children) : undefined,
    }))

  return [
    {
      title: '顶级部门',
      value: 0,
      key: 0,
      children: mapTree(rows.value),
    },
  ]
})

const fetchDeptList = async () => {
  loading.value = true
  try {
    const res = await listDepts({
      deptName: query.deptName || undefined,
      status: query.status,
    })
    rows.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  await fetchDeptList()
}

const handleReset = async () => {
  query.deptName = ''
  query.status = undefined
  await fetchDeptList()
}

const resetForm = () => {
  form.parentId = 0
  form.deptName = ''
  form.orderNum = 1
  form.leader = ''
  form.phone = ''
  form.email = ''
  form.status = '0'
}

const openCreateModal = (parent?: SysDeptItem) => {
  editingDeptId.value = null
  resetForm()
  form.parentId = parent?.deptId ?? 0
  modalOpen.value = true
}

const openEditModal = (record: SysDeptItem) => {
  editingDeptId.value = record.deptId
  form.parentId = record.parentId ?? 0
  form.deptName = record.deptName || ''
  form.orderNum = record.orderNum ?? 1
  form.leader = record.leader || ''
  form.phone = record.phone || ''
  form.email = record.email || ''
  form.status = record.status || '0'
  modalOpen.value = true
}

const validateForm = () => {
  if (!form.deptName.trim()) {
    message.warning('请输入部门名称')
    return false
  }
  if (form.orderNum < 1) {
    message.warning('显示排序必须大于 0')
    return false
  }
  return true
}

const submitModal = async () => {
  if (!validateForm()) return

  modalLoading.value = true
  try {
    const payload: SysDeptForm = {
      deptId: editingDeptId.value ?? undefined,
      parentId: form.parentId,
      deptName: form.deptName.trim(),
      orderNum: form.orderNum,
      leader: form.leader?.trim(),
      phone: form.phone?.trim(),
      email: form.email?.trim(),
      status: form.status,
    }

    if (editingDeptId.value) {
      await updateDept(payload)
      message.success('修改成功')
    } else {
      await addDept(payload)
      message.success('新增成功')
    }

    modalOpen.value = false
    await fetchDeptList()
  } finally {
    modalLoading.value = false
  }
}

const handleDelete = (record: SysDeptItem) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除部门「${record.deptName}」吗？`,
    onOk: async () => {
      await deleteDept(record.deptId)
      message.success('删除成功')
      await fetchDeptList()
    },
  })
}

onMounted(fetchDeptList)
</script>

<template>
  <a-space direction="vertical" size="middle" style="width: 100%">
    <a-card :bordered="false">
      <a-form layout="inline">
        <a-form-item label="部门名称">
          <a-input v-model:value="query.deptName" placeholder="请输入部门名称" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="query.status"
            placeholder="部门状态"
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
        <a-button type="primary" @click="openCreateModal()">新增</a-button>
      </a-space>

      <a-table
        row-key="deptId"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === '0' ? 'green' : 'default'">
              {{ record.status === '0' ? '正常' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" @click="openCreateModal(record)">新增</a-button>
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
            <a-form-item label="上级部门">
              <a-tree-select
                v-model:value="form.parentId"
                :tree-data="deptTreeData"
                tree-default-expand-all
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="部门名称" required>
              <a-input v-model:value="form.deptName" placeholder="请输入部门名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="显示排序">
              <a-input-number v-model:value="form.orderNum" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="负责人">
              <a-input v-model:value="form.leader" placeholder="请输入负责人" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="联系电话">
              <a-input v-model:value="form.phone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input v-model:value="form.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="部门状态">
          <a-select v-model:value="form.status" :options="statusOptions" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-space>
</template>
