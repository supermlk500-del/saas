<script setup lang="ts">
type Field = {
  label: string
  name: string
  type: 'input' | 'select' | 'date' | 'datetime' | 'number' | string
  placeholder?: string
  options?: { label: string; value: string | number }[]
  width?: string
}

defineProps<{
  model: Record<string, any>
  fields: Field[]
}>()

const emit = defineEmits<{
  (e: 'search'): void
  (e: 'reset'): void
}>()
</script>

<template>
  <a-form layout="inline" :model="model">
    <template v-for="field in fields" :key="field.name">
      <a-form-item :label="field.label">
        <a-input
          v-if="field.type === 'input'"
          v-model:value="model[field.name]"
          :placeholder="field.placeholder"
          :style="{ width: field.width || '180px' }"
        />
        <a-input-number
          v-else-if="field.type === 'number'"
          v-model:value="model[field.name]"
          :placeholder="field.placeholder"
          :style="{ width: field.width || '180px' }"
        />
        <a-select
          v-else-if="field.type === 'select'"
          v-model:value="model[field.name]"
          :placeholder="field.placeholder"
          :style="{ width: field.width || '160px' }"
        >
          <a-select-option
            v-for="opt in field.options || []"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </a-select-option>
        </a-select>
        <a-date-picker
          v-else-if="field.type === 'date'"
          v-model:value="model[field.name]"
          :style="{ width: field.width || '180px' }"
        />
        <a-date-picker
          v-else-if="field.type === 'datetime'"
          v-model:value="model[field.name]"
          show-time
          value-format="YYYY-MM-DD HH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          :style="{ width: field.width || '220px' }"
        />
      </a-form-item>
    </template>
    <a-form-item>
      <a-button type="primary" @click="emit('search')">查询</a-button>
      <a-button style="margin-left: 8px" @click="emit('reset')">重置</a-button>
      <slot name="actions" />
    </a-form-item>
  </a-form>
</template>

<style scoped>
:deep(.ant-form-item-label > label) {
  color: #64748b;
  font-weight: 500;
}

:deep(.ant-input),
:deep(.ant-select-selector) {
  border-radius: 8px !important;
}
</style>
