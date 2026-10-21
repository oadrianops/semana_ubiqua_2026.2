import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Circle, MapContainer, Marker, Popup, TileLayer, useMapEvents } from 'react-leaflet';
import { Link } from 'react-router';
import { appConfig } from '../config';

/**
 * Mapa OpenStreetMap (9.6): URL e atribuição da configuração em tempo de execução, atribuição sempre visível com
 * link para os direitos; marcador em SVG (sem os ícones padrão do Leaflet); nenhum tile é guardado pelo service
 * worker. Carregado sob demanda e sempre com alternativa em lista (RNF-07).
 */
export interface MapPoint {
  id: string;
  lat: number;
  lng: number;
  title: string;
  subtitle?: string;
  href?: string;
  radius?: number;
}

function token(name: string, fallback: string) {
  if (typeof document === 'undefined') return fallback;
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

/** Marcador em SVG com as cores dos tokens (Açaí 700 e Tucupi 400), sem os ícones padrão do Leaflet. */
function makePin() {
  const brand = token('--color-brand', 'currentColor');
  const highlight = token('--color-highlight', 'currentColor');
  const surface = token('--color-surface', 'white');
  return L.divIcon({
    className: 'nd-pin',
    html: `<svg width="32" height="40" viewBox="0 0 32 40" aria-hidden="true"><path d="M16 39s13-12.4 13-23A13 13 0 0 0 3 16c0 10.6 13 23 13 23z" fill="${brand}" stroke="${surface}" stroke-width="2"/><circle cx="16" cy="16" r="5" fill="${highlight}"/></svg>`,
    iconSize: [32, 40],
    iconAnchor: [16, 39],
    popupAnchor: [0, -34],
  });
}

function ClickCapture({ onPick }: { onPick?: (lat: number, lng: number) => void }) {
  useMapEvents({ click: (e) => onPick?.(e.latlng.lat, e.latlng.lng) });
  return null;
}

export default function MapView({
  points,
  center,
  zoom = 14,
  height = 320,
  onPick,
  label,
}: {
  points: MapPoint[];
  center?: [number, number];
  zoom?: number;
  height?: number;
  onPick?: (lat: number, lng: number) => void;
  label: string;
}) {
  const { t } = useTranslation();
  const config = appConfig();
  const pin = useMemo(() => makePin(), []);
  const start = useMemo<[number, number]>(
    () => center ?? (points[0] ? [points[0].lat, points[0].lng] : [-1.455, -48.49]),
    [center, points],
  );
  const attribution = `<a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">${config.mapAttribution}</a>`;
  return (
    <figure className="grid gap-1">
      <div
        style={{ height }}
        className="overflow-hidden rounded-[var(--radius-panel)] border border-line"
        role="region"
        aria-label={label}
      >
        <MapContainer
          center={start}
          zoom={zoom}
          scrollWheelZoom={false}
          style={{ height: '100%', width: '100%', overscrollBehavior: 'contain' }}
        >
          <TileLayer url={config.mapTileUrl} attribution={attribution} />
          {points.map((p) => (
            <Marker key={p.id} position={[p.lat, p.lng]} icon={pin} title={p.title}>
              <Popup>
                <strong>{p.title}</strong>
                {p.subtitle && <div>{p.subtitle}</div>}
                {p.href && <Link to={p.href}>{t('map.open')}</Link>}
              </Popup>
            </Marker>
          ))}
          {points
            .filter((p) => p.radius)
            .map((p) => (
              <Circle
                key={`${p.id}-r`}
                center={[p.lat, p.lng]}
                radius={p.radius!}
                pathOptions={{ color: token('--color-brand', 'currentColor'), weight: 2, fillOpacity: 0.08 }}
              />
            ))}
          <ClickCapture onPick={onPick} />
        </MapContainer>
      </div>
      <figcaption className="text-sm text-support">
        <a href="https://www.openstreetmap.org/fixthemap" target="_blank" rel="noopener">
          {t('map.reportProblem')}
        </a>
      </figcaption>
    </figure>
  );
}
