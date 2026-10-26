import { useEffect, useState } from 'react'
import {
  AGENCIES,
  EQUIPMENT_STATUSES,
  EQUIPMENT_TYPES,
  MAINTENANCE_NEEDS,
  REGIONS,
  type EquipmentFilters as Filters,
} from '../lib/api/equipments'
import { AGENCY_LABEL, MAINTENANCE_NEED_LABEL, REGION_LABEL, STATUS_LABEL, TYPE_LABEL } from '../lib/labels'

type Props = {
  filters: Filters
  onChange: (filters: Filters) => void
}

type SelectKey = Exclude<keyof Filters, 'search'>

const SELECTS: { key: SelectKey; label: string; options: readonly string[]; labels: Record<string, string> }[] = [
  { key: 'type', label: 'Tipo', options: EQUIPMENT_TYPES, labels: TYPE_LABEL },
  { key: 'status', label: 'Situação', options: EQUIPMENT_STATUSES, labels: STATUS_LABEL },
  { key: 'region', label: 'Região', options: REGIONS, labels: REGION_LABEL },
  { key: 'agency', label: 'Órgão', options: AGENCIES, labels: AGENCY_LABEL },
  { key: 'maintenanceNeed', label: 'Manutenção', options: MAINTENANCE_NEEDS, labels: MAINTENANCE_NEED_LABEL },
]

export default function EquipmentFilters({ filters, onChange }: Props) {
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
          placeholder="Série, marca, modelo ou placa"
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
