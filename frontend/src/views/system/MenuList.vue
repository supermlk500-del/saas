<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType, TreeSelectProps } from 'ant-design-vue'
import { addMenu, deleteMenu, listMenus, updateMenu, type SysMenuForm, type SysMenuItem } from '@/api/system/menu'

const loading = ref(false)
const modalOpen = ref(false)
const modalLoading = ref(false)
const editingMenuId = ref<number | null>(null)

const rows = ref<SysMenuItem[]>([])
const expandedRowKeys = ref<number[]>([])
const originalOrderMap = ref<Record<number, number>>({})

const query = reactive({
  menuName: '',
  status: undefined as string | undefined,
})
const showSearch = ref(true)

const form = reactive<SysMenuForm>({
  menuName: '',
  parentId: 0,
  orderNum: 1,
  path: '',
  component: '',
  query: '',
  isFrame: 1,
  isCache: 0,
  menuType: 'M',
  visible: '0',
  status: '0',
  perms: '',
  icon: '',
  remark: '',
})

const statusOptions = [
  { label: '显示', value: '0' },
  { label: '隐藏', value: '1' },
]

const menuTypeOptions = [
  { label: '目录', value: 'M' },
  { label: '菜单', value: 'C' },
  { label: '按钮', value: 'F' },
] as const

const menuStatusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
]

const modalTitle = computed(() => (editingMenuId.value ? '修改菜单' : '新增菜单'))
const showPathAndComponent = computed(() => form.menuType !== 'F')
const showPerms = computed(() => form.menuType !== 'M')
const hasExpandableRows = computed(() => rows.value.some((item) => (item.children?.length ?? 0) > 0))
const expandAllText = computed(() => (expandedRowKeys.value.length > 0 ? '折叠' : '展开'))

const columns = computed<TableColumnsType<SysMenuItem>>(() => [
  { title: '菜单名称', dataIndex: 'menuName', key: 'menuName', width: 180 },
  { title: '类型', dataIndex: 'menuType', key: 'menuType', width: 80 },
  { title: '排序', dataIndex: 'orderNum', key: 'orderNum', width: 96 },
  { title: '权限标识', dataIndex: 'perms', key: 'perms', width: 130, ellipsis: true },
  { title: '组件路径', dataIndex: 'component', key: 'component', width: 140, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 86 },
  { title: '操作', key: 'action', width: 140 },
])

const collectExpandableKeys = (items: SysMenuItem[]) => {
  const keys: number[] = []
  const walk = (nodes: SysMenuItem[]) => {
    nodes.forEach((node) => {
      if ((node.children?.length ?? 0) > 0) {
        keys.push(node.menuId)
        walk(node.children!)
      }
    })
  }
  walk(items)
  return keys
}

const normalizeMenuTree = (items: SysMenuItem[]) => {
  if (items.some((item) => (item.children?.length ?? 0) > 0)) {
    return items
  }

  const nodeMap = new Map<number, SysMenuItem>()
  items.forEach((item) => {
    nodeMap.set(item.menuId, { ...item, children: [] })
  })

  const tree: SysMenuItem[] = []
  nodeMap.forEach((node) => {
    const parentId = Number(node.parentId ?? 0)
    const parent = nodeMap.get(parentId)
    if (parentId !== 0 && parent && parentId !== node.menuId) {
      ;(parent.children ||= []).push(node)
    } else {
      tree.push(node)
    }
  })

  const sortAndCleanup = (nodes: SysMenuItem[]) => {
    nodes.sort((a, b) => Number(a.orderNum ?? 0) - Number(b.orderNum ?? 0))
    nodes.forEach((node) => {
      if ((node.children?.length ?? 0) > 0) {
        sortAndCleanup(node.children!)
      } else {
        delete node.children
      }
    })
  }

  sortAndCleanup(tree)
  return tree
}

const flattenMenus = (items: SysMenuItem[]): SysMenuItem[] => {
  const flat: SysMenuItem[] = []
  const walk = (nodes: SysMenuItem[]) => {
    nodes.forEach((node) => {
      flat.push(node)
      if ((node.children?.length ?? 0) > 0) {
        walk(node.children!)
      }
    })
  }
  walk(items)
  return flat
}

