import { AGENT_STATUSES } from '../lib/api/agents'
import { EQUIPMENT_STATUSES } from '../lib/api/equipments'
import { AGENT_STATUS_LABEL, STATUS_LABEL } from '../lib/labels'
import { AGENT_STATUS_COLOR, EQUIPMENT_STATUS_COLOR } from '../lib/mapIcons'

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
        </ul>
      </div>
    </div>
  )
}
