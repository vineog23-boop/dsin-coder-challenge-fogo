import L from 'leaflet'
import type { AgentStatus } from './api/agents'
import type { EquipmentStatus, EquipmentType } from './api/equipments'

/** Símbolo de cada tipo de equipamento no mapa (regra do front: ícone por tipo). */
export function equipmentGlyph(type: EquipmentType): string {
  switch (type) {
    case 'RADAR':
      return '📡'
    case 'SURVEILLANCE_CAMERA':
      return '📷'
    case 'TRUCK_SCALE':
      return '⚖️'
    case 'MOTO_TAXI':
    case 'TRAFFIC_AGENT_MOTORCYCLE':
      return '🏍️'
    case 'SCHOOL_TRANSPORT':
      return '🚌'
    case 'MUNICIPAL_GUARD_VEHICLE':
    case 'MILITARY_POLICE_VEHICLE':
      return '🚓'
    case 'TAXI':
    case 'PARKING_ENFORCEMENT_VEHICLE':
      return '🚗'
    case 'ELECTRIC_SCOOTER':
      return '🛴'
    case 'AUTOMATED_GATE':
      return '🚧'
    case 'PARKING_KIOSK':
      return '🅿️'
    case 'OTHER':
      return '⚙️'
  }
}

/** Cor por situação (regra do front: cor por situação); as mesmas da legenda. */
export const EQUIPMENT_STATUS_COLOR: Record<EquipmentStatus, string> = {
  OPERATIONAL: '#16a34a',
  MALFUNCTION: '#dc2626',
  UNDER_MAINTENANCE: '#d97706',
  STOLEN: '#7c3aed',
  DEACTIVATED: '#6b7280',
  NOT_INSTALLED: '#9ca3af',
}

export const AGENT_STATUS_COLOR: Record<AgentStatus, string> = {
  AVAILABLE: '#2563eb',
  ON_OCCURRENCE: '#dc2626',
  OFF_DUTY: '#6b7280',
  ON_LEAVE: '#7c3aed',
}

/**
 * Ícones desenhados em HTML (`divIcon`): sem imagens, então nada de assets quebrados no build.
 * Equipamento é um quadrado com borda na cor da situação; agente é um círculo cheio, para não confundir.
 */
export function equipmentIcon(type: EquipmentType, status: EquipmentStatus): L.DivIcon {
  return L.divIcon({
    className: 'map-marker',
    html: `<span class="marker-equipment" style="border-color:${EQUIPMENT_STATUS_COLOR[status]}">${equipmentGlyph(type)}</span>`,
    iconSize: [30, 30],
    iconAnchor: [15, 15],
    popupAnchor: [0, -14],
  })
}

export function agentIcon(status: AgentStatus): L.DivIcon {
  return L.divIcon({
    className: 'map-marker',
    html: `<span class="marker-agent" style="background:${AGENT_STATUS_COLOR[status]}">👮</span>`,
    iconSize: [26, 26],
    iconAnchor: [13, 13],
    popupAnchor: [0, -12],
  })
}
