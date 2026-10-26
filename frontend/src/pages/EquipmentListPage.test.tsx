import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Equipment, Page } from '../lib/api/equipments'
import { SessionContext, type Session } from '../lib/session/session'
import EquipmentListPage from './EquipmentListPage'

function json(body: unknown, status = 200, contentType = 'application/json') {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } })
}

const equipment = {
  id: 'e1',
  type: 'RADAR',
  serialNumber: 'RAD-001',
  brand: 'Marca',
  model: 'Modelo',
  agency: 'MUNICIPAL_TRAFFIC',
  status: 'MALFUNCTION',
  maintenanceNeed: 'NONE',
  nextMaintenanceDate: null,
  maintenanceIntervalDays: null,
  location: { region: 'NORTH', neighborhood: 'Jardim Bongiovani' },
  radar: null,
  camera: null,
  truckScale: null,
  vehicle: null,
  scooter: null,
} as unknown as Equipment

function pageOf(content: Equipment[]): Page<Equipment> {
  return { content, page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 } }
}

function sessionOf(role: 'MANAGER' | 'OPERATOR'): Session {
  return { token: 't', role, username: 'u', expiresAt: Date.now() + 60_000 }
}

function UrlProbe() {
  const location = useLocation()
  return <output data-testid="url">{location.pathname + location.search}</output>
}

function renderList(url: string, role: 'MANAGER' | 'OPERATOR' = 'MANAGER') {
  render(
    <SessionContext.Provider value={{ session: sessionOf(role), login: vi.fn(), logout: vi.fn() }}>
      <MemoryRouter initialEntries={[url]}>
        <EquipmentListPage />
        <UrlProbe />
      </MemoryRouter>
    </SessionContext.Provider>,
  )
}

function stubFetch(handler: () => Response | Promise<Response>) {
  const mock = vi.fn(async () => handler())
  vi.stubGlobal('fetch', mock)
  return mock
}

function calledUrls(mock: ReturnType<typeof stubFetch>): string[] {
  return (mock.mock.calls as unknown[][]).map((call) => String(call[0]))
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('EquipmentListPage', () => {
  it('deveEnviarFiltrosDaUrlParaApiQuandoAbre', async () => {
    const fetchMock = stubFetch(() => json(pageOf([equipment])))

    renderList('/equipamentos?type=RADAR&region=NORTH&page=1')

    await screen.findByText('RAD-001')
    const url = calledUrls(fetchMock)[0]
    expect(url).toContain('type=RADAR')
    expect(url).toContain('region=NORTH')
    expect(url).toContain('page=1')
    expect((screen.getByLabelText('Tipo') as HTMLSelectElement).value).toBe('RADAR')
    expect((screen.getByLabelText('Região') as HTMLSelectElement).value).toBe('NORTH')
  })

  it('deveAtualizarUrlEVoltarParaPrimeiraPaginaQuandoMudaFiltro', async () => {
    const fetchMock = stubFetch(() => json(pageOf([equipment])))
    renderList('/equipamentos?type=RADAR&page=1')
    await screen.findByText('RAD-001')

    fireEvent.change(screen.getByLabelText('Situação'), { target: { value: 'MALFUNCTION' } })

    await waitFor(() => expect(calledUrls(fetchMock)).toHaveLength(2))
    const url = calledUrls(fetchMock)[1]
    expect(url).toContain('status=MALFUNCTION')
    expect(url).toContain('type=RADAR')
    expect(url).toContain('page=0')
    const shown = screen.getByTestId('url').textContent ?? ''
    expect(shown).toContain('status=MALFUNCTION')
    expect(shown).not.toContain('page=1')
  })

  it('deveMostrarRotulosEmPortuguesQuandoListaCarrega', async () => {
    stubFetch(() => json(pageOf([equipment])))

    renderList('/equipamentos')

    const row = (await screen.findByText('RAD-001')).closest('tr') as HTMLElement
    expect(within(row).getByText('Radar')).toBeTruthy()
    expect(within(row).getByText('Com falha')).toBeTruthy()
    expect(within(row).getByText(/Norte/)).toBeTruthy()
  })

  it('deveMostrarMensagemDeVazioQuandoNaoHaResultados', async () => {
    stubFetch(() => json(pageOf([])))

    renderList('/equipamentos?search=xyz')

    expect(await screen.findByText('Nenhum equipamento encontrado com esses filtros.')).toBeTruthy()
  })

  it('deveMostrarErroComTentarDeNovoQuandoApiFalha', async () => {
    let fail = true
    const fetchMock = stubFetch(() =>
      fail
        ? json({ type: 'urn:fogo:problem:internal', title: 'Erro interno', status: 500, detail: 'Falha no servidor.' }, 500, 'application/problem+json')
        : json(pageOf([equipment])),
    )
    renderList('/equipamentos')

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('Falha no servidor.')
    fail = false
    fireEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    await screen.findByText('RAD-001')
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('naoDeveMostrarBotaoNovoQuandoOperador', async () => {
    stubFetch(() => json(pageOf([equipment])))

    renderList('/equipamentos', 'OPERATOR')

    await screen.findByText('RAD-001')
    expect(screen.queryByRole('link', { name: 'Novo equipamento' })).toBeNull()
  })

  it('deveMostrarBotaoNovoQuandoGestor', async () => {
    stubFetch(() => json(pageOf([equipment])))

    renderList('/equipamentos', 'MANAGER')

    await screen.findByText('RAD-001')
    expect(screen.getByRole('link', { name: 'Novo equipamento' }).getAttribute('href')).toBe('/equipamentos/novo')
  })
})
