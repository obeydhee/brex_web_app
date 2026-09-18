import { useEffect, useState } from 'react'
import { deposit, getBalance, getLedger, withdraw } from '../api/accounts'
import { useAuth } from '../context/AuthContext'

function formatAmount(amount) {
  return `$${Number(amount).toFixed(2)}`
}

export default function DashboardView() {
  const { token } = useAuth()
  const [balance, setBalance] = useState(null)
  const [ledger, setLedger] = useState(null)
  const [ledgerPage, setLedgerPage] = useState(0)
  const [amount, setAmount] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  function loadAccount() {
    setLoading(true)
    Promise.all([getBalance(token), getLedger(token, ledgerPage)])
      .then(([accountRes, ledgerRes]) => {
        setBalance(accountRes.balance)
        setLedger(ledgerRes)
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(loadAccount, [token, ledgerPage])

  async function handleDeposit(event) {
    event.preventDefault()
    await runMutation(() => deposit(token, Number(amount)))
  }

  async function handleWithdraw(event) {
    event.preventDefault()
    await runMutation(() => withdraw(token, Number(amount)))
  }

  async function runMutation(action) {
    setSubmitting(true)
    setError(null)
    try {
      await action()
      setAmount('')
      setLedgerPage(0)
      loadAccount()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="dashboard">
      <div className="balance-card">
        <span className="balance-label">Balance</span>
        <span className="balance-value">{balance === null ? '...' : formatAmount(balance)}</span>
      </div>

      <form className="amount-form" onSubmit={handleDeposit}>
        <input
          type="number"
          min="0.01"
          step="0.01"
          placeholder="Amount"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          required
        />
        <button type="submit" disabled={submitting}>Add money</button>
        <button type="button" disabled={submitting} onClick={handleWithdraw}>Withdraw</button>
      </form>

      {error && <p className="error">Error: {error}</p>}
      {loading && <p>Loading...</p>}

      <h3>Recent account activity</h3>
      <ul className="ledger-list">
        {ledger?.content.map((entry) => (
          <li key={entry.id}>
            <span className={`entry-type entry-type-${entry.entryType.toLowerCase()}`}>{entry.entryType}</span>
            <span>{formatAmount(entry.amount)}</span>
            <span className="ledger-balance-after">balance after: {formatAmount(entry.balanceAfter)}</span>
            <span className="ledger-date">{new Date(entry.createdAt).toLocaleString()}</span>
          </li>
        ))}
        {ledger?.content.length === 0 && <li className="empty">No activity yet.</li>}
      </ul>

      {ledger && (
        <div className="pagination">
          <button disabled={ledgerPage === 0} onClick={() => setLedgerPage((p) => p - 1)}>Prev</button>
          <span>Page {ledger.number + 1} of {Math.max(ledger.totalPages, 1)}</span>
          <button disabled={ledger.number + 1 >= ledger.totalPages} onClick={() => setLedgerPage((p) => p + 1)}>Next</button>
        </div>
      )}
    </section>
  )
}
