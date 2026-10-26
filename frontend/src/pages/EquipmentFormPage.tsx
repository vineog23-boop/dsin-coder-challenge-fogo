import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router'
import Field from '../components/Field'
import TextField from '../components/TextField'
import LocationPicker from '../components/LocationPicker'
import TypeSpecificFields from '../components/TypeSpecificFields'
import { ApiError } from '../lib/api/client'
import {
  AGENCIES,
  EQUIPMENT_STATUSES,
  EQUIPMENT_TYPES,
  MAINTENANCE_NEEDS,
  REGIONS,
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
  type Region,
} from '../lib/api/equipments'
import { lookupCep } from '../lib/api/viaCep'
import { EMPTY_BLOCKS, type BlockValues } from '../lib/blockValues'
import { describedBy } from '../lib/describedBy'
import { regionOf } from '../lib/geo'
import {
  AGENCY_LABEL,
  MAINTENANCE_NEED_LABEL,
  REGION_LABEL,
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
  zipCode: string
  street: string
  reference: string
  neighborhood: string
  city: string
  state: string
  region: Region
  latitude: number | null
  longitude: number | null
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
  zipCode: '',
  street: '',
  reference: '',
  neighborhood: '',
  city: 'Presidente Prudente',
  state: 'SP',
  region: 'CENTER',
  latitude: null,
  longitude: null,
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
    location: {
      zipCode: form.zipCode.replace(/\D/g, '') || null,
      street: form.street.trim(),
      reference: form.reference.trim() || null,
      neighborhood: form.neighborhood.trim(),
      city: form.city.trim(),
      state: form.state.trim().toUpperCase(),
      region: form.region,
      latitude: form.latitude as number,
      longitude: form.longitude as number,
    },
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
    zipCode: location.zipCode ?? '',
    street: location.street ?? '',
    reference: location.reference ?? '',
    neighborhood: location.neighborhood ?? '',
    city: location.city ?? '',
    state: location.state ?? '',
    region: location.region,
    latitude: location.latitude,
    longitude: location.longitude,
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
  const [cepHint, setCepHint] = useState<string | null>(null)
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

  async function fillFromCep() {
    const digits = form.zipCode.replace(/\D/g, '')
    if (digits.length !== 8) return
    setCepHint('Buscando CEP…')
    const address = await lookupCep(digits)
    if (!address) {
      setCepHint('CEP não encontrado ou serviço indisponível. Preencha o endereço manualmente.')
      return
    }
    setCepHint(null)
    setForm((current) => ({
      ...current,
      street: address.street || current.street,
      neighborhood: address.neighborhood || current.neighborhood,
      city: address.city || current.city,
      state: address.state || current.state,
    }))
  }

  function pickLocation(latitude: number, longitude: number) {
    setForm((current) => ({ ...current, latitude, longitude, region: regionOf(latitude, longitude) }))
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

  const locationError = errors['location.latitude'] ?? errors['location.longitude'] ?? errors.location

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

        <fieldset>
          <legend>Localização{blockOf(form.type) === 'vehicle' ? ' da base' : ''}</legend>
          <Field id="location.zipCode" label="CEP" error={errors['location.zipCode']} hint={cepHint ?? 'Preenche rua, bairro e cidade.'}>
            <input
              id="location.zipCode"
              inputMode="numeric"
              maxLength={9}
              value={form.zipCode}
              aria-invalid={!!errors['location.zipCode']}
              aria-describedby={describedBy('location.zipCode', errors['location.zipCode'], 'cep')}
              onChange={(e) => set('zipCode', e.target.value.replace(/\D/g, '').slice(0, 8))}
              onBlur={fillFromCep}
            />
          </Field>
          <TextField id="location.street" label="Rua *" value={form.street} error={errors['location.street']}
            maxLength={120} onChange={(value) => set('street', value)} />
          <TextField id="location.reference" label="Referência" value={form.reference}
            error={errors['location.reference']} maxLength={120} placeholder="número 181, oposto ao 181, S/N…"
            onChange={(value) => set('reference', value)} />
          <TextField id="location.neighborhood" label="Bairro *" value={form.neighborhood}
            error={errors['location.neighborhood']} maxLength={80} onChange={(value) => set('neighborhood', value)} />
          <TextField id="location.city" label="Cidade *" value={form.city} error={errors['location.city']}
            maxLength={80} onChange={(value) => set('city', value)} />
          <TextField id="location.state" label="UF *" value={form.state} error={errors['location.state']}
            maxLength={2} onChange={(value) => set('state', value.toUpperCase())} />
          <Field id="location.region" label="Região *" error={errors['location.region']}
            hint="Sugerida pelo ponto no mapa; pode ajustar.">
            <select id="location.region" value={form.region} onChange={(e) => set('region', e.target.value as Region)}>
              {REGIONS.map((region) => (
                <option key={region} value={region}>
                  {REGION_LABEL[region]}
                </option>
              ))}
            </select>
          </Field>
          <div className={`field field-wide${locationError ? ' field-invalid' : ''}`}>
            <span className="field-label" id="map-label">
              Posição no mapa * <small className="muted">(clique para marcar)</small>
            </span>
            <LocationPicker latitude={form.latitude} longitude={form.longitude} onPick={pickLocation} />
            <small className="muted" aria-live="polite">
              {form.latitude !== null && form.longitude !== null
                ? `Latitude ${form.latitude.toFixed(6)}, longitude ${form.longitude.toFixed(6)}`
                : 'Nenhum ponto marcado.'}
            </small>
            {locationError && <small className="field-error">{locationError}</small>}
          </div>
        </fieldset>

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
