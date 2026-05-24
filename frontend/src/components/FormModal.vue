<script setup lang="ts">
import { computed } from 'vue'

type Field = {
  label: string
  name: string
  type: string
  placeholder?: string
  options?: { label: string; value: string | number }[]
}

const props = defineProps<{
  open: boolean
  title: string
  model: Record<string, any>
  rules?: Record<string, any[]>
  fields: Field[]
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'ok'): void
}>()

const modalOpen = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val),
})
</script>

<template>
  <a-modal v-model:open="modalOpen" :title="title" ok-text="确认" cancel-text="取消" @ok="emit('ok')">
    <a-form :model="model" :rules="rules" layout="vertical">
      <template v-for="field in fields" :key="field.name">
        <a-form-item :label="field.label" :name="field.name">
          <a-input
            v-if="field.type === 'input'"
            v-model:value="model[field.name]"
            :placeholder="field.placeholder"
          />
          <a-input-number
            v-else-if="field.type === 'number'"
            v-model:value="model[field.name]"
            style="width: 100%"
          />
          <a-select
            v-else-if="field.type === 'select'"
            v-model:value="model[field.name]"
            :placeholder="field.placeholder"
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
            style="width: 100%"
          />
          <a-date-picker
            v-else-if="field.type === 'datetime'"
            v-model:value="model[field.name]"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
          <a-textarea
            v-else-if="field.type === 'textarea'"
            v-model:value="model[field.name]"
            :rows="3"
            :placeholder="field.placeholder"
          />
        </a-form-item>
      </template>
    </a-form>
  </a-modal>
</template>
