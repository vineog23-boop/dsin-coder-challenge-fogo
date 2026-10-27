import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import OperationsPage from './OperationsPage'

vi.mock('../components/OperationsMap', () => ({
  default: ({ equipments, agents }: { equipments: unknown[]; agents: unknown[] }) => (
    <p>
      mapa: {equipments.length} equipamentos, {agents.length} agentes
    </p>
  ),
}))

function json(body: unknown, status = 200, contentType = 'application/json') {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } })
}

const MAP = {
  equipments: [
    { id: 'e1', type: 'RADAR', serialNumber: 'RAD-1', status: 'MALFUNCTION', maintenanceNeed: 'NONE', latitude: -22.1, longitude: -51.4, region: 'NORTH' },
    { id: 'e2', type: 'TAXI', serialNumber: 'TX-1', status: 'OPERATIONAL', maintenanceNeed: 'NONE', latitude: -22.2, longitude: -51.4, region: 'SOUTH' },
  ],
  agents: [
    { id: 'a1', name: 'Ana', agentType: 'TRAFFIC_AGENT', status: 'AVAILABLE', latitude: -22.1, longitude: -51.4, positionSource: 'CURRENT', positionUpdatedAt: null, vehiclePlate: null, region: 'NORTH' },
  ],
  truncated: false,
}

const SUMMARY = {
  equipments: {
    total: 40,
    byStatus: { NOT_INSTALLED: 0, OPERATIONAL: 30, MALFUNCTION: 7, UNDER_MAINTENANCE: 1, DEACTIVATED: 1, STOLEN: 1 },
    immediateMaintenance: 3,
    overdueMaintenance: 5,
    upcomingMaintenance: 2,
  },
  agents: { total: 9, active: 4, byStatus: { AVAILABLE: 3, ON_OCCURRENCE: 1, OFF_DUTY: 3, ON_LEAVE: 2 } },
  regions: [{ region: 'NORTH', equipmentsInMalfunction: 2, activeAgents: 0 }],
  generatedAt: '2026-01-01T00:00:00Z',
}

const ALERTS = [
  { kind: 'EQUIPMENT_MALFUNCTION', severity: 'HIGH', message: 'Radar RAD-1 com falha', region: 'NORTH', equipmentId: 'e1', equipmentType: 'RADAR', since: null },
  { kind: 'REGION_WITHOUT_ACTIVE_AGENT', severity: 'MEDIUM', message: 'Região Norte sem agente ativo', region: 'NORTH', equipmentId: null, equipmentType: null, since: null },
  { kind: 'MAINTENANCE_DUE_SOON', severity: 'LOW', message: 'Manutenção do equipamento CAN-9 prevista para 30/10/2026.', region: 'WEST', equipmentId: 'e9', equipmentType: 'AUTOMATED_GATE', since: null },
]

function alertsPage(content: unknown[]) {
  return { content, page: { size: 50, number: 0, totalElements: content.length, totalPages: 1 } }
}

type Routes = { map?: () => Response; alerts?: () => Response }

/** Roteia por URL; cada rota pode ser trocada por teste. */
function stubApi({ map = () => json(MAP), alerts = () => json(alertsPage(ALERTS)) }: Routes = {}) {
  const mock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input)
    if (url.startsWith('/api/operations/map')) return map()
    if (url === '/api/operations/summary') return json(SUMMARY)
    if (url.startsWith('/api/operations/alerts')) return alerts()
    if (url === '/api/operations/neighborhoods') return json(['Centro', 'Vila Marcondes'])
    throw new Error(`fetch inesperado: ${url}`)
  })
  vi.stubGlobal('fetch', mock)
  return mock
}

function mapUrls(mock: ReturnType<typeof stubApi>): string[] {
  return (mock.mock.calls as unknown[][]).map((c) => String(c[0])).filter((u) => u.startsWith('/api/operations/map'))
}

function UrlProbe() {
  const location = useLocation()
  return <output data-testid="url">{location.pathname + location.search}</output>
}

