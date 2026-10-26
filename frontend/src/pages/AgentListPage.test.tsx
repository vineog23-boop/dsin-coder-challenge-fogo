import { cleanup, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Agent } from '../lib/api/agents'
import type { Page } from '../lib/api/equipments'
import { SessionContext, type Session } from '../lib/session/session'
import AgentListPage from './AgentListPage'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const agent = {
  id: 'a1',
  name: 'Maria Souza',
  registrationNumber: 'GM-100',
  agentType: 'MUNICIPAL_GUARD',
  agency: 'MUNICIPAL_GUARD',
  status: 'AVAILABLE',
  baseLocation: { region: 'NORTH', neighborhood: 'Jardim Bongiovani' },
  vehicle: null,
} as unknown as Agent

function pageOf(content: Agent[]): Page<Agent> {
  return { content, page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 } }
}

function sessionOf(role: 'MANAGER' | 'OPERATOR'): Session {
  return { token: 't', role, username: 'u', expiresAt: Date.now() + 60_000 }
}

function renderList(url: string, role: 'MANAGER' | 'OPERATOR' = 'MANAGER') {
  const fetchMock = vi.fn(async () => json(pageOf([agent])))
  vi.stubGlobal('fetch', fetchMock)
  render(
    <SessionContext.Provider value={{ session: sessionOf(role), login: vi.fn(), logout: vi.fn() }}>
      <MemoryRouter initialEntries={[url]}>
        <AgentListPage />
      </MemoryRouter>
    </SessionContext.Provider>,
  )
  return fetchMock
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('AgentListPage', () => {
  it('deveEnviarFiltrosDaUrlParaApiQuandoAbre', async () => {
    const fetchMock = renderList('/agentes?agency=MUNICIPAL_GUARD&status=AVAILABLE')

    await screen.findByText('Maria Souza')

    const url = String((fetchMock.mock.calls as unknown[][])[0][0])
    expect(url).toContain('/api/agents?')
    expect(url).toContain('agency=MUNICIPAL_GUARD')
    expect(url).toContain('status=AVAILABLE')
  })

  it('deveMostrarRotulosEmPortuguesQuandoListaCarrega', async () => {
    renderList('/agentes')

    const row = (await screen.findByText('Maria Souza')).closest('tr') as HTMLElement

    expect(within(row).getByText('Guarda municipal')).toBeTruthy()
    expect(within(row).getByText('Disponível')).toBeTruthy()
  })

  it('deveMostrarBotaoNovoSoQuandoGestor', async () => {
    renderList('/agentes', 'MANAGER')
    await screen.findByText('Maria Souza')
    expect(screen.getByRole('link', { name: 'Novo agente' }).getAttribute('href')).toBe('/agentes/novo')

    cleanup()
    renderList('/agentes', 'OPERATOR')
    await screen.findByText('Maria Souza')

    expect(screen.queryByRole('link', { name: 'Novo agente' })).toBeNull()
  })
})
