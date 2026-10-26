import {
  CAMERA_RESOLUTIONS,
  RADAR_MODALITIES,
  SCOOTER_OWNERSHIPS,
  type CameraResolution,
  type CharacteristicsBlock,
  type RadarModality,
  type ScooterOwnership,
} from '../lib/api/equipments'
import type { BlockValues } from '../lib/blockValues'
import { CAMERA_RESOLUTION_LABEL, RADAR_MODALITY_LABEL, SCOOTER_OWNERSHIP_LABEL } from '../lib/labels'
import Checkbox from './Checkbox'
import { describedBy } from '../lib/describedBy'
import Field from './Field'

type Props = {
  block: CharacteristicsBlock | null
  values: BlockValues
  errors: Record<string, string>
  onChange: <B extends CharacteristicsBlock>(block: B, values: BlockValues[B]) => void
}

/** Campos de características do tipo escolhido; trocar o tipo troca este bloco inteiro. */
export default function TypeSpecificFields({ block, values, errors, onChange }: Props) {
  if (!block) {
    return <p className="muted">Este tipo não tem características próprias.</p>
  }

  const blockError = errors[block]
  const error = (field: string) => errors[`${block}.${field}`]

  return (
    <div className="type-fields" data-block={block}>
      {blockError && (
        <p className="field-error" role="alert">
          {blockError}
        </p>
      )}

      {block === 'radar' && (
        <>
          <Field id="radar.modality" label="Modalidade" error={error('modality')}>
            <select
              id="radar.modality"
              value={values.radar.modality}
              onChange={(e) => onChange('radar', { ...values.radar, modality: e.target.value as RadarModality })}
            >
              {RADAR_MODALITIES.map((modality) => (
                <option key={modality} value={modality}>
                  {RADAR_MODALITY_LABEL[modality]}
                </option>
              ))}
            </select>
          </Field>
          <Field
            id="radar.speedLimitKmh"
            label={`Limite de velocidade (km/h)${values.radar.modality === 'SPEED' ? ' *' : ''}`}
            error={error('speedLimitKmh')}
          >
            <input
              id="radar.speedLimitKmh"
              type="number"
              min={1}
              inputMode="numeric"
              value={values.radar.speedLimitKmh}
              aria-invalid={!!error('speedLimitKmh')}
              aria-describedby={describedBy('radar.speedLimitKmh', error('speedLimitKmh'))}
              onChange={(e) => onChange('radar', { ...values.radar, speedLimitKmh: e.target.value })}
            />
          </Field>
          <Field id="radar.lanes" label="Faixas monitoradas" error={error('lanes')}>
            <input
              id="radar.lanes"
              type="number"
              min={1}
              inputMode="numeric"
              value={values.radar.lanes}
              aria-invalid={!!error('lanes')}
              aria-describedby={describedBy('radar.lanes', error('lanes'))}
              onChange={(e) => onChange('radar', { ...values.radar, lanes: e.target.value })}
            />
          </Field>
        </>
      )}

      {block === 'camera' && (
        <>
          <Field id="camera.resolution" label="Resolução" error={error('resolution')}>
            <select
              id="camera.resolution"
              value={values.camera.resolution}
              onChange={(e) =>
                onChange('camera', { ...values.camera, resolution: e.target.value as CameraResolution })
              }
            >
              {CAMERA_RESOLUTIONS.map((resolution) => (
                <option key={resolution} value={resolution}>
                  {CAMERA_RESOLUTION_LABEL[resolution]}
                </option>
              ))}
            </select>
          </Field>
          <Checkbox
            id="camera.nightVision"
            label="Visão noturna"
            checked={values.camera.nightVision}
            onChange={(checked) => onChange('camera', { ...values.camera, nightVision: checked })}
          />
          <Checkbox
            id="camera.color"
            label="Colorida"
            checked={values.camera.color}
            onChange={(checked) => onChange('camera', { ...values.camera, color: checked })}
          />
          <Checkbox
            id="camera.ptz"
            label="Gira e dá zoom (PTZ)"
            checked={values.camera.ptz}
            onChange={(checked) => onChange('camera', { ...values.camera, ptz: checked })}
          />
        </>
      )}

      {block === 'truckScale' && (
        <>
          <Field id="truckScale.maxCapacityTons" label="Capacidade máxima (t) *" error={error('maxCapacityTons')}>
            <input
              id="truckScale.maxCapacityTons"
              type="number"
              min={0.01}
              step={0.01}
              inputMode="decimal"
              value={values.truckScale.maxCapacityTons}
              aria-invalid={!!error('maxCapacityTons')}
              aria-describedby={describedBy('truckScale.maxCapacityTons', error('maxCapacityTons'))}
              onChange={(e) => onChange('truckScale', { ...values.truckScale, maxCapacityTons: e.target.value })}
            />
          </Field>
          <Checkbox
            id="truckScale.weighInMotion"
            label="Pesa em movimento"
            checked={values.truckScale.weighInMotion}
            onChange={(checked) => onChange('truckScale', { ...values.truckScale, weighInMotion: checked })}
          />
        </>
      )}

      {block === 'vehicle' && (
        <>
          <Field
            id="vehicle.licensePlate"
            label="Placa *"
            error={error('licensePlate')}
            hint="Mercosul (ABC1D23) ou antiga (ABC1234), sem hífen"
          >
            <input
              id="vehicle.licensePlate"
              value={values.vehicle.licensePlate}
              maxLength={7}
              autoCapitalize="characters"
              aria-invalid={!!error('licensePlate')}
              aria-describedby={describedBy(
                'vehicle.licensePlate',
                error('licensePlate'),
                'Mercosul (ABC1D23) ou antiga (ABC1234), sem hífen',
              )}
              onChange={(e) =>
                onChange('vehicle', {
                  ...values.vehicle,
                  licensePlate: e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''),
                })
              }
            />
          </Field>
          <Field id="vehicle.passengerCapacity" label="Passageiros" error={error('passengerCapacity')}>
            <input
              id="vehicle.passengerCapacity"
              type="number"
              min={0}
              inputMode="numeric"
              value={values.vehicle.passengerCapacity}
              aria-invalid={!!error('passengerCapacity')}
              aria-describedby={describedBy('vehicle.passengerCapacity', error('passengerCapacity'))}
              onChange={(e) => onChange('vehicle', { ...values.vehicle, passengerCapacity: e.target.value })}
            />
          </Field>
          <Checkbox
            id="vehicle.autonomous"
            label="Autônomo"
            checked={values.vehicle.autonomous}
            onChange={(checked) => onChange('vehicle', { ...values.vehicle, autonomous: checked })}
          />
        </>
      )}

      {block === 'scooter' && (
        <>
          <Field id="scooter.ownership" label="Propriedade" error={error('ownership')}>
            <select
              id="scooter.ownership"
              value={values.scooter.ownership}
              onChange={(e) =>
                onChange('scooter', { ...values.scooter, ownership: e.target.value as ScooterOwnership })
              }
            >
              {SCOOTER_OWNERSHIPS.map((ownership) => (
                <option key={ownership} value={ownership}>
                  {SCOOTER_OWNERSHIP_LABEL[ownership]}
                </option>
              ))}
            </select>
          </Field>
          {values.scooter.ownership === 'SHARED' && (
            <Field id="scooter.operatorName" label="Operadora *" error={error('operatorName')}>
              <input
                id="scooter.operatorName"
                value={values.scooter.operatorName}
                maxLength={100}
                aria-invalid={!!error('operatorName')}
                aria-describedby={describedBy('scooter.operatorName', error('operatorName'))}
                onChange={(e) => onChange('scooter', { ...values.scooter, operatorName: e.target.value })}
              />
            </Field>
          )}
          <Field id="scooter.batteryLevel" label="Bateria (%)" error={error('batteryLevel')}>
            <input
              id="scooter.batteryLevel"
              type="number"
              min={0}
              max={100}
              inputMode="numeric"
              value={values.scooter.batteryLevel}
              aria-invalid={!!error('batteryLevel')}
              aria-describedby={describedBy('scooter.batteryLevel', error('batteryLevel'))}
              onChange={(e) => onChange('scooter', { ...values.scooter, batteryLevel: e.target.value })}
            />
          </Field>
        </>
      )}
    </div>
  )
}
