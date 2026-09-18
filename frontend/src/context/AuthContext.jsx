import { createContext, useCallback, useContext, useState } from 'react'

const AuthContext = createContext(null)

function readStoredProfile() {
  try {
    const stored = localStorage.getItem('profile')
    return stored ? JSON.parse(stored) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => {
    try {
      return localStorage.getItem('token')
    } catch {
      return null
    }
  })
  const [profile, setProfile] = useState(readStoredProfile)

  const login = useCallback((newToken, newProfile) => {
    try {
      localStorage.setItem('token', newToken)
      localStorage.setItem('profile', JSON.stringify(newProfile))
    } catch {
      // localStorage unavailable (private browsing, etc.) — session still
      // works for this page load, just won't survive a refresh.
    }
    setToken(newToken)
    setProfile(newProfile)
  }, [])

  const logout = useCallback(() => {
    try {
      localStorage.removeItem('token')
      localStorage.removeItem('profile')
    } catch {
      // ignore
    }
    setToken(null)
    setProfile(null)
  }, [])

  return (
    <AuthContext.Provider value={{ token, profile, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
