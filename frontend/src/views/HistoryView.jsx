import { useEffect, useState } from 'react'
import { getHistory } from '../api/payments'
import { useAuth } from '../context/AuthContext'

const WINDOWS = [
  { value: '1D', label: '1 day' },
  { value: '1W', label: '1 week' },
  { value: '1M', label: '1 month' },
]

export default function HistoryView() {
  const { token, profile } = useAuth()
  const [windowFilter, setWindowFilter] = useState('1W')
  const [page, setPage] = useState(0)
  const [history, setHistory] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    getHistory(token, windowFilter, page)
      .then(setHistory)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [token, windowFilter, page])

  function handleWindowChange(newWindow) {
    setWindowFilter(newWindow)
    setPage(0)
  }

  return (
    <section className="history">
      <div className="history-controls">
        <h2>Transaction history</h2>
        <div className="window-tabs">
          {WINDOWS.map((w) => (
            <button
              key={w.value}
              className={w.value === windowFilter ? 'active' : ''}
              onClick={() => handleWindowChange(w.value)}
            >
              {w.label}
            </button>
          ))}
        </div>
      </div>

      {error && <p className="error">Error: {error}</p>}
      {loading && <p>Loading...</p>}

      <ul className="transaction-list">
        {history?.content.map((tx) => {
          const isOutgoing = tx.senderPaymentName === profile.paymentName
          return (
            <li key={tx.id} className={`transaction-status-${tx.status.toLowerCase()}`}>
              <div className="transaction-parties">
                <strong>{isOutgoing ? `To @${tx.receiverPaymentName}` : `From @${tx.senderPaymentName}`}</strong>
                {tx.note && <p className="transaction-note">{tx.note}</p>}
              </div>
              <span className="transaction-amount">
                {isOutgoing ? '-' : '+'}${Number(tx.amount).toFixed(2)}
              </span>
              <span className="transaction-status">{tx.status}</span>
              <span className="transaction-date">{new Date(tx.createdAt).toLocaleString()}</span>
            </li>
          )
        })}
        {history?.content.length === 0 && <li className="empty">No transactions in this window.</li>}
      </ul>

      {history && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Prev</button>
          <span>Page {history.number + 1} of {Math.max(history.totalPages, 1)}</span>
          <button disabled={history.number + 1 >= history.totalPages} onClick={() => setPage((p) => p + 1)}>Next</button>
        </div>
      )}
    </section>
  )
}
