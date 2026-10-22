import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from './client'

function mockFetch(response: Response | Error) {
  const fetchMock = response instanceof Error
    ? vi.fn().mockRejectedValue(response)
    : vi.fn().mockResolvedValue(response)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function json(body: unknown, status: number) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('api', () => {
  it('deveEnviarJsonEmApiERetornarOCorpoQuandoRespostaOk', async () => {
    const fetchMock = mockFetch(json({ id: '1' }, 201))

    const result = await api<{ id: string }>('/equipments', { method: 'POST', body: { model: 'X' } })

    expect(result).toEqual({ id: '1' })
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/equipments')
    expect(init.body).toBe('{"model":"X"}')
    expect(init.headers['Content-Type']).toBe('application/json')
  })

  it('deveRetornarUndefinedQuandoRespostaSemCorpo', async () => {
    mockFetch(new Response(null, { status: 204 }))

    const result = await api('/equipments/1')

    expect(result).toBeUndefined()
  })

  it('deveLancarApiErrorComOsCamposQuandoRespostaEhProblemDetails', async () => {
    const problem = {
      type: 'urn:fogo:problem:validation',
      title: 'Dados inválidos',
      status: 400,
      detail: 'Um ou mais campos estão inválidos.',
      errors: [{ field: 'serialNumber', message: 'é obrigatório' }],
    }
    mockFetch(json(problem, 400))

    const error = await api('/equipments').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).problem).toEqual(problem)
    expect((error as ApiError).message).toBe('Um ou mais campos estão inválidos.')
  })

  it('deveMontarProblemaGenericoQuandoErroNaoEhJson', async () => {
    mockFetch(new Response('<html>Bad Gateway</html>', { status: 502, statusText: 'Bad Gateway' }))

    const error = await api('/equipments').catch((e: unknown) => e)

    expect((error as ApiError).problem).toMatchObject({ type: 'about:blank', title: 'Bad Gateway', status: 502 })
  })

  it('deveLancarErroDeRedeQuandoFetchFalha', async () => {
    mockFetch(new TypeError('Failed to fetch'))

    const error = await api('/equipments').catch((e: unknown) => e)

    expect((error as ApiError).problem).toMatchObject({ type: 'urn:fogo:problem:network', status: 0 })
  })
})
