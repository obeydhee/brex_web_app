import { apiFetch } from './client'

export function signup(payload) {
  return apiFetch('/api/users', { method: 'POST', body: payload })
}

export function login(payload) {
  return apiFetch('/api/auth/login', { method: 'POST', body: payload })
}
