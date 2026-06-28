<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal, type TableColumnsType } from 'ant-design-vue'
import { addUser, changeUserStatus, deleteUser, listUsers, resetUserPassword, updateUser, type SysUserForm, type SysUserItem } from '@/api/system/user'
import { listRoles, type SysRoleItem } from '@/api/system/role'
import { listDepts, type SysDeptItem } from '@/api/system/dept'
import { listPosts, type SysPostItem } from '@/api/system/post'

const loading = ref(false), modalOpen = ref(false), modalLoading = ref(false)
const users = ref<SysUserItem[]>([]), roles = ref<SysRoleItem[]>([]), depts = ref<SysDeptItem[]>([]), posts = ref<SysPostItem[]>([])
const editingId = ref<number | null>(null)
const pager = reactive({ current: 1, pageSize: 10, total: 0 })
const query = reactive({ userName: '', phonenumber: '', status: undefined as string | undefined, deptId: undefined as number | undefined })
const form = reactive<SysUserForm>({ userName: '', nickName: '', password: '', deptId: 0, postId: 0, roleId: 0, phonenumber: '', email: '', status: '0', remark: '' })
const statusOptions = [{ label: '正常', value: '0' }, { label: '停用', value: '1' }]
const columns: TableColumnsType<SysUserItem> = [
  { title: '账号', dataIndex: 'userName', key: 'userName', width: 130 }, { title: '姓名', dataIndex: 'nickName', key: 'nickName', width: 130 },
  { title: '角色', dataIndex: 'roleName', key: 'roleName', width: 140 }, { title: '部门', dataIndex: 'deptName', key: 'deptName', width: 140 },
  { title: '岗位', dataIndex: 'postName', key: 'postName', width: 140 }, { title: '手机号', dataIndex: 'phonenumber', key: 'phonenumber', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 }, { title: '最近登录', dataIndex: 'loginDate', key: 'loginDate', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 220 },
]
const roleOptions = computed(() => roles.value.map(x => ({ label: x.roleName, value: x.roleId })))
const deptOptions = computed(() => depts.value.map(x => ({ label: x.deptName, value: x.deptId })))
const postOptions = computed(() => posts.value.map(x => ({ label: x.postName, value: x.postId })))

const loadOptions = async () => {
  const [roleRes, deptRes, postRes] = await Promise.all([listRoles({ pageNum: 1, pageSize: 200, status: '0' }), listDepts({ status: '0' }), listPosts({ pageNum: 1, pageSize: 200, status: '0' })])
  roles.value = roleRes.rows ?? []; depts.value = deptRes.data ?? []; posts.value = postRes.rows ?? []
}
const load = async () => { loading.value = true; try { const res = await listUsers({ pageNum: pager.current, pageSize: pager.pageSize, ...query, userName: query.userName || undefined, phonenumber: query.phonenumber || undefined }); users.value = res.rows ?? []; pager.total = res.total ?? 0 } finally { loading.value = false } }
const resetForm = () => Object.assign(form, { userName: '', nickName: '', password: '', deptId: depts.value[0]?.deptId ?? 0, postId: posts.value[0]?.postId ?? 0, roleId: roles.value[0]?.roleId ?? 0, phonenumber: '', email: '', status: '0', remark: '' })
const openCreate = () => { editingId.value = null; resetForm(); modalOpen.value = true }
const openEdit = (row: SysUserItem) => { editingId.value = row.userId; Object.assign(form, { ...row, password: '' }); modalOpen.value = true }
const submit = async () => { if (!form.userName.trim() || !form.nickName.trim() || !form.deptId || !form.postId || !form.roleId || (!editingId.value && !form.password)) { message.warning('请完整填写账号、姓名、角色、部门、岗位和初始密码'); return } modalLoading.value = true; try { const data = { ...form, userId: editingId.value ?? undefined }; editingId.value ? await updateUser(data) : await addUser(data); message.success('保存成功'); modalOpen.value = false; await load() } finally { modalLoading.value = false } }
const remove = (row: SysUserItem) => Modal.confirm({ title: '删除用户', content: `确定删除「${row.userName}」吗？`, onOk: async () => { await deleteUser(row.userId); message.success('删除成功'); await load() } })
const resetPassword = (row: SysUserItem) => Modal.confirm({ title: '重置密码', content: `将「${row.userName}」密码重置为 Temp@123456？`, onOk: async () => { await resetUserPassword(row.userId, 'Temp@123456'); message.success('密码已重置，该用户会被强制下线') } })
const toggle = async (row: SysUserItem, checked: boolean) => { const old = row.status; row.status = checked ? '0' : '1'; try { await changeUserStatus(row.userId, row.status) } catch { row.status = old } }
onMounted(async () => { await loadOptions(); await load() })
</script>

