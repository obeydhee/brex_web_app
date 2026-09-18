import { apiFetch } from './client'

export function getBalance(token) {
  return apiFetch('/api/accounts/me', { token })
}

export function deposit(token, amount) {
  return apiFetch('/api/accounts/me/deposit', { method: 'POST', token, body: { amount } })
}

export function withdraw(token, amount) {
  return apiFetch('/api/accounts/me/withdraw', { method: 'POST', token, body: { amount } })
}

export function getLedger(token, page = 0) {
  return apiFetch(`/api/accounts/me/ledger?page=${page}`, { token })
}
