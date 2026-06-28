export const AUTH_TOKEN_KEY = 'zhihuitong_access_token'
export const readAccessToken = () => localStorage.getItem(AUTH_TOKEN_KEY) || ''
export const writeAccessToken = (token: string) => localStorage.setItem(AUTH_TOKEN_KEY, token)
export const removeAccessToken = () => localStorage.removeItem(AUTH_TOKEN_KEY)