<template><a-space direction="vertical" size="middle" style="width:100%">
  <a-card :bordered="false"><a-form layout="inline"><a-form-item label="账号"><a-input v-model:value="query.userName" allow-clear /></a-form-item><a-form-item label="手机号"><a-input v-model:value="query.phonenumber" allow-clear /></a-form-item><a-form-item label="部门"><a-select v-model:value="query.deptId" :options="deptOptions" allow-clear style="width:160px" /></a-form-item><a-form-item><a-button type="primary" @click="pager.current=1; load()">查询</a-button></a-form-item></a-form></a-card>
  <a-card :bordered="false"><a-button v-permission="'system:user:manage'" type="primary" style="margin-bottom:12px" @click="openCreate">新增用户</a-button>
    <a-table row-key="userId" :columns="columns" :data-source="users" :loading="loading" :scroll="{x:1300}" :pagination="{current:pager.current,pageSize:pager.pageSize,total:pager.total,showSizeChanger:true,onChange:(p:number,s:number)=>{pager.current=p;pager.pageSize=s;load()}}">
      <template #bodyCell="{column,record}"><a-switch v-if="column.key==='status'" v-permission="'system:user:manage'" :checked="record.status==='0'" checked-children="正常" un-checked-children="停用" @change="(v:boolean)=>toggle(record,v)" /><a-space v-else-if="column.key==='action'"><a-button v-permission="'system:user:manage'" type="link" @click="openEdit(record)">修改</a-button><a-button v-permission="'system:user:manage'" type="link" @click="resetPassword(record)">重置密码</a-button><a-button v-permission="'system:user:manage'" type="link" danger @click="remove(record)">删除</a-button></a-space></template>
    </a-table></a-card>
  <a-modal v-model:open="modalOpen" :title="editingId?'修改用户':'新增用户'" width="720px" :confirm-loading="modalLoading" @ok="submit"><a-form layout="vertical"><a-row :gutter="12"><a-col :span="12"><a-form-item label="账号" required><a-input v-model:value="form.userName" /></a-form-item></a-col><a-col :span="12"><a-form-item label="姓名" required><a-input v-model:value="form.nickName" /></a-form-item></a-col></a-row><a-row :gutter="12"><a-col :span="8"><a-form-item label="角色（单选）" required><a-select v-model:value="form.roleId" :options="roleOptions" /></a-form-item></a-col><a-col :span="8"><a-form-item label="部门" required><a-select v-model:value="form.deptId" :options="deptOptions" /></a-form-item></a-col><a-col :span="8"><a-form-item label="岗位" required><a-select v-model:value="form.postId" :options="postOptions" /></a-form-item></a-col></a-row><a-row :gutter="12"><a-col :span="12"><a-form-item :label="editingId?'密码由“重置密码”单独处理':'初始密码'" :required="!editingId"><a-input-password v-model:value="form.password" :disabled="!!editingId" /></a-form-item></a-col><a-col :span="12"><a-form-item label="状态"><a-select v-model:value="form.status" :options="statusOptions" /></a-form-item></a-col></a-row><a-row :gutter="12"><a-col :span="12"><a-form-item label="手机号"><a-input v-model:value="form.phonenumber" /></a-form-item></a-col><a-col :span="12"><a-form-item label="邮箱"><a-input v-model:value="form.email" /></a-form-item></a-col></a-row><a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" /></a-form-item></a-form></a-modal>
</a-space></template>