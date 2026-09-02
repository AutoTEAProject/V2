import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api, getToken, setToken } from '../api/client'
import type { AuthResponse } from '../api/types'

const USER_KEY = 'autotea_user'

interface AuthContextValue {
  user: AuthResponse | null
  loginWithGoogleIdToken: (idToken: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function loadStoredUser(): AuthResponse | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthResponse
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(() => (getToken() ? loadStoredUser() : null))

  const loginWithGoogleIdToken = useCallback(async (idToken: string) => {
    const response = await api.loginWithGoogle(idToken)
    setToken(response.token)
    localStorage.setItem(USER_KEY, JSON.stringify(response))
    setUser(response)
  }, [])

  const logout = useCallback(() => {
    setToken(null)
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, loginWithGoogleIdToken, logout }),
    [user, loginWithGoogleIdToken, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
