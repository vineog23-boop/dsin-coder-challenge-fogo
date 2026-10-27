import type { AgentStatus, AgentType } from './agents'
import { api } from './client'
import type {
  Agency,
  EquipmentStatus,
  EquipmentType,
  MaintenanceNeed,
  Page,
  Region,
} from './equipments'

export type EquipmentMarker = {
  id: string
  type: EquipmentType
  serialNumber: string
  status: EquipmentStatus
  maintenanceNeed: MaintenanceNeed
  latitude: number
  longitude: number
  region: Region
}

export type AgentMarker = {
  id: string
  name: string
  agentType: AgentType
  status: AgentStatus
  latitude: number
  longitude: number
  /** `CURRENT`: posição em campo; `BASE`: sem posição, o ponto é a base (regra 1). */
  positionSource: 'CURRENT' | 'BASE'
  positionUpdatedAt: string | null
  vehiclePlate: string | null
  region: Region
}

export type MapData = {
  equipments: EquipmentMarker[]
  agents: AgentMarker[]
  /** Alguma camada passou do teto e veio cortada. */
  truncated: boolean
}

export type Summary = {
  equipments: {
    total: number
    byStatus: Record<EquipmentStatus, number>
    immediateMaintenance: number
    overdueMaintenance: number
    /** Agendada vencendo de hoje até 7 dias. */
    upcomingMaintenance: number
  }
  agents: { total: number; active: number; byStatus: Record<AgentStatus, number> }
  regions: { region: Region; equipmentsInMalfunction: number; activeAgents: number }[]
  generatedAt: string
}

export type AlertKind =
  | 'EQUIPMENT_MALFUNCTION'
  | 'EQUIPMENT_STOLEN'
  | 'REGION_WITHOUT_ACTIVE_AGENT'
  | 'MAINTENANCE_OVERDUE'
  | 'MAINTENANCE_DUE_SOON'

export type Alert = {
  kind: AlertKind
  severity: 'HIGH' | 'MEDIUM' | 'LOW'
  message: string
  region: Region
  equipmentId: string | null
  equipmentType: EquipmentType | null
  since: string | null
}

export type MapFilters = {
  equipmentType?: EquipmentType
  equipmentStatus?: EquipmentStatus
  agentStatus?: AgentStatus
  region?: Region
  agency?: Agency
  /** Comparado sem diferenciar maiúsculas; no agente, vale o bairro da base. */
  neighborhood?: string
}

export function getMap(filters: MapFilters): Promise<MapData> {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value) params.set(key, value)
  }
  const query = params.toString()
  return api<MapData>(`/operations/map${query ? `?${query}` : ''}`)
}

export function getSummary(): Promise<Summary> {
  return api<Summary>('/operations/summary')
}

/** Bairros que existem nos dados, para o filtro do mapa. */
export function getNeighborhoods(): Promise<string[]> {
  return api<string[]>('/operations/neighborhoods')
}

export function getAlerts(size = 50): Promise<Page<Alert>> {
  return api<Page<Alert>>(`/operations/alerts?size=${size}`)
}
