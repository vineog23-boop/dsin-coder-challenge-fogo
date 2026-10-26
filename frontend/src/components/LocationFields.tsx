import { useState } from 'react'
import { REGIONS, type Region } from '../lib/api/equipments'
import { lookupCep } from '../lib/api/viaCep'
import { describedBy } from '../lib/describedBy'
import { regionOf } from '../lib/geo'
import { REGION_LABEL } from '../lib/labels'
import type { LocationForm } from '../lib/locationForm'
import Field from './Field'
import LocationPicker from './LocationPicker'
import TextField from './TextField'

type Props = {
  /** Nome do objeto no JSON (`location`, `baseLocation`): prefixo dos ids e dos erros da API. */
  prefix: string
  legend: string
  value: LocationForm
  errors: Record<string, string>
  onChange: (value: LocationForm) => void
}

const CEP_HINT = 'Preenche rua, bairro e cidade.'

/** Endereço com CEP pelo ViaCEP (falha não bloqueia) e coordenada escolhida no mini-mapa. */
export default function LocationFields({ prefix, legend, value, errors, onChange }: Props) {
  const [cepHint, setCepHint] = useState<string | null>(null)
  const id = (field: string) => `${prefix}.${field}`
  const set = <K extends keyof LocationForm>(key: K, fieldValue: LocationForm[K]) =>
    onChange({ ...value, [key]: fieldValue })

  async function fillFromCep() {
    const digits = value.zipCode.replace(/\D/g, '')
    if (digits.length !== 8) return
    setCepHint('Buscando CEP…')
    const address = await lookupCep(digits)
    if (!address) {
      setCepHint('CEP não encontrado ou serviço indisponível. Preencha o endereço manualmente.')
      return
    }
    setCepHint(null)
    onChange({
      ...value,
      street: address.street || value.street,
      neighborhood: address.neighborhood || value.neighborhood,
      city: address.city || value.city,
      state: address.state || value.state,
    })
  }

  const mapError = errors[id('latitude')] ?? errors[id('longitude')] ?? errors[prefix]
  const zipError = errors[id('zipCode')]

  return (
    <fieldset>
      <legend>{legend}</legend>
      <Field id={id('zipCode')} label="CEP" error={zipError} hint={cepHint ?? CEP_HINT}>
        <input
          id={id('zipCode')}
          inputMode="numeric"
          maxLength={8}
          value={value.zipCode}
          aria-invalid={!!zipError}
          aria-describedby={describedBy(id('zipCode'), zipError, cepHint ?? CEP_HINT)}
          onChange={(e) => set('zipCode', e.target.value.replace(/\D/g, '').slice(0, 8))}
          onBlur={fillFromCep}
        />
      </Field>
      <TextField id={id('street')} label="Rua *" value={value.street} error={errors[id('street')]} maxLength={120}
        onChange={(text) => set('street', text)} />
      <TextField id={id('reference')} label="Referência" value={value.reference} error={errors[id('reference')]}
        maxLength={120} placeholder="número 181, oposto ao 181, S/N…" onChange={(text) => set('reference', text)} />
      <TextField id={id('neighborhood')} label="Bairro *" value={value.neighborhood}
        error={errors[id('neighborhood')]} maxLength={80} onChange={(text) => set('neighborhood', text)} />
      <TextField id={id('city')} label="Cidade *" value={value.city} error={errors[id('city')]} maxLength={80}
        onChange={(text) => set('city', text)} />
      <TextField id={id('state')} label="UF *" value={value.state} error={errors[id('state')]} maxLength={2}
        onChange={(text) => set('state', text.toUpperCase())} />
      <Field id={id('region')} label="Região *" error={errors[id('region')]}
        hint="Sugerida pelo ponto no mapa; pode ajustar.">
        <select id={id('region')} value={value.region} onChange={(e) => set('region', e.target.value as Region)}>
          {REGIONS.map((region) => (
            <option key={region} value={region}>
              {REGION_LABEL[region]}
            </option>
          ))}
        </select>
      </Field>
      <div className={`field field-wide${mapError ? ' field-invalid' : ''}`}>
        <span className="field-label">
          Posição no mapa * <small className="muted">(clique para marcar)</small>
        </span>
        <LocationPicker
          latitude={value.latitude}
          longitude={value.longitude}
          onPick={(latitude, longitude) =>
            onChange({ ...value, latitude, longitude, region: regionOf(latitude, longitude) })
          }
        />
        <small className="muted" aria-live="polite">
          {value.latitude !== null && value.longitude !== null
            ? `Latitude ${value.latitude.toFixed(6)}, longitude ${value.longitude.toFixed(6)}`
            : 'Nenhum ponto marcado.'}
        </small>
        {mapError && <small className="field-error">{mapError}</small>}
      </div>
    </fieldset>
  )
}
