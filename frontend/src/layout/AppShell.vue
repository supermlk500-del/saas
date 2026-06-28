<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CalendarOutlined, CarryOutOutlined, DashboardOutlined, InboxOutlined, MenuFoldOutlined, MenuUnfoldOutlined, SafetyCertificateOutlined, SettingOutlined, BellOutlined, UserOutlined, TeamOutlined, LogoutOutlined, KeyOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { storeToRefs } from 'pinia'
import { appMenuSections, type AppMenuSectionKey } from '@/router/routes'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { changePassword } from '@/api/system/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const { collapsed } = storeToRefs(appStore)
const passwordModalOpen = ref(false)
const passwordSaving = ref(false)
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

const sectionIcons: Record<AppMenuSectionKey, typeof DashboardOutlined> = {
  dashboard: DashboardOutlined, gray: InboxOutlined, 'process-center': SettingOutlined,
  schedule: CalendarOutlined, quality: SafetyCertificateOutlined, order: CarryOutOutlined, system: TeamOutlined,
}
const visibleMenuSections = computed(() => appMenuSections.map(section => ({ ...section, items: section.items.filter(item => authStore.hasMenu(item.key)) })).filter(section => section.items.length > 0))
const selectedKeys = computed(() => [route.path])
const activeSectionKey = computed<AppMenuSectionKey | undefined>(() => visibleMenuSections.value.flatMap(section => section.items.map(item => ({ sectionKey: section.key, to: item.to }))).find(item => route.path.startsWith(item.to))?.sectionKey ?? (route.meta.sectionKey as AppMenuSectionKey | undefined))
const openKeys = computed(() => activeSectionKey.value ? [activeSectionKey.value] : [])
const breadcrumbItems = computed(() => {
  const items: string[] = []
  if (typeof route.meta.section === 'string') items.push(route.meta.section)
  if (typeof route.meta.title === 'string' && route.meta.title !== route.meta.section) items.push(route.meta.title)
  return items
})

const openPasswordModal = () => { Object.assign(passwordForm, { currentPassword: '', newPassword: '', confirmPassword: '' }); passwordModalOpen.value = true }
const submitPassword = async () => {
  if (passwordForm.newPassword.length < 8) return void message.warning('新密码至少 8 位')
  if (passwordForm.newPassword !== passwordForm.confirmPassword) return void message.warning('两次输入的新密码不一致')
  passwordSaving.value = true
  try {
    await changePassword({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword })
    message.success('密码已修改，请重新登录')
    passwordModalOpen.value = false
    authStore.clearSession()
    await router.replace('/login')
  } finally { passwordSaving.value = false }
}
const handleLogout = async () => { await authStore.logout(); await router.replace('/login') }
</script>

<template>
  <a-layout class="app-shell">
    <a-layout-header class="app-header">
      <div class="header-left">
        <a-button type="text" class="collapse-btn" @click="appStore.toggleCollapsed()"><MenuUnfoldOutlined v-if="collapsed" /><MenuFoldOutlined v-else /></a-button>
        <div class="brand"><img class="brand-logo" src="/images/branding/logo.png" alt="织慧通" /><div class="brand-copy"><strong>织慧通</strong><span>AI胚布排产质检系统</span></div></div>
      </div>
      <div class="header-right"><a-space size="middle"><a-badge dot color="#d66f22"><BellOutlined class="header-icon" /></a-badge>
        <a-dropdown placement="bottomRight"><div class="user-chip" role="button" tabindex="0"><a-avatar size="small" class="login-avatar"><template #icon><UserOutlined /></template></a-avatar><span class="user-name">{{ authStore.user?.nickName || authStore.user?.userName }}</span></div>
          <template #overlay><a-menu><a-menu-item @click="openPasswordModal"><KeyOutlined /> 修改密码</a-menu-item><a-menu-divider /><a-menu-item @click="handleLogout"><LogoutOutlined /> 退出登录</a-menu-item></a-menu></template>
        </a-dropdown>
      </a-space></div>
    </a-layout-header>
    <a-layout class="app-body">
      <a-layout-sider :width="224" :collapsed-width="54" :collapsed="collapsed" class="app-sider" theme="light">
        <div v-if="!collapsed" class="sider-title">核心业务导航</div>
        <a-menu mode="inline" theme="light" :selected-keys="selectedKeys" :open-keys="openKeys" class="app-menu">
          <template v-for="section in visibleMenuSections" :key="section.key">
            <a-menu-item v-if="section.items.length === 1" :key="section.items[0]?.to"><template #icon><component :is="sectionIcons[section.key]" /></template><router-link :to="section.items[0]?.to || '/dashboard'">{{ section.title }}</router-link></a-menu-item>
            <a-sub-menu v-else :key="section.key" :title="section.title"><template #icon><component :is="sectionIcons[section.key]" /></template><a-menu-item v-for="item in section.items" :key="item.to"><router-link :to="item.to">{{ item.title }}</router-link></a-menu-item></a-sub-menu>
          </template>
        </a-menu>
      </a-layout-sider>
      <a-layout class="app-main"><a-layout-content class="app-content" :class="{ 'app-content--dashboard': route.path === '/dashboard' }"><a-breadcrumb class="app-breadcrumb"><a-breadcrumb-item v-for="item in breadcrumbItems" :key="item">{{ item }}</a-breadcrumb-item></a-breadcrumb><router-view /></a-layout-content></a-layout>
    </a-layout>
  </a-layout>
  <a-modal v-model:open="passwordModalOpen" title="修改密码" :confirm-loading="passwordSaving" @ok="submitPassword">
    <a-form layout="vertical"><a-form-item label="当前密码" required><a-input-password v-model:value="passwordForm.currentPassword" autocomplete="current-password" /></a-form-item><a-form-item label="新密码" required><a-input-password v-model:value="passwordForm.newPassword" autocomplete="new-password" /></a-form-item><a-form-item label="确认新密码" required><a-input-password v-model:value="passwordForm.confirmPassword" autocomplete="new-password" /></a-form-item></a-form>
  </a-modal>
