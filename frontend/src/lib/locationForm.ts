import type { Location, Region } from './api/equipments'

/** Localização como o formulário guarda: texto livre e coordenadas ainda opcionais. */
export type LocationForm = {
  zipCode: string
  street: string
  reference: string
  neighborhood: string
  city: string
  state: string
  region: Region
  latitude: number | null
  longitude: number | null
}

export const EMPTY_LOCATION: LocationForm = {
  zipCode: '',
  street: '',
  reference: '',
  neighborhood: '',
  city: 'Presidente Prudente',
  state: 'SP',
  region: 'CENTER',
  latitude: null,
  longitude: null,
}

export function toLocation(form: LocationForm): Location {
  return {
    zipCode: form.zipCode.replace(/\D/g, '') || null,
    street: form.street.trim(),
    reference: form.reference.trim() || null,
    neighborhood: form.neighborhood.trim(),
    city: form.city.trim(),
    state: form.state.trim().toUpperCase(),
    region: form.region,
    // Sem ponto marcado vai nulo, e a API aponta o erro no campo.
    latitude: form.latitude as number,
    longitude: form.longitude as number,
  }
}

export function fromLocation(location: Location): LocationForm {
  return {
    zipCode: location.zipCode ?? '',
    street: location.street ?? '',
    reference: location.reference ?? '',
    neighborhood: location.neighborhood ?? '',
    city: location.city ?? '',
    state: location.state ?? '',
    region: location.region,
    latitude: location.latitude,
    longitude: location.longitude,
  }
}
