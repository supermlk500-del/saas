<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import { addPost, deletePost, listPosts, updatePost, type SysPostForm, type SysPostItem } from '@/api/system/post'

const loading = ref(false)
const rows = ref<SysPostItem[]>([])
const selectedRowKeys = ref<number[]>([])

const pager = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const query = reactive({
  postCode: '',
  postName: '',
  status: undefined as string | undefined,
})

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
]

const columns = computed<TableColumnsType<SysPostItem>>(() => [
  { title: '岗位编号', dataIndex: 'postId', key: 'postId', width: 90 },
  { title: '岗位编码', dataIndex: 'postCode', key: 'postCode', width: 170 },
  { title: '岗位名称', dataIndex: 'postName', key: 'postName', width: 170 },
  { title: '显示顺序', dataIndex: 'postSort', key: 'postSort', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 200 },
])

const modalOpen = ref(false)
const modalLoading = ref(false)
const editingPostId = ref<number | null>(null)

const form = reactive<SysPostForm>({
  postCode: '',
  postName: '',
  postSort: 1,
  status: '0',
  remark: '',
})

const modalTitle = computed(() => (editingPostId.value ? '修改岗位' : '新增岗位'))

const fetchPostList = async () => {
  loading.value = true
  try {
    const res = await listPosts({
      pageNum: pager.current,
      pageSize: pager.pageSize,
      postCode: query.postCode || undefined,
      postName: query.postName || undefined,
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
  await fetchPostList()
}

const handleReset = async () => {
  query.postCode = ''
  query.postName = ''
  query.status = undefined
  pager.current = 1
  await fetchPostList()
}

const handleTableChange = async (page: number, pageSize: number) => {
  pager.current = page
  pager.pageSize = pageSize
  await fetchPostList()
}

const resetForm = () => {
  form.postCode = ''
  form.postName = ''
  form.postSort = 1
  form.status = '0'
  form.remark = ''
}

const openCreateModal = () => {
  editingPostId.value = null
  resetForm()
  modalOpen.value = true
}

const openEditModal = (record: SysPostItem) => {
  editingPostId.value = record.postId
  form.postCode = record.postCode || ''
  form.postName = record.postName || ''
  form.postSort = record.postSort ?? 1
  form.status = record.status || '0'
  form.remark = record.remark || ''
  modalOpen.value = true
}

const validateForm = () => {
  if (!form.postCode.trim()) {
    message.warning('请输入岗位编码')
    return false
  }
  if (!form.postName.trim()) {
    message.warning('请输入岗位名称')
    return false
  }
  if (form.postSort < 1) {
    message.warning('显示顺序必须大于 0')
    return false
  }
  return true
}

const submitModal = async () => {
  if (!validateForm()) return

  modalLoading.value = true
  try {
    const payload: SysPostForm = {
      postId: editingPostId.value ?? undefined,
      postCode: form.postCode.trim(),
      postName: form.postName.trim(),
      postSort: form.postSort,
      status: form.status,
      remark: form.remark?.trim(),
    }

    if (editingPostId.value) {
      await updatePost(payload)
      message.success('修改成功')
    } else {
      await addPost(payload)
      message.success('新增成功')
    }

    modalOpen.value = false
    await fetchPostList()
  } finally {
    modalLoading.value = false
  }
}

const handleDelete = (record: SysPostItem) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除岗位「${record.postName}」吗？`,
    onOk: async () => {
      await deletePost(record.postId)
      message.success('删除成功')
      await fetchPostList()
    },
  })
}

const selectedLabel = computed(() => (selectedRowKeys.value.length ? `已选 ${selectedRowKeys.value.length} 项` : ''))
const selectedOne = computed(() => rows.value.find((item) => item.postId === selectedRowKeys.value[0]))

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
    content: `确定删除已选中的 ${selectedRowKeys.value.length} 个岗位吗？`,
    onOk: async () => {
      await Promise.all(selectedRowKeys.value.map((id) => deletePost(id)))
      selectedRowKeys.value = []
      message.success('删除成功')
      await fetchPostList()
    },
  })
}

onMounted(fetchPostList)
</script>

<template>
  <a-space direction="vertical" size="middle" style="width: 100%">
    <a-card :bordered="false">
      <a-form layout="inline">
        <a-form-item label="岗位编码">
          <a-input v-model:value="query.postCode" placeholder="请输入岗位编码" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="岗位名称">
          <a-input v-model:value="query.postName" placeholder="请输入岗位名称" allow-clear style="width: 220px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="query.status"
            placeholder="岗位状态"
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
        row-key="postId"
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
            <a-tag :color="record.status === '0' ? 'green' : 'default'">
              {{ record.status === '0' ? '正常' : '停用' }}
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
      width="620px"
      @ok="submitModal"
    >
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="岗位编码" required>
              <a-input v-model:value="form.postCode" placeholder="请输入岗位编码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="岗位名称" required>
              <a-input v-model:value="form.postName" placeholder="请输入岗位名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="显示顺序">
              <a-input-number v-model:value="form.postSort" :min="1" style="width: 100%" />
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
