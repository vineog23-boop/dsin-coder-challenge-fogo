import type { ReactNode } from 'react'

type Props = {
  id: string
  label: string
  error?: string
  hint?: string
  children: ReactNode
}

/**
 * Rótulo, controle e mensagem de erro ligados por id. O controle filho deve usar `id`,
 * `aria-invalid` e `aria-describedby={describedBy(id, error)}` (de `lib/describedBy`) para o leitor de tela anunciar o erro.
 */
export default function Field({ id, label, error, hint, children }: Props) {
  return (
    <div className={`field${error ? ' field-invalid' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {children}
      {hint && !error && (
        <small id={`${id}-hint`} className="muted">
          {hint}
        </small>
      )}
      {error && (
        <small id={`${id}-error`} className="field-error">
          {error}
        </small>
      )}
    </div>
  )
}
