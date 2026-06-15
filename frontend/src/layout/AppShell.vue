<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  CalendarOutlined,
  CarryOutOutlined,
  DashboardOutlined,
  InboxOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  BellOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { storeToRefs } from 'pinia'
import { appMenuSections, type AppMenuSectionKey } from '@/router/routes'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()
const { collapsed } = storeToRefs(appStore)

const sectionIcons: Record<AppMenuSectionKey, typeof DashboardOutlined> = {
  dashboard: DashboardOutlined,
  gray: InboxOutlined,
  'process-center': SettingOutlined,
  schedule: CalendarOutlined,
  quality: SafetyCertificateOutlined,
  order: CarryOutOutlined,
}

const selectedKeys = computed(() => [route.path])

const activeSectionKey = computed<AppMenuSectionKey | undefined>(() => {
  const matchedRoute = [...appMenuSections]
    .flatMap((section) => section.items.map((item) => ({ sectionKey: section.key, to: item.to })))
    .find((item) => route.path.startsWith(item.to))

  return matchedRoute?.sectionKey ?? (route.meta.sectionKey as AppMenuSectionKey | undefined)
})

const openKeys = computed(() => (activeSectionKey.value ? [activeSectionKey.value] : []))

const breadcrumbItems = computed(() => {
  const items: string[] = []
  if (typeof route.meta.section === 'string') {
    items.push(route.meta.section)
  }
  if (typeof route.meta.title === 'string' && route.meta.title !== route.meta.section) {
    items.push(route.meta.title)
  }
  return items
})
</script>

<template>
  <a-layout class="app-shell">
    <a-layout-header class="app-header">
      <div class="header-left">
        <a-button type="text" class="collapse-btn" @click="appStore.toggleCollapsed()">
          <MenuUnfoldOutlined v-if="collapsed" />
          <MenuFoldOutlined v-else />
        </a-button>
        <div class="brand">
          <img class="brand-logo" src="/images/branding/logo.png" alt="织慧通" />
          <div class="brand-copy">
            <strong>织慧通</strong>
            <span>胚布排产质检系统</span>
          </div>
        </div>
      </div>

      <div class="header-right">
        <a-space size="middle">
          <a-badge dot color="#d66f22">
            <BellOutlined class="header-icon" />
          </a-badge>
          <div class="user-chip">
            <a-avatar size="small" class="login-avatar">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <span class="user-name">admin</span>
          </div>
        </a-space>
      </div>
    </a-layout-header>

    <a-layout>
      <a-layout-sider
        :width="272"
        :collapsed-width="76"
        :collapsed="collapsed"
        class="app-sider"
        theme="light"
      >
        <div v-if="!collapsed" class="sider-title">核心业务导航</div>
        <a-menu mode="inline" theme="light" :selected-keys="selectedKeys" :open-keys="openKeys" class="app-menu">
          <template v-for="section in appMenuSections" :key="section.key">
            <a-menu-item v-if="section.items.length === 1" :key="section.items[0]?.to">
              <template #icon>
                <component :is="sectionIcons[section.key]" />
              </template>
              <router-link :to="section.items[0]?.to || '/dashboard'">{{ section.title }}</router-link>
            </a-menu-item>

            <a-sub-menu
              v-else
              :key="section.key"
              :title="section.title"
            >
              <template #icon>
                <component :is="sectionIcons[section.key]" />
              </template>
              <a-menu-item v-for="item in section.items" :key="item.to">
                <router-link :to="item.to">{{ item.title }}</router-link>
              </a-menu-item>
            </a-sub-menu>
          </template>
        </a-menu>
      </a-layout-sider>

      <a-layout class="app-main">
        <a-layout-content class="app-content">
          <a-breadcrumb class="app-breadcrumb">
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
  background:
    radial-gradient(circle at top left, rgba(214, 111, 34, 0.12), transparent 24rem),
    linear-gradient(180deg, #f8f5f1 0%, #f4f7fb 100%);
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid rgba(145, 158, 171, 0.15);
  backdrop-filter: blur(18px);
  height: 72px;
  line-height: 72px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  color: #5d6b7b;
  font-size: 18px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  line-height: 1;
}

.brand-logo {
  width: auto;
  height: 48px;
  display: block;
  object-fit: contain;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-copy strong {
  color: #1f2937;
  font-size: 18px;
  letter-spacing: 0.04em;
}

.brand-copy span {
  color: #7b8794;
  font-size: 12px;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-icon {
  font-size: 20px;
  color: #5d6b7b;
}

.login-avatar {
  background-color: #d66f22;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #344054;
  background: rgba(255, 248, 240, 0.92);
  border: 1px solid rgba(214, 111, 34, 0.18);
  border-radius: 999px;
  padding: 4px 10px 4px 4px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.app-sider {
  background: rgba(255, 255, 255, 0.92);
  border-right: 1px solid rgba(145, 158, 171, 0.15);
}

.sider-title {
  color: #98a2b3;
  padding: 20px 28px 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.app-menu {
  border-right: none;
  background: transparent;
}

:deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
}

:deep(.ant-menu-inline) {
  padding: 8px 0 12px;
}

:deep(.ant-menu-submenu-title) {
  height: 48px !important;
  line-height: 48px !important;
  margin: 4px 14px;
  width: calc(100% - 28px);
  border-radius: 12px;
}

:deep(.ant-menu-inline .ant-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 4px 14px;
  width: calc(100% - 28px);
  border-radius: 12px;
}

:deep(.ant-menu-item-selected) {
  background-color: rgba(214, 111, 34, 0.1) !important;
  color: #d66f22 !important;
}

:deep(.ant-menu-item-selected::after) {
  border-right-color: #d66f22 !important;
}

:deep(.ant-menu-sub.ant-menu-inline) {
  background: transparent !important;
}

.app-main {
  padding: 28px;
  background: transparent;
}

.app-content {
  padding: 0;
  min-height: auto;
}

.app-breadcrumb {
  margin-bottom: 20px;
}

:deep(.ant-breadcrumb-link) {
  color: #667085;
  font-weight: 500;
}

:deep(.ant-breadcrumb-separator) {
  color: #c0c7d1;
}
</style>
