<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal, type TableColumnsType } from 'ant-design-vue'
import { addRole, changeRoleStatus, deleteRole, getRole, listRoles, updateRole, type DataScope, type SysRoleForm, type SysRoleItem } from '@/api/system/role'
import { listMenus, type SysMenuItem } from '@/api/system/menu'
import { listDepts, type SysDeptItem } from '@/api/system/dept'

const loading = ref(false), modalOpen = ref(false), modalLoading = ref(false)
const rows = ref<SysRoleItem[]>([]), menus = ref<SysMenuItem[]>([]), depts = ref<SysDeptItem[]>([])
const editingId = ref<number | null>(null)
const pager = reactive({ current: 1, pageSize: 10, total: 0 })
const query = reactive({ roleName: '', roleKey: '', status: undefined as string | undefined })
const form = reactive<SysRoleForm>({ roleName: '', roleKey: '', roleSort: 1, status: '0', dataScope: 'DEPT', remark: '', menuIds: [], deptIds: [] })
const statusOptions = [{ label: '正常', value: '0' }, { label: '停用', value: '1' }]
const dataScopeOptions: { label: string; value: DataScope }[] = [
  { label: '全部数据', value: 'ALL' }, { label: '指定部门', value: 'CUSTOM' }, { label: '本部门', value: 'DEPT' },
  { label: '本部门及下级', value: 'DEPT_AND_CHILD' }, { label: '仅本人', value: 'SELF' },
]
const columns: TableColumnsType<SysRoleItem> = [
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 160 }, { title: '角色标识', dataIndex: 'roleKey', key: 'roleKey', width: 170 },
  { title: '排序', dataIndex: 'roleSort', key: 'roleSort', width: 80 }, { title: '数据范围', dataIndex: 'dataScope', key: 'dataScope', width: 160 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 }, { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 150 },
]
const menuOptions = computed(() => menus.value.map(x => ({ label: `${x.menuName}${x.perms ? `（${x.perms}）` : ''}`, value: x.menuId })))
const deptOptions = computed(() => depts.value.map(x => ({ label: x.deptName, value: x.deptId })))
const scopeLabel = (value: DataScope) => dataScopeOptions.find(x => x.value === value)?.label || value
const loadOptions = async () => { const [m, d] = await Promise.all([listMenus(), listDepts()]); menus.value = m.data ?? []; depts.value = d.data ?? [] }
const load = async () => { loading.value = true; try { const res = await listRoles({ pageNum: pager.current, pageSize: pager.pageSize, roleName: query.roleName || undefined, roleKey: query.roleKey || undefined, status: query.status }); rows.value = res.rows ?? []; pager.total = res.total ?? 0 } finally { loading.value = false } }
const resetForm = () => Object.assign(form, { roleName: '', roleKey: '', roleSort: 1, status: '0', dataScope: 'DEPT' as DataScope, remark: '', menuIds: [], deptIds: [] })
const openCreate = () => { editingId.value = null; resetForm(); modalOpen.value = true }
const openEdit = async (row: SysRoleItem) => { editingId.value = row.roleId; const res = await getRole(row.roleId); Object.assign(form, res.data.role, { menuIds: res.data.menuIds ?? [], deptIds: res.data.deptIds ?? [] }); modalOpen.value = true }
const submit = async () => { if (!form.roleName.trim() || !form.roleKey.trim() || !form.menuIds.length) { message.warning('请填写角色名称、角色标识并至少选择一个菜单权限'); return } if (form.dataScope === 'CUSTOM' && !form.deptIds.length) { message.warning('指定部门数据范围必须选择部门'); return } modalLoading.value = true; try { const data = { ...form, roleId: editingId.value ?? undefined, deptIds: form.dataScope === 'CUSTOM' ? form.deptIds : [] }; editingId.value ? await updateRole(data) : await addRole(data); message.success('保存成功，关联用户会话已失效'); modalOpen.value = false; await load() } finally { modalLoading.value = false } }
const remove = (row: SysRoleItem) => Modal.confirm({ title: '删除角色', content: `确定删除「${row.roleName}」吗？`, onOk: async () => { await deleteRole(row.roleId); message.success('删除成功'); await load() } })
const toggle = async (row: SysRoleItem, checked: boolean) => { const old = row.status; row.status = checked ? '0' : '1'; try { await changeRoleStatus(row.roleId, row.status) } catch { row.status = old } }
onMounted(async () => { await loadOptions(); await load() })
</script>

<template><a-space direction="vertical" size="middle" style="width:100%">
  <a-card :bordered="false"><a-form layout="inline"><a-form-item label="角色名称"><a-input v-model:value="query.roleName" allow-clear /></a-form-item><a-form-item label="角色标识"><a-input v-model:value="query.roleKey" allow-clear /></a-form-item><a-form-item><a-button type="primary" @click="pager.current=1;load()">查询</a-button></a-form-item></a-form></a-card>
  <a-card :bordered="false"><a-button v-permission="'system:role:manage'" type="primary" style="margin-bottom:12px" @click="openCreate">新增角色</a-button><a-table row-key="roleId" :columns="columns" :data-source="rows" :loading="loading" :pagination="{current:pager.current,pageSize:pager.pageSize,total:pager.total,onChange:(p:number,s:number)=>{pager.current=p;pager.pageSize=s;load()}}"><template #bodyCell="{column,record}"><span v-if="column.key==='dataScope'">{{ scopeLabel(record.dataScope) }}</span><a-switch v-else-if="column.key==='status'" v-permission="'system:role:manage'" :checked="record.status==='0'" @change="(v:boolean)=>toggle(record,v)" /><a-space v-else-if="column.key==='action'"><a-button v-permission="'system:role:manage'" type="link" @click="openEdit(record)">配置</a-button><a-button v-permission="'system:role:manage'" type="link" danger @click="remove(record)">删除</a-button></a-space></template></a-table></a-card>
  <a-modal v-model:open="modalOpen" :title="editingId?'配置角色':'新增角色'" width="820px" :confirm-loading="modalLoading" @ok="submit"><a-form layout="vertical"><a-row :gutter="12"><a-col :span="10"><a-form-item label="角色名称" required><a-input v-model:value="form.roleName" /></a-form-item></a-col><a-col :span="10"><a-form-item label="角色标识" required><a-input v-model:value="form.roleKey" :disabled="editingId===1" /></a-form-item></a-col><a-col :span="4"><a-form-item label="排序"><a-input-number v-model:value="form.roleSort" :min="1" style="width:100%" /></a-form-item></a-col></a-row><a-row :gutter="12"><a-col :span="12"><a-form-item label="数据范围" required><a-select v-model:value="form.dataScope" :options="dataScopeOptions" /></a-form-item></a-col><a-col :span="12"><a-form-item label="状态"><a-select v-model:value="form.status" :options="statusOptions" :disabled="editingId===1" /></a-form-item></a-col></a-row><a-form-item label="菜单与操作权限" required><a-select v-model:value="form.menuIds" mode="multiple" show-search :options="menuOptions" :max-tag-count="5" placeholder="选择页面及按钮权限" /></a-form-item><a-form-item v-if="form.dataScope==='CUSTOM'" label="可访问部门" required><a-select v-model:value="form.deptIds" mode="multiple" :options="deptOptions" placeholder="选择部门" /></a-form-item><a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" /></a-form-item></a-form></a-modal>
</a-space></template>