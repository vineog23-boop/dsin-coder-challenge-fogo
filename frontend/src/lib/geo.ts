import type { Region } from './api/equipments'

/** Centro de Presidente Prudente, ponto inicial dos mapas. */
export const CITY_CENTER: [number, number] = [-22.1256, -51.3889]

const HALF_HEIGHT = 0.035
const HALF_WIDTH = 0.045
const CENTER_RADIUS = 0.3

/** Região sugerida pelo quadrante em relação ao centro; mesma regra dos dados de demonstração. */
export function regionOf(lat: number, lng: number): Region {
  const dy = (lat - CITY_CENTER[0]) / HALF_HEIGHT
  const dx = (lng - CITY_CENTER[1]) / HALF_WIDTH
  if (Math.hypot(dx, dy) < CENTER_RADIUS) return 'CENTER'
  if (Math.abs(dy) > Math.abs(dx)) return dy > 0 ? 'NORTH' : 'SOUTH'
  return dx > 0 ? 'EAST' : 'WEST'
}
