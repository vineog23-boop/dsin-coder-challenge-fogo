import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import Characteristics from '../components/Characteristics'
import LocationPicker from '../components/LocationPicker'
import MaintenanceForm from '../components/MaintenanceForm'
import StatusBadge from '../components/StatusBadge'
import StatusChangeForm from '../components/StatusChangeForm'
import StatusHistory from '../components/StatusHistory'
import { ApiError } from '../lib/api/client'
import { getEquipment, getHistory, type Equipment, type HistoryEvent, type Page } from '../lib/api/equipments'
import {
  AGENCY_LABEL,
  MAINTENANCE_NEED_LABEL,
  REGION_LABEL,
  TYPE_LABEL,
  formatDate,
} from '../lib/labels'
import { canEdit, useSession } from '../lib/session/session'

type Action = 'status' | 'maintenance' | null

export default function EquipmentDetailPage() {
  const { id = '' } = useParams()
  const { session } = useSession()
  const [equipment, setEquipment] = useState<Equipment | null>(null)
  const [history, setHistory] = useState<Page<HistoryEvent> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [action, setAction] = useState<Action>(null)
  const [message, setMessage] = useState<string | null>(null)

  // Detalhe e histórico sempre juntos: as ações mudam os dois.
  const fetchAll = useCallback(() => Promise.all([getEquipment(id), getHistory(id)]), [id])

  const show = useCallback(([loaded, events]: [Equipment, Page<HistoryEvent>]) => {
    setEquipment(loaded)
    setHistory(events)
    setError(null)
  }, [])

  const fail = useCallback((e: unknown) => {
    setError(e instanceof ApiError ? e.message : 'Erro ao carregar o equipamento.')
  }, [])

  useEffect(() => {
    let active = true
    fetchAll()
      .then((data) => active && show(data))
      .catch((e) => active && fail(e))
    return () => {
      active = false
    }
  }, [fetchAll, show, fail])

  async function finish(text: string) {
    setAction(null)
    try {
      show(await fetchAll())
      setMessage(text)
    } catch (e) {
      fail(e)
    }
  }

  if (error) {
    return (
      <div className="callout callout-error" role="alert">
        <p>{error}</p>
        <Link to="/equipamentos">Voltar à lista</Link>
      </div>
    )
  }
  if (!equipment || !history) return <p aria-live="polite">Carregando…</p>

  const { location } = equipment

  return (
    <section className="page">
      <p>
        <Link to="/equipamentos">← Equipamentos</Link>
      </p>
      <div className="page-header">
        <div>
          <h1>{equipment.serialNumber}</h1>
          <p className="muted">
            {TYPE_LABEL[equipment.type]} · {equipment.brand} {equipment.model}
          </p>
        </div>
        <StatusBadge status={equipment.status} />
      </div>

      {message && (
        <p className="callout callout-success" role="status">
          {message}
        </p>
      )}

      {canEdit(session) && (
        <div className="actions">
          <Link className="button" to={`/equipamentos/${equipment.id}/editar`}>
            Editar
          </Link>
          <button type="button" aria-expanded={action === 'status'} onClick={() => { setMessage(null); setAction('status') }}>
            Mudar situação
          </button>
          <button
            type="button"
            aria-expanded={action === 'maintenance'}
            onClick={() => { setMessage(null); setAction('maintenance') }}
          >
            Registrar manutenção
          </button>
        </div>
      )}

      {action === 'status' && (
        <StatusChangeForm
          equipment={equipment}
          onCancel={() => setAction(null)}
          onDone={() => finish('Situação atualizada.')}
        />
      )}
      {action === 'maintenance' && (
        <MaintenanceForm
          equipmentId={equipment.id}
          onCancel={() => setAction(null)}
          onDone={() => finish('Manutenção registrada.')}
        />
      )}

      <div className="detail-grid">
        <section className="card" aria-labelledby="general-title">
          <h2 id="general-title">Dados gerais</h2>
          <dl className="details">
            <div>
              <dt>Órgão</dt>
              <dd>{AGENCY_LABEL[equipment.agency]}</dd>
            </div>
            <div>
              <dt>Manutenção</dt>
              <dd>
                {MAINTENANCE_NEED_LABEL[equipment.maintenanceNeed]}
                {equipment.nextMaintenanceDate && ` · próxima em ${formatDate(equipment.nextMaintenanceDate)}`}
              </dd>
            </div>
            <div>
              <dt>Intervalo</dt>
              <dd>{equipment.maintenanceIntervalDays ? `${equipment.maintenanceIntervalDays} dias` : 'Sem intervalo'}</dd>
            </div>
          </dl>
          <h3>Características</h3>
          <Characteristics equipment={equipment} />
        </section>

        <section className="card" aria-labelledby="location-title">
          <h2 id="location-title">Localização</h2>
          <p>
            {location.street}
            {location.reference && `, ${location.reference}`}
            <br />
            {location.neighborhood} · {location.city}/{location.state}
            {location.zipCode && ` · CEP ${location.zipCode.replace(/(\d{5})(\d{3})/, '$1-$2')}`}
            <br />
            Região {REGION_LABEL[location.region]}
          </p>
          <LocationPicker latitude={location.latitude} longitude={location.longitude} height={200} />
        </section>
      </div>

      <section className="card" aria-labelledby="history-title">
        <h2 id="history-title">Histórico</h2>
        <StatusHistory events={history.content} total={history.page.totalElements} />
      </section>
    </section>
  )
}
