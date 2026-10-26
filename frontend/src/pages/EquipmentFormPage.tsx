import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router'
import Field from '../components/Field'
import TextField from '../components/TextField'
import LocationFields from '../components/LocationFields'
import TypeSpecificFields from '../components/TypeSpecificFields'
import { ApiError } from '../lib/api/client'
import {
  AGENCIES,
  EQUIPMENT_STATUSES,
  EQUIPMENT_TYPES,
  MAINTENANCE_NEEDS,
  blockOf,
  createEquipment,
  getEquipment,
  updateEquipment,
  type Agency,
  type CharacteristicsBlock,
  type Equipment,
  type EquipmentRequest,
  type EquipmentStatus,
  type EquipmentType,
  type MaintenanceNeed,
} from '../lib/api/equipments'
import { EMPTY_BLOCKS, type BlockValues } from '../lib/blockValues'
import { EMPTY_LOCATION, fromLocation, toLocation, type LocationForm } from '../lib/locationForm'
import { describedBy } from '../lib/describedBy'
import {
  AGENCY_LABEL,
  MAINTENANCE_NEED_LABEL,
  STATUS_LABEL,
  TYPE_LABEL,
} from '../lib/labels'
import { canEdit, useSession } from '../lib/session/session'

type FormState = {
  type: EquipmentType
  serialNumber: string
  brand: string
  model: string
  agency: Agency
  status: EquipmentStatus
  maintenanceNeed: MaintenanceNeed
  nextMaintenanceDate: string
  maintenanceIntervalDays: string
  location: LocationForm
  blocks: BlockValues
}

const EMPTY_FORM: FormState = {
  type: 'RADAR',
  serialNumber: '',
  brand: '',
  model: '',
  agency: 'MUNICIPAL_TRAFFIC',
  status: 'OPERATIONAL',
  maintenanceNeed: 'NONE',
  nextMaintenanceDate: '',
  maintenanceIntervalDays: '',
  location: EMPTY_LOCATION,
  blocks: EMPTY_BLOCKS,
}

function toNumber(value: string): number | null {
  return value.trim() === '' ? null : Number(value)
}

function toText(value: number | null | undefined): string {
  return value === null || value === undefined ? '' : String(value)
}

/** Só o bloco do tipo escolhido vai para a API; os outros seguem nulos, como o back exige. */
function toRequest(form: FormState, isNew: boolean): EquipmentRequest {
  const block = blockOf(form.type)
  const { radar, camera, truckScale, vehicle, scooter } = form.blocks

  return {
    type: form.type,
    serialNumber: form.serialNumber.trim(),
    brand: form.brand.trim(),
    model: form.model.trim(),
    agency: form.agency,
    status: isNew ? form.status : null,
    maintenanceNeed: form.maintenanceNeed,
    nextMaintenanceDate: form.nextMaintenanceDate || null,
    maintenanceIntervalDays: toNumber(form.maintenanceIntervalDays),
    location: toLocation(form.location),
    radar:
      block === 'radar'
        ? { modality: radar.modality, speedLimitKmh: toNumber(radar.speedLimitKmh), lanes: toNumber(radar.lanes) }
        : null,
    camera: block === 'camera' ? camera : null,
    truckScale:
      block === 'truckScale'
        ? { maxCapacityTons: toNumber(truckScale.maxCapacityTons) as number, weighInMotion: truckScale.weighInMotion }
        : null,
    vehicle:
      block === 'vehicle'
        ? {
            licensePlate: vehicle.licensePlate,
            passengerCapacity: toNumber(vehicle.passengerCapacity),
            autonomous: vehicle.autonomous,
          }
        : null,
    scooter:
      block === 'scooter'
        ? {
            ownership: scooter.ownership,
            operatorName: scooter.ownership === 'SHARED' ? scooter.operatorName.trim() || null : null,
            batteryLevel: toNumber(scooter.batteryLevel),
          }
        : null,
  }
}

