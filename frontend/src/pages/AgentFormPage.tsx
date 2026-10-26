import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router'
import Field from '../components/Field'
import LocationFields from '../components/LocationFields'
import TextField from '../components/TextField'
import VehicleSelect from '../components/VehicleSelect'
import {
  AGENT_AGENCY,
  AGENT_STATUSES,
  AGENT_TYPES,
  createAgent,
  getAgent,
  isOnDuty,
  updateAgent,
  type Agent,
  type AgentStatus,
  type AgentType,
  type VehicleSummary,
} from '../lib/api/agents'
import { ApiError } from '../lib/api/client'
import { AGENCY_LABEL, AGENT_STATUS_LABEL, AGENT_TYPE_LABEL } from '../lib/labels'
import { EMPTY_LOCATION, fromLocation, toLocation, type LocationForm } from '../lib/locationForm'
import { canEdit, useSession } from '../lib/session/session'

type FormState = {
  name: string
  registrationNumber: string
  agentType: AgentType
  status: AgentStatus
  baseLocation: LocationForm
  vehicleId: string
}

const EMPTY_FORM: FormState = {
  name: '',
  registrationNumber: '',
  agentType: 'TRAFFIC_AGENT',
  status: 'AVAILABLE',
  baseLocation: EMPTY_LOCATION,
  vehicleId: '',
}

function toForm(agent: Agent): FormState {
  return {
    name: agent.name,
    registrationNumber: agent.registrationNumber,
    agentType: agent.agentType,
    status: agent.status,
    baseLocation: fromLocation(agent.baseLocation),
    vehicleId: agent.vehicle?.id ?? '',
  }
}

export default function AgentFormPage() {
  const { id } = useParams()
  const isNew = !id
  const navigate = useNavigate()
  const { session } = useSession()
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [currentVehicle, setCurrentVehicle] = useState<VehicleSummary | null>(null)
  const [loading, setLoading] = useState(!isNew)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (isNew) return
    let active = true
    getAgent(id)
      .then((agent) => {
        if (!active) return
        setForm(toForm(agent))
        setCurrentVehicle(agent.vehicle)
      })
      .catch((e) => active && setLoadError(e instanceof ApiError ? e.message : 'Erro ao carregar o agente.'))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [id, isNew])

  if (!canEdit(session)) {
    return <Navigate to={isNew ? '/agentes' : `/agentes/${id}`} replace />
  }

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrors({})
    setFormError(null)
    setSubmitting(true)
    try {
      const request = {
        name: form.name.trim(),
        registrationNumber: form.registrationNumber.trim(),
        agentType: form.agentType,
        status: isNew ? form.status : null,
        baseLocation: toLocation(form.baseLocation),
        vehicleId: isOnDuty(form.status) && form.vehicleId ? form.vehicleId : null,
      }
      const saved = isNew ? await createAgent(request) : await updateAgent(id, request)
      navigate(`/agentes/${saved.id}`)
    } catch (e) {
      if (e instanceof ApiError && e.problem.errors?.length) {
        setErrors(Object.fromEntries(e.problem.errors.map((error) => [error.field, error.message])))
        setFormError('Corrija os campos destacados.')
      } else if (e instanceof ApiError && (e.problem.status === 422 || e.problem.status === 404)) {
        // Regra da viatura (órgão, situação) ou viatura que sumiu: o erro é desse campo.
        setErrors({ vehicleId: e.message })
        setFormError(e.message)
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
        <Link to="/agentes">Voltar à lista</Link>
      </div>
    )
  }

  return (
    <section className="page">
      <div className="page-header">
        <h1>{isNew ? 'Novo agente' : `Editar ${form.name}`}</h1>
        <Link to={isNew ? '/agentes' : `/agentes/${id}`}>Cancelar</Link>
      </div>

      <form className="equipment-form" onSubmit={handleSubmit} noValidate>
        <fieldset>
          <legend>Identificação</legend>
          <TextField id="name" label="Nome *" value={form.name} error={errors.name} maxLength={120}
            onChange={(value) => set('name', value)} />
          <TextField id="registrationNumber" label="Matrícula *" value={form.registrationNumber}
            error={errors.registrationNumber} maxLength={30} onChange={(value) => set('registrationNumber', value)} />
          <Field id="agentType" label="Tipo *" error={errors.agentType}
            hint={`Órgão: ${AGENCY_LABEL[AGENT_AGENCY[form.agentType]]}`}>
            <select
              id="agentType"
              value={form.agentType}
              onChange={(e) => {
                // Trocar o tipo troca o órgão: a viatura escolhida deixa de valer.
                setForm((current) => ({ ...current, agentType: e.target.value as AgentType, vehicleId: '' }))
              }}
            >
              {AGENT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {AGENT_TYPE_LABEL[type]}
                </option>
              ))}
            </select>
          </Field>
          {isNew && (
            <Field id="status" label="Situação inicial" error={errors.status}>
              <select id="status" value={form.status} onChange={(e) => set('status', e.target.value as AgentStatus)}>
                {AGENT_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {AGENT_STATUS_LABEL[status]}
                  </option>
                ))}
              </select>
            </Field>
          )}
          <VehicleSelect
            agentType={form.agentType}
            value={form.vehicleId}
            current={currentVehicle}
            disabled={!isOnDuty(form.status)}
            disabledReason="Agente fora de serviço ou afastado não fica com viatura."
            error={errors.vehicleId}
            onChange={(vehicleId) => set('vehicleId', vehicleId)}
          />
        </fieldset>

        <LocationFields
          prefix="baseLocation"
          legend="Base (batalhão, posto ou garagem)"
          value={form.baseLocation}
          errors={errors}
          onChange={(baseLocation) => set('baseLocation', baseLocation)}
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
