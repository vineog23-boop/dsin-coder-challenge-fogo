import { AGENT_STATUSES } from '../lib/api/agents'
import { AGENCIES, EQUIPMENT_STATUSES, EQUIPMENT_TYPES, REGIONS } from '../lib/api/equipments'
import {
  AGENCY_LABEL,
  AGENT_STATUS_LABEL,
  REGION_LABEL,
  STATUS_LABEL,
  TYPE_LABEL,
} from '../lib/labels'

export type OperationsFilterValues = {
  equipmentType?: string
  equipmentStatus?: string
  agentStatus?: string
  region?: string
  agency?: string
  /** Camadas desligadas; ficam na URL como `hide=equipments` / `hide=agents`. */
  hideEquipments: boolean
  hideAgents: boolean
}

type Props = {
  values: OperationsFilterValues
  onChange: (values: OperationsFilterValues) => void
}

type SelectKey = 'equipmentType' | 'equipmentStatus' | 'agentStatus' | 'region' | 'agency'

const SELECTS: { key: SelectKey; label: string; empty: string; options: readonly string[]; labels: Record<string, string> }[] = [
  { key: 'equipmentType', label: 'Tipo de equipamento', empty: 'Todos', options: EQUIPMENT_TYPES, labels: TYPE_LABEL },
  { key: 'equipmentStatus', label: 'Situação do equipamento', empty: 'Todas', options: EQUIPMENT_STATUSES, labels: STATUS_LABEL },
  // Sem escolha, o back mostra só os agentes em atividade (regra 2).
  { key: 'agentStatus', label: 'Situação do agente', empty: 'Em atividade', options: AGENT_STATUSES, labels: AGENT_STATUS_LABEL },
  { key: 'region', label: 'Região', empty: 'Todas', options: REGIONS, labels: REGION_LABEL },
  { key: 'agency', label: 'Órgão', empty: 'Todos', options: AGENCIES, labels: AGENCY_LABEL },
]

export default function OperationsFilters({ values, onChange }: Props) {
  const hasFilters =
    SELECTS.some(({ key }) => values[key]) || values.hideEquipments || values.hideAgents

  return (
    <form className="filters" onSubmit={(e) => e.preventDefault()} aria-label="Filtros do mapa">
      <fieldset className="filters-layers">
        <legend>Camadas</legend>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={!values.hideEquipments}
            onChange={(e) => onChange({ ...values, hideEquipments: !e.target.checked })}
          />
          Equipamentos
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={!values.hideAgents}
            onChange={(e) => onChange({ ...values, hideAgents: !e.target.checked })}
          />
          Agentes
        </label>
      </fieldset>
      {SELECTS.map(({ key, label, empty, options, labels }) => (
        <label key={key}>
          <span>{label}</span>
          <select value={values[key] ?? ''} onChange={(e) => onChange({ ...values, [key]: e.target.value || undefined })}>
            <option value="">{empty}</option>
            {options.map((option) => (
              <option key={option} value={option}>
                {labels[option]}
              </option>
            ))}
          </select>
        </label>
      ))}
      {hasFilters && (
        <button type="button" onClick={() => onChange({ hideEquipments: false, hideAgents: false })}>
          Limpar filtros
        </button>
      )}
    </form>
  )
}
