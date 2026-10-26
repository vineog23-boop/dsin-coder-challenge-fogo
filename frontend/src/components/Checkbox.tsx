export default function Checkbox(props: { id: string; label: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <label className="checkbox" htmlFor={props.id}>
      <input
        id={props.id}
        type="checkbox"
        checked={props.checked}
        onChange={(e) => props.onChange(e.target.checked)}
      />
      {props.label}
    </label>
  )
}
