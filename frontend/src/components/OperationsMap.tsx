import 'leaflet/dist/leaflet.css'
import { Link } from 'react-router'
import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet'
import type { AgentMarker, EquipmentMarker } from '../lib/api/operations'
import { CITY_CENTER } from '../lib/geo'
import {
  AGENT_STATUS_LABEL,
  AGENT_TYPE_LABEL,
  MAINTENANCE_NEED_LABEL,
  REGION_LABEL,
  STATUS_LABEL,
  TYPE_LABEL,
  formatDateTime,
} from '../lib/labels'
import { agentIcon, equipmentIcon } from '../lib/mapIcons'

type Props = {
  equipments: EquipmentMarker[]
  agents: AgentMarker[]
}

/** Mapa da central: equipamentos (ícone por tipo, cor por situação) e agentes; clique abre o resumo. */
export default function OperationsMap({ equipments, agents }: Props) {
  return (
    <div className="operations-map">
      <MapContainer center={CITY_CENTER} zoom={13} scrollWheelZoom style={{ height: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {equipments.map((equipment) => (
          <Marker
            key={equipment.id}
            position={[equipment.latitude, equipment.longitude]}
            icon={equipmentIcon(equipment.type, equipment.status)}
            title={`${TYPE_LABEL[equipment.type]} ${equipment.serialNumber}: ${STATUS_LABEL[equipment.status]}`}
          >
            <Popup>
              <strong>{TYPE_LABEL[equipment.type]}</strong>
              <br />
              {equipment.serialNumber} · {STATUS_LABEL[equipment.status]}
              <br />
              Manutenção: {MAINTENANCE_NEED_LABEL[equipment.maintenanceNeed]} · região {REGION_LABEL[equipment.region]}
              <br />
              <Link to={`/equipamentos/${equipment.id}`}>Ver detalhes</Link>
            </Popup>
          </Marker>
        ))}
        {agents.map((agent) => (
          <Marker
            key={agent.id}
            position={[agent.latitude, agent.longitude]}
            icon={agentIcon(agent.agentType, agent.status)}
            title={`${agent.name}: ${AGENT_STATUS_LABEL[agent.status]}`}
            zIndexOffset={1000}
          >
            <Popup>
              <strong>{agent.name}</strong>
              <br />
              {AGENT_TYPE_LABEL[agent.agentType]} · {AGENT_STATUS_LABEL[agent.status]}
              <br />
              {agent.vehiclePlate ? `Viatura ${agent.vehiclePlate}` : 'Sem viatura'}
              <br />
              {agent.positionSource === 'CURRENT' && agent.positionUpdatedAt
                ? `Posição em campo (${formatDateTime(agent.positionUpdatedAt)})`
                : 'Sem posição em campo: mostrando a base'}
              <br />
              <Link to={`/agentes/${agent.id}`}>Ver detalhes</Link>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  )
}
