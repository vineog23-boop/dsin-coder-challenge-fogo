import { describedBy } from '../lib/describedBy'
import Field from './Field'

export default function TextField(props: {
  id: string
  label: string
  value: string
  error?: string
  maxLength?: number
  placeholder?: string
  onChange: (value: string) => void
}) {
  return (
    <Field id={props.id} label={props.label} error={props.error}>
      <input
        id={props.id}
        value={props.value}
        maxLength={props.maxLength}
        placeholder={props.placeholder}
        aria-invalid={!!props.error}
        aria-describedby={describedBy(props.id, props.error)}
        onChange={(e) => props.onChange(e.target.value)}
      />
    </Field>
  )
}
