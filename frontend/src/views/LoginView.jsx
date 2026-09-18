import { useState } from 'react'
import { login as loginRequest } from '../api/auth'
import { useAuth } from '../context/AuthContext'

export default function LoginView({ onSwitchToSignup }) {
  const { login } = useAuth()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const { token, profile } = await loginRequest({ identifier, password })
      login(token, profile)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <h2>Log in</h2>
      <input
        type="text"
        placeholder="Email or payment name"
        value={identifier}
        onChange={(e) => setIdentifier(e.target.value)}
        required
      />
      <input
        type="password"
        placeholder="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        required
      />
      {error && <p className="error">Error: {error}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Logging in...' : 'Log in'}
      </button>
      <p className="auth-switch">
        No account? <button type="button" onClick={onSwitchToSignup}>Sign up</button>
      </p>
    </form>
  )
}
