import { useState } from 'react'
import { AuthProvider, useAuth } from './context/AuthContext'
import LoginView from './views/LoginView'
import SignupView from './views/SignupView'
import DashboardView from './views/DashboardView'
import SendPaymentView from './views/SendPaymentView'
import HistoryView from './views/HistoryView'

const VIEWS = [
  { value: 'dashboard', label: 'Dashboard' },
  { value: 'send', label: 'Send' },
  { value: 'history', label: 'History' },
]

function AuthenticatedApp() {
  const { profile, logout } = useAuth()
  const [view, setView] = useState('dashboard')

  return (
    <div className="app">
      <header className="app-header">
        <h1>Brex Pay</h1>
        <nav className="app-nav">
          {VIEWS.map((v) => (
            <button key={v.value} className={v.value === view ? 'active' : ''} onClick={() => setView(v.value)}>
              {v.label}
            </button>
          ))}
        </nav>
        <div className="app-user">
          <span>@{profile.paymentName}</span>
          <button onClick={logout}>Log out</button>
        </div>
      </header>
      <main className="app-main">
        {view === 'dashboard' && <DashboardView />}
        {view === 'send' && <SendPaymentView />}
        {view === 'history' && <HistoryView />}
      </main>
    </div>
  )
}

function UnauthenticatedApp() {
  const [mode, setMode] = useState('login')

  return (
    <div className="app app-unauth">
      <h1>Brex Pay</h1>
      {mode === 'login' ? (
        <LoginView onSwitchToSignup={() => setMode('signup')} />
      ) : (
        <SignupView onSwitchToLogin={() => setMode('login')} />
      )}
    </div>
  )
}

function Shell() {
  const { token } = useAuth()
  return token ? <AuthenticatedApp /> : <UnauthenticatedApp />
}

export default function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  )
}
