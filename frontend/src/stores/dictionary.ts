import { defineStore } from 'pinia'
import {
  batchStatusOptions,
  enabledStatusOptions,
  exceptionLevelOptions,
  exceptionStatusOptions,
  inspectTypeOptions,
  machineStatusOptions,
  planStatusOptions,
  planStepStatusOptions,
  resultJudgeOptions,
} from '@/constants/dictionaries'
import type { DictionaryOption } from '@/types/dictionary'

export const useDictionaryStore = defineStore('dictionary', {
  state: () => ({
    batchStatusOptions,
    planStatusOptions,
    planStepStatusOptions,
    machineStatusOptions,
    inspectTypeOptions,
    resultJudgeOptions,
    exceptionLevelOptions,
    exceptionStatusOptions,
    enabledStatusOptions,
  }),
  getters: {
    getLabel:
      () =>
      <T extends string | number>(options: DictionaryOption<T>[], value?: T | null): string => {
        const target = options.find((item) => item.value === value)
        return target?.label ?? String(value ?? '-')
      },
  },
})
