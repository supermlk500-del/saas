import { createRouter, createWebHistory } from 'vue-router'
import { appRoutes } from '@/router/routes'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { public: true, title: '登录' } },
    {
      path: '/',
      component: () => import('@/layout/AppShell.vue'),
      redirect: '/dashboard',
      children: appRoutes,
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) return auth.token ? '/dashboard' : true
  if (!auth.token) return { path: '/login', query: { redirect: to.fullPath } }
  await auth.initialize()
  if (!auth.isAuthenticated) return { path: '/login', query: { redirect: to.fullPath } }
  const menuKey = typeof to.meta.menuKey === 'string'
    ? to.meta.menuKey
    : typeof to.meta.sectionKey === 'string' ? to.meta.sectionKey : undefined
  if (!auth.hasMenu(menuKey)) return '/dashboard'
  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '工作台')} - 织慧通`
})

export default router