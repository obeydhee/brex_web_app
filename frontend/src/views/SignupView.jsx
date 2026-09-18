import { useState } from 'react'
import { signup } from '../api/auth'
import { useAuth } from '../context/AuthContext'

const initialForm = {
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  paymentName: '',
  password: '',
}

export default function SignupView({ onSwitchToLogin }) {
  const { login } = useAuth()
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  function update(field) {
    return (event) => setForm((prev) => ({ ...prev, [field]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const { token, profile } = await signup(form)
      login(token, profile)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <h2>Create account</h2>
      <input type="text" placeholder="First name" value={form.firstName} onChange={update('firstName')} required />
      <input type="text" placeholder="Last name" value={form.lastName} onChange={update('lastName')} required />
      <input type="email" placeholder="Email" value={form.email} onChange={update('email')} required />
      <input type="tel" placeholder="Phone number" value={form.phoneNumber} onChange={update('phoneNumber')} required />
      <input
        type="text"
        placeholder="Payment name (e.g. jane_doe)"
        value={form.paymentName}
        onChange={update('paymentName')}
        pattern="[a-z0-9_]{3,20}"
        title="3-20 lowercase letters, digits, or underscores"
        required
      />
      <input type="password" placeholder="Password" value={form.password} onChange={update('password')} required />
      {error && <p className="error">Error: {error}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Creating account...' : 'Sign up'}
      </button>
      <p className="auth-switch">
        Already have an account? <button type="button" onClick={onSwitchToLogin}>Log in</button>
      </p>
    </form>
  )
}