</template>
<style scoped>
.app-shell {
  min-height: 100vh;
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(214, 111, 34, 0.12), transparent 24rem),
    linear-gradient(180deg, #f8f5f1 0%, #f4f7fb 100%);
}

.app-body {
  height: calc(100vh - 48px);
  min-height: 0;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid rgba(145, 158, 171, 0.15);
  backdrop-filter: blur(18px);
  height: 48px;
  line-height: 48px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.collapse-btn {
  color: #5d6b7b;
  font-size: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  line-height: 1;
}

.brand-logo {
  width: auto;
  height: 31px;
  height: 48px;
  display: block;
  object-fit: contain;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brand-copy strong {
  color: #1f2937;
  font-size: 15px;
  letter-spacing: 0.04em;
}

.brand-copy span {
  color: #7b8794;
  font-size: 10px;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-icon {
  font-size: 18px;
  color: #5d6b7b;
}

.login-avatar {
  background-color: #d66f22;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #344054;
  background: rgba(255, 248, 240, 0.92);
  border: 1px solid rgba(214, 111, 34, 0.18);
  border-radius: 999px;
  padding: 3px 9px 3px 3px;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
}

.app-sider {
  background: rgba(255, 255, 255, 0.92);
  border-right: 1px solid rgba(145, 158, 171, 0.15);
}

.sider-title {
  color: #98a2b3;
  padding: 14px 20px 6px;
  font-size: 10px;
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
  padding: 6px 0 10px;
}

:deep(.ant-menu-submenu-title) {
  height: 36px !important;
  line-height: 36px !important;
  margin: 3px 13px;
  width: calc(100% - 26px);
  border-radius: 10px;
  font-size: 12px;
}

:deep(.ant-menu-inline .ant-menu-item) {
  height: 35px;
  line-height: 35px;
  margin: 3px 13px;
  width: calc(100% - 26px);
  border-radius: 10px;
  font-size: 12px;
}

:deep(.ant-menu-item-icon),
:deep(.ant-menu-submenu-title .anticon) {
  font-size: 14px;
}

:deep(.ant-menu-title-content) {
  font-weight: 500;
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
  padding: 11px;
  background: transparent;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.app-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
  min-height: auto;
  overflow: auto;
}

.app-content--dashboard {
  overflow: hidden;
}

.app-breadcrumb {
  flex: none;
  margin-bottom: 9px;
}

:deep(.ant-breadcrumb) {
  font-size: 12px;
}

@media (max-width: 1024px) {
  .app-body {
    height: calc(100vh - 52px);
  }

  .app-header {
    height: 52px;
    line-height: 52px;
  }

  .brand-logo {
    height: 34px;
  }

  .app-main {
    padding: 13px;
  }

  .sider-title {
    padding: 16px 22px 7px;
  }

  :deep(.ant-menu-inline) {
    padding: 7px 0 11px;
  }

  :deep(.ant-menu-submenu-title) {
    height: 40px !important;
    line-height: 40px !important;
    margin: 4px 13px;
    border-radius: 11px;
  }

  :deep(.ant-menu-inline .ant-menu-item) {
    height: 38px;
    line-height: 38px;
    margin: 4px 13px;
    border-radius: 11px;
  }

  .app-breadcrumb {
    margin-bottom: 11px;
  }

  :deep(.ant-breadcrumb) {
    font-size: 13px;
  }
}

:deep(.ant-breadcrumb-link) {
  color: #667085;
  font-weight: 500;
}

:deep(.ant-breadcrumb-separator) {
  color: #c0c7d1;
}
</style>
