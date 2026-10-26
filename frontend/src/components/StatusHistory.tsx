import type { HistoryEvent } from '../lib/api/equipments'
import { MAINTENANCE_KIND_LABEL, STATUS_LABEL, formatDateTime } from '../lib/labels'

type Props = {
  events: HistoryEvent[]
  total: number
}

/** Linha do tempo de situações e manutenções, do mais recente para o mais antigo. */
export default function StatusHistory({ events, total }: Props) {
  if (events.length === 0) {
    return <p className="empty">Sem histórico ainda.</p>
  }

  return (
    <>
      <ol className="timeline">
        {events.map((event, index) => (
          <li key={`${event.occurredAt}-${index}`} className={`timeline-item timeline-${event.eventType.toLowerCase()}`}>
            <time dateTime={event.occurredAt}>{formatDateTime(event.occurredAt)}</time>
            {event.eventType === 'STATUS_CHANGE' ? (
              <p>
                <strong>
                  {event.fromStatus ? `${STATUS_LABEL[event.fromStatus]} → ` : ''}
                  {event.toStatus && STATUS_LABEL[event.toStatus]}
                </strong>
                {event.note && <> · {event.note}</>}
              </p>
            ) : (
              <p>
                <strong>Manutenção {event.maintenanceKind && MAINTENANCE_KIND_LABEL[event.maintenanceKind].toLowerCase()}</strong>
                {event.description && <> · {event.description}</>}
              </p>
            )}
            <small className="muted">por {event.performedBy}</small>
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
