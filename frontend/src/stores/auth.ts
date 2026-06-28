import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getCurrentUser, logout as logoutRequest, type CurrentUser } from '@/api/system/auth'
import { readAccessToken, writeAccessToken, removeAccessToken } from '@/utils/authToken'


export const useAuthStore = defineStore('auth', () => {
  const token = ref(readAccessToken())
  const user = ref<CurrentUser | null>(null)
  const roleKey = ref('')
  const permissions = ref<string[]>([])
  const menuKeys = ref<string[]>([])
  const initialized = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value && user.value))
  const isAdmin = computed(() => roleKey.value === 'admin')

  function setToken(value: string) {
    token.value = value
    user.value = null
    roleKey.value = ''
    permissions.value = []
    menuKeys.value = []
    initialized.value = false
    writeAccessToken(value)
  }

  function clearSession() {
    token.value = ''
    user.value = null
    roleKey.value = ''
    permissions.value = []
    menuKeys.value = []
    initialized.value = true
    removeAccessToken()
  }

  async function initialize() {
    if (initialized.value) return
    if (!token.value) {
      initialized.value = true
      return
    }
    try {
      const response = await getCurrentUser()
      user.value = response.data.user
      roleKey.value = response.data.roleKey
      permissions.value = response.data.permissions ?? []
      menuKeys.value = response.data.menuKeys ?? []
    } catch {
      clearSession()
    } finally {
      initialized.value = true
    }
  }

  function hasPermission(permission?: string) {
    if (!permission) return true
    return isAdmin.value || permissions.value.includes('*:*:*') || permissions.value.includes(permission)
  }

  function hasMenu(menuKey?: string) {
    if (!menuKey) return true
    return isAdmin.value || menuKeys.value.includes(menuKey)
  }

  async function logout() {
    try {
      await logoutRequest()
    } finally {
      clearSession()
    }
  }

  return {
    token, user, roleKey, permissions, menuKeys, initialized,
    isAuthenticated, isAdmin, setToken, clearSession, initialize,
    hasPermission, hasMenu, logout,
  }
})
