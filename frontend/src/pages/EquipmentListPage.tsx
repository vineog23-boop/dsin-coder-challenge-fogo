import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import EquipmentFilters from '../components/EquipmentFilters'
import StatusBadge from '../components/StatusBadge'
import { ApiError } from '../lib/api/client'
import { listEquipments, type Equipment, type EquipmentFilters as Filters, type Page } from '../lib/api/equipments'
import { MAINTENANCE_NEED_LABEL, REGION_LABEL, TYPE_LABEL } from '../lib/labels'
import { canEdit, useSession } from '../lib/session/session'

const FILTER_KEYS = ['type', 'status', 'region', 'agency', 'maintenanceNeed', 'search'] as const

/** Filtros e página moram na URL: dá para recarregar, voltar e compartilhar a mesma busca. */
function readFilters(params: URLSearchParams): Filters {
  const filters: Record<string, string> = {}
  for (const key of FILTER_KEYS) {
    const value = params.get(key)
    if (value) filters[key] = value
  }
  return filters as Filters
}

function filterQuery(params: URLSearchParams): string {
  const subset = new URLSearchParams()
  for (const key of FILTER_KEYS) {
    const value = params.get(key)
    if (value) subset.set(key, value)
  }
  return subset.toString()
}

type Result = { kind: 'error'; message: string } | { kind: 'ready'; page: Page<Equipment> }
type State = { kind: 'loading' } | Result

export default function EquipmentListPage() {
  const { session } = useSession()
  const [params, setParams] = useSearchParams()
  const [reloadKey, setReloadKey] = useState(0)
  const [result, setResult] = useState<{ key: string; value: Result } | null>(null)

  // Só os parâmetros de filtro: trocar de página não recria o objeto nem refaz a busca à toa.
  const filtersQuery = filterQuery(params)
  const filters = useMemo(() => readFilters(new URLSearchParams(filtersQuery)), [filtersQuery])
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)

  // O resultado guarda a chave da busca que o gerou: chave diferente da atual = ainda carregando.
  const requestKey = `${filtersQuery}#${page}#${reloadKey}`
  const state: State = result?.key === requestKey ? result.value : { kind: 'loading' }

  useEffect(() => {
    let active = true
    listEquipments(filters, page)
      .then((loaded) => active && setResult({ key: requestKey, value: { kind: 'ready', page: loaded } }))
      .catch(
        (e) =>
          active &&
          setResult({
            key: requestKey,
            value: { kind: 'error', message: e instanceof ApiError ? e.message : 'Erro ao carregar equipamentos.' },
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
        <h1>Equipamentos</h1>
        {canEdit(session) && (
          <Link className="button button-primary" to="/equipamentos/novo">
            Novo equipamento
          </Link>
        )}
      </div>

      <EquipmentFilters filters={filters} onChange={changeFilters} />

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
        <p className="empty">Nenhum equipamento encontrado com esses filtros.</p>
      )}

      {state.kind === 'ready' && state.page.content.length > 0 && (
        <>
          <p className="muted" aria-live="polite">
            {state.page.page.totalElements} equipamento(s)
          </p>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th scope="col">Série</th>
                  <th scope="col">Tipo</th>
                  <th scope="col">Marca / modelo</th>
                  <th scope="col">Região / bairro</th>
                  <th scope="col">Situação</th>
                  <th scope="col">Manutenção</th>
                </tr>
              </thead>
              <tbody>
                {state.page.content.map((equipment) => (
                  <tr key={equipment.id}>
                    <td data-label="Série">
                      <Link to={`/equipamentos/${equipment.id}`}>{equipment.serialNumber}</Link>
                      {equipment.vehicle && <span className="muted"> · {equipment.vehicle.licensePlate}</span>}
                    </td>
                    <td data-label="Tipo">{TYPE_LABEL[equipment.type]}</td>
                    <td data-label="Marca / modelo">
                      {equipment.brand} {equipment.model}
                    </td>
                    <td data-label="Região / bairro">
                      {REGION_LABEL[equipment.location.region]} · {equipment.location.neighborhood}
                    </td>
                    <td data-label="Situação">
                      <StatusBadge status={equipment.status} />
                    </td>
                    <td data-label="Manutenção">{MAINTENANCE_NEED_LABEL[equipment.maintenanceNeed]}</td>
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
