import { act, cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './api/client'
import { usePolling } from './usePolling'

const INTERVAL = 10_000

function Probe({ fetcher }: { fetcher: () => Promise<string> }) {
  const { data, error } = usePolling(fetcher, INTERVAL)
  return (
    <div>
      <p data-testid="data">{data ?? 'sem dados'}</p>
      <p data-testid="error">{error ?? 'sem erro'}</p>
    </div>
  )
}

function setHidden(value: boolean) {
  Object.defineProperty(document, 'hidden', { configurable: true, get: () => value })
  document.dispatchEvent(new Event('visibilitychange'))
}

async function advance(ms: number) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
}

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  cleanup()
  vi.useRealTimers()
  Reflect.deleteProperty(document, 'hidden')
})

describe('usePolling', () => {
  it('deveChamarNaMontagemEDeNovoAposIntervaloQuandoAtivo', async () => {
    const fetcher = vi.fn(async () => 'ok')
    render(<Probe fetcher={fetcher} />)
    await advance(0)
    expect(fetcher).toHaveBeenCalledTimes(1)
    expect(screen.getByTestId('data').textContent).toBe('ok')

    await advance(INTERVAL)

    expect(fetcher).toHaveBeenCalledTimes(2)
  })

  it('deveManterDadosAnterioresEPreencherErroQuandoFetcherFalha', async () => {
    const fetcher = vi
      .fn<() => Promise<string>>()
      .mockResolvedValueOnce('primeiro')
      .mockRejectedValueOnce(new ApiError({ type: 'urn:fogo:problem:internal', title: 'Erro', status: 500, detail: 'Falhou.' }))
    render(<Probe fetcher={fetcher} />)
    await advance(0)

    await advance(INTERVAL)

    expect(screen.getByTestId('data').textContent).toBe('primeiro')
    expect(screen.getByTestId('error').textContent).toBe('Falhou.')
  })

  it('deveUsarMensagemGenericaQuandoErroNaoEApiError', async () => {
    const fetcher = vi.fn<() => Promise<string>>().mockRejectedValue(new Error('boom'))
    render(<Probe fetcher={fetcher} />)

    await advance(0)

    expect(screen.getByTestId('error').textContent).toBe('Falha ao atualizar.')
    expect(screen.getByTestId('data').textContent).toBe('sem dados')
  })

  it('naoDeveChamarQuandoAbaOculta', async () => {
    const fetcher = vi.fn(async () => 'ok')
    render(<Probe fetcher={fetcher} />)
    await advance(0)

    setHidden(true)
    await advance(INTERVAL * 3)

    expect(fetcher).toHaveBeenCalledTimes(1)
  })

  it('deveChamarNaHoraQuandoAbaVoltaAFicarVisivel', async () => {
    const fetcher = vi.fn(async () => 'ok')
    render(<Probe fetcher={fetcher} />)
    await advance(0)
    setHidden(true)
    await advance(INTERVAL * 3)

    setHidden(false)
    await advance(0)

    expect(fetcher).toHaveBeenCalledTimes(2)
  })

  it('naoDeveChamarQuandoDesmontado', async () => {
    const fetcher = vi.fn(async () => 'ok')
    const { unmount } = render(<Probe fetcher={fetcher} />)
    await advance(0)

    unmount()
    await advance(INTERVAL * 3)
    document.dispatchEvent(new Event('visibilitychange'))
    await advance(0)

    expect(fetcher).toHaveBeenCalledTimes(1)
  })
})
