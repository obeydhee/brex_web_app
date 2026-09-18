import { useState } from 'react'
import { sendPayment } from '../api/payments'
import { useAuth } from '../context/AuthContext'

export default function SendPaymentView() {
  const { token } = useAuth()
  const [receiverPaymentName, setReceiverPaymentName] = useState('')
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    setResult(null)
    try {
      const transaction = await sendPayment(token, {
        receiverPaymentName: receiverPaymentName.replace(/^@/, ''),
        amount: Number(amount),
        note: note || undefined,
      })
      setResult(transaction)
      if (transaction.status === 'SUCCESS') {
        setReceiverPaymentName('')
        setAmount('')
        setNote('')
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="send-payment">
      <form className="send-payment-form" onSubmit={handleSubmit}>
        <h2>Send payment</h2>
        <input
          type="text"
          placeholder="Recipient payment name (e.g. jane_doe)"
          value={receiverPaymentName}
          onChange={(e) => setReceiverPaymentName(e.target.value)}
          required
        />
        <input
          type="number"
          min="0.01"
          step="0.01"
          placeholder="Amount"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          required
        />
        <input
          type="text"
          placeholder="Note (optional)"
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />
        {error && <p className="error">Error: {error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Sending...' : 'Send'}
        </button>
      </form>

      {result && (
        <div className={`payment-result payment-result-${result.status.toLowerCase()}`}>
          {result.status === 'SUCCESS' ? (
            <p>Sent ${Number(result.amount).toFixed(2)} to @{result.receiverPaymentName}.</p>
          ) : (
            <p>Payment failed: insufficient funds sending ${Number(result.amount).toFixed(2)} to @{result.receiverPaymentName}.</p>
          )}
        </div>
      )}
    </section>
  )
}
