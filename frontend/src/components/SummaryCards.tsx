import type { Summary } from '../lib/api/operations'

type Card = { label: string; value: string | number; detail?: string; tone: 'danger' | 'warning' | 'info' | 'ok' }

/**
 * Disponibilidade sobre o parque em uso: desativados e não instalados não contam, porque não deveriam estar
 * operando. Indisponíveis são os que deveriam operar e não operam: com falha, em manutenção ou roubados.
 */
function availability({ total, byStatus }: Summary['equipments']) {
  const inUse = total - byStatus.DEACTIVATED - byStatus.NOT_INSTALLED
  const unavailable = byStatus.MALFUNCTION + byStatus.UNDER_MAINTENANCE + byStatus.STOLEN
  const percent = inUse > 0 ? Math.round((byStatus.OPERATIONAL / inUse) * 100) : 0
  return { inUse, unavailable, percent }
}

/** Primeiro a foto do parque, depois o que pede ação. */
export default function SummaryCards({ summary }: { summary: Summary }) {
  const { equipments, agents } = summary
  const { inUse, unavailable, percent } = availability(equipments)
  const cards: Card[] = [
    { label: 'Equipamentos', value: equipments.total, tone: 'info' },
    { label: 'Operacionais', value: equipments.byStatus.OPERATIONAL, tone: 'ok' },
    { label: 'Indisponíveis', value: unavailable, detail: 'falha, manutenção ou roubo', tone: 'danger' },
    { label: 'Disponibilidade', value: `${percent}%`, detail: `de ${inUse} em uso`, tone: percent < 90 ? 'warning' : 'ok' },
    { label: 'Agentes em atividade', value: agents.active, detail: `de ${agents.total}`, tone: 'info' },
    { label: 'Equipamentos com falha', value: equipments.byStatus.MALFUNCTION, tone: 'danger' },
    { label: 'Manutenção imediata', value: equipments.immediateMaintenance, tone: 'warning' },
    { label: 'Manutenção atrasada', value: equipments.overdueMaintenance, tone: 'warning' },
    { label: 'Manutenção nos próximos 7 dias', value: equipments.upcomingMaintenance, tone: 'info' },
  ]

  return (
    <ul className="summary-cards" aria-label="Indicadores">
      {cards.map((card) => (
        <li key={card.label} className={`summary-card summary-${card.tone}`}>
          <span className="summary-value">{card.value}</span>
          <span className="summary-label">
            {card.label}
            {card.detail && <small className="muted"> {card.detail}</small>}
          </span>
        </li>
      ))}
    </ul>
  )
}
