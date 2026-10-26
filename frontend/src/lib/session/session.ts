import { createContext, useContext } from 'react'
import type { Role } from '../api/auth'

export type Session = {
  token: string
  role: Role
  username: string
  /** Instante (ms) em que o token expira; sessão vencida é descartada ao recarregar. */
  expiresAt: number
}

export type SessionContextValue = {
  session: Session | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

export const SessionContext = createContext<SessionContextValue | null>(null)

export function useSession(): SessionContextValue {
  const value = useContext(SessionContext)
  if (!value) {
    throw new Error('useSession precisa estar dentro de <SessionProvider>.')
  }
  return value
}

/** Só o gestor altera dados; a API também recusa (403), aqui é para não mostrar o botão. */
export function canEdit(session: Session | null): boolean {
  return session?.role === 'MANAGER'
}

const STORAGE_KEY = 'fogo.session'

export function readStoredSession(now = Date.now()): Session | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const session = JSON.parse(raw) as Session
    return session.token && session.expiresAt > now ? session : null
  } catch {
    return null
  }
}

export function storeSession(session: Session | null) {
  try {
    if (session) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    } else {
      sessionStorage.removeItem(STORAGE_KEY)
    }
  } catch {
    // sessionStorage bloqueado: a sessão segue só em memória
  }
}
