<script setup lang="ts">
type ActionItem = { key: string; label: string; type?: string }
const props = defineProps<{ actions: ActionItem[] }>()
const emit = defineEmits<{ (e: 'action', key: string): void }>()
</script>

<template>
  <a-space>
    <template v-for="act in props.actions" :key="act.key">
      <a-button :type="act.type || 'default'" @click="() => emit('action', act.key)">
        {{ act.label }}
      </a-button>
    </template>
  </a-space>
</template>
