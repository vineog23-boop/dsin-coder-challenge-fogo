import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { SessionContext, type Session } from '../lib/session/session'
import EquipmentFormPage from './EquipmentFormPage'

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

function sessionOf(role: 'MANAGER' | 'OPERATOR'): Session {
  return { token: 't', role, username: 'u', expiresAt: Date.now() + 60_000 }
}

function renderForm(role: 'MANAGER' | 'OPERATOR' = 'MANAGER') {
  render(
    <SessionContext.Provider value={{ session: sessionOf(role), login: vi.fn(), logout: vi.fn() }}>
      <MemoryRouter initialEntries={['/equipamentos/novo']}>
        <Routes>
          <Route path="/equipamentos/novo" element={<EquipmentFormPage />} />
          <Route path="/equipamentos/:id" element={<p>detalhe</p>} />
          <Route path="/equipamentos" element={<p>lista</p>} />
        </Routes>
      </MemoryRouter>
    </SessionContext.Provider>,
  )
}

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

function fillMinimum() {
  type('Número de série *', 'SN-1')
  type('Marca *', 'Marca')
  type('Modelo *', 'Modelo')
  type('Rua *', 'Rua A')
  type('Bairro *', 'Centro')
  fireEvent.click(screen.getByRole('button', { name: 'marcar ponto' }))
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('EquipmentFormPage', () => {
  it('deveTrocarCamposQuandoTrocaTipo', () => {
    renderForm()
    expect(screen.getByLabelText('Modalidade')).toBeTruthy()

    type('Tipo *', 'TAXI')

    expect(screen.getByLabelText('Placa *')).toBeTruthy()
    expect(screen.queryByLabelText('Modalidade')).toBeNull()

    type('Tipo *', 'AUTOMATED_GATE')

    expect(screen.getByText('Este tipo não tem características próprias.')).toBeTruthy()
    expect(screen.queryByLabelText('Placa *')).toBeNull()
  })

  it('deveMostrarErroDaApiNoCampoCertoQuandoRecebe400', async () => {
    const message = 'O limite de velocidade é obrigatório para radar de velocidade.'
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        json(
          {
            type: 'urn:fogo:problem:validation',
            title: 'Dados inválidos',
            status: 400,
            errors: [{ field: 'radar.speedLimitKmh', message }],
          },
          400,
          'application/problem+json',
        ),
      ),
    )
    renderForm()
    fillMinimum()

    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    const error = await screen.findByText(message)
    const input = screen.getByLabelText(/Limite de velocidade/)
    expect(input.getAttribute('aria-invalid')).toBe('true')
    expect(input.getAttribute('aria-describedby')).toBe(error.id)
    expect(error.id).toBe('radar.speedLimitKmh-error')
    expect(screen.getByText('Corrija os campos destacados.')).toBeTruthy()
  })

  it('deveEnviarSoOBlocoDoTipoQuandoSalva', async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ id: 'novo-id' }, 201))
    vi.stubGlobal('fetch', fetchMock)
    renderForm()
    type('Tipo *', 'TAXI')
    fillMinimum()
    type('Placa *', 'abc1d23')

    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    await screen.findByText('detalhe')
    const [url, init] = fetchMock.mock.calls[0]
    const body = JSON.parse(init.body)
    expect(url).toBe('/api/equipments')
    expect(init.method).toBe('POST')
    expect(body.type).toBe('TAXI')
    expect(body.vehicle.licensePlate).toBe('ABC1D23')
    expect(body.radar).toBeNull()
    expect(body.camera).toBeNull()
    expect(body.truckScale).toBeNull()
    expect(body.scooter).toBeNull()
    expect(body.location.latitude).toBe(-22.1)
    expect(body.location.longitude).toBe(-51.38)
  })

  it('deveRedirecionarQuandoOperadorAbreFormulario', async () => {
    renderForm('OPERATOR')

    await waitFor(() => expect(screen.getByText('lista')).toBeTruthy())
    expect(screen.queryByRole('button', { name: 'Salvar' })).toBeNull()
  })
})
