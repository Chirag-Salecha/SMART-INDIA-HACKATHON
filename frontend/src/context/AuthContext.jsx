import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

const STORAGE_KEY = 'bcvp_auth_user'

/**
 * Mock authentication for the MVP milestone. No real backend auth exists yet
 * (by design - see project requirements). This just gates the Dashboard
 * behind a login screen so the app has a realistic flow to demo.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : null
  })

  const login = (username) => {
    const mockUser = { username, role: 'Procurement Officer' }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(mockUser))
    setUser(mockUser)
  }

  const logout = () => {
    localStorage.removeItem(STORAGE_KEY)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
