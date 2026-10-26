import { api } from './client'
import type { Agency, EquipmentStatus, EquipmentType, Location, Page } from './equipments'

export const AGENT_TYPES = ['TRAFFIC_AGENT', 'MUNICIPAL_GUARD', 'MILITARY_POLICE'] as const
export type AgentType = (typeof AGENT_TYPES)[number]

export const AGENT_STATUSES = ['AVAILABLE', 'ON_OCCURRENCE', 'OFF_DUTY', 'ON_LEAVE'] as const
export type AgentStatus = (typeof AGENT_STATUSES)[number]

/** Órgão e viatura de cada tipo; espelha `AgentType` do back (o órgão é derivado, não escolhido). */
export const AGENT_AGENCY: Record<AgentType, Agency> = {
  TRAFFIC_AGENT: 'MUNICIPAL_TRAFFIC',
  MUNICIPAL_GUARD: 'MUNICIPAL_GUARD',
  MILITARY_POLICE: 'MILITARY_POLICE',
}

export const AGENT_VEHICLE_TYPE: Record<AgentType, EquipmentType> = {
  TRAFFIC_AGENT: 'TRAFFIC_AGENT_MOTORCYCLE',
  MUNICIPAL_GUARD: 'MUNICIPAL_GUARD_VEHICLE',
  MILITARY_POLICE: 'MILITARY_POLICE_VEHICLE',
}

/** Só agente em serviço pode ficar com viatura (regra 5). */
export function isOnDuty(status: AgentStatus): boolean {
  return status === 'AVAILABLE' || status === 'ON_OCCURRENCE'
}

export type VehicleSummary = {
  id: string
  type: EquipmentType
  serialNumber: string
  licensePlate: string
  status: EquipmentStatus
}

export type Agent = {
  id: string
  name: string
  registrationNumber: string
  agentType: AgentType
  agency: Agency
  status: AgentStatus
  baseLocation: Location
  currentLatitude: number | null
  currentLongitude: number | null
  positionUpdatedAt: string | null
  vehicle: VehicleSummary | null
  createdAt: string
  updatedAt: string
}

export type AgentRequest = {
  name: string
  registrationNumber: string
  agentType: AgentType
  status?: AgentStatus | null
  baseLocation: Location
  vehicleId: string | null
}

export type AgentFilters = {
  agentType?: AgentType
  status?: AgentStatus
  agency?: Agency
  region?: string
  search?: string
}

export type AgentHistoryEvent = {
  fromStatus: AgentStatus | null
  toStatus: AgentStatus
  changedAt: string
  changedBy: string
  note: string | null
}

export function listAgents(filters: AgentFilters, page: number, size = 20): Promise<Page<Agent>> {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value) params.set(key, value)
  }
  params.set('page', String(page))
  params.set('size', String(size))
  return api<Page<Agent>>(`/agents?${params}`)
}

export function getAgent(id: string): Promise<Agent> {
  return api<Agent>(`/agents/${id}`)
}

export function createAgent(request: AgentRequest): Promise<Agent> {
  return api<Agent>('/agents', { method: 'POST', body: request })
}

export function updateAgent(id: string, request: AgentRequest): Promise<Agent> {
  return api<Agent>(`/agents/${id}`, { method: 'PUT', body: request })
}

export function changeAgentStatus(id: string, status: AgentStatus, note: string): Promise<Agent> {
  return api<Agent>(`/agents/${id}/status`, { method: 'PATCH', body: { status, note: note || null } })
}

export function getAgentHistory(id: string, page = 0, size = 50): Promise<Page<AgentHistoryEvent>> {
  return api<Page<AgentHistoryEvent>>(`/agents/${id}/history?page=${page}&size=${size}`)
}