function renderPage(url = '/central') {
  render(
    <MemoryRouter initialEntries={[url]}>
      <OperationsPage />
      <UrlProbe />
    </MemoryRouter>,
  )
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

describe('OperationsPage', () => {
  it('deveMostrarIndicadoresAlertasEMapaQuandoCarrega', async () => {
    stubApi()

    renderPage()

    await screen.findByText('mapa: 2 equipamentos, 1 agentes')
    const cards = within(screen.getByRole('list', { name: 'Indicadores' }))
    expect(cards.getByText('Equipamentos com falha').parentElement?.textContent).toContain('7')
    expect(cards.getByText('Manutenção imediata').parentElement?.textContent).toContain('3')
    expect(cards.getByText('Manutenção atrasada').parentElement?.textContent).toContain('5')
    expect(cards.getByText('Agentes em atividade').parentElement?.textContent).toContain('4')
    expect(cards.getByText('Equipamentos').parentElement?.textContent).toContain('40')
    expect(cards.getByText('Operacionais').parentElement?.textContent).toContain('30')
    // indisponíveis = com falha + em manutenção + roubados
    expect(cards.getByText('Indisponíveis').parentElement?.textContent).toContain('9')
    // 30 operacionais de 39 em uso (fora desativados e não instalados)
    expect(cards.getByText('Disponibilidade').parentElement?.textContent).toContain('77%')
    expect(cards.getByText('Manutenção nos próximos 7 dias').parentElement?.textContent).toContain('2')
    const lowAlert = screen.getByRole('link', { name: 'Manutenção do equipamento CAN-9 prevista para 30/10/2026.' })
    expect(lowAlert.getAttribute('href')).toBe('/equipamentos/e9')
    expect(lowAlert.closest('li')?.textContent).toContain('Baixa')
    expect(screen.getByRole('link', { name: 'Radar RAD-1 com falha' }).getAttribute('href')).toBe('/equipamentos/e1')
    expect(screen.getByRole('link', { name: 'Região Norte sem agente ativo' }).getAttribute('href')).toBe('/agentes?region=NORTH')
  })

  it('deveEnviarFiltrosDaUrlParaOMapaQuandoAbre', async () => {
    const fetchMock = stubApi()

    renderPage('/central?equipmentStatus=MALFUNCTION&region=NORTH')

    await screen.findByText(/^mapa:/)
    const url = mapUrls(fetchMock)[0]
    expect(url).toContain('equipmentStatus=MALFUNCTION')
    expect(url).toContain('region=NORTH')
    expect((screen.getByLabelText('Região') as HTMLSelectElement).value).toBe('NORTH')
  })

  it('deveEnviarBairroAoMapaEOferecerBairrosConhecidosQuandoFiltra', async () => {
    const fetchMock = stubApi()

    renderPage('/central?neighborhood=Vila+Marcondes')

    await screen.findByText(/^mapa:/)
    expect(mapUrls(fetchMock)[0]).toContain('neighborhood=Vila+Marcondes')
    const select = screen.getByLabelText('Bairro') as HTMLSelectElement
    await within(select).findByRole('option', { name: 'Centro' })
    expect(select.value).toBe('Vila Marcondes')
  })

  it('deveEsconderCamadaSemNovaBuscaQuandoDesmarcaAgentes', async () => {
    const fetchMock = stubApi()
    renderPage()
    await screen.findByText('mapa: 2 equipamentos, 1 agentes')
    const before = mapUrls(fetchMock).length

    fireEvent.click(screen.getByLabelText('Agentes'))

    await screen.findByText('mapa: 2 equipamentos, 0 agentes')
    expect(screen.getByTestId('url').textContent).toContain('hide=agents')
    expect(mapUrls(fetchMock)).toHaveLength(before)
  })

  it('deveManterDadosEAvisarQuandoAtualizacaoFalha', async () => {
    vi.useFakeTimers()
    let fail = false
    stubApi({
      map: () =>
        fail
          ? json({ type: 'urn:fogo:problem:internal', title: 'Erro interno', status: 500, detail: 'Falha no servidor.' }, 500, 'application/problem+json')
          : json(MAP),
    })
    renderPage()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    expect(screen.getByText('mapa: 2 equipamentos, 1 agentes')).toBeTruthy()
    fail = true

    await act(async () => {
      await vi.advanceTimersByTimeAsync(10_000)
    })

    expect(screen.getByRole('alert').textContent).toContain('Mostrando os últimos dados recebidos')
    expect(screen.getByRole('list', { name: 'Indicadores' })).toBeTruthy()
    expect(screen.getByText('mapa: 2 equipamentos, 1 agentes')).toBeTruthy()
  })

  it('deveMostrarMensagemQuandoNaoHaAlertas', async () => {
    stubApi({ alerts: () => json(alertsPage([])) })

    renderPage()

    expect(await screen.findByText('Nenhum alerta no momento.')).toBeTruthy()
    await waitFor(() => expect(screen.queryByText('Carregando…')).toBeNull())
  })
})
