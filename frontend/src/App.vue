<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  BellOutlined,
  UserOutlined,
  DashboardOutlined,
  InboxOutlined,
  CarryOutOutlined,
  SafetyCertificateOutlined,
  CalendarOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const collapsed = ref(false)

const selectedKeys = computed(() => [route.path])

const openKeys = computed(() => {
  if (route.path.startsWith('/dashboard')) return ['dashboard']
  if (route.path.startsWith('/gray')) return ['gray']
  if (route.path.startsWith('/order')) return ['order']
  if (
    route.path.startsWith('/process-center') ||
    route.path.startsWith('/master/machine') ||
    route.path.startsWith('/master/fabric-process') ||
    route.path.startsWith('/master/fabric-defect')
  ) {
    return ['process-center']
  }
  if (route.path.startsWith('/schedule')) return ['schedule']
  if (route.path.startsWith('/quality')) return ['quality']
  return []
})

const breadcrumbItems = computed(() => {
  const items: string[] = []
  if (route.meta.section) items.push(route.meta.section as string)
  if (route.meta.title) items.push(route.meta.title as string)
  return items
})

const showBreadcrumb = computed(() => route.meta?.hideBreadcrumb !== true)
</script>

<template>
  <a-layout class="app-shell">
    <a-layout-header class="app-header">
      <div class="header-left">
        <a-button type="text" class="collapse-btn" @click="collapsed = !collapsed">
          <MenuUnfoldOutlined v-if="collapsed" />
          <MenuFoldOutlined v-else />
        </a-button>
        <div class="brand">
          <img class="brand-logo" src="/images/branding/title-top.png" alt="logo" />
          织慧通
        </div>
      </div>
      <div class="header-right">
        <a-space size="large">
          <a-badge dot color="#ff7a45">
            <BellOutlined class="header-icon" />
          </a-badge>
          <div class="user-chip">
            <a-avatar size="small" class="login-avatar">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <span class="user-name">访客</span>
          </div>
        </a-space>
      </div>
    </a-layout-header>

    <a-layout>
      <a-layout-sider :width="268" :collapsed-width="72" :collapsed="collapsed" class="app-sider" theme="light">
        <div v-if="!collapsed" class="sider-title">核心工作台</div>
        <a-menu mode="inline" theme="light" :selected-keys="selectedKeys" :open-keys="openKeys" class="app-menu">
          <a-sub-menu key="dashboard" title="看板">
            <template #icon><DashboardOutlined /></template>
            <a-menu-item key="/dashboard">
              <router-link to="/dashboard">任务看板</router-link>
            </a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="gray" title="来料管理">
            <template #icon><InboxOutlined /></template>
            <a-menu-item key="/gray/fabric">
              <router-link to="/gray/fabric">来料主数据</router-link>
            </a-menu-item>
            <a-menu-item key="/gray/inbound">
              <router-link to="/gray/inbound">来料批次</router-link>
            </a-menu-item>
            <a-menu-item key="/gray/iqc-task">
              <router-link to="/gray/iqc-task">IQC任务</router-link>
            </a-menu-item>
            <a-menu-item key="/gray/iqc-result">
              <router-link to="/gray/iqc-result">IQC结果</router-link>
            </a-menu-item>
            <a-menu-item key="/gray/release">
              <router-link to="/gray/release">放行决策</router-link>
            </a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="order" title="订单">
            <template #icon><CarryOutOutlined /></template>
            <a-menu-item key="/order/list">
              <router-link to="/order/list">订单管理</router-link>
            </a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="process-center" title="工艺中心">
            <template #icon><SettingOutlined /></template>
            <a-menu-item key="/process-center/process">
              <router-link to="/process-center/process">工艺路线</router-link>
            </a-menu-item>
            <a-menu-item key="/process-center/shift">
              <router-link to="/process-center/shift">班次日历</router-link>
            </a-menu-item>
            <a-menu-item key="/master/machine">
              <router-link to="/master/machine">机器管理</router-link>
            </a-menu-item>
            <a-menu-item key="/master/fabric-process">
              <router-link to="/master/fabric-process">工艺档案中心</router-link>
            </a-menu-item>
            <a-menu-item key="/master/fabric-defect">
              <router-link to="/master/fabric-defect">布料瑕疵种类</router-link>
            </a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="schedule" title="排产">
            <template #icon><CalendarOutlined /></template>
            <a-menu-item key="/schedule/pool">
              <router-link to="/schedule/pool">排产池</router-link>
            </a-menu-item>
            <a-menu-item key="/schedule/main">
              <router-link to="/schedule/main">主生产计划</router-link>
            </a-menu-item>
            <a-menu-item key="/schedule/board">
              <router-link to="/schedule/board">生产调度甘特图</router-link>
            </a-menu-item>
            <a-menu-item key="/schedule/reschedule-log">
              <router-link to="/schedule/reschedule-log">重排日志</router-link>
            </a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="quality" title="质检">
            <template #icon><SafetyCertificateOutlined /></template>
            <a-menu-item key="/quality/realtime">
              <router-link to="/quality/realtime">实时质检</router-link>
            </a-menu-item>
            <a-menu-item key="/quality/ncr">
              <router-link to="/quality/ncr">NCR管理</router-link>
            </a-menu-item>
            <a-menu-item key="/quality/rework">
              <router-link to="/quality/rework">返工单</router-link>
            </a-menu-item>
          </a-sub-menu>
        </a-menu>
      </a-layout-sider>

      <a-layout class="app-main">
        <a-layout-content class="app-content">
          <a-breadcrumb v-if="showBreadcrumb" class="app-breadcrumb">
            <a-breadcrumb-item v-for="item in breadcrumbItems" :key="item">
              {{ item }}
            </a-breadcrumb-item>
          </a-breadcrumb>
          <router-view />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f8fafc;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
  height: 64px;
  line-height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  color: #64748b;
  font-size: 18px;
}

