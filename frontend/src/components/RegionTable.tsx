import type { Summary } from '../lib/api/operations'
import { REGION_LABEL } from '../lib/labels'

/** Falhas e agentes em atividade lado a lado: região com muita falha e pouco agente salta aos olhos. */
export default function RegionTable({ regions }: { regions: Summary['regions'] }) {
  return (
    <table className="data-table region-table">
      <caption className="muted">Por região</caption>
      <thead>
        <tr>
          <th scope="col">Região</th>
          <th scope="col">Com falha</th>
          <th scope="col">Agentes em atividade</th>
        </tr>
      </thead>
      <tbody>
        {regions.map((row) => (
          <tr key={row.region} className={row.activeAgents === 0 ? 'row-alert' : undefined}>
            <th scope="row">{REGION_LABEL[row.region]}</th>
            <td>{row.equipmentsInMalfunction}</td>
            <td>{row.activeAgents}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
