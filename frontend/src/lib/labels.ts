import type {
  Agency,
  CameraResolution,
  EquipmentStatus,
  EquipmentType,
  MaintenanceKind,
  MaintenanceNeed,
  RadarModality,
  Region,
  ScooterOwnership,
} from './api/equipments'

/** Rótulos em português de todos os enums da API; a tela nunca mostra o valor cru. */
export const TYPE_LABEL: Record<EquipmentType, string> = {
  RADAR: 'Radar',
  SURVEILLANCE_CAMERA: 'Câmera de monitoramento',
  TRUCK_SCALE: 'Balança de caminhão',
  TAXI: 'Táxi',
  MOTO_TAXI: 'Mototáxi',
  SCHOOL_TRANSPORT: 'Transporte escolar',
  PARKING_ENFORCEMENT_VEHICLE: 'Veículo de fiscalização (autônomo)',
  TRAFFIC_AGENT_MOTORCYCLE: 'Moto de agente de trânsito',
  MUNICIPAL_GUARD_VEHICLE: 'Viatura da Guarda Municipal',
  MILITARY_POLICE_VEHICLE: 'Viatura da Polícia Militar',
  ELECTRIC_SCOOTER: 'Patinete elétrico',
  AUTOMATED_GATE: 'Cancela automática',
  PARKING_KIOSK: 'Totem de estacionamento',
  OTHER: 'Outro',
}

export const STATUS_LABEL: Record<EquipmentStatus, string> = {
  NOT_INSTALLED: 'Não instalado',
  OPERATIONAL: 'Operando',
  MALFUNCTION: 'Com falha',
  UNDER_MAINTENANCE: 'Em manutenção',
  DEACTIVATED: 'Desativado',
  STOLEN: 'Roubado',
}

export const MAINTENANCE_NEED_LABEL: Record<MaintenanceNeed, string> = {
  NONE: 'Nenhuma',
  IMMEDIATE: 'Imediata',
  SCHEDULED: 'Agendada',
}

export const AGENCY_LABEL: Record<Agency, string> = {
  MUNICIPAL_TRAFFIC: 'Trânsito municipal',
  MUNICIPAL_GUARD: 'Guarda Municipal',
  MILITARY_POLICE: 'Polícia Militar',
  PRIVATE_OPERATOR: 'Operador privado',
}

export const REGION_LABEL: Record<Region, string> = {
  CENTER: 'Centro',
  NORTH: 'Norte',
  SOUTH: 'Sul',
  EAST: 'Leste',
  WEST: 'Oeste',
}

export const RADAR_MODALITY_LABEL: Record<RadarModality, string> = {
  SPEED: 'Velocidade',
  RED_LIGHT: 'Avanço de sinal',
  ILLEGAL_TURN: 'Conversão proibida',
  ILLEGAL_U_TURN: 'Retorno proibido',
  BUS_LANE: 'Faixa de ônibus',
}

export const CAMERA_RESOLUTION_LABEL: Record<CameraResolution, string> = {
  HD_720: 'HD (720p)',
  FULL_HD_1080: 'Full HD (1080p)',
  QHD_1440: 'QHD (1440p)',
  UHD_4K: '4K',
}

export const SCOOTER_OWNERSHIP_LABEL: Record<ScooterOwnership, string> = {
  PUBLIC: 'Público',
  SHARED: 'Compartilhado',
}

export const MAINTENANCE_KIND_LABEL: Record<MaintenanceKind, string> = {
  CORRECTIVE: 'Corretiva',
  PREVENTIVE: 'Preventiva',
}

const dateTime = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso))
}

/** Data `yyyy-mm-dd` sem passar por `Date`, que a deslocaria pelo fuso. */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}
