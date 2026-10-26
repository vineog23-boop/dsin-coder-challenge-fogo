import type { AgentHistoryEvent } from '../lib/api/agents'
import { AGENT_STATUS_LABEL, formatDateTime } from '../lib/labels'

type Props = {
  events: AgentHistoryEvent[]
  total: number
}

/** Linha do tempo das mudanças de situação do agente, da mais recente para a mais antiga. */
export default function AgentHistory({ events, total }: Props) {
  if (events.length === 0) {
    return <p className="empty">Sem histórico ainda.</p>
  }

  return (
    <>
      <ol className="timeline">
        {events.map((event, index) => (
          <li key={`${event.changedAt}-${index}`} className="timeline-item">
            <time dateTime={event.changedAt}>{formatDateTime(event.changedAt)}</time>
            <p>
              <strong>
                {event.fromStatus ? `${AGENT_STATUS_LABEL[event.fromStatus]} → ` : ''}
                {AGENT_STATUS_LABEL[event.toStatus]}
              </strong>
              {event.note && <> · {event.note}</>}
            </p>
            <small className="muted">por {event.changedBy}</small>
          </li>
        ))}
      </ol>
      {total > events.length && (
        <p className="muted">
          Mostrando os {events.length} eventos mais recentes de {total}.
        </p>
      )}
    </>
  )
}
