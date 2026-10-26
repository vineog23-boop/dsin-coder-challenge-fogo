import type { EquipmentStatus } from '../lib/api/equipments'
import { STATUS_LABEL } from '../lib/labels'

export default function StatusBadge({ status }: { status: EquipmentStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{STATUS_LABEL[status]}</span>
}
