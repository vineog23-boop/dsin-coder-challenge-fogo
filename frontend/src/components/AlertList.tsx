import { Link } from 'react-router'
import type { Alert } from '../lib/api/operations'
import { REGION_LABEL, TYPE_LABEL, formatDateTime } from '../lib/labels'

type Props = {
  alerts: Alert[]
  total: number
}

const SEVERITY_LABEL = { HIGH: 'Alta', MEDIUM: 'Média', LOW: 'Baixa' } as const

/** Alertas na ordem da API (severidade e depois o mais antigo); cada um leva ao que precisa de ação. */
export default function AlertList({ alerts, total }: Props) {
  if (alerts.length === 0) {
    return <p className="empty">Nenhum alerta no momento.</p>
  }

  return (
    <>
      <ol className="alert-list">
        {alerts.map((alert, index) => (
          <li key={`${alert.kind}-${alert.equipmentId ?? alert.region}-${index}`} className={`alert alert-${alert.severity.toLowerCase()}`}>
            <span className="alert-severity">{SEVERITY_LABEL[alert.severity]}</span>
            <p>
              {alert.equipmentId ? (
                <Link to={`/equipamentos/${alert.equipmentId}`}>{alert.message}</Link>
              ) : (
                <Link to={`/agentes?region=${alert.region}`}>{alert.message}</Link>
              )}
            </p>
            <small className="muted">
              {alert.equipmentType && `${TYPE_LABEL[alert.equipmentType]} · `}
              {REGION_LABEL[alert.region]}
              {alert.since && ` · desde ${formatDateTime(alert.since)}`}
            </small>
          </li>
        ))}
      </ol>
      {total > alerts.length && (
        <p className="muted">
          Mostrando {alerts.length} de {total} alertas.
        </p>
      )}
    </>
  )
}
