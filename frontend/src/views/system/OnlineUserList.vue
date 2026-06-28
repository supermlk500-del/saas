<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Modal, message, type TableColumnsType } from 'ant-design-vue'
import { forceLogout, listOnlineUsers, type OnlineSession } from '@/api/system/online'

const loading = ref(false)
const rows = ref<OnlineSession[]>([])
const columns: TableColumnsType<OnlineSession> = [
  { title: '用户', dataIndex: 'nickname', key: 'nickname', width: 140 },
  { title: '账号', dataIndex: 'username', key: 'username', width: 140 },
  { title: '角色', dataIndex: 'roleKey', key: 'roleKey', width: 150 },
  { title: '登录 IP', dataIndex: 'ipAddress', key: 'ipAddress', width: 150 },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', width: 180 },
  { title: '最近访问', dataIndex: 'lastAccessTime', key: 'lastAccessTime', width: 180 },
  { title: '操作', key: 'action', width: 120 },
]

const load = async () => {
  loading.value = true
  try { rows.value = (await listOnlineUsers()).data ?? [] } finally { loading.value = false }
}

const handleForceLogout = (record: OnlineSession) => {
  Modal.confirm({
    title: '强制下线', content: `确定让「${record.nickname || record.username}」立即退出吗？`,
    onOk: async () => { await forceLogout(record.sessionId); message.success('用户已下线'); await load() },
  })
}

onMounted(load)
</script>

<template>
  <section class="page-band">
    <div class="page-toolbar"><div><h2>在线用户</h2><p>查看 Redis 中仍有效的登录会话</p></div><a-button @click="load">刷新</a-button></div>
    <a-table row-key="sessionId" :columns="columns" :data-source="rows" :loading="loading" :scroll="{ x: 1000 }">
      <template #bodyCell="{ column, record }"><a-button v-if="column.key === 'action'" v-permission="'system:online:forceLogout'" type="link" danger @click="handleForceLogout(record)">强制下线</a-button></template>
    </a-table>
  </section>
</template>

<style scoped>
.page-band { background: #fff; border: 1px solid #e4e7ec; border-radius: 8px; padding: 20px; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
h2, p { margin: 0; } h2 { font-size: 20px; } p { margin-top: 4px; color: #7b8794; font-size: 13px; }
</style>