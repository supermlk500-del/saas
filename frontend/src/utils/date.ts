export const parseDateTime = (value?: string | null) => {
  if (!value) {
    return null
  }

  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export const formatDateTime = (value?: string | null, withSeconds = true) => {
  const date = parseDateTime(value)
  if (!date) {
    return '-'
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return withSeconds
    ? `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    : `${year}-${month}-${day} ${hours}:${minutes}`
}

export const isAfterDateTime = (start?: string | null, end?: string | null) => {
  const startDate = parseDateTime(start)
  const endDate = parseDateTime(end)
  if (!startDate || !endDate) {
    return false
  }
  return startDate.getTime() > endDate.getTime()
}
