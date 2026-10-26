import { useEffect, useState } from 'react'
import { AGENT_STATUSES, AGENT_TYPES, type AgentFilters as Filters } from '../lib/api/agents'
import { AGENCIES, REGIONS } from '../lib/api/equipments'
import { AGENCY_LABEL, AGENT_STATUS_LABEL, AGENT_TYPE_LABEL, REGION_LABEL } from '../lib/labels'

type Props = {
  filters: Filters
  onChange: (filters: Filters) => void
}

type SelectKey = Exclude<keyof Filters, 'search'>

const SELECTS: { key: SelectKey; label: string; options: readonly string[]; labels: Record<string, string> }[] = [
  { key: 'agentType', label: 'Tipo', options: AGENT_TYPES, labels: AGENT_TYPE_LABEL },
  { key: 'status', label: 'Situação', options: AGENT_STATUSES, labels: AGENT_STATUS_LABEL },
  { key: 'region', label: 'Região da base', options: REGIONS, labels: REGION_LABEL },
  // Agentes só existem nos três órgãos públicos.
  { key: 'agency', label: 'Órgão', options: AGENCIES.filter((a) => a !== 'PRIVATE_OPERATOR'), labels: AGENCY_LABEL },
]

export default function AgentFilters({ filters, onChange }: Props) {
  // A busca só vai para a URL ao parar de digitar, para não disparar uma requisição por tecla.
  const [search, setSearch] = useState(filters.search ?? '')
  const [appliedSearch, setAppliedSearch] = useState(filters.search ?? '')

  // A URL mudou por fora (voltar, limpar filtros): o campo acompanha, sem efeito extra.
  if ((filters.search ?? '') !== appliedSearch) {
    setAppliedSearch(filters.search ?? '')
    setSearch(filters.search ?? '')
  }

  useEffect(() => {
    if (search === (filters.search ?? '')) return
    const timer = setTimeout(() => onChange({ ...filters, search: search.trim() || undefined }), 400)
    return () => clearTimeout(timer)
  }, [search, filters, onChange])

  const hasFilters = Object.values(filters).some(Boolean)

  return (
    <form className="filters" role="search" onSubmit={(e) => e.preventDefault()}>
      <label className="filters-search">
        <span>Buscar</span>
        <input
          type="search"
          placeholder="Nome ou matrícula"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </label>
      {SELECTS.map(({ key, label, options, labels }) => (
        <label key={key}>
          <span>{label}</span>
          <select
            value={filters[key] ?? ''}
            onChange={(e) => onChange({ ...filters, [key]: e.target.value || undefined })}
          >
            <option value="">Todos</option>
            {options.map((option) => (
              <option key={option} value={option}>
                {labels[option]}
              </option>
            ))}
          </select>
        </label>
      ))}
      {hasFilters && (
        <button type="button" className="filters-clear" onClick={() => onChange({})}>
          Limpar filtros
        </button>
      )}
    </form>
  )
}
