import { useState, type FormEvent } from 'react'
import { ApiError } from '../lib/api/client'
import { EQUIPMENT_STATUSES, changeStatus, type Equipment, type EquipmentStatus } from '../lib/api/equipments'
import { STATUS_LABEL } from '../lib/labels'

type Props = {
  equipment: Equipment
  onDone: (updated: Equipment) => void
  onCancel: () => void
}

export default function StatusChangeForm({ equipment, onDone, onCancel }: Props) {
  const options = EQUIPMENT_STATUSES.filter((status) => status !== equipment.status)
  const [status, setStatus] = useState<EquipmentStatus>(options[0])
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      onDone(await changeStatus(equipment.id, status, note.trim()))
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Não foi possível mudar a situação.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="action-form" onSubmit={handleSubmit} aria-labelledby="status-form-title">
      <h3 id="status-form-title">Mudar situação</h3>
      <label htmlFor="new-status">Nova situação</label>
      <select id="new-status" value={status} onChange={(e) => setStatus(e.target.value as EquipmentStatus)}>
        {options.map((option) => (
          <option key={option} value={option}>
            {STATUS_LABEL[option]}
          </option>
        ))}
      </select>
      {status === 'MALFUNCTION' && <p className="muted">A manutenção passa a ser imediata.</p>}
      <label htmlFor="status-note">Observação</label>
      <textarea id="status-note" maxLength={500} rows={2} value={note} onChange={(e) => setNote(e.target.value)} />
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <div className="form-actions">
        <button type="button" onClick={onCancel}>
          Cancelar
        </button>
        <button type="submit" disabled={submitting}>
          {submitting ? 'Salvando…' : 'Confirmar'}
        </button>
      </div>
    </form>
  )
}
