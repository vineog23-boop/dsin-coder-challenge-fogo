import { api } from './client'

export type Role = 'MANAGER' | 'OPERATOR'

export type LoginResponse = {
  accessToken: string
  /** Segundos até o token expirar. */
  expiresIn: number
  role: Role
}

export function login(username: string, password: string): Promise<LoginResponse> {
  return api<LoginResponse>('/auth/login', { method: 'POST', body: { username, password } })
}
