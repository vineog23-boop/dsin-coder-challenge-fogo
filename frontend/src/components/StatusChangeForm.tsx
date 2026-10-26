import { useState, type FormEvent } from 'react'
import { ApiError } from '../lib/api/client'

type Props<S extends string> = {
  current: S
  statuses: readonly S[]
  labels: Record<S, string>
  /** Aviso sobre o efeito da nova situação (ex.: falha torna a manutenção imediata). */
  hint?: (status: S) => string | null
  onSubmit: (status: S, note: string) => Promise<unknown>
  onDone: () => void
  onCancel: () => void
}

/** Mudança de situação com observação; serve a equipamento e agente, que gravam histórico igual. */
export default function StatusChangeForm<S extends string>({
  current,
  statuses,
  labels,
  hint,
  onSubmit,
  onDone,
  onCancel,
}: Props<S>) {
  const options = statuses.filter((status) => status !== current)
  const [status, setStatus] = useState<S>(options[0])
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const warning = hint?.(status)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await onSubmit(status, note.trim())
      onDone()
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
      <select id="new-status" value={status} onChange={(e) => setStatus(e.target.value as S)}>
        {options.map((option) => (
          <option key={option} value={option}>
            {labels[option]}
          </option>
        ))}
      </select>
      {warning && <p className="muted">{warning}</p>}
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
