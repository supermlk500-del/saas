import { defineStore } from 'pinia'

const APP_SHELL_STORAGE_KEY = 'zhihuitong_app_shell_v1'

type AppShellState = {
  collapsed: boolean
}

const readInitialState = (): AppShellState => {
  if (typeof window === 'undefined') {
    return { collapsed: false }
  }

  try {
    const raw = window.localStorage.getItem(APP_SHELL_STORAGE_KEY)
    if (!raw) {
      return { collapsed: false }
    }

    const parsed = JSON.parse(raw) as Partial<AppShellState>
    return {
      collapsed: Boolean(parsed.collapsed),
    }
  } catch {
    return { collapsed: false }
  }
}

export const useAppStore = defineStore('app', {
  state: (): AppShellState => readInitialState(),
  actions: {
    setCollapsed(value: boolean) {
      this.collapsed = value
      this.persist()
    },
    toggleCollapsed() {
      this.collapsed = !this.collapsed
      this.persist()
    },
    persist() {
      if (typeof window === 'undefined') {
        return
      }

      window.localStorage.setItem(
        APP_SHELL_STORAGE_KEY,
        JSON.stringify({
          collapsed: this.collapsed,
        }),
      )
    },
  },
})
