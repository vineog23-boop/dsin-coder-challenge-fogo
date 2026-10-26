import { api } from './client'

export const EQUIPMENT_TYPES = [
  'RADAR',
  'SURVEILLANCE_CAMERA',
  'TRUCK_SCALE',
  'TAXI',
  'MOTO_TAXI',
  'SCHOOL_TRANSPORT',
  'PARKING_ENFORCEMENT_VEHICLE',
  'TRAFFIC_AGENT_MOTORCYCLE',
  'MUNICIPAL_GUARD_VEHICLE',
  'MILITARY_POLICE_VEHICLE',
  'ELECTRIC_SCOOTER',
  'AUTOMATED_GATE',
  'PARKING_KIOSK',
  'OTHER',
] as const
export type EquipmentType = (typeof EQUIPMENT_TYPES)[number]

export const EQUIPMENT_STATUSES = [
  'NOT_INSTALLED',
  'OPERATIONAL',
  'MALFUNCTION',
  'UNDER_MAINTENANCE',
  'DEACTIVATED',
  'STOLEN',
] as const
export type EquipmentStatus = (typeof EQUIPMENT_STATUSES)[number]

export const MAINTENANCE_NEEDS = ['NONE', 'IMMEDIATE', 'SCHEDULED'] as const
export type MaintenanceNeed = (typeof MAINTENANCE_NEEDS)[number]

export const AGENCIES = ['MUNICIPAL_TRAFFIC', 'MUNICIPAL_GUARD', 'MILITARY_POLICE', 'PRIVATE_OPERATOR'] as const
export type Agency = (typeof AGENCIES)[number]

export const REGIONS = ['CENTER', 'NORTH', 'SOUTH', 'EAST', 'WEST'] as const
export type Region = (typeof REGIONS)[number]

export const RADAR_MODALITIES = ['SPEED', 'RED_LIGHT', 'ILLEGAL_TURN', 'ILLEGAL_U_TURN', 'BUS_LANE'] as const
export type RadarModality = (typeof RADAR_MODALITIES)[number]

export const CAMERA_RESOLUTIONS = ['HD_720', 'FULL_HD_1080', 'QHD_1440', 'UHD_4K'] as const
export type CameraResolution = (typeof CAMERA_RESOLUTIONS)[number]

export const SCOOTER_OWNERSHIPS = ['PUBLIC', 'SHARED'] as const
export type ScooterOwnership = (typeof SCOOTER_OWNERSHIPS)[number]

export const MAINTENANCE_KINDS = ['CORRECTIVE', 'PREVENTIVE'] as const
export type MaintenanceKind = (typeof MAINTENANCE_KINDS)[number]

/** Bloco de características de cada tipo; espelha `EquipmentType.block()` do back. */
export type CharacteristicsBlock = 'radar' | 'camera' | 'truckScale' | 'vehicle' | 'scooter'

export function blockOf(type: EquipmentType): CharacteristicsBlock | null {
  switch (type) {
    case 'RADAR':
      return 'radar'
    case 'SURVEILLANCE_CAMERA':
      return 'camera'
    case 'TRUCK_SCALE':
      return 'truckScale'
    case 'ELECTRIC_SCOOTER':
      return 'scooter'
    case 'AUTOMATED_GATE':
    case 'PARKING_KIOSK':
    case 'OTHER':
      return null
    default:
      return 'vehicle'
  }
}

export type Location = {
  zipCode: string | null
  street: string
  reference: string | null
  neighborhood: string
  city: string
  state: string
  region: Region
  latitude: number
  longitude: number
}

export type Radar = { modality: RadarModality; speedLimitKmh: number | null; lanes: number | null }
export type Camera = { nightVision: boolean; resolution: CameraResolution; color: boolean; ptz: boolean }
export type TruckScale = { maxCapacityTons: number; weighInMotion: boolean }
export type Vehicle = { licensePlate: string; passengerCapacity: number | null; autonomous: boolean }
export type Scooter = { ownership: ScooterOwnership; operatorName: string | null; batteryLevel: number | null }

type Characteristics = {
  radar: Radar | null
  camera: Camera | null
  truckScale: TruckScale | null
  vehicle: Vehicle | null
  scooter: Scooter | null
}

export type Equipment = Characteristics & {
  id: string
  type: EquipmentType
  serialNumber: string
  brand: string
  model: string
  agency: Agency
  status: EquipmentStatus
  maintenanceNeed: MaintenanceNeed
  nextMaintenanceDate: string | null
  maintenanceIntervalDays: number | null
  location: Location
  createdAt: string
  updatedAt: string
}

export type EquipmentRequest = Characteristics & {
  type: EquipmentType
  serialNumber: string
  brand: string
  model: string
  agency: Agency
  status?: EquipmentStatus | null
  maintenanceNeed: MaintenanceNeed
  nextMaintenanceDate: string | null
  maintenanceIntervalDays: number | null
  location: Location
}

export type Page<T> = {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export type EquipmentFilters = {
  type?: EquipmentType
  status?: EquipmentStatus
  region?: Region
  agency?: Agency
  maintenanceNeed?: MaintenanceNeed
  search?: string
}

export type HistoryEvent = {
  eventType: 'STATUS_CHANGE' | 'MAINTENANCE'
  occurredAt: string
  performedBy: string
  fromStatus: EquipmentStatus | null
  toStatus: EquipmentStatus | null
  note: string | null
  maintenanceKind: MaintenanceKind | null
  description: string | null
}

export function listEquipments(filters: EquipmentFilters, page: number, size = 20): Promise<Page<Equipment>> {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value) params.set(key, value)
  }
  params.set('page', String(page))
  params.set('size', String(size))
  return api<Page<Equipment>>(`/equipments?${params}`)
}

export function getEquipment(id: string): Promise<Equipment> {
  return api<Equipment>(`/equipments/${id}`)
}

export function createEquipment(request: EquipmentRequest): Promise<Equipment> {
  return api<Equipment>('/equipments', { method: 'POST', body: request })
}

export function updateEquipment(id: string, request: EquipmentRequest): Promise<Equipment> {
  return api<Equipment>(`/equipments/${id}`, { method: 'PUT', body: request })
}

export function changeStatus(id: string, status: EquipmentStatus, note: string): Promise<Equipment> {
  return api<Equipment>(`/equipments/${id}/status`, { method: 'PATCH', body: { status, note: note || null } })
}

export function registerMaintenance(
  id: string,
  maintenance: { kind: MaintenanceKind; performedAt: string; description: string },
): Promise<unknown> {
  return api(`/equipments/${id}/maintenances`, { method: 'POST', body: maintenance })
}

export function getHistory(id: string, page = 0, size = 50): Promise<Page<HistoryEvent>> {
  return api<Page<HistoryEvent>>(`/equipments/${id}/history?page=${page}&size=${size}`)
}
