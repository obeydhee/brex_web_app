import { apiFetch } from './client'

export function sendPayment(token, payload) {
  return apiFetch('/api/payments', { method: 'POST', token, body: payload })
}

export function getHistory(token, window, page = 0) {
  const params = new URLSearchParams({ page: String(page) })
  if (window) params.set('window', window)
  return apiFetch(`/api/transactions/me?${params.toString()}`, { token })
}

export function getTransaction(token, id) {
  return apiFetch(`/api/transactions/${id}`, { token })
}

export function lookupUser(token, paymentName) {
  return apiFetch(`/api/users/lookup?paymentName=${encodeURIComponent(paymentName)}`, { token })
}
