import { AGENT_STATUSES, AGENT_TYPES } from '../lib/api/agents'
import { EQUIPMENT_STATUSES } from '../lib/api/equipments'
import { AGENT_STATUS_LABEL, AGENT_TYPE_LABEL, STATUS_LABEL } from '../lib/labels'
import { AGENT_STATUS_COLOR, EQUIPMENT_STATUS_COLOR, agentGlyph } from '../lib/mapIcons'

/** Legenda das cores: quadrado = equipamento (borda), círculo = agente. */
export default function MapLegend() {
  return (
    <div className="map-legend" aria-label="Legenda do mapa">
      <div>
        <strong>Equipamentos</strong>
        <ul>
          {EQUIPMENT_STATUSES.map((status) => (
            <li key={status}>
              <span className="legend-square" style={{ borderColor: EQUIPMENT_STATUS_COLOR[status] }} />
              {STATUS_LABEL[status]}
            </li>
          ))}
        </ul>
      </div>
      <div>
        <strong>Agentes</strong>
        <ul>
          {AGENT_STATUSES.map((status) => (
            <li key={status}>
              <span className="legend-dot" style={{ background: AGENT_STATUS_COLOR[status] }} />
              {AGENT_STATUS_LABEL[status]}
            </li>
          ))}
          {AGENT_TYPES.map((type) => (
            <li key={type}>
              <span aria-hidden="true">{agentGlyph(type)}</span> {AGENT_TYPE_LABEL[type]}
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