const mapOrder = (items: SysMenuItem[]) => {
  const map: Record<number, number> = {}
  flattenMenus(items).forEach((item) => {
    map[item.menuId] = Number(item.orderNum ?? 0)
  })
  return map
}

const buildPayload = (item: SysMenuItem): SysMenuForm => ({
  menuId: item.menuId,
  menuName: item.menuName || '',
  parentId: item.parentId ?? 0,
  orderNum: Number(item.orderNum ?? 1),
  path: item.path || '',
  component: item.component || '',
  query: item.query || '',
  isFrame: item.isFrame ?? 1,
  isCache: item.isCache ?? 0,
  menuType: (item.menuType || 'M') as 'M' | 'C' | 'F',
  visible: item.visible || '0',
  status: item.status || '0',
  perms: item.perms || '',
  icon: item.icon || '',
  remark: '',
})

const menuTypeTag = (type?: string) => {
  if (type === 'M') return { text: '目录', color: 'orange' }
  if (type === 'C') return { text: '菜单', color: 'green' }
  return { text: '按钮', color: 'blue' }
}

const fetchMenus = async () => {
  loading.value = true
  try {
    const res = await listMenus({
      menuName: query.menuName || undefined,
      status: query.status,
    })
    rows.value = normalizeMenuTree(res.data ?? [])
    expandedRowKeys.value = []
    originalOrderMap.value = mapOrder(rows.value)
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  await fetchMenus()
}

const handleReset = async () => {
  query.menuName = ''
  query.status = undefined
  await fetchMenus()
}

const toggleSearch = () => {
  showSearch.value = !showSearch.value
}

const toggleExpandAll = () => {
  if (expandedRowKeys.value.length > 0) {
    expandedRowKeys.value = []
    return
  }
  expandedRowKeys.value = collectExpandableKeys(rows.value)
}

const saveOrder = async () => {
  const changed = flattenMenus(rows.value).filter(
    (item) => Number(item.orderNum ?? 0) !== originalOrderMap.value[item.menuId],
  )
  if (changed.length === 0) {
    message.info('排序未变化')
    return
  }

  loading.value = true
  try {
    await Promise.all(changed.map((item) => updateMenu(buildPayload(item))))
    message.success(`排序已保存（${changed.length}项）`)
    await fetchMenus()
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.menuName = ''
  form.parentId = 0
  form.orderNum = 1
  form.path = ''
  form.component = ''
  form.query = ''
  form.isFrame = 1
  form.isCache = 0
  form.menuType = 'M'
  form.visible = '0'
  form.status = '0'
  form.perms = ''
  form.icon = ''
  form.remark = ''
}

const openCreateModal = (parent?: SysMenuItem) => {
  editingMenuId.value = null
  resetForm()
  form.parentId = parent?.menuId ?? 0
  modalOpen.value = true
}

const openEditModal = (record: SysMenuItem) => {
  editingMenuId.value = record.menuId
  form.menuName = record.menuName || ''
  form.parentId = record.parentId ?? 0
  form.orderNum = record.orderNum ?? 1
  form.path = record.path || ''
  form.component = record.component || ''
  form.query = record.query || ''
  form.isFrame = record.isFrame ?? 1
  form.isCache = record.isCache ?? 0
  form.menuType = (record.menuType || 'M') as 'M' | 'C' | 'F'
  form.visible = record.visible || '0'
  form.status = record.status || '0'
  form.perms = record.perms || ''
  form.icon = record.icon || ''
  form.remark = ''
  modalOpen.value = true
}

const submitModal = async () => {
  if (!form.menuName.trim()) {
    message.warning('请输入菜单名称')
    return
  }
  if (showPathAndComponent.value && !form.path?.trim()) {
    message.warning('请输入路由地址')
    return
  }
  if (showPerms.value && !form.perms?.trim()) {
    message.warning('请输入权限标识')
    return
  }

  modalLoading.value = true
  try {
    const payload: SysMenuForm = {
      ...form,
      menuId: editingMenuId.value ?? undefined,
      menuName: form.menuName.trim(),
      path: form.path?.trim(),
      component: form.component?.trim(),
      query: form.query?.trim(),
      perms: form.perms?.trim(),
      icon: form.icon?.trim(),
      remark: form.remark?.trim(),
    }
    if (editingMenuId.value) {
      await updateMenu(payload)
      message.success('修改成功')
    } else {
      await addMenu(payload)
      message.success('新增成功')
    }
    modalOpen.value = false
    await fetchMenus()
  } finally {
    modalLoading.value = false
  }
}

const handleDelete = (record: SysMenuItem) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除菜单「${record.menuName}」吗？`,
    onOk: async () => {
      await deleteMenu(record.menuId)
      message.success('删除成功')
      await fetchMenus()
    },
  })
}

const parentTreeData = computed<TreeSelectProps['treeData']>(() => {
  const wrap = (items: SysMenuItem[]): TreeSelectProps['treeData'] =>
    items.map((item) => ({
      title: item.menuName,
      value: item.menuId,
      key: item.menuId,
      children: item.children ? wrap(item.children) : undefined,
    }))

  return [{ title: '主类目', value: 0, key: 0, children: wrap(rows.value) }]
})

onMounted(fetchMenus)
</script>

<template>
  <a-space direction="vertical" size="middle" style="width: 100%">
    <a-card v-if="showSearch" :bordered="false">
      <a-form layout="inline">
        <a-form-item label="菜单名称">
          <a-input v-model:value="query.menuName" placeholder="请输入菜单名称" allow-clear style="width: 240px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="query.status"
            placeholder="菜单状态"
            allow-clear
            :options="statusOptions"
            style="width: 160px"
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
      <div class="toolbar-row">
        <a-space style="margin-bottom: 12px">
          <a-button type="primary" @click="openCreateModal()">新增</a-button>
          <a-button @click="saveOrder">保存排序</a-button>
          <a-button v-if="hasExpandableRows" @click="toggleExpandAll">{{ expandAllText }}/折叠</a-button>
        </a-space>
        <a-space>
          <a-button shape="circle" @click="toggleSearch">
            <template #icon>
              <span>{{ showSearch ? '⌕' : '◉' }}</span>
            </template>
          </a-button>
          <a-button shape="circle" @click="fetchMenus">
            <template #icon>
              <span>↻</span>
            </template>
          </a-button>
        </a-space>
      </div>

      <a-table
        row-key="menuId"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        v-model:expandedRowKeys="expandedRowKeys"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'menuType'">
            <a-tag :color="menuTypeTag(record.menuType).color">
              {{ menuTypeTag(record.menuType).text }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'orderNum'">
            <a-input-number v-model:value="record.orderNum" :min="0" :precision="0" style="width: 84px" />
          </template>
          <template v-else-if="column.key === 'perms'">
            <span>{{ record.perms || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'component'">
            <span>{{ record.component || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === '0' ? 'green' : 'default'">
              {{ record.status === '0' ? '正常' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <div class="action-links">
              <a-button type="link" @click="openCreateModal(record)">新增</a-button>
              <a-button type="link" @click="openEditModal(record)">修改</a-button>
              <a-button type="link" danger @click="handleDelete(record)">删除</a-button>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      width="760px"
      @ok="submitModal"
    >
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="上级菜单">
              <a-tree-select
                v-model:value="form.parentId"
                :tree-data="parentTreeData"
                tree-default-expand-all
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单类型">
              <a-select v-model:value="form.menuType" :options="menuTypeOptions" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="菜单名称" required>
              <a-input v-model:value="form.menuName" placeholder="请输入菜单名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="显示排序">
              <a-input-number v-model:value="form.orderNum" :min="0" :precision="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row v-if="showPathAndComponent" :gutter="12">
          <a-col :span="12">
            <a-form-item label="路由地址" required>
              <a-input v-model:value="form.path" placeholder="请输入路由地址" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="组件路径">
              <a-input v-model:value="form.component" placeholder="请输入组件路径" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row v-if="showPerms" :gutter="12">
          <a-col :span="12">
            <a-form-item label="权限标识" required>
              <a-input v-model:value="form.perms" placeholder="请输入权限标识" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单图标">
              <a-input v-model:value="form.icon" placeholder="请输入图标名称" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="显示状态">
              <a-select v-model:value="form.visible" :options="statusOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单状态">
              <a-select v-model:value="form.status" :options="menuStatusOptions" />
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
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-links {
  display: flex;
  align-items: center;
  gap: 6px;
}

:deep(.action-links .ant-btn-link) {
  padding-inline: 0;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  white-space: nowrap;
  word-break: keep-all;
}
</style>