function toForm(equipment: Equipment): FormState {
  const { location } = equipment
  return {
    type: equipment.type,
    serialNumber: equipment.serialNumber,
    brand: equipment.brand,
    model: equipment.model,
    agency: equipment.agency,
    status: equipment.status,
    maintenanceNeed: equipment.maintenanceNeed,
    nextMaintenanceDate: equipment.nextMaintenanceDate ?? '',
    maintenanceIntervalDays: toText(equipment.maintenanceIntervalDays),
    location: fromLocation(location),
    blocks: {
      radar: equipment.radar
        ? {
            modality: equipment.radar.modality,
            speedLimitKmh: toText(equipment.radar.speedLimitKmh),
            lanes: toText(equipment.radar.lanes),
          }
        : EMPTY_BLOCKS.radar,
      camera: equipment.camera ?? EMPTY_BLOCKS.camera,
      truckScale: equipment.truckScale
        ? {
            maxCapacityTons: toText(equipment.truckScale.maxCapacityTons),
            weighInMotion: equipment.truckScale.weighInMotion,
          }
        : EMPTY_BLOCKS.truckScale,
      vehicle: equipment.vehicle
        ? {
            licensePlate: equipment.vehicle.licensePlate,
            passengerCapacity: toText(equipment.vehicle.passengerCapacity),
            autonomous: equipment.vehicle.autonomous,
          }
        : EMPTY_BLOCKS.vehicle,
      scooter: equipment.scooter
        ? {
            ownership: equipment.scooter.ownership,
            operatorName: equipment.scooter.operatorName ?? '',
            batteryLevel: toText(equipment.scooter.batteryLevel),
          }
        : EMPTY_BLOCKS.scooter,
    },
  }
}

