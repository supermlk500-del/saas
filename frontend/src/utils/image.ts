import { readAccessToken } from '@/utils/authToken'

const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '')

export const normalizeImagePath = (path?: string | null) => {
  if (!path) return ''
  return path.trim().replace(/\\/g, '/')
}

export const isLikelyResultImage = (path?: string | null) => normalizeImagePath(path).toLowerCase().includes('photo/results/')
export const isLikelySourceImage = (path?: string | null) => normalizeImagePath(path).toLowerCase().includes('photo/upload/')

export const resolveImageUrl = (path?: string | null) => {
  const normalized = normalizeImagePath(path)
  if (!normalized) return ''
  if (/^(blob:|data:|https?:\/\/)/i.test(normalized)) return normalized

  const base = trimTrailingSlash(import.meta.env.VITE_APP_BASE_API ?? '/prod-api')
  const withoutLeadingSlash = normalized.replace(/^\/+/, '')
  const token = readAccessToken()
  const separator = withoutLeadingSlash.includes('?') ? '&' : '?'
  return token
    ? `${base}/${withoutLeadingSlash}${separator}access_token=${encodeURIComponent(token)}`
    : `${base}/${withoutLeadingSlash}`
}