import type { Summary } from '../lib/api/operations'

/** Os quatro números que o gestor olha primeiro. */
export default function SummaryCards({ summary }: { summary: Summary }) {
  const { equipments, agents } = summary
  const cards = [
    { label: 'Equipamentos com falha', value: equipments.byStatus.MALFUNCTION, tone: 'danger' },
    { label: 'Manutenção imediata', value: equipments.immediateMaintenance, tone: 'warning' },
    { label: 'Manutenção atrasada', value: equipments.overdueMaintenance, tone: 'warning' },
    { label: 'Agentes em atividade', value: agents.active, detail: `de ${agents.total}`, tone: 'info' },
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
