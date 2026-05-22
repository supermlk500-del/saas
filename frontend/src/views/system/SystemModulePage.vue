<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRoute } from 'vue-router'
import TablePage from '@/components/TablePage.vue'

type ColumnConfig = { title: string; dataIndex: string; key: string }
type ModuleConfig = {
  title: string
  searchPlaceholders: string[]
  columns: ColumnConfig[]
  rows: Record<string, string>[]
}

const MODULES: Record<string, ModuleConfig> = {
  user: {
    title: '用户管理',
    searchPlaceholders: ['请输入用户名称', '请输入手机号码'],
    columns: [
      { title: '用户名称', dataIndex: 'name', key: 'name' },
      { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
      { title: '部门', dataIndex: 'dept', key: 'dept' },
      { title: '手机', dataIndex: 'mobile', key: 'mobile' },
      { title: '状态', dataIndex: 'status', key: 'status' },
    ],
    rows: [
      { key: '1', name: 'admin', nickname: '管理员', dept: '总经办', mobile: '13800000000', status: '正常' },
      { key: '2', name: 'zhangsan', nickname: '张三', dept: '生产部', mobile: '13900000001', status: '正常' },
    ],
  },
  role: {
    title: '角色管理',
    searchPlaceholders: ['请输入角色名称', '请输入权限字符'],
    columns: [
      { title: '角色名称', dataIndex: 'name', key: 'name' },
      { title: '权限字符', dataIndex: 'code', key: 'code' },
      { title: '数据范围', dataIndex: 'scope', key: 'scope' },
      { title: '状态', dataIndex: 'status', key: 'status' },
    ],
    rows: [
      { key: '1', name: '超级管理员', code: 'admin', scope: '全部数据', status: '正常' },
      { key: '2', name: '生产主管', code: 'prod_manager', scope: '本部门数据', status: '正常' },
    ],
  },
  menu: {
    title: '菜单管理',
    searchPlaceholders: ['请输入菜单名称'],
    columns: [
      { title: '菜单名称', dataIndex: 'name', key: 'name' },
      { title: '类型', dataIndex: 'type', key: 'type' },
      { title: '权限标识', dataIndex: 'perm', key: 'perm' },
      { title: '路由地址', dataIndex: 'path', key: 'path' },
    ],
    rows: [
      { key: '1', name: '系统管理', type: '目录', perm: '-', path: '/system' },
      { key: '2', name: '用户管理', type: '菜单', perm: 'system:user:list', path: '/system/user' },
    ],
  },
  dept: {
    title: '部门管理',
    searchPlaceholders: ['请输入部门名称'],
    columns: [
      { title: '部门名称', dataIndex: 'name', key: 'name' },
      { title: '负责人', dataIndex: 'leader', key: 'leader' },
      { title: '电话', dataIndex: 'phone', key: 'phone' },
      { title: '状态', dataIndex: 'status', key: 'status' },
    ],
    rows: [
      { key: '1', name: '总经办', leader: '管理员', phone: '021-88886666', status: '正常' },
      { key: '2', name: '生产部', leader: '王工', phone: '021-88887777', status: '正常' },
    ],
  },
  post: {
    title: '岗位管理',
    searchPlaceholders: ['请输入岗位编码', '请输入岗位名称'],
    columns: [
      { title: '岗位编码', dataIndex: 'code', key: 'code' },
      { title: '岗位名称', dataIndex: 'name', key: 'name' },
      { title: '排序', dataIndex: 'sort', key: 'sort' },
      { title: '状态', dataIndex: 'status', key: 'status' },
    ],
    rows: [
      { key: '1', code: 'CEO', name: '总经理', sort: '1', status: '正常' },
      { key: '2', code: 'LINE_LEADER', name: '产线组长', sort: '2', status: '正常' },
    ],
  },
  dict: {
    title: '字典管理',
    searchPlaceholders: ['请输入字典名称', '请输入字典类型'],
    columns: [
      { title: '字典名称', dataIndex: 'name', key: 'name' },
      { title: '字典类型', dataIndex: 'type', key: 'type' },
      { title: '状态', dataIndex: 'status', key: 'status' },
      { title: '备注', dataIndex: 'remark', key: 'remark' },
    ],
    rows: [
      { key: '1', name: '用户性别', type: 'sys_user_sex', status: '正常', remark: '系统内置' },
      { key: '2', name: '任务状态', type: 'mes_task_status', status: '正常', remark: '业务扩展' },
    ],
  },
  config: {
    title: '参数设置',
    searchPlaceholders: ['请输入参数名称', '请输入参数键名'],
    columns: [
      { title: '参数名称', dataIndex: 'name', key: 'name' },
      { title: '参数键名', dataIndex: 'keyName', key: 'keyName' },
      { title: '参数键值', dataIndex: 'value', key: 'value' },
      { title: '系统内置', dataIndex: 'builtin', key: 'builtin' },
    ],
    rows: [
      { key: '1', name: '主框架页-默认主题', keyName: 'sys.index.skinName', value: 'skin-blue', builtin: '是' },
      { key: '2', name: '用户管理-账号初始密码', keyName: 'sys.user.initPassword', value: '123456', builtin: '是' },
    ],
  },
  notice: {
    title: '通知公告',
    searchPlaceholders: ['请输入公告标题'],
    columns: [
      { title: '公告标题', dataIndex: 'title', key: 'title' },
      { title: '公告类型', dataIndex: 'type', key: 'type' },
      { title: '发布人', dataIndex: 'publisher', key: 'publisher' },
      { title: '状态', dataIndex: 'status', key: 'status' },
    ],
    rows: [
      { key: '1', title: '系统维护通知', type: '通知', publisher: '管理员', status: '已发布' },
      { key: '2', title: '节假日排班调整', type: '公告', publisher: '管理员', status: '已发布' },
    ],
  },
  operlog: {
    title: '操作日志',
    searchPlaceholders: ['请输入操作人员', '请输入业务类型'],
    columns: [
      { title: '操作人员', dataIndex: 'operator', key: 'operator' },
      { title: '业务类型', dataIndex: 'businessType', key: 'businessType' },
      { title: '方法', dataIndex: 'method', key: 'method' },
      { title: '请求地址', dataIndex: 'url', key: 'url' },
      { title: '操作状态', dataIndex: 'status', key: 'status' },
    ],
    rows: [
      { key: '1', operator: 'admin', businessType: '新增', method: 'POST', url: '/system/user', status: '成功' },
      { key: '2', operator: 'admin', businessType: '修改', method: 'PUT', url: '/system/role', status: '成功' },
    ],
  },
  logininfor: {
    title: '登录日志',
    searchPlaceholders: ['请输入用户账号', '请输入登录地址'],
    columns: [
      { title: '用户账号', dataIndex: 'username', key: 'username' },
      { title: '登录地址', dataIndex: 'ip', key: 'ip' },
      { title: '登录地点', dataIndex: 'location', key: 'location' },
      { title: '浏览器', dataIndex: 'browser', key: 'browser' },
      { title: '状态', dataIndex: 'status', key: 'status' },
    ],
    rows: [
      { key: '1', username: 'admin', ip: '127.0.0.1', location: '内网', browser: 'Chrome', status: '成功' },
      { key: '2', username: 'zhangsan', ip: '192.168.1.10', location: '上海', browser: 'Edge', status: '成功' },
    ],
  },
}

const route = useRoute()
const searchForm = reactive<Record<string, string>>({})
const fallbackModule = MODULES.user as ModuleConfig

const moduleConfig = computed<ModuleConfig>(() => {
  const key = String(route.meta.systemKey ?? 'user')
  return (MODULES[key] ?? fallbackModule) as ModuleConfig
})

const resetSearch = () => {
  for (const key of Object.keys(searchForm)) {
    searchForm[key] = ''
  }
}
</script>

<template>
  <TablePage :title="moduleConfig.title" :columns="moduleConfig.columns" :data="moduleConfig.rows">
    <template #search>
      <a-card :bordered="false">
        <a-form layout="inline">
          <a-form-item v-for="placeholder in moduleConfig.searchPlaceholders" :key="placeholder">
            <a-input
              v-model:value="searchForm[placeholder]"
              :placeholder="placeholder"
              style="width: 220px"
              allow-clear
            />
          </a-form-item>
          <a-form-item>
            <a-button type="primary">搜索</a-button>
          </a-form-item>
          <a-form-item>
            <a-button @click="resetSearch">重置</a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </template>

    <template #actions>
      <a-space>
        <a-button type="primary">新增</a-button>
        <a-button>修改</a-button>
        <a-button danger>删除</a-button>
        <a-button>导出</a-button>
      </a-space>
    </template>
  </TablePage>
</template>
