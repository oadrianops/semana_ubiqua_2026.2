// Configuração em tempo de execução (seção 9.4). Em produção este arquivo é gerado pelo contêiner web
// a partir das variáveis WEB_* (nginx/30-config-js.sh). Este é o padrão do modo de desenvolvimento.
window.__APP_CONFIG__ = {
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
