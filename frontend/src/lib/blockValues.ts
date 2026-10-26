import type { CameraResolution, RadarModality, ScooterOwnership } from './api/equipments'

/** Valores dos blocos como o formulário guarda (números em texto, para o campo poder ficar vazio). */
export type BlockValues = {
  radar: { modality: RadarModality; speedLimitKmh: string; lanes: string }
  camera: { nightVision: boolean; resolution: CameraResolution; color: boolean; ptz: boolean }
  truckScale: { maxCapacityTons: string; weighInMotion: boolean }
  vehicle: { licensePlate: string; passengerCapacity: string; autonomous: boolean }
  scooter: { ownership: ScooterOwnership; operatorName: string; batteryLevel: string }
}

export const EMPTY_BLOCKS: BlockValues = {
  radar: { modality: 'SPEED', speedLimitKmh: '', lanes: '' },
  camera: { nightVision: false, resolution: 'FULL_HD_1080', color: true, ptz: false },
  truckScale: { maxCapacityTons: '', weighInMotion: false },
  vehicle: { licensePlate: '', passengerCapacity: '', autonomous: false },
  scooter: { ownership: 'PUBLIC', operatorName: '', batteryLevel: '' },
}
