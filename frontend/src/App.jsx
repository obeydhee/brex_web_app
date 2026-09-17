import { useEffect, useState } from 'react'
import { fetchItems, createItem, deleteItem } from './api'

export default function App() {
  const [items, setItems] = useState([])
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [quantity, setQuantity] = useState('0')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  function loadItems() {
    setLoading(true)
    fetchItems()
      .then(setItems)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadItems()
  }, [])

  async function handleSubmit(event) {
    event.preventDefault()
    if (!name.trim()) return
    try {
      await createItem({ name, description, quantity: Number(quantity) || 0 })
      setName('')
      setDescription('')
      setQuantity('0')
      loadItems()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleDelete(id) {
    try {
      await deleteItem(id)
      loadItems()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <main className="app">
      <h1>Brex Demo App</h1>
      <p className="subtitle">React frontend &rarr; Spring Boot API &rarr; SQLite database</p>

      <form className="item-form" onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Item name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <input
          type="text"
          placeholder="Description (optional)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <input
          type="number"
          min="0"
          placeholder="Quantity"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
        />
        <button type="submit">Add item</button>
      </form>

      {error && <p className="error">Error: {error}</p>}
      {loading && <p>Loading...</p>}

      <ul className="item-list">
        {items.map((item) => (
          <li key={item.id}>
            <div>
              <strong>{item.name}</strong>
              <span className="quantity-badge">Qty: {item.quantity}</span>
              {item.description && <p>{item.description}</p>}
            </div>
            <button onClick={() => handleDelete(item.id)}>Delete</button>
          </li>
        ))}
      </ul>
    </main>
  )
}