.brand {
  color: #1e293b;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1;
}

.brand-logo {
  width: auto;
  height: 24px;
  display: block;
  object-fit: contain;
  transform: translateY(-1px);
}

.header-icon {
  font-size: 20px;
  color: #64748b;
  cursor: pointer;
}

.login-avatar {
  background-color: #ff7a45;
  cursor: default;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 999px;
  padding: 4px 10px 4px 4px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.app-sider {
  background: #ffffff;
  border-right: 1px solid #f1f5f9;
}

.sider-title {
  color: #94a3b8;
  padding: 20px 28px 12px;
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.app-menu {
  border-right: none;
}

:deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
}

:deep(.ant-menu-inline) {
  padding: 4px 0 12px;
}

:deep(.ant-menu-submenu-title) {
  height: 48px !important;
  line-height: 48px !important;
  margin: 6px 14px;
  width: calc(100% - 28px);
  border-radius: 10px;
}

:deep(.ant-menu-item-selected) {
  background-color: #fff1e6 !important;
  color: #ff7a45 !important;
}

:deep(.ant-menu-item-selected::after) {
  border-right-color: #ff7a45 !important;
}

:deep(.ant-menu-inline .ant-menu-item) {
  height: 46px;
  line-height: 46px;
  margin: 6px 14px;
  width: calc(100% - 28px);
  border-radius: 10px;
}

:deep(.ant-menu-sub.ant-menu-inline) {
  background: transparent !important;
}

.app-main {
  padding: 28px;
  background: #f8fafc;
}

.app-content {
  background: transparent;
  padding: 0;
  border-radius: 0;
  box-shadow: none;
  min-height: auto;
}

.app-breadcrumb {
  margin-bottom: 24px;
}

:deep(.ant-breadcrumb-link) {
  color: #64748b;
  font-weight: 500;
}

:deep(.ant-breadcrumb-separator) {
  color: #cbd5e1;
}

:deep(.ant-btn-primary) {
  background-color: #ff7a45;
  border-color: #ff7a45;
}

:deep(.ant-btn-primary:hover),
:deep(.ant-btn-primary:focus) {
  background-color: #ff9c6e;
  border-color: #ff9c6e;
}

:deep(.ant-tag-blue) {
  background: #fff1e6;
  border-color: #ffd8bf;
  color: #ff7a45;
}
</style>
