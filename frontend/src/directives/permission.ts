import type { App, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

const applyPermission = (element: HTMLElement, binding: DirectiveBinding<string>) => {
  const auth = useAuthStore()
  element.style.display = auth.hasPermission(binding.value) ? '' : 'none'
}

export const installPermissionDirective = (app: App) => {
  app.directive('permission', {
    mounted: applyPermission,
    updated: applyPermission,
  })
}