import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import AlertList from '../components/AlertList'
import MapLegend from '../components/MapLegend'
import OperationsFilters, { type OperationsFilterValues } from '../components/OperationsFilters'
import OperationsMap from '../components/OperationsMap'
import RegionTable from '../components/RegionTable'
import SummaryCards from '../components/SummaryCards'
import UpdatedAgo from '../components/UpdatedAgo'
import { getAlerts, getMap, getNeighborhoods, getSummary, type MapFilters } from '../lib/api/operations'
import { usePolling } from '../lib/usePolling'

const REFRESH_MS = 10_000
const FILTER_KEYS = ['equipmentType', 'equipmentStatus', 'agentStatus', 'region', 'agency', 'neighborhood'] as const

/** Só os filtros que vão à API, numa string estável: mudar camada não refaz a busca. */
function apiFilterQuery(params: URLSearchParams): string {
  const subset = new URLSearchParams()
  for (const key of FILTER_KEYS) {
    const value = params.get(key)
    if (value) subset.set(key, value)
  }
  return subset.toString()
}

export default function OperationsPage() {
  const [params, setParams] = useSearchParams()
  const filtersQuery = apiFilterQuery(params)
  const hidden = params.getAll('hide')
  const values: OperationsFilterValues = {
    ...(Object.fromEntries(new URLSearchParams(filtersQuery)) as Partial<OperationsFilterValues>),
    hideEquipments: hidden.includes('equipments'),
    hideAgents: hidden.includes('agents'),
  }

  // Mapa, indicadores e alertas no mesmo ciclo: os três números contam a mesma história.
  const fetcher = useMemo(() => {
    const filters = Object.fromEntries(new URLSearchParams(filtersQuery)) as MapFilters
    return () => Promise.all([getMap(filters), getSummary(), getAlerts()])
  }, [filtersQuery])
  const { data, error, updatedAt } = usePolling(fetcher, REFRESH_MS)

  // A lista de bairros muda pouco: busca uma vez. Se falhar, o filtro só fica sem opções.
  const [neighborhoods, setNeighborhoods] = useState<string[]>([])
  useEffect(() => {
    let active = true
    getNeighborhoods()
      .then((names) => {
        if (active) setNeighborhoods(names)
      })
      .catch(() => {})
    return () => {
      active = false
    }
  }, [])

  const changeFilters = useCallback(
    (next: OperationsFilterValues) => {
      const nextParams = new URLSearchParams()
      for (const key of FILTER_KEYS) {
        const value = next[key]
        if (value) nextParams.set(key, value)
      }
      if (next.hideEquipments) nextParams.append('hide', 'equipments')
      if (next.hideAgents) nextParams.append('hide', 'agents')
      setParams(nextParams, { replace: true })
    },
    [setParams],
  )

  const [map, summary, alerts] = data ?? [null, null, null]

  return (
    <section className="page operations">
      <div className="page-header">
        <h1>Central de operações</h1>
        <UpdatedAgo updatedAt={updatedAt} />
      </div>

      {error && (
        <p className="callout callout-error" role="alert">
          {data ? `${error} Mostrando os últimos dados recebidos.` : error}
        </p>
      )}
      {!data && !error && <p aria-live="polite">Carregando…</p>}

      {summary && <SummaryCards summary={summary} />}

      <OperationsFilters values={values} neighborhoods={neighborhoods} onChange={changeFilters} />

      {map && (
        <div className="operations-grid">
          <div className="operations-main">
            {map.truncated && (
              <p className="callout" role="status">
                Muitos pontos: o mapa mostra só parte deles. Use os filtros para refinar.
              </p>
            )}
            <OperationsMap
              equipments={values.hideEquipments ? [] : map.equipments}
              agents={values.hideAgents ? [] : map.agents}
            />
            <p className="muted" aria-live="polite">
              {values.hideEquipments ? 0 : map.equipments.length} equipamento(s) e{' '}
              {values.hideAgents ? 0 : map.agents.length} agente(s) no mapa
            </p>
            <MapLegend />
          </div>
          <aside className="operations-side" aria-labelledby="alerts-title">
            <h2 id="alerts-title">Alertas{alerts ? ` (${alerts.page.totalElements})` : ''}</h2>
            {alerts && <AlertList alerts={alerts.content} total={alerts.page.totalElements} />}
            {summary && <RegionTable regions={summary.regions} />}
          </aside>
        </div>
      )}
    </section>
  )
}
