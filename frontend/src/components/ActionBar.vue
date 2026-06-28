<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
type ActionItem = { key: string; label: string; type?: string; permission?: string }
const props = defineProps<{ actions: ActionItem[] }>()
const authStore = useAuthStore()
const emit = defineEmits<{ (e: 'action', key: string): void }>()
</script>

<template>
  <a-space>
    <template v-for="act in props.actions" :key="act.key">
      <a-button v-if="authStore.hasPermission(act.permission)" :type="act.type || 'default'" @click="() => emit('action', act.key)">
        {{ act.label }}
      </a-button>
    </template>
  </a-space>
</template>
