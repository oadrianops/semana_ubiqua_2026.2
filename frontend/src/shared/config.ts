/** Configuração em tempo de execução, carregada de /config.js (seção 9.4): uma imagem serve todos os ambientes. */
export interface AppConfig {
  appName: string;
  apiBaseUrl: string;
  mapTileUrl: string;
  mapAttribution: string;
  directionsUrlTemplate: string;
  contactEmail: string;
  vapidPublicKey: string;
  webVitalsSampleRate: number;
  demoNotice: boolean;
}

declare global {
  interface Window {
    __APP_CONFIG__?: Partial<AppConfig>;
  }
}

const defaults: AppConfig = {
  appName: 'NANDESK',
  apiBaseUrl: '/api',
  mapTileUrl: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  mapAttribution: '© OpenStreetMap contributors',
  directionsUrlTemplate: 'https://www.google.com/maps/dir/?api=1&destination={lat},{lng}',
  contactEmail: 'contato@nandesk.com.br',
  vapidPublicKey: '',
  webVitalsSampleRate: 0.1,
  demoNotice: true,
};

/** Marcadores sem os quais a URL não serve: sem eles o mapa fica sem tiles e o "Como chegar" abre um destino inválido. */
const required: Partial<Record<keyof AppConfig, string[]>> = {
  mapTileUrl: ['{z}', '{x}', '{y}'],
  directionsUrlTemplate: ['{lat}', '{lng}'],
};

export function appConfig(): AppConfig {
  const runtime = typeof window === 'undefined' ? {} : (window.__APP_CONFIG__ ?? {});
  const merged: AppConfig = { ...defaults, ...runtime };
  for (const [key, markers] of Object.entries(required) as [keyof AppConfig, string[]][]) {
    const value = merged[key];
    if (typeof value !== 'string' || !markers.every((m) => value.includes(m))) {
      Object.assign(merged, { [key]: defaults[key] });
    }
  }
  return merged;
}

export function directionsUrl(lat: number | string, lng: number | string): string {
  return appConfig().directionsUrlTemplate.replace('{lat}', String(lat)).replace('{lng}', String(lng));
}
