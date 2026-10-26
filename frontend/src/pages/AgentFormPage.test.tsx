import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Equipment } from '../lib/api/equipments'
import { SessionContext, type Session } from '../lib/session/session'
import AgentFormPage from './AgentFormPage'

vi.mock('../components/LocationPicker', () => ({
  default: ({ onPick }: { onPick?: (latitude: number, longitude: number) => void }) => (
    <button type="button" onClick={() => onPick?.(-22.1, -51.38)}>
      marcar ponto
    </button>
  ),
}))

function json(body: unknown, status: number, contentType = 'application/json') {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } })
}

function vehicle(id: string, plate: string, status: string): Equipment {
  return {
    id,
    type: 'TRAFFIC_AGENT_MOTORCYCLE',
    serialNumber: `SN-${plate}`,
    status,
    vehicle: { licensePlate: plate },
  } as unknown as Equipment
}

function pageOf(content: Equipment[]) {
  return { content, page: { size: 100, number: 0, totalElements: content.length, totalPages: 1 } }
}

const OPERATIONAL = vehicle('v1', 'AAA1A11', 'OPERATIONAL')

/** Roteia por URL: viaturas no GET de equipments, resultado do teste no POST de agents. */
function stubApi(
  vehicles: Equipment[] = [OPERATIONAL],
  post: () => Response = () => json({ id: 'novo-id' }, 201),
) {
  const mock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input)
    if (url.startsWith('/api/equipments')) return json(pageOf(vehicles), 200)
    if (url === '/api/agents') return post()
    throw new Error(`fetch inesperado: ${url}`)
  })
  vi.stubGlobal('fetch', mock)
  return mock
}

function equipmentUrls(mock: ReturnType<typeof stubApi>): string[] {
  return (mock.mock.calls as unknown[][]).map((call) => String(call[0])).filter((u) => u.startsWith('/api/equipments'))
}

function sessionOf(role: 'MANAGER' | 'OPERATOR'): Session {
  return { token: 't', role, username: 'u', expiresAt: Date.now() + 60_000 }
}

function renderForm() {
  render(
    <SessionContext.Provider value={{ session: sessionOf('MANAGER'), login: vi.fn(), logout: vi.fn() }}>
      <MemoryRouter initialEntries={['/agentes/novo']}>
        <Routes>
          <Route path="/agentes/novo" element={<AgentFormPage />} />
          <Route path="/agentes/:id" element={<p>detalhe</p>} />
          <Route path="/agentes" element={<p>lista</p>} />
        </Routes>
      </MemoryRouter>
    </SessionContext.Provider>,
  )
}

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

function vehicleSelect(): HTMLSelectElement {
  return document.getElementById('vehicleId') as HTMLSelectElement
}

async function waitVehiclesLoaded() {
  await waitFor(() => expect(vehicleSelect().disabled).toBe(false))
}

function fillMinimum() {
  type('Nome *', 'Maria')
  type('Matrícula *', 'MAT-1')
  type('Rua *', 'Rua A')
  type('Bairro *', 'Centro')
  fireEvent.click(screen.getByRole('button', { name: 'marcar ponto' }))
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('AgentFormPage', () => {
  it('deveBuscarSoViaturasCompativeisQuandoTrocaTipo', async () => {
    const mock = stubApi()
    renderForm()
    await waitVehiclesLoaded()

    type('Tipo *', 'MILITARY_POLICE')

    await waitFor(() => expect(equipmentUrls(mock)).toHaveLength(2))
    const [first, second] = equipmentUrls(mock)
    expect(first).toContain('type=TRAFFIC_AGENT_MOTORCYCLE')
    expect(first).toContain('agency=MUNICIPAL_TRAFFIC')
    expect(second).toContain('type=MILITARY_POLICE_VEHICLE')
    expect(second).toContain('agency=MILITARY_POLICE')
  })

  it('naoDeveOferecerViaturaRoubadaOuDesativada', async () => {
    stubApi([OPERATIONAL, vehicle('v2', 'BBB2B22', 'STOLEN'), vehicle('v3', 'CCC3C33', 'DEACTIVATED')])
    renderForm()

    await waitVehiclesLoaded()

    const labels = Array.from(vehicleSelect().options).map((o) => o.textContent ?? '')
    expect(labels).toHaveLength(2)
    expect(labels[0]).toBe('Sem viatura')
    expect(labels[1]).toContain('AAA1A11')
  })

  it('deveDesabilitarViaturaQuandoSituacaoInicialForaDeServico', async () => {
    stubApi()
    renderForm()
    await waitVehiclesLoaded()

    type('Situação inicial', 'OFF_DUTY')

    expect(vehicleSelect().disabled).toBe(true)
    expect(screen.getByText('Agente fora de serviço ou afastado não fica com viatura.')).toBeTruthy()
  })

  it('deveMostrarErro422NoCampoDeViaturaQuandoApiRecusa', async () => {
    const message = 'A viatura pertence a outro órgão.'
    stubApi([OPERATIONAL], () =>
      json(
        { type: 'urn:fogo:problem:business-rule', title: 'Regra de negócio violada', status: 422, detail: message },
        422,
        'application/problem+json',
      ),
    )
    renderForm()
    await waitVehiclesLoaded()
    fireEvent.change(vehicleSelect(), { target: { value: 'v1' } })
    fillMinimum()

    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain(message)
    expect(vehicleSelect().getAttribute('aria-invalid')).toBe('true')
  })

  it('deveEnviarVeiculoEBaseQuandoSalva', async () => {
    const mock = stubApi()
    renderForm()
    await waitVehiclesLoaded()
    fireEvent.change(vehicleSelect(), { target: { value: 'v1' } })
    fillMinimum()

    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    await screen.findByText('detalhe')
    const call = (mock.mock.calls as unknown[][]).find((c) => String(c[0]) === '/api/agents') as [string, RequestInit]
    const body = JSON.parse(String(call[1].body))
    expect(call[1].method).toBe('POST')
    expect(body.vehicleId).toBe('v1')
    expect(body.agentType).toBe('TRAFFIC_AGENT')
    expect(body.status).toBe('AVAILABLE')
    expect(body.baseLocation.latitude).toBe(-22.1)
  })
})
