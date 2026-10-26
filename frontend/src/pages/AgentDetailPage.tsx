import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import AgentHistory from '../components/AgentHistory'
import AgentStatusBadge from '../components/AgentStatusBadge'
import LocationPicker from '../components/LocationPicker'
import StatusChangeForm from '../components/StatusChangeForm'
import {
  AGENT_STATUSES,
  changeAgentStatus,
  getAgent,
  getAgentHistory,
  isOnDuty,
  type Agent,
  type AgentHistoryEvent,
} from '../lib/api/agents'
import { ApiError } from '../lib/api/client'
import type { Page } from '../lib/api/equipments'
import {
  AGENCY_LABEL,
  AGENT_STATUS_LABEL,
  AGENT_TYPE_LABEL,
  REGION_LABEL,
  STATUS_LABEL,
  formatDateTime,
} from '../lib/labels'
import { canEdit, useSession } from '../lib/session/session'

export default function AgentDetailPage() {
  const { id = '' } = useParams()
  const { session } = useSession()
  const [agent, setAgent] = useState<Agent | null>(null)
  const [history, setHistory] = useState<Page<AgentHistoryEvent> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [changing, setChanging] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  // Agente e histórico sempre juntos: mudar a situação altera os dois.
  const fetchAll = useCallback(() => Promise.all([getAgent(id), getAgentHistory(id)]), [id])

  const show = useCallback(([loaded, events]: [Agent, Page<AgentHistoryEvent>]) => {
    setAgent(loaded)
    setHistory(events)
    setError(null)
  }, [])

  const fail = useCallback((e: unknown) => {
    setError(e instanceof ApiError ? e.message : 'Erro ao carregar o agente.')
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

  async function finish() {
    setChanging(false)
    try {
      show(await fetchAll())
      setMessage('Situação atualizada.')
    } catch (e) {
      fail(e)
    }
  }

  if (error) {
    return (
      <div className="callout callout-error" role="alert">
        <p>{error}</p>
        <Link to="/agentes">Voltar à lista</Link>
      </div>
    )
  }
  if (!agent || !history) return <p aria-live="polite">Carregando…</p>

  const base = agent.baseLocation
  const hasPosition = agent.currentLatitude !== null && agent.currentLongitude !== null

  return (
    <section className="page">
      <p>
        <Link to="/agentes">← Agentes</Link>
      </p>
      <div className="page-header">
        <div>
          <h1>{agent.name}</h1>
          <p className="muted">
            {AGENT_TYPE_LABEL[agent.agentType]} · matrícula {agent.registrationNumber}
          </p>
        </div>
        <AgentStatusBadge status={agent.status} />
      </div>

      {message && (
        <p className="callout callout-success" role="status">
          {message}
        </p>
      )}

      {canEdit(session) && (
        <div className="actions">
          <Link className="button" to={`/agentes/${agent.id}/editar`}>
            Editar
          </Link>
          <button type="button" aria-expanded={changing} onClick={() => { setMessage(null); setChanging(true) }}>
            Mudar situação
          </button>
        </div>
      )}

      {changing && (
        <StatusChangeForm
          current={agent.status}
          statuses={AGENT_STATUSES}
          labels={AGENT_STATUS_LABEL}
          hint={(status) =>
            !isOnDuty(status) && agent.vehicle ? 'O agente sai de serviço e perde o vínculo com a viatura.' : null
          }
          onSubmit={(status, note) => changeAgentStatus(agent.id, status, note)}
          onCancel={() => setChanging(false)}
          onDone={finish}
        />
      )}

      <div className="detail-grid">
        <section className="card" aria-labelledby="general-title">
          <h2 id="general-title">Dados</h2>
          <dl className="details">
            <div>
              <dt>Órgão</dt>
              <dd>{AGENCY_LABEL[agent.agency]}</dd>
            </div>
            <div>
              <dt>Viatura</dt>
              <dd>
                {agent.vehicle ? (
                  <Link to={`/equipamentos/${agent.vehicle.id}`}>
                    {agent.vehicle.licensePlate} ({STATUS_LABEL[agent.vehicle.status]})
                  </Link>
                ) : (
                  'Sem viatura'
                )}
              </dd>
            </div>
            <div>
              <dt>Posição em campo</dt>
              <dd>
                {hasPosition && agent.positionUpdatedAt
                  ? `atualizada em ${formatDateTime(agent.positionUpdatedAt)}`
                  : 'Sem posição; o mapa mostra a base'}
              </dd>
            </div>
          </dl>
        </section>

        <section className="card" aria-labelledby="location-title">
          <h2 id="location-title">{hasPosition ? 'Posição atual' : 'Base'}</h2>
          <p>
            Base: {base.street}
            {base.reference && `, ${base.reference}`} · {base.neighborhood} · região {REGION_LABEL[base.region]}
          </p>
          {/* Regra 3: a posição em campo, se houver; senão, a base. */}
          <LocationPicker
            latitude={hasPosition ? agent.currentLatitude : base.latitude}
            longitude={hasPosition ? agent.currentLongitude : base.longitude}
            height={200}
          />
        </section>
      </div>

      <section className="card" aria-labelledby="history-title">
        <h2 id="history-title">Histórico</h2>
        <AgentHistory events={history.content} total={history.page.totalElements} />
      </section>
    </section>
  )
}
