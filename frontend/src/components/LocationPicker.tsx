import 'leaflet/dist/leaflet.css'
import { CircleMarker, MapContainer, TileLayer, useMapEvents } from 'react-leaflet'
import { CITY_CENTER } from '../lib/geo'

type Props = {
  latitude: number | null
  longitude: number | null
  onPick?: (latitude: number, longitude: number) => void
  height?: number
}

function ClickHandler({ onPick }: { onPick: (latitude: number, longitude: number) => void }) {
  useMapEvents({
    click: (event) => onPick(round6(event.latlng.lat), round6(event.latlng.lng)),
  })
  return null
}

function round6(value: number) {
  return Math.round(value * 1e6) / 1e6
}

/** Mini-mapa: mostra o ponto e, com `onPick`, troca a coordenada a cada clique. */
export default function LocationPicker({ latitude, longitude, onPick, height = 260 }: Props) {
  const hasPoint = latitude !== null && longitude !== null
  const center: [number, number] = hasPoint ? [latitude, longitude] : CITY_CENTER

  return (
    <div className="mini-map" style={{ height }}>
      <MapContainer center={center} zoom={hasPoint ? 15 : 13} scrollWheelZoom={false} style={{ height: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {hasPoint && (
          <CircleMarker center={[latitude, longitude]} radius={9} pathOptions={{ color: '#c2410c', fillOpacity: 0.6 }} />
        )}
        {onPick && <ClickHandler onPick={onPick} />}
      </MapContainer>
    </div>
  )
}
