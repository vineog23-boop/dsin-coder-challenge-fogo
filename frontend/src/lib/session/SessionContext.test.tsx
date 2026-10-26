import { act, cleanup, render, screen } from '@testing-library/react'
import { useEffect } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '../api/client'
import { canEdit, useSession, type SessionContextValue } from './session'
import { SessionProvider } from './SessionContext'

function json(body: unknown, status: number) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

let ctx: SessionContextValue

function Probe() {
  const value = useSession()
  useEffect(() => {
    ctx = value
  })
  return <span>{value.session ? `${value.session.username}:${value.session.role}` : 'anon'}</span>
}

function renderProvider() {
  render(
    <SessionProvider>
      <Probe />
    </SessionProvider>,
  )
}

async function loginAs(role: 'MANAGER' | 'OPERATOR') {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ accessToken: 'tok', expiresIn: 3600, role }, 200)))
  await act(() => ctx.login('gestor', 'gestor123'))
}

afterEach(() => {
  cleanup()
  sessionStorage.clear()
  vi.unstubAllGlobals()
})

describe('SessionProvider', () => {
  it('deveGuardarSessaoQuandoLoginOk', async () => {
    renderProvider()

    await loginAs('MANAGER')

    expect(screen.getByText('gestor:MANAGER')).toBeTruthy()
    expect(canEdit(ctx.session)).toBe(true)
    expect(sessionStorage.getItem('fogo.session')).toContain('"token":"tok"')
  })

  it('deveRestaurarSessaoQuandoRecarregaAPagina', async () => {
    renderProvider()
    await loginAs('OPERATOR')
    cleanup()

    renderProvider()

    expect(screen.getByText('gestor:OPERATOR')).toBeTruthy()
    expect(canEdit(ctx.session)).toBe(false)
  })

  it('naoDeveRestaurarSessaoQuandoTokenExpirou', () => {
    sessionStorage.setItem(
      'fogo.session',
      JSON.stringify({ token: 'old', role: 'MANAGER', username: 'gestor', expiresAt: Date.now() - 1 }),
    )

    renderProvider()

    expect(screen.getByText('anon')).toBeTruthy()
  })

  it('deveLimparSessaoQuandoLogout', async () => {
    renderProvider()
    await loginAs('MANAGER')

    act(() => ctx.logout())

    expect(screen.getByText('anon')).toBeTruthy()
    expect(sessionStorage.getItem('fogo.session')).toBeNull()
  })

  it('deveEnviarTokenEEncerrarSessaoQuandoApiDevolve401', async () => {
    renderProvider()
    await loginAs('MANAGER')
    const fetchMock = vi.fn().mockResolvedValue(json({ type: 'urn:fogo:problem:unauthorized', title: 'Não autenticado' }, 401))
    vi.stubGlobal('fetch', fetchMock)

    await act(() => api('/equipments').catch(() => undefined))

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer tok')
    expect(screen.getByText('anon')).toBeTruthy()
  })

  it('naoDeveCriarSessaoQuandoCredencialErrada', async () => {
    renderProvider()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(json({ type: 'urn:fogo:problem:invalid-credentials', title: 'Não autorizado', detail: 'Usuário ou senha inválidos.' }, 401)),
    )

    const error = await act(() => ctx.login('gestor', 'errada').catch((e: unknown) => e))

    expect((error as Error).message).toBe('Usuário ou senha inválidos.')
    expect(screen.getByText('anon')).toBeTruthy()
  })
})
