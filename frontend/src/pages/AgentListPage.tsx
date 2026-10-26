import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import AgentFilters from '../components/AgentFilters'
import AgentStatusBadge from '../components/AgentStatusBadge'
import { ApiError } from '../lib/api/client'
import { listAgents, type Agent, type AgentFilters as Filters } from '../lib/api/agents'
import type { Page } from '../lib/api/equipments'
import { AGENT_TYPE_LABEL, REGION_LABEL } from '../lib/labels'
import { canEdit, useSession } from '../lib/session/session'

const FILTER_KEYS = ['agentType', 'status', 'region', 'agency', 'search'] as const

/** Só os parâmetros de filtro: trocar de página não refaz o objeto de filtros. */
function filterQuery(params: URLSearchParams): string {
  const subset = new URLSearchParams()
  for (const key of FILTER_KEYS) {
    const value = params.get(key)
    if (value) subset.set(key, value)
  }
  return subset.toString()
}

type Result = { kind: 'error'; message: string } | { kind: 'ready'; page: Page<Agent> }
type State = { kind: 'loading' } | Result

export default function AgentListPage() {
  const { session } = useSession()
  const [params, setParams] = useSearchParams()
  const [reloadKey, setReloadKey] = useState(0)
  const [result, setResult] = useState<{ key: string; value: Result } | null>(null)

  const filtersQuery = filterQuery(params)
  const filters = useMemo(
    () => Object.fromEntries(new URLSearchParams(filtersQuery)) as Filters,
    [filtersQuery],
  )
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)

  // O resultado guarda a chave da busca que o gerou: chave diferente da atual = ainda carregando.
  const requestKey = `${filtersQuery}#${page}#${reloadKey}`
  const state: State = result?.key === requestKey ? result.value : { kind: 'loading' }

  useEffect(() => {
    let active = true
    listAgents(filters, page)
      .then((loaded) => active && setResult({ key: requestKey, value: { kind: 'ready', page: loaded } }))
      .catch(
        (e) =>
          active &&
          setResult({
            key: requestKey,
            value: { kind: 'error', message: e instanceof ApiError ? e.message : 'Erro ao carregar agentes.' },
          }),
      )
    return () => {
      active = false
    }
  }, [filters, page, requestKey])

  const changeFilters = useCallback(
    (next: Filters) => {
      const nextParams = new URLSearchParams()
      for (const [key, value] of Object.entries(next)) {
        if (value) nextParams.set(key, value)
      }
      setParams(nextParams) // filtro novo volta para a primeira página
    },
    [setParams],
  )

  function goToPage(target: number) {
    const nextParams = new URLSearchParams(params)
    nextParams.set('page', String(target))
    setParams(nextParams)
  }

  return (
    <section className="page">
      <div className="page-header">
        <h1>Agentes</h1>
        {canEdit(session) && (
          <Link className="button button-primary" to="/agentes/novo">
            Novo agente
          </Link>
        )}
      </div>

      <AgentFilters filters={filters} onChange={changeFilters} />

      {state.kind === 'loading' && <p aria-live="polite">Carregando…</p>}

      {state.kind === 'error' && (
        <div className="callout callout-error" role="alert">
          <p>{state.message}</p>
          <button type="button" onClick={() => setReloadKey((key) => key + 1)}>
            Tentar de novo
          </button>
        </div>
      )}

      {state.kind === 'ready' && state.page.content.length === 0 && (
        <p className="empty">Nenhum agente encontrado com esses filtros.</p>
      )}

      {state.kind === 'ready' && state.page.content.length > 0 && (
        <>
          <p className="muted" aria-live="polite">
            {state.page.page.totalElements} agente(s)
          </p>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th scope="col">Nome</th>
                  <th scope="col">Matrícula</th>
                  <th scope="col">Tipo</th>
                  <th scope="col">Base</th>
                  <th scope="col">Situação</th>
                  <th scope="col">Viatura</th>
                </tr>
              </thead>
              <tbody>
                {state.page.content.map((agent) => (
                  <tr key={agent.id}>
                    <td data-label="Nome">
                      <Link to={`/agentes/${agent.id}`}>{agent.name}</Link>
                    </td>
                    <td data-label="Matrícula">{agent.registrationNumber}</td>
                    <td data-label="Tipo">{AGENT_TYPE_LABEL[agent.agentType]}</td>
                    <td data-label="Base">
                      {REGION_LABEL[agent.baseLocation.region]} · {agent.baseLocation.neighborhood}
                    </td>
                    <td data-label="Situação">
                      <AgentStatusBadge status={agent.status} />
                    </td>
                    <td data-label="Viatura">{agent.vehicle ? agent.vehicle.licensePlate : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className="pagination" aria-label="Paginação">
            <button type="button" disabled={page === 0} onClick={() => goToPage(page - 1)}>
              Anterior
            </button>
            <span>
              Página {page + 1} de {Math.max(1, state.page.page.totalPages)}
            </span>
            <button
              type="button"
              disabled={page + 1 >= state.page.page.totalPages}
              onClick={() => goToPage(page + 1)}
            >
              Próxima
            </button>
          </nav>
        </>
      )}
    </section>
  )
}
