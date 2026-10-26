import { useEffect, useState } from 'react'
import { AGENT_AGENCY, AGENT_VEHICLE_TYPE, type AgentType, type VehicleSummary } from '../lib/api/agents'
import { listEquipments, type Equipment } from '../lib/api/equipments'
import { describedBy } from '../lib/describedBy'
import { STATUS_LABEL } from '../lib/labels'
import Field from './Field'

type Props = {
  agentType: AgentType
  value: string
  /** Viatura atual do agente: continua na lista mesmo que não viesse na busca. */
  current: VehicleSummary | null
  disabled: boolean
  disabledReason?: string
  error?: string
  onChange: (vehicleId: string) => void
}

type Option = { id: string; label: string }

function toOption(vehicle: Pick<Equipment, 'id' | 'serialNumber' | 'status'> & { licensePlate: string }): Option {
  return { id: vehicle.id, label: `${vehicle.licensePlate} · ${vehicle.serialNumber} (${STATUS_LABEL[vehicle.status]})` }
}

/**
 * Só oferece viaturas compatíveis com a regra 2: tipo de viatura do órgão do agente, do mesmo órgão,
 * nem desativada nem roubada. A API confere de novo e responde 422 se algo mudar no meio do caminho.
 */
export default function VehicleSelect({ agentType, value, current, disabled, disabledReason, error, onChange }: Props) {
  const [result, setResult] = useState<{ type: AgentType; options: Option[] | null; failed: boolean } | null>(null)
  const loaded = result?.type === agentType ? result : null

  useEffect(() => {
    let active = true
    listEquipments({ type: AGENT_VEHICLE_TYPE[agentType], agency: AGENT_AGENCY[agentType] }, 0, 100)
      .then((page) => {
        if (!active) return
        const options = page.content
          .filter((equipment) => equipment.status !== 'DEACTIVATED' && equipment.status !== 'STOLEN')
          .filter((equipment) => equipment.vehicle)
          .map((equipment) => toOption({ ...equipment, licensePlate: equipment.vehicle!.licensePlate }))
        setResult({ type: agentType, options, failed: false })
      })
      .catch(() => active && setResult({ type: agentType, options: null, failed: true }))
    return () => {
      active = false
    }
  }, [agentType])

  const options = loaded?.options ?? []
  const withCurrent =
    current && !options.some((option) => option.id === current.id) && AGENT_VEHICLE_TYPE[agentType] === current.type
      ? [toOption(current), ...options]
      : options
  const hint = disabled
    ? disabledReason
    : loaded?.failed
      ? 'Não foi possível carregar as viaturas.'
      : loaded && withCurrent.length === 0
        ? 'Nenhuma viatura disponível para este órgão.'
        : undefined

  return (
    <Field id="vehicleId" label="Viatura" error={error} hint={hint}>
      <select
        id="vehicleId"
        value={disabled ? '' : value}
        disabled={disabled || !loaded}
        aria-invalid={!!error}
        aria-describedby={describedBy('vehicleId', error, hint)}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="">{loaded || disabled ? 'Sem viatura' : 'Carregando…'}</option>
        {withCurrent.map((option) => (
          <option key={option.id} value={option.id}>
            {option.label}
          </option>
        ))}
      </select>
    </Field>
  )
}