export default function EquipmentFormPage() {
  const { id } = useParams()
  const isNew = !id
  const navigate = useNavigate()
  const { session } = useSession()
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [loading, setLoading] = useState(!isNew)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (isNew) return
    let active = true
    getEquipment(id)
      .then((equipment) => active && setForm(toForm(equipment)))
      .catch((e) => active && setLoadError(e instanceof ApiError ? e.message : 'Erro ao carregar o equipamento.'))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [id, isNew])

  if (!canEdit(session)) {
    return <Navigate to={isNew ? '/equipamentos' : `/equipamentos/${id}`} replace />
  }

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  function setBlock<B extends CharacteristicsBlock>(block: B, values: BlockValues[B]) {
    setForm((current) => ({ ...current, blocks: { ...current.blocks, [block]: values } }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrors({})
    setFormError(null)
    setSubmitting(true)
    try {
      const request = toRequest(form, isNew)
      const saved = isNew ? await createEquipment(request) : await updateEquipment(id, request)
      navigate(`/equipamentos/${saved.id}`)
    } catch (e) {
      if (e instanceof ApiError && e.problem.errors?.length) {
        setErrors(Object.fromEntries(e.problem.errors.map((error) => [error.field, error.message])))
        setFormError('Corrija os campos destacados.')
      } else {
        setFormError(e instanceof ApiError ? e.message : 'Não foi possível salvar.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <p aria-live="polite">Carregando…</p>
  if (loadError) {
    return (
      <div className="callout callout-error" role="alert">
        <p>{loadError}</p>
        <Link to="/equipamentos">Voltar à lista</Link>
      </div>
    )
  }

  return (
    <section className="page">
      <div className="page-header">
        <h1>{isNew ? 'Novo equipamento' : `Editar ${form.serialNumber}`}</h1>
        <Link to={isNew ? '/equipamentos' : `/equipamentos/${id}`}>Cancelar</Link>
      </div>

      <form className="equipment-form" onSubmit={handleSubmit} noValidate>
        <fieldset>
          <legend>Identificação</legend>
          <Field
            id="type"
            label="Tipo *"
            error={errors.type}
            hint={isNew ? undefined : 'O tipo não muda depois de criado.'}
          >
            <select
              id="type"
              value={form.type}
              disabled={!isNew}
              aria-describedby={describedBy('type', errors.type, isNew ? undefined : 'O tipo não muda depois de criado.')}
              onChange={(e) => set('type', e.target.value as EquipmentType)}
            >
              {EQUIPMENT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {TYPE_LABEL[type]}
                </option>
              ))}
            </select>
          </Field>
          <TextField id="serialNumber" label="Número de série *" value={form.serialNumber} error={errors.serialNumber}
            maxLength={60} onChange={(value) => set('serialNumber', value)} />
          <TextField id="brand" label="Marca *" value={form.brand} error={errors.brand} maxLength={60}
            onChange={(value) => set('brand', value)} />
          <TextField id="model" label="Modelo *" value={form.model} error={errors.model} maxLength={60}
            onChange={(value) => set('model', value)} />
          <Field id="agency" label="Órgão responsável *" error={errors.agency}>
            <select id="agency" value={form.agency} onChange={(e) => set('agency', e.target.value as Agency)}>
              {AGENCIES.map((agency) => (
                <option key={agency} value={agency}>
                  {AGENCY_LABEL[agency]}
                </option>
              ))}
            </select>
          </Field>
          {isNew && (
            <Field id="status" label="Situação inicial" error={errors.status}>
              <select id="status" value={form.status} onChange={(e) => set('status', e.target.value as EquipmentStatus)}>
                {EQUIPMENT_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {STATUS_LABEL[status]}
                  </option>
                ))}
              </select>
            </Field>
          )}
        </fieldset>

        <fieldset>
          <legend>Características: {TYPE_LABEL[form.type]}</legend>
          <TypeSpecificFields block={blockOf(form.type)} values={form.blocks} errors={errors} onChange={setBlock} />
        </fieldset>

        <fieldset>
          <legend>Manutenção</legend>
          <Field id="maintenanceNeed" label="Necessidade" error={errors.maintenanceNeed}>
            <select
              id="maintenanceNeed"
              value={form.maintenanceNeed}
              onChange={(e) => set('maintenanceNeed', e.target.value as MaintenanceNeed)}
            >
              {MAINTENANCE_NEEDS.map((need) => (
                <option key={need} value={need}>
                  {MAINTENANCE_NEED_LABEL[need]}
                </option>
              ))}
            </select>
          </Field>
          <Field id="nextMaintenanceDate" label={`Próxima manutenção${form.maintenanceNeed === 'SCHEDULED' ? ' *' : ''}`}
            error={errors.nextMaintenanceDate}>
            <input
              id="nextMaintenanceDate"
              type="date"
              value={form.nextMaintenanceDate}
              aria-invalid={!!errors.nextMaintenanceDate}
              aria-describedby={describedBy('nextMaintenanceDate', errors.nextMaintenanceDate)}
              onChange={(e) => set('nextMaintenanceDate', e.target.value)}
            />
          </Field>
          <Field id="maintenanceIntervalDays" label="Intervalo (dias)" error={errors.maintenanceIntervalDays}
            hint="Com intervalo, cada manutenção agenda a próxima.">
            <input
              id="maintenanceIntervalDays"
              type="number"
              min={1}
              inputMode="numeric"
              value={form.maintenanceIntervalDays}
              aria-invalid={!!errors.maintenanceIntervalDays}
              aria-describedby={describedBy('maintenanceIntervalDays', errors.maintenanceIntervalDays,
                'Com intervalo, cada manutenção agenda a próxima.')}
              onChange={(e) => set('maintenanceIntervalDays', e.target.value)}
            />
          </Field>
        </fieldset>

        <LocationFields
          prefix="location"
          legend={`Localização${blockOf(form.type) === 'vehicle' ? ' da base' : ''}`}
          value={form.location}
          errors={errors}
          onChange={(location) => set('location', location)}
        />

        {formError && (
          <p className="form-error" role="alert">
            {formError}
          </p>
        )}
        <div className="form-actions">
          <button type="submit" disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>
    </section>
  )
}
