export type FieldError = {
  field: string
  message: string
}

/** Corpo de erro da API (RFC 9457). `type` é uma URN `urn:fogo:problem:<erro>`. */
export type ProblemDetail = {
  type: string
  title: string
  status: number
  detail?: string
  instance?: string
  errors?: FieldError[]
}

export class ApiError extends Error {
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title)
    this.name = 'ApiError'
    this.problem = problem
  }
}

let authToken: string | null = null
let onUnauthorized: (() => void) | null = null

/** A sessão registra aqui o token; o cliente o envia em toda chamada sem cada tela repassá-lo. */
export function setAuthToken(token: string | null) {
  authToken = token
}

/** Chamado quando a API recusa um token enviado (expirado ou inválido), para a sessão encerrar. */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler
}

type RequestOptions = Omit<RequestInit, 'body' | 'headers'> & {
  body?: unknown
  headers?: Record<string, string>
}

/** Chama `/api<path>` com JSON e lança `ApiError` em qualquer falha, para a tela tratar um só formato. */
export async function api<T>(path: string, { body, headers, ...init }: RequestOptions = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined && { 'Content-Type': 'application/json' }),
        ...(authToken && { Authorization: `Bearer ${authToken}` }),
        ...headers,
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError({
      type: 'urn:fogo:problem:network',
      title: 'Sem conexão',
      status: 0,
      detail: 'Não foi possível falar com o servidor.',
    })
  }

  if (!response.ok) {
    // Sem token, 401 é credencial errada no login: a tela trata, não há sessão a encerrar.
    if (response.status === 401 && authToken) {
      onUnauthorized?.()
    }
    throw new ApiError(await readProblem(response))
  }

  // 201/204 podem vir sem corpo.
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** Um proxy fora do ar devolve HTML, não Problem Details; nesse caso monta um problema genérico. */
async function readProblem(response: Response): Promise<ProblemDetail> {
  try {
    const problem = (await response.json()) as Partial<ProblemDetail>
    if (problem.type && problem.title) {
      return { ...problem, status: response.status } as ProblemDetail
    }
  } catch {
    // corpo não é JSON
  }

  return {
    type: 'about:blank',
    title: response.statusText || 'Erro',
    status: response.status,
    detail: `A requisição falhou (${response.status}).`,
  }
}
