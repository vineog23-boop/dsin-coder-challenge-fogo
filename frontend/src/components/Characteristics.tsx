import type { Equipment } from '../lib/api/equipments'
import { CAMERA_RESOLUTION_LABEL, RADAR_MODALITY_LABEL, SCOOTER_OWNERSHIP_LABEL } from '../lib/labels'

function yesNo(value: boolean) {
  return value ? 'Sim' : 'Não'
}

/** Pares rótulo/valor do bloco de características do tipo. */
export default function Characteristics({ equipment }: { equipment: Equipment }) {
  const { radar, camera, truckScale, vehicle, scooter } = equipment
  const rows: [string, string][] = []

  if (radar) {
    rows.push(['Modalidade', RADAR_MODALITY_LABEL[radar.modality]])
    if (radar.speedLimitKmh !== null) rows.push(['Limite de velocidade', `${radar.speedLimitKmh} km/h`])
    if (radar.lanes !== null) rows.push(['Faixas', String(radar.lanes)])
  }
  if (camera) {
    rows.push(['Resolução', CAMERA_RESOLUTION_LABEL[camera.resolution]])
    rows.push(['Visão noturna', yesNo(camera.nightVision)])
    rows.push(['Colorida', yesNo(camera.color)])
    rows.push(['Gira e dá zoom (PTZ)', yesNo(camera.ptz)])
  }
  if (truckScale) {
    rows.push(['Capacidade máxima', `${truckScale.maxCapacityTons} t`])
    rows.push(['Pesa em movimento', yesNo(truckScale.weighInMotion)])
  }
  if (vehicle) {
    rows.push(['Placa', vehicle.licensePlate])
    if (vehicle.passengerCapacity !== null) rows.push(['Passageiros', String(vehicle.passengerCapacity)])
    rows.push(['Autônomo', yesNo(vehicle.autonomous)])
  }
  if (scooter) {
    rows.push(['Propriedade', SCOOTER_OWNERSHIP_LABEL[scooter.ownership]])
    if (scooter.operatorName) rows.push(['Operadora', scooter.operatorName])
    if (scooter.batteryLevel !== null) rows.push(['Bateria', `${scooter.batteryLevel}%`])
  }

  if (rows.length === 0) {
    return <p className="muted">Sem características próprias.</p>
  }

  return (
    <dl className="details">
      {rows.map(([label, value]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  )
}
