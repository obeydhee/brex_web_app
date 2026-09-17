const BASE_URL = '/api/items'

async function handleResponse(response) {
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || `Request failed with status ${response.status}`)
  }
  if (response.status === 204) return null
  return response.json()
}

export function fetchItems() {
  return fetch(BASE_URL).then(handleResponse)
}

export function createItem(item) {
  return fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(item),
  }).then(handleResponse)
}

export function deleteItem(id) {
  return fetch(`${BASE_URL}/${id}`, { method: 'DELETE' }).then(handleResponse)
}
