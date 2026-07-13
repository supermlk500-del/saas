import type { IdValue } from '@/types/domain'

const compactId = (value?: IdValue | null, prefix = '', size = 5) => {
  const text = String(value ?? '').trim()
  if (!text) {
    return '-'
  }
  const body = text.length > size ? text.slice(-size) : text
  return `${prefix}${body}`
}

export const formatPlanId = (value?: IdValue | null) => compactId(value, 'P-', 5)

export const formatPlanStepId = (value?: IdValue | null) => compactId(value, 'S-', 5)

export const formatInspectionId = (value?: IdValue | null) => compactId(value, 'I-', 5)
