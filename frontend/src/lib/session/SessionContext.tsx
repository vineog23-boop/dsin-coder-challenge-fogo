import { useCallback, useLayoutEffect, useMemo, useState, type ReactNode } from 'react'
import { login as loginRequest } from '../api/auth'
import { setAuthToken, setUnauthorizedHandler } from '../api/client'
import { readStoredSession, SessionContext, storeSession, type Session } from './session'

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => readStoredSession())

  const logout = useCallback(() => {
    storeSession(null)
    setSession(null)
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const response = await loginRequest(username, password)
    const next: Session = {
      token: response.accessToken,
      role: response.role,
      username,
      expiresAt: Date.now() + response.expiresIn * 1000,
    }
    storeSession(next)
    setSession(next)
  }, [])

  // Layout effect: o token chega ao cliente antes dos efeitos das telas filhas dispararem chamadas.
  useLayoutEffect(() => {
    setAuthToken(session?.token ?? null)
  }, [session])

  useLayoutEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  const value = useMemo(() => ({ session, login, logout }), [session, login, logout])

  return <SessionContext value={value}>{children}</SessionContext>
}
