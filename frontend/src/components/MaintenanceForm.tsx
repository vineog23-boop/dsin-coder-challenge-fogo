import { useState, type FormEvent } from 'react'
import { ApiError } from '../lib/api/client'
import { MAINTENANCE_KINDS, registerMaintenance, type MaintenanceKind } from '../lib/api/equipments'
import { MAINTENANCE_KIND_LABEL } from '../lib/labels'

type Props = {
  equipmentId: string
  onDone: () => void
  onCancel: () => void
}

/** `datetime-local` no formato do input, no horário de quem está usando. */
function nowForInput(): string {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 16)
}

export default function MaintenanceForm({ equipmentId, onDone, onCancel }: Props) {
  const [kind, setKind] = useState<MaintenanceKind>('CORRECTIVE')
  const [performedAt, setPerformedAt] = useState(nowForInput)
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await registerMaintenance(equipmentId, {
        kind,
        performedAt: new Date(performedAt).toISOString(),
        description: description.trim(),
      })
      onDone()
    } catch (e) {
      if (e instanceof ApiError && e.problem.errors?.length) {
        setError(e.problem.errors.map((fieldError) => fieldError.message).join(' '))
      } else {
        setError(e instanceof ApiError ? e.message : 'Não foi possível registrar a manutenção.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="action-form" onSubmit={handleSubmit} aria-labelledby="maintenance-form-title">
      <h3 id="maintenance-form-title">Registrar manutenção</h3>
      <label htmlFor="maintenance-kind">Tipo</label>
      <select id="maintenance-kind" value={kind} onChange={(e) => setKind(e.target.value as MaintenanceKind)}>
        {MAINTENANCE_KINDS.map((option) => (
          <option key={option} value={option}>
            {MAINTENANCE_KIND_LABEL[option]}
          </option>
        ))}
      </select>
      <label htmlFor="maintenance-date">Realizada em</label>
      <input
        id="maintenance-date"
        type="datetime-local"
        required
        max={nowForInput()}
        value={performedAt}
        onChange={(e) => setPerformedAt(e.target.value)}
      />
      <label htmlFor="maintenance-description">Descrição</label>
      <textarea
        id="maintenance-description"
        required
        maxLength={500}
        rows={2}
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
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
          {submitting ? 'Salvando…' : 'Registrar'}
        </button>
      </div>
    </form>
  )
}